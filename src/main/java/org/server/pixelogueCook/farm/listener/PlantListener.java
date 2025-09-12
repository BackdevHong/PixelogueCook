package org.server.pixelogueCook.farm.listener;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.server.pixelogueCook.PixelogueCook;
import org.server.pixelogueCook.farm.service.CropService;

public class PlantListener implements Listener {
    private final PixelogueCook plugin;
    private final CropService crops;
    public PlantListener(PixelogueCook plugin, CropService crops) {
        this.plugin = plugin; this.crops = crops;
    }
    @EventHandler
    public void onPlace(BlockPlaceEvent e) {
        Block b = e.getBlockPlaced();
        if (!crops.isSupportedCrop(b.getType())) return;
        if (b.getRelative(0, -1, 0).getType() != Material.FARMLAND) return;

        crops.get(b).ifPresent(ci -> {}); // no-op
        plugin.getServer().getScheduler().runTask(plugin, () -> crops.create(b));
    }

    @EventHandler
    public void onEntityChangeBlock(EntityChangeBlockEvent e) {
        Block b = e.getBlock();
        if (b.getType() == Material.FARMLAND) {
            // to가 무엇이든 FARMLAND 유지
            e.setCancelled(true);
        }
    }

    // 플레이어의 물리적 상호작용(밟기)도 보조적으로 차단
    @EventHandler
    public void onPhysicalInteract(PlayerInteractEvent e) {
        if (e.getAction() != Action.PHYSICAL || e.getClickedBlock() == null) return;
        if (e.getClickedBlock().getType() == Material.FARMLAND) {
            e.setCancelled(true);
        }
    }
}