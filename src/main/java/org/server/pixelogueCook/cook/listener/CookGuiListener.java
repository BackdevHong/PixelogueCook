package org.server.pixelogueCook.cook.listener;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
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
                    p.openInventory(new RecipeListInventory(list).getInventory());
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
                    // 재료 수집
                    List<ItemStack> grids = new ArrayList<>();
                    for (int s : CookInventory.GRID){
                        ItemStack it = e.getInventory().getItem(s);
                        if (it != null && !it.getType().isAir()) grids.add(it.clone());
                    }
                    // 자동 매칭 시도
                    String err = cook.startCookAuto(p, grids);
                    if (err != null) {
                        p.sendMessage("§c" + err);
                        return; // 재료는 소비하지 않음
                    }
                    // 시작 성공 시 재료 소비(상단 인벤에서 제거) + 닫기
                    for (int s : CookInventory.GRID) e.getInventory().setItem(s, null);
                    p.closeInventory();
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
            String title = cur.hasItemMeta() && Objects.requireNonNull(cur.getItemMeta()).hasDisplayName()
                ? cur.getItemMeta().getDisplayName() : "";
            int l = title.lastIndexOf('('), r = title.lastIndexOf(')');
            if (l >= 0 && r > l) {
                String rid = title.substring(l+1, r);
                var recipe = cook.recipes().get(rid);
                if (recipe != null) {
                    p.openInventory(new RecipeDetailInventory(recipe).getInventory());
                }
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

        // 1) 허용 안 된 음식은 차단(경고)
        if (!allowed) {
            e.setCancelled(true);
            p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("§c이 음식은 먹을 수 없습니다."));
            p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 0.6f);
            return;
        }

        // 2) 허용 음식이지만 커스텀 회복값이 없으면 바닐라 그대로 적용
        if ((addFood == null || addFood <= 0) && (addSat == null || addSat <= 0f)) {
            return;
        }

        // 3) 커스텀 회복값이 있으면 바닐라를 취소하고 직접 적용
        e.setCancelled(true);

        // 수량 1개 소모 처리 (주로 메인/보조손 체크)
        consumeOneFromHands(p, item);

        // 회복 적용 (허기: 최대 20, 포화도: 현재 허기 한도까지)
        int curFood = p.getFoodLevel();
        float curSat = p.getSaturation();

        int deltaFood = Math.max(0, addFood == null ? 0 : addFood);
        float deltaSat = Math.max(0f, addSat == null ? 0f : addSat);

        int newFood = Math.min(20, curFood + deltaFood);
        float newSat = Math.min(newFood, curSat + deltaSat);

        p.setFoodLevel(newFood);
        p.setSaturation(newSat);

        // 스튜류 빈 그릇 반환(선택)
        Material t = item.getType();
        if (t == Material.MUSHROOM_STEW || t == Material.RABBIT_STEW || t == Material.BEETROOT_SOUP || t == Material.SUSPICIOUS_STEW) {
            p.getInventory().addItem(new ItemStack(Material.BOWL));
        }

        // 피드백
        p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("§a허기 +" + deltaFood + " §7/ §b포화도 +" + (deltaSat % 1f == 0 ? String.valueOf((int)deltaSat) : String.format("%.1f", deltaSat))));
        p.playSound(p.getLocation(), Sound.ENTITY_GENERIC_EAT, 0.8f, 1.0f);
    }

    private void consumeOneFromHands(Player p, ItemStack consumed) {
        // 메인핸드
        ItemStack main = p.getInventory().getItemInMainHand();
        if (main != null && main.isSimilar(consumed)) {
            int amt = main.getAmount() - 1;
            if (amt <= 0) p.getInventory().setItemInMainHand(null);
            else { main.setAmount(amt); p.getInventory().setItemInMainHand(main); }
            return;
        }
        // 보조핸드
        ItemStack off = p.getInventory().getItemInOffHand();
        if (off != null && off.isSimilar(consumed)) {
            int amt = off.getAmount() - 1;
            if (amt <= 0) p.getInventory().setItemInOffHand(null);
            else { off.setAmount(amt); p.getInventory().setItemInOffHand(off); }
        }
    }
}