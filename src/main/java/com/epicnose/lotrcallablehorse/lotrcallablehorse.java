package com.epicnose.lotrcallablehorse;

import com.epicnose.lotrcallablehorse.lotr.common.CallableHorseCommonProxy;
import com.epicnose.lotrcallablehorse.lotr.common.CallableHorseConfig;
import com.epicnose.lotrcallablehorse.lotr.common.CallableHorseEventHandler;
import com.epicnose.lotrcallablehorse.lotr.common.CallableHorseLevelData;
import com.epicnose.lotrcallablehorse.lotr.common.CallableHorseServerTasks;
import com.epicnose.lotrcallablehorse.lotr.common.CallableHorseTickHandlerServer;
import com.epicnose.lotrcallablehorse.lotr.common.commands.CommandAddHorse;
import com.epicnose.lotrcallablehorse.lotr.common.commands.CommandCallHorse;
import com.epicnose.lotrcallablehorse.lotr.common.commands.CommandTestMount;
import com.epicnose.lotrcallablehorse.lotr.common.network.CallableHorsePacketHandler;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.event.FMLServerStartedEvent;
import cpw.mods.fml.common.event.FMLServerStoppingEvent;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;

@Mod(modid = "lotrcallablehorse", name = "LOTR CallableHorse",version = lotrcallablehorse.VERSION, dependencies = "after:lotr")
public class lotrcallablehorse
{


    @SidedProxy(clientSide = "com.epicnose.lotrcallablehorse.lotr.client.CallableHorseClientProxy",serverSide = "com.epicnose.lotrcallablehorse.lotr.common.CallableHorseCommonProxy")
    public static CallableHorseCommonProxy proxy;
//    @Mod.Instance(value = "lotrcallablehorse")

    public static final String MODID = "lotrcallablehorse";
    public static final String VERSION = "alpha-1.1.0";
    public static CallableHorseTickHandlerServer serverTickHandler;
    public static CallableHorseEventHandler modEventHandler;
    public static CallableHorsePacketHandler packetHandler;
    @EventHandler
    public void init(FMLInitializationEvent event)
    {
		// some example code
//        System.out.println("DIRT BLOCK >> "+Blocks.dirt.getUnlocalizedName());
//        serverTickHandler = new CallableHorseTickHandlerServer();

    }
    @Mod.EventHandler
    public void preload(FMLPreInitializationEvent event) {
        CallableHorseConfig.load(event.getSuggestedConfigurationFile());
        NetworkRegistry.INSTANCE.registerGuiHandler( this, proxy);
        serverTickHandler = new CallableHorseTickHandlerServer();
        modEventHandler = new CallableHorseEventHandler();
        packetHandler = new CallableHorsePacketHandler();
    }

    @Mod.EventHandler
    public void onServerStarting(FMLServerStartingEvent event)  {
        event.registerServerCommand(new CommandAddHorse());
        event.registerServerCommand(new CommandCallHorse());
        event.registerServerCommand(new CommandTestMount());
    }

    @Mod.EventHandler
    public void onServerStarted(FMLServerStartedEvent event) {
        CallableHorseServerTasks.beginServerSession();
        CallableHorseLevelData.load();
    }

    @Mod.EventHandler
    public void onServerStopping(FMLServerStoppingEvent event) {
        // Stop accepting network work before the final entity snapshot.
        CallableHorseServerTasks.endServerSession();
        CallableHorseEventHandler.recallAllTrackedVehicles();
        CallableHorseEventHandler.clearTracking();
        CallableHorseLevelData.resetForServer();
    }
//    @Mod.EventHandler
//    public void onServerStarting(FMLServerStartingEvent event) throws NoSuchFieldException, IllegalAccessException {
//        //在这里通过反射注册按钮
//
//        LOTRGuiMenu menu = new LOTRGuiMenu();
//
//        // 获取 buttonList 字段
//        Field buttonListField = GuiScreen.class.getDeclaredField("buttonList");
//
//        // 设置字段为可访问
//        buttonListField.setAccessible(true);
//
//        // 获取 buttonList 的值
//        ArrayList<?> buttonList = (ArrayList<?>) buttonListField.get(menu);
////        protected java.util.List buttonList = new ArrayList();
//        // 往 buttonList 中添加元素
////        buttonList.add("New Button");
//
//        buttonList.add(new HorseButton(menu,8,0,0, HorseGui.class,"LOTR-召唤载具",45));
//
//        buttonListField.set(menu,buttonList);
//        LOTRLog.logger.info("马按钮反射完毕");
//    }
//    @Mod.EventHandler
//    public void postload(FMLPostInitializationEvent event) throws NoSuchFieldException, IllegalAccessException {
//
//    }



}
