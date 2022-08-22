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
    paperDevBundle("1.19.2-R0.1-SNAPSHOT")
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