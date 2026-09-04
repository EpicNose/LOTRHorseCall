package com.epicnose.lotrcallablehorse.lotr.common;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import lotr.common.LOTRConfig;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

public class CallableHorseTickHandlerServer {
    private static final long DIRTY_SAVE_INTERVAL_TICKS = 100L;
    private long nextDirtySaveTick;

    public CallableHorseTickHandlerServer() {
        FMLCommonHandler.instance().bus().register(this);
        nextDirtySaveTick = 0L;
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.START) {
            return;
        }
        // ServerTickEvent is dimension independent, so queued network work is
        // still drained when the overworld is temporarily unavailable.
        if (CallableHorseLevelData.needsLoad) {
            CallableHorseLevelData.load();
        }
        CallableHorseServerTasks.runPendingTasks();
    }

    @SubscribeEvent
    public void onWorldTick(TickEvent.WorldTickEvent event) {
        World world = event.world;
        if (world.isRemote) {
            return;
        }
        if (event.phase == TickEvent.Phase.START && isOverworld(world)) {
            if (CallableHorseLevelData.needsLoad) {
                CallableHorseLevelData.load();
            }
            CallableHorseServerTasks.runPendingTasks();
        }
        if (event.phase == TickEvent.Phase.END) {
            if (isOverworld(world)) {
            if (CallableHorseLevelData.anyDataNeedsSave()) {
                long worldTime = world.getTotalWorldTime();
                if (worldTime >= nextDirtySaveTick) {
                    CallableHorseLevelData.save();
                    nextDirtySaveTick = worldTime + DIRTY_SAVE_INTERVAL_TICKS;
                }
            }

                if (world.getTotalWorldTime() % 600L == 0L) {
                    CallableHorseLevelData.save();
//                    FMLLog.info("周期保存");
                }
                int playerDataClearingInterval = LOTRConfig.playerDataClearingInterval;
                playerDataClearingInterval = Math.max(playerDataClearingInterval, 800);
                if (world.getTotalWorldTime() % playerDataClearingInterval == 0L) {
                    CallableHorseLevelData.saveAndClearUnusedPlayerData();
                }


            }


        }
    }

    private static boolean isOverworld(World world) {
        return world != null && world.provider != null && world.provider.dimensionId == 0;
    }


    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        EntityPlayer player = event.player;
        World world = player.worldObj;
        if (world == null || world.isRemote) {
            return;
        }
        if (player instanceof EntityPlayerMP) {
            EntityPlayerMP entityplayer = (EntityPlayerMP) player;
            if (event.phase == TickEvent.Phase.END) {
                CallableHorseLevelData.getData(entityplayer).onUpdate(entityplayer, (WorldServer) world);


            }
        }


    }
}
