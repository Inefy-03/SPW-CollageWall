package dev.spw.collage.ui

import dev.spw.collage.CollageLayout

/**
 * Pixel geometry of one wall.
 *
 * Every card is measured from a single pitch: a card spanning `cols` cells starts at
 * `origin + x * pitch` and is `cols * pitch - gap` wide. Because both the offset and the
 * size are whole multiples of the same pitch, every gap on the wall is exactly `gap`
 * pixels — including the seam between two blocks, which used to be a separate, wider gap.
 */
internal data class WallMetrics(
    val pitch: Int,
    val gap: Int,
    /** Leading margin that absorbs the pixels a whole pitch cannot take. */
    val originX: Int = 0
) {
    /** Side of one cell; only used to size the type inside a card. */
    val cell: Int get() = pitch - gap
    /** Distance between two blocks: the block owns its trailing gap, so the seam matches. */
    val stepX: Int get() = CollageLayout.BLOCK_COLS * pitch
    val stepY: Int get() = CollageLayout.BLOCK_ROWS * pitch

    fun startX(cells: Float): Int = originX + Math.round(cells * pitch)
    fun startY(cells: Float): Int = Math.round(cells * pitch)

    /** Size of a span, in pixels: the trailing gap belongs to the seam, not to the card. */
    fun span(cells: Float): Int = Math.round(cells * pitch) - gap

    companion object {
        /** Cards sized from a cell: the pitch already carries the gap. */
        fun forCell(cell: Int, gap: Int): WallMetrics = WallMetrics(pitch = cell + gap, gap = gap)

        /**
         * A block that fills [available]: whatever pixels are left over are split evenly as
         * a leading margin, so no seam has to absorb them.
         */
        fun forWidth(available: Int, gap: Int): WallMetrics {
            val pitch = ((available + gap) / CollageLayout.BLOCK_COLS).coerceAtLeast(gap + 1)
            val drawn = CollageLayout.BLOCK_COLS * pitch - gap
            return WallMetrics(
                pitch = pitch,
                gap = gap,
                originX = ((available - drawn) / 2).coerceAtLeast(0)
            )
        }
    }
}
