package com.example.dependencyupdater;

import com.intellij.openapi.project.Project;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DependencyUpdaterPanelTest {

    @Test
    void testCompareVersions() {
        Project mockProject = Mockito.mock(Project.class);
        DependencyUpdaterPanel panel = new DependencyUpdaterPanel(mockProject);

        assertTrue(panel.compareVersions("5.3.10", "5.3.9") > 0);
        assertTrue(panel.compareVersions("6.0.0", "5.9.9") > 0);
        assertTrue(panel.compareVersions("2.1", "2.1.0") == 0); // 2.1 vs 2.1.0 are the same
        assertTrue(panel.compareVersions("1.0.0", "1.0.1") < 0);
        
        // With textual suffixes (Release, etc)
        assertTrue(panel.compareVersions("3.0.0-RELEASE", "2.9.9") > 0);
        assertTrue(panel.compareVersions("2.1.0-M1", "2.1.0") > 0); 
    }

    @Test
    void testParseInt() {
        Project mockProject = Mockito.mock(Project.class);
        DependencyUpdaterPanel panel = new DependencyUpdaterPanel(mockProject);

        assertEquals(5, panel.parseInt("5"));
        assertEquals(10, panel.parseInt("10-RELEASE"));
        assertEquals(0, panel.parseInt("abc")); // Fallback to 0 on error
    }

    @Test
    void testDropdownValue() {
        List<String> options = List.of("6.0.0", "5.3.10");
        DependencyUpdaterPanel.DropdownValue dropdown = new DependencyUpdaterPanel.DropdownValue(options);
        
        assertEquals("6.0.0", dropdown.selected);
        assertEquals("6.0.0", dropdown.toString());
        
        DependencyUpdaterPanel.DropdownValue emptyDropdown = new DependencyUpdaterPanel.DropdownValue(null);
        assertEquals("Latest version :)", emptyDropdown.selected);
        
        // Test compareTo
        DependencyUpdaterPanel.DropdownValue drop1 = new DependencyUpdaterPanel.DropdownValue(List.of("1.0.0"));
        DependencyUpdaterPanel.DropdownValue drop2 = new DependencyUpdaterPanel.DropdownValue(List.of("2.0.0"));
        assertTrue(drop1.compareTo(drop2) < 0);
    }
}
