package org.server.pixelogueCook.cook.inventories;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.server.pixelogueCook.cook.data.CookRecipe;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class RecipeDetailInventory implements InventoryHolder {
    private final CookRecipe recipe;
    private final Inventory inv;

    public RecipeDetailInventory(CookRecipe recipe) {
        this.recipe = recipe;
        this.inv = Bukkit.createInventory(this, 54, "레시피: " + recipe.id);
        init();
    }

    public CookRecipe recipe(){ return recipe; }
    @Override public Inventory getInventory(){ return inv; }

    private void init(){
        ItemStack pane = named(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i=0;i<54;i++) inv.setItem(i, pane);

        int slot = 0;
        for (Map.Entry<Material, Integer> e : recipe.ingredients.entrySet()) {
            if (slot >= 45) break; // 아래 한 줄(컨트롤 버튼용) 비워두기

            Material mat = e.getKey();
            int need = Math.max(1, e.getValue());

            ItemStack icon = new ItemStack(mat);
            int shown = Math.min(icon.getMaxStackSize(), need); // 스택 초과 방지
            icon.setAmount(shown);

            ItemMeta im = icon.getItemMeta();
            if (im != null) {
                List<String> lore = new ArrayList<>();
                lore.add("§7필요 수량: §e" + need);
                im.setLore(lore);
                icon.setItemMeta(im);
            }
            inv.setItem(slot++, icon);
        }
    }

    private ItemStack named(Material m, String name){
        ItemStack it = new ItemStack(m);
        ItemMeta im = it.getItemMeta();
        if (im != null) {
            im.setDisplayName(name);
            it.setItemMeta(im);
        }
        return it;
    }
}