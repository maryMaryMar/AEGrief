package me.ryanhamshire.GriefPrevention.gui;

import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.protection.ProtectionBlockType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ProtectionConfirmGui {

    public static final String CONFIRM_TITLE_PREFIX = "✦ ¿Retirar Terreno #";

    public static void open(GriefPrevention plugin, Player player, Claim claim) {
        if (claim == null) return;

        ProtectionGuiListener.openGuiClaims.put(player.getUniqueId(), claim.getID());

        String title = CONFIRM_TITLE_PREFIX + claim.getID() + "?";
        if (title.length() > 32) {
            title = title.substring(0, 32);
        }

        Inventory inv = Bukkit.createInventory(null, 27, title);

        // Fondo estético con cristal gris
        ItemStack filler = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 27; i++) {
            inv.setItem(i, filler);
        }

        // Slot 11: Lana Roja - Cancelar (Izquierda)
        inv.setItem(11, createItem(
                Material.RED_WOOL,
                "&c&l✖ CANCELAR",
                "&7Haz clic aquí para conservar tu protección",
                "&7y regresar al panel de gestión.",
                "",
                "&e▶ Clic para volver"
        ));

        // Slot 13: Información del bloque y terreno
        ProtectionBlockType type = null;
        if (claim.centerBlockLocation != null && claim.centerBlockLocation.getWorld() != null) {
            type = ProtectionBlockType.fromMaterial(claim.centerBlockLocation.getBlock().getType());
        }
        if (type == null) {
            type = ProtectionBlockType.fromArea(claim.getArea());
            if (type == null) {
                type = ProtectionBlockType.IRON;
            }
        }
        int radius = plugin.protectionConfig.getRadius(type);
        String claimName = claim.getCustomName() != null ? claim.getCustomName() : "#" + claim.getID();
        String formattedArea = NumberFormat.getNumberInstance(Locale.US).format(claim.getArea());

        inv.setItem(13, createItem(
                type.getMaterial(),
                "&e&l✦ " + plugin.protectionConfig.getDisplayName(type),
                "&7Terreno: &f" + claimName,
                "&7Dimensiones: &e" + (radius * 2) + "x" + (radius * 2) + " bloques",
                "&7Radio: &e" + radius + "b &7(hacia cada lado)",
                "&7Área protegida: &b" + formattedArea + " ClaimBlocks",
                "",
                "&6&l✦ Retiro Inmediato:",
                "&7Al confirmar, la protección &cdejará de existir",
                "&7y recibirás 1 bloque de " + plugin.protectionConfig.getDisplayName(type),
                "&7directamente en tu inventario."
        ));

        // Slot 15: Lana Verde - Confirmar Retiro (Derecha)
        inv.setItem(15, createItem(
                Material.LIME_WOOL,
                "&a&l✔ CONFIRMAR RETIRO",
                "&7Haz clic para desmantelar este terreno",
                "&7y recibir el bloque en tu inventario.",
                "",
                "&a▶ Clic para desproteger y recoger"
        ));

        player.openInventory(inv);
    }

    private static ItemStack createItem(Material mat, String name, String... loreLines) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
            if (loreLines != null && loreLines.length > 0) {
                List<String> lore = new ArrayList<>();
                for (String line : loreLines) {
                    lore.add(ChatColor.translateAlternateColorCodes('&', line));
                }
                meta.setLore(lore);
            }
            item.setItemMeta(meta);
        }
        return item;
    }
}
