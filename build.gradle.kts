plugins {
    id("java")
    id("com.github.johnrengelman.shadow").version("7.1.2")
    id("io.papermc.paperweight.userdev").version("1.3.5")
    id("net.minecrell.plugin-yml.bukkit").version("0.5.1")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

repositories {
    mavenCentral()
}

dependencies {
    paperDevBundle("1.18.2-R0.1-SNAPSHOT")
    compileOnly(files("R:\\Evil Kingdom\\sources\\Basics (Server)\\build\\libs\\Basics (Server)-unspecified.jar"))
    compileOnly(files("R:\\Evil Kingdom\\sources\\Commons (Server)\\build\\libs\\Commons (Server)-unspecified.jar"))
}

tasks {
    assemble {
        dependsOn(reobfJar)
    }
    compileJava {
        options.release.set(17)
        options.encoding = "UTF-8"
    }
    javadoc {
        options.encoding = "UTF-8"
    }
    processResources {
        filteringCharset = "UTF-8"
    }

}

bukkit {
    name = "Prison"
    author = "kodirati"
    website = "kodirati.com"
    description = "Used as the core for the prison server of Evil Kingdom."
    main = "net.evilkingdom.prison.Prison"
    version = "commit-" + gitCommit()
    apiVersion = "1.18"
    depend = listOf("Commons", "Basics")
}

fun gitCommit(): String {
    return try {
        val byteOut = org.apache.commons.io.output.ByteArrayOutputStream()
        project.exec {
            commandLine = "git rev-parse --short HEAD".split(" ")
            standardOutput = byteOut
        }
        String(byteOut.toByteArray()).trim().also {
            if (it == "HEAD")
                logger.warn("Unable to determine current commit: Project is checked out with detached head!")
        }
    } catch (e: Exception) {
        logger.warn("Unable to determine current commit: ${e.message}")
        "Unknown Commit"
    }
}