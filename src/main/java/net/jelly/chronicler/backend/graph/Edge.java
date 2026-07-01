package net.jelly.chronicler.backend.graph;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.util.UUID;

/**
 * A directed edge between two nodes in the Chronicler knowledge graph.
 * See design doc §9.5.
 */
public final class Edge {

    private final UUID id;
    private final UUID fromNodeId;
    private final UUID toNodeId;
    private final EdgeType edgeType;
    private float weight;              // 0-100, clamped server-side (§15.3)
    private JsonObject attributes;     // e.g. {"trust": 62}
    private final long createdAtTick;
    private long lastUpdatedTick;

    public Edge(UUID id, UUID fromNodeId, UUID toNodeId, EdgeType edgeType,
                float weight, JsonObject attributes, long createdAtTick, long lastUpdatedTick) {
        this.id = id;
        this.fromNodeId = fromNodeId;
        this.toNodeId = toNodeId;
        this.edgeType = edgeType;
        this.weight = weight;
        this.attributes = (attributes != null) ? attributes : new JsonObject();
        this.createdAtTick = createdAtTick;
        this.lastUpdatedTick = lastUpdatedTick;
    }

    public static Edge newEdge(UUID fromNodeId, UUID toNodeId, EdgeType edgeType,
                                float weight, long currentTick) {
        return new Edge(UUID.randomUUID(), fromNodeId, toNodeId, edgeType, weight,
                new JsonObject(), currentTick, currentTick);
    }

    public UUID getId() {
        return id;
    }

    public UUID getFromNodeId() {
        return fromNodeId;
    }

    public UUID getToNodeId() {
        return toNodeId;
    }

    public EdgeType getEdgeType() {
        return edgeType;
    }

    public float getWeight() {
        return weight;
    }

    /** Callers are responsible for clamping  */
    public void setWeight(float weight) {
        this.weight = weight;
    }

    public JsonObject getAttributes() {
        return attributes;
    }

    public void setAttributes(JsonObject attributes) {
        this.attributes = (attributes != null) ? attributes : new JsonObject();
    }

    public Integer getAttrInt(String key, Integer defaultValue) {
        JsonElement el = attributes.get(key);
        return (el != null && !el.isJsonNull()) ? el.getAsInt() : defaultValue;
    }

    public void putAttr(String key, Number value) {
        attributes.add(key, value == null ? null : new JsonPrimitive(value));
    }

    public void putAttr(String key, String value) {
        attributes.add(key, value == null ? null : new JsonPrimitive(value));
    }

    public long getCreatedAtTick() {
        return createdAtTick;
    }

    public long getLastUpdatedTick() {
        return lastUpdatedTick;
    }

    public void touch(long currentTick) {
        this.lastUpdatedTick = currentTick;
    }

    @Override
    public String toString() {
        return "Edge{" + edgeType + " " + fromNodeId + " -> " + toNodeId + " w=" + weight + "}";
    }
}
