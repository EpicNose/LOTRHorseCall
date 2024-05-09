package com.epicnose.lotrcallablehorse.lotr.common.network;

import com.epicnose.lotrcallablehorse.lotr.common.CallableHorseLevelData;
import com.epicnose.lotrcallablehorse.lotr.common.PlayerHorseData;
import com.epicnose.lotrcallablehorse.lotrcallablehorse;
import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import lotr.common.LOTRMod;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;

import java.io.IOException;

public class PacketLoginPlayerHorseData implements IMessage{
    public NBTTagCompound playerData;
    public PacketLoginPlayerHorseData() {
    }

    public PacketLoginPlayerHorseData(NBTTagCompound nbt) {
        this.playerData = nbt;
    }


    @Override
    public void fromBytes(ByteBuf data) {
        try {
            playerData = new PacketBuffer(data).readNBTTagCompoundFromBuffer();
        } catch (IOException e) {
            FMLLog.severe("Error reading CallableHorse login player data");
            e.printStackTrace();
        }
    }

    @Override
    public void toBytes(ByteBuf data) {
        try {
            new PacketBuffer(data).writeNBTTagCompoundToBuffer(playerData);
        } catch (IOException e) {
            FMLLog.severe("Error writing LOTR login player data");
            e.printStackTrace();
        }
    }

    public static class Handler implements IMessageHandler<PacketLoginPlayerHorseData, IMessage> {
        @Override
        public IMessage onMessage(PacketLoginPlayerHorseData packet, MessageContext context) {
            NBTTagCompound nbt = packet.playerData;
            EntityPlayer entityplayer = lotrcallablehorse.proxy.getClientPlayer();
            PlayerHorseData pd = CallableHorseLevelData.getData(entityplayer);
            if (!lotrcallablehorse.proxy.isSingleplayer()) {
                pd.load(nbt);
            }
//            LOTRMod.proxy.setWaypointModes(pd.showWaypoints(), pd.showCustomWaypoints(), pd.showHiddenSharedWaypoints());
            return null;
        }
    }
}
