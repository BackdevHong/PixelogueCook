package org.server.pixelogueCook.farm.data;

public enum FertilizerTier {
    NONE, MID, HIGH, TOP;
    public static FertilizerTier from(String s) {
        try { return valueOf(s.toUpperCase()); } catch (Exception e) { return NONE; }
    }
}