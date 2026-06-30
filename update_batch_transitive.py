import sys

# GradleParser.java
with open('src/main/java/com/example/dependencyupdater/GradleParser.java', 'r', encoding='utf-8') as f:
    content = f.read()

old_method = '''    public void updateTransitiveDependency(Project project, Dependency dependency, String newVersion) {
        String coord = dependency.getGroupId() + ":" + dependency.getArtifactId() + ":" + newVersion;
        
        for (VirtualFile gradleFile : getGradleFiles(project)) {
            modifyFile(gradleFile, content -> {
                String toAppend;
                if (gradleFile.getName().endsWith(".kts")) {
                    toAppend = "\\nconfigurations.all {\\n    resolutionStrategy {\\n        force(\\"" + coord + "\\")\\n    }\\n}\\n";
                } else {
                    toAppend = "\\nconfigurations.all {\\n    resolutionStrategy {\\n        force '" + coord + "'\\n    }\\n}\\n";
                }
                
                if (content.contains("force '" + coord + "'") || content.contains("force(\\"" + coord + "\\")")) {
                    return content;
                }
                return content + toAppend;
            });
        }
    }'''

new_method = '''    public void updateTransitiveDependencies(Project project, java.util.List<Dependency> dependencies, java.util.List<String> newVersions) {
        if (dependencies.isEmpty()) return;
        
        for (VirtualFile gradleFile : getGradleFiles(project)) {
            modifyFile(gradleFile, content -> {
                StringBuilder block = new StringBuilder();
                block.append("\\nconfigurations.all {\\n    resolutionStrategy {\\n");
                
                boolean addedAny = false;
                for (int i = 0; i < dependencies.size(); i++) {
                    Dependency dep = dependencies.get(i);
                    String coord = dep.getGroupId() + ":" + dep.getArtifactId() + ":" + newVersions.get(i);
                    
                    if (content.contains("force '" + coord + "'") || content.contains("force(\\"" + coord + "\\")")) {
                        continue; // Already there
                    }
                    
                    addedAny = true;
                    if (gradleFile.getName().endsWith(".kts")) {
                        block.append("        force(\"").append(coord).append("\")\\n");
                    } else {
                        block.append("        force '").append(coord).append("'\\n");
                    }
                }
                block.append("    }\\n}\\n");
                
                if (!addedAny) {
                    return content;
                }
                return content + block.toString();
            });
        }
    }'''

content = content.replace(old_method, new_method)
with open('src/main/java/com/example/dependencyupdater/GradleParser.java', 'w', encoding='utf-8') as f:
    f.write(content)

# TransitiveDependencyPanel.java
with open('src/main/java/com/example/dependencyupdater/TransitiveDependencyPanel.java', 'r', encoding='utf-8') as f:
    content = f.read()

old_loop = '''            com.intellij.openapi.command.WriteCommandAction.runWriteCommandAction(project, () -> {
                for (int i = 0; i < toUpdate.size(); i++) {
                    gradleParser.updateTransitiveDependency(project, toUpdate.get(i), newVersions.get(i));
                }
            });'''

new_loop = '''            com.intellij.openapi.command.WriteCommandAction.runWriteCommandAction(project, () -> {
                gradleParser.updateTransitiveDependencies(project, toUpdate, newVersions);
            });'''

content = content.replace(old_loop, new_loop)
with open('src/main/java/com/example/dependencyupdater/TransitiveDependencyPanel.java', 'w', encoding='utf-8') as f:
    f.write(content)

