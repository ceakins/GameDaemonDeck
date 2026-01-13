package io.github.ceakins.daemondeck.db;

import java.io.Serializable;
import java.util.List;

public class Configuration implements Serializable {

    private static final long serialVersionUID = 1L;

    private String adminUsername;
    private String adminPasswordHash;
    private String steamCmdPath;
    private List<String> allowedIps;

    public Configuration() {
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

    public List<String> getAllowedIps() {
        return allowedIps;
    }

    public void setAllowedIps(List<String> allowedIps) {
        this.allowedIps = allowedIps;
    }
}