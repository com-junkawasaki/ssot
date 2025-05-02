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
import ssot_parser.ast.nodes.TransitionNode;
import ssot_parser.ast.nodes.AnnotationNode;
import ssot_parser.ast.nodes.InvokeStateNode;
import ssot_parser.ast.nodes.HistoryNode;
import ssot_parser.ast.nodes.type.StateType;

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
    public final List<TransitionNode> transitions; // Transitions defined within this state using on
    public final Optional<InvokeStateNode> invoke; // Changed to Optional, as a state has at most one invoke
    public final List<StateNode> nestedStates; // List for nested states (children)
    public final Optional<HistoryNode> history; // Optional history node
    public final StateType type; // Type of state (ATOMIC, COMPOUND, PARALLEL, FINAL)
    public final String initialStateName; // Name of the initial child state (for compound/parallel)

    public StateNode(Optional<Long> id, String stateName, List<AnnotationNode> annotations,
                     List<String> entryActions, List<String> exitActions,
                     List<TransitionNode> transitions,
                     Optional<InvokeStateNode> invoke,
                     List<StateNode> nestedStates,
                     Optional<HistoryNode> history,
                     StateType type,
                     String initialStateName) {
        this.id = id;
        this.stateName = stateName;
        this.annotations = Collections.unmodifiableList(annotations != null ? new ArrayList<>(annotations) : Collections.emptyList());
        this.entryActions = entryActions != null ? Collections.unmodifiableList(new ArrayList<>(entryActions)) : Collections.emptyList();
        this.exitActions = exitActions != null ? Collections.unmodifiableList(new ArrayList<>(exitActions)) : Collections.emptyList();
        this.transitions = transitions != null ? Collections.unmodifiableList(new ArrayList<>(transitions)) : Collections.emptyList();
        this.invoke = invoke;
        this.nestedStates = nestedStates != null ? Collections.unmodifiableList(new ArrayList<>(nestedStates)) : Collections.emptyList();
        this.history = history;
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

    public List<TransitionNode> getTransitions() {
        return transitions;
    }

    public Optional<InvokeStateNode> getInvoke() {
        return invoke;
    }

    public List<StateNode> getNestedStates() {
        return nestedStates;
    }

    public Optional<HistoryNode> getHistory() {
        return history;
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
               ", transitions=" + transitions +
               ", invoke=" + invoke +
               ", nestedStates=" + nestedStates +
               ", history=" + history +
               "}";
    }

    // Consider adding equals() and hashCode()
} 