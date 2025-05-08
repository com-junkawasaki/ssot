package ssot_parser.ast.nodes;

import ssot_parser.ast.AstNode;
import ssot_parser.ast.NodeVisitor;
import ssot_parser.ast.nodes.state.HistoryStateType;

import java.util.Collections;
import java.util.List;
import java.util.ArrayList;

/**
 * Represents a history pseudo-state (shallow or deep) within a state machine.
 * Can contain transitions (e.g., default target if no history exists).
 */
public class HistoryStateNode implements AstNode {
    private final HistoryStateType type;
    private final List<EventHandlerNode> eventHandlers; // Transitions defined within the history state body
    private final List<AnnotationNode> annotations;

    public HistoryStateNode(HistoryStateType type,
                            List<EventHandlerNode> eventHandlers,
                            List<AnnotationNode> annotations) {
        this.type = type;
        this.eventHandlers = Collections.unmodifiableList(eventHandlers != null ? new ArrayList<>(eventHandlers) : Collections.emptyList());
        this.annotations = Collections.unmodifiableList(annotations != null ? new ArrayList<>(annotations) : Collections.emptyList());
    }

    public HistoryStateType getType() {
        return type;
    }

    public List<EventHandlerNode> getEventHandlers() {
        return eventHandlers;
    }

    public List<AnnotationNode> getAnnotations() {
        return annotations;
    }

    @Override
    public <T> T accept(NodeVisitor<T> visitor) {
        return visitor.visitHistoryStateNode(this);
    }

    @Override
    public String toString() {
        return "HistoryStateNode{" +
               "type=" + type +
               ", eventHandlers=" + eventHandlers +
               ", annotations=" + annotations +
               "}";
    }

    // Consider adding equals() and hashCode()
}
