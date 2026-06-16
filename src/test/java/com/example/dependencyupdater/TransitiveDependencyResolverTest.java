package com.example.dependencyupdater;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
class TransitiveDependencyResolverTest {
    @Test
    void testParsePaths() {
        TransitiveDependencyResolver resolver = new TransitiveDependencyResolver();
        List<String> mockPaths = List.of(
            "C:/Users/mike/.gradle/caches/modules-2/files-2.1/org.springframework/spring-core/5.3.9/abcdef/spring-core-5.3.9.jar!/",
            "C:/Users/mike/.gradle/caches/modules-2/files-2.1/com.fasterxml.jackson.core/jackson-databind/2.14.0/xyz/jackson-databind-2.14.0.jar!/",
            "C:/Users/mike/.gradle/caches/modules-2/files-2.1/org.springframework/spring-core/5.3.9/12345/spring-core-5.3.9-sources.jar!/"
        );
        List<Dependency> parsed = resolver.parsePaths(mockPaths);
        assertEquals(2, parsed.size());
        assertEquals("org.springframework", parsed.get(0).getGroupId());
        assertEquals("spring-core", parsed.get(0).getArtifactId());
        assertEquals("5.3.9", parsed.get(0).getCurrentVersion());
        assertEquals("com.fasterxml.jackson.core", parsed.get(1).getGroupId());
        assertEquals("jackson-databind", parsed.get(1).getArtifactId());
        assertEquals("2.14.0", parsed.get(1).getCurrentVersion());
    }
    @Test
    void testFilterTransitive() {
        TransitiveDependencyResolver resolver = new TransitiveDependencyResolver();
        List<Dependency> allDependencies = List.of(
            new Dependency("org.springframework", "spring-core", "5.3.9"),
            new Dependency("org.springframework", "spring-web", "5.3.9"),
            new Dependency("com.fasterxml.jackson.core", "jackson-databind", "2.14.0")
        );
        List<Dependency> directDependencies = List.of(
            new Dependency("org.springframework", "spring-core", "5.3.9") 
        );
        List<Dependency> transitiveOnly = resolver.filterTransitive(allDependencies, directDependencies);
        assertEquals(2, transitiveOnly.size());
        assertEquals("spring-web", transitiveOnly.get(0).getArtifactId());
        assertEquals("jackson-databind", transitiveOnly.get(1).getArtifactId());
    }
}
