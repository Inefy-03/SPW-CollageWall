@file:OptIn(androidx.compose.runtime.InternalComposeApi::class)

package dev.spw.collage.host

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.currentComposer
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.clearAndSetSemantics
import dev.spw.collage.CollageController
import dev.spw.collage.ui.CollageWall

/** Keeps SPW's page/state in composition while the wall owns the content area. */
@Composable
fun CollageSurface(
    bridge: HostUiBridge,
    controller: CollageController
) {
    val active = controller.active
    // Tells the entry hook that the replaced page is really running, so it can stop trying
    // to repair a page that was composed before this plugin was loaded.
    SideEffect { bridge.markPageHosted() }
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier.fillMaxSize()
                .drawWithContent { if (!active) drawContent() }
                .focusProperties { canFocus = !active }
                .pointerInput(active) {
                    if (active) awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            // Clicks stay blocked so the hidden host page cannot react under
                            // the wall, but wheel, drag and hover changes must reach the wall:
                            // LazyColumn's wheel logic drops any change that is already
                            // consumed, and cards could never notice the pointer otherwise.
                            if (event.type == PointerEventType.Scroll ||
                                event.type == PointerEventType.Move ||
                                event.type == PointerEventType.Enter ||
                                event.type == PointerEventType.Exit
                            ) {
                                continue
                            }
                            event.changes.forEach { it.consume() }
                        }
                    }
                }
                .then(if (active) Modifier.clearAndSetSemantics {} else Modifier)
        ) {
            bridge.HostPage()
            bridge.capture(currentComposer)
        }
        if (active) {
            // First click can precede the host page's first navigation composition. Retry
            // once captured rather than throwing through the click handler.
            androidx.compose.runtime.LaunchedEffect(active) {
                repeat(20) {
                    if (bridge.enter()) return@LaunchedEffect
                    androidx.compose.runtime.withFrameNanos { }
                }
            }
        }
        if (active) {
            CollageWall(controller, bridge.isDarkTheme())
        }
    }
}
