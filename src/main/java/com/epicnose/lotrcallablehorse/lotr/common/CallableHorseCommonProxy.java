package com.epicnose.lotrcallablehorse.lotr.common;

import com.epicnose.lotrcallablehorse.lotr.common.network.PacketSingleHorseInfo;
import cpw.mods.fml.common.network.IGuiHandler;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

public class CallableHorseCommonProxy implements IGuiHandler {
    @Override
    public Object getServerGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
        return null;
    }

    @Override
    public Object getClientGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
        return null;
    }

    public World getClientWorld() {
        return null;
    }

    public EntityPlayer getClientPlayer() {
        return null;
    }

    public boolean isSingleplayer() {
        return false;
    }

    /** Client-side packet callback. Dedicated servers intentionally do nothing. */
    public void applyPlayerHorseData(NBTTagCompound data) {
    }

    /** Client-side packet callback. Dedicated servers intentionally do nothing. */
    public void applyHorseInfo(PacketSingleHorseInfo packet) {
    }
}
