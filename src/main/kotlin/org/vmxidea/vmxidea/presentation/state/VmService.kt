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
import org.vmxidea.vmxidea.model.VmState
import org.vmxidea.vmxidea.presentation.notifications.VmNotifier
import java.util.concurrent.TimeUnit

@Service(Service.Level.PROJECT)
class VmService(private val project: Project) : Disposable {
    private val controller = VmcliControllerImpl()
    @Volatile var currentState: VmState = VmState.UNKNOWN
        private set

    private val executor = AppExecutorUtil.createBoundedScheduledExecutorService("VmxIdea Poller", 1)

    init {
        executor.scheduleWithFixedDelay({ refreshState() }, 0, 5, TimeUnit.SECONDS)
    }

    fun refreshState() {
        val config = VmProjectConfigStorage.getInstance(project).getConfig()
        if (config.vmxPath.isBlank()) {
            updateState(VmState.UNKNOWN)
            return
        }
        controller.queryState(config).onSuccess { newState ->
            updateState(newState)
        }
    }

    private fun updateState(newState: VmState) {
        if (currentState != newState) {
            currentState = newState
            ApplicationManager.getApplication().invokeLater {
                ActivityTracker.getInstance().inc()
            }
        }
    }

    fun toggleVm() {
        if (currentState == VmState.RUNNING) stopVm() else startVm()
    }

    private fun startVm() {
        val config = VmProjectConfigStorage.getInstance(project).getConfig()
        updateState(VmState.RUNNING)
        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Starting Virtual Machine...", false) {
            override fun run(indicator: ProgressIndicator) {
                controller.start(config).onSuccess {
                    VmNotifier.notifySuccess(project, "VM started successfully")
                }.onFailure { err ->
                    VmNotifier.notifyError(project, "Failed to start VM: ${err.message}")
                }.also { refreshState() }
            }
        })
    }

    private fun stopVm() {
        val config = VmProjectConfigStorage.getInstance(project).getConfig()
        updateState(VmState.STOPPED)
        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Stopping Virtual Machine...", false) {
            override fun run(indicator: ProgressIndicator) {
                controller.stop(config).onSuccess {
                    VmNotifier.notifySuccess(project, "VM stopped successfully")
                }.onFailure { err ->
                    VmNotifier.notifyError(project, "Failed to stop VM: ${err.message}")
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