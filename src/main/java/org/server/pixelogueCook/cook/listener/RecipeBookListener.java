package org.server.pixelogueCook.cook.listener;


import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.persistence.PersistentDataType;
import org.server.pixelogueCook.PixelogueCook;
import org.server.pixelogueCook.cook.service.CookService;
import org.server.pixelogueCook.cook.util.FoodKeys;

public class RecipeBookListener implements Listener {
    private final PixelogueCook plugin;
    private final CookService cook;
    public RecipeBookListener(PixelogueCook plugin, CookService cook){ this.plugin=plugin; this.cook=cook; }

    @EventHandler
    public void onUse(PlayerInteractEvent e){
        if (e.getItem()==null || e.getItem().getType()!= Material.WRITTEN_BOOK) return;
        var im = e.getItem().getItemMeta(); if (im==null) return;
        String id = im.getPersistentDataContainer().get(FoodKeys.recipeBookId(plugin), PersistentDataType.STRING);
        if (id == null) return;

        boolean added = cook.learn(e.getPlayer().getUniqueId(), id);
        if (added) {
            e.getPlayer().sendMessage("새로운 레시피를 배웠습니다!");
            e.getItem().setAmount(e.getItem().getAmount()-1); // 책 소비
        } else {
            e.getPlayer().sendMessage("이미 배운 레시피입니다.");
        }
    }

    @org.bukkit.event.EventHandler
    public void onQuit(org.bukkit.event.player.PlayerQuitEvent e){ cook.onQuit(e.getPlayer()); }
    @org.bukkit.event.EventHandler
    public void onJoin(org.bukkit.event.player.PlayerJoinEvent e){ cook.onJoin(e.getPlayer()); }
}