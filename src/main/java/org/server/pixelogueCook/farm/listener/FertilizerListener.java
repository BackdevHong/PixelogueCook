package org.server.pixelogueCook.farm.listener;

import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.server.pixelogueCook.PixelogueCook;
import org.server.pixelogueCook.farm.data.FertilizerTier;
import org.server.pixelogueCook.farm.service.CropService;
import org.server.pixelogueCook.farm.util.ItemUtil;

public class FertilizerListener implements Listener {
    private final PixelogueCook plugin;
    private final CropService crops;

    public FertilizerListener(PixelogueCook plugin, CropService crops) {
        this.plugin = plugin;
        this.crops = crops;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;
        if (e.getClickedBlock() == null) return;

        Block b = e.getClickedBlock();
        if (!crops.isSupportedCrop(b.getType())) return;

        ItemStack it = e.getItem();
        if (it == null || !it.hasItemMeta()) return;

        var pdc = it.getItemMeta().getPersistentDataContainer();
        String tierStr = pdc.get(new NamespacedKey(plugin, ItemUtil.KEY_FERT_ITEM), PersistentDataType.STRING);
        if (tierStr == null) return;

        FertilizerTier tier = FertilizerTier.from(tierStr);
        FertilizerTier applied = crops.applyFertilizer(b, tier);
        if (applied == FertilizerTier.NONE) {
            e.getPlayer().sendMessage("이미 영양제가 적용된 작물입니다!");
            return;
        }
        Player p = e.getPlayer();
        p.sendMessage("해당 작물에 " + applied + " 영양제를 적용했습니다. 성장 속도가 빨라집니다!");
        it.setAmount(it.getAmount() - 1);
        e.setCancelled(true);
    }
}