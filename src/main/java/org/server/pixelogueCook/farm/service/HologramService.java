package org.server.pixelogueCook.farm.service;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.scheduler.BukkitTask;
import org.server.pixelogueCook.PixelogueCook;
import org.server.pixelogueCook.farm.data.CropInstance;
import org.server.pixelogueCook.farm.util.ColorUtil;

import java.util.Objects;

public class HologramService {
    private final PixelogueCook plugin;
    private final CropService cropService;
    private BukkitTask loop;

    private final int refreshTicks;
    private final String titleFmt;
    private final String grownFmt;

    public HologramService(PixelogueCook plugin, CropService cropService) {
        this.plugin = plugin;
        this.cropService = cropService;
        this.refreshTicks = plugin.getConfig().getInt("hologram.refresh-ticks", 20);
        this.titleFmt = plugin.getConfig().getString("hologram.title-format", "&a%mm:ss%");
        this.grownFmt = plugin.getConfig().getString("hologram.grown-format", "&e수확 가능!");
    }

    public void start() {
        if (loop != null) return;
        loop = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 20L, refreshTicks);
    }

    public void stop() {
        if (loop != null) loop.cancel();
        loop = null;
    }

    private void tick() {
        long now = System.currentTimeMillis();
        for (CropInstance ci : cropService.all()) {
            World w = plugin.getServer().getWorld(ci.worldId);
            if (w == null) continue;

            Location base = new Location(w, ci.x + 0.5, ci.y + 0.9, ci.z + 0.5);
            long remain = ci.remain(now);
            boolean grown = remain <= 0;
            if (grown) cropService.markGrown(ci);
            Block block = base.getBlock();
            if (block.getType() == ci.type.block) {
                BlockData bd = block.getBlockData();
                if (bd instanceof Ageable ag) {
                    int max = ag.getMaximumAge();

                    double ratio = 1.0 - (double) ci.remain(now) / (double) ci.growMillis;
                    if (ratio < 0) ratio = 0;
                    if (ratio > 1) ratio = 1;

                    int targetAge = (int) Math.floor(ratio * max);
                    // 수확 가능 상태면 확실히 최대치로
                    if (grown) targetAge = max;

                    if (ag.getAge() != targetAge) {
                        ag.setAge(targetAge);
                        block.setBlockData(ag, false); // 물리 갱신 최소화
                    }
                }
            }

            ArmorStand as = resolve(ci, base);
            String text = grown ? grownFmt : titleFmt.replace("%mm:ss%", format(remain));
            as.setCustomName(ColorUtil.color(text));
            as.setCustomNameVisible(true);
        }
    }

    private ArmorStand resolve(CropInstance ci, Location where) {
        if (ci.hologramId != null) {
            Entity e = Bukkit.getEntity(ci.hologramId);
            if (e instanceof ArmorStand a && a.isValid()) return a;
        }
        ArmorStand a = where.getWorld().spawn(where, ArmorStand.class, s -> {
            s.setInvisible(true);
            s.setMarker(true);
            s.setGravity(false);
            s.setSmall(true);
            s.setPersistent(true);
            s.setCustomNameVisible(true);
            s.setInvulnerable(true);
            s.setRemoveWhenFarAway(false);
        });
        ci.hologramId = a.getUniqueId();
        return a;
    }

    private String format(long millis) {
        long s = Math.max(0, millis / 1000L);
        long mm = s / 60, ss = s % 60;
        return String.format("%02d:%02d", mm, ss);
    }

    public void removeHologram(CropInstance ci) {
        if (ci.hologramId == null) return;
        Entity e = Bukkit.getEntity(ci.hologramId);
        if (e != null) e.remove();
        ci.hologramId = null;
    }
}