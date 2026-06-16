package com.example.dependencyupdater;

import com.intellij.openapi.command.WriteCommandAction;

import com.intellij.openapi.editor.Document;

import com.intellij.openapi.fileEditor.FileDocumentManager;

import com.intellij.openapi.project.Project;

import com.intellij.openapi.vfs.VirtualFile;

import com.intellij.psi.search.FilenameIndex;

import com.intellij.psi.search.GlobalSearchScope;

import org.jetbrains.annotations.NotNull;

import java.util.*;

import java.util.regex.Matcher;

import java.util.regex.Pattern;

public class GradleParser {

    static final Pattern DEPENDENCY_PATTERN = Pattern.compile("['\"]([a-zA-Z0-9.\\-_]+):([a-zA-Z0-9.\\-_]+):([^'\"]+)['\"]");

    static final Pattern MAP_DEPENDENCY_PATTERN = Pattern.compile("group\\s*:\\s*['\"]([a-zA-Z0-9.\\-_]+)['\"]\\s*,\\s*name\\s*:\\s*['\"]([a-zA-Z0-9.\\-_]+)['\"]\\s*,\\s*version\\s*:\\s*([^,\\)\\}\\]\\s]+)");

    static final Pattern TOML_LIBRARY_REF_PATTERN = Pattern.compile("([a-zA-Z0-9.\\-_]+)\\s*=\\s*\\{\\s*module\\s*=\\s*['\"]([a-zA-Z0-9.\\-_]+):([a-zA-Z0-9.\\-_]+)['\"]\\s*,\\s*version\\.ref\\s*=\\s*['\"](.*?)['\"]");

    static final Pattern TOML_VERSION_PATTERN = Pattern.compile("^\\s*([a-zA-Z0-9.\\-_]+)\\s*=\\s*['\"]([^'\"]+)['\"]", Pattern.MULTILINE);

    public List<Dependency> extractDependencies(Project project) {

        List<Dependency> dependencies = new ArrayList<>();

        Collection<VirtualFile> gradleFiles = getGradleFiles(project);

        for (VirtualFile file : gradleFiles) {

            String content = readFileContent(file);

            Matcher matcher = DEPENDENCY_PATTERN.matcher(content);

            while (matcher.find()) {

                handleMatchedVersion(matcher.group(1), matcher.group(2), matcher.group(3), project, dependencies);

            }

            Matcher mapMatcher = MAP_DEPENDENCY_PATTERN.matcher(content);

            while (mapMatcher.find()) {

                String versionRaw = mapMatcher.group(3).trim();

                if (versionRaw.startsWith("'") || versionRaw.startsWith("\"")) {

                    versionRaw = versionRaw.replaceAll("['\"]", "");

                } else {

                    if (!versionRaw.startsWith("$")) versionRaw = "$" + versionRaw; 

                }

                handleMatchedVersion(mapMatcher.group(1), mapMatcher.group(2), versionRaw, project, dependencies);

            }

        }

        extractFromToml(project, dependencies);

        return dependencies;

    }

    private void handleMatchedVersion(String groupId, String artifactId, String versionRaw, Project project, List<Dependency> dependencies) {

        if (versionRaw.startsWith("$")) {

            String varName = versionRaw.replace("${", "").replace("}", "").replace("$", "");

            String resolvedVersion = resolveVariableValue(project, varName);

            if (resolvedVersion != null) {

                Dependency dep = new Dependency(groupId, artifactId, resolvedVersion);

                dep.setVariable(true);

                dep.setVariableName(varName);

                dependencies.add(dep);

            }

        } else {

            dependencies.add(new Dependency(groupId, artifactId, versionRaw));

        }

    }

    private void extractFromToml(Project project, List<Dependency> dependencies) {

        Collection<VirtualFile> tomlFiles = FilenameIndex.getVirtualFilesByName("libs.versions.toml", GlobalSearchScope.projectScope(project));

        for (VirtualFile toml : tomlFiles) {

            String content = readFileContent(toml);

            Map<String, String> versionsMap = new HashMap<>();

            Matcher versionMatcher = TOML_VERSION_PATTERN.matcher(content);

            while (versionMatcher.find()) {

                versionsMap.put(versionMatcher.group(1), versionMatcher.group(2));

            }

            Matcher libMatcher = TOML_LIBRARY_REF_PATTERN.matcher(content);

            while (libMatcher.find()) {

                String groupId = libMatcher.group(2);

                String artifactId = libMatcher.group(3);

                String versionRef = libMatcher.group(4);

                if (versionsMap.containsKey(versionRef)) {

                    Dependency dep = new Dependency(groupId, artifactId, versionsMap.get(versionRef));

                    dep.setVariable(true);

                    dep.setVariableName(versionRef);

                    dependencies.add(dep);

                }

            }

        }

    }

    private String resolveVariableValue(Project project, String variableName) {

        for (VirtualFile propFile : FilenameIndex.getVirtualFilesByName("gradle.properties", GlobalSearchScope.projectScope(project))) {

            String content = readFileContent(propFile);

            Matcher m = Pattern.compile("^\\s*" + Pattern.quote(variableName) + "\\s*=\\s*(.*?)\\s*$", Pattern.MULTILINE).matcher(content);

            if (m.find()) return m.group(1);

        }

        for (VirtualFile gradleFile : getGradleFiles(project)) {

            String content = readFileContent(gradleFile);

            Matcher m1 = Pattern.compile(Pattern.quote(variableName) + "\\s*=\\s*['\"](.*?)['\"]").matcher(content);

            if (m1.find()) return m1.group(1);

            Matcher m2 = Pattern.compile("set\\s*\\(\\s*['\"]" + Pattern.quote(variableName) + "['\"]\\s*,\\s*['\"](.*?)['\"]\\s*\\)").matcher(content);

            if (m2.find()) return m2.group(1);

        }

        return null;

    }

    public void updateDependencyVersion(Project project, @NotNull Dependency dependency, @NotNull String newVersion) {

        WriteCommandAction.runWriteCommandAction(project, "Update Gradle Dependency", "DependencyUpdaterPlugin", () -> {

            if (dependency.isVariable()) {

                updateVariableInFiles(project, dependency.getVariableName(), newVersion);

                updateTomlVersionRef(project, dependency.getVariableName(), newVersion);

            } else {

                updateDirectStringInFiles(project, dependency, newVersion);

            }

        });

    }

    private void updateVariableInFiles(Project project, String variableName, String newVersion) {

        for (VirtualFile propFile : FilenameIndex.getVirtualFilesByName("gradle.properties", GlobalSearchScope.projectScope(project))) {

            modifyFile(propFile, content -> content.replaceAll(

                "(?m)^(\\s*" + Pattern.quote(variableName) + "\\s*=\\s*).*?(\\s*)$",

                "$1" + newVersion + "$2"

            ));

        }

        for (VirtualFile gradleFile : getGradleFiles(project)) {

            modifyFile(gradleFile, content -> {

                String updated = content.replaceAll(

                    "(" + Pattern.quote(variableName) + "\\s*=\\s*['\"])(.*?)(['\"])",

                    "$1" + newVersion + "$3"

                );

                updated = updated.replaceAll(

                    "(set\\s*\\(\\s*['\"]" + Pattern.quote(variableName) + "['\"]\\s*,\\s*['\"])(.*?)(['\"]\\s*\\))",

                    "$1" + newVersion + "$3"

                );

                return updated;

            });

        }

    }

    private void updateTomlVersionRef(Project project, String versionRef, String newVersion) {

        for (VirtualFile toml : FilenameIndex.getVirtualFilesByName("libs.versions.toml", GlobalSearchScope.projectScope(project))) {

            modifyFile(toml, content -> content.replaceAll(

                "(?m)^(\\s*" + Pattern.quote(versionRef) + "\\s*=\\s*['\"])(.*?)(['\"]\\s*)$",

                "$1" + newVersion + "$3"

            ));

        }

    }

    private void updateDirectStringInFiles(Project project, Dependency dependency, String newVersion) {

        for (VirtualFile gradleFile : getGradleFiles(project)) {

            modifyFile(gradleFile, content -> {

                String searchTarget = dependency.getGroupId() + ":" + dependency.getArtifactId() + ":" + dependency.getCurrentVersion();

                String replacement = dependency.getGroupId() + ":" + dependency.getArtifactId() + ":" + newVersion;

                String updated = content.replace(searchTarget, replacement);

                String mapSearchTarget = "group\\s*:\\s*['\"]" + Pattern.quote(dependency.getGroupId()) + "['\"]\\s*,\\s*name\\s*:\\s*['\"]" + Pattern.quote(dependency.getArtifactId()) + "['\"]\\s*,\\s*version\\s*:\\s*['\"]" + Pattern.quote(dependency.getCurrentVersion()) + "['\"]";

                String mapReplacement = "group: '" + dependency.getGroupId() + "', name: '" + dependency.getArtifactId() + "', version: '" + newVersion + "'";

                updated = updated.replaceAll(mapSearchTarget, mapReplacement);

                return updated;

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
