package org.server.pixelogueCook.farm.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.server.pixelogueCook.farm.data.FertilizerTier;
import org.server.pixelogueCook.farm.data.Grade;

import java.util.EnumMap;
import java.util.Map;
import java.util.Random;

public class ProbTable {
    private final Map<Grade, Double> base = new EnumMap<>(Grade.class);
    private final Map<FertilizerTier, Map<Grade, Double>> mult = new EnumMap<>(FertilizerTier.class);
    private final Random rng = new Random();

    public ProbTable(FileConfiguration cfg) {
        ConfigurationSection baseSec = cfg.getConfigurationSection("base-prob");
        if (baseSec != null) {
            for (Grade g : Grade.values()) base.put(g, baseSec.getDouble(g.name(), 0));
        }
        ConfigurationSection multSec = cfg.getConfigurationSection("fertilizer-multiplier");
        if (multSec != null) {
            for (String key : multSec.getKeys(false)) {
                FertilizerTier t = FertilizerTier.valueOf(key);
                Map<Grade, Double> m = new EnumMap<>(Grade.class);
                ConfigurationSection s = multSec.getConfigurationSection(key);
                for (Grade g : Grade.values()) m.put(g, s.getDouble(g.name(), 1.0));
                mult.put(t, m);
            }
        }
        // 기본값 보정
        for (Grade g : Grade.values()) base.putIfAbsent(g, 0.2);
        for (FertilizerTier t : FertilizerTier.values()) {
            mult.putIfAbsent(t, new EnumMap<>(Grade.class));
            for (Grade g : Grade.values()) mult.get(t).putIfAbsent(g, 1.0);
        }
    }

    public Grade draw(FertilizerTier tier) {
        Map<Grade, Double> mm = mult.getOrDefault(tier, mult.get(FertilizerTier.NONE));
        double sum = 0;
        for (Grade g : Grade.values()) sum += base.get(g) * mm.get(g);
        if (sum <= 0) return Grade.D;
        double r = rng.nextDouble() * sum, acc = 0;
        for (Grade g : Grade.values()) {
            acc += base.get(g) * mm.get(g);
            if (r <= acc) return g;
        }
        return Grade.D;
    }
}