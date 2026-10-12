package me.ryanhamshire.GriefPrevention.protection;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public enum ProtectionBlockType {
    COAL(
            Material.COAL_BLOCK,
            15,
            "&8Bloque de Protección: &7Carbón",
            ChatColor.DARK_GRAY
    ),
    COPPER(
            Material.COPPER_BLOCK,
            30,
            "&6Bloque de Protección: &cCobre",
            ChatColor.GOLD
    ),
    IRON(
            Material.IRON_BLOCK,
            50,
            "&fBloque de Protección: &fHierro",
            ChatColor.WHITE
    ),
    GOLD(
            Material.GOLD_BLOCK,
            100,
            "&eBloque de Protección: &eOro",
            ChatColor.YELLOW
    ),
    DIAMOND(
            Material.DIAMOND_BLOCK,
            150,
            "&bBloque de Protección: &bDiamante",
            ChatColor.AQUA
    ),
    EMERALD(
            Material.EMERALD_BLOCK,
            250,
            "&aBloque de Protección: &aEsmeralda",
            ChatColor.GREEN
    );

    public static final NamespacedKey PDC_KEY = new NamespacedKey("aegrief", "protection_type");

    private final Material material;
    private final int defaultRadius;
    private final String defaultDisplayName;
    private final ChatColor primaryColor;

    ProtectionBlockType(Material material, int defaultRadius, String defaultDisplayName, ChatColor primaryColor) {
        this.material = material;
        this.defaultRadius = defaultRadius;
        this.defaultDisplayName = defaultDisplayName;
        this.primaryColor = primaryColor;
    }

    public Material getMaterial() {
        return material;
    }

    public int getDefaultRadius() {
        return defaultRadius;
    }

    public String getDefaultDisplayName() {
        return defaultDisplayName;
    }

    public ChatColor getPrimaryColor() {
        return primaryColor;
    }

    /**
     * Creates an ItemStack with beautiful and clear descriptive lore.
     *
     * @param amount Quantity of items
     * @param radius Configured or default radius
     * @return Formatted ItemStack
     */
    public ItemStack createItemStack(int amount, int radius) {
        ItemStack item = new ItemStack(this.material, amount);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        // Display Name
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', this.defaultDisplayName));

        // Format dimensions and numbers
        int side = radius * 2;
        long totalArea = (long) side * side;
        NumberFormat nf = NumberFormat.getInstance(Locale.US);

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.translateAlternateColorCodes('&', "&8&m-----------------------------"));
        lore.add(ChatColor.translateAlternateColorCodes('&', "&7Dimensiones: &e" + side + " x " + side + " bloques &8(&7" + radius + " hacia cada lado&8)"));
        lore.add(ChatColor.translateAlternateColorCodes('&', "&7Terreno protegido: &a" + nf.format(totalArea) + " ClaimBlocks"));
        lore.add(ChatColor.translateAlternateColorCodes('&', "&7Rango vertical: &fDesde el fondo hasta el cielo &8(&7Y: -64 a 320&8)"));
        lore.add(ChatColor.translateAlternateColorCodes('&', "&7Seguridad: &6Auto Pick-Up exclusivo para el dueño"));
        lore.add(ChatColor.translateAlternateColorCodes('&', "&8&m-----------------------------"));
        lore.add(ChatColor.translateAlternateColorCodes('&', "&e✦ Colócalo en el suelo para proteger tu terreno."));
        lore.add(ChatColor.translateAlternateColorCodes('&', "&c✦ Solo tú como dueño puedes retirarlo y recuperarlo."));

        meta.setLore(lore);

        // PersistentDataContainer tag
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(PDC_KEY, PersistentDataType.STRING, this.name());

        item.setItemMeta(meta);
        return item;
    }

    public ItemStack createItemStack(int amount) {
        return createItemStack(amount, this.defaultRadius);
    }

    /**
     * Determines if an ItemStack is a Protection Block of this or any type.
     */
    public static ProtectionBlockType fromItemStack(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;

        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (pdc.has(PDC_KEY, PersistentDataType.STRING)) {
            String typeName = pdc.get(PDC_KEY, PersistentDataType.STRING);
            if (typeName != null) {
                try {
                    return ProtectionBlockType.valueOf(typeName.toUpperCase());
                } catch (IllegalArgumentException ignored) {}
            }
        }

        // Fallback: check material and display name / lore keywords
        Material mat = item.getType();
        for (ProtectionBlockType type : values()) {
            if (type.getMaterial() == mat) {
                if (meta.hasDisplayName() && meta.getDisplayName().contains("Protección")) {
                    return type;
                }
            }
        }

        return null;
    }

    public static ProtectionBlockType fromMaterial(Material material) {
        for (ProtectionBlockType type : values()) {
            if (type.getMaterial() == material) {
                return type;
            }
        }
        return null;
    }

    public static ProtectionBlockType fromArea(long area) {
        if (area <= 1200) return COAL;
        if (area <= 5000) return COPPER;
        if (area <= 20000) return IRON;
        if (area <= 60000) return GOLD;
        if (area <= 150000) return DIAMOND;
        return EMERALD;
    }
}
