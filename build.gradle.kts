plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.1.20"
    id("org.jetbrains.intellij.platform") version "2.10.2"
    // id("org.jetbrains.kotlin.plugin.compose") version "2.1.20"
}

group = "org.geode-sdk.clion"
version = providers.environmentVariable("RELEASE_VERSION").orElse("1.1.0-dev").get()

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
                <li><code>Launch Geometry Dash</code> run configuration</li>
                <li>Support color tags in JSON files</li>
                <li>Fixed settings lint not correctly validating types with namespaces</li>
                <li>Fixed old dependency style schema</li>
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
