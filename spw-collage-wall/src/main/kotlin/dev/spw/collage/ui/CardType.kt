package dev.spw.collage.ui

/**
 * How the type inside a card is sized, as fractions of one wall cell.
 *
 * Two anchors: a card sitting in the mosaic, and a card the wall has opened. Everything
 * between is interpolated by the block's expansion progress, so the labels grow and shrink
 * with the card instead of snapping to their opened size on the first frame. Both anchors
 * are the values the wall has always used, so a card at rest looks exactly as it did.
 *
 * Pure arithmetic on purpose: the wall has to be sure the type is continuous, and this is
 * the cheapest place to prove it.
 */
internal object CardType {
    private const val TitleRest = 0.20f
    private const val TitleFocus = 0.44f
    private const val ArtistRest = 0.12f
    private const val ArtistFocus = 0.16f
    private const val InsetRest = 0.155f
    private const val InsetFocus = 0.29f
    private const val LetterRest = -0.5f
    private const val LetterFocus = -1.1f

    /** Bounds that keep a label readable on a tiny card and sane on a wall-sized one. */
    const val MinTitle = 11f
    const val MaxTitle = 48f
    const val MinArtist = 9f
    const val MaxArtist = 17f

    /** Space between the title and the artist, also in dp. */
    const val ArtistGap = 5f

    fun title(cell: Float, focus: Float): Float =
        (cell * blend(TitleRest, TitleFocus, focus)).coerceIn(MinTitle, MaxTitle)

    fun artist(cell: Float, focus: Float): Float =
        (cell * blend(ArtistRest, ArtistFocus, focus)).coerceIn(MinArtist, MaxArtist)

    /** Margin between the labels and the card's edge. */
    fun inset(cell: Float, focus: Float): Float =
        cell * blend(InsetRest, InsetFocus, focus)

    fun letterSpacing(focus: Float): Float = blend(LetterRest, LetterFocus, focus)

    private fun blend(rest: Float, open: Float, amount: Float): Float =
        rest + (open - rest) * amount.coerceIn(0f, 1f)
}
