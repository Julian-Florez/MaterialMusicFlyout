import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.Copy
import org.gradle.api.distribution.DistributionContainer

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.kotlin.compose)
    id("distribution")
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":shared"))
    implementation(compose.desktop.currentOs)
}

val publishWinRtHelper by tasks.registering(Exec::class) {
    group = "build"
    description = "Publica el helper WinRT para lectura/control multimedia en Windows"
    workingDir = projectDir
    commandLine(
        "dotnet",
        "publish",
        "winrt-helper/MediaBridgeHelper.csproj",
        "-c",
        "Release",
        "-r",
        "win-x64",
        "--self-contained",
        "false"
    )
}

val winRtHelperPublishDir = layout.projectDirectory.dir(
    "winrt-helper/bin/Release/net8.0-windows10.0.19041.0/win-x64/publish"
)

val stageWinRtHelperForDist by tasks.registering(Copy::class) {
    group = "build"
    description = "Copia el helper WinRT publicado dentro de la distribución de la app"
    dependsOn(publishWinRtHelper)
    from(winRtHelperPublishDir)
    into(layout.buildDirectory.dir("staged-winrt-helper"))
}

extensions.configure(DistributionContainer::class.java) {
    named("main") {
        contents {
            from(stageWinRtHelperForDist) {
                into("winrt-helper")
            }
        }
    }
}

tasks.matching { it.name == "run" }.configureEach {
    dependsOn(publishWinRtHelper)
}

tasks.matching {
    it.name in setOf(
        "createDistributable",
        "packageDistributionForCurrentOS",
        "packageExe"
    )
}.configureEach {
    dependsOn(stageWinRtHelperForDist)
}

compose.desktop {
    application {
        mainClass = "com.myg.materialmusicflyout.desktop.DesktopMainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Exe)
            packageName = "MaterialMusicFlyout"
            packageVersion = "1.0.0"
            vendor = "Julian Florez"

            windows {
                iconFile.set(project.file("src/main/resources/app-icon.ico"))
            }
        }
    }
}
