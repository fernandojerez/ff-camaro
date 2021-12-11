import java.io.FileInputStream
import java.net.URI
import java.util.*

plugins {
    `java-library`
    `maven-publish`
    signing
}

project.buildDir = System.getenv("FF_BUILD_DIR")?.let {
    File("${it}/ff@camaro/build").mkdirs()
    File("${it}/ff@camaro/build")
}

val props = FileInputStream(File("repo.properties")).use {
    val p = Properties()
    p.load(it)
    p
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
    withJavadocJar()
    withSourcesJar()
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(gradleApi())
    compileOnly("org.apache.maven:maven-model:3.8.1")
    compileOnly("org.apache.maven:maven-model-builder:3.8.1")
    implementation("org.snakeyaml:snakeyaml-engine:2.3")
}

tasks.withType<Jar> {
    manifest {
        attributes(
            "Implementation-Title" to "${project.property("project_group")}@${project.property("project_name")}",
            "Implementation-Version" to project.property("project_version"),
            "Automatic-Module-Name" to "${project.property("project_group")}.${project.property("project_name")}"
        )
    }
}

fun MavenPublication.setupPublication() {
    from(components["java"])
    groupId = project.property("project_group").toString()
    artifactId = project.property("project_name").toString()
    version = project.property("project_version").toString()
    pom {
        name.set("FF Camaro")
        description.set(
            "Camaro is a build system layer over gradle. Its purporse is to allow the compilation of FF projects."
        )
        url.set("https://github.com/fernandojerez/ff-camaro")
        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }
        developers {
            developer {
                id.set("fernandojerez")
                name.set("Fernando Jerez")
                email.set("fernandojerez@gmail.com")
            }
        }
        scm {
            connection.set("scm:git:git://git@github.com:fernandojerez/ff-camaro.git")
            developerConnection.set("scm:git:ssh://git@github.com:fernandojerez/ff-camaro.git")
            url.set("https://github.com/fernandojerez/ff-camaro")
        }
    }
}

publishing {
    publications {
        register("local", MavenPublication::class) {
            setupPublication()
        }
        register("maven", MavenPublication::class) {
            setupPublication()
        }
    }
    repositories {
        maven {
            val repoUrl = (System.getenv("FF_REPO") ?: "file://${System.getProperty("user.home")}/.ff").let {
                if (it.startsWith("file://")) it
                else "file://$it"
            }
            url = URI(repoUrl)
            name = "local"
        }
        maven {
            url = URI("https://oss.sonatype.org/service/local/staging/deploy/maven2/")
            credentials {
                username = props.getProperty("sonatypeUsername")
                password = props.getProperty("sonatypePassword")
            }
        }
    }
}

ext.set("signing.keyId", props.getProperty("signing.keyId"))
ext.set("signing.password", props.getProperty("signing.password"))
ext.set("signing.secretKeyRingFile", props.getProperty("signing.secretKeyRingFile"))

signing {
    sign(publishing.publications["maven"])
}
