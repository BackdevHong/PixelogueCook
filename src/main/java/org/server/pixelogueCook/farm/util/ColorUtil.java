package org.server.pixelogueCook.farm.util;

import org.bukkit.ChatColor;

public final class ColorUtil {
    private ColorUtil() {}
    public static String color(String s) {
        return s == null ? null : ChatColor.translateAlternateColorCodes('&', s);
    }
}