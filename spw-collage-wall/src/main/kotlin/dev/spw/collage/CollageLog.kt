@file:OptIn(com.xuncorp.spw.workshop.api.UnstableSpwWorkshopApi::class)

package dev.spw.collage

import com.xuncorp.spw.workshop.api.WorkshopApi
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/** Persists host-integration failures where they can be inspected after a toast disappears. */
object CollageLog {
    private val timestamp = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
    @Volatile
    private var resolved: File? = null

    fun info(message: String) {
        write("INFO ", message, null)
    }

    fun error(message: String, failure: Throwable? = null) {
        write("ERROR", message, failure)
    }

    private fun write(level: String, message: String, failure: Throwable?) {
        val detail = buildString {
            append("[").append(LocalDateTime.now().format(timestamp)).append("] ")
            append(level).append(' ').append(message)
            if (failure != null) {
                append('\n')
                val trace = StringWriter()
                failure.printStackTrace(PrintWriter(trace))
                append(trace)
            }
            append('\n')
        }
        System.err.print(detail)
        runCatching {
            val file = logFile()
            file.parentFile?.mkdirs()
            file.appendText(detail)
        }
    }

    private fun logFile(): File {
        resolved?.let { return it }
        val config = WorkshopApi.manager.createConfigManager().getConfig()
        val dir = config.getConfigPath().toFile().parentFile
        return File(dir, "spw-collage.log").also { resolved = it }
    }
}
