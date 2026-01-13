package io.github.ceakins.gamedaemondeck.db;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public class Configuration implements Serializable {

    private static final long serialVersionUID = 1L;

    private String adminUsername;
    private String adminPasswordHash;
    private String steamCmdPath;
    private List<String> allowedIps;
    private int sessionTimeoutSeconds;

    public Configuration() {
    }

    public int getSessionTimeoutSeconds() {
        return sessionTimeoutSeconds;
    }

    public void setSessionTimeoutSeconds(int sessionTimeoutSeconds) {
        this.sessionTimeoutSeconds = sessionTimeoutSeconds;
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
