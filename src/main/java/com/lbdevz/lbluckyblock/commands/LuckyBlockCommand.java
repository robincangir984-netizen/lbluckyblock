package com.lbdevz.lbluckyblock.commands;

import com.lbdevz.lbluckyblock.LBLuckyBlock;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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
            plugin.getLuckyBlockManager().loadSavedBlocks();
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + plugin.getConfig().getString("messages.reload")));
            return true;
        }

        if (args.length >= 2 && args[0].equalsIgnoreCase("give")) {
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(ChatColor.RED + "Oyuncu bulunamadi!");
                return true;
            }

            target.getInventory().addItem(plugin.getLuckyBlockManager().getLuckyBlockItem());
            String msg = plugin.getConfig().getString("messages.given-item", "").replace("%player%", target.getName());
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + msg));
            return true;
        }

        sender.sendMessage(ChatColor.YELLOW + "Kullanim: /lbluckyblock <give|reload> [oyuncu]");
        return true;
    }
}
