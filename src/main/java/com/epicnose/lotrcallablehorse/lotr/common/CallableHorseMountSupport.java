package com.epicnose.lotrcallablehorse.lotr.common;

import lotr.common.entity.npc.LOTRNPCMount;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.nbt.NBTTagCompound;

/**
 * Compatibility boundary for LOTR mounts.  LOTR deliberately exposes
 * rideable creatures through LOTRNPCMount, so callers must not hard-code
 * concrete horse, warg or spider classes here.
 */
public final class CallableHorseMountSupport {
    public static final int MAX_NAME_LENGTH = 128;

    private CallableHorseMountSupport() {
    }

    public static boolean isSupported(Entity entity) {
        return entity instanceof EntityLivingBase && entity instanceof LOTRNPCMount;
    }

    public static boolean belongsToNpc(Entity entity) {
        if (!(entity instanceof LOTRNPCMount)) {
            return false;
        }
        try {
            return ((LOTRNPCMount) entity).getBelongsToNPC();
        } catch (Throwable ignored) {
            // A broken optional mount must not break command or tick handling.
            return true;
        }
    }

    public static double attributeValue(EntityLivingBase entity,
                                        net.minecraft.entity.ai.attributes.IAttribute attribute,
                                        double fallback) {
        if (entity == null || attribute == null) {
            return fallback;
        }
        try {
            net.minecraft.entity.ai.attributes.IAttributeInstance instance = entity.getEntityAttribute(attribute);
            if (instance == null) {
                return fallback;
            }
            double value = instance.getAttributeValue();
            return Double.isNaN(value) || Double.isInfinite(value) ? fallback : value;
        } catch (Throwable ignored) {
            return fallback;
        }
    }

    public static double maxHealth(EntityLivingBase entity) {
        return Math.max(1.0D, attributeValue(entity, SharedMonsterAttributes.maxHealth, 20.0D));
    }

    public static double movementSpeed(EntityLivingBase entity) {
        return Math.max(0.0D, attributeValue(entity, SharedMonsterAttributes.movementSpeed, 0.0D));
    }

    public static String safeName(String name) {
        if (name == null || name.length() == 0) {
            return "";
        }
        return name.length() <= MAX_NAME_LENGTH ? name : name.substring(0, MAX_NAME_LENGTH);
    }

    public static String registryName(Entity entity, NBTTagCompound data) {
        String name = null;
        try {
            name = net.minecraft.entity.EntityList.getEntityString(entity);
        } catch (Throwable ignored) {
            // Fall through to the NBT id written by Forge/LOTR.
        }
        if ((name == null || name.length() == 0) && data != null && data.hasKey("id")) {
            name = data.getString("id");
        }
        return name == null ? "" : name;
    }

    /** Remove relationship/identity and transient physics data that must never be restored from a slot. */
    public static void sanitizeStoredEntityData(NBTTagCompound data) {
        if (data == null) {
            return;
        }
        data.removeTag("UUIDMost");
        data.removeTag("UUIDLeast");
        data.removeTag("UUID");
        data.removeTag("Dimension");
        data.removeTag("Riding");
        data.removeTag("Passengers");
        data.removeTag("Leash");
        data.removeTag("Pos");
        data.removeTag("Motion");
        data.removeTag("Rotation");
        data.removeTag("FallDistance");
        data.removeTag("Fire");
        data.removeTag("Air");
        data.removeTag("OnGround");
        data.removeTag("HurtTime");
        data.removeTag("DeathTime");
        data.removeTag("HurtByTimestamp");
        data.removeTag("PortalCooldown");
        data.removeTag("InLove");

        // Clean any stale ForgeData tracking tags so stored mounts never carry transient tracking tags
        if (data.hasKey("ForgeData")) {
            NBTTagCompound forgeData = data.getCompoundTag("ForgeData");
            forgeData.removeTag("LOTR_CallableHorse_Owner");
            forgeData.removeTag("LOTR_CallableHorse_Slot");
            forgeData.removeTag("LOTR_CallableHorse_Spawning");
        }
    }
}
