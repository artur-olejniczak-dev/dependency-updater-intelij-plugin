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
import java.util.Collection;
import java.util.List;

public class PomParser {

    /**
     * Wyszukuje wszystkie pliki pom.xml w projekcie i wyciąga z nich zadeklarowane zależności.
     */
    public List<Dependency> extractDependencies(Project project) {
        List<Dependency> dependencies = new ArrayList<>();
        
        // Szukamy wszystkich plików pom.xml w projekcie
        PsiFile[] pomFiles = FilenameIndex.getFilesByName(project, "pom.xml", GlobalSearchScope.projectScope(project));

        for (PsiFile psiFile : pomFiles) {
            if (psiFile instanceof XmlFile) {
                XmlFile xmlFile = (XmlFile) psiFile;
                XmlTag rootTag = xmlFile.getRootTag();
                if (rootTag != null) {
                    XmlTag dependenciesTag = rootTag.findFirstSubTag("dependencies");
                    if (dependenciesTag != null) {
                        for (XmlTag depTag : dependenciesTag.findSubTags("dependency")) {
                            String groupId = getSubTagText(depTag, "groupId");
                            String artifactId = getSubTagText(depTag, "artifactId");
                            String version = getSubTagText(depTag, "version");

                            // Ignorujemy zależności bez jawnej wersji (np. zarządzane przez dependencyManagement) dla uproszczenia MVP
                            if (groupId != null && artifactId != null && version != null && !version.startsWith("$")) {
                                dependencies.add(new Dependency(groupId, artifactId, version));
                            }
                        }
                    }
                }
            }
        }
        return dependencies;
    }

    /**
     * Bezpiecznie modyfikuje plik pom.xml aktualizując wersję wskazanej zależności.
     */
    public void updateDependencyVersion(Project project, @NotNull Dependency dependency, @NotNull String newVersion) {
        PsiFile[] pomFiles = FilenameIndex.getFilesByName(project, "pom.xml", GlobalSearchScope.projectScope(project));

        // Modyfikacje PSI muszą być objęte WriteCommandAction, aby IntelliJ obsługiwało "Undo" (Ctrl+Z) i blokady odczytu
        WriteCommandAction.runWriteCommandAction(project, "Update Dependency Version", "DependencyUpdaterPlugin", () -> {
            for (PsiFile psiFile : pomFiles) {
                if (psiFile instanceof XmlFile) {
                    XmlFile xmlFile = (XmlFile) psiFile;
                    XmlTag rootTag = xmlFile.getRootTag();
                    if (rootTag != null) {
                        XmlTag dependenciesTag = rootTag.findFirstSubTag("dependencies");
                        if (dependenciesTag != null) {
                            for (XmlTag depTag : dependenciesTag.findSubTags("dependency")) {
                                String groupId = getSubTagText(depTag, "groupId");
                                String artifactId = getSubTagText(depTag, "artifactId");

                                if (dependency.getGroupId().equals(groupId) && dependency.getArtifactId().equals(artifactId)) {
                                    XmlTag versionTag = depTag.findFirstSubTag("version");
                                    if (versionTag != null) {
                                        // Modyfikacja kodu (AST/PSI)
                                        versionTag.getValue().setText(newVersion);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        });
    }

    private String getSubTagText(XmlTag parentTag, String subTagName) {
        XmlTag subTag = parentTag.findFirstSubTag(subTagName);
        return subTag != null ? subTag.getValue().getTrimmedText() : null;
    }
}
