package com.epicnose.lotrcallablehorse.lotr.common.commands;

import com.epicnose.lotrcallablehorse.lotr.common.CallableHorseConfig;
import com.epicnose.lotrcallablehorse.lotr.common.CallableHorseLevelData;
import com.epicnose.lotrcallablehorse.lotr.common.CallableHorseMountSupport;
import com.epicnose.lotrcallablehorse.lotr.common.PlayerHorseData;
import com.epicnose.lotrcallablehorse.lotr.common.SingleVehicle;
import com.epicnose.lotrcallablehorse.lotr.common.mount.IMountAdapter;
import com.epicnose.lotrcallablehorse.lotr.common.mount.MountAdapterRegistry;
import cpw.mods.fml.common.FMLLog;
import lotr.common.entity.LOTREntities;
import lotr.common.entity.npc.LOTRNPCMount;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.World;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 创造模式专用：全系坐骑自动化测试与个人上限配置指令。
 * 指令体系：
 * - /lotrmount test [info | dryrun | fill | clear]
 * - /lotrmount limit [玩家名] [数量 | reset]
 */
public class CommandTestMount extends CommandBase {

    @Override
    public String getCommandName() {
        return "lotrmount";
    }

    @Override
    public List getCommandAliases() {
        return Arrays.asList("lotrhorse");
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0; // 内部针对创造模式和 OP 细粒度校验
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/lotrmount <test | limit | cooldown | log> [args...]";
    }

    @Override
    public boolean canCommandSenderUseCommand(ICommandSender sender) {
        return true;
    }

    private boolean checkAdminOrCreative(ICommandSender sender) {
        if (sender instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) sender;
            if (player.capabilities.isCreativeMode) {
                return true;
            }
        }
        return MinecraftServer.getServer() == null || MinecraftServer.getServer().isSinglePlayer()
                || super.canCommandSenderUseCommand(sender);
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length < 1) {
            sendUsage(sender);
            return;
        }

        String sub = args[0].toLowerCase();
        if ("test".equals(sub)) {
            handleTest(sender, args);
        } else if ("limit".equals(sub)) {
            handleLimit(sender, args);
        } else if ("cooldown".equals(sub) || "cd".equals(sub)) {
            handleCooldown(sender, args);
        } else if ("log".equals(sub)) {
            handleLog(sender, args);
        } else {
            sendUsage(sender);
        }
    }

    private void sendUsage(ICommandSender sender) {
        sender.addChatMessage(new ChatComponentText(EnumChatFormatting.GOLD + "=== [召之马来] 管理与测试指令 ==="));
        sender.addChatMessage(new ChatComponentText(EnumChatFormatting.YELLOW + "/lotrmount test info"
                + EnumChatFormatting.GRAY + " - 扫描当前魔戒环境所有受支持的可骑乘生物"));
        sender.addChatMessage(new ChatComponentText(EnumChatFormatting.YELLOW + "/lotrmount test dryrun"
                + EnumChatFormatting.GRAY + " - 执行全系生物无损内存测试(NBT/属性/还原度)"));
        sender.addChatMessage(new ChatComponentText(EnumChatFormatting.YELLOW + "/lotrmount test fill"
                + EnumChatFormatting.GRAY + " - 向自身卡槽填充全系坐骑样例"));
        sender.addChatMessage(new ChatComponentText(EnumChatFormatting.YELLOW + "/lotrmount test clear"
                + EnumChatFormatting.GRAY + " - 清空自身载具卡槽"));
        sender.addChatMessage(new ChatComponentText(EnumChatFormatting.YELLOW + "/lotrmount limit [数量]"
                + EnumChatFormatting.GRAY + " - 调整自身载具存储上限(默认: 3)"));
        sender.addChatMessage(new ChatComponentText(EnumChatFormatting.YELLOW + "/lotrmount limit <玩家名> <数量|reset>"
                + EnumChatFormatting.GRAY + " - 调整/重置目标玩家的载具上限"));
        sender.addChatMessage(new ChatComponentText(EnumChatFormatting.YELLOW + "/lotrmount cooldown [秒数]"
                + EnumChatFormatting.GRAY + " - 查询或修改全局召唤冷却时间(0-3600秒, 0为无冷却)"));
        sender.addChatMessage(new ChatComponentText(EnumChatFormatting.YELLOW + "/lotrmount log [on|off]"
                + EnumChatFormatting.GRAY + " - 查询或开关玩家操作日志上报服务端控制台(默认: 开启)"));
    }

    // ==================== 测试子指令逻辑 ====================

    private void handleTest(ICommandSender sender, String[] args) {
        if (!checkAdminOrCreative(sender)) {
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "[召之马来] 只有创造模式玩家或管理员可以执行测试！"));
            return;
        }
        String testAction = args.length >= 2 ? args[1].toLowerCase() : "info";

        if ("info".equals(testAction)) {
            List<Class<? extends Entity>> mountClasses = scanAllMountClasses();
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.GREEN + String.format(
                    "[召之马来-测试] 扫描完成：当前环境共发现 %d 种 LOTR 可骑乘生物实体：", mountClasses.size())));
            int horses = 0, wargs = 0, spiders = 0, others = 0;
            for (Class<? extends Entity> cls : mountClasses) {
                String name = cls.getSimpleName();
                if (name.contains("Warg")) wargs++;
                else if (name.contains("Spider")) spiders++;
                else if (name.contains("Horse") || name.contains("Elk") || name.contains("Boar")
                        || name.contains("Camel") || name.contains("Giraffe") || name.contains("Rhino")
                        || name.contains("Zebra") || name.contains("Mumakil") || name.contains("Ram")) horses++;
                else others++;
            }
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.AQUA + String.format(
                    "  - 马系/动物系: %d 种 | 座狼系: %d 种 | 巨蛛系: %d 种 | 其它: %d 种",
                    horses, wargs, spiders, others)));
        } else if ("dryrun".equals(testAction)) {
            runDryRunTest(sender);
        } else if ("fill".equals(testAction)) {
            runFillTest(sender);
        } else if ("clear".equals(testAction)) {
            runClearTest(sender);
        } else {
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "未知的测试子指令: " + testAction));
        }
    }

    private void runDryRunTest(ICommandSender sender) {
        World world = sender.getEntityWorld();
        if (world == null) {
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "无法获取世界上下文！"));
            return;
        }
        EntityPlayer player = sender instanceof EntityPlayer ? (EntityPlayer) sender : null;

        List<Class<? extends Entity>> mountClasses = scanAllMountClasses();
        sender.addChatMessage(new ChatComponentText(EnumChatFormatting.YELLOW + String.format(
                "[召之马来-测试] 开始执行全系生物无损内存体检（共 %d 种）...", mountClasses.size())));

        int passed = 0;
        int failed = 0;

        for (Class<? extends Entity> cls : mountClasses) {
            String className = cls.getSimpleName();
            try {
                String entityName = LOTREntities.getStringFromClass(cls);
                Entity entity = entityName != null ? EntityList.createEntityByName(entityName, world) : null;
                if (entity == null) {
                    try {
                        entity = cls.getConstructor(World.class).newInstance(world);
                    } catch (Throwable ignored) {
                    }
                }
                if (entity == null) {
                    FMLLog.warning("[召之马来-测试] 无法实例化生物: %s", className);
                    failed++;
                    continue;
                }

                IMountAdapter adapter = MountAdapterRegistry.getAdapter(entity);
                if (player != null) {
                    adapter.setTamed(entity, player);
                }
                adapter.setSaddled(entity, true);

                // 验证录入模型
                SingleVehicle sv = new SingleVehicle(entity);
                if (!sv.isValid) {
                    FMLLog.warning("[召之马来-测试] 实体未通过 SingleVehicle 校验: %s", className);
                    failed++;
                    entity.setDead();
                    continue;
                }

                // 验证 NBT 序列化与反序列化
                NBTTagCompound nbt = new NBTTagCompound();
                sv.writeToNBT(nbt);

                SingleVehicle restored = new SingleVehicle();
                restored.readFromNBT(nbt);

                // 验证预览实体创建
                EntityLivingBase preview = restored.createPreviewEntity(world);
                if (preview == null) {
                    FMLLog.warning("[召之马来-测试] 实体还原预览失败: %s", className);
                    failed++;
                    entity.setDead();
                    continue;
                }

                // 清理临时实体
                entity.setDead();
                preview.setDead();
                passed++;
            } catch (Throwable t) {
                FMLLog.severe("[召之马来-测试] 生物测试发生异常: %s, 原因: %s", className, t.toString());
                failed++;
            }
        }

        EnumChatFormatting color = failed == 0 ? EnumChatFormatting.GREEN : EnumChatFormatting.RED;
        sender.addChatMessage(new ChatComponentText(color + String.format(
                "[召之马来-测试] 兼容性诊断完成！测试总数: %d | 成功通过: %d | 失败: %d (通过率: %.1f%%)",
                mountClasses.size(), passed, failed, (passed * 100.0F / Math.max(1, mountClasses.size())))));
    }

    private void runFillTest(ICommandSender sender) {
        if (!(sender instanceof EntityPlayerMP)) {
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "该命令仅限玩家执行！"));
            return;
        }
        EntityPlayerMP player = (EntityPlayerMP) sender;
        PlayerHorseData data = CallableHorseLevelData.getData(player);
        if (data == null || data.horseInfo == null) {
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "无法获取玩家数据！"));
            return;
        }

        // 扩容槽位以装下样本
        if (data.getMaxSlots() < 10) {
            data.setCustomMaxSlots(10);
        }

        // 清理已有
        data.horseInfo.vehicles.clear();

        World world = player.worldObj;
        List<Class<? extends Entity>> mountClasses = scanAllMountClasses();
        int added = 0;

        for (Class<? extends Entity> cls : mountClasses) {
            if (added >= data.getMaxSlots()) break;
            try {
                String entityName = LOTREntities.getStringFromClass(cls);
                Entity entity = entityName != null ? EntityList.createEntityByName(entityName, world) : null;
                if (entity == null) {
                    try {
                        entity = cls.getConstructor(World.class).newInstance(world);
                    } catch (Throwable ignored) {
                    }
                }
                if (entity instanceof EntityLivingBase) {
                    entity.setPosition(player.posX, player.posY, player.posZ);
                    IMountAdapter adapter = MountAdapterRegistry.getAdapter(entity);
                    adapter.setTamed(entity, player);
                    adapter.setSaddled(entity, true);

                    SingleVehicle sv = new SingleVehicle(entity);
                    if (sv.isValid) {
                        sv.index = data.horseInfo.vehicles.size();
                        sv.ownerUUID = player.getUniqueID();
                        data.horseInfo.vehicles.add(sv);
                        added++;
                    }
                    entity.setDead();
                }
            } catch (Throwable ignored) {
            }
        }

        data.markDirty();
        CallableHorseLevelData.saveData(player.getUniqueID());
        CallableHorseLevelData.sendPlayerData(player);

        CallableHorseConfig.logOperation("玩家 %s (UUID: %s) 运行测试命令注入了 %d 个测试样本载具",
                player.getCommandSenderName(), player.getUniqueID(), added);
        sender.addChatMessage(new ChatComponentText(EnumChatFormatting.GREEN + String.format(
                "[召之马来-测试] 已成功为您注入 %d 个不同品类的坐骑测试样本！请打开 GUI 浏览与召唤！", added)));
    }

    private void runClearTest(ICommandSender sender) {
        if (!(sender instanceof EntityPlayerMP)) {
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "该命令仅限玩家执行！"));
            return;
        }
        EntityPlayerMP player = (EntityPlayerMP) sender;
        PlayerHorseData data = CallableHorseLevelData.getData(player);
        if (data != null && data.horseInfo != null) {
            data.horseInfo.vehicles.clear();
            data.markDirty();
            CallableHorseLevelData.saveData(player.getUniqueID());
            CallableHorseLevelData.sendPlayerData(player);
            CallableHorseConfig.logOperation("玩家 %s (UUID: %s) 运行测试命令清空了全部载具卡槽",
                    player.getCommandSenderName(), player.getUniqueID());
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.GREEN + "[召之马来-测试] 已成功清空您的所有载具卡槽！"));
        }
    }

    // ==================== 个人上限指令逻辑 ====================

    private void handleLimit(ICommandSender sender, String[] args) {
        // /lotrmount limit [数量]
        // /lotrmount limit <玩家名> <数量|reset>
        if (args.length == 1) {
            // 查询自己
            if (sender instanceof EntityPlayer) {
                EntityPlayer player = (EntityPlayer) sender;
                showPlayerLimit(sender, player);
            } else {
                sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "控制台查询请指定玩家名: /lotrmount limit <玩家名>"));
            }
            return;
        }

        if (args.length == 2) {
            // 可能是 /lotrmount limit <数量> 或 /lotrmount limit <玩家名>
            String arg = args[1];
            if (isInteger(arg) && sender instanceof EntityPlayerMP) {
                if (!checkAdminOrCreative(sender)) {
                    sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "[召之马来] 只有创造模式玩家或管理员可以修改存储上限！"));
                    return;
                }
                int newLimit = parseIntBounded(sender, arg, 1, CallableHorseConfig.MAX_STORED_VEHICLES);
                EntityPlayerMP player = (EntityPlayerMP) sender;
                setPlayerLimit(sender, player, newLimit);
                return;
            }

            // 查询目标玩家
            EntityPlayerMP target = getPlayer(sender, arg);
            if (target != null) {
                showPlayerLimit(sender, target);
            }
            return;
        }

        if (args.length >= 3) {
            if (!checkAdminOrCreative(sender)) {
                sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "[召之马来] 只有创造模式玩家或管理员可以修改存储上限！"));
                return;
            }
            // /lotrmount limit <玩家名> <数量|reset>
            EntityPlayerMP target = getPlayer(sender, args[1]);
            if (target == null) return;

            String action = args[2].toLowerCase();
            if ("reset".equals(action)) {
                resetPlayerLimit(sender, target);
            } else if (isInteger(action)) {
                int newLimit = parseIntBounded(sender, action, 1, CallableHorseConfig.MAX_STORED_VEHICLES);
                setPlayerLimit(sender, target, newLimit);
            } else {
                sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "参数无效！请指定数量(1-100)或 'reset'。"));
            }
        }
    }

    private void showPlayerLimit(ICommandSender sender, EntityPlayer player) {
        PlayerHorseData data = CallableHorseLevelData.getData(player);
        int currentLimit = data != null ? data.getMaxSlots() : CallableHorseConfig.getConfiguredSlotCount();
        int used = data != null && data.horseInfo != null ? data.horseInfo.getVehicleCount() : 0;
        int custom = data != null ? data.getCustomMaxSlots() : -1;

        sender.addChatMessage(new ChatComponentText(EnumChatFormatting.AQUA + String.format(
                "[召之马来] 玩家 %s 当前载具状态：已用 %d / 上限 %d (个人定制: %s, 全局默认: %d)",
                player.getCommandSenderName(), used, currentLimit,
                (custom > 0 ? custom + " 个" : "跟随全局默认"), CallableHorseConfig.getConfiguredSlotCount())));
    }

    private void setPlayerLimit(ICommandSender sender, EntityPlayerMP target, int newLimit) {
        PlayerHorseData data = CallableHorseLevelData.getData(target);
        if (data != null) {
            data.setCustomMaxSlots(newLimit);
            CallableHorseLevelData.saveData(target.getUniqueID());
            CallableHorseLevelData.sendPlayerData(target);

            CallableHorseConfig.logOperation("管理员 %s 将玩家 %s (UUID: %s) 的载具存储上限调整为 %d",
                    sender.getCommandSenderName(), target.getCommandSenderName(), target.getUniqueID(), newLimit);
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.GREEN + String.format(
                    "[召之马来] 成功将玩家 %s 的载具存储上限调整为 %d 个（默认: %d）！",
                    target.getCommandSenderName(), newLimit, CallableHorseConfig.getConfiguredSlotCount())));
            if (sender != target) {
                target.addChatMessage(new ChatComponentText(EnumChatFormatting.GOLD + String.format(
                        "[召之马来] 管理员已将您的载具存储上限调整为 %d 个！", newLimit)));
            }
        }
    }

    private void resetPlayerLimit(ICommandSender sender, EntityPlayerMP target) {
        PlayerHorseData data = CallableHorseLevelData.getData(target);
        if (data != null) {
            data.setCustomMaxSlots(-1);
            CallableHorseLevelData.saveData(target.getUniqueID());
            CallableHorseLevelData.sendPlayerData(target);

            CallableHorseConfig.logOperation("管理员 %s 重置了玩家 %s (UUID: %s) 的载具存储上限为全局默认值 (%d)",
                    sender.getCommandSenderName(), target.getCommandSenderName(), target.getUniqueID(), CallableHorseConfig.getConfiguredSlotCount());
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.GREEN + String.format(
                    "[召之马来] 已重置玩家 %s 的载具存储上限为全局默认值（%d 个）！",
                    target.getCommandSenderName(), CallableHorseConfig.getConfiguredSlotCount())));
            if (sender != target) {
                target.addChatMessage(new ChatComponentText(EnumChatFormatting.GOLD + String.format(
                        "[召之马来] 您的载具存储上限已被重置为默认值（%d 个）。", CallableHorseConfig.getConfiguredSlotCount())));
            }
        }
    }

    // ==================== 冷却时间指令逻辑 ====================

    private void handleCooldown(ICommandSender sender, String[] args) {
        if (args.length == 1) {
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.GREEN
                    + String.format("[召之马来] 当前全局坐骑召唤冷却时间为: %d 秒%s (默认: 60秒)",
                    CallableHorseConfig.summonCooldownSeconds,
                    CallableHorseConfig.summonCooldownSeconds == 0 ? " [无冷却]" : "")));
            return;
        }

        if (!checkAdminOrCreative(sender)) {
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "[召之马来] 只有创造模式玩家或管理员可以修改召唤冷却时间！"));
            return;
        }

        try {
            int newCooldown = Integer.parseInt(args[1]);
            if (newCooldown < 0 || newCooldown > 3600) {
                sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "[召之马来] 冷却时间范围必须在 0 到 3600 秒之间！"));
                return;
            }
            CallableHorseConfig.setSummonCooldown(newCooldown);
            CallableHorseConfig.logOperation("管理员 %s 将全局坐骑召唤冷却时间调整为 %d 秒",
                    sender.getCommandSenderName(), newCooldown);
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.GREEN
                    + String.format("[召之马来] 成功将全局坐骑召唤冷却时间调整为 %d 秒%s！配置已实时写入磁盘生效！",
                    newCooldown, newCooldown == 0 ? " (已关闭冷却)" : "")));
        } catch (NumberFormatException e) {
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "[召之马来] 请输入合法的整数秒数！格式: /lotrmount cooldown <秒数>"));
        }
    }

    // ==================== 操作审计日志开关指令逻辑 ====================

    private void handleLog(ICommandSender sender, String[] args) {
        if (args.length == 1 || (args.length == 2 && "status".equalsIgnoreCase(args[1]))) {
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.AQUA
                    + String.format("[召之马来] 当前玩家操作服务端审计日志上报状态: %s (默认: 开启)",
                    CallableHorseConfig.logPlayerOperations ? EnumChatFormatting.GREEN + "开启 (ON)" : EnumChatFormatting.RED + "关闭 (OFF)")));
            return;
        }

        if (!checkAdminOrCreative(sender)) {
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "[召之马来] 只有创造模式玩家或管理员可以修改操作日志上报开关！"));
            return;
        }

        String sub = args[1].toLowerCase();
        boolean enable;
        if ("on".equals(sub) || "true".equals(sub) || "1".equals(sub) || "enable".equals(sub)) {
            enable = true;
        } else if ("off".equals(sub) || "false".equals(sub) || "0".equals(sub) || "disable".equals(sub)) {
            enable = false;
        } else {
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "[召之马来] 参数无效！请使用: /lotrmount log [on|off|status]"));
            return;
        }

        CallableHorseConfig.setLogPlayerOperations(enable);
        CallableHorseConfig.logOperation("管理员 %s 将玩家操作审计日志上报开关调整为: %s",
                sender.getCommandSenderName(), enable ? "开启" : "关闭");
        sender.addChatMessage(new ChatComponentText(EnumChatFormatting.GREEN
                + String.format("[召之马来] 成功将玩家操作服务端审计日志上报调整为: %s！配置已实时写入磁盘生效！",
                enable ? EnumChatFormatting.GOLD + "开启" : EnumChatFormatting.RED + "关闭")));
    }

    private static boolean isInteger(String s) {
        try {
            Integer.parseInt(s);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    // ==================== 反射扫描 LOTR 坐骑类 ====================

    @SuppressWarnings("unchecked")
    private static List<Class<? extends Entity>> scanAllMountClasses() {
        List<Class<? extends Entity>> list = new ArrayList<Class<? extends Entity>>();

        // 1. 优先从 LOTR 自身官方注册表扫描全部已注册实体
        try {
            if (LOTREntities.classToIDMapping != null) {
                for (Class<? extends Entity> cls : LOTREntities.classToIDMapping.keySet()) {
                    if (cls != null && LOTRNPCMount.class.isAssignableFrom(cls)) {
                        if (!list.contains(cls)) {
                            list.add(cls);
                        }
                    }
                }
            }
        } catch (Throwable t) {
            FMLLog.warning("[召之马来-测试] 扫描 LOTREntities 映射表失败: %s", t.toString());
        }

        // 2. 双混淆兼容 (开发环境 MCP stringToClassMapping / 生产与服务端 SRG field_75625_b)
        try {
            Field stringToClassMapping = null;
            try {
                stringToClassMapping = EntityList.class.getDeclaredField("stringToClassMapping");
            } catch (NoSuchFieldException e) {
                try {
                    stringToClassMapping = EntityList.class.getDeclaredField("field_75625_b");
                } catch (Throwable ignored) {
                }
            }
            if (stringToClassMapping != null) {
                stringToClassMapping.setAccessible(true);
                Map<String, Class<? extends Entity>> mapping = (Map<String, Class<? extends Entity>>) stringToClassMapping.get(null);
                if (mapping != null) {
                    for (Class<? extends Entity> cls : mapping.values()) {
                        if (cls != null && (LOTRNPCMount.class.isAssignableFrom(cls) || net.minecraft.entity.passive.EntityHorse.class.isAssignableFrom(cls))) {
                            if (!list.contains(cls)) {
                                list.add(cls);
                            }
                        }
                    }
                }
            }
        } catch (Throwable t) {
            FMLLog.warning("[召之马来-测试] 扫描 EntityList 映射表失败: %s", t.toString());
        }

        // 3. 原版可骑乘马匹保底加入
        if (!list.contains(net.minecraft.entity.passive.EntityHorse.class)) {
            list.add(net.minecraft.entity.passive.EntityHorse.class);
        }

        return list;
    }

    @Override
    public List addTabCompletionOptions(ICommandSender sender, String[] args) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, "test", "limit", "cooldown", "log");
        }
        if (args.length == 2 && "test".equalsIgnoreCase(args[0])) {
            return getListOfStringsMatchingLastWord(args, "info", "dryrun", "fill", "clear");
        }
        if (args.length == 2 && "limit".equalsIgnoreCase(args[0])) {
            return getListOfStringsMatchingLastWord(args, MinecraftServer.getServer().getAllUsernames());
        }
        if (args.length == 3 && "limit".equalsIgnoreCase(args[0])) {
            return getListOfStringsMatchingLastWord(args, "3", "5", "10", "20", "reset");
        }
        if (args.length == 2 && ("cooldown".equalsIgnoreCase(args[0]) || "cd".equalsIgnoreCase(args[0]))) {
            return getListOfStringsMatchingLastWord(args, "0", "10", "30", "60", "120");
        }
        if (args.length == 2 && "log".equalsIgnoreCase(args[0])) {
            return getListOfStringsMatchingLastWord(args, "on", "off", "status");
        }
        return null;
    }
}
