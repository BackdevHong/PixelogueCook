package org.server.pixelogueCook.cook.command.sub.food;

import org.bukkit.command.CommandSender;
import org.server.pixelogueCook.cook.command.AbstractSubCommand;
import org.server.pixelogueCook.cook.data.CookRecipe;
import org.server.pixelogueCook.cook.service.CookService;

import java.util.List;
import java.util.Locale;

public class FoodCapCmd extends AbstractSubCommand {
    private final CookService cook;
    public FoodCapCmd(CookService cook){ this.cook=cook; }

    @Override public String name(){ return "최대치"; }
    @Override public List<String> aliases(){ return List.of("cap"); }
    @Override public String permission(){ return "cook.admin"; }

    @Override
    public boolean execute(CommandSender s, String label, String[] a) {
        if (a.length < 2) {
            s.sendMessage("§e/"+label+" 최대치 <레시피ID> <포만감> [포화도]");
            s.sendMessage("§7예) /"+label+" 최대치 pixel_munch 4칸 1.0");
            return true;
        }
        String recipeId = a[0];
        CookRecipe r = cook.recipes().get(recipeId);
        if (r == null) { s.sendMessage("§c레시피 없음: " + recipeId); return true; }

        try {
            Integer maxFood = parseFoodPoints(a[1]); // "4칸" -> 8, "7" -> 7
            Float   maxSat  = null;
            if (a.length >= 3) maxSat = Float.parseFloat(a[2]);

            r.maxFood = maxFood;
            r.maxSat  = maxSat;
            cook.saveRecipes();

            s.sendMessage("§a최대치 설정 완료: §f" + recipeId
                + " §7포만감=" + (maxFood==null? "제한없음" : maxFood + "p")
                + ", 포화도=" + (maxSat==null? "제한없음" : maxSat));
        } catch (Exception ex) {
            s.sendMessage("§c입력이 올바르지 않습니다. 예) /"+label+" 최대치 pixel_munch 4칸 1.0");
        }
        return true;
    }

    private Integer parseFoodPoints(String s) {
        String t = s.toLowerCase(Locale.ROOT).trim();
        if (t.endsWith("칸")) {
            String num = t.substring(0, t.length()-1).trim();
            int bars = Integer.parseInt(num);
            return Math.max(0, Math.min(20, bars * 2)); // 1칸=2p
        } else if (t.equals("무제한") || t.equals("x") || t.equals("-")) {
            return null; // 제한없음
        } else {
            int p = Integer.parseInt(t);
            return Math.max(0, Math.min(20, p));
        }
    }
}