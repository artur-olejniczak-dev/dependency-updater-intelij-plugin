import sys
import re

with open('src/main/java/com/example/dependencyupdater/DependencyUpdaterPanel.java', 'r', encoding='utf-8') as f:
    content = f.read()

# 1. Remove the filter
content = re.sub(r'\.filter\(v -> compareVersions\(v, dep\.getCurrentVersion\(\)\) > 0\)\s*\n\s*\.sorted', '.sorted', content)

# 2. Change limit to 15
content = re.sub(r'\.limit\(10\)', '.limit(15)', content)

# 3. Update isUpdatable logic
old_logic = '''boolean isUpdatable = dropdown.options != null && !dropdown.options.isEmpty() && 
                                  !dropdown.options.get(0).contains("Latest") && 
                                  !dropdown.options.get(0).contains("No newer") && 
                                  !dropdown.options.get(0).contains("Error") && 
                                  !dropdown.options.get(0).contains("Connection");'''

new_logic = '''String topVer = dropdown.options != null && !dropdown.options.isEmpty() ? dropdown.options.get(0).split(" ")[0] : "Latest";
            boolean isUpdatable = !topVer.equals("Latest") && !topVer.equals("No") && !topVer.equals("Error") && !topVer.equals("Connection") && compareVersions(topVer, dep.getCurrentVersion()) > 0;'''

# We need to escape old_logic for regex, or use plain string replacement.
content = content.replace(old_logic, new_logic)

with open('src/main/java/com/example/dependencyupdater/DependencyUpdaterPanel.java', 'w', encoding='utf-8') as f:
    f.write(content)
