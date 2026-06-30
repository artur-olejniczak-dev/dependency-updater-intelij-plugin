import sys

with open('src/main/java/com/example/dependencyupdater/GradleParser.java', 'r', encoding='utf-8') as f:
    content = f.read()

method_code = '''
    public void updateTransitiveDependency(Project project, Dependency dependency, String newVersion) {
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
    }
'''

last_brace_index = content.rfind('}')
content = content[:last_brace_index] + method_code + '\n}'

with open('src/main/java/com/example/dependencyupdater/GradleParser.java', 'w', encoding='utf-8') as f:
    f.write(content)
