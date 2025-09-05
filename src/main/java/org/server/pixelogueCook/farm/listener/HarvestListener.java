package org.server.pixelogueCook.farm.listener;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.server.pixelogueCook.PixelogueCook;
import org.server.pixelogueCook.farm.data.CropType;
import org.server.pixelogueCook.farm.data.Grade;
import org.server.pixelogueCook.farm.service.CropService;
import org.server.pixelogueCook.farm.util.ItemUtil;

public class HarvestListener implements Listener {
    private final PixelogueCook plugin;
    private final CropService crops;

    public HarvestListener(PixelogueCook plugin, CropService crops) {
        this.plugin = plugin;
        this.crops = crops;
    }

    @EventHandler
    public void onBreak(BlockBreakEvent e) {
        Block b = e.getBlock();
        CropType ct = CropType.from(b.getType());
        if (ct == null) return;

        var opt = crops.get(b);
        if (opt.isEmpty()) return;

        var ci = opt.get();

        if (!ci.grown) {
            e.setCancelled(true);
            e.getPlayer().sendMessage("아직 자라지 않았습니다!");
            return;
        }

        // 바닐라 드롭 막기
        e.setDropItems(false);

        // 등급 보상 아이템 (기존 로직)
        Grade grade = crops.drawGrade(ci.fertilizer);
        ItemStack gradeDrop = ItemUtil.makeGradeDrop(plugin, ct.name(), grade, ci.fertilizer, e.getPlayer());

        // 재심기용 아이템(씨앗/작물) 1개
        ItemStack replantItem = new ItemStack(ct.replantItem, 1);

        if (e.getPlayer().getGameMode() == GameMode.CREATIVE) {
            // 인벤으로 직접 지급
            e.getPlayer().getInventory().addItem(gradeDrop);
            e.getPlayer().getInventory().addItem(replantItem);
        } else {
            // 자연 드롭
            var dropLoc = b.getLocation().add(0.5, 0.2, 0.5);
            b.getWorld().dropItemNaturally(dropLoc, gradeDrop);
            b.getWorld().dropItemNaturally(dropLoc, replantItem);
        }

        // 블록 제거 및 정리
        e.setCancelled(true);
        b.setType(Material.AIR);

        plugin.hologramService().removeHologram(ci);
        crops.remove(b);
    }
}