package org.server.pixelogueCook.cook.util;

import org.bukkit.NamespacedKey;
import org.server.pixelogueCook.PixelogueCook;

public final class FoodKeys {
    private FoodKeys(){}
    public static NamespacedKey foodAllowed(PixelogueCook p){ return new NamespacedKey(p,"food-allowed"); }
    public static NamespacedKey dishGrade(PixelogueCook p){ return new NamespacedKey(p,"grade"); }
    public static NamespacedKey recipeId(PixelogueCook p){ return new NamespacedKey(p,"recipe-id"); }
    public static NamespacedKey recipeBookId(PixelogueCook p){ return new NamespacedKey(p,"recipe-book-id"); }
}