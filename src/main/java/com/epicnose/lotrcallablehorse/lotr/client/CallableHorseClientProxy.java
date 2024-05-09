package com.epicnose.lotrcallablehorse.lotr.client;

import com.epicnose.lotrcallablehorse.lotr.client.gui.CallableHorseGUIHandler;
import com.epicnose.lotrcallablehorse.lotr.common.CallableHorseCommonProxy;
import lotr.common.LOTRCommonProxy;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

public class CallableHorseClientProxy extends CallableHorseCommonProxy {
    public static CallableHorseGUIHandler guiHandler;
    static {
        guiHandler = new CallableHorseGUIHandler();
    }
//1
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
}
