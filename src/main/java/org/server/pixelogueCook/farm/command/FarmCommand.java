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
                    sender.sendMessage(ChatColor.YELLOW + "/" + label + " givecrop <CROP_ID> <S|A|B|C|D> [amount] [player]");
                    return true;
                }
                String cropId = args[1].toUpperCase(Locale.ROOT);

                Grade grade;
                try { grade = Grade.valueOf(args[2].toUpperCase(Locale.ROOT)); }
                catch (IllegalArgumentException ex) {
                    sender.sendMessage(ChatColor.RED + "등급은 S, A, B, C, D 중 하나여야 합니다.");
                    return true;
                }

                // amount(optional) 파싱
                int amt = 1;
                int idx = 3;
                if (args.length >= 4) {
                    try {
                        amt = Math.max(1, Integer.parseInt(args[3]));
                        idx = 4; // 다음 인자는 player
                    } catch (NumberFormatException ignore) {
                        // 숫자가 아니면 amount 생략, args[3]은 player로 간주
                    }
                }

                Player target;
                if (args.length >= idx + 1) {
                    target = Bukkit.getPlayerExact(args[idx]);
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

                // 등급 적용 드랍 아이템 생성 (이름/등급 PDC 포함)
                ItemStack drop = ItemUtil.makeGradeDrop(plugin, cropId, grade, FertilizerTier.NONE, target);
                drop.setAmount(amt);
                target.getInventory().addItem(drop);

                sender.sendMessage(ChatColor.GREEN + "등급 작물 지급: "
                        + cropId + " " + grade.name() + " x" + amt + " → " + target.getName());
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
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " givecrop <CROP_ID> <S|A|B|C|D> [amount] [player]"
                + ChatColor.GRAY + " - 등급 작물 지급"); // ★ 추가/업데이트
    }

    @Override
    public @Nullable List<String> onTabComplete(@Nonnull CommandSender sender,
                                                @Nonnull Command command,
                                                @Nonnull String alias,
                                                @Nonnull String[] args) {
        if (!sender.hasPermission("farm.admin")) return List.of();

        if (args.length == 1) {
            return prefix(List.of("reload","settime","give","givecrop"), args[0]);
        }

        if (args.length == 2) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            if ("settime".equals(sub)) {
                var cs = plugin.getConfig().getConfigurationSection("crops");
                return cs != null ? prefix(new ArrayList<>(cs.getKeys(false)), args[1]) : List.of();
            } else if ("give".equals(sub)) {
                return prefix(List.of("MID","HIGH","TOP"), args[1]);
            } else if ("givecrop".equals(sub)) {
                var cs = plugin.getConfig().getConfigurationSection("crops");
                List<String> base = (cs != null) ? new ArrayList<>(cs.getKeys(false)) : new ArrayList<>();
                // 블록/아이템 키 혼용 입력을 고려해 기본 키도 추가
                base.addAll(List.of("WHEAT","CARROTS","POTATOES","BEETROOTS","NETHER_WART","COCOA",
                        "CARROT","POTATO","BEETROOT","COCOA_BEANS"));
                // 중복 제거
                base = new ArrayList<>(new java.util.LinkedHashSet<>(base));
                return prefix(base, args[1]);
            }
        }

        if (args.length == 3) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            if ("settime".equals(sub)) {
                return prefix(List.of("30","60","120","300","600"), args[2]);
            } else if ("give".equals(sub)) {
                return null; // 플레이어 자동 탭
            } else if ("givecrop".equals(sub)) {
                return prefix(List.of("S","A","B","C","D"), args[2]);
            }
        }

        if (args.length == 4) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            if ("give".equals(sub)) return null; // 플레이어 자동 탭

            if ("givecrop".equals(sub)) {
                // 4번째 인수: 수량 또는 플레이어
                String tok = args[3];
                boolean looksNumber = !tok.isEmpty() && Character.isDigit(tok.charAt(0));
                if (looksNumber) {
                    return prefix(List.of("1","8","16","32","64"), tok);
                } else {
                    return null; // 플레이어 자동 탭
                }
            }
        }

        if (args.length == 5 && "givecrop".equalsIgnoreCase(args[0])) {
            return null; // amount를 썼다면 5번째는 플레이어 자동 탭
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