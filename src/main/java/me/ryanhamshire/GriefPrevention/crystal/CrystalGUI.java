package me.ryanhamshire.GriefPrevention.crystal;

import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.flags.ClaimFlagListener;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class CrystalGUI implements InventoryHolder {

    private static final Enchantment GLOW_ENCHANT = Enchantment.getByKey(NamespacedKey.minecraft("unbreaking"));

    private final Claim claim;
    private final Player viewer;
    private final Inventory inventory;

    public CrystalGUI(Claim claim, Player viewer) {
        this.claim = claim.parent != null ? claim.parent : claim;
        this.viewer = viewer;
        this.inventory = Bukkit.createInventory(this, 27, ChatColor.translateAlternateColorCodes('&', "&8&lGemas y Cristales"));
        refreshItems();
    }

    public void open() {
        this.viewer.openInventory(this.inventory);
    }

    @Override
    public Inventory getInventory() {
        return this.inventory;
    }

    public void refreshItems() {
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        if (fillerMeta != null) {
            fillerMeta.setDisplayName(" ");
            filler.setItemMeta(fillerMeta);
        }

        for (int i = 0; i < 27; i++) {
            this.inventory.setItem(i, filler);
        }

        setupCrystalItem(11, CrystalType.FLIGHT, Material.FEATHER);
        setupCrystalItem(12, CrystalType.HASTE, Material.GOLDEN_PICKAXE);
        setupCrystalItem(13, CrystalType.SUSTENANCE, Material.GOLDEN_APPLE);
        setupCrystalItem(14, CrystalType.PHOENIX, Material.TOTEM_OF_UNDYING);
        setupCrystalItem(15, CrystalType.GUARDIAN, Material.SHIELD);
        setupCrystalItem(16, CrystalType.SPEED, Material.SUGAR);
    }

    private void setupCrystalItem(int slot, CrystalType type, Material material) {
        ItemStack item = new ItemStack(material);
        Long rootClaimId = ClaimFlagListener.getRootClaimId(claim);
        boolean active = GriefPrevention.instance.crystalManager != null && GriefPrevention.instance.crystalManager.hasCrystal(rootClaimId, type);
        boolean hasCrystalPerm = viewer != null && (viewer.hasPermission("aegrief.crystal." + type.getId())
                || viewer.hasPermission("aegrief.admin")
                || viewer.hasPermission("griefprevention.admin"));
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(type.getDisplayName());
            List<String> lore = new ArrayList<>();
            lore.add(type.getDescription());
            lore.add("");
            if (active) {
                lore.add(ChatColor.GREEN + "✔ Activo (Click para desactivar)");
                if (GLOW_ENCHANT != null) {
                    meta.addEnchant(GLOW_ENCHANT, 1, true);
                    meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                }
            } else if (!hasCrystalPerm) {
                lore.add(ChatColor.RED + "✖ Bloqueado (Requiere rango / permiso: aegrief.crystal." + type.getId() + ")");
            } else {
                lore.add(ChatColor.RED + "✘ Inactivo (Click para activar)");
            }
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        this.inventory.setItem(slot, item);
    }

    public static class CrystalListener implements Listener {
        @EventHandler
        public void onInventoryClick(InventoryClickEvent event) {
            if (event.getInventory().getHolder() instanceof CrystalGUI) {
                event.setCancelled(true);
                CrystalGUI gui = (CrystalGUI) event.getInventory().getHolder();
                int slot = event.getRawSlot();
                if (slot < 0 || slot >= 27) return;

                CrystalType clicked = null;
                if (slot == 11) clicked = CrystalType.FLIGHT;
                else if (slot == 12) clicked = CrystalType.HASTE;
                else if (slot == 13) clicked = CrystalType.SUSTENANCE;
                else if (slot == 14) clicked = CrystalType.PHOENIX;
                else if (slot == 15) clicked = CrystalType.GUARDIAN;
                else if (slot == 16) clicked = CrystalType.SPEED;

                if (clicked != null && GriefPrevention.instance.crystalManager != null && event.getWhoClicked() instanceof Player) {
                    Player p = (Player) event.getWhoClicked();
                    boolean isOwner = gui.claim.getOwnerID() != null && gui.claim.getOwnerID().equals(p.getUniqueId());
                    boolean isAdmin = p.hasPermission("griefprevention.admin") || p.hasPermission("aegrief.admin");
                    if (!isOwner && !isAdmin && gui.claim.allowEdit(p) != null) {
                        p.sendMessage(ChatColor.RED + "Solo el dueño del claim o un administrador puede gestionar los cristales.");
                        p.closeInventory();
                        return;
                    }

                    Long rootClaimId = ClaimFlagListener.getRootClaimId(gui.claim);
                    if (rootClaimId == null) return;

                    boolean currentlyActive = GriefPrevention.instance.crystalManager.hasCrystal(rootClaimId, clicked);
                    if (!currentlyActive && !isAdmin && !p.hasPermission("aegrief.crystal." + clicked.getId())) {
                        p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                        p.sendMessage(ChatColor.RED + "✖ No tienes permiso para activar este cristal (requiere " + ChatColor.YELLOW + "aegrief.crystal." + clicked.getId() + ChatColor.RED + ").");
                        return;
                    }

                    GriefPrevention.instance.crystalManager.toggleCrystal(rootClaimId, clicked);
                    if (clicked == CrystalType.FLIGHT) {
                        for (Player online : Bukkit.getOnlinePlayers()) {
                            ClaimFlagListener.enforceFlightAtLocation(online);
                        }
                    }
                    gui.refreshItems();
                    p.playSound(p.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 1f, 1f);
                }
            }
        }

        @EventHandler
        public void onInventoryDrag(InventoryDragEvent event) {
            if (event.getInventory().getHolder() instanceof CrystalGUI) {
                event.setCancelled(true);
            }
        }
    }
}
