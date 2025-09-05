package org.server.pixelogueCook.cook.inventories;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class CookInventory implements InventoryHolder {
    public static final int SIZE=45;
    public static final int[] GRID={10,11,12, 19,20,21, 28,29,30};
    public static final int CONFIRM=24;     // ✅ 시작
    public static final int CLEAR=33;       // ❌ 비우기
    public static final int BOOK=44;        // 📖 레시피 북 확인하기

    private final Inventory inv;

    public CookInventory() {
        inv = Bukkit.createInventory(this, SIZE, "요리");
        init();
    }
    @Override public Inventory getInventory(){ return inv; }

    private void init(){
        ItemStack pane = named(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i=0;i<SIZE;i++) inv.setItem(i, pane);
        for (int s: GRID) inv.setItem(s, null);
        inv.setItem(CONFIRM, named(Material.LIME_CONCRETE, ChatColor.GREEN+"✔ 시작"));
        inv.setItem(CLEAR,   named(Material.RED_CONCRETE,  ChatColor.RED+"✖ 비우기"));
        inv.setItem(BOOK,    named(Material.WRITABLE_BOOK, ChatColor.GOLD+"레시피 북 확인하기"));
    }
    private ItemStack named(Material m, String name){
        ItemStack it = new ItemStack(m);
        ItemMeta im = it.getItemMeta();
        im.setDisplayName(name);
        it.setItemMeta(im);
        return it;
    }
}