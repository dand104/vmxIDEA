package org.vmxidea.vmxidea.repository

import org.vmxidea.vmxidea.model.VmConfig

interface ConfigRepository {
    fun getConfig(): VmConfig
    fun saveConfig(config: VmConfig)
}