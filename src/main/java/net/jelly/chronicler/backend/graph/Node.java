package net.jelly.chronicler.backend.graph;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.util.UUID;

/**
 * A node in the Chronicler knowledge graph. This class is Minecraft-agnostic, as is much of this package.
 *
 */
public final class Node {

    private final UUID id;
    private final NodeType nodeType;
    private String name;
    private String dimension;          // nullable
    private Double locationX;          // nullable
    private Double locationY;          // nullable
    private Double locationZ;          // nullable
    private long createdAtTick;
    private long lastUpdatedTick;
    private JsonObject attributes;

    public Node(UUID id, NodeType nodeType, String name, String dimension,
                Double locationX, Double locationY, Double locationZ,
                long createdAtTick, long lastUpdatedTick, JsonObject attributes) {
        this.id = id;
        this.nodeType = nodeType;
        this.name = name;
        this.dimension = dimension;
        this.locationX = locationX;
        this.locationY = locationY;
        this.locationZ = locationZ;
        this.createdAtTick = createdAtTick;
        this.lastUpdatedTick = lastUpdatedTick;
        this.attributes = (attributes != null) ? attributes : new JsonObject();
    }

    /** Convenience constructor for freshly-created nodes. */
    public static Node newNode(NodeType nodeType, String name, long currentTick) {
        return new Node(UUID.randomUUID(), nodeType, name, null, null, null, null,
                currentTick, currentTick, new JsonObject());
    }

    // --- identity / core fields ---

    public UUID getId() {
        return id;
    }

    public NodeType getNodeType() {
        return nodeType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDimension() {
        return dimension;
    }

    public void setDimension(String dimension) {
        this.dimension = dimension;
    }

    public Double getLocationX() {
        return locationX;
    }

    public Double getLocationY() {
        return locationY;
    }

    public Double getLocationZ() {
        return locationZ;
    }

    public void setLocation(String dimension, double x, double y, double z) {
        this.dimension = dimension;
        this.locationX = x;
        this.locationY = y;
        this.locationZ = z;
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

    // --- attributes (JSON extension bag, §9.3/§9.4) ---

    public JsonObject getAttributes() {
        return attributes;
    }

    public void setAttributes(JsonObject attributes) {
        this.attributes = (attributes != null) ? attributes : new JsonObject();
    }

    public String getAttrString(String key, String defaultValue) {
        JsonElement el = attributes.get(key);
        return (el != null && !el.isJsonNull()) ? el.getAsString() : defaultValue;
    }

    public Double getAttrDouble(String key, Double defaultValue) {
        JsonElement el = attributes.get(key);
        return (el != null && !el.isJsonNull()) ? el.getAsDouble() : defaultValue;
    }

    public Integer getAttrInt(String key, Integer defaultValue) {
        JsonElement el = attributes.get(key);
        return (el != null && !el.isJsonNull()) ? el.getAsInt() : defaultValue;
    }

    public Boolean getAttrBoolean(String key, Boolean defaultValue) {
        JsonElement el = attributes.get(key);
        return (el != null && !el.isJsonNull()) ? el.getAsBoolean() : defaultValue;
    }

    public void putAttr(String key, String value) {
        attributes.add(key, value == null ? null : new JsonPrimitive(value));
    }

    public void putAttr(String key, Number value) {
        attributes.add(key, value == null ? null : new JsonPrimitive(value));
    }

    public void putAttr(String key, Boolean value) {
        attributes.add(key, value == null ? null : new JsonPrimitive(value));
    }

    public void putAttr(String key, JsonElement value) {
        attributes.add(key, value);
    }

    @Override
    public String toString() {
        return "Node{" + nodeType + " id=" + id + " name='" + name + "'}";
    }
}
