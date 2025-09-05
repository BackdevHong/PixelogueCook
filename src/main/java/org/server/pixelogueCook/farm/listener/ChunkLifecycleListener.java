package org.server.pixelogueCook.farm.listener;

import org.bukkit.entity.ArmorStand;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.server.pixelogueCook.farm.data.CropInstance;
import org.server.pixelogueCook.farm.service.CropService;
import org.server.pixelogueCook.farm.service.HologramService;

public class ChunkLifecycleListener implements Listener {
    private final CropService crops;
    private final HologramService holo;

    public ChunkLifecycleListener(CropService crops, HologramService holo) {
        this.crops = crops; this.holo = holo;
    }

    @EventHandler
    public void onUnload(ChunkUnloadEvent e) {
        var c = e.getChunk();
        for (CropInstance ci : crops.all()) {
            if (!ci.worldId.equals(c.getWorld().getUID())) continue;
            if ((ci.x >> 4) == c.getX() && (ci.z >> 4) == c.getZ()) {
                holo.removeHologram(ci);
            }
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent e) {
        if (e.getEntity() instanceof ArmorStand a && a.isMarker()) {
            e.setCancelled(true);
        }
    }
}