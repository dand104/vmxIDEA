package org.vmxidea.vmxidea.presentation.state

import com.intellij.ide.ActivityTracker
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.util.concurrency.AppExecutorUtil
import org.vmxidea.vmxidea.data.cli.VmcliControllerImpl
import org.vmxidea.vmxidea.data.storage.VmProjectConfigStorage
import org.vmxidea.vmxidea.model.VmInstance
import org.vmxidea.vmxidea.model.VmState
import org.vmxidea.vmxidea.presentation.notifications.VmNotifier
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

@Service(Service.Level.PROJECT)
class VmService(private val project: Project) : Disposable {
    private val controller = VmcliControllerImpl()
    private val states = ConcurrentHashMap<String, VmState>()

    private val executor = AppExecutorUtil.createBoundedScheduledExecutorService("VmxIdea Poller", 1)

    init {
        executor.scheduleWithFixedDelay({ refreshState() }, 0, 5, TimeUnit.SECONDS)
    }

    fun getSelectedVm(): VmInstance? {
        val config = VmProjectConfigStorage.getInstance(project).getConfig()
        return config.vms.find { it.id == config.selectedVmId } ?: config.vms.firstOrNull()
    }

    fun getState(vmxPath: String): VmState {
        return states[vmxPath] ?: VmState.UNKNOWN
    }

    fun refreshState() {
        val config = VmProjectConfigStorage.getInstance(project).getConfig()
        if (config.vms.isEmpty()) return

        controller.getRunningVms(config.vmrunPath).onSuccess { runningPaths ->
            var changed = false
            for (vm in config.vms) {
                if (vm.vmxPath.isBlank()) continue
                val isRunning = runningPaths.any { it.equals(File(vm.vmxPath).absolutePath, ignoreCase = true) }
                val newState = if (isRunning) VmState.RUNNING else VmState.STOPPED

                if (states[vm.vmxPath] != newState) {
                    states[vm.vmxPath] = newState
                    changed = true
                }
            }
            if (changed) {
                ApplicationManager.getApplication().invokeLater {
                    ActivityTracker.getInstance().inc()
                }
            }
        }
    }

    fun toggleVm(instance: VmInstance? = getSelectedVm()) {
        if (instance == null || instance.vmxPath.isBlank()) return
        val state = getState(instance.vmxPath)
        if (state == VmState.RUNNING) stopVm(instance) else startVm(instance)
    }

    private fun startVm(instance: VmInstance) {
        val config = VmProjectConfigStorage.getInstance(project).getConfig()
        states[instance.vmxPath] = VmState.RUNNING

        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Starting ${instance.name}...", false) {
            override fun run(indicator: ProgressIndicator) {
                indicator.isIndeterminate = true
                controller.start(instance, config.vmrunPath).onSuccess {
                    VmNotifier.notifySuccess(project, "VM '${instance.name}' started successfully")
                }.onFailure { err ->
                    VmNotifier.notifyError(project, "Failed to start '${instance.name}': ${err.message}")
                }.also { refreshState() }
            }
        })
    }

    private fun stopVm(instance: VmInstance) {
        val config = VmProjectConfigStorage.getInstance(project).getConfig()
        states[instance.vmxPath] = VmState.STOPPED

        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Stopping ${instance.name}...", false) {
            override fun run(indicator: ProgressIndicator) {
                indicator.isIndeterminate = true
                controller.stop(instance, config.vmrunPath).onSuccess {
                    VmNotifier.notifySuccess(project, "VM '${instance.name}' stopped successfully")
                }.onFailure { err ->
                    VmNotifier.notifyError(project, "Failed to stop '${instance.name}': ${err.message}")
                }.also { refreshState() }
            }
        })
    }

    override fun dispose() {
        executor.shutdownNow()
    }

    companion object {
        fun getInstance(project: Project): VmService = project.service()
    }
}