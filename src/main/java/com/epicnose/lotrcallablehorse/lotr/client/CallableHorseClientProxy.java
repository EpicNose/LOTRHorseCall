package com.epicnose.lotrcallablehorse.lotr.client;

import com.epicnose.lotrcallablehorse.lotr.client.gui.CallableHorseGUIHandler;
import com.epicnose.lotrcallablehorse.lotr.common.CallableHorseCommonProxy;
import com.epicnose.lotrcallablehorse.lotr.common.CallableHorseLevelData;
import com.epicnose.lotrcallablehorse.lotr.common.PlayerHorseData;
import com.epicnose.lotrcallablehorse.lotr.common.network.PacketSingleHorseInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

public class CallableHorseClientProxy extends CallableHorseCommonProxy {
    public static CallableHorseGUIHandler guiHandler;
    private static final CallableHorseClientNetworkHandler NETWORK_HANDLER;
    static {
        guiHandler = new CallableHorseGUIHandler();
        NETWORK_HANDLER = new CallableHorseClientNetworkHandler();
    }

    @Override
    public EntityPlayer getClientPlayer() {
        return Minecraft.getMinecraft().thePlayer;
    }
    @Override
    public World getClientWorld() {
        return Minecraft.getMinecraft().theWorld;
    }
    @Override
    public boolean isSingleplayer() {
        return Minecraft.getMinecraft().isSingleplayer();
    }

    @Override
    public void applyPlayerHorseData(final NBTTagCompound data) {
        if (data == null) {
            return;
        }
        final int connectionGeneration = NETWORK_HANDLER.getConnectionGeneration();
        Minecraft.getMinecraft().func_152344_a(new Runnable() {
            @Override
            public void run() {
                if (!NETWORK_HANDLER.isCurrentConnection(connectionGeneration) || isSingleplayer()) {
                    return;
                }
                EntityPlayer player = getClientPlayer();
                PlayerHorseData playerData = CallableHorseLevelData.getData(player);
                if (playerData != null) {
                    playerData.load(data);
                }
            }
        });
    }

    @Override
    public void applyHorseInfo(final PacketSingleHorseInfo packet) {
        if (packet == null) {
            return;
        }
        final int connectionGeneration = NETWORK_HANDLER.getConnectionGeneration();
        Minecraft.getMinecraft().func_152344_a(new Runnable() {
            @Override
            public void run() {
                if (!NETWORK_HANDLER.isCurrentConnection(connectionGeneration)) {
                    return;
                }
                EntityPlayer player = getClientPlayer();
                PlayerHorseData playerData = CallableHorseLevelData.getData(player);
                if (playerData != null && playerData.horseInfo != null) {
                    playerData.horseInfo.receiveBasicData(packet);
                }
            }
        });
    }
}
