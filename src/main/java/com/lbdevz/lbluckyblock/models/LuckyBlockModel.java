package com.lbdevz.lbluckyblock.models;

import org.bukkit.Location;
import org.bukkit.Material;

import java.util.List;
import java.util.Map;

public class LuckyBlockModel {
    private final String id;
    private final String displayName;
    private final Material blockType;
    private final int maxHits;
    private final int respawnDelaySeconds;
    private final String hitSound;
    private final String breakSound;
    private final Location location;
    private final List<Map<String, Object>> hitRewards;
    private final List<String> lastHitCommands;
    private final List<Map<String, Object>> lastHitItems;
    private final List<String> lastHitSounds;
    private final int lastHitExp;

    public LuckyBlockModel(String id, String displayName, Material blockType, int maxHits, int respawnDelaySeconds,
                           String hitSound, String breakSound, Location location,
                           List<Map<String, Object>> hitRewards, List<String> lastHitCommands,
                           List<Map<String, Object>> lastHitItems, List<String> lastHitSounds, int lastHitExp) {
        this.id = id;
        this.displayName = displayName;
        this.blockType = blockType;
        this.maxHits = maxHits;
        this.respawnDelaySeconds = respawnDelaySeconds;
        this.hitSound = hitSound;
        this.breakSound = breakSound;
        this.location = location;
        this.hitRewards = hitRewards;
        this.lastHitCommands = lastHitCommands;
        this.lastHitItems = lastHitItems;
        this.lastHitSounds = lastHitSounds;
        this.lastHitExp = lastHitExp;
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public Material getBlockType() { return blockType; }
    public int getMaxHits() { return maxHits; }
    public int getRespawnDelaySeconds() { return respawnDelaySeconds; }
    public String getHitSound() { return hitSound; }
    public String getBreakSound() { return breakSound; }
    public Location getLocation() { return location; }
    public List<Map<String, Object>> getHitRewards() { return hitRewards; }
    public List<String> getLastHitCommands() { return lastHitCommands; }
    public List<Map<String, Object>> getLastHitItems() { return lastHitItems; }
    public List<String> getLastHitSounds() { return lastHitSounds; }
    public int getLastHitExp() { return lastHitExp; }
}
