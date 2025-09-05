package org.server.pixelogueCook.cook.command.sub.food;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.server.pixelogueCook.cook.command.AbstractSubCommand;
import org.server.pixelogueCook.cook.service.CookService;

public class FoodGiveBookCmd extends AbstractSubCommand {
    private final CookService cook;
    public FoodGiveBookCmd(CookService cook){ this.cook=cook; }

    @Override public String name(){ return "책지급"; }
    @Override public java.util.List<String> aliases(){ return java.util.List.of("givebook"); }
    @Override public String permission(){ return "cook.admin"; }

    @Override
    public boolean execute(CommandSender s, String label, String[] a) {
        if (a.length < 1) { s.sendMessage("§e/"+label+" 책지급 <레시피ID> [플레이어]"); return true; }
        var item = cook.makeRecipeBook(a[0]);
        if (item == null) { s.sendMessage("레시피 없음"); return true; }
        Player target = (a.length>=2) ? Bukkit.getPlayerExact(a[1]) : (s instanceof Player p ? p : null);
        if (target == null){ s.sendMessage("대상 없음"); return true; }
        target.getInventory().addItem(item);
        s.sendMessage("레시피 북 지급: "+a[0]+" → "+target.getName());
        return true;
    }
}