object Jetbrains {
    object Kotlin {
        const val version = "1.4.0"
        const val stdLib = "org.jetbrains.kotlin:kotlin-stdlib-jdk8:$version"
        const val reflect = "org.jetbrains.kotlin:kotlin-reflect:$version"

        private const val annotationsVersion = "20.0.0"
        const val annotations = "org.jetbrains:annotations:$annotationsVersion"
    }

    object Kotlinx {
        private const val coroutineVersion = "1.3.9"
        const val coroutinesCore = "org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:$coroutineVersion"
        const val coroutines = "org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:$coroutineVersion"
        const val serialization = "org.jetbrains.kotlinx:kotlinx-serialization-runtime:0.20.0"
    }
}

object Mods {
    const val modmenu = "io.github.prospector:modmenu:1.14.5+build.+"
}

object Fabric {

    object Kotlin {
        const val version = "${Jetbrains.Kotlin.version}+build.+"
    }

    object Loader {
        const val version = "0.10.6+build.214" 
    }

    object API {
        const val version = "0.25.0+build.415-1.16"
    }

    object Loom {
        const val version = "0.5.34"
    }

    object YarnMappings {
        const val version = "${Minecraft.version}+build.47"
    }
}

object Minecraft {
    const val version = "1.16.3"
}

object CurseGradle {
    const val version = "1.4.0"
}
