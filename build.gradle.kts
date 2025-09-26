import org.apache.tools.ant.filters.ReplaceTokens
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.jvm.toolchain.JvmVendorSpec

plugins {
    id("java")
    id("java-library")
    id("eclipse")
    id("com.gradleup.shadow") version "8.3.6"
    kotlin("jvm") version "2.1.21"
}

group = "org.mvplugins.multiverse.core"
version = System.getenv("GITHUB_VERSION") ?: "4.3.14"
description = "Multiverse-Core"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
        vendor.set(JvmVendorSpec.GRAAL_VM)
    }
    withSourcesJar()
    withJavadocJar()
}

kotlin { jvmToolchain(17) }

repositories {
    mavenLocal()
    mavenCentral()
    maven { url = uri("https://hub.spigotmc.org/nexus/content/repositories/snapshots/") }
    maven { url = uri("https://repo.onarandombox.com/content/groups/public") }
    maven { url = uri("https://hub.spigotmc.org/nexus/content/groups/public/") }
    maven { url = uri("https://jitpack.io") }
    maven { url = uri("https://repo.minebench.de/") }
    maven { url = uri("https://repo.maven.apache.org/maven2/") }
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
}

val serverApi by configurations.creating
val externalPlugin by configurations.creating
val shadowed by configurations.creating

configurations.named("compileOnly") { extendsFrom(serverApi) }
configurations.named("runtimeClasspath") { extendsFrom(serverApi) }
configurations.named("implementation") { extendsFrom(externalPlugin) }
configurations.named("compileClasspath") { extendsFrom(shadowed) }
configurations.named("testCompileClasspath") { extendsFrom(shadowed) }
configurations.named("testRuntimeClasspath") { extendsFrom(shadowed) }

sourceSets {
    create("oldTest") {
        java.srcDir("src/old-test/java")
        resources.srcDir("src/old-test/resources")
        compileClasspath += sourceSets["main"].output
        runtimeClasspath += sourceSets["main"].output
    }
}

configurations.named("oldTestCompileClasspath") { extendsFrom(shadowed) }
configurations.named("oldTestRuntimeClasspath") { extendsFrom(shadowed) }
configurations.named("oldTestImplementation") { extendsFrom(configurations["implementation"]) }
configurations.named("oldTestRuntimeOnly") { extendsFrom(configurations["runtimeOnly"]) }

dependencies {
    serverApi("io.papermc.paper:paper-api:1.19.4-R0.1-SNAPSHOT")

    externalPlugin("com.github.MilkBowl:VaultAPI:1.7.1") {
        exclude(group = "org.bukkit", module = "bukkit")
    }
    externalPlugin("me.clip:placeholderapi:2.11.6")

    shadowed("co.aikar:acf-paper:0.5.1-SNAPSHOT")
    shadowed("io.github.townyadvanced.commentedconfiguration:CommentedConfiguration:1.0.1") {
        exclude(group = "org.spigotmc", module = "spigot-api")
    }
    shadowed("io.vavr:vavr:0.10.4")
    shadowed("org.glassfish.hk2:hk2-locator:3.0.3")
    shadowed("org.glassfish.hk2:hk2-inhabitant-generator:3.0.3") {
        exclude(group = "org.apache.maven", module = "maven-core")
    }
    shadowed("com.dumptruckman.minecraft:Logging:1.1.1") {
        exclude(group = "junit", module = "junit")
    }
    shadowed("de.themoep.idconverter:mappings:1.2-SNAPSHOT")
    shadowed("org.bstats:bstats-bukkit:2.2.1")
    shadowed("net.minidev:json-smart:2.4.9")
    shadowed("org.jetbrains:annotations:22.0.0")
    shadowed("io.papermc:paperlib:1.0.8")

    testImplementation(kotlin("test"))
    testImplementation("com.github.seeseemelk:MockBukkit-v1.20:3.20.2")
    testImplementation("com.googlecode.json-simple:json-simple:1.1.1") {
        exclude(group = "junit", module = "junit")
    }
    testImplementation("com.natpryce:hamkrest:1.8.0.1")
    testImplementation("org.mockito.kotlin:mockito-kotlin:4.1.0")

    add("oldTestImplementation", "org.spigotmc:spigot-api:1.20.1-R0.1-SNAPSHOT")
    add("oldTestImplementation", "com.googlecode.json-simple:json-simple:1.1.1") {
        exclude(group = "junit", module = "junit")
    }
    add("oldTestImplementation", "junit:junit:4.13.1")
    add("oldTestImplementation", "org.mockito:mockito-core:3.11.2")
    add("oldTestImplementation", "commons-io:commons-io:2.7")

    annotationProcessor("org.glassfish.hk2:hk2-metadata-generator:3.0.3")
    testAnnotationProcessor("org.glassfish.hk2:hk2-metadata-generator:3.0.3")
}

configurations.configureEach {
    if (!name.startsWith("test")) {
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-jdk8")
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.compilerArgs.add("-Aorg.glassfish.hk2.metadata.location=META-INF/hk2-locator/Multiverse-Core")
}

tasks.withType<Javadoc>().configureEach {
    options.encoding = "UTF-8"
}

tasks.named<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>("compileKotlin") {
    enabled = false
}

val bitlyAccessToken = System.getenv("BITLY_ACCESS_TOKEN") ?: "bitly-access-token"

val prepareSource by tasks.registering(Sync::class) {
    inputs.property("bitlyAccessToken", bitlyAccessToken)
    from(sourceSets["main"].java.srcDirs)
    into(layout.buildDirectory.dir("src"))
    filter<ReplaceTokens>(mapOf("tokens" to mapOf("bitly-access-token" to bitlyAccessToken)))
}

tasks.named<JavaCompile>("compileJava") {
    source = prepareSource.get().outputs.files.asFileTree
    dependsOn(prepareSource)
}

tasks.named<ProcessResources>("processResources") {
    val props = mapOf("version" to version.toString())
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") { expand(props) }
    outputs.upToDateWhen { false }
}

tasks.named<Javadoc>("javadoc") {
    setSource(sourceSets["main"].allJava)
    classpath = files(configurations["compileClasspath"])
}

configurations["api"].isCanBeResolved = true

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
    configurations = listOf(project.configurations.getByName("shadowed"))
    archiveClassifier.set("")
    archiveBaseName.set("Multiverse-Core")
    archiveVersion.set(project.version.toString())
}

tasks.build { dependsOn(tasks.named("shadowJar")) }
tasks.jar { enabled = false }

tasks.test {
    useJUnitPlatform()
    testLogging { exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL }
}

