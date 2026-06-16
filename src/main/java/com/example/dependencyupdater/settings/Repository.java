package com.example.dependencyupdater.settings;

import java.util.Objects;
import java.util.UUID;

public class Repository {
    public enum AuthType {
        NONE("None"),
        BASIC("Basic (Username/Password)"),
        BEARER("Bearer Token");

        private final String displayName;

        AuthType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    private String id;
    private String name;
    private String url;
    private AuthType authType = AuthType.NONE;
    private String username;

    public Repository() {
        this.id = UUID.randomUUID().toString();
    }

    public Repository(String name, String url) {
        this();
        this.name = name;
        this.url = url;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public AuthType getAuthType() {
        return authType;
    }

    public void setAuthType(AuthType authType) {
        this.authType = authType;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Repository cloneRepo() {
        Repository copy = new Repository();
        copy.id = this.id;
        copy.name = this.name;
        copy.url = this.url;
        copy.authType = this.authType;
        copy.username = this.username;
        return copy;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Repository that = (Repository) o;
        return Objects.equals(id, that.id) && Objects.equals(name, that.name) && Objects.equals(url, that.url) && authType == that.authType && Objects.equals(username, that.username);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, url, authType, username);
    }
}
