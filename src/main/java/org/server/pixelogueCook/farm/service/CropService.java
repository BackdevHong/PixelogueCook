package org.server.pixelogueCook.farm.service;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.server.pixelogueCook.PixelogueCook;
import org.server.pixelogueCook.farm.config.ProbTable;
import org.server.pixelogueCook.farm.data.CropInstance;
import org.server.pixelogueCook.farm.data.CropType;
import org.server.pixelogueCook.farm.data.FertilizerTier;
import org.server.pixelogueCook.farm.data.Grade;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CropService {

    private final PixelogueCook plugin;
    private final Map<String, Long> cropGrowSeconds = new HashMap<>();
    private final Map<String, CropInstance> crops = new ConcurrentHashMap<>();
    private ProbTable prob;

    public CropService(PixelogueCook plugin) {
        this.plugin = plugin;
        reloadSettings();
    }

    public void reloadSettings() {
        cropGrowSeconds.clear();
        ConfigurationSection cs = plugin.getConfig().getConfigurationSection("crops");
        if (cs != null) for (String k : cs.getKeys(false)) cropGrowSeconds.put(k, cs.getLong(k));
        prob = new ProbTable(plugin.getConfig());
    }

    public boolean isSupportedCrop(Material type) { return CropType.from(type) != null; }

    public String keyOf(Block b) { return keyOf(b.getLocation()); }

    public String keyOf(Location l) { return l.getWorld().getUID()+":"+l.getBlockX()+":"+l.getBlockY()+":"+l.getBlockZ(); }

    public Optional<CropInstance> get(Block b) { return Optional.ofNullable(crops.get(keyOf(b))); }

    public CropInstance create(Block b) {
        CropType t = CropType.from(b.getType());
        long sec = cropGrowSeconds.getOrDefault(t.name(), 180L);
        CropInstance ci = new CropInstance(b.getLocation(), t, System.currentTimeMillis(), sec * 1000L);
        crops.put(keyOf(b), ci);
        return ci;
    }

    public void remove(Block b) { crops.remove(keyOf(b)); }

    public void markGrown(CropInstance ci) { ci.grown = true; }

    public Grade drawGrade(FertilizerTier tier) { return prob.draw(tier); }

    public FertilizerTier applyFertilizer(Block cropBlock, FertilizerTier tier) {
        CropInstance ci = crops.get(keyOf(cropBlock));
        if (ci == null) return FertilizerTier.NONE;
        if (ci.fertilizer != FertilizerTier.NONE) {
            // 이미 적용됨
            return FertilizerTier.NONE;
        }
        ci.fertilizer = tier;

        // 성장 시간 단축 반영
        double factor = plugin.getConfig().getDouble("fertilizer-time-reduce." + tier.name(), 1.0);
        if (factor < 1.0) {
            long remain = ci.remain(System.currentTimeMillis());
            long reduced = (long)(remain * factor);
            ci.growMillis = ci.elapsed(System.currentTimeMillis()) + reduced;
        }
        return tier;
    }

    public Collection<CropInstance> all() { return crops.values(); }

    public void setGrowSeconds(String cropName, long seconds) { cropGrowSeconds.put(cropName, seconds); }

    // ===== 저장/로드 =====
    private File storeFile() { return new File(plugin.getDataFolder(), "crops.yml"); }

    public void saveAll() {
        try {
            var list = new ArrayList<Map<String, Object>>();
            for (CropInstance ci : crops.values()) {
                var m = new HashMap<String, Object>();
                m.put("world", ci.worldId.toString());
                m.put("x", ci.x); m.put("y", ci.y); m.put("z", ci.z);
                m.put("type", ci.type.name());
                m.put("plantedAt", ci.plantedAtMillis);
                m.put("growMs", ci.growMillis);
                m.put("fertilizer", ci.fertilizer.name());
                m.put("grown", ci.grown);
                list.add(m);
            }
            YamlConfiguration y = new YamlConfiguration();
            y.set("crops", list);
            y.save(storeFile());
        } catch (Exception ex) {
            plugin.getLogger().warning("Crop save failed: " + ex.getMessage());
        }
    }

    public void loadAll() {
        try {
            File f = storeFile();
            if (!f.exists()) return;
            YamlConfiguration y = YamlConfiguration.loadConfiguration(f);
            var list = y.getMapList("crops");
            long now = System.currentTimeMillis();
            for (var m : list) {
                try {
                    UUID world = UUID.fromString(String.valueOf(m.get("world")));
                    int x = (int) m.get("x");
                    int yb = (int) m.get("y");
                    int z = (int) m.get("z");
                    CropType type = CropType.valueOf(String.valueOf(m.get("type")));
                    long planted = ( (Number) m.get("plantedAt") ).longValue();
                    long growMs  = ( (Number) m.get("growMs") ).longValue();
                    Object rawFert = m.get("fertilizer");
                    String fertStr = rawFert == null ? "NONE" : String.valueOf(rawFert);
                    FertilizerTier fert = FertilizerTier.from(fertStr);

                    Object gObj = m.get("grown");
                    boolean grown = (gObj instanceof Boolean b) ? b : Boolean.parseBoolean(String.valueOf(gObj));

                    var w = plugin.getServer().getWorld(world);
                    if (w == null) continue;
                    var loc = new Location(w, x, yb, z);
                    var ci = new CropInstance(loc, type, planted, growMs);
                    ci.fertilizer = fert;
                    ci.grown = grown || (planted + growMs <= now);
                    crops.put(keyOf(loc), ci);
                } catch (Exception ignore) {}
            }
        } catch (Exception ex) {
            plugin.getLogger().warning("Crop load failed: " + ex.getMessage());
        }
    }
}