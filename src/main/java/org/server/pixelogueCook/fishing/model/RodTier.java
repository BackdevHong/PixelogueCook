package org.server.pixelogueCook.fishing.model;

public enum RodTier {
    BASIC, INTERMEDIATE, ADVANCED, MASTER;

    public static RodTier of(String s){
        try { return RodTier.valueOf(s.toUpperCase()); }
        catch (Exception e){ return null; }
    }
}