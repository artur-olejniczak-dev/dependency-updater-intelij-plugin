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
    private volatile int currentScanId = 0; // Unikalny ID skanowania służący do anulowania/stopu

    private JBTable table;
    private DefaultTableModel tableModel;
    private List<Dependency> currentDependencies = new ArrayList<>();
    
    private JCheckBox ltsOnlyCheckBox;
    private JButton scanButton;
    private JButton updateButton;
    private JButton selectAllButton;
    private JButton stopButton; // Przycisk czyszczący

    public DependencyUpdaterPanel(Project project) {
        super(new BorderLayout());
        this.project = project;
        initUI();
    }

    private void initUI() {
        // Tabela
        String[] columnNames = {"Zaktualizuj?", "Biblioteka", "Obecna Wersja", "Dostępne Wersje", "Bezpieczeństwo"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 0) return Boolean.class;
                if (columnIndex == 3) return DropdownValue.class;
                return String.class;
            }
            @Override
            public boolean isCellEditable(int row, int column) {
                if (column == 0) return true; // Zawsze można klikać checkbox
                if (column == 3) {
                    Object val = getValueAt(row, 3);
                    // Pole 3 (dropdown) jest wyłączone dla najnowszych paczek lub błędów
                    if (val instanceof DropdownValue && ((DropdownValue) val).options.isEmpty()) return false;
                    return true;
                }
                return false;
            }
        };

        table = new JBTable(tableModel);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getColumnModel().getColumn(0).setMaxWidth(80);
        
        // Sortowanie w dwie strony
        table.setAutoCreateRowSorter(true);
        // Pogrubienie nagłówków
        table.getTableHeader().setFont(table.getTableHeader().getFont().deriveFont(Font.BOLD));

        // Niestandardowy edytor dla JComboBox w kolumnie 3
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

        JBScrollPane scrollPane = new JBScrollPane(table);
        add(scrollPane, BorderLayout.CENTER);

        // Toolbar podzielony na lewą i prawą stronę
        JPanel topPanel = new JPanel(new BorderLayout());
        JPanel leftToolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JPanel rightToolbar = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        
        scanButton = new JButton("Skanuj Zależności");
        stopButton = new JButton("Wyczyść / Stop"); // Nowy przycisk
        updateButton = new JButton("Zaktualizuj Zaznaczone");
        selectAllButton = new JButton("Zaznacz Wszystkie");
        ltsOnlyCheckBox = new JCheckBox("Tylko LTS", true);

        scanButton.addActionListener(e -> performScan());
        stopButton.addActionListener(e -> stopAndClear());
        updateButton.addActionListener(e -> performUpdate());
        selectAllButton.addActionListener(e -> selectAllRows());
        ltsOnlyCheckBox.addActionListener(e -> performScan());

        leftToolbar.add(scanButton);
        leftToolbar.add(stopButton);
        leftToolbar.add(selectAllButton);
        leftToolbar.add(ltsOnlyCheckBox);
        
        rightToolbar.add(updateButton); // Przycisk po prawej
        
        topPanel.add(leftToolbar, BorderLayout.WEST);
        topPanel.add(rightToolbar, BorderLayout.EAST);
        
        add(topPanel, BorderLayout.NORTH);
    }
    
    private void stopAndClear() {
        currentScanId++; // Unieważnia wszelkie trwające w tle zapytania!
        tableModel.setRowCount(0);
        currentDependencies.clear();
        scanButton.setEnabled(true);
    }

    private void selectAllRows() {
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            Object val = tableModel.getValueAt(i, 3);
            if (val instanceof DropdownValue) {
                if (!((DropdownValue) val).options.isEmpty() && !((DropdownValue) val).options.contains("Błąd połączenia")) {
                    tableModel.setValueAt(true, i, 0);
                }
            }
        }
        table.repaint();
    }

    private void performScan() {
        int myScanId = ++currentScanId;
        scanButton.setEnabled(false);
        tableModel.setRowCount(0);

        // Wyciąganie plików używa indeksów w IntelliJ 2025+, a to rzuca błędem jeśli odpalimy to w głównym wątku graficznym (EDT).
        com.intellij.openapi.application.ApplicationManager.getApplication().executeOnPooledThread(() -> {
            com.intellij.openapi.application.ApplicationManager.getApplication().runReadAction(() -> {
                if (myScanId != currentScanId) return; // Przerwano!
                
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
                    if (myScanId != currentScanId) return; // Przerwano!
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
                .exceptionally(ex -> new ArrayList<>()) // Jeśli OSV zawiedzie, zwracamy pustą listę
                .thenAccept(vulns -> {
                    if (myScanId == currentScanId) {
                        dep.setVulnerabilities(vulns);
                    }
                });
            
            CompletableFuture<Void> mavenFuture = mavenApi.fetchAvailableVersions(dep.getGroupId(), dep.getArtifactId())
                .exceptionally(ex -> new ArrayList<>()) // Jeśli główny strzał po wersje padnie, nie blokujemy tabeli!
                .thenCompose(versions -> {
                    if (myScanId != currentScanId) return CompletableFuture.completedFuture(null);
                    
                    if (versions == null || versions.isEmpty()) {
                        // Obsługa błędu dla tej pojedynczej biblioteki (reszta będzie działać poprawnie)
                        dep.setAvailableVersions(List.of("Błąd połączenia"));
                        return CompletableFuture.completedFuture(null);
                    }

                    // Filtrujemy wyższe wersje, SORTUJEMY OD NAJWYŻSZEJ, a następnie OGRANICZAMY do 10
                    List<String> higherVersions = versions.stream()
                            .filter(v -> compareVersions(v, dep.getCurrentVersion()) > 0)
                            .sorted((v1, v2) -> compareVersions(v2, v1)) // Sortowanie malejąco
                            .limit(10)
                            .collect(Collectors.toList());

                    // Dla każdej wyższej wersji robimy asynchroniczny test podatności, zabezpieczając go przed wyjątkiem
                    List<CompletableFuture<String>> versionFutures = new ArrayList<>();
                    for (String v : higherVersions) {
                        CompletableFuture<String> vf = osvApi.checkVulnerabilities(dep.getGroupId(), dep.getArtifactId(), v)
                            .exceptionally(ex -> new ArrayList<>()) // Zabezpieczenie przed błędem 429/timeoutem!
                            .thenApply(vulns -> {
                                String tag = VersionUtils.isStableVersion(v) ? " (LTS)" : " (BETA)";
                                if (vulns == null || vulns.isEmpty()) {
                                    return v + tag;
                                } else {
                                    return v + tag + " ⚠️ " + vulns.size() + " podatności";
                                }
                            });
                        versionFutures.add(vf);
                    }

                    return CompletableFuture.allOf(versionFutures.toArray(new CompletableFuture[0]))
                        .exceptionally(ex -> null) // Ochrona całej grupy zapytań
                        .thenAccept(vVoid -> {
                            if (myScanId != currentScanId) return; // Przerwano
                            
                            List<String> mappedVersions = versionFutures.stream()
                                    .map(f -> {
                                        try { return f.get(); } catch (Exception e) { return "Błąd"; }
                                    })
                                    .filter(val -> !val.equals("Błąd"))
                                    .collect(Collectors.toList());
                            
                            if (mappedVersions.isEmpty()) {
                                dep.setAvailableVersions(List.of("Najnowsza mordo :)"));
                                return;
                            }
                            
                            if (ltsOnlyCheckBox.isSelected()) {
                                List<String> ltsOnly = mappedVersions.stream().filter(v -> v.contains("(LTS)")).collect(Collectors.toList());
                                if (ltsOnly.isEmpty()) ltsOnly = List.of("Brak nowszych wersji LTS");
                                dep.setAvailableVersions(ltsOnly);
                            } else {
                                dep.setAvailableVersions(mappedVersions);
                            }
                        });
                });

            futures.add(osvFuture);
            futures.add(mavenFuture);
        }

        // Czekamy na wszystko. Niezależnie od błędów poszczególnych komponentów, wymuszamy odświeżenie interfejsu (exceptionally zwraca null omijając zawieszenie)
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
            String vulnText = "✅ Bezpieczna";
            if (hasVulnerabilities) {
                List<String> vulns = dep.getVulnerabilities();
                if (vulns.size() <= 3) {
                    vulnText = "⚠️ Podatności: " + String.join(", ", vulns);
                } else {
                    vulnText = "⚠️ UWAGA: " + vulns.size() + " podatności! (" + String.join(", ", vulns.subList(0, 3)) + "...)";
                }
            }
            
            DropdownValue dropdown = new DropdownValue(dep.getAvailableVersions());

            tableModel.addRow(new Object[]{
                    hasVulnerabilities && dropdown.options != null && !dropdown.options.isEmpty() && !dropdown.options.get(0).contains("Błąd") && !dropdown.options.get(0).contains("Brak"),
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
                
                if (newVersionWithTag.equals("Najnowsza mordo :)") || newVersionWithTag.contains("Brak nowszych") || newVersionWithTag.contains("Błąd")) continue; 
                
                // Usuwamy z tekstu "(LTS)" i spajające spacje, żeby przekazać czysty numer wersji
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
                JOptionPane.showMessageDialog(this, "Zaktualizowano pliki Gradle. Może być konieczne kliknięcie 'Load Gradle Changes' w IDE.");
            } else {
                MavenProjectsManager.getInstance(project).forceUpdateAllProjectsOrFindAllAvailablePomFiles();
                JOptionPane.showMessageDialog(this, "Zaktualizowano pliki pom.xml i rozpoczęto przeładowanie Mavena.");
            }
            performScan(); 
        } else {
            JOptionPane.showMessageDialog(this, "Nie zaznaczono żadnych bibliotek wymagających aktualizacji.");
        }
    }

    private int compareVersions(String v1, String v2) {
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

    private int parseInt(String str) {
        try {
            return Integer.parseInt(str.replaceAll("[^0-9]", ""));
        } catch (Exception e) {
            return 0;
        }
    }

    private static class DropdownValue implements Comparable<DropdownValue> {
        List<String> options;
        String selected;

        DropdownValue(List<String> options) {
            this.options = options;
            if (options != null && !options.isEmpty()) {
                this.selected = options.get(0);
            } else {
                this.selected = "Najnowsza mordo :)"; 
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
                    comboBox.addItem("Najnowsza mordo :)");
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
