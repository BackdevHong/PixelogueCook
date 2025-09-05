package org.server.pixelogueCook.cook.command;


import org.server.pixelogueCook.cook.command.sub.food.*;
import org.server.pixelogueCook.cook.service.CookService;

public class FoodRootCommand extends BaseCommand {
    public FoodRootCommand(String label, CookService cook) {
        super(label);
        register(new FoodRegisterCmd(cook));
        register(new FoodAddIngHandCmd(cook));
        register(new FoodGiveBookCmd(cook));
        register(new FoodListCmd(cook));
        register(new FoodRemoveCmd(cook));
        register(new FoodGiveCmd(cook));
    }
}