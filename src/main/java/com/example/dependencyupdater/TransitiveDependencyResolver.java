package com.example.dependencyupdater;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.OrderEnumerator;
import com.intellij.openapi.vfs.VirtualFile;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TransitiveDependencyResolver {

    // Przechwytuje np: .../files-2.1/com.fasterxml.jackson.core/jackson-databind/2.14.0/hash/...
    private static final Pattern GRADLE_CACHE_PATTERN = Pattern.compile("files-2\\.1[\\\\/]([^\\\\/]+)[\\\\/]([^\\\\/]+)[\\\\/]([^\\\\/]+)");

    public List<Dependency> extractTransitiveDependencies(Project project, List<Dependency> directDependencies) {
        // Magia API IntelliJ: Zwraca absolutnie wszystkie biblioteki, jakie Gradle zarejestrował w projekcie (zbudowany graf)
        VirtualFile[] files = OrderEnumerator.orderEntries(project)
                .withoutSdk()
                .librariesOnly()
                .classes()
                .getRoots();

        List<String> paths = new ArrayList<>();
        for (VirtualFile file : files) {
            paths.add(file.getPath());
        }

        List<Dependency> allDependencies = parsePaths(paths);
        return filterTransitive(allDependencies, directDependencies);
    }

    List<Dependency> parsePaths(List<String> paths) {
        List<Dependency> parsed = new ArrayList<>();
        for (String path : paths) {
            Matcher m = GRADLE_CACHE_PATTERN.matcher(path);
            if (m.find()) {
                String groupId = m.group(1);
                String artifactId = m.group(2);
                String version = m.group(3);
                
                boolean alreadyAdded = parsed.stream().anyMatch(d -> 
                    d.getGroupId().equals(groupId) && d.getArtifactId().equals(artifactId));
                
                if (!alreadyAdded) {
                    parsed.add(new Dependency(groupId, artifactId, version));
                }
            }
        }
        return parsed;
    }

    List<Dependency> filterTransitive(List<Dependency> allDependencies, List<Dependency> directDependencies) {
        List<Dependency> transitiveOnly = new ArrayList<>();
        for (Dependency dep : allDependencies) {
            boolean isDirect = directDependencies.stream().anyMatch(direct -> 
                direct.getGroupId().equals(dep.getGroupId()) && direct.getArtifactId().equals(dep.getArtifactId())
            );
            if (!isDirect) {
                transitiveOnly.add(dep);
            }
        }
        return transitiveOnly;
    }
}
