package org.server.pixelogueCook.cook.util;

import org.bukkit.NamespacedKey;
import org.server.pixelogueCook.PixelogueCook;

public final class FoodKeys {
    private FoodKeys(){}
    public static NamespacedKey foodAllowed(PixelogueCook p){ return new NamespacedKey(p,"food-allowed"); }
    public static NamespacedKey dishGrade(PixelogueCook p){ return new NamespacedKey(p,"grade"); }
    public static NamespacedKey recipeId(PixelogueCook p){ return new NamespacedKey(p,"recipe-id"); }
    public static NamespacedKey recipeBookId(PixelogueCook p){ return new NamespacedKey(p,"recipe-book-id"); }

    public static NamespacedKey recipeCheckId(PixelogueCook p) { return new NamespacedKey(p,"recipe-check-id"); }
    // ✅ 커스텀 회복값
    public static NamespacedKey foodHunger(PixelogueCook p){ return new NamespacedKey(p,"food-hunger"); }         // int (추가 허기)
    public static NamespacedKey foodSaturation(PixelogueCook p){ return new NamespacedKey(p,"food-saturation"); } // float (추가 포화도)
}