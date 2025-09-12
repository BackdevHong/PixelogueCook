package org.server.pixelogueCook.fishing.util;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;

public final class Keys {
    public static final NamespacedKey ROD_TIER =
        new NamespacedKey(JavaPlugin.getProvidingPlugin(Keys.class), "rod_tier");
    public static final NamespacedKey FISH_RANK =
        new NamespacedKey(JavaPlugin.getProvidingPlugin(Keys.class), "fish_rank");

    private Keys(){}
}