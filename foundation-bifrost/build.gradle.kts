plugins {
  id("gay.pizza.foundation.concrete-plugin")
}

dependencies {
  implementation(libs.discord.jda) {
    exclude(module = "opus-java")
  }

  implementation(project(":common-plugin"))
  compileOnly(project(":foundation-shared"))
}

concreteItem {
  dependency(project(":foundation-core"))
}
