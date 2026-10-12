package me.ryanhamshire.GriefPrevention.commands;

import me.ryanhamshire.GriefPrevention.clans.UltimateClansHook;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ClanChatCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Comando solo ejecutable por jugadores.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            player.sendMessage(ChatColor.YELLOW + "Uso: /" + label + " <mensaje>");
            return true;
        }

        if (!UltimateClansHook.isAvailable()) {
            player.sendMessage(ChatColor.RED + "El sistema de clanes no está disponible en este momento.");
            return true;
        }

        StringBuilder msg = new StringBuilder();
        for (int i = 0; i < args.length; i++) {
            if (i > 0) msg.append(" ");
            msg.append(args[i]);
        }

        boolean sent = UltimateClansHook.sendClanChat(player, msg.toString());
        if (!sent) {
            player.sendMessage(ChatColor.RED + "No perteneces a ningún clan o no se pudo enviar el mensaje.");
        }
        return true;
    }
}
