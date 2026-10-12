package me.ryanhamshire.GriefPrevention.clans;

import me.ulrich.clans.Clans;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.UUID;

public class UltimateClansHook {

    private static Boolean available = null;

    public static boolean isAvailable() {
        if (available == null) {
            Plugin plugin = Bukkit.getPluginManager().getPlugin("UltimateClans");
            available = (plugin != null && plugin.isEnabled());
        }
        return available;
    }

    public static void resetCache() {
        available = null;
    }

    public static boolean isSameClan(UUID player1, UUID player2) {
        if (!isAvailable() || player1 == null || player2 == null) {
            return false;
        }

        try {
            Plugin plugin = Bukkit.getPluginManager().getPlugin("UltimateClans");
            if (plugin instanceof Clans) {
                Clans clans = (Clans) plugin;
                return clans.getPlayerAPI().isSameClan(player1, player2);
            }
        } catch (Throwable t) {
            // Silently return false if API call fails
        }
        return false;
    }

    public static boolean sendClanChat(Player player, String message) {
        if (!isAvailable() || player == null || message == null || message.trim().isEmpty()) {
            return false;
        }

        try {
            Plugin plugin = Bukkit.getPluginManager().getPlugin("UltimateClans");
            if (plugin instanceof Clans) {
                Clans clans = (Clans) plugin;
                clans.getClanAPI().clanChatSend(player, message, false);
                return true;
            }
        } catch (Throwable t) {
            // Silently return false
        }
        return false;
    }
}
