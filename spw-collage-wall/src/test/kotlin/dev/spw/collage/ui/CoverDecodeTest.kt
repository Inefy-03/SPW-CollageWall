package dev.spw.collage.ui

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CoverDecodeTest {

    private fun metrics(pitch: Int) = WallMetrics.forCell(pitch - 8, 8)

    @Test
    fun `budget covers the largest card plus hover`() {
        val metrics = metrics(pitch = 120)
        val largestCard = metrics.span(6f)
        val budget = CoverDecode.budgetFor(metrics)
        assertTrue(budget >= largestCard * 1.05f, "budget $budget must resolve a hovered 6x6 card")
        assertEquals(0, budget and 63, "budget should be quantised to survive small resizes")
    }

    @Test
    fun `budget grows with the pitch`() {
        val small = CoverDecode.budgetFor(metrics(pitch = 100))
        val large = CoverDecode.budgetFor(metrics(pitch = 200))
        assertTrue(large > small)
    }

    @Test
    fun `oversized square cover is capped on both sides`() {
        assertEquals(1008 to 1008, CoverDecode.targetSize(1500, 1500, 1008))
    }

    @Test
    fun `portrait cover keeps its shape and hits the budget on the short side`() {
        assertEquals(1008 to 1680, CoverDecode.targetSize(1200, 2000, 1008))
    }

    @Test
    fun `landscape cover keeps its shape and hits the budget on the short side`() {
        assertEquals(1680 to 1008, CoverDecode.targetSize(2000, 1200, 1008))
    }

    @Test
    fun `smaller short side is left alone even when the long side is huge`() {
        // A banner cover is cropped, not resolved, along its long side: the shorter
        // side is the only axis any card can draw 1:1.
        assertNull(CoverDecode.targetSize(4000, 1000, 1008))
    }

    @Test
    fun `cover that already fits is not touched`() {
        assertNull(CoverDecode.targetSize(1000, 1000, 1008))
        assertNull(CoverDecode.targetSize(600, 900, 1008))
    }
}
