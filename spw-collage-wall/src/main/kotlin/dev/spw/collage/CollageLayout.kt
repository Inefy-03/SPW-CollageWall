package dev.spw.collage

/** Integer grid geometry for the 12 x 8 mixed-proportion blocks. */
data class GridRect(val x: Int, val y: Int, val cols: Int, val rows: Int) {
    val area: Int get() = cols * rows
}

/**
 * Continuous form of a block cell, used while a block animates between two layouts.
 *
 * Unitless on purpose: the wall converts it to pixels in the layout phase, so the
 * interpolation itself stays testable without a composition.
 *
 * [focus] rides along with the geometry: 0 is a card sitting in the mosaic, 1 is the card
 * the wall has opened. Because it is interpolated by the same fraction as the rectangle,
 * the type inside a card grows and shrinks with the card instead of snapping to its opened
 * size on the first frame.
 */
data class GridFrame(
    val x: Float,
    val y: Float,
    val cols: Float,
    val rows: Float,
    val focus: Float = 0f
) {
    /** Blends towards [target]: 0 keeps this frame, 1 lands exactly on [target]. */
    fun towards(target: GridFrame, fraction: Float): GridFrame = GridFrame(
        x = x + (target.x - x) * fraction,
        y = y + (target.y - y) * fraction,
        cols = cols + (target.cols - cols) * fraction,
        rows = rows + (target.rows - rows) * fraction,
        focus = focus + (target.focus - focus) * fraction
    )
}

fun GridRect.frame(focus: Float = 0f): GridFrame =
    GridFrame(x.toFloat(), y.toFloat(), cols.toFloat(), rows.toFloat(), focus)

/**
 * Pure geometry for the wall; no Compose types, so every rule is unit-testable.
 *
 * A block is twelve cards on a 12 x 8 lattice. Both the idle mosaic and every expansion
 * refit cover that lattice exactly, so the wall never has a seam or a hole. Opening a card
 * hands it a 6 x 6 square and the other eleven take smaller gears from the same fixed set,
 * which is what keeps their proportions readable instead of dissolving into arbitrary
 * strips.
 */
object CollageLayout {
    const val BLOCK_COLS = 12
    const val BLOCK_ROWS = 8
    const val SLOTS_PER_BLOCK = 12
    const val EXPANDED_COLS = 6
    const val EXPANDED_ROWS = 6

    /** One solved block: a rectangle per slot, always inside the fixed 12 x 8 footprint. */
    data class Block(val rects: List<GridRect>) {
        operator fun get(slot: Int): GridRect = rects[slot]
    }

    fun blockCount(trackCount: Int): Int =
        if (trackCount <= 0) 0 else (trackCount + SLOTS_PER_BLOCK - 1) / SLOTS_PER_BLOCK

    fun template(blockIndex: Int): List<GridRect> {
        require(blockIndex >= 0) { "blockIndex must be non-negative" }
        val (variant, reflection) = orientation(blockIndex)
        return reflect(CollageTemplates.templates[variant], reflection)
    }

    /**
     * Solves one block for the current focus.
     *
     * With nothing open every card sits on its mosaic cell. With a card open the refit for
     * that slot is used: the opened card takes the 6 x 6 gear and the rest of the block is
     * re-geared around it, keeping the footprint and the seams exactly where they were.
     */
    fun layout(blockIndex: Int, expandedSlot: Int?): Block {
        val (variant, reflection) = orientation(blockIndex)
        val base = if (expandedSlot != null && expandedSlot in 0 until SLOTS_PER_BLOCK) {
            CollageTemplates.refits[variant][expandedSlot]
        } else {
            CollageTemplates.templates[variant]
        }
        return Block(reflect(base, reflection))
    }

    /**
     * Which mosaic a block uses and how it is mirrored.
     *
     * The mosaic steps by one per block, so no two neighbours show the same pattern; the
     * mirror comes from a bit-mixing hash of the block index, which keeps the field from
     * settling into a visible period the way a linear choice would.
     */
    private fun orientation(blockIndex: Int): Pair<Int, Int> {
        val variants = CollageTemplates.templates.size
        val variant = Math.floorMod(blockIndex, variants)
        return variant to Math.floorMod(mix(blockIndex), 4)
    }

    /** Integer hash (the reference mix): cheap, stable, and free of short cycles. */
    private fun mix(value: Int): Int {
        var hash = value * -0x61c88647 // 0x9E3779B1
        hash = hash xor (hash ushr 15)
        hash *= 0x2C1B3C6D
        hash = hash xor (hash ushr 12)
        return hash
    }

    /** Mirrors a layout. Slot order is preserved, so a card keeps its index. */
    private fun reflect(layout: List<GridRect>, reflection: Int): List<GridRect> = layout.map { rect ->
        GridRect(
            x = if (reflection and 1 != 0) BLOCK_COLS - rect.x - rect.cols else rect.x,
            y = if (reflection and 2 != 0) BLOCK_ROWS - rect.y - rect.rows else rect.y,
            cols = rect.cols,
            rows = rect.rows
        )
    }
}
