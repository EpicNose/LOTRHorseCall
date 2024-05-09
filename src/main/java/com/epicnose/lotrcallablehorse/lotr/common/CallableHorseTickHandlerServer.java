package com.epicnose.lotrcallablehorse.lotr.common;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import lotr.common.*;
import lotr.common.block.LOTRBlockPortal;
import lotr.common.entity.item.LOTREntityPortal;
import lotr.common.fac.LOTRFaction;
import lotr.common.world.LOTRTeleporter;
import lotr.common.world.LOTRUtumnoLevel;
import lotr.common.world.LOTRWorldProviderUtumno;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemEditableBook;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemWritableBook;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;

import java.util.List;

public class CallableHorseTickHandlerServer {
    public CallableHorseTickHandlerServer() {
        FMLCommonHandler.instance().bus().register(this);
    }
    @SubscribeEvent
    public void onWorldTick(TickEvent.WorldTickEvent event) {
        World world = event.world;
        if (world.isRemote) {
            return;
        }
        if (event.phase == TickEvent.Phase.START && world == DimensionManager.getWorld(0)) {

            if (CallableHorseLevelData.needsLoad) {
                CallableHorseLevelData.load();
            }

        }
        if (event.phase == TickEvent.Phase.END) {
            if (world == DimensionManager.getWorld(0)) {
                if (CallableHorseLevelData.anyDataNeedsSave()) {
                    CallableHorseLevelData.save();
                }

                if (world.getTotalWorldTime() % 600L == 0L) {
                    CallableHorseLevelData.save();
//                    FMLLog.info("周期保存");
                }
                int playerDataClearingInterval = LOTRConfig.playerDataClearingInterval;
                playerDataClearingInterval = Math.max(playerDataClearingInterval, 600);
                if (world.getTotalWorldTime() % playerDataClearingInterval == 0L) {
                    CallableHorseLevelData.saveAndClearUnusedPlayerData();
                }


            }


        }
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
