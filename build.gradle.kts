
import org.jetbrains.changelog.Changelog
import org.jetbrains.changelog.markdownToHTML

fun properties(key: String) = project.findProperty(key).toString()

dependencies {
    implementation("org.junit.jupiter:junit-jupiter:5.8.1")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mongodb:mongodb-driver-sync:4.9.0") // added this line for MongoDB driver
    implementation(kotlin("stdlib-jdk8"))
//    2.162
    implementation("ai.grazie.api:api-gateway-client-jvm:0.3.99"){
        exclude("org.slf4j", "slf4j-api")
    }
    // https://packages.jetbrains.team/maven/p/grazi/grazie-platform-public/ai/grazie/client/client-ktor-jvm/
    implementation("ai.grazie.client:client-ktor:0.3.99"){
        exclude("org.slf4j", "slf4j-api")
    }

    implementation("dev.langchain4j:langchain4j:0.33.0")
    implementation("dev.langchain4j:langchain4j-ollama:0.33.0")
    implementation("dev.langchain4j:langchain4j-open-ai:0.33.0")
//    implementation("org.testcontainers:testcontainers:1.19.1")
    implementation("dev.langchain4j:langchain4j-voyage-ai:0.35.0")
    implementation("org.eclipse.jgit:org.eclipse.jgit:6.4.0.202211300538-r")
    implementation("com.github.javaparser:javaparser-symbol-solver-core:3.26.2")

    implementation("org.jetbrains.kotlinx:kotlinx-cli:0.3.6")

    testImplementation(kotlin("test"))
}

plugins {
    id("java")
    id("org.jetbrains.changelog") version "2.0.0"
    id("org.jetbrains.intellij") version "1.12.0"
    kotlin("jvm") version "1.8.21"
    application
}
configure<ApplicationPluginConvention> {
    mainClassName = "com.intellij.ml.llm.template.cli.CLIKt"
}

group = properties("pluginGroup")
version = properties("pluginVersion")

repositories {
    mavenCentral()
    maven {
        url = uri("https://packages.jetbrains.team/maven/p/grazi/grazie-platform-public")
    }
}

kotlin {
    jvmToolchain(17)
}

intellij {
    pluginName.set(properties("pluginName"))
    version.set(properties("platformVersion"))
    type.set(properties("platformType"))

    plugins.set(properties("platformPlugins").split(',').map(String::trim).filter(String::isNotEmpty))
}

changelog {
    groups.set(emptyList())
    repositoryUrl.set(properties("pluginRepositoryUrl"))
}

tasks {
    wrapper {
        gradleVersion = properties("gradleVersion")
    }

    buildPlugin {
            archiveFileName.set("refactoring-assistant-plugin.zip")
    }

    patchPluginXml {
        version.set(properties("pluginVersion"))
        sinceBuild.set(properties("pluginSinceBuild"))
        untilBuild.set(properties("pluginUntilBuild"))

        pluginDescription.set(
            file("README.md").readText().lines().run {
                val start = "<!-- Plugin description -->"
                val end = "<!-- Plugin description end -->"

                if (!containsAll(listOf(start, end))) {
                    throw GradleException("Plugin description section not found in README.md:\n$start ... $end")
                }
                subList(indexOf(start) + 1, indexOf(end))
            }.joinToString("\n").let { markdownToHTML(it) }
        )

        changeNotes.set(provider {
            with(changelog) {
                renderItem(
                    getOrNull(properties("pluginVersion")) ?: getLatest(),
                    Changelog.OutputType.HTML,
                )
            }
        })
    }
    test{

    }
    runIde{
        systemProperties(Pair("idea.log.warn.categories", "com.intellij,com.android"))
        systemProperties(Pair("idea.log.info.categories", "com.intellij.ml.llm.template"))
    }
}
