package org.vmxidea.vmxidea.presentation.lifecycle

import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.project.ProjectManagerListener
import org.vmxidea.vmxidea.data.cli.VmcliControllerImpl
import org.vmxidea.vmxidea.data.storage.VmProjectConfigStorage
import org.vmxidea.vmxidea.presentation.state.VmService

class VmStartupActivity : ProjectActivity {
    override suspend fun execute(project: Project) {
        val vmService = VmService.getInstance(project)

        val config = VmProjectConfigStorage.getInstance(project).getConfig()
        if (config.autoStartOnProjectOpen && config.vmxPath.isNotBlank()) {
            vmService.toggleVm()
        }
    }
}

class VmProjectCloseListener : ProjectManagerListener {
    override fun projectClosing(project: Project) {
        val config = VmProjectConfigStorage.getInstance(project).getConfig()
        if (config.autoStopOnProjectClose && config.vmxPath.isNotBlank()) {
            val controller = VmcliControllerImpl()
            controller.stop(config)
        }
    }
}