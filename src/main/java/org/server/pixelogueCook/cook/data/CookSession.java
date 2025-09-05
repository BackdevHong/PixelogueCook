package org.server.pixelogueCook.cook.data;

import java.util.UUID;

public class CookSession {
    public final UUID playerId;
    public final String recipeId;
    public final long startedAt;
    public final long cookMillis;

    public CookSession(UUID playerId, String recipeId, long startedAt, long cookMillis) {
        this.playerId = playerId;
        this.recipeId = recipeId;
        this.startedAt = startedAt;
        this.cookMillis = cookMillis;
    }

    public long remain(long now){ return Math.max(0, cookMillis - (now - startedAt)); }
    public boolean done(long now){ return remain(now) == 0; }
}