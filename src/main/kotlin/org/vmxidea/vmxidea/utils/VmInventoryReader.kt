package org.vmxidea.vmxidea.utils

import java.io.File

data class VmInfo(
    val path: String,
    val name: String,
    val os: String,
    val cpus: String,
    val ramMB: String
) {
    val displayText: String
        get() = "$name ($os) — $cpus Cores, ${ramMB}MB RAM"
}

object VmInventoryReader {
    fun getAvailableVms(): List<VmInfo> {
        val inventoryPath = getInventoryPath() ?: return emptyList()
        val file = File(inventoryPath)
        if (!file.exists()) return emptyList()

        val vmxPaths = mutableListOf<String>()
        file.forEachLine { line ->
            if (line.contains(".config = ")) {
                val path = line.substringAfter("\"").substringBeforeLast("\"")
                if (File(path).exists()) {
                    vmxPaths.add(path)
                }
            }
        }
        return vmxPaths.mapNotNull { getVmInfo(it) }
    }

    fun getVmInfo(vmxPath: String): VmInfo? {
        val file = File(vmxPath)
        if (!file.exists()) return null

        var name = file.nameWithoutExtension
        var os = "Unknown OS"
        var cpus = "1"
        var ram = "Unknown"

        file.forEachLine { line ->
            when {
                line.startsWith("displayName") -> name = extractValue(line)
                line.startsWith("guestOS") -> os = extractValue(line)
                line.startsWith("numvcpus") -> cpus = extractValue(line)
                line.startsWith("memsize") -> ram = extractValue(line)
            }
        }
        return VmInfo(vmxPath, name, os, cpus, ram)
    }

    private fun getInventoryPath(): String? {
        val os = System.getProperty("os.name").lowercase()
        return when {
            os.contains("win") -> System.getenv("APPDATA") + "\\VMware\\inventory.vmls"
            os.contains("mac") -> System.getProperty("user.home") + "/Library/Application Support/VMware Fusion/vmInstances"
            else -> System.getProperty("user.home") + "/.vmware/inventory.vmls"
        }
    }

    private fun extractValue(line: String): String = line.substringAfter("\"").substringBeforeLast("\"")
}