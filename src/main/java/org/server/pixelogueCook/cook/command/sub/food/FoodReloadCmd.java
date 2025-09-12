package org.server.pixelogueCook.cook.command.sub.food;

import org.bukkit.command.CommandSender;
import org.server.pixelogueCook.PixelogueCook;
import org.server.pixelogueCook.cook.command.AbstractSubCommand;
import org.server.pixelogueCook.cook.service.CookService;

public class FoodReloadCmd extends AbstractSubCommand {
    private final PixelogueCook plugin;
    private final CookService cook;

    public FoodReloadCmd(PixelogueCook plugin, CookService cook) {
        this.plugin = plugin;
        this.cook = cook;
    }

    @Override public String name(){ return "리로드"; }
    @Override public java.util.List<String> aliases(){ return java.util.List.of("reload"); }
    @Override public String permission(){ return "cook.admin"; }

    @Override
    public boolean execute(CommandSender s, String label, String[] args) {
        plugin.reloadConfig();
        cook.loadRecipes();
        cook.loadLearned();
        s.sendMessage("§aPixelogueCook 설정과 레시피를 리로드했습니다!");
        return true;
    }
}