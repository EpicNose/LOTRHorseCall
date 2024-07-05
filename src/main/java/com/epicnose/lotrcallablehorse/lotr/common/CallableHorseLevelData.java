package com.epicnose.lotrcallablehorse.lotr.common;

import cpw.mods.fml.common.FMLLog;
import lotr.common.*;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;

import java.io.*;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CallableHorseLevelData {
//    public UUID playerUUID;
    public static boolean needsLoad;
    public static boolean needsSave;
    public static ConcurrentHashMap<UUID, PlayerHorseData> playerDataMap;

    static {
        playerDataMap = new ConcurrentHashMap<>();
//        playerTitleOfflineCacheMap = new ConcurrentHashMap<>();
    }


    public static PlayerHorseData getData(EntityPlayer entityplayer) {
        return getData(entityplayer.getUniqueID());
    }

    public static PlayerHorseData getData(UUID player) {
        PlayerHorseData pd = playerDataMap.get(player);
        if (pd == null) {
            pd = loadData(player);
//            playerTitleOfflineCacheMap.remove(player);
            if (pd == null) {
                pd = new PlayerHorseData(player);
            }
            playerDataMap.put(player, pd);
        }
        return pd;
    }
    public static void destroyAllPlayerData() {
        playerDataMap.clear();
    }

    public static void load() {
        try {
            NBTTagCompound levelData = loadNBTFromFile(getCallableHorseDat());
            File oldLOTRDat = new File(DimensionManager.getCurrentSaveRootDirectory(), "CallableHorse.dat");
            if (oldLOTRDat.exists()) {
                levelData = loadNBTFromFile(oldLOTRDat);
                oldLOTRDat.delete();
                if (levelData.hasKey("PlayerData")) {
                    NBTTagList playerDataTags = levelData.getTagList("PlayerData", 10);
                    for (int i = 0; i < playerDataTags.tagCount(); ++i) {
                        NBTTagCompound nbt = playerDataTags.getCompoundTagAt(i);
                        UUID player = UUID.fromString(nbt.getString("PlayerUUID"));
                        saveNBTToFile(getCallableHorsePlayerDat(player), nbt);
                    }
                }
            }


            destroyAllPlayerData();


            needsLoad = false;
            needsSave = true;
            save();
        } catch (Exception e) {
            FMLLog.severe("Error loading CallableHorse data");
            e.printStackTrace();
        }
    }
    public static PlayerHorseData loadData(UUID player) {
        try {
            NBTTagCompound nbt = loadNBTFromFile(getCallableHorsePlayerDat(player));
            PlayerHorseData pd = new PlayerHorseData(player);
            pd.load(nbt);
            return pd;
        } catch (Exception e) {
            FMLLog.severe("Error loading CallableHorse player data for %s", player);
            e.printStackTrace();
            return null;
        }
    }
    public static File getCallableHorsePlayerDat(UUID player) {
        File playerDir = new File(getOrCreateCallableHorseDir(), "playerHorses");
        if (!playerDir.exists()) {
            playerDir.mkdirs();
        }
        return new File(playerDir, player.toString() + ".dat");
    }
    public static File getCallableHorseDat() {
        return new File(getOrCreateCallableHorseDir(), "CallableHorse.dat");
    }
    public static File getOrCreateCallableHorseDir() {
        File file = new File(DimensionManager.getCurrentSaveRootDirectory(), "CallableHorse");
        if (!file.exists()) {
            file.mkdirs();
        }
        return file;
    }
    public static NBTTagCompound loadNBTFromFile(File file) throws FileNotFoundException, IOException {
        if (file.exists()) {
            FileInputStream fis = new FileInputStream(file);
            NBTTagCompound nbt = CompressedStreamTools.readCompressed(fis);
            fis.close();
            return nbt;
        }
        return new NBTTagCompound();
    }
    public static void saveData(UUID player) {
        try {
            NBTTagCompound nbt = new NBTTagCompound();
            PlayerHorseData pd = playerDataMap.get(player);
            pd.save(nbt);
            pd.needsSave=false;
            saveNBTToFile(getCallableHorsePlayerDat(player), nbt);
            FMLLog.info("Have Saved CallableHorse player data for %s", player);
        } catch (Exception e) {
            FMLLog.severe("Error saving CallableHorse player data for %s", player);
            e.printStackTrace();
        }
    }

    public static void saveNBTToFile(File file, NBTTagCompound nbt) throws FileNotFoundException, IOException {
        CompressedStreamTools.writeCompressed(nbt, new FileOutputStream(file));
    }
    public static void sendPlayerData(EntityPlayerMP entityplayer) {
        try {
            PlayerHorseData pd = getData(entityplayer);
            pd.sendPlayerData(entityplayer);
        } catch (Exception e) {
            FMLLog.severe("Failed to send player data to player " + entityplayer.getCommandSenderName());
            e.printStackTrace();
        }
    }
    public static void save() {
        try {
            if (needsSave) {
                File CallableHorse_dat = CallableHorseLevelData.getCallableHorseDat();
                if (!CallableHorse_dat.exists()) {
                    CallableHorseLevelData.saveNBTToFile(CallableHorse_dat, new NBTTagCompound());
                }
                NBTTagCompound levelData = new NBTTagCompound();

                CallableHorseLevelData.saveNBTToFile(CallableHorse_dat, levelData);
                needsSave = false;
            }
            for (Map.Entry<UUID, PlayerHorseData> e : playerDataMap.entrySet()) {
                UUID player = e.getKey();
                PlayerHorseData pd = e.getValue();
                if (pd.needsSave()) {
                    CallableHorseLevelData.saveData(player);
                    FMLLog.info("tagtagtag");
                }
            }

            needsSave=false;
//            if (LOTRSpawnDamping.needsSave) {
//                LOTRSpawnDamping.saveAll();
//            }
        } catch (Exception e) {
            FMLLog.severe("Error saving Player Horse data");
            e.printStackTrace();
        }
    }


    public static boolean anyDataNeedsSave() {

        for (PlayerHorseData pd : playerDataMap.values()) {
            if (!pd.needsSave()) {
                continue;
            }
            return true;
        }
        return false;
    }

    public static void saveAndClearUnusedPlayerData() {
        ArrayList<UUID> clearing = new ArrayList<>();
        for (UUID player : playerDataMap.keySet()) {
            boolean foundPlayer = false;
            for (WorldServer world : MinecraftServer.getServer().worldServers) {
                if (world.func_152378_a(player) == null) {
                    continue;
                }
                foundPlayer = true;
                break;
            }
            if (foundPlayer) {
                continue;
            }
            clearing.add(player);
        }
        clearing.size();
        playerDataMap.size();
        for (UUID player : clearing) {
            boolean saved = CallableHorseLevelData.saveAndClearData(player);
            if (!saved) {
                continue;
            }
        }
        playerDataMap.size();
    }

    public static boolean saveAndClearData(UUID player) {
        PlayerHorseData pd = playerDataMap.get(player);
        if (pd != null) {
            boolean saved = false;
            if (pd.needsSave()) {
                CallableHorseLevelData.saveData(player);
                saved = true;
            }
            playerDataMap.remove(player);
            return saved;
        }
        FMLLog.severe("Attempted to clear CallableHorseData player data for %s; no data found", player);
        return false;
    }



}

