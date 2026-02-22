package org.geodesdk.clion.newproject

import com.intellij.facet.ui.ValidationResult
import com.intellij.ide.util.projectWizard.AbstractNewProjectStep
import com.intellij.ide.util.projectWizard.CustomStepProjectGenerator
import com.intellij.ide.util.projectWizard.SettingsStep
import com.intellij.openapi.module.Module
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.impl.welcomeScreen.AbstractActionWithPanel
import com.intellij.platform.DirectoryProjectGenerator
import com.intellij.platform.DirectoryProjectGeneratorBase
import com.intellij.platform.ProjectGeneratorPeer
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.IconUtil
import git4idea.commands.Git
import git4idea.commands.GitCommand
import git4idea.commands.GitLineHandler
import java.io.File
import java.net.URI
import javax.swing.Icon
import javax.swing.JComponent
import kotlin.math.min

@Suppress("DialogTitleCapitalization")
class GeodeProjectGenerator : DirectoryProjectGeneratorBase<GeodeProjectSettings>(),
    CustomStepProjectGenerator<GeodeProjectSettings> {

    private var peer: GeodeProjectGeneratorPeer? = null
    override fun createPeer() = GeodeProjectGeneratorPeer().also { peer = it }

    override fun getName() = "Geode Mod"
    override fun getLogo(): Icon {
        val icon = IconLoader.getIcon("/icons/geode.png", javaClass)
        val targetSize = 16
        val scale = min(targetSize.toFloat() / icon.iconWidth, targetSize.toFloat() / icon.iconHeight)
        return IconUtil.scale(icon, null, scale)
    }

    override fun validate(baseDirPath: String): ValidationResult {
        val errors = peer?.newProjectPanel?.validate() ?: return ValidationResult("Settings not initialized")
        return if (errors.isEmpty()) ValidationResult.OK
        else ValidationResult(errors.first().message)
    }

    override fun generateProject(
        project: Project, baseDir: VirtualFile, settings: GeodeProjectSettings, module: Module
    ) {
        ProgressManager.getInstance().run(object : Task.Modal(project, "Generating Geode Mod", false) {
            override fun run(indicator: ProgressIndicator) {
                try {
                    generate(project, File(baseDir.path), settings, indicator)
                } catch (e: Exception) {
                    com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater {
                        Messages.showErrorDialog(
                            project, e.message ?: e.toString(), "Geode: Project Creation Failed"
                        )
                    }
                }
            }
        })
    }

    override fun createStep(
        projectGenerator: DirectoryProjectGenerator<GeodeProjectSettings>,
        callback: AbstractNewProjectStep.AbstractCallback<GeodeProjectSettings>
    ): AbstractActionWithPanel = GeodeProjectWizardStep(projectGenerator)

    private fun generate(
        project: Project, baseDir: File, settings: GeodeProjectSettings, indicator: ProgressIndicator
    ) {
        val source = settings.template.resolveSource(settings.repoInput)
        val modId = deriveModId(settings.developer, settings.name)

        indicator.text = "Cloning template from ${source.url}..."
        indicator.fraction = 0.1

        cloneRepo(project, source, baseDir)
        baseDir.resolve(".git").deleteRecursively()

        indicator.text = "Configuring project files..."
        indicator.fraction = 0.5

        val filteredName = settings.name.filter { !it.isWhitespace() }
        baseDir.resolve("CMakeLists.txt").replaceAllText("Template", filteredName)
        baseDir.resolve("README.md").replaceAllText("Template", settings.name)

        indicator.fraction = 0.6
        val modJsonFile = baseDir.resolve("mod.json")
        if (modJsonFile.exists()) modJsonFile.substitutePlaceholders(settings, modId)
        else modJsonFile.writeText(buildDefaultModJson(settings, modId))

        if (settings.removeComments) {
            indicator.text = "Stripping comments..."
            indicator.fraction = 0.7

            baseDir.resolve("CMakeLists.txt").stripCmakeComments()
            baseDir.resolve("src").walkTopDown().filter { it.isFile && it.extension in setOf("cpp", "hpp") }.forEach { it.stripCppComments() }
        }

        if (settings.githubActions) {
            indicator.text = "Setting up GitHub Actions workflow..."
            indicator.fraction = 0.8
            downloadGithubAction(baseDir)
        }

        if (settings.initGit) {
            indicator.text = "Initializing Git repository..."
            indicator.fraction = 0.9

            val handler = GitLineHandler(project, baseDir, GitCommand.INIT)
            val result = Git.getInstance().runCommand(handler)
            if (result.exitCode != 0) throw RuntimeException(
                "git init failed with exit code ${result.exitCode}:\n${result.errorOutput.joinToString("\n")}"
            )
        }

        indicator.fraction = 1.0
    }

    private fun deriveModId(developer: String, name: String): String {
        fun String.toIdPart() = lowercase().replace(' ', '-').replace("\"", "")
        return "${developer.toIdPart()}.${name.toIdPart()}"
    }

    private fun cloneRepo(project: Project, source: TemplateSource, targetDir: File) {
        // clion creates the target directory and adds files before we can clone into it,
        // so we create a temporary subdirectory and move the files out after cloning
        val tmpDir = targetDir.resolve(".geode-template-tmp")
        try {
            val handler = GitLineHandler(project, targetDir, GitCommand.CLONE).apply {
                addParameters(source.url, tmpDir.absolutePath)
                addParameters("--branch", source.branch)
                addParameters("--depth", "1")
            }
            val result = Git.getInstance().runCommand(handler)
            if (result.exitCode != 0) throw RuntimeException(
                "git clone failed with exit code ${result.exitCode}:\n${result.errorOutput.joinToString("\n")}"
            )

            tmpDir.listFiles()?.forEach { file ->
                val dest = targetDir.resolve(file.name)
                dest.deleteRecursively()
                file.renameTo(dest)
            }
        } finally {
            tmpDir.deleteRecursively()
        }
    }

    private fun File.substitutePlaceholders(s: GeodeProjectSettings, modId: String) {
        writeText(
            readText()
                .replace("\$GEODE_VERSION", s.geodeVersion)
                .replace("\$MOD_VERSION", s.version)
                .replace("\$MOD_ID", modId)
                .replace("\$MOD_NAME", s.name.escapeJson())
                .replace("\$MOD_DEVELOPER", s.developer.escapeJson())
                .replace("\$MOD_DESCRIPTION", s.description.escapeJson())
        )
    }

    private fun buildDefaultModJson(s: GeodeProjectSettings, modId: String) = """
        {
            "geode":"${s.geodeVersion}",
            "version": "${s.version}",
            "id": "$modId",
            "name": "${s.name.escapeJson()}",
            "developer": "${s.developer.escapeJson()}",
            "description": "${s.description.escapeJson()}"
        }
    """.trimIndent()

    private fun String.escapeJson() = replace("\"", "\\\"")
    private fun File.replaceAllText(old: String, new: String) {
        if (exists()) writeText(readText().replace(old, new))
    }

    private fun File.stripCmakeComments() {
        if (exists()) writeText(readText().replace(Regex("\n#[^\n]*"), ""))
    }

    private fun File.stripCppComments() {
        if (exists()) writeText(readText().replace(
            Regex("""(?m)^.*?/\*[\s\S]*?\*/\r?\n?|^.*//[^\n]*\r?\n?"""), ""
        ))
    }

    private fun downloadGithubAction(location: File) {
        val url  = "https://raw.githubusercontent.com/geode-sdk/build-geode-mod/main/examples/multi-platform.yml"
        val dest = location.resolve(".github/workflows/multi-platform.yml")
        dest.parentFile.mkdirs()
        try { dest.writeBytes(URI(url).toURL().readBytes()) }
        catch (e: Exception) {
            throw RuntimeException("Failed to download GitHub Actions workflow: ${e.message}")
        }
    }
}

data class TemplateSource(val url: String, val branch: String = "main")

fun GeodeTemplate.resolveSource(repoInput: String): TemplateSource = when (this) {
    GeodeTemplate.DEFAULT -> TemplateSource("https://github.com/geode-sdk/example-mod")
    GeodeTemplate.MINIMAL -> TemplateSource("https://github.com/geode-sdk/example-mod", "minimal")
    GeodeTemplate.GITHUB_REPO -> parseRepoInput("https://github.com/$repoInput")
    GeodeTemplate.LOCAL_REPO -> parseRepoInput(repoInput)
}

private fun parseRepoInput(raw: String): TemplateSource {
    val (base, branch) = if ('@' in raw) raw.split('@', limit = 2).let { it[0] to it[1] }
    else raw to "main"
    val url = if (!base.startsWith("http") && '/' in base) "https://github.com/$base" else base
    return TemplateSource(url, branch)
}

class GeodeProjectGeneratorPeer : ProjectGeneratorPeer<GeodeProjectSettings> {
    internal val newProjectPanel = GeodeNewProjectPanel()

    override fun getComponent(locationField: TextFieldWithBrowseButton, checkValid: Runnable): JComponent {
        newProjectPanel.onAnyChange = { checkValid.run() }
        return panel { newProjectPanel.attachTo(this) }
    }

    @Deprecated("Used by older platform versions")
    override fun getComponent(): JComponent = panel { newProjectPanel.attachTo(this) }
    override fun getSettings(): GeodeProjectSettings = newProjectPanel.settings
    override fun buildUI(step: SettingsStep) {}
    override fun isBackgroundJobRunning() = false
    override fun validate(): ValidationInfo? = newProjectPanel.validate().firstOrNull()?.let {
        ValidationInfo(it.message)
    }
}
