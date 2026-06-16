package com.example.dependencyupdater;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
class DependencyTest {
    @Test
    void testDependencyGettersAndSetters() {
        Dependency dep = new Dependency("org.springframework", "spring-core", "5.3.9");
        assertEquals("org.springframework", dep.getGroupId());
        assertEquals("spring-core", dep.getArtifactId());
        assertEquals("5.3.9", dep.getCurrentVersion());
        assertEquals("org.springframework:spring-core", dep.getCoordinates());
        dep.setAvailableVersions(List.of("5.3.10", "6.0.0"));
        assertEquals(2, dep.getAvailableVersions().size());
        assertEquals("6.0.0", dep.getAvailableVersions().get(1));
        dep.setVulnerabilities(List.of("CVE-2021-22112"));
        assertEquals(1, dep.getVulnerabilities().size());
        assertEquals("CVE-2021-22112", dep.getVulnerabilities().get(0));
    }
}
