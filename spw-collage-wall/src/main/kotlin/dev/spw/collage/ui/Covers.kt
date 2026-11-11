package dev.spw.collage.ui

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import com.xuncorp.spw.workshop.api.WorkshopApi
import dev.spw.collage.CollageLog
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.ceil
import kotlin.math.roundToInt
import org.jetbrains.skia.Image as SkiaImage
import org.jetbrains.skia.Rect
import org.jetbrains.skia.SamplingMode
import org.jetbrains.skia.Surface

/** Pure decode-size arithmetic, unit-testable without a composition. */
internal object CoverDecode {
    /** Hover measures a non-expanded card five percent larger than its cell. */
    private const val HOVER = 1.05f

    /**
     * Pixels a cover must offer on its shorter side so no card can ever upscale it.
     *
     * The biggest card on the wall spans six cells, so with `ContentScale.Crop` no source
     * axis is ever resolved past `span(6)` — cropping discards the overflow. The hover
     * margin covers the largest measured size of a non-expanded card. Rounded up to a
     * multiple of 64 so small window resizes do not invalidate what is already decoded.
     */
    fun budgetFor(metrics: WallMetrics): Int {
        val needed = ceil(metrics.span(6f) * HOVER).toInt()
        return (needed + 63) and -64
    }

    /**
     * Downscaled `(width, height)` for a stored cover, or null when it already fits.
     *
     * The shorter side is the binding one: with Crop, whichever source axis is scaled up
     * the most is drawn 1:1, and only the rest of the other axis is cropped away.
     */
    fun targetSize(width: Int, height: Int, budget: Int): Pair<Int, Int>? {
        val shorter = minOf(width, height)
        if (shorter <= budget) return null
        val scale = budget.toFloat() / shorter
        return (width * scale).roundToInt() to (height * scale).roundToInt()
    }
}

/**
 * Decoded covers for the wall, sized to what the wall can actually show.
 *
 * Covers used to be kept at their stored size — up to nine megabytes a card and half a
 * gigabyte for a full cache — and were sampled down by the renderer every frame, from
 * textures larger than any card on screen. They are now downscaled once, on decode, to
 * the largest card the current wall can draw; entries that small let the byte-budgeted
 * cache hold every visible cover even on a tall window, so panning never re-decodes.
 */
internal object CoverStore {
    /**
     * Cap on decoded pixels held at once. Entries are a few megabytes each, so the cap
     * both bounds memory far below the old worst case and keeps more covers resident
     * than the old sixty-four-slot count ever could.
     */
    private const val MAX_BYTES = 256 shl 20

    /** A cached cover and the budget it was decoded for. */
    private class Entry(val bitmap: ImageBitmap, val budget: Int) {
        val bytes = bitmap.width * bitmap.height * 4
    }

    /** One host fetch in flight; the budget grows if a larger request joins it. */
    private class Pending(var budget: Int, val callbacks: MutableList<(ImageBitmap?) -> Unit>)

    private val lock = Any()
    /** Access-ordered, so `cached` counts as a use and eviction is true LRU. */
    private val entries = LinkedHashMap<String, Entry>(64, 0.75f, true)
    private var bytes = 0
    private val pending = HashMap<String, Pending>()
    private val reported = AtomicInteger()

    /** Returns the cover if it already resolves at least [budget] pixels. */
    fun cached(id: String, budget: Int): ImageBitmap? = synchronized(lock) {
        val entry = entries[id] ?: return null
        if (entry.budget < budget) null else entry.bitmap
    }

    /** Returns whatever resolution is on hand; used to bridge a re-decode without a flash. */
    fun anyCached(id: String): ImageBitmap? = synchronized(lock) { entries[id]?.bitmap }

    fun load(id: String, budget: Int, callback: (ImageBitmap?) -> Unit) {
        if (id.isBlank()) return
        var resolved: ImageBitmap? = null
        var joined = false
        synchronized(lock) {
            val entry = entries[id]
            if (entry != null && entry.budget >= budget) {
                resolved = entry.bitmap
            } else {
                val inFlight = pending[id]
                if (inFlight != null) {
                    if (budget > inFlight.budget) inFlight.budget = budget
                    inFlight.callbacks += callback
                    joined = true
                } else {
                    pending[id] = Pending(budget, mutableListOf(callback))
                }
            }
        }
        // Deliver outside the lock: the callback writes compose state, which must never
        // run while another thread is waiting on the store.
        val ready = resolved
        if (ready != null) {
            callback(ready)
            return
        }
        if (joined) return
        WorkshopApi.library.getCoverById(id).whenComplete { raw, failure ->
            val encoded = raw?.takeIf { it.isNotEmpty() }
            if (encoded == null) {
                if (reported.getAndIncrement() < 5) {
                    CollageLog.info("cover unavailable for $id: ${failure?.message ?: "no artwork"}")
                }
                deliver(id, null, budget)
                return@whenComplete
            }
            val decodeBudget = synchronized(lock) { pending[id]?.budget ?: budget }
            val decoded = decode(id, encoded, decodeBudget)
            deliver(id, decoded?.bitmap, decoded?.budget ?: decodeBudget)
        }
    }

    private fun deliver(id: String, bitmap: ImageBitmap?, budget: Int) {
        if (bitmap != null) store(id, Entry(bitmap, budget))
        val waiting = synchronized(lock) { pending.remove(id) } ?: return
        for (callback in waiting.callbacks) runCatching { callback(bitmap) }
    }

    private fun store(id: String, entry: Entry) {
        synchronized(lock) {
            val previous = entries.put(id, entry)
            if (previous != null) bytes -= previous.bytes
            bytes += entry.bytes
            val eldest = entries.entries.iterator()
            while (bytes > MAX_BYTES && entries.size > 1) {
                bytes -= eldest.next().value.bytes
                eldest.remove()
            }
        }
    }

    private data class Decoded(val bitmap: ImageBitmap, val budget: Int)

    private fun decode(id: String, bytes: ByteArray, budget: Int): Decoded? = try {
        val image = SkiaImage.makeFromEncoded(bytes)
        val target = CoverDecode.targetSize(image.width, image.height, budget)
        if (target == null) {
            // Full size already fits. The compose bitmap wraps this skia image, so — as
            // before the downscale work — the source is deliberately not closed here.
            if (reported.getAndIncrement() < 3) {
                CollageLog.info("cover $id ${image.width}x${image.height} bytes=${bytes.size}")
            }
            Decoded(image.toComposeImageBitmap(), Int.MAX_VALUE)
        } else {
            val (width, height) = target
            // Raster snapshot downscale. The 0.1.6 attempt failed for the wrong reason:
            // the black covers came from the wall's nested offscreen layers, which are
            // gone since 0.1.9. Linear sampling matches what the renderer itself did
            // when it shrank the full-size texture, so the wall looks as it did.
            val bitmap = Surface.makeRasterN32Premul(width, height).use { surface ->
                surface.canvas.drawImageRect(
                    image,
                    Rect.makeWH(image.width.toFloat(), image.height.toFloat()),
                    Rect.makeWH(width.toFloat(), height.toFloat()),
                    SamplingMode.LINEAR,
                    null,
                    true
                )
                surface.makeImageSnapshot().toComposeImageBitmap()
            }
            // The snapshot owns its pixels, so the oversized source can be released.
            if (reported.getAndIncrement() < 3) {
                CollageLog.info(
                    "cover $id ${image.width}x${image.height} -> ${bitmap.width}x${bitmap.height} bytes=${bytes.size}"
                )
            }
            image.close()
            Decoded(bitmap, budget)
        }
    } catch (failure: Throwable) {
        if (reported.getAndIncrement() < 5) {
            CollageLog.info("cover decode failed for $id: ${failure.message}")
        }
        null
    }
}
