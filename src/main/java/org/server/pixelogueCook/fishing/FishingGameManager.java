package org.server.pixelogueCook.fishing;

import org.bukkit.entity.FishHook;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.server.pixelogueCook.fishing.game.GameFactory;
import org.server.pixelogueCook.fishing.game.GameKind;
import org.server.pixelogueCook.fishing.game.MiniGame;
import org.server.pixelogueCook.fishing.game.types.DpsClickGame;
import org.server.pixelogueCook.fishing.game.types.TimingBarGame;
import org.server.pixelogueCook.fishing.model.FishRank;
import org.server.pixelogueCook.fishing.model.RodTier;
import org.server.pixelogueCook.fishing.reward.RankPicker;
import org.server.pixelogueCook.fishing.reward.Rewarder;
import org.server.pixelogueCook.fishing.reward.SimpleRewarder;
import org.server.pixelogueCook.fishing.util.RodUtil;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class FishingGameManager {
    private final JavaPlugin plugin;

    // 진행 중 세션
    private final Map<UUID, MiniGame> active = new ConcurrentHashMap<>();
    private final Map<UUID, FishHook> hooks = new ConcurrentHashMap<>();

    // 게임 공장 등록
    private final EnumMap<GameKind, GameFactory> factories = new EnumMap<>(GameKind.class);
    private final Random rnd = new Random();

    private final Rewarder rewarder;

    public FishingGameManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.rewarder = new SimpleRewarder();
        registerDefaultGames();
    }

    public JavaPlugin plugin() { return plugin; }

    // ===== 등록/상태 =====
    public boolean isBusy(Player p) { return active.containsKey(p.getUniqueId()); }

    public void registerGame(GameKind kind, GameFactory factory) {
        factories.put(kind, factory);
    }

    private void registerDefaultGames() {
        // 기본 3종 미니게임 등록
        registerGame(GameKind.DPS, DpsClickGame::new);
        registerGame(GameKind.TIMING, TimingBarGame::new);
    }

    // ===== 시작/입력/종료 =====
    public void startRandom(Player p) {
        if (isBusy(p)) return;

        var enabledKinds = new ArrayList<>(factories.keySet());
        if (enabledKinds.isEmpty()) {
            p.sendMessage("§c낚시 미니게임이 등록되어 있지 않습니다.");
            return;
        }
        GameKind pick = enabledKinds.get(rnd.nextInt(enabledKinds.size()));
        start(p, pick);
    }

    public void start(Player p, GameKind kind) {
        if (isBusy(p)) return;
        GameFactory factory = factories.get(kind);
        if (factory == null) {
            p.sendMessage("§c해당 미니게임이 비활성화되어 있습니다: " + kind);
            return;
        }
        MiniGame game = factory.create(p, this);
        active.put(p.getUniqueId(), game);
        game.start();
    }

    public void onClick(PlayerInteractEvent e) {
        MiniGame g = active.get(e.getPlayer().getUniqueId());
        if (g != null) g.onClick(e);
    }

    public void bindHook(Player p, FishHook hook) {
        if (p == null || hook == null) return;
        hooks.put(p.getUniqueId(), hook);
    }

    public void releaseHook(Player p) {
        FishHook hook = hooks.remove(p.getUniqueId());
        if (hook != null && !hook.isDead()) {
            try { hook.remove(); } catch (Throwable ignore) {}
        }
    }

    public void succeed(Player p) {
        releaseHook(p);

        active.remove(p.getUniqueId());
        RodTier tier = RodUtil.getTier(p.getInventory().getItemInMainHand());
        FishRank rank = RankPicker.pick(plugin, tier);
        rewarder.give(p, rank);
        p.sendMessage("§a성공! §f" + rank.name() + " §7등급 물고기를 획득했습니다.");
    }

    public void fail(Player p, String reason) {
        releaseHook(p);

        active.remove(p.getUniqueId());
        // === 위로 보상: D~C 등급 고기 지급 ===
        RodTier tier = RodUtil.getTier(p.getInventory().getItemInMainHand());
        FishRank consolation = pickConsolationRank(tier); // D 또는 C
        rewarder.give(p, consolation);
        p.sendMessage("§c실패! 불쌍한 물고기가 잡혀주었습니다.." );
    }

    /** RodTier가 높을수록 C 확률이 약간 올라가는 간단한 위로보상 랭크 선택기 (D~C 범위 고정) */
    private FishRank pickConsolationRank(RodTier tier) {
        // 기본 C 확률 30%에서 RodTier의 ordinal에 비례하여 증가(최대 80%)
        double cChance = 0.30;
        if (tier != null) {
            int ord = tier.ordinal();
            cChance = Math.min(0.30 + ord * 0.10, 0.80);
        }
        return (rnd.nextDouble() < cChance) ? FishRank.C : FishRank.D;
    }

    public void abort(Player p){
         releaseHook(p);

        MiniGame g = active.remove(p.getUniqueId());
        if (g != null) g.cancel("중단");
    }

    public void shutdown(){
        for (UUID id : hooks.keySet()) {
            FishHook h = hooks.get(id);
            if (h != null && !h.isDead()) {
                try { h.remove(); } catch (Throwable ignore) {}
            }
        }
        hooks.clear();

        // 기존 세션 정리
        for (MiniGame g : new ArrayList<>(active.values())) g.cancel("플러그인 종료");
        active.clear();
    }
}