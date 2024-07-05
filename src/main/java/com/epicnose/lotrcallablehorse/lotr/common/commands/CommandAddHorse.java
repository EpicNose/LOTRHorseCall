package com.epicnose.lotrcallablehorse.lotr.common.commands;

import com.epicnose.lotrcallablehorse.lotr.common.CallableHorseEventHandler;
import com.epicnose.lotrcallablehorse.lotr.common.CallableHorseLevelData;
import com.epicnose.lotrcallablehorse.lotr.common.PlayerHorseData;
import com.epicnose.lotrcallablehorse.lotr.common.SingleVehicle;
import lotr.common.LOTREventHandler;
import lotr.common.entity.animal.LOTREntityHorse;
import lotr.common.entity.npc.LOTREntityTroll;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.EntityHorse;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.MathHelper;
import net.minecraft.world.chunk.Chunk;

import java.util.List;

public class CommandAddHorse extends CommandBase {


    @Override
    public String getCommandName() {
        return "addhorse";
    }

    @Override
    public String getCommandUsage(ICommandSender p_71518_1_) {
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
    public void processCommand(ICommandSender sender, String[] p_71515_2_) {
        // 获取玩家的位置
        if(sender instanceof EntityPlayer){
            EntityPlayer p=(EntityPlayer) sender;
            PlayerHorseData lpd= CallableHorseLevelData.getData(p);
            List<Entity> nearbyHorses = p.worldObj.getEntitiesWithinAABB(LOTREntityHorse.class, p.boundingBox.expand(1.5, 1.5, 1.5));


            LOTREntityHorse nearestHorse = null;
            double nearestDistance = Double.MAX_VALUE;

            // 遍历找到最近的马
            for (Entity entity : nearbyHorses) {
                if (entity instanceof LOTREntityHorse) {
                    double distance = entity.getDistance(p.posX, p.posY, p.posZ);
                    if (distance < nearestDistance) {
                        nearestHorse = (LOTREntityHorse) entity;
                        nearestDistance = distance;
                    }
                }
            }

            // 将最近的马添加到列表中
            if (nearestHorse != null) {
                // 在这里添加将马添加到列表的代码，这可能涉及到文件操作或者与其他模组的交互
//                sender.sendMessage(new TextComponentString("最近的马已经添加到列表中！"));
                if(CallableHorseEventHandler.VehicleEntityAndOwner.containsKey(nearestHorse.getUniqueID())) {
//                    p.sendMessage("该生物疑似他人坐骑，请换个生物！");
                    p.addChatMessage(new ChatComponentText("[召之马来]该生物疑似他人坐骑，请换个生物！"));
                    return;

                }
                if(!(lpd.horseInfo.vehicles.size()<3)) {
                    p.addChatMessage(new ChatComponentText("[召之马来]玩家载具栏位为3，登记失败!"));
                    return;
                }
//                net.minecraft.entity.Entity forgeent=(net.minecraft.entity.Entity) (Object)((org.bukkit.craftbukkit.v1_7_R4.entity.CraftEntity)ent).getHandle();
//                if(forgeent instanceof LOTREntityHorse) {
//                    LOTREntityHorse horse=(LOTREntityHorse)forgeent;


                    SingleVehicle sv=new SingleVehicle(nearestHorse);
                    sv.ownerUUID=p.getUniqueID();
                    lpd.horseInfo.addVehicle(sv);
//                    nearestHorse;
                    Chunk chunk = nearestHorse.worldObj.getChunkFromChunkCoords(MathHelper.floor_double(nearestHorse.posX) >> 4,MathHelper.floor_double(nearestHorse.posZ) >> 4);
                    chunk.removeEntity(nearestHorse);
                    p.addChatMessage(new ChatComponentText("[召之马来]载具登记成功"));
                CallableHorseLevelData.saveData(p.getUniqueID());
                CallableHorseLevelData.sendPlayerData((EntityPlayerMP) p); //同步一下信息
//                    CallableHorseLevelData.sendPlayerData((EntityPlayerMP) p);
//                    CallableHorseLevelData.getData(sv.ownerUUID).markDirty();
                    return;
                }



            } else {
               throw new WrongUsageException("[招之马来]请以玩家实体身份执行本命令");
            }
        }


}

