package net.jelly.chronicler.backend.graph;

import net.jelly.chronicler.ChroniclerMod;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;
import java.sql.SQLException;


/**
 * Owns the GraphStore's lifecycle and ties it to the dedicated/integrated server's
 * own lifecycle. This is the ONLY place graph init/teardown should happen.
 *
 */
@Mod.EventBusSubscriber(modid = ChroniclerMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class GraphManager {

    private static final Logger LOGGER = LogManager.getLogger();
    private static GraphStore instance;
    private static GraphDatabase database;

    private GraphManager() {
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        try {
            LOGGER.info("[Chronicler/H2] Server Starting");
            Path worldDir = resolveWorldChroniclerDir(event);
            Path dbFile = worldDir.resolve("graph");
            LOGGER.info("[Chronicler/H2] Database path resolved");

            database = GraphDatabase.open(dbFile);
            instance = new GraphStore(database);

            logSanityCheck();
        } catch (SQLException e) {
            throw new RuntimeException("Chronicler: failed to initialize graph store", e);
        }
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        if (database != null) {
            try {
                database.close();
            } catch (SQLException e) {
                LOGGER.info("[Chronicler/H2] Database failed to close");
            } finally {
                database = null;
                instance = null;
            }
        }
    }

    public static boolean isReady() {
        return instance != null;
    }

    public static GraphStore get() {
        if (instance == null) {
            throw new IllegalStateException("GraphStore accessed before sever started, or after shutdown");
        }
        return instance;
    }

    /**
     * Placeholder — replace with the real world-save-relative path once wired up,
     * e.g.:
     *   event.getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT)
     *        .resolve("chronicler")
     */
    private static Path resolveWorldChroniclerDir(ServerStartedEvent event) {
        try {
            return event.getServer().getWorldPath(LevelResource.ROOT).resolve("chronicler");
        } catch (Exception e) {
            throw new IllegalStateException("Path cannot be resolved", e);
        }
    }

    /**
     * Logs a summary count at INFO (always visible, cheap, tells you at a glance
     * whether the load actually pulled anything off disk) and the full node/edge
     * dump at DEBUG (verbose — enable only when you actually need to eyeball the
     * graph contents, e.g. during these early in-game tests).
     */
    private static void logSanityCheck() {
        var nodes = instance.allNodes();
        var edges = instance.allEdges();

        LOGGER.info("[Chronicler/H2] Loaded {} node(s), {} edge(s) from disk", nodes.size(), edges.size());

        if (LOGGER.isDebugEnabled()) {
            for (Node node : nodes) {
                LOGGER.debug("[Chronicler/H2]   node: id={} type={} name='{}' dim={} attrs={}",
                        node.getId(), node.getNodeType(), node.getName(),
                        node.getDimension(), node.getAttributes());
            }
            for (Edge edge : edges) {
                LOGGER.debug("[Chronicler/H2]   edge: id={} type={} {} -> {} weight={} attrs={}",
                        edge.getId(), edge.getEdgeType(), edge.getFromNodeId(),
                        edge.getToNodeId(), edge.getWeight(), edge.getAttributes());
            }
        }
    }

}
