package org.vmxidea.vmxidea.model

data class VmConfig(
    var vmrunPath: String = "vmrun",
    var selectedVmId: String = "",
    var vms: MutableList<VmInstance> = mutableListOf()
)