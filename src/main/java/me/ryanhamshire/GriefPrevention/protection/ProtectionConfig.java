package me.ryanhamshire.GriefPrevention.protection;

import me.ryanhamshire.GriefPrevention.GriefPrevention;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;

public class ProtectionConfig {

    private final GriefPrevention plugin;
    private final File configFile;
    private FileConfiguration config;

    private final Map<ProtectionBlockType, Integer> blockRadii = new EnumMap<>(ProtectionBlockType.class);
    private final Map<ProtectionBlockType, Boolean> blockEnabled = new EnumMap<>(ProtectionBlockType.class);
    private final Map<ProtectionBlockType, String> blockDisplayNames = new EnumMap<>(ProtectionBlockType.class);

    public ProtectionConfig(GriefPrevention plugin) {
        this.plugin = plugin;
        this.configFile = new File("plugins" + File.separator + "GriefPreventionData" + File.separator + "protection_blocks.yml");
        loadConfig();
    }

    public synchronized void loadConfig() {
        if (!configFile.exists()) {
            try {
                configFile.getParentFile().mkdirs();
                configFile.createNewFile();
            } catch (IOException e) {
                GriefPrevention.AddLogEntry("Could not create protection_blocks.yml: " + e.getMessage());
            }
        }

        config = YamlConfiguration.loadConfiguration(configFile);
        boolean saveNeeded = false;

        // Header comments
        config.options().header(
                "====================================================\n" +
                "  AEGrief - Configuración de Bloques de Protección  \n" +
                "====================================================\n" +
                "NOTA IMPORTANTE: Al modificar los radios en este archivo,\n" +
                "los cambios se aplicarán ÚNICAMENTE a las nuevas protecciones\n" +
                "colocadas a partir de la recarga. Las protecciones existentes\n" +
                "conservarán sus dimensiones originales de forma 100% segura.\n"
        );

        for (ProtectionBlockType type : ProtectionBlockType.values()) {
            String path = "blocks." + type.name().toLowerCase();

            if (!config.contains(path + ".enabled")) {
                config.set(path + ".enabled", true);
                saveNeeded = true;
            }
            if (!config.contains(path + ".radius")) {
                config.set(path + ".radius", type.getDefaultRadius());
                saveNeeded = true;
            }
            if (!config.contains(path + ".name")) {
                config.set(path + ".name", type.getDefaultDisplayName());
                saveNeeded = true;
            }

            boolean enabled = config.getBoolean(path + ".enabled", true);
            int radius = config.getInt(path + ".radius", type.getDefaultRadius());
            String name = config.getString(path + ".name", type.getDefaultDisplayName());

            blockEnabled.put(type, enabled);
            blockRadii.put(type, radius);
            blockDisplayNames.put(type, ChatColor.translateAlternateColorCodes('&', name));
        }

        if (saveNeeded) {
            try {
                config.save(configFile);
            } catch (IOException e) {
                GriefPrevention.AddLogEntry("Could not save defaults to protection_blocks.yml: " + e.getMessage());
            }
        }
    }

    public int getRadius(ProtectionBlockType type) {
        return blockRadii.getOrDefault(type, type.getDefaultRadius());
    }

    public boolean isEnabled(ProtectionBlockType type) {
        return blockEnabled.getOrDefault(type, true);
    }

    public String getDisplayName(ProtectionBlockType type) {
        return blockDisplayNames.getOrDefault(type, type.getDefaultDisplayName());
    }
}
