@file:OptIn(
    androidx.compose.runtime.InternalComposeApi::class,
    com.xuncorp.spw.workshop.api.UnstableSpwWorkshopApi::class
)

package dev.spw.collage.host

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocal
import androidx.compose.runtime.Composer
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.currentComposer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xuncorp.spw.workshop.api.WorkshopApi
import dev.spw.collage.CollageController
import dev.spw.collage.CollageLog
import java.lang.invoke.MethodType
import java.lang.reflect.Field
import java.lang.reflect.Method

/** The only boundary that translates discovered JVM members into Compose calls. */
class HostUiBridge(
    val targets: HostUiDiscovery.Targets,
    loader: ClassLoader,
    private val controller: CollageController
) {
    private val targetsClassLoader = loader
    private val caption = method(targets.captionControl())
    private val page = method(targets.page().target())
    private val themeConfigs = method(targets.theme().configs())
    private val themeDark = method(targets.theme().dark())
    private val navigate = method(targets.navigate())
    private val collapsePlayer = method(targets.collapsePlayer())
    private val playerState = field(targets.playerState())
    private val playerExpanded = method(targets.playerExpanded())
    private val stack = field(targets.backStack())
    private val navigatorType = stack.declaringClass
    private val locals = targets.locals().map(::field)

    var replaying = false
        private set
    private var navigator: Any? = null

    /**
     * True once the replaced page component has actually composed.
     *
     * A retransformed call site only changes behaviour on its next recomposition. When the
     * plugin is loaded while the page is already on screen, the host's hook publication may
     * invalidate compositions before the retransform lands, so the original page stays and
     * nothing re-runs it. This flag lets the entry button notice that and repair it.
     */
    var pageHosted by mutableStateOf(false)
        private set

    fun markPageHosted() {
        if (!pageHosted) {
            pageHosted = true
            CollageLog.info("host page hook is live")
        }
    }

    fun method(key: HostBytecodeIndex.Method): Method {
        val signature = MethodType.fromMethodDescriptorString(
            key.descriptor(), targetsClassLoader
        )
        return Class.forName(key.owner().replace('/', '.'), false, targetsClassLoader)
            .getDeclaredMethod(key.name(), *signature.parameterArray())
            .also {
                check(it.returnType == signature.returnType() && it.trySetAccessible()) {
                    "SPW UI method is inaccessible: ${key.owner()}.${key.name()}"
                }
            }
    }

    private fun field(key: HostBytecodeIndex.Field): Field =
        Class.forName(key.owner().replace('/', '.'), false, targetsClassLoader)
            .getDeclaredField(key.name())
            .also {
                check(it.type.descriptorString() == key.descriptor() && it.trySetAccessible()) {
                    "SPW UI state is inaccessible: ${key.owner()}.${key.name()}"
                }
            }

    fun capture(composer: Composer): Boolean {
        for (field in locals) {
            val local = field.get(null) as CompositionLocal<*>
            val value = runCatching { composer.consume(local) }.getOrNull()
            if (navigatorType.isInstance(value)) {
                navigator = value
                return true
            }
        }
        return false
    }

    /** Best-effort refresh; a click may legitimately arrive before navigation is composed. */
    fun enter(): Boolean {
        val host = navigator ?: return false
        val current = (stack.get(host) as? List<*>)?.lastOrNull() ?: return false
        replay {
            collapsePlayer.invoke(host)
            navigate.invoke(host, current)
        }
        return true
    }

    /**
     * Re-navigates to the route the user is already on, which rebuilds the page subtree and
     * therefore re-invokes the call site we replaced. Returns false while navigation is not
     * captured yet, so callers can retry instead of assuming the page is unreachable.
     */
    fun refreshPage(): Boolean = runCatching { enter() }.getOrDefault(false)

    fun replay(action: () -> Unit) {
        replaying = true
        try {
            action()
        } finally {
            replaying = false
        }
    }

    fun hasNavigator(): Boolean = navigator != null

    fun navigatorValue(): Any = checkNotNull(navigator) { "SPW navigation context is unavailable" }

    fun playerExpanded(): Boolean = navigator?.let {
        playerExpanded.invoke(playerState.get(it)) as Boolean
    } == true

    @Composable
    fun isDarkTheme(): Boolean =
        themeDark.invoke(themeConfigs.invoke(null, currentComposer)) as Boolean

    @Composable
    fun HostPage() {
        page.invoke(null, currentComposer, 0)
    }

    @Composable
    fun Entry() {
        capture(currentComposer)
        recoverPage()
        val openMenu: () -> Unit = controller::open
        val icon: @Composable BoxScope.() -> Unit = {
            Box(Modifier.align(Alignment.Center).size(20.dp)) {
                BasicText(
                    "拼",
                    Modifier.align(Alignment.Center),
                    TextStyle(
                        color = if (isDarkTheme()) Color.White else Color.Black,
                        fontSize = 15.sp
                    )
                )
            }
        }
        caption.invoke(
            null, openMenu, "拼贴", Modifier, true, null, 0f,
            icon, currentComposer, 0, 32
        )
    }

    /**
     * Keeps the entry usable after a hot install or update.
     *
     * The entry lives on the caption bar, which the host keeps recomposing, so it is the one
     * place that is guaranteed to run when the plugin was loaded into a running player. From
     * here we can rebuild the page subtree until the replaced call site finally takes over.
     * On a normal start the page composes in the same frame and this costs nothing.
     */
    @Composable
    private fun recoverPage() {
        if (pageHosted) return
        LaunchedEffect(Unit) {
            // A normal start hosts the page in the first frame; only act when it did not.
            repeat(3) { withFrameNanos { } }
            repeat(3) {
                if (pageHosted) return@LaunchedEffect
                if (refreshPage()) CollageLog.info("page refresh requested after late hook install")
                repeat(4) { withFrameNanos { } }
            }
        }
        val opened = controller.active
        LaunchedEffect(opened) {
            if (!opened) return@LaunchedEffect
            repeat(3) {
                if (pageHosted) return@LaunchedEffect
                refreshPage()
                repeat(4) { withFrameNanos { } }
            }
            if (!pageHosted) {
                CollageLog.info("page hook never ran; the player has to be restarted once")
                WorkshopApi.ui.toast(
                    "墙面未生效：此 SPW 版本需重启一次才能载入插件的界面",
                    WorkshopApi.Ui.ToastType.Warning
                )
            }
        }
    }
}
