package org.server.pixelogueCook.cook.inventories;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.server.pixelogueCook.cook.data.CookRecipe;

import java.util.List;

public class RecipeListInventory implements InventoryHolder {
    private final Inventory inv;

    public RecipeListInventory(List<CookRecipe> recipes) {
        inv = Bukkit.createInventory(this, 54, "내 레시피 북");
        ItemStack pane = named(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i=0;i<54;i++) inv.setItem(i, pane);
        int idx=0;
        for (CookRecipe r : recipes){
            if (idx>=45) break; // 5줄만 채우고 마지막 줄은 여유
            ItemStack icon = new ItemStack(r.resultTemplate.getType());
            ItemMeta im = icon.getItemMeta();
            im.setDisplayName("§f"+r.displayName+" §7("+r.id+")");
            im.setLore(List.of("§7클릭하여 재료 확인", "§8조리시간: "+(r.cookMillis/1000)+"초"));
            icon.setItemMeta(im);
            inv.setItem(idx++, icon);
        }
    }
    @Override public Inventory getInventory(){ return inv; }

    private ItemStack named(Material m, String name){
        ItemStack it = new ItemStack(m);
        ItemMeta im = it.getItemMeta();
        im.setDisplayName(name);
        it.setItemMeta(im);
        return it;
    }
}