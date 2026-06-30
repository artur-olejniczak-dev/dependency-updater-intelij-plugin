package com.example.dependencyupdater;
import java.util.ArrayList;
import java.util.List;
public class Dependency {
    private String groupId;
    private String artifactId;
    private String currentVersion;
    private List<String> availableVersions = new ArrayList<>();
    private List<String> vulnerabilities = new ArrayList<>();
    private boolean isVariable = false;
    private String variableName = null;
    private boolean isPlugin = false;

    public Dependency(String groupId, String artifactId, String currentVersion) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.currentVersion = currentVersion;
    }

    public String getGroupId() { return groupId; }
    public String getArtifactId() { return artifactId; }
    public String getCurrentVersion() { return currentVersion; }
    public List<String> getAvailableVersions() { return availableVersions; }
    public List<String> getVulnerabilities() { return vulnerabilities; }
    public void setAvailableVersions(List<String> availableVersions) {
        this.availableVersions = availableVersions;
    }
    public void setVulnerabilities(List<String> vulnerabilities) {
        this.vulnerabilities = vulnerabilities;
    }
    public boolean isVariable() { return isVariable; }
    public void setVariable(boolean variable) { isVariable = variable; }
    public String getVariableName() { return variableName; }
    public void setVariableName(String variableName) { this.variableName = variableName; }
    
    public boolean isPlugin() { return isPlugin; }
    public void setPlugin(boolean plugin) { isPlugin = plugin; }

    public String getCoordinates() {
        return groupId + ":" + artifactId;
    }
}
