package org.vmxidea.vmxidea.presentation.actions

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.wm.impl.ExpandableComboAction
import org.vmxidea.vmxidea.data.storage.VmProjectConfigStorage
import org.vmxidea.vmxidea.model.VmInstance
import org.vmxidea.vmxidea.model.VmState
import org.vmxidea.vmxidea.presentation.state.VmService
import java.awt.Dimension
import javax.swing.JComponent

class TargetVmComboBoxAction : ExpandableComboAction(), DumbAware {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        val project = e.project

        if (project == null || project.isDisposed) {
            e.presentation.isEnabledAndVisible = false
            return
        }

        val service = VmService.getInstance(project)
        val selectedVm = service.getSelectedVm()

        if (selectedVm != null) {
            val state = service.getState(selectedVm.vmxPath)
            e.presentation.text = selectedVm.name

            e.presentation.icon = AllIcons.General.Web

            val statusText = if (state == VmState.RUNNING) "Running" else "Stopped"
            e.presentation.description = "Selected VM: ${selectedVm.name} ($statusText)"
        } else {
            e.presentation.text = "Select VM..."
            e.presentation.description = "No VM configured"
            e.presentation.icon = AllIcons.General.Web
        }

        e.presentation.isEnabledAndVisible = true
    }

    override fun createCustomComponent(presentation: Presentation, place: String): JComponent {
        return super.createCustomComponent(presentation, place).apply {
            maximumSize = Dimension(Int.MAX_VALUE, maximumSize.height)
        }
    }

    override fun createPopup(event: AnActionEvent): JBPopup? {
        val project = event.project ?: return null
        val group = DefaultActionGroup()
        val config = VmProjectConfigStorage.getInstance(project).getConfig()
        val service = VmService.getInstance(project)

        if (config.vms.isEmpty()) {
            group.add(object : AnAction("No VMs Configured") {
                override fun actionPerformed(e: AnActionEvent) {}
                override fun update(e: AnActionEvent) { e.presentation.isEnabled = false }
                override fun getActionUpdateThread() = ActionUpdateThread.BGT
            })
        } else {
            config.vms.forEach { vm ->
                group.add(SelectVmAction(vm, service))
            }
        }

        group.add(Separator.create())
        group.add(OpenVmSettingsAction("Configure VMs..."))

        return JBPopupFactory.getInstance()
            .createActionGroupPopup(
                null,
                group,
                event.dataContext,
                JBPopupFactory.ActionSelectionAid.SPEEDSEARCH,
                true,
                ActionPlaces.getPopupPlace("VmxIdea.TargetSelection")
            )
    }

    private class SelectVmAction(private val vm: VmInstance, private val service: VmService) : AnAction(), DumbAware {

        override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

        override fun update(e: AnActionEvent) {
            e.presentation.text = vm.name

            val state = service.getState(vm.vmxPath)
            e.presentation.icon = if (state == VmState.RUNNING) {
                AllIcons.RunConfigurations.TestState.Run
            } else {
                AllIcons.Nodes.EmptyNode
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