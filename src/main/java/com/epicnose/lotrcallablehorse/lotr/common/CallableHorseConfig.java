package com.epicnose.lotrcallablehorse.lotr.common;

import net.minecraftforge.common.config.Configuration;

import java.io.File;

public final class CallableHorseConfig {
    private static final int DEFAULT_MAX_VEHICLES = 3;
    public static final int MAX_STORED_VEHICLES = 100;

    public static int maxVehicleSlots = DEFAULT_MAX_VEHICLES;
    public static int summonCooldownSeconds = 60;
    public static int autoRecallDistance = 0;
    public static boolean logPlayerOperations = true;
    private static Configuration configuration;

    private CallableHorseConfig() {
    }

    public static void load(File configFile) {
        Configuration config = new Configuration(configFile);
        configuration = config;
        try {
            config.load();
            config.setCategoryComment(Configuration.CATEGORY_GENERAL,
                    "【魔戒 - 召之马来】模组基础与服务端运行配置选项。");

            maxVehicleSlots = config.get(
                    Configuration.CATEGORY_GENERAL,
                    "maxVehicleSlots",
                    DEFAULT_MAX_VEHICLES,
                    "每位玩家默认可存储的魔戒载具数量上限 (取值范围: 1-100，默认值: 3)。\n"
                    + "管理员或创造模式玩家亦可在游戏内使用 /lotrmount limit 指令对特定玩家进行独立的个性化上限调整。")
                    .getInt(DEFAULT_MAX_VEHICLES);
            maxVehicleSlots = Math.max(1, Math.min(MAX_STORED_VEHICLES, maxVehicleSlots));

            summonCooldownSeconds = config.get(
                    Configuration.CATEGORY_GENERAL,
                    "summonCooldownSeconds",
                    60,
                    "玩家每次召唤坐骑的全局冷却时间 (单位: 秒，取值范围: 0-3600，默认值: 60，设为 0 则无冷却随点随召)。\n"
                    + "管理员或创造模式玩家可在游戏内使用 /lotrmount cooldown <秒数> 随时动态调整并自动存盘；处于创造模式的玩家自动免除冷却限制。")
                    .getInt(60);
            summonCooldownSeconds = Math.max(0, Math.min(3600, summonCooldownSeconds));

            autoRecallDistance = config.get(
                    Configuration.CATEGORY_GENERAL,
                    "autoRecallDistance",
                    0,
                    "当玩家远离已召唤的坐骑时，触发自动强制收回的超距阈值 (单位: 方块格数，默认值: 0 表示禁用距离收回，坐骑将安静待命保留纯正 RPG 沉浸感)。\n"
                    + "本模组具备完善的区块卸载拦截机制，即使区块卸载坐骑也绝不会丢失。若大型服务器需要激进清理实体，可设为 128 或 256。")
                    .getInt(0);
            autoRecallDistance = Math.max(0, autoRecallDistance);

            logPlayerOperations = config.get(
                    Configuration.CATEGORY_GENERAL,
                    "logPlayerOperations",
                    true,
                    "是否将玩家的所有【召之马来】关键操作（登记载具、召唤载具、召回载具、销毁载具）详细上报至服务端控制台与运行日志 (Server Log)。\n"
                    + "默认值: true。开启后便于管理员随时通过检索日志审计玩家的载具行为与防刷记录。\n"
                    + "游戏内管理命令: 管理员或创造模式玩家可在游戏内使用 /lotrmount log <on|off> 随时动态开启/关闭上报并自动存盘；使用 /lotrmount log 查询当前状态。")
                    .getBoolean(true);
        } finally {
            if (config.hasChanged()) {
                config.save();
            }
        }
    }

    public static boolean isValidSlotIndex(int index) {
        return index >= 0 && index < getConfiguredSlotCount();
    }

    /**
     * Validates an index against the hard storage limit rather than the
     * current registration limit.  This keeps mounts already stored by a
     * previous configuration available after an administrator lowers the
     * registration cap.
     */
    public static boolean isValidStoredSlotIndex(int index) {
        return index >= 0 && index < MAX_STORED_VEHICLES;
    }

    /**
     * Returns a clamped value even while Forge is still constructing the
     * configuration. Keeping this in one place prevents commands, packets and
     * NBT loading from disagreeing about the active slot range.
     */
    public static int getConfiguredSlotCount() {
        return Math.max(1, Math.min(MAX_STORED_VEHICLES, maxVehicleSlots));
    }

    /** Dynamically updates summon cooldown seconds and saves to disk. */
    public static void setSummonCooldown(int seconds) {
        summonCooldownSeconds = Math.max(0, Math.min(3600, seconds));
        if (configuration != null) {
            try {
                configuration.get(Configuration.CATEGORY_GENERAL, "summonCooldownSeconds", 60)
                        .set(summonCooldownSeconds);
                configuration.save();
            } catch (Throwable ignored) {
            }
        }
    }

    /** Dynamically updates player operation logging and saves to disk. */
    public static void setLogPlayerOperations(boolean enable) {
        logPlayerOperations = enable;
        if (configuration != null) {
            try {
                configuration.get(Configuration.CATEGORY_GENERAL, "logPlayerOperations", true)
                        .set(logPlayerOperations);
                configuration.save();
            } catch (Throwable ignored) {
            }
        }
    }

    /** Formats and logs player operation info to server console/log if enabled. */
    public static void logOperation(String format, Object... args) {
        if (logPlayerOperations) {
            cpw.mods.fml.common.FMLLog.info("[召之马来-操作审计] " + String.format(format, args));
        }
    }
}
