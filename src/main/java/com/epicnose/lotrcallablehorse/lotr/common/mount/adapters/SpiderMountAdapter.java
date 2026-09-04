package com.epicnose.lotrcallablehorse.lotr.common.mount.adapters;

import com.epicnose.lotrcallablehorse.lotr.common.SingleVehicle;
import com.epicnose.lotrcallablehorse.lotr.common.mount.IMountAdapter;
import lotr.common.entity.npc.LOTREntityNPCRideable;
import lotr.common.entity.npc.LOTRNPCMount;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * 适配器：覆盖魔戒全阵营巨型蜘蛛（幽暗密林蜘蛛、魔多蜘蛛、乌图诺冰蜘蛛等）。
 */
public class SpiderMountAdapter implements IMountAdapter {

    @Override
    public boolean matches(Entity entity) {
        if (entity == null) return false;
        return (entity instanceof LOTREntityNPCRideable || entity instanceof LOTRNPCMount)
                && entity.getClass().getSimpleName().contains("Spider");
    }

    @Override
    public String getCategory() {
        return "spider";
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
            Method m = entity.getClass().getMethod("setSpiderSaddled", boolean.class);
            m.invoke(entity, saddled);
        } catch (Throwable ignored) {
        }
    }

    @Override
    public ItemStack getArmor(Entity entity) {
        return null; // 蜘蛛无铠甲槽位
    }

    @Override
    public void setArmor(Entity entity, ItemStack armor) {
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
        return 0.0D;
    }

    @Override
    public int getVariant(Entity entity) {
        return 0;
    }

    @Override
    public int getMountType(Entity entity) {
        if (entity == null) return 0;
        try {
            Method m = entity.getClass().getMethod("getSpiderType");
            Object type = m.invoke(entity);
            return type instanceof Integer ? (Integer) type : 0;
        } catch (Throwable ignored) {
            return 0;
        }
    }

    @Override
    public float getRenderScaleMultiplier(Entity entity) {
        return 0.9F;
    }

    @Override
    public float getRenderYOffset(Entity entity) {
        return 0.0F;
    }

    @Override
    public void clearAllEquipment(Entity entity) {
        if (entity == null) return;
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
        lines.add("巨蛛名称: " + vehicle.horseName);
        lines.add("生命上限: " + String.format("%.1f", vehicle.health));
        lines.add("爬行速度: " + String.format("%.2f", vehicle.horseSpeed));
        lines.add("特殊能力: 墙壁攀爬");
        return lines;
    }
}
