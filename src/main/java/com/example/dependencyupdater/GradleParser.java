package com.example.dependencyupdater;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GradleParser {

    // Wyłapuje: 'group:artifact:version' lub "group:artifact:$version"
    private static final Pattern DEPENDENCY_PATTERN = Pattern.compile("['\"]([a-zA-Z0-9.\\-_]+):([a-zA-Z0-9.\\-_]+):([^'\"]+)['\"]");

    public List<Dependency> extractDependencies(Project project) {
        List<Dependency> dependencies = new ArrayList<>();
        Collection<VirtualFile> gradleFiles = getGradleFiles(project);

        for (VirtualFile file : gradleFiles) {
            String content = readFileContent(file);
            Matcher matcher = DEPENDENCY_PATTERN.matcher(content);

            while (matcher.find()) {
                String groupId = matcher.group(1);
                String artifactId = matcher.group(2);
                String versionRaw = matcher.group(3);

                Dependency dep = new Dependency(groupId, artifactId, "");

                // Weryfikacja czy wersja jest zmienną np. $version lub ${version}
                if (versionRaw.startsWith("$")) {
                    dep.setVariable(true);
                    String varName = versionRaw.replace("${", "").replace("}", "").replace("$", "");
                    dep.setVariableName(varName);
                    
                    // Szukamy przypisanej wartości zmiennej w całym projekcie
                    String resolvedVersion = resolveVariableValue(project, varName);
                    if (resolvedVersion != null) {
                        dep = new Dependency(groupId, artifactId, resolvedVersion);
                        dep.setVariable(true);
                        dep.setVariableName(varName);
                        dependencies.add(dep);
                    }
                } else {
                    dep = new Dependency(groupId, artifactId, versionRaw);
                    dependencies.add(dep);
                }
            }
        }
        return dependencies;
    }

    private String resolveVariableValue(Project project, String variableName) {
        // Szukaj w gradle.properties
        for (VirtualFile propFile : FilenameIndex.getVirtualFilesByName("gradle.properties", GlobalSearchScope.projectScope(project))) {
            String content = readFileContent(propFile);
            Matcher m = Pattern.compile("^\\s*" + Pattern.quote(variableName) + "\\s*=\\s*(.*?)\\s*$", Pattern.MULTILINE).matcher(content);
            if (m.find()) return m.group(1);
        }

        // Szukaj w build.gradle (np. ext.version = '1.0' lub set('version', '1.0') lub val version = '1.0')
        for (VirtualFile gradleFile : getGradleFiles(project)) {
            String content = readFileContent(gradleFile);
            
            // Standardowe przypisanie: ext.var = '1.0' lub var = "1.0"
            Matcher m1 = Pattern.compile(Pattern.quote(variableName) + "\\s*=\\s*['\"](.*?)['\"]").matcher(content);
            if (m1.find()) return m1.group(1);

            // Funkcja set(): set('var', '1.0')
            Matcher m2 = Pattern.compile("set\\s*\\(\\s*['\"]" + Pattern.quote(variableName) + "['\"]\\s*,\\s*['\"](.*?)['\"]\\s*\\)").matcher(content);
            if (m2.find()) return m2.group(1);
        }
        return null; // Nie znaleziono wartości
    }

    public void updateDependencyVersion(Project project, @NotNull Dependency dependency, @NotNull String newVersion) {
        WriteCommandAction.runWriteCommandAction(project, "Update Gradle Dependency", "DependencyUpdaterPlugin", () -> {
            if (dependency.isVariable()) {
                // Aktualizujemy zmienną we wszystkich plikach (properties i gradle)
                updateVariableInFiles(project, dependency.getVariableName(), newVersion);
            } else {
                // Aktualizujemy twardy ciąg w plikach gradle
                updateDirectStringInFiles(project, dependency, newVersion);
            }
        });
    }

    private void updateVariableInFiles(Project project, String variableName, String newVersion) {
        // properties
        for (VirtualFile propFile : FilenameIndex.getVirtualFilesByName("gradle.properties", GlobalSearchScope.projectScope(project))) {
            modifyFile(propFile, content -> {
                return content.replaceAll(
                    "(?m)^(\\s*" + Pattern.quote(variableName) + "\\s*=\\s*).*?(\\s*)$",
                    "$1" + newVersion + "$2"
                );
            });
        }

        // gradle
        for (VirtualFile gradleFile : getGradleFiles(project)) {
            modifyFile(gradleFile, content -> {
                // Przypisanie = 
                String updated = content.replaceAll(
                    "(" + Pattern.quote(variableName) + "\\s*=\\s*['\"])(.*?)(['\"])",
                    "$1" + newVersion + "$3"
                );
                // set()
                updated = updated.replaceAll(
                    "(set\\s*\\(\\s*['\"]" + Pattern.quote(variableName) + "['\"]\\s*,\\s*['\"])(.*?)(['\"]\\s*\\))",
                    "$1" + newVersion + "$3"
                );
                return updated;
            });
        }
    }

    private void updateDirectStringInFiles(Project project, Dependency dependency, String newVersion) {
        for (VirtualFile gradleFile : getGradleFiles(project)) {
            modifyFile(gradleFile, content -> {
                String searchTarget = dependency.getGroupId() + ":" + dependency.getArtifactId() + ":" + dependency.getCurrentVersion();
                String replacement = dependency.getGroupId() + ":" + dependency.getArtifactId() + ":" + newVersion;
                return content.replace(searchTarget, replacement);
            });
        }
    }

    private Collection<VirtualFile> getGradleFiles(Project project) {
        Collection<VirtualFile> files = new ArrayList<>();
        files.addAll(FilenameIndex.getVirtualFilesByName("build.gradle", GlobalSearchScope.projectScope(project)));
        files.addAll(FilenameIndex.getVirtualFilesByName("build.gradle.kts", GlobalSearchScope.projectScope(project)));
        return files;
    }

    private String readFileContent(VirtualFile file) {
        Document document = FileDocumentManager.getInstance().getDocument(file);
        return document != null ? document.getText() : "";
    }

    private void modifyFile(VirtualFile file, java.util.function.Function<String, String> modifier) {
        Document document = FileDocumentManager.getInstance().getDocument(file);
        if (document != null) {
            String originalContent = document.getText();
            String newContent = modifier.apply(originalContent);
            if (!originalContent.equals(newContent)) {
                document.setText(newContent);
            }
        }
    }
}
