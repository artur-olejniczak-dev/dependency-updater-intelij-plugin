package com.example.dependencyupdater;

import org.junit.jupiter.api.Test;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MavenCentralApiClientTest {

    @Test
    void testParseVersions() {
        MavenCentralApiClient client = new MavenCentralApiClient();
        
        String xmlResponse = "<metadata>\n" +
                             "  <versioning>\n" +
                             "    <versions>\n" +
                             "      <version>1.0.0</version>\n" +
                             "      <version>1.5.0</version>\n" +
                             "      <version>2.0.0</version>\n" +
                             "    </versions>\n" +
                             "  </versioning>\n" +
                             "</metadata>";

        List<String> versions = client.parseVersions(xmlResponse);
        
        assertEquals(3, versions.size());
        assertEquals("1.0.0", versions.get(0));
        assertEquals("1.5.0", versions.get(1));
        assertEquals("2.0.0", versions.get(2));
    }

    @Test
    void testParseVersionsWithMalformedXml() {
        MavenCentralApiClient client = new MavenCentralApiClient();
        
        String badXml = "<versions><version>2.0</vers"; // urwany
        List<String> versions = client.parseVersions(badXml);
        
        // Z regexem po prostu nie złapie błędnego fragmentu
        assertTrue(versions.isEmpty());
    }
}
