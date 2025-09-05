package org.server.pixelogueCook.cook.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

public class BaseCommand implements CommandExecutor, TabCompleter {
    private final String label;
    private final Map<String, AbstractSubCommand> subs = new LinkedHashMap<>();
    private AbstractSubCommand defaultSub = null;

    public BaseCommand(String label) { this.label = label; }

    public BaseCommand register(AbstractSubCommand sub) {
        subs.put(sub.name().toLowerCase(Locale.ROOT), sub);
        for (String a : sub.aliases()) subs.put(a.toLowerCase(Locale.ROOT), sub);
        return this;
    }

    public BaseCommand setDefault(AbstractSubCommand sub) {
        this.defaultSub = sub; return this;
    }

    @Override
    public boolean onCommand(@Nonnull CommandSender s, @Nonnull Command c, @Nonnull String l, @Nonnull String[] a) {
        if (a.length == 0) {
            if (defaultSub != null) return defaultSub.execute(s, label, new String[0]);
            s.sendMessage("§6/"+label+" help §7- 도움말");
            return true;
        }
        AbstractSubCommand sub = subs.get(a[0].toLowerCase(Locale.ROOT));
        if (sub == null) {
            s.sendMessage("§c알 수 없는 서브명령입니다. §7/"+label+" help");
            return true;
        }
        if (!sub.hasPerm(s)) { s.sendMessage("§c권한이 없습니다."); return true; }
        return sub.execute(s, label, Arrays.copyOfRange(a,1,a.length));
    }

    @Override
    public @Nullable List<String> onTabComplete(@Nonnull CommandSender s, @Nonnull Command c, @Nonnull String l, @Nonnull String[] a) {
        if (a.length == 1) {
            String tok = a[0].toLowerCase(Locale.ROOT);
            Set<String> names = new LinkedHashSet<>();
            for (var e : subs.entrySet()) {
                if (!e.getValue().hasPerm(s)) continue;
                if (e.getKey().startsWith(tok)) names.add(e.getKey());
            }
            return new ArrayList<>(names);
        }
        AbstractSubCommand sub = subs.get(a[0].toLowerCase(Locale.ROOT));
        if (sub == null || !sub.hasPerm(s)) return List.of();
        return sub.tab(s, Arrays.copyOfRange(a,1,a.length));
    }
}