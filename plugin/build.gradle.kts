import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `java`
    `java-gradle-plugin`
    `kotlin-dsl`
    // Makes sure to update the kotlin version below in the test resources as well.
    kotlin("jvm") version "1.2.51"
    `maven-publish`
    id("com.gradle.plugin-publish") version "0.9.10"
}

group = "org.gradleweaver.plugins"
version = "0.1.0"

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    compileOnly(gradleApi())
    testCompileOnly(gradleTestKit())

    fun junitJupiter(name: String, version: String = "5.2.0") =
            create(group = "org.junit.jupiter", name = name, version = version)
    testCompile(junitJupiter(name = "junit-jupiter-api"))
    testCompile(junitJupiter(name = "junit-jupiter-engine"))
    testCompile(junitJupiter(name = "junit-jupiter-params"))
    testCompile(group = "org.junit-pioneer", name = "junit-pioneer", version = "0.2.2")
    testRuntime(create(group = "org.junit.platform", name = "junit-platform-launcher", version = "1.0.0"))
}

publishing {
    repositories {
        // Work around Gradle TestKit limitations in order to allow for compileOnly dependencies
        maven {
            name = "test"
            url = uri("$buildDir/plugin-test-repository")
        }
    }

    publications {
        create<MavenPublication>("mavenJar") {
            from(components.getByName("java"))
        }
    }
}


java {
    sourceCompatibility = JavaVersion.VERSION_1_8
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions.jvmTarget = "1.8"
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

val registeredPlugin = gradlePlugin.plugins.register("jlink-plugin") {
    id = "${project.group}.${project.name}"
    implementationClass = "org.gradleweaver.plugins.jlink.JLinkPlugin"
    description = "A Gradle plugin for handling platform-specific dependencies and releases."
}

pluginBundle {
    website = "https://github.com/gradleweaver/jlink-plugin"
    vcsUrl = "https://github.com/gradleweaver/jlink-plugin"
    tags = listOf("jlink")

    plugins {
        create("jlink-plugin") {
            id = registeredPlugin.get().id
            displayName = registeredPlugin.get().displayName
        }
    }
}

tasks {
    val publishPluginsToTestRepository by creating {
        dependsOn("publishPluginMavenPublicationToTestRepository")
    }
    val processTestResources: ProcessResources by getting
    val writeTestProperties by creating(WriteProperties::class) {
        outputFile = processTestResources.destinationDir.resolve("test.properties")
        property("version", version)
        property("kotlinVersion", KotlinVersion.CURRENT)
    }
    processTestResources.dependsOn(writeTestProperties)
    "test" {
        dependsOn(publishPluginsToTestRepository)
    }
}

apply(from = "../gradle/wrapper.gradle.kts")
