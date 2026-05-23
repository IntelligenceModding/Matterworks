package de.artemis.matterworks.common.world;

import de.artemis.matterworks.Matterworks;
import de.artemis.matterworks.common.blockentity.MatterPylonBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.neoforge.common.world.chunk.RegisterTicketControllersEvent;
import net.neoforged.neoforge.common.world.chunk.TicketController;
import net.neoforged.neoforge.common.world.chunk.TicketHelper;

public final class PylonChunkLoading {
    private static final ResourceLocation CONTROLLER_ID = ResourceLocation.fromNamespaceAndPath(Matterworks.MOD_ID, "matter_pylon");
    private static final TicketController CONTROLLER = new TicketController(CONTROLLER_ID, PylonChunkLoading::validateTickets);

    private PylonChunkLoading() {
    }

    public static void registerTicketControllers(RegisterTicketControllersEvent event) {
        event.register(CONTROLLER);
    }

    public static void forcePylonTickets(ServerLevel level, BlockPos pylonPos, Direction facing) {
        forceNodeTickets(level, pylonPos);

        BlockPos attachedPos = pylonPos.relative(facing.getOpposite());
        if (!new ChunkPos(pylonPos).equals(new ChunkPos(attachedPos))) {
            forceChunk(level, pylonPos, attachedPos, true);
        }
    }

    public static void releasePylonTickets(ServerLevel level, BlockPos pylonPos, Direction facing) {
        releaseNodeTickets(level, pylonPos);

        BlockPos attachedPos = pylonPos.relative(facing.getOpposite());
        if (!new ChunkPos(pylonPos).equals(new ChunkPos(attachedPos))) {
            forceChunk(level, pylonPos, attachedPos, false);
        }
    }

    public static void forceNodeTickets(ServerLevel level, BlockPos nodePos) {
        forceChunk(level, nodePos, nodePos, true);
    }

    public static void releaseNodeTickets(ServerLevel level, BlockPos nodePos) {
        forceChunk(level, nodePos, nodePos, false);
    }

    public static void forceRemoteNodeTickets(ServerLevel level, BlockPos ownerPos, BlockPos remoteNodePos) {
        forceChunk(level, ownerPos, remoteNodePos, true);
    }

    public static void releaseRemoteNodeTickets(ServerLevel level, BlockPos ownerPos, BlockPos remoteNodePos) {
        forceChunk(level, ownerPos, remoteNodePos, false);
    }

    private static void forceChunk(ServerLevel level, BlockPos ownerPos, BlockPos chunkAnchorPos, boolean add) {
        ChunkPos chunkPos = new ChunkPos(chunkAnchorPos);
        CONTROLLER.forceChunk(level, ownerPos.immutable(), chunkPos.x, chunkPos.z, add, true);
    }

    private static void validateTickets(ServerLevel level, TicketHelper ticketHelper) {
        for (BlockPos ownerPos : ticketHelper.getBlockTickets().keySet()) {
            if (!(level.getBlockEntity(ownerPos) instanceof MatterPylonBlockEntity)) {
                ticketHelper.removeAllTickets(ownerPos);
            }
        }
    }
}
