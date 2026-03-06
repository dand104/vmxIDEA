package org.vmxidea.vmxidea.presentation.actions

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.Separator
import com.intellij.openapi.actionSystem.ex.ComboBoxAction
import com.intellij.openapi.project.DumbAware
import org.vmxidea.vmxidea.data.storage.VmProjectConfigStorage
import org.vmxidea.vmxidea.model.VmInstance
import org.vmxidea.vmxidea.model.VmState
import org.vmxidea.vmxidea.presentation.state.VmService
import javax.swing.JComponent

class TargetVmComboBoxAction : ComboBoxAction(), DumbAware {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        val project = e.project

        if (project == null || project.isDisposed) {
            e.presentation.isEnabledAndVisible = false
            return
        }

        val service = VmService.getInstance(project)
        val selectedVm = service.getSelectedVm()

        e.presentation.icon = AllIcons.General.Web

        if (selectedVm != null) {
            val state = service.getState(selectedVm.vmxPath)
            e.presentation.text = selectedVm.name

            val statusText = if (state == VmState.RUNNING) "Running" else "Stopped"
            e.presentation.description = "Selected VM: ${selectedVm.name} ($statusText)"
        } else {
            e.presentation.text = "Select VM..."
            e.presentation.description = "No VM configured"
        }

        e.presentation.isEnabledAndVisible = true
    }

    override fun createPopupActionGroup(button: JComponent, dataContext: DataContext): DefaultActionGroup {
        val group = DefaultActionGroup()
        val project = CommonDataKeys.PROJECT.getData(dataContext) ?: return group
        val config = VmProjectConfigStorage.getInstance(project).getConfig()
        val service = VmService.getInstance(project)

        group.addSeparator("Available Virtual Machines")

        if (config.vms.isEmpty()) {
            group.add(object : AnAction("No VMs Configured") {
                override fun actionPerformed(e: AnActionEvent) {}
                override fun update(e: AnActionEvent) { e.presentation.isEnabled = false }
            })
        } else {
            config.vms.forEach { vm ->
                group.add(SelectVmAction(vm, service))
            }
        }

        group.add(Separator.create())
        group.add(OpenVmSettingsAction("Configure VMs..."))

        return group
    }

    private class SelectVmAction(private val vm: VmInstance, private val service: VmService) : AnAction() {

        override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

        override fun update(e: AnActionEvent) {
            e.presentation.text = vm.name

            val state = service.getState(vm.vmxPath)
            if (state == VmState.RUNNING) {
                e.presentation.icon = AllIcons.Actions.Execute
            } else {
                e.presentation.icon = null
            }
        }

        override fun actionPerformed(e: AnActionEvent) {
            val project = e.project ?: return
            val storage = VmProjectConfigStorage.getInstance(project)
            val config = storage.getConfig()

            if (config.selectedVmId != vm.id) {
                config.selectedVmId = vm.id
                storage.saveConfig(config)
                VmService.getInstance(project).refreshState()
            }
        }
    }
}