package org.vmxidea.vmxidea.presentation.settings

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.TextBrowseFolderListener
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ui.CollectionListModel
import com.intellij.ui.JBSplitter
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.components.JBList
import com.intellij.util.ui.FormBuilder
import com.intellij.util.ui.JBUI
import org.vmxidea.vmxidea.data.storage.VmProjectConfigStorage
import org.vmxidea.vmxidea.model.VmConfig
import org.vmxidea.vmxidea.model.VmInstance
import org.vmxidea.vmxidea.utils.VmInventoryReader
import java.awt.BorderLayout
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

class VmwareSettingsConfigurable(private val project: Project) : Configurable {

    private val configStorage = VmProjectConfigStorage.getInstance(project)

    private lateinit var listModel: CollectionListModel<VmInstance>
    private lateinit var vmList: JBList<VmInstance>

    private val inventoryCombo = ComboBox<String>()
    private val nameField = JTextField()
    private val pathField = TextFieldWithBrowseButton()
    private val vmInfoLabel = JLabel(" ")
    private val headlessCheckBox = JCheckBox("Start VM in headless mode (no GUI)")
    private val autoStartCheckBox = JCheckBox("Start VM when project opens")
    private val autoStopCheckBox = JCheckBox("Stop VM when project closes")
    private val vmrunPathField = JTextField()

    private var detailPanel: JPanel? = null
    private var isUpdatingUI = false

    override fun getDisplayName() = "VmxIdea"

    override fun createComponent(): JComponent {
        listModel = CollectionListModel()
        vmList = JBList(listModel).apply {
            selectionMode = ListSelectionModel.SINGLE_SELECTION
            addListSelectionListener { updateDetailPanel() }
        }

        val listPanel = ToolbarDecorator.createDecorator(vmList)
            .setAddAction {
                val newVm = VmInstance(name = "New VM")
                listModel.add(newVm)
                vmList.selectedIndex = listModel.size - 1
            }
            .setRemoveAction {
                val index = vmList.selectedIndex
                if (index >= 0) {
                    listModel.remove(index)
                    if (listModel.size > 0) vmList.selectedIndex = maxOf(0, index - 1)
                }
            }
            .createPanel()

        val descriptor = FileChooserDescriptorFactory.createSingleFileDescriptor("vmx").apply {
            title = "Select .vmx File"
        }
        pathField.addBrowseFolderListener(TextBrowseFolderListener(descriptor, project))

        setupInventoryCombo()
        setupListeners()

        val formPanel = FormBuilder.createFormBuilder()
            .addLabeledComponent("Import from Library:", inventoryCombo)
            .addSeparator()
            .addLabeledComponent("VM Name:", nameField)
            .addLabeledComponent("VMX File Path:", pathField)
            .addComponentToRightColumn(vmInfoLabel)
            .addSeparator()
            .addComponent(headlessCheckBox)
            .addComponent(autoStartCheckBox)
            .addComponent(autoStopCheckBox)
            .panel

        detailPanel = JPanel(BorderLayout()).apply {
            border = JBUI.Borders.empty(10)
            add(formPanel, BorderLayout.NORTH)
        }

        val globalSettingsPanel = FormBuilder.createFormBuilder()
            .addSeparator()
            .addLabeledComponent("vmrun Executable:", vmrunPathField)
            .addTooltip("Leave as 'vmrun' to auto-detect from system by standard VMware installation")
            .panel

        detailPanel?.add(globalSettingsPanel, BorderLayout.SOUTH)

        return JBSplitter(false, 0.3f).apply {
            firstComponent = listPanel
            secondComponent = detailPanel
        }
    }

    private fun setupInventoryCombo() {
        val availableVms = VmInventoryReader.getAvailableVms()
        inventoryCombo.addItem("Choose VM...")
        availableVms.forEach { inventoryCombo.addItem(it.name) }

        inventoryCombo.addActionListener {
            if (inventoryCombo.selectedIndex > 0 && !isUpdatingUI) {
                val selectedName = inventoryCombo.selectedItem as String
                val foundVm = availableVms.find { it.name == selectedName }
                if (foundVm != null) {
                    nameField.text = foundVm.name
                    pathField.text = foundVm.path

                    SwingUtilities.invokeLater {
                        isUpdatingUI = true
                        inventoryCombo.selectedIndex = 0
                        isUpdatingUI = false
                    }
                }
            }
        }
    }

    private fun updateDetailPanel() {
        val selected = vmList.selectedValue
        val hasSelection = selected != null

        isUpdatingUI = true
        inventoryCombo.isEnabled = hasSelection
        nameField.isEnabled = hasSelection
        pathField.isEnabled = hasSelection
        headlessCheckBox.isEnabled = hasSelection
        autoStartCheckBox.isEnabled = hasSelection
        autoStopCheckBox.isEnabled = hasSelection

        if (selected != null) {
            nameField.text = selected.name
            pathField.text = selected.vmxPath
            headlessCheckBox.isSelected = selected.startHeadless
            autoStartCheckBox.isSelected = selected.autoStartOnProjectOpen
            autoStopCheckBox.isSelected = selected.autoStopOnProjectClose
            updateVmInfoLabel(selected.vmxPath)
        } else {
            nameField.text = ""
            pathField.text = ""
            headlessCheckBox.isSelected = false
            autoStartCheckBox.isSelected = false
            autoStopCheckBox.isSelected = false
            vmInfoLabel.text = " "
        }
        isUpdatingUI = false
    }

    private fun setupListeners() {
        val updateVm: () -> Unit = {
            if (!isUpdatingUI) {
                vmList.selectedValue?.let { vm ->
                    vm.name = nameField.text
                    vm.vmxPath = pathField.text
                    vm.startHeadless = headlessCheckBox.isSelected
                    vm.autoStartOnProjectOpen = autoStartCheckBox.isSelected
                    vm.autoStopOnProjectClose = autoStopCheckBox.isSelected
                    vmList.repaint()
                }
            }
        }

        nameField.document.addDocumentListener(object : DocumentAdapter() { override fun textChanged(e: DocumentEvent) = updateVm() })
        pathField.textField.document.addDocumentListener(object : DocumentAdapter() {
            override fun textChanged(e: DocumentEvent) {
                updateVm()
                if (!isUpdatingUI) updateVmInfoLabel(pathField.text)
            }
        })
        headlessCheckBox.addActionListener { updateVm() }
        autoStartCheckBox.addActionListener { updateVm() }
        autoStopCheckBox.addActionListener { updateVm() }
    }

    private fun updateVmInfoLabel(path: String) {
        vmInfoLabel.text = if (path.isBlank()) "No file selected." else {
            VmInventoryReader.getVmInfo(path)?.displayText ?: "File not found or invalid format."
        }
    }

    override fun isModified(): Boolean {
        val original = configStorage.getConfig()

        if (vmrunPathField.text != original.vmrunPath) return true

        val currentItems = listModel.items
        if (currentItems.size != original.vms.size) return true

        for (i in currentItems.indices) {
            if (currentItems[i] != original.vms[i]) return true
        }

        return false
    }

    override fun apply() {
        val original = configStorage.getConfig()

        val newVms = listModel.items.map { it.copy() }.toMutableList()
        var newSelectedId = original.selectedVmId

        if (newVms.none { it.id == newSelectedId }) {
            newSelectedId = newVms.firstOrNull()?.id ?: ""
        }

        val newConfig = VmConfig(
            vmrunPath = vmrunPathField.text,
            selectedVmId = newSelectedId,
            vms = newVms
        )

        configStorage.saveConfig(newConfig)
    }

    override fun reset() {
        val currentConfig = configStorage.getConfig()
        vmrunPathField.text = currentConfig.vmrunPath

        listModel.removeAll()
        listModel.addAll(0, currentConfig.vms.map { it.copy() })

        if (listModel.size > 0) {
            vmList.selectedIndex = 0
        } else {
            updateDetailPanel()
        }
    }
}

abstract class DocumentAdapter : DocumentListener {
    override fun insertUpdate(e: DocumentEvent) = textChanged(e)
    override fun removeUpdate(e: DocumentEvent) = textChanged(e)
    override fun changedUpdate(e: DocumentEvent) = textChanged(e)
    abstract fun textChanged(e: DocumentEvent)
}