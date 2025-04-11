import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

@Suppress("DSL_SCOPE_VIOLATION")
plugins {
  java

  alias(libs.plugins.concrete.root)
  alias(libs.plugins.concrete.base) apply false
  alias(libs.plugins.concrete.library) apply false
  alias(libs.plugins.concrete.plugin) apply false

  alias(libs.plugins.versions)
}

allprojects {
  repositories {
    mavenCentral()

    maven {
      name = "sonatype"
      url = uri("https://oss.sonatype.org/content/groups/public/")
    }
  }
}

version = "0.2"

subprojects {
  group = "gay.pizza.foundation"

  tasks.withType<KotlinCompile> {
    compilerOptions {
      freeCompilerArgs.add("-opt-in=kotlinx.serialization.ExperimentalSerializationApi")
    }
  }
}

val paperServerVersion: String = project.properties["paperServerVersion"]?.toString() ?: "1.21"

concreteRoot {
  minecraftServerPath.set("server")
  paperServerVersionGroup.set(paperServerVersion)
  paperApiVersion.set("1.21.4-R0.1-SNAPSHOT")
  acceptServerEula.set(true)
}
