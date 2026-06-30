import sys

# DependencyUpdaterPanel.java
with open('src/main/java/com/example/dependencyupdater/DependencyUpdaterPanel.java', 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace(
    'if (column == 1 || column == 4) return true;',
    'if (column == 1 || column == 2 || column == 4) return true;'
)

content = content.replace(
    '''        table.getColumnModel().getColumn(4).setCellEditor(readOnlyEditor);''',
    '''        table.getColumnModel().getColumn(2).setCellEditor(readOnlyEditor);
        table.getColumnModel().getColumn(4).setCellEditor(readOnlyEditor);'''
)

with open('src/main/java/com/example/dependencyupdater/DependencyUpdaterPanel.java', 'w', encoding='utf-8') as f:
    f.write(content)


# TransitiveDependencyPanel.java
with open('src/main/java/com/example/dependencyupdater/TransitiveDependencyPanel.java', 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace(
    'if (column == 0 || column == 3) return true;',
    'if (column == 0 || column == 1 || column == 3) return true;'
)

content = content.replace(
    '''        table.getColumnModel().getColumn(3).setCellEditor(readOnlyEditor);''',
    '''        table.getColumnModel().getColumn(1).setCellEditor(readOnlyEditor);
        table.getColumnModel().getColumn(3).setCellEditor(readOnlyEditor);'''
)

with open('src/main/java/com/example/dependencyupdater/TransitiveDependencyPanel.java', 'w', encoding='utf-8') as f:
    f.write(content)

