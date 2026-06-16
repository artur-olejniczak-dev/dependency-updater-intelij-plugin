package com.example.dependencyupdater;

import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;
import org.jetbrains.idea.maven.project.MavenProjectsManager;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class DependencyUpdaterPanel extends JPanel {

    private final Project project;
    private final PomParser pomParser = new PomParser();
    private final GradleParser gradleParser = new GradleParser();
    private final MavenCentralApiClient mavenApi = new MavenCentralApiClient();
    private final OsvApiClient osvApi = new OsvApiClient();

    private boolean isGradleProject = false;
    private volatile int currentScanId = 0; // Unique scan ID used for cancellation/stopping background tasks

    private JBTable table;
    private DefaultTableModel tableModel;
    private List<Dependency> currentDependencies = new ArrayList<>();
    
    private JCheckBox ltsOnlyCheckBox;
    private JCheckBox hideUpToDateCheckBox;
    private JButton scanButton;
    private JButton updateButton;
    private JButton selectAllButton;
    private JButton deselectAllButton;
    private JButton stopButton; // Clear/Stop button

    public DependencyUpdaterPanel(Project project) {
        super(new BorderLayout());
        this.project = project;
        initUI();
    }

    private void initUI() {
        // Table structure
        String[] columnNames = {"Update", "Library", "Current Version", "Available Versions", "Current Version Vulnerabilities"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 0) return Boolean.class;
                if (columnIndex == 3) return DropdownValue.class;
                return String.class;
            }
            @Override
            public boolean isCellEditable(int row, int column) {
                // If column 0 is null (update disabled), block checkbox and dropdown editing
                if (column == 0 || column == 3) {
                    return getValueAt(row, 0) != null;
                }
                if (column == 1 || column == 4) return true; // Allows entering read mode (text selection)
                return false;
            }
        };

        table = new JBTable(tableModel);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getColumnModel().getColumn(0).setMaxWidth(110);
        table.getColumnModel().getColumn(0).setMinWidth(90);
        table.getColumnModel().getColumn(0).setPreferredWidth(100);
        
        // Disappearing checkbox (custom renderer)
        table.getColumnModel().getColumn(0).setCellRenderer(new DefaultTableCellRenderer() {
            private final JCheckBox checkBox = new JCheckBox();
            private final JLabel emptyLabel = new JLabel("");

            {
                checkBox.setHorizontalAlignment(SwingConstants.CENTER);
                checkBox.setOpaque(true);
                emptyLabel.setOpaque(true);
            }

            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component compToReturn;
                
                // Tri-state logic: if value is null, hide the checkbox, which also enables correct sorting!
                if (value == null) {
                    compToReturn = emptyLabel;
                } else {
                    checkBox.setSelected((Boolean) value);
                    compToReturn = checkBox;
                }
                
                // Maintain correct background color (e.g. after row selection)
                if (isSelected) {
                    compToReturn.setBackground(table.getSelectionBackground());
                    compToReturn.setForeground(table.getSelectionForeground());
                } else {
                    compToReturn.setBackground(table.getBackground());
                    compToReturn.setForeground(table.getForeground());
                }

                return compToReturn;
            }
        });
        
        // Two-way sorting
        table.setAutoCreateRowSorter(true);
        // Bold headers
        table.getTableHeader().setFont(table.getTableHeader().getFont().deriveFont(Font.BOLD));

        // Custom editor for JComboBox in column 3
        table.getColumnModel().getColumn(3).setCellEditor(new ComboBoxCellEditor());
        table.getColumnModel().getColumn(3).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            protected void setValue(Object value) {
                if (value instanceof DropdownValue) {
                    setText(((DropdownValue) value).selected);
                } else {
                    super.setValue(value);
                }
            }
        });

        // Editor for columns 1 (Library) and 4 (Security) allowing text copy
        JTextField readOnlyField = new JTextField();
        readOnlyField.setEditable(false);
        readOnlyField.setBorder(null);
        DefaultCellEditor readOnlyEditor = new DefaultCellEditor(readOnlyField);
        table.getColumnModel().getColumn(1).setCellEditor(readOnlyEditor);
        table.getColumnModel().getColumn(4).setCellEditor(readOnlyEditor);

        JBScrollPane scrollPane = new JBScrollPane(table);
        add(scrollPane, BorderLayout.CENTER);

        // Toolbar split into left and right sides
        JPanel topPanel = new JPanel(new BorderLayout());
        JPanel leftToolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JPanel rightToolbar = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        
        scanButton = new JButton("Scan Dependencies");
        stopButton = new JButton("Clear / Stop");
        updateButton = new JButton("Update Selected");
        selectAllButton = new JButton("Select All");
        deselectAllButton = new JButton("Deselect All");
        ltsOnlyCheckBox = new JCheckBox("LTS Only", true);
        hideUpToDateCheckBox = new JCheckBox("Hide Up-to-date", false);

        scanButton.addActionListener(e -> performScan());
        stopButton.addActionListener(e -> stopAndClear());
        updateButton.addActionListener(e -> performUpdate());
        selectAllButton.addActionListener(e -> selectAllRows());
        deselectAllButton.addActionListener(e -> deselectAllRows());
        ltsOnlyCheckBox.addActionListener(e -> performScan());
        hideUpToDateCheckBox.addActionListener(e -> {
            tableModel.setRowCount(0);
            refreshTable();
        });

        leftToolbar.add(scanButton);
        leftToolbar.add(stopButton);
        leftToolbar.add(selectAllButton);
        leftToolbar.add(deselectAllButton);
        leftToolbar.add(ltsOnlyCheckBox);
        leftToolbar.add(hideUpToDateCheckBox);
        
        rightToolbar.add(updateButton); // Button on the right
        
        topPanel.add(leftToolbar, BorderLayout.WEST);
        topPanel.add(rightToolbar, BorderLayout.EAST);
        
        add(topPanel, BorderLayout.NORTH);
    }
    
    private void stopAndClear() {
        currentScanId++; // Invalidates any ongoing background requests!
        tableModel.setRowCount(0);
        currentDependencies.clear();
        scanButton.setEnabled(true);
    }

    private void selectAllRows() {
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            if (tableModel.getValueAt(i, 0) != null) {
                tableModel.setValueAt(true, i, 0);
            }
        }
        table.repaint();
    }

    private void deselectAllRows() {
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            if (tableModel.getValueAt(i, 0) != null) {
                tableModel.setValueAt(false, i, 0);
            }
        }
        table.repaint();
    }

    private void performScan() {
        int myScanId = ++currentScanId;
        scanButton.setEnabled(false);
        tableModel.setRowCount(0);

        // Extracting files uses indexes in IntelliJ 2025+, which throws an error if run on the Event Dispatch Thread (EDT).
        com.intellij.openapi.application.ApplicationManager.getApplication().executeOnPooledThread(() -> {
            com.intellij.openapi.application.ApplicationManager.getApplication().runReadAction(() -> {
                if (myScanId != currentScanId) return; // Interrupted!
                
                List<Dependency> extracted = pomParser.extractDependencies(project);
                boolean isGradle = false;

                if (extracted.isEmpty()) {
                    extracted = gradleParser.extractDependencies(project);
                    if (!extracted.isEmpty()) {
                        isGradle = true;
                    }
                }

                final List<Dependency> finalExtracted = extracted;
                final boolean finalIsGradle = isGradle;

                SwingUtilities.invokeLater(() -> {
                    if (myScanId != currentScanId) return; // Interrupted!
                    currentDependencies = finalExtracted;
                    isGradleProject = finalIsGradle;
                    startApiQueries(myScanId);
                });
            });
        });
    }

    private void startApiQueries(int myScanId) {
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (Dependency dep : currentDependencies) {
            CompletableFuture<Void> osvFuture = osvApi.checkVulnerabilities(dep.getGroupId(), dep.getArtifactId(), dep.getCurrentVersion())
                .exceptionally(ex -> new ArrayList<>()) // If OSV fails, return an empty list
                .thenAccept(vulns -> {
                    if (myScanId == currentScanId) {
                        dep.setVulnerabilities(vulns);
                    }
                });
            
            CompletableFuture<Void> mavenFuture = mavenApi.fetchAvailableVersions(dep.getGroupId(), dep.getArtifactId())
                .exceptionally(ex -> new ArrayList<>()) // If the main fetch for versions fails, don't block the table!
                .thenCompose(versions -> {
                    if (myScanId != currentScanId) return CompletableFuture.completedFuture(null);
                    
                    if (versions == null || versions.isEmpty()) {
                        // Error handling for this single library (the rest will continue working)
                        dep.setAvailableVersions(List.of("Connection Error"));
                        return CompletableFuture.completedFuture(null);
                    }

                    // Filter higher versions, SORT DESCENDING, and then LIMIT to 10
                    List<String> higherVersions = versions.stream()
                            .filter(v -> compareVersions(v, dep.getCurrentVersion()) > 0)
                            .sorted((v1, v2) -> compareVersions(v2, v1)) // Sort descending
                            .limit(10)
                            .collect(Collectors.toList());

                    // For each higher version perform an async vulnerability check, protected against exceptions
                    List<CompletableFuture<String>> versionFutures = new ArrayList<>();
                    for (String v : higherVersions) {
                        CompletableFuture<String> vf = osvApi.checkVulnerabilities(dep.getGroupId(), dep.getArtifactId(), v)
                            .exceptionally(ex -> new ArrayList<>()) // Protection against 429/timeout errors!
                            .thenApply(vulns -> {
                                String tag = VersionUtils.isStableVersion(v) ? " (LTS)" : " (BETA)";
                                if (vulns == null || vulns.isEmpty()) {
                                    return v + tag;
                                } else {
                                    return v + tag + " ⚠️ " + vulns.size() + " vulnerabilities";
                                }
                            });
                        versionFutures.add(vf);
                    }

                    return CompletableFuture.allOf(versionFutures.toArray(new CompletableFuture[0]))
                        .exceptionally(ex -> null) // Protect the entire group of queries
                        .thenAccept(vVoid -> {
                            if (myScanId != currentScanId) return; // Interrupted
                            
                            List<String> mappedVersions = versionFutures.stream()
                                    .map(f -> {
                                        try { return f.get(); } catch (Exception e) { return "Error"; }
                                    })
                                    .filter(val -> !val.equals("Error"))
                                    .collect(Collectors.toList());
                            
                            if (mappedVersions.isEmpty()) {
                                dep.setAvailableVersions(List.of("Latest version :)"));
                                return;
                            }
                            
                            if (ltsOnlyCheckBox.isSelected()) {
                                List<String> ltsOnly = mappedVersions.stream().filter(v -> v.contains("(LTS)")).collect(Collectors.toList());
                                if (ltsOnly.isEmpty()) ltsOnly = List.of("No newer LTS versions");
                                dep.setAvailableVersions(ltsOnly);
                            } else {
                                dep.setAvailableVersions(mappedVersions);
                            }
                        });
                });

            futures.add(osvFuture);
            futures.add(mavenFuture);
        }

        // Wait for all futures. Regardless of individual component errors, force UI refresh (exceptionally returns null bypassing hangs)
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .exceptionally(ex -> null)
            .thenRun(() -> SwingUtilities.invokeLater(() -> {
                if (myScanId == currentScanId) {
                    refreshTable();
                }
            }));
    }

    private void refreshTable() {
        for (Dependency dep : currentDependencies) {
            boolean hasVulnerabilities = !dep.getVulnerabilities().isEmpty();
            String vulnText = "✅ Secure";
            if (hasVulnerabilities) {
                List<String> vulns = dep.getVulnerabilities();
                vulnText = "⚠️ " + vulns.size() + " vulnerabilities: " + String.join(", ", vulns);
            }
            
            DropdownValue dropdown = new DropdownValue(dep.getAvailableVersions());
            
            boolean isUpdatable = dropdown.options != null && !dropdown.options.isEmpty() && 
                                  !dropdown.options.get(0).contains("Latest") && 
                                  !dropdown.options.get(0).contains("No newer") && 
                                  !dropdown.options.get(0).contains("Error") && 
                                  !dropdown.options.get(0).contains("Connection");

            if (hideUpToDateCheckBox.isSelected() && !isUpdatable) {
                continue; // Skip rendering this row
            }

            tableModel.addRow(new Object[]{
                    isUpdatable ? false : null, // null used to differentiate blocked from unchecked when sorting
                    dep.getCoordinates(),
                    dep.getCurrentVersion(),
                    dropdown,
                    vulnText
            });
        }
        scanButton.setEnabled(true);
    }

    private void performUpdate() {
        int rowCount = tableModel.getRowCount();
        boolean anyUpdated = false;

        for (int i = 0; i < rowCount; i++) {
            Boolean isSelected = (Boolean) tableModel.getValueAt(i, 0);
            if (isSelected != null && isSelected) {
                String coordinates = (String) tableModel.getValueAt(i, 1);
                DropdownValue dropdown = (DropdownValue) tableModel.getValueAt(i, 3);
                String newVersionWithTag = dropdown.selected;
                
                if (newVersionWithTag.equals("Latest version :)") || newVersionWithTag.contains("No newer") || newVersionWithTag.contains("Error") || newVersionWithTag.contains("Connection")) continue; 
                
                // Remove "(LTS)" and connecting spaces from text to extract clean version number
                String newVersion = newVersionWithTag.split(" ")[0];
                
                Dependency targetDep = currentDependencies.stream()
                        .filter(d -> d.getCoordinates().equals(coordinates))
                        .findFirst().orElse(null);

                if (targetDep != null && !targetDep.getCurrentVersion().equals(newVersion)) {
                    if (isGradleProject) {
                        gradleParser.updateDependencyVersion(project, targetDep, newVersion);
                    } else {
                        pomParser.updateDependencyVersion(project, targetDep, newVersion);
                    }
                    anyUpdated = true;
                }
            }
        }

        if (anyUpdated) {
            if (isGradleProject) {
                com.intellij.openapi.externalSystem.util.ExternalSystemUtil.refreshProjects(
                    new com.intellij.openapi.externalSystem.importing.ImportSpecBuilder(
                        project, com.intellij.openapi.externalSystem.model.ProjectSystemId.IDE
                    )
                );
                JOptionPane.showMessageDialog(this, "Gradle files updated. You may need to click 'Load Gradle Changes' in the IDE.");
            } else {
                MavenProjectsManager.getInstance(project).forceUpdateAllProjectsOrFindAllAvailablePomFiles();
                JOptionPane.showMessageDialog(this, "pom.xml files updated. Maven reload started.");
            }
            performScan(); 
        } else {
            JOptionPane.showMessageDialog(this, "No libraries selected for update.");
        }
    }

    int compareVersions(String v1, String v2) {
        String[] p1 = v1.split("[.\\-]");
        String[] p2 = v2.split("[.\\-]");
        int len = Math.max(p1.length, p2.length);
        for (int i = 0; i < len; i++) {
            int n1 = i < p1.length ? parseInt(p1[i]) : 0;
            int n2 = i < p2.length ? parseInt(p2[i]) : 0;
            if (n1 < n2) return -1;
            if (n1 > n2) return 1;
        }
        return 0;
    }

    int parseInt(String str) {
        try {
            return Integer.parseInt(str.replaceAll("[^0-9]", ""));
        } catch (Exception e) {
            return 0;
        }
    }

    static class DropdownValue implements Comparable<DropdownValue> {
        List<String> options;
        String selected;

        DropdownValue(List<String> options) {
            this.options = options;
            if (options != null && !options.isEmpty()) {
                this.selected = options.get(0);
            } else {
                this.selected = "Latest version :)"; 
            }
        }

        @Override
        public String toString() { return selected; }

        @Override
        public int compareTo(DropdownValue o) { return this.selected.compareTo(o.selected); }
    }

    private static class ComboBoxCellEditor extends AbstractCellEditor implements TableCellEditor {
        private JComboBox<String> comboBox = new JComboBox<>();
        private DropdownValue currentValue;

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            comboBox.removeAllItems();
            if (value instanceof DropdownValue) {
                currentValue = (DropdownValue) value;
                if (currentValue.options == null || currentValue.options.isEmpty()) {
                    comboBox.addItem("Latest version :)");
                } else {
                    for (String opt : currentValue.options) {
                        comboBox.addItem(opt);
                    }
                    comboBox.setSelectedItem(currentValue.selected);
                }
            }
            
            comboBox.addActionListener(e -> {
                if (comboBox.getSelectedItem() != null) {
                    currentValue.selected = comboBox.getSelectedItem().toString();
                }
            });
            return comboBox;
        }

        @Override
        public Object getCellEditorValue() {
            return currentValue;
        }
    }
}
