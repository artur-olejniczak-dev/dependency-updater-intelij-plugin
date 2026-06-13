package com.example.dependencyupdater;

import org.junit.jupiter.api.Test;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OsvApiClientTest {

    @Test
    void testParseVulnerabilitiesWithCVE() {
        OsvApiClient client = new OsvApiClient();
        
        String jsonResponse = "{\n" +
                              "  \"vulns\": [\n" +
                              "    {\n" +
                              "      \"id\": \"GHSA-1234\",\n" +
                              "      \"aliases\": [\"CVE-2023-1001\", \"SONATYPE-2023-01\"]\n" +
                              "    }\n" +
                              "  ]\n" +
                              "}";

        List<String> vulns = client.parseVulnerabilities(jsonResponse);
        
        assertEquals(1, vulns.size());
        assertEquals("CVE-2023-1001", vulns.get(0)); // Powinno preferować CVE
    }

    @Test
    void testParseVulnerabilitiesWithoutCVE() {
        OsvApiClient client = new OsvApiClient();
        
        String jsonResponse = "{\n" +
                              "  \"vulns\": [\n" +
                              "    {\n" +
                              "      \"id\": \"GHSA-5678\",\n" +
                              "      \"aliases\": [\"OSV-2023-02\"]\n" +
                              "    }\n" +
                              "  ]\n" +
                              "}";

        List<String> vulns = client.parseVulnerabilities(jsonResponse);
        
        assertEquals(1, vulns.size());
        assertEquals("GHSA-5678", vulns.get(0)); // Fallback na główne ID
    }

    @Test
    void testParseVulnerabilitiesEmpty() {
        OsvApiClient client = new OsvApiClient();
        
        String jsonResponse = "{}"; // Brak obiektu vulns

        List<String> vulns = client.parseVulnerabilities(jsonResponse);
        
        assertTrue(vulns.isEmpty());
    }
}
