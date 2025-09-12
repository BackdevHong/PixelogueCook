package org.server.pixelogueCook.fishing.game;

import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;

public interface MiniGame {
    Player player();
    void start();
    void onClick(PlayerInteractEvent e);
    void cancel(String reason);
}