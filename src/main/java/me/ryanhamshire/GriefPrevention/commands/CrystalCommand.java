package me.ryanhamshire.GriefPrevention.commands;

import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.crystal.CrystalGUI;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CrystalCommand implements CommandExecutor, TabCompleter {

    private final GriefPrevention plugin;

    public CrystalCommand(GriefPrevention plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Este comando solo puede ser ejecutado por un jugador.");
            return true;
        }

        Player player = (Player) sender;
        
        // Obtener el claim en la ubicación actual del jugador
        Claim claim = GriefPrevention.instance.dataStore.getClaimAt(player.getLocation(), true, null);

        if (claim == null) {
            player.sendMessage(ChatColor.RED + "Debes estar parado dentro de un claim para gestionar los cristales.");
            return true;
        }

        // Verificar si el jugador es el dueño o tiene permisos de administración en el claim
        boolean isOwner = claim.getOwnerID() != null && claim.getOwnerID().equals(player.getUniqueId());
        boolean hasAdminPermission = player.hasPermission("griefprevention.admin") || claim.allowEdit(player) == null;

        if (!isOwner && !hasAdminPermission) {
            player.sendMessage(ChatColor.RED + "Solo el dueño del claim o un administrador puede gestionar los cristales.");
            return true;
        }

        // Abrir la interfaz gráfica de cristales
        try {
            new CrystalGUI(claim, player).open();
        } catch (NoClassDefFoundError | Exception e) {
            player.sendMessage(ChatColor.RED + "Error al abrir la interfaz de cristales.");
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            String input = args[0].toLowerCase();
            
            if ("menu".startsWith(input)) {
                completions.add("menu");
            }
            if ("help".startsWith(input)) {
                completions.add("help");
            }
            
            return completions;
        }
        return Collections.emptyList();
    }
}
