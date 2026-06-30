import sys

with open('src/main/java/com/example/dependencyupdater/MavenCentralApiClient.java', 'r', encoding='utf-8') as f:
    content = f.read()

old_logic = '''                if (repo.isHtmlListing()) {
                    url = String.format("%s%s/%s/", baseUrl, groupPath, artifactId);'''

new_logic = '''                if (repo.isHtmlListing()) {
                    url = String.format("%slibs-release/%s/%s/", baseUrl, groupPath, artifactId);'''

content = content.replace(old_logic, new_logic)

with open('src/main/java/com/example/dependencyupdater/MavenCentralApiClient.java', 'w', encoding='utf-8') as f:
    f.write(content)
