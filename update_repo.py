import sys

with open('src/main/java/com/example/dependencyupdater/settings/Repository.java', 'r', encoding='utf-8') as f:
    content = f.read()

# Add field
content = content.replace(
    'private String username;',
    'private String username;\n    private boolean isHtmlListing = false;'
)

# Add getters/setters
getters_setters = '''
    public boolean isHtmlListing() {
        return isHtmlListing;
    }

    public void setHtmlListing(boolean htmlListing) {
        isHtmlListing = htmlListing;
    }
'''
content = content.replace(
    '    public Repository cloneRepo() {',
    getters_setters + '\n    public Repository cloneRepo() {'
)

# Update cloneRepo
content = content.replace(
    'copy.username = this.username;',
    'copy.username = this.username;\n        copy.isHtmlListing = this.isHtmlListing;'
)

# Update equals
content = content.replace(
    '&& authType == that.authType && Objects.equals(username, that.username);',
    '&& authType == that.authType && Objects.equals(username, that.username) && isHtmlListing == that.isHtmlListing;'
)

# Update hashCode
content = content.replace(
    'Objects.hash(id, name, url, authType, username);',
    'Objects.hash(id, name, url, authType, username, isHtmlListing);'
)

with open('src/main/java/com/example/dependencyupdater/settings/Repository.java', 'w', encoding='utf-8') as f:
    f.write(content)
