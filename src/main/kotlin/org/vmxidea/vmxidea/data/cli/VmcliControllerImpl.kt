package org.vmxidea.vmxidea.data.cli

import com.intellij.openapi.diagnostic.Logger
import org.vmxidea.vmxidea.model.VmConfig
import org.vmxidea.vmxidea.model.VmState
import org.vmxidea.vmxidea.repository.VmController
import org.vmxidea.vmxidea.utils.VmrunPathResolver
import java.io.File
import java.util.concurrent.TimeUnit

class VmcliControllerImpl : VmController {
    private val log = Logger.getInstance(VmcliControllerImpl::class.java)

    private val hostType = if (System.getProperty("os.name").lowercase().contains("mac")) "fusion" else "ws"

    override fun start(config: VmConfig): Result<Unit> {
        val mode = if (config.startHeadless) "nogui" else "gui"
        return runVmrun(config, listOf("start", config.vmxPath, mode)).map { }
    }
    override fun stop(config: VmConfig): Result<Unit> =
        runVmrun(config, listOf("stop", config.vmxPath, "soft")).map { }

    override fun queryState(config: VmConfig): Result<VmState> {
        val result = runVmrun(config, listOf("list"))
        return result.map { output ->
            val normalizedVmxPath = File(config.vmxPath).absolutePath

            val isRunning = output.lines().any { line ->
                val linePath = File(line.trim()).absolutePath
                linePath.equals(normalizedVmxPath, ignoreCase = true)
            }

            if (isRunning) VmState.RUNNING else VmState.STOPPED
        }
    }

    private fun runVmrun(config: VmConfig, args: List<String>): Result<String> {
        if (config.vmxPath.isBlank() && args.first() != "list") {
            return Result.failure(Exception("VMX path is empty"))
        }

        return try {
            val actualPath = VmrunPathResolver.resolve(config.vmrunPath)

            val command = mutableListOf(actualPath, "-T", hostType)
            command.addAll(args)

            log.info("Executing: ${command.joinToString(" ")}")

            val process = ProcessBuilder(command).start()
            val output = process.inputStream.bufferedReader().readText()
            val error = process.errorStream.bufferedReader().readText()
            process.waitFor(15, TimeUnit.SECONDS)

            if (process.exitValue() == 0) {
                Result.success(output)
            } else {
                Result.failure(Exception("vmrun error: $error"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}