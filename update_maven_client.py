import sys
import re

with open('src/main/java/com/example/dependencyupdater/MavenCentralApiClient.java', 'r', encoding='utf-8') as f:
    content = f.read()

# Replace URL logic
old_url_logic = '''String url = String.format("%s%s/%s/maven-metadata.xml", baseUrl, groupPath, artifactId);'''
new_url_logic = '''String url;
                if (repo.isHtmlListing()) {
                    url = String.format("%s%s/%s/", baseUrl, groupPath, artifactId);
                } else {
                    url = String.format("%s%s/%s/maven-metadata.xml", baseUrl, groupPath, artifactId);
                }'''
content = content.replace(old_url_logic, new_url_logic)

# Replace thenApply body
old_then_apply = '''            }).thenApply(body -> {
                if (body != null) {
                    return parseVersions(body);
                }
                return new ArrayList<String>();
            })'''

new_then_apply = '''            }).thenApply(body -> {
                if (body != null) {
                    if (repo.isHtmlListing()) {
                        return parseHtmlVersions(body);
                    } else {
                        return parseVersions(body);
                    }
                }
                return new ArrayList<String>();
            })'''
content = content.replace(old_then_apply, new_then_apply)

# Add parseHtmlVersions function
parse_html_code = '''
    List<String> parseHtmlVersions(String htmlBody) {
        List<String> versions = new ArrayList<>();
        if (htmlBody == null || htmlBody.isEmpty()) return versions;
        try {
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("href=\\"([0-9][^\\"]+)/\\"");
            java.util.regex.Matcher matcher = pattern.matcher(htmlBody);
            while (matcher.find()) {
                versions.add(matcher.group(1));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return versions;
    }
}
'''
# find the last closing brace and replace it
last_brace_index = content.rfind('}')
content = content[:last_brace_index] + parse_html_code

with open('src/main/java/com/example/dependencyupdater/MavenCentralApiClient.java', 'w', encoding='utf-8') as f:
    f.write(content)
