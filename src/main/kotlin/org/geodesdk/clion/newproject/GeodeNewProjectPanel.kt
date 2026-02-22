package org.geodesdk.clion.newproject

import com.intellij.openapi.observable.properties.PropertyGraph
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.Row
import com.intellij.ui.dsl.builder.bindItem
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.bindText

private val GEODE_VERSION_REGEX = Regex("""Geode SDK version:\s*(\S+)""")
private val GEODE_DEVELOPER_REGEX = Regex("""default-developer\s*=\s*(.+)""")

private fun runGeodeCli(vararg args: String): String? = try {
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

class GeodeNewProjectPanel {
    var onAnyChange: (() -> Unit)? = null

    private val graph = PropertyGraph()
    private val templateProp = graph.property(GeodeTemplate.DEFAULT)
    private val repoInputProp = graph.property("")
    private val nameProp = graph.property("")
    private val versionProp = graph.property("v1.0.0")
    private val developerProp = graph.property(detectGeodeDefaultDeveloper() ?: "")
    private val descriptionProp = graph.property("")
    private val geodeVerProp = graph.property(detectGeodeSdkVersion() ?: "")
    private val githubActProp = graph.property(true)
    private val removeComProp = graph.property(false)
    private val needsRepoProp = graph.property(false)
    private val initGitProp = graph.property(true)

    init {
        needsRepoProp.dependsOn(templateProp) { templateProp.get().needsRepoInput }

        listOf(
            templateProp, repoInputProp, nameProp, versionProp,
            developerProp, descriptionProp, geodeVerProp,
            githubActProp, removeComProp, initGitProp
        ).forEach { prop ->
            prop.afterChange { onAnyChange?.invoke() }
        }
    }

    val settings: GeodeProjectSettings
        get() = GeodeProjectSettings(
            template = templateProp.get(),
            repoInput = repoInputProp.get(),
            name = nameProp.get(),
            version = versionProp.get(),
            developer = developerProp.get(),
            description = descriptionProp.get(),
            geodeVersion = geodeVerProp.get(),
            githubActions = githubActProp.get(),
            removeComments = removeComProp.get(),
            initGit = initGitProp.get()
        )

    fun validate(): List<ValidationInfo> = buildList {
        if (nameProp.get().isBlank())
            add(ValidationInfo("Mod name is required"))
        if (developerProp.get().isBlank())
            add(ValidationInfo("Developer name is required"))
        if (versionProp.get().isBlank())
            add(ValidationInfo("Version is required"))
        if (geodeVerProp.get().isBlank())
            add(ValidationInfo("Geode version is required"))
        if (templateProp.get().needsRepoInput && repoInputProp.get().isBlank())
            add(ValidationInfo("Repository input is required for the selected template"))
    }

    fun attachTo(panel: Panel) = with(panel) {
        row("Template:") {
            comboBox(GeodeTemplate.entries)
                .bindItem(templateProp)
        }

        row("Repo:") {
            textField()
                .bindText(repoInputProp)
                .comment(
                    "GitHub: <code>user/repo</code> or <code>user/repo@branch</code><br>" +
                            "Local: <code>/path/to/repo</code> or <code>/path/to/repo@branch</code>"
                )
                .align(AlignX.FILL)
        }.visibleIf(needsRepoProp)

        row("Name:") {
            textField()
                .bindText(nameProp)
                .align(AlignX.FILL)
        }

        row("Version:") {
            textField()
                .bindText(versionProp)
                .align(AlignX.FILL)
        }

        row("Developer:") {
            textField()
                .bindText(developerProp)
                .applyToComponent {
                    if (developerProp.get().isNotEmpty())
                        toolTipText = "Auto-detected from 'geode config get default-developer'"
                }
                .align(AlignX.FILL)
        }

        row("Description:") {
            textField()
                .bindText(descriptionProp)
                .comment("Optional")
                .align(AlignX.FILL)
        }

        row("Geode SDK:") {
            textField()
                .bindText(geodeVerProp)
                .applyToComponent {
                    toolTipText = if (geodeVerProp.get().isNotEmpty())
                        "Auto-detected from 'geode sdk version'"
                    else
                        "'geode' CLI not found — enter version manually"
                }
                .comment("Auto-detected from installed Geode SDK. Edit if targeting a different version.")
                .align(AlignX.FILL)
        }

        separator()

        row {
            checkBox("Add cross-platform GitHub Actions workflow")
                .bindSelected(githubActProp)
        }
        row {
            checkBox("Remove comments from template")
                .bindSelected(removeComProp)
        }
        row {
            checkBox("Initialize Git repository")
                .bindSelected(initGitProp)
        }
    }
}
