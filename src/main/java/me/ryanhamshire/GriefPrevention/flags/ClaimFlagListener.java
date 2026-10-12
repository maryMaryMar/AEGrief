package me.ryanhamshire.GriefPrevention.flags;

import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wither;
import org.bukkit.entity.WitherSkull;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockFormEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.weather.LightningStrikeEvent;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ClaimFlagListener implements Listener {

    private final GriefPrevention plugin;

    // Track players who received flight via claim FLY flag
    public static final Set<UUID> flyingPlayers = Collections.newSetFromMap(new ConcurrentHashMap<>());

    // Track fall immunity expiration timestamp for players exiting FLY claims (8 seconds)
    public static final Map<UUID, Long> fallGraceMap = new ConcurrentHashMap<>();

    public ClaimFlagListener(GriefPrevention plugin) {
        this.plugin = plugin;

        // Background Watcher Task (every 10 ticks / 0.5s) for instant flight safety across the server
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                enforceFlightAtLocation(p);
            }
        }, 20L, 10L);
    }

    public static boolean hasActiveRankFly(Player player) {
        if (player == null) return false;
        try {
            org.bukkit.plugin.Plugin essPlugin = Bukkit.getPluginManager().getPlugin("Essentials");
            if (essPlugin != null && essPlugin.isEnabled()) {
                java.lang.reflect.Method getUserMethod = essPlugin.getClass().getMethod("getUser", Player.class);
                Object user = getUserMethod.invoke(essPlugin, player);
                if (user != null) {
                    java.lang.reflect.Method isFlyModeEnabled = user.getClass().getMethod("isFlyModeEnabled");
                    Object res = isFlyModeEnabled.invoke(user);
                    if (res instanceof Boolean && (Boolean) res) {
                        return true;
                    }
                }
            }
        } catch (Throwable ignored) {}
        return false;
    }

    public static void enforceFlightAtLocation(Player player) {
        if (player == null || !player.isOnline()) return;
        if (player.getGameMode() != GameMode.SURVIVAL && player.getGameMode() != GameMode.ADVENTURE) return;
        if (hasActiveRankFly(player)) return;

        UUID uuid = player.getUniqueId();
        Claim claim = GriefPrevention.instance.dataStore.getClaimAt(player.getLocation(), true, null);
        boolean inFlyClaim = (claim != null && claim.isFlagAllowed(ClaimFlag.FLY));

        if (inFlyClaim) {
            if (!player.getAllowFlight()) {
                player.setAllowFlight(true);
                flyingPlayers.add(uuid);
                sendActionBar(player, ChatColor.GREEN + "✈ Modo Vuelo activado en esta protección.");
            }
        } else {
            if (flyingPlayers.contains(uuid) || player.getAllowFlight() || player.isFlying()) {
                revokeFlight(player, uuid);
            }
        }
    }

    /* ----------------------------------------------------
     *  FLAG: ENTRY (Campo de Fuerza / Aislamiento de Intrusos)
     * ---------------------------------------------------- */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerEntryRestriction(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) return;
        if (from.getBlockX() == to.getBlockX() && from.getBlockY() == to.getBlockY() && from.getBlockZ() == to.getBlockZ()) {
            return;
        }

        Player player = event.getPlayer();
        if (player.hasPermission("griefprevention.adminclaims") || player.hasPermission("griefprevention.claimflag.bypass")) {
            return;
        }

        Claim toClaim = plugin.dataStore.getClaimAt(to, true, null);
        if (toClaim == null || toClaim.isFlagAllowed(ClaimFlag.ENTRY)) {
            return;
        }

        // ENTRY flag is DENIED (Campo de fuerza activo). Check if player has access permission/trust
        if (toClaim.allowAccess(player) == null) {
            return; // Player is owner, trusted or manager
        }

        // Intruder detected!
        Claim fromClaim = plugin.dataStore.getClaimAt(from, true, null);
        if (fromClaim != null && (fromClaim.getID().equals(toClaim.getID()) || (fromClaim.parent != null && fromClaim.parent.getID().equals(toClaim.getID())))) {
            // Player is already inside the restricted claim -> Eject them outside!
            plugin.ejectPlayer(player);
            player.sendMessage(ChatColor.RED + "✖ Has sido expulsado fuera del terreno: El propietario ha cerrado la entrada a intrusos.");
            return;
        }

        // Intruding from outside -> Block passage by keeping 'from' coordinates
        Location safeFrom = from.clone();
        safeFrom.setPitch(to.getPitch());
        safeFrom.setYaw(to.getYaw());
        event.setTo(safeFrom);
        sendActionBar(player, ChatColor.RED + "✖ Campo de fuerza activo: Entrada denegada a intrusos.");
    }

    /* ----------------------------------------------------
     *  FLAG: FLY (Auto flight + 8s Fall Grace + Combat cancel)
     *  FLAG: ENTRY_MESSAGE & EXIT_MESSAGE
     * ---------------------------------------------------- */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) return;
        if (from.getBlockX() == to.getBlockX() && from.getBlockY() == to.getBlockY() && from.getBlockZ() == to.getBlockZ()) {
            return;
        }

        Player player = event.getPlayer();
        if (hasActiveRankFly(player)) return;

        UUID uuid = player.getUniqueId();
        Claim fromClaim = plugin.dataStore.getClaimAt(from, true, null);
        Claim toClaim = plugin.dataStore.getClaimAt(to, true, null);

        // When crossing borders between different claims or wilderness
        if (fromClaim != toClaim) {
            // Exit from previous claim
            if (fromClaim != null) {
                String exitMsg = fromClaim.getFlag(ClaimFlag.EXIT_MESSAGE);
                if (exitMsg != null && !exitMsg.isEmpty()) {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', exitMsg));
                }
                if (toClaim == null || !toClaim.isFlagAllowed(ClaimFlag.FLY)) {
                    if (flyingPlayers.contains(uuid)) {
                        revokeFlight(player, uuid);
                    }
                }
            }

            // Enter new claim
            if (toClaim != null) {
                String entryMsg = toClaim.getFlag(ClaimFlag.ENTRY_MESSAGE);
                if (entryMsg != null && !entryMsg.isEmpty()) {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', entryMsg));
                }
                if (toClaim.isFlagAllowed(ClaimFlag.FLY)) {
                    if (player.getGameMode() == GameMode.SURVIVAL || player.getGameMode() == GameMode.ADVENTURE) {
                        player.setAllowFlight(true);
                        flyingPlayers.add(uuid);
                        sendActionBar(player, ChatColor.GREEN + "✈ Modo Vuelo activado en esta protección.");
                    }
                }
            }
        } else {
            // Moving within same claim or wilderness
            if (toClaim != null && toClaim.isFlagAllowed(ClaimFlag.FLY)) {
                if ((player.getGameMode() == GameMode.SURVIVAL || player.getGameMode() == GameMode.ADVENTURE) && !player.getAllowFlight()) {
                    player.setAllowFlight(true);
                    flyingPlayers.add(uuid);
                    sendActionBar(player, ChatColor.GREEN + "✈ Modo Vuelo activado en esta protección.");
                }
            } else if (flyingPlayers.contains(uuid)) {
                revokeFlight(player, uuid);
            }
        }
    }

    public static void revokeFlight(Player player, UUID uuid) {
        flyingPlayers.remove(uuid);
        if (hasActiveRankFly(player)) return;

        if (player.getGameMode() == GameMode.SURVIVAL || player.getGameMode() == GameMode.ADVENTURE) {
            player.setAllowFlight(false);
            player.setFlying(false);
            // 8 seconds fall damage grace period
            fallGraceMap.put(uuid, System.currentTimeMillis() + 8000L);
            sendActionBar(player, ChatColor.YELLOW + "⚠ Saliendo de la protección: Vuelo desactivado (8s inmunidad a caída).");
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFallDamage(EntityDamageEvent event) {
        if (event.getCause() != EntityDamageEvent.DamageCause.FALL) return;
        if (!(event.getEntity() instanceof Player)) return;

        Player player = (Player) event.getEntity();
        Long expireTime = fallGraceMap.get(player.getUniqueId());
        if (expireTime != null) {
            if (System.currentTimeMillis() < expireTime) {
                event.setCancelled(true);
            } else {
                fallGraceMap.remove(player.getUniqueId());
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        flyingPlayers.remove(uuid);
        fallGraceMap.remove(uuid);
    }

    /* ----------------------------------------------------
     *  FLAG: PVP (Allow/Deny + Combat flight cancel)
     * ---------------------------------------------------- */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCombatDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player defender = (Player) event.getEntity();

        Player attacker = null;
        if (event.getDamager() instanceof Player) {
            attacker = (Player) event.getDamager();
        }

        if (attacker != null) {
            Claim claim = plugin.dataStore.getClaimAt(defender.getLocation(), true, null);
            if (claim != null) {
                if (!claim.isFlagAllowed(ClaimFlag.PVP)) {
                    attacker.sendMessage(ChatColor.RED + "✖ El combate PvP está desactivado en esta protección.");
                    event.setCancelled(true);
                    return;
                }
            }

            // In combat: disable fly immediately to prevent combat flying
            if (flyingPlayers.contains(attacker.getUniqueId())) {
                revokeFlight(attacker, attacker.getUniqueId());
                attacker.sendMessage(ChatColor.RED + "✖ ¡Modo Vuelo cancelado por entrar en combate!");
            }
            if (flyingPlayers.contains(defender.getUniqueId())) {
                revokeFlight(defender, defender.getUniqueId());
                defender.sendMessage(ChatColor.RED + "✖ ¡Modo Vuelo cancelado por entrar en combate!");
            }
        }
    }

    /* ----------------------------------------------------
     *  FLAG: MONSTER_DAMAGE
     * ---------------------------------------------------- */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMonsterDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        if (event.getDamager() instanceof Monster) {
            Claim claim = plugin.dataStore.getClaimAt(event.getEntity().getLocation(), true, null);
            if (claim != null && !claim.isFlagAllowed(ClaimFlag.MONSTER_DAMAGE)) {
                event.setCancelled(true);
            }
        }
    }

    /* ----------------------------------------------------
     *  FLAG: EXPLOSIONS
     * ---------------------------------------------------- */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onExplosion(EntityExplodeEvent event) {
        Claim claim = plugin.dataStore.getClaimAt(event.getLocation(), true, null);
        if (claim != null && !claim.isFlagAllowed(ClaimFlag.EXPLOSIONS)) {
            event.blockList().clear();
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockExplosion(BlockExplodeEvent event) {
        Claim claim = plugin.dataStore.getClaimAt(event.getBlock().getLocation(), true, null);
        if (claim != null && !claim.isFlagAllowed(ClaimFlag.EXPLOSIONS)) {
            event.blockList().clear();
            event.setCancelled(true);
        }
    }

    /* ----------------------------------------------------
     *  FLAG: MOB_SPAWNING & PASSIVE_SPAWNING
     * ---------------------------------------------------- */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        Claim claim = plugin.dataStore.getClaimAt(event.getLocation(), true, null);
        if (claim == null) return;

        Entity entity = event.getEntity();
        if (entity instanceof Monster) {
            if (!claim.isFlagAllowed(ClaimFlag.MOB_SPAWNING)) {
                event.setCancelled(true);
            }
        } else if (entity instanceof Animals) {
            if (!claim.isFlagAllowed(ClaimFlag.PASSIVE_SPAWNING)) {
                event.setCancelled(true);
            }
        }
    }

    /* ----------------------------------------------------
     *  FLAG: FIRE_SPREAD
     * ---------------------------------------------------- */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBurn(BlockBurnEvent event) {
        Claim claim = plugin.dataStore.getClaimAt(event.getBlock().getLocation(), true, null);
        if (claim != null && !claim.isFlagAllowed(ClaimFlag.FIRE_SPREAD)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFireSpread(BlockSpreadEvent event) {
        if (event.getSource().getType() == Material.FIRE || event.getNewState().getType() == Material.FIRE) {
            Claim claim = plugin.dataStore.getClaimAt(event.getBlock().getLocation(), true, null);
            if (claim != null && !claim.isFlagAllowed(ClaimFlag.FIRE_SPREAD)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockIgnite(BlockIgniteEvent event) {
        Claim claim = plugin.dataStore.getClaimAt(event.getBlock().getLocation(), true, null);
        if (claim != null && !claim.isFlagAllowed(ClaimFlag.FIRE_SPREAD)) {
            if (event.getCause() == BlockIgniteEvent.IgniteCause.SPREAD || event.getCause() == BlockIgniteEvent.IgniteCause.LAVA) {
                event.setCancelled(true);
            }
        }
    }

    /* ----------------------------------------------------
     *  FLAG: LAVA_FLOW (Liquid Flow + Lava & Fire Damage Immunity)
     *  FLAG: WATER_FLOW
     * ---------------------------------------------------- */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onLavaDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();
        EntityDamageEvent.DamageCause cause = event.getCause();

        if (cause == EntityDamageEvent.DamageCause.LAVA || cause == EntityDamageEvent.DamageCause.HOT_FLOOR) {
            Claim claim = plugin.dataStore.getClaimAt(player.getLocation(), true, null);
            if (claim != null && !claim.isFlagAllowed(ClaimFlag.LAVA_FLOW)) {
                event.setCancelled(true);
                player.setFireTicks(0);
                return;
            }
        }

        if (cause == EntityDamageEvent.DamageCause.FIRE_TICK || cause == EntityDamageEvent.DamageCause.FIRE) {
            Claim claim = plugin.dataStore.getClaimAt(player.getLocation(), true, null);
            if (claim != null && (!claim.isFlagAllowed(ClaimFlag.LAVA_FLOW) || !claim.isFlagAllowed(ClaimFlag.FIRE_SPREAD))) {
                event.setCancelled(true);
                player.setFireTicks(0);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onLiquidFlow(BlockFromToEvent event) {
        Material type = event.getBlock().getType();
        Claim claim = plugin.dataStore.getClaimAt(event.getToBlock().getLocation(), true, null);
        if (claim == null) return;

        if (type == Material.LAVA) {
            if (!claim.isFlagAllowed(ClaimFlag.LAVA_FLOW)) {
                event.setCancelled(true);
            }
        } else if (type == Material.WATER) {
            if (!claim.isFlagAllowed(ClaimFlag.WATER_FLOW)) {
                event.setCancelled(true);
            }
        }
    }

    /* ----------------------------------------------------
     *  FLAG: LEAF_DECAY
     * ---------------------------------------------------- */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onLeafDecay(LeavesDecayEvent event) {
        Claim claim = plugin.dataStore.getClaimAt(event.getBlock().getLocation(), true, null);
        if (claim != null && !claim.isFlagAllowed(ClaimFlag.LEAF_DECAY)) {
            event.setCancelled(true);
        }
    }

    /* ----------------------------------------------------
     *  FLAG: PISTONS
     * ---------------------------------------------------- */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        for (Block block : event.getBlocks()) {
            Claim claim = plugin.dataStore.getClaimAt(block.getLocation(), true, null);
            if (claim != null && !claim.isFlagAllowed(ClaimFlag.PISTONS)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        for (Block block : event.getBlocks()) {
            Claim claim = plugin.dataStore.getClaimAt(block.getLocation(), true, null);
            if (claim != null && !claim.isFlagAllowed(ClaimFlag.PISTONS)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    /* ----------------------------------------------------
     *  FLAG: ITEM_DROP & ITEM_PICKUP
     * ---------------------------------------------------- */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onItemDrop(PlayerDropItemEvent event) {
        Claim claim = plugin.dataStore.getClaimAt(event.getPlayer().getLocation(), true, null);
        if (claim != null && !claim.isFlagAllowed(ClaimFlag.ITEM_DROP)) {
            event.getPlayer().sendMessage(ChatColor.RED + "✖ No puedes soltar objetos en esta protección.");
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onItemPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Claim claim = plugin.dataStore.getClaimAt(event.getItem().getLocation(), true, null);
        if (claim != null && !claim.isFlagAllowed(ClaimFlag.ITEM_PICKUP)) {
            event.setCancelled(true);
        }
    }

    /* ----------------------------------------------------
     *  FLAG: LIGHTNING
     * ---------------------------------------------------- */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onLightning(LightningStrikeEvent event) {
        Claim claim = plugin.dataStore.getClaimAt(event.getLightning().getLocation(), true, null);
        if (claim != null && !claim.isFlagAllowed(ClaimFlag.LIGHTNING)) {
            event.setCancelled(true);
        }
    }

    /* ----------------------------------------------------
     *  FLAG: WITHER_DAMAGE
     * ---------------------------------------------------- */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onWitherChangeBlock(EntityChangeBlockEvent event) {
        if (event.getEntity() instanceof Wither || event.getEntity() instanceof WitherSkull) {
            Claim claim = plugin.dataStore.getClaimAt(event.getBlock().getLocation(), true, null);
            if (claim != null && !claim.isFlagAllowed(ClaimFlag.WITHER_DAMAGE)) {
                event.setCancelled(true);
            }
        }
    }

    /* ----------------------------------------------------
     *  FLAG: CHORUS_FRUIT & ENDER_PEARL & FLY TELEPORT CHECK
     * ---------------------------------------------------- */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        PlayerTeleportEvent.TeleportCause cause = event.getCause();
        Location to = event.getTo();
        if (to == null) return;

        Claim targetClaim = plugin.dataStore.getClaimAt(to, true, null);
        if (targetClaim != null) {
            if (!targetClaim.isFlagAllowed(ClaimFlag.ENTRY)) {
                if (!player.hasPermission("griefprevention.adminclaims") && !player.hasPermission("griefprevention.claimflag.bypass")) {
                    if (targetClaim.allowAccess(player) != null) {
                        player.sendMessage(ChatColor.RED + "✖ No puedes teletransportarte dentro de esta protección (Entrada restringida / Campo de fuerza).");
                        event.setCancelled(true);
                        return;
                    }
                }
            }
            if (cause == PlayerTeleportEvent.TeleportCause.CHORUS_FRUIT) {
                if (!targetClaim.isFlagAllowed(ClaimFlag.CHORUS_FRUIT)) {
                    player.sendMessage(ChatColor.RED + "✖ La fruta coral está desactivada en esta protección.");
                    event.setCancelled(true);
                    return;
                }
            } else if (cause == PlayerTeleportEvent.TeleportCause.ENDER_PEARL) {
                if (!targetClaim.isFlagAllowed(ClaimFlag.ENDER_PEARL)) {
                    player.sendMessage(ChatColor.RED + "✖ Las perlas de ender están desactivadas en esta protección.");
                    event.setCancelled(true);
                    return;
                }
            }
        }

        if (hasActiveRankFly(player)) return;

        boolean wasFlying = flyingPlayers.contains(uuid) || player.getAllowFlight() || player.isFlying();
        if (wasFlying) {
            if (targetClaim == null || !targetClaim.isFlagAllowed(ClaimFlag.FLY)) {
                if (player.getGameMode() == GameMode.SURVIVAL || player.getGameMode() == GameMode.ADVENTURE) {
                    player.setAllowFlight(false);
                    player.setFlying(false);
                }
            }
            Bukkit.getScheduler().runTaskLater(plugin, () -> enforceFlightAtLocation(player), 1L);
            Bukkit.getScheduler().runTaskLater(plugin, () -> enforceFlightAtLocation(player), 3L);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldChange(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        if (hasActiveRankFly(player)) return;
        Bukkit.getScheduler().runTaskLater(plugin, () -> enforceFlightAtLocation(player), 1L);
        Bukkit.getScheduler().runTaskLater(plugin, () -> enforceFlightAtLocation(player), 3L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        if (hasActiveRankFly(player)) return;
        Bukkit.getScheduler().runTaskLater(plugin, () -> enforceFlightAtLocation(player), 1L);
        Bukkit.getScheduler().runTaskLater(plugin, () -> enforceFlightAtLocation(player), 3L);
    }

    /* ----------------------------------------------------
     *  FLAG: SLEEP
     * ---------------------------------------------------- */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBedEnter(PlayerBedEnterEvent event) {
        Claim claim = plugin.dataStore.getClaimAt(event.getBed().getLocation(), true, null);
        if (claim != null && !claim.isFlagAllowed(ClaimFlag.SLEEP)) {
            event.getPlayer().sendMessage(ChatColor.RED + "✖ No puedes dormir en esta protección.");
            event.setCancelled(true);
        }
    }

    /* ----------------------------------------------------
     *  FLAG: ICE_MELT
     * ---------------------------------------------------- */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockFade(BlockFadeEvent event) {
        Material mat = event.getBlock().getType();
        if (mat == Material.ICE || mat == Material.PACKED_ICE || mat == Material.FROSTED_ICE) {
            Claim claim = plugin.dataStore.getClaimAt(event.getBlock().getLocation(), true, null);
            if (claim != null && !claim.isFlagAllowed(ClaimFlag.ICE_MELT)) {
                event.setCancelled(true);
            }
        }
    }

    /* ----------------------------------------------------
     *  FLAG: SNOW_FALL
     * ---------------------------------------------------- */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockForm(BlockFormEvent event) {
        if (event.getNewState().getType() == Material.SNOW) {
            Claim claim = plugin.dataStore.getClaimAt(event.getBlock().getLocation(), true, null);
            if (claim != null && !claim.isFlagAllowed(ClaimFlag.SNOW_FALL)) {
                event.setCancelled(true);
            }
        }
    }

    /* ----------------------------------------------------
     *  FLAG: VILLAGER_TRADING
     * ---------------------------------------------------- */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (event.getInventory().getType() == InventoryType.MERCHANT) {
            Claim claim = plugin.dataStore.getClaimAt(event.getPlayer().getLocation(), true, null);
            if (claim != null && !claim.isFlagAllowed(ClaimFlag.VILLAGER_TRADING)) {
                event.getPlayer().sendMessage(ChatColor.RED + "✖ El comercio con aldeanos está desactivado en esta protección.");
                event.setCancelled(true);
            }
        }
    }

    public static void sendActionBar(Player player, String message) {
        try {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(message));
        } catch (Throwable t) {
            // Fallback
        }
    }
}
