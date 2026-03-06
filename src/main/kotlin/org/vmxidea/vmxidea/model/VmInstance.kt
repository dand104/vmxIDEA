package org.vmxidea.vmxidea.model

import java.util.UUID

data class VmInstance(
    var id: String = UUID.randomUUID().toString(),
    var name: String = "New Virtual Machine",
    var vmxPath: String = "",
    var startHeadless: Boolean = false,
    var autoStartOnProjectOpen: Boolean = false,
    var autoStopOnProjectClose: Boolean = false
) {
    override fun toString(): String = name.ifBlank { "Unnamed VM" }
}