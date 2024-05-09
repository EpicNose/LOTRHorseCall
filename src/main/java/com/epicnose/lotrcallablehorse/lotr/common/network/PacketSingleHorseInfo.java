package com.epicnose.lotrcallablehorse.lotr.common.network;

import com.epicnose.lotrcallablehorse.lotr.client.CallableHorseClientProxy;
import com.epicnose.lotrcallablehorse.lotr.common.CallableHorseLevelData;
import com.epicnose.lotrcallablehorse.lotr.common.SingleVehicle;
import com.epicnose.lotrcallablehorse.lotr.common.PlayerHorseData;
import com.epicnose.lotrcallablehorse.lotrcallablehorse;
import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import lotr.common.LOTRMod;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;

import java.util.UUID;
//告诉客户端哪些处于使用状态 哪些失效
public class PacketSingleHorseInfo implements IMessage {
    public String horseName;
    public double horseSpeed;
    public double horseJump;
    public double health;
    public int armortype;
    public boolean isValid;

    public boolean isUsing;
    public UUID entityuuid;
    public int horsetype;
    public int variant;

    public int index;
    public PacketSingleHorseInfo(){}
    public PacketSingleHorseInfo(int indexonserver, SingleVehicle sv){
        this.horseName=sv.horseName;
        this.horseSpeed= sv.horseSpeed;
        this.horseJump=sv.horseJump;
        this.health=sv.health;
        this.armortype=sv.armorid;
        this.index=indexonserver;
        this.isUsing=sv.isUsing;
        this.isValid=sv.isValid;
        this.entityuuid=sv.entityuuid;
        this.variant=sv.variant;
    }



    @Override
    public void fromBytes(ByteBuf buf) {
        horseName = ByteBufUtils.readUTF8String(buf);
        horseSpeed = buf.readDouble();
        horseJump = buf.readDouble();
        health = buf.readDouble();
        armortype = buf.readInt();
        isValid = buf.readBoolean();
        isUsing = buf.readBoolean();
        entityuuid = new UUID(buf.readLong(), buf.readLong());
        index=buf.readInt();
        horsetype=buf.readInt();
        variant=buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeUTF8String(buf, horseName);
        buf.writeDouble(horseSpeed);
        buf.writeDouble(horseJump);
        buf.writeDouble(health);
        buf.writeInt(armortype);
        buf.writeBoolean(isValid);
        buf.writeBoolean(isUsing);
        buf.writeLong(entityuuid.getMostSignificantBits());
        buf.writeLong(entityuuid.getLeastSignificantBits());
        buf.writeInt(index);
        buf.writeInt(horsetype);
        buf.writeInt(variant);
    }

    //    @Override
//    public void fromBytes(ByteBuf buf) {
//
//    }
//
//    @Override
//    public void toBytes(ByteBuf buf) {
//
//    }
    public static class Handler implements IMessageHandler<PacketSingleHorseInfo, IMessage> {
        @Override
        public IMessage onMessage(PacketSingleHorseInfo packet, MessageContext context) {
//        World world = LOTRMod.proxy.getClientWorld();
            if(Minecraft.isGuiEnabled() ){
                EntityPlayer entityplayer = lotrcallablehorse.proxy.getClientPlayer();
                PlayerHorseData phd= CallableHorseLevelData.getData(entityplayer);
                phd.horseInfo.receiveBasicData(packet);
            }





            return null;
        }
    }
}
