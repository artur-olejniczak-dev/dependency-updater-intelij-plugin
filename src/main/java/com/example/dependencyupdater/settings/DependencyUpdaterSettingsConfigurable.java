package com.example.dependencyupdater.settings;

import com.intellij.openapi.options.Configurable;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.List;

public class DependencyUpdaterSettingsConfigurable implements Configurable {

    private DependencyUpdaterSettingsComponent mySettingsComponent;

    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return "Dependency Updater";
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        mySettingsComponent = new DependencyUpdaterSettingsComponent();
        return mySettingsComponent.getPanel();
    }

    @Override
    public boolean isModified() {
        DependencyUpdaterSettingsState state = DependencyUpdaterSettingsState.getInstance();
        List<Repository> currentRepos = mySettingsComponent.getRepositories();
        return !currentRepos.equals(state.repositories);
    }

    @Override
    public void apply() {
        DependencyUpdaterSettingsState state = DependencyUpdaterSettingsState.getInstance();
        state.repositories.clear();
        for (Repository r : mySettingsComponent.getRepositories()) {
            state.repositories.add(r.cloneRepo());
        }
    }

    @Override
    public void reset() {
        DependencyUpdaterSettingsState state = DependencyUpdaterSettingsState.getInstance();
        mySettingsComponent.setRepositories(state.repositories);
    }

    @Override
    public void disposeUIResources() {
        mySettingsComponent = null;
    }
}
