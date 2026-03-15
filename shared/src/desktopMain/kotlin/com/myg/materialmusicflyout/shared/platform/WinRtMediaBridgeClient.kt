package com.myg.materialmusicflyout.shared.platform

import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

internal class WinRtMediaBridgeClient {
    fun getState(): Map<String, String>? = execute("get-state", 15000L)

    fun toggle(): Boolean = execute("toggle", 2500L)?.get("ok") == "1"

    fun next(): Boolean = execute("next", 2500L)?.get("ok") == "1"

    fun previous(): Boolean = execute("previous", 2500L)?.get("ok") == "1"

    private fun execute(command: String, timeoutMs: Long): Map<String, String>? {
        val executable = findHelperExecutable() ?: return null
        val process = try {
            ProcessBuilder(executable.absolutePath, command)
                .redirectErrorStream(true)
                .start()
        } catch (_: Throwable) {
            return null
        }

        // Read output in parallel; otherwise `get-state` can block when cover base64 fills the pipe.
        val readerExecutor = Executors.newSingleThreadExecutor()
        val outputFuture = readerExecutor.submit<String> {
            process.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        }

        val completed = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS)
        if (!completed) {
            process.destroyForcibly()
            outputFuture.cancel(true)
            readerExecutor.shutdownNow()
            return null
        }

        val output = try {
            outputFuture.get(1200L, TimeUnit.MILLISECONDS)
        } catch (_: TimeoutException) {
            process.destroyForcibly()
            readerExecutor.shutdownNow()
            return null
        } catch (_: Throwable) {
            readerExecutor.shutdownNow()
            return null
        }

        readerExecutor.shutdown()

        val parsed = parseKeyValueOutput(output)
        return parsed.takeIf { it.isNotEmpty() }
    }

    private fun findHelperExecutable(): File? {
        val overridePath = System.getenv("MMF_WINRT_HELPER")
        if (!overridePath.isNullOrBlank()) {
            val overrideFile = File(overridePath)
            if (overrideFile.isFile) return overrideFile
        }

        val roots = generateSequence(File(System.getProperty("user.dir"))) { it.parentFile }
            .take(8)
            .toList()

        val relativeCandidates = listOf(
            "desktopApp/winrt-helper/bin/Release/net8.0-windows10.0.19041.0/win-x64/publish/MediaBridgeHelper.exe",
            "desktopApp/winrt-helper/bin/Release/net8.0-windows/win-x64/publish/MediaBridgeHelper.exe",
            "winrt-helper/bin/Release/net8.0-windows10.0.19041.0/win-x64/publish/MediaBridgeHelper.exe",
            "winrt-helper/bin/Release/net8.0-windows/win-x64/publish/MediaBridgeHelper.exe"
        )

        for (root in roots) {
            for (relative in relativeCandidates) {
                val candidate = File(root, relative)
                if (candidate.isFile) return candidate
            }
        }

        return null
    }

    private fun parseKeyValueOutput(output: String): Map<String, String> {
        return output
            .lineSequence()
            .map { it.trim() }
            .filter { it.contains('=') }
            .associate {
                val idx = it.indexOf('=')
                it.substring(0, idx) to it.substring(idx + 1)
            }
    }
}
