package org.server.pixelogueCook.fishing.util;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.server.pixelogueCook.fishing.model.FishRank;

import java.util.ArrayList;
import java.util.List;

public final class FishItemUtil {
    private FishItemUtil(){}

    /**
     * 랭크를 PDC에 저장하고 config의 name-format대로 아이템 이름에 반영.
     * 플레이스홀더:
     *  - %name% / %crop% : 기본 이름(한국어 표기)
     *  - %grade% : S/A/B/C/D
     *  - %color% : 랭크 컬러 코드(§6 등)
     */
    public static void setRank(ItemStack it, FishRank rank){
        if (it == null) return;
        ItemMeta m = it.getItemMeta();
        if (m == null) return;

        // 1) PDC 저장
        m.getPersistentDataContainer().set(Keys.FISH_RANK, PersistentDataType.STRING, rank.name());

        // 2) 이름 구성 (한국어 표기 + 포맷 적용)
        String baseName = prettifyMaterialKo(it.getType()); // 한국어
        String format = JavaPlugin.getProvidingPlugin(FishItemUtil.class)
            .getConfig()
            .getString("fishing.name-format", "&f%name% &7(등급 %color%%grade%&7)");

        String colored = format
            .replace("%name%", baseName)
            .replace("%grade%", rank.name())
            .replace("%color%", color(rank));

        m.setDisplayName(ChatColor.translateAlternateColorCodes('&', colored));
        it.setItemMeta(m);
    }

    public static FishRank getRank(ItemStack it){
        if (it == null) return null;
        ItemMeta m = it.getItemMeta();
        if (m == null) return null;
        String s = m.getPersistentDataContainer().get(Keys.FISH_RANK, PersistentDataType.STRING);
        try { return s == null ? null : FishRank.valueOf(s); }
        catch (Exception ignore){ return null; }
    }

    private static String color(FishRank r){
        return switch (r){
            case S -> "§6"; // 금색
            case A -> "§a";
            case B -> "§b";
            case C -> "§f";
            case D -> "§7";
        };
    }

    /** 한국어 표기 (필요 시 여기에 추가 매핑) */
    private static String prettifyMaterialKo(Material mat){
        return switch (mat){
            case COD -> "대구";
            case SALMON -> "연어";
            case PUFFERFISH -> "복어";
            case TROPICAL_FISH -> "열대어";
            // 그 외는 TitleCase로:
            default -> toTitleCase(mat.name());
        };
    }

    private static String toTitleCase(String s){
        String name = s.toLowerCase().replace('_', ' ');
        String[] parts = name.split(" ");
        StringBuilder sb = new StringBuilder();
        for (String p : parts){
            if (p.isEmpty()) continue;
            sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1)).append(' ');
        }
        return sb.toString().trim();
    }
}