package io.github.ceakins.gamedaemondeck.plugins;

public class ConfigField {
    private String name;
    private String label;
    private String type;
    private String defaultValue;

    public ConfigField(String name, String label, String type, String defaultValue) {
        this.name = name;
        this.label = label;
        this.type = type;
        this.defaultValue = defaultValue;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }
}
