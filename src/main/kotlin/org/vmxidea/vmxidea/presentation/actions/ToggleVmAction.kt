package org.vmxidea.vmxidea.presentation.actions

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import org.vmxidea.vmxidea.data.storage.VmProjectConfigStorage
import org.vmxidea.vmxidea.model.VmState
import org.vmxidea.vmxidea.presentation.state.VmService

class ToggleVmAction : AnAction() {
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        val project = e.project ?: return
        val config = VmProjectConfigStorage.getInstance(project).getConfig()

        if (config.vmxPath.isBlank()) {
            e.presentation.isEnabled = false
            e.presentation.text = "VM Not Configured"
            e.presentation.icon = AllIcons.Actions.Execute
            return
        }

        e.presentation.isEnabled = true
        val currentState = VmService.getInstance(project).currentState

        when (currentState) {
            VmState.RUNNING -> {
                e.presentation.text = "Stop VM"
                e.presentation.icon = AllIcons.Actions.Suspend
            }
            VmState.STOPPED, VmState.UNKNOWN -> {
                e.presentation.text = "Start VM"
                e.presentation.icon = AllIcons.Actions.Execute
            }
            VmState.PAUSED -> {
                e.presentation.text = "Resume VM"
                e.presentation.icon = AllIcons.Actions.Resume
            }
        }
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        VmService.getInstance(project).toggleVm()
    }
}