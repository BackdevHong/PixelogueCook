package org.server.pixelogueCook.farm.data;

import org.bukkit.Location;

import java.util.Objects;
import java.util.UUID;

public class CropInstance {
    public final UUID worldId;
    public final int x, y, z;
    public final CropType type;
    public final long plantedAtMillis;
    public long growMillis;
    public FertilizerTier fertilizer = FertilizerTier.NONE;
    public boolean grown = false;

    // 홀로그램(ArmorStand) UUID — Spigot은 Bukkit.getEntity(UUID)로 조회
    public UUID hologramId = null;

    public CropInstance(Location loc, CropType type, long plantedAtMillis, long growMillis) {
        this.worldId = Objects.requireNonNull(loc.getWorld()).getUID();
        this.x = loc.getBlockX();
        this.y = loc.getBlockY();
        this.z = loc.getBlockZ();
        this.type = type;
        this.plantedAtMillis = plantedAtMillis;
        this.growMillis = growMillis;
    }

    public long elapsed(long now) { return Math.max(0, now - plantedAtMillis); }
    public long remain(long now) { return Math.max(0, growMillis - elapsed(now)); }
}