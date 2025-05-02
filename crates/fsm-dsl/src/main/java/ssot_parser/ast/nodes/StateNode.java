package ssot_parser.ast.nodes;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ArrayList;
import ssot_parser.ast.AstNode;
import ssot_parser.NodeWithId;
import ssot_parser.ast.NodeVisitor;
import ssot_parser.ast.nodes.AnnotationNode;
import ssot_parser.ast.nodes.InvokeStateNode;
import ssot_parser.ast.nodes.EventHandlerNode;
import ssot_parser.ast.nodes.ConditionalTransitionNode;
import ssot_parser.ast.type.StateType;

/**
 * Represents a state definition within the 'states { ... }' block of a machine.
 * Corresponds to the 'stateDefinition' rule.
 */
public class StateNode implements AstNode, NodeWithId {
    private final Optional<Long> id; // Optional @id annotation
    public final String stateName;
    private final List<AnnotationNode> annotations; // Keep internal representation as List
    public final List<String> entryActions; // List of action names referenced in entry
    public final List<String> exitActions;  // List of action names referenced in exit
    public final List<EventHandlerNode> eventHandlers; // Handles 'on Event' and always transitions
    public final List<ConditionalTransitionNode> conditionalTransitions; // Handles 'if guard ...'
    public final Optional<InvokeStateNode> invoke; // Changed to Optional, as a state has at most one invoke
    public final List<StateNode> nestedStates; // List for nested states (children)
    public final StateType type; // Type of state (ATOMIC, COMPOUND, PARALLEL, FINAL)
    public final String initialStateName; // Name of the initial child state (for compound/parallel)

    public StateNode(Optional<Long> id, String stateName, List<AnnotationNode> annotations,
                     List<String> entryActions, List<String> exitActions,
                     List<EventHandlerNode> eventHandlers,
                     List<ConditionalTransitionNode> conditionalTransitions,
                     Optional<InvokeStateNode> invoke,
                     List<StateNode> nestedStates,
                     StateType type,
                     String initialStateName) {
        this.id = id;
        this.stateName = stateName;
        this.annotations = Collections.unmodifiableList(annotations != null ? new ArrayList<>(annotations) : Collections.emptyList());
        this.entryActions = entryActions != null ? Collections.unmodifiableList(new ArrayList<>(entryActions)) : Collections.emptyList();
        this.exitActions = exitActions != null ? Collections.unmodifiableList(new ArrayList<>(exitActions)) : Collections.emptyList();
        this.eventHandlers = eventHandlers != null ? Collections.unmodifiableList(new ArrayList<>(eventHandlers)) : Collections.emptyList();
        this.conditionalTransitions = conditionalTransitions != null ? Collections.unmodifiableList(new ArrayList<>(conditionalTransitions)) : Collections.emptyList();
        this.invoke = invoke;
        this.nestedStates = nestedStates != null ? Collections.unmodifiableList(new ArrayList<>(nestedStates)) : Collections.emptyList();
        this.type = type;
        this.initialStateName = initialStateName;
    }

    @Override
    public Optional<Long> getId() {
        return id;
    }

    public String getStateName() {
        return stateName;
    }

    @Override
    public Map<String, Object> getAnnotations() {
        Map<String, Object> annotationMap = new HashMap<>();
        for (AnnotationNode annotation : this.annotations) {
            annotationMap.put(annotation.name, annotation.value);
        }
        return Collections.unmodifiableMap(annotationMap);
    }

    public List<String> getEntryActions() {
        return entryActions;
    }

    public List<String> getExitActions() {
        return exitActions;
    }

    public List<EventHandlerNode> getEventHandlers() {
        return eventHandlers;
    }

    public List<ConditionalTransitionNode> getConditionalTransitions() {
        return conditionalTransitions;
    }

    public Optional<InvokeStateNode> getInvoke() {
        return invoke;
    }

    public List<StateNode> getNestedStates() {
        return nestedStates;
    }

    public StateType getType() {
        return type;
    }

    public String getInitialStateName() {
        return initialStateName;
    }

    @Override
    public <T> T accept(NodeVisitor<T> visitor) {
        return visitor.visitStateNode(this);
    }

    @Override
    public String toString() {
        return "StateNode{" +
               "id=" + id.map(String::valueOf).orElse("none") +
               ", stateName='" + stateName + "\'" +
               ", type=" + type +
               ", initialStateName='" + initialStateName + "\'" +
               ", annotations=" + annotations +
               ", entryActions=" + entryActions +
               ", exitActions=" + exitActions +
               ", eventHandlers=" + eventHandlers +
               ", conditionalTransitions=" + conditionalTransitions +
               ", invoke=" + invoke +
               ", nestedStates=" + nestedStates +
               "}";
    }

    // Consider adding equals() and hashCode()
} 