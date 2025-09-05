package org.server.pixelogueCook;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import org.server.pixelogueCook.cook.command.CookRootCommand;
import org.server.pixelogueCook.cook.command.FoodRootCommand;
import org.server.pixelogueCook.cook.listener.CookGuiListener;
import org.server.pixelogueCook.cook.listener.RecipeBookListener;
import org.server.pixelogueCook.cook.service.CookService;
import org.server.pixelogueCook.farm.command.FarmCommand;
import org.server.pixelogueCook.farm.listener.*;
import org.server.pixelogueCook.farm.service.CropService;
import org.server.pixelogueCook.farm.service.HologramService;

public final class PixelogueCook extends JavaPlugin {

    private CropService cropService;
    private HologramService hologramService;
    private CookService cookService;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.cropService = new CropService(this);
        this.cropService.loadAll();

        this.hologramService = new HologramService(this, cropService);

        this.cookService = new CookService(this);
        cookService.loadRecipes();
        cookService.loadLearned();

        getServer().getPluginManager().registerEvents(new CookGuiListener(this, cookService), this);
        getServer().getPluginManager().registerEvents(new RecipeBookListener(this, cookService), this);

        getServer().getPluginManager().registerEvents(new PlantListener(this, cropService, hologramService), this);
        getServer().getPluginManager().registerEvents(new FertilizerListener(this, cropService), this);
        getServer().getPluginManager().registerEvents(new HarvestListener(this, cropService), this);
        getServer().getPluginManager().registerEvents(new NaturalGrowthBlocker(cropService), this);
        getServer().getPluginManager().registerEvents(new ChunkLifecycleListener(cropService, hologramService), this);

        hologramService.start();

        var farmCmd = new FarmCommand(this);
        getCommand("farm").setExecutor(farmCmd);
        getCommand("farm").setTabCompleter(farmCmd);

        PluginCommand food = getCommand("food");
        var foodRoot = new FoodRootCommand("food", cookService);
        food.setExecutor(foodRoot); food.setTabCompleter(foodRoot);

        PluginCommand cook = getCommand("cook");
        var cookRoot = new CookRootCommand("cook");
        cook.setExecutor(cookRoot); cook.setTabCompleter(cookRoot);
    }

    @Override
    public void onDisable() {
        try {
            hologramService.stop();
        } catch (Exception ignore) {}
        try {
            cropService.saveAll();
        } catch (Exception ignore) {}
        try { cookService.saveRecipes(); } catch (Exception ignore) {}
        try { cookService.saveLearned(); } catch (Exception ignore) {}
    }

    public CropService cropService() { return cropService; }
    public HologramService hologramService() { return hologramService; }
    public CookService cookService(){ return cookService; }
}
