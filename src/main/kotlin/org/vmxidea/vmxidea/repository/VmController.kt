package org.vmxidea.vmxidea.repository

import org.vmxidea.vmxidea.model.VmConfig
import org.vmxidea.vmxidea.model.VmState

interface VmController {
    fun start(config: VmConfig): Result<Unit>
    fun stop(config: VmConfig): Result<Unit>
    fun queryState(config: VmConfig): Result<VmState>
}