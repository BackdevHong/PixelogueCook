package org.server.pixelogueCook.cook.command.sub.food;

import org.bukkit.command.CommandSender;
import org.server.pixelogueCook.cook.command.AbstractSubCommand;
import org.server.pixelogueCook.cook.data.CookRecipe;
import org.server.pixelogueCook.cook.service.CookService;

public class FoodListCmd extends AbstractSubCommand {
    private final CookService cook;
    public FoodListCmd(CookService cook){ this.cook=cook; }

    @Override public String name(){ return "목록"; }
    @Override public java.util.List<String> aliases(){ return java.util.List.of("list"); }
    @Override public String permission(){ return "cook.admin"; }

    @Override
    public boolean execute(CommandSender s, String label, String[] a) {
        if (cook.recipes().isEmpty()) { s.sendMessage("등록된 레시피가 없습니다."); return true; }
        s.sendMessage("§e=== 레시피 목록 ===");
        for (CookRecipe r : cook.recipes().values()){
            s.sendMessage("§f- "+r.id+" §7("+r.displayName+", "+(r.cookMillis/1000)+"초, 재료 "+r.ingredients.size()+"종)");
        }
        return true;
    }
}