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
import ssot_parser.ast.nodes.TransitionNode;
import ssot_parser.ast.type.StateType;

/**
 * Represents a state definition within the 'states { ... }' block of a machine.
 * Corresponds to the 'stateDefinition' rule.
 */
public class StateNode implements AstNode, NodeWithId {
    private final Optional<Long> id; // Optional @id annotation
    public final String stateName;
    private final String displayName; // From $name annotation or defaults to stateName
    public final List<String> entryActions; // List of action names referenced in entry
    public final List<String> exitActions;  // List of action names referenced in exit
    public final List<InvokeStateNode> invokeInvocations;
    public final List<TransitionNode> eventHandlers; // Changed from EventHandlerNode
    public final List<TransitionNode> ifTransitions; // Changed from ConditionalTransitionNode
    public final List<StateNode> nestedStates; // List for nested states (children)
    public final StateType type; // Type of state (ATOMIC, COMPOUND, PARALLEL, FINAL)
    public final Optional<String> initialStateName; // Changed from String, made Optional
    public final List<HistoryStateNode> historyStates;
    private final List<AnnotationNode> annotations;

    public StateNode(Optional<Long> id,
                     String stateName,
                     List<AnnotationNode> annotations, // Parameter order and type changed
                     String displayNameValue, // Used to pass displayName from visitor
                     StateType type,
                     List<String> entryActions,
                     List<String> exitActions,
                     List<InvokeStateNode> invokeInvocations,
                     List<TransitionNode> eventHandlers, // Changed from EventHandlerNode
                     List<TransitionNode> ifTransitions, // Changed from ConditionalTransitionNode
                     List<StateNode> nestedStates,
                     List<HistoryStateNode> historyStates,
                     Optional<String> initialStateNameValue) { // Parameter added
        this.id = id;
        this.stateName = stateName;
        this.annotations = Collections.unmodifiableList(annotations != null ? new ArrayList<>(annotations) : Collections.emptyList());
        this.displayName = displayNameValue != null ? displayNameValue : stateName; // Initialize from passed value
        this.type = type;
        this.entryActions = Collections.unmodifiableList(entryActions != null ? new ArrayList<>(entryActions) : Collections.emptyList());
        this.exitActions = Collections.unmodifiableList(exitActions != null ? new ArrayList<>(exitActions) : Collections.emptyList());
        this.invokeInvocations = Collections.unmodifiableList(invokeInvocations != null ? new ArrayList<>(invokeInvocations) : Collections.emptyList());
        this.eventHandlers = Collections.unmodifiableList(eventHandlers != null ? new ArrayList<>(eventHandlers) : Collections.emptyList());
        this.ifTransitions = Collections.unmodifiableList(ifTransitions != null ? new ArrayList<>(ifTransitions) : Collections.emptyList());
        this.nestedStates = Collections.unmodifiableList(nestedStates != null ? new ArrayList<>(nestedStates) : Collections.emptyList());
        this.historyStates = Collections.unmodifiableList(historyStates != null ? new ArrayList<>(historyStates) : Collections.emptyList());
        this.initialStateName = initialStateNameValue; // Initialize from parameter
    }

    @Override
    public Optional<Long> getId() {
        return id;
    }

    public String getStateName() {
        return stateName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public Map<String, Object> getAnnotations() {
        Map<String, Object> annotationMap = new HashMap<>();
        if (this.annotations != null) {
             for (AnnotationNode annotation : this.annotations) {
                if (!"@id".equals(annotation.name) || !id.isPresent()) {
                     annotationMap.put(annotation.name, annotation.value);
                 }
             }
        }
        // Add $name implicitly if not present?
        // annotationMap.putIfAbsent("$name", this.displayName);
        return Collections.unmodifiableMap(annotationMap);
    }

    public List<String> getEntryActions() {
        return entryActions;
    }

    public List<String> getExitActions() {
        return exitActions;
    }

    public List<InvokeStateNode> getInvokeInvocations() {
        return invokeInvocations;
    }

    public List<TransitionNode> getEventHandlers() {
        return eventHandlers;
    }

    public List<TransitionNode> getIfTransitions() {
        return ifTransitions;
    }

    public List<StateNode> getNestedStates() {
        return nestedStates;
    }

    public StateType getType() {
        return type;
    }

    public Optional<String> getInitialStateName() { // Return type changed to Optional<String>
        return initialStateName;
    }

    public List<HistoryStateNode> getHistoryStates() {
        return historyStates;
    }

    public List<AnnotationNode> getAnnotationNodes() {
        return annotations;
    }

    public List<TransitionNode> getTransitions() {
        List<TransitionNode> allTransitions = new ArrayList<>();
        if (this.eventHandlers != null) {
            allTransitions.addAll(this.eventHandlers);
        }
        // Assuming ifTransitions are also a direct part of StateNode's transitions.
        // If IfConditionTransitionNode is different and needs to be converted or handled separately,
        // this will need adjustment. Based on current StateNode, ifTransitions is List<TransitionNode>.
        if (this.ifTransitions != null) {
            allTransitions.addAll(this.ifTransitions);
        }
        return Collections.unmodifiableList(allTransitions);
    }

    public Optional<HistoryStateNode> getHistory() {
        if (this.historyStates != null && !this.historyStates.isEmpty()) {
            return Optional.of(this.historyStates.get(0)); // Returning the first one if multiple exist
        }
        return Optional.empty();
    }

    public Optional<InvokeStateNode> getInvoke() {
        if (this.invokeInvocations != null && !this.invokeInvocations.isEmpty()) {
            return Optional.of(this.invokeInvocations.get(0)); // Returning the first one
        }
        return Optional.empty();
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
               ", entryActions=" + entryActions +
               ", exitActions=" + exitActions +
               ", invokes=" + invokeInvocations +
               ", eventHandlers=" + eventHandlers +
               ", ifTransitions=" + ifTransitions +
               ", nestedStates=" + nestedStates.size() +
               ", initialStateName=" + initialStateName.orElse("none") +
               ", historyStates=" + historyStates.size() +
               ", annotations=" + annotations +
               "}";
    }

    // Consider adding equals() and hashCode()
} 