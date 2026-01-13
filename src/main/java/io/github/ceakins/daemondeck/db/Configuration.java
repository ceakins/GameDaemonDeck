package io.github.ceakins.daemondeck.db;

import org.dizitart.no2.repository.annotations.Id;

import java.util.Set;

public class Configuration {

    @Id
    private long id;
    private String adminUsername;
    private String adminPasswordHash;
    private String steamCmdPath;
    private Set<String> allowedIps;

    public Configuration() {
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getAdminUsername() {
        return adminUsername;
    }

    public void setAdminUsername(String adminUsername) {
        this.adminUsername = adminUsername;
    }

    public String getAdminPasswordHash() {
        return adminPasswordHash;
    }

    public void setAdminPasswordHash(String adminPasswordHash) {
        this.adminPasswordHash = adminPasswordHash;
    }

    public String getSteamCmdPath() {
        return steamCmdPath;
    }

    public void setSteamCmdPath(String steamCmdPath) {
        this.steamCmdPath = steamCmdPath;
    }

    public Set<String> getAllowedIps() {
        return allowedIps;
    }

    public void setAllowedIps(Set<String> allowedIps) {
        this.allowedIps = allowedIps;
    }
}
