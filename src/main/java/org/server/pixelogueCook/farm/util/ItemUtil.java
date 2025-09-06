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
import java.util.Locale;

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
                tier.toUpperCase(Locale.ROOT)
        );
        it.setItemMeta(im);
        return it;
    }

    // ---------- 핵심: 블록/문자열을 '아이템 머티리얼'로 정규화 ----------
    private static Material toItemMaterial(Material any) {
        // 블록/아이템 모두 입력 가능
        return switch (any) {
            // 밀
            case WHEAT, WHEAT_SEEDS -> Material.WHEAT;

            // 당근
            case CARROTS, CARROT -> Material.CARROT;

            // 감자
            case POTATOES, POTATO -> Material.POTATO;

            // 비트
            case BEETROOTS, BEETROOT_SEEDS, BEETROOT -> Material.BEETROOT;

            // 네더 사마귀
            case NETHER_WART -> Material.NETHER_WART;

            // 코코아(블록) / 코코아콩(아이템)
            case COCOA, COCOA_BEANS -> Material.COCOA_BEANS;

            default -> any.isItem() ? any : Material.WHEAT; // 폴백
        };
    }

    private static Material toItemMaterial(String key) {
        if (key == null) return Material.WHEAT;
        Material m = Material.matchMaterial(key);
        if (m != null) return toItemMaterial(m);

        // 문자열이 머티리얼로 안 잡히는 경우 대비
        return switch (key.toUpperCase(Locale.ROOT)) {
            case "CARROTS"   -> Material.CARROT;
            case "POTATOES"  -> Material.POTATO;
            case "BEETROOTS" -> Material.BEETROOT;
            case "COCOA"     -> Material.COCOA_BEANS;
            case "WHEAT"     -> Material.WHEAT;
            case "NETHER_WART" -> Material.NETHER_WART;
            default -> Material.WHEAT;
        };
    }

    // ---------- 아이템 머티리얼 → 한국어 표시명 ----------
    private static String cropDisplayName(PixelogueCook plugin, Material itemMat) {
        // config에서 우선
        String cfg = plugin.getConfig().getString("crop-display-names." + itemMat.name());
        if (cfg != null && !cfg.isEmpty()) return cfg;

        // 기본값 (아이템 머티리얼 기준)
        return switch (itemMat) {
            case WHEAT       -> "밀";
            case CARROT      -> "당근";
            case POTATO      -> "감자";
            case BEETROOT    -> "비트";
            case NETHER_WART -> "네더사마귀";
            case COCOA_BEANS -> "코코아콩";
            default          -> itemMat.name();
        };
    }

    // ---------- 드롭 생성: Material 버전(권장) ----------
    public static ItemStack makeGradeDrop(PixelogueCook plugin,
                                          Material cropBlockOrItem, // 블록/아이템 무엇이든 가능
                                          Grade grade,
                                          FertilizerTier fert,
                                          Player harvester) {

        Material itemMat = toItemMaterial(cropBlockOrItem);   // 블록→아이템 정규화
        String displayCrop = cropDisplayName(plugin, itemMat); // 한국어 이름

        String nameFmt = plugin.getConfig().getString("grade-item.name-format", "&f%crop% (등급 %grade%)");
        String name = nameFmt.replace("%crop%", displayCrop).replace("%grade%", grade.name());

        ItemStack it = new ItemStack(itemMat, 1);
        ItemMeta im = it.getItemMeta();
        im.setDisplayName(ColorUtil.color(name));
        im.getPersistentDataContainer().set(new NamespacedKey(plugin, "grade"), PersistentDataType.STRING, grade.name());
        it.setItemMeta(im);
        return it;
    }

    // ---------- 드롭 생성: String 버전(호환용) ----------
    public static ItemStack makeGradeDrop(PixelogueCook plugin,
                                          String cropKey,
                                          Grade grade,
                                          FertilizerTier fert,
                                          Player harvester) {
        return makeGradeDrop(plugin, toItemMaterial(cropKey), grade, fert, harvester);
    }
}