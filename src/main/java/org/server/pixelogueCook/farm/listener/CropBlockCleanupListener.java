package org.server.pixelogueCook.farm.listener;

import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.server.pixelogueCook.farm.data.CropType;
import org.server.pixelogueCook.farm.service.CropService;

import java.util.Objects;

public class CropBlockCleanupListener implements Listener {
    private final CropService crops;
    public CropBlockCleanupListener(CropService crops) { this.crops = crops; }

    private void tryCleanup(Block b) {
        var opt = crops.get(b);
        if (opt.isPresent()) {
            crops.remove(b);
        }
    }

    private boolean wasCrop(Block b) {
        return CropType.from(b.getType()) != null;
    }

    @EventHandler
    public void onBlockPhysics(BlockPhysicsEvent e) {
        Block b = e.getBlock();
        if (wasCrop(b) && b.getType() != Objects.requireNonNull(CropType.from(b.getType())).block) tryCleanup(b);
    }

    @EventHandler
    public void onFluid(BlockFromToEvent e) {
        if (wasCrop(e.getToBlock())) tryCleanup(e.getToBlock());
    }

    @EventHandler
    public void onBurn(BlockBurnEvent e) {
        if (wasCrop(e.getBlock())) tryCleanup(e.getBlock());
    }

    @EventHandler
    public void onExplode(EntityExplodeEvent e) {
        e.blockList().removeIf(b -> {
            if (wasCrop(b)) tryCleanup(b);
            return false;
        });
    }

    @EventHandler
    public void onPistonExtend(BlockPistonExtendEvent e) {
        for (Block b : e.getBlocks()) if (wasCrop(b)) tryCleanup(b);
    }

    @EventHandler
    public void onPistonRetract(BlockPistonRetractEvent e) {
        for (Block b : e.getBlocks()) if (wasCrop(b)) tryCleanup(b);
    }
}