plugins {
    id("org.openjfx.javafxplugin") version "0.1.0" apply false
}

allprojects {
    group = "org.takoyaki.taskboardmanager"
    version = "1.8.0-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}

subprojects {
    plugins.withType<JavaPlugin> {
        extensions.configure<JavaPluginExtension> {
            toolchain {
                languageVersion.set(JavaLanguageVersion.of(21))
            }
        }

        tasks.withType<JavaCompile>().configureEach {
            options.encoding = "UTF-8"
        }
    }
}
