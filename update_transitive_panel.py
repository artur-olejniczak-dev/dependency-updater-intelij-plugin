import sys

with open('src/main/java/com/example/dependencyupdater/TransitiveDependencyPanel.java', 'r', encoding='utf-8') as f:
    content = f.read()

# Fix toolbar alignment
old_toolbar = '''        scanButton.addActionListener(e -> performScan());
        stopButton.addActionListener(e -> stopAndClear());
        updateButton.addActionListener(e -> performUpdate());
        
        leftToolbar.add(scanButton);
        leftToolbar.add(stopButton);
        leftToolbar.add(updateButton);
        
        JLabel infoLabel = new JLabel(" (Detects vulnerabilities in hidden background packages.)");
        infoLabel.setForeground(Color.GRAY);
        leftToolbar.add(infoLabel);
        
        topPanel.add(leftToolbar, BorderLayout.WEST);'''

new_toolbar = '''        scanButton.addActionListener(e -> performScan());
        stopButton.addActionListener(e -> stopAndClear());
        updateButton.addActionListener(e -> performUpdate());
        
        leftToolbar.add(scanButton);
        leftToolbar.add(stopButton);
        
        JLabel infoLabel = new JLabel(" (Detects vulnerabilities in hidden background packages.)");
        infoLabel.setForeground(Color.GRAY);
        leftToolbar.add(infoLabel);
        
        JPanel rightToolbar = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));
        rightToolbar.add(updateButton);
        
        topPanel.add(leftToolbar, BorderLayout.WEST);
        topPanel.add(rightToolbar, BorderLayout.EAST);'''

content = content.replace(old_toolbar, new_toolbar)

# Hide up-to-date dependencies in refreshTable
old_refresh = '''    private void refreshTable() {
        for (Dependency dep : currentTransitiveDependencies) {
            String vulnText = "?? " + dep.getVulnerabilities().size() + " vulnerabilities: " + String.join(", ", dep.getVulnerabilities());'''

new_refresh = '''    private void refreshTable() {
        for (Dependency dep : currentTransitiveDependencies) {
            if (dep.getAvailableVersions().contains("Latest version :)")) {
                continue;
            }
            String vulnText = "?? " + dep.getVulnerabilities().size() + " vulnerabilities: " + String.join(", ", dep.getVulnerabilities());'''

content = content.replace(old_refresh, new_refresh)

with open('src/main/java/com/example/dependencyupdater/TransitiveDependencyPanel.java', 'w', encoding='utf-8') as f:
    f.write(content)

# Also fix the removed hideUpToDateCheckBox field in DependencyUpdaterPanel
with open('src/main/java/com/example/dependencyupdater/DependencyUpdaterPanel.java', 'r', encoding='utf-8') as f:
    content = f.read()
content = content.replace('    private JCheckBox hideUpToDateCheckBox;\n', '')
with open('src/main/java/com/example/dependencyupdater/DependencyUpdaterPanel.java', 'w', encoding='utf-8') as f:
    f.write(content)
