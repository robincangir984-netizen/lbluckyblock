package com.lbdevz.lbluckyblock.managers;

import com.lbdevz.lbluckyblock.LBLuckyBlock;
import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class LuckyBlockManager {

    private final LBLuckyBlock plugin;
    private final Set<Location> activeLocations = new HashSet<>();
    private final Set<Location> cooldownLocations = new HashSet<>();
    private final Map<Location, Integer> currentHits = new HashMap<>();
    private final Map<Location, ArmorStand> holograms = new HashMap<>();
    private final Map<UUID, List<Long>> playerClicks = new HashMap<>();

    private File dataFile;
    private FileConfiguration dataConfig;

    public LuckyBlockManager(LBLuckyBlock plugin) {
        this.plugin = plugin;
        createDataConfig();
    }

    private void createDataConfig() {
        dataFile = new File(plugin.getDataFolder(), "blocks.yml");
        if (!dataFile.exists()) {
            try {
                dataFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
    }

    public void saveBlocks() {
        List<String> locs = new ArrayList<>();
        for (Location loc : activeLocations) {
            locs.add(loc.getWorld().getName() + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ());
        }
        dataConfig.set("placed-blocks", locs);
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void loadSavedBlocks() {
        removeAllHolograms();
        activeLocations.clear();
        cooldownLocations.clear();
        currentHits.clear();

        List<String> locs = dataConfig.getStringList("placed-blocks");
        int maxHits = plugin.getConfig().getInt("blok-ayarlari.max-hits", 20);

        for (String s : locs) {
            String[] parts = s.split(",");
            if (parts.length == 4) {
                World w = Bukkit.getWorld(parts[0]);
                if (w != null) {
                    Location loc = new Location(w, Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), Integer.parseInt(parts[3]));
                    activeLocations.add(loc);
                    currentHits.put(loc, maxHits);
                    loc.getBlock().setType(Material.BUBBLE_CORAL);
                    spawnHologram(loc, maxHits);
                }
            }
        }
    }

    public ItemStack getLuckyBlockItem() {
        Material mat = Material.matchMaterial(plugin.getConfig().getString("lucky-block-item.material", "BUBBLE_CORAL"));
        if (mat == null) mat = Material.BUBBLE_CORAL;

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("lucky-block-item.name", "&e&lŞANS BLOĞU")));
            List<String> lore = new ArrayList<>();
            for (String l : plugin.getConfig().getStringList("lucky-block-item.lore")) {
                lore.add(ChatColor.translateAlternateColorCodes('&', l));
            }
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    public void registerNewBlock(Location loc) {
        Location blockLoc = loc.getBlock().getLocation();
        activeLocations.add(blockLoc);
        int maxHits = plugin.getConfig().getInt("blok-ayarlari.max-hits", 20);
        currentHits.put(blockLoc, maxHits);
        spawnHologram(blockLoc, maxHits);
        saveBlocks();
    }

    public void removeBlock(Location loc) {
        Location blockLoc = loc.getBlock().getLocation();
        activeLocations.remove(blockLoc);
        cooldownLocations.remove(blockLoc);
        currentHits.remove(blockLoc);
        removeHologram(blockLoc);
        saveBlocks();
    }

    public boolean isMacro(Player player) {
        int maxCps = plugin.getConfig().getInt("macro-protection.max-cps", 5);
        long now = System.currentTimeMillis();
        UUID uuid = player.getUniqueId();

        playerClicks.putIfAbsent(uuid, new ArrayList<>());
        List<Long> times = playerClicks.get(uuid);
        times.add(now);
        times.removeIf(time -> now - time > 1000);

        return times.size() > maxCps;
    }

    public void processHit(Player player, Location loc) {
        Location blockLoc = loc.getBlock().getLocation();

        if (cooldownLocations.contains(blockLoc)) return;

        if (isMacro(player)) {
            String prefix = plugin.getConfig().getString("messages.prefix", "");
            String warn = plugin.getConfig().getString("messages.macro-warning", "");
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + warn));
            return;
        }

        int maxHits = plugin.getConfig().getInt("blok-ayarlari.max-hits", 20);
        int remaining = currentHits.getOrDefault(blockLoc, maxHits) - 1;
        currentHits.put(blockLoc, remaining);

        World world = blockLoc.getWorld();
        if (world != null) {
            String hitSound = plugin.getConfig().getString("blok-ayarlari.sounds.hit", "BLOCK_NOTE_BLOCK_PLING");
            try {
                world.playSound(blockLoc, Sound.valueOf(hitSound), 1.0f, 1.2f);
            } catch (Exception ignored) {}
            world.spawnParticle(Particle.CRIT, blockLoc.clone().add(0.5, 0.5, 0.5), 10);
        }

        dropCoralRewards(blockLoc);
        updateHologram(blockLoc, remaining);

        if (remaining <= 0) {
            handleBlockBreak(player, blockLoc);
        }
    }

    private void dropCoralRewards(Location loc) {
        World world = loc.getWorld();
        if (world == null) return;

        Random rand = new Random();
        int roll = rand.nextInt(100);
        int amount = 1;

        if (roll < 70) {
            amount = 1;
        } else if (roll < 90) {
            amount = 2;
        } else {
            amount = 3;
        }

        world.dropItemNaturally(loc.clone().add(0.5, 1.0, 0.5), new ItemStack(Material.BUBBLE_CORAL, amount));
    }

    private void handleBlockBreak(Player player, Location blockLoc) {
        World world = blockLoc.getWorld();
        if (world != null) {
            String breakSound = plugin.getConfig().getString("blok-ayarlari.sounds.break", "ENTITY_GENERIC_EXPLODE");
            try {
                world.playSound(blockLoc, Sound.valueOf(breakSound), 1.0f, 1.0f);
            } catch (Exception ignored) {}
            world.spawnParticle(Particle.EXPLOSION_LARGE, blockLoc.clone().add(0.5, 0.5, 0.5), 1);
        }

        String prefix = plugin.getConfig().getString("messages.prefix", "");
        String destroyMsg = plugin.getConfig().getString("messages.block-destroyed", "").replace("%player%", player.getName());
        String respawnMsg = plugin.getConfig().getString("messages.block-respawning", "");

        Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', prefix + destroyMsg));
        Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', prefix + respawnMsg));

        blockLoc.getBlock().setType(Material.BEDROCK);
        cooldownLocations.add(blockLoc);
        removeHologram(blockLoc);

        int delay = plugin.getConfig().getInt("blok-ayarlari.respawn-delay-seconds", 15);

        new BukkitRunnable() {
            @Override
            public void run() {
                int maxHits = plugin.getConfig().getInt("blok-ayarlari.max-hits", 20);
                blockLoc.getBlock().setType(Material.BUBBLE_CORAL);
                cooldownLocations.remove(blockLoc);
                currentHits.put(blockLoc, maxHits);
                spawnHologram(blockLoc, maxHits);

                String respawnedMsg = plugin.getConfig().getString("messages.block-respawned", "");
                Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', prefix + respawnedMsg));
            }
        }.runTaskLater(plugin, delay * 20L);
    }

    private void spawnHologram(Location loc, int hits) {
        Location hologramLoc = loc.clone().add(0.5, 1.2, 0.5);
        World world = hologramLoc.getWorld();
        if (world == null) return;

        removeHologram(loc);

        ArmorStand stand = (ArmorStand) world.spawnEntity(hologramLoc, EntityType.ARMOR_STAND);
        stand.setVisible(false);
        stand.setGravity(false);
        stand.setCustomNameVisible(true);
        stand.setMarker(true);

        holograms.put(loc, stand);
        updateHologram(loc, hits);
    }

    public void updateHologram(Location loc, int currentHits) {
        ArmorStand stand = holograms.get(loc);
        if (stand == null || !stand.isValid()) return;

        int maxHits = plugin.getConfig().getInt("blok-ayarlari.max-hits", 20);
        String displayName = plugin.getConfig().getString("blok-ayarlari.display-name", "&e&lŞANS BLOĞU");
        String healthBar = getHealthBar(currentHits, maxHits);
        
        String name = ChatColor.translateAlternateColorCodes('&', displayName + "\n&7[" + healthBar + "&7]");
        stand.setCustomName(name);
    }

    private String getHealthBar(int current, int max) {
        int totalBlocks = 10;
        int greenBlocks = (int) Math.ceil(((double) current / max) * totalBlocks);
        if (greenBlocks < 0) greenBlocks = 0;
        if (greenBlocks > totalBlocks) greenBlocks = totalBlocks;

        int redBlocks = totalBlocks - greenBlocks;

        StringBuilder bar = new StringBuilder("&a");
        for (int i = 0; i < greenBlocks; i++) bar.append("█");
        bar.append("&c");
        for (int i = 0; i < redBlocks; i++) bar.append("█");

        return bar.toString();
    }

    public void removeHologram(Location loc) {
        ArmorStand stand = holograms.remove(loc);
        if (stand != null && stand.isValid()) {
            stand.remove();
        }
    }

    public void removeAllHolograms() {
        for (ArmorStand stand : holograms.values()) {
            if (stand != null && stand.isValid()) {
                stand.remove();
            }
        }
        holograms.clear();
    }

    public boolean isLuckyBlock(Location loc) {
        return activeLocations.contains(loc.getBlock().getLocation());
    }
}
