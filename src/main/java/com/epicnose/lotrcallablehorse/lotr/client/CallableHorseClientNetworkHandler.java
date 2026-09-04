package com.epicnose.lotrcallablehorse.lotr.client;

import com.epicnose.lotrcallablehorse.lotr.common.network.PacketLoginPlayerHorseData;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Owns the client-side lifetime of login-data chunk assembly.
 *
 * <p>Packets may arrive out of order or stop halfway through a transfer.  A
 * small periodic sweep prevents those partial buffers from surviving forever,
 * while the disconnect hook drops them immediately when a server connection
 * is closed.</p>
 */
@SideOnly(Side.CLIENT)
public final class CallableHorseClientNetworkHandler {
    /** One sweep per second is enough for the 30-second transfer timeout. */
    private static final int CLEANUP_INTERVAL_TICKS = 20;

    private final AtomicInteger connectionGeneration = new AtomicInteger();
    private int ticksSinceCleanup;

    public CallableHorseClientNetworkHandler() {
        FMLCommonHandler.instance().bus().register(this);
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event == null || event.phase != TickEvent.Phase.END) {
            return;
        }

        if (++ticksSinceCleanup >= CLEANUP_INTERVAL_TICKS) {
            ticksSinceCleanup = 0;
            PacketLoginPlayerHorseData.cleanupExpiredTransfers();
        }
    }

    @SubscribeEvent
    public void onClientDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        resetConnectionState();
    }

    @SubscribeEvent
    public void onClientConnect(FMLNetworkEvent.ClientConnectedToServerEvent event) {
        resetConnectionState();
    }

    int getConnectionGeneration() {
        return connectionGeneration.get();
    }

    boolean isCurrentConnection(int generation) {
        return connectionGeneration.get() == generation;
    }

    private void resetConnectionState() {
        // Invalidates main-thread tasks queued by the previous connection.
        connectionGeneration.incrementAndGet();
        PacketLoginPlayerHorseData.clearClientTransfers();
        ticksSinceCleanup = 0;
    }
}
