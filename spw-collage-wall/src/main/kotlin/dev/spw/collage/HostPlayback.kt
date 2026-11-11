package dev.spw.collage

import com.xuncorp.spw.workshop.api.PlaybackExtensionPoint.MediaItem
import com.xuncorp.spw.workshop.api.WorkshopApi
import java.lang.reflect.Constructor
import java.lang.reflect.Method

/** Stable-name playback facade; the wall owns no audio/playback implementation. */
class HostPlayback {
    private val itemConstructor: Constructor<*> by lazy {
        hostClass("com.xuncorp.pisces.PiscesMediaItem").getConstructor(
            String::class.java, String::class.java, String::class.java,
            String::class.java, String::class.java, String::class.java
        )
    }
    private val controller: Any by lazy {
        val type = hostClass("com.xuncorp.voxzen.service.PlaybackController")
        type.getField("INSTANCE").get(null)
            ?: error("PlaybackController.INSTANCE is unavailable")
    }
    private val setQueue: Method by lazy {
        controller.javaClass.getMethod(
            "setPlaybackQueue", List::class.java, Int::class.javaPrimitiveType
        )
    }
    private val playAt: Method by lazy {
        controller.javaClass.getMethod(
            "playMusicAt", Int::class.javaPrimitiveType, Boolean::class.javaPrimitiveType
        )
    }
    private val queueState: Method by lazy {
        controller.javaClass.getMethod("getPlaybackQueueState")
    }
    private val stateValue: Method by lazy {
        hostClass("kotlinx.coroutines.flow.StateFlow").getMethod("getValue")
    }

    fun play(tracks: List<MediaItem>, selected: MediaItem) {
        val index = tracks.indexOfFirst { it.id == selected.id }
        require(index >= 0) { "Selected track is no longer in the library" }
        val items = tracks.map(::toHostMediaItem)

        val flow = queueState.invoke(controller)
        val previousState = flow?.let(stateValue::invoke)
        setQueue.invoke(controller, items, index)
        awaitQueueReplacement(flow, previousState)
        playAt.invoke(controller, index, false)
    }

    fun toHostMediaItem(track: MediaItem): Any {
        require(track.id.isNotBlank()) { "Track id is blank" }
        require(track.path.isNotBlank()) { "Track path is blank: ${track.title}" }
        return itemConstructor.newInstance(
            track.id, track.title, track.artist, track.album,
            track.albumArtist, fileUriToOsPath(track.path)
        )
    }

    private fun awaitQueueReplacement(flow: Any?, previousState: Any?) {
        if (flow == null || previousState == null) {
            Thread.sleep(150)
            return
        }
        val deadline = System.nanoTime() + 800_000_000L
        while (stateValue.invoke(flow) === previousState && System.nanoTime() < deadline) {
            Thread.sleep(10)
        }
        // Some host builds mutate rather than replace the state object. In that case
        // the conservative fallback preserves the established SPW integration behavior.
        if (stateValue.invoke(flow) === previousState) Thread.sleep(150)
    }

    companion object {
        fun fileUriToOsPath(value: String, windows: Boolean =
            System.getProperty("os.name").contains("win", ignoreCase = true)): String {
            if (value.startsWith("file:///")) {
                val path = value.removePrefix("file:///")
                return if (windows) path.replace('/', '\\') else "/$path"
            }
            if (value.startsWith("file://")) {
                val path = value.removePrefix("file://")
                return if (windows) "\\\\" + path.replace('/', '\\') else "/$path"
            }
            return value
        }

        internal fun hostClass(name: String): Class<*> {
            val loaders = listOfNotNull(
                WorkshopApi::class.java.classLoader,
                Thread.currentThread().contextClassLoader,
                HostPlayback::class.java.classLoader
            ).distinct()
            for (loader in loaders) {
                try {
                    return Class.forName(name, false, loader)
                } catch (_: ClassNotFoundException) {
                    // Plugin and host loaders differ between SPW packages.
                }
            }
            throw ClassNotFoundException(name)
        }
    }
}
