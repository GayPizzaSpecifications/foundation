@file:Suppress("UnstableApiUsage")
rootProject.name = "foundation"

pluginManagement {
  repositories {
    mavenCentral()
    gradlePluginPortal()
    maven(url = "https://gitlab.com/api/v4/projects/42873094/packages/maven")
  }
}

var localConcretePathString: String? = System.getenv("FOUNDATION_CONCRETE_PATH")

if (localConcretePathString == null) {
  val concreteLocalPathFile = rootProject.projectDir.resolve(".concrete-local-path")
  if (concreteLocalPathFile.exists()) {
    localConcretePathString = concreteLocalPathFile.readText().trim()
  }
}

if (localConcretePathString != null) {
  println("[Using Local Concrete] $localConcretePathString")

  includeBuild(localConcretePathString!!)
}

include(
  ":common-all",
  ":common-plugin",
  ":common-heimdall",
  ":foundation-core",
  ":foundation-shared",
  ":foundation-bifrost",
  ":foundation-chaos",
  ":foundation-heimdall",
  ":foundation-tailscale",
  ":tool-gjallarhorn",
)

dependencyResolutionManagement {
  versionCatalogs {
    create("libs") {
      version("versions-plugin", "0.45.0")
      version("concrete", "0.16.0")

      plugin("versions", "com.github.ben-manes.versions").versionRef("versions-plugin")

      val concretePlugins = listOf(
        plugin("concrete-root", "gay.pizza.foundation.concrete-root"),
        plugin("concrete-base", "gay.pizza.foundation.concrete-base"),
        plugin("concrete-library", "gay.pizza.foundation.concrete-library"),
        plugin("concrete-plugin", "gay.pizza.foundation.concrete-plugin")
      )

      for (concrete in concretePlugins) {
        if (localConcretePathString == null) {
          concrete.versionRef("concrete")
        } else {
          concrete.version("DEV")
        }
      }

      version("clikt", "3.5.1")
      version("xodus", "2.0.1")
      version("quartz", "2.3.2")
      version("guava", "31.1-jre")
      version("koin", "3.3.2")
      version("aws-sdk-s3", "2.19.31")
      version("slf4j-simple", "2.0.6")
      version("discord-jda", "5.0.0-alpha.2")
      version("kaml", "0.51.0")
      version("kotlin-serialization-json", "1.3.1")
      version("postgresql", "42.5.3")
      version("exposed", "0.41.1")
      version("hikaricp", "5.0.1")
      version("libtailscale", "0.1.6-SNAPSHOT")

      library("clikt", "com.github.ajalt.clikt", "clikt").versionRef("clikt")
      library("xodus-core", "org.jetbrains.xodus", "xodus-openAPI").versionRef("xodus")
      library("xodus-entity-store", "org.jetbrains.xodus", "xodus-entity-store").versionRef("xodus")
      library("quartz-core", "org.quartz-scheduler", "quartz").versionRef("quartz")
      library("guava", "com.google.guava", "guava").versionRef("guava")
      library("koin-core", "io.insert-koin", "koin-core").versionRef("koin")
      library("koin-test", "io.insert-koin", "koin-test").versionRef("koin")
      library("aws-sdk-s3", "software.amazon.awssdk", "s3").versionRef("aws-sdk-s3")
      library("slf4j-simple", "org.slf4j", "slf4j-simple").versionRef("slf4j-simple")
      library("discord-jda","net.dv8tion", "JDA").versionRef("discord-jda")
      library("kotlin-serialization-json", "org.jetbrains.kotlinx", "kotlinx-serialization-json").versionRef("kotlin-serialization-json")
      library("kotlin-serialization-yaml", "com.charleskorn.kaml", "kaml").versionRef("kaml")

      library("postgresql", "org.postgresql", "postgresql").versionRef("postgresql")
      library("exposed-jdbc", "org.jetbrains.exposed", "exposed-jdbc").versionRef("exposed")
      library("exposed-java-time", "org.jetbrains.exposed", "exposed-java-time").versionRef("exposed")
      library("hikaricp", "com.zaxxer", "HikariCP").versionRef("hikaricp")
      library("tailscale", "gay.pizza.tailscale", "tailscale").versionRef("libtailscale")
      library("tailscale-channel", "gay.pizza.tailscale", "tailscale-channel").versionRef("libtailscale")
    }
  }
}
