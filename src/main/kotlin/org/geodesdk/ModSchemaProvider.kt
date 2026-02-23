package org.geodesdk

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.jsonSchema.extension.JsonSchemaFileProvider
import com.jetbrains.jsonSchema.extension.JsonSchemaProviderFactory
import com.jetbrains.jsonSchema.extension.SchemaType

class ModSchemaProvider : JsonSchemaProviderFactory {
    override fun getProviders(project: Project): List<JsonSchemaFileProvider> = listOf(ModJsonSchemaProvider())
}

class ModJsonSchemaProvider : JsonSchemaFileProvider {
    override fun isAvailable(file: VirtualFile): Boolean = file.name == "mod.json"
    override fun getName(): String = "Geode Mod"
    override fun getSchemaType(): SchemaType = SchemaType.embeddedSchema
    override fun getSchemaFile(): VirtualFile? =
        JsonSchemaProviderFactory.getResourceFile(javaClass, "/schemas/mod.schema.json")
}