package dev.spw.collage.host

import com.xuncorp.spw.workshop.api.WorkshopApi
import dev.spw.collage.CollageController
import dev.spw.collage.CollageLog
import java.awt.EventQueue
import java.util.concurrent.Executors

/** Resolves obfuscated SPW targets off the UI thread, then installs on the EDT. */
class HostUiIntegration(
    private val controller: CollageController,
    private val onFailure: (Throwable) -> Unit
) : AutoCloseable {
    private val worker = Executors.newSingleThreadExecutor { task ->
        Thread(task, "spw-collage-host-discovery").apply { isDaemon = true }
    }
    @Volatile
    private var closed = false
    private var hooks: HostUiHooks? = null

    fun start() {
        worker.execute {
            val startedAt = System.nanoTime()
            try {
                val host = WorkshopApi.instance.javaClass
                val index = HostBytecodeIndex.read(host)
                val main = HostUiDiscovery.discover(index)
                val discoveryMs = (System.nanoTime() - startedAt) / 1_000_000
                CollageLog.info("host structure resolved in $discoveryMs ms")
                if (closed) return@execute
                EventQueue.invokeLater {
                    if (closed) return@invokeLater
                    try {
                        val bridge = HostUiBridge(main, host.classLoader, controller)
                        HostUiHooks(bridge, controller).also {
                            it.install()
                            hooks = it
                        }
                        CollageLog.info("host hooks installed; waiting for the page component")
                    } catch (failure: Throwable) {
                        CollageLog.error("Host hook installation failed", failure)
                        if (!closed) onFailure(failure)
                    }
                }
            } catch (failure: Throwable) {
                if (!closed) {
                    CollageLog.error("Host structural discovery failed", failure)
                    onFailure(failure)
                }
            }
        }
    }

    override fun close() {
        closed = true
        CollageLog.info("host integration closing")
        worker.shutdownNow()
        val release = {
            hooks?.close()
            hooks = null
        }
        if (EventQueue.isDispatchThread()) release() else EventQueue.invokeLater { release() }
    }
}
