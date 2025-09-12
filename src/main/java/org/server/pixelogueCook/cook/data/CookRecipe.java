package org.server.pixelogueCook.cook.data;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CookRecipe {
    public final String id;
    public String displayName;
    public ItemStack resultTemplate;  // 결과 아이템 템플릿(손에 든 음식)
    public long cookMillis;

    // ✅ 재료: 각 ItemStack의 amount가 "필요 수량"을 의미
    public final Map<Material, Integer> ingredients = new LinkedHashMap<>();

    public Integer maxFood = null;
    public Float   maxSat  = null;

    public CookRecipe(String id, String displayName, ItemStack resultTemplate, long cookMillis) {
        this.id = id;
        this.displayName = displayName;
        this.resultTemplate = resultTemplate;
        this.cookMillis = cookMillis;
    }
}