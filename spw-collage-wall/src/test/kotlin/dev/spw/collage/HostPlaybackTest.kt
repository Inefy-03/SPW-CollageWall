package dev.spw.collage

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class HostPlaybackTest {
    @Test
    fun `converts SPW file URI to Windows path`() {
        assertEquals(
            "D:\\Music\\song.flac",
            HostPlayback.fileUriToOsPath("file:///D:/Music/song.flac", windows = true)
        )
    }

    @Test
    fun `converts SPW file URI to Unix path`() {
        assertEquals(
            "/home/user/Music/song.flac",
            HostPlayback.fileUriToOsPath("file:///home/user/Music/song.flac", windows = false)
        )
    }

    @Test
    fun `leaves an OS path unchanged`() {
        assertEquals(
            "C:\\Music\\song.flac",
            HostPlayback.fileUriToOsPath("C:\\Music\\song.flac", windows = true)
        )
    }
}
