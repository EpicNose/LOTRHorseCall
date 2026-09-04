package com.epicnose.lotrcallablehorse.lotr.common.network;

import com.epicnose.lotrcallablehorse.lotr.common.CallableHorseConfig;
import com.epicnose.lotrcallablehorse.lotr.common.CallableHorseMountSupport;
import net.minecraft.server.management.ServerConfigurationManager;
import net.minecraft.entity.player.EntityPlayerMP;

import java.util.UUID;

/** Shared validation for packets received from clients. */
public final class CallableHorsePacketUtil {
    private CallableHorsePacketUtil() {
    }

    public static boolean validIndex(int index) {
        // Existing slots remain addressable after a config decrease; the
        // server-side HorseInfo lookup still decides whether a slot exists.
        return CallableHorseConfig.isValidStoredSlotIndex(index);
    }

    public static boolean validOwner(UUID claimedOwner, EntityPlayerMP actualPlayer) {
        return actualPlayer != null && actualPlayer.getUniqueID() != null;
    }

    public static boolean isActiveServerPlayer(EntityPlayerMP player) {
        return player != null && !player.isDead && player.worldObj != null && !player.worldObj.isRemote;
    }

    public static String safeName(String name) {
        return CallableHorseMountSupport.safeName(name);
    }

    public static boolean finite(double value) {
        return !Double.isNaN(value) && !Double.isInfinite(value);
    }
}
