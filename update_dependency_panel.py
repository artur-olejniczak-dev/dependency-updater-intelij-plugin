import sys

# Update DependencyUpdaterPanel.java
with open('src/main/java/com/example/dependencyupdater/DependencyUpdaterPanel.java', 'r', encoding='utf-8') as f:
    content = f.read()

# Remove checkbox instantiation and action
old_checkbox = '''        hideUpToDateCheckBox = new JCheckBox("Hide Up-to-date", false);

        scanButton.addActionListener(e -> performScan());'''
new_checkbox = '''        scanButton.addActionListener(e -> performScan());'''
content = content.replace(old_checkbox, new_checkbox)

old_checkbox_action = '''        ltsOnlyCheckBox.addActionListener(e -> performScan());

        hideUpToDateCheckBox.addActionListener(e -> {

            if (currentDependencies != null && !currentDependencies.isEmpty()) {

                refreshTable();

            }

        });'''
new_checkbox_action = '''        ltsOnlyCheckBox.addActionListener(e -> performScan());'''
content = content.replace(old_checkbox_action, new_checkbox_action)

# Update toolbar layout
# leftToolbar -> scan, stop, lts
# rightToolbar -> select, deselect, update
old_toolbar = '''        leftToolbar.add(scanButton);

        leftToolbar.add(stopButton);

        leftToolbar.add(updateButton);

        rightToolbar.add(selectAllButton);

        rightToolbar.add(deselectAllButton);

        rightToolbar.add(ltsOnlyCheckBox);

        rightToolbar.add(hideUpToDateCheckBox);'''

new_toolbar = '''        leftToolbar.add(scanButton);
        leftToolbar.add(stopButton);
        leftToolbar.add(ltsOnlyCheckBox);

        rightToolbar.add(selectAllButton);
        rightToolbar.add(deselectAllButton);
        rightToolbar.add(updateButton);'''

content = content.replace(old_toolbar, new_toolbar)

# Update refreshTable logic
old_refresh = '''            boolean hideUpToDate = hideUpToDateCheckBox.isSelected();

            if (hideUpToDate && "Latest version :)".equals(dropdown.selected)) {

                continue;

            }'''
new_refresh = '''            if ("Latest version :)".equals(dropdown.selected)) {
                continue;
            }'''
content = content.replace(old_refresh, new_refresh)


with open('src/main/java/com/example/dependencyupdater/DependencyUpdaterPanel.java', 'w', encoding='utf-8') as f:
    f.write(content)
