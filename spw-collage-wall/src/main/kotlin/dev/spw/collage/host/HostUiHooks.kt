@file:OptIn(com.xuncorp.spw.workshop.api.UnstableSpwWorkshopApi::class)

package dev.spw.collage.host

import com.xuncorp.spw.workshop.api.WorkshopApi
import com.xuncorp.spw.workshop.api.hook.HookHandle
import com.xuncorp.spw.workshop.api.hook.MethodHook
import com.xuncorp.spw.workshop.api.hook.MethodHookParam
import dev.spw.collage.CollageController

/** Installs a complete hook set or rolls back every partially installed handle. */
class HostUiHooks(
    private val bridge: HostUiBridge,
    private val controller: CollageController
) : AutoCloseable {
    private val handles = mutableListOf<HookHandle>()

    fun install() {
        try {
            for (key in bridge.targets.navigation() + bridge.targets.back() +
                bridge.targets.closeWindow()) {
                val reflected = bridge.method(key)
                handles += WorkshopApi.hookRegistrar.hookMethod(
                    key.owner().replace('/', '.'),
                    key.name(),
                    reflected.parameterTypes.map { it.name },
                    object : MethodHook(80) {
                        override fun before(call: MethodHookParam) {
                            if (bridge.replaying || !controller.active) return
                            if (key == bridge.targets.back() && bridge.playerExpanded()) return
                            call.setResult(
                                if (key == bridge.targets.closeWindow()) Unit else null
                            )
                            controller.close()
                        }
                    }
                )
            }

            val page = bridge.targets.page()
            handles += WorkshopApi.hookRegistrar.replaceComposable(
                page.target().owner().replace('/', '.'),
                page.target().name(),
                page.caller().replace('/', '.'),
                parameterTypes = emptyList(),
                // A replacement registered at the same priority as an earlier instance loses
                // to it ("earliest registration wins"), which would keep a stale page alive
                // after an in-place update. Stay above the priority previous builds used.
                priority = 100
            ) {
                CollageSurface(bridge, controller)
            }

            val entry = bridge.targets.captionEntry()
            handles += WorkshopApi.hookRegistrar.afterComposable(
                entry.target().owner().replace('/', '.'),
                entry.target().name(),
                entry.caller().replace('/', '.'),
                parameterTypes = emptyList(),
                priority = 100
            ) {
                bridge.Entry()
            }
        } catch (failure: Throwable) {
            runCatching(::close).onFailure(failure::addSuppressed)
            throw failure
        }
    }

    override fun close() {
        handles.asReversed().forEach { handle -> runCatching { handle.close() } }
        handles.clear()
    }
}
