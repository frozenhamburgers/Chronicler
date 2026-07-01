package net.jelly.chronicler.backend.graph;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * The single point of contact between game logic and the persisted graph.
 *
 * Holds the durable H2 store (see {@link GraphDatabase}) and an in-memory working
 * representation ({@code Map<UUID, Node>}, {@code Map<UUID, List<Edge>>}) per §10,
 * rebuilt from H2 on construction and kept in sync on every write.
 *
 * All writes are funneled through this class and synchronized on a single lock,
 * per §10's "single writer path" requirement (NFR-9). This is deliberately a
 * coarse lock for now; it's cheap at this project's scale (a few dozen players)
 * and easy to reason about. Revisit only if necessary
 *
 * This class is Minecraft agonstic. It should be constructed and owned by glue code to Forge.
 */
public final class GraphStore {

    private final GraphDatabase db;

    private final Map<UUID, Node> nodes = new ConcurrentHashMap<>();
    private final Map<UUID, List<Edge>> edgesByFromNode = new ConcurrentHashMap<>();
    private final Map<UUID, Edge> edgesById = new ConcurrentHashMap<>();

    private final Object writeLock = new Object();

    public GraphStore(GraphDatabase db) throws SQLException {
        this.db = db;
        loadAllFromDisk();
    }

    // ------------------------------------------------------------------
    // Startup load
    // ------------------------------------------------------------------

    private void loadAllFromDisk() throws SQLException {
        Connection conn = db.getConnection();

        try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM nodes");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Node node = readNode(rs);
                nodes.put(node.getId(), node);
            }
        }

        try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM edges");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Edge edge = readEdge(rs);
                edgesById.put(edge.getId(), edge);
                edgesByFromNode.computeIfAbsent(edge.getFromNodeId(), k -> new ArrayList<>()).add(edge);
            }
        }
    }

    // ------------------------------------------------------------------
    // Reads (served from the in-memory cache — never hit H2 on the hot path)
    // ------------------------------------------------------------------

    public Node getNode(UUID id) {
        return nodes.get(id);
    }

    public boolean nodeExists(UUID id) {
        return nodes.containsKey(id);
    }

    /** Direct outgoing edges only — no traversal. Used by Tier-1 retrieval (§13). */
    public List<Edge> getOutgoingEdges(UUID nodeId) {
        return Collections.unmodifiableList(edgesByFromNode.getOrDefault(nodeId, Collections.emptyList()));
    }

    public Edge getEdge(UUID edgeId) {
        return edgesById.get(edgeId);
    }

    /** Snapshot of every node currently held in memory — callers should not mutate the result. */
    public List<Node> allNodes() {
        return new ArrayList<>(nodes.values());
    }

    /** Snapshot of every edge currently held in memory — callers should not mutate the result. */
    public List<Edge> allEdges() {
        return new ArrayList<>(edgesById.values());
    }

    // ------------------------------------------------------------------
    // Writes (single-writer path; each call is its own small transaction)
    // ------------------------------------------------------------------

    public void insertNode(Node node) throws SQLException {
        synchronized (writeLock) {
            String sql = """
                INSERT INTO nodes (id, node_type, name, dimension, location_x, location_y, location_z,
                                    created_at_tick, last_updated_tick, attributes)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
            try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
                bindNode(ps, node);
                ps.executeUpdate();
            }
            nodes.put(node.getId(), node);
        }
    }

    public void updateNode(Node node) throws SQLException {
        synchronized (writeLock) {
            String sql = """
                UPDATE nodes SET node_type=?, name=?, dimension=?, location_x=?, location_y=?,
                                  location_z=?, created_at_tick=?, last_updated_tick=?, attributes=?
                WHERE id=?
            """;
            try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
                ps.setString(1, node.getNodeType().name());
                ps.setString(2, node.getName());
                ps.setString(3, node.getDimension());
                setNullableDouble(ps, 4, node.getLocationX());
                setNullableDouble(ps, 5, node.getLocationY());
                setNullableDouble(ps, 6, node.getLocationZ());
                ps.setLong(7, node.getCreatedAtTick());
                ps.setLong(8, node.getLastUpdatedTick());
                ps.setString(9, node.getAttributes().toString());
                ps.setObject(10, node.getId());
                ps.executeUpdate();
            }
            nodes.put(node.getId(), node);
        }
    }

    public void insertEdge(Edge edge) throws SQLException {
        synchronized (writeLock) {
            String sql = """
                INSERT INTO edges (id, from_node_id, to_node_id, edge_type, weight, attributes,
                                    created_at_tick, last_updated_tick)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;
            try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
                bindEdge(ps, edge);
                ps.executeUpdate();
            }
            edgesById.put(edge.getId(), edge);
            edgesByFromNode.computeIfAbsent(edge.getFromNodeId(), k -> new ArrayList<>()).add(edge);
        }
    }

    public void updateEdge(Edge edge) throws SQLException {
        synchronized (writeLock) {
            String sql = """
                UPDATE edges SET weight=?, attributes=?, last_updated_tick=? WHERE id=?
            """;
            try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
                ps.setFloat(1, edge.getWeight());
                ps.setString(2, edge.getAttributes().toString());
                ps.setLong(3, edge.getLastUpdatedTick());
                ps.setObject(4, edge.getId());
                ps.executeUpdate();
            }
            edgesById.put(edge.getId(), edge);
        }
    }

    public void insertInteraction(Interaction interaction) throws SQLException {
        synchronized (writeLock) {
            String sql = """
                INSERT INTO interactions (id, edge_id, game_tick, summary_text, salience_score)
                VALUES (?, ?, ?, ?, ?)
            """;
            try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
                ps.setObject(1, interaction.getId());
                ps.setObject(2, interaction.getEdgeId());
                ps.setLong(3, interaction.getGameTick());
                ps.setString(4, interaction.getSummaryText());
                ps.setFloat(5, interaction.getSalienceScore());
                ps.executeUpdate();
            }
        }
    }

    /**
     * Runs {@code work} inside a single H2 transaction, committing only if it
     * completes without throwing (NFR-2 — "a batch of graph mutations from one
     * extraction pass commits entirely or not at all").
     *
     * Phase 1 provides the mechanism; the extraction pass (Phase 3) is the actual
     * caller that will batch several inserts/updates through this.
     */
    public <T> T runInTransaction(Supplier<T> work) throws SQLException {
        synchronized (writeLock) {
            Connection conn = db.getConnection();
            boolean prevAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try {
                T result = work.get();
                conn.commit();
                return result;
            } catch (RuntimeException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(prevAutoCommit);
            }
        }
    }

    // ------------------------------------------------------------------
    // Row <-> object mapping
    // ------------------------------------------------------------------

    private Node readNode(ResultSet rs) throws SQLException {
        UUID id = (UUID) rs.getObject("id");
        NodeType type = NodeType.valueOf(rs.getString("node_type"));
        String name = rs.getString("name");
        String dimension = rs.getString("dimension");
        Double x = getNullableDouble(rs, "location_x");
        Double y = getNullableDouble(rs, "location_y");
        Double z = getNullableDouble(rs, "location_z");
        long createdAt = rs.getLong("created_at_tick");
        long updatedAt = rs.getLong("last_updated_tick");
        JsonObject attrs = JsonParser.parseString(rs.getString("attributes")).getAsJsonObject();
        return new Node(id, type, name, dimension, x, y, z, createdAt, updatedAt, attrs);
    }

    private Edge readEdge(ResultSet rs) throws SQLException {
        UUID id = (UUID) rs.getObject("id");
        UUID from = (UUID) rs.getObject("from_node_id");
        UUID to = (UUID) rs.getObject("to_node_id");
        EdgeType type = EdgeType.valueOf(rs.getString("edge_type"));
        float weight = rs.getFloat("weight");
        JsonObject attrs = JsonParser.parseString(rs.getString("attributes")).getAsJsonObject();
        long createdAt = rs.getLong("created_at_tick");
        long updatedAt = rs.getLong("last_updated_tick");
        return new Edge(id, from, to, type, weight, attrs, createdAt, updatedAt);
    }

    private void bindNode(PreparedStatement ps, Node node) throws SQLException {
        ps.setObject(1, node.getId());
        ps.setString(2, node.getNodeType().name());
        ps.setString(3, node.getName());
        ps.setString(4, node.getDimension());
        setNullableDouble(ps, 5, node.getLocationX());
        setNullableDouble(ps, 6, node.getLocationY());
        setNullableDouble(ps, 7, node.getLocationZ());
        ps.setLong(8, node.getCreatedAtTick());
        ps.setLong(9, node.getLastUpdatedTick());
        ps.setString(10, node.getAttributes().toString());
    }

    private void bindEdge(PreparedStatement ps, Edge edge) throws SQLException {
        ps.setObject(1, edge.getId());
        ps.setObject(2, edge.getFromNodeId());
        ps.setObject(3, edge.getToNodeId());
        ps.setString(4, edge.getEdgeType().name());
        ps.setFloat(5, edge.getWeight());
        ps.setString(6, edge.getAttributes().toString());
        ps.setLong(7, edge.getCreatedAtTick());
        ps.setLong(8, edge.getLastUpdatedTick());
    }

    private static void setNullableDouble(PreparedStatement ps, int index, Double value) throws SQLException {
        if (value == null) {
            ps.setNull(index, java.sql.Types.DOUBLE);
        } else {
            ps.setDouble(index, value);
        }
    }

    private static Double getNullableDouble(ResultSet rs, String column) throws SQLException {
        double v = rs.getDouble(column);
        return rs.wasNull() ? null : v;
    }
}
