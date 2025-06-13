import org.apache.tools.ant.filters.ReplaceTokens
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.compile.JavaCompile
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.jvm.tasks.Jar

plugins {
    id("eclipse")
    id("java-library")
    id("maven-publish")
    id("com.gradleup.shadow") version "8.3.6"
}

version = System.getenv("GITHUB_VERSION") ?: "1.19.4"
group = "com.onarandombox.multiversecore"
description = "Multiverse-Core"

java {
    sourceCompatibility = JavaVersion.VERSION_11
    withSourcesJar()
    withJavadocJar()
}

repositories {
    mavenLocal()
    mavenCentral()
    maven { url = uri("https://hub.spigotmc.org/nexus/content/repositories/snapshots/") }
    maven { url = uri("https://repo.onarandombox.com/content/groups/public") }
    maven { url = uri("https://hub.spigotmc.org/nexus/content/groups/public/") }
    maven { url = uri("https://jitpack.io") }
    maven { url = uri("https://repo.minebench.de/") }
    maven { url = uri("https://repo.maven.apache.org/maven2/") }
}

dependencies {
    implementation("org.bukkit:bukkit:1.13.2-R0.1-SNAPSHOT")
    implementation("com.github.MilkBowl:VaultAPI:1.7") { exclude(group = "org.bukkit", module = "bukkit") }
    compileOnly("me.main__.util:SerializationConfig:1.7") { exclude(group = "org.bukkit", module = "bukkit") }
    compileOnly("com.pneumaticraft.commandhandler:CommandHandler:11") {
        exclude(group = "org.bukkit", module = "bukkit")
        exclude(group = "junit", module = "junit")
    }
    compileOnly("com.dumptruckman.minecraft:buscript:2.0-SNAPSHOT")
    compileOnly("org.bstats:bstats-bukkit:2.2.1")
    compileOnly("com.dumptruckman.minecraft:Logging:1.1.1") { exclude(group = "junit", module = "junit") }
    compileOnly("de.themoep.idconverter:mappings:1.2-SNAPSHOT")
    compileOnly("org.jetbrains:annotations:16.0.2")
    testImplementation("org.spigotmc:spigot-api:1.19.3-R0.1-SNAPSHOT")
    testImplementation("com.googlecode.json-simple:json-simple:1.1.1") { exclude(group = "junit", module = "junit") }
    testImplementation("junit:junit:4.12")
    testImplementation("org.mockito:mockito-core:3.11.2")
    testImplementation("commons-io:commons-io:2.4")
}

tasks.withType<JavaCompile> { options.encoding = "UTF-8" }
tasks.withType<Javadoc> { options.encoding = "UTF-8" }

configurations {
    val compileOnly by getting
    val testCompileOnly by getting
    val testRuntimeOnly by getting
    val apiElements by getting
    val runtimeElements by getting

    create("shadowed") {
        extendsFrom(compileOnly)
        isCanBeResolved = true
    }

    testCompileOnly.extendsFrom(compileOnly)
    testRuntimeOnly.extendsFrom(testCompileOnly)

    listOf(apiElements, runtimeElements).forEach { cfg ->
        cfg.outgoing.artifacts.removeIf { it.buildDependencies.getDependencies(null).contains(tasks.named("jar").get()) }
        cfg.outgoing.artifact(tasks.named("shadowJar").get())
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            pom.withXml {
                val implNames = configurations["implementation"].allDependencies.map { it.name }.toSet()
                val deps = (asNode().get("dependencies") as groovy.util.Node)
                    .children()
                    .filterIsInstance<groovy.util.Node>()
                deps.filter { dep ->
                    (dep.get("scope") as groovy.util.Node).text() == "runtime" &&
                    implNames.contains((dep.get("artifactId") as groovy.util.Node).text())
                }.forEach { dep ->
                    (dep.get("scope") as groovy.util.Node).setValue("provided")
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
    }
}

val bitlyAccessToken = System.getenv("BITLY_ACCESS_TOKEN") ?: "bitly-access-token"
val originalJavaDir = layout.projectDirectory.dir("src/main/java")
val generatedDir = layout.buildDirectory.dir("src")

val prepareSource by tasks.registering(Sync::class) {
    inputs.property("bitlyAccessToken", bitlyAccessToken)
    from(originalJavaDir)
    into(generatedDir)
    filter<ReplaceTokens>("tokens" to mapOf("bitly-access-token" to bitlyAccessToken))
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.named<Jar>("sourcesJar") {
    dependsOn(prepareSource)
    from(sourceSets["main"].allSource)
}

val sourceSets = extensions.getByName("sourceSets") as SourceSetContainer
sourceSets["main"].java.setSrcDirs(listOf(generatedDir))

tasks.named<JavaCompile>("compileJava") { dependsOn(prepareSource) }

tasks.named<org.gradle.api.tasks.Copy>("processResources") {
    val props = mapOf("version" to "$version")
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") { expand(props) }
    outputs.upToDateWhen { false }
}

tasks.named<ShadowJar>("shadowJar") {
    relocate("me.main__.util", "com.onarandombox.serializationconfig")
    relocate("com.pneumaticraft.commandhandler", "com.onarandombox.commandhandler")
    relocate("buscript", "com.onarandombox.buscript")
    relocate("org.bstats", "com.onarandombox.bstats")
    relocate("com.dumptruckman.minecraft.util.Logging", "com.onarandombox.MultiverseCore.utils.CoreLogging")
    relocate("com.dumptruckman.minecraft.util.DebugLog", "com.onarandombox.MultiverseCore.utils.DebugFileLogger")
    relocate("org.codehaus.jettison", "com.onarandombox.jettison")
    relocate("de.themoep.idconverter", "com.onarandombox.idconverter")
    configurations = listOf(project.configurations["shadowed"])
    archiveFileName.set("${archiveBaseName.get()}-$version.${archiveExtension.get()}")
    archiveClassifier.set("")
}

tasks.named("build") { dependsOn("shadowJar") }
tasks.named<Jar>("jar") { enabled = false }

