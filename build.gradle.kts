plugins {
    id("java")
    id("jacoco")
    id("org.jetbrains.intellij.platform") version "2.16.0"
}

group = "com.example.dependencyupdater"
version = "1.2-SNAPSHOT"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    implementation("com.google.code.gson:gson:2.10.1")

    intellijPlatform {
        intellijIdea("2026.1.3")
        bundledPlugin("com.intellij.java")
        bundledPlugin("org.jetbrains.idea.maven")
        bundledPlugin("org.jetbrains.plugins.gradle")
        
        pluginVerifier()
    }

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    testImplementation("org.mockito:mockito-core:5.14.2")
    testImplementation("org.mockito:mockito-junit-jupiter:5.14.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks {
    withType<JavaCompile> {
        sourceCompatibility = "21"
        targetCompatibility = "21"
    }
    test {
        useJUnitPlatform()
        jvmArgs("-Dnet.bytebuddy.experimental=true")
        finalizedBy("jacocoTestReport")
    }
    jacocoTestReport {
        dependsOn(test)
        reports {
            csv.required.set(true)
            xml.required.set(false)
            html.required.set(true)
        }
    }
}

intellijPlatform {
    pluginConfiguration {
        version.set("1.2")
        ideaVersion {
            sinceBuild.set("251")
            untilBuild.set("262.*")
        }
    }
}
