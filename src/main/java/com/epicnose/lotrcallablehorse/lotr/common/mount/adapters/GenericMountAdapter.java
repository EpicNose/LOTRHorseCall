package com.epicnose.lotrcallablehorse.lotr.common.mount.adapters;

import com.epicnose.lotrcallablehorse.lotr.common.SingleVehicle;
import com.epicnose.lotrcallablehorse.lotr.common.mount.IMountAdapter;
import lotr.common.entity.npc.LOTREntityNPCRideable;
import lotr.common.entity.npc.LOTRNPCMount;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * 通用兜底适配器：针对任意实现 LOTRNPCMount 的未知或自定义载具提供安全支持。
 */
public class GenericMountAdapter implements IMountAdapter {

    @Override
    public boolean matches(Entity entity) {
        return entity instanceof LOTRNPCMount;
    }

    @Override
    public String getCategory() {
        return "generic";
    }

    @Override
    public boolean isSaddled(Entity entity) {
        if (entity instanceof LOTRNPCMount) {
            return ((LOTRNPCMount) entity).isMountSaddled();
        }
        return false;
    }

    @Override
    public void setSaddled(Entity entity, boolean saddled) {
    }

    @Override
    public ItemStack getArmor(Entity entity) {
        return null;
    }

    @Override
    public void setArmor(Entity entity, ItemStack armor) {
    }

    @Override
    public boolean isTamed(Entity entity) {
        if (entity instanceof LOTREntityNPCRideable) {
            return ((LOTREntityNPCRideable) entity).isNPCTamed();
        }
        return true;
    }

    @Override
    public void setTamed(Entity entity, EntityPlayer player) {
        if (entity instanceof LOTREntityNPCRideable) {
            ((LOTREntityNPCRideable) entity).setNPCTamed(true);
        }
    }

    @Override
    public IInventory getInventory(Entity entity) {
        if (entity instanceof LOTREntityNPCRideable) {
            return ((LOTREntityNPCRideable) entity).getMountInventory();
        }
        return null;
    }

    @Override
    public double getJumpStrength(Entity entity) {
        return 0.0D;
    }

    @Override
    public int getVariant(Entity entity) {
        return 0;
    }

    @Override
    public int getMountType(Entity entity) {
        return 0;
    }

    @Override
    public float getRenderScaleMultiplier(Entity entity) {
        return 1.0F;
    }

    @Override
    public float getRenderYOffset(Entity entity) {
        return 0.0F;
    }

    @Override
    public void clearAllEquipment(Entity entity) {
        IInventory inv = getInventory(entity);
        if (inv != null) {
            try {
                for (int i = 0; i < inv.getSizeInventory(); i++) {
                    inv.setInventorySlotContents(i, null);
                }
                inv.markDirty();
            } catch (Throwable ignored) {
            }
        }
    }

    @Override
    public List<String> getDisplayStats(SingleVehicle vehicle) {
        List<String> lines = new ArrayList<String>();
        if (vehicle == null) return lines;
        lines.add("载具名称: " + vehicle.horseName);
        lines.add("生命上限: " + String.format("%.1f", vehicle.health));
        lines.add("移动速度: " + String.format("%.2f", vehicle.horseSpeed));
        return lines;
    }
}
