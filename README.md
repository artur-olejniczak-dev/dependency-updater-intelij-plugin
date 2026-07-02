# Dependency Updater (IntelliJ Plugin)

The ultimate dependency manager and security scanner for IntelliJ IDEA.
**DependencyUpdater** is a powerful plugin designed explicitly for **Maven** and **Gradle** projects. It acts as your intelligent assistant to keep your build files clean, updated, and secure.

## Key Features
* 🚀 **Full Maven & Gradle Support:** Seamlessly parses `pom.xml`, `build.gradle` / `build.gradle.kts` and `libs.versions.toml` files.
* 🧩 **Gradle Plugins Update:** Support for scanning and updating Gradle plugins configured within `plugins {}` block (both Groovy and Kotlin DSLs).
* 🛡️ **Security Scanning (OSV API):** Automatically detects Common Vulnerabilities and Exposures (CVE) in your direct and transitive dependencies.
* 🔄 **Smart Version Updates & Filtering:** Discovers newer library versions directly from your configured artifact repositories. Automatically filters out already up-to-date dependencies for clean, distraction-free lists.
* 🏢 **Enterprise Ready:** Built-in native support for Corporate Proxies and custom SSL Certificates (JFrog Artifactory compatibility).
* 🔐 **Custom Repositories:** Connects to private registries with safe Bearer Token and Basic authentication. Includes predefined HTML directory listing scanner for private JFrog instances.
* 🎯 **LTS & Beta Filtering:** Advanced version recognition differentiating between Long Term Support (LTS) releases and experimental Beta/Snapshot builds.

## What's New in Version 1.5
* **Smart Up-to-Date Filtering:** The Direct Dependencies table now intelligently hides libraries that are already up to date. Toggling between *LTS Only* and *Beta* modes dynamically updates table visibility based on available builds.
* **Advanced LTS/Beta Recognition:** Enhanced version parsing engine that accurately classifies corporate naming conventions and JFrog Artifactory builds (such as timestamped snapshots, `-dev`, `-preview`, and `-ea` qualifiers).
* **Streamlined Transitive Scanning:** Transitive dependency analysis has been streamlined to focus purely on background vulnerability detection (CVE scanning) and version auditing without automatic build file modifications.

## Installation
1. Open IntelliJ IDEA.
2. Go to **Settings/Preferences** > **Plugins** > **Marketplace**.
3. Search for **DependencyUpdater**.
4. Click **Install** and restart your IDE.

## Usage
1. Open any Maven or Gradle project.
2. Open the **Dependency Updater** Tool Window at the bottom of the IDE.
3. Configure your Repositories (Settings > Tools > Dependency Updater) if you use internal corporate registries. (Select **JFrog** if your Artifactory uses HTML directory listings).
4. Click **Scan Dependencies**.
5. Select the outdated libraries and click **Update Selected**.

## Building from Source
```bash
git clone https://github.com/artur-olejniczak-dev/dependency-updater-intelij-plugin.git
cd dependency-updater-intelij-plugin
./gradlew buildPlugin
```
