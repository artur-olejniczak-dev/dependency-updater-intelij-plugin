import sys

with open('src/main/java/com/example/dependencyupdater/DependencyUpdaterPanel.java', 'r', encoding='utf-8') as f:
    content = f.read()

# Fix 1: leftover action listener
action_listener_bad = '''        hideUpToDateCheckBox.addActionListener(e -> {
            if (currentDependencies != null && !currentDependencies.isEmpty()) {
                refreshTable();
            }
        });'''
if action_listener_bad in content:
    content = content.replace(action_listener_bad, '')

# Fix 2: leftover adding to toolbar (might be in initUI differently)
toolbar_bad = '''        leftToolbar.add(hideUpToDateCheckBox);'''
if toolbar_bad in content:
    content = content.replace(toolbar_bad, '')

toolbar_bad_2 = '''        rightToolbar.add(hideUpToDateCheckBox);'''
if toolbar_bad_2 in content:
    content = content.replace(toolbar_bad_2, '')

# Fix 3: leftover reference in refreshTable
isupdatable_bad = '''            if (hideUpToDateCheckBox.isSelected() && !isUpdatable) {'''
if isupdatable_bad in content:
    content = content.replace(isupdatable_bad, '''            if (!isUpdatable) {''')
    
# Let's just blindly remove any remaining line with hideUpToDateCheckBox
lines = content.split('\n')
new_lines = [line for line in lines if 'hideUpToDateCheckBox' not in line]
content = '\n'.join(new_lines)

with open('src/main/java/com/example/dependencyupdater/DependencyUpdaterPanel.java', 'w', encoding='utf-8') as f:
    f.write(content)
