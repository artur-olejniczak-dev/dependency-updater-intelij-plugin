import sys

with open('src/main/java/com/example/dependencyupdater/TransitiveDependencyPanel.java', 'r', encoding='utf-8') as f:
    content = f.read()

# Add Update Button to toolbar
old_toolbar = '''        scanButton = new JButton("Scan Transitive Dependencies (Gradle)");

        stopButton = new JButton("Clear / Stop");

        scanButton.addActionListener(e -> performScan());

        stopButton.addActionListener(e -> stopAndClear());

        leftToolbar.add(scanButton);

        leftToolbar.add(stopButton);

        JLabel infoLabel = new JLabel(" (Detects vulnerabilities in hidden background packages. Auto-update disabled.)");'''

new_toolbar = '''        scanButton = new JButton("Scan Transitive Dependencies (Gradle)");
        stopButton = new JButton("Clear / Stop");
        JButton updateButton = new JButton("Update Selected");
        
        scanButton.addActionListener(e -> performScan());
        stopButton.addActionListener(e -> stopAndClear());
        updateButton.addActionListener(e -> performUpdate());
        
        leftToolbar.add(scanButton);
        leftToolbar.add(stopButton);
        leftToolbar.add(updateButton);
        
        JLabel infoLabel = new JLabel(" (Detects vulnerabilities in hidden background packages.)");'''

content = content.replace(old_toolbar, new_toolbar)

# Add performUpdate logic
update_logic = '''
    private void performUpdate() {
        java.util.List<Dependency> toUpdate = new java.util.ArrayList<>();
        java.util.List<String> newVersions = new java.util.ArrayList<>();
        
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            Boolean isSelected = (Boolean) tableModel.getValueAt(i, 0);
            if (isSelected != null && isSelected) {
                String libraryCoords = (String) tableModel.getValueAt(i, 1);
                String[] parts = libraryCoords.split(":");
                if (parts.length >= 2) {
                    Dependency dep = new Dependency(parts[0], parts[1], (String) tableModel.getValueAt(i, 2));
                    toUpdate.add(dep);
                    DropdownValue dv = (DropdownValue) tableModel.getValueAt(i, 3);
                    newVersions.add(dv.selected.split(" ")[0]);
                }
            }
        }
        
        if (toUpdate.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No dependencies selected for update.", "Information", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        int result = JOptionPane.showConfirmDialog(this, "Are you sure you want to enforce new versions for " + toUpdate.size() + " transitive dependencies?", "Confirm Update", JOptionPane.YES_NO_OPTION);
        if (result == JOptionPane.YES_OPTION) {
            com.intellij.openapi.command.WriteCommandAction.runWriteCommandAction(project, () -> {
                for (int i = 0; i < toUpdate.size(); i++) {
                    gradleParser.updateTransitiveDependency(project, toUpdate.get(i), newVersions.get(i));
                }
            });
            JOptionPane.showMessageDialog(this, "Transitive dependencies updated successfully! Please refresh Gradle.", "Success", JOptionPane.INFORMATION_MESSAGE);
        }
    }
'''

content = content.replace(
    '    private void stopAndClear() {',
    update_logic + '\n    private void stopAndClear() {'
)

# Update refreshTable row addition
old_add_row = '''            tableModel.addRow(new Object[]{

                    dep.getCoordinates(),

                    dep.getCurrentVersion(),

                    dropdown,

                    vulnText

            });'''

new_add_row = '''            tableModel.addRow(new Object[]{
                    Boolean.FALSE,
                    dep.getCoordinates(),
                    dep.getCurrentVersion(),
                    dropdown,
                    vulnText
            });'''

content = content.replace(old_add_row, new_add_row)

with open('src/main/java/com/example/dependencyupdater/TransitiveDependencyPanel.java', 'w', encoding='utf-8') as f:
    f.write(content)
