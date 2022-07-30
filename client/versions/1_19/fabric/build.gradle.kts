import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

val buildVersion: String by rootProject

// todo: придумать как использовать из gradle.properties. Сейчас не получается использовать, потому что это не rootProject
val minecraftVersion = "1.19"
val fabricLoaderVersion = "0.14.8"
val fabricVersion = "0.58.0+1.19"

configurations {
    create("shadowCommon")
}

architectury {
    platformSetupLoomIde()
    fabric()
}

dependencies {
    modApi("net.fabricmc:fabric-loader:$fabricLoaderVersion")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${fabricVersion}")

    compileOnly(project(":client:versions:1_19:common", "dev")) {
        isTransitive = false
    }
    project.configurations.getByName("developmentFabric")(project(":client:versions:1_19:common", "dev")) {
        isTransitive = false
    }
    "shadowCommon"(project(":client:versions:1_19:common", "transformProductionFabric")) {
        isTransitive = false
    }
}

tasks {
    jar {
        archiveClassifier.set("dev")
    }

    processResources {
        filesMatching("fabric.mod.json") {
            expand(
                mutableMapOf(
                    "version" to buildVersion,
                    "loader_version" to fabricLoaderVersion,
                    "fabric_version" to fabricVersion
                )
            )
        }
    }

    shadowJar {
        configurations = listOf(project.configurations.getByName("shadowCommon"))
        archiveClassifier.set("dev-shadow")

        dependencies {
            exclude(dependency("net.java.dev.jna:jna"))
            exclude(dependency("org.slf4j:slf4j-api"))
        }
    }

    remapJar {
        println(architectury.minecraft)
        dependsOn(getByName<ShadowJar>("shadowJar"))
        inputFile.set(shadowJar.get().archiveFile)
        archiveBaseName.set("plasmovoice-fabric-${minecraftVersion}")
    }

    build {
        doLast {
            shadowJar.get().archiveFile.get().asFile.delete()
        }
    }
}