package ssot_parser.ast.nodes;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import ssot_parser.ast.AstNode;
import ssot_parser.NodeWithId;
import ssot_parser.ast.NodeVisitor;

/**
 * Represents a state transition triggered by an event.
 * Corresponds to the 'onTransition' rule within a state definition.
 */
public class TransitionNode implements NodeWithId {
    private final Optional<Long> id;
    public final String sourceState; // Renamed from fromState
    public final String targetState; // Renamed from toState
    public final String event;
    public final Optional<String> condition; // Optional guard condition reference (name)
    public final Optional<String> action;    // Optional action reference (name)
    private final Map<String, Object> annotations;

    // Constructor - includes source state, guard condition (optional), and actions (list, optional)
    public TransitionNode(Optional<Long> id, String sourceState, String targetState, String event,
                          Optional<String> condition, Optional<String> action,
                          Map<String, Object> annotations) {
        this.id = id;
        this.sourceState = sourceState;
        this.targetState = targetState;
        this.event = event;
        this.condition = condition;
        this.action = action;
        this.annotations = Collections.unmodifiableMap(annotations != null ? new HashMap<>(annotations) : Collections.emptyMap());
    }

    @Override
    public Optional<Long> getId() {
        return id;
    }

    public String getSourceState() {
        return sourceState;
    }

    public String getTargetState() {
        return targetState;
    }

    public String getEvent() {
        return event;
    }

    public Optional<String> getCondition() {
        return condition;
    }

    public Optional<String> getAction() {
        return action;
    }

    @Override
    public Map<String, Object> getAnnotations() {
        return annotations;
    }

    @Override
    public <T> T accept(NodeVisitor<T> visitor) {
        return visitor.visitTransitionNode(this);
    }

    @Override
    public String toString() {
        return "TransitionNode{" +
               "id=" + id +
               ", sourceState='" + sourceState + "\'" +
               ", targetState='" + targetState + "\'" +
               ", event='" + event + "\'" +
               ", condition=" + condition +
               ", action=" + action +
               ", annotations=" + annotations +
               "}";
    }

    // Consider adding equals() and hashCode()
} 