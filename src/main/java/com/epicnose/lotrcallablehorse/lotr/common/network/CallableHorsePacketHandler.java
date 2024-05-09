package com.epicnose.lotrcallablehorse.lotr.common.network;

import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.relauncher.Side;

public class CallableHorsePacketHandler {
    public static SimpleNetworkWrapper networkWrapper = NetworkRegistry.INSTANCE.newSimpleChannel("lotrhorse_");

    public CallableHorsePacketHandler(){
        int id=0;
        networkWrapper.registerMessage(PacketLoginPlayerHorseData.Handler.class, PacketLoginPlayerHorseData.class, id++, Side.CLIENT);
        networkWrapper.registerMessage(PacketSingleHorseInfo.Handler.class,PacketSingleHorseInfo.class,id++, Side.CLIENT);
        networkWrapper.registerMessage(PacketCallHorse.Handler.class,PacketCallHorse.class,id++,Side.SERVER);
        networkWrapper.registerMessage(PacketCallBackHorse.Handler.class,PacketCallBackHorse.class,id++,Side.SERVER);
        networkWrapper.registerMessage(PacketReleaseHorse.Handler.class,PacketReleaseHorse.class,id++,Side.SERVER);
        System.out.println("LOTRCallableHorse: Registered " + id + " packet types");
//        LOTRLog.logger.info("LOTRCallableHorse: Registered " + id + " packet types");
    }

}
