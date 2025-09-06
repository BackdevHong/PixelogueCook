package org.server.pixelogueCook.cook.service;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.server.pixelogueCook.PixelogueCook;
import org.server.pixelogueCook.cook.data.CookRecipe;
import org.server.pixelogueCook.cook.util.FoodKeys;
import org.server.pixelogueCook.farm.data.Grade;

import javax.annotation.Nullable;
import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CookService {
    private final PixelogueCook plugin;
    private final Map<String, CookRecipe> recipes = new ConcurrentHashMap<>();
    private final Map<UUID, Set<String>> learned = new ConcurrentHashMap<>();
    private final Map<UUID, Long> cookingUntil = new ConcurrentHashMap<>();

    public CookService(PixelogueCook plugin) { this.plugin = plugin; }

    public Map<String, CookRecipe> recipes(){ return recipes; }
    public Set<String> learned(UUID pid){ return learned.computeIfAbsent(pid, k->new LinkedHashSet<>()); }

    // ====== 저장/로드 (기존 그대로) ======
    private File recipesFile(){ return new File(plugin.getDataFolder(), "recipes.yml"); }
    private File learnedFile(){ return new File(plugin.getDataFolder(), "learned.yml"); }

    public void saveRecipes(){
        try {
            YamlConfiguration y = new YamlConfiguration();
            List<Map<String,Object>> list = new ArrayList<>();
            for (var r : recipes.values()){
                Map<String,Object> m = new LinkedHashMap<>();
                m.put("id", r.id);
                m.put("name", r.displayName);
                m.put("cookSec", r.cookMillis/1000L);
                m.put("result", r.resultTemplate);

                // ✅ Material 이름으로 저장
                Map<String,Integer> ing = new LinkedHashMap<>();
                for (var e : r.ingredients.entrySet()) {
                    ing.put(e.getKey().name(), e.getValue());
                }
                m.put("ingredients", ing);

                list.add(m);
            }
            y.set("recipes", list);
            y.save(recipesFile());
        } catch (Exception ex){
            plugin.getLogger().warning("Save recipes failed: "+ex.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    public void loadRecipes() {
        recipes.clear();
        File f = recipesFile(); if (!f.exists()) return;
        YamlConfiguration y = YamlConfiguration.loadConfiguration(f);

        for (Map<?, ?> m : y.getMapList("recipes")) {   // ← 여기!
            try {
                String id   = String.valueOf(m.get("id"));
                String name = String.valueOf(m.get("name"));
                long cookMs = ((Number)m.get("cookSec")).longValue()*1000L;
                ItemStack tmpl = (ItemStack) m.get("result");
                if (tmpl == null) continue;

                CookRecipe r = new CookRecipe(id, name, tmpl, cookMs);

                // ingredients: Map<String, Integer> 기대
                Object rawIng = m.get("ingredients");
                if (rawIng instanceof Map<?, ?> mm) {
                    for (Map.Entry<?, ?> e : mm.entrySet()) {
                        Material mat = Material.matchMaterial(String.valueOf(e.getKey()));
                        int amt = ((Number) e.getValue()).intValue();
                        if (mat != null && amt > 0) r.ingredients.put(mat, amt);
                    }
                } else {
                    // (선택) 구버전 v2: List<ItemStack> → 타입별 합산
                    Object v2 = m.get("ingredientsV2");
                    if (v2 instanceof List<?> list) {
                        Map<Material,Integer> conv = new LinkedHashMap<>();
                        for (Object o : list) {
                            if (o instanceof ItemStack it && !it.getType().isAir()) {
                                conv.merge(it.getType(), it.getAmount(), Integer::sum);
                            }
                        }
                        r.ingredients.putAll(conv);
                    }
                }

                recipes.put(id, r);
            } catch (Exception ignore) {}
        }
    }

    public void saveLearned(){
        try{
            YamlConfiguration y = new YamlConfiguration();
            for (var e : learned.entrySet()) {
                y.set(e.getKey().toString(), new ArrayList<>(e.getValue()));
            }
            y.save(learnedFile());
        }catch (Exception ex){ plugin.getLogger().warning("Save learned failed: "+ex.getMessage()); }
    }
    public void loadLearned(){
        learned.clear();
        File f = learnedFile(); if (!f.exists()) return;
        YamlConfiguration y = YamlConfiguration.loadConfiguration(f);
        for (String key : y.getKeys(false)){
            try {
                UUID u = UUID.fromString(key);
                List<String> list = y.getStringList(key);
                learned.put(u, new LinkedHashSet<>(list));
            } catch (Exception ignore){}
        }
    }

    // ===== 등록: 손에 든 음식으로 =====
    public String registerFromHand(Player p, String id, long cookSec){
        ItemStack hand = p.getInventory().getItemInMainHand();
        if (hand == null || hand.getType().isAir()) return "손에 든 아이템이 없습니다.";
        if (!hand.getType().isEdible()) return "먹을 수 있는 아이템만 등록할 수 있습니다.";
        if (recipes.containsKey(id)) return "이미 존재하는 레시피 ID 입니다.";
        ItemStack tmpl = hand.clone(); tmpl.setAmount(1);
        String name = (tmpl.hasItemMeta() && tmpl.getItemMeta().hasDisplayName())
            ? tmpl.getItemMeta().getDisplayName() : tmpl.getType().name();
        CookRecipe r = new CookRecipe(id, name, tmpl, cookSec*1000L);
        recipes.put(id, r); saveRecipes();
        return null;
    }

    // ===== 배우기/확인 =====
    public ItemStack makeRecipeBook(String recipeId) {
        CookRecipe r = recipes.get(recipeId);
        if (r == null) return null;

        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        ItemMeta im = book.getItemMeta();
        if (im == null) return null;

        im.setDisplayName("§e레시피 북: §f" +  r.id);
        im.setLore(List.of(
            "§7우클릭하면 이 레시피를 배웁니다.",
            "§8조리시간: " + (r.cookMillis / 1000) + "초"
        ));
        im.getPersistentDataContainer().set(
            FoodKeys.recipeBookId(plugin),
            PersistentDataType.STRING,
            r.id
        );
        book.setItemMeta(im);
        return book;
    }
    public boolean learn(UUID pid, String recipeId){
        if (!recipes.containsKey(recipeId)) return false;
        boolean added = learned(pid).add(recipeId);
        if (added) saveLearned();
        return added;
    }
    public boolean hasLearned(UUID pid, String recipeId){ return learned(pid).contains(recipeId); }

    // ===== 자동 매칭 모드 =====
    public enum MatchMode { EXACT, CONTAINS }
    private MatchMode matchMode() {
        try { return MatchMode.valueOf(plugin.getConfig().getString("cook.match-mode","EXACT").toUpperCase()); }
        catch (Exception e) { return MatchMode.EXACT; }
    }

    private boolean matchesContains(List<ItemStack> grid, Map<Material,Integer> reqs) {
        // 플레이어 풀을 타입별 수량으로 합산
        Map<Material,Integer> pool = new HashMap<>();
        for (ItemStack it : grid) {
            if (it == null || it.getType().isAir()) continue;
            pool.merge(it.getType(), it.getAmount(), Integer::sum);
        }
        // 요구 수량을 모두 충족해야 함 (여분 허용)
        for (var e : reqs.entrySet()) {
            int have = pool.getOrDefault(e.getKey(), 0);
            if (have < e.getValue()) return false;
        }
        return true;
    }

    private boolean matchesExact(List<ItemStack> grid, Map<Material,Integer> reqs) {
        if (!matchesContains(grid, reqs)) return false;

        // EXACT: 남는 게 없어야 함 (총합 동일 + 각 타입 수량 동일)
        Map<Material,Integer> pool = new HashMap<>();
        int totalGrid = 0, totalReq = 0;

        for (ItemStack it : grid) {
            if (it == null || it.getType().isAir()) continue;
            pool.merge(it.getType(), it.getAmount(), Integer::sum);
            totalGrid += it.getAmount();
        }
        for (int v : reqs.values()) totalReq += v;

        if (totalGrid != totalReq) return false;
        for (var e : reqs.entrySet()) {
            if (pool.getOrDefault(e.getKey(), 0) != e.getValue()) return false;
        }
        // pool에 reqs에 없는 타입이 남아있을 가능성은 total 비교에서 이미 배제됨
        return true;
    }

    private CookRecipe findMatch(UUID pid, List<ItemStack> grid){
        var learnedIds = learned(pid);
        if (grid == null || grid.isEmpty() || learnedIds.isEmpty()) return null;
        var mode = matchMode();

        List<CookRecipe> candidates = new ArrayList<>();
        for (String id : learnedIds) {
            CookRecipe r = recipes.get(id);
            if (r == null || r.ingredients.isEmpty()) continue;

            boolean ok = (mode==MatchMode.EXACT)
                ? matchesExact(grid, r.ingredients)
                : matchesContains(grid, r.ingredients);

            if (ok) candidates.add(r);
        }
        if (candidates.isEmpty()) return null;
        if (candidates.size()==1) return candidates.get(0);

        // 여러 개면 요구 총수량(∑amount) 큰 레시피 우선
        candidates.sort((a,b)-> Integer.compare(
            b.ingredients.values().stream().mapToInt(Integer::intValue).sum(),
            a.ingredients.values().stream().mapToInt(Integer::intValue).sum()
        ));
        return candidates.get(0);
    }

    // ===== 자동 시작 =====
    public String startCookAuto(org.bukkit.entity.Player p, java.util.List<org.bukkit.inventory.ItemStack> grid){
        long now = System.currentTimeMillis();
        if (cookingUntil.getOrDefault(p.getUniqueId(), 0L) > now) return "이미 요리 중입니다.";
        CookRecipe r = findMatch(p.getUniqueId(), grid);
        if (r == null) return "해당 조합의 레시피를 찾을 수 없습니다.";
        Grade finalGrade = calcFinalGradeFromIngredients(grid);

        cookingUntil.put(p.getUniqueId(), now + r.cookMillis);
        p.sendMessage("요리를 시작했습니다: " + r.id + " (" + (r.cookMillis/1000) + "초, 등급 " + finalGrade.name() + ")");

        // ✅ 완료 예약 시 등급 함께 전달
        Bukkit.getScheduler().runTaskLater(plugin, () -> completeCook(p, r.id, finalGrade), r.cookMillis/50L);
        return null;
    }

    private void completeCook(Player p, String recipeId, Grade grade){
        cookingUntil.remove(p.getUniqueId());
        ItemStack dish = buildDish(recipeId, grade);
        if (dish == null) return;

        p.getInventory().addItem(dish);
        p.sendMessage("완성되었습니다: " + recipeId + " (등급 " + grade.name() + ")");
    }

    public ItemStack buildDish(String recipeId, @Nullable Grade grade) {
        CookRecipe r = recipes.get(recipeId);
        if (r == null) return null;

        ItemStack dish = r.resultTemplate.clone();
        ItemMeta im = dish.getItemMeta();
        if (im == null) im = Bukkit.getItemFactory().getItemMeta(dish.getType());

        String baseName = r.id;
        String display = "§f" + baseName;
        if (grade != null) display += " §7(등급 §6" + grade.name() + "§7)";
        im.setDisplayName(display);

        if (grade != null) {
            List<String> lore = im.hasLore() ? im.getLore() : new java.util.ArrayList<>();
            lore.add("§7등급: §6" + grade.name());
            // 보여주기용: 회복 수치도 로어에 표기
            lore.add("§7허기: §a+" + hungerByGrade(grade) + " §7포화도: §b+" + saturationByGrade(grade));
            im.setLore(lore);
        }

        var pdc = im.getPersistentDataContainer();
        pdc.set(FoodKeys.foodAllowed(plugin), PersistentDataType.INTEGER, 1);
        pdc.set(FoodKeys.recipeId(plugin), PersistentDataType.STRING, r.id);

        // ✅ 커스텀 회복값 저장
        int hunger = (grade != null) ? hungerByGrade(grade) : 0;
        float sat   = (grade != null) ? saturationByGrade(grade) : 0f;
        if (hunger > 0) pdc.set(FoodKeys.foodHunger(plugin), PersistentDataType.INTEGER, hunger);
        if (sat > 0f)   pdc.set(FoodKeys.foodSaturation(plugin), PersistentDataType.FLOAT, sat);

        dish.setItemMeta(im);
        return dish;
    }

    // 등급별 기본 회복값(예시) — 필요 시 config로 이동해도 됩니다.
    private int hungerByGrade(Grade g){
        return switch (g){
            case S -> 10; // 허기 +10
            case A -> 8;
            case B -> 6;
            case C -> 4;
            case D -> 2;
        };
    }
    private float saturationByGrade(Grade g){
        return switch (g){
            case S -> 4.0f; // 포화도 +4.0
            case A -> 3.0f;
            case B -> 2.0f;
            case C -> 1.0f;
            case D -> 0.5f;
        };
    }


    private int scoreOf(Grade g){
        return switch (g){
            case S -> 5; case A -> 4; case B -> 3; case C -> 2; case D -> 1;
        };
    }
    private Grade gradeOf(int score){
        return switch (Math.max(1, Math.min(5, score))){
            case 5 -> Grade.S;
            case 4 -> Grade.A;
            case 3 -> Grade.B;
            case 2 -> Grade.C;
            default -> Grade.D;
        };
    }

    // config: cook-grade-rule = MIN | AVERAGE (기본 MIN)
    private enum GradeRule { MIN, AVERAGE }
    private GradeRule gradeRule(){
        try {
            return GradeRule.valueOf(getPlugin().getConfig().getString("cook-grade-rule","MIN").toUpperCase());
        } catch (Exception e) { return GradeRule.MIN; }
    }
    private PixelogueCook getPlugin(){ return this.plugin; } // 편의

    private Grade readGradeFromItem(ItemStack it){
        if (it == null || it.getType().isAir()) return Grade.C;
        var im = it.getItemMeta();
        if (im != null){
            String g = im.getPersistentDataContainer().get(FoodKeys.dishGrade(plugin), PersistentDataType.STRING);
            if (g != null){
                try { return Grade.valueOf(g); } catch (Exception ignored){}
            }
        }
        return Grade.C;
    }

    // CookService.java 내부에 추가: ✅ 재료 등급 → 최종 등급 계산
    public Grade calcFinalGradeFromIngredients(java.util.Collection<ItemStack> ingredients){
        if (ingredients == null) return Grade.C;

        // amount만큼 등급을 반복 반영
        java.util.List<Grade> pool = new java.util.ArrayList<>();
        for (ItemStack it : ingredients){
            if (it == null || it.getType().isAir()) continue;
            Grade g = readGradeFromItem(it);
            int times = Math.max(1, it.getAmount());
            for (int i=0;i<times;i++) pool.add(g);
        }
        if (pool.isEmpty()) return Grade.C;

        if (gradeRule() == GradeRule.MIN){
            // 최솟값 규칙: S > A > B > C > D 중 가장 낮은 등급 반환
            Grade out = Grade.S;
            for (Grade g : pool){
                if (scoreOf(g) < scoreOf(out)) out = g;
            }
            return out;
        } else {
            // 평균 규칙: 매핑 후 평균을 반올림
            int sum = 0;
            for (Grade g : pool) sum += scoreOf(g);
            double avg = (double) sum / pool.size();
            return gradeOf((int)Math.round(avg));
        }
    }
}