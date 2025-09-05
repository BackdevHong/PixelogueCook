package org.server.pixelogueCook.cook.command;

import org.bukkit.command.CommandSender;

import javax.annotation.Nonnull;
import java.util.List;

public abstract class AbstractSubCommand {
    public abstract @Nonnull String name();                // 서브커맨드 이름 (예: "등록")
    public List<String> aliases() { return List.of(); }    // 별칭들
    public String permission() { return ""; }              // 필요 권한
    public abstract boolean execute(CommandSender sender, String label, String[] args);
    public List<String> tab(CommandSender sender, String[] args) { return List.of(); }

    protected boolean hasPerm(CommandSender s){
        var perm = permission();
        return perm == null || perm.isBlank() || s.hasPermission(perm);
    }
}