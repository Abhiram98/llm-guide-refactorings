
import org.jetbrains.changelog.Changelog
import org.jetbrains.changelog.markdownToHTML

fun properties(key: String) = project.findProperty(key).toString()

dependencies {
    implementation("org.junit.jupiter:junit-jupiter:5.8.1")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mongodb:mongodb-driver-sync:4.9.0") // added this line for MongoDB driver
    implementation(kotlin("stdlib-jdk8"))
    implementation("com.squareup.okhttp3:okhttp:4.9.3")
//    2.162
    implementation("ai.grazie.api:api-gateway-client-jvm:0.3.99"){
        exclude("org.slf4j", "slf4j-api")
    }
    // https://packages.jetbrains.team/maven/p/grazi/grazie-platform-public/ai/grazie/client/client-ktor-jvm/
    implementation("ai.grazie.client:client-ktor:0.3.99"){
        exclude("org.slf4j", "slf4j-api")
    }

    implementation ("com.github.tsantalis:refactoring-miner:3.0.10")

    implementation("dev.langchain4j:langchain4j:0.33.0")
    implementation("dev.langchain4j:langchain4j-ollama:0.33.0")
    implementation("dev.langchain4j:langchain4j-open-ai:0.33.0")
//    implementation("org.testcontainers:testcontainers:1.19.1")
    implementation("dev.langchain4j:langchain4j-voyage-ai:0.35.0")

    implementation("org.eclipse.jgit:org.eclipse.jgit:6.4.0.202211300538-r")
    implementation("com.github.javaparser:javaparser-symbol-solver-core:3.26.2")

    implementation("org.jetbrains.kotlinx:kotlinx-cli:0.3.6")
    implementation("io.ktor:ktor-server-netty:3.0.3")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.0")
    implementation("io.ktor:ktor-server-core-jvm:3.0.3")
    implementation("io.ktor:ktor-server-host-common-jvm:3.0.3")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:3.0.3")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:3.0.3")
    implementation("io.ktor:ktor-server-netty-jvm:3.0.3")
//    implementation("ch.qos.logback:logback-classic")
    implementation("io.ktor:ktor-server-config-yaml:3.0.3")
//    implementation("ai.grazie.agents:code-agents:0.3.122")
    implementation("ai.jetbrains.code.agents:code-agents-core:1.0.0-beta.25+0.4.12")
    implementation("ai.jetbrains.code.agents:code-agents-core-tools:1.0.0-beta.25+0.4.12")
    implementation("ai.jetbrains.code.agents:code-agents-tools-registry:1.0.0-beta.25+0.4.12")
    implementation("ai.jetbrains.code.agents:code-agents-ideformer-client:1.0.0-beta.25+0.4.12")
    implementation("ai.jetbrains.code.agents:code-agents-ideformer-daemon:1.0.0-beta.25+0.4.12")
    implementation("ai.jetbrains.code.agents:code-agents-ideformer-executable:1.0.0-beta.25+0.4.12")
//    implementation("ai.jetbrains.code.files:code-files-jvm:1.0.0-beta.25+0.4.12")
//    implementation("ai.jetbrains.code.files:code-files-model:1.0.0-beta.25+0.4.12")
//    implementation("org.jetbrains:ij-parsing-core:0.3.175")



    testImplementation("io.ktor:ktor-client-content-negotiation:3.0.3")
    testImplementation("io.ktor:ktor-server-test-host-jvm")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:3.0.3")
    testImplementation("com.jayway.jsonpath:json-path:2.9.0")

    testImplementation(kotlin("test"))
    testImplementation("io.ktor:ktor-server-test-host-jvm:3.0.3")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:2.1.0")
}

plugins {
    id("java")
    id("org.jetbrains.changelog") version "2.0.0"
    id("org.jetbrains.intellij") version "1.12.0"
    id("org.jetbrains.kotlin.plugin.serialization") version "2.0.0"
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
