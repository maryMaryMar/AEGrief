package me.ryanhamshire.GriefPrevention.commands;

import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.flags.ClaimFlag;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ClaimFlagCommand implements CommandExecutor, TabCompleter {

    private final GriefPrevention plugin;

    public ClaimFlagCommand(GriefPrevention plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Comando solo ejecutable por jugadores.");
            return true;
        }

        Player player = (Player) sender;
        Claim claim = plugin.dataStore.getClaimAt(player.getLocation(), true, null);

        if (claim == null) {
            player.sendMessage(ChatColor.RED + "Debes estar dentro de tu protección para modificar sus flags.");
            return true;
        }

        boolean isOwner = player.getUniqueId().equals(claim.getOwnerID());
        boolean isAdmin = player.hasPermission("griefprevention.admin");

        if (!isOwner && !isAdmin) {
            player.sendMessage(ChatColor.RED + "Solo el dueño de la protección puede configurar sus flags.");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(ChatColor.DARK_GRAY + "========== " + ChatColor.GOLD + "FLAGS DE LA PROTECCIÓN" + ChatColor.DARK_GRAY + " ==========");
            for (ClaimFlag f : ClaimFlag.values()) {
                String val = claim.getFlag(f);
                ChatColor color = "allow".equalsIgnoreCase(val) ? ChatColor.GREEN : ("deny".equalsIgnoreCase(val) ? ChatColor.RED : ChatColor.YELLOW);
                player.sendMessage(ChatColor.YELLOW + f.name().toLowerCase() + ChatColor.DARK_GRAY + ": " + color + val + ChatColor.GRAY + " - " + f.getDescription());
            }
            player.sendMessage(ChatColor.DARK_GRAY + "=============================================");
            player.sendMessage(ChatColor.YELLOW + "Uso: /" + label + " <flag> [allow|deny|texto]");
            return true;
        }

        ClaimFlag flag = ClaimFlag.fromString(args[0]);
        if (flag == null) {
            player.sendMessage(ChatColor.RED + "Flag desconocida: " + args[0] + ". Ejecuta /" + label + " para ver la lista.");
            return true;
        }

        if (plugin.flagConfig != null && !plugin.flagConfig.isEnabled(flag)) {
            player.sendMessage(ChatColor.RED + "✖ La flag '" + flag.name().toLowerCase() + "' ha sido desactivada por la administración del servidor.");
            return true;
        }

        if (args.length == 1) {
            if (flag.getType() == ClaimFlag.FlagType.BOOLEAN) {
                boolean current = claim.isFlagAllowed(flag);
                String newVal = current ? "deny" : "allow";
                claim.setFlag(flag, newVal);
                plugin.dataStore.saveClaim(claim);
                player.sendMessage(ChatColor.GREEN + "Flag " + ChatColor.YELLOW + flag.name().toLowerCase() + ChatColor.GREEN + " establecida a: " + (newVal.equals("allow") ? ChatColor.AQUA + "ALLOW" : ChatColor.RED + "DENY"));
            } else {
                player.sendMessage(ChatColor.GRAY + "Valor actual de " + flag.name().toLowerCase() + ": " + ChatColor.YELLOW + claim.getFlag(flag));
            }
            return true;
        }

        StringBuilder valBuilder = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            if (i > 1) valBuilder.append(" ");
            valBuilder.append(args[i]);
        }
        String val = valBuilder.toString();
        claim.setFlag(flag, val);
        plugin.dataStore.saveClaim(claim);
        player.sendMessage(ChatColor.GREEN + "Flag " + ChatColor.YELLOW + flag.name().toLowerCase() + ChatColor.GREEN + " configurada a: " + ChatColor.WHITE + val);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.stream(ClaimFlag.values())
                    .map(f -> f.name().toLowerCase())
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        if (args.length == 2) {
            List<String> vals = Arrays.asList("allow", "deny");
            return vals.stream().filter(s -> s.startsWith(args[1].toLowerCase())).collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}
