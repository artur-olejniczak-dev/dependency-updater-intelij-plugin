import sys

with open('src/main/java/com/example/dependencyupdater/MavenCentralApiClient.java', 'r', encoding='utf-8') as f:
    content = f.read()

old_fetch_logic = '''    public CompletableFuture<List<String>> fetchAvailableVersions(String groupId, String artifactId) {
        DependencyUpdaterSettingsState state = DependencyUpdaterSettingsState.getInstance();
        List<Repository> repositories = state.repositories;

        if (repositories == null || repositories.isEmpty()) {
            return CompletableFuture.completedFuture(Collections.emptyList());
        }

        String groupPath = groupId.replace(".", "/");

        List<CompletableFuture<List<String>>> futures = repositories.stream().map(repo -> {
            return CompletableFuture.supplyAsync(() -> {
                String baseUrl = repo.getUrl();
                if (!baseUrl.endsWith("/")) {
                    baseUrl += "/";
                }
                String url;
                if (repo.isHtmlListing()) {
                    url = String.format("%slibs-release/%s/%s/", baseUrl, groupPath, artifactId);
                } else {
                    url = String.format("%s%s/%s/maven-metadata.xml", baseUrl, groupPath, artifactId);
                }

                try {
                    return HttpRequests.request(url)
                            .tuner(connection -> {
                                if (repo.getAuthType() == Repository.AuthType.NONE) return;
                                
                                CredentialAttributes attributes = new CredentialAttributes(
                                        CredentialAttributesKt.generateServiceName("DependencyUpdater", repo.getId())
                                );
                                Credentials credentials = PasswordSafe.getInstance().get(attributes);
                                String secret = credentials != null ? credentials.getPasswordAsString() : null;
                                if (secret == null || secret.isEmpty()) return;

                                if (repo.getAuthType() == Repository.AuthType.BASIC) {
                                    String username = repo.getUsername() != null ? repo.getUsername() : "";
                                    String authString = username + ":" + secret;
                                    String encodedAuth = Base64.getEncoder().encodeToString(authString.getBytes(StandardCharsets.UTF_8));
                                    connection.setRequestProperty("Authorization", "Basic " + encodedAuth);
                                } else if (repo.getAuthType() == Repository.AuthType.BEARER) {
                                    connection.setRequestProperty("Authorization", "Bearer " + secret);
                                }
                            })
                            .readString();
                } catch (Exception e) {
                    return null;
                }
            }).thenApply(body -> {
                if (body != null) {
                    if (repo.isHtmlListing()) {
                        return parseHtmlVersions(body);
                    } else {
                        return parseVersions(body);
                    }
                }
                return new ArrayList<String>();
            }).exceptionally(ex -> new ArrayList<>());
        }).collect(Collectors.toList());

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> {
                    Set<String> allVersions = new HashSet<>();
                    for (CompletableFuture<List<String>> future : futures) {
                        allVersions.addAll(future.join());
                    }
                    return new ArrayList<>(allVersions);
                });
    }'''

new_fetch_logic = '''    public CompletableFuture<List<String>> fetchAvailableVersions(String groupId, String artifactId) {
        DependencyUpdaterSettingsState state = DependencyUpdaterSettingsState.getInstance();
        List<Repository> repositories = state.repositories;

        if (repositories == null || repositories.isEmpty()) {
            return CompletableFuture.completedFuture(Collections.emptyList());
        }

        String groupPath = groupId.replace(".", "/");
        List<CompletableFuture<List<String>>> futures = new ArrayList<>();

        for (Repository repo : repositories) {
            String baseUrl = repo.getUrl();
            if (!baseUrl.endsWith("/")) {
                baseUrl += "/";
            }
            if (repo.isHtmlListing()) {
                String releaseUrl = String.format("%slibs-release/%s/%s/", baseUrl, groupPath, artifactId);
                String snapshotUrl = String.format("%slibs-snapshot/%s/%s/", baseUrl, groupPath, artifactId);
                futures.add(createFetchFuture(repo, releaseUrl, true));
                futures.add(createFetchFuture(repo, snapshotUrl, true));
            } else {
                String xmlUrl = String.format("%s%s/%s/maven-metadata.xml", baseUrl, groupPath, artifactId);
                futures.add(createFetchFuture(repo, xmlUrl, false));
            }
        }

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> {
                    Set<String> allVersions = new HashSet<>();
                    for (CompletableFuture<List<String>> future : futures) {
                        allVersions.addAll(future.join());
                    }
                    return new ArrayList<>(allVersions);
                });
    }

    private CompletableFuture<List<String>> createFetchFuture(Repository repo, String url, boolean isHtml) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return HttpRequests.request(url)
                        .tuner(connection -> {
                            if (repo.getAuthType() == Repository.AuthType.NONE) return;
                            
                            CredentialAttributes attributes = new CredentialAttributes(
                                    CredentialAttributesKt.generateServiceName("DependencyUpdater", repo.getId())
                            );
                            Credentials credentials = PasswordSafe.getInstance().get(attributes);
                            String secret = credentials != null ? credentials.getPasswordAsString() : null;
                            if (secret == null || secret.isEmpty()) return;

                            if (repo.getAuthType() == Repository.AuthType.BASIC) {
                                String username = repo.getUsername() != null ? repo.getUsername() : "";
                                String authString = username + ":" + secret;
                                String encodedAuth = Base64.getEncoder().encodeToString(authString.getBytes(StandardCharsets.UTF_8));
                                connection.setRequestProperty("Authorization", "Basic " + encodedAuth);
                            } else if (repo.getAuthType() == Repository.AuthType.BEARER) {
                                connection.setRequestProperty("Authorization", "Bearer " + secret);
                            }
                        })
                        .readString();
            } catch (Exception e) {
                return null;
            }
        }).thenApply(body -> {
            if (body != null) {
                if (isHtml) {
                    return parseHtmlVersions(body);
                } else {
                    return parseVersions(body);
                }
            }
            return new ArrayList<String>();
        }).exceptionally(ex -> new ArrayList<>());
    }'''

content = content.replace(old_fetch_logic, new_fetch_logic)

with open('src/main/java/com/example/dependencyupdater/MavenCentralApiClient.java', 'w', encoding='utf-8') as f:
    f.write(content)
