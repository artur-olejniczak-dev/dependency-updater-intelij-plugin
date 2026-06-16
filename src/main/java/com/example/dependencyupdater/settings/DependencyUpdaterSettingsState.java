package com.example.dependencyupdater.settings;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@State(
        name = "com.example.dependencyupdater.settings.DependencyUpdaterSettingsState",
        storages = @Storage("DependencyUpdaterSettingsPlugin.xml")
)
public class DependencyUpdaterSettingsState implements PersistentStateComponent<DependencyUpdaterSettingsState> {

    public List<Repository> repositories = new ArrayList<>();

    public DependencyUpdaterSettingsState() {
        if (repositories.isEmpty()) {
            Repository maven = new Repository("Maven Central", "https://repo1.maven.org/maven2/");
            maven.setAuthType(Repository.AuthType.NONE);
            repositories.add(maven);
        }
    }

    public static DependencyUpdaterSettingsState getInstance() {
        return ApplicationManager.getApplication().getService(DependencyUpdaterSettingsState.class);
    }

    @Nullable
    @Override
    public DependencyUpdaterSettingsState getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull DependencyUpdaterSettingsState state) {
        XmlSerializerUtil.copyBean(state, this);
    }
}
