package org.vmxidea.vmxidea.repository

import org.vmxidea.vmxidea.model.VmInstance

interface VmController {
    fun start(instance: VmInstance, vmrunPath: String): Result<Unit>
    fun stop(instance: VmInstance, vmrunPath: String): Result<Unit>
    fun getRunningVms(vmrunPath: String): Result<List<String>>
}