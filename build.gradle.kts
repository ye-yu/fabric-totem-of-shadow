plugins {
  kotlin("jvm") version Jetbrains.Kotlin.version
  kotlin("plugin.serialization") version Jetbrains.Kotlin.version
  id("fabric-loom") version Fabric.Loom.version
  id("com.matthewprenger.cursegradle") version CurseGradle.version
  `maven-publish`
}

group = Info.group
version = Info.version

repositories {
  maven(url = "http://maven.fabricmc.net") { name = "Fabric" }
  maven(url = "https://libraries.minecraft.net/") { name = "Mojang" }
  maven(url = "https://kotlin.bintray.com/kotlinx/") { name = "Kotlinx" }
  mavenCentral()
  jcenter()
  flatDir {
    dirs("libs")
  }
}

minecraft {
}

dependencies {
  minecraft("com.mojang", "minecraft", Minecraft.version)
  mappings("net.fabricmc", "yarn", Fabric.YarnMappings.version, classifier = "v2")

  modImplementation("net.fabricmc", "fabric-loader", Fabric.Loader.version)
  modImplementation("net.fabricmc", "fabric-language-kotlin", Fabric.Kotlin.version)
  modImplementation("net.fabricmc.fabric-api", "fabric-api", Fabric.API.version)

  modImplementation(Mods.modmenu)

  includeApi(Jetbrains.Kotlin.stdLib)
  includeApi(Jetbrains.Kotlin.reflect)
  includeApi(Jetbrains.Kotlinx.coroutines)
  includeApi(Jetbrains.Kotlinx.serialization)
  
  
  implementation("io.github.ye-yu:jeasing:0.0.1-alpha.3")
  include("io.github.ye-yu:jeasing:0.0.1-alpha.3")

  modImplementation("me.lambdaurora:lambdynamiclights:1.3.2+1.16.4")
  modRuntime("com.github.lambdaurora:spruceui:1.6.4")
  runtime("org.aperlambda.lambdacommon:lambdajcommon:1.8.1")
  runtime("com.electronwill.night-config:core:3.6.3")
  runtime("com.electronwill.night-config:toml:3.6.3")
}

tasks {
  val sourcesJar by creating(Jar::class) {
    archiveClassifier.set("sources")

    from(sourceSets["main"].allSource)

    dependsOn(JavaPlugin.CLASSES_TASK_NAME)
  }

  val javadocJar by creating(Jar::class) {
    archiveClassifier.set("javadoc")

    from(project.tasks["javadoc"])

    dependsOn(JavaPlugin.JAVADOC_TASK_NAME)
  }

  compileJava {
    targetCompatibility = "11"
    sourceCompatibility = "11"
  }

  compileKotlin {
    kotlinOptions {
      jvmTarget = "11"
      freeCompilerArgs = listOf(
        "-Xopt-in=kotlin.RequiresOptIn",
        "-Xopt-in=kotlin.ExperimentalStdlibApi"
      )
    }
  }

  processResources {
    filesMatching("fabric.mod.json") {
      expand(
        "modid" to Info.modid,
        "name" to Info.name,
        "version" to Info.version,
        "description" to Info.description,
        "kotlinVersion" to Jetbrains.Kotlin.version,
        "fabricApiVersion" to Fabric.API.version
      )
    }
  }
}

publishing {
  publications {
    create<MavenPublication>("maven") {
      artifacts {
        artifact(tasks["sourcesJar"]) {
          builtBy(tasks["remapSourcesJar"])
        }

        artifact(tasks["javadocJar"])
        artifact(tasks["remapJar"])
      }
    }

    repositories {
      mavenLocal()
    }
  }
}

fun DependencyHandlerScope.includeApi(notation: String) {
  include(notation)
  modApi(notation)
}

