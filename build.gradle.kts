import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `maven-publish`

    // extremely questionable hack to get the list of all pluginAliases
    fun <T : java.lang.reflect.AccessibleObject> T.jailbreak() = apply { isAccessible = true }
    val createPlugin =
        libs.plugins.javaClass.superclass
            .getDeclaredMethod("createPlugin", String::class.java)
            .jailbreak()
    val configField =
        libs.javaClass.superclass.superclass
            .getDeclaredField("config")
            .jailbreak()
    val actualLibs = configField.get(libs) as org.gradle.api.internal.catalog.DefaultVersionCatalog
    @Suppress("UNCHECKED_CAST")
    actualLibs.pluginAliases.forEach { alias(createPlugin(libs.plugins, it) as Provider<PluginDependency>) }
}

/**
 * Retrieves a value from the `gradle.properties` map.
 */
fun prop(key: String) = extra.properties[key] as String

group = "ai.arcblroth"
version = "4.0-SNAPSHOT"
val mainClassName = "ai.arcblroth.somnus3.SomnusMain"

repositories {
    mavenCentral()
    maven("https://oss.sonatype.org/content/repositories/snapshots")
    maven("https://m2.dv8tion.net/releases")
    maven("https://jitpack.io")
}

dependencies {
    compileOnly(kotlin("stdlib-jdk8"))

    val versionCatalog = project.extensions.getByType<VersionCatalogsExtension>().named("libs")
    versionCatalog.libraryAliases.forEach { implementation(versionCatalog.findLibrary(it).get()) }
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_21
        freeCompilerArgs = listOf("-opt-in=kotlin.concurrent.atomics.ExperimentalAtomicApi,kotlin.time.ExperimentalTime")
    }
}

tasks {
    shadowJar {
        archiveBaseName.set("somnus")
        archiveClassifier.set("")
        manifest {
            attributes(
                mapOf(
                    "Main-Class" to mainClassName,
                    "Implementation-Version" to archiveVersion,
                ),
            )
        }
    }

    jar.get().enabled = false
    assemble.get().dependsOn(shadowJar.get())
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifact(tasks.shadowJar) {
                artifactId = "somnus"
            }
        }
    }
}
