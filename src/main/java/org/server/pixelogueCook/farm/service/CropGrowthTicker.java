package org.server.pixelogueCook.farm.service;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.server.pixelogueCook.PixelogueCook;
import org.server.pixelogueCook.farm.data.CropInstance;

public class CropGrowthTicker {
    private final PixelogueCook plugin;
    private final CropService crops;
    private final int refreshTicks;
    private int taskId = -1;

    public CropGrowthTicker(PixelogueCook plugin, CropService crops) {
        this.plugin = plugin; this.crops = crops;
        this.refreshTicks = plugin.getConfig().getInt("growth.refresh-ticks", 20); // 1초마다
    }

    public void start() {
        if (taskId != -1) return;
        taskId = plugin.getServer().getScheduler().scheduleSyncRepeatingTask(
            plugin, this::tick, 20L, refreshTicks
        );
    }

    public void stop() {
        if (taskId != -1) {
            plugin.getServer().getScheduler().cancelTask(taskId);
            taskId = -1;
        }
    }

    private void tick() {
        long now = System.currentTimeMillis();
        for (CropInstance ci : crops.all()) {
            World w = plugin.getServer().getWorld(ci.worldId);
            if (w == null) continue;
            // 청크 미로드면 스킵
            if (!w.isChunkLoaded(ci.x >> 4, ci.z >> 4)) continue;

            Location base = new Location(w, ci.x, ci.y, ci.z);
            Block block = base.getBlock();
            if (block.getType() != ci.type.block) continue;

            long remain = ci.remain(now);
            boolean grown = remain <= 0;
            if (grown) crops.markGrown(ci);

            BlockData bd = block.getBlockData();
            if (bd instanceof Ageable ag) {
                int max = ag.getMaximumAge();
                double ratio = 1.0 - (double) ci.remain(now) / (double) ci.growMillis;
                if (ratio < 0) ratio = 0; if (ratio > 1) ratio = 1;
                int targetAge = grown ? max : (int) Math.floor(ratio * max);
                if (ag.getAge() != targetAge) {
                    ag.setAge(targetAge);
                    block.setBlockData(ag, false); // 물리 최소화
                }
            }
        }
    }
}