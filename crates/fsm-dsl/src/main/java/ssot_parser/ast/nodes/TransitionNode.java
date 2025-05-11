package ssot_parser.ast.nodes;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import ssot_parser.ast.AstNode;
import ssot_parser.ast.NodeVisitor;
import java.util.ArrayList;
import ssot_parser.NodeWithId;

/**
 * Represents a state transition triggered by an event, timer, or condition.
 * Includes target state, optional guard, actions, and annotations.
 */
public class TransitionNode implements AstNode, NodeWithId {

    public enum TransitionType {
        EVENT,      // Triggered by an event (e.g., ON event)
        AFTER,      // Triggered after a delay (e.g., AFTER duration)
        CONDITIONAL, // Triggered by a guard condition (e.g., IF guard)
        ALWAYS      // An immediate, unconditional transition (not directly in grammar yet, but common in FSMs)
    }

    private final Optional<Long> id;
    private String sourceStateName; // Added later during linking/validation?
    private final String event; // Event name for EVENT type, synthetic for others (e.g., "@IF:guardName")
    public final String targetStateName;
    public final Optional<String> conditionRef; // Guard reference
    public final List<String> actionRefs; // Action references
    private final List<AnnotationNode> annotations;
    private final TargetStateNode targetState;
    private final Optional<GuardReferenceNode> condition; // Explicit condition for CONDITIONAL, or guard within TransitionSpec
    private final List<ActionReferenceNode> actions;
    private final List<GuardReferenceNode> guards; // Guards specified within a transition block
    private final List<String> allowedActors; // Actors specified within a transition block
    private final Optional<DurationNode> delay; // For AFTER transitions
    private final TransitionType type;
    private final Map<String, Object> annotationsMap; // From annotations on the transition line itself

    // Fields for specific transition types
    private boolean always = false; // For ALWAYS transitions

    // Constructor - potentially needs updates based on usage
    public TransitionNode(Optional<Long> id,
                          String sourceStateName, // Can be null initially
                          String event,
                          String targetStateName,
                          Optional<String> conditionRef,
                          List<String> actionRefs,
                          List<AnnotationNode> annotations,
                          TargetStateNode targetState,
                          Optional<GuardReferenceNode> condition, // Main condition for IF, or empty for ON/AFTER
                          List<ActionReferenceNode> actions,      // Actions from transition block
                          List<GuardReferenceNode> guards,        // Guards from transition block
                          List<String> allowedActors,       // Allowed actors from transition block
                          Optional<DurationNode> delay,         // Delay for AFTER transitions
                          TransitionType type,
                          Map<String, Object> annotationsMap) {
        this.id = id;
        this.sourceStateName = sourceStateName;
        this.event = event;
        this.targetStateName = targetStateName;
        this.conditionRef = conditionRef;
        this.actionRefs = Collections.unmodifiableList(actionRefs != null ? new ArrayList<>(actionRefs) : Collections.emptyList());
        this.annotations = Collections.unmodifiableList(annotations != null ? new ArrayList<>(annotations) : Collections.emptyList());
        this.targetState = targetState;
        this.condition = condition;
        this.actions = Collections.unmodifiableList(actions != null ? new ArrayList<>(actions) : Collections.emptyList());
        this.guards = Collections.unmodifiableList(guards != null ? new ArrayList<>(guards) : Collections.emptyList());
        this.allowedActors = Collections.unmodifiableList(allowedActors != null ? new ArrayList<>(allowedActors) : Collections.emptyList());
        this.delay = delay;
        this.type = type;
        this.annotationsMap = Collections.unmodifiableMap(annotationsMap != null ? new HashMap<>(annotationsMap) : Collections.emptyMap());
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

    public String getEvent() {
        return event;
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

    public TargetStateNode getTargetState() {
        return targetState;
    }

    public Optional<GuardReferenceNode> getCondition() {
        return condition;
    }

    public List<ActionReferenceNode> getActions() {
        return actions;
    }

    public List<GuardReferenceNode> getGuards() {
        return guards;
    }

    public List<String> getAllowedActors() {
        return allowedActors;
    }

    public Optional<DurationNode> getDelay() {
        return delay;
    }

    public TransitionType getType() {
        return type;
    }

    @Override
    public Map<String, Object> getAnnotations() {
        return annotationsMap;
    }

    public List<AnnotationNode> getAnnotationNodes() {
        return annotations;
    }

    // New method
    public Optional<ActionReferenceNode> getAction() {
        if (this.actions != null && !this.actions.isEmpty()) {
            return Optional.of(this.actions.get(0)); // Return the first action if multiple are present
        }
        return Optional.empty();
    }

    // Getters and Setters for new fields
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
        sb.append(", type=").append(type);
        if (event != null) sb.append(", event='").append(event).append("'");
        getSourceStateName().ifPresent(s -> sb.append(", source=").append(s));
        sb.append(", target=").append(targetStateName);
        conditionRef.ifPresent(c -> sb.append(", condition=").append(c));
        if (!actionRefs.isEmpty()) sb.append(", actions=").append(actionRefs);
        if (!guards.isEmpty()) sb.append(", guards=").append(guards);
        if (!allowedActors.isEmpty()) sb.append(", allowedActors=").append(allowedActors);
        delay.ifPresent(d -> sb.append(", delay=").append(d));
        if (always) sb.append(", always=true");
        if (!annotationsMap.isEmpty()) sb.append(", annotations=").append(annotationsMap);
        sb.append("}");
        return sb.toString();
    }

    // Consider adding equals() and hashCode()
} 