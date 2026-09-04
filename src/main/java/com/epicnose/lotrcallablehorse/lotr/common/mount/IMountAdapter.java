package com.epicnose.lotrcallablehorse.lotr.common.mount;

import com.epicnose.lotrcallablehorse.lotr.common.SingleVehicle;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

import java.util.List;

/**
 * 全系可骑乘生物通用适配器接口。
 * 解耦具体的实体类类型，抹平动物系（马、鹿、象等）与 NPC 载具系（座狼、巨蛛）的接口差异。
 */
public interface IMountAdapter {
    /** 判定该适配器是否适用于此实体 */
    boolean matches(Entity entity);

    /** 类别标识 (如 "horse", "warg", "spider", "mumakil", "generic") */
    String getCategory();

    /** 坐骑是否装备了鞍具 */
    boolean isSaddled(Entity entity);

    /** 设置鞍具状态 */
    void setSaddled(Entity entity, boolean saddled);

    /** 获取护甲物品栈 */
    ItemStack getArmor(Entity entity);

    /** 设置护甲 */
    void setArmor(Entity entity, ItemStack armor);

    /** 实体是否已被驯服 */
    boolean isTamed(Entity entity);

    /** 驯服坐骑 */
    void setTamed(Entity entity, EntityPlayer player);

    /** 获取背包（若有） */
    IInventory getInventory(Entity entity);

    /** 跳跃能力属性值 */
    double getJumpStrength(Entity entity);

    /** 变种代码 */
    int getVariant(Entity entity);

    /** 专属类型代码 */
    int getMountType(Entity entity);

    /** GUI 模型渲染缩放倍率 (战象缩小, 矮种马放大) */
    float getRenderScaleMultiplier(Entity entity);

    /** GUI 模型渲染 Y 轴偏移量 */
    float getRenderYOffset(Entity entity);

    /** 清空坐骑所有装备与背包道具（用于死亡或收回逻辑） */
    void clearAllEquipment(Entity entity);

    /** 用于在 GUI 界面中格式化展示的多态属性列表 */
    List<String> getDisplayStats(SingleVehicle vehicle);
}
