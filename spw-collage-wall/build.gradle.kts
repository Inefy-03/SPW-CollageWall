import com.xuncorp.spw.workshop.gradle.PluginPermission
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("java-library")
    id("org.jetbrains.kotlin.jvm") version "2.4.20"
    id("org.jetbrains.kotlin.kapt") version "2.4.20"
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.20"
    id("com.xuncorp.spw.workshop") version "0.1.0-dev23"
}

group = "dev.spw"
version = "1.0.0"

java {
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}

kotlin {
    jvmToolchain(25)
    compilerOptions {
        jvmTarget = JvmTarget.JVM_25
        freeCompilerArgs.add("-Xjvm-default=all")
    }
}

dependencies {
    // Host-provided dependencies must not be packaged into the mod.
    compileOnly(kotlin("stdlib"))
    compileOnly("com.github.Moriafly.spw-workshop-api:spw-workshop-api:0.1.0-dev23")
    kapt("com.github.Moriafly.spw-workshop-api:spw-workshop-api:0.1.0-dev23")
    compileOnly("org.pf4j:pf4j:3.12.0")
    kapt("org.pf4j:pf4j:3.12.0")

    compileOnly("org.jetbrains.compose.runtime:runtime-desktop:1.12.1")
    compileOnly("org.jetbrains.compose.foundation:foundation-desktop:1.12.1")
    compileOnly("org.jetbrains.compose.ui:ui-desktop:1.12.1")

    testImplementation(kotlin("stdlib"))
    testImplementation("com.github.Moriafly.spw-workshop-api:spw-workshop-api:0.1.0-dev23")
    testImplementation("org.jetbrains.compose.runtime:runtime-desktop:1.12.1")
    testImplementation("org.junit.jupiter:junit-jupiter:5.12.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

spmod {
    PluginClass = "dev.spw.collage.MainPlugin"
    PluginId = "dev.spw.collage"
    PluginName = "SPW Collage Wall"
    PluginDescription = "混排比例的唱片封面墙：可自由平移到任意歌曲，当前歌曲高亮、点击展开并重排补位"
    PluginVersion = "1.0.0"
    PluginProvider = "Inefy"
    PluginHasConfig = true
    PluginPermissions = listOf(
        PluginPermission.LIBRARY_READ,
        PluginPermission.CLASS_TRANSFORM
    )
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    systemProperty("file.encoding", "UTF-8")
    testLogging {
        events("passed", "failed", "skipped")
    }
}
