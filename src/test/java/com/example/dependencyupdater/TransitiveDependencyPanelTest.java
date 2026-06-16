package com.example.dependencyupdater;

import com.intellij.openapi.project.Project;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TransitiveDependencyPanelTest {

    @Test
    void testCompareVersions() {
        Project mockProject = Mockito.mock(Project.class);
        TransitiveDependencyPanel panel = new TransitiveDependencyPanel(mockProject);

        assertTrue(panel.compareVersions("2.14.1", "2.14.0") > 0);
        assertTrue(panel.compareVersions("3.0", "3.0.0") == 0); 
        assertTrue(panel.compareVersions("1.2.3", "1.2.4") < 0);
        
        // Protection against text suffixes (often found in Gradle cache paths)
        assertTrue(panel.compareVersions("2.0.0-rc1", "1.9.9") > 0);
    }

    @Test
    void testParseInt() {
        Project mockProject = Mockito.mock(Project.class);
        TransitiveDependencyPanel panel = new TransitiveDependencyPanel(mockProject);

        assertEquals(2, panel.parseInt("2"));
        assertEquals(14, panel.parseInt("14-beta"));
        assertEquals(0, panel.parseInt("unknown")); 
    }

    @Test
    void testDropdownValue() {
        List<String> options = List.of("2.14.2", "2.14.1");
        TransitiveDependencyPanel.DropdownValue dropdown = new TransitiveDependencyPanel.DropdownValue(options);
        
        assertEquals("2.14.2", dropdown.selected);
        assertEquals("2.14.2", dropdown.toString());
        
        TransitiveDependencyPanel.DropdownValue emptyDropdown = new TransitiveDependencyPanel.DropdownValue(null);
        assertEquals("Latest version :)", emptyDropdown.selected);
    }
}
