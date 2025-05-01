package ssot_parser.ast.nodes;

import ssot_parser.ast.AstNode;
import ssot_parser.ast.NodeVisitor;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Represents a state transition triggered by an event.
 * Corresponds to the 'onTransition' rule within a state definition.
 */
public class TransitionNode implements AstNode {
    private final Optional<Long> id;
    private final String sourceState; // Name of the state where this transition originates
    private final String targetState; // Name of the state this transition leads to
    private final String event;       // Name of the event triggering the transition
    // Condition that must be met for the transition (references a guard name)
    private final Optional<String> condition; // Guard reference
    // Actions to execute upon transitioning (references action names)
    private final List<String> actions; // List of action references
    private final Map<String, Object> annotations;

    // Constructor - includes source state, guard condition (optional), and actions (list, optional)
    public TransitionNode(Optional<Long> id, String sourceState, String targetState, String event, Optional<String> condition, List<String> actions, Map<String, Object> annotations) {
        this.id = id;
        this.sourceState = sourceState;
        this.targetState = targetState;
        this.event = event;
        this.condition = condition; // Optional guard reference
        this.actions = actions != null ? Collections.unmodifiableList(actions) : Collections.emptyList();
        this.annotations = annotations != null ? Collections.unmodifiableMap(annotations) : Collections.emptyMap();
    }

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

    public List<String> getActions() {
        return actions;
    }

    public Map<String, Object> getAnnotations() {
        return annotations;
    }

    @Override
    public <T> T accept(NodeVisitor<T> visitor) {
        // Assuming a visitor pattern
        // return visitor.visitTransitionNode(this);
        throw new UnsupportedOperationException("Visitor pattern not yet implemented for TransitionNode");
    }

    @Override
    public String toString() {
        return "TransitionNode{" +
               "id=" + id +
               ", sourceState='" + sourceState + "\'" +
               ", targetState='" + targetState + "\'" +
               ", event='" + event + "\'" +
               ", condition=" + condition +
               ", actions=" + actions +
               ", annotations=" + annotations +
               "}";
    }

    // Consider adding equals() and hashCode()
} 