package com.lbdevz.lbluckyblock.listeners;

import com.lbdevz.lbluckyblock.LBLuckyBlock;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class LuckyBlockListener implements Listener {

    private final LBLuckyBlock plugin;

    public LuckyBlockListener(LBLuckyBlock plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() == Action.LEFT_CLICK_BLOCK && event.getClickedBlock() != null) {
            if (plugin.getLuckyBlockManager().isLuckyBlock(event.getClickedBlock().getLocation())) {
                event.setCancelled(true);
                plugin.getLuckyBlockManager().processHit(event.getPlayer(), event.getClickedBlock().getLocation());
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (plugin.getLuckyBlockManager().isLuckyBlock(event.getBlock().getLocation())) {
            event.setCancelled(true);
        }
    }
}
