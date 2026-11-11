package dev.spw.collage.ui

import dev.spw.collage.CollageLayout
import dev.spw.collage.GridFrame
import dev.spw.collage.frame
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * A card that grows has to take its labels with it, and both ends of that transition must
 * land on the sizes the wall has always used — otherwise the type follows the cover at the
 * cost of changing how a card at rest looks.
 */
class CardTypeTest {

    /** A cell the clamps never reach, so the transitions can be read off directly. */
    private val cell = 90f

    @Test
    fun `a card at rest keeps the sizes the wall always used`() {
        assertEquals(0.20f * cell, CardType.title(cell, focus = 0f), 1e-4f)
        assertEquals(0.12f * cell, CardType.artist(cell, focus = 0f), 1e-4f)
        assertEquals(0.155f * cell, CardType.inset(cell, focus = 0f), 1e-4f)
        assertEquals(-0.5f, CardType.letterSpacing(focus = 0f), 1e-4f)
    }

    @Test
    fun `an opened card keeps the sizes the wall always used`() {
        assertEquals(0.44f * cell, CardType.title(cell, focus = 1f), 1e-4f)
        assertEquals(0.16f * cell, CardType.artist(cell, focus = 1f), 1e-4f)
        assertEquals(0.29f * cell, CardType.inset(cell, focus = 1f), 1e-4f)
        assertEquals(-1.1f, CardType.letterSpacing(focus = 1f), 1e-4f)
    }

    @Test
    fun `a wall-sized cell still caps the type`() {
        // The clamps are part of the look, not of the transition: a 114 px cell would ask
        // for 50 px of title, and the wall has never shown more than 48.
        assertEquals(CardType.MaxTitle, CardType.title(114f, focus = 1f), 1e-4f)
        assertEquals(CardType.MaxArtist, CardType.artist(114f, focus = 1f), 1e-4f)
    }

    @Test
    fun `every step of the expansion moves the type`() {
        // A jump anywhere in the middle would show up as a card whose cover grows while its
        // labels stand still for a frame or two.
        var previous = CardType.title(cell, focus = 0f)
        for (step in 1..40) {
            val size = CardType.title(cell, focus = step / 40f)
            assertTrue(size > previous, "title stalled between steps ${step - 1} and $step")
            previous = size
        }
        var previousInset = CardType.inset(cell, focus = 0f)
        for (step in 1..40) {
            val inset = CardType.inset(cell, focus = step / 40f)
            assertTrue(inset > previousInset, "inset stalled between steps ${step - 1} and $step")
            previousInset = inset
        }
    }

    @Test
    fun `focus rides along with the rectangle instead of being a separate switch`() {
        val mosaic = GridLayoutFixtures.mosaic(block = 3, slot = 5)
        val opened = GridLayoutFixtures.opened(block = 3, slot = 5)
        val middle = mosaic.towards(opened, 0.5f)

        assertEquals(0f, mosaic.focus, 1e-4f)
        assertEquals(1f, opened.focus, 1e-4f)
        assertEquals(0.5f, middle.focus, 1e-4f)
        // The card really is halfway between the two gears at that point.
        assertEquals((mosaic.cols + opened.cols) / 2f, middle.cols, 1e-4f)
        assertEquals((mosaic.rows + opened.rows) / 2f, middle.rows, 1e-4f)
    }

    @Test
    fun `a card that is not the one being opened keeps a steady focus`() {
        // Every other card in the block carries focus 0 into both endpoints, so the frame
        // interpolates to exactly 0 on every animation frame and their labels are never
        // recomposed — this is the filter the wall relies on for its frame cost.
        val rest = GridFrame(0f, 0f, 2f, 2f)
        val movedTo = GridFrame(0f, 0f, 6f, 2f)
        for (step in 0..10) {
            assertEquals(0f, rest.towards(movedTo, step / 10f).focus, 0f)
        }
    }

    private object GridLayoutFixtures {
        fun mosaic(block: Int, slot: Int): GridFrame =
            CollageLayout.layout(block, null).rects[slot].frame()

        fun opened(block: Int, slot: Int): GridFrame =
            CollageLayout.layout(block, slot).rects[slot].frame(focus = 1f)
    }
}
