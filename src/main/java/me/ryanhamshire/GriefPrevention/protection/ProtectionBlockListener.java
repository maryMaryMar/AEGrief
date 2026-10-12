package me.ryanhamshire.GriefPrevention.protection;

import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.CreateClaimResult;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.Visualization;
import me.ryanhamshire.GriefPrevention.VisualizationType;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import me.ryanhamshire.GriefPrevention.gui.ProtectionGui;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Iterator;

public class ProtectionBlockListener implements Listener {

    private final GriefPrevention plugin;

    public ProtectionBlockListener(GriefPrevention plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        ProtectionBlockType type = ProtectionBlockType.fromItemStack(item);
        if (type == null) {
            return;
        }

        Player player = event.getPlayer();
        Block block = event.getBlockPlaced();
        World world = block.getWorld();

        if (!plugin.claimsEnabledForWorld(world)) {
            player.sendMessage(ChatColor.RED + "Las protecciones están desactivadas en este mundo.");
            event.setCancelled(true);
            return;
        }

        if (!plugin.protectionConfig.isEnabled(type)) {
            player.sendMessage(ChatColor.RED + "Este tipo de bloque de protección está temporalmente desactivado.");
            event.setCancelled(true);
            return;
        }

        if (!player.hasPermission("griefprevention.createclaims")) {
            player.sendMessage(ChatColor.RED + "No tienes permiso para crear protecciones.");
            event.setCancelled(true);
            return;
        }

        int radius = plugin.protectionConfig.getRadius(type);
        int blockX = block.getX();
        int blockZ = block.getZ();

        int x1 = blockX - radius;
        int x2 = blockX + radius;
        int z1 = blockZ - radius;
        int z2 = blockZ + radius;
        int y1 = world.getMinHeight();
        int y2 = world.getMaxHeight() - 1;

        CreateClaimResult result = plugin.dataStore.createClaim(
                world, x1, x2, y1, y2, z1, z2,
                player.getUniqueId(), null, null, player
        );

        if (!result.succeeded) {
            event.setCancelled(true);
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            player.sendMessage(ChatColor.RED + "✖ No puedes colocar una protección aquí: El área de " + (radius * 2) + "x" + (radius * 2) + " se solapa con otra protección existente.");
            return;
        }

        Claim newClaim = result.claim;
        if (newClaim != null) {
            newClaim.centerBlockLocation = block.getLocation();
            plugin.dataStore.saveClaim(newClaim);

            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.9f, 1.2f);
            player.sendTitle(ChatColor.GREEN + "¡Protección Creada!", ChatColor.GRAY + "Radio: " + ChatColor.YELLOW + radius + "b " + ChatColor.DARK_GRAY + "(" + (radius * 2) + "x" + (radius * 2) + ")", 10, 40, 10);
            player.sendMessage(ChatColor.DARK_GRAY + "▪ " + ChatColor.GREEN + "Has activado tu " + plugin.protectionConfig.getDisplayName(type) + ChatColor.GREEN + ".");
            player.sendMessage(ChatColor.DARK_GRAY + "▪ " + ChatColor.GRAY + "Límites: " + ChatColor.YELLOW + (radius * 2) + "x" + (radius * 2) + " bloques" + ChatColor.GRAY + " (" + radius + " hacia cada lado).");
            player.sendMessage(ChatColor.DARK_GRAY + "▪ " + ChatColor.YELLOW + "Indestructible: Gestiónalo o retíralo con clic derecho o /ps.");

            Visualization visual = Visualization.FromClaim(newClaim, block.getY(), VisualizationType.Claim, player.getLocation());
            Visualization.Apply(player, visual);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block block = event.getClickedBlock();
        if (block == null) return;

        Claim claim = plugin.dataStore.getClaimAt(block.getLocation(), false, null);
        if (claim == null) return;

        boolean isCenter = false;
        if (claim.centerBlockLocation != null) {
            isCenter = isSameBlock(claim.centerBlockLocation, block.getLocation());
        } else if (ProtectionBlockType.fromMaterial(block.getType()) != null) {
            isCenter = true;
        }

        if (isCenter) {
            Player player = event.getPlayer();
            // Allow placing blocks against it when sneaking
            if (player.isSneaking()) {
                return;
            }
            event.setCancelled(true);
            ProtectionGui.open(this.plugin, player, claim);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Claim claim = plugin.dataStore.getClaimAt(block.getLocation(), false, null);
        if (claim == null) {
            return;
        }

        // Verify if this exact block is the center block OR a fallback legacy protection block
        boolean isCenter = false;
        if (claim.centerBlockLocation != null) {
            isCenter = isSameBlock(claim.centerBlockLocation, block.getLocation());
        } else {
            ProtectionBlockType legacyType = ProtectionBlockType.fromMaterial(block.getType());
            if (legacyType != null) {
                isCenter = true;
            }
        }

        if (!isCenter) {
            return;
        }

        // The center protection block is 100% INDESTRUCTIBLE: nobody (not even the owner
        // or an admin) can break it. The only way to remove it is the GUI (/ps) or right
        // click > "Retirar Protección" with shift + click.
        Player player = event.getPlayer();

        event.setCancelled(true);
        event.setExpToDrop(0);
        event.setDropItems(false);

        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 1.0f, 1.0f);
        player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "✖ ¡Los bloques de protección de AEGrief son INDESTRUCTIBLES!");
        player.sendMessage(ChatColor.GRAY + "▪ No se pueden romper en el mundo. Para gestionarlo o retirarlo:");
        player.sendMessage(ChatColor.GRAY + "▪ Usa " + ChatColor.WHITE + "clic derecho" + ChatColor.GRAY + " sobre el bloque o el comando " + ChatColor.YELLOW + "/ps" + ChatColor.GRAY + ".");
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onBlockDropItem(BlockDropItemEvent event) {
        Location loc = event.getBlock().getLocation();

        Claim claim = plugin.dataStore.getClaimAt(loc, false, null);
        if (claim != null && claim.centerBlockLocation != null && isSameBlock(claim.centerBlockLocation, loc)) {
            event.getItems().clear();
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        Iterator<Block> iterator = event.blockList().iterator();
        while (iterator.hasNext()) {
            Block block = iterator.next();
            Claim claim = plugin.dataStore.getClaimAt(block.getLocation(), false, null);
            if (claim != null && claim.centerBlockLocation != null && isSameBlock(claim.centerBlockLocation, block.getLocation())) {
                iterator.remove();
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        Iterator<Block> iterator = event.blockList().iterator();
        while (iterator.hasNext()) {
            Block block = iterator.next();
            Claim claim = plugin.dataStore.getClaimAt(block.getLocation(), false, null);
            if (claim != null && claim.centerBlockLocation != null && isSameBlock(claim.centerBlockLocation, block.getLocation())) {
                iterator.remove();
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        for (Block block : event.getBlocks()) {
            Claim claim = plugin.dataStore.getClaimAt(block.getLocation(), false, null);
            if (claim != null && claim.centerBlockLocation != null && isSameBlock(claim.centerBlockLocation, block.getLocation())) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        for (Block block : event.getBlocks()) {
            Claim claim = plugin.dataStore.getClaimAt(block.getLocation(), false, null);
            if (claim != null && claim.centerBlockLocation != null && isSameBlock(claim.centerBlockLocation, block.getLocation())) {
                event.setCancelled(true);
                return;
            }
        }
    }

    private boolean isSameBlock(Location loc1, Location loc2) {
        if (loc1 == null || loc2 == null) return false;
        if (loc1.getWorld() != null && loc2.getWorld() != null && !loc1.getWorld().equals(loc2.getWorld())) {
            return false;
        }
        return loc1.getBlockX() == loc2.getBlockX()
                && loc1.getBlockY() == loc2.getBlockY()
                && loc1.getBlockZ() == loc2.getBlockZ();
    }
}
