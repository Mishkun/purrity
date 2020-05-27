buildscript {
    repositories {
        maven(url = "https://plugins.gradle.org/m2/")
        maven(url = "http://dl.bintray.com/jetbrains/intellij-plugin-service")
    }
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.3.72")
        classpath("gradle.plugin.org.jetbrains.intellij.plugins:gradle-intellij-plugin:0.4.21")
    }
}

plugins {
    kotlin("jvm") version ("1.3.72")
    id("org.jetbrains.intellij") version "0.4.21"
}


repositories {
    mavenCentral()
}

group = "io.github.mishkun"
version = "1.0-SNAPSHOT"

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.3.72")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}


intellij {
    version = "2020.1"
    setPlugins("Kotlin", "java")
}

tasks.getByName<org.jetbrains.intellij.tasks.PatchPluginXmlTask>("patchPluginXml") {
    changeNotes("""
      Add change notes here.<br>
      <em>most HTML tags may be used</em>""")
}
