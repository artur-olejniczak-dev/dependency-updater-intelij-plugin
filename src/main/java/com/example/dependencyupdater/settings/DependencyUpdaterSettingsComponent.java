package com.example.dependencyupdater.settings;

import com.intellij.credentialStore.CredentialAttributes;
import com.intellij.credentialStore.CredentialAttributesKt;
import com.intellij.credentialStore.Credentials;
import com.intellij.ide.passwordSafe.PasswordSafe;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.CollectionListModel;
import com.intellij.ui.JBSplitter;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBPasswordField;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class DependencyUpdaterSettingsComponent {

    private final JPanel mainPanel;
    private final JBList<Repository> repositoryList;
    private final CollectionListModel<Repository> listModel;

    private final JBTextField nameField = new JBTextField();
    private final JBTextField urlField = new JBTextField();
    private final ComboBox<Repository.AuthType> authTypeComboBox = new ComboBox<>(Repository.AuthType.values());
    private final JBTextField usernameField = new JBTextField();
    private final JBPasswordField passwordField = new JBPasswordField();

    private JPanel detailsPanel;
    private boolean isUpdatingFields = false;

    public DependencyUpdaterSettingsComponent() {
        listModel = new CollectionListModel<>();
        repositoryList = new JBList<>(listModel);
        repositoryList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Repository) {
                    setText(((Repository) value).getName());
                }
                return c;
            }
        });

        JPanel listPanel = ToolbarDecorator.createDecorator(repositoryList)
                .setAddAction(button -> showAddPopupMenu(button.getContextComponent()))
                .setRemoveAction(button -> removeRepository())
                .createPanel();

        detailsPanel = createDetailsPanel();
        updateDetailsPanelVisibility(null);

        repositoryList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                Repository selected = repositoryList.getSelectedValue();
                updateDetailsPanelVisibility(selected);
            }
        });

        setupFieldListeners();

        JBSplitter splitter = new JBSplitter(false, 0.3f);
        splitter.setFirstComponent(listPanel);
        splitter.setSecondComponent(detailsPanel);

        mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(splitter, BorderLayout.CENTER);
    }

    private JPanel createDetailsPanel() {
        authTypeComboBox.addActionListener(e -> updateAuthFieldsVisibility());

        return FormBuilder.createFormBuilder()
                .addLabeledComponent("Name:", nameField)
                .addLabeledComponent("URL:", urlField)
                .addLabeledComponent("Auth Type:", authTypeComboBox)
                .addLabeledComponent("Username:", usernameField)
                .addLabeledComponent("Password / Token:", passwordField)
                .addComponentFillVertically(new JPanel(), 0)
                .getPanel();
    }

    private void updateAuthFieldsVisibility() {
        Repository.AuthType type = (Repository.AuthType) authTypeComboBox.getSelectedItem();
        boolean isBasic = type == Repository.AuthType.BASIC;
        boolean isBearer = type == Repository.AuthType.BEARER;
        
        usernameField.setEnabled(isBasic);
        passwordField.setEnabled(isBasic || isBearer);
    }

    private void updateDetailsPanelVisibility(Repository repo) {
        isUpdatingFields = true;
        try {
            if (repo == null) {
                nameField.setText("");
                urlField.setText("");
                authTypeComboBox.setSelectedItem(Repository.AuthType.NONE);
                usernameField.setText("");
                passwordField.setText("");
                detailsPanel.setVisible(false);
            } else {
                nameField.setText(repo.getName());
                urlField.setText(repo.getUrl());
                authTypeComboBox.setSelectedItem(repo.getAuthType());
                usernameField.setText(repo.getUsername() != null ? repo.getUsername() : "");
                
                String password = getPasswordFromSafe(repo.getId());
                passwordField.setText(password != null ? password : "");

                detailsPanel.setVisible(true);
                updateAuthFieldsVisibility();
            }
        } finally {
            isUpdatingFields = false;
        }
    }

    private void setupFieldListeners() {
        DocumentListener listener = new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { saveCurrentToModel(); }
            @Override public void removeUpdate(DocumentEvent e) { saveCurrentToModel(); }
            @Override public void changedUpdate(DocumentEvent e) { saveCurrentToModel(); }
        };

        nameField.getDocument().addDocumentListener(listener);
        urlField.getDocument().addDocumentListener(listener);
        usernameField.getDocument().addDocumentListener(listener);
        passwordField.getDocument().addDocumentListener(listener);
        authTypeComboBox.addActionListener(e -> saveCurrentToModel());
    }

    private void saveCurrentToModel() {
        if (isUpdatingFields) return;
        Repository selected = repositoryList.getSelectedValue();
        if (selected != null) {
            selected.setName(nameField.getText());
            selected.setUrl(urlField.getText());
            selected.setAuthType((Repository.AuthType) authTypeComboBox.getSelectedItem());
            selected.setUsername(usernameField.getText());
            
            savePasswordToSafe(selected.getId(), new String(passwordField.getPassword()));
            repositoryList.repaint();
        }
    }

    private void showAddPopupMenu(Component parent) {
        JPopupMenu popup = new JPopupMenu();
        
        JMenuItem customItem = new JMenuItem("Custom Repository...");
        customItem.addActionListener(e -> addRepository("New Custom Repository", "https://"));
        popup.add(customItem);
        
        popup.addSeparator();

        JMenuItem mavenItem = new JMenuItem("Maven Central");
        mavenItem.addActionListener(e -> addRepository("Maven Central", "https://repo1.maven.org/maven2/"));
        popup.add(mavenItem);

        JMenuItem googleItem = new JMenuItem("Google Maven");
        googleItem.addActionListener(e -> addRepository("Google Maven", "https://maven.google.com/"));
        popup.add(googleItem);

        JMenuItem jbossItem = new JMenuItem("JBoss Repository");
        jbossItem.addActionListener(e -> addRepository("JBoss Repository", "https://repository.jboss.org/nexus/content/groups/public/"));
        popup.add(jbossItem);

        JMenuItem springItem = new JMenuItem("Spring Plugins");
        springItem.addActionListener(e -> addRepository("Spring Plugins", "https://repo.spring.io/plugins-release/"));
        popup.add(springItem);

        JMenuItem gradleItem = new JMenuItem("Gradle Plugin Portal");
        gradleItem.addActionListener(e -> addRepository("Gradle Plugin Portal", "https://plugins.gradle.org/m2/"));
        popup.add(gradleItem);

        popup.show(parent, 0, parent.getHeight());
    }

    private void addRepository(String name, String url) {
        Repository repo = new Repository(name, url);
        listModel.add(repo);
        repositoryList.setSelectedValue(repo, true);
    }

    private void removeRepository() {
        int index = repositoryList.getSelectedIndex();
        if (index != -1) {
            Repository repo = listModel.getElementAt(index);
            removePasswordFromSafe(repo.getId());
            listModel.remove(index);
        }
    }

    public JPanel getPanel() {
        return mainPanel;
    }

    public List<Repository> getRepositories() {
        return listModel.getItems();
    }

    public void setRepositories(List<Repository> repos) {
        listModel.removeAll();
        for (Repository r : repos) {
            listModel.add(r.cloneRepo());
        }
        if (!listModel.isEmpty()) {
            repositoryList.setSelectedIndex(0);
        }
    }

    private CredentialAttributes createCredentialAttributes(String repoId) {
        return new CredentialAttributes(
                CredentialAttributesKt.generateServiceName("DependencyUpdater", repoId)
        );
    }

    private String getPasswordFromSafe(String repoId) {
        Credentials credentials = PasswordSafe.getInstance().get(createCredentialAttributes(repoId));
        return credentials != null ? credentials.getPasswordAsString() : null;
    }

    private void savePasswordToSafe(String repoId, String password) {
        if (password == null || password.isEmpty()) {
            removePasswordFromSafe(repoId);
            return;
        }
        Credentials credentials = new Credentials(repoId, password);
        PasswordSafe.getInstance().set(createCredentialAttributes(repoId), credentials);
    }

    private void removePasswordFromSafe(String repoId) {
        PasswordSafe.getInstance().set(createCredentialAttributes(repoId), null);
    }
}
