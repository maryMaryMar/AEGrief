package me.ryanhamshire.GriefPrevention.gui;

import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.flags.ClaimFlag;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ProtectionGui {

    public static final String GUI_TITLE_PREFIX = "✦ Gestión: ";

    // Slots for the boolean flags across rows 1-4
    private static final int[] FLAG_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };

    private static final ClaimFlag[] DISPLAY_FLAGS = {
            ClaimFlag.ENTRY,
            ClaimFlag.PVP,
            ClaimFlag.FLY,
            ClaimFlag.EXPLOSIONS,
            ClaimFlag.MOB_SPAWNING,
            ClaimFlag.PASSIVE_SPAWNING,
            ClaimFlag.FIRE_SPREAD,
            ClaimFlag.LAVA_FLOW,
            ClaimFlag.WATER_FLOW,
            ClaimFlag.LEAF_DECAY,
            ClaimFlag.PISTONS,
            ClaimFlag.ITEM_DROP,
            ClaimFlag.ITEM_PICKUP,
            ClaimFlag.MONSTER_DAMAGE,
            ClaimFlag.LIGHTNING,
            ClaimFlag.WITHER_DAMAGE,
            ClaimFlag.CHORUS_FRUIT,
            ClaimFlag.ENDER_PEARL,
            ClaimFlag.SLEEP,
            ClaimFlag.ICE_MELT,
            ClaimFlag.SNOW_FALL,
            ClaimFlag.VILLAGER_TRADING
    };

    public static void open(GriefPrevention plugin, Player player, Claim claim) {
        if (claim == null) return;

        ProtectionGuiListener.openGuiClaims.put(player.getUniqueId(), claim.getID());

        String name = claim.getCustomName() != null ? claim.getCustomName() : "#" + claim.getID();
        String title = GUI_TITLE_PREFIX + name;
        if (title.length() > 32) {
            title = title.substring(0, 32);
        }

        Inventory inv = Bukkit.createInventory(null, 54, title);

        // Fill background borders
        ItemStack filler = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 9; i++) inv.setItem(i, filler);
        inv.setItem(9, filler);
        inv.setItem(17, filler);
        inv.setItem(18, filler);
        inv.setItem(26, filler);
        inv.setItem(27, filler);
        inv.setItem(35, filler);
        inv.setItem(36, filler);
        inv.setItem(44, filler);
        inv.setItem(50, filler);
        inv.setItem(52, filler);

        // Place Flags
        int slotIdx = 0;
        for (int i = 0; i < DISPLAY_FLAGS.length && slotIdx < FLAG_SLOTS.length; i++) {
            ClaimFlag flag = DISPLAY_FLAGS[i];
            if (plugin != null && plugin.flagConfig != null && !plugin.flagConfig.isEnabled(flag)) {
                continue;
            }
            int slot = FLAG_SLOTS[slotIdx++];
            inv.setItem(slot, createFlagItem(plugin, claim, flag));
        }

        // Fill remaining unused flag slots
        while (slotIdx < FLAG_SLOTS.length) {
            inv.setItem(FLAG_SLOTS[slotIdx++], filler);
        }

        // Bottom Row Items:
        // Slot 46: Fijar Spawn
        inv.setItem(46, createItem(
                Material.COMPASS,
                "&e✦ Fijar Spawn Aquí",
                "&7Establece tu posición actual",
                "&7como el punto de aparición (/ps setspawn).",
                "",
                "&e▶ Clic para fijar spawn"
        ));

        // Slot 47: Renombrar
        inv.setItem(47, createItem(
                Material.OAK_SIGN,
                "&e✦ Renombrar Protección",
                "&7Nombre actual: &f" + (claim.getCustomName() != null ? claim.getCustomName() : "&7(Sin nombre)"),
                "",
                "&7Escribe &f/ps rename <nombre> &7en el chat",
                "&7para asignar un nombre personalizado."
        ));

        // Slot 45: Volver a la Lista de Protecciones
        inv.setItem(45, createItem(
                Material.ARROW,
                "&e◀ Volver al Selector de Terrenos",
                "&7Haz clic para regresar al menú",
                "&7principal de categorías (/ps)."
        ));

        // Slot 48: Cabeza del Dueño
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta skullMeta = (SkullMeta) skull.getItemMeta();
        if (skullMeta != null) {
            String ownerName = claim.getOwnerName();
            if (claim.ownerID != null) {
                skullMeta.setOwningPlayer(Bukkit.getOfflinePlayer(claim.ownerID));
            }
            skullMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&ePropietario: &a" + ownerName));

            int width = claim.getGreaterBoundaryCorner().getBlockX() - claim.getLesserBoundaryCorner().getBlockX() + 1;
            int length = claim.getGreaterBoundaryCorner().getBlockZ() - claim.getLesserBoundaryCorner().getBlockZ() + 1;
            NumberFormat nf = NumberFormat.getInstance(Locale.US);

            List<String> skullLore = new ArrayList<>();
            skullLore.add(ChatColor.translateAlternateColorCodes('&', "&8&m-----------------------------"));
            skullLore.add(ChatColor.translateAlternateColorCodes('&', "&7ID de Protección: &e#" + claim.getID()));
            skullLore.add(ChatColor.translateAlternateColorCodes('&', "&7Nombre: &f" + (claim.getCustomName() != null ? claim.getCustomName() : "&7(Sin nombre)")));
            skullLore.add(ChatColor.translateAlternateColorCodes('&', "&7Dimensiones: &f" + width + " x " + length + " bloques"));
            skullLore.add(ChatColor.translateAlternateColorCodes('&', "&7Terreno: &a" + nf.format(claim.getArea()) + " ClaimBlocks"));
            skullLore.add(ChatColor.translateAlternateColorCodes('&', "&7Confianza Clan: &b" + claim.getClanTrust().name()));
            skullLore.add(ChatColor.translateAlternateColorCodes('&', "&7Punto Teleport: &bCentro del Bloque"));
            skullLore.add(ChatColor.translateAlternateColorCodes('&', "&8&m-----------------------------"));
            skullMeta.setLore(skullLore);
            skull.setItemMeta(skullMeta);
        }
        inv.setItem(48, skull);

        // Slot 49: Ender Pearl (Teletransportarse al Bloque Central)
        inv.setItem(49, createItem(
                Material.ENDER_PEARL,
                "&b✦ Teletransportarse al Centro",
                "&7Haz clic aquí para viajar de forma 100% segura",
                "&7directamente encima del bloque central de esta protección.",
                "",
                "&8&m-----------------------------",
                "&e▶ Clic para teletransportarte al bloque central"
        ));

        // Slot 51: Retirar Protección (Abre menú de confirmación)
        inv.setItem(51, createItem(
                Material.HOPPER,
                "&c&l✦ Retirar Protección AEGrief",
                "&7Desmantela y elimina de forma definitiva",
                "&7este terreno protegido.",
                "&7El bloque de protección volverá a tu",
                "&7inventario de forma 100% segura.",
                "",
                "&c⚠ Requiere al menos 1 slot libre en inventario.",
                "&8&m-----------------------------",
                "&e▶ Clic para abrir el menú de confirmación"
        ));

        // Slot 53: Cerrar
        inv.setItem(53, createItem(
                Material.BARRIER,
                "&c✖ Cerrar Menú"
        ));

        player.openInventory(inv);
    }

    public static ItemStack createFlagItem(GriefPrevention plugin, Claim claim, ClaimFlag flag) {
        Material mat = getMaterialForFlag(flag);
        String currentVal = claim.getFlag(flag);
        boolean allowed = "allow".equalsIgnoreCase(currentVal);

        String statusColor = allowed ? "&a" : "&c";
        String statusText = allowed ? "✔ PERMITIDO" : "✖ DENEGADO";

        String desc = (plugin != null && plugin.flagConfig != null) ? plugin.flagConfig.getDescription(flag) : flag.getDescription();

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.translateAlternateColorCodes('&', "&7" + desc));
        lore.add(ChatColor.translateAlternateColorCodes('&', "&8&m-----------------------------"));
        lore.add(ChatColor.translateAlternateColorCodes('&', "&7Estado actual: " + statusColor + statusText));
        lore.add(ChatColor.translateAlternateColorCodes('&', "&8&m-----------------------------"));
        lore.add(ChatColor.translateAlternateColorCodes('&', "&e▶ Clic para alternar estado"));

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&e✦ &6" + flag.name()));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static Material getMaterialForFlag(ClaimFlag flag) {
        switch (flag) {
            case ENTRY: return Material.IRON_DOOR;
            case PVP: return Material.DIAMOND_SWORD;
            case FLY: return Material.FEATHER;
            case EXPLOSIONS: return Material.TNT;
            case MOB_SPAWNING: return Material.ZOMBIE_HEAD;
            case PASSIVE_SPAWNING: return Material.WHEAT;
            case FIRE_SPREAD: return Material.FLINT_AND_STEEL;
            case LAVA_FLOW: return Material.LAVA_BUCKET;
            case WATER_FLOW: return Material.WATER_BUCKET;
            case LEAF_DECAY: return Material.OAK_LEAVES;
            case PISTONS: return Material.PISTON;
            case ITEM_DROP: return Material.DROPPER;
            case ITEM_PICKUP: return Material.HOPPER;
            case MONSTER_DAMAGE: return Material.SHIELD;
            case LIGHTNING: return Material.LIGHTNING_ROD;
            case WITHER_DAMAGE: return Material.WITHER_SKELETON_SKULL;
            case CHORUS_FRUIT: return Material.CHORUS_FRUIT;
            case ENDER_PEARL: return Material.ENDER_EYE;
            case SLEEP: return Material.RED_BED;
            case ICE_MELT: return Material.ICE;
            case SNOW_FALL: return Material.SNOWBALL;
            case VILLAGER_TRADING: return Material.EMERALD;
            default: return Material.PAPER;
        }
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

    public static ClaimFlag getFlagBySlot(int slot) {
        for (int i = 0; i < FLAG_SLOTS.length; i++) {
            if (FLAG_SLOTS[i] == slot && i < DISPLAY_FLAGS.length) {
                return DISPLAY_FLAGS[i];
            }
        }
        return null;
    }
}
