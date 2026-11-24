package me.ryanhamshire.GriefPrevention.clans;

import me.ulrich.clans.Clans;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.UUID;

public class UltimateClansHook {

    private static Boolean available = null;

    private enum Backend {
        NONE,
        ULTIMATE_CLANS,
        AEGIS_CLANS
    }

    private static Backend backend = null;

    private static Backend resolveBackend() {
        if (backend == null) {
            Plugin uc = Bukkit.getPluginManager().getPlugin("UltimateClans");
            if (uc != null && uc.isEnabled()) {
                backend = Backend.ULTIMATE_CLANS;
                available = true;
                return backend;
            }
            Plugin ac = Bukkit.getPluginManager().getPlugin("AegisClans");
            if (ac != null && ac.isEnabled()) {
                backend = Backend.AEGIS_CLANS;
                available = true;
                return backend;
            }
            backend = Backend.NONE;
            available = false;
        }
        return backend;
    }

    public static boolean isAvailable() {
        resolveBackend();
        return available != null && available;
    }

    public static void resetCache() {
        available = null;
        backend = null;
    }

    public static boolean isSameClan(UUID player1, UUID player2) {
        if (!isAvailable() || player1 == null || player2 == null) {
            return false;
        }

        Backend active = resolveBackend();
        try {
            if (active == Backend.ULTIMATE_CLANS) {
                Plugin plugin = Bukkit.getPluginManager().getPlugin("UltimateClans");
                if (plugin instanceof Clans) {
                    Clans clans = (Clans) plugin;
                    return clans.getPlayerAPI().isSameClan(player1, player2);
                }
            } else if (active == Backend.AEGIS_CLANS) {
                Plugin ac = Bukkit.getPluginManager().getPlugin("AegisClans");
                if (ac != null) {
                    ClassLoader cl = ac.getClass().getClassLoader();
                    Class<?> mgrClass = Class.forName("com.aegisclans.clans.manager.AegisClanManager", false, cl);
                    Object mgr = mgrClass.getMethod("getInstance").invoke(null);
                    if (mgr != null) {
                        Object res = mgrClass.getMethod("inSameClan", UUID.class, UUID.class).invoke(mgr, player1, player2);
                        return res instanceof Boolean && (Boolean) res;
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    public static boolean sendClanChat(Player player, String message) {
        if (!isAvailable() || player == null || message == null || message.trim().isEmpty()) {
            return false;
        }

        Backend active = resolveBackend();
        try {
            if (active == Backend.ULTIMATE_CLANS) {
                Plugin plugin = Bukkit.getPluginManager().getPlugin("UltimateClans");
                if (plugin instanceof Clans) {
                    Clans clans = (Clans) plugin;
                    clans.getClanAPI().clanChatSend(player, message, false);
                    return true;
                }
            } else if (active == Backend.AEGIS_CLANS) {
                Plugin ac = Bukkit.getPluginManager().getPlugin("AegisClans");
                if (ac != null) {
                    ClassLoader cl = ac.getClass().getClassLoader();
                    Class<?> mgrClass = Class.forName("com.aegisclans.clans.manager.AegisClanManager", false, cl);
                    Object mgr = mgrClass.getMethod("getInstance").invoke(null);
                    if (mgr != null) {
                        Object opt = mgrClass.getMethod("getClanByPlayer", UUID.class).invoke(mgr, player.getUniqueId());
                        if (opt instanceof Optional<?> optional && optional.isPresent()) {
                            Object clanData = optional.get();
                            Class<?> clanDataClass = Class.forName("me.ulrich.clans.data.ClanData", false, cl);
                            Method broadcast = mgrClass.getMethod("broadcastClan", clanDataClass, String.class);
                            String formatted = ChatColor.translateAlternateColorCodes('&',
                                    "&8[&6Clan&8] &e" + player.getName() + "&7: &f") + message;
                            broadcast.invoke(mgr, clanData, formatted);
                            return true;
                        }
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return false;
    }
}
