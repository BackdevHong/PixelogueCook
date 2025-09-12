package org.server.pixelogueCook.cook.service;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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

    private enum BatchTimeMode { PER_BATCH, FIXED }
    private enum CapMode { CLIP, SCALE }
    private enum OfflineMode { PAUSE, DELIVER_ON_JOIN }

    private final Map<UUID, Integer> taskIds = new ConcurrentHashMap<>();

    private File cookingFile(){ return new File(plugin.getDataFolder(), "cooking.yml"); }

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

                Map<String,Integer> ing = new LinkedHashMap<>();
                for (var e : r.ingredients.entrySet()) ing.put(e.getKey().name(), e.getValue());
                m.put("ingredients", ing);

                // ✅ [추가] 최대치 저장(있을 때만)
                if (r.maxFood != null) m.put("maxFood", r.maxFood);
                if (r.maxSat  != null) m.put("maxSat",  r.maxSat);

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

        for (Map<?, ?> m : y.getMapList("recipes")) {
            try {
                String id   = String.valueOf(m.get("id"));
                String name = String.valueOf(m.get("name"));
                long cookMs = ((Number)m.get("cookSec")).longValue()*1000L;
                ItemStack tmpl = (ItemStack) m.get("result");
                if (tmpl == null) continue;

                CookRecipe r = new CookRecipe(id, name, tmpl, cookMs);

                Object rawIng = m.get("ingredients");
                if (rawIng instanceof Map<?, ?> mm) {
                    for (Map.Entry<?, ?> e : mm.entrySet()) {
                        Material mat = Material.matchMaterial(String.valueOf(e.getKey()));
                        int amt = ((Number) e.getValue()).intValue();
                        if (mat != null && amt > 0) r.ingredients.put(mat, amt);
                    }
                } else {
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

                // ✅ [추가] 최대치 로드(있을 때만)
                Object mf = m.get("maxFood");
                if (mf instanceof Number n) r.maxFood = n.intValue();
                Object ms = m.get("maxSat");
                if (ms instanceof Number n) r.maxSat = n.floatValue();

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
        if (hand.getType().isAir()) return "손에 든 아이템이 없습니다.";
        if (!hand.getType().isEdible()) return "먹을 수 있는 아이템만 등록할 수 있습니다.";
        if (recipes.containsKey(id)) return "이미 존재하는 레시피 ID 입니다.";

        ItemStack tmpl = hand.clone(); tmpl.setAmount(1);

        // ✅ 템플릿의 안내 로어 제거(또는 전부 초기화)
        ItemMeta im = tmpl.getItemMeta();
        if (im != null) {
            if (im.hasLore()) {
                List<String> filtered = new ArrayList<>();
                for (String line : im.getLore()) {
                    String plain = ChatColor.stripColor(line);
                    if (plain == null) plain = line;
                    if (plain.contains("조리시간") || plain.contains("클릭하여 재료 확인")) continue;
                    filtered.add(line);
                }
                im.setLore(filtered.isEmpty() ? null : filtered);
            }
            // 혹시 전체 초기화를 원하면 위 블록 대신
            // im.setLore(null);

            tmpl.setItemMeta(im);
        }

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
            if (!Objects.equals(pool.getOrDefault(e.getKey(), 0), e.getValue())) return false;
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
        if (candidates.size()==1) return candidates.getFirst();

        // 여러 개면 요구 총수량(∑amount) 큰 레시피 우선
        candidates.sort((a,b)-> Integer.compare(
            b.ingredients.values().stream().mapToInt(Integer::intValue).sum(),
            a.ingredients.values().stream().mapToInt(Integer::intValue).sum()
        ));
        return candidates.getFirst();
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

        // ✅ (A안) 특정 안내 로어만 제거
        if (im.hasLore()) {
            List<String> filtered = new ArrayList<>();
            for (String line : im.getLore()) {
                String plain = ChatColor.stripColor(line);
                if (plain == null) plain = line;
                if (plain.contains("조리시간") || plain.contains("클릭하여 재료 확인")) continue;
                filtered.add(line);
            }
            im.setLore(filtered.isEmpty() ? null : filtered);
        }

        String baseName = r.id;
        String display = "§f" + baseName;
        if (grade != null) display += " §7(등급 §6" + grade.name() + "§7)";
        im.setDisplayName(display);

        List<String> lore = im.hasLore() ? new ArrayList<>(im.getLore()) : new ArrayList<>();
        if (grade != null) {
            lore.add("§7등급: §6" + grade.name());
        }
        im.setLore(lore);

        // ✅ 최종 회복량 계산 (요리별 최대치 반영)
        int   hunger = (grade != null) ? computeHungerFor(r, grade) : 0;
        float sat    = (grade != null) ? computeSaturationFor(r, grade) : 0f;


        // ✅ PDC 저장(섭취 리스너에서 이 값을 사용)
        var pdc = im.getPersistentDataContainer();
        pdc.set(FoodKeys.foodAllowed(plugin), PersistentDataType.INTEGER, 1);
        pdc.set(FoodKeys.recipeId(plugin), PersistentDataType.STRING, r.id);
        if (hunger > 0) pdc.set(FoodKeys.foodHunger(plugin), PersistentDataType.INTEGER, hunger);
        if (sat > 0f)   pdc.set(FoodKeys.foodSaturation(plugin), PersistentDataType.FLOAT, sat);

        // (선택) 보기용 로어에 표기
        if (grade != null) {
            pdc.set(FoodKeys.dishGrade(plugin), PersistentDataType.STRING, grade.name());
            lore = im.getLore(); if (lore == null) lore = new ArrayList<>();
            lore.add("§7허기: §a+" + hunger + " §7포화도: §b+" + (sat % 1f == 0 ? (int)sat : String.format(java.util.Locale.ROOT,"%.1f", sat)));
            // (선택) 최대치도 노출하고 싶으면:
            if (r.maxFood != null) lore.add("§8최대 포만감: " + r.maxFood + "p" + " (" + (r.maxFood/2) + "칸)");
            if (r.maxSat  != null) lore.add("§8최대 포화도: " + (r.maxSat % 1f == 0 ? String.valueOf(r.maxSat.intValue()) : String.format(java.util.Locale.ROOT,"%.1f", r.maxSat)));
            im.setLore(lore);
        }

        dish.setItemMeta(im);
        return dish;
    }

    private BatchTimeMode batchTimeMode() {
        try {
            return BatchTimeMode.valueOf(plugin.getConfig()
                .getString("cook.batch-time-mode", "PER_BATCH").toUpperCase());
        } catch (Exception e) { return BatchTimeMode.PER_BATCH; }
    }

    private int maxBatchPerCook() {
        try {
            return Math.max(1, plugin.getConfig().getInt("cook.max-batch-per-cook", 16));
        } catch (Exception e) { return 16; }
    }

    // grid(3x3)로 만들 수 있는 최대 배치 수 계산: floor(min(pool[mat]/req[mat]))
    private int maxBatchFromGrid(List<ItemStack> grid, Map<Material,Integer> reqs) {
        if (grid == null || grid.isEmpty() || reqs == null || reqs.isEmpty()) return 0;

        Map<Material,Integer> pool = new HashMap<>();
        for (ItemStack it : grid) {
            if (it == null || it.getType().isAir()) continue;
            pool.merge(it.getType(), it.getAmount(), Integer::sum);
        }

        int batches = Integer.MAX_VALUE;
        for (var e : reqs.entrySet()) {
            int have = pool.getOrDefault(e.getKey(), 0);
            int need = Math.max(1, e.getValue());
            if (have < need) return 0; // 하나도 못 만듦
            batches = Math.min(batches, have / need);
        }
        if (batches == Integer.MAX_VALUE) return 0;
        return Math.max(0, Math.min(batches, maxBatchPerCook()));
    }

    // 조리 계획 객체
        public record Plan(CookRecipe recipe, Grade finalGrade, int batches, long cookMillisTotal) {
    }

    /**
     * 자동 매칭 + 배치 수 계산까지 해서 계획을 돌려줍니다.
     * 실패 시 String(에러메시지) 반환.
     */
    public Object planAuto(Player p, List<ItemStack> grid) {
        long now = System.currentTimeMillis();
        if (cookingUntil.getOrDefault(p.getUniqueId(), 0L) > now) return "이미 요리 중입니다.";

        CookRecipe r = findMatch(p.getUniqueId(), grid);
        if (r == null) return "해당 조합의 레시피를 찾을 수 없습니다.";

        int batches = maxBatchFromGrid(grid, r.ingredients);
        if (batches <= 0) return "재료가 부족합니다.";

        Grade finalGrade = calcFinalGradeFromIngredients(grid);

        long total;
        if (batchTimeMode() == BatchTimeMode.PER_BATCH) {
            total = r.cookMillis * (long) batches;
        } else {
            total = r.cookMillis;
        }
        return new Plan(r, finalGrade, batches, total);
    }

    // 계획을 실제 시작으로 전환(타이머 설정 및 지급)
    public void startPlanned(Player p, Plan plan) {
        long now = System.currentTimeMillis();
        long end = now + plan.cookMillisTotal;
        cookingUntil.put(p.getUniqueId(), end);

        // pending 등록
        PersistCook pc = new PersistCook();
        pc.playerId = p.getUniqueId();
        pc.recipeId = plan.recipe().id;
        pc.grade = plan.finalGrade() == null ? null : plan.finalGrade().name();
        pc.batches = Math.max(1, plan.batches());
        pc.remainMs = plan.cookMillisTotal();
        pending.put(p.getUniqueId(), pc);
        saveActiveCooks();

        p.sendMessage("요리를 시작했습니다: " + plan.recipe().id + " x" + plan.batches()
            + " (" + (plan.cookMillisTotal()/1000) + "초, 등급 " + plan.finalGrade().name() + ")");

        scheduleFinish(p.getUniqueId(), plan.cookMillisTotal());
    }

    private CapMode capMode() {
        try {
            return CapMode.valueOf(plugin.getConfig()
                .getString("cook.cap-mode", "SCALE").toUpperCase());
        } catch (Exception e) {
            return CapMode.SCALE;
        }
    }

    // 등급별 기본 테이블은 기존 hungerByGrade/saturationByGrade 사용
    private int computeHungerFor(CookRecipe r, Grade g){
        int base = hungerByGrade(g);
        Integer cap = r.maxFood;
        if (cap == null || cap <= 0) return base;

        if (capMode() == CapMode.CLIP) {
            return Math.min(base, cap);
        } else {
            int baseMax = hungerByGrade(Grade.S);
            if (baseMax <= 0) return Math.min(base, cap);
            double f = cap / (double) baseMax;
            int scaled = (int) Math.round(base * f);
            if (base > 0 && scaled <= 0) scaled = 1; // 최소 1포인트 보장
            return Math.min(scaled, cap);
        }
    }

    private float computeSaturationFor(CookRecipe r, Grade g){
        float base = saturationByGrade(g);
        Float cap = r.maxSat;
        if (cap == null || cap <= 0f) return base;

        if (capMode() == CapMode.CLIP) {
            return Math.min(base, cap);
        } else {
            float baseMax = saturationByGrade(Grade.S);
            if (baseMax <= 0f) return Math.min(base, cap);
            float f = cap / baseMax;
            float scaled = Math.round(base * f * 10f) / 10f; // 소수1자리 반올림
            if (base > 0f && scaled <= 0f) scaled = 0.5f;    // 최소 0.5 보장(원하면 조절)
            return Math.min(scaled, cap);
        }
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

    private OfflineMode offlineMode() {
        try {
            return OfflineMode.valueOf(plugin.getConfig()
                .getString("cook.offline-mode","PAUSE").toUpperCase());
        } catch (Exception e) { return OfflineMode.PAUSE; }
    }

    public static final class PersistCook {
        public UUID playerId;
        public String recipeId;
        public String grade;   // "S","A",... (없으면 null)
        public int batches = 1;
        public long remainMs;
    }
    private final Map<UUID, PersistCook> pending = new ConcurrentHashMap<>();


    private void scheduleFinish(UUID pid, long delayMs) {
        cancelTask(pid);
        int id = Bukkit.getScheduler().runTaskLater(plugin, () -> finishCooking(pid), delayMs / 50L).getTaskId();
        taskIds.put(pid, id);
    }
    private void cancelTask(UUID pid){
        Integer id = taskIds.remove(pid);
        if (id != null) Bukkit.getScheduler().cancelTask(id);
    }

    private void giveResult(Player p, PersistCook pc) {
        Grade g = null;
        try { if (pc.grade != null) g = Grade.valueOf(pc.grade); } catch (Exception ignored) {}
        for (int i=0;i<Math.max(1, pc.batches); i++) {
            ItemStack dish = buildDish(pc.recipeId, g);
            if (dish != null) p.getInventory().addItem(dish);
        }
        p.sendMessage("완성되었습니다: " + pc.recipeId + " x" + Math.max(1, pc.batches)
            + (pc.grade!=null? " (등급 " + pc.grade + ")" : ""));
    }

    private void cleanup(UUID pid){
        cancelTask(pid);
        cookingUntil.remove(pid);
        pending.remove(pid);
        saveActiveCooks();
    }

    // 완료 시 호출: 온라인/오프라인 정책 반영
    private void finishCooking(UUID pid) {
        PersistCook pc = pending.get(pid);
        if (pc == null) return;

        Player p = Bukkit.getPlayer(pid);
        boolean online = (p != null && p.isOnline());

        if (!online) {
            if (offlineMode() == OfflineMode.PAUSE) {
                // 일시정지: remainMs는 0, 재접속 시 즉시 지급 대신 "재개"로 처리하고 싶다면 시간을 유지해도 됨
                pc.remainMs = Math.max(0, pc.remainMs); // 그대로 두기
                saveActiveCooks();
                return;
            } else { // DELIVER_ON_JOIN
                pc.remainMs = 0; // 대기지급 표시
                saveActiveCooks();
                return;
            }
        }

        // 온라인이면 즉시 지급
        giveResult(p, pc);
        cleanup(pid);
    }

    public void onQuit(Player p){
        UUID pid = p.getUniqueId();
        PersistCook pc = pending.get(pid);
        if (pc == null) return;

        if (offlineMode() == OfflineMode.PAUSE) {
            long now = System.currentTimeMillis();
            long until = cookingUntil.getOrDefault(pid, now);
            long remain = Math.max(0L, until - now);
            pc.remainMs = remain;
            cancelTask(pid);
            saveActiveCooks();
        } else {
            // DELIVER_ON_JOIN: 완료될 때 finishCooking이 대기지급으로 표기
            cancelTask(pid);
            saveActiveCooks();
        }
    }

    public void onJoin(Player p){
        UUID pid = p.getUniqueId();
        PersistCook pc = pending.get(pid);
        if (pc == null) return;

        if (offlineMode() == OfflineMode.PAUSE) {
            if (pc.remainMs <= 0) {
                giveResult(p, pc);
                cleanup(pid);
            } else {
                cookingUntil.put(pid, System.currentTimeMillis() + pc.remainMs);
                scheduleFinish(pid, pc.remainMs);
                p.sendMessage("§7일시정지된 요리를 재개합니다... (남은 " + (pc.remainMs/1000) + "초)");
            }
        } else { // DELIVER_ON_JOIN
            if (pc.remainMs <= 0) {
                giveResult(p, pc);
                cleanup(pid);
            } else {
                scheduleFinish(pid, pc.remainMs);
            }
        }
    }

    public void saveActiveCooks(){
        try {
            YamlConfiguration y = new YamlConfiguration();
            for (var e : pending.entrySet()){
                String k = e.getKey().toString();
                PersistCook pc = e.getValue();
                y.set(k + ".recipeId", pc.recipeId);
                y.set(k + ".grade", pc.grade);
                y.set(k + ".batches", pc.batches);
                y.set(k + ".remainMs", Math.max(0, pc.remainMs));
            }
            y.save(cookingFile());
        } catch (Exception ex){ plugin.getLogger().warning("Save cooking failed: "+ex.getMessage()); }
    }

    public void loadActiveCooks(){
        pending.clear();
        File f = cookingFile(); if (!f.exists()) return;
        YamlConfiguration y = YamlConfiguration.loadConfiguration(f);
        for (String key : y.getKeys(false)){
            try {
                UUID pid = UUID.fromString(key);
                PersistCook pc = new PersistCook();
                pc.playerId = pid;
                pc.recipeId = y.getString(key+".recipeId");
                pc.grade = y.getString(key+".grade", null);
                pc.batches = Math.max(1, y.getInt(key+".batches", 1));
                pc.remainMs = Math.max(0, y.getLong(key+".remainMs", 0));
                if (pc.recipeId != null) pending.put(pid, pc);
            } catch (Exception ignore){}
        }
    }
}