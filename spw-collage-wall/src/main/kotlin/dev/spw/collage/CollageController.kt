@file:OptIn(com.xuncorp.spw.workshop.api.UnstableSpwWorkshopApi::class)

package dev.spw.collage

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.xuncorp.spw.workshop.api.PlaybackExtensionPoint.MediaItem
import com.xuncorp.spw.workshop.api.WorkshopApi
import java.awt.EventQueue
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * The wall's state and its host calls.
 *
 * Marked stable because every property the wall reads is snapshot state: a change is always
 * observable, so the wall may skip a block or a card whose inputs did not change instead of
 * recomposing the whole wall whenever anything moves.
 */
@Stable
class CollageController {
    private val playback = HostPlayback()
    private var worker = newWorker()

    var tracks by mutableStateOf<List<MediaItem>>(emptyList()); private set
    var loading by mutableStateOf(true); private set
    var error by mutableStateOf<String?>(null); private set
    var active by mutableStateOf(false); private set

    /**
     * The card the wall has opened, as `row:column:slot`.
     *
     * Keyed by position rather than by song: the wall repeats, so one song can be on screen
     * in more than one place, and only the copy that was clicked should grow.
     */
    var expandedKey by mutableStateOf<String?>(null)

    /** Track the host is playing right now; the wall rings that card in white. */
    var playingId by mutableStateOf<String?>(null); private set

    /** Module setting: when false the wall pans in both axes, when true only vertically. */
    var verticalOnly by mutableStateOf(false); private set

    /** Module setting: cover corner radius in dp; 0 keeps the cards square. */
    var cornerRadius by mutableStateOf(0f); private set

    private var nowPlaying: Thread? = null
    private var preferencesWired = false

    @Volatile
    private var closed = false

    fun start() {
        closed = false
        if (worker.isShutdown) worker = newWorker()
        readPreferences()
        startNowPlayingWatch()
        refresh()
    }

    fun open() {
        expandedKey = null
        // The replaced SPW page is already in composition; flipping this state shows
        // the wall without requiring a navigation-context round trip on first click.
        active = true
        readNowPlaying()
    }

    fun close() {
        active = false
        expandedKey = null
    }

    fun toggleExpanded(key: String) {
        expandedKey = if (expandedKey == key) null else key
    }

    /**
     * A double click opens the card and starts the song in one gesture, so the ring and
     * the opened card are always the same card. Setting — not toggling — the key keeps an
     * already open card open across the two taps of the double click.
     */
    fun openAndPlay(key: String, track: MediaItem) {
        expandedKey = key
        play(track)
    }

    fun play(track: MediaItem) {
        // Optimistic: the ring moves to the tapped card before the host finishes loading
        // its queue, so the wall answers the double click immediately.
        playingId = track.id
        runHostAction("无法播放这首歌曲") {
            playback.play(tracks, track)
        }
    }

    /**
     * Reads the module settings and follows later edits.
     *
     * The host renders both controls from `preference_config.json`; all this does is keep
     * the values in sync, so an edit applies without restarting the player.
     */
    private fun readPreferences() {
        runCatching {
            val manager = WorkshopApi.manager.createConfigManager()
            val helper = manager.getConfig()
            runCatching { helper.reload() }
            applyPreferences { key, fallback -> helper.get(key, fallback) }
            // The plugin instance outlives disable/enable cycles; wire the listener once.
            if (preferencesWired) return@runCatching
            preferencesWired = true
            manager.addConfigChangeListener { changed ->
                EventQueue.invokeLater {
                    if (closed) return@invokeLater
                    applyPreferences { key, fallback ->
                        runCatching { changed.get(key, fallback) }.getOrDefault(fallback)
                    }
                }
            }
        }.onFailure { CollageLog.error("Reading module preferences failed", it) }
    }

    private fun applyPreferences(read: (String, Any) -> Any) {
        verticalOnly = read(PREF_VERTICAL_ONLY, false) as? Boolean ?: false
        val radius = when (val raw = read(PREF_CORNER_RADIUS, 0.0)) {
            is Number -> raw.toDouble()
            else -> 0.0
        }
        cornerRadius = radius.toFloat().coerceIn(0f, MAX_CORNER_RADIUS)
    }

    /**
     * Keeps [playingId] in step with the host.
     *
     * The workshop API only exposes a snapshot query, so the wall polls it while it is on
     * screen. Playing from SPW's own player, from the queue or from a card all end up in
     * the same ring because nothing here assumes who started playback.
     */
    private fun startNowPlayingWatch() {
        if (nowPlaying?.isAlive == true) return
        nowPlaying = Thread({
            while (!closed) {
                try {
                    if (active) readNowPlaying()
                    Thread.sleep(NOW_PLAYING_POLL_MILLIS)
                } catch (_: InterruptedException) {
                    return@Thread
                } catch (_: Throwable) {
                    // A host build without the snapshot query simply keeps the ring off.
                }
            }
        }, "spw-collage-now-playing").apply {
            isDaemon = true
            start()
        }
    }

    private fun readNowPlaying() {
        val id = runCatching { WorkshopApi.playback.getCurrentMediaItem()?.id ?: "" }
            .getOrDefault("")
        if (id == playingId) return
        EventQueue.invokeLater {
            if (!closed) playingId = id.ifBlank { null }
        }
    }

    private fun refresh() {
        loading = true
        error = null
        WorkshopApi.library.getAllTracks().whenComplete { result, failure ->
            EventQueue.invokeLater {
                if (closed) return@invokeLater
                if (failure != null) {
                    error = failure.cause?.message ?: failure.message ?: "曲库读取失败"
                    loading = false
                } else {
                    tracks = result.orEmpty()
                    loading = false
                }
            }
        }
    }

    private fun runHostAction(message: String, action: () -> Unit) {
        if (closed) return
        worker.execute {
            try {
                action()
            } catch (failure: Throwable) {
                EventQueue.invokeLater {
                    if (!closed) WorkshopApi.ui.toast(
                        "$message：${failure.cause?.message ?: failure.message}",
                        WorkshopApi.Ui.ToastType.Error
                    )
                }
            }
        }
    }

    private fun newWorker() = Executors.newSingleThreadExecutor { task ->
        Thread(task, "spw-collage-actions").apply { isDaemon = true }
    }

    fun closeResources() {
        closed = true
        close()
        nowPlaying?.interrupt()
        worker.shutdownNow()
        try {
            worker.awaitTermination(500, TimeUnit.MILLISECONDS)
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
        }
    }

    private companion object {
        /** Fast enough to follow a track change, slow enough to stay off the database. */
        const val NOW_PLAYING_POLL_MILLIS = 1200L

        /** Key of the "vertical only" switch declared in preference_config.json. */
        const val PREF_VERTICAL_ONLY = "scroll.vertical_only"

        /** Key of the corner-radius seekbar declared in preference_config.json. */
        const val PREF_CORNER_RADIUS = "card.corner_radius"

        /** Upper bound of the corner radius, matching the seekbar in the settings page. */
        const val MAX_CORNER_RADIUS = 32f
    }
}
