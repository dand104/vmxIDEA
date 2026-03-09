package org.vmxidea.vmxidea.data.cli

import com.intellij.openapi.diagnostic.Logger
import com.intellij.util.concurrency.AppExecutorUtil
import org.vmxidea.vmxidea.model.VmInstance
import org.vmxidea.vmxidea.repository.VmController
import org.vmxidea.vmxidea.utils.VmrunPathResolver
import java.io.File
import java.util.concurrent.Callable
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class VmcliControllerImpl : VmController {
    private val log = Logger.getInstance(VmcliControllerImpl::class.java)
    private val hostType = if (System.getProperty("os.name").lowercase().contains("mac")) "fusion" else "ws"

    override fun start(instance: VmInstance, vmrunPath: String): Result<Unit> {
        val mode = if (instance.startHeadless) "nogui" else "gui"
        return runVmrun(vmrunPath, listOf("start", instance.vmxPath, mode)).map { }
    }

    override fun stop(instance: VmInstance, vmrunPath: String): Result<Unit> =
        runVmrun(vmrunPath, listOf("stop", instance.vmxPath, "soft")).map { }

    override fun getRunningVms(vmrunPath: String): Result<List<String>> {
        return runVmrun(vmrunPath, listOf("list")).map { output ->
            output.lines()
                .drop(1)
                .filter { it.isNotBlank() }
                .map { File(it.trim()).absolutePath }
        }
    }

    private fun runVmrun(vmrunPath: String, args: List<String>): Result<String> {
        return try {
            val actualPath = VmrunPathResolver.resolve(vmrunPath)
            val command = mutableListOf(actualPath, "-T", hostType)
            command.addAll(args)

            val process = ProcessBuilder(command)
                .redirectErrorStream(true)
                .start()

            val futureOutput = AppExecutorUtil.getAppExecutorService().submit(Callable {
                process.inputStream.bufferedReader().use { it.readText() }
            })

            val finished = process.waitFor(30, TimeUnit.SECONDS)

            if (!finished) {
                process.destroyForcibly()
                futureOutput.cancel(true)
                return Result.failure(Exception("Timeout: vmrun command took longer than 30 seconds"))
            }

            val output = try {
                futureOutput.get(1, TimeUnit.SECONDS)
            } catch (e: TimeoutException) {
                futureOutput.cancel(true)
                "VM process spawned and stream kept open."
            } catch (e: Exception) {
                futureOutput.cancel(true)
                "Error reading output."
            }

            if (process.exitValue() == 0) {
                Result.success(output)
            } else {
                Result.failure(Exception("vmrun exit code ${process.exitValue()}: $output"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}