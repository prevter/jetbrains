package org.geodesdk.newproject

enum class GeodeTemplate(val displayName: String, val needsRepoInput: Boolean) {
    DEFAULT("Default", false),
    MINIMAL("Minimal", false),
    GITHUB_REPO("GitHub Repository", true),
    LOCAL_REPO("Local Repository", true);

    override fun toString(): String = displayName
}

data class GeodeProjectSettings(
    var template: GeodeTemplate = GeodeTemplate.DEFAULT,
    var repoInput: String = "",
    var name: String = "",
    var version: String = "v1.0.0",
    var developer: String = "",
    var description: String = "",
    var geodeVersion: String = "",
    var githubActions: Boolean = true,
    var removeComments: Boolean = false,
    var initGit: Boolean = true
)
