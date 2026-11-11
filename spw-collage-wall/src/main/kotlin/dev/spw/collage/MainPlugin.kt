@file:OptIn(com.xuncorp.spw.workshop.api.UnstableSpwWorkshopApi::class)

package dev.spw.collage

import com.xuncorp.spw.workshop.api.PluginContext
import com.xuncorp.spw.workshop.api.PluginPermission
import com.xuncorp.spw.workshop.api.SpwPlugin
import com.xuncorp.spw.workshop.api.WorkshopApi
import dev.spw.collage.host.HostUiIntegration

class MainPlugin(pluginContext: PluginContext) : SpwPlugin(pluginContext) {
    private val controller = CollageController()
    private var integration: HostUiIntegration? = null

    override fun start() {
        CollageLog.info("plugin start requested")
        if (!WorkshopApi.manager.isPermissionGranted(PluginPermission.LIBRARY_READ)) {
            WorkshopApi.ui.toast(
                "SPW Collage Wall 需要“读取曲库”权限",
                WorkshopApi.Ui.ToastType.Warning
            )
            return
        }
        if (!WorkshopApi.manager.isPermissionGranted(PluginPermission.CLASS_TRANSFORM)) {
            WorkshopApi.ui.toast(
                "SPW Collage Wall 需要“类变换”权限以接入主界面",
                WorkshopApi.Ui.ToastType.Warning
            )
            return
        }

        controller.start()
        integration = HostUiIntegration(controller) { failure ->
            CollageLog.error("Host UI integration failed", failure)
            WorkshopApi.ui.toast(
                "SPW Collage Wall 未能接入此 SPW 版本：${failure.cause?.message ?: failure.message}",
                WorkshopApi.Ui.ToastType.Error
            )
        }.also { it.start() }
    }

    override fun update() {
        CollageLog.info("plugin update requested by the player")
    }

    override fun stop() {
        CollageLog.info("plugin stop requested")
        integration?.close()
        integration = null
        controller.closeResources()
    }
}
