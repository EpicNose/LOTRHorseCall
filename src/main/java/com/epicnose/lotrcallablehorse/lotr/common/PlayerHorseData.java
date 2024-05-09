package com.epicnose.lotrcallablehorse.lotr.common;

import com.epicnose.lotrcallablehorse.lotr.common.network.CallableHorsePacketHandler;
import com.epicnose.lotrcallablehorse.lotr.common.network.PacketLoginPlayerHorseData;
import lotr.common.LOTRDimension;
import lotr.common.LOTRLevelData;
import lotr.common.LOTRMod;
import lotr.common.LOTRPlayerData;
import lotr.common.command.LOTRCommandAdminHideMap;
import lotr.common.fac.LOTRFaction;
import lotr.common.network.LOTRPacketLoginPlayerData;
import lotr.common.quest.LOTRMiniQuest;
import lotr.common.util.LOTRLog;
import lotr.common.world.LOTRWorldProvider;
import lotr.common.world.biome.LOTRBiome;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PlayerHorseData {
    public HorseInfo horseInfo;
    public UUID playerUUID;
    public boolean needsSave = false;
    public UUID getPlayerUUID() {
        return playerUUID;
    }
    public void markDirty() {
        needsSave = true;
    }
    public int pdTick = 0;
    public boolean needsSave() {
        return needsSave;
    }

    public PlayerHorseData(UUID uuid) {
        playerUUID = uuid;
    }

    public EntityPlayer getPlayer() {
        World[] searchWorlds = LOTRMod.proxy.isClient() ? new World[] { LOTRMod.proxy.getClientWorld() } : MinecraftServer.getServer().worldServers;
        for (World world : searchWorlds) {
            EntityPlayer entityplayer = world.func_152378_a(playerUUID);
            if (entityplayer == null) {
                continue;
            }
            return entityplayer;
        }
        return null;
    }
    public void save(NBTTagCompound playerData) {
        needsSave = false;
        if(horseInfo!=null){
            horseInfo.writeToNBT(playerData);//存储玩家的HorseInfo
        }


    }
    public void load(NBTTagCompound playerData) {
        if(playerData.hasKey("HorseInfo")){
//			LOTRLog.logger.info("读取到了horseinfo");
//			this.horseInfo=new HorseInfo(this.getPlayerUUID());
            if(this.horseInfo!=null){
                this.horseInfo.readVehiclesFromNBT(playerData);

//				LOTRLog.logger.info("读取到了horseinfo然后其中vehicle"+this.horseInfo.vehicles.size());


//				this.horseInfo.vehicles.size();
//				horseInfo.
            }else{
                this.horseInfo=new HorseInfo(this.getPlayerUUID());
                this.horseInfo.readVehiclesFromNBT(playerData);
            }
        }else{
            this.horseInfo=new HorseInfo(this.getPlayerUUID());
//			LOTRLog.logger.info("没有horseinfo");
        }
    }
    public void sendPlayerData(EntityPlayerMP entityplayer) throws IOException {
        NBTTagCompound nbt = new NBTTagCompound();
        save(nbt);
        PacketLoginPlayerHorseData packet = new PacketLoginPlayerHorseData(nbt);
        CallableHorsePacketHandler.networkWrapper.sendTo(packet, entityplayer);
    }
    public void onUpdate(EntityPlayerMP entityplayer, WorldServer world) {
        ++pdTick;
//        if(pdTick>10){
            if(horseInfo!=null){
//			horseInfo.lpd=this;
                this.horseInfo.onUpdate();
//                this.horseInfo.lpd =this;
            }
//            pdTick=0;
//        }


//		LOTRLevelData.sendAllPlayerDataToLoginPlayer(entityplayer,entityplayer.worldObj);
//		LOTRLevelData.sendAllPlayerDataToLoginPlayer();
    }


}
