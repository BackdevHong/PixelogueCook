package org.server.pixelogueCook.cook.command;

import org.server.pixelogueCook.cook.command.sub.cook.CookOpenCmd;

public class CookRootCommand extends BaseCommand {
    public CookRootCommand(String label) {
        super(label);
        var open = new CookOpenCmd();
        register(open);
        setDefault(open); // ← "/요리"만 쳐도 바로 열기
    }
}