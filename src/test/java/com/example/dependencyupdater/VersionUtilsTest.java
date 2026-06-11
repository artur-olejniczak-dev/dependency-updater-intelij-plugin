package com.example.dependencyupdater;

import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class VersionUtilsTest {

    @Test
    void testIsStableVersion() {
        assertTrue(VersionUtils.isStableVersion("1.0.0"));
        assertTrue(VersionUtils.isStableVersion("2.14.1"));
        assertTrue(VersionUtils.isStableVersion("3.0.0.RELEASE"));
        assertTrue(VersionUtils.isStableVersion("5.3.9"));
        
        assertFalse(VersionUtils.isStableVersion("1.0.0-alpha"));
        assertFalse(VersionUtils.isStableVersion("2.0.0-BETA2"));
        assertFalse(VersionUtils.isStableVersion("1.2.3-SNAPSHOT"));
        assertFalse(VersionUtils.isStableVersion("5.0.0.RC1"));
        assertFalse(VersionUtils.isStableVersion("4.0.0.M2"));
    }

    @Test
    void testFilterStableLtsVersions() {
        List<String> mixedVersions = Arrays.asList(
                "1.0.0", "1.1.0-alpha", "1.1.0-beta", "1.1.0"
        );
        List<String> stableOnly = VersionUtils.filterStableLtsVersions(mixedVersions);
        
        assertEquals(2, stableOnly.size());
        assertTrue(stableOnly.contains("1.0.0"));
        assertTrue(stableOnly.contains("1.1.0"));
    }
}
