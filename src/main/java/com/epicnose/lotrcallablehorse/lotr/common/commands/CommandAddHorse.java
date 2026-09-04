package com.epicnose.lotrcallablehorse.lotr.common.commands;

import com.epicnose.lotrcallablehorse.lotr.common.CallableHorseConfig;
import com.epicnose.lotrcallablehorse.lotr.common.CallableHorseEventHandler;
import com.epicnose.lotrcallablehorse.lotr.common.CallableHorseLevelData;
import com.epicnose.lotrcallablehorse.lotr.common.CallableHorseMountSupport;
import com.epicnose.lotrcallablehorse.lotr.common.PlayerHorseData;
import com.epicnose.lotrcallablehorse.lotr.common.SingleVehicle;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChatComponentText;

import java.util.List;

public class CommandAddHorse extends CommandBase {
    private static final double SEARCH_RADIUS = 3.0D;

    @Override
    public String getCommandName() {
        return "addhorse";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/addhorse";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public boolean canCommandSenderUseCommand(ICommandSender sender) {
        return true;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (!(sender instanceof EntityPlayerMP)) {
            throw new WrongUsageException("[召之马来]该命令只能由玩家执行");
        }
        if (args.length != 0) {
            throw new WrongUsageException(getCommandUsage(sender));
        }

        EntityPlayerMP player = (EntityPlayerMP) sender;
        PlayerHorseData playerData = CallableHorseLevelData.getData(player);
        if (playerData == null || playerData.horseInfo == null) {
            player.addChatMessage(new ChatComponentText("[召之马来]载具数据尚未准备好，请稍后再试"));
            return;
        }
        int maxSlots = playerData.getMaxSlots();
        if (playerData.horseInfo.vehicles.size() >= maxSlots) {
            player.addChatMessage(new ChatComponentText(String.format(
                    "[召之马来]载具栏位上限为%d，登记失败", maxSlots)));
            return;
        }

        Entity mount = findMount(player);
        if (mount == null) {
            player.addChatMessage(new ChatComponentText(
                    "[召之马来]请骑乘载具，或站在可骑乘的魔戒载具附近再登记"));
            return;
        }

        if (player.ridingEntity == mount) {
            player.mountEntity(null);
        }
        SingleVehicle vehicle;
        try {
            vehicle = new SingleVehicle(mount);
        } catch (IllegalArgumentException e) {
            player.addChatMessage(new ChatComponentText("[召之马来]该载具没有可恢复的注册信息，无法登记"));
            return;
        }
        vehicle.ownerUUID = player.getUniqueID();
        if (!playerData.horseInfo.addVehicle(vehicle)) {
            player.addChatMessage(new ChatComponentText("[召之马来]载具栏位已满，登记失败"));
            return;
        }

        CallableHorseConfig.logOperation("玩家 %s (UUID: %s) 成功登记载具: [%s] (实体类型: %s, 实体UUID: %s, 槽位: %d/%d) 位置: [维度 %d, X:%.1f, Y:%.1f, Z:%.1f]",
                player.getCommandSenderName(), player.getUniqueID(), vehicle.horseName, mount.getClass().getSimpleName(), mount.getUniqueID(),
                playerData.horseInfo.getVehicleCount(), playerData.getMaxSlots(),
                player.dimension, player.posX, player.posY, player.posZ);
        player.addChatMessage(new ChatComponentText("[召之马来]载具登记成功：" + vehicle.horseName));
        CallableHorseLevelData.saveData(player.getUniqueID());
        CallableHorseLevelData.sendPlayerData(player);
    }

    private static Entity findMount(EntityPlayer player) {
        if (isEligibleMount(player, player.ridingEntity)) {
            return player.ridingEntity;
        }

        List nearbyEntities = player.worldObj.getEntitiesWithinAABB(
                Entity.class, player.boundingBox.expand(SEARCH_RADIUS, SEARCH_RADIUS, SEARCH_RADIUS));
        Entity nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (Object value : nearbyEntities) {
            Entity entity = (Entity) value;
            if (!isEligibleMount(player, entity)) {
                continue;
            }
            double distance = entity.getDistanceSqToEntity(player);
            if (distance < nearestDistance) {
                nearest = entity;
                nearestDistance = distance;
            }
        }
        return nearest;
    }

    private static boolean isEligibleMount(EntityPlayer player, Entity entity) {
        if (player == null || entity == null || entity.worldObj != player.worldObj || entity.isDead
                || !SingleVehicle.isSupportedVehicle(entity)) {
            return false;
        }
        if (CallableHorseEventHandler.VehicleEntityAndOwner.containsKey(entity.getUniqueID())) {
            return false;
        }
        if (CallableHorseMountSupport.belongsToNpc(entity)) {
            return false;
        }
        return entity.riddenByEntity == null || entity.riddenByEntity == player;
    }
}
