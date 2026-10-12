package me.ryanhamshire.GriefPrevention.gui;

import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.ClaimPermission;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.clans.UltimateClansHook;
import me.ryanhamshire.GriefPrevention.flags.ClaimFlag;
import me.ryanhamshire.GriefPrevention.protection.ProtectionBlockType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ProtectionGuiListener implements Listener {

    private final GriefPrevention plugin;
    public static final Map<UUID, Long> openGuiClaims = new ConcurrentHashMap<>();

    public ProtectionGuiListener(GriefPrevention plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();
        if (title == null) return;

        // 1. Selector Menu Handling
        if (title.startsWith(ProtectionSelectorGui.SELECTOR_TITLE_PREFIX)) {
            event.setCancelled(true);
            int slot = event.getRawSlot();
            if (slot < 0 || slot >= 54) return;

            UUID uuid = player.getUniqueId();
            ProtectionSelectorGui.CategoryState state = ProtectionSelectorGui.playerStates.get(uuid);
            if (state == null) {
                state = new ProtectionSelectorGui.CategoryState(ProtectionSelectorGui.Category.OWNED, 0);
            }

            // Category Tab Clicks (Slots 10, 19, 28)
            if (slot == 10) {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.2f);
                ProtectionSelectorGui.open(plugin, player, ProtectionSelectorGui.Category.OWNED, 0);
                return;
            }
            if (slot == 19) {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.2f);
                ProtectionSelectorGui.open(plugin, player, ProtectionSelectorGui.Category.TRUSTED, 0);
                return;
            }
            if (slot == 28) {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.2f);
                ProtectionSelectorGui.open(plugin, player, ProtectionSelectorGui.Category.CLAN, 0);
                return;
            }

            // Pagination Clicks (Slots 48, 50)
            if (slot == 48 && state.page > 0) {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.0f);
                ProtectionSelectorGui.open(plugin, player, state.category, state.page - 1);
                return;
            }
            if (slot == 50) {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.0f);
                ProtectionSelectorGui.open(plugin, player, state.category, state.page + 1);
                return;
            }

            // Close (Slot 49)
            if (slot == 49) {
                player.closeInventory();
                return;
            }

            // Claim Item Click from Grid
            Map<Integer, Long> currentSlots = ProtectionSelectorGui.slotClaimMap.get(uuid);
            if (currentSlots != null && currentSlots.containsKey(slot)) {
                Long claimId = currentSlots.get(slot);
                Claim selected = plugin.dataStore.getClaim(claimId);
                if (selected != null) {
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1.3f);
                    ProtectionGui.open(plugin, player, selected);
                }
            }
            return;
        }

        // 2. Individual Protection Management GUI Handling
        if (title.startsWith(ProtectionGui.GUI_TITLE_PREFIX)) {
            event.setCancelled(true);
            int slot = event.getRawSlot();
            if (slot < 0 || slot >= 54) return;

            Long claimId = openGuiClaims.get(player.getUniqueId());
            if (claimId == null) return;

            Claim claim = plugin.dataStore.getClaim(claimId);
            if (claim == null) {
                player.closeInventory();
                return;
            }

            boolean isOwner = player.getUniqueId().equals(claim.ownerID);
            boolean isAdmin = player.hasPermission("griefprevention.admin");
            boolean canManage = isOwner || isAdmin || claim.checkPermission(player, ClaimPermission.Manage, null) == null;

            // Slot 45: Back Arrow to Selector GUI
            if (slot == 45) {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1.0f);
                ProtectionSelectorGui.open(plugin, player);
                return;
            }

            // Check if clicked slot is a Flag
            ClaimFlag flag = ProtectionGui.getFlagBySlot(slot);
            if (flag != null) {
                if (plugin.flagConfig != null && !plugin.flagConfig.isEnabled(flag)) {
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    player.sendMessage(ChatColor.RED + "✖ Esta flag ha sido desactivada por la administración del servidor.");
                    return;
                }

                if (!canManage) {
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    player.sendMessage(ChatColor.RED + "✖ No tienes permisos para modificar flags en esta protección (Propietario: " + claim.getOwnerName() + ").");
                    return;
                }

                String current = claim.getFlag(flag);
                String next = "allow".equalsIgnoreCase(current) ? "deny" : "allow";
                claim.setFlag(flag, next);
                plugin.dataStore.saveClaim(claim);

                if (flag == ClaimFlag.FLY) {
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        Claim pClaim = plugin.dataStore.getClaimAt(p.getLocation(), true, null);
                        if (pClaim != null && pClaim.getID().equals(claim.getID())) {
                            if ("allow".equalsIgnoreCase(next)) {
                                if (p.getGameMode() == org.bukkit.GameMode.SURVIVAL || p.getGameMode() == org.bukkit.GameMode.ADVENTURE) {
                                    p.setAllowFlight(true);
                                    me.ryanhamshire.GriefPrevention.flags.ClaimFlagListener.flyingPlayers.add(p.getUniqueId());
                                    me.ryanhamshire.GriefPrevention.flags.ClaimFlagListener.sendActionBar(p, ChatColor.GREEN + "✈ Modo Vuelo activado en esta protección.");
                                }
                            } else {
                                if (me.ryanhamshire.GriefPrevention.flags.ClaimFlagListener.flyingPlayers.contains(p.getUniqueId())) {
                                    me.ryanhamshire.GriefPrevention.flags.ClaimFlagListener.revokeFlight(p, p.getUniqueId());
                                }
                            }
                        }
                    }
                }

                if (flag == ClaimFlag.ENTRY) {
                    if ("deny".equalsIgnoreCase(next)) {
                        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&6🛡 &eCampo de fuerza &cACTIVADO&e: Los intrusos no autorizados serán expulsados y bloqueados."));
                        for (Player p : Bukkit.getOnlinePlayers()) {
                            if (p.equals(player) || p.hasPermission("griefprevention.adminclaims") || p.hasPermission("griefprevention.claimflag.bypass")) {
                                continue;
                            }
                            Claim pClaim = plugin.dataStore.getClaimAt(p.getLocation(), true, null);
                            if (pClaim != null && (pClaim.getID().equals(claim.getID()) || (pClaim.parent != null && pClaim.parent.getID().equals(claim.getID())))) {
                                if (claim.allowAccess(p) != null) {
                                    plugin.ejectPlayer(p);
                                    p.sendMessage(ChatColor.RED + "✖ El propietario ha activado el campo de fuerza y has sido expulsado de la protección.");
                                }
                            }
                        }
                    } else {
                        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&6🛡 &eCampo de fuerza &aDESACTIVADO&e: La entrada a la protección vuelve a ser pública."));
                    }
                }
                event.getInventory().setItem(slot, ProtectionGui.createFlagItem(plugin, claim, flag));
                return;
            }

            // Slot 46: Fijar Spawn Aquí
            if (slot == 46) {
                if (!canManage) {
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    player.sendMessage(ChatColor.RED + "✖ Solo el propietario o administradores pueden fijar el punto de spawn.");
                    return;
                }
                Claim currentClaim = plugin.dataStore.getClaimAt(player.getLocation(), true, null);
                if (currentClaim == null || !currentClaim.getID().equals(claim.getID())) {
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    player.sendMessage(ChatColor.RED + "✖ Debes estar parado físicamente dentro de esta protección para fijar el spawn aquí.");
                    return;
                }
                claim.setSpawnLocation(player.getLocation());
                plugin.dataStore.saveClaim(claim);
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.5f);
                player.sendMessage(ChatColor.GREEN + "✔ Punto de aparición fijado con éxito en tu posición actual.");
                player.closeInventory();
                return;
            }

            // Slot 47: Renombrar
            if (slot == 47) {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1.0f);
                player.closeInventory();
                player.sendMessage(ChatColor.YELLOW + "Para renombrar esta protección escribe: " + ChatColor.WHITE + "/ps rename <nuevo_nombre>");
                return;
            }

            // Slot 49: Ender Pearl (Teletransportarse exactamente al centro / bloque central)
            if (slot == 49) {
                player.closeInventory();

                boolean canAccess = isOwner || isAdmin || claim.checkPermission(player, ClaimPermission.Access, null) == null;
                if (!canAccess && claim.getClanTrust() != me.ryanhamshire.GriefPrevention.clans.ClanTrustLevel.OFF) {
                    if (UltimateClansHook.isAvailable() && UltimateClansHook.isSameClan(claim.ownerID, player.getUniqueId())) {
                        canAccess = true;
                    }
                }

                if (!canAccess) {
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    player.sendMessage(ChatColor.RED + "✖ No tienes permiso para teletransportarte a esta protección.");
                    return;
                }

                Location target = getSafeCenterTarget(claim);
                if (target == null || target.getWorld() == null) {
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    player.sendMessage(ChatColor.RED + "✖ El mundo donde se encuentra esta protección no está cargado.");
                    return;
                }

                player.teleport(target);
                player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
                player.sendMessage(ChatColor.GREEN + "✦ ¡Teletransportado al centro de la protección!");
                return;
            }

            // Slot 51: Hopper (Abrir Menú de Confirmación de Retiro)
            if (slot == 51) {
                if (!isOwner && !isAdmin) {
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    player.sendMessage(ChatColor.RED + "✖ Solo el propietario (" + claim.getOwnerName() + ") o un administrador pueden retirar esta protección.");
                    return;
                }

                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1.2f);
                ProtectionConfirmGui.open(this.plugin, player, claim);
                return;
            }

            // Slot 53: Cerrar
            if (slot == 53) {
                player.closeInventory();
            }
        }

        // Manejo del Menú de Confirmación de Retiro (Lana Roja y Lana Verde)
        if (title.startsWith(ProtectionConfirmGui.CONFIRM_TITLE_PREFIX)) {
            event.setCancelled(true);

            Long claimId = openGuiClaims.get(player.getUniqueId());
            if (claimId == null) {
                player.closeInventory();
                return;
            }

            Claim claim = plugin.dataStore.getClaim(claimId);
            if (claim == null) {
                player.closeInventory();
                player.sendMessage(ChatColor.RED + "✖ Este terreno ya no existe o fue retirado.");
                return;
            }

            int slot = event.getRawSlot();

            // Slot 11: Cancelar (Lana Roja - Izquierda)
            if (slot == 11) {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1.0f);
                ProtectionGui.open(this.plugin, player, claim);
                return;
            }

            // Slot 15: Confirmar Retiro (Lana Verde - Derecha)
            if (slot == 15) {
                boolean isOwner = player.getUniqueId().equals(claim.getOwnerID());
                boolean isAdmin = player.hasPermission("griefprevention.admin") || player.hasPermission("griefprevention.deleteclaims");

                if (!isOwner && !isAdmin) {
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    player.sendMessage(ChatColor.RED + "✖ Solo el propietario (" + claim.getOwnerName() + ") o un administrador pueden retirar esta protección.");
                    player.closeInventory();
                    return;
                }

                // Identificar el tipo de bloque de protección con fallback a partir del área
                Location center = claim.centerBlockLocation;
                ProtectionBlockType type = null;
                if (center != null && center.getWorld() != null) {
                    type = ProtectionBlockType.fromMaterial(center.getBlock().getType());
                }
                if (type == null) {
                    type = ProtectionBlockType.fromArea(claim.getArea());
                    if (type == null) {
                        type = ProtectionBlockType.IRON;
                    }
                }

                int radius = plugin.protectionConfig.getRadius(type);
                ItemStack protectionItem = type.createItemStack(1, radius);

                player.closeInventory();

                // Eliminar el bloque central del mundo
                if (center != null && center.getWorld() != null) {
                    center.getBlock().setType(Material.AIR);
                }

                // Inmediatamente la claim deja de existir
                plugin.dataStore.deleteClaim(claim);

                // Entregar el bloque de protección al inventario
                java.util.HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(protectionItem);
                if (!leftover.isEmpty()) {
                    for (ItemStack remaining : leftover.values()) {
                        player.getWorld().dropItemNaturally(player.getLocation(), remaining);
                    }
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
                    player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "¡INVENTARIO LLENO! " + ChatColor.YELLOW + "Tu bloque de protección ha caído a tus pies.");
                } else {
                    player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.8f, 1.2f);
                }

                player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1.2f);
                player.sendMessage(ChatColor.GREEN + "✔ Terreno desprotegido con éxito. El bloque de " + plugin.protectionConfig.getDisplayName(type) + ChatColor.GREEN + " ha sido devuelto a tu inventario.");
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryDrag(InventoryDragEvent event) {
        String title = event.getView().getTitle();
        if (title != null && (title.startsWith(ProtectionGui.GUI_TITLE_PREFIX)
                || title.startsWith(ProtectionSelectorGui.SELECTOR_TITLE_PREFIX)
                || title.startsWith(ProtectionConfirmGui.CONFIRM_TITLE_PREFIX))) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        openGuiClaims.remove(uuid);
        ProtectionSelectorGui.playerStates.remove(uuid);
        ProtectionSelectorGui.slotClaimMap.remove(uuid);
    }

    public static Location getSafeCenterTarget(Claim claim) {
        if (claim == null) return null;

        // If physical center block is present, teleport directly on top of it
        if (claim.centerBlockLocation != null) {
            return claim.centerBlockLocation.clone().add(0.5, 1.0, 0.5);
        }

        // Fallback for legacy shovel claims: geometric center at highest safe ground
        Location lesser = claim.getLesserBoundaryCorner();
        Location greater = claim.getGreaterBoundaryCorner();
        if (lesser == null || greater == null || lesser.getWorld() == null) return null;

        World world = lesser.getWorld();
        int cx = (lesser.getBlockX() + greater.getBlockX()) / 2;
        int cz = (lesser.getBlockZ() + greater.getBlockZ()) / 2;
        int y = world.getHighestBlockYAt(cx, cz) + 1;

        return new Location(world, cx + 0.5, y, cz + 0.5);
    }
}
