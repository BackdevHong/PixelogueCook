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

public class DpsClickGame implements MiniGame {
    private final Player p;
    private final FishingGameManager mgr;

    private final int target;
    private int count = 0;              // 누른 횟수
    private final long deadlineMs;
    private BukkitRunnable ticker;

    public DpsClickGame(Player p, FishingGameManager mgr) {
        this.p = p;
        this.mgr = mgr;
        var cfg = mgr.plugin().getConfig();
        int min = cfg.getInt("dps.clicks_min", 20);
        int max = cfg.getInt("dps.clicks_max", 90);
        int time = cfg.getInt("dps.time_seconds", 4);
        this.target = new Random().nextInt(max - min + 1) + min;
        this.deadlineMs = System.currentTimeMillis() + time * 1000L;
    }

    @Override public Player player() { return p; }

    @Override
    public void start() {
        p.sendMessage("§b[DPS] §f우클릭으로 §e" + target + "회 §f달성하세요!");
        renderTitle(); // 초기 표시

        ticker = new BukkitRunnable() {
            @Override public void run() {
                long remain = deadlineMs - System.currentTimeMillis();
                if (remain <= 0) {
                    cancel();
                    clearTitle();
                    mgr.fail(p, "시간 초과");
                    return;
                }
                renderTitle(); // 남은 시간 갱신
            }
        };
        ticker.runTaskTimer(mgr.plugin(), 0L, 2L); // 0.1초 간격
    }

    @Override
    public void onClick(PlayerInteractEvent e) {
        e.setCancelled(true);
        if (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK) {
            clearTitle(); // 클릭 시 먼저 지움

            count++;
            int left = Math.max(0, target - count);
            if (left == 0) {
                if (ticker != null) ticker.cancel();
                clearTitle();
                mgr.succeed(p);
                return;
            }

            renderTitle(); // 최신 상태 재표시
        }
    }

    @Override
    public void cancel(String reason) {
        if (ticker != null) ticker.cancel();
        clearTitle();
    }

    // ----------------- helpers -----------------

    private void renderTitle() {
        long remainMs = Math.max(0, deadlineMs - System.currentTimeMillis());
        int left = Math.max(0, target - count);

        String title = "§e" + left; // 남은 클릭 수만 크게
        String subtitle = "§7남은시간: §e" + String.format("%.1f", remainMs / 1000.0) + "s"
            + " §8| §7목표: §f" + target;

        // 짧게 깜빡이며 갱신
        p.sendTitle(title, subtitle, 0, 10, 5);
    }

    private void clearTitle() {
        try {
            p.resetTitle();
        } catch (Throwable ignored) {
            p.sendTitle("", "", 0, 0, 0);
        }
    }
}