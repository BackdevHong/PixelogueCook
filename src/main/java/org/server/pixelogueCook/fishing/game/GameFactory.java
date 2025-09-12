package org.server.pixelogueCook.fishing.game;

import org.bukkit.entity.Player;
import org.server.pixelogueCook.fishing.FishingGameManager;

@FunctionalInterface
public interface GameFactory {
    MiniGame create(Player player, FishingGameManager mgr);
}