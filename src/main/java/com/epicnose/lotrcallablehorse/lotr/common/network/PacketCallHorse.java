package com.epicnose.lotrcallablehorse.lotr.common.network;

import com.epicnose.lotrcallablehorse.lotr.common.CallableHorseLevelData;
import com.epicnose.lotrcallablehorse.lotr.common.PlayerHorseData;
import com.epicnose.lotrcallablehorse.lotr.common.CallableHorseServerTasks;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
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
        playeruuid = null;
        horseindex = -1;
        if (buf == null || buf.readableBytes() < 20) {
            return;
        }
        try {
            UUID decoded = new UUID(buf.readLong(), buf.readLong());
            playeruuid = decoded.getMostSignificantBits() == 0L
                    && decoded.getLeastSignificantBits() == 0L ? null : decoded;
            horseindex = buf.readInt();
        } catch (RuntimeException ignored) {
            playeruuid = null;
            horseindex = -1;
        }

    }

    @Override
    public void toBytes(ByteBuf buf) {
        UUID safeOwner = playeruuid == null ? new UUID(0L, 0L) : playeruuid;
        buf.writeLong(safeOwner.getMostSignificantBits());
        buf.writeLong(safeOwner.getLeastSignificantBits());



        buf.writeInt(horseindex);
    }
    public static class Handler implements IMessageHandler<PacketCallHorse, IMessage> {
        @Override
        public IMessage onMessage(PacketCallHorse packet, MessageContext context) {
            if (packet == null || context == null || context.getServerHandler() == null
                    || context.side != Side.SERVER
                    || context.getServerHandler().playerEntity == null) {
                return null;
            }
            final EntityPlayerMP entityplayer = context.getServerHandler().playerEntity;
            if (entityplayer.isDead || entityplayer.worldObj == null || entityplayer.worldObj.isRemote) {
                return null;
            }
            if (!CallableHorsePacketUtil.validIndex(packet.horseindex)) {
                return null;
            }
            PlayerHorseData playerData = CallableHorseLevelData.getData(entityplayer);
            if (playerData != null && playerData.horseInfo != null) {
                playerData.horseInfo.spawnSpecificVehicleByIndex(packet.horseindex, entityplayer);
            }
            return null;
        }
    }

}
