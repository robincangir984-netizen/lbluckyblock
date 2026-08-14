package com.lbdevz.lbluckyblock.managers;

import com.lbdevz.lbluckyblock.LBLuckyBlock;
import com.lbdevz.lbluckyblock.models.LuckyBlockModel;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class LuckyBlockManager {

    private final LBLuckyBlock plugin;
    private final Map<String, LuckyBlockModel> blokModels = new HashMap<>();
    private final Map<Location, String> activeLocations = new HashMap<>();
    private final Map<String, Integer> currentHits = new HashMap<>();
    private final Map<String, ArmorStand> holograms = new HashMap<>();
    private final Map<UUID, List<Long>> playerClicks = new HashMap<>();

    public LuckyBlockManager(LBLuckyBlock plugin) {
        this.plugin = plugin;
    }

    public void loadBloklar() {
        removeAllHolograms();
        blokModels.clear();
        activeLocations.clear();
        currentHits.clear();

        ConfigurationSection section = plugin.getConfig().getConfigurationSection("bloklar");
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            String path = "bloklar." + key + ".";
            String displayName = plugin.getConfig().getString(path + "display-name", "&e&lSans Blogu");
            String matStr = plugin.getConfig().getString(path + "block-type", "SPONGE");
            Material mat = Material.matchMaterial(matStr);
            if (mat == null) mat = Material.SPONGE;

            int maxHits = plugin.getConfig().getInt(path + "max-hits", 20);
            int delay = plugin.getConfig().getInt(path + "respawn-delay-seconds", 15);
            String hitSound = plugin.getConfig().getString(path + "sounds.hit", "BLOCK_NOTE_BLOCK_PLING");
            String breakSound = plugin.getConfig().getString(path + "sounds.break", "ENTITY_GENERIC_EXPLODE");

            String worldName = plugin.getConfig().getString(path + "location.world", "world");
            World world = Bukkit.getWorld(worldName);
            double x = plugin.getConfig().getDouble(path + "location.x");
            double y = plugin.getConfig().getDouble(path + "location.y");
            double z = plugin.getConfig().getDouble(path + "location.z");

            Location loc = (world != null) ? new Location(world, x, y, z) : null;

            List<Map<String, Object>> hitRewards = (List<Map<String, Object>>) plugin.getConfig().getList(path + "hit-rewards");
            List<String> lastHitCmds = plugin.getConfig().getStringList(path + "last-hit-rewards.commands");
            List<Map<String, Object>> lastHitItems = (List<Map<String, Object>>) plugin.getConfig().getList(path + "last-hit-rewards.items");
            List<String> lastHitSounds = plugin.getConfig().getStringList(path + "last-hit-rewards.sounds");
            int lastHitExp = plugin.getConfig().getInt(path + "last-hit-rewards.experience", 0);

            LuckyBlockModel model = new LuckyBlockModel(key, displayName, mat, maxHits, delay, hitSound, breakSound, loc,
                    hitRewards != null ? hitRewards : new ArrayList<>(),
                    lastHitCmds, lastHitItems != null ? lastHitItems : new ArrayList<>(),
                    lastHitSounds, lastHitExp);

            blokModels.put(key, model);

            if (loc != null && world != null) {
                activeLocations.put(loc.getBlock().getLocation(), key);
                currentHits.put(key, maxHits);
                loc.getBlock().setType(mat);
                spawnHologram(model, maxHits);
            }
        }
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

    public void processHit(Player player, Location blockLoc) {
        String id = activeLocations.get(blockLoc);
        if (id == null) return;

        LuckyBlockModel model = blokModels.get(id);
        if (model == null) return;

        if (isMacro(player)) {
            String prefix = plugin.getConfig().getString("messages.prefix", "");
            String warn = plugin.getConfig().getString("messages.macro-warning", "&cMakro algilandi!");
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + warn));
            return;
        }

        int remaining = currentHits.getOrDefault(id, model.getMaxHits()) - 1;
        currentHits.put(id, remaining);

        World world = blockLoc.getWorld();
        if (world != null) {
            try {
                world.playSound(blockLoc, Sound.valueOf(model.getHitSound()), 1.0f, 1.2f);
            } catch (Exception ignored) {}
            world.spawnParticle(Particle.CRIT, blockLoc.clone().add(0.5, 0.5, 0.5), 10);
        }

        giveHitRewards(player, model, blockLoc);
        updateHologram(model, remaining);

        if (remaining <= 0) {
            handleBlockBreak(player, model, blockLoc);
        }
    }

    private void giveHitRewards(Player player, LuckyBlockModel model, Location blockLoc) {
        Random rand = new Random();
        for (Map<String, Object> reward : model.getHitRewards()) {
            int chance = (int) reward.getOrDefault("chance", 100);
            if (rand.nextInt(100) < chance) {
                if (reward.containsKey("material")) {
                    Material mat = Material.matchMaterial((String) reward.get("material"));
                    int amount = (int) reward.getOrDefault("amount", 1);
                    if (mat != null && blockLoc.getWorld() != null) {
                        blockLoc.getWorld().dropItemNaturally(blockLoc.clone().add(0.5, 1, 0.5), new ItemStack(mat, amount));
                    }
                }
                if (reward.containsKey("command")) {
                    String cmd = ((String) reward.get("command")).replace("%player%", player.getName());
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
                }
            }
        }
    }

    private void handleBlockBreak(Player player, LuckyBlockModel model, Location blockLoc) {
        World world = blockLoc.getWorld();
        if (world != null) {
            try {
                world.playSound(blockLoc, Sound.valueOf(model.getBreakSound()), 1.0f, 1.0f);
            } catch (Exception ignored) {}
            world.spawnParticle(Particle.EXPLOSION_LARGE, blockLoc.clone().add(0.5, 0.5, 0.5), 1);

            for (String soundName : model.getLastHitSounds()) {
                try {
                    player.playSound(player.getLocation(), Sound.valueOf(soundName), 1.0f, 1.0f);
                } catch (Exception ignored) {}
            }
        }

        if (model.getLastHitExp() > 0) {
            player.giveExp(model.getLastHitExp());
        }

        for (String cmd : model.getLastHitCommands()) {
            String formattedCmd = cmd.replace("%player%", player.getName());
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), formattedCmd);
        }

        for (Map<String, Object> itemMap : model.getLastHitItems()) {
            Material mat = Material.matchMaterial((String) itemMap.get("material"));
            int amount = (int) itemMap.getOrDefault("amount", 1);
            if (mat != null && world != null) {
                world.dropItemNaturally(blockLoc.clone().add(0.5, 1, 0.5), new ItemStack(mat, amount));
            }
        }

        blockLoc.getBlock().setType(Material.BEDROCK);
        removeHologram(model.getId());

        new BukkitRunnable() {
            @Override
            public void run() {
                blockLoc.getBlock().setType(model.getBlockType());
                currentHits.put(model.getId(), model.getMaxHits());
                spawnHologram(model, model.getMaxHits());
            }
        }.runTaskLater(plugin, model.getRespawnDelaySeconds() * 20L);
    }

    private void spawnHologram(LuckyBlockModel model, int hits) {
        Location hologramLoc = model.getLocation().clone().add(0.5, 1.2, 0.5);
        World world = hologramLoc.getWorld();
        if (world == null) return;

        removeHologram(model.getId());

        ArmorStand stand = (ArmorStand) world.spawnEntity(hologramLoc, EntityType.ARMOR_STAND);
        stand.setVisible(false);
        stand.setGravity(false);
        stand.setCustomNameVisible(true);
        stand.setMarker(true);

        holograms.put(model.getId(), stand);
        updateHologram(model, hits);
    }

    public void updateHologram(LuckyBlockModel model, int currentHits) {
        ArmorStand stand = holograms.get(model.getId());
        if (stand == null || !stand.isValid()) return;

        String healthBar = getHealthBar(currentHits, model.getMaxHits());
        String name = ChatColor.translateAlternateColorCodes('&', model.getDisplayName() + "\n&7[" + healthBar + "&7]");
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

    public void removeHologram(String id) {
        ArmorStand stand = holograms.remove(id);
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
        return activeLocations.containsKey(loc.getBlock().getLocation());
    }

    public LuckyBlockModel getModel(String id) { return blokModels.get(id); }
    public Set<String> getBlokIds() { return blokModels.keySet(); }
}
