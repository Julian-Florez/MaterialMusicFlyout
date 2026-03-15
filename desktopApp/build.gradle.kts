import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.gradle.api.tasks.Exec

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.kotlin.compose)
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

tasks.matching { it.name == "run" }.configureEach {
    dependsOn(publishWinRtHelper)
}

compose.desktop {
    application {
        mainClass = "com.myg.materialmusicflyout.desktop.DesktopMainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Exe)
            packageName = "MaterialMusicFlyout"
            packageVersion = "1.0.0"
        }
    }
}
