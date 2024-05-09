package com.epicnose.lotrcallablehorse.lotr.common.network;

import com.epicnose.lotrcallablehorse.lotr.common.CallableHorseLevelData;
import com.epicnose.lotrcallablehorse.lotr.common.PlayerHorseData;
import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;

import java.util.UUID;

public class PacketCallBackHorse implements IMessage {
    UUID playeruuid;
    int horseindex;
    public PacketCallBackHorse(){}

    public PacketCallBackHorse(int indexonclient, UUID puuid){
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


    public static class Handler implements IMessageHandler<PacketCallBackHorse, IMessage> {


        @Override
        public IMessage onMessage(PacketCallBackHorse message, MessageContext ctx) {
            EntityPlayerMP entityplayer = ctx.getServerHandler().playerEntity;
            if(!entityplayer.worldObj.isRemote){
                PlayerHorseData phd= CallableHorseLevelData.getData(entityplayer);
                phd.horseInfo.removeVehicle(phd.horseInfo.getIsUsing());
//                FMLLog.info("接到了召回包");
            }
            return null;
        }
    }
}

