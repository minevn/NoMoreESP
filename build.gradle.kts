plugins {
    java
    id("io.papermc.paperweight.userdev") version "1.3.8"
}

group = "net.minevn"
version = "3.0"

repositories {
    mavenCentral()
    mavenLocal()
    maven("https://papermc.io/repo/repository/maven-public/")

    maven("https://repo.dmulloy2.net/repository/public/")
    maven {
        setUrl("http://pack.minevn.net/repo/")
        isAllowInsecureProtocol = true
    }
}

dependencies {
    paperDevBundle("1.19.4-R0.1-SNAPSHOT")
    compileOnly("io.papermc.paper:paper-api:1.19.2-R0.1-SNAPSHOT")

    compileOnly("net.minevn:MineStrike:3.0")
    compileOnly("com.comphenix.protocol:ProtocolLib:4.6.0")
}

tasks {
    assemble {
        dependsOn(reobfJar)
    }

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(17))
        }
    }

    compileJava {
        options.release.set(17)
    }
}

tasks.withType<JavaCompile> {
    options.encoding = Charsets.UTF_8.name()
//    options.compilerArgs.add("-Xlint:deprecation")
}

tasks {
    var jarName = ""

    jar {
        jarName = archiveFileName.get()
    }

    register("customCopy") {
        dependsOn(reobfJar)

        val path = project.properties["shadowPath"]

        if (path != null) {
            doLast {
                println("Copying $jarName to $path")
                val to = File("$path/$jarName")
                val rename = File("$path/NoMoreESP.jar")
                File(project.projectDir, "build/libs/$jarName").copyTo(to, true)
                if (rename.exists()) rename.delete()
                to.renameTo(rename)
                println("Copied")
            }
        }
    }

    assemble {
        dependsOn(get("customCopy"))
    }

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(17))
        }
    }

    compileJava {
        options.release.set(17)
    }
}