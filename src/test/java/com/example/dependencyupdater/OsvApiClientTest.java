package com.example.dependencyupdater;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;

class OsvApiClientTest {

    private final OsvApiClient client = new OsvApiClient();

    @Test
    void testVulnerableLibrary() throws ExecutionException, InterruptedException, TimeoutException {
        // Sprawdzamy podatną paczkę: Log4j 2.14.1 (Log4Shell)
        CompletableFuture<List<String>> future = client.checkVulnerabilities("org.apache.logging.log4j", "log4j-core", "2.14.1");
        
        // Blokujemy na max 5 sekund by zapobiec wiecznemu zawieszeniu w testach
        List<String> vulnerabilities = future.get(5, TimeUnit.SECONDS);
        
        assertNotNull(vulnerabilities);
        assertFalse(vulnerabilities.isEmpty(), "Log4j 2.14.1 powinno posiadać zidentyfikowane podatności!");
        assertTrue(vulnerabilities.contains("CVE-2021-44228") || vulnerabilities.contains("CVE-2021-45046") || vulnerabilities.stream().anyMatch(v -> v.contains("GHSA")));
    }

    @Test
    void testSafeLibrary() throws ExecutionException, InterruptedException, TimeoutException {
        // Nowa, (oby) bezpieczna wersja log4j
        CompletableFuture<List<String>> future = client.checkVulnerabilities("org.apache.logging.log4j", "log4j-core", "2.23.1");
        
        List<String> vulnerabilities = future.get(5, TimeUnit.SECONDS);
        
        assertNotNull(vulnerabilities);
        assertTrue(vulnerabilities.isEmpty(), "Log4j 2.23.1 nie powinno mieć znanych krytycznych luk dla testu OSV.");
    }
}
