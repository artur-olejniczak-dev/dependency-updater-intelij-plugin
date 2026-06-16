package com.example.dependencyupdater;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class VersionUtilsTest {
    @Test
    void testIsStableVersion() {
        assertTrue(VersionUtils.isStableVersion("1.0.0"));
        assertTrue(VersionUtils.isStableVersion("2.5.3.RELEASE"));
        assertTrue(VersionUtils.isStableVersion("4.1.100.Final"));
        assertTrue(VersionUtils.isStableVersion("v2.0"));
        assertFalse(VersionUtils.isStableVersion("1.0.0-rc1"));
        assertFalse(VersionUtils.isStableVersion("2.0-beta"));
        assertFalse(VersionUtils.isStableVersion("3.0.0.alpha2"));
        assertFalse(VersionUtils.isStableVersion("1.0-M1"));
        assertFalse(VersionUtils.isStableVersion("4.0.0-SNAPSHOT"));
    }
}
