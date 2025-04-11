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
      version("versions-plugin", "0.52.0")
      version("concrete", "0.17.0")

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

      version("clikt", "5.0.3")
      version("xodus", "2.0.1")
      version("quartz", "2.5.0")
      version("guava", "33.4.7-jre")
      version("koin", "4.0.4")
      version("aws-sdk-s3", "2.31.19")
      version("slf4j-simple", "2.0.17")
      version("discord-jda", "5.3.2")
      version("kaml", "0.76.0")
      version("kotlin-serialization-json", "1.8.1")
      version("postgresql", "42.7.5")
      version("exposed", "0.61.0")
      version("hikaricp", "6.3.0")
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
