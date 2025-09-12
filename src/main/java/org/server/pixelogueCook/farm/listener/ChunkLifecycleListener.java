package org.server.pixelogueCook.farm.listener;

import org.bukkit.entity.ArmorStand;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

public class ChunkLifecycleListener implements Listener {
    @EventHandler
    public void onDamage(EntityDamageEvent e) {
        if (e.getEntity() instanceof ArmorStand a && a.isMarker()) {
            e.setCancelled(true);
        }
    }
}