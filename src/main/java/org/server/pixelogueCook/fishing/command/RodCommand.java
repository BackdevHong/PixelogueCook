package org.server.pixelogueCook.fishing.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.server.pixelogueCook.fishing.model.RodTier;
import org.server.pixelogueCook.fishing.util.RodUtil;

public class RodCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)){
            sender.sendMessage("Player only.");
            return true;
        }
        if (args.length >= 2 && args[0].equalsIgnoreCase("rod")){
            RodTier tier = RodTier.of(args[1]);
            if (tier == null){
                p.sendMessage("§c사용법: /fishing rod <basic|intermediate|advanced|master>");
                return true;
            }
            p.getInventory().addItem(RodUtil.makeRod(tier));
            p.sendMessage("§a지급: §f" + RodUtil.pretty(tier) + " §7낚시대");
            return true;
        }
        p.sendMessage("§e사용법: §f/fishing rod <basic|intermediate|advanced|master>");
        return true;
    }
}