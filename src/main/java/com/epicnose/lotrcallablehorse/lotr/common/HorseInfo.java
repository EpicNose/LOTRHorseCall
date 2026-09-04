package com.epicnose.lotrcallablehorse.lotr.common;

import com.epicnose.lotrcallablehorse.lotr.common.network.CallableHorsePacketHandler;
import com.epicnose.lotrcallablehorse.lotr.common.network.PacketCallBackHorse;
import com.epicnose.lotrcallablehorse.lotr.common.network.PacketCallHorse;
import com.epicnose.lotrcallablehorse.lotr.common.network.PacketReleaseHorse;
import com.epicnose.lotrcallablehorse.lotr.common.network.PacketSingleHorseInfo;
import com.epicnose.lotrcallablehorse.lotr.common.mount.IMountAdapter;
import com.epicnose.lotrcallablehorse.lotr.common.mount.MountAdapterRegistry;
import cpw.mods.fml.common.FMLLog;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/** Player-owned vehicle slots and their server-side state transitions. */
public class HorseInfo {
    public List<SingleVehicle> vehicles;
    /** Kept for compatibility with older callers; the config is authoritative. */
    public int size = CallableHorseConfig.maxVehicleSlots;
    public UUID puuid;
    public PlayerHorseData parentData;
    private long lastSummonTime;

    public HorseInfo(UUID playerUUID) {
        this(playerUUID, null);
    }

    public HorseInfo(UUID playerUUID, PlayerHorseData parent) {
        puuid = playerUUID;
        parentData = parent;
        vehicles = Collections.synchronizedList(new ArrayList<SingleVehicle>());
        size = getMaxSlots();
    }

    public void onUpdate() {
        // Kept for binary compatibility with existing callers while keeping
        // the legacy capacity field in sync with the live configuration.
        size = getMaxSlots();
    }

    public SingleVehicle getIsUsing() {
        synchronized (vehicles) {
            for (SingleVehicle vehicle : vehicles) {
                if (vehicle == null) {
                    continue;
                }
                if (vehicle.isUsing) {
                    if (vehicle.ent == null || !vehicle.ent.isDead) {
                        return vehicle;
                    }
                    vehicle.isUsing = false;
                    vehicle.ent = null;
                }
            }
        }
        return null;
    }

    public void setIsUsing(int index) {
        synchronized (vehicles) {
            for (SingleVehicle vehicle : vehicles) {
                if (vehicle != null) {
                    vehicle.isUsing = vehicle.index == index;
                }
            }
        }
    }

    public void sendBasicData(EntityPlayerMP player) {
        if (player == null) {
            return;
        }
        synchronized (vehicles) {
            for (int i = 0; i < vehicles.size(); i++) {
                SingleVehicle vehicle = vehicles.get(i);
                if (vehicle != null) {
                    CallableHorsePacketHandler.networkWrapper.sendTo(new PacketSingleHorseInfo(i, vehicle), player);
                }
            }
        }
    }

    public boolean isVehicleEntity(Entity entity) {
        return getSingleVehicleByEntity(entity) != null;
    }

    public SingleVehicle getSingleVehicleByEntity(Entity entity) {
        return entity == null ? null : getSingleVehicleByUUID(entity.getUniqueID());
    }

    public SingleVehicle getSingleVehicleByUUID(UUID entityUUID) {
        if (entityUUID == null) {
            return null;
        }
        synchronized (vehicles) {
            for (SingleVehicle vehicle : vehicles) {
                if (vehicle != null && entityUUID.equals(vehicle.entityuuid)) {
                    return vehicle;
                }
            }
        }
        return null;
    }

    public SingleVehicle getSingleVehicleByIndex(int index) {
        synchronized (vehicles) {
            return index >= 0 && index < vehicles.size() ? vehicles.get(index) : null;
        }
    }

    public int getVehicleCount() {
        synchronized (vehicles) {
            return vehicles.size();
        }
    }

    public int getMaxSlots() {
        if (parentData != null) {
            return parentData.getMaxSlots();
        }
        PlayerHorseData data = CallableHorseLevelData.getLoadedData(puuid);
        if (data != null) {
            return data.getMaxSlots();
        }
        return CallableHorseConfig.getConfiguredSlotCount();
    }

    public boolean addVehicle(SingleVehicle vehicle) {
        if (vehicle == null || !vehicle.isValid) {
            return false;
        }
        synchronized (vehicles) {
            if (vehicles.size() >= getMaxSlots()) {
                return false;
            }
            vehicle.index = vehicles.size();
            vehicle.ownerUUID = puuid;
            vehicles.add(vehicle);
        }
        // Remove the source entity only after the slot has been committed.
        // A failed registration must leave the player's mount untouched.
        vehicle.detachSourceEntity();
        markDirty();
        return true;
    }

    /** Stores the live state and removes the summoned entity from its world. */
    public void removeVehicle(SingleVehicle vehicle) {
        removeVehicle(vehicle, true);
    }

    void removeVehicle(SingleVehicle vehicle, boolean sync) {
        if (vehicle == null) {
            return;
        }

        UUID trackedUUID = vehicle.entityuuid;
        Entity activeEntity = vehicle.ent;
        if (activeEntity != null && !activeEntity.isDead) {
            try {
                if (activeEntity.riddenByEntity instanceof EntityPlayer) {
                    ((EntityPlayer) activeEntity.riddenByEntity).mountEntity(null);
                }
                vehicle.captureEntityState(activeEntity);
                closeOpenContainers(activeEntity);
                try {
                    IMountAdapter adapter = MountAdapterRegistry.getAdapter(activeEntity);
                    if (adapter != null) {
                        adapter.clearAllEquipment(activeEntity);
                    }
                } catch (Throwable ignored) {
                }
                activeEntity.setDead();
            } catch (Throwable throwable) {
                // A third-party mount must not prevent the slot and tracking
                // state from being released during logout/chunk unload.
                FMLLog.warning("[召之马来]收回载具实体失败: %s", throwable.toString());
            }
        }

        vehicle.ent = null;
        vehicle.isUsing = false;
        CallableHorseEventHandler.untrackVehicle(trackedUUID);
        if (sync) {
            markDirtyAndSync();
        } else {
            markDirty();
        }
    }

    /** Force close any open inventory GUI container for any player currently interacting with this mount. */
    public static void closeOpenContainers(Entity activeEntity) {
        if (activeEntity == null || activeEntity.worldObj == null || activeEntity.worldObj.isRemote) {
            return;
        }
        try {
            IMountAdapter adapter = MountAdapterRegistry.getAdapter(activeEntity);
            IInventory mountInv = adapter != null ? adapter.getInventory(activeEntity) : null;
            if (activeEntity.worldObj.playerEntities == null) {
                return;
            }
            for (Object obj : activeEntity.worldObj.playerEntities) {
                if (obj instanceof EntityPlayerMP) {
                    EntityPlayerMP player = (EntityPlayerMP) obj;
                    if (player.openContainer != null && player.openContainer != player.inventoryContainer) {
                        boolean matches = false;
                        if (player.ridingEntity == activeEntity) {
                            matches = true;
                        } else if (mountInv != null && player.openContainer.inventorySlots != null) {
                            for (Object slotObj : player.openContainer.inventorySlots) {
                                if (slotObj instanceof Slot) {
                                    Slot slot = (Slot) slotObj;
                                    if (slot.inventory == mountInv || slot.inventory == activeEntity) {
                                        matches = true;
                                        break;
                                    }
                                }
                            }
                        }
                        if (matches) {
                            player.closeScreen();
                        }
                    }
                }
            }
        } catch (Throwable ignored) {
        }
    }

    /** Recall a slot. If the live entity exists, captures and despawns it; otherwise safely resets state. */
    public boolean recallVehicleByIndex(int index) {
        SingleVehicle vehicle = getSingleVehicleByIndex(index);
        if (vehicle == null) {
            return false;
        }
        EntityPlayer player = parentData != null ? parentData.getPlayer() : null;
        if (player == null && puuid != null) {
            PlayerHorseData pd = CallableHorseLevelData.getLoadedData(puuid);
            if (pd != null) {
                player = pd.getPlayer();
            }
        }
        if (vehicle.ent != null && !vehicle.ent.isDead) {
            String riderInfo = vehicle.ent.riddenByEntity != null ? vehicle.ent.riddenByEntity.getCommandSenderName() : "无骑乘者";
            CallableHorseConfig.logOperation("玩家 %s (UUID: %s) 召回栏位 %d 载具: 名称 [%s], 实体UUID: %s, 骑乘者: [%s], 位置: [维度 %d, X:%.1f, Y:%.1f, Z:%.1f]",
                    player != null ? player.getCommandSenderName() : "未知",
                    puuid != null ? puuid.toString() : "未知", index, vehicle.horseName, vehicle.ent.getUniqueID(),
                    riderInfo, vehicle.ent.dimension, vehicle.ent.posX, vehicle.ent.posY, vehicle.ent.posZ);
            removeVehicle(vehicle);
            if (player != null) {
                player.addChatMessage(new net.minecraft.util.ChatComponentText(
                        String.format("[召之马来]召回%d号载具 [%s] 成功！", index, vehicle.horseName)));
            }
            return true;
        }
        if (vehicle.isUsing) {
            CallableHorseConfig.logOperation("玩家 %s (UUID: %s) 强制重置栏位 %d 待命状态 (无存活实体): 名称 [%s]",
                    player != null ? player.getCommandSenderName() : "未知",
                    puuid != null ? puuid.toString() : "未知", index, vehicle.horseName);
            vehicle.isUsing = false;
            vehicle.ent = null;
            CallableHorseEventHandler.untrackVehicle(vehicle.entityuuid);
            markDirtyAndSync();
            if (player != null) {
                player.addChatMessage(new net.minecraft.util.ChatComponentText(
                        String.format("[召之马来]已重置%d号载具状态为待命中！", index)));
            }
            return true;
        }
        if (player != null) {
            player.addChatMessage(new net.minecraft.util.ChatComponentText("[召之马来]该载具当前处于待命状态，无需收回"));
        }
        return false;
    }

    public boolean spawnSpecificVehicleByIndex(int index, EntityPlayer player) {
        if (player == null || player.worldObj == null
                || !CallableHorseConfig.isValidStoredSlotIndex(index)) {
            return false;
        }
        SingleVehicle vehicleData = getSingleVehicleByIndex(index);
        if (vehicleData == null || !vehicleData.isValid) {
            return false;
        }

        // Treat a live entity already attached to this slot as an idempotent
        // request.  This avoids duplicate mounts after a delayed packet or a
        // client/server state update crossing in flight.
        if (vehicleData.ent != null && !vehicleData.ent.isDead) {
            if (vehicleData.ent.worldObj == player.worldObj) {
                // 同一世界：直接将坐骑召唤/传送至玩家身旁，杜绝原地不动
                SingleVehicle.placeNearPlayer(vehicleData.ent, player);
                vehicleData.ent.motionX = 0.0D;
                vehicleData.ent.motionY = 0.0D;
                vehicleData.ent.motionZ = 0.0D;
                vehicleData.ent.fallDistance = 0.0F;
                setIsUsing(index);
                CallableHorseEventHandler.trackVehicle(vehicleData.ent, player.getUniqueID());
                CallableHorseConfig.logOperation("玩家 %s (UUID: %s) 召唤/拉回已存在的载具: 栏位 %d, 名称 [%s], 实体UUID: %s, 位置: [维度 %d, X:%.1f, Y:%.1f, Z:%.1f]",
                        player.getCommandSenderName(), player.getUniqueID(), index, vehicleData.horseName, vehicleData.ent.getUniqueID(),
                        player.dimension, player.posX, player.posY, player.posZ);
                player.addChatMessage(new net.minecraft.util.ChatComponentText("[召之马来]载具已飞奔至您的身边！"));
                markDirtyAndSync();
                return true;
            } else {
                // 跨维度：先安全销毁旧维度实体，再在当前世界重新召唤
                removeVehicle(vehicleData, false);
            }
        }

        boolean isCreative = player.capabilities != null && player.capabilities.isCreativeMode;
        long cooldownMillis = CallableHorseConfig.summonCooldownSeconds * 1000L;
        long elapsed = Math.max(0L, System.currentTimeMillis() - lastSummonTime);
        if (!isCreative && cooldownMillis > 0L && lastSummonTime > 0L && elapsed < cooldownMillis) {
            long remainingSeconds = (cooldownMillis - elapsed + 999L) / 1000L;
            player.addChatMessage(new net.minecraft.util.ChatComponentText(
                    "[召之马来]请等待" + remainingSeconds + "秒后再召唤"));
            return false;
        }

        SingleVehicle activeVehicle = getIsUsing();
        if (activeVehicle == vehicleData && activeVehicle.ent != null && !activeVehicle.ent.isDead) {
            return true;
        }
        if (activeVehicle != null) {
            if (activeVehicle.ent == player.ridingEntity) {
                player.mountEntity(null);
            }
            removeVehicle(activeVehicle, false);
        }

        EntityLivingBase vehicle = vehicleData.spawnVehicle(player);
        if (vehicle == null) {
            player.addChatMessage(new net.minecraft.util.ChatComponentText(
                    "[召之马来]载具实体无法创建，可能当前魔戒版本缺少该实体"));
            markDirtyAndSync();
            return false;
        }

        vehicleData.ent = vehicle;
        vehicleData.entityuuid = vehicle.getUniqueID();
        vehicleData.world = vehicle.worldObj;
        vehicleData.ownerUUID = player.getUniqueID();
        setIsUsing(index);
        CallableHorseEventHandler.trackVehicle(vehicle, player.getUniqueID());
        lastSummonTime = System.currentTimeMillis();
        markDirtyAndSync();
        CallableHorseConfig.logOperation("玩家 %s (UUID: %s) 成功召唤载具: 栏位 %d, 名称 [%s], 实体类型: %s, 实体UUID: %s, 位置: [维度 %d, X:%.1f, Y:%.1f, Z:%.1f]",
                player.getCommandSenderName(), player.getUniqueID(), index, vehicleData.horseName, vehicle.getClass().getSimpleName(), vehicle.getUniqueID(),
                player.dimension, player.posX, player.posY, player.posZ);
        player.addChatMessage(new net.minecraft.util.ChatComponentText("[召之马来]载具召唤成功！"));
        FMLLog.info("[召之马来]%s召唤了%d号载具 (%s)", player.getDisplayName(), index,
                vehicleData.entityName);
        return true;
    }

    /** Kept for packet compatibility; releasing deletes the stored mount. */
    public void spawnSpecificNormalHorseByIndex(int index, EntityPlayer player) {
        SingleVehicle vehicle = getSingleVehicleByIndex(index);
        if (vehicle == null) {
            return;
        }
        if (player != null && vehicle.ent == player.ridingEntity) {
            player.mountEntity(null);
        }
        CallableHorseConfig.logOperation("玩家 %s (UUID: %s) 销毁/释放载具: 栏位 %d, 名称 [%s], 实体UUID: %s",
                player != null ? player.getCommandSenderName() : "未知",
                player != null ? player.getUniqueID().toString() : (puuid != null ? puuid.toString() : "未知"),
                index, vehicle.horseName, vehicle.entityuuid != null ? vehicle.entityuuid.toString() : "无");
        removeVehicle(vehicle, false);
        deleteVehicle(vehicle);
        if (player != null) {
            CallableHorseLevelData.saveData(player.getUniqueID());
            if (player instanceof EntityPlayerMP) {
                CallableHorseLevelData.sendPlayerData((EntityPlayerMP) player);
            }
            player.addChatMessage(new net.minecraft.util.ChatComponentText("[召之马来]载具销毁成功！"));
        }
    }

    public void deleteVehicle(SingleVehicle vehicle) {
        if (vehicle == null) {
            return;
        }
        // Direct callers must receive the same cleanup guarantees as the
        // release packet path: detach riders, snapshot live state and clear
        // UUID tracking before removing the slot.
        if (vehicle.ent != null || vehicle.isUsing) {
            removeVehicle(vehicle, false);
        } else {
            CallableHorseEventHandler.untrackVehicle(vehicle.entityuuid);
        }
        synchronized (vehicles) {
            if (!vehicles.remove(vehicle)) {
                return;
            }
            for (int i = 0; i < vehicles.size(); i++) {
                vehicles.get(i).index = i;
            }
        }
        markDirty();
    }

    public UUID getUUID() {
        return puuid;
    }

    public void writeToNBT(NBTTagCompound nbt) {
        if (nbt == null) {
            return;
        }
        NBTTagCompound data = new NBTTagCompound();
        NBTTagList vehicleTags = new NBTTagList();
        synchronized (vehicles) {
            for (SingleVehicle vehicle : vehicles) {
                if (vehicle == null) {
                    continue;
                }
                NBTTagCompound vehicleTag = new NBTTagCompound();
                vehicle.writeToNBT(vehicleTag);
                vehicleTags.appendTag(vehicleTag);
            }
        }
        data.setTag("Vehicles", vehicleTags);
        nbt.setTag("HorseInfo", data);
    }

    public void readVehiclesFromNBT(NBTTagCompound nbt) {
        List<SingleVehicle> loadedVehicles = Collections.synchronizedList(new ArrayList<SingleVehicle>());
        NBTTagCompound data = nbt == null ? new NBTTagCompound() : nbt.getCompoundTag("HorseInfo");
        NBTTagList vehicleTags = data.getTagList("Vehicles", Constants.NBT.TAG_COMPOUND);
        // Configuration limits new registrations.  Never truncate existing
        // slots when an administrator lowers that limit; otherwise a reload
        // would irreversibly discard data.  The hard cap still bounds memory
        // and network usage for corrupt/hostile files.
        int count = Math.min(vehicleTags.tagCount(), CallableHorseConfig.MAX_STORED_VEHICLES);
        for (int i = 0; i < count; i++) {
            try {
                SingleVehicle vehicle = new SingleVehicle();
                vehicle.readFromNBT(vehicleTags.getCompoundTagAt(i));
                vehicle.index = i;
                vehicle.ownerUUID = puuid;
                vehicle.ent = null;
                if (cpw.mods.fml.common.FMLCommonHandler.instance().getEffectiveSide().isServer()) {
                    if (CallableHorseEventHandler.VehicleEntityAndOwner == null
                            || vehicle.entityuuid == null
                            || !CallableHorseEventHandler.VehicleEntityAndOwner.containsKey(vehicle.entityuuid)) {
                        vehicle.isUsing = false;
                    }
                }
                loadedVehicles.add(vehicle);
            } catch (Throwable throwable) {
                FMLLog.warning("[召之马来]跳过损坏的载具栏位 %d: %s", i, throwable.toString());
            }
        }
        vehicles = loadedVehicles;
        size = getMaxSlots();
    }

    /** Compatibility path for old per-slot packets. */
    public void receiveBasicData(PacketSingleHorseInfo packet) {
        if (packet == null || !CallableHorseConfig.isValidStoredSlotIndex(packet.index)) {
            return;
        }
        SingleVehicle previous = getSingleVehicleByIndex(packet.index);
        SingleVehicle vehicle = new SingleVehicle();
        vehicle.index = packet.index;
        vehicle.horseSpeed = packet.horseSpeed;
        vehicle.health = packet.health;
        vehicle.currentHealth = packet.health;
        vehicle.horseJump = packet.horseJump;
        vehicle.isUsing = packet.isUsing;
        vehicle.isValid = packet.isValid;
        vehicle.horseType = packet.horsetype;
        vehicle.armorid = packet.armortype;
        vehicle.entityuuid = packet.entityuuid;
        vehicle.horseName = CallableHorseMountSupport.safeName(packet.horseName);
        vehicle.variant = packet.variant;
        if (previous != null) {
            vehicle.entityName = previous.entityName;
            vehicle.entityData = previous.entityData == null ? null
                    : (NBTTagCompound) previous.entityData.copy();
        }

        synchronized (vehicles) {
            while (vehicles.size() <= vehicle.index
                    && vehicles.size() < CallableHorseConfig.MAX_STORED_VEHICLES) {
                vehicles.add(new SingleVehicle());
            }
            if (vehicle.index < vehicles.size()) {
                vehicles.set(vehicle.index, vehicle);
            }
        }
    }

    public void sendCallHorseMessage2Server(int index, UUID playerUUID) {
        CallableHorsePacketHandler.networkWrapper.sendToServer(new PacketCallHorse(index, playerUUID));
    }

    public void sendCallBackHorseMessage2Server(int index, UUID playerUUID) {
        CallableHorsePacketHandler.networkWrapper.sendToServer(new PacketCallBackHorse(index, playerUUID));
    }

    public void sendReleaseHorseMessage2Server(int index, UUID playerUUID) {
        CallableHorsePacketHandler.networkWrapper.sendToServer(new PacketReleaseHorse(index, playerUUID));
    }

    private void markDirty() {
        PlayerHorseData data = parentData != null ? parentData : CallableHorseLevelData.getLoadedData(puuid);
        if (data != null) {
            data.markDirty();
        }
    }

    private void markDirtyAndSync() {
        PlayerHorseData playerData = parentData != null ? parentData : CallableHorseLevelData.getLoadedData(puuid);
        if (playerData == null) {
            return;
        }
        playerData.markDirty();
        EntityPlayer player = playerData.getPlayer();
        if (player instanceof EntityPlayerMP) {
            CallableHorseLevelData.sendPlayerData((EntityPlayerMP) player);
        }
    }
}
