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
import ssot_parser.NodeWithId;

/**
 * Represents a state transition triggered by an event, timer, or condition.
 * Includes target state, optional guard, actions, and annotations.
 */
public class TransitionNode implements AstNode, NodeWithId {
    private final Optional<Long> id;
    private String sourceStateName; // Added later during linking/validation?
    private String event; // Event name, or synthetic like AFTER_duration, ALWAYS, IF_guard
    public final String targetStateName;
    public final Optional<String> conditionRef; // Guard reference
    public final List<String> actionRefs; // Action references
    private final List<AnnotationNode> annotations;

    // Fields for specific transition types
    private String delay; // For AFTER transitions (e.g., "100ms")
    private boolean always = false; // For ALWAYS transitions

    // Constructor - potentially needs updates based on usage
    public TransitionNode(Optional<Long> id,
                          String sourceStateName, // Can be null initially
                          String event, // Can be null initially
                          String targetStateName,
                          Optional<String> conditionRef,
                          List<String> actionRefs,
                          List<AnnotationNode> annotations) {
        this.id = id;
        this.sourceStateName = sourceStateName;
        this.event = event;
        this.targetStateName = targetStateName;
        this.conditionRef = conditionRef;
        this.actionRefs = Collections.unmodifiableList(actionRefs != null ? new ArrayList<>(actionRefs) : Collections.emptyList());
        this.annotations = Collections.unmodifiableList(annotations != null ? new ArrayList<>(annotations) : Collections.emptyList());
        this.delay = null;
        this.always = false;
    }

    @Override
    public Optional<Long> getId() {
        return id;
    }

    public Optional<String> getSourceStateName() {
        return Optional.ofNullable(sourceStateName);
    }

    public void setSourceStateName(String sourceStateName) {
        this.sourceStateName = sourceStateName;
    }

    public Optional<String> getEvent() {
        return Optional.ofNullable(event);
    }

    public void setEvent(String event) {
        this.event = event;
    }

    public String getTargetStateName() {
        return targetStateName;
    }

    public Optional<String> getConditionRef() {
        return conditionRef;
    }

    public List<String> getActionRefs() {
        return actionRefs;
    }

    @Override
    public Map<String, Object> getAnnotations() {
        Map<String, Object> annotationMap = new HashMap<>();
        for (AnnotationNode annotation : this.annotations) {
             if (!"@id".equals(annotation.name) || !id.isPresent()) { // Exclude @id if already exposed via getId()
                annotationMap.put(annotation.name, annotation.value);
            }
        }
        return Collections.unmodifiableMap(annotationMap);
    }

     public List<AnnotationNode> getAnnotationNodes() {
        return annotations;
    }

    // Getters and Setters for new fields
    public Optional<String> getDelay() {
        return Optional.ofNullable(delay);
    }

    public void setDelay(String delay) {
        this.delay = delay;
    }

    public boolean isAlways() {
        return always;
    }

    public void setAlways(boolean always) {
        this.always = always;
    }

    @Override
    public <T> T accept(NodeVisitor<T> visitor) {
        return visitor.visitTransitionNode(this);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("TransitionNode{");
        sb.append("id=").append(id.map(String::valueOf).orElse("none"));
        getEvent().ifPresent(e -> sb.append(", event=").append(e));
        getSourceStateName().ifPresent(s -> sb.append(", source=").append(s));
        sb.append(", target=").append(targetStateName);
        conditionRef.ifPresent(c -> sb.append(", condition=").append(c));
        if (!actionRefs.isEmpty()) sb.append(", actions=").append(actionRefs);
        getDelay().ifPresent(d -> sb.append(", delay=").append(d));
        if (always) sb.append(", always=true");
        if (!annotations.isEmpty()) sb.append(", annotations=").append(annotations);
        sb.append("}");
        return sb.toString();
    }

    // Consider adding equals() and hashCode()
} 