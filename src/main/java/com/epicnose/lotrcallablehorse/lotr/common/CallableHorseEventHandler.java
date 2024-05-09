package com.epicnose.lotrcallablehorse.lotr.common;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import lotr.common.LOTRLevelData;
import lotr.common.entity.animal.LOTREntityHorse;
import lotr.common.entity.npc.LOTRNPCMount;
import lotr.common.util.LOTRLog;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.world.ChunkEvent;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CallableHorseEventHandler {
    public static ConcurrentHashMap<UUID, Chunk>vehicleAndChunk;

    public static ConcurrentHashMap<Chunk,UUID>ChunkAndVehicle;
    //	public static ConcurrentHashMap<UUID,UUID>ownerAndVehicleEntity=new ConcurrentHashMap<>();
    public static ConcurrentHashMap<UUID,UUID> VehicleEntityAndOwner;

    public CallableHorseEventHandler() {
        FMLCommonHandler.instance().bus().register(this);
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.TERRAIN_GEN_BUS.register(this);
        vehicleAndChunk = new ConcurrentHashMap<>();
        ChunkAndVehicle = new ConcurrentHashMap<>();
        VehicleEntityAndOwner = new ConcurrentHashMap<>();
//        new LOTRStructureTimelapse();
    }

    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        EntityPlayer entityplayer = event.player;
        World world = entityplayer.worldObj;
        if (!world.isRemote) {
            EntityPlayerMP entityplayermp = (EntityPlayerMP) entityplayer;
            CallableHorseLevelData.sendPlayerData(entityplayermp);
        }

    }

    @SubscribeEvent
    public void onChunkUnload(ChunkEvent.Unload event){
        if(!event.world.isRemote){
            if(ChunkAndVehicle.containsKey(event.getChunk())){
//				for
//				for(){
//
//				}
                for (List<Entity> entityList : event.getChunk().entityLists) {
                    for(Entity ent:entityList){
                        if(ent.getUniqueID().equals(ChunkAndVehicle.get(event.getChunk()))){
                            if(VehicleEntityAndOwner.get(ent.getUniqueID())!=null){
                                PlayerHorseData lpd= CallableHorseLevelData.getData(VehicleEntityAndOwner.get(ent.getUniqueID()));
                                if(lpd!=null){
                                    if(lpd.horseInfo!=null){
                                        SingleVehicle sv=lpd.horseInfo.getSingleVehicleByEntity(ent);
                                        if(sv!=null){
                                            lpd.horseInfo.removeVehicle(sv);
                                            ent.setDead();
                                        }

                                    }
                                }

                            }


                        }
                    }
                }


//				event.getChunk().
//				for(Entity ent:event.getChunk().entityLists[]){
//
//				}


            }
        }
    }

    @SubscribeEvent
    public void onPlayerLeave(PlayerEvent.PlayerLoggedOutEvent e){
        if(!e.player.worldObj.isRemote){
            PlayerHorseData lpd=CallableHorseLevelData.getData(e.player);
            if(lpd.horseInfo!=null){
                if(lpd.horseInfo.getIsUsing()!=null){
//                    LOTRLog.logger.info("下线前在使用载具");
                    SingleVehicle sv=lpd.horseInfo.getIsUsing();
                    if(sv!=null){
//                        LOTRLog.logger.info("下线前使用载具不为空");
                        sv.isValid=false;
                        sv.isUsing=false;
//						VehicleEntityAndOwner.remove(sv.entityuuid);
//						Chunk before=vehicleAndChunk.get(sv.entityuuid);
//						removeSpecificEntityInChunk(before,sv.entityuuid);  //删除
                        lpd.horseInfo.removeVehicle(sv);
//                        LOTRLog.logger.info("下线前载具移除完毕");
//			before.
//						vehicleAndChunk.remove(sv.entityuuid);
//						ChunkAndVehicle.remove(before);
//						if(!sv.ent.isDead)
//						sv.ent.setDead();
                    }
                }

            }


//			ChunkAndVehicle.

        }
    }


    @SubscribeEvent
    public void onHorseUpdate(LivingEvent.LivingUpdateEvent event) {  //把已登记的 被骑着的 载具记录在静态map里
        World world = event.entity.worldObj;
        if(!world.isRemote){
            if(world.getTotalWorldTime() % 20L == 0L){
                if (event.entity instanceof LOTRNPCMount) {
                    if(event.entity instanceof LOTREntityHorse){
                        LOTREntityHorse horse = (LOTREntityHorse) event.entityLiving;
                        if(!horse.isDead){
                            if (horse.riddenByEntity!=null & horse.riddenByEntity instanceof EntityPlayer) {
                                EntityPlayer p=(EntityPlayer) horse.riddenByEntity;
                                PlayerHorseData lpd=CallableHorseLevelData.getData(p);
                                if(lpd.horseInfo!=null){
                                    if(lpd.horseInfo.isVehicleEntity(event.entity)){
                                        SingleVehicle sv=lpd.horseInfo.getSingleVehicleByEntity(event.entity);
                                        if(sv!=null){
                                            ChunkAndVehicle.put(world.getChunkFromChunkCoords(MathHelper.floor_double(event.entity.posX) >> 4, MathHelper.floor_double(event.entity.posZ) >> 4),event.entity.getUniqueID());
                                            vehicleAndChunk.put(event.entity.getUniqueID(),world.getChunkFromChunkCoords(MathHelper.floor_double(event.entity.posX) >> 4, MathHelper.floor_double(event.entity.posZ) >> 4));
                                            VehicleEntityAndOwner.put(event.entity.getUniqueID(),p.getUniqueID());
                                        }else {
//                                            FMLLog.info("sv是空的");
                                        }
                                    }else {
//                                        FMLLog.info("不是载具单位");
                                    }
//                                    FMLLog.info("aaaaaa");
                                }else {
//                                    FMLLog.info("horseInfo是空的");
                                }



                            }else if(horse.riddenByEntity==null ){
                                if(VehicleEntityAndOwner.containsKey(horse.getUniqueID())){
                                    PlayerHorseData lpd=CallableHorseLevelData.getData(VehicleEntityAndOwner.get(horse.getUniqueID()));
                                    if(lpd.horseInfo!=null){
                                        if(lpd.horseInfo.isVehicleEntity(horse)){
                                            boolean shouldremove=true;
                                            int radius = 15; // 检查玩家存在的半径范围
                                            // 检查马周围半径为radius的范围内是否有指定UUID的玩家
                                            for (Object player : world.playerEntities) {
                                                if (horse.getDistanceToEntity((Entity) player) <= radius && ((Entity) player).getUniqueID().equals(VehicleEntityAndOwner.get(horse.getUniqueID()))) {
                                                    // 马周围存在指定UUID的玩家
                                                    // 在这里执行你的逻辑
                                                    shouldremove=false;
                                                }
                                            }


                                            if(shouldremove){
                                                SingleVehicle sv=lpd.horseInfo.getSingleVehicleByEntity(event.entity);
                                                if(sv!=null){
                                                    lpd.horseInfo.removeVehicle(sv);
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                    }
                }
            }

        }

    }
    @SubscribeEvent
    public void onHorseDeath(LivingDeathEvent event) {
        if (event.entity instanceof LOTREntityHorse) {
            LOTREntityHorse horse = (LOTREntityHorse) event.entity;
            // 在这里执行马死亡后的操作
//			System.out.println("马死亡了：" + horse.getCommandSenderName());
            if(VehicleEntityAndOwner.containsKey(horse.getUniqueID())){
                horse.setMountArmor(null);
            }
        }
    }


}
