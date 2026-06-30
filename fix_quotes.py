import sys

with open('src/main/java/com/example/dependencyupdater/GradleParser.java', 'r', encoding='utf-8') as f:
    content = f.read()

bad_line = 'block.append("        force("").append(coord).append("")\\n");'
good_line = 'block.append("        force(\\"").append(coord).append("\\")\\n");'

content = content.replace(bad_line, good_line)

with open('src/main/java/com/example/dependencyupdater/GradleParser.java', 'w', encoding='utf-8') as f:
    f.write(content)
