import os
content = '''# Dependency Updater (IntelliJ Plugin)

The ultimate dependency manager and security scanner for IntelliJ IDEA.
**DependencyUpdater** is a powerful plugin designed explicitly for **Maven** and **Gradle** projects. It acts as your intelligent assistant to keep your build files clean, updated, and secure.

## Key Features
* ?? **Full Maven & Gradle Support:** Seamlessly parses pom.xml, uild.gradle / build.gradle.kts and libs.versions.toml files.
* ?? **Gradle Plugins Update:** Support for scanning and updating Gradle plugins configured within plugins {} block (both Groovy and Kotlin DSLs).
* ??? **Security Scanning (OSV API):** Automatically detects Common Vulnerabilities and Exposures (CVE) in your direct and transitive dependencies.
* ?? **Smart Version Updates:** Discovers newer library versions directly from your configured artifact repositories.
* ?? **Enterprise Ready:** Built-in native support for Corporate Proxies and custom SSL Certificates (JFrog Artifactory compatibility).
* ?? **Custom Repositories:** Connects to private registries with safe Bearer Token and Basic authentication.
* ?? **LTS Filtering:** Highlights Long Term Support (LTS) releases for safer enterprise upgrades.

## Installation
1. Open IntelliJ IDEA.
2. Go to **Settings/Preferences** > **Plugins** > **Marketplace**.
3. Search for **DependencyUpdater**.
4. Click **Install** and restart your IDE.

## Usage
1. Open any Maven or Gradle project.
2. Open the **Dependency Updater** Tool Window at the bottom of the IDE.
3. Configure your Repositories (Settings > Tools > Dependency Updater) if you use internal corporate registries.
4. Click **Scan Dependencies**.
5. Select the outdated libraries and click **Update Selected**.

## Building from source
`ash
git clone https://github.com/artur-olejniczak-dev/dependency-updater-intelij-plugin.git
cd dependency-updater-intelij-plugin
./gradlew buildPlugin
`
'''
with open('README.md', 'w', encoding='utf-8') as f:
    f.write(content)
