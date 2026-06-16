# Dependency Updater Plugin

A smart and lightweight IntelliJ IDEA plugin designed to help developers manage, upgrade, and secure their project dependencies effortlessly.

## Features
- **Direct Dependencies Scanning**: Scans your Maven (`pom.xml`) and Gradle (`build.gradle`, `build.gradle.kts`) build files for declared libraries.
- **Transitive Dependencies & Vulnerability Checking**: Dives deep into the background dependency tree built by Gradle/Maven. It connects to the open-source **OSV API** to identify CVEs (Common Vulnerabilities and Exposures) lurking in hidden packages.
- **Smart Update Suggestions**: Interrogates Maven Central metadata to find newer versions of libraries, cleanly prioritizing long-term support (LTS) releases over unverified betas.
- **One-Click Upgrades**: Modifies your build files locally to the selected safe version and triggers IDE synchronization seamlessly.

## How to use
Once installed, open the **Dependency Updater** tool window (usually docked at the bottom of the IDE).
1. Click **Scan Dependencies** to populate the list.
2. Select target versions using the dropdowns.
3. Click **Update Selected** to rewrite your build files automatically.
4. Use the **Transitive Dependencies** tab to inspect hidden risks.

## Build From Source
The plugin relies on the official IntelliJ Platform Gradle Plugin and requires **Java 21** or newer to build.
```bash
./gradlew buildPlugin
```
The final distribution artifact will be created at `build/distributions/`.

## Stack & Architecture
- UI: Java Swing (IntelliJ Platform UI)
- Build System Parsers: IntelliJ PSI (Program Structure Interface)
- Asynchronous API calls (CompletableFuture) targeting `api.osv.dev` and `repo1.maven.org`
- Unit Testing: JUnit 5, Mockito, JaCoCo (>80% Code Coverage)

