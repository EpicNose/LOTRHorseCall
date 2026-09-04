package com.epicnose.lotrcallablehorse.lotr.common;

import com.epicnose.lotrcallablehorse.lotr.common.network.PacketLoginPlayerHorseData;
import cpw.mods.fml.common.FMLCommonHandler;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import java.io.IOException;
import java.util.UUID;

public class PlayerHorseData {
    public HorseInfo horseInfo;
    public UUID playerUUID;
    public boolean needsSave = false;
    public int pdTick = 0;
    private int customMaxSlots = -1;

    public PlayerHorseData(UUID uuid) {
        playerUUID = uuid;
        horseInfo = new HorseInfo(uuid, this);
    }

    public UUID getPlayerUUID() {
        return playerUUID;
    }

    public void markDirty() {
        needsSave = true;
    }

    public boolean needsSave() {
        return needsSave;
    }

    public int getMaxSlots() {
        if (customMaxSlots > 0) {
            return Math.max(1, Math.min(CallableHorseConfig.MAX_STORED_VEHICLES, customMaxSlots));
        }
        return CallableHorseConfig.getConfiguredSlotCount();
    }

    public void setCustomMaxSlots(int slots) {
        this.customMaxSlots = slots <= 0 ? -1 : Math.max(1, Math.min(CallableHorseConfig.MAX_STORED_VEHICLES, slots));
        markDirty();
    }

    public int getCustomMaxSlots() {
        return customMaxSlots;
    }

    public EntityPlayer getPlayer() {
        MinecraftServer server = MinecraftServer.getServer();
        if (server != null && server.worldServers != null) {
            for (WorldServer world : server.worldServers) {
                EntityPlayer entityplayer = world.func_152378_a(playerUUID);
                if (entityplayer != null) {
                    return entityplayer;
                }
            }
        }
        return null;
    }

    public void save(NBTTagCompound playerData) {
        writeData(playerData);
        needsSave = false;
    }

    private void writeData(NBTTagCompound playerData) {
        if (customMaxSlots > 0) {
            playerData.setInteger("CustomMaxSlots", customMaxSlots);
        }
        if (horseInfo != null) {
            horseInfo.writeToNBT(playerData);
        }
    }

    public void load(NBTTagCompound playerData) {
        if (playerData == null) {
            customMaxSlots = -1;
            horseInfo = new HorseInfo(this.getPlayerUUID(), this);
            needsSave = false;
            return;
        }
        if (playerData.hasKey("CustomMaxSlots")) {
            customMaxSlots = playerData.getInteger("CustomMaxSlots");
        } else {
            customMaxSlots = -1;
        }
        if (playerData.hasKey("HorseInfo", 10)) {
            if (this.horseInfo == null) {
                this.horseInfo = new HorseInfo(this.getPlayerUUID(), this);
            } else {
                this.horseInfo.parentData = this;
            }
            this.horseInfo.readVehiclesFromNBT(playerData);
        } else {
            this.horseInfo = new HorseInfo(this.getPlayerUUID(), this);
        }
        needsSave = false;
    }

    public void sendPlayerData(EntityPlayerMP entityplayer) throws IOException {
        if (entityplayer == null) {
            return;
        }
        NBTTagCompound nbt = new NBTTagCompound();
        writeData(nbt);
        PacketLoginPlayerHorseData.sendTo(nbt, entityplayer);
    }

    public void onUpdate(EntityPlayerMP entityplayer, WorldServer world) {
        ++pdTick;
    }
}
