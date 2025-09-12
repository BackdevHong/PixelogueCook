package org.server.pixelogueCook.farm.service;


import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;
import org.server.pixelogueCook.PixelogueCook;
import org.server.pixelogueCook.farm.data.CropInstance;
import org.server.pixelogueCook.farm.util.ColorUtil;

import java.util.Optional;

public class GrowthHudService {
    private final PixelogueCook plugin;
    private final CropService crops;
    private final int rayDist;
    private final int refreshTicks;
    private final boolean sneakOnly;
    private int taskId = -1;

    public GrowthHudService(PixelogueCook plugin, CropService crops) {
        this.plugin = plugin; this.crops = crops;
        this.rayDist = plugin.getConfig().getInt("hud.ray-distance", 5);
        this.refreshTicks = plugin.getConfig().getInt("hud.refresh-ticks", 10); // 0.5s
        this.sneakOnly = plugin.getConfig().getBoolean("hud.sneak-only", false);
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
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            if (p.getGameMode() == GameMode.SPECTATOR) continue;
            if (sneakOnly && !p.isSneaking()) continue;

            RayTraceResult ray = p.rayTraceBlocks(rayDist);
            if (ray == null || ray.getHitBlock() == null) continue;

            var b = ray.getHitBlock();
            if (!crops.isSupportedCrop(b.getType())) continue;

            Optional<CropInstance> opt = crops.get(b);
            if (opt.isEmpty()) continue; // 우리 시스템 외 일반 작물
            CropInstance ci = opt.get();

            long remain = ci.remain(now);
            String msg = remain <= 0
                ? plugin.getConfig().getString("hud.grown-text", "&e수확 가능!")
                : plugin.getConfig().getString("hud.timer-text", "&a%mm:ss%")
                .replace("%mm:ss%", mmss(remain));

            // Action Bar 출력
            p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ColorUtil.color(msg)));
        }
    }

    private String mmss(long millis) {
        long s = Math.max(0, millis / 1000L);
        long mm = s / 60, ss = s % 60;
        return String.format("%02d:%02d", mm, ss);
    }
}