package com.epicnose.lotrcallablehorse.lotr.common.network;

import com.epicnose.lotrcallablehorse.lotr.common.CallableHorseLevelData;
import com.epicnose.lotrcallablehorse.lotr.common.PlayerHorseData;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

import net.minecraft.entity.player.EntityPlayerMP;

import java.util.UUID;

public class PacketCallHorse implements IMessage {
    UUID playeruuid;
    int horseindex;
    public PacketCallHorse(){}
    public PacketCallHorse(int indexonclient, UUID puuid){
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
    public static class Handler implements IMessageHandler<PacketCallHorse, IMessage> {
        @Override
        public IMessage onMessage(PacketCallHorse packet, MessageContext context) {
            EntityPlayerMP entityplayer = context.getServerHandler().playerEntity;
            if(!entityplayer.worldObj.isRemote){
                PlayerHorseData phd= CallableHorseLevelData.getData(entityplayer);
                phd.horseInfo.spawnSpecificVehicleByIndex(packet.horseindex, entityplayer);

            }


            return null;
        }
    }
}
