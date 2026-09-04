package com.epicnose.lotrcallablehorse.lotr.common.network;

import com.epicnose.lotrcallablehorse.lotrcallablehorse;
import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTSizeTracker;
import net.minecraft.nbt.NBTTagCompound;

import java.io.IOException;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.CRC32;

/**
 * Synchronizes the per-player vehicle data without using PacketBuffer's
 * signed-short NBT length. The compressed NBT is split into small, bounded
 * chunks and reassembled on the client before it is applied to game state.
 */
public class PacketLoginPlayerHorseData implements IMessage {
    private static final int MAGIC = 0x4C484350; // "LHCP"
    private static final int PROTOCOL_VERSION = 1;

    /** Keep every encoded message comfortably below the 32767-byte NBT limit. */
    public static final int CHUNK_SIZE = 24 * 1024;
    /** Guard both server mistakes and malicious client payloads. */
    public static final int MAX_TRANSFER_BYTES = 8 * 1024 * 1024;
    private static final int MAX_NBT_BYTES = 16 * 1024 * 1024;
    private static final int MAX_ACTIVE_TRANSFERS = 4;
    private static final long TRANSFER_TIMEOUT_MILLIS = 30_000L;
    private static final int HEADER_BYTES = 4 + 1 + (6 * 4);
    private static final AtomicInteger NEXT_TRANSFER_ID = new AtomicInteger(1);

    private static final Object TRANSFER_LOCK = new Object();
    private static final Map<Integer, IncomingTransfer> INCOMING_TRANSFERS =
            new HashMap<Integer, IncomingTransfer>();

    /** Kept for source compatibility with the original packet class. */
    public NBTTagCompound playerData;

    private int transferId;
    private int chunkIndex;
    private int chunkCount;
    private int totalLength;
    private int checksum;
    private int payloadLength;
    private byte[] payload;
    private int payloadOffset;
    private boolean valid;

    public PacketLoginPlayerHorseData() {
        // Fields are populated by fromBytes on the receiving side.
    }

    /**
     * Creates a one-chunk packet for callers that still construct the message
     * directly. PlayerHorseData uses sendTo(...) so large data is split.
     */
    public PacketLoginPlayerHorseData(NBTTagCompound nbt) {
        this.playerData = nbt == null ? new NBTTagCompound() : (NBTTagCompound) nbt.copy();
        try {
            byte[] compressed = CompressedStreamTools.compress(this.playerData);
            if (compressed.length > MAX_TRANSFER_BYTES || compressed.length > CHUNK_SIZE) {
                throw new IOException("CallableHorse player data does not fit in one packet");
            }
            initializeChunk(nextTransferId(), 0, 1, compressed.length,
                    checksum(compressed), compressed, 0, compressed.length);
        } catch (IOException e) {
            FMLLog.severe("Error compressing CallableHorse login player data: " + e.getMessage());
            invalidate();
        }
    }

    private PacketLoginPlayerHorseData(int transferId, int chunkIndex, int chunkCount,
                                       int totalLength, int checksum, byte[] payload,
                                       int payloadOffset, int payloadLength) {
        initializeChunk(transferId, chunkIndex, chunkCount, totalLength, checksum,
                payload, payloadOffset, payloadLength);
    }

    private void initializeChunk(int transferId, int chunkIndex, int chunkCount,
                                 int totalLength, int checksum, byte[] payload,
                                 int payloadOffset, int payloadLength) {
        this.transferId = transferId;
        this.chunkIndex = chunkIndex;
        this.chunkCount = chunkCount;
        this.totalLength = totalLength;
        this.checksum = checksum;
        this.payload = payload;
        this.payloadOffset = payloadOffset;
        this.payloadLength = payloadLength;
        this.valid = isValidChunkValues(transferId, chunkIndex, chunkCount, totalLength,
                payload, payloadOffset, payloadLength);
    }

    private void invalidate() {
        this.valid = false;
        this.playerData = null;
        this.payload = null;
        this.payloadOffset = 0;
        this.payloadLength = 0;
    }

    /** Sends a compressed NBT transfer, splitting it into bounded messages. */
    public static void sendTo(NBTTagCompound nbt, EntityPlayerMP player) throws IOException {
        if (player == null) {
            return;
        }
        NBTTagCompound safeData = nbt == null ? new NBTTagCompound() : nbt;
        byte[] compressed = CompressedStreamTools.compress(safeData);
        if (compressed.length <= 0 || compressed.length > MAX_TRANSFER_BYTES) {
            throw new IOException("CallableHorse player data is too large: " + compressed.length);
        }

        int chunkCount = (int) (((long) compressed.length + CHUNK_SIZE - 1L) / CHUNK_SIZE);
        if (chunkCount <= 0 || chunkCount > maxChunkCount()) {
            throw new IOException("Invalid CallableHorse chunk count: " + chunkCount);
        }

        int transferId = nextTransferId();
        int checksum = checksum(compressed);
        for (int chunkIndex = 0; chunkIndex < chunkCount; chunkIndex++) {
            int offset = chunkIndex * CHUNK_SIZE;
            int length = Math.min(CHUNK_SIZE, compressed.length - offset);
            PacketLoginPlayerHorseData packet = new PacketLoginPlayerHorseData(
                    transferId, chunkIndex, chunkCount, compressed.length, checksum,
                    compressed, offset, length);
            if (!packet.valid) {
                throw new IOException("Unable to encode CallableHorse data chunk " + chunkIndex);
            }
            CallableHorsePacketHandler.networkWrapper.sendTo(packet, player);
        }
    }

    private static int maxChunkCount() {
        return (MAX_TRANSFER_BYTES + CHUNK_SIZE - 1) / CHUNK_SIZE;
    }

    private static int nextTransferId() {
        int id = NEXT_TRANSFER_ID.getAndIncrement() & 0x7fffffff;
        return id == 0 ? 1 : id;
    }

    private static int checksum(byte[] bytes) {
        CRC32 crc = new CRC32();
        crc.update(bytes, 0, bytes.length);
        return (int) crc.getValue();
    }

    private static boolean isValidChunkValues(int transferId, int chunkIndex, int chunkCount,
                                              int totalLength, byte[] payload,
                                              int payloadOffset, int payloadLength) {
        if (payload == null || transferId == 0 || chunkIndex < 0 || chunkCount <= 0
                || chunkCount > maxChunkCount() || totalLength <= 0
                || totalLength > MAX_TRANSFER_BYTES || payloadOffset < 0
                || payloadLength < 0 || payloadLength > CHUNK_SIZE
                || payloadOffset > payload.length - payloadLength) {
            return false;
        }
        long expectedChunks = ((long) totalLength + CHUNK_SIZE - 1L) / CHUNK_SIZE;
        if (expectedChunks != chunkCount || chunkIndex >= chunkCount) {
            return false;
        }
        int expectedLength = (int) Math.min(CHUNK_SIZE,
                (long) totalLength - ((long) chunkIndex * CHUNK_SIZE));
        return payloadLength == expectedLength;
    }

    @Override
    public void fromBytes(ByteBuf data) {
        invalidate();
        if (data == null || data.readableBytes() < HEADER_BYTES) {
            return;
        }

        try {
            int magic = data.readInt();
            int version = data.readUnsignedByte();
            int decodedTransferId = data.readInt();
            int decodedChunkIndex = data.readInt();
            int decodedChunkCount = data.readInt();
            int decodedTotalLength = data.readInt();
            int decodedChecksum = data.readInt();
            int decodedPayloadLength = data.readInt();

            if (magic != MAGIC || version != PROTOCOL_VERSION
                    || decodedTransferId <= 0 || decodedChunkIndex < 0
                    || decodedChunkCount <= 0 || decodedChunkCount > maxChunkCount()
                    || decodedTotalLength <= 0 || decodedTotalLength > MAX_TRANSFER_BYTES
                    || decodedPayloadLength < 0 || decodedPayloadLength > CHUNK_SIZE
                    || decodedPayloadLength != data.readableBytes()) {
                return;
            }

            long expectedChunks = ((long) decodedTotalLength + CHUNK_SIZE - 1L) / CHUNK_SIZE;
            if (expectedChunks != decodedChunkCount || decodedChunkIndex >= decodedChunkCount) {
                return;
            }
            int expectedLength = (int) Math.min(CHUNK_SIZE,
                    (long) decodedTotalLength - ((long) decodedChunkIndex * CHUNK_SIZE));
            if (decodedPayloadLength != expectedLength) {
                return;
            }

            byte[] decodedPayload = new byte[decodedPayloadLength];
            data.readBytes(decodedPayload);
            initializeChunk(decodedTransferId, decodedChunkIndex, decodedChunkCount,
                    decodedTotalLength, decodedChecksum, decodedPayload, 0, decodedPayloadLength);
        } catch (RuntimeException ignored) {
            // A malformed network payload must not take down the Netty thread.
            invalidate();
        }
    }

    @Override
    public void toBytes(ByteBuf data) {
        if (data == null) {
            return;
        }
        if (!valid) {
            // Keep the message well formed; the receiver will reject this
            // zero-length sentinel instead of throwing while decoding.
            data.writeInt(MAGIC);
            data.writeByte(PROTOCOL_VERSION);
            data.writeInt(1);
            data.writeInt(0);
            data.writeInt(0);
            data.writeInt(0);
            data.writeInt(0);
            data.writeInt(0);
            return;
        }
        data.writeInt(MAGIC);
        data.writeByte(PROTOCOL_VERSION);
        data.writeInt(transferId);
        data.writeInt(chunkIndex);
        data.writeInt(chunkCount);
        data.writeInt(totalLength);
        data.writeInt(checksum);
        data.writeInt(payloadLength);
        data.writeBytes(payload, payloadOffset, payloadLength);
    }

    /** Allows a client disconnect hook to discard partially received data. */
    public static void clearClientTransfers() {
        synchronized (TRANSFER_LOCK) {
            INCOMING_TRANSFERS.clear();
        }
    }

    /** Opportunistic timeout cleanup; safe to call from a client tick hook. */
    public static void cleanupExpiredTransfers() {
        synchronized (TRANSFER_LOCK) {
            cleanupExpiredTransfers(System.currentTimeMillis());
        }
    }

    private static void cleanupExpiredTransfers(long now) {
        Iterator<Map.Entry<Integer, IncomingTransfer>> iterator =
                INCOMING_TRANSFERS.entrySet().iterator();
        while (iterator.hasNext()) {
            IncomingTransfer transfer = iterator.next().getValue();
            if (now - transfer.lastUpdatedAt > TRANSFER_TIMEOUT_MILLIS) {
                iterator.remove();
            }
        }
    }

    /** Called by the client-only message handler after a chunk is decoded. */
    public static NBTTagCompound acceptChunk(PacketLoginPlayerHorseData packet) {
        if (packet == null || !packet.valid) {
            return null;
        }

        byte[] complete = null;
        synchronized (TRANSFER_LOCK) {
            long now = System.currentTimeMillis();
            cleanupExpiredTransfers(now);
            IncomingTransfer transfer = INCOMING_TRANSFERS.get(packet.transferId);
            if (transfer == null) {
                if (INCOMING_TRANSFERS.size() >= MAX_ACTIVE_TRANSFERS) {
                    removeOldestTransfer();
                }
                transfer = new IncomingTransfer(packet, now);
                INCOMING_TRANSFERS.put(packet.transferId, transfer);
            } else if (!transfer.matches(packet)) {
                // Reusing an ID with different metadata is ambiguous. Drop
                // the old transfer and reject this packet.
                INCOMING_TRANSFERS.remove(packet.transferId);
                return null;
            }

            if (!transfer.add(packet, now)) {
                INCOMING_TRANSFERS.remove(packet.transferId);
                return null;
            }
            if (transfer.isComplete()) {
                complete = transfer.bytes;
                INCOMING_TRANSFERS.remove(packet.transferId);
            }
        }

        if (complete == null || checksum(complete) != packet.checksum) {
            return null;
        }
        try {
            return CompressedStreamTools.func_152457_a(complete,
                    new NBTSizeTracker((long) MAX_NBT_BYTES * 8L));
        } catch (IOException ignored) {
            return null;
        } catch (RuntimeException ignored) {
            // Includes NBTSizeTracker's oversized-payload guard.
            return null;
        }
    }

    private static void removeOldestTransfer() {
        Integer oldestId = null;
        long oldestTimestamp = Long.MAX_VALUE;
        for (Map.Entry<Integer, IncomingTransfer> entry : INCOMING_TRANSFERS.entrySet()) {
            if (entry.getValue().lastUpdatedAt < oldestTimestamp) {
                oldestTimestamp = entry.getValue().lastUpdatedAt;
                oldestId = entry.getKey();
            }
        }
        if (oldestId != null) {
            INCOMING_TRANSFERS.remove(oldestId);
        }
    }

    private static final class IncomingTransfer {
        private final int transferId;
        private final int chunkCount;
        private final int totalLength;
        private final int checksum;
        private final byte[] bytes;
        private final BitSet received;
        private int receivedChunks;
        private long lastUpdatedAt;

        private IncomingTransfer(PacketLoginPlayerHorseData packet, long now) {
            this.transferId = packet.transferId;
            this.chunkCount = packet.chunkCount;
            this.totalLength = packet.totalLength;
            this.checksum = packet.checksum;
            this.bytes = new byte[packet.totalLength];
            this.received = new BitSet(packet.chunkCount);
            this.lastUpdatedAt = now;
        }

        private boolean matches(PacketLoginPlayerHorseData packet) {
            return transferId == packet.transferId && chunkCount == packet.chunkCount
                    && totalLength == packet.totalLength && checksum == packet.checksum;
        }

        private boolean add(PacketLoginPlayerHorseData packet, long now) {
            if (!matches(packet) || packet.chunkIndex < 0 || packet.chunkIndex >= chunkCount) {
                return false;
            }
            if (received.get(packet.chunkIndex)) {
                // Duplicate chunks are harmless and do not alter the first
                // accepted bytes.
                lastUpdatedAt = now;
                return true;
            }
            int offset = packet.chunkIndex * CHUNK_SIZE;
            if (offset < 0 || offset > bytes.length - packet.payloadLength) {
                return false;
            }
            System.arraycopy(packet.payload, packet.payloadOffset, bytes, offset, packet.payloadLength);
            received.set(packet.chunkIndex);
            receivedChunks++;
            lastUpdatedAt = now;
            return true;
        }

        private boolean isComplete() {
            return receivedChunks == chunkCount;
        }
    }

    /** Common-safe handler; all Minecraft client access lives in the client proxy. */
    public static class Handler implements IMessageHandler<PacketLoginPlayerHorseData, IMessage> {
        @Override
        public IMessage onMessage(PacketLoginPlayerHorseData packet, MessageContext context) {
            if (context == null || context.side != Side.CLIENT || lotrcallablehorse.proxy == null) {
                return null;
            }
            NBTTagCompound completeData = acceptChunk(packet);
            if (completeData != null) {
                lotrcallablehorse.proxy.applyPlayerHorseData(completeData);
            }
            return null;
        }
    }

}
