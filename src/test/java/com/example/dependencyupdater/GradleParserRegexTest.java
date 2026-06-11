package com.example.dependencyupdater;

import org.junit.jupiter.api.Test;
import java.util.regex.Matcher;

import static org.junit.jupiter.api.Assertions.*;

class GradleParserRegexTest {

    @Test
    void testStandardDependencyPattern() {
        String content = "implementation 'org.springframework:spring-core:5.3.9'\n" +
                         "testImplementation \"junit:junit:$junitVersion\"";

        Matcher m1 = GradleParser.DEPENDENCY_PATTERN.matcher(content);
        assertTrue(m1.find());
        assertEquals("org.springframework", m1.group(1));
        assertEquals("spring-core", m1.group(2));
        assertEquals("5.3.9", m1.group(3));

        assertTrue(m1.find());
        assertEquals("junit", m1.group(1));
        assertEquals("junit", m1.group(2));
        assertEquals("$junitVersion", m1.group(3));
    }

    @Test
    void testMapDependencyPattern() {
        String content = "implementation group: 'org.apache.commons', name: 'commons-lang3', version: '3.0'\n" +
                         "implementation group: 'com.fasterxml', name: 'jackson', version: jacksonVar";

        Matcher m1 = GradleParser.MAP_DEPENDENCY_PATTERN.matcher(content);
        assertTrue(m1.find());
        assertEquals("org.apache.commons", m1.group(1));
        assertEquals("commons-lang3", m1.group(2));
        assertEquals("'3.0'", m1.group(3));

        assertTrue(m1.find());
        assertEquals("com.fasterxml", m1.group(1));
        assertEquals("jackson", m1.group(2));
        assertEquals("jacksonVar", m1.group(3));
    }

    @Test
    void testTomlPatterns() {
        String toml = "[versions]\n" +
                      "retrofit = \"2.9.0\"\n" +
                      "[libraries]\n" +
                      "retrofit = { module = \"com.squareup.retrofit2:retrofit\", version.ref = \"retrofit\" }";

        Matcher mVer = GradleParser.TOML_VERSION_PATTERN.matcher(toml);
        assertTrue(mVer.find());
        assertEquals("retrofit", mVer.group(1));
        assertEquals("2.9.0", mVer.group(2));

        Matcher mLib = GradleParser.TOML_LIBRARY_REF_PATTERN.matcher(toml);
        assertTrue(mLib.find());
        assertEquals("retrofit", mLib.group(1)); // alias
        assertEquals("com.squareup.retrofit2", mLib.group(2)); // group
        assertEquals("retrofit", mLib.group(3)); // artifact
        assertEquals("retrofit", mLib.group(4)); // version.ref
    }
}
