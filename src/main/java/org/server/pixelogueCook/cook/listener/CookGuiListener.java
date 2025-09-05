package org.server.pixelogueCook.cook.listener;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;
import org.server.pixelogueCook.PixelogueCook;
import org.server.pixelogueCook.cook.inventories.CookInventory;
import org.server.pixelogueCook.cook.inventories.RecipeDetailInventory;
import org.server.pixelogueCook.cook.inventories.RecipeListInventory;
import org.server.pixelogueCook.cook.service.CookService;

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
}