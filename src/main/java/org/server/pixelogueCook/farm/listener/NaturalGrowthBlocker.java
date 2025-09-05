package org.server.pixelogueCook.farm.listener;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockFormEvent;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.server.pixelogueCook.farm.service.CropService;

public class NaturalGrowthBlocker implements Listener {
    private final CropService crops;
    public NaturalGrowthBlocker(CropService crops) { this.crops = crops; }

    @EventHandler
    public void onGrow(BlockGrowEvent e) {
        if (crops.isSupportedCrop(e.getBlock().getType())) e.setCancelled(true);
    }

    // 바닐라 본밀로 성장시키는 상호작용도 무력화(아이템 소비는 클라/서버 상태에 따라 달라질 수 있음)
    @EventHandler
    public void onUseBoneMeal(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK || e.getClickedBlock() == null) return;
        if (!crops.isSupportedCrop(e.getClickedBlock().getType())) return;
        var it = e.getItem();
        if (it != null && it.getType() == Material.BONE_MEAL) {
            // 우리 시스템 외 본밀 성장 방지
            e.setCancelled(true);
        }
    }
}
