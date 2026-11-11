package dev.spw.collage.ui

import dev.spw.collage.CollageLayout
import dev.spw.collage.GridRect
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * The wall's whole point is that every seam is the same width. That only holds if offsets
 * and sizes come from one integer pitch: measuring the gap and the card separately rounds
 * them independently, which is what made neighbouring gaps differ by a pixel or two.
 */
class WallMetricsTest {

    private data class Px(val left: Int, val top: Int, val right: Int, val bottom: Int)

    private fun place(metrics: WallMetrics, rect: GridRect) = Px(
        left = metrics.startX(rect.x.toFloat()),
        top = metrics.startY(rect.y.toFloat()),
        right = metrics.startX(rect.x.toFloat()) + metrics.span(rect.cols.toFloat()),
        bottom = metrics.startY(rect.y.toFloat()) + metrics.span(rect.rows.toFloat())
    )

    /** Owner of every cell, so a seam can be measured where two different cards meet. */
    private fun owners(rects: List<GridRect>): Array<Array<Int>> {
        val owners = Array(CollageLayout.BLOCK_COLS) { Array(CollageLayout.BLOCK_ROWS) { -1 } }
        rects.forEachIndexed { index, rect ->
            for (y in rect.y until rect.y + rect.rows) {
                for (x in rect.x until rect.x + rect.cols) owners[x][y] = index
            }
        }
        return owners
    }

    private fun assertUniformSeams(metrics: WallMetrics, rects: List<GridRect>, label: String) {
        val tiles = rects.map { place(metrics, it) }
        val owner = owners(rects)
        for (x in 0 until CollageLayout.BLOCK_COLS - 1) {
            for (y in 0 until CollageLayout.BLOCK_ROWS) {
                val left = owner[x][y]
                val right = owner[x + 1][y]
                if (left == right) continue
                assertEquals(
                    metrics.gap,
                    tiles[right].left - tiles[left].right,
                    "$label: horizontal seam between cards $left and $right at $x,$y"
                )
            }
        }
        for (x in 0 until CollageLayout.BLOCK_COLS) {
            for (y in 0 until CollageLayout.BLOCK_ROWS - 1) {
                val top = owner[x][y]
                val bottom = owner[x][y + 1]
                if (top == bottom) continue
                assertEquals(
                    metrics.gap,
                    tiles[bottom].top - tiles[top].bottom,
                    "$label: vertical seam between cards $top and $bottom at $x,$y"
                )
            }
        }
    }

    @Test
    fun `every seam of the idle wall is the same width`() {
        val metrics = WallMetrics.forCell(cell = 114, gap = 12)
        for (block in 0 until 8) {
            assertUniformSeams(metrics, CollageLayout.template(block), "mosaic $block")
        }
    }

    @Test
    fun `every seam of an expanded block is the same width`() {
        val metrics = WallMetrics.forCell(cell = 114, gap = 12)
        for (block in 0 until 8) {
            for (slot in 0 until CollageLayout.SLOTS_PER_BLOCK) {
                assertUniformSeams(
                    metrics,
                    CollageLayout.layout(block, slot).rects,
                    "block $block slot $slot"
                )
            }
        }
    }

    @Test
    fun `blocks keep the same seam as the cards inside them`() {
        val metrics = WallMetrics.forCell(cell = 114, gap = 12)
        val tiles = CollageLayout.template(0).map { place(metrics, it) }

        // The rightmost card ends one gap before the next block starts.
        assertEquals(metrics.stepX - metrics.gap, tiles.maxOf { it.right })
        assertEquals(metrics.stepY - metrics.gap, tiles.maxOf { it.bottom })
    }

    @Test
    fun `a block that fills the window keeps whole pixels only`() {
        val metrics = WallMetrics.forWidth(available = 1500, gap = 12)

        assertEquals(126, metrics.pitch)
        assertEquals(114, metrics.cell)
        assertEquals(0, metrics.originX)
        // The rightmost card ends exactly on the window edge; the trailing gap stays outside.
        assertEquals(1500, metrics.startX(12f) - metrics.gap)
        assertEquals(1512, metrics.stepX)
    }

    @Test
    fun `leftover pixels become a margin instead of uneven seams`() {
        val metrics = WallMetrics.forWidth(available = 1490, gap = 12)

        assertEquals(125, metrics.pitch)
        assertEquals(1, metrics.originX)
        assertEquals(1489, metrics.startX(12f) - metrics.gap)
        assertTrue(metrics.startX(0f) >= 0, "the leading margin must stay inside the window")
    }
}
