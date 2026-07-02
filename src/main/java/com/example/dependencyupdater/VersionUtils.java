package com.example.dependencyupdater;

import java.util.List;
import java.util.stream.Collectors;

public class VersionUtils {

    public static List<String> filterStableLtsVersions(List<String> allVersions) {
        return allVersions.stream()
                .filter(VersionUtils::isStableVersion)
                .collect(Collectors.toList());
    }

    public static boolean isStableVersion(String version) {
        if (version == null) return false;
        String v = version.toLowerCase();
        
        // Common unstable keywords
        if (v.contains("alpha") || v.contains("beta") || v.contains("rc") ||
            v.contains("cr") || v.contains("snapshot") || v.contains("milestone") ||
            v.contains("preview") || v.contains("dev") || v.contains("test") ||
            v.contains("experimental") || v.contains("exp") ||
            v.contains("wip") || v.contains("tmp") ||
            v.contains("temp") || v.contains("internal")) {
            return false;
        }
        
        // Check short tags with boundaries (ea, pre, int, build, m, b, a, u)
        if (v.matches(".*[-._](ea|pre|int|build|m|b|a|u|rc|cr)([-._\\d]|$).*")) {
            return false;
        }
        
        // Match timestamped snapshots (e.g., 20260701.123456-1 or 20260701123456)
        if (v.matches(".*\\d{8}\\.?\\d{6}.*")) {
            return false;
        }
        
        return true;
    }
}
