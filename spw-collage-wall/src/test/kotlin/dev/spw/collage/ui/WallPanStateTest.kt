package dev.spw.collage.ui

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * The wall opens in the middle of the lattice, which is only stable if the bounds are
 * allowed to re-centre while the library is still loading and the window is still being
 * measured. Once a hand touches the wall the position must be kept instead.
 */
class WallPanStateTest {

    @Test
    fun `opens in the middle of the lattice`() {
        val state = WallPanState()
        state.bounds(contentWidth = 3000f, contentHeight = 800f, viewportWidth = 1000f, viewportHeight = 800f)

        assertEquals(1000f, state.x, "the window should sit halfway across the lattice")
        assertEquals(0f, state.y, "the vertical axis keeps the library order, so it starts at the top")
        assertFalse(state.touched)
    }

    @Test
    fun `keeps re-centring while the library and the window are still settling`() {
        val state = WallPanState()
        state.bounds(1600f, 600f, 1000f, 800f)
        assertEquals(300f, state.x)

        // The tracks arrive after the first frame: the lattice grows and must be re-centred.
        state.bounds(3000f, 600f, 1000f, 800f)
        assertEquals(1000f, state.x)

        // A window resize to the very same effect.
        state.bounds(3000f, 600f, 1200f, 800f)
        assertEquals(900f, state.x)
    }

    @Test
    fun `keeps the hand position once the wall has been panned`() {
        val state = WallPanState()
        state.bounds(3000f, 800f, 1000f, 800f)
        state.moveTo(180f, 0f)
        assertTrue(state.touched)

        state.bounds(3000f, 800f, 1000f, 800f)
        assertEquals(180f, state.x, "a resize must not yank the wall back to the middle")

        // Shrinking the lattice clamps the old position instead of losing it.
        state.bounds(1100f, 800f, 1000f, 800f)
        assertEquals(100f, state.x)
    }

    @Test
    fun `cannot drift when the lattice fits the window`() {
        val state = WallPanState()
        state.bounds(820f, 700f, 1000f, 800f)

        assertEquals(0f, state.limitX)
        assertEquals(0f, state.x)
        state.moveTo(-240f, -120f)
        assertEquals(0f, state.x)
        assertEquals(0f, state.y)
    }

    @Test
    fun `a repeating axis drifts freely while the fold recycles the content`() {
        val state = WallPanState()
        state.bounds(3000f, 600f, 1000f, 800f, repeatX = true)

        assertEquals(1000f, state.x, "a repeating axis still opens centred")

        // The scalar never wraps: wrapping it once per turn would rename every visible
        // block and recompose the whole screen mid-slide. The caller folds block indices,
        // so the content loops no matter how far the scalar drifts.
        state.moveBy(2400f, 0f, horizontal = true)
        assertEquals(3400f, state.x)

        state.moveBy(-800f, 0f, horizontal = true)
        assertEquals(2600f, state.x, "and backwards as well, unclamped")
    }

    @Test
    fun `a single column repeats only down the wall`() {
        val state = WallPanState()
        state.bounds(900f, 4000f, 900f, 800f, repeatY = true)

        state.moveBy(600f, 3900f, horizontal = false)
        assertEquals(0f, state.x, "the vertical wall must not drift sideways")
        assertEquals(3900f, state.y)

        state.moveBy(0f, 200f, horizontal = false)
        assertEquals(4100f, state.y, "the scalar keeps drifting past the turn; the fold recycles the songs")
    }

    @Test
    fun `a repeating wall draws exactly the blocks the window touches`() {
        // 100 px blocks under a 250 px window: three blocks are always in sight, and the
        // ones past the end of the turn are named by index and folded back by the caller.
        listOf(0f, 1f, 99f, 100f, 150f, 299f).forEach { pan ->
            val range = visibleBlocks(pan, step = 100, count = 3, viewport = 250, repeat = true)
            assertTrue(range.first * 100 <= pan, "pan $pan misses the block on its left")
            assertTrue((range.last + 1) * 100 >= pan + 250f, "pan $pan stops short on the right")
            assertTrue(range.all { Math.floorMod(it, 3) in 0..2 })
        }
    }

    @Test
    fun `a clamped wall never names a block that is not there`() {
        assertEquals(
            0..2,
            visibleBlocks(pan = 0f, step = 100, count = 3, viewport = 250, repeat = false)
        )
        assertEquals(
            2..2,
            visibleBlocks(pan = 400f, step = 100, count = 3, viewport = 250, repeat = false)
        )
    }

    @Test
    fun `wheel pans one axis at a time`() {
        val state = WallPanState()
        state.bounds(3000f, 2400f, 1000f, 800f)

        state.moveBy(120f, 0f, horizontal = true)
        assertEquals(1120f, state.x, "the centred position is the starting point of a wheel scroll")
        state.moveBy(120f, 0f, horizontal = false)
        assertEquals(
            1120f,
            state.x,
            "a vertical-only move must leave the horizontal position alone, not park it at zero"
        )
    }
}
