package org.server.pixelogueCook.cook.listener;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.server.pixelogueCook.PixelogueCook;
import org.server.pixelogueCook.cook.inventories.CookInventory;
import org.server.pixelogueCook.cook.inventories.RecipeDetailInventory;
import org.server.pixelogueCook.cook.inventories.RecipeListInventory;
import org.server.pixelogueCook.cook.service.CookService;
import org.server.pixelogueCook.cook.util.FoodKeys;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class CookGuiListener implements Listener {
    private final PixelogueCook plugin;
    private final CookService cook;
    public CookGuiListener(PixelogueCook plugin, CookService cook){ this.plugin=plugin; this.cook=cook; }

    @EventHandler
    public void onClick(InventoryClickEvent e){
        if (!(e.getWhoClicked() instanceof Player p)) return;

        // === 요리 GUI ===
        if (e.getInventory().getHolder() instanceof CookInventory) {
            int slot = e.getRawSlot();
            if (slot < e.getView().getTopInventory().getSize()) {
                boolean grid = false;
                for (int s : CookInventory.GRID) if (s==slot) { grid=true; break; }

                if (slot == CookInventory.BOOK) {
                    e.setCancelled(true);
                    var list = cook.learned(p.getUniqueId()).stream()
                        .map(id -> cook.recipes().get(id))
                        .filter(Objects::nonNull).collect(Collectors.toList());
                    p.openInventory(new RecipeListInventory(list, plugin).getInventory());
                    return;
                }

                if (slot == CookInventory.CLEAR) {
                    e.setCancelled(true);
                    for (int s : CookInventory.GRID){
                        ItemStack it = e.getInventory().getItem(s);
                        if (it != null && it.getType()!= Material.AIR) {
                            p.getInventory().addItem(it);
                            e.getInventory().setItem(s, null);
                        }
                    }
                    p.sendMessage("요리 재료를 비웠습니다.");
                    return;
                }

                if (slot == CookInventory.CONFIRM) {
                    e.setCancelled(true);

                    // 1) 그리드 수집
                    List<ItemStack> grids = new ArrayList<>();
                    for (int s2 : CookInventory.GRID){
                        ItemStack it = e.getInventory().getItem(s2);
                        if (it != null && !it.getType().isAir()) grids.add(it.clone());
                    }

                    // 2) 계획 수립
                    Object planOrErr = cook.planAuto(p, grids);
                    if (planOrErr instanceof String errMsg) {
                        p.sendMessage("§c" + errMsg);
                        return;
                    }
                    CookService.Plan plan = (CookService.Plan) planOrErr;

                    // 3) 재료 소비: 필요한 만큼만 차감, 남는 건 돌려주기
                    // 필요 수량 맵: needLeft[mat] = req * batches
                    Map<Material, Integer> needLeft = new java.util.HashMap<>();
                    for (var en : plan.recipe().ingredients.entrySet()) {
                        needLeft.put(en.getKey(), en.getValue() * plan.batches());
                    }

                    for (int gridSlot : CookInventory.GRID) {
                        ItemStack cur = e.getInventory().getItem(gridSlot);
                        if (cur == null || cur.getType().isAir()) continue;

                        Material m = cur.getType();
                        int need = needLeft.getOrDefault(m, 0);
                        if (need <= 0) {
                            // 이 재료는 더 이상 필요 없음 → 그대로 플레이어에게 반환
                            p.getInventory().addItem(cur);
                            e.getInventory().setItem(gridSlot, null);
                            continue;
                        }

                        int have = cur.getAmount();
                        if (have <= need) {
                            // 전량 소모
                            needLeft.put(m, need - have);
                            e.getInventory().setItem(gridSlot, null);
                        } else {
                            // 일부만 소모
                            cur.setAmount(have - need);
                            e.getInventory().setItem(gridSlot, cur);
                            needLeft.put(m, 0);
                        }
                    }

                    // 4) 혹시 남아버린(필요량 충족 후) 재료가 그리드에 남아있다면 모두 반환
                    for (int s2 : CookInventory.GRID){
                        ItemStack it = e.getInventory().getItem(s2);
                        if (it != null && !it.getType().isAir()){
                            p.getInventory().addItem(it);
                            e.getInventory().setItem(s2, null);
                        }
                    }

                    // 5) 인벤 닫고 조리 시작
                    p.closeInventory();
                    cook.startPlanned(p, plan);
                    return;
                }

                if (!grid) e.setCancelled(true);
            }
        }

        // === 레시피 목록 GUI ===
        if (e.getInventory().getHolder() instanceof RecipeListInventory) {
            e.setCancelled(true);
            ItemStack cur = e.getCurrentItem();
            if (cur == null || cur.getType()==Material.AIR) return;
            var recipe = cook.recipes().get(
                cur.getItemMeta().getPersistentDataContainer()
                    .get(FoodKeys.recipeCheckId(plugin), PersistentDataType.STRING)
            );

            if (recipe != null) {
                p.openInventory(new RecipeDetailInventory(recipe).getInventory());
            }
            return;
        }

        // === 레시피 상세 GUI === (이제 선택 버튼 없음, 단순 확인용)
        if (e.getInventory().getHolder() instanceof RecipeDetailInventory) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e){
        if (!(e.getPlayer() instanceof Player p)) return;
        if (e.getInventory().getHolder() instanceof CookInventory) {
            // 닫을 때 남은 재료 반환
            for (int s : CookInventory.GRID){
                ItemStack it = e.getInventory().getItem(s);
                if (it != null && !it.getType().isAir()){
                    p.getInventory().addItem(it);
                    e.getInventory().setItem(s, null);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent e) {
        ItemStack item = e.getItem();
        if (item == null || !item.getType().isEdible()) return;

        ItemMeta im = item.getItemMeta();
        boolean allowed = false;
        Integer addFood = null;
        Float addSat = null;

        if (im != null) {
            var pdc = im.getPersistentDataContainer();
            Integer flag = pdc.get(FoodKeys.foodAllowed(plugin), PersistentDataType.INTEGER);
            allowed = (flag != null && flag == 1);
            addFood = pdc.get(FoodKeys.foodHunger(plugin), PersistentDataType.INTEGER);
            addSat  = pdc.get(FoodKeys.foodSaturation(plugin), PersistentDataType.FLOAT);
        }

        Player p = e.getPlayer();

        // (1) 허용 안 된 음식은 차단
        if (!allowed) {
            e.setCancelled(true);
            p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("§c이 음식은 먹을 수 없습니다."));
            p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 0.6f);
            return;
        }

        // (2) 커스텀 값이 없으면 바닐라 그대로
        boolean hasCustom = (addFood != null && addFood > 0) || (addSat != null && addSat > 0f);
        if (!hasCustom) return;

        // (3) 바닐라 섭취는 그대로 진행(취소 X). 다음 틱에 커스텀 적용해서 덮어쓰기.
        final int beforeFood = p.getFoodLevel();
        final float beforeSat = p.getSaturation();
        final int deltaFood = Math.max(0, addFood == null ? 0 : addFood);
        final float deltaSat = Math.max(0f, addSat == null ? 0f : addSat);

        // 다음 틱(바닐라가 섭취 처리한 직후)에 원하는 값으로 세팅
        Bukkit.getScheduler().runTask(plugin, () -> {
            int targetFood = Math.min(20, beforeFood + deltaFood);
            float targetSat = Math.min(targetFood, beforeSat + deltaSat);

            p.setFoodLevel(targetFood);
            p.setSaturation(targetSat);

            // 피드백
            String satText = (deltaSat % 1f == 0f) ? String.valueOf((int) deltaSat) : String.format("%.1f", deltaSat);
            p.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                new TextComponent("§a허기 +" + deltaFood + " §7/ §b포화도 +" + satText));
            p.playSound(p.getLocation(), Sound.ENTITY_GENERIC_EAT, 0.8f, 1.0f);
        });
    }
}