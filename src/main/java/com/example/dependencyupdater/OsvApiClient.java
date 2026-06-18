package com.example.dependencyupdater;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.intellij.util.io.HttpRequests;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class OsvApiClient {
    public CompletableFuture<List<String>> checkVulnerabilities(String groupId, String artifactId, String version) {
        String url = "https://api.osv.dev/v1/query";
        String mavenPackage = groupId + ":" + artifactId;
        String requestBody = String.format(
            "{\"version\": \"%s\", \"package\": {\"name\": \"%s\", \"ecosystem\": \"Maven\"}}",
            version, mavenPackage
        );

        return CompletableFuture.supplyAsync(() -> {
            try {
                return HttpRequests.post(url, "application/json")
                    .connect(request -> {
                        request.write(requestBody);
                        return request.readString();
                    });
            } catch (Exception e) {
                return null;
            }
        }).thenApply(body -> {
            if (body != null) {
                return parseVulnerabilities(body);
            }
            return new ArrayList<>();
        });
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
