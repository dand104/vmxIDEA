package org.vmxidea.vmxidea.presentation.settings

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import org.vmxidea.vmxidea.data.storage.VmProjectConfigStorage
import org.vmxidea.vmxidea.utils.VmInventoryReader
import javax.swing.JLabel
import javax.swing.JTextField
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

class VmwareSettingsConfigurable(private val project: Project) : BoundConfigurable("VmxIdea (VMware Workstation VMs)") {

    private val configStorage = VmProjectConfigStorage.getInstance(project)
    private val workingConfig = configStorage.getConfig().copy()

    private lateinit var pathTextField: JTextField
    private lateinit var vmInfoLabel: JLabel

    override fun createPanel(): DialogPanel {
        val availableVms = VmInventoryReader.getAvailableVms()

        return panel {
            group("Virtual Machine Selection") {

                if (availableVms.isNotEmpty()) {
                    row("Discovered VMs:") {
                        val vmNames = listOf("Select from library...") + availableVms.map { it.name }
                        comboBox(vmNames).applyToComponent {
                            addActionListener {
                                if (selectedIndex > 0) {
                                    val selectedName = selectedItem as String
                                    val foundVm = availableVms.find { it.name == selectedName }
                                    if (foundVm != null && ::pathTextField.isInitialized) {
                                        pathTextField.text = foundVm.path
                                    }
                                }
                            }
                        }
                    }
                }

                row("VMX File Path:") {
                    val descriptor = FileChooserDescriptorFactory.createSingleFileDescriptor("vmx")
                        .withTitle("Select .vmx File")

                    textFieldWithBrowseButton(descriptor, project)
                        .bindText(workingConfig::vmxPath)
                        .align(AlignX.FILL)
                        .applyToComponent {
                            pathTextField = this.textField

                            pathTextField.document.addDocumentListener(object : DocumentListener {
                                override fun insertUpdate(e: DocumentEvent?) = updateVmInfoLabel()
                                override fun removeUpdate(e: DocumentEvent?) = updateVmInfoLabel()
                                override fun changedUpdate(e: DocumentEvent?) = updateVmInfoLabel()
                            })
                        }
                }

                row {
                    label("").applyToComponent {
                        vmInfoLabel = this

                        if (::pathTextField.isInitialized) {
                            updateVmInfoLabel()
                        } else {
                            updateVmInfoDirect(workingConfig.vmxPath)
                        }
                    }
                }
            }

            group("Advanced & Automation") {
                row("vmrun Executable:") {
                    textField().bindText(workingConfig::vmrunPath)
                        .comment("Leave as 'vmrun' to auto-detect from system")
                }
                row {
                    checkBox("Start VM in headless mode (no GUI)").bindSelected(workingConfig::startHeadless)
                }
                row {
                    checkBox("Start VM when project opens").bindSelected(workingConfig::autoStartOnProjectOpen)
                }
                row {
                    checkBox("Stop VM when project closes").bindSelected(workingConfig::autoStopOnProjectClose)
                }
            }
        }
    }

    private fun updateVmInfoLabel() {
        if (::pathTextField.isInitialized) {
            updateVmInfoDirect(pathTextField.text)
        }
    }

    private fun updateVmInfoDirect(path: String) {
        val text = if (path.isBlank()) {
            "No VM selected."
        } else {
            VmInventoryReader.getVmInfo(path)?.displayText ?: "File not found or invalid format."
        }

        if (::vmInfoLabel.isInitialized) {
            vmInfoLabel.text = text
        }
    }

    override fun apply() {
        super.apply()
        configStorage.saveConfig(workingConfig.copy())
    }
}