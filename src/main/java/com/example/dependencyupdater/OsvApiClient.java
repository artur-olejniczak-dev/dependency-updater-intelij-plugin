package com.example.dependencyupdater;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class OsvApiClient {

    private final HttpClient httpClient = HttpClient.newHttpClient();

    public CompletableFuture<List<String>> checkVulnerabilities(String groupId, String artifactId, String version) {
        String url = "https://api.osv.dev/v1/query";
        String mavenPackage = groupId + ":" + artifactId;
        
        // Ciało zapytania JSON dla OSV API
        String requestBody = String.format(
            "{\"version\": \"%s\", \"package\": {\"name\": \"%s\", \"ecosystem\": \"Maven\"}}",
            version, mavenPackage
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenApply(this::parseVulnerabilities);
    }

    List<String> parseVulnerabilities(String jsonBody) {
        List<String> vulns = new ArrayList<>();
        try {
            JsonObject jsonObject = JsonParser.parseString(jsonBody).getAsJsonObject();
            if (jsonObject.has("vulns")) {
                JsonArray vulnsArray = jsonObject.getAsJsonArray("vulns");
                for (JsonElement el : vulnsArray) {
                    JsonObject vuln = el.getAsJsonObject();
                    String bestId = null;
                    // Szukamy aliasu CVE, bo programiści wolą CVE niż wewnętrzne ID GitHuba (GHSA)
                    if (vuln.has("aliases")) {
                        JsonArray aliases = vuln.getAsJsonArray("aliases");
                        for (JsonElement alias : aliases) {
                            String aliasStr = alias.getAsString();
                            if (aliasStr.startsWith("CVE-")) {
                                bestId = aliasStr;
                                break;
                            }
                        }
                    }
                    // Fallback na domyślne ID (np. GHSA) jeśli brak odpowiednika CVE
                    if (bestId == null && vuln.has("id")) {
                        bestId = vuln.get("id").getAsString();
                    }
                    
                    if (bestId != null) {
                        vulns.add(bestId);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return vulns;
    }
}
