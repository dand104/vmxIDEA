package org.vmxidea.vmxidea.utils

import java.io.File

object VmrunPathResolver {
    fun resolve(configuredPath: String): String {
        if (configuredPath.isNotBlank() && configuredPath != "vmrun") {
            return configuredPath
        }

        val os = System.getProperty("os.name").lowercase()
        val defaultPaths = mutableListOf<String>()

        if (os.contains("win")) {
            defaultPaths.add("C:\\Program Files (x86)\\VMware\\VMware Workstation\\vmrun.exe")
            defaultPaths.add("C:\\Program Files\\VMware\\VMware Workstation\\vmrun.exe")
        } else if (os.contains("mac")) {
            defaultPaths.add("/Applications/VMware Fusion.app/Contents/Library/vmrun")
            defaultPaths.add("/Applications/VMware Fusion.app/Contents/Public/vmrun")
        } else {
            defaultPaths.add("/usr/bin/vmrun")
        }

        for (path in defaultPaths) {
            if (File(path).exists()) return path
        }

        return "vmrun"
    }
}