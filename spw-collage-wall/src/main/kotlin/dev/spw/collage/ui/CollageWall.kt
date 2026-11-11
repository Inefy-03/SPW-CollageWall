package dev.spw.collage.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.xuncorp.spw.workshop.api.PlaybackExtensionPoint.MediaItem
import dev.spw.collage.CollageController
import dev.spw.collage.CollageLayout
import dev.spw.collage.GridFrame
import dev.spw.collage.frame
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlinx.coroutines.delay

private val TileGap = 8.dp

/** Most a hovered card grows by, before the gap around it gets a say. */
private const val LiftMax = 1.04f

/** Share of the surrounding gap a lift may close; the rest stays visible as a margin. */
private const val LiftGapShare = 0.8f
private val NowPlayingRing = 2.dp

/**
 * The library as one immutable value.
 *
 * `List<MediaItem>` is an interface and `MediaItem` belongs to the host, so Compose has to
 * treat both as unstable and cannot skip a block that takes them: every time a block scrolled
 * into view the whole wall recomposed. The wall only ever reads this box and replaces it
 * wholesale when the library changes, which is the contract Compose needs to skip a block
 * whose own inputs did not change.
 */
@Immutable
private class TrackSet(private val items: List<MediaItem>) {
    val size: Int get() = items.size
    val isNotEmpty: Boolean get() = items.isNotEmpty()
    operator fun get(index: Int): MediaItem = items[index]
}

/** The lattice is three blocks wide, so horizontal panning always has somewhere to go. */
private const val PanColumns = 3

/**
 * Painted over every card, above the cover and below the labels.
 *
 * The stops are the reference wall's poster shade — 5% black across the top third, 24% at the
 * middle, 92% at the foot — so the artwork reads at a glance while the labels stay legible.
 */
private val TileShade = Brush.verticalGradient(
    0f to Color(0x0D000000),
    0.34f to Color(0x0D000000),
    0.56f to Color(0x3D000000),
    1f to Color(0xEB000000)
)

/**
 * Quiets every card that is not the one the wall is on.
 *
 * The reference tints its background posters with `color-mix(bg 78-84%, accent)` at half
 * opacity. Measured against its own shade gradient, that lands within a few levels of a 36%
 * black veil, so a plain veil is used here: a wall with no accent colour of its own should not
 * invent one. Hovering, opening or playing a card fades the veil out, which is the same rule
 * the reference applies to its current and hovered posters.
 */
private const val TileDimAlpha = 0.36f

@Composable
fun CollageWall(controller: CollageController, darkTheme: Boolean) {
    val background = if (darkTheme) Color(0xFF11100F) else Color(0xFFF4F2EE)
    Box(
        Modifier.fillMaxSize().background(background),
        contentAlignment = Alignment.Center
    ) {
        when {
            controller.loading -> CenteredText("正在读取歌曲…", darkTheme)
            controller.error != null -> CenteredText(controller.error.orEmpty(), darkTheme)
            controller.tracks.isEmpty() -> CenteredText("音乐库中还没有歌曲", darkTheme)
            else -> SongWall(controller)
        }
    }
}

@Composable
private fun CenteredText(text: String, darkTheme: Boolean) {
    BasicText(
        text = text,
        style = TextStyle(
            color = if (darkTheme) Color(0xB3FFFFFF) else Color(0xB31C1917),
            fontSize = 15.sp
        )
    )
}

@Composable
private fun SongWall(controller: CollageController) {
    val tracks = remember(controller.tracks) { TrackSet(controller.tracks) }
    val blockCount = CollageLayout.blockCount(tracks.size)
    val pan = rememberWallPanState()

    BoxWithConstraints(Modifier.fillMaxSize().clipToViewport()) {
        val density = LocalDensity.current
        val gap = with(density) { TileGap.roundToPx() }
        val viewportWidth = with(density) { maxWidth.roundToPx() }
        val viewportHeight = with(density) { maxHeight.roundToPx() }
        val verticalOnly = controller.verticalOnly

        // One column makes the largest cards and keeps the wall exactly one window wide,
        // which is what "vertical only" means; three columns give the free wall something
        // to move across.
        val columns = if (verticalOnly) 1 else minOf(PanColumns, blockCount.coerceAtLeast(1))
        val rows = ((blockCount + columns - 1) / columns).coerceAtLeast(1)
        val metrics = remember(verticalOnly, viewportWidth, gap) {
            if (verticalOnly) {
                WallMetrics.forWidth(viewportWidth, gap)
            } else {
                val cell = with(density) { (maxWidth / 14).coerceIn(72.dp, 112.dp).roundToPx() }
                WallMetrics.forCell(cell, gap)
            }
        }
        val coverBudget = remember(metrics) { CoverDecode.budgetFor(metrics) }
        // One measurer for the whole wall: its paragraph cache spans every tile, so a block
        // the loop brings back around reuses the layouts it already paid for instead of
        // re-breaking two dozen texts inside the frame it re-enters — that re-break was the
        // hitch every block crossing, and the loop makes re-entry the common case.
        val textMeasurer = rememberTextMeasurer(cacheSize = 512)

        val stepX = metrics.stepX
        val stepY = metrics.stepY
        val contentWidth = stepX * columns
        val contentHeight = stepY * rows
        // An axis repeats once one turn of the wall is at least a screen long. Shorter than
        // that a loop would show the same songs twice at the same time, so it stays clamped.
        val repeatX = columns > 1 && contentWidth >= viewportWidth
        val repeatY = contentHeight >= viewportHeight
        // Only a loop that closes below has an end meeting a beginning: there the seats the
        // library leaves empty are filled from its head and the spare block seats take the
        // first blocks again, so the turn has no bald patch to recur on every pass. A wall
        // that stops before its floor simply ends.
        val loop = repeatY

        LaunchedEffect(contentWidth, contentHeight, viewportWidth, viewportHeight, repeatX, repeatY) {
            pan.bounds(
                contentWidth = contentWidth.toFloat(),
                contentHeight = contentHeight.toFloat(),
                viewportWidth = viewportWidth.toFloat(),
                viewportHeight = viewportHeight.toFloat(),
                repeatX = repeatX,
                repeatY = repeatY
            )
        }

        // Only the blocks around the viewport are composed, and each range is a derived state,
        // so a pan frame moves the wall through the placement phase and recomposes nothing
        // until a whole block scrolls past.
        val columnRange by remember(stepX, columns, repeatX, viewportWidth) {
            derivedStateOf { visibleBlocks(pan.x, stepX, columns, viewportWidth, repeatX) }
        }
        val rowRange by remember(stepY, rows, repeatY, viewportHeight) {
            derivedStateOf { visibleBlocks(pan.y, stepY, rows, viewportHeight, repeatY) }
        }

        Box(Modifier.fillMaxSize().wallPan(pan, horizontal = columns > 1)) {
            // Keyed by the unbounded (row, column) the pan names, so a block keeps its
            // identity for as long as it stays in view and every block that enters is
            // composed fresh. Positional slot reuse handed a moving block the previous
            // block's state, which is what swapped covers between seats in motion.
            for ((row, column) in rowRange.flatMap { row -> columnRange.map { column -> row to column } }) {
                key(row, column) {
                    // A repeating axis addresses blocks by unbounded coordinates and folds
                    // them back into the one turn of the wall; a clamped one only ever walks
                    // the blocks it has. A turn can hold more block seats than the library
                    // has blocks, so the leftover seats take the first blocks again rather
                    // than leaving holes that would recur at the same spot on every loop.
                    val seat = Math.floorMod(row, rows) * columns + Math.floorMod(column, columns)
                    val blockIndex = if (loop) Math.floorMod(seat, blockCount) else seat
                    if (blockIndex >= blockCount) continue
                    Box(
                        Modifier
                            // Both offsets are read in the placement phase, so panning never
                            // goes through composition.
                            .offset {
                                IntOffset(
                                    (column * stepX - pan.x).roundToInt(),
                                    (row * stepY - pan.y).roundToInt()
                                )
                            }
                            .width(with(density) { stepX.toDp() })
                            .height(with(density) { stepY.toDp() })
                    ) {
                        CollageBlock(
                            blockIndex = blockIndex,
                            posKey = "$row:$column",
                            tracks = tracks,
                            metrics = metrics,
                            controller = controller,
                            coverBudget = coverBudget,
                            textMeasurer = textMeasurer,
                            loop = loop
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CollageBlock(
    blockIndex: Int,
    posKey: String,
    tracks: TrackSet,
    metrics: WallMetrics,
    controller: CollageController,
    coverBudget: Int,
    textMeasurer: TextMeasurer,
    loop: Boolean
) {
    val start = blockIndex * CollageLayout.SLOTS_PER_BLOCK
    val remaining = tracks.size - start
    // A looping wall seats the head of the library on the seats the last block leaves empty;
    // a bounded one leaves them bare, because one screen must not show a song twice.
    val slotCount = when {
        remaining >= CollageLayout.SLOTS_PER_BLOCK -> CollageLayout.SLOTS_PER_BLOCK
        loop && tracks.isNotEmpty -> CollageLayout.SLOTS_PER_BLOCK
        else -> remaining.coerceAtLeast(0)
    }
    // Identity of this block's spot on screen, not of its place in one turn of the wall:
    // the two copies a seam can show open separately, and a block that scrolls out of view
    // closes instead of staying open behind the wall.
    val keyPrefix = "$posKey:"

    // Derived, not read straight from the controller: a block that resolves to -1 both
    // before and after an expansion elsewhere keeps the same value, so it is never
    // recomposed just because some other card was opened.
    val expandedSlot by remember(keyPrefix) {
        derivedStateOf {
            val key = controller.expandedKey ?: return@derivedStateOf -1
            if (!key.startsWith(keyPrefix)) return@derivedStateOf -1
            key.substring(keyPrefix.length).toIntOrNull()?.takeIf { it in 0 until slotCount } ?: -1
        }
    }
    val idle = remember(blockIndex) { CollageLayout.layout(blockIndex, null) }
    val target = remember(blockIndex, expandedSlot) {
        if (expandedSlot < 0) idle else CollageLayout.layout(blockIndex, expandedSlot)
    }
    // The opened card's frame carries a focus of 1, every other card a 0. Ridden along with
    // the rectangle, it lets the type inside a card grow and shrink with the card; see
    // [TileText].
    val targetFrames = remember(target, expandedSlot) {
        target.rects.mapIndexed { slot, rect -> rect.frame(if (slot == expandedSlot) 1f else 0f) }
    }

    // Animation endpoints. A block animates from whatever is currently on screen, so a
    // collapsed card shrinks back from its grown cell instead of snapping to the template
    // first, and switching straight from one card to another keeps both on screen.
    var fromFrames by remember(keyPrefix) { mutableStateOf(targetFrames) }
    var toFrames by remember(keyPrefix) { mutableStateOf(targetFrames) }
    // One progress value drives the whole block. Every card used to own four
    // animateDpAsState instances (offset x/y, size w/h) that each wrote a snapshot state
    // on every animation frame, which recomposed and remeasured the block twelve times over.
    // The endpoints are read in the layout phase, so a frame costs placement, not recomposition.
    val progress = remember(keyPrefix) { Animatable(1f) }
    LaunchedEffect(target) {
        // First composition already sits on the target; only later changes animate.
        if (targetFrames == toFrames) return@LaunchedEffect
        val shown = progress.value
        fromFrames = fromFrames.mapIndexed { index, frame ->
            frame.towards(toFrames[index], shown)
        }
        toFrames = targetFrames
        progress.snapTo(0f)
        progress.animateTo(1f, tween(if (expandedSlot >= 0) 360 else 280))
    }
    val fraction = progress.asState()

    val density = LocalDensity.current
    val cell = with(density) { metrics.cell.toDp() }

    Box(Modifier.fillMaxSize()) {
        for (slot in 0 until slotCount) {
            val track = tracks[(start + slot) % tracks.size]
            val focused = slot == expandedSlot
            SongTile(
                track = track,
                expanded = focused,
                cell = cell,
                corner = controller.cornerRadius.dp,
                from = fromFrames[slot],
                to = toFrames[slot],
                fraction = fraction,
                controller = controller,
                coverBudget = coverBudget,
                textMeasurer = textMeasurer,
                modifier = Modifier
                    .tileGeometry(fromFrames[slot], toFrames[slot], metrics, fraction),
                onClick = { controller.toggleExpanded(keyPrefix + slot) },
                onDoubleClick = { controller.openAndPlay(keyPrefix + slot, track) }
            )
        }
    }
}

/**
 * Card position and size, solved in one layout pass.
 *
 * The frame is read in the measure phase from the block's single progress — the animation
 * always re-measured the card anyway, because the size depends on it — and the solved
 * frame also carries the offset, so one layout node now does what a placement node and a
 * size node used to do separately.
 */
private fun Modifier.tileGeometry(
    from: GridFrame,
    to: GridFrame,
    metrics: WallMetrics,
    fraction: State<Float>
): Modifier = layout { measurable, _ ->
    val frame = from.towards(to, fraction.value)
    val width = metrics.span(frame.cols)
    val height = metrics.span(frame.rows)
    val placeable = measurable.measure(Constraints.fixed(width, height))
    layout(width, height) {
        placeable.place(metrics.startX(frame.x), metrics.startY(frame.y))
    }
}

/** Outline reused by [clipToShape]; rebuilt only when the card is resized. */
private class ClipOutline {
    var width = -1f
    var height = -1f
    var path: Path? = null
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SongTile(
    track: MediaItem,
    expanded: Boolean,
    cell: Dp,
    corner: Dp,
    from: GridFrame,
    to: GridFrame,
    fraction: State<Float>,
    controller: CollageController,
    coverBudget: Int,
    textMeasurer: TextMeasurer,
    modifier: Modifier,
    onClick: () -> Unit,
    onDoubleClick: () -> Unit
) {
    // Bridged, not just cached: when a larger budget is needed (the window grew), the
    // smaller bitmap stays on screen until the sharper decode lands, so the wall never
    // flashes its fallback gradient over a cover it is already showing. A failed
    // re-decode keeps whatever resolution was on hand.
    val cover by produceState(
        initialValue = CoverStore.cached(track.id, coverBudget)
            ?: CoverStore.anyCached(track.id),
        track.id,
        coverBudget
    ) {
        if (CoverStore.cached(track.id, coverBudget) == null) {
            CoverStore.load(track.id, coverBudget) { value = it ?: value }
        }
    }
    val fallback = remember(track.id) { fallbackBrush(track.id) }
    // Square by default; the settings page can round the corners up to 32 dp. The shape and
    // its outline are built once per value instead of once per recomposition.
    val shape = remember(corner) { RoundedCornerShape(corner) }
    val outline = remember(corner) { ClipOutline() }
    val rounded = corner > 0.dp
    // Derived, so a track change recomposes only the card that took or lost the ring;
    // reading playingId in the block used to recompose every visible block.
    val playing by remember(track.id) { derivedStateOf { track.id == controller.playingId } }
    val bringIntoView = remember(track.id) { BringIntoViewRequester() }
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()

    // Hover lift: the card grows a little and rises above its neighbours, so the pointer
    // always reads the whole cover. The opened card already owns the top of the stack and
    // does not lift again. A tween rather than a spring: the step is a few percent, and a
    // spring that settles in ~120 ms reads as a jump instead of a transition.
    //
    // The lift is applied while measuring rather than through graphicsLayer: an offscreen
    // layer around the cover made the host's Skia backend paint the artwork solid black.
    //
    // The step is capped by the card's own size, so a wide card grows only as many pixels as
    // the gap around it can spare and its edge never reaches the artwork beside it, while a
    // small card keeps the full 4%. The scale is uniform, so the longer side decides: it is
    // the one that moves the most pixels. The size comes from the grid instead of the measured
    // box because the target is needed before the frame can be measured; the estimate misses
    // the gaps inside the card, which makes it a few percent smaller than the real span — and
    // the cap already leaves a fifth of the gap unused, so that error stays on the safe side.
    val liftStep = remember(cell, from, to) {
        val longest = maxOf(maxOf(from.cols, to.cols), maxOf(from.rows, to.rows)) * cell.value
        if (longest <= 0f) {
            LiftMax
        } else {
            minOf(LiftMax, 1f + 2f * LiftGapShare * TileGap.value / longest)
        }
    }
    val lift = animateFloatAsState(
        targetValue = if (hovered && !expanded) liftStep else 1f,
        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
        label = "tile-lift"
    )
    // Every card that is not the current song sits behind a light scrim so the current one
    // stands out; pointing at a card fades its scrim away before the cover is opened.
    val scrim = animateFloatAsState(
        targetValue = if (playing || hovered || expanded) 0f else 1f,
        animationSpec = tween(if (hovered) 170 else 320),
        label = "tile-scrim"
    )

    // A grown card can extend past the viewport; keep it fully readable. Only the opened
    // card needs the request node, so the others do not carry one.
    LaunchedEffect(expanded) {
        if (expanded) {
            delay(64)
            runCatching { bringIntoView.bringIntoView() }
        }
    }

    Box(
        modifier
            .then(if (expanded) Modifier.bringIntoViewRequester(bringIntoView) else Modifier)
            .zIndex(if (expanded) 20f else if (hovered) 8f else 0f)
            // Hover is measured on the un-scaled bounds. Behind the scale it would sit on the
            // grown edge, so pointing near a border flipped hover on and off every frame.
            .hoverable(interaction)
            .hoverLift(lift)
            .clipToShape(shape, outline, rounded)
            .background(fallback)
            .combinedClickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
                onDoubleClick = onDoubleClick
            )
    ) {
        // Both veils sit on the artwork only: labels below keep their contrast. Drawn
        // straight onto the canvas in one pass instead of through a layer, and read
        // during draw, so fading neither recomposes nor buffers the card. They ride on the
        // artwork's own node instead of a second full-size one, which is a draw node fewer
        // per card for exactly the same picture.
        val veils = Modifier.drawWithContent {
            drawContent()
            drawRect(TileShade)
            drawRect(Color.Black, alpha = TileDimAlpha * scrim.value)
        }
        val art = cover
        if (art != null) {
            Image(
                bitmap = art,
                contentDescription = null,
                modifier = Modifier.fillMaxSize().then(veils),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(Modifier.fillMaxSize().then(veils))
        }
        TileText(
            track = track,
            expanded = expanded,
            cell = cell,
            from = from,
            to = to,
            fraction = fraction,
            lift = lift,
            measurer = textMeasurer
        )
        // A white ring marks exactly the card the wall has opened — the ring and the opened
        // card are one and the same, so a playing card that is not open stays unadorned and
        // no two cards can ever wear the ring at once. The stroke path sits a full stroke
        // inside the clip: a path whose outer edge coincided with the clip's antialiased
        // boundary left a pale fringe along the rounded corners.
        if (expanded) {
            val stroke = with(LocalDensity.current) { NowPlayingRing.toPx() }
            val ringRadius = with(LocalDensity.current) {
                (corner - NowPlayingRing).coerceAtLeast(0.dp).toPx()
            }
            Box(
                Modifier
                    .fillMaxSize()
                    .drawBehind {
                        drawRoundRect(
                            color = Color.White,
                            topLeft = Offset(stroke, stroke),
                            size = Size(
                                (size.width - 2 * stroke).coerceAtLeast(0f),
                                (size.height - 2 * stroke).coerceAtLeast(0f)
                            ),
                            cornerRadius = CornerRadius(ringRadius),
                            style = Stroke(width = stroke)
                        )
                    }
            )
        }
    }
}

/**
 * Title and artist of one card, sized from the card.
 *
 * Split out of [SongTile] because the type has to follow the card while it grows, which
 * means this composable reads the block's animation frame on every frame of an expansion.
 * Reading it here keeps that churn to the type: the cover, the veils and the ring are not
 * recomposed while a card grows, and — because the frame is read through a derived state
 * that filters equal results — the eleven cards that are not the one being opened are not
 * recomposed at all.
 */
@Composable
private fun TileText(
    track: MediaItem,
    expanded: Boolean,
    cell: Dp,
    from: GridFrame,
    to: GridFrame,
    fraction: State<Float>,
    lift: State<Float>,
    measurer: TextMeasurer
) {
    val focus by remember(from, to) {
        derivedStateOf {
            val shown = fraction.value
            from.focus + (to.focus - from.focus) * shown
        }
    }
    // The hover lift is applied while measuring, so the cover grows while a fixed font
    // would not; scaling the type by the same factor keeps the two in step.
    val scale = lift.value
    val titleSize = (CardType.title(cell.value, focus) * scale).sp
    val artistSize = (CardType.artist(cell.value, focus) * scale).sp
    val inset = CardType.inset(cell.value, focus).dp * scale
    val letterSpacing = CardType.letterSpacing(focus).sp
    val titleStyle = remember(titleSize, letterSpacing) {
        TextStyle(
            color = Color(0xFFF7F4EE),
            fontSize = titleSize,
            fontWeight = FontWeight.SemiBold,
            lineHeight = titleSize * 1.06f,
            letterSpacing = letterSpacing
        )
    }
    val artistStyle = remember(artistSize) {
        TextStyle(
            color = Color(0xC7F7F4EE),
            fontSize = artistSize,
            fontWeight = FontWeight.Medium
        )
    }

    val title = track.title.ifBlank { "未知歌曲" }
    val artist = track.artist.ifBlank { "未知艺术家" }
    val maxLines = if (expanded) 3 else 2

    // The text is drawn straight onto the card's canvas through the wall's shared measurer
    // instead of living in per-tile text nodes. Compose breaks paragraphs per instance, so
    // a block the loop brings back used to re-break two dozen texts inside the frame it
    // re-entered; the measurer's cache spans the whole wall, and a re-entry is a cache hit.
    Box(
        Modifier
            .fillMaxSize()
            .drawBehind {
                val insetPx = inset.toPx()
                val constraints = Constraints(
                    maxWidth = (size.width - 2 * insetPx).roundToInt().coerceAtLeast(1)
                )
                val titleLayout = measurer.measure(
                    text = title,
                    style = titleStyle,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = maxLines,
                    constraints = constraints
                )
                val artistLayout = measurer.measure(
                    text = artist,
                    style = artistStyle,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                    constraints = constraints
                )
                val gap = CardType.ArtistGap.dp.toPx() * scale
                val block = titleLayout.size.height + gap + artistLayout.size.height
                val top = size.height - insetPx - block
                drawText(titleLayout, topLeft = Offset(insetPx, top))
                drawText(artistLayout, topLeft = Offset(insetPx, top + titleLayout.size.height + gap))
            }
    )
}

/**
 * Grows the card while measuring instead of scaling it through a graphics layer.
 *
 * An offscreen layer around the cover made the host's Skia backend paint the artwork black,
 * while text in the same layer stayed correct. Measuring at the grown size keeps the card on
 * the wall's own canvas, so the cover is drawn exactly like before the free panning work.
 */
private fun Modifier.hoverLift(scale: State<Float>): Modifier = layout { measurable, constraints ->
    val factor = scale.value
    val width = constraints.maxWidth
    val height = constraints.maxHeight
    if (factor == 1f || width <= 0 || height <= 0) {
        val placeable = measurable.measure(constraints)
        layout(width, height) { placeable.place(0, 0) }
    } else {
        val placeable = measurable.measure(
            Constraints.fixed((width * factor).roundToInt(), (height * factor).roundToInt())
        )
        layout(width, height) {
            placeable.place(
                -(placeable.width - width) / 2,
                -(placeable.height - height) / 2
            )
        }
    }
}

/**
 * Rounded clipping on the canvas itself; no offscreen layer is created.
 *
 * A square card — the default, and every card unless the setting was raised — is clipped to
 * its rectangle: Skia can intersect a rect without a mask, while a rounded path forces one on
 * every card of every frame. The rounded outline is kept in [cache] so it is rebuilt only
 * when the card is actually resized, not on every recomposition and not on every draw.
 */
private fun Modifier.clipToShape(shape: Shape, cache: ClipOutline, rounded: Boolean): Modifier =
    if (!rounded) {
        drawWithContent { clipRect { this@drawWithContent.drawContent() } }
    } else {
        drawWithContent {
            val path = cache.path?.takeIf {
                cache.width == size.width && cache.height == size.height
            } ?: Path().apply {
                addOutline(shape.createOutline(size, layoutDirection, this@drawWithContent))
                cache.width = size.width
                cache.height = size.height
                cache.path = this
            }
            clipPath(path) { this@drawWithContent.drawContent() }
        }
    }

/** Clips a panned wall to the viewport without the layer clipToBounds() allocates. */
private fun Modifier.clipToViewport(): Modifier = drawWithContent {
    clipRect { this@drawWithContent.drawContent() }
}

private fun fallbackBrush(id: String): Brush {
    val hue = Math.floorMod(id.fold(0) { acc, char -> acc + char.code }, 360)
    val start = hslColor(hue, 0.52f, 0.52f)
    val end = hslColor((hue + 52) % 360, 0.48f, 0.18f)
    return Brush.linearGradient(listOf(start, end))
}

/** Small HSL conversion used only when a track has no embedded cover. */
private fun hslColor(hue: Int, saturation: Float, lightness: Float): Color {
    val c = (1f - kotlin.math.abs(2f * lightness - 1f)) * saturation
    val x = c * (1f - kotlin.math.abs((hue / 60f) % 2f - 1f))
    val m = lightness - c / 2f
    val (r, g, b) = when ((hue / 60) % 6) {
        0 -> Triple(c, x, 0f)
        1 -> Triple(x, c, 0f)
        2 -> Triple(0f, c, x)
        3 -> Triple(0f, x, c)
        4 -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }
    return Color(r + m, g + m, b + m)
}
