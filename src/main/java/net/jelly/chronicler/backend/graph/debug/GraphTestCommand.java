package net.jelly.chronicler.backend.graph.debug;

import com.mojang.brigadier.CommandDispatcher;
import net.jelly.chronicler.ChroniclerMod;
import net.jelly.chronicler.backend.graph.Edge;
import net.jelly.chronicler.backend.graph.EdgeType;
import net.jelly.chronicler.backend.graph.GraphManager;
import net.jelly.chronicler.backend.graph.Node;
import net.jelly.chronicler.backend.graph.NodeType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.SQLException;

@Mod.EventBusSubscriber(modid = ChroniclerMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class GraphTestCommand {

    private static final Logger LOGGER = LogManager.getLogger();

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
      GraphTestCommand.register(event.getDispatcher());
    }

    private GraphTestCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("chronicler")
                        .then(Commands.literal("test-graph")
                                .requires(source -> source.hasPermission(2))
                                .executes(ctx -> runSampleGraphTest(ctx.getSource())))
        );
    }

    private static int runSampleGraphTest(CommandSourceStack source) {
        if (!GraphManager.isReady()) {
            source.sendFailure(Component.literal("[Chronicler] GraphStore is not ready yet."));
            return 0;
        }

        try {
            long tick = source.getLevel().getGameTime();
            String dimension = source.getLevel().dimension().location().toString();
            var pos = source.getPosition();

            // Two sample NPC nodes, both dropped at the command sender's position
            // for convenience — in real use these would come from actual entity
            // promotion (§11.3), not a debug command.
            Node npcA = Node.newNode(NodeType.NPC, "Test NPC Alaric", tick);
            npcA.setLocation(dimension, pos.x, pos.y, pos.z);
            npcA.putAttr("profession", "minecraft:farmer");
            npcA.putAttr("promotion_tier", 1);

            Node npcB = Node.newNode(NodeType.NPC, "Test NPC Branwen", tick);
            npcB.setLocation(dimension, pos.x + 2, pos.y, pos.z);
            npcB.putAttr("profession", "minecraft:librarian");
            npcB.putAttr("promotion_tier", 1);

            GraphManager.get().insertNode(npcA);
            GraphManager.get().insertNode(npcB);

            // Sample weak KNOWS edge, roughly matching a bootstrap-promotion
            // starting weight (§11.3 step 3: uniformly sampled 5-15).
            Edge edge = Edge.newEdge(npcA.getId(), npcB.getId(), EdgeType.KNOWS, 10f, tick);
            edge.putAttr("trust", 10);

            GraphManager.get().insertEdge(edge);

            LOGGER.info("[Chronicler/H2] test-graph: created nodes {} and {}, edge {}",
                    npcA.getId(), npcB.getId(), edge.getId());

            source.sendSuccess(() -> Component.literal(String.format(
                    "[Chronicler] Created '%s' (%s) and '%s' (%s), connected by a KNOWS edge (weight=%.1f).",
                    npcA.getName(), npcA.getId(), npcB.getName(), npcB.getId(), edge.getWeight()
            )), false);

            return 1;
        } catch (SQLException e) {
            LOGGER.error("[Chronicler/H2] test-graph command failed", e);
            source.sendFailure(Component.literal("[Chronicler] Failed to write test graph — see server log."));
            return 0;
        }
    }
}