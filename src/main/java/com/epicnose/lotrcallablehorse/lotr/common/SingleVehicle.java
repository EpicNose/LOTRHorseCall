package com.epicnose.lotrcallablehorse.lotr.common;

import com.epicnose.lotrcallablehorse.lotr.common.mount.IMountAdapter;
import com.epicnose.lotrcallablehorse.lotr.common.mount.MountAdapterRegistry;
import cpw.mods.fml.common.FMLLog;
import lotr.common.LOTRReflection;
import lotr.common.entity.LOTREntities;
import lotr.common.entity.animal.LOTREntityHorse;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

import java.util.List;
import java.util.UUID;

/** One persistent vehicle slot. The entity's own NBT is the source of truth. */
public class SingleVehicle {
    private static final String ENTITY_DATA_TAG = "EntityData";
    private static final String ENTITY_NAME_TAG = "EntityName";
    private static final String CURRENT_HEALTH_TAG = "CurrentHealth";

    public int index;
    public String horseName = "";
    public double horseSpeed;
    public double horseJump;
    public int horseType;
    public int color;
    public double health;
    public double currentHealth = -1.0D;
    public int armorid;
    public int variant;
    public boolean isValid;
    public boolean isUsing;
    public UUID entityuuid;
    public World world;
    public UUID ownerUUID;
    public Entity ent;

    /** Transient source entity retained until registration commits. */
    private Entity sourceEntity;

    /** Registry id and complete entity NBT make the slot generic across LOTR mounts. */
    public String entityName = "";
    public NBTTagCompound entityData;

    public SingleVehicle() {
    }

    public SingleVehicle(Entity vehicle) {
        if (!isSupportedVehicle(vehicle)) {
            throw new IllegalArgumentException("Entity is not a LOTR rideable mount: " + vehicle);
        }
        captureEntityState(vehicle);
        if (entityData == null || entityName.length() == 0) {
            throw new IllegalArgumentException("LOTR mount has no registered entity id: " + vehicle);
        }
        isValid = true;
        isUsing = false;
        entityuuid = vehicle.getUniqueID();
        world = vehicle.worldObj;
        sourceEntity = vehicle;
    }

    /**
     * Consume the source entity after the slot has been added successfully.
     * Keeping this out of the constructor prevents a full slot from deleting
     * a mount that could not be registered.
     */
    public void detachSourceEntity() {
        Entity source = sourceEntity;
        sourceEntity = null;
        if (source == null || source.isDead) {
            return;
        }
        try {
            if (source.riddenByEntity instanceof EntityPlayer) {
                ((EntityPlayer) source.riddenByEntity).mountEntity(null);
            }
            HorseInfo.closeOpenContainers(source);
            try {
                IMountAdapter adapter = MountAdapterRegistry.getAdapter(source);
                if (adapter != null) {
                    adapter.clearAllEquipment(source);
                }
            } catch (Throwable ignored) {
            }
            source.setDead();
        } catch (Throwable throwable) {
            FMLLog.warning("[召之马来]移除已登记的源载具失败: %s", throwable.toString());
        }
    }

    /**
     * LOTR exposes all of its rideable creatures through LOTRNPCMount. Keep
     * this method as the compatibility entry point for old callers.
     */
    public static boolean isSupportedVehicle(Entity entity) {
        return CallableHorseMountSupport.isSupported(entity);
    }

    /** Refresh the snapshot before removing a live entity from the world. */
    public void captureEntityState(Entity vehicle) {
        if (!isSupportedVehicle(vehicle)) {
            return;
        }

        NBTTagCompound data = new NBTTagCompound();
        try {
            vehicle.writeToNBT(data);
        } catch (Throwable throwable) {
            FMLLog.warning("[召之马来]保存载具 NBT 失败: %s", throwable.toString());
            // Keep an existing slot usable when a third-party mount fails to
            // serialize transient state.  New slots still fail below because
            // no entity id/data is available for construction.
            return;
        }

        entityName = CallableHorseMountSupport.registryName(vehicle, data);
        try {
            if (vehicle.getEntityData() != null) {
                vehicle.getEntityData().removeTag("LOTR_CallableHorse_Owner");
                vehicle.getEntityData().removeTag("LOTR_CallableHorse_Slot");
                vehicle.getEntityData().removeTag("LOTR_CallableHorse_Spawning");
            }
        } catch (Throwable ignored) {
        }
        if (entityName != null && entityName.length() > 0) {
            data.setString("id", entityName);
        }
        CallableHorseMountSupport.sanitizeStoredEntityData(data);
        entityData = data;

        EntityLivingBase living = (EntityLivingBase) vehicle;
        health = finitePositive(CallableHorseMountSupport.maxHealth(living), 20.0D);
        currentHealth = safeHealth(living, health);
        horseSpeed = finiteNonNegative(CallableHorseMountSupport.movementSpeed(living), 0.0D);
        String displayName = "";
        if (living instanceof EntityLiving) {
            displayName = ((EntityLiving) living).getCustomNameTag();
        }
        if (displayName == null || displayName.length() == 0) {
            try {
                displayName = living.getCommandSenderName();
            } catch (Throwable ignored) {
                displayName = entityName;
            }
        }
        horseName = CallableHorseMountSupport.safeName(displayName);

        // Extract attributes dynamically through universal adapter system
        IMountAdapter adapter = MountAdapterRegistry.getAdapter(vehicle);
        horseType = adapter.getMountType(vehicle);
        horseJump = finiteNonNegative(adapter.getJumpStrength(vehicle), 0.0D);
        variant = adapter.getVariant(vehicle);
        ItemStack armor = adapter.getArmor(vehicle);
        armorid = 0;
        if (armor != null && armor.getItem() != null) {
            armorid = Item.getIdFromItem(armor.getItem());
        }
    }

    private static double safeHealth(EntityLivingBase living, double maxHealth) {
        maxHealth = finitePositive(maxHealth, 20.0D);
        double value;
        try {
            value = living.getHealth();
        } catch (Throwable ignored) {
            return maxHealth;
        }
        if (Double.isNaN(value) || Double.isInfinite(value) || value <= 0.0D) {
            return maxHealth;
        }
        return Math.max(1.0D, Math.min(value, maxHealth));
    }

    private static double finitePositive(double value, double fallback) {
        return Double.isNaN(value) || Double.isInfinite(value) || value <= 0.0D
                ? fallback : value;
    }

    private static double finiteNonNegative(double value, double fallback) {
        return Double.isNaN(value) || Double.isInfinite(value) || value < 0.0D
                ? fallback : value;
    }

    public EntityLivingBase createPreviewEntity(World targetWorld) {
        return createVehicle(targetWorld);
    }

    public EntityLivingBase spawnVehicle(EntityPlayer player) {
        if (player == null || player.worldObj == null || !isValid) {
            return null;
        }

        EntityLivingBase vehicle = createVehicle(player.worldObj);
        if (vehicle == null) {
            // A missing/temporarily unavailable LOTR registration must not
            // destroy the persistent slot.  The player can retry after the
            // compatible LOTR jar is loaded.
            return null;
        }

        double restoredHealth = finitePositive(currentHealth, finitePositive(health, 20.0D));
        UUID newVehicleUUID = vehicle.getUniqueID();
        try {
            placeNearPlayer(vehicle, player);
            IMountAdapter adapter = MountAdapterRegistry.getAdapter(vehicle);
            adapter.setTamed(vehicle, player);
            adapter.setSaddled(vehicle, true);
            vehicle.motionX = 0.0D;
            vehicle.motionY = 0.0D;
            vehicle.motionZ = 0.0D;
            vehicle.fallDistance = 0.0F;

            double maxHealth = finitePositive(vehicle.getMaxHealth(), finitePositive(health, 20.0D));
            restoredHealth = finitePositive(currentHealth, maxHealth);
            restoredHealth = Math.max(1.0D, Math.min(restoredHealth, maxHealth));
            vehicle.setHealth((float) Math.min(restoredHealth, Float.MAX_VALUE));

            // 安全放行标记与提前追踪：在实体入界前打上防误杀标记并加入追踪，杜绝被误判为孤儿实体清除
            vehicle.getEntityData().setBoolean("LOTR_CallableHorse_Spawning", true);
            vehicle.getEntityData().setString("LOTR_CallableHorse_Owner", player.getUniqueID().toString());
            vehicle.getEntityData().setInteger("LOTR_CallableHorse_Slot", index);
            CallableHorseEventHandler.trackVehicle(vehicle, player.getUniqueID());

            this.ent = vehicle;
            this.entityuuid = newVehicleUUID;
            this.isUsing = true;

            if (!player.worldObj.isRemote && !player.worldObj.spawnEntityInWorld(vehicle)) {
                CallableHorseEventHandler.untrackVehicle(newVehicleUUID);
                this.ent = null;
                this.isUsing = false;
                vehicle.setDead();
                return null;
            }
        } catch (Throwable throwable) {
            FMLLog.warning("[召之马来]召唤载具失败 %s: %s", entityName, throwable.toString());
            try {
                CallableHorseEventHandler.untrackVehicle(newVehicleUUID);
                this.ent = null;
                this.isUsing = false;
                vehicle.setDead();
            } catch (Throwable ignored) {
                // Best effort cleanup for a partially constructed entity.
            }
            return null;
        }

        isValid = true;
        ownerUUID = player.getUniqueID();
        world = player.worldObj;
        currentHealth = restoredHealth;
        return vehicle;
    }

    private EntityLivingBase createVehicle(World targetWorld) {
        if (targetWorld == null) {
            return null;
        }
        Entity entity = null;
        if (entityData != null && !entityData.hasNoTags()) {
            NBTTagCompound data = (NBTTagCompound) entityData.copy();
            if (entityName != null && entityName.length() > 0) {
                data.setString("id", entityName);
            }
            // Entity NBT may have been captured in another dimension.  The
            // target world supplied to EntityList is authoritative for a new
            // summon, so never let a stale Dimension tag leak across worlds.
            data.removeTag("Dimension");
            CallableHorseMountSupport.sanitizeStoredEntityData(data);
            try {
                entity = EntityList.createEntityFromNBT(data, targetWorld);
            } catch (Throwable throwable) {
                FMLLog.warning("[召之马来]无法从NBT创建载具 %s: %s", entityName, throwable.toString());
            }
            if (entity == null && entityName != null && entityName.length() > 0) {
                try {
                    entity = EntityList.createEntityByName(entityName, targetWorld);
                    if (entity != null) {
                        entity.readFromNBT(data);
                    }
                } catch (Throwable throwable) {
                    FMLLog.warning("[召之马来]按名称保底创建载具 %s 失败: %s", entityName, throwable.toString());
                }
            }
        }

        // alpha-1.0.1 and earlier stored only horse fields.
        if (entity == null && (entityName == null || entityName.length() == 0)) {
            entity = createLegacyHorse(targetWorld);
        }
        if (entity != null && targetWorld != null && targetWorld.provider != null) {
            entity.dimension = targetWorld.provider.dimensionId;
        }
        if (entity != null && !isSupportedVehicle(entity)) {
            FMLLog.warning("[召之马来]注册实体 %s 不是 LOTR 可骑乘载具，保留原槽位数据", entityName);
            try {
                entity.setDead();
            } catch (Throwable ignored) {
                // Ignore cleanup failures from a malformed third-party entity.
            }
            return null;
        }
        return entity instanceof EntityLivingBase ? (EntityLivingBase) entity : null;
    }

    private LOTREntityHorse createLegacyHorse(World targetWorld) {
        LOTREntityHorse horse = (LOTREntityHorse) EntityList.createEntityByName(
                LOTREntities.getStringFromClass(LOTREntityHorse.class), targetWorld);
        if (horse == null) {
            return null;
        }

        double safeHealth = finitePositive(health, 20.0D);
        double safeJump = finiteNonNegative(horseJump, 0.0D);
        double safeSpeed = finiteNonNegative(horseSpeed, 0.0D);
        horse.getEntityAttribute(SharedMonsterAttributes.maxHealth).setBaseValue(safeHealth);
        horse.setHorseType(horseType);
        horse.getEntityAttribute(LOTRReflection.getHorseJumpStrength()).setBaseValue(safeJump);
        horse.getEntityAttribute(SharedMonsterAttributes.movementSpeed).setBaseValue(safeSpeed);
        horse.setCustomNameTag(horseName == null ? "" : horseName);
        horse.setHorseTamed(true);
        horse.setHorseVariant(variant);
        if (armorid != 0) {
            Item armorItem = Item.getItemById(armorid);
            if (armorItem != null) {
                ItemStack armor = new ItemStack(armorItem);
                if (horse.isMountArmorValid(armor)) {
                    horse.setMountArmor(armor);
                }
            }
        }
        horse.setHorseSaddled(true);
        return horse;
    }

    public static void placeNearPlayer(Entity vehicle, EntityPlayer player) {
        double distance = Math.max(2.0D, vehicle.width + 1.0D);
        double[][] offsets = {
                {distance, 0.0D}, {-distance, 0.0D}, {0.0D, distance}, {0.0D, -distance},
                {distance, distance}, {-distance, distance}, {distance, -distance}, {-distance, -distance}
        };
        double[] verticalOffsets = {0.0D, 1.0D, 2.0D};

        for (double verticalOffset : verticalOffsets) {
            for (double[] offset : offsets) {
                vehicle.setLocationAndAngles(player.posX + offset[0], player.posY + verticalOffset,
                        player.posZ + offset[1], player.rotationYaw, 0.0F);
                List collisions = player.worldObj.getCollidingBoundingBoxes(vehicle, vehicle.boundingBox);
                if (collisions.isEmpty() && player.worldObj.checkNoEntityCollision(vehicle.boundingBox)
                        && !player.worldObj.isAnyLiquid(vehicle.boundingBox)) {
                    return;
                }
            }
        }

        vehicle.setLocationAndAngles(player.posX, player.posY + 1.0D, player.posZ, player.rotationYaw, 0.0F);
    }

    public void writeToNBT(NBTTagCompound nbt) {
        nbt.setString("HorseName", CallableHorseMountSupport.safeName(horseName));
        nbt.setDouble("HorseSpeed", finiteNonNegative(horseSpeed, 0.0D));
        nbt.setDouble("HorseJump", finiteNonNegative(horseJump, 0.0D));
        nbt.setDouble("Health", finitePositive(health, 20.0D));
        nbt.setDouble(CURRENT_HEALTH_TAG, finitePositive(currentHealth, -1.0D));
        nbt.setInteger("ArmorId", armorid);
        nbt.setBoolean("isValid", isValid);
        nbt.setBoolean("isUsing", isUsing);
        nbt.setInteger("index", index);
        nbt.setInteger("HorseType", horseType);
        nbt.setInteger("Variant", variant);
        if (entityuuid != null) {
            nbt.setString("entityuuid", entityuuid.toString());
        }
        if (entityName != null && entityName.length() > 0) {
            nbt.setString(ENTITY_NAME_TAG, entityName);
        }
        if (entityData != null && !entityData.hasNoTags()) {
            NBTTagCompound copy = (NBTTagCompound) entityData.copy();
            CallableHorseMountSupport.sanitizeStoredEntityData(copy);
            nbt.setTag(ENTITY_DATA_TAG, copy);
        }
    }

    public void readFromNBT(NBTTagCompound nbt) {
        horseName = CallableHorseMountSupport.safeName(nbt.getString("HorseName"));
        horseSpeed = finiteNonNegative(nbt.getDouble("HorseSpeed"), 0.0D);
        horseJump = finiteNonNegative(nbt.getDouble("HorseJump"), 0.0D);
        health = finitePositive(nbt.getDouble("Health"), 20.0D);
        currentHealth = nbt.hasKey(CURRENT_HEALTH_TAG)
                ? finitePositive(nbt.getDouble(CURRENT_HEALTH_TAG), -1.0D) : -1.0D;
        armorid = nbt.getInteger("ArmorId");
        isValid = !nbt.hasKey("isValid") || nbt.getBoolean("isValid");
        isUsing = nbt.getBoolean("isUsing");
        index = nbt.getInteger("index");
        horseType = nbt.getInteger("HorseType");
        variant = nbt.getInteger("Variant");
        entityuuid = null;
        if (nbt.hasKey("entityuuid")) {
            try {
                entityuuid = UUID.fromString(nbt.getString("entityuuid"));
            } catch (IllegalArgumentException ignored) {
                // Ignore malformed legacy UUIDs; a newly summoned entity gets a fresh UUID.
            }
        }
        entityName = nbt.getString(ENTITY_NAME_TAG);
        entityData = null;
        if (nbt.hasKey(ENTITY_DATA_TAG)) {
            entityData = (NBTTagCompound) nbt.getCompoundTag(ENTITY_DATA_TAG).copy();
            CallableHorseMountSupport.sanitizeStoredEntityData(entityData);
            if (entityName == null || entityName.length() == 0) {
                entityName = entityData.getString("id");
            }
        }
        if (entityName == null) {
            entityName = "";
        }
    }
}
