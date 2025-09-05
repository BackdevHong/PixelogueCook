package org.server.pixelogueCook.farm.command;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.server.pixelogueCook.PixelogueCook;
import org.server.pixelogueCook.farm.data.FertilizerTier;
import org.server.pixelogueCook.farm.data.Grade;
import org.server.pixelogueCook.farm.util.ItemUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FarmCommand implements CommandExecutor, TabCompleter {
    private final PixelogueCook plugin;

    public FarmCommand(PixelogueCook plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@Nonnull CommandSender sender,
                             @Nonnull Command command,
                             @Nonnull String label,
                             @Nonnull String[] args) {
        if (!sender.hasPermission("farm.admin")) {
            sender.sendMessage(ChatColor.RED + "권한이 없습니다.");
            return true;
        }

        if (args.length == 0) {
            help(sender, label);
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "reload" -> {
                plugin.reloadConfig();
                plugin.cropService().reloadSettings();
                sender.sendMessage(ChatColor.GREEN + "농사 설정을 리로드했습니다.");
                return true;
            }
            case "settime" -> {
                if (args.length < 3) {
                    sender.sendMessage(ChatColor.YELLOW + "/" + label + " settime <CROP> <seconds>");
                    return true;
                }
                String crop = args[1].toUpperCase(Locale.ROOT);
                long sec;
                try { sec = Long.parseLong(args[2]); }
                catch (NumberFormatException ex) {
                    sender.sendMessage(ChatColor.RED + "초(second)는 숫자로 입력하세요.");
                    return true;
                }
                plugin.cropService().setGrowSeconds(crop, sec);
                sender.sendMessage(ChatColor.AQUA + crop + ChatColor.GRAY + " 성장시간 → " + ChatColor.WHITE + sec + "초");
                return true;
            }
            case "give" -> {
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.YELLOW + "/" + label + " give <MID|HIGH|TOP> [player]");
                    return true;
                }
                String tier = args[1].toUpperCase(Locale.ROOT);
                Player target;
                if (args.length >= 3) {
                    target = Bukkit.getPlayerExact(args[2]);
                    if (target == null) {
                        sender.sendMessage(ChatColor.RED + "대상 플레이어를 찾을 수 없습니다.");
                        return true;
                    }
                } else if (sender instanceof Player p) {
                    target = p;
                } else {
                    sender.sendMessage(ChatColor.RED + "콘솔에서는 대상 플레이어를 지정하세요.");
                    return true;
                }
                target.getInventory().addItem(ItemUtil.makeFertilizerItem(plugin, tier));
                sender.sendMessage(ChatColor.GREEN + "영양제 지급: " + tier + " → " + target.getName());
                return true;
            }
            case "givecrop" -> {
                if (args.length < 3) {
                    sender.sendMessage(ChatColor.YELLOW + "/" + label + " givecrop <CROP_ID> <S|A|B|C|D> [player]");
                    return true;
                }
                String cropId = args[1].toUpperCase(Locale.ROOT);

                Grade grade;
                try { grade = Grade.valueOf(args[2].toUpperCase(Locale.ROOT)); }
                catch (IllegalArgumentException ex) {
                    sender.sendMessage(ChatColor.RED + "등급은 S, A, B, C, D 중 하나여야 합니다.");
                    return true;
                }

                Player target;
                if (args.length >= 4) {
                    target = Bukkit.getPlayerExact(args[3]);
                    if (target == null) {
                        sender.sendMessage(ChatColor.RED + "대상 플레이어를 찾을 수 없습니다.");
                        return true;
                    }
                } else if (sender instanceof Player p) {
                    target = p;
                } else {
                    sender.sendMessage(ChatColor.RED + "콘솔에서는 대상 플레이어를 지정하세요.");
                    return true;
                }

                // 영양제 영향 없이 등급만 적용된 작물 드랍 아이템 생성
                ItemStack drop = ItemUtil.makeGradeDrop(plugin, cropId, grade, FertilizerTier.NONE, target);
                target.getInventory().addItem(drop);

                sender.sendMessage(ChatColor.GREEN + "등급 작물 지급: "
                    + cropId + " " + grade.name() + " → " + target.getName());
                return true;
            }
            default -> {
                help(sender, label);
                return true;
            }
        }
    }

    private void help(CommandSender sender, String label) {
        sender.sendMessage(ChatColor.GOLD + "=== Farm Admin ===");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " reload" + ChatColor.GRAY + " - 설정 리로드");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " settime <CROP> <seconds>" + ChatColor.GRAY + " - 성장시간 설정");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " give <MID|HIGH|TOP> [player]" + ChatColor.GRAY + " - 영양제 지급");
    }

    @Override
    public @Nullable List<String> onTabComplete(@Nonnull CommandSender sender,
                                                @Nonnull Command command,
                                                @Nonnull String alias,
                                                @Nonnull String[] args) {
        if (!sender.hasPermission("farm.admin")) return List.of();

        if (args.length == 1) {
            return prefix(List.of("reload","settime","give","givecrop"), args[0]); // ← givecrop 추가
        }
        if (args.length == 2) {
            if ("settime".equalsIgnoreCase(args[0])) {
                var cs = plugin.getConfig().getConfigurationSection("crops");
                if (cs != null) return prefix(new ArrayList<>(cs.getKeys(false)), args[1]);
            } else if ("give".equalsIgnoreCase(args[0])) {
                return prefix(List.of("MID","HIGH","TOP"), args[1]);
            } else if ("givecrop".equalsIgnoreCase(args[0])) {
                var cs = plugin.getConfig().getConfigurationSection("crops");
                if (cs != null) return prefix(new ArrayList<>(cs.getKeys(false)), args[1]); // CROP_ID 제안
            }
        }
        if (args.length == 3) {
            if ("givecrop".equalsIgnoreCase(args[0])) {
                return prefix(List.of("S","A","B","C","D"), args[2]); // 등급 제안
            }
        }
        if (args.length == 4 && "givecrop".equalsIgnoreCase(args[0])) {
            return null; // 플레이어 이름 자동 탭
        }
        if (args.length == 3 && "give".equalsIgnoreCase(args[0])) {
            return null; // 플레이어 자동탭
        }
        return List.of();
    }

    private List<String> prefix(List<String> base, String token) {
        String t = token.toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (String s : base) if (s.toLowerCase(Locale.ROOT).startsWith(t)) out.add(s);
        return out;
    }
}