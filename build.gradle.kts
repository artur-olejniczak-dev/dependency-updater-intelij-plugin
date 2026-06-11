 plugins {
    id("java")
    id("org.jetbrains.intellij.platform") version "2.2.1"
}

group = "com.example.dependencyupdater"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    implementation("com.google.code.gson:gson:2.10.1")

    intellijPlatform {
        intellijIdeaCommunity("2025.1") // Kompilacja pod SDK 2025.1 (będzie działać na Twoim 2025.3.3)
        bundledPlugin("com.intellij.java")
        bundledPlugin("org.jetbrains.idea.maven")
        bundledPlugin("org.jetbrains.plugins.gradle")
        
        pluginVerifier()
    }
    
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
}

tasks {
    withType<JavaCompile> {
        sourceCompatibility = "21"
        targetCompatibility = "21"
    }
    test {
        useJUnitPlatform()
    }
}

intellijPlatform {
    pluginConfiguration {
        version.set("1.0")
        ideaVersion {
            sinceBuild.set("251")
            untilBuild.set("254.*")
        }
    }
}
