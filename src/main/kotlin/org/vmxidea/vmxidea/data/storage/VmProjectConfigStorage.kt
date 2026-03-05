package org.vmxidea.vmxidea.data.storage

import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import org.vmxidea.vmxidea.model.VmConfig
import org.vmxidea.vmxidea.repository.ConfigRepository

@Service(Service.Level.PROJECT)
@State(name = "vmxIdea", storages = [Storage("vmxidea_config.xml")])
class VmProjectConfigStorage : PersistentStateComponent<VmConfig>, ConfigRepository {
    private var myState = VmConfig()

    override fun getState(): VmConfig = myState

    override fun loadState(state: VmConfig) {
        myState = state
    }

    override fun getConfig(): VmConfig = myState

    override fun saveConfig(config: VmConfig) {
        myState = config
    }

    companion object {
        fun getInstance(project: Project): VmProjectConfigStorage = project.service()
    }
}