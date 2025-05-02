package ssot_parser.ast.nodes;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import ssot_parser.ast.AstNode;
import ssot_parser.ast.NodeVisitor;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

/**
 * Represents a handler for a specific event (or lack thereof for always transitions)
 * within a state, containing one or more possible transitions (TransitionConfig).
 * Corresponds to the 'on EventName { ... }' or 'transition Target { ... }' syntax.
 */
public class EventHandlerNode implements AstNode {
    public final Optional<String> eventName; // Empty for always transitions
    public final List<TransitionConfig> transitions; // Usually one, list for potential future extensions

    public EventHandlerNode(Optional<String> eventName, List<TransitionConfig> transitions) {
        this.eventName = eventName;
        this.transitions = transitions != null ? Collections.unmodifiableList(new ArrayList<>(transitions)) : Collections.emptyList();
    }

    public Optional<String> getEventName() {
        return eventName;
    }

    public List<TransitionConfig> getTransitions() {
        return transitions;
    }

    @Override
    public Map<String, Object> getAnnotations() {
        // This node type currently does not support annotations directly
        return Collections.emptyMap();
    }

    @Override
    public <T> T accept(NodeVisitor<T> visitor) {
        return visitor.visitEventHandlerNode(this);
    }

    @Override
    public String toString() {
        return "EventHandlerNode{" +
               "event='" + eventName.orElse("always") + "\'" +
               ", transitions=" + transitions +
               "}";
    }

     // Consider adding equals() and hashCode()
} 