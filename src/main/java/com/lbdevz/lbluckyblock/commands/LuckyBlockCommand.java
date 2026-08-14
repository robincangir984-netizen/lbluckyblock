package com.lbdevz.lbluckyblock.commands;

import com.lbdevz.lbluckyblock.LBLuckyBlock;
import com.lbdevz.lbluckyblock.models.LuckyBlockModel;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class LuckyBlockCommand implements CommandExecutor {

    private final LBLuckyBlock plugin;

    public LuckyBlockCommand(LBLuckyBlock plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String prefix = plugin.getConfig().getString("messages.prefix", "");

        if (!sender.hasPermission("lbluckyblock.admin")) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + plugin.getConfig().getString("messages.no-permission")));
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            plugin.reloadConfig();
            plugin.getLuckyBlockManager().loadBloklar();
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + plugin.getConfig().getString("messages.reload")));
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage("Bu komut sadece oyunda kullanilabilir.");
            return true;
        }

        if (args.length >= 2 && args[0].equalsIgnoreCase("setloc")) {
            String id = args[1];
            Block targetBlock = player.getTargetBlockExact(5);

            if (targetBlock == null || targetBlock.getType().isAir()) {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + plugin.getConfig().getString("messages.not-a-block")));
                return true;
            }

            LuckyBlockModel model = plugin.getLuckyBlockManager().getModel(id);
            if (model == null) {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + plugin.getConfig().getString("messages.block-not-found")));
                return true;
            }

            String path = "bloklar." + id + ".location.";
            plugin.getConfig().set(path + "world", targetBlock.getWorld().getName());
            plugin.getConfig().set(path + "x", targetBlock.getX());
            plugin.getConfig().set(path + "y", targetBlock.getY());
            plugin.getConfig().set(path + "z", targetBlock.getZ());
            plugin.saveConfig();

            plugin.getLuckyBlockManager().loadBloklar();

            String msg = plugin.getConfig().getString("messages.loc-set", "").replace("%id%", id);
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + msg));
            return true;
        }

        player.sendMessage(ChatColor.YELLOW + "Kullanim: /lbluckyblock <setloc|reload> [id]");
        return true;
    }
}
