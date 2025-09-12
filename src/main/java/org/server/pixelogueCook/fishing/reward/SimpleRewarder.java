package org.server.pixelogueCook.fishing.reward;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.server.pixelogueCook.fishing.model.FishRank;
import org.server.pixelogueCook.fishing.util.FishItemUtil;

import java.util.concurrent.ThreadLocalRandom;

public final class SimpleRewarder implements Rewarder {

    // 익힌/양동이 제외: 생물고기 4종만
    private static final Material[] FISH_POOL = new Material[] {
        Material.COD, Material.SALMON, Material.PUFFERFISH, Material.TROPICAL_FISH
    };

    @Override
    public void give(Player p, FishRank rank) {
        Material base = FISH_POOL[ThreadLocalRandom.current().nextInt(FISH_POOL.length)];
        ItemStack it = new ItemStack(base);

        // 이름은 FishItemUtil에서 한국어 표기 + 포맷으로 설정
        FishItemUtil.setRank(it, rank);

        p.getInventory().addItem(it);
    }
}