package com.example.dependencyupdater;

import java.util.List;
import java.util.stream.Collectors;

public class VersionUtils {

    /**
     * Zwraca tylko stabilne i potencjalnie długoterminowe (LTS) wersje biblioteki,
     * odrzucając wydania developerskie, testowe i "Release Candidate".
     */
    public static List<String> filterStableLtsVersions(List<String> allVersions) {
        return allVersions.stream()
                .filter(VersionUtils::isStableVersion)
                .collect(Collectors.toList());
    }

    public static boolean isStableVersion(String version) {
        String v = version.toLowerCase();
        // Odrzucamy typowe suffixy wersji niestabilnych
        return !v.contains("alpha") &&
               !v.contains("beta") &&
               !v.contains("rc") &&
               !v.contains("snapshot") &&
               !v.contains("m1") &&
               !v.contains("m2") &&
               !v.contains("m3") &&
               !v.contains("milestone");
    }
}
