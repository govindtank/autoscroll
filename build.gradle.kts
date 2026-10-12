// Top-level build file for YouTube Auto Scroll Android App
plugins {
    id("com.android.application") version "8.2.0" apply false
    id("org.jetbrains.kotlin.android") version "1.9.20" apply false
    id("com.google.devtools.ksp") version "1.9.20-1.0.14" apply false
}

task<Copy>("copyAppBuildConfig") {
    from(layout.projectDirectory.dir("app"))
    into(file("$buildDir/generated/app"))
    include("**/*.kts")
}
