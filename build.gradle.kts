import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import groovy.util.Node
import groovy.util.NodeList
import org.apache.tools.ant.filters.ReplaceTokens
import org.gradle.api.artifacts.Configuration
import org.gradle.api.credentials.PasswordCredentials
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.external.javadoc.StandardJavadocDocletOptions
import org.gradle.jvm.tasks.Jar
import org.gradle.language.jvm.tasks.ProcessResources
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `java-library`
    eclipse
    `maven-publish`
    id("com.gradleup.shadow") version "8.3.9"
    kotlin("jvm") version "2.0.21"
}

group = "org.mvplugins.multiverse.core"
version = System.getenv("GITHUB_VERSION") ?: "local"
description = "Multiverse-Core"

val sourceSets = extensions.getByType<SourceSetContainer>()

val serverApi: Configuration by configurations.creating
val externalPlugin: Configuration by configurations.creating
val shadowed: Configuration by configurations.creating

repositories {
    mavenLocal()
    mavenCentral()

    maven {
        url = uri("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    }

    maven {
        url = uri("https://repo.onarandombox.com/content/groups/public")
    }

    maven {
        url = uri("https://hub.spigotmc.org/nexus/content/groups/public/")
    }

    maven {
        url = uri("https://jitpack.io")
    }

    maven {
        url = uri("https://repo.minebench.de/")
    }

    maven {
        url = uri("https://repo.maven.apache.org/maven2/")
    }

    maven {
        name = "aikar repo"
        url = uri("https://repo.aikar.co/content/groups/aikar/")
    }

    maven {
        name = "glaremasters repo"
        url = uri("https://repo.glaremasters.me/repository/towny/")
    }

    maven {
        name = "helpchatRepoReleases"
        url = uri("https://repo.helpch.at/releases/")
    }

    maven {
        name = "papermc repo"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }

    // Purpur API
    maven("https://repo.purpurmc.org/snapshots")
}

// Configuration wiring like in the Groovy build
configurations["compileOnly"].extendsFrom(serverApi)
configurations["runtimeClasspath"].extendsFrom(serverApi)

configurations["implementation"].extendsFrom(externalPlugin)

configurations["compileClasspath"].extendsFrom(shadowed)
configurations["testCompileClasspath"].extendsFrom(shadowed)
configurations["testRuntimeClasspath"].extendsFrom(shadowed)

dependencies {
    // Server API: Purpur 1.19.4
    serverApi("org.purpurmc.purpur:purpur-api:1.19.4-R0.1-SNAPSHOT")

    // Economy
    externalPlugin("com.github.MilkBowl:VaultAPI:1.7.1") {
        exclude(group = "org.bukkit", module = "bukkit")
    }

    // PlaceholderAPI
    externalPlugin("me.clip:placeholderapi:2.11.6")

    // Command Framework
    shadowed("co.aikar:acf-paper:0.5.1-SNAPSHOT")

    // Config
    shadowed("io.github.townyadvanced.commentedconfiguration:CommentedConfiguration:1.0.1") {
        exclude(group = "org.spigotmc", module = "spigot-api")
    }

    // Utils
    shadowed("io.vavr:vavr:0.10.4")
    shadowed("org.glassfish.hk2:hk2-locator:3.0.3")
    shadowed("org.glassfish.hk2:hk2-inhabitant-generator:3.0.3") {
        exclude(group = "org.apache.maven", module = "maven-core")
    }
    shadowed("com.dumptruckman.minecraft:Logging:1.1.1") {
        exclude(group = "junit", module = "junit")
    }
    shadowed("de.themoep.idconverter:mappings:1.2-SNAPSHOT")
    shadowed("org.bstats:bstats-bukkit:3.1.0") {
        exclude(group = "org.bukkit", module = "bukkit")
    }
    shadowed("net.minidev:json-smart:2.4.9")
    shadowed("org.jetbrains:annotations:22.0.0")
    shadowed("io.papermc:paperlib:1.0.8")

    // Tests
    testImplementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:2.0.21")
    testImplementation("com.github.seeseemelk:MockBukkit-v1.19:3.1.0")
    testImplementation("com.googlecode.json-simple:json-simple:1.1.1") {
        exclude(group = "junit", module = "junit")
    }
    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("com.natpryce:hamkrest:1.8.0.1")
    testImplementation("org.mockito.kotlin:mockito-kotlin:4.1.0")

    // Annotation Processors
    annotationProcessor("org.glassfish.hk2:hk2-metadata-generator:3.0.3")
    testAnnotationProcessor("org.glassfish.hk2:hk2-metadata-generator:3.0.3")
}

java {
    // Plugin is compiled for Java 17 (GraalVM 17-friendly)
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
    withSourcesJar()
    withJavadocJar()
}

val bitlyAccessToken: String = System.getenv("BITLY_ACCESS_TOKEN") ?: "bitly-access-token"

// === Token-filtered source generation (prepareSource) ===
val prepareSource by tasks.registering(Sync::class) {
    inputs.property("bitlyAccessToken", bitlyAccessToken)
    from(sourceSets["main"].java)
    // avoid deprecated buildDir getter
    into(layout.buildDirectory.dir("src"))
    filter<ReplaceTokens>(
        "tokens" to mapOf(
            "bitly-access-token" to bitlyAccessToken,
        ),
    )
}

// Use ONLY the filtered sources for compileJava (fixes duplicate classes)
tasks.named<JavaCompile>("compileJava") {
    dependsOn(prepareSource)
    source = prepareSource.get().outputs.files.asFileTree
    sourceCompatibility = JavaVersion.VERSION_17.toString()
    targetCompatibility = JavaVersion.VERSION_17.toString()
}

tasks.named<JavaCompile>("compileTestJava") {
    sourceCompatibility = JavaVersion.VERSION_17.toString()
    targetCompatibility = JavaVersion.VERSION_17.toString()
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.compilerArgs.add(
        "-Aorg.glassfish.hk2.metadata.location=META-INF/hk2-locator/Multiverse-Core",
    )
}

tasks.withType<Javadoc>().configureEach {
    (options as StandardJavadocDocletOptions).encoding = "UTF-8"
}

tasks.named<KotlinCompile>("compileTestKotlin") {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
        javaParameters.set(true)
    }
}

// We're not using Kotlin in the plugin itself, just tests!
tasks.named<KotlinCompile>("compileKotlin") {
    enabled = false
}

configurations
    .filterNot { it.name.startsWith("test") }
    .forEach {
        it.exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-jdk8")
    }

tasks.named<ProcessResources>("processResources") {
    val props = mapOf("version" to project.version.toString())
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") {
        expand(props)
    }
    // Never up-to-date: tests depend on fresh version expansion
    outputs.upToDateWhen { false }
}

tasks.named<Javadoc>("javadoc") {
    source = sourceSets["main"].allJava
    classpath = configurations["compileClasspath"]
}

// Removed: configurations["api"].isCanBeResolved = true (deprecated in Gradle 8/9)

val jarTask = tasks.named<Jar>("jar")
val shadowJarTask = tasks.named<ShadowJar>("shadowJar")

listOf("apiElements", "runtimeElements").forEach { configurationName ->
    configurations.named(configurationName) {
        outgoing.artifacts.removeIf { artifact ->
            artifact.buildDependencies.getDependencies(null).contains(jarTask.get())
        }
        outgoing.artifact(shadowJarTask)
    }
}

tasks.named<ShadowJar>("shadowJar") {
    relocate("co.aikar", "org.mvplugins.multiverse.external.acf")
    relocate("com.dumptruckman.minecraft.util.Logging", "org.mvplugins.multiverse.core.utils.CoreLogging")
    relocate("com.dumptruckman.minecraft.util.DebugLog", "org.mvplugins.multiverse.core.utils.DebugFileLogger")
    relocate("de.themoep.idconverter", "org.mvplugins.multiverse.external.idconverter")
    relocate("io.github.townyadvanced.commentedconfiguration", "org.mvplugins.multiverse.external.commentedconfiguration")
    relocate("me.main__.util", "org.mvplugins.multiverse.external.serializationconfig")
    relocate("org.bstats", "org.mvplugins.multiverse.external.bstats")
    relocate("com.sun", "org.mvplugins.multiverse.external.sun")
    relocate("net.minidev", "org.mvplugins.multiverse.external.minidev")
    relocate("org.objectweb", "org.mvplugins.multiverse.external.objectweb")
    relocate("io.vavr", "org.mvplugins.multiverse.external.vavr")
    relocate("jakarta", "org.mvplugins.multiverse.external.jakarta")
    relocate("javassist", "org.mvplugins.multiverse.external.javassist")
    relocate("org.aopalliance", "org.mvplugins.multiverse.external.aopalliance")
    relocate("org.glassfish", "org.mvplugins.multiverse.external.glassfish")
    relocate("org.jvnet", "org.mvplugins.multiverse.external.jvnet")
    relocate("org.intellij", "org.mvplugins.multiverse.external.intellij")
    relocate("org.jetbrains", "org.mvplugins.multiverse.external.jetbrains")
    relocate("io.papermc.lib", "org.mvplugins.multiverse.external.paperlib")

    configurations = listOf(project.configurations["shadowed"])

    archiveClassifier.set("")

    dependencies {
        exclude { dependency ->
            dependency.moduleGroup == "org.jetbrains.kotlin"
        }
    }
}

tasks.named("build") {
    dependsOn("shadowJar")
}

tasks.named<Jar>("jar") {
    enabled = false
}

tasks.named<Test>("test") {
    useJUnitPlatform()
    testLogging {
        exceptionFormat = TestExceptionFormat.FULL
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])

            pom.withXml {
                val pomNode = asNode()

                val dependenciesNode = (
                    (pomNode.get("dependencies") as? NodeList)
                        ?.firstOrNull() as? Node
                    ) ?: (pomNode.appendNode("dependencies") as Node)

                val dependencyNodes = dependenciesNode.children().filterIsInstance<Node>().toList()

                // Remove Kotlin dependency
                dependencyNodes
                    .filter { dependencyNode ->
                        val groupIdNodes = dependencyNode.get("groupId") as? NodeList ?: return@filter false
                        val groupId = (groupIdNodes.firstOrNull() as? Node)?.text()
                        groupId == "org.jetbrains.kotlin"
                    }
                    .forEach { dependencyNode ->
                        dependenciesNode.remove(dependencyNode)
                    }

                // Switch runtime deps to provided
                dependencyNodes.forEach { dependencyNode ->
                    val scopeNodes = dependencyNode.get("scope") as? NodeList ?: return@forEach
                    val scopeNode = scopeNodes.firstOrNull() as? Node ?: return@forEach
                    if (scopeNode.text() == "runtime") {
                        scopeNode.setValue("provided")
                    }
                }

                // Add server API (Purpur) to pom as provided
                configurations["serverApi"].allDependencies.forEach { dependency ->
                    val depNode = dependenciesNode.appendNode("dependency")
                    depNode.appendNode("groupId", dependency.group)
                    depNode.appendNode("artifactId", dependency.name)
                    depNode.appendNode("version", dependency.version)
                    depNode.appendNode("scope", "provided")
                }
            }
        }
    }
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/Multiverse/Multiverse-Core")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
        maven {
            name = "multiverse"
            val releasesRepoUrl = uri("https://repo.dumptruckman.com/multiverse-releases")
            val snapshotsRepoUrl = uri("https://repo.dumptruckman.com/multiverse-snapshots")
            url = if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl
            credentials(PasswordCredentials::class)
        }

        maven {
            // todo: remove before mv5 release
            name = "multiverseBeta"
            url = uri("https://repo.c0ding.party/multiverse-beta")
            credentials(PasswordCredentials::class)
        }
    }
}

