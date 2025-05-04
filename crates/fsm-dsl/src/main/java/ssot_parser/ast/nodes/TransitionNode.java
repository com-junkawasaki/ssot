package ssot_parser.ast.nodes;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import ssot_parser.ast.AstNode;
import ssot_parser.ast.NodeVisitor;
import java.util.ArrayList;
import ssot_parser.ast.nodes.AnnotationNode;

/**
 * Represents a state transition specification, typically used within
 * an EventHandlerNode, ConditionalTransitionNode, or InvokeCompletionHandler.
 * It defines the target state, optional guard condition, and actions to execute.
 */
public class TransitionNode implements AstNode {
    public final String targetStateName; // Name of the target state (e.g., "StateName", ".history")
    public final Optional<String> condition;   // Optional guard condition reference (name, includes potential "(not)")
    public final List<String> actions;     // List of action references (names)
    private final List<AnnotationNode> annotations; // Annotations defined within the transition { ... } block
    // Optional ID can be derived from annotations if needed.

    // Simplified Constructor
    public TransitionNode(String targetStateName,
                          Optional<String> condition,
                          List<String> actions,
                          List<AnnotationNode> annotations) {
        // Source state and event are implicit from the context where this node is used.
        this.targetStateName = targetStateName;
        this.condition = condition;
        this.actions = actions != null ? Collections.unmodifiableList(new ArrayList<>(actions)) : Collections.emptyList();
        this.annotations = Collections.unmodifiableList(annotations != null ? new ArrayList<>(annotations) : Collections.emptyList());
    }

    // Optional: Add getId() that searches annotations if needed.
    public Optional<Long> getId() {
        return annotations.stream()
                .filter(anno -> "@id".equals(anno.name) && anno.value instanceof Long)
                .map(anno -> (Long) anno.value)
                .findFirst();
    }

    // getAnnotations() can remain similar, but it's now based on internal list
    public Map<String, Object> getAnnotations() {
        Map<String, Object> annotationMap = new HashMap<>();
        for (AnnotationNode annotation : this.annotations) {
             // Filter out @id if getId() is the primary way to access it?
             // Or include it here? Let's include it for completeness.
            annotationMap.put(annotation.name, annotation.value);
        }
        return Collections.unmodifiableMap(annotationMap);
    }

    // Getters for main properties
    public String getTargetStateName() { return targetStateName; }
    public Optional<String> getCondition() { return condition; }
    public List<String> getActions() { return actions; }

    @Override
    public <T> T accept(NodeVisitor<T> visitor) {
        return visitor.visitTransitionNode(this);
    }

    @Override
    public String toString() {
        return "TransitionNode{" +
               "target='" + targetStateName + "\'" +
               ", condition=" + condition.orElse("none") +
               ", actions=" + actions +
               ", annotations=" + annotations +
               "}";
    }

    // Consider adding equals() and hashCode()
} 