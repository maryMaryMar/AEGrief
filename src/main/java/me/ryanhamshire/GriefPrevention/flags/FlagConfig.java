package me.ryanhamshire.GriefPrevention.flags;

import me.ryanhamshire.GriefPrevention.GriefPrevention;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;

public class FlagConfig {

    private final GriefPrevention plugin;
    private final File configFile;
    private FileConfiguration config;

    private final Map<ClaimFlag, Boolean> flagEnabled = new EnumMap<>(ClaimFlag.class);
    private final Map<ClaimFlag, String> flagDefaults = new EnumMap<>(ClaimFlag.class);
    private final Map<ClaimFlag, String> flagDescriptions = new EnumMap<>(ClaimFlag.class);

    public FlagConfig(GriefPrevention plugin) {
        this.plugin = plugin;
        this.configFile = new File("plugins" + File.separator + "GriefPreventionData" + File.separator + "claim_flags.yml");
        loadConfig();
    }

    public synchronized void loadConfig() {
        if (!configFile.exists()) {
            try {
                configFile.getParentFile().mkdirs();
                configFile.createNewFile();
            } catch (IOException e) {
                GriefPrevention.AddLogEntry("Could not create claim_flags.yml: " + e.getMessage());
            }
        }

        config = YamlConfiguration.loadConfiguration(configFile);
        boolean saveNeeded = false;

        config.options().header(
                "====================================================\n" +
                "       AEGrief - Configuración de Claim Flags       \n" +
                "====================================================\n" +
                "En este archivo puedes activar o desactivar cada flag\n" +
                "en todo el servidor, cambiar su valor por defecto al\n" +
                "crear terrenos (allow / deny) y personalizar sus textos.\n" +
                "Usa '/ps reload' en el servidor para aplicar cambios.\n"
        );

        for (ClaimFlag flag : ClaimFlag.values()) {
            String path = "flags." + flag.name().toLowerCase();

            if (!config.contains(path + ".enabled")) {
                config.set(path + ".enabled", true);
                saveNeeded = true;
            }
            if (!config.contains(path + ".default")) {
                config.set(path + ".default", flag.getDefaultValue());
                saveNeeded = true;
            }
            if (!config.contains(path + ".description")) {
                config.set(path + ".description", flag.getDescription());
                saveNeeded = true;
            }

            boolean enabled = config.getBoolean(path + ".enabled", true);
            String defVal = config.getString(path + ".default", flag.getDefaultValue());
            String desc = config.getString(path + ".description", flag.getDescription());

            flagEnabled.put(flag, enabled);
            flagDefaults.put(flag, defVal);
            flagDescriptions.put(flag, ChatColor.translateAlternateColorCodes('&', desc));
        }

        if (saveNeeded) {
            try {
                config.save(configFile);
            } catch (IOException e) {
                GriefPrevention.AddLogEntry("Could not save defaults to claim_flags.yml: " + e.getMessage());
            }
        }
    }

    public boolean isEnabled(ClaimFlag flag) {
        if (flag == null) return false;
        return flagEnabled.getOrDefault(flag, true);
    }

    public String getDefaultValue(ClaimFlag flag) {
        if (flag == null) return "deny";
        return flagDefaults.getOrDefault(flag, flag.getDefaultValue());
    }

    public String getDescription(ClaimFlag flag) {
        if (flag == null) return "";
        return flagDescriptions.getOrDefault(flag, flag.getDescription());
    }
}
