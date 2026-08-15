package com.lbdevz.lbluckyblock;

import com.lbdevz.lbluckyblock.commands.LuckyBlockCommand;
import com.lbdevz.lbluckyblock.commands.LuckyBlockTabCompleter;
import com.lbdevz.lbluckyblock.listeners.LuckyBlockListener;
import com.lbdevz.lbluckyblock.managers.LuckyBlockManager;
import org.bukkit.plugin.java.JavaPlugin;

public class LBLuckyBlock extends JavaPlugin {

    private LuckyBlockManager luckyBlockManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.luckyBlockManager = new LuckyBlockManager(this);

        LuckyBlockCommand cmd = new LuckyBlockCommand(this);
        LuckyBlockTabCompleter tab = new LuckyBlockTabCompleter(this);

        if (getCommand("lbluckyblock") != null) {
            getCommand("lbluckyblock").setExecutor(cmd);
            getCommand("lbluckyblock").setTabCompleter(tab);
        }

        getServer().getPluginManager().registerEvents(new LuckyBlockListener(this), this);

        luckyBlockManager.loadSavedBlocks();

        getLogger().info("LBLuckyBlock aktif edildi!");
    }

    @Override
    public void onDisable() {
        if (luckyBlockManager != null) {
            luckyBlockManager.removeAllHolograms();
        }
        getLogger().info("LBLuckyBlock devre disi birakildi.");
    }

    public LuckyBlockManager getLuckyBlockManager() {
        return luckyBlockManager;
    }
}
