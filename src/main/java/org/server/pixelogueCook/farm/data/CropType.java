package org.server.pixelogueCook.farm.data;

import org.bukkit.Material;

public enum CropType {
    WHEAT(Material.WHEAT, Material.WHEAT_SEEDS),
    POTATO(Material.POTATOES, Material.POTATO),
    CARROT(Material.CARROTS, Material.CARROT),
    BEETROOT(Material.BEETROOTS, Material.BEETROOT_SEEDS),
    NETHER_WART(Material.NETHER_WART, Material.NETHER_WART),
    COCOA(Material.COCOA, Material.COCOA_BEANS);

    public final Material block;
    public final Material replantItem;

    CropType(Material block, Material replantItem) {
        this.block = block;
        this.replantItem = replantItem;
    }

    public static CropType from(Material m) {
        for (var ct : values()) if (ct.block == m) return ct;
        return null;
    }
}