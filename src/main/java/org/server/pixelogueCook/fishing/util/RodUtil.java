package org.server.pixelogueCook.fishing.util;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.server.pixelogueCook.fishing.model.RodTier;

public final class RodUtil {
    private RodUtil(){}

    public static RodTier getTier(ItemStack item){
        if (item == null || item.getType() != Material.FISHING_ROD) return RodTier.BASIC;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return RodTier.BASIC;
        String v = meta.getPersistentDataContainer().get(Keys.ROD_TIER, PersistentDataType.STRING);
        if (v == null) return RodTier.BASIC;
        RodTier t = RodTier.of(v);
        return t == null ? RodTier.BASIC : t;
    }

    public static ItemStack makeRod(RodTier tier){
        ItemStack rod = new ItemStack(Material.FISHING_ROD);
        ItemMeta m = rod.getItemMeta();
        m.setDisplayName("§f" + pretty(tier) + " §7낚시대");
        m.getPersistentDataContainer().set(Keys.ROD_TIER, PersistentDataType.STRING, tier.name());
        rod.setItemMeta(m);
        return rod;
    }

    public static String pretty(RodTier t){
        return switch (t){
            case BASIC -> "일반";
            case INTERMEDIATE -> "중급";
            case ADVANCED -> "고급";
            case MASTER -> "초고급";
        };
    }
}