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
    private final String displayName; // From $name annotation or defaults to stateName
    public final List<String> entryActions; // List of action names referenced in entry
    public final List<String> exitActions;  // List of action names referenced in exit
    public final List<InvokeStateNode> invokeInvocations;
    public final List<EventHandlerNode> eventHandlers; // Handles 'on Event' and always transitions
    public final List<ConditionalTransitionNode> ifTransitions; // Handles 'if guard ...'
    public final List<StateNode> nestedStates; // List for nested states (children)
    public final StateType type; // Type of state (ATOMIC, COMPOUND, PARALLEL, FINAL)
    public final String initialStateName; // Name of the initial child state (for compound/parallel)
    public final List<HistoryStateNode> historyStates;
    private final List<AnnotationNode> annotations;

    public StateNode(Optional<Long> id,
                     String stateName,
                     String displayName,
                     StateType type,
                     List<String> entryActions,
                     List<String> exitActions,
                     List<InvokeStateNode> invokeInvocations,
                     List<EventHandlerNode> eventHandlers,
                     List<ConditionalTransitionNode> ifTransitions,
                     List<StateNode> nestedStates,
                     List<HistoryStateNode> historyStates,
                     List<AnnotationNode> annotations) {
        this.id = id;
        this.stateName = stateName;
        this.displayName = displayName != null ? displayName : stateName;
        this.type = type;
        this.entryActions = Collections.unmodifiableList(entryActions != null ? new ArrayList<>(entryActions) : Collections.emptyList());
        this.exitActions = Collections.unmodifiableList(exitActions != null ? new ArrayList<>(exitActions) : Collections.emptyList());
        this.invokeInvocations = Collections.unmodifiableList(invokeInvocations != null ? new ArrayList<>(invokeInvocations) : Collections.emptyList());
        this.eventHandlers = Collections.unmodifiableList(eventHandlers != null ? new ArrayList<>(eventHandlers) : Collections.emptyList());
        this.ifTransitions = Collections.unmodifiableList(ifTransitions != null ? new ArrayList<>(ifTransitions) : Collections.emptyList());
        this.nestedStates = Collections.unmodifiableList(nestedStates != null ? new ArrayList<>(nestedStates) : Collections.emptyList());
        this.historyStates = Collections.unmodifiableList(historyStates != null ? new ArrayList<>(historyStates) : Collections.emptyList());
        this.annotations = Collections.unmodifiableList(annotations != null ? new ArrayList<>(annotations) : Collections.emptyList());
        this.initialStateName = null; // Assuming initialStateName is not provided in the constructor
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

    public List<EventHandlerNode> getEventHandlers() {
        return eventHandlers;
    }

    public List<ConditionalTransitionNode> getIfTransitions() {
        return ifTransitions;
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

    public List<HistoryStateNode> getHistoryStates() {
        return historyStates;
    }

    public List<AnnotationNode> getAnnotationNodes() {
        return annotations;
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
               ", historyStates=" + historyStates.size() +
               ", annotations=" + annotations +
               "}";
    }

    // Consider adding equals() and hashCode()
} 