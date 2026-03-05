package org.vmxidea.vmxidea.model

data class VmConfig(
    var vmxPath: String = "",
    var vmrunPath: String = "vmrun",
    var startHeadless: Boolean = false,
    var autoStartOnProjectOpen: Boolean = false,
    var autoStopOnProjectClose: Boolean = false
)