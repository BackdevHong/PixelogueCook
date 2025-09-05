package org.server.pixelogueCook.cook.command.sub.food;

import org.bukkit.command.CommandSender;
import org.server.pixelogueCook.cook.command.AbstractSubCommand;
import org.server.pixelogueCook.cook.service.CookService;

public class FoodRemoveCmd extends AbstractSubCommand {
    private final CookService cook;
    public FoodRemoveCmd(CookService cook){ this.cook=cook; }

    @Override public String name(){ return "삭제"; }
    @Override public java.util.List<String> aliases(){ return java.util.List.of("remove"); }
    @Override public String permission(){ return "cook.admin"; }

    @Override
    public boolean execute(CommandSender s, String label, String[] a) {
        if (a.length<1){ s.sendMessage("§e/"+label+" 삭제 <레시피ID>"); return true; }
        var r = cook.recipes().remove(a[0]);
        cook.saveRecipes();
        s.sendMessage(r==null ? "레시피 없음" : "삭제 완료: "+a[0]);
        return true;
    }
}