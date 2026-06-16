package com.example.dependencyupdater;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class PomParser {

    /**
     * Wyszukuje wszystkie pliki pom.xml w projekcie i wyciąga z nich zadeklarowane zależności.
     */
    public List<Dependency> extractDependencies(Project project) {
        List<Dependency> dependencies = new ArrayList<>();
        
        PsiFile[] pomFiles = FilenameIndex.getFilesByName(project, "pom.xml", GlobalSearchScope.projectScope(project));

        for (PsiFile psiFile : pomFiles) {
            if (psiFile instanceof XmlFile) {
                XmlFile xmlFile = (XmlFile) psiFile;
                XmlTag rootTag = xmlFile.getRootTag();
                if (rootTag != null) {
                    // 1. Regular dependencies
                    extractFromDependenciesTag(rootTag.findFirstSubTag("dependencies"), dependencies, xmlFile);
                    
                    // 2. Dependency management
                    XmlTag depMgmtTag = rootTag.findFirstSubTag("dependencyManagement");
                    if (depMgmtTag != null) {
                        extractFromDependenciesTag(depMgmtTag.findFirstSubTag("dependencies"), dependencies, xmlFile);
                    }
                }
            }
        }
        return dependencies;
    }

    private void extractFromDependenciesTag(XmlTag dependenciesTag, List<Dependency> dependencies, XmlFile xmlFile) {
        if (dependenciesTag == null) return;
        
        for (XmlTag depTag : dependenciesTag.findSubTags("dependency")) {
            String groupId = getSubTagText(depTag, "groupId");
            String artifactId = getSubTagText(depTag, "artifactId");
            String versionRaw = getSubTagText(depTag, "version");

            if (groupId != null && artifactId != null && versionRaw != null) {
                // Resolve variables, e.g. ${spring.version}
                if (versionRaw.startsWith("${") && versionRaw.endsWith("}")) {
                    String propName = versionRaw.substring(2, versionRaw.length() - 1);
                    String resolvedVersion = resolveProperty(xmlFile, propName);
                    
                    if (resolvedVersion != null) {
                        Dependency dep = new Dependency(groupId, artifactId, resolvedVersion);
                        dep.setVariable(true);
                        dep.setVariableName(propName);
                        dependencies.add(dep);
                    }
                } else if (!versionRaw.startsWith("$")) { // Regular text string, not another unsupported variable
                    dependencies.add(new Dependency(groupId, artifactId, versionRaw));
                }
            }
        }
    }

    private String resolveProperty(XmlFile xmlFile, String propName) {
        XmlTag rootTag = xmlFile.getRootTag();
        if (rootTag != null) {
            XmlTag propertiesTag = rootTag.findFirstSubTag("properties");
            if (propertiesTag != null) {
                XmlTag propTag = propertiesTag.findFirstSubTag(propName);
                if (propTag != null) {
                    return propTag.getValue().getTrimmedText();
                }
            }
        }
        return null;
    }

    /**
     * Bezpiecznie modyfikuje plik pom.xml aktualizując wersję wskazanej zależności (lub jej zmienną).
     */
    public void updateDependencyVersion(Project project, @NotNull Dependency dependency, @NotNull String newVersion) {
        PsiFile[] pomFiles = FilenameIndex.getFilesByName(project, "pom.xml", GlobalSearchScope.projectScope(project));

        WriteCommandAction.runWriteCommandAction(project, "Update Dependency Version", "DependencyUpdaterPlugin", () -> {
            for (PsiFile psiFile : pomFiles) {
                if (psiFile instanceof XmlFile) {
                    XmlFile xmlFile = (XmlFile) psiFile;
                    
                    if (dependency.isVariable()) {
                        // Update in the <properties> block
                        updatePropertyTag(xmlFile, dependency.getVariableName(), newVersion);
                    } else {
                        // Hard update in the <dependency> block
                        updateDependencyTag(xmlFile, dependency, newVersion);
                    }
                }
            }
        });
    }

    private void updatePropertyTag(XmlFile xmlFile, String propName, String newVersion) {
        XmlTag rootTag = xmlFile.getRootTag();
        if (rootTag != null) {
            XmlTag propertiesTag = rootTag.findFirstSubTag("properties");
            if (propertiesTag != null) {
                XmlTag propTag = propertiesTag.findFirstSubTag(propName);
                if (propTag != null) {
                    propTag.getValue().setText(newVersion);
                }
            }
        }
    }

    private void updateDependencyTag(XmlFile xmlFile, Dependency dependency, String newVersion) {
        XmlTag rootTag = xmlFile.getRootTag();
        if (rootTag != null) {
            updateInDependenciesTag(rootTag.findFirstSubTag("dependencies"), dependency, newVersion);
            
            XmlTag depMgmtTag = rootTag.findFirstSubTag("dependencyManagement");
            if (depMgmtTag != null) {
                updateInDependenciesTag(depMgmtTag.findFirstSubTag("dependencies"), dependency, newVersion);
            }
        }
    }

    private void updateInDependenciesTag(XmlTag dependenciesTag, Dependency dependency, String newVersion) {
        if (dependenciesTag == null) return;
        
        for (XmlTag depTag : dependenciesTag.findSubTags("dependency")) {
            String groupId = getSubTagText(depTag, "groupId");
            String artifactId = getSubTagText(depTag, "artifactId");

            if (dependency.getGroupId().equals(groupId) && dependency.getArtifactId().equals(artifactId)) {
                XmlTag versionTag = depTag.findFirstSubTag("version");
                if (versionTag != null && !versionTag.getValue().getTrimmedText().startsWith("${")) {
                    versionTag.getValue().setText(newVersion);
                }
            }
        }
    }

    private String getSubTagText(XmlTag parentTag, String subTagName) {
        XmlTag subTag = parentTag.findFirstSubTag(subTagName);
        return subTag != null ? subTag.getValue().getTrimmedText() : null;
    }
}
