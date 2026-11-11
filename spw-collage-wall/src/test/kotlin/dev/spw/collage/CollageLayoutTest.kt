package dev.spw.collage

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CollageLayoutTest {
    @Test
    fun `every mosaic covers 12 by 8 cells exactly once`() {
        for (block in 0 until 16) {
            val template = CollageLayout.template(block)
            assertEquals(CollageLayout.SLOTS_PER_BLOCK, template.size)
            assertExactTiling(template, "mosaic $block")
        }
    }

    @Test
    fun `idle blocks keep every mosaic rectangle`() {
        for (block in 0 until 8) {
            val layout = CollageLayout.layout(block, null)
            assertEquals(CollageLayout.template(block), layout.rects)
        }
    }

    @Test
    fun `opened card is six by six and stays inside its block`() {
        for (block in 0 until 8) {
            for (slot in 0 until CollageLayout.SLOTS_PER_BLOCK) {
                val rect = CollageLayout.layout(block, slot)[slot]
                assertEquals(6, rect.cols)
                assertEquals(6, rect.rows)
                assertTrue(rect.x in 0..6, "x=${rect.x}")
                assertTrue(rect.y in 0..2, "y=${rect.y}")
            }
        }
    }

    @Test
    fun `opening a card refits the block without a single empty cell`() {
        for (block in 0 until 16) {
            for (slot in 0 until CollageLayout.SLOTS_PER_BLOCK) {
                val layout = CollageLayout.layout(block, slot)
                assertExactTiling(layout.rects, "block $block slot $slot")
            }
        }
    }

    /**
     * The point of the hand-solved refits is that every card keeps a proportion from one
     * small vocabulary. Anything thinner than two cells reads as a strip, and anything
     * taller than the block cannot be a card at all.
     */
    @Test
    fun `every refit keeps the gear vocabulary`() {
        for (block in 0 until 16) {
            for (slot in 0 until CollageLayout.SLOTS_PER_BLOCK) {
                for (rect in CollageLayout.layout(block, slot).rects) {
                    assertTrue(
                        rect.cols >= 2 && rect.rows >= 2,
                        "block $block slot $slot has a ${rect.cols}x${rect.rows} strip"
                    )
                    assertTrue(
                        rect.area <= CollageLayout.EXPANDED_COLS * CollageLayout.EXPANDED_ROWS,
                        "block $block slot $slot has a card larger than the focus gear"
                    )
                }
            }
        }
    }

    @Test
    fun `cards keep moving and resizing so the freed space is covered`() {
        var changed = 0
        for (block in 0 until 4) {
            val template = CollageLayout.template(block)
            for (slot in 0 until CollageLayout.SLOTS_PER_BLOCK) {
                val layout = CollageLayout.layout(block, slot)
                template.indices.filter { it != slot }.forEach { index ->
                    if (layout.rects[index] != template[index]) changed++
                }
            }
        }
        assertTrue(changed > 300, "only $changed cards moved or resized")
    }

    @Test
    fun `neighbouring blocks never repeat the same mosaic`() {
        // The single-column wall stacks blocks vertically; the panning wall puts three
        // blocks side by side, so a block's neighbours are one and three indices away.
        for (block in 0 until 12) {
            assertTrue(
                CollageLayout.template(block) != CollageLayout.template(block + 1),
                "block $block and ${block + 1} share a mosaic"
            )
            assertTrue(
                CollageLayout.template(block) != CollageLayout.template(block + 3),
                "block $block and ${block + 3} share a mosaic"
            )
        }
    }

    @Test
    fun `block count covers every track without an empty trailing block`() {
        assertEquals(0, CollageLayout.blockCount(0))
        assertEquals(1, CollageLayout.blockCount(1))
        assertEquals(1, CollageLayout.blockCount(12))
        assertEquals(2, CollageLayout.blockCount(13))
        assertEquals(84, CollageLayout.blockCount(1000))
    }

    private fun assertExactTiling(rects: List<GridRect>, label: String) {
        val occupied = mutableSetOf<Pair<Int, Int>>()
        for (rect in rects) {
            assertTrue(rect.cols > 0 && rect.rows > 0, "$label has an empty rect")
            assertTrue(
                rect.x >= 0 && rect.x + rect.cols <= CollageLayout.BLOCK_COLS,
                "$label rect $rect leaves the block horizontally"
            )
            assertTrue(
                rect.y >= 0 && rect.y + rect.rows <= CollageLayout.BLOCK_ROWS,
                "$label rect $rect leaves the block vertically"
            )
            for (y in rect.y until rect.y + rect.rows) {
                for (x in rect.x until rect.x + rect.cols) {
                    assertTrue(occupied.add(x to y), "$label overlaps at $x,$y")
                }
            }
        }
        assertEquals(
            CollageLayout.BLOCK_COLS * CollageLayout.BLOCK_ROWS,
            occupied.size,
            "$label leaves holes"
        )
    }
}
