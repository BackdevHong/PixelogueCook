package org.server.pixelogueCook.cook.command.sub.food;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.server.pixelogueCook.cook.command.AbstractSubCommand;
import org.server.pixelogueCook.cook.data.CookRecipe;
import org.server.pixelogueCook.cook.service.CookService;
import org.server.pixelogueCook.farm.data.Grade;

import java.util.List;
import java.util.Locale;

public class FoodGiveCmd extends AbstractSubCommand {
    private final CookService cook;

    public FoodGiveCmd(CookService cook) { this.cook = cook; }

    @Override public String name(){ return "지급"; }
    @Override public List<String> aliases(){ return List.of("give"); }
    @Override public String permission(){ return "cook.admin"; }

    @Override
    public boolean execute(CommandSender s, String label, String[] args) {
        if (args.length < 1) {
            s.sendMessage("§e/"+label+" 지급 <레시피ID> [등급] [플레이어]");
            return true;
        }

        String recipeId = args[0];
        String gradeArg = (args.length >= 2) ? args[1] : null;

        Player target;
        if (args.length >= 3) {
            target = Bukkit.getPlayerExact(args[2]);
        } else {
            if (!(s instanceof Player p)) { s.sendMessage("플레이어를 지정해야 합니다."); return true; }
            target = p;
        }

        CookRecipe recipe = cook.recipes().get(recipeId);
        if (recipe == null) {
            s.sendMessage("§c해당 레시피를 찾을 수 없습니다: " + recipeId);
            return true;
        }

        Grade grade = parseGradeOrNull(gradeArg); // 선택값: 없거나 파싱 실패 시 null
        ItemStack dish = cook.buildDish(recipeId, grade);
        if (dish == null) {
            s.sendMessage("§c완성품 생성에 실패했습니다: " + recipeId);
            return true;
        }

        target.getInventory().addItem(dish);
        s.sendMessage("§a" + target.getName() + "님에게 " + recipeId
            + (grade != null ? " (등급 " + grade.name() + ")" : "")
            + " 음식이 지급되었습니다.");
        return true;
    }

    // 등급 파싱 유틸 (Enum 이름과 대소문자 무관)
    private Grade parseGradeOrNull(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            // 예: "s+" -> "SS" 같은 매핑이 필요하면 여기서 추가
            String norm = s.toUpperCase(Locale.ROOT).replace("+", "S"); // S+ -> SS
            return Grade.valueOf(norm);
        } catch (Exception ignored) {
            return null;
        }
    }
}