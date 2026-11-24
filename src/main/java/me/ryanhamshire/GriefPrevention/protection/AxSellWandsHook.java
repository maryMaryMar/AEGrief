package me.ryanhamshire.GriefPrevention.protection;

import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.ClaimPermission;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public class AxSellWandsHook implements Listener {

    public AxSellWandsHook() {
    }

    public boolean canInteract(Player player, BlockState blockState, Event event) {
        if (blockState instanceof Container) {
            Claim claim = GriefPrevention.instance.dataStore.getClaimAt(blockState.getLocation(), true, null);
            if (claim != null) {
                return claim.checkPermission(player, ClaimPermission.Inventory, event) == null;
            }
        }
        return true;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock() != null) {
            Block block = event.getClickedBlock();
            if (block.getState() instanceof Container) {
                Player player = event.getPlayer();
                Claim claim = GriefPrevention.instance.dataStore.getClaimAt(block.getLocation(), true, null);
                if (claim != null && claim.checkPermission(player, ClaimPermission.Inventory, event) != null) {
                    event.setCancelled(true);
                }
            }
        }
    }
}
