plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.1.20"
    id("org.jetbrains.intellij.platform") version "2.10.2"
    // id("org.jetbrains.kotlin.plugin.compose") version "2.1.20"
}

group = "org.geode-sdk.clion"
version = providers.environmentVariable("RELEASE_VERSION").orElse("1.0.0-dev").get()

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        clion("2025.3.2")
        testFramework(org.jetbrains.intellij.platform.gradle.TestFrameworkType.Platform)

        // composeUI()

        bundledPlugin("com.intellij.modules.json")
        bundledPlugin("Git4Idea")
    }
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "253"
        }

        changeNotes = """
            <ul>
                <li>JSON schema validation for <code>mod.json</code></li>
                <li>Inline color previews for Cocos2d-x color types</li>
                <li>Syntax highlighting for FLAlertLayer color tags in strings and Markdown</li>
                <li>New project wizard for creating Geode mods</li>
                <li>Code inspections for common mistakes (std::cout, dynamic_cast, sto*)</li>
                <li>Code inspections for getSettingValue misuse</li>
            </ul>
        """.trimIndent()
    }
    publishing {
        token = providers.environmentVariable("PUBLISH_TOKEN")
        channels = listOf("stable")
    }
}

tasks {
    withType<JavaCompile> {
        sourceCompatibility = "21"
        targetCompatibility = "21"
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}
