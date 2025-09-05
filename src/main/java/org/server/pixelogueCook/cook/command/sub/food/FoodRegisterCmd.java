package org.server.pixelogueCook.cook.command.sub.food;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.server.pixelogueCook.cook.command.AbstractSubCommand;
import org.server.pixelogueCook.cook.service.CookService;

public class FoodRegisterCmd extends AbstractSubCommand {
    private final CookService cook;
    public FoodRegisterCmd(CookService cook){ this.cook=cook; }

    @Override public String name(){ return "등록"; }
    @Override public java.util.List<String> aliases(){ return java.util.List.of("register"); }
    @Override public String permission(){ return "cook.admin"; }

    @Override
    public boolean execute(CommandSender s, String label, String[] args) {
        if (!(s instanceof Player p)) { s.sendMessage("플레이어만 사용 가능합니다."); return true; }
        if (args.length < 2) { s.sendMessage("§e/"+label+" 등록 <레시피ID> <요리시간초>"); return true; }
        String id = args[0];
        long sec;
        try { sec = Long.parseLong(args[1]); } catch (Exception e){ s.sendMessage("시간은 숫자(초)"); return true; }

        String err = cook.registerFromHand(p, id, sec);
        s.sendMessage(err==null ? "등록 완료: "+id+" ("+sec+"초)" : "§c"+err);
        return true;
    }
}