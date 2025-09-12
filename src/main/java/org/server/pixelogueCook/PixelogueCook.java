package org.server.pixelogueCook;

import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import org.server.pixelogueCook.cook.command.CookRootCommand;
import org.server.pixelogueCook.cook.command.FoodRootCommand;
import org.server.pixelogueCook.cook.listener.CookGuiListener;
import org.server.pixelogueCook.cook.listener.RecipeBookListener;
import org.server.pixelogueCook.cook.service.CookService;
import org.server.pixelogueCook.farm.command.FarmCommand;
import org.server.pixelogueCook.farm.listener.*;
import org.server.pixelogueCook.farm.service.CropGrowthTicker;
import org.server.pixelogueCook.farm.service.CropService;
import org.server.pixelogueCook.farm.service.GrowthHudService;
import org.server.pixelogueCook.fishing.FishingGameManager;
import org.server.pixelogueCook.fishing.command.FishingCommand;
import org.server.pixelogueCook.fishing.command.RodCommand;
import org.server.pixelogueCook.fishing.listener.FishingListener;

public final class PixelogueCook extends JavaPlugin {

    // === Farm / Cook 서비스 ===
    private CropService cropService;
    private CookService cookService;
    private CropGrowthTicker growthTicker;
    private GrowthHudService hudService;

    // === Fishing 서비스 ===
    private FishingGameManager fishingGameManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        // 모듈 ON/OFF 스위치 (config.yml에 없으면 기본 true)
        boolean enableFarm    = getConfig().getBoolean("modules.farm", true);
        boolean enableCook    = getConfig().getBoolean("modules.cook", true);
        boolean enableFishing = getConfig().getBoolean("modules.fishing", true);

        // 모듈별 초기화 (격리 & 로깅)
        if (enableCook)    safeInit("Cook",    this::initCookModule);
        if (enableFarm)    safeInit("Farm",    this::initFarmModule);
        if (enableFishing) safeInit("Fishing", this::initFishingModule);

        getLogger().info("PixelogueCook enabled.");
    }

    @Override
    public void onDisable() {
        // 종료 순서: 표시/스케줄 → 저장
        safeStop("Fishing", () -> {
            if (fishingGameManager != null) fishingGameManager.shutdown();
        });

        safeStop("Farm SaveAll", () -> {
            if (cropService != null) cropService.saveAll();
            if (hudService != null) hudService.stop();
            if (growthTicker != null) growthTicker.stop();
        });

        safeStop("Cook Save", () -> {
            if (cookService != null) {
                cookService.saveRecipes();
                cookService.saveLearned();
                cookService.saveActiveCooks();
            }
        });

        getLogger().info("PixelogueCook disabled.");
    }

    // -----------------------------
    // Cook 모듈
    // -----------------------------
    private void initCookModule() {
        this.cookService = new CookService(this);
        cookService.loadRecipes();
        cookService.loadLearned();

        cookService.loadActiveCooks();

        // 리스너
        getServer().getPluginManager().registerEvents(new CookGuiListener(this, cookService), this);
        getServer().getPluginManager().registerEvents(new RecipeBookListener(this, cookService), this);
        Bukkit.getOnlinePlayers().forEach(cookService::onJoin);

        // 명령어
        bindCommand("food",  new FoodRootCommand("food", cookService, this));
        bindCommand("cook",  new CookRootCommand("cook"));
    }

    // -----------------------------
    // Farm 모듈
    // -----------------------------
    private void initFarmModule() {
        this.cropService = new CropService(this);

        growthTicker = new CropGrowthTicker(this, cropService);
        growthTicker.start();

        hudService = new GrowthHudService(this, cropService);
        hudService.start();

        cropService.loadAll();


        // 리스너
        var pm = getServer().getPluginManager();
        pm.registerEvents(new PlantListener(this, cropService), this);
        pm.registerEvents(new FertilizerListener(this, cropService), this);
        pm.registerEvents(new HarvestListener(this, cropService), this);
        pm.registerEvents(new NaturalGrowthBlocker(cropService), this);
        pm.registerEvents(new ChunkLifecycleListener(), this);

        // 명령어
        var farmCmd = new FarmCommand(this);
        bindCommand("farm", farmCmd, farmCmd);
    }

    // -----------------------------
    // Fishing 모듈 (미니게임 3종)
    // -----------------------------
    private void initFishingModule() {
        // GameManager 생성
        this.fishingGameManager = new FishingGameManager(/* plugin = */ nullShim(this));

        // 리스너 등록 (낚시 트리거, 입력 캡처, 종료 정리)
        var pm = getServer().getPluginManager();
        pm.registerEvents(new FishingListener(fishingGameManager), this);

        // 테스트/운영용 낚시대 지급 커맨드
        bindCommand("fishing", new RodCommand());
        bindCommand("fish", new FishingCommand());
        getLogger().info("Fishing module initialized (Right/Left, DPS, Timing).");
    }

    // -----------------------------
    // 유틸 & 안전 헬퍼
    // -----------------------------
    private void bindCommand(String name, org.bukkit.command.CommandExecutor exec) {
        PluginCommand cmd = getCommand(name);
        if (cmd == null) {
            getLogger().warning("Command '" + name + "' is not defined in plugin.yml");
            return;
        }
        cmd.setExecutor(exec);
    }

    private void bindCommand(String name,
                             org.bukkit.command.CommandExecutor exec,
                             org.bukkit.command.TabCompleter tab) {
        PluginCommand cmd = getCommand(name);
        if (cmd == null) {
            getLogger().warning("Command '" + name + "' is not defined in plugin.yml");
            return;
        }
        cmd.setExecutor(exec);
        cmd.setTabCompleter(tab);
    }

    private void safeInit(String label, Runnable r) {
        long t0 = System.nanoTime();
        try {
            r.run();
            long ms = (System.nanoTime() - t0) / 1_000_000;
            getLogger().info(label + " module enabled in " + ms + " ms");
        } catch (Throwable t) {
            getLogger().severe(label + " module failed to enable: " + t.getMessage());
            t.printStackTrace();
        }
    }

    private void safeStop(String label, Runnable r) {
        try { r.run(); }
        catch (Throwable ignore) {
            getLogger().warning(label + " stop failed: " + ignore.getMessage());
        }
    }

    // 불필요하지만, 의도를 드러내기 위한 null 방지 래퍼
    private JavaPlugin nullShim(JavaPlugin plugin) { return plugin; }

    // ===== Getter =====
    public CropService cropService() { return cropService; }
    public CookService cookService(){ return cookService; }
    public FishingGameManager fishingGame(){ return fishingGameManager; }
}