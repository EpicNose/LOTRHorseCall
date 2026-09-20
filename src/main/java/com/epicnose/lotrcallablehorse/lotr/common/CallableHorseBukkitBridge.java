package com.epicnose.lotrcallablehorse.lotr.common;

import cpw.mods.fml.common.FMLLog;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * 针对 Thermos / Cauldron / Crucible / Uranium / CatServer / Mohist 等 Forge+Bukkit 混合端的权限兼容桥接器。
 * 解决混合端中普通玩家执行模组指令时被 Bukkit 拦截并提示
 * "I'm sorry, but you do not have permission to perform this command" 的经典问题。
 */
public class CallableHorseBukkitBridge {

    public static void registerPermissions() {
        try {
            Class<?> bukkitClass = Class.forName("org.bukkit.Bukkit");
            Method getServerMethod = bukkitClass.getMethod("getServer");
            Object server = getServerMethod.invoke(null);
            if (server == null) {
                return;
            }

            // 1. 获取 PluginManager 并将指令权限节点注册为 PermissionDefault.TRUE（所有玩家默认拥有权限）
            try {
                Method getPluginManagerMethod = server.getClass().getMethod("getPluginManager");
                Object pluginManager = getPluginManagerMethod.invoke(server);
                if (pluginManager != null) {
                    Class<?> permDefClass = Class.forName("org.bukkit.permissions.PermissionDefault");
                    @SuppressWarnings("unchecked")
                    Object permDefTrue = Enum.valueOf((Class<Enum>) permDefClass, "TRUE");

                    Class<?> permClass = Class.forName("org.bukkit.permissions.Permission");
                    Method addPermMethod = pluginManager.getClass().getMethod("addPermission", permClass);
                    Method getPermMethod = pluginManager.getClass().getMethod("getPermission", String.class);
                    Method setDefaultMethod = permClass.getMethod("setDefault", permDefClass);

                    String[] nodes = new String[]{
                            "com.epicnose.lotrcallablehorse.lotr.common.commands.CommandAddHorse",
                            "com.epicnose.lotrcallablehorse.lotr.common.commands.CommandCallHorse",
                            "com.epicnose.lotrcallablehorse.lotr.common.commands.CommandTestMount",
                            "lotrcallablehorse.command.addhorse",
                            "lotrcallablehorse.command.callhorse",
                            "lotrcallablehorse.command.lotrmount",
                            "addhorse",
                            "callhorse",
                            "lotrmount",
                            "lotrhorse"
                    };

                    for (String node : nodes) {
                        try {
                            Object existing = getPermMethod.invoke(pluginManager, node);
                            if (existing != null) {
                                setDefaultMethod.invoke(existing, permDefTrue);
                            } else {
                                Object newPerm = permClass.getConstructor(String.class, permDefClass).newInstance(node, permDefTrue);
                                addPermMethod.invoke(pluginManager, newPerm);
                            }
                        } catch (Throwable ignored) {
                        }
                    }
                }
            } catch (Throwable t) {
                FMLLog.warning("[召之马来] Bukkit PluginManager 权限注册兼容处理异常: %s", t.toString());
            }

            // 2. 获取 CommandMap 并清除命令本身的权限拦截 (setPermission(null))
            try {
                Object commandMap = null;
                try {
                    Method getCommandMapMethod = server.getClass().getMethod("getCommandMap");
                    commandMap = getCommandMapMethod.invoke(server);
                } catch (Throwable e) {
                    try {
                        Field f = server.getClass().getDeclaredField("commandMap");
                        f.setAccessible(true);
                        commandMap = f.get(server);
                    } catch (Throwable ignored) {
                    }
                }

                if (commandMap != null) {
                    Method getCommandMethod = commandMap.getClass().getMethod("getCommand", String.class);
                    String[] cmdNames = new String[]{"addhorse", "callhorse", "lotrmount", "lotrhorse"};

                    for (String name : cmdNames) {
                        try {
                            Object cmd = getCommandMethod.invoke(commandMap, name);
                            if (cmd != null) {
                                Method setPermMethod = cmd.getClass().getMethod("setPermission", String.class);
                                setPermMethod.invoke(cmd, new Object[]{null});
                            }
                        } catch (Throwable ignored) {
                        }
                    }
                }
            } catch (Throwable t) {
                FMLLog.warning("[召之马来] Bukkit CommandMap 权限穿透兼容处理异常: %s", t.toString());
            }

            FMLLog.info("[召之马来] 混合端 (Thermos/Cauldron/Crucible) 权限穿透与默认权限注册完毕。");
        } catch (ClassNotFoundException e) {
            // 纯净 Forge 服务端或单人客户端环境，无需 Bukkit 处理
        } catch (Throwable t) {
            FMLLog.warning("[召之马来] 混合端兼容检测发生异常: %s", t.toString());
        }
    }
}
