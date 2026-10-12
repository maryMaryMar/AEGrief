package me.ryanhamshire.GriefPrevention.gui;

import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.ClaimPermission;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.clans.ClanTrustLevel;
import me.ryanhamshire.GriefPrevention.clans.UltimateClansHook;
import me.ryanhamshire.GriefPrevention.protection.ProtectionBlockType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

public class ProtectionSelectorGui {

    public static final String SELECTOR_TITLE_PREFIX = "✦ Red de Terrenos: ";

    public enum Category {
        OWNED("&6👑 Mis Protecciones", Material.GOLD_BLOCK, "&7Terrenos que te pertenecen como dueño."),
        TRUSTED("&b🤝 Terrenos de Amigos", Material.WRITABLE_BOOK, "&7Terrenos donde tienes permisos (/trust)."),
        CLAN("&e🛡️ Terrenos de Clan", Material.SHIELD, "&7Bases pertenecientes a los miembros de tu clan.");

        private final String displayName;
        private final Material icon;
        private final String description;

        Category(String displayName, Material icon, String description) {
            this.displayName = displayName;
            this.icon = icon;
            this.description = description;
        }

        public String getDisplayName() {
            return displayName;
        }

        public Material getIcon() {
            return icon;
        }

        public String getDescription() {
            return description;
        }
    }

    public static class CategoryState {
        public Category category;
        public int page;

        public CategoryState(Category category, int page) {
            this.category = category;
            this.page = page;
        }
    }

    public static final Map<UUID, CategoryState> playerStates = new ConcurrentHashMap<>();
    public static final Map<UUID, Map<Integer, Long>> slotClaimMap = new ConcurrentHashMap<>();

    // Grid slots for displaying claims (20 slots per page)
    public static final int[] CLAIM_SLOTS = {
            12, 13, 14, 15, 16,
            21, 22, 23, 24, 25,
            30, 31, 32, 33, 34,
            39, 40, 41, 42, 43
    };

    public static void open(GriefPrevention plugin, Player player) {
        open(plugin, player, Category.OWNED, 0);
    }

    public static void open(GriefPrevention plugin, Player player, Category category, int page) {
        playerStates.put(player.getUniqueId(), new CategoryState(category, page));

        String title = SELECTOR_TITLE_PREFIX + category.name();
        if (title.length() > 32) title = title.substring(0, 32);

        Inventory inv = Bukkit.createInventory(null, 54, title);

        // Fill background borders
        ItemStack border = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        ItemStack divider = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");

        for (int i = 0; i < 9; i++) inv.setItem(i, border);
        for (int i = 45; i < 54; i++) inv.setItem(i, border);
        inv.setItem(9, border);
        inv.setItem(18, border);
        inv.setItem(27, border);
        inv.setItem(36, border);
        inv.setItem(37, border);

        inv.setItem(17, border);
        inv.setItem(26, border);
        inv.setItem(35, border);
        inv.setItem(44, border);

        // Vertical divider column
        inv.setItem(2, divider);
        inv.setItem(11, divider);
        inv.setItem(20, divider);
        inv.setItem(29, divider);
        inv.setItem(38, divider);
        inv.setItem(47, divider);

        // Sidebar Category Tabs (Slots 10, 19, 28)
        inv.setItem(10, createCategoryItem(Category.OWNED, category == Category.OWNED));
        inv.setItem(19, createCategoryItem(Category.TRUSTED, category == Category.TRUSTED));
        inv.setItem(28, createCategoryItem(Category.CLAN, category == Category.CLAN));

        // Get Claims for category
        List<Claim> claims = getClaimsForCategory(plugin, player, category);
        Map<Integer, Long> currentSlotMap = new HashMap<>();

        int perPage = CLAIM_SLOTS.length; // 20
        int totalClaims = claims.size();
        int maxPages = Math.max(1, (int) Math.ceil((double) totalClaims / perPage));
        if (page >= maxPages) page = maxPages - 1;
        if (page < 0) page = 0;

        int startIndex = page * perPage;
        int endIndex = Math.min(startIndex + perPage, totalClaims);

        if (totalClaims == 0) {
            // Empty category message item at center (slot 23)
            inv.setItem(23, createItem(
                    Material.BARRIER,
                    "&cNo se encontraron terrenos",
                    "&7No posees ni perteneces a ninguna",
                    "&7protección dentro de esta categoría."
            ));
        } else {
            int countInPage = endIndex - startIndex;
            int[] targetSlots = getCenteredSlots(countInPage);
            int slotIdx = 0;
            for (int i = startIndex; i < endIndex; i++) {
                Claim c = claims.get(i);
                int targetSlot = targetSlots[slotIdx++];
                inv.setItem(targetSlot, createClaimDisplayItem(c, player));
                currentSlotMap.put(targetSlot, c.getID());
            }
        }
        slotClaimMap.put(player.getUniqueId(), currentSlotMap);

        // Navigation bottom controls
        if (page > 0) {
            inv.setItem(48, createItem(Material.ARROW, "&e◀ Página Anterior", "&7Ir a la página " + page));
        }
        inv.setItem(49, createItem(Material.BARRIER, "&c✖ Cerrar Menú"));
        if (endIndex < totalClaims) {
            inv.setItem(50, createItem(Material.ARROW, "&e▶ Página Siguiente", "&7Ir a la página " + (page + 2)));
        }

        player.openInventory(inv);
    }

    private static ItemStack createCategoryItem(Category cat, boolean isSelected) {
        String prefix = isSelected ? "&a✔ " : "&7";
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.translateAlternateColorCodes('&', cat.getDescription()));
        lore.add(ChatColor.translateAlternateColorCodes('&', "&8&m-----------------------------"));
        if (isSelected) {
            lore.add(ChatColor.translateAlternateColorCodes('&', "&a✦ Categoría actualmente visible"));
        } else {
            lore.add(ChatColor.translateAlternateColorCodes('&', "&e▶ Clic para ver esta categoría"));
        }

        ItemStack item = new ItemStack(cat.getIcon());
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', prefix + cat.getDisplayName()));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack createClaimDisplayItem(Claim claim, Player player) {
        Location loc = claim.centerBlockLocation != null ? claim.centerBlockLocation : claim.getLesserBoundaryCorner();
        Material mat = inferMaterialByArea(claim.getArea());

        if (claim.centerBlockLocation != null && loc.getWorld() != null) {
            int cx = loc.getBlockX() >> 4;
            int cz = loc.getBlockZ() >> 4;
            if (loc.getWorld().isChunkLoaded(cx, cz)) {
                Material currentMat = loc.getBlock().getType();
                mat = toOreMaterial(currentMat, claim.getArea());
            }
        }

        String claimName = claim.getCustomName() != null ? claim.getCustomName() : "Protección #" + claim.getID();
        int width = claim.getGreaterBoundaryCorner().getBlockX() - claim.getLesserBoundaryCorner().getBlockX() + 1;
        int length = claim.getGreaterBoundaryCorner().getBlockZ() - claim.getLesserBoundaryCorner().getBlockZ() + 1;
        NumberFormat nf = NumberFormat.getInstance(Locale.US);
        String worldName = loc.getWorld() != null ? loc.getWorld().getName() : "world";

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.translateAlternateColorCodes('&', "&8&m-----------------------------"));
        lore.add(ChatColor.translateAlternateColorCodes('&', "&7Dueño: &e" + claim.getOwnerName()));
        lore.add(ChatColor.translateAlternateColorCodes('&', "&7Dimensiones: &f" + width + " x " + length + " bloques"));
        lore.add(ChatColor.translateAlternateColorCodes('&', "&7Terreno: &a" + nf.format(claim.getArea()) + " ClaimBlocks"));
        lore.add(ChatColor.translateAlternateColorCodes('&', "&7Ubicación: &f" + worldName + " &8(&7X: " + loc.getBlockX() + ", Z: " + loc.getBlockZ() + "&8)"));
        lore.add(ChatColor.translateAlternateColorCodes('&', "&7Punto Teleport: &bCentro del Bloque"));
        lore.add(ChatColor.translateAlternateColorCodes('&', "&8&m-----------------------------"));
        lore.add(ChatColor.translateAlternateColorCodes('&', "&e▶ Clic para abrir gestión y teletransportarte"));

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&e✦ &6" + claimName));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private static Material toOreMaterial(Material mat, long area) {
        if (mat != null) {
            switch (mat) {
                case COAL_BLOCK: case COAL_ORE: case DEEPSLATE_COAL_ORE: return Material.COAL_ORE;
                case COPPER_BLOCK: case COPPER_ORE: case DEEPSLATE_COPPER_ORE: return Material.COPPER_ORE;
                case IRON_BLOCK: case IRON_ORE: case DEEPSLATE_IRON_ORE: return Material.IRON_ORE;
                case GOLD_BLOCK: case GOLD_ORE: case DEEPSLATE_GOLD_ORE: return Material.GOLD_ORE;
                case DIAMOND_BLOCK: case DIAMOND_ORE: case DEEPSLATE_DIAMOND_ORE: return Material.DIAMOND_ORE;
                case EMERALD_BLOCK: case EMERALD_ORE: case DEEPSLATE_EMERALD_ORE: return Material.EMERALD_ORE;
                default: break;
            }
        }
        return inferMaterialByArea(area);
    }

    private static Material inferMaterialByArea(long area) {
        if (area <= 1200) return Material.COAL_ORE;
        if (area <= 5000) return Material.COPPER_ORE;
        if (area <= 20000) return Material.IRON_ORE;
        if (area <= 60000) return Material.GOLD_ORE;
        if (area <= 150000) return Material.DIAMOND_ORE;
        return Material.EMERALD_ORE;
    }

    public static int[] getCenteredSlots(int count) {
        if (count <= 0) return CLAIM_SLOTS;
        if (count == 1) return new int[]{23};
        if (count == 2) return new int[]{22, 24};
        if (count == 3) return new int[]{22, 23, 24};
        if (count == 4) return new int[]{21, 22, 24, 25};
        if (count == 5) return new int[]{21, 22, 23, 24, 25};
        if (count <= 10) {
            int[] slots = new int[count];
            int[] candidate = {21, 22, 23, 24, 25, 30, 31, 32, 33, 34};
            System.arraycopy(candidate, 0, slots, 0, count);
            return slots;
        }
        return CLAIM_SLOTS;
    }

    public static List<Claim> getClaimsForCategory(GriefPrevention plugin, Player player, Category category) {
        UUID uuid = player.getUniqueId();
        List<Claim> result = new ArrayList<>();

        if (category == Category.OWNED) {
            Vector<Claim> owned = plugin.dataStore.getPlayerData(uuid).getClaims();
            if (owned != null) {
                result.addAll(owned);
            }
            return result;
        }

        if (category == Category.TRUSTED) {
            for (Claim c : plugin.dataStore.getClaims()) {
                if (c.ownerID != null && !c.ownerID.equals(uuid)) {
                    if (c.checkPermission(player, ClaimPermission.Access, null) == null) {
                        result.add(c);
                    }
                }
            }
            return result;
        }

        if (category == Category.CLAN) {
            if (UltimateClansHook.isAvailable()) {
                for (Claim c : plugin.dataStore.getClaims()) {
                    if (c.ownerID != null && !c.ownerID.equals(uuid)) {
                        if (c.getClanTrust() != ClanTrustLevel.OFF && UltimateClansHook.isSameClan(c.ownerID, uuid)) {
                            result.add(c);
                        }
                    }
                }
            }
            return result;
        }

        return result;
    }

    private static ItemStack createItem(Material mat, String name, String... loreLines) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
            if (loreLines.length > 0) {
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
