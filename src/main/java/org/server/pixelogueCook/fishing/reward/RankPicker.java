package org.server.pixelogueCook.fishing.reward;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;
import org.server.pixelogueCook.fishing.model.FishRank;
import org.server.pixelogueCook.fishing.model.RodTier;
import org.server.pixelogueCook.fishing.util.WeightedTable;

public final class RankPicker {
    public static FishRank pick(JavaPlugin plugin, RodTier tier){
        var cfg = plugin.getConfig();
        var base = cfg.getConfigurationSection("fish_rank_weights");
        var multi = cfg.getConfigurationSection("rod_tier_multipliers." + tier.name());

        double wS = weight(base,"S") * mult(multi,"S");
        double wA = weight(base,"A") * mult(multi,"A");
        double wB = weight(base,"B") * mult(multi,"B");
        double wC = weight(base,"C") * mult(multi,"C");
        double wD = weight(base,"D") * mult(multi,"D");

        return new WeightedTable<FishRank>()
            .add(wS, FishRank.S)
            .add(wA, FishRank.A)
            .add(wB, FishRank.B)
            .add(wC, FishRank.C)
            .add(wD, FishRank.D)
            .pick();
    }

    private static double weight(ConfigurationSection s, String k){ return s == null ? 1.0 : s.getDouble(k, 1.0); }
    private static double mult(ConfigurationSection s, String k){ return s == null ? 1.0 : s.getDouble(k, 1.0); }
}