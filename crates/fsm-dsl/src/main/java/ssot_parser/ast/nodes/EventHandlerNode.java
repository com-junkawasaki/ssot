package ssot_parser.ast.nodes;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import ssot_parser.ast.AstNode;
import ssot_parser.ast.NodeVisitor;
import java.util.ArrayList;
import java.util.Map;

/**
 * Represents an event handler within a state, such as 'on EVENT', 'after DURATION', or 'always'.
 * It links the trigger (event, delay, always) to a specific transition specification.
 */
public class EventHandlerNode implements AstNode {
    // Event details
    public final Optional<String> eventName; // Name of the event, or special markers like "always", "after(delay)"
    public final boolean isAlwaysTransition;
    public final Optional<String> delay; // e.g., "100ms", present only for 'after' transitions

    // The transition specification triggered by this handler
    public final TransitionNode transition;

    public EventHandlerNode(String eventName, boolean isAlwaysTransition, Optional<String> delay, TransitionNode transition) {
        this.eventName = Optional.ofNullable(eventName);
        this.isAlwaysTransition = isAlwaysTransition;
        this.delay = delay;
        if (transition == null) {
             System.err.println("Error: EventHandlerNode created with null transition for event: " + eventName);
             // Create a dummy transition to avoid NullPointerExceptions downstream?
             this.transition = new TransitionNode("ERROR_NULL_TRANSITION", Optional.empty(), Collections.emptyList(), Collections.emptyList());
        } else {
             this.transition = transition;
        }
    }

    // Getters
    public Optional<String> getEventName() {
        return eventName;
    }

    public boolean isAlwaysTransition() {
        return isAlwaysTransition;
    }

    public Optional<String> getDelay() {
        return delay;
    }

    public TransitionNode getTransition() {
        return transition;
    }

    @Override
    public <T> T accept(NodeVisitor<T> visitor) {
        return visitor.visitEventHandlerNode(this);
    }

    @Override
    public String toString() {
        return "EventHandlerNode{" +
               "event='" + eventName.orElse("none") + "\'" +
               ", isAlways=" + isAlwaysTransition +
               ", delay=" + delay.orElse("none") +
               ", transition=" + transition +
               "}";
    }

    // No ID or annotations directly on the handler, they are within the TransitionNode
     @Override public Map<String, Object> getAnnotations() { return transition != null ? transition.getAnnotations() : Collections.emptyMap(); }
     @Override public Optional<Long> getId() { return transition != null ? transition.getId() : Optional.empty(); }
} 