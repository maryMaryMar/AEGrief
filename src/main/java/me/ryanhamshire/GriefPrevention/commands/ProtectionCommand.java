package me.ryanhamshire.GriefPrevention.commands;

import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.ClaimPermission;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.Visualization;
import me.ryanhamshire.GriefPrevention.VisualizationType;
import me.ryanhamshire.GriefPrevention.clans.UltimateClansHook;
import me.ryanhamshire.GriefPrevention.flags.ClaimFlag;
import me.ryanhamshire.GriefPrevention.gui.ProtectionGui;
import me.ryanhamshire.GriefPrevention.gui.ProtectionGuiListener;
import me.ryanhamshire.GriefPrevention.gui.ProtectionSelectorGui;
import me.ryanhamshire.GriefPrevention.protection.ProtectionBlockType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Vector;
import java.util.stream.Collectors;

public class ProtectionCommand implements CommandExecutor, TabCompleter {

    private final GriefPrevention plugin;

    public ProtectionCommand(GriefPrevention plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("menu") || args[0].equalsIgnoreCase("gui")) {
            if (!(sender instanceof Player)) {
                sendHelp(sender);
                return true;
            }
            Player player = (Player) sender;
            ProtectionSelectorGui.open(plugin, player);
            return true;
        }

        String sub = args[0].toLowerCase();

        if (sub.equals("help")) {
            sendHelp(sender);
            return true;
        }

        // /ps spawn [nombre|id] or /ps tp [nombre|id]
        if (sub.equals("spawn") || sub.equals("tp")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Comando solo ejecutable por jugadores.");
                return true;
            }
            Player player = (Player) sender;
            Claim targetClaim = null;

            if (args.length >= 2) {
                String query = args[1].trim();
                Vector<Claim> claims = plugin.dataStore.getPlayerData(player.getUniqueId()).getClaims();
                for (Claim c : claims) {
                    if (c.getCustomName() != null && c.getCustomName().equalsIgnoreCase(query)) {
                        targetClaim = c;
                        break;
                    }
                }
                if (targetClaim == null) {
                    try {
                        long cid = Long.parseLong(query);
                        targetClaim = plugin.dataStore.getClaim(cid);
                    } catch (NumberFormatException ignored) {}
                }
            } else {
                targetClaim = plugin.dataStore.getClaimAt(player.getLocation(), true, null);
                if (targetClaim == null) {
                    Vector<Claim> claims = plugin.dataStore.getPlayerData(player.getUniqueId()).getClaims();
                    if (!claims.isEmpty()) {
                        targetClaim = claims.get(0);
                    }
                }
            }

            if (targetClaim == null) {
                player.sendMessage(ChatColor.RED + "Protección no encontrada. Usa " + ChatColor.YELLOW + "/ps list" + ChatColor.RED + " para ver tus propiedades.");
                return true;
            }

            boolean isOwner = player.getUniqueId().equals(targetClaim.ownerID);
            boolean isAdmin = player.hasPermission("griefprevention.admin");
            boolean canAccess = isOwner || isAdmin || targetClaim.checkPermission(player, ClaimPermission.Access, null) == null;
            if (!canAccess && targetClaim.getClanTrust() != me.ryanhamshire.GriefPrevention.clans.ClanTrustLevel.OFF) {
                if (UltimateClansHook.isAvailable() && UltimateClansHook.isSameClan(targetClaim.ownerID, player.getUniqueId())) {
                    canAccess = true;
                }
            }

            if (!canAccess) {
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                player.sendMessage(ChatColor.RED + "✖ No tienes permiso para teletransportarte a esta protección.");
                return true;
            }

            Location target = ProtectionGuiListener.getSafeCenterTarget(targetClaim);
            if (target == null || target.getWorld() == null) {
                player.sendMessage(ChatColor.RED + "El mundo donde se encuentra esta protección no está cargado.");
                return true;
            }

            player.teleport(target);
            player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
            player.sendMessage(ChatColor.GREEN + "✦ ¡Teletransportado al centro de la protección " + ChatColor.YELLOW + (targetClaim.getCustomName() != null ? targetClaim.getCustomName() : "#" + targetClaim.getID()) + ChatColor.GREEN + "!");
            return true;
        }

        // /ps setspawn [nombre]
        if (sub.equals("setspawn")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Comando solo ejecutable por jugadores.");
                return true;
            }
            Player player = (Player) sender;
            Claim claim = plugin.dataStore.getClaimAt(player.getLocation(), true, null);
            if (claim == null) {
                player.sendMessage(ChatColor.RED + "Debes estar parado físicamente dentro de tu protección para fijar el spawn.");
                return true;
            }

            boolean isOwner = player.getUniqueId().equals(claim.ownerID);
            boolean isAdmin = player.hasPermission("griefprevention.admin");
            boolean canManage = isOwner || isAdmin || claim.checkPermission(player, ClaimPermission.Manage, null) == null;

            if (!canManage) {
                player.sendMessage(ChatColor.RED + "Solo el dueño o administradores pueden fijar el punto de spawn.");
                return true;
            }

            claim.setSpawnLocation(player.getLocation());
            if (args.length >= 2) {
                claim.setCustomName(args[1]);
            }
            plugin.dataStore.saveClaim(claim);

            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.5f);
            player.sendMessage(ChatColor.GREEN + "✔ ¡Punto de spawn fijado exitosamente en tu posición!");
            if (claim.getCustomName() != null) {
                player.sendMessage(ChatColor.GRAY + "Nombre asignado: " + ChatColor.YELLOW + claim.getCustomName());
            }
            return true;
        }

        // /ps rename <nombre>
        if (sub.equals("rename")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Comando solo ejecutable por jugadores.");
                return true;
            }
            if (args.length < 2) {
                sender.sendMessage(ChatColor.YELLOW + "Uso: /ps rename <nuevo_nombre>");
                return true;
            }
            Player player = (Player) sender;
            Claim claim = plugin.dataStore.getClaimAt(player.getLocation(), true, null);
            if (claim == null) {
                player.sendMessage(ChatColor.RED + "Debes estar dentro de la protección que deseas renombrar.");
                return true;
            }

            boolean isOwner = player.getUniqueId().equals(claim.ownerID);
            boolean isAdmin = player.hasPermission("griefprevention.admin");
            if (!isOwner && !isAdmin) {
                player.sendMessage(ChatColor.RED + "Solo el propietario puede renombrar esta protección.");
                return true;
            }

            String newName = args[1].trim();
            claim.setCustomName(newName);
            plugin.dataStore.saveClaim(claim);

            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.4f);
            player.sendMessage(ChatColor.GREEN + "✔ Esta protección ahora se llama: " + ChatColor.YELLOW + newName);
            return true;
        }

        // /ps list
        if (sub.equals("list")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Comando solo ejecutable por jugadores.");
                return true;
            }
            Player player = (Player) sender;
            Vector<Claim> claims = plugin.dataStore.getPlayerData(player.getUniqueId()).getClaims();
            if (claims.isEmpty()) {
                player.sendMessage(ChatColor.RED + "No tienes ninguna protección registrada.");
                return true;
            }

            player.sendMessage(ChatColor.DARK_GRAY + "========== " + ChatColor.GOLD + "MIS PROTECCIONES (" + claims.size() + ")" + ChatColor.DARK_GRAY + " ==========");
            NumberFormat nf = NumberFormat.getInstance(Locale.US);
            for (Claim c : claims) {
                String cName = c.getCustomName() != null ? c.getCustomName() : "#" + c.getID();
                Location loc = c.getSpawnLocation() != null ? c.getSpawnLocation() : (c.centerBlockLocation != null ? c.centerBlockLocation : c.getLesserBoundaryCorner());
                player.sendMessage(ChatColor.YELLOW + "▪ " + ChatColor.GOLD + cName + ChatColor.GRAY + " (ID: #" + c.getID() + ") - " +
                        ChatColor.GREEN + nf.format(c.getArea()) + " CB " +
                        ChatColor.DARK_GRAY + "[" + loc.getWorld().getName() + " " + loc.getBlockX() + ", " + loc.getBlockZ() + "]");
            }
            player.sendMessage(ChatColor.YELLOW + "Usa " + ChatColor.WHITE + "/ps spawn <nombre>" + ChatColor.YELLOW + " o " + ChatColor.WHITE + "/ps menu" + ChatColor.YELLOW + " para viajar.");
            player.sendMessage(ChatColor.DARK_GRAY + "=============================================");
            return true;
        }

        // /ps get <tipo>
        if (sub.equals("get")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Comando solo ejecutable por jugadores.");
                return true;
            }
            Player player = (Player) sender;
            if (!player.hasPermission("griefprevention.admin") && !player.hasPermission("aegrief.get")) {
                player.sendMessage(ChatColor.RED + "No tienes permiso para obtener bloques de protección.");
                return true;
            }
            if (args.length < 2) {
                player.sendMessage(ChatColor.YELLOW + "Uso: /ps get <carbón|cobre|hierro|oro|diamante|esmeralda>");
                return true;
            }
            ProtectionBlockType type = parseType(args[1]);
            if (type == null) {
                player.sendMessage(ChatColor.RED + "Tipo de bloque desconocido: " + args[1]);
                return true;
            }
            int radius = plugin.protectionConfig.getRadius(type);
            ItemStack item = type.createItemStack(1, radius);
            player.getInventory().addItem(item);
            player.sendMessage(ChatColor.GREEN + "Has recibido un " + plugin.protectionConfig.getDisplayName(type) + ChatColor.GREEN + ".");
            return true;
        }

        // /ps give <jugador> <tipo> [cantidad]
        if (sub.equals("give")) {
            if (!sender.hasPermission("griefprevention.admin") && !sender.hasPermission("aegrief.give")) {
                sender.sendMessage(ChatColor.RED + "No tienes permiso para dar bloques de protección.");
                return true;
            }
            if (args.length < 3) {
                sender.sendMessage(ChatColor.YELLOW + "Uso: /ps give <jugador> <tipo> [cantidad]");
                return true;
            }
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(ChatColor.RED + "Jugador no encontrado: " + args[1]);
                return true;
            }
            ProtectionBlockType type = parseType(args[2]);
            if (type == null) {
                sender.sendMessage(ChatColor.RED + "Tipo de bloque desconocido: " + args[2]);
                return true;
            }
            int amount = 1;
            if (args.length >= 4) {
                try {
                    amount = Math.max(1, Integer.parseInt(args[3]));
                } catch (NumberFormatException ignored) {}
            }
            int radius = plugin.protectionConfig.getRadius(type);
            ItemStack item = type.createItemStack(amount, radius);
            target.getInventory().addItem(item);
            sender.sendMessage(ChatColor.GREEN + "Entregado " + amount + "x " + plugin.protectionConfig.getDisplayName(type) + ChatColor.GREEN + " a " + target.getName() + ".");
            target.sendMessage(ChatColor.GREEN + "Has recibido " + amount + "x " + plugin.protectionConfig.getDisplayName(type) + ChatColor.GREEN + ".");
            return true;
        }

        // /ps info
        if (sub.equals("info")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Comando solo ejecutable por jugadores.");
                return true;
            }
            Player player = (Player) sender;
            Claim claim = plugin.dataStore.getClaimAt(player.getLocation(), true, null);
            if (claim == null) {
                player.sendMessage(ChatColor.RED + "No estás dentro de ninguna protección.");
                return true;
            }
            player.sendMessage(ChatColor.DARK_GRAY + "----------------------------------------");
            player.sendMessage(ChatColor.GOLD + "✦ Información de la Protección #" + claim.getID() + (claim.getCustomName() != null ? " (" + claim.getCustomName() + ")" : ""));
            player.sendMessage(ChatColor.GRAY + "Propietario: " + ChatColor.WHITE + claim.getOwnerName());
            player.sendMessage(ChatColor.GRAY + "Confianza de Clan: " + ChatColor.YELLOW + claim.getClanTrust().name());
            int width = claim.getGreaterBoundaryCorner().getBlockX() - claim.getLesserBoundaryCorner().getBlockX() + 1;
            int length = claim.getGreaterBoundaryCorner().getBlockZ() - claim.getLesserBoundaryCorner().getBlockZ() + 1;
            player.sendMessage(ChatColor.GRAY + "Dimensiones: " + ChatColor.YELLOW + width + " x " + length + " bloques");
            player.sendMessage(ChatColor.GRAY + "Terreno protegido: " + ChatColor.GREEN + claim.getArea() + " ClaimBlocks");
            player.sendMessage(ChatColor.GRAY + "Punto Spawn: " + ChatColor.AQUA + (claim.getSpawnLocation() != null ? "Configurado" : "Centro"));
            player.sendMessage(ChatColor.DARK_GRAY + "----------------------------------------");
            return true;
        }

        // /ps flag <flag> [valor]
        if (sub.equals("flag")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Comando solo ejecutable por jugadores.");
                return true;
            }
            Player player = (Player) sender;
            Claim claim = plugin.dataStore.getClaimAt(player.getLocation(), true, null);
            if (claim == null) {
                player.sendMessage(ChatColor.RED + "✖ No te encuentras dentro de ninguna protección.");
                player.sendMessage(ChatColor.GRAY + "Párate dentro de tu terreno protegido o coloca un bloque de protección para configurarlo.");
                return true;
            }
            boolean isOwner = player.getUniqueId().equals(claim.getOwnerID());
            boolean isAdmin = player.hasPermission("griefprevention.admin");
            boolean canManage = isOwner || isAdmin || claim.checkPermission(player, ClaimPermission.Manage, null) == null;
            if (!canManage) {
                player.sendMessage(ChatColor.RED + "✖ No tienes permisos para configurar flags en esta protección (Propietario: " + claim.getOwnerName() + ").");
                return true;
            }
            if (args.length < 2) {
                player.sendMessage(ChatColor.YELLOW + "Uso: /ps flag <nombre-flag> [allow|deny|texto]");
                return true;
            }
            ClaimFlag flag = ClaimFlag.fromString(args[1]);
            if (flag == null) {
                player.sendMessage(ChatColor.RED + "Flag desconocida. Usa /ps flags para ver la lista completa.");
                return true;
            }
            if (plugin.flagConfig != null && !plugin.flagConfig.isEnabled(flag)) {
                player.sendMessage(ChatColor.RED + "✖ La flag '" + flag.name().toLowerCase() + "' ha sido desactivada por la administración del servidor.");
                return true;
            }
            if (args.length == 2) {
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
            for (int i = 2; i < args.length; i++) {
                if (i > 2) valBuilder.append(" ");
                valBuilder.append(args[i]);
            }
            String val = valBuilder.toString();
            claim.setFlag(flag, val);
            plugin.dataStore.saveClaim(claim);
            player.sendMessage(ChatColor.GREEN + "Flag " + ChatColor.YELLOW + flag.name().toLowerCase() + ChatColor.GREEN + " configurada a: " + ChatColor.WHITE + val);
            return true;
        }

        // /ps flags
        if (sub.equals("flags")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Comando solo ejecutable por jugadores.");
                return true;
            }
            Player player = (Player) sender;
            Claim claim = plugin.dataStore.getClaimAt(player.getLocation(), true, null);
            if (claim == null) {
                player.sendMessage(ChatColor.RED + "✖ No te encuentras dentro de ninguna protección.");
                player.sendMessage(ChatColor.GRAY + "Usa " + ChatColor.YELLOW + "/ps" + ChatColor.GRAY + " para ver tu lista de terrenos o entra a una protección para ver sus flags.");
                return true;
            }
            ProtectionGui.open(plugin, player, claim);
            return true;
        }

        // /ps show or /ps border
        if (sub.equals("show") || sub.equals("border") || sub.equals("ver")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Comando solo ejecutable por jugadores.");
                return true;
            }
            Player player = (Player) sender;
            Claim claim = plugin.dataStore.getClaimAt(player.getLocation(), true, null);
            if (claim == null) {
                player.sendMessage(ChatColor.RED + "Debes estar parado dentro de una protección para ver sus límites.");
                return true;
            }
            Visualization visual = Visualization.FromClaim(claim, player.getLocation().getBlockY(), VisualizationType.Claim, player.getLocation());
            Visualization.Apply(player, visual);
            player.sendMessage(ChatColor.GREEN + "✦ Bordes de la protección visualizados.");
            return true;
        }

        // /ps reload
        if (sub.equals("reload")) {
            if (!sender.hasPermission("griefprevention.admin")) {
                sender.sendMessage(ChatColor.RED + "No tienes permiso para recargar la configuración.");
                return true;
            }
            plugin.protectionConfig.loadConfig();
            if (plugin.flagConfig != null) {
                plugin.flagConfig.loadConfig();
            }
            sender.sendMessage(ChatColor.GREEN + "✔ Configuración de bloques (protection_blocks.yml) y flags (claim_flags.yml) recargada con éxito.");
            sender.sendMessage(ChatColor.GRAY + "Nota: Los nuevos radios solo aplicarán a las protecciones colocadas a partir de ahora.");
            return true;
        }

        sendHelp(sender);
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.DARK_GRAY + "============== " + ChatColor.GOLD + "AEGrief Protection Stones" + ChatColor.DARK_GRAY + " ==============");
        sender.sendMessage(ChatColor.YELLOW + "/ps" + ChatColor.GRAY + " - Abrir el Menú Gráfico de categorías y gestión.");
        sender.sendMessage(ChatColor.YELLOW + "/ps list" + ChatColor.GRAY + " - Ver todas tus protecciones registradas.");
        sender.sendMessage(ChatColor.YELLOW + "/ps spawn [nombre]" + ChatColor.GRAY + " - Teletransportarse al centro de la protección.");
        sender.sendMessage(ChatColor.YELLOW + "/ps setspawn [nombre]" + ChatColor.GRAY + " - Fijar punto de spawn aquí.");
        sender.sendMessage(ChatColor.YELLOW + "/ps rename <nombre>" + ChatColor.GRAY + " - Asignar un nombre a tu terreno.");
        sender.sendMessage(ChatColor.YELLOW + "/ps show" + ChatColor.GRAY + " - Visualizar los bordes del terreno (también con un palo).");
        sender.sendMessage(ChatColor.YELLOW + "/ps info" + ChatColor.GRAY + " - Ver información del terreno actual.");
        sender.sendMessage(ChatColor.YELLOW + "/ps flags" + ChatColor.GRAY + " - Abrir panel de flags.");
        sender.sendMessage(ChatColor.YELLOW + "/ps get <tipo>" + ChatColor.GRAY + " - Obtener un bloque de protección.");
        sender.sendMessage(ChatColor.YELLOW + "/ps give <jugador> <tipo>" + ChatColor.GRAY + " - Dar un bloque a un jugador.");
        sender.sendMessage(ChatColor.YELLOW + "/clantrust <build|container|access|off>" + ChatColor.GRAY + " - Acceso de clan.");
        sender.sendMessage(ChatColor.YELLOW + "/c <mensaje>" + ChatColor.GRAY + " - Chat privado del clan.");
        sender.sendMessage(ChatColor.DARK_GRAY + "=====================================================");
    }

    private ProtectionBlockType parseType(String str) {
        if (str == null) return null;
        String s = str.toLowerCase().replace("á", "a").replace("é", "e").replace("í", "i").replace("ó", "o");
        switch (s) {
            case "carbon":
            case "coal":
                return ProtectionBlockType.COAL;
            case "cobre":
            case "copper":
                return ProtectionBlockType.COPPER;
            case "hierro":
            case "iron":
                return ProtectionBlockType.IRON;
            case "oro":
            case "gold":
                return ProtectionBlockType.GOLD;
            case "diamante":
            case "diamond":
                return ProtectionBlockType.DIAMOND;
            case "esmeralda":
            case "emerald":
                return ProtectionBlockType.EMERALD;
            default:
                try {
                    return ProtectionBlockType.valueOf(str.toUpperCase());
                } catch (IllegalArgumentException e) {
                    return null;
                }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            List<String> subs = Arrays.asList("menu", "spawn", "tp", "setspawn", "rename", "show", "border", "list", "get", "give", "info", "flag", "flags", "reload");
            return subs.stream().filter(s -> s.startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("spawn") || args[0].equalsIgnoreCase("tp"))) {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                Vector<Claim> claims = plugin.dataStore.getPlayerData(player.getUniqueId()).getClaims();
                List<String> names = new ArrayList<>();
                for (Claim c : claims) {
                    if (c.getCustomName() != null) names.add(c.getCustomName());
                    names.add(String.valueOf(c.getID()));
                }
                return names.stream().filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase())).collect(Collectors.toList());
            }
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("get") || args[0].equalsIgnoreCase("give"))) {
            if (args[0].equalsIgnoreCase("give") && args.length == 2) {
                return null;
            }
            List<String> types = Arrays.asList("carbon", "cobre", "hierro", "oro", "diamante", "esmeralda");
            return types.stream().filter(s -> s.startsWith(args[1].toLowerCase())).collect(Collectors.toList());
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            List<String> types = Arrays.asList("carbon", "cobre", "hierro", "oro", "diamante", "esmeralda");
            return types.stream().filter(s -> s.startsWith(args[2].toLowerCase())).collect(Collectors.toList());
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("flag")) {
            return Arrays.stream(ClaimFlag.values())
                    .map(f -> f.name().toLowerCase())
                    .filter(s -> s.startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("flag")) {
            List<String> vals = Arrays.asList("allow", "deny");
            return vals.stream().filter(s -> s.startsWith(args[2].toLowerCase())).collect(Collectors.toList());
        }
        return completions;
    }
}
