package net.jelly.chronicler.backend.graph;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Owns the H2 connection and schema for the Chronicler knowledge graph.
 *
 * One instance of this class = one open embedded H2 database, file-backed inside
 * the world save directory.
 */
public final class GraphDatabase implements AutoCloseable {

    private static final String DRIVER_CLASS = "org.h2.Driver";

    private final Connection connection;

    private static final Logger LOGGER = LogManager.getLogger();

    private GraphDatabase(Connection connection) {
        this.connection = connection;
    }

    /**
     * Opens (creating if necessary) the H2 database at {@code dbFile} and ensures
     * the schema exists. {@code dbFile} should be a path *without* the .mv.db
     * extension — H2 appends that itself.
     *
     * Typical call site: world save directory, e.g.
     * {@code <world>/chronicler/graph} -> resolves to {@code <world>/chronicler/graph.mv.db}.
     */
    public static GraphDatabase open(Path dbFile) throws SQLException {
        LOGGER.debug("Loader: " + GraphDatabase.class.getClassLoader());
        LOGGER.debug("Context loader: " + Thread.currentThread().getContextClassLoader());
        try {
            Class.forName(DRIVER_CLASS);
        } catch (ClassNotFoundException e) {
            throw new SQLException("H2 driver not found on classpath", e);
        }

        String jdbcUrl = "jdbc:h2:file:" + dbFile.toAbsolutePath()
                + ";DB_CLOSE_ON_EXIT=FALSE";
        Connection conn = DriverManager.getConnection(jdbcUrl);
        conn.setAutoCommit(true);

        GraphDatabase db = new GraphDatabase(conn);
        db.initSchema();
        return db;
    }

    public Connection getConnection() {
        return connection;
    }

    private void initSchema() throws SQLException {
        try (Statement stmt = connection.createStatement()) {

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS nodes (
                    id                  UUID PRIMARY KEY,
                    node_type           VARCHAR(16)  NOT NULL,
                    name                VARCHAR(256) NOT NULL,
                    dimension           VARCHAR(256),
                    location_x          DOUBLE,
                    location_y          DOUBLE,
                    location_z          DOUBLE,
                    created_at_tick     BIGINT NOT NULL,
                    last_updated_tick   BIGINT NOT NULL,
                    attributes          CLOB NOT NULL
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS edges (
                    id                  UUID PRIMARY KEY,
                    from_node_id        UUID NOT NULL REFERENCES nodes(id),
                    to_node_id          UUID NOT NULL REFERENCES nodes(id),
                    edge_type           VARCHAR(16) NOT NULL,
                    weight              REAL NOT NULL,
                    attributes          CLOB NOT NULL,
                    created_at_tick     BIGINT NOT NULL,
                    last_updated_tick   BIGINT NOT NULL
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS interactions (
                    id                  UUID PRIMARY KEY,
                    edge_id             UUID NOT NULL REFERENCES edges(id),
                    game_tick           BIGINT NOT NULL,
                    summary_text        CLOB NOT NULL,
                    salience_score      REAL NOT NULL
                )
            """);

            // Indexes per §10 — support the spatial/temporal queries BFS alone
            // can't answer efficiently (e.g. "events near location X recently").
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_nodes_type ON nodes(node_type)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_nodes_location ON nodes(dimension, location_x, location_y, location_z)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_nodes_updated ON nodes(last_updated_tick)");

            stmt.execute("CREATE INDEX IF NOT EXISTS idx_edges_from ON edges(from_node_id)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_edges_to ON edges(to_node_id)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_edges_type ON edges(edge_type)");

            stmt.execute("CREATE INDEX IF NOT EXISTS idx_interactions_edge ON interactions(edge_id)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_interactions_tick ON interactions(game_tick)");
        }
    }

    @Override
    public void close() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }
}
