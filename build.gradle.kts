plugins {
  kotlin("jvm") version "1.5.0" apply false
  kotlin("plugin.spring") version "1.5.0" apply false
  id("org.springframework.boot") version "2.5.0" apply false
  id("io.spring.dependency-management") version "1.0.11.RELEASE" apply false
}

allprojects {
  repositories {
    mavenCentral()
    maven("https://packages.confluent.io/maven")
    maven("https://repository.mulesoft.org/nexus/content/repositories/public/")
  }
}
