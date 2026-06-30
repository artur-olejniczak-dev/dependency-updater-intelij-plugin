import sys

with open('src/main/java/com/example/dependencyupdater/GradleParser.java', 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace('versionRaw.startsWith(\"\"\"', 'versionRaw.startsWith(\"\\\"\")')

with open('src/main/java/com/example/dependencyupdater/GradleParser.java', 'w', encoding='utf-8') as f:
    f.write(content)
