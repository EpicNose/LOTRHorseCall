package com.epicnose.lotrcallablehorse.lotr.common.mount.adapters;

import com.epicnose.lotrcallablehorse.lotr.common.SingleVehicle;
import com.epicnose.lotrcallablehorse.lotr.common.mount.IMountAdapter;
import lotr.common.entity.npc.LOTREntityNPCRideable;
import lotr.common.entity.npc.LOTRNPCMount;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * 适配器：覆盖魔戒全阵营座狼（安格玛、刚达巴、魔多、艾辛格、乌图诺全系列及投弹座狼）。
 * 动态反射调用座狼专属 API，彻底兼容传承版与重制版不同子包路径。
 */
public class WargMountAdapter implements IMountAdapter {

    @Override
    public boolean matches(Entity entity) {
        if (entity == null) return false;
        return (entity instanceof LOTREntityNPCRideable || entity instanceof LOTRNPCMount)
                && entity.getClass().getSimpleName().contains("Warg");
    }

    @Override
    public String getCategory() {
        return "warg";
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
        if (entity == null) return;
        try {
            Method m = entity.getClass().getMethod("setWargSaddled", boolean.class);
            m.invoke(entity, saddled);
        } catch (Throwable ignored) {
        }
    }

    @Override
    public ItemStack getArmor(Entity entity) {
        if (entity == null) return null;
        try {
            Method m = entity.getClass().getMethod("getWargArmor");
            Object res = m.invoke(entity);
            return res instanceof ItemStack ? (ItemStack) res : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    @Override
    public void setArmor(Entity entity, ItemStack armor) {
        if (entity == null) return;
        try {
            Method m = entity.getClass().getMethod("setWargArmor", ItemStack.class);
            m.invoke(entity, armor);
        } catch (Throwable ignored) {
        }
    }

    @Override
    public boolean isTamed(Entity entity) {
        if (entity instanceof LOTREntityNPCRideable) {
            return ((LOTREntityNPCRideable) entity).isNPCTamed();
        }
        return false;
    }

    @Override
    public void setTamed(Entity entity, EntityPlayer player) {
        if (entity instanceof LOTREntityNPCRideable) {
            LOTREntityNPCRideable rideable = (LOTREntityNPCRideable) entity;
            rideable.setNPCTamed(true);
            if (player != null) {
                try {
                    rideable.tameNPC(player);
                } catch (Throwable ignored) {
                }
            }
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
        return 0.0D; // 座狼在魔戒中没有马匹跳跃蓄力槽
    }

    @Override
    public int getVariant(Entity entity) {
        return 0;
    }

    @Override
    public int getMountType(Entity entity) {
        if (entity == null) return 0;
        try {
            Method m = entity.getClass().getMethod("getWargType");
            Object type = m.invoke(entity);
            return type != null ? type.hashCode() : 0;
        } catch (Throwable ignored) {
            return 0;
        }
    }

    @Override
    public float getRenderScaleMultiplier(Entity entity) {
        return 1.05F;
    }

    @Override
    public float getRenderYOffset(Entity entity) {
        return 0.0F;
    }

    @Override
    public void clearAllEquipment(Entity entity) {
        if (entity == null) return;
        setArmor(entity, null);
        setSaddled(entity, false);
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
        lines.add("座狼名称: " + vehicle.horseName);
        lines.add("生命上限: " + String.format("%.1f", vehicle.health));
        lines.add("奔跑速度: " + String.format("%.2f", vehicle.horseSpeed));
        lines.add("特有能力: 狼嚎与威慑");
        if (vehicle.armorid > 0) {
            Item item = Item.getItemById(vehicle.armorid);
            if (item != null) {
                lines.add("座狼铠甲: " + new ItemStack(item).getDisplayName());
            }
        }
        return lines;
    }
}
