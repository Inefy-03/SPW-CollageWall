package dev.spw.collage

/**
 * The idle mosaics and the expansion refits for one 12 x 8 block.
 *
 * Four base mosaics, each with a hand-solved refit per slot: the opened card takes the
 * 6 x 6 gear and the other eleven fall back onto a fixed set of smaller gears, so the
 * block stays exactly covered and every card keeps a proportion from the same small
 * vocabulary instead of degenerating into arbitrary slivers. Slot order is preserved,
 * so a card keeps its identity while it moves.
 *
 * The refits are data, not a solver: they are the arrangement that moves the existing
 * cards the least out of every exact cover, which is why they cannot be derived by
 * eye. Do not hand-edit them.
 */
internal object CollageTemplates {

    /** One template per base mosaic; reflections are applied by [CollageLayout.template]. */
    val templates: List<List<GridRect>> = listOf(
        listOf(GridRect(0, 0, 3, 2), GridRect(3, 0, 6, 4), GridRect(9, 0, 3, 2), GridRect(0, 2, 3, 2), GridRect(9, 2, 3, 4), GridRect(0, 4, 4, 4), GridRect(4, 4, 2, 2), GridRect(6, 4, 3, 2), GridRect(4, 6, 2, 2), GridRect(6, 6, 2, 2), GridRect(8, 6, 2, 2), GridRect(10, 6, 2, 2)),
        listOf(GridRect(0, 0, 2, 3), GridRect(2, 0, 2, 3), GridRect(4, 0, 2, 3), GridRect(6, 0, 6, 4), GridRect(0, 3, 6, 2), GridRect(6, 4, 2, 4), GridRect(8, 4, 2, 4), GridRect(10, 4, 2, 2), GridRect(0, 5, 2, 3), GridRect(2, 5, 2, 3), GridRect(4, 5, 2, 3), GridRect(10, 6, 2, 2)),
        listOf(GridRect(0, 0, 6, 4), GridRect(6, 0, 6, 3), GridRect(6, 3, 4, 3), GridRect(10, 3, 2, 2), GridRect(0, 4, 4, 2), GridRect(4, 4, 2, 2), GridRect(10, 5, 2, 3), GridRect(0, 6, 2, 2), GridRect(2, 6, 2, 2), GridRect(4, 6, 2, 2), GridRect(6, 6, 2, 2), GridRect(8, 6, 2, 2)),
        listOf(GridRect(0, 0, 2, 2), GridRect(2, 0, 2, 2), GridRect(4, 0, 4, 3), GridRect(8, 0, 2, 4), GridRect(10, 0, 2, 4), GridRect(0, 2, 2, 3), GridRect(2, 2, 2, 2), GridRect(4, 3, 4, 3), GridRect(2, 4, 2, 2), GridRect(8, 4, 4, 4), GridRect(0, 5, 2, 3), GridRect(2, 6, 6, 2)),
    )

    /**
     * `refits[template][slot]` is the whole block while `slot` is the opened card.
     */
    val refits: List<List<List<GridRect>>> = listOf(
        // Mosaic 0
        listOf(
            listOf(GridRect(0, 0, 6, 6), GridRect(6, 0, 4, 4), GridRect(10, 0, 2, 2), GridRect(2, 6, 2, 2), GridRect(10, 2, 2, 4), GridRect(0, 6, 2, 2), GridRect(4, 6, 2, 2), GridRect(6, 4, 2, 2), GridRect(6, 6, 2, 2), GridRect(8, 6, 2, 2), GridRect(8, 4, 2, 2), GridRect(10, 6, 2, 2)),
            listOf(GridRect(0, 0, 3, 2), GridRect(3, 0, 6, 6), GridRect(9, 0, 3, 2), GridRect(0, 2, 3, 2), GridRect(9, 2, 3, 2), GridRect(0, 4, 3, 2), GridRect(3, 6, 3, 2), GridRect(6, 6, 2, 2), GridRect(0, 6, 3, 2), GridRect(8, 6, 2, 2), GridRect(10, 6, 2, 2), GridRect(9, 4, 3, 2)),
            listOf(GridRect(0, 0, 3, 2), GridRect(3, 0, 3, 4), GridRect(6, 0, 6, 6), GridRect(0, 2, 3, 2), GridRect(10, 6, 2, 2), GridRect(0, 4, 2, 4), GridRect(4, 4, 2, 2), GridRect(6, 6, 2, 2), GridRect(4, 6, 2, 2), GridRect(8, 6, 2, 2), GridRect(2, 6, 2, 2), GridRect(2, 4, 2, 2)),
            listOf(GridRect(2, 0, 2, 2), GridRect(4, 0, 6, 2), GridRect(8, 2, 2, 2), GridRect(0, 2, 6, 6), GridRect(10, 0, 2, 6), GridRect(0, 0, 2, 2), GridRect(6, 2, 2, 2), GridRect(6, 4, 2, 2), GridRect(6, 6, 2, 2), GridRect(8, 6, 2, 2), GridRect(8, 4, 2, 2), GridRect(10, 6, 2, 2)),
            listOf(GridRect(0, 0, 3, 2), GridRect(3, 0, 4, 2), GridRect(10, 0, 2, 2), GridRect(0, 2, 2, 3), GridRect(6, 2, 6, 6), GridRect(0, 5, 2, 3), GridRect(2, 4, 2, 2), GridRect(4, 4, 2, 2), GridRect(4, 6, 2, 2), GridRect(2, 6, 2, 2), GridRect(7, 0, 3, 2), GridRect(2, 2, 4, 2)),
            listOf(GridRect(0, 0, 3, 2), GridRect(5, 0, 4, 2), GridRect(9, 0, 3, 2), GridRect(3, 0, 2, 2), GridRect(8, 2, 4, 2), GridRect(0, 2, 6, 6), GridRect(6, 2, 2, 2), GridRect(6, 4, 2, 2), GridRect(6, 6, 2, 2), GridRect(8, 4, 2, 4), GridRect(10, 6, 2, 2), GridRect(10, 4, 2, 2)),
            listOf(GridRect(0, 0, 4, 2), GridRect(4, 0, 3, 2), GridRect(9, 0, 3, 2), GridRect(0, 2, 2, 3), GridRect(10, 2, 2, 2), GridRect(0, 5, 2, 3), GridRect(4, 2, 6, 6), GridRect(10, 4, 2, 2), GridRect(2, 6, 2, 2), GridRect(10, 6, 2, 2), GridRect(7, 0, 2, 2), GridRect(2, 2, 2, 4)),
            listOf(GridRect(0, 0, 4, 2), GridRect(4, 0, 4, 2), GridRect(8, 0, 2, 2), GridRect(0, 2, 2, 3), GridRect(10, 0, 2, 2), GridRect(0, 5, 2, 3), GridRect(4, 4, 2, 2), GridRect(6, 2, 6, 6), GridRect(4, 6, 2, 2), GridRect(2, 6, 2, 2), GridRect(2, 4, 2, 2), GridRect(2, 2, 4, 2)),
            listOf(GridRect(0, 0, 4, 2), GridRect(4, 0, 3, 2), GridRect(9, 0, 3, 2), GridRect(0, 2, 2, 3), GridRect(10, 2, 2, 2), GridRect(0, 5, 2, 3), GridRect(2, 2, 2, 4), GridRect(10, 4, 2, 2), GridRect(4, 2, 6, 6), GridRect(2, 6, 2, 2), GridRect(10, 6, 2, 2), GridRect(7, 0, 2, 2)),
            listOf(GridRect(0, 0, 4, 2), GridRect(4, 0, 4, 2), GridRect(8, 0, 2, 2), GridRect(0, 2, 2, 3), GridRect(10, 0, 2, 2), GridRect(0, 5, 2, 3), GridRect(2, 4, 2, 2), GridRect(4, 4, 2, 2), GridRect(4, 6, 2, 2), GridRect(6, 2, 6, 6), GridRect(2, 6, 2, 2), GridRect(2, 2, 4, 2)),
            listOf(GridRect(0, 0, 4, 2), GridRect(4, 0, 4, 2), GridRect(8, 0, 2, 2), GridRect(0, 2, 2, 3), GridRect(10, 0, 2, 2), GridRect(0, 5, 2, 3), GridRect(2, 4, 2, 2), GridRect(4, 4, 2, 2), GridRect(4, 6, 2, 2), GridRect(2, 6, 2, 2), GridRect(6, 2, 6, 6), GridRect(2, 2, 4, 2)),
            listOf(GridRect(0, 0, 4, 2), GridRect(4, 0, 4, 2), GridRect(8, 0, 2, 2), GridRect(0, 2, 2, 3), GridRect(10, 0, 2, 2), GridRect(0, 5, 2, 3), GridRect(2, 4, 2, 2), GridRect(4, 4, 2, 2), GridRect(4, 6, 2, 2), GridRect(2, 6, 2, 2), GridRect(2, 2, 4, 2), GridRect(6, 2, 6, 6)),
        ),
        // Mosaic 1
        listOf(
            listOf(GridRect(0, 0, 6, 6), GridRect(6, 0, 2, 2), GridRect(6, 2, 2, 2), GridRect(8, 0, 4, 4), GridRect(2, 6, 2, 2), GridRect(6, 4, 2, 4), GridRect(8, 4, 2, 2), GridRect(10, 4, 2, 2), GridRect(0, 6, 2, 2), GridRect(4, 6, 2, 2), GridRect(8, 6, 2, 2), GridRect(10, 6, 2, 2)),
            listOf(GridRect(0, 0, 2, 3), GridRect(2, 0, 6, 6), GridRect(8, 2, 4, 2), GridRect(8, 0, 4, 2), GridRect(0, 3, 2, 2), GridRect(6, 6, 2, 2), GridRect(8, 4, 2, 4), GridRect(10, 4, 2, 2), GridRect(0, 5, 2, 3), GridRect(2, 6, 2, 2), GridRect(4, 6, 2, 2), GridRect(10, 6, 2, 2)),
            listOf(GridRect(0, 0, 2, 3), GridRect(2, 0, 2, 3), GridRect(4, 0, 6, 6), GridRect(10, 0, 2, 4), GridRect(0, 3, 4, 2), GridRect(6, 6, 2, 2), GridRect(8, 6, 2, 2), GridRect(10, 4, 2, 2), GridRect(0, 5, 2, 3), GridRect(2, 5, 2, 3), GridRect(4, 6, 2, 2), GridRect(10, 6, 2, 2)),
            listOf(GridRect(0, 0, 2, 3), GridRect(2, 0, 2, 3), GridRect(4, 0, 2, 4), GridRect(6, 0, 6, 6), GridRect(0, 3, 4, 2), GridRect(6, 6, 2, 2), GridRect(8, 6, 2, 2), GridRect(10, 6, 2, 2), GridRect(0, 5, 2, 3), GridRect(2, 5, 2, 3), GridRect(4, 6, 2, 2), GridRect(4, 4, 2, 2)),
            listOf(GridRect(0, 0, 2, 2), GridRect(2, 0, 2, 2), GridRect(4, 0, 4, 2), GridRect(8, 0, 4, 4), GridRect(0, 2, 6, 6), GridRect(6, 4, 2, 2), GridRect(8, 4, 2, 2), GridRect(10, 4, 2, 2), GridRect(6, 6, 2, 2), GridRect(6, 2, 2, 2), GridRect(8, 6, 2, 2), GridRect(10, 6, 2, 2)),
            listOf(GridRect(0, 0, 2, 4), GridRect(2, 0, 2, 3), GridRect(4, 0, 4, 2), GridRect(8, 0, 2, 2), GridRect(2, 3, 2, 2), GridRect(6, 2, 6, 6), GridRect(4, 4, 2, 2), GridRect(10, 0, 2, 2), GridRect(0, 4, 2, 4), GridRect(2, 5, 2, 3), GridRect(4, 6, 2, 2), GridRect(4, 2, 2, 2)),
            listOf(GridRect(0, 0, 2, 4), GridRect(2, 0, 2, 3), GridRect(4, 0, 4, 2), GridRect(8, 0, 2, 2), GridRect(2, 3, 2, 2), GridRect(4, 4, 2, 2), GridRect(6, 2, 6, 6), GridRect(10, 0, 2, 2), GridRect(0, 4, 2, 4), GridRect(2, 5, 2, 3), GridRect(4, 6, 2, 2), GridRect(4, 2, 2, 2)),
            listOf(GridRect(0, 0, 2, 4), GridRect(2, 0, 2, 3), GridRect(4, 0, 4, 2), GridRect(8, 0, 2, 2), GridRect(2, 3, 2, 2), GridRect(4, 4, 2, 2), GridRect(4, 6, 2, 2), GridRect(6, 2, 6, 6), GridRect(0, 4, 2, 4), GridRect(2, 5, 2, 3), GridRect(4, 2, 2, 2), GridRect(10, 0, 2, 2)),
            listOf(GridRect(0, 0, 2, 2), GridRect(4, 0, 2, 2), GridRect(6, 0, 2, 3), GridRect(8, 0, 4, 4), GridRect(2, 0, 2, 2), GridRect(6, 6, 2, 2), GridRect(8, 4, 2, 2), GridRect(10, 4, 2, 2), GridRect(0, 2, 6, 6), GridRect(6, 3, 2, 3), GridRect(8, 6, 2, 2), GridRect(10, 6, 2, 2)),
            listOf(GridRect(0, 0, 2, 2), GridRect(2, 0, 2, 2), GridRect(4, 0, 4, 2), GridRect(8, 0, 4, 4), GridRect(0, 2, 2, 2), GridRect(8, 4, 2, 2), GridRect(8, 6, 2, 2), GridRect(10, 4, 2, 2), GridRect(0, 6, 2, 2), GridRect(2, 2, 6, 6), GridRect(0, 4, 2, 2), GridRect(10, 6, 2, 2)),
            listOf(GridRect(0, 0, 2, 3), GridRect(2, 0, 2, 4), GridRect(4, 0, 4, 2), GridRect(8, 0, 4, 2), GridRect(0, 3, 2, 3), GridRect(2, 4, 2, 2), GridRect(10, 4, 2, 2), GridRect(10, 2, 2, 2), GridRect(0, 6, 2, 2), GridRect(2, 6, 2, 2), GridRect(4, 2, 6, 6), GridRect(10, 6, 2, 2)),
            listOf(GridRect(0, 0, 2, 4), GridRect(2, 0, 2, 3), GridRect(4, 0, 4, 2), GridRect(8, 0, 2, 2), GridRect(2, 3, 2, 2), GridRect(4, 4, 2, 2), GridRect(4, 6, 2, 2), GridRect(10, 0, 2, 2), GridRect(0, 4, 2, 4), GridRect(2, 5, 2, 3), GridRect(4, 2, 2, 2), GridRect(6, 2, 6, 6)),
        ),
        // Mosaic 2
        listOf(
            listOf(GridRect(0, 0, 6, 6), GridRect(6, 0, 6, 2), GridRect(8, 2, 2, 4), GridRect(10, 2, 2, 3), GridRect(0, 6, 2, 2), GridRect(6, 4, 2, 2), GridRect(10, 5, 2, 3), GridRect(2, 6, 2, 2), GridRect(4, 6, 2, 2), GridRect(6, 6, 2, 2), GridRect(8, 6, 2, 2), GridRect(6, 2, 2, 2)),
            listOf(GridRect(0, 0, 4, 4), GridRect(6, 0, 6, 6), GridRect(6, 6, 2, 2), GridRect(8, 6, 2, 2), GridRect(0, 4, 2, 2), GridRect(4, 4, 2, 2), GridRect(10, 6, 2, 2), GridRect(0, 6, 2, 2), GridRect(2, 6, 2, 2), GridRect(4, 6, 2, 2), GridRect(2, 4, 2, 2), GridRect(4, 0, 2, 4)),
            listOf(GridRect(0, 0, 4, 4), GridRect(8, 0, 2, 2), GridRect(6, 2, 6, 6), GridRect(6, 0, 2, 2), GridRect(0, 4, 2, 2), GridRect(4, 4, 2, 2), GridRect(10, 0, 2, 2), GridRect(0, 6, 2, 2), GridRect(2, 6, 2, 2), GridRect(4, 6, 2, 2), GridRect(2, 4, 2, 2), GridRect(4, 0, 2, 4)),
            listOf(GridRect(0, 0, 4, 4), GridRect(8, 0, 2, 2), GridRect(4, 4, 2, 2), GridRect(6, 2, 6, 6), GridRect(0, 4, 2, 2), GridRect(4, 2, 2, 2), GridRect(10, 0, 2, 2), GridRect(0, 6, 2, 2), GridRect(2, 6, 2, 2), GridRect(4, 6, 2, 2), GridRect(2, 4, 2, 2), GridRect(4, 0, 4, 2)),
            listOf(GridRect(2, 0, 4, 2), GridRect(6, 0, 6, 2), GridRect(6, 4, 2, 2), GridRect(10, 4, 2, 2), GridRect(0, 2, 6, 6), GridRect(6, 2, 2, 2), GridRect(10, 6, 2, 2), GridRect(0, 0, 2, 2), GridRect(6, 6, 2, 2), GridRect(8, 6, 2, 2), GridRect(8, 4, 2, 2), GridRect(8, 2, 4, 2)),
            listOf(GridRect(0, 0, 4, 4), GridRect(8, 0, 2, 2), GridRect(10, 4, 2, 2), GridRect(10, 2, 2, 2), GridRect(0, 4, 2, 2), GridRect(4, 2, 6, 6), GridRect(10, 6, 2, 2), GridRect(0, 6, 2, 2), GridRect(2, 6, 2, 2), GridRect(2, 4, 2, 2), GridRect(4, 0, 4, 2), GridRect(10, 0, 2, 2)),
            listOf(GridRect(0, 0, 4, 4), GridRect(8, 0, 2, 2), GridRect(4, 4, 2, 2), GridRect(10, 0, 2, 2), GridRect(0, 4, 2, 2), GridRect(4, 2, 2, 2), GridRect(6, 2, 6, 6), GridRect(0, 6, 2, 2), GridRect(2, 6, 2, 2), GridRect(4, 6, 2, 2), GridRect(2, 4, 2, 2), GridRect(4, 0, 4, 2)),
            listOf(GridRect(0, 0, 4, 2), GridRect(6, 0, 6, 2), GridRect(6, 4, 2, 2), GridRect(10, 2, 2, 3), GridRect(4, 0, 2, 2), GridRect(6, 2, 2, 2), GridRect(10, 5, 2, 3), GridRect(0, 2, 6, 6), GridRect(6, 6, 2, 2), GridRect(8, 6, 2, 2), GridRect(8, 4, 2, 2), GridRect(8, 2, 2, 2)),
            listOf(GridRect(0, 0, 6, 2), GridRect(6, 0, 6, 2), GridRect(8, 4, 2, 2), GridRect(10, 2, 2, 2), GridRect(0, 4, 2, 2), GridRect(0, 2, 2, 2), GridRect(10, 6, 2, 2), GridRect(0, 6, 2, 2), GridRect(2, 2, 6, 6), GridRect(8, 6, 2, 2), GridRect(8, 2, 2, 2), GridRect(10, 4, 2, 2)),
            listOf(GridRect(0, 0, 4, 4), GridRect(8, 0, 2, 2), GridRect(10, 4, 2, 2), GridRect(10, 2, 2, 2), GridRect(0, 4, 2, 2), GridRect(2, 4, 2, 2), GridRect(10, 6, 2, 2), GridRect(0, 6, 2, 2), GridRect(2, 6, 2, 2), GridRect(4, 2, 6, 6), GridRect(4, 0, 4, 2), GridRect(10, 0, 2, 2)),
            listOf(GridRect(0, 0, 4, 4), GridRect(8, 0, 2, 2), GridRect(4, 4, 2, 2), GridRect(6, 0, 2, 2), GridRect(0, 4, 2, 2), GridRect(2, 4, 2, 2), GridRect(10, 0, 2, 2), GridRect(0, 6, 2, 2), GridRect(2, 6, 2, 2), GridRect(4, 6, 2, 2), GridRect(6, 2, 6, 6), GridRect(4, 0, 2, 4)),
            listOf(GridRect(0, 0, 4, 4), GridRect(8, 0, 2, 2), GridRect(4, 4, 2, 2), GridRect(4, 0, 4, 2), GridRect(0, 4, 2, 2), GridRect(4, 2, 2, 2), GridRect(10, 0, 2, 2), GridRect(0, 6, 2, 2), GridRect(2, 6, 2, 2), GridRect(4, 6, 2, 2), GridRect(2, 4, 2, 2), GridRect(6, 2, 6, 6)),
        ),
        // Mosaic 3
        listOf(
            listOf(GridRect(0, 0, 6, 6), GridRect(6, 2, 2, 2), GridRect(6, 0, 2, 2), GridRect(8, 0, 2, 6), GridRect(10, 0, 2, 2), GridRect(0, 6, 2, 2), GridRect(6, 6, 2, 2), GridRect(6, 4, 2, 2), GridRect(8, 6, 2, 2), GridRect(10, 2, 2, 6), GridRect(2, 6, 2, 2), GridRect(4, 6, 2, 2)),
            listOf(GridRect(0, 0, 2, 2), GridRect(2, 0, 6, 6), GridRect(8, 0, 2, 3), GridRect(8, 3, 2, 3), GridRect(10, 0, 2, 4), GridRect(0, 2, 2, 2), GridRect(6, 6, 2, 2), GridRect(4, 6, 2, 2), GridRect(8, 6, 2, 2), GridRect(10, 4, 2, 4), GridRect(0, 4, 2, 4), GridRect(2, 6, 2, 2)),
            listOf(GridRect(0, 0, 2, 2), GridRect(2, 0, 2, 3), GridRect(4, 0, 6, 6), GridRect(10, 0, 2, 2), GridRect(10, 2, 2, 3), GridRect(0, 2, 2, 2), GridRect(2, 3, 2, 2), GridRect(4, 6, 6, 2), GridRect(0, 4, 2, 2), GridRect(10, 5, 2, 3), GridRect(0, 6, 2, 2), GridRect(2, 5, 2, 3)),
            listOf(GridRect(0, 0, 2, 2), GridRect(2, 0, 2, 2), GridRect(4, 0, 2, 4), GridRect(6, 0, 6, 6), GridRect(7, 6, 2, 2), GridRect(0, 2, 2, 4), GridRect(2, 2, 2, 2), GridRect(4, 4, 2, 2), GridRect(2, 4, 2, 4), GridRect(9, 6, 3, 2), GridRect(0, 6, 2, 2), GridRect(4, 6, 3, 2)),
            listOf(GridRect(0, 0, 2, 2), GridRect(2, 0, 2, 2), GridRect(4, 0, 2, 4), GridRect(7, 6, 2, 2), GridRect(6, 0, 6, 6), GridRect(0, 2, 2, 4), GridRect(2, 2, 2, 2), GridRect(4, 4, 2, 2), GridRect(2, 4, 2, 4), GridRect(9, 6, 3, 2), GridRect(0, 6, 2, 2), GridRect(4, 6, 3, 2)),
            listOf(GridRect(3, 0, 2, 2), GridRect(6, 2, 2, 2), GridRect(5, 0, 2, 2), GridRect(8, 2, 2, 3), GridRect(9, 0, 3, 2), GridRect(0, 2, 6, 6), GridRect(7, 0, 2, 2), GridRect(6, 4, 2, 2), GridRect(8, 5, 2, 3), GridRect(10, 2, 2, 6), GridRect(0, 0, 3, 2), GridRect(6, 6, 2, 2)),
            listOf(GridRect(0, 0, 2, 2), GridRect(2, 0, 2, 2), GridRect(4, 0, 6, 2), GridRect(8, 2, 2, 2), GridRect(10, 0, 2, 3), GridRect(0, 2, 2, 3), GridRect(2, 2, 6, 6), GridRect(8, 4, 2, 2), GridRect(10, 3, 2, 2), GridRect(10, 5, 2, 3), GridRect(0, 5, 2, 3), GridRect(8, 6, 2, 2)),
            listOf(GridRect(0, 0, 2, 3), GridRect(2, 0, 2, 2), GridRect(4, 0, 3, 2), GridRect(7, 0, 3, 2), GridRect(10, 0, 2, 6), GridRect(0, 3, 2, 2), GridRect(2, 2, 2, 2), GridRect(4, 2, 6, 6), GridRect(2, 4, 2, 2), GridRect(10, 6, 2, 2), GridRect(0, 5, 2, 3), GridRect(2, 6, 2, 2)),
            listOf(GridRect(0, 0, 2, 2), GridRect(2, 0, 2, 2), GridRect(4, 0, 6, 2), GridRect(8, 2, 2, 2), GridRect(10, 0, 2, 3), GridRect(0, 2, 2, 3), GridRect(10, 3, 2, 2), GridRect(8, 4, 2, 2), GridRect(2, 2, 6, 6), GridRect(10, 5, 2, 3), GridRect(0, 5, 2, 3), GridRect(8, 6, 2, 2)),
            listOf(GridRect(0, 0, 2, 2), GridRect(2, 0, 3, 2), GridRect(5, 0, 3, 2), GridRect(8, 0, 2, 2), GridRect(10, 0, 2, 2), GridRect(0, 2, 2, 3), GridRect(2, 2, 2, 2), GridRect(4, 2, 2, 6), GridRect(2, 4, 2, 2), GridRect(6, 2, 6, 6), GridRect(0, 5, 2, 3), GridRect(2, 6, 2, 2)),
            listOf(GridRect(2, 0, 3, 2), GridRect(6, 2, 2, 2), GridRect(5, 0, 3, 2), GridRect(8, 0, 4, 3), GridRect(10, 3, 2, 2), GridRect(0, 0, 2, 2), GridRect(8, 3, 2, 3), GridRect(6, 4, 2, 2), GridRect(8, 6, 2, 2), GridRect(10, 5, 2, 3), GridRect(0, 2, 6, 6), GridRect(6, 6, 2, 2)),
            listOf(GridRect(0, 0, 2, 2), GridRect(2, 0, 4, 2), GridRect(6, 0, 2, 2), GridRect(8, 0, 2, 4), GridRect(10, 0, 2, 3), GridRect(0, 2, 2, 2), GridRect(0, 4, 2, 2), GridRect(8, 4, 2, 2), GridRect(10, 3, 2, 3), GridRect(8, 6, 4, 2), GridRect(0, 6, 2, 2), GridRect(2, 2, 6, 6)),
        ),
    )
}
