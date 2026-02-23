package org.geodesdk

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.AsyncFileListener
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.events.VFileContentChangeEvent
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.psi.PsiFile
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File

data class ModSetting(val type: String)
data class ModJson(val id: String, val settings: Map<String, ModSetting>)

@Service(Service.Level.PROJECT)
class GeodeModJsonService(private val project: Project) {
    private var cachedRoot: File? = null
    private var cachedModJson: ModJson? = null

    private var cachedIsGeodeMod: Boolean? = null

    init {
        VirtualFileManager.getInstance().addAsyncFileListener(
            { events -> onVfsEvents(events) },
            project
        )
    }

    fun getModJson(sourceFile: PsiFile): ModJson? {
        val startDir = sourceFile.virtualFile?.parent?.let { File(it.path) } ?: return null
        val modJsonFile = findModJson(startDir) ?: return null

        if (modJsonFile.parentFile != cachedRoot) {
            cachedRoot = modJsonFile.parentFile
            cachedModJson = parseModJson(modJsonFile)
        }
        return cachedModJson
    }

    fun isGeodeMod(): Boolean {
        cachedIsGeodeMod?.let { return it }
        val projectRoot = project.guessProjectDir()?.let { File(it.path) }
        val result = projectRoot != null && File(projectRoot, "mod.json").exists()
        cachedIsGeodeMod = result
        return result
    }

    fun invalidate() {
        cachedRoot = null
        cachedModJson = null
        cachedIsGeodeMod = null
    }

    private fun onVfsEvents(events: List<VFileEvent>): AsyncFileListener.ChangeApplier? {
        val relevant = events.any { event ->
            event is VFileContentChangeEvent && event.file.name == "mod.json"
        }

        if (!relevant) return null
        return object : AsyncFileListener.ChangeApplier {
            override fun afterVfsChange() = invalidate()
        }
    }

    private fun findModJson(dir: File): File? {
        var current: File? = dir
        repeat(10) {
            val candidate = File(current, "mod.json")
            if (candidate.exists()) return candidate
            current = current?.parentFile
        }
        return null
    }

    private fun parseModJson(file: File): ModJson? = try {
        val root = Json.parseToJsonElement(file.readText()).jsonObject
        val id = root["id"]?.jsonPrimitive?.content ?: ""
        val settings = root["settings"]?.jsonObject?.mapValues { (_, v) ->
            ModSetting(type = v.jsonObject["type"]?.jsonPrimitive?.content ?: "unknown")
        } ?: emptyMap()
        ModJson(id = id, settings = settings)
    } catch (_: Exception) {
        null
    }

    companion object {
        fun getInstance(project: Project): GeodeModJsonService = project.service()
    }
}