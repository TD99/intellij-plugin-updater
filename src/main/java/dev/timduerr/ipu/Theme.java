package dev.timduerr.ipu;

public enum Theme {
    MAC_LIGHT("macLight"),
    MAC_DARK("macDark"),
    INTELLIJ("intellij"),
    DRACULA("dracula"),
    CLASSIC_LIGHT("classicLight"),
    CLASSIC_DARK("classicDark");

    private final String name;

    Theme(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static Theme fromName(String name) {
        for (Theme theme : values()) {
            if (theme.name.equalsIgnoreCase(name)) {
                return theme;
            }
        }
        return null;
    }
}
