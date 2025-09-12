package org.server.pixelogueCook.fishing.listener;

import org.bukkit.entity.FishHook;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.server.pixelogueCook.fishing.FishingGameManager;

public class FishingListener implements Listener {
    private final FishingGameManager mgr;
    public FishingListener(FishingGameManager mgr) { this.mgr = mgr; }

    @EventHandler
    public void onFish(PlayerFishEvent e){
        if (e.getState() == PlayerFishEvent.State.CAUGHT_FISH){
            if (mgr.isBusy(e.getPlayer())) return;

            // ▶ 훅(찌) 저장
            FishHook hook = e.getHook();
            mgr.bindHook(e.getPlayer(), hook);

            // 기본 드랍 제거 & 이벤트 취소
            if (e.getCaught() instanceof Item item) item.remove();
            e.setExpToDrop(0);
            e.setCancelled(true);

            // 미니게임 시작
            mgr.startRandom(e.getPlayer());
        }
    }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInteract(PlayerInteractEvent e){
        if (mgr.isBusy(e.getPlayer())){
            mgr.onClick(e);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e){
        mgr.abort(e.getPlayer());
    }
}