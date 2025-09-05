package org.server.pixelogueCook.farm.util;


import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.server.pixelogueCook.PixelogueCook;
import org.server.pixelogueCook.farm.data.FertilizerTier;
import org.server.pixelogueCook.farm.data.Grade;

import java.util.ArrayList;
import java.util.List;


public class ItemUtil {
    public static final String KEY_FERT_ITEM = "fertilizer-tier";

    public static ItemStack makeFertilizerItem(PixelogueCook plugin, String tier) {
        FileConfiguration cfg = plugin.getConfig();
        String display = cfg.getString("fertilizer-items." + tier, tier + " Fertilizer");
        ItemStack it = new ItemStack(Material.BONE_MEAL);
        ItemMeta im = it.getItemMeta();
        im.setDisplayName(ColorUtil.color("&b" + display));
        im.getPersistentDataContainer().set(
            new NamespacedKey(plugin, KEY_FERT_ITEM),
            PersistentDataType.STRING,
            tier.toUpperCase()
        );
        it.setItemMeta(im);
        return it;
    }

    private static String cropDisplayName(PixelogueCook plugin, String cropName) {
        String cfg = plugin.getConfig().getString("crop-display-names." + cropName);
        if (cfg != null && !cfg.isEmpty()) return cfg;

        // 폴백(기본값)
        return switch (cropName) {
            case "WHEAT"     -> "밀";
            case "CARROTS"   -> "당근";
            case "POTATOES"  -> "감자";
            case "BEETROOTS" -> "비트";
            case "NETHER_WART" -> "네더사마귀";
            case "COCOA" -> "코코아콩";
            default          -> cropName; // 알 수 없는 작물은 원문 표기
        };
    }

    private static Material cropMaterial(String cropName) {
        return switch (cropName) {
            case "WHEAT"     -> Material.WHEAT;
            case "CARROTS"   -> Material.CARROT;
            case "POTATOES"  -> Material.POTATO;
            case "BEETROOTS" -> Material.BEETROOT;
            case "NETHER_WART" ->  Material.NETHER_WART;
            case "COCOA" -> Material.COCOA;
            default          -> Material.WHEAT; // 폴백
        };
    }

    public static ItemStack makeGradeDrop(PixelogueCook plugin, String cropName, Grade grade,
                                          FertilizerTier fert, Player harvester) {
        var cfg = plugin.getConfig();

        // 이름 포맷에 %crop%가 들어가는데, 여기서 한글 표시명으로 치환
        String nameFmt = cfg.getString("grade-item.name-format", "&f%crop% (등급 %grade%)");
        String displayCrop = cropDisplayName(plugin, cropName);
        String name = nameFmt
            .replace("%crop%", displayCrop)   // ← 한글 표시명 적용!
            .replace("%grade%", grade.name());

        // 씨앗 대신 ‘작물 본품’으로 드랍
        Material mat = cropMaterial(cropName);
        ItemStack it = new ItemStack(mat, 1); // 등급별 수량 조절 원하면 amount 계산 추가

        ItemMeta im = it.getItemMeta();
        im.setDisplayName(ColorUtil.color(name)); // ← 커스텀 한글 이름 지정
        im.getPersistentDataContainer().set(
            new NamespacedKey(plugin, "grade"), PersistentDataType.STRING, grade.name()
        );
        it.setItemMeta(im);

        return it;
    }
}
