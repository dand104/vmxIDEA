package org.vmxidea.vmxidea.presentation.actions

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import org.vmxidea.vmxidea.model.VmState
import org.vmxidea.vmxidea.presentation.state.VmService

class ToggleVmAction : AnAction() {
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        val project = e.project ?: return
        val service = VmService.getInstance(project)
        val selectedVm = service.getSelectedVm()

        if (selectedVm == null || selectedVm.vmxPath.isBlank()) {
            e.presentation.isEnabled = false
            e.presentation.text = "Start VM"
            e.presentation.icon = AllIcons.Actions.Execute
            return
        }

        e.presentation.isEnabled = true
        val currentState = service.getState(selectedVm.vmxPath)

        when (currentState) {
            VmState.RUNNING -> {
                e.presentation.text = "Stop ${selectedVm.name}"
                e.presentation.icon = AllIcons.Actions.Suspend
            }
            else -> {
                e.presentation.text = "Start ${selectedVm.name}"
                e.presentation.icon = AllIcons.Actions.Execute
            }
        }
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        VmService.getInstance(project).toggleVm()
    }
}