package org.server.pixelogueCook.cook.command.sub.food;

import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.server.pixelogueCook.cook.command.AbstractSubCommand;
import org.server.pixelogueCook.cook.data.CookRecipe;
import org.server.pixelogueCook.cook.service.CookService;

public class FoodAddIngHandCmd extends AbstractSubCommand {
    private final CookService cook;
    public FoodAddIngHandCmd(CookService cook){ this.cook=cook; }

    @Override public String name(){ return "재료추가"; }
    @Override public java.util.List<String> aliases(){ return java.util.List.of("adding"); }
    @Override public String permission(){ return "cook.admin"; }

    @Override
    public boolean execute(CommandSender s, String label, String[] a) {
        if (!(s instanceof Player p)) { s.sendMessage("플레이어만 사용 가능합니다."); return true; }
        if (a.length < 2) { s.sendMessage("§e/"+label+" 재료추가 <레시피ID> <수량>"); return true; }

        String recipeId = a[0];
        int amt;
        try {
            amt = Integer.parseInt(a[1]);
        } catch (NumberFormatException e){
            s.sendMessage("수량은 숫자여야 합니다.");
            return true;
        }
        if (amt <= 0) { s.sendMessage("수량은 1 이상이어야 합니다."); return true; }

        CookRecipe r = cook.recipes().get(recipeId);
        if (r == null){ s.sendMessage("레시피가 없습니다: " + recipeId); return true; }

        ItemStack hand = p.getInventory().getItemInMainHand();
        if (hand == null || hand.getType().isAir()) { s.sendMessage("손에 든 아이템이 없습니다."); return true; }

        Material mat = hand.getType();
        r.ingredients.merge(mat, amt, Integer::sum);  // ✅ 타입+수량 누적
        cook.saveRecipes();

        s.sendMessage("§a재료 추가 완료: §f" + mat.name() + " §7x§e" + amt
            + " §8(총 " + r.ingredients.get(mat) + "개)");
        return true;
    }
}