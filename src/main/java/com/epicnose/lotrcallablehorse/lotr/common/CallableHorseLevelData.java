package com.epicnose.lotrcallablehorse.lotr.common;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.FMLLog;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Persistent per-player storage with a small, server-only runtime cache. */
public class CallableHorseLevelData {
    /** Kept for compatibility with the original tick handler. */
    public static volatile boolean needsLoad = true;
    public static volatile boolean needsSave;
    public static final ConcurrentHashMap<UUID, PlayerHorseData> playerDataMap =
            new ConcurrentHashMap<UUID, PlayerHorseData>();

    private static volatile boolean loaded;
    private static volatile File loadedRoot;

    private CallableHorseLevelData() {
    }

    public static PlayerHorseData getData(EntityPlayer entityplayer) {
        return entityplayer == null ? null : getData(entityplayer.getUniqueID());
    }

    public static PlayerHorseData getData(UUID player) {
        if (player == null) {
            return null;
        }
        ensureLoaded();
        PlayerHorseData cached = playerDataMap.get(player);
        if (cached != null) {
            return cached;
        }

        PlayerHorseData loadedData = loadData(player);
        if (loadedData == null) {
            loadedData = new PlayerHorseData(player);
        }
        PlayerHorseData previous = playerDataMap.putIfAbsent(player, loadedData);
        return previous == null ? loadedData : previous;
    }

    /** Does not touch disk; useful from hot entity events where a cache miss is expensive. */
    public static PlayerHorseData getLoadedData(UUID player) {
        return player == null ? null : playerDataMap.get(player);
    }

    public static void destroyAllPlayerData() {
        playerDataMap.clear();
    }

    /** Initialize a server session and migrate the pre-alpha-1.1.0 file once. */
    public static synchronized void load() {
        if (isClientSide()) {
            return;
        }
        File root = DimensionManager.getCurrentSaveRootDirectory();
        if (root == null) {
            return;
        }
        if (loaded && root.equals(loadedRoot)) {
            needsLoad = false;
            return;
        }

        try {
            migrateLegacyFile(root);
            destroyAllPlayerData();
            loadedRoot = root;
            loaded = true;
            needsLoad = false;
            needsSave = false;
        } catch (Exception e) {
            // Keep needsLoad set so the next server tick can retry safely.
            FMLLog.severe("Error loading CallableHorse data");
            e.printStackTrace();
        }
    }

    public static synchronized void resetForServer() {
        if (!isClientSide()) {
            save();
        }
        destroyAllPlayerData();
        loadedRoot = null;
        loaded = false;
        needsLoad = true;
        needsSave = false;
    }

    private static void ensureLoaded() {
        if (!isClientSide() && needsLoad) {
            load();
        }
    }

    private static boolean isClientSide() {
        try {
            return FMLCommonHandler.instance().getEffectiveSide().isClient();
        } catch (Throwable ignored) {
            return MinecraftServer.getServer() == null;
        }
    }

    private static void migrateLegacyFile(File root) throws IOException {
        if (root == null) {
            return;
        }

        // Versions before the per-player files were released used both
        // <save>/CallableHorse.dat and <save>/CallableHorse/CallableHorse.dat
        // depending on which intermediate build was installed.  Process both
        // candidates, but never overwrite a non-empty per-player file.
        File nestedDir = new File(root, "CallableHorse");
        File[] legacyFiles = {
                new File(root, "CallableHorse.dat"),
                new File(nestedDir, "CallableHorse.dat")
        };
        for (File legacy : legacyFiles) {
            migrateLegacyFile(legacy, root);
        }
    }

    private static void migrateLegacyFile(File legacy, File root) throws IOException {
        if (legacy == null || !legacy.exists() || !legacy.isFile()) {
            return;
        }

        NBTTagCompound legacyData = loadNBTFromFile(legacy);
        if (!legacyData.hasKey("PlayerData", 9)) {
            // Do not rename an unknown file just because it happens to share
            // the old filename.  Some intermediate builds left an empty
            // aggregate file here; leaving it untouched is safer than
            // modifying administrator data that this mod does not recognize.
            return;
        }

        NBTTagList playerDataTags = legacyData.getTagList("PlayerData", 10);
        boolean recognizedEntry = false;
        for (int i = 0; i < playerDataTags.tagCount(); i++) {
            NBTTagCompound nbt = playerDataTags.getCompoundTagAt(i);
            if (!nbt.hasKey("PlayerUUID")) {
                continue;
            }
            try {
                UUID player = UUID.fromString(nbt.getString("PlayerUUID"));
                recognizedEntry = true;
                File target = getCallableHorsePlayerDat(root, player);
                if (!target.exists() || target.length() == 0L) {
                    saveNBTToFile(target, nbt);
                }
            } catch (IllegalArgumentException ignored) {
                FMLLog.warning("[召之马来]跳过无效的旧玩家 UUID");
            }
        }
        if (recognizedEntry) {
            archiveLegacyFile(legacy);
        } else {
            FMLLog.warning("[召之马来]旧存档文件没有可迁移的玩家数据，保留原文件: %s",
                    legacy.getAbsolutePath());
        }
    }

    private static void archiveLegacyFile(File legacy) {
        if (legacy == null || !legacy.exists()) {
            return;
        }

        File parent = legacy.getParentFile();
        String baseName = legacy.getName() + ".migrated";
        File archived = new File(parent, baseName);
        int suffix = 1;
        while (archived.exists()) {
            archived = new File(parent, baseName + "." + suffix++);
        }

        try {
            try {
                Files.move(legacy.toPath(), archived.toPath(), StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(legacy.toPath(), archived.toPath());
            }
        } catch (IOException e) {
            if (legacy.renameTo(archived)) {
                return;
            }
            // A failed archive is harmless; leaving the source permits retry.
            // Do not delete or replace either file: an administrator may want
            // to recover the original data manually.
            FMLLog.warning("[召之马来]无法归档旧存档文件 %s: %s",
                    legacy.getAbsolutePath(), e.toString());
        }
    }

    public static PlayerHorseData loadData(UUID player) {
        if (player == null) {
            return null;
        }
        if (isClientSide()) {
            return new PlayerHorseData(player);
        }

        File primary = getCallableHorsePlayerDat(player);
        File backup = new File(primary.getAbsolutePath() + ".bak");

        // 1. 尝试正常加载主存档
        if (primary.exists()) {
            try {
                NBTTagCompound nbt = loadNBTFromFile(primary);
                PlayerHorseData data = new PlayerHorseData(player);
                data.load(nbt);
                return data;
            } catch (Exception e) {
                FMLLog.severe("[召之马来] 玩家 %s 载具数据正本损坏: %s，尝试从 .bak 备份自愈恢复...", player, e.toString());
            }
        }

        // 2. 主存档损坏或不存在，自动尝试从 .bak 备份拯救自愈
        if (backup.exists()) {
            try {
                NBTTagCompound backupNbt = loadNBTFromFile(backup);
                PlayerHorseData data = new PlayerHorseData(player);
                data.load(backupNbt);
                FMLLog.warning("[召之马来] 成功从 .bak 备份中拯救玩家 %s 的载具数据！正在同步回正本...", player);
                try {
                    saveNBTToFile(primary, backupNbt);
                } catch (Throwable ignored) {
                }
                return data;
            } catch (Exception ex) {
                FMLLog.severe("[召之马来] 玩家 %s 载具数据备份亦无法读取: %s", player, ex.toString());
            }
        }

        // 3. 若正本存在却无法读取且无有效备份，隔离封存现场，严禁直接静默清空覆写！
        if (primary.exists()) {
            File corrupt = new File(primary.getParentFile(), primary.getName() + ".corrupt." + System.currentTimeMillis());
            if (primary.renameTo(corrupt)) {
                FMLLog.severe("[召之马来] 严重警告！玩家 %s 数据损坏无法读取，已隔离封存至 %s，拒绝静默覆盖！",
                        player, corrupt.getName());
            }
        }

        return new PlayerHorseData(player);
    }

    public static File getCallableHorsePlayerDat(UUID player) {
        return getCallableHorsePlayerDat(DimensionManager.getCurrentSaveRootDirectory(), player);
    }

    private static File getCallableHorsePlayerDat(File root, UUID player) {
        File playerDir = new File(getOrCreateCallableHorseDir(root), "playerHorses");
        if (!playerDir.exists()) {
            playerDir.mkdirs();
        }
        return new File(playerDir, player.toString() + ".dat");
    }

    public static File getCallableHorseDat() {
        return new File(getOrCreateCallableHorseDir(), "CallableHorse.dat");
    }

    public static File getOrCreateCallableHorseDir() {
        return getOrCreateCallableHorseDir(DimensionManager.getCurrentSaveRootDirectory());
    }

    private static File getOrCreateCallableHorseDir(File root) {
        if (root == null) {
            throw new IllegalStateException("Minecraft save root is not available");
        }
        File file = new File(root, "CallableHorse");
        if (!file.exists() && !file.mkdirs() && !file.exists()) {
            throw new IllegalStateException("Cannot create " + file.getAbsolutePath());
        }
        return file;
    }

    public static NBTTagCompound loadNBTFromFile(File file) throws FileNotFoundException, IOException {
        if (file == null || !file.exists()) {
            return new NBTTagCompound();
        }
        FileInputStream input = new FileInputStream(file);
        try {
            return CompressedStreamTools.readCompressed(input);
        } finally {
            input.close();
        }
    }

    public static void saveData(UUID player) {
        if (player == null || isClientSide()) {
            return;
        }
        PlayerHorseData data = playerDataMap.get(player);
        if (data == null) {
            return;
        }
        try {
            NBTTagCompound nbt = new NBTTagCompound();
            synchronized (data) {
                data.save(nbt);
            }
            saveNBTToFile(getCallableHorsePlayerDat(player), nbt);
            FMLLog.info("Saved CallableHorse player data for %s", player);
        } catch (Exception e) {
            data.markDirty();
            FMLLog.severe("Error saving CallableHorse player data for %s", player);
            e.printStackTrace();
        }
    }

    public static void saveNBTToFile(File file, NBTTagCompound nbt) throws IOException {
        if (file == null || nbt == null) {
            return;
        }
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        File temporary = new File(file.getAbsolutePath() + ".tmp");
        File backup = new File(file.getAbsolutePath() + ".bak");

        // 1. 写入临时文件
        FileOutputStream output = new FileOutputStream(temporary);
        try {
            CompressedStreamTools.writeCompressed(nbt, output);
        } finally {
            output.close();
        }

        // 2. 将现有有效文件备份为 .bak，形成双保险
        if (file.exists()) {
            try {
                Files.copy(file.toPath(), backup.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (Throwable ignored) {
            }
        }

        // 3. 原子提交正式文件
        try {
            Files.move(temporary.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(temporary.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            if (file.exists() && !file.delete()) {
                throw e;
            }
            if (!temporary.renameTo(file)) {
                throw e;
            }
        }
    }

    public static void sendPlayerData(EntityPlayerMP entityplayer) {
        if (entityplayer == null || entityplayer.worldObj == null || entityplayer.worldObj.isRemote) {
            return;
        }
        try {
            PlayerHorseData data = getData(entityplayer);
            if (data != null) {
                data.sendPlayerData(entityplayer);
            }
        } catch (Exception e) {
            FMLLog.severe("Failed to send player data to player " + entityplayer.getCommandSenderName());
            e.printStackTrace();
        }
    }

    public static void save() {
        if (isClientSide()) {
            return;
        }
        boolean failed = false;
        for (Map.Entry<UUID, PlayerHorseData> entry : playerDataMap.entrySet()) {
            PlayerHorseData data = entry.getValue();
            if (data == null || !data.needsSave()) {
                continue;
            }
            try {
                saveData(entry.getKey());
                failed |= data.needsSave();
            } catch (Throwable throwable) {
                failed = true;
                FMLLog.severe("Error saving CallableHorse player data for %s", entry.getKey());
                throwable.printStackTrace();
            }
        }
        needsSave = failed;
    }

    public static boolean anyDataNeedsSave() {
        if (needsSave) {
            return true;
        }
        for (PlayerHorseData data : playerDataMap.values()) {
            if (data != null && data.needsSave()) {
                return true;
            }
        }
        return false;
    }

    public static void saveAndClearUnusedPlayerData() {
        MinecraftServer server = MinecraftServer.getServer();
        if (server == null) {
            return;
        }
        ArrayList<UUID> clearing = new ArrayList<UUID>();
        for (UUID player : playerDataMap.keySet()) {
            boolean foundPlayer = false;
            for (WorldServer world : server.worldServers) {
                if (world != null && world.func_152378_a(player) != null) {
                    foundPlayer = true;
                    break;
                }
            }
            if (!foundPlayer) {
                clearing.add(player);
            }
        }
        for (UUID player : clearing) {
            saveAndClearData(player);
        }
    }

    public static boolean saveAndClearData(UUID player) {
        PlayerHorseData data = playerDataMap.get(player);
        if (data == null) {
            return false;
        }
        if (data.needsSave()) {
            saveData(player);
        }
        // Keep the in-memory record when persistence failed.  Removing it
        // here would lose the only copy and make a later retry impossible.
        if (data.needsSave()) {
            needsSave = true;
            return false;
        }
        playerDataMap.remove(player, data);
        return true;
    }
}
