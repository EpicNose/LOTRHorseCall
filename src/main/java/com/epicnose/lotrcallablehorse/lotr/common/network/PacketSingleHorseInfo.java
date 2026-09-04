package com.epicnose.lotrcallablehorse.lotr.common.network;

import com.epicnose.lotrcallablehorse.lotr.common.SingleVehicle;
import com.epicnose.lotrcallablehorse.lotrcallablehorse;
import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import io.netty.buffer.ByteBuf;

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
        if (sv == null) {
            return;
        }
        this.horseName=CallableHorsePacketUtil.safeName(sv.horseName);
        this.horseSpeed= sv.horseSpeed;
        this.horseJump=sv.horseJump;
        this.health=sv.health;
        this.armortype=sv.armorid;
        this.index=indexonserver;
        this.isUsing=sv.isUsing;
        this.isValid=sv.isValid;
        this.entityuuid=sv.entityuuid;
        this.variant=sv.variant;
        this.horsetype=sv.horseType;
    }



    @Override
    public void fromBytes(ByteBuf buf) {
        horseName = "";
        horseSpeed = Double.NaN;
        horseJump = Double.NaN;
        health = Double.NaN;
        armortype = 0;
        isValid = false;
        isUsing = false;
        entityuuid = null;
        index = -1;
        horsetype = 0;
        variant = 0;
        if (buf == null) {
            return;
        }
        try {
            horseName = CallableHorsePacketUtil.safeName(ByteBufUtils.readUTF8String(buf));
            // Three doubles, armor id, two flags, UUID, index, horse type and
            // variant remain after the variable-length name.
            if (buf.readableBytes() < 58) {
                horseName = "";
                return;
            }
            horseSpeed = buf.readDouble();
            horseJump = buf.readDouble();
            health = buf.readDouble();
            armortype = buf.readInt();
            isValid = buf.readBoolean();
            isUsing = buf.readBoolean();
            UUID decodedUUID = new UUID(buf.readLong(), buf.readLong());
            entityuuid = decodedUUID.getMostSignificantBits() == 0L
                    && decodedUUID.getLeastSignificantBits() == 0L ? null : decodedUUID;
            index = buf.readInt();
            horsetype = buf.readInt();
            variant = buf.readInt();
        } catch (RuntimeException ignored) {
            horseName = "";
            horseSpeed = Double.NaN;
            horseJump = Double.NaN;
            health = Double.NaN;
            index = -1;
            entityuuid = null;
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeUTF8String(buf, CallableHorsePacketUtil.safeName(horseName));
        buf.writeDouble(horseSpeed);
        buf.writeDouble(horseJump);
        buf.writeDouble(health);
        buf.writeInt(armortype);
        buf.writeBoolean(isValid);
        buf.writeBoolean(isUsing);
        UUID safeUUID = entityuuid == null ? new UUID(0L, 0L) : entityuuid;
        buf.writeLong(safeUUID.getMostSignificantBits());
        buf.writeLong(safeUUID.getLeastSignificantBits());
        buf.writeInt(index);
        buf.writeInt(horsetype);
        buf.writeInt(variant);
    }

    public static class Handler implements IMessageHandler<PacketSingleHorseInfo, IMessage> {
        @Override
        public IMessage onMessage(PacketSingleHorseInfo packet, MessageContext context) {
            if (packet == null || !CallableHorsePacketUtil.validIndex(packet.index)
                    || context == null || context.side != Side.CLIENT
                    || lotrcallablehorse.proxy == null
                    || !CallableHorsePacketUtil.finite(packet.horseSpeed)
                    || !CallableHorsePacketUtil.finite(packet.horseJump)
                    || !CallableHorsePacketUtil.finite(packet.health)) {
                return null;
            }
            lotrcallablehorse.proxy.applyHorseInfo(packet);
            return null;
        }
    }
}
