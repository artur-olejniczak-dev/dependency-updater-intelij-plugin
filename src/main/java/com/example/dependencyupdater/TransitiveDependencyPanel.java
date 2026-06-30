package com.example.dependencyupdater;

import com.intellij.openapi.project.Project;

import com.intellij.ui.components.JBScrollPane;

import com.intellij.ui.table.JBTable;

import javax.swing.*;

import javax.swing.table.DefaultTableCellRenderer;

import javax.swing.table.DefaultTableModel;

import javax.swing.table.TableCellEditor;

import java.awt.*;

import java.util.ArrayList;

import java.util.List;

import java.util.concurrent.CompletableFuture;

import java.util.stream.Collectors;

public class TransitiveDependencyPanel extends JPanel {

    private final Project project;

    private final GradleParser gradleParser = new GradleParser();

    private final TransitiveDependencyResolver resolver = new TransitiveDependencyResolver();

    private final MavenCentralApiClient mavenApi = new MavenCentralApiClient();

    private final OsvApiClient osvApi = new OsvApiClient();

    private volatile int currentScanId = 0;

    private JBTable table;

    private DefaultTableModel tableModel;

    private List<Dependency> currentTransitiveDependencies = new ArrayList<>();

    private JButton scanButton;

    private JButton stopButton;

    public TransitiveDependencyPanel(Project project) {

        super(new BorderLayout());

        this.project = project;

        initUI();

    }

    private void initUI() {

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
                if (column == 0 || column == 3) return true;
                if (column == 1 || column == 2 || column == 4) return true;
                return false;
            }

        };

        table = new JBTable(tableModel);

        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        table.setAutoCreateRowSorter(true);

        table.getTableHeader().setFont(table.getTableHeader().getFont().deriveFont(Font.BOLD));

        // CheckBox renderer for Update column
        table.getColumnModel().getColumn(0).setMaxWidth(110);
        table.getColumnModel().getColumn(0).setMinWidth(90);
        table.getColumnModel().getColumn(0).setPreferredWidth(100);
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
                if (value == null) return emptyLabel;
                checkBox.setSelected((Boolean) value);
                checkBox.setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
                return checkBox;
            }
        });

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

        JTextField readOnlyField = new JTextField();
        readOnlyField.setEditable(false);
        readOnlyField.setBorder(null);
        DefaultCellEditor readOnlyEditor = new DefaultCellEditor(readOnlyField);

        table.getColumnModel().getColumn(1).setCellEditor(readOnlyEditor);
        table.getColumnModel().getColumn(2).setCellEditor(readOnlyEditor);
        table.getColumnModel().getColumn(4).setCellEditor(readOnlyEditor);

        JBScrollPane scrollPane = new JBScrollPane(table);

        add(scrollPane, BorderLayout.CENTER);

        JPanel topPanel = new JPanel(new BorderLayout());

        JPanel leftToolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));

        scanButton = new JButton("Scan Transitive Dependencies (Gradle)");
        stopButton = new JButton("Clear / Stop");
        JButton updateButton = new JButton("Update Selected");
        
        scanButton.addActionListener(e -> performScan());
        stopButton.addActionListener(e -> stopAndClear());
        updateButton.addActionListener(e -> performUpdate());
        
        leftToolbar.add(scanButton);
        leftToolbar.add(stopButton);
        
        JLabel infoLabel = new JLabel(" (Detects vulnerabilities in hidden background packages.)");
        infoLabel.setForeground(Color.GRAY);
        leftToolbar.add(infoLabel);
        
        JPanel rightToolbar = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        rightToolbar.add(updateButton);
        
        topPanel.add(leftToolbar, BorderLayout.WEST);
        topPanel.add(rightToolbar, BorderLayout.EAST);
        add(topPanel, BorderLayout.NORTH);
    }


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
                gradleParser.updateTransitiveDependencies(project, toUpdate, newVersions);
            });
            JOptionPane.showMessageDialog(this, "Transitive dependencies updated successfully! Please refresh Gradle.", "Success", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void stopAndClear() {

        currentScanId++; 

        tableModel.setRowCount(0);

        currentTransitiveDependencies.clear();

        scanButton.setEnabled(true);

    }

    private void performScan() {

        int myScanId = ++currentScanId;

        scanButton.setEnabled(false);

        tableModel.setRowCount(0);

        com.intellij.openapi.application.ApplicationManager.getApplication().executeOnPooledThread(() -> {

            com.intellij.openapi.application.ApplicationManager.getApplication().runReadAction(() -> {

                if (myScanId != currentScanId) return;

                List<Dependency> direct = gradleParser.extractDependencies(project);

                List<Dependency> extracted = resolver.extractTransitiveDependencies(project, direct);

                SwingUtilities.invokeLater(() -> {

                    if (myScanId != currentScanId) return;

                    currentTransitiveDependencies = extracted;

                    startApiQueries(myScanId);

                });

            });

        });

    }

    private void startApiQueries(int myScanId) {

        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (Dependency dep : currentTransitiveDependencies) {

            CompletableFuture<Void> osvFuture = osvApi.checkVulnerabilities(dep.getGroupId(), dep.getArtifactId(), dep.getCurrentVersion())

                .exceptionally(ex -> new ArrayList<>())

                .thenAccept(vulns -> {

                    if (myScanId == currentScanId) {

                        dep.setVulnerabilities(vulns);

                    }

                });

            futures.add(osvFuture);

        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))

            .exceptionally(ex -> null)

            .thenRun(() -> {

                if (myScanId != currentScanId) return;

                List<Dependency> vulnerableDeps = currentTransitiveDependencies.stream()

                        .filter(d -> !d.getVulnerabilities().isEmpty())

                        .collect(Collectors.toList());

                currentTransitiveDependencies = vulnerableDeps; 

                fetchAvailableVersionsForVulnerable(myScanId);

            });

    }

    private void fetchAvailableVersionsForVulnerable(int myScanId) {

        if (currentTransitiveDependencies.isEmpty()) {

            SwingUtilities.invokeLater(() -> {

                if (myScanId == currentScanId) {

                    scanButton.setEnabled(true);

                    JOptionPane.showMessageDialog(this, "Congratulations! Your background dependencies have no security vulnerabilities.");

                }

            });

            return;

        }

        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (Dependency dep : currentTransitiveDependencies) {

            CompletableFuture<Void> mavenFuture = mavenApi.fetchAvailableVersions(dep.getGroupId(), dep.getArtifactId())

                .exceptionally(ex -> new ArrayList<>())

                .thenCompose(versions -> {

                    if (myScanId != currentScanId) return CompletableFuture.completedFuture(null);

                    if (versions == null || versions.isEmpty()) {

                        dep.setAvailableVersions(List.of("Connection Error"));

                        return CompletableFuture.completedFuture(null);

                    }

                    List<String> higherVersions = versions.stream()

                            .filter(v -> compareVersions(v, dep.getCurrentVersion()) > 0)

                            .sorted((v1, v2) -> compareVersions(v2, v1))

                            .limit(10)

                            .collect(Collectors.toList());

                    List<CompletableFuture<String>> versionFutures = new ArrayList<>();

                    for (String v : higherVersions) {

                        CompletableFuture<String> vf = osvApi.checkVulnerabilities(dep.getGroupId(), dep.getArtifactId(), v)

                            .exceptionally(ex -> new ArrayList<>())

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

                        .exceptionally(ex -> null)

                        .thenAccept(vVoid -> {

                            if (myScanId != currentScanId) return;

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

                            dep.setAvailableVersions(mappedVersions);

                        });

                });

            futures.add(mavenFuture);

        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))

            .exceptionally(ex -> null)

            .thenRun(() -> SwingUtilities.invokeLater(() -> {

                if (myScanId == currentScanId) {

                    refreshTable();

                }

            }));

    }

    private void refreshTable() {

        for (Dependency dep : currentTransitiveDependencies) {

            String vulnText = "⚠️ " + dep.getVulnerabilities().size() + " vulnerabilities: " + String.join(", ", dep.getVulnerabilities());

            DropdownValue dropdown = new DropdownValue(dep.getAvailableVersions());

            tableModel.addRow(new Object[]{
                    Boolean.FALSE,
                    dep.getCoordinates(),
                    dep.getCurrentVersion(),
                    dropdown,
                    vulnText
            });

        }

        scanButton.setEnabled(true);

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
