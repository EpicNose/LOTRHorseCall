package com.epicnose.lotrcallablehorse.lotr.common;

import com.epicnose.lotrcallablehorse.lotr.common.mount.IMountAdapter;
import com.epicnose.lotrcallablehorse.lotr.common.mount.MountAdapterRegistry;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import lotr.common.LOTRReflection;
import lotr.common.entity.animal.LOTREntityHorse;
import lotr.common.entity.npc.LOTREntityNPCRideable;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.passive.EntityHorse;
import net.minecraft.inventory.IInventory;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import cpw.mods.fml.common.FMLLog;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.world.ChunkEvent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Tracks only summoned entities; persistent slot data lives in HorseInfo. */
public class CallableHorseEventHandler {
    public static final class ChunkLocation {
        public final int dimension;
        public final int chunkX;
        public final int chunkZ;

        public ChunkLocation(int dimension, int chunkX, int chunkZ) {
            this.dimension = dimension;
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ChunkLocation)) return false;
            ChunkLocation that = (ChunkLocation) o;
            return dimension == that.dimension && chunkX == that.chunkX && chunkZ == that.chunkZ;
        }

        @Override
        public int hashCode() {
            int result = dimension;
            result = 31 * result + chunkX;
            result = 31 * result + chunkZ;
            return result;
        }
    }

    public static ConcurrentHashMap<UUID, ChunkLocation> vehicleAndChunk;
    public static ConcurrentHashMap<UUID, UUID> VehicleEntityAndOwner;
    /** Mounts whose death event was observed but whose drops event may still be pending. */
    private static ConcurrentHashMap<UUID, Boolean> deathSnapshots;

    public CallableHorseEventHandler() {
        FMLCommonHandler.instance().bus().register(this);
        MinecraftForge.EVENT_BUS.register(this);
        vehicleAndChunk = new ConcurrentHashMap<UUID, ChunkLocation>();
        VehicleEntityAndOwner = new ConcurrentHashMap<UUID, UUID>();
        deathSnapshots = new ConcurrentHashMap<UUID, Boolean>();
    }

    public static void trackVehicle(Entity vehicle, UUID ownerUUID) {
        if (vehicle == null || ownerUUID == null || !CallableHorseMountSupport.isSupported(vehicle)) {
            return;
        }
        UUID vehicleUUID = vehicle.getUniqueID();
        VehicleEntityAndOwner.put(vehicleUUID, ownerUUID);
        updateVehicleChunk(vehicle);
    }

    public static void updateVehicleChunk(Entity vehicle) {
        if (vehicle != null && vehicle.worldObj != null) {
            int dim = vehicle.worldObj.provider != null ? vehicle.worldObj.provider.dimensionId : 0;
            int cx = MathHelper.floor_double(vehicle.posX) >> 4;
            int cz = MathHelper.floor_double(vehicle.posZ) >> 4;
            vehicleAndChunk.put(vehicle.getUniqueID(), new ChunkLocation(dim, cx, cz));
        }
    }

    public static void untrackVehicle(UUID vehicleUUID) {
        if (vehicleUUID == null) {
            return;
        }
        if (vehicleAndChunk != null) {
            vehicleAndChunk.remove(vehicleUUID);
        }
        if (VehicleEntityAndOwner != null) {
            VehicleEntityAndOwner.remove(vehicleUUID);
        }
        if (deathSnapshots != null) {
            deathSnapshots.remove(vehicleUUID);
        }
    }

    /** Called when a server instance ends, including an integrated-server restart. */
    public static void clearTracking() {
        if (vehicleAndChunk != null) {
            vehicleAndChunk.clear();
        }
        if (VehicleEntityAndOwner != null) {
            VehicleEntityAndOwner.clear();
        }
        if (deathSnapshots != null) {
            deathSnapshots.clear();
        }
    }

    /** Snapshot and remove every active mount before the world is closed. */
    public static void recallAllTrackedVehicles() {
        // The UUID index is only an optimization.  Include active slots as
        // well so a mount spawned during an index update is still recalled.
        if (CallableHorseLevelData.playerDataMap != null) {
            for (PlayerHorseData playerData : CallableHorseLevelData.playerDataMap.values()) {
                recallActiveVehicles(playerData, false);
            }
        }
        if (VehicleEntityAndOwner == null) {
            return;
        }
        ArrayList<UUID> mounts = new ArrayList<UUID>(VehicleEntityAndOwner.keySet());
        for (UUID mountUUID : mounts) {
            UUID ownerUUID = VehicleEntityAndOwner.get(mountUUID);
            PlayerHorseData playerData = ownerUUID == null ? null : CallableHorseLevelData.getLoadedData(ownerUUID);
            SingleVehicle vehicle = playerData == null || playerData.horseInfo == null
                    ? null : playerData.horseInfo.getSingleVehicleByUUID(mountUUID);
            if (vehicle != null) {
                playerData.horseInfo.removeVehicle(vehicle, false);
            } else {
                untrackVehicle(mountUUID);
            }
        }
    }

    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!event.player.worldObj.isRemote && event.player instanceof EntityPlayerMP) {
            CallableHorseLevelData.sendPlayerData((EntityPlayerMP) event.player);
        }
    }

    @SubscribeEvent
    public void onChunkUnload(ChunkEvent.Unload event) {
        if (event == null || event.world == null || event.world.isRemote
                || VehicleEntityAndOwner == null) {
            return;
        }

        Chunk unloading = event.getChunk();
        if (unloading == null) {
            return;
        }

        Set<UUID> trackedMounts = new HashSet<UUID>();
        collectTrackedMountsInChunk(unloading, trackedMounts);

        for (UUID mountUUID : trackedMounts) {
            recallTrackedVehicle(mountUUID, unloading);
        }
    }

    @SubscribeEvent
    public void onEntityJoinWorld(EntityJoinWorldEvent event) {
        if (event == null || event.world == null || event.world.isRemote || event.entity == null) {
            return;
        }
        Entity entity = event.entity;
        if (entity.getEntityData() != null) {
            // 正在由模组主动召唤的新载具，打上了放行标记，立即放行绝不拦截
            if (entity.getEntityData().getBoolean("LOTR_CallableHorse_Spawning")) {
                entity.getEntityData().removeTag("LOTR_CallableHorse_Spawning");
                return;
            }
            if (entity.getEntityData().hasKey("LOTR_CallableHorse_Owner")) {
                UUID entityUUID = entity.getUniqueID();
                // 如果已在活跃跟踪列表中，允许入界
                if (VehicleEntityAndOwner != null && VehicleEntityAndOwner.containsKey(entityUUID)) {
                    return;
                }
                // 检查是否为旧崩溃或意外卸载遗留的孤儿实体
                try {
                    String ownerStr = entity.getEntityData().getString("LOTR_CallableHorse_Owner");
                    if (ownerStr != null && !ownerStr.isEmpty()) {
                        UUID ownerUUID = UUID.fromString(ownerStr);
                        PlayerHorseData playerData = CallableHorseLevelData.getLoadedData(ownerUUID);
                        if (playerData != null && playerData.horseInfo != null) {
                            SingleVehicle vehicle = playerData.horseInfo.getSingleVehicleByUUID(entityUUID);
                            if (vehicle == null || !vehicle.isUsing || vehicle.ent != entity) {
                                entity.setDead();
                                event.setCanceled(true);
                                FMLLog.info("[召之马来]拦截并清除区块重载的旧残留孤儿载具实体: %s (UUID: %s)",
                                        entity.getCommandSenderName(), entityUUID);
                            }
                        }
                    }
                } catch (Throwable ignored) {
                }
            }
        }
    }

    @SubscribeEvent
    public void onPlayerLeave(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.player == null || event.player.worldObj == null || event.player.worldObj.isRemote) {
            return;
        }
        PlayerHorseData playerData = CallableHorseLevelData.getLoadedData(event.player.getUniqueID());
        if (playerData != null && playerData.horseInfo != null) {
            int recalled = recallActiveVehicles(playerData, false);
            // Also clean stale owner-index entries that no longer point at a
            // slot (for example after a failed network update).
            if (VehicleEntityAndOwner != null) {
                ArrayList<UUID> stale = new ArrayList<UUID>();
                for (Map.Entry<UUID, UUID> entry : VehicleEntityAndOwner.entrySet()) {
                    if (event.player.getUniqueID().equals(entry.getValue())
                            && playerData.horseInfo.getSingleVehicleByUUID(entry.getKey()) == null) {
                        stale.add(entry.getKey());
                    }
                }
                for (UUID mountUUID : stale) {
                    untrackVehicle(mountUUID);
                }
            }
            if (recalled > 0 || playerData.needsSave()) {
                CallableHorseLevelData.saveData(event.player.getUniqueID());
            }
        }
    }

    @SubscribeEvent
    public void onMountUpdate(LivingEvent.LivingUpdateEvent event) {
        if (event == null || event.entity == null || VehicleEntityAndOwner == null) {
            return;
        }
        Entity mount = event.entity;
        UUID mountUUID = mount.getUniqueID();
        // 极速过滤：绝大多数普通实体直接快速跳过，极大节约服务端每 Tick 性能开销
        if (!VehicleEntityAndOwner.containsKey(mountUUID) && !(mount.riddenByEntity instanceof EntityPlayer)) {
            return;
        }

        World world = mount.worldObj;
        if (world == null || world.isRemote || !CallableHorseMountSupport.isSupported(mount)
                || world.getTotalWorldTime() % 20L != 0L) {
            return;
        }
        if (mount.isDead) {
            finalizeDeadMount(mount);
            return;
        }

        // A listener may cancel a death after our HIGHEST-priority snapshot.
        // The live mount is authoritative in that case; discard the pending
        // marker and keep tracking it normally.
        if (deathSnapshots != null) {
            deathSnapshots.remove(mountUUID);
        }

        UUID ownerUUID = VehicleEntityAndOwner.get(mountUUID);
        if (ownerUUID == null && mount.riddenByEntity instanceof EntityPlayer) {
            EntityPlayer rider = (EntityPlayer) mount.riddenByEntity;
            PlayerHorseData riderData = CallableHorseLevelData.getLoadedData(rider.getUniqueID());
            if (riderData != null && riderData.horseInfo != null && riderData.horseInfo.isVehicleEntity(mount)) {
                ownerUUID = rider.getUniqueID();
                trackVehicle(mount, ownerUUID);
            }
        }
        if (ownerUUID == null) {
            return;
        }

        PlayerHorseData playerData = CallableHorseLevelData.getLoadedData(ownerUUID);
        SingleVehicle vehicle = playerData == null || playerData.horseInfo == null
                ? null : playerData.horseInfo.getSingleVehicleByEntity(mount);
        if (vehicle == null) {
            untrackVehicle(mountUUID);
            return;
        }

        vehicle.ent = mount;
        vehicle.world = world;
        updateVehicleChunk(mount);
        // 已彻底移除原版超过 15 米自动强制收回的破坏性逻辑，保持原汁原味的沉浸式骑乘体验。
        // 如服务端确实有超远距离清理需求，可在配置项 autoRecallDistance 中自定义距离（默认 0 = 禁用）。
        if (CallableHorseConfig.autoRecallDistance > 0 && playerData.getPlayer() != null) {
            EntityPlayer owner = playerData.getPlayer();
            if (owner.worldObj == mount.worldObj) {
                double distSq = mount.getDistanceSqToEntity(owner);
                if (distSq > (double) (CallableHorseConfig.autoRecallDistance * CallableHorseConfig.autoRecallDistance)) {
                    playerData.horseInfo.removeVehicle(vehicle);
                    return;
                }
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onMountDeath(LivingDeathEvent event) {
        if (event == null || event.isCanceled() || event.entity == null
                || event.entity.worldObj == null || event.entity.worldObj.isRemote
                || VehicleEntityAndOwner == null) {
            return;
        }
        Entity mount = event.entity;
        UUID ownerUUID = VehicleEntityAndOwner.get(mount.getUniqueID());
        if (ownerUUID == null) {
            return;
        }

        PlayerHorseData playerData = CallableHorseLevelData.getLoadedData(ownerUUID);
        SingleVehicle vehicle = playerData == null || playerData.horseInfo == null
                ? null : playerData.horseInfo.getSingleVehicleByEntity(mount);
        if (vehicle == null) {
            untrackVehicle(mount.getUniqueID());
            return;
        }

        // LivingDeathEvent is dispatched before vanilla/LOTR's internal drop
        // code.  Capture first, but defer destructive cleanup until the drops
        // event (or the next tick when mob loot is disabled).  This keeps a
        // later listener that cancels the death from losing the live mount.
        vehicle.captureEntityState(mount);
        if (deathSnapshots != null) {
            deathSnapshots.put(mount.getUniqueID(), Boolean.TRUE);
        }
        playerData.markDirty();
    }

    /**
     * Prevent mount equipment/inventory from being spawned as dropped items.
     * The complete pre-death state is already held by the slot snapshot.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onMountDrops(LivingDropsEvent event) {
        if (event == null || event.entity == null || event.entity.worldObj == null
                || event.entity.worldObj.isRemote || VehicleEntityAndOwner == null
                || event.drops == null) {
            return;
        }
        UUID mountUUID = event.entity.getUniqueID();
        if (!VehicleEntityAndOwner.containsKey(mountUUID)) {
            return;
        }
        // A later listener may cancel a death after our early snapshot.  Do
        // not finalize a still-live mount merely because another mod posted a
        // drops event for it; the normal update hook will discard the marker.
        if (!event.entity.isDead
                && (deathSnapshots == null || !deathSnapshots.containsKey(mountUUID))) {
            return;
        }
        event.drops.clear();
        if (event.entity.isDead) {
            finalizeDeadMount(event.entity);
        }
    }

    private static void finalizeDeadMount(Entity mount) {
        if (mount == null || VehicleEntityAndOwner == null) {
            return;
        }
        UUID mountUUID = mount.getUniqueID();
        UUID ownerUUID = VehicleEntityAndOwner.get(mountUUID);
        if (ownerUUID == null) {
            untrackVehicle(mountUUID);
            return;
        }
        PlayerHorseData playerData = CallableHorseLevelData.getLoadedData(ownerUUID);
        SingleVehicle vehicle = playerData == null || playerData.horseInfo == null
                ? null : playerData.horseInfo.getSingleVehicleByUUID(mountUUID);
        if (vehicle == null) {
            untrackVehicle(mountUUID);
            return;
        }

        // If the death event was skipped (for example doMobLoot=false), take
        // the snapshot now.  Otherwise retain the pre-drop snapshot captured
        // by onMountDeath so emptied inventories do not overwrite it.
        if (deathSnapshots == null || !deathSnapshots.containsKey(mountUUID)) {
            vehicle.captureEntityState(mount);
        }
        if (vehicle.isValid && vehicle.entityData != null && vehicle.entityName.length() > 0) {
            clearMountEquipment(mount);
        }
        vehicle.ent = null;
        vehicle.isUsing = false;
        untrackVehicle(mountUUID);
        if (deathSnapshots != null) {
            deathSnapshots.remove(mountUUID);
        }
        playerData.markDirty();
        EntityPlayer owner = playerData.getPlayer();
        if (owner instanceof EntityPlayerMP) {
            CallableHorseLevelData.sendPlayerData((EntityPlayerMP) owner);
        }
    }

    private static int recallActiveVehicles(PlayerHorseData playerData, boolean sync) {
        if (playerData == null || playerData.horseInfo == null
                || playerData.horseInfo.vehicles == null) {
            return 0;
        }
        ArrayList<SingleVehicle> active = new ArrayList<SingleVehicle>();
        synchronized (playerData.horseInfo.vehicles) {
            for (SingleVehicle vehicle : playerData.horseInfo.vehicles) {
                if (vehicle == null) {
                    continue;
                }
                if (vehicle.isUsing || (vehicle.ent != null && !vehicle.ent.isDead)) {
                    active.add(vehicle);
                }
            }
        }
        for (SingleVehicle vehicle : active) {
            playerData.horseInfo.removeVehicle(vehicle, sync);
        }
        return active.size();
    }

    private static void collectTrackedMountsInChunk(Chunk chunk, Set<UUID> result) {
        if (chunk == null || result == null || VehicleEntityAndOwner == null) {
            return;
        }
        if (vehicleAndChunk != null) {
            int dim = chunk.worldObj != null && chunk.worldObj.provider != null
                    ? chunk.worldObj.provider.dimensionId : 0;
            for (Map.Entry<UUID, ChunkLocation> entry : vehicleAndChunk.entrySet()) {
                ChunkLocation loc = entry.getValue();
                if (loc != null && loc.dimension == dim
                        && loc.chunkX == chunk.xPosition && loc.chunkZ == chunk.zPosition) {
                    result.add(entry.getKey());
                }
            }
        }

        // The UUID->Chunk index is refreshed periodically.  During movement
        // or chunk unload it can lag behind the actual Chunk.entityLists, so
        // inspect the authoritative entity lists as a fallback.
        List[] entityLists = chunk.entityLists;
        if (entityLists == null) {
            return;
        }
        for (List entities : entityLists) {
            if (entities == null || entities.isEmpty()) {
                continue;
            }
            Object[] snapshot;
            try {
                snapshot = entities.toArray();
            } catch (Throwable ignored) {
                continue;
            }
            for (Object value : snapshot) {
                if (!(value instanceof Entity)) {
                    continue;
                }
                Entity entity = (Entity) value;
                UUID entityUUID;
                try {
                    entityUUID = entity.getUniqueID();
                } catch (Throwable ignored) {
                    continue;
                }
                if (entityUUID != null && VehicleEntityAndOwner.containsKey(entityUUID)) {
                    result.add(entityUUID);
                }
            }
        }
    }

    private static Entity findEntityInChunk(Chunk chunk, UUID mountUUID) {
        if (chunk == null || mountUUID == null || chunk.entityLists == null) {
            return null;
        }
        for (List entities : chunk.entityLists) {
            if (entities == null || entities.isEmpty()) {
                continue;
            }
            Object[] snapshot;
            try {
                snapshot = entities.toArray();
            } catch (Throwable ignored) {
                continue;
            }
            for (Object value : snapshot) {
                if (value instanceof Entity) {
                    Entity entity = (Entity) value;
                    try {
                        if (mountUUID.equals(entity.getUniqueID())) {
                            return entity;
                        }
                    } catch (Throwable ignored) {
                        // Ignore malformed third-party entities.
                    }
                }
            }
        }
        return null;
    }

    private static void recallTrackedVehicle(UUID mountUUID, Chunk unloading) {
        if (mountUUID == null || VehicleEntityAndOwner == null) {
            return;
        }
        UUID ownerUUID = VehicleEntityAndOwner.get(mountUUID);
        PlayerHorseData playerData = ownerUUID == null ? null : CallableHorseLevelData.getLoadedData(ownerUUID);
        SingleVehicle vehicle = playerData == null || playerData.horseInfo == null
                ? null : playerData.horseInfo.getSingleVehicleByUUID(mountUUID);
        if (vehicle == null) {
            untrackVehicle(mountUUID);
            return;
        }
        if (vehicle.ent == null || vehicle.ent.isDead) {
            Entity found = findEntityInChunk(unloading, mountUUID);
            if (found != null && !found.isDead) {
                vehicle.ent = found;
            }
        }
        playerData.horseInfo.removeVehicle(vehicle);
    }

    private static void clearMountEquipment(Entity mount) {
        if (mount == null) {
            return;
        }
        try {
            IMountAdapter adapter = MountAdapterRegistry.getAdapter(mount);
            adapter.clearAllEquipment(mount);
        } catch (Throwable ignored) {
        }
    }

    private static void clearInventory(IInventory inventory) {
        if (inventory == null) {
            return;
        }
        try {
            int size = inventory.getSizeInventory();
            for (int slot = 0; slot < size; slot++) {
                if (inventory.getStackInSlot(slot) != null) {
                    inventory.setInventorySlotContents(slot, null);
                }
            }
            inventory.markDirty();
        } catch (Throwable ignored) {
            // A third-party inventory must never break the death event.
        }
    }
}
