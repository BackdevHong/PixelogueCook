package org.server.pixelogueCook.fishing.command;


import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.server.pixelogueCook.fishing.model.FishRank;
import org.server.pixelogueCook.fishing.model.RodTier;
import org.server.pixelogueCook.fishing.util.FishItemUtil;
import org.server.pixelogueCook.fishing.util.RodUtil;

import java.util.*;
import java.util.stream.Collectors;

public class FishingCommand implements CommandExecutor, TabCompleter {

    // 지급 가능한 생물고기 4종(익힌/양동이 제외)
    private static final Material[] RAW_FISH = new Material[] {
        Material.COD, Material.SALMON, Material.PUFFERFISH, Material.TROPICAL_FISH
    };

    private static final List<String> SUBS = List.of("rod", "give");
    private static final List<String> ROD_TIERS = List.of("basic","intermediate","advanced","master");
    private static final List<String> RANKS = List.of("S","A","B","C","D");
    private static final List<String> FISH_TYPES = List.of(
        "cod","salmon","pufferfish","tropical_fish",
        "대구","연어","복어","열대어"
    );
    private static final List<String> AMOUNTS = List.of("1","2","4","8","16","32","64");

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (args.length == 0) {
            help(sender, label);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);

        // /fishing rod <tier>
        if (sub.equals("rod")) {
            if (!checkAdmin(sender)) return true;

            if (!(sender instanceof Player p)) {
                sender.sendMessage("§c플레이어만 사용 가능합니다.");
                return true;
            }
            if (args.length < 2) {
                sender.sendMessage("§e사용법: §f/" + label + " rod <basic|intermediate|advanced|master>");
                return true;
            }
            RodTier t = RodTier.of(args[1]);
            if (t == null) {
                sender.sendMessage("§c잘못된 등급입니다. " + ROD_TIERS);
                return true;
            }
            p.getInventory().addItem(RodUtil.makeRod(t));
            p.sendMessage("§a지급: §f" + RodUtil.pretty(t) + " §7낚시대");
            return true;
        }

        // /fishing give <player> <rank> [type] [amount]
        if (sub.equals("give")) {
            if (!checkAdmin(sender)) return true;

            if (args.length < 3) {
                sender.sendMessage("§e사용법: §f/" + label + " give <플레이어> <등급:S|A|B|C|D> [종류] [개수]");
                sender.sendMessage("§7종류: cod, salmon, pufferfish, tropical_fish / 한국어: 대구, 연어, 복어, 열대어");
                return true;
            }

            Player target = findOnline(args[1]);
            if (target == null) {
                sender.sendMessage("§c플레이어를 찾을 수 없습니다: §f" + args[1]);
                return true;
            }

            FishRank rank = parseRank(args[2]);
            if (rank == null) {
                sender.sendMessage("§c등급은 S/A/B/C/D 중 하나여야 합니다.");
                return true;
            }

            Material type = null;
            if (args.length >= 4) {
                type = parseFishType(args[3]);
                if (type == null) {
                    sender.sendMessage("§c알 수 없는 물고기 종류입니다. §7" + FISH_TYPES);
                    return true;
                }
            } else {
                type = RAW_FISH[new Random().nextInt(RAW_FISH.length)];
            }

            int amount = 1;
            if (args.length >= 5) {
                try {
                    amount = Math.max(1, Math.min(64, Integer.parseInt(args[4])));
                } catch (NumberFormatException nfe) {
                    sender.sendMessage("§c개수는 1~64 사이의 숫자여야 합니다.");
                    return true;
                }
            }

            // 아이템 생성 + 랭크 부여
            ItemStack it = new ItemStack(type, amount);
            FishItemUtil.setRank(it, rank);

            // 지급(인벤 가득이면 떨어뜨리기)
            Map<Integer, ItemStack> remain = target.getInventory().addItem(it);
            if (!remain.isEmpty()) {
                for (ItemStack r : remain.values()) {
                    target.getWorld().dropItemNaturally(target.getLocation(), r);
                }
            }

            target.sendMessage("§a지급: §f등급 " + rank.name() + " §7물고기 ×" + amount);
            if (!sender.equals(target)) {
                sender.sendMessage("§a완료: §f" + target.getName() + " §7에게 등급 " + rank.name() + " 물고기 ×" + amount + " 지급");
            }
            return true;
        }

        help(sender, label);
        return true;
    }

    private void help(CommandSender sender, String label) {
        String tiers = String.join("|", ROD_TIERS); // basic|intermediate|advanced|master
        String ranks = String.join("|", RANKS);     // S|A|B|C|D
        String types = String.join("|", FISH_TYPES);

        sender.sendMessage("§6==== /" + label + " 도움말 ====");
        sender.sendMessage("§e/" + label + " rod <" + tiers + ">");
        sender.sendMessage("§7- 등급 낚시대 지급");

        sender.sendMessage("§e/" + label + " give <플레이어> <" + ranks + "> [종류] [개수]");
        sender.sendMessage("§7- 등급이 붙은 물고기 직접 지급");
        sender.sendMessage("§7종류: " + types);
        sender.sendMessage("§7예: /" + label + " give Steve S 연어 3");
    }

    // ---------------- Tab Complete ----------------
    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (args.length == 1)
            return filterPrefix(SUBS, args[0]);

        // /fishing rod <tier>
        if (args[0].equalsIgnoreCase("rod")) {
            if (args.length == 2) return filterPrefix(ROD_TIERS, args[1]);
            return Collections.emptyList();
        }

        // /fishing give <player> <rank> [type] [amount]
        if (args[0].equalsIgnoreCase("give")) {
            if (args.length == 2) return filterPrefix(onlinePlayerNames(), args[1]);
            if (args.length == 3) return filterPrefix(RANKS, args[2].toUpperCase(Locale.ROOT));
            if (args.length == 4) return filterPrefix(FISH_TYPES, args[3].toLowerCase(Locale.ROOT));
            if (args.length == 5) return filterPrefix(AMOUNTS, args[4]);
        }
        return Collections.emptyList();
    }

    // ---------------- Helpers ----------------
    private static boolean checkAdmin(CommandSender sender) {
        if (sender.hasPermission("fishing.admin")) return true;
        sender.sendMessage(ChatColor.RED + "권한이 없습니다: fishing.admin");
        return false;
    }

    private static Player findOnline(String name) {
        Player p = Bukkit.getPlayerExact(name);
        if (p != null) return p;
        // 부분 일치 보조
        String lower = name.toLowerCase(Locale.ROOT);
        for (Player pl : Bukkit.getOnlinePlayers()) {
            if (pl.getName().toLowerCase(Locale.ROOT).startsWith(lower)) return pl;
        }
        return null;
    }

    private static FishRank parseRank(String s) {
        if (s == null) return null;
        s = s.trim().toUpperCase(Locale.ROOT);
        try { return FishRank.valueOf(s); } catch (Exception ignore) { return null; }
    }

    private static Material parseFishType(String s) {
        String k = s.trim().toLowerCase(Locale.ROOT);
        return switch (k) {
            case "cod", "대구" -> Material.COD;
            case "salmon", "연어" -> Material.SALMON;
            case "pufferfish", "복어" -> Material.PUFFERFISH;
            case "tropical_fish", "tropicalfish", "열대어" -> Material.TROPICAL_FISH;
            default -> null;
        };
    }

    private static List<String> onlinePlayerNames() {
        return Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
    }

    private static List<String> filterPrefix(Collection<String> src, String prefix) {
        String p = prefix == null ? "" : prefix.toLowerCase(Locale.ROOT);
        return src.stream().filter(s -> s.toLowerCase(Locale.ROOT).startsWith(p)).collect(Collectors.toList());
    }

    private static List<String> filterPrefix(List<String> src, String prefix) {
        return filterPrefix((Collection<String>) src, prefix);
    }
}