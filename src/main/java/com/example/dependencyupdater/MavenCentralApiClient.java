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

public class MavenCentralApiClient {

    private final HttpClient httpClient = HttpClient.newHttpClient();

    public CompletableFuture<List<String>> fetchAvailableVersions(String groupId, String artifactId) {
        String url = String.format("https://search.maven.org/solrsearch/select?q=g:%%22%s%%22+AND+a:%%22%s%%22&core=gav&rows=50&wt=json", 
                                   groupId, artifactId);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenApply(this::parseVersions);
    }

    private List<String> parseVersions(String jsonBody) {
        List<String> versions = new ArrayList<>();
        try {
            JsonObject jsonObject = JsonParser.parseString(jsonBody).getAsJsonObject();
            JsonObject response = jsonObject.getAsJsonObject("response");
            JsonArray docs = response.getAsJsonArray("docs");

            for (JsonElement docElement : docs) {
                JsonObject doc = docElement.getAsJsonObject();
                if (doc.has("v")) {
                    versions.add(doc.get("v").getAsString());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return versions;
    }
}
