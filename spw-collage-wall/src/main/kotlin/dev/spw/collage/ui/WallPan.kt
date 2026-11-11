package dev.spw.collage.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.exp
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Where the wall is looking, in pixels.
 *
 * Each axis either clamps to the lattice it holds, or drifts freely: a repeating axis's
 * content is periodic and the caller folds block indices, so the scalar keeps going and
 * the same songs meet the window again from the start.
 */
internal class WallPanState {
    var x by mutableStateOf(0f)
        private set
    var y by mutableStateOf(0f)
        private set
    var limitX by mutableStateOf(0f)
        private set
    var limitY by mutableStateOf(0f)
        private set

    /** Whether each axis repeats (drifts freely) or clamps to the lattice it has. */
    private var repeatX = false
    private var repeatY = false

    /**
     * True as soon as the wall has been panned by hand.
     *
     * Until then every bounds update re-centres: the library and the window size both
     * arrive after the first frame, so centring only once would leave the wall stuck
     * against the left edge of the very first (empty) measurement.
     */
    var touched: Boolean = false
        private set

    /** Records the lattice and viewport sizes; the window is pulled back into range. */
    fun bounds(
        contentWidth: Float,
        contentHeight: Float,
        viewportWidth: Float,
        viewportHeight: Float,
        repeatX: Boolean = false,
        repeatY: Boolean = false
    ) {
        this.repeatX = repeatX
        this.repeatY = repeatY
        limitX = (contentWidth - viewportWidth).coerceAtLeast(0f)
        limitY = (contentHeight - viewportHeight).coerceAtLeast(0f)
        if (touched) {
            x = place(x, this.repeatX, limitX)
            y = place(y, this.repeatY, limitY)
        } else {
            // Open in the middle of the lattice, so both sides show cut-off cards and the
            // wall reads as a canvas that continues in every direction. Vertically it stays
            // at the top: that axis carries the library order.
            x = limitX / 2f
            y = 0f
        }
    }

    /** Jumps to a position, clamped where the axis has an edge. Used by the pan gesture. */
    fun moveTo(nextX: Float, nextY: Float, horizontal: Boolean = true) {
        touched = true
        // "horizontal = false" means "this move must not touch the horizontal axis", not
        // "park it at zero": zeroing it here teleported the wall to the far left whenever a
        // vertical fling or a vertical-only drag ran.
        if (horizontal) x = place(nextX, repeatX, limitX)
        y = place(nextY, repeatY, limitY)
    }

    fun moveBy(dx: Float, dy: Float, horizontal: Boolean) {
        moveTo(x + dx, y + dy, horizontal)
    }

    /**
     * A repeating axis drifts freely: its content is periodic (the caller folds block
     * indices with a floor modulus), so the scalar itself never needs to wrap. Wrapping it
     * renamed every visible block once per turn, which recomposed the whole screen mid-slide
     * and read as the wall snapping back. A clamped axis sticks to the lattice.
     */
    private fun place(value: Float, repeat: Boolean, limit: Float): Float =
        if (repeat) value else value.coerceIn(0f, limit)
}

@Composable
internal fun rememberWallPanState(): WallPanState = remember { WallPanState() }

/**
 * The blocks that touch the viewport on one axis, as unbounded indices.
 *
 * A repeating axis returns indices outside `0 until count` on purpose: the caller folds
 * them back with a floor modulus, which is what puts the beginning of the wall on the far
 * side of the end. A clamped axis only ever names the blocks it actually has.
 */
internal fun visibleBlocks(
    pan: Float,
    step: Int,
    count: Int,
    viewport: Int,
    repeat: Boolean
): IntRange {
    val start = floor(pan / step).toInt()
    val stop = floor((pan + viewport) / step).toInt()
    val last = (count - 1).coerceAtLeast(0)
    return if (repeat) start..stop else start.coerceIn(0, last)..stop.coerceIn(0, last)
}

/**
 * Free panning across the wall: wheel in both axes, drag in both axes, and a fling when
 * the pointer is released.
 *
 * The wall reads the gesture itself, because the replaced SPW page under it consumes
 * pointer changes on the initial pass and a scroll container behind it would drop them.
 * Drags stay unconsumed below the touch slop so a card still gets its click.
 */
@Composable
internal fun Modifier.wallPan(
    state: WallPanState,
    horizontal: Boolean,
    wheelStep: Dp = 72.dp
): Modifier {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    return this.pointerInput(state, horizontal, wheelStep, density) {
        val step = with(density) { wheelStep.toPx() }
        var glideJob: Job? = null
        fun stopGlide() {
            glideJob?.cancel()
            glideJob = null
        }
        awaitPointerEventScope {
            // Compose's tracker fits the last ~100 ms of pointer samples, so the flick speed
            // does not depend on how big the final step happened to be. Dividing the last two
            // events by hand read as "still" whenever the pointer was already easing off, and
            // that is why the glide only fired on some releases.
            val tracker = VelocityTracker()
            var dragging = false
            var lastX = 0f
            var lastY = 0f
            var travelled = 0f
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                when (event.type) {
                    PointerEventType.Scroll -> {
                        val dx = event.changes.fold(0f) { acc, change ->
                            acc + change.scrollDelta.x
                        }
                        val dy = event.changes.fold(0f) { acc, change ->
                            acc + change.scrollDelta.y
                        }
                        if (dx != 0f || dy != 0f) {
                            stopGlide()
                            state.moveBy(dx * step, dy * step, horizontal)
                            event.changes.forEach { it.consume() }
                        }
                    }

                    PointerEventType.Press -> {
                        stopGlide()
                        dragging = false
                        travelled = 0f
                        tracker.resetTracking()
                        event.changes.firstOrNull()?.let { change ->
                            lastX = change.position.x
                            lastY = change.position.y
                            tracker.addPosition(change.uptimeMillis, change.position)
                        }
                    }

                    PointerEventType.Move -> {
                        val change = event.changes.firstOrNull() ?: continue
                        if (!change.pressed) continue
                        val dx = change.position.x - lastX
                        val dy = change.position.y - lastY
                        lastX = change.position.x
                        lastY = change.position.y
                        travelled += abs(dx) + abs(dy)
                        if (!dragging && travelled > viewConfiguration.touchSlop) dragging = true
                        if (dragging) {
                            tracker.addPosition(change.uptimeMillis, change.position)
                            state.moveBy(-dx, -dy, horizontal)
                            event.changes.forEach { it.consume() }
                        }
                    }

                    PointerEventType.Release -> {
                        if (dragging) {
                            val velocity = tracker.calculateVelocity()
                            val speedX = if (horizontal) -velocity.x else 0f
                            val speedY = -velocity.y
                            if (abs(speedX) > MinFlingSpeed || abs(speedY) > MinFlingSpeed) {
                                // Both axes share one glide: two independent jobs fought over
                                // the same state and each clamped the other's frame.
                                glideJob = scope.launch {
                                    glideToStop(state, speedX, speedY, horizontal)
                                }
                            }
                        }
                        dragging = false
                        travelled = 0f
                    }
                }
            }
        }
    }
}

/** Below this the release is a tap, not a flick. */
private const val MinFlingSpeed = 250f

/** Exponential damping; at 4.5 a 2000 px/s flick travels about 380 px in its first half second. */
private const val FlingDecay = 4.5f

/** The glide ends once both axes fall under this, which is slow enough to read as a stop. */
private const val FlingStopSpeed = 30f

/** Exponential glide on both axes at once, ending where the wall runs out of room. */
private suspend fun glideToStop(
    state: WallPanState,
    speedX: Float,
    speedY: Float,
    horizontal: Boolean
) {
    var vx = speedX
    var vy = speedY
    var frame = 0L
    while (abs(vx) > FlingStopSpeed || abs(vy) > FlingStopSpeed) {
        withFrameNanos { now ->
            if (frame != 0L) {
                val seconds = (now - frame) / 1_000_000_000f
                vx *= exp(-FlingDecay * seconds)
                vy *= exp(-FlingDecay * seconds)
                val beforeX = state.x
                val beforeY = state.y
                state.moveBy(vx * seconds, vy * seconds, horizontal)
                // A clamping axis that did not move has reached its edge; a free one always
                // moves, so the glide runs its course and decays to a stop.
                if (state.x == beforeX) vx = 0f
                if (state.y == beforeY) vy = 0f
            }
            frame = now
        }
    }
}
