package com.epicnose.lotrcallablehorse.lotr.common;


import com.epicnose.lotrcallablehorse.lotr.common.network.*;
import cpw.mods.fml.common.FMLLog;
import lotr.common.LOTRMod;
import lotr.common.entity.animal.LOTREntityHorse;
//import lotr.common.network.PacketHorseInfo;
import lotr.common.util.LOTRLog;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.util.Constants;

import java.util.*;

public class HorseInfo {//仅允许在中洲世界召唤马匹

    //    public static List<SingleVehicle> vehicles=  Collections.synchronizedList(new ArrayList<SingleVehicle>());
    public  List<SingleVehicle> vehicles;
    //    static World world=DimensionManager.getWorld(LOTRDimension.MIDDLE_EARTH.dimensionID);
    int tick=0;
    public  int size=1;
    public  UUID puuid;
    public  PlayerHorseData lpd;
    public HorseInfo(UUID puu){
        puuid=puu;
        vehicles=  Collections.synchronizedList(new ArrayList<SingleVehicle>());
//        this.lpd=lpd;
//        lpd=LOTRLevelData.getData(puu);
//        if(lpd.isLord){
//            size=2;
//        }else if(lpd.isKing){
//            size=3;
//        }else{
//            size=1;
//        }
    }

    public void onUpdate(){
        if(tick>40){
            tick=0;

//            LOTRPlayerData lpd=LOTRLevelData.getData(puuid);
//            EntityPlayer p=lpd.getOtherPlayer(puuid);
//            if(!p.worldObj.isRemote){
//                MinecraftServer.getServer().getPl
            if(vehicles.size()>0){
                if(lpd!=null){
                    if(!LOTRMod.proxy.isClient()  )
                    {
                        if(lpd.getPlayer()!=null){
                            sendBasicData((EntityPlayerMP) lpd.getPlayer());
                        }
                    }

                }


            }
//            }
//            if(lpd!=null){
//                if(lpd.isLord){
//                    size=2;
//                }else if(lpd.isKing){
//                    size=3;
//                }else{
                    size=3;            //这里得改成读配置
//                }
//            }
        }else{
            tick++;
        }
    }


    public SingleVehicle getIsUsing(){
        for(SingleVehicle vehicle:vehicles){
            if(vehicle.isUsing ){
                return vehicle;
            }
        }
        return null;
    }

    public void setIsUsing(int index){
        for(SingleVehicle sv:vehicles){
            if(sv.index!=index){
                sv.isUsing=false;
            }else{
                sv.isUsing=true;
            }
//            if(sv.isUsing & sv.index!=index){
//                sv.isUsing=false;
//            }
        }
    }



    public void sendBasicData(EntityPlayerMP entityplayer) {
        if(vehicles.size()>0){
            for(int i=0;i<vehicles.size();i++){
                PacketSingleHorseInfo packet=new PacketSingleHorseInfo(i,vehicles.get(i));
//                FMLLog.info("传回"+i+"号SingleHorse包同步信息");
                CallableHorsePacketHandler.networkWrapper.sendTo(packet, (EntityPlayerMP)entityplayer);

            }
        }
    }


    public boolean isVehicleEntity(Entity ent) {
        if(this.vehicles.size()>0){
            List<SingleVehicle> vehiclesToCheck = new ArrayList<>(vehicles);

            // 使用迭代器遍历列表
            Iterator<SingleVehicle> iterator = vehiclesToCheck.iterator();
            while (iterator.hasNext()) {
                SingleVehicle vehicle = iterator.next();
                if (vehicle.entityuuid != null  ) {
                    if(vehicle.entityuuid.equals(ent.getUniqueID())){
//                        LOTRLog.logger.info("成功匹配上");
                        return true;
                    }
//                    LOTRLog.logger.info("UUID"+vehicle.entityuuid.toString()+"列表中"+"序号:"+vehicle.index);
//                    LOTRLog.logger.info("UUID"+ent.getUniqueID()+"源生物");
                }
            }
//            LOTRLog.logger.info("没匹配上");
            return false;
        }
        // 创建一个新的列表来存放要遍历的车辆信息

        return false;
    }

//    public boolean isVehicleEntity(Entity ent){
//        if(vehicles.size()>0){
////            vehicles.
//            List<SingleVehicle> vehiclenew = vehicles;
//            for(SingleVehicle vehicle:vehiclenew){
//                if(vehicle.entityuuid!=null){
//                    if(vehicle.entityuuid.equals(ent.getUniqueID())){
//                        return true;
//                    }
//                }
//
//
//            }
//            return false;
//        }
//
////        else return false;
//        return false;
//    }

    public SingleVehicle getSingleVehicleByEntity(Entity ent){
        if(vehicles.size()>0 ){
            for(SingleVehicle vehicle:vehicles){
                if(vehicle.entityuuid.equals(ent.getUniqueID())){
                    return vehicle;
                }
            }
        }

        return null;
    }

    public SingleVehicle getSingleVehicleByIndex(int i){
        if(i<vehicles.size() & i>=0){
            return vehicles.get(i);
        }else return null;
    }

    public void addVehicle(SingleVehicle sv){
        if(!vehicles.contains(sv)){
            if(vehicles.size()<size){
//                if(sv.horseSpeed>0.35)sv.horseSpeed=0.35;
                sv.index=vehicles.size();
                vehicles.add(sv);
            }
        }
    }

    //    public void removeVehicle(SingleVehicle sv){
//        if(vehicles.contains(sv)){
//            int index=vehicles.indexOf(sv);
//            if(index>0){
//                for(int i=vehicles.size();i>index & i>0;i--){
//                    vehicles.set(i-1,vehicles.get(i));
//                }
//            }
//            else{
//                vehicles.clear();
//            }
//        }
//    }
    public void removeVehicle(SingleVehicle sv){
//        if(sv.isUsing){
        if(sv!=null){

            Chunk chunk= CallableHorseEventHandler.vehicleAndChunk.get(sv.entityuuid);
            if(chunk!=null){
                if(!chunk.isChunkLoaded){
                    chunk.onChunkLoad();
                }
//                LOTRLog.logger.info("区块正常读取");
                if(sv.ent!=null){
                    LOTREntityHorse horse= (LOTREntityHorse) sv.ent;
                    if(horse.getMountArmor()!=null){
//                            if(horse.getMountArmor().getItem()!=LOTRMod.horseArmorMithril){
                        sv.armorid=Item.getIdFromItem(horse.getMountArmor().getItem());
//                            }



                    }
                    sv.ent.setDead();
                    sv.isUsing=false;
//                    LOTRLog.logger.info("召回 取消使用状态");
                }else {
//                    FMLLog.info("实体获取不到");
                }
                sv.isUsing=false;
                CallableHorseEventHandler.vehicleAndChunk.remove(sv.entityuuid);
                CallableHorseEventHandler.ChunkAndVehicle.remove(chunk);
                CallableHorseEventHandler.VehicleEntityAndOwner.remove(sv.entityuuid);
//                            LOTRLog.logger.info("已清理旧载具"+sv.entityuuid);
//                CallableHorseLevelData.getData(sv.ownerUUID).markDirty();
//                            break;
//                        }
//                    }
//                }



//        }
            }else {
//                FMLLog.info("区块是空的");
            }

        }else {
//            FMLLog.info("sv是空的");
        }






    }
    public void spawnSpecificVehicleByIndex(int index,  EntityPlayer p){
        if(vehicles!=null){
            if(index<vehicles.size()){
                if(vehicles.get(index)!=null){




//            if(getIsUsing()!=null && getIsUsing().index!=index){
                    if(getIsUsing()!=null ){
//                LOTRLog.logger.info("尝试移除之前正使用的载具");
                        removeVehicle(getIsUsing());

                    }

                    SingleVehicle sv=vehicles.get(index);
//                    LOTRLog.logger.info("原来的svuuid"+sv.entityuuid);
                    LOTREntityHorse horse=sv.spawnVehicle(p);   //具体位置呢？
//                    LOTRLog.logger.info("新马的uuid"+horse.getUniqueID());
                    horse.setHealth(horse.getMaxHealth());
                    sv.ent=horse;
                    sv.entityuuid=horse.getUniqueID();
                    CallableHorseEventHandler.vehicleAndChunk.put(horse.getUniqueID(),horse.worldObj.getChunkFromBlockCoords(horse.chunkCoordX,horse.chunkCoordZ));
                    CallableHorseEventHandler.ChunkAndVehicle.put(horse.worldObj.getChunkFromBlockCoords(horse.chunkCoordX,horse.chunkCoordZ),sv.entityuuid);
                    CallableHorseEventHandler.VehicleEntityAndOwner.put(horse.getUniqueID(),p.getUniqueID());
                    this.setIsUsing(index);
                    FMLLog.info("[召之马来]"+p.getDisplayName()+"召唤了"+index+"号载具");
//                    LOTRLog.logger.info("区块x"+CallableHorseEventHandler.vehicleAndChunk.get(sv.entityuuid).xPosition);
                }
            }
        }
    }
    public void spawnSpecificNormalHorseByIndex(int index,  EntityPlayer p){ //用于销毁载具时 给玩家召唤的普通马  // 现在已经不返回马了
        if(vehicles!=null){
//            LOTRLog.logger.info("vehicles不为空");
//            LOTRLog.logger.info("vehicles的大小"+vehicles.size());
//            LOTRLog.logger.info("index"+index);
            if(index<vehicles.size()){
//                LOTRLog.logger.info("序号小于size");
                if(vehicles.get(index)!=null){
//                    LOTRLog.logger.info("按序号取出的sv不为空");
//                    if(getIsUsing()!=null  ){
//                        if(index== getIsUsing().index){
//                            LOTRLog.logger.info("要删除的序号是正在使用的");
////                            return;
//                            removeVehicle(getIsUsing());
//                            LOTRLog.logger.info("remove正在使用的sv");
//                        }
//
//                    }
                    SingleVehicle sv=vehicles.get(index);
//                    LOTREntityHorse horse=sv.spawnVehicle(p);   //具体位置呢？
//                    sv.ent=horse;
                    removeVehicle(sv);
                    deleteVehicle(sv);
//                    LOTRLog.logger.info("收到信息 删除这个sv");
                    CallableHorseLevelData.saveData(p.getUniqueID());
                    CallableHorseLevelData.sendPlayerData((EntityPlayerMP) p); //同步一下信息
//                    LOTRLog.logger.info("删完后同步一下信息");
//                    removeVehicle(sv);
//                    LOTREventHandler.vehicleAndChunk.put(sv.entityuuid,horse.worldObj.getChunkFromBlockCoords(horse.chunkCoordX,horse.chunkCoordZ));
//                    LOTREventHandler.ChunkAndVehicle.put(horse.worldObj.getChunkFromBlockCoords(horse.chunkCoordX,horse.chunkCoordZ),sv.entityuuid);
//                    LOTREventHandler.VehicleEntityAndOwner.put(horse.getUniqueID(),p.getUniqueID());
//                    this.setIsUsing(index);
                }
            }
        }
    }

    public void deleteVehicle(SingleVehicle sv){
        UUID puuid=sv.ownerUUID;
        if(vehicles!=null){
            if(vehicles.size()>1){
                if(sv.index<vehicles.size()){
                    if(vehicles.get(sv.index)!=null){
                        int comp=vehicles.size();
                        int deleteindex=sv.index;
//                    vehicles.remove(sv);
//                    vehicles.
                        if(sv.index>0 & sv.index<comp-1){

                            for(int i=deleteindex;i<comp;i++){
                                vehicles.get(i).index=i-1;
//                                vehicles.set(i-1,vehicles.get(i));
                            }
                            vehicles.remove(sv);
                        }else{ //index==0  index==size-1
                            if(sv.index==0){
//                                vehicles.remove(sv);
                                for(int i=deleteindex+1;i<comp;i++){
                                    vehicles.get(i).index=i-1;
//                                    vehicles.set(i-1,vehicles.get(i));
                                }
                                vehicles.remove(sv);
                            }else if(sv.index==comp-1){
                                vehicles.remove(sv);
                            }

                        }
                    }
                }
            }else{
                vehicles.remove(0);
//                vehicles.clear();
                vehicles=Collections.synchronizedList(new ArrayList<SingleVehicle>());
//                LOTRLog.logger.info("总长度为1 并移除了0号");
            }
            CallableHorseLevelData.getData(puuid).markDirty();
        }
    }

    public UUID getUUID(){
        return this.puuid;
    }
    public void writeToNBT(NBTTagCompound nbt) {
//        NBTTagCompound data = new NBTTagCompound();

        //每个载具一个compound  循环存入

        if(this.vehicles.size()>0){
            NBTTagCompound data = new NBTTagCompound();

            NBTTagList herevehicles = new NBTTagList();    //玩家的载具组 list

            for(SingleVehicle sv:vehicles){
                NBTTagCompound single = new NBTTagCompound();
                sv.writeToNBT(single);
                herevehicles.appendTag(single);
            }
            data.setTag("Vehicles",herevehicles);
            nbt.setTag("HorseInfo",data);
        }


    }


    public void readVehiclesFromNBT(NBTTagCompound nbt) {
        List<SingleVehicle> herevehicles=  Collections.synchronizedList(new ArrayList<SingleVehicle>());
        NBTTagCompound data = nbt.getCompoundTag("HorseInfo");
        NBTTagList vehiclesTag = data.getTagList("Vehicles", Constants.NBT.TAG_COMPOUND);
//        LOTRLog.logger.info("vehiclesTag中的数量"+vehiclesTag.tagCount());
        for (int i = 0; i < vehiclesTag.tagCount(); i++) {
            NBTTagCompound vehicleTag = vehiclesTag.getCompoundTagAt(i);
            SingleVehicle sv = new SingleVehicle();
            sv.readFromNBT(vehicleTag);//完成了下面的方法
            sv.ownerUUID=this.getUUID();
//            String horseName = vehicleTag.getString("HorseName");
//            double horseSpeed = vehicleTag.getDouble("HorseSpeed");
//            double horseJump = vehicleTag.getDouble("HorseJump");
//            int horseType = vehicleTag.getInteger("HorseType");
////            int color = vehicleTag.getInteger("Color");
//            double health = vehicleTag.getDouble("Health");
//            int armorid = vehicleTag.getInteger("ArmorID");
//            boolean isValid = vehicleTag.getBoolean("IsValid");
//
//            SingleVehicle sv = new SingleVehicle();
//            sv.isValid=isValid;
//            sv.armorid=armorid;
//            sv.horseName=horseName;
//            sv.horseJump=horseJump;
//            sv.horseType=horseType;
//            sv.health=health;
//            sv.horseSpeed=horseSpeed;
//            sv.color=color;
//            herevehicles.set(sv.index,sv);
            herevehicles.add(sv);
        }
        vehicles=herevehicles;
//        LOTRLog.logger.info("vehicles数量"+vehicles.size());
//        return vehicles;
    }

    public void receiveBasicData(PacketSingleHorseInfo packet){   //注意，这里的是单个的
//        if(vehicles.size()==0){   //初次登记时
//            vehicles.add(new SingleVehicle());
//        }

        SingleVehicle sv=new SingleVehicle();
        sv.index=packet.index;
        sv.horseSpeed= packet.horseSpeed;
        sv.health= packet.health;
        sv.horseJump= packet.horseJump;
        sv.isUsing= packet.isUsing;
        sv.isValid= packet.isValid;
        sv.horseType= packet.horsetype;
        sv.armorid= packet.armortype;
        sv.entityuuid=packet.entityuuid;
        sv.horseName= packet.horseName;
        sv.variant=packet.variant;
        vehicles.set(sv.index, sv);

    }


    public void sendCallHorseMessage2Server(int index,UUID puuid){  //来自客户端的召唤按钮
        PacketCallHorse pkt = new PacketCallHorse(index,puuid);
        CallableHorsePacketHandler.networkWrapper.sendToServer(pkt);
    }
    public void sendCallBackHorseMessage2Server(int index,UUID puuid){
        PacketCallBackHorse pkt = new PacketCallBackHorse(index,puuid);
        CallableHorsePacketHandler.networkWrapper.sendToServer(pkt);
    }

    public void sendReleaseHorseMessage2Server(int index,UUID puuid){
        PacketReleaseHorse pkt=new PacketReleaseHorse(index,puuid);
        CallableHorsePacketHandler.networkWrapper.sendToServer(pkt);
//        LOTRLog.logger.info("");
    }


}
