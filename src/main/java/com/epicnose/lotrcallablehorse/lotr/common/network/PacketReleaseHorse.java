package com.epicnose.lotrcallablehorse.lotr.common.network;

import com.epicnose.lotrcallablehorse.lotr.common.CallableHorseLevelData;
import com.epicnose.lotrcallablehorse.lotr.common.PlayerHorseData;
import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import lotr.common.util.LOTRLog;
import net.minecraft.entity.player.EntityPlayerMP;

import java.util.UUID;

public class PacketReleaseHorse implements IMessage {
    UUID playeruuid;
    int horseindex;

    public PacketReleaseHorse(){}
    public PacketReleaseHorse(int indexonclient, UUID puuid){
        this.playeruuid=puuid;
        this.horseindex=indexonclient;



    }
    @Override
    public void fromBytes(ByteBuf buf) {
        playeruuid = new UUID(buf.readLong(), buf.readLong());

        horseindex=buf.readInt();

    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeLong(playeruuid.getMostSignificantBits());
        buf.writeLong(playeruuid.getLeastSignificantBits());



        buf.writeInt(horseindex);
    }
    public static class Handler implements IMessageHandler<PacketReleaseHorse, IMessage> {
        @Override
        public IMessage onMessage(PacketReleaseHorse packet, MessageContext context) {
            EntityPlayerMP entityplayer = context.getServerHandler().playerEntity;
            if(!entityplayer.worldObj.isRemote){
                PlayerHorseData phd= CallableHorseLevelData.getData(entityplayer);
                phd.horseInfo.spawnSpecificNormalHorseByIndex(packet.horseindex, entityplayer);   //2023/6/18 不再返回普通马
//                phd.markDirty();
                //同步信息给客户端
//                CallableHorseLevelData.sendPlayerData(entityplayer);
                FMLLog.info(entityplayer.getDisplayName()+" 销毁载具"+packet.horseindex+"号");
//                LOTRLog.logger.info();
//                phd.horseInfo.sendBasicData(entityplayer);
//                if(LOTRLevelData.getData(entityplayer).horseInfo.vehicles.size()==0){ //同步一下客户端
//                    LOTRLevelData.sendPlayerData(entityplayer);
//                }
//                lpd.horseInfo.sendBasicData(entityplayer);
//                lpd.horseInfo.removeVehicle();
            }


            return null;
        }
    }
}
