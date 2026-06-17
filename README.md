# Dependency Updater Plugin for IntelliJ IDEA

A smart, high-performance, and lightweight IntelliJ IDEA plugin designed to help developers manage, upgrade, and secure their **Maven** and **Gradle** project dependencies effortlessly.

## 🚀 Key Features

- **Full Maven & Gradle Support**: Scans `pom.xml` and `build.gradle` / `build.gradle.kts` files seamlessly using IntelliJ's native PSI parsers.
- **Transitive Dependencies & Vulnerability Checking**: Dives deep into the background dependency tree built by Gradle/Maven. It connects to the open-source **OSV API** to identify CVEs (Common Vulnerabilities and Exposures) lurking in hidden, nested packages.
- **Custom Repositories & Secure Auth**: Configure your own private artifact registries (like JFrog Artifactory or Sonatype Nexus). The plugin queries all defined repositories concurrently. Supports **Basic Authentication (Username/Password)** and **Bearer Tokens**.
- **Enterprise-Grade Security**: All your sensitive tokens and passwords are encrypted and securely stored using IntelliJ's native **PasswordSafe** integration (synced with Windows Credential Manager or macOS Keychain).
- **Standalone Compatibility**: Built to run gracefully in strictly restricted corporate environments. It does not require official Maven/Gradle IDE plugins to be enabled to function perfectly!
- **Smart Update Suggestions**: Interrogates artifact repositories to find newer library versions, cleanly prioritizing long-term support (LTS) releases over unverified betas.
- **One-Click Upgrades**: Modifies your build files locally to the selected safe version and triggers IDE synchronization seamlessly.

## ⚙️ Configuration (Custom Repositories)
You can configure global or private repositories by going to:
`Settings (Preferences)` -> `Tools` -> `Dependency Updater`

From there you can:
- Use the **"+"** button to add global predefined repositories (Maven Central, Google Maven, Spring Plugins, JBoss, etc.)
- Add custom private URLs.
- Safely inject your Auth Tokens.

## 🛠️ How to use
Once installed, open the **Dependency Updater** tool window (usually docked at the bottom of the IDE).
1. Click **Scan Dependencies** to populate the list.
2. Select target versions using the dropdowns.
3. Click **Update Selected** to rewrite your build files automatically.
4. Use the **Transitive Dependencies** tab to inspect hidden risks and CVEs.

## 🏗️ Build From Source
The plugin relies on the official IntelliJ Platform Gradle Plugin and requires **Java 21** or newer to build.
```bash
./gradlew buildPlugin
```
The final distribution artifact will be created at `build/distributions/`.

## 💻 Stack & Architecture
- **UI**: Java Swing (IntelliJ Platform UI)
- **Parsers**: IntelliJ PSI (Program Structure Interface) for uncoupled file reading.
- **Network**: Asynchronous API calls (Java `CompletableFuture` + `HttpClient`) targeting `api.osv.dev` and multiple user-configured endpoints simultaneously.
- **Security**: IntelliJ `PasswordSafe` API.
- **Testing**: JUnit 5, Mockito, JaCoCo (>80% Code Coverage)
