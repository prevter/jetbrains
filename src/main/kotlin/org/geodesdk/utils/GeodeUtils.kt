package org.geodesdk.utils;

private val GEODE_VERSION_REGEX = Regex("""Geode SDK version:\s*(\S+)""")
private val GEODE_DEVELOPER_REGEX = Regex("""default-developer\s*=\s*(.+)""")

class GeodeUtils {
    companion object {
        fun runGeodeCli(vararg args: String): String? = try {
            val process = ProcessBuilder("geode", *args)
                .redirectErrorStream(true)
                .start()
            val output = process.inputStream.bufferedReader().readText()
            process.waitFor()
            output.takeIf { process.exitValue() == 0 }
        } catch (_: Exception) {
            null
        }

        fun detectGeodeSdkVersion(): String? =
            runGeodeCli("sdk", "version")
                ?.let { GEODE_VERSION_REGEX.find(it)?.groupValues?.get(1) }

        fun detectGeodeDefaultDeveloper(): String? =
            runGeodeCli("config", "get", "default-developer")
                ?.let { GEODE_DEVELOPER_REGEX.find(it)?.groupValues?.get(1)?.trim() }
    }
}