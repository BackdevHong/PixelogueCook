package org.server.pixelogueCook.fishing.reward;

import org.bukkit.entity.Player;
import org.server.pixelogueCook.fishing.model.FishRank;

public interface Rewarder {
    void give(Player p, FishRank rank);
}