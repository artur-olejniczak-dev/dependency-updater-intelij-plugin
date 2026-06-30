import sys

with open('src/main/java/com/example/dependencyupdater/GradleParser.java', 'r', encoding='utf-8') as f:
    content = f.read()

old_method = '''    public void updateTransitiveDependencies(Project project, java.util.List<Dependency> dependencies, java.util.List<String> newVersions) {
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

new_method = '''    public void updateTransitiveDependencies(Project project, java.util.List<Dependency> dependencies, java.util.List<String> newVersions) {
        if (dependencies.isEmpty()) return;
        
        for (VirtualFile gradleFile : getGradleFiles(project)) {
            modifyFile(gradleFile, content -> {
                StringBuilder block = new StringBuilder();
                
                boolean isKts = gradleFile.getName().endsWith(".kts");
                if (isKts) {
                    block.append("\\next {\\n");
                } else {
                    block.append("\\next {\\n");
                }
                
                boolean addedAny = false;
                for (int i = 0; i < dependencies.size(); i++) {
                    Dependency dep = dependencies.get(i);
                    String propName = dep.getArtifactId() + ".version";
                    String version = newVersions.get(i);
                    
                    // Basic check if already exists
                    if (content.contains(propName) && content.contains(version)) {
                        continue;
                    }
                    
                    addedAny = true;
                    if (isKts) {
                        block.append("    set(\\"").append(propName).append("\\", \\"").append(version).append("\\")\\n");
                    } else {
                        block.append("    set('").append(propName).append("', '").append(version).append("')\\n");
                    }
                }
                block.append("}\\n");
                
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

