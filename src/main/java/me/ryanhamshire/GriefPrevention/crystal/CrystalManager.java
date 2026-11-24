package me.ryanhamshire.GriefPrevention.crystal;

import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.ClaimPermission;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.flags.ClaimFlagListener;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CrystalManager implements Listener {

    private final GriefPrevention plugin;
    private final File dataFile;
    private final Map<Long, Set<CrystalType>> claimCrystals = new ConcurrentHashMap<>();

    // Tracking Phoenix cooldowns (Flight & fall protection are unified in ClaimFlagListener)
    private final Map<UUID, Long> phoenixCooldowns = new ConcurrentHashMap<>();
    private BukkitTask task;

    private static final PotionEffectType HASTE_EFFECT = resolvePotionEffect("haste", "FAST_DIGGING");
    private static final PotionEffectType RESISTANCE_EFFECT = resolvePotionEffect("resistance", "DAMAGE_RESISTANCE");
    private static final Particle TOTEM_PARTICLE = resolveParticle("TOTEM_OF_UNDYING", "TOTEM");

    public CrystalManager(GriefPrevention plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "claim_crystals.yml");
        load();
        startTask();
    }

    private static PotionEffectType resolvePotionEffect(String minecraftKey, String legacyName) {
        PotionEffectType byKey = PotionEffectType.getByKey(NamespacedKey.minecraft(minecraftKey));
        if (byKey != null) return byKey;
        return PotionEffectType.getByName(legacyName);
    }

    private static Particle resolveParticle(String primary, String fallback) {
        try {
            return Particle.valueOf(primary);
        } catch (IllegalArgumentException e) {
            try {
                return Particle.valueOf(fallback);
            } catch (IllegalArgumentException ignored) {
                return Particle.CRIT;
            }
        }
    }

    public void save() {
        YamlConfiguration config = new YamlConfiguration();
        for (Map.Entry<Long, Set<CrystalType>> entry : claimCrystals.entrySet()) {
            List<String> typeNames = new ArrayList<>();
            for (CrystalType type : entry.getValue()) {
                typeNames.add(type.name());
            }
            config.set("claims." + entry.getKey(), typeNames);
        }
        try {
            config.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save claim_crystals.yml: " + e.getMessage());
        }
    }

    public void load() {
        claimCrystals.clear();
        if (!dataFile.exists()) return;
        YamlConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
        if (config.isConfigurationSection("claims")) {
            for (String key : config.getConfigurationSection("claims").getKeys(false)) {
                try {
                    long claimId = Long.parseLong(key);
                    List<String> typeNames = config.getStringList("claims." + key);
                    Set<CrystalType> types = EnumSet.noneOf(CrystalType.class);
                    for (String name : typeNames) {
                        try {
                            types.add(CrystalType.valueOf(name));
                        } catch (IllegalArgumentException ignored) {}
                    }
                    if (!types.isEmpty()) {
                        claimCrystals.put(claimId, types);
                    }
                } catch (NumberFormatException ignored) {}
            }
        }
    }

    public boolean toggleCrystal(Long claimId, CrystalType type) {
        if (claimId == null || type == null) return false;
        Set<CrystalType> types = claimCrystals.computeIfAbsent(claimId, k -> EnumSet.noneOf(CrystalType.class));
        boolean added;
        if (types.contains(type)) {
            types.remove(type);
            if (types.isEmpty()) {
                claimCrystals.remove(claimId);
            }
            added = false;
        } else {
            types.add(type);
            added = true;
        }
        save();
        return added;
    }

    public Set<CrystalType> getActiveCrystals(Long claimId) {
        if (claimId == null) return Collections.emptySet();
        Set<CrystalType> types = claimCrystals.get(claimId);
        return types != null ? Collections.unmodifiableSet(types) : Collections.emptySet();
    }

    public boolean hasCrystal(Long claimId, CrystalType type) {
        if (claimId == null || type == null) return false;
        Set<CrystalType> types = claimCrystals.get(claimId);
        return types != null && types.contains(type);
    }

    private void startTask() {
        task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                Location loc = player.getLocation();
                Claim claim = plugin.dataStore.getClaimAt(loc, true, null);
                if (claim == null) continue;

                boolean hasAccess = claim.checkPermission(player, ClaimPermission.Access, null) == null;
                if (!hasAccess) continue;

                Long rootClaimId = ClaimFlagListener.getRootClaimId(claim);
                Set<CrystalType> crystals = getActiveCrystals(rootClaimId);
                if (crystals.isEmpty()) continue;

                if (crystals.contains(CrystalType.HASTE) && HASTE_EFFECT != null) {
                    player.addPotionEffect(new PotionEffect(HASTE_EFFECT, 60, 1, true, false, false));
                }
                if (crystals.contains(CrystalType.SPEED)) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 60, 1, true, false, false));
                }
                if (crystals.contains(CrystalType.GUARDIAN)) {
                    if (RESISTANCE_EFFECT != null) {
                        player.addPotionEffect(new PotionEffect(RESISTANCE_EFFECT, 60, 0, true, false, false));
                    }
                    player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 60, 0, true, false, false));
                }
                if (crystals.contains(CrystalType.SUSTENANCE)) {
                    if (player.getFoodLevel() < 20 || player.getSaturation() < 20.0f) {
                        player.setFoodLevel(20);
                        player.setSaturation(20.0f);
                    }
                }
            }
        }, 0L, 20L);
    }

    public void shutdown() {
        if (task != null) {
            task.cancel();
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();
        UUID uuid = player.getUniqueId();

        // Handle PHOENIX crystal
        double finalDamage = event.getFinalDamage();
        if (player.getHealth() - finalDamage <= 0.0) {
            Location loc = player.getLocation();
            Claim claim = plugin.dataStore.getClaimAt(loc, true, null);
            if (claim != null && claim.checkPermission(player, ClaimPermission.Access, null) == null) {
                Long rootClaimId = ClaimFlagListener.getRootClaimId(claim);
                if (hasCrystal(rootClaimId, CrystalType.PHOENIX)) {
                    long now = System.currentTimeMillis();
                    long cooldownEnd = phoenixCooldowns.getOrDefault(uuid, 0L);
                    if (now >= cooldownEnd) {
                        event.setCancelled(true);
                        phoenixCooldowns.put(uuid, now + 300000L); // 5 minutes cooldown

                        player.setHealth(Math.min(player.getMaxHealth(), 8.0));
                        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 100, 1, true, false, true));
                        player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 100, 1, true, false, true));

                        if (loc.getWorld() != null) {
                            loc.getWorld().playSound(loc, Sound.ITEM_TOTEM_USE, 1.0f, 1.0f);
                            loc.getWorld().spawnParticle(TOTEM_PARTICLE, loc.clone().add(0, 1, 0), 50, 0.5, 0.5, 0.5, 0.1);
                        }

                        player.sendMessage("§6[Crystal] §eEl Cristal Fénix ha evitado tu muerte en este terreno.");
                    }
                }
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        phoenixCooldowns.remove(event.getPlayer().getUniqueId());
    }
}
