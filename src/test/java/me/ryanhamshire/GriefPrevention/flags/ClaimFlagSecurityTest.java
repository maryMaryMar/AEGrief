package me.ryanhamshire.GriefPrevention.flags;

import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.DataStore;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.protection.ProtectionBlockType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Phantom;
import org.bukkit.entity.Player;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.Slime;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.entity.Witch;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.entity.AreaEffectCloudApplyEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ClaimFlagSecurityTest {

    private ClaimFlagListener createListener(GriefPrevention plugin) {
        try (var bukkit = mockStatic(Bukkit.class)) {
            BukkitScheduler scheduler = mock(BukkitScheduler.class);
            bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
            when(scheduler.runTaskTimer(eq(plugin), any(Runnable.class), anyLong(), anyLong())).thenReturn(null);
            return new ClaimFlagListener(plugin);
        }
    }

    private Claim mockClaim(ClaimFlag flag, boolean allowed) {
        Claim claim = mock(Claim.class);
        when(claim.isFlagAllowed(flag)).thenReturn(allowed);
        return claim;
    }

    @Test
    void skeletonArrowBlockedWhenMonsterDamageDenied() {
        GriefPrevention plugin = mock(GriefPrevention.class);
        plugin.dataStore = mock(DataStore.class);
        ClaimFlagListener listener = createListener(plugin);

        Player victim = mock(Player.class);
        Location loc = new Location(mock(World.class), 10, 64, 10);
        when(victim.getLocation()).thenReturn(loc);

        Skeleton skeleton = mock(Skeleton.class);
        Arrow arrow = mock(Arrow.class);
        when(arrow.getShooter()).thenReturn(skeleton);

        Claim claim = mockClaim(ClaimFlag.MONSTER_DAMAGE, false);
        when(plugin.dataStore.getClaimAt(loc, true, null)).thenReturn(claim);

        EntityDamageByEntityEvent event = new EntityDamageByEntityEvent(arrow, victim, EntityDamageEvent.DamageCause.PROJECTILE, 4.0);
        listener.onMonsterDamage(event);

        assertTrue(event.isCancelled());
    }

    @Test
    void witchDirectPotionAndSplashPotionBlockedWhenMonsterDamageDenied() {
        GriefPrevention plugin = mock(GriefPrevention.class);
        plugin.dataStore = mock(DataStore.class);
        ClaimFlagListener listener = createListener(plugin);

        Player victim = mock(Player.class);
        Location loc = new Location(mock(World.class), 20, 64, 20);
        when(victim.getLocation()).thenReturn(loc);

        Witch witch = mock(Witch.class);
        ThrownPotion potion = mock(ThrownPotion.class);
        when(potion.getShooter()).thenReturn(witch);

        Claim claim = mockClaim(ClaimFlag.MONSTER_DAMAGE, false);
        when(plugin.dataStore.getClaimAt(loc, true, null)).thenReturn(claim);

        // 1) Direct instant harm potion hit
        EntityDamageByEntityEvent dmgEvent = new EntityDamageByEntityEvent(potion, victim, EntityDamageEvent.DamageCause.MAGIC, 6.0);
        listener.onMonsterDamage(dmgEvent);
        assertTrue(dmgEvent.isCancelled());

        // 2) PotionSplashEvent for poison/slowness/weakness
        PotionSplashEvent splashEvent = mock(PotionSplashEvent.class);
        when(splashEvent.getPotion()).thenReturn(potion);
        when(splashEvent.getAffectedEntities()).thenReturn(List.of(victim));
        listener.onPotionSplash(splashEvent);
        verify(splashEvent).setIntensity(victim, 0.0);

        // 3) Lingering / AreaEffectCloud from Witch
        AreaEffectCloud cloud = mock(AreaEffectCloud.class);
        when(cloud.getSource()).thenReturn(witch);
        List<org.bukkit.entity.LivingEntity> affected = new ArrayList<>(List.of(victim));
        AreaEffectCloudApplyEvent cloudEvent = new AreaEffectCloudApplyEvent(cloud, affected);
        listener.onAreaEffectCloudApply(cloudEvent);
        assertTrue(affected.isEmpty());
    }

    @Test
    void enemyNonMonsterMobsBlockedOnDamageAndSpawn() {
        GriefPrevention plugin = mock(GriefPrevention.class);
        plugin.dataStore = mock(DataStore.class);
        ClaimFlagListener listener = createListener(plugin);

        Location loc = new Location(mock(World.class), 30, 64, 30);
        Claim claim = mock(Claim.class);
        when(claim.isFlagAllowed(ClaimFlag.MONSTER_DAMAGE)).thenReturn(false);
        when(claim.isFlagAllowed(ClaimFlag.MOB_SPAWNING)).thenReturn(false);
        when(plugin.dataStore.getClaimAt(loc, true, null)).thenReturn(claim);

        Player victim = mock(Player.class);
        when(victim.getLocation()).thenReturn(loc);
        Slime slime = mock(Slime.class);

        EntityDamageByEntityEvent dmgEvent = new EntityDamageByEntityEvent(slime, victim, EntityDamageEvent.DamageCause.ENTITY_ATTACK, 3.0);
        listener.onMonsterDamage(dmgEvent);
        assertTrue(dmgEvent.isCancelled());

        Phantom phantom = mock(Phantom.class);
        when(phantom.getType()).thenReturn(EntityType.PHANTOM);
        CreatureSpawnEvent spawnEvent = new CreatureSpawnEvent(phantom, CreatureSpawnEvent.SpawnReason.NATURAL);
        // Spawn event location comes from phantom.getLocation()
        when(phantom.getLocation()).thenReturn(loc);
        listener.onCreatureSpawn(spawnEvent);
        assertTrue(spawnEvent.isCancelled());
    }

    @Test
    void pvpProjectilesTntAndSplashPotionsBlockedWhenPvpDenied() {
        GriefPrevention plugin = mock(GriefPrevention.class);
        plugin.dataStore = mock(DataStore.class);
        ClaimFlagListener listener = createListener(plugin);

        Player attacker = mock(Player.class);
        Player defender = mock(Player.class);
        UUID attackerId = UUID.randomUUID();
        UUID defenderId = UUID.randomUUID();
        when(attacker.getUniqueId()).thenReturn(attackerId);
        when(defender.getUniqueId()).thenReturn(defenderId);

        Location atkLoc = new Location(mock(World.class), 0, 64, 0);
        Location defLoc = new Location(mock(World.class), 5, 64, 5);
        when(attacker.getLocation()).thenReturn(atkLoc);
        when(defender.getLocation()).thenReturn(defLoc);

        Claim claim = mockClaim(ClaimFlag.PVP, false);
        when(plugin.dataStore.getClaimAt(defLoc, true, null)).thenReturn(claim);
        when(plugin.dataStore.getClaimAt(atkLoc, true, null)).thenReturn(null);

        Arrow arrow = mock(Arrow.class);
        when(arrow.getShooter()).thenReturn(attacker);
        EntityDamageByEntityEvent arrowEvent = new EntityDamageByEntityEvent(arrow, defender, EntityDamageEvent.DamageCause.PROJECTILE, 5.0);
        listener.onCombatDamage(arrowEvent);
        assertTrue(arrowEvent.isCancelled());

        TNTPrimed tnt = mock(TNTPrimed.class);
        when(tnt.getSource()).thenReturn(attacker);
        EntityDamageByEntityEvent tntEvent = new EntityDamageByEntityEvent(tnt, defender, EntityDamageEvent.DamageCause.ENTITY_EXPLOSION, 10.0);
        listener.onCombatDamage(tntEvent);
        assertTrue(tntEvent.isCancelled());

        ThrownPotion splash = mock(ThrownPotion.class);
        when(splash.getShooter()).thenReturn(attacker);
        PotionSplashEvent splashEvent = mock(PotionSplashEvent.class);
        when(splashEvent.getPotion()).thenReturn(splash);
        when(splashEvent.getAffectedEntities()).thenReturn(List.of(attacker, defender));
        listener.onPotionSplash(splashEvent);
        verify(splashEvent).setIntensity(defender, 0.0);
        verify(splashEvent, never()).setIntensity(attacker, 0.0);
    }

    @Test
    void explosionsAndPistonsFilterBlocksAcrossClaimBorder() {
        GriefPrevention plugin = mock(GriefPrevention.class);
        plugin.dataStore = mock(DataStore.class);
        ClaimFlagListener listener = createListener(plugin);

        World world = mock(World.class);
        Location outsideLoc = new Location(world, 0, 64, 0);
        Location insideLoc = new Location(world, 2, 64, 0);

        Block outsideBlock = mock(Block.class);
        Block insideBlock = mock(Block.class);
        when(outsideBlock.getLocation()).thenReturn(outsideLoc);
        when(insideBlock.getLocation()).thenReturn(insideLoc);

        Claim protectedClaim = mock(Claim.class);
        when(protectedClaim.isFlagAllowed(ClaimFlag.EXPLOSIONS)).thenReturn(false);
        when(protectedClaim.isFlagAllowed(ClaimFlag.PISTONS)).thenReturn(false);

        when(plugin.dataStore.getClaimAt(outsideLoc, true, null)).thenReturn(null);
        when(plugin.dataStore.getClaimAt(insideLoc, true, null)).thenReturn(protectedClaim);

        List<Block> blocks = new ArrayList<>(List.of(outsideBlock, insideBlock));
        EntityExplodeEvent explodeEvent = new EntityExplodeEvent(mock(TNTPrimed.class), outsideLoc, blocks, 1.0f);
        listener.onExplosion(explodeEvent);
        assertEquals(List.of(outsideBlock), blocks);

        Block pistonBase = mock(Block.class);
        Block pushedFromOutside = mock(Block.class);
        when(pistonBase.getRelative(BlockFace.EAST)).thenReturn(pushedFromOutside);
        when(pushedFromOutside.getLocation()).thenReturn(outsideLoc);
        when(pushedFromOutside.getRelative(BlockFace.EAST)).thenReturn(insideBlock);
        BlockPistonExtendEvent pistonEvent = new BlockPistonExtendEvent(pistonBase, List.of(pushedFromOutside), BlockFace.EAST);
        listener.onPistonExtend(pistonEvent);
        assertTrue(pistonEvent.isCancelled());
    }

    @Test
    void forgedAnvilBlockWithoutPdcIsRejectedAndSubclaimRootIdResolvesSafely() {
        ItemStack forged = mock(ItemStack.class);
        ItemMeta meta = mock(ItemMeta.class);
        PersistentDataContainer pdc = mock(PersistentDataContainer.class);
        when(forged.getType()).thenReturn(Material.EMERALD_BLOCK);
        when(forged.hasItemMeta()).thenReturn(true);
        when(forged.getItemMeta()).thenReturn(meta);
        when(meta.getPersistentDataContainer()).thenReturn(pdc);
        when(pdc.has(any(), any())).thenReturn(false);
        when(meta.hasDisplayName()).thenReturn(true);
        when(meta.getDisplayName()).thenReturn("Protección");

        assertNull(ProtectionBlockType.fromItemStack(forged));

        Claim parent = mock(Claim.class);
        when(parent.getID()).thenReturn(42L);
        Claim subclaim = mock(Claim.class);
        subclaim.parent = parent;
        when(subclaim.getID()).thenReturn(null);

        assertEquals(42L, ClaimFlagListener.getRootClaimId(subclaim));
    }
}
