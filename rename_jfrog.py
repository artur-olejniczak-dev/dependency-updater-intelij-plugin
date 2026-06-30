import sys

with open('src/main/java/com/example/dependencyupdater/settings/DependencyUpdaterSettingsComponent.java', 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace('"JFrog Artifactory (HTML)"', '"JFrog"')

with open('src/main/java/com/example/dependencyupdater/settings/DependencyUpdaterSettingsComponent.java', 'w', encoding='utf-8') as f:
    f.write(content)
