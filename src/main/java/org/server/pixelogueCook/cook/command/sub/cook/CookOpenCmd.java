package org.server.pixelogueCook.cook.command.sub.cook;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.server.pixelogueCook.cook.command.AbstractSubCommand;
import org.server.pixelogueCook.cook.inventories.CookInventory;

public class CookOpenCmd extends AbstractSubCommand {
    @Override public String name(){ return "open"; }
    @Override public java.util.List<String> aliases(){ return java.util.List.of("열기"); }
    @Override public String permission(){ return "cook.chef"; }

    @Override
    public boolean execute(CommandSender s, String label, String[] args) {
        if (!(s instanceof Player p)){ s.sendMessage("플레이어만 사용 가능합니다."); return true; }
        p.openInventory(new CookInventory().getInventory()); // 자동 매칭 GUI
        return true;
    }
}