package com.lbdevz.lbluckyblock.listeners;

import com.lbdevz.lbluckyblock.LBLuckyBlock;
import org.bukkit.GameMode;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class LuckyBlockListener implements Listener {

    private final LBLuckyBlock plugin;

    public LuckyBlockListener(LBLuckyBlock plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        ItemStack customItem = plugin.getLuckyBlockManager().getLuckyBlockItem();

        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            if (item.getItemMeta().getDisplayName().equals(customItem.getItemMeta().getDisplayName())) {
                plugin.getLuckyBlockManager().registerNewBlock(event.getBlock().getLocation());
                String prefix = plugin.getConfig().getString("messages.prefix", "");
                String msg = plugin.getConfig().getString("messages.block-placed", "");
                event.getPlayer().sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + msg));
            }
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() == Action.LEFT_CLICK_BLOCK && event.getClickedBlock() != null) {
            if (plugin.getLuckyBlockManager().isLuckyBlock(event.getClickedBlock().getLocation())) {
                
                if (event.getPlayer().getGameMode() == GameMode.CREATIVE) {
                    plugin.getLuckyBlockManager().removeBlock(event.getClickedBlock().getLocation());
                    event.getClickedBlock().setType(org.bukkit.Material.AIR);
                    String prefix = plugin.getConfig().getString("messages.prefix", "");
                    String msg = plugin.getConfig().getString("messages.block-removed", "");
                    event.getPlayer().sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + msg));
                    event.setCancelled(true);
                    return;
                }

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
