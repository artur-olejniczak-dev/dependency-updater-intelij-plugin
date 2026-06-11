package com.example.dependencyupdater;

import org.junit.jupiter.api.Test;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MavenCentralApiClientTest {

    @Test
    void testParseVersions() {
        MavenCentralApiClient client = new MavenCentralApiClient();
        
        String jsonResponse = "{\n" +
                              "  \"response\": {\n" +
                              "    \"docs\": [\n" +
                              "      {\"v\": \"2.0.0\"},\n" +
                              "      {\"v\": \"1.5.0\"},\n" +
                              "      {\"v\": \"1.0.0\"}\n" +
                              "    ]\n" +
                              "  }\n" +
                              "}";

        List<String> versions = client.parseVersions(jsonResponse);
        
        assertEquals(3, versions.size());
        assertEquals("2.0.0", versions.get(0));
        assertEquals("1.5.0", versions.get(1));
        assertEquals("1.0.0", versions.get(2));
    }

    @Test
    void testParseVersionsWithMalformedJson() {
        MavenCentralApiClient client = new MavenCentralApiClient();
        
        String badJson = "{ \"malformed\": ";
        List<String> versions = client.parseVersions(badJson);
        
        // Funkcja powinna wyłapać wyjątek i zwrócić bezpiecznie pustą listę zamiast crashować
        assertTrue(versions.isEmpty());
    }
}
