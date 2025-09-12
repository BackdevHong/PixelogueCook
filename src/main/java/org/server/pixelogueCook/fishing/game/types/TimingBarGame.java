package org.server.pixelogueCook.fishing.game.types;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.server.pixelogueCook.fishing.FishingGameManager;
import org.server.pixelogueCook.fishing.game.MiniGame;

import java.util.Random;

public class TimingBarGame implements MiniGame {
    private final Player p;
    private final FishingGameManager mgr;
    private final int barLen;
    private final int gStart, gEnd;
    private final int yStart, yEnd;
    private int cursor = 0;
    private int dir = 1; // 1:right, -1:left
    private final long deadlineMs;
    private BukkitRunnable ticker;

    public TimingBarGame(Player p, FishingGameManager mgr) {
        this.p = p;
        this.mgr = mgr;
        var cfg = mgr.plugin().getConfig();
        int time = cfg.getInt("timing.time_seconds", 6);
        this.deadlineMs = System.currentTimeMillis() + time * 1000L;

        this.barLen = Math.max(16, cfg.getInt("timing.bar_length", 32));
        int gMin = cfg.getInt("timing.green_len_min", 3);
        int gMax = cfg.getInt("timing.green_len_max", 6);
        int yMin = cfg.getInt("timing.yellow_len_min", 6);
        int yMax = cfg.getInt("timing.yellow_len_max", 12);

        Random r = new Random();
        int gLen = clamp(r.nextInt(gMax - gMin + 1) + gMin, 1, barLen / 2);
        int yLen = clamp(r.nextInt(yMax - yMin + 1) + yMin, 1, barLen - gLen);

        int gCenter = barLen / 2;
        this.gStart = gCenter - gLen / 2;
        this.gEnd   = gStart + gLen - 1;

        int leftYLen = yLen / 2;
        int rightYLen = yLen - leftYLen;
        this.yStart = Math.max(0, gStart - leftYLen);
        this.yEnd   = Math.min(barLen - 1, gEnd + rightYLen);
        this.cursor = 0;
    }

    private static int clamp(int v, int min, int max){ return Math.max(min, Math.min(max, v)); }

    @Override public Player player() { return p; }

    @Override
    public void start() {
        p.sendMessage("§b[타이밍] §f우클릭으로 포인터를 멈추세요! §a초록=성공 §e노랑=70% §c빨강=실패");

        int interval = mgr.plugin().getConfig().getInt("timing.pointer_tick_interval", 2);

        ticker = new BukkitRunnable() {
            @Override public void run() {
                long remain = deadlineMs - System.currentTimeMillis();
                if (remain <= 0) {
                    cancel();
                    clearTitle();
                    mgr.fail(p, "시간 초과");
                    return;
                }
                // 포인터 이동(양끝 반사)
                cursor += dir;
                if (cursor <= 0) { cursor = 0; dir = 1; }
                if (cursor >= barLen - 1) { cursor = barLen - 1; dir = -1; }

                renderTitle(remain);
            }
        };
        ticker.runTaskTimer(mgr.plugin(), 0L, Math.max(1, interval));
    }

    private void renderTitle(long remainMs){
        String bar = buildBar();
        String subtitle = "§7남은시간: §e" + String.format("%.1f", Math.max(0, remainMs)/1000.0) + "s  §8| §7우클릭으로 멈추기";
        // 깜빡임 최소화: 짧은 stay로 주기적 업데이트
        p.sendTitle(bar, subtitle, 0, 10, 5);
    }

    private String buildBar(){
        StringBuilder sb = new StringBuilder(barLen + 10);
        sb.append("§8[");
        for (int i=0;i<barLen;i++){
            boolean isG = (i >= gStart && i <= gEnd);
            boolean isY = (!isG) && (i >= yStart && i <= yEnd);
            String color = isG ? "§a" : (isY ? "§e" : "§c");
            String ch = (i == cursor) ? "§f⬤" : "■";
            sb.append(color).append(ch);
        }
        sb.append("§8]");
        return sb.toString();
    }

    @Override
    public void onClick(PlayerInteractEvent e) {
        e.setCancelled(true);
        if (!(e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK)) return;

        // 클릭 시 먼저 타이틀 지움(요청 일관성)
        clearTitle();

        if (ticker != null) ticker.cancel();

        boolean inGreen  = cursor >= gStart && cursor <= gEnd;
        boolean inYellow = !inGreen && cursor >= yStart && cursor <= yEnd;

        if (inGreen){
            mgr.succeed(p);
        } else if (inYellow){
            if (new Random().nextDouble() < 0.70) mgr.succeed(p);
            else mgr.fail(p, "아슬아슬하게 놓침");
        } else {
            mgr.fail(p, "빨간 존");
        }
    }

    @Override
    public void cancel(String reason) {
        if (ticker != null) ticker.cancel();
        clearTitle();
    }

    private void clearTitle() {
        try {
            p.resetTitle();
        } catch (Throwable ignored) {
            p.sendTitle("", "", 0, 0, 0);
        }
    }
}