package net.jelly.chronicler.backend.graph;

import java.util.UUID;

/**
 * A single episodic log entry attached to a relationship edge.
 * See design doc §9.6 and the memory model in §12.
 */
public final class Interaction {

    private final UUID id;
    private final UUID edgeId;
    private final long gameTick;
    private String summaryText;
    private float salienceScore;

    public Interaction(UUID id, UUID edgeId, long gameTick, String summaryText, float salienceScore) {
        this.id = id;
        this.edgeId = edgeId;
        this.gameTick = gameTick;
        this.summaryText = summaryText;
        this.salienceScore = salienceScore;
    }

    public static Interaction newInteraction(UUID edgeId, long gameTick, String summaryText, float salienceScore) {
        return new Interaction(UUID.randomUUID(), edgeId, gameTick, summaryText, salienceScore);
    }

    public UUID getId() {
        return id;
    }

    public UUID getEdgeId() {
        return edgeId;
    }

    public long getGameTick() {
        return gameTick;
    }

    public String getSummaryText() {
        return summaryText;
    }

    public void setSummaryText(String summaryText) {
        this.summaryText = summaryText;
    }

    public float getSalienceScore() {
        return salienceScore;
    }

    public void setSalienceScore(float salienceScore) {
        this.salienceScore = salienceScore;
    }

    @Override
    public String toString() {
        return "Interaction{edge=" + edgeId + " tick=" + gameTick + " salience=" + salienceScore + "}";
    }
}
