package com.epicnose.lotrcallablehorse.lotr.common.mount.adapters;

import com.epicnose.lotrcallablehorse.lotr.common.CallableHorseMountSupport;
import com.epicnose.lotrcallablehorse.lotr.common.SingleVehicle;
import com.epicnose.lotrcallablehorse.lotr.common.mount.IMountAdapter;
import lotr.common.LOTRReflection;
import lotr.common.entity.animal.LOTREntityHorse;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * 适配器：覆盖魔戒所有继承自 LOTREntityHorse 的动物系坐骑。
 * 包含：战马、夏尔矮种马、斑马、近哈拉德骆驼、大角鹿、野猪、犀牛、长颈鹿，
 * 以及重制版新增的战斗猛犸象 (Mumakil)、野生猛犸象与战羊 (Ram)。
 */
public class HorseMountAdapter implements IMountAdapter {

    @Override
    public boolean matches(Entity entity) {
        return entity instanceof LOTREntityHorse;
    }

    @Override
    public String getCategory() {
        return "horse";
    }

    private boolean isMumakil(Entity entity) {
        return entity != null && entity.getClass().getSimpleName().contains("Mumakil");
    }

    private boolean isShirePony(Entity entity) {
        return entity != null && entity.getClass().getSimpleName().contains("ShirePony");
    }

    @Override
    public boolean isSaddled(Entity entity) {
        if (entity instanceof LOTREntityHorse) {
            return ((LOTREntityHorse) entity).isMountSaddled();
        }
        return false;
    }

    @Override
    public void setSaddled(Entity entity, boolean saddled) {
        if (entity instanceof LOTREntityHorse) {
            try {
                ((LOTREntityHorse) entity).setHorseSaddled(saddled);
            } catch (Throwable ignored) {
            }
        }
    }

    @Override
    public ItemStack getArmor(Entity entity) {
        if (entity instanceof LOTREntityHorse) {
            return ((LOTREntityHorse) entity).getMountArmor();
        }
        return null;
    }

    @Override
    public void setArmor(Entity entity, ItemStack armor) {
        if (entity instanceof LOTREntityHorse) {
            try {
                ((LOTREntityHorse) entity).setMountArmor(armor);
            } catch (Throwable ignored) {
            }
        }
    }

    @Override
    public boolean isTamed(Entity entity) {
        if (entity instanceof LOTREntityHorse) {
            return ((LOTREntityHorse) entity).isTame();
        }
        return false;
    }

    @Override
    public void setTamed(Entity entity, EntityPlayer player) {
        if (entity instanceof LOTREntityHorse) {
            LOTREntityHorse horse = (LOTREntityHorse) entity;
            horse.setHorseTamed(true);
            if (player != null) {
                try {
                    horse.func_152120_b(player.getUniqueID().toString());
                } catch (Throwable ignored) {
                }
            }
        }
    }

    @Override
    public IInventory getInventory(Entity entity) {
        if (entity instanceof LOTREntityHorse) {
            try {
                return LOTRReflection.getHorseInv((LOTREntityHorse) entity);
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    @Override
    public double getJumpStrength(Entity entity) {
        if (entity instanceof LOTREntityHorse) {
            return CallableHorseMountSupport.attributeValue(
                    (LOTREntityHorse) entity, LOTRReflection.getHorseJumpStrength(), 0.0D);
        }
        return 0.0D;
    }

    @Override
    public int getVariant(Entity entity) {
        if (entity instanceof LOTREntityHorse) {
            return ((LOTREntityHorse) entity).getHorseVariant();
        }
        return 0;
    }

    @Override
    public int getMountType(Entity entity) {
        if (entity instanceof LOTREntityHorse) {
            return ((LOTREntityHorse) entity).getHorseType();
        }
        return 0;
    }

    @Override
    public float getRenderScaleMultiplier(Entity entity) {
        if (isMumakil(entity)) {
            return 0.35F; // 战象体型巨大，深度缩放防止超出界面
        }
        if (isShirePony(entity)) {
            return 1.25F; // 夏尔矮种马体型较小，适度放大
        }
        return 1.0F;
    }

    @Override
    public float getRenderYOffset(Entity entity) {
        if (isMumakil(entity)) {
            return 12.0F; // 战象向下微调，防止象牙遮挡标题
        }
        return 0.0F;
    }

    @Override
    public void clearAllEquipment(Entity entity) {
        if (!(entity instanceof LOTREntityHorse)) {
            return;
        }
        LOTREntityHorse horse = (LOTREntityHorse) entity;
        try {
            horse.setMountArmor(null);
            horse.setHorseSaddled(false);
        } catch (Throwable ignored) {
        }
        try {
            IInventory inv = LOTRReflection.getHorseInv(horse);
            if (inv != null) {
                for (int i = 0; i < inv.getSizeInventory(); i++) {
                    inv.setInventorySlotContents(i, null);
                }
                inv.markDirty();
            }
        } catch (Throwable ignored) {
        }
    }

    @Override
    public List<String> getDisplayStats(SingleVehicle vehicle) {
        List<String> lines = new ArrayList<String>();
        if (vehicle == null) {
            return lines;
        }
        boolean mumakil = vehicle.entityName != null && vehicle.entityName.contains("Mumakil");
        lines.add((mumakil ? "巨兽名称: " : "载具名称: ") + vehicle.horseName);
        lines.add("生命上限: " + String.format("%.1f", vehicle.health));
        lines.add("移动速度: " + String.format("%.2f", vehicle.horseSpeed));
        if (vehicle.horseJump > 0.0D) {
            lines.add("跳跃能力: " + String.format("%.2f", vehicle.horseJump));
        }
        if (vehicle.armorid > 0) {
            Item item = Item.getItemById(vehicle.armorid);
            if (item != null) {
                lines.add("装配护甲: " + new ItemStack(item).getDisplayName());
            }
        }
        if (vehicle.variant > 0 && !mumakil) {
            lines.add("变种编码: " + vehicle.variant);
        }
        return lines;
    }
}
