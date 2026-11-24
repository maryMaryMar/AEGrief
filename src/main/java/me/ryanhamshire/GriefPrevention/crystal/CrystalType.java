package me.ryanhamshire.GriefPrevention.crystal;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import me.ryanhamshire.GriefPrevention.ClaimPermission;

public enum CrystalType {
    FLIGHT("flight", ChatColor.AQUA + "Vuelo", Material.FEATHER, "Permite volar dentro del terreno.", ClaimPermission.Access),
    HASTE("haste", ChatColor.YELLOW + "Prisa", Material.GOLDEN_PICKAXE, "Aumenta la velocidad de minería en el terreno.", ClaimPermission.Access),
    SUSTENANCE("sustenance", ChatColor.GREEN + "Sustento", Material.BREAD, "Alimenta automáticamente a los jugadores en el terreno.", ClaimPermission.Access),
    PHOENIX("phoenix", ChatColor.RED + "Fénix", Material.TOTEM_OF_UNDYING, "Evita tu muerte en el terreno (cooldown 5m).", ClaimPermission.Inventory),
    GUARDIAN("guardian", ChatColor.DARK_PURPLE + "Guardián", Material.SHIELD, "Otorga resistencia contra ataques de entidades hostiles.", ClaimPermission.Access),
    SPEED("speed", ChatColor.BLUE + "Velocidad", Material.SUGAR, "Aumenta la velocidad de movimiento en el terreno.", ClaimPermission.Access);

    private final String id;
    private final String displayName;
    private final Material icon;
    private final String lore;
    private final ClaimPermission requiredPermission;

    CrystalType(String id, String displayName, Material icon, String lore, ClaimPermission requiredPermission) {
        this.id = id;
        this.displayName = displayName;
        this.icon = icon;
        this.lore = lore;
        this.requiredPermission = requiredPermission;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Material getIcon() {
        return icon;
    }

    public String getLore() {
        return lore;
    }

    public String getDescription() {
        return lore;
    }

    public ClaimPermission getRequiredPermission() {
        return requiredPermission;
    }

    public static CrystalType fromId(String id) {
        for (CrystalType type : values()) {
            if (type.id.equalsIgnoreCase(id)) {
                return type;
            }
        }
        return null;
    }
}
