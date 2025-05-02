package ssot_parser.ast.nodes;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import ssot_parser.ast.AstNode;
import ssot_parser.NodeWithId;
import ssot_parser.ast.NodeVisitor;
import java.util.ArrayList;
import ssot_parser.ast.nodes.AnnotationNode;

/**
 * Represents a state transition triggered by an event.
 * Corresponds to the 'onTransition' or 'invokeTransition' rule.
 */
public class TransitionNode implements AstNode, NodeWithId {
    public final Optional<Long> id;
    public final String sourceStateName; // Name of the source state (if applicable, e.g., not for invoke transitions)
    public final String targetStateName; // Name of the target state
    public final Optional<String> event;         // Event triggering the transition (or onDone/onError for invoke), now optional
    public final Optional<String> condition;   // Optional guard condition reference (name)
    public final List<String> actions;     // List of action references (names)
    private final List<AnnotationNode> annotations; // Keep internal representation as List

    // Constructor - includes source state, guard condition (optional), and actions (list, optional)
    public TransitionNode(Optional<Long> id, String sourceStateName, String targetStateName, Optional<String> event,
                          Optional<String> condition, List<String> actions,
                          List<AnnotationNode> annotations) {
        this.id = id;
        this.sourceStateName = sourceStateName; // Can be null/empty for invoke transitions
        this.targetStateName = targetStateName;
        this.event = event;
        this.condition = condition;
        this.actions = actions != null ? Collections.unmodifiableList(new ArrayList<>(actions)) : Collections.emptyList(); // Ensure list initialization
        this.annotations = Collections.unmodifiableList(annotations != null ? new ArrayList<>(annotations) : Collections.emptyList());
    }

    @Override
    public Optional<Long> getId() {
        return id;
    }

    @Override
    public Map<String, Object> getAnnotations() { // Correct return type
        Map<String, Object> annotationMap = new HashMap<>();
        for (AnnotationNode annotation : this.annotations) {
            annotationMap.put(annotation.name, annotation.value);
        }
        return Collections.unmodifiableMap(annotationMap);
    }

    // Getters for main properties (optional, use public fields instead if preferred)
    public String getSourceStateName() { return sourceStateName; }
    public String getTargetStateName() { return targetStateName; }
    public Optional<String> getEvent() { return event; }
    public Optional<String> getCondition() { return condition; }
    public List<String> getActions() { return actions; } // Changed getter

    @Override
    public <T> T accept(NodeVisitor<T> visitor) {
        return visitor.visitTransitionNode(this);
    }

    @Override
    public String toString() {
        // Updated toString
        return "TransitionNode{" +
               "id=" + id.map(String::valueOf).orElse("none") +
               ", source='" + sourceStateName + "\'" +
               ", target='" + targetStateName + "\'" +
               ", event=" + event.orElse("always") + // Indicate always transitions
               ", condition=" + condition.orElse("none") +
               ", actions=" + actions + // Updated for list
               ", annotations=" + annotations +
               "}";
    }

    // Consider adding equals() and hashCode()
} 