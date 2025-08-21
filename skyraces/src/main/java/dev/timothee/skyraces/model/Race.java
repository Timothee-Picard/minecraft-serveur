package dev.timothee.skyraces.model;

public enum Race {
    SOLAIRES, OMBRES;

    public static Race from(String s) {
        if (s == null) return null;
        return switch (s.toLowerCase()) {
            case "solaires", "solaire", "solar" -> SOLAIRES;
            case "ombres", "ombre", "shadow", "shadows" -> OMBRES;
            default -> null;
        };
    }
}
