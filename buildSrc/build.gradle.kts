plugins {
  `kotlin-dsl`
  kotlin("plugin.serialization") version "1.5.31"
}

repositories {
  gradlePluginPortal()
}

dependencies {
  implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.6.10")
  implementation("org.jetbrains.kotlin:kotlin-serialization:1.6.10")
  implementation("gradle.plugin.com.github.johnrengelman:shadow:7.1.1")
  implementation("com.google.code.gson:gson:2.8.9")
}

java.sourceCompatibility = JavaVersion.VERSION_1_8
java.targetCompatibility = JavaVersion.VERSION_1_8