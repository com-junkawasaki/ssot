package ssot_parser.ast.nodes;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import ssot_parser.ast.AstNode;
import ssot_parser.NodeWithId;
import ssot_parser.ast.NodeVisitor;
import ssot_parser.ast.nodes.TransitionNode;

/**
 * Represents a state definition within the 'states { ... }' block of a machine.
 * Corresponds to the 'stateDefinition' rule.
 */
public class StateNode implements NodeWithId {
    private final Optional<Long> id; // Optional @id annotation
    public final String name;
    private final Map<String, Object> annotations; // Other annotations like $initial?
    private final List<String> entryActions; // List of action names referenced in ON_ENTRY
    private final List<String> exitActions;  // List of action names referenced in ON_EXIT
    private final List<TransitionNode> transitions; // Transitions defined within this state using ON
    private final boolean isInitial; // Flag indicating if this is the initial state
    // TODO: Add fields for nested states (List<StateNode>), invokes (List<InvokeNode>), history, etc. based on full grammar

    // Constructor - includes isInitial flag
    public StateNode(Optional<Long> id, String name, Map<String, Object> annotations,
                     List<String> entryActions, List<String> exitActions,
                     List<TransitionNode> transitions, boolean isInitial) {
        this.id = id;
        this.name = name;
        this.annotations = Collections.unmodifiableMap(annotations != null ? new HashMap<>(annotations) : Collections.emptyMap());
        this.entryActions = entryActions != null ? Collections.unmodifiableList(entryActions) : Collections.emptyList();
        this.exitActions = exitActions != null ? Collections.unmodifiableList(exitActions) : Collections.emptyList();
        this.transitions = transitions != null ? Collections.unmodifiableList(transitions) : Collections.emptyList();
        this.isInitial = isInitial;
    }

    @Override
    public Optional<Long> getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    @Override
    public Map<String, Object> getAnnotations() {
        return annotations;
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

    public boolean isInitial() {
        return isInitial;
    }

    @Override
    public <T> T accept(NodeVisitor<T> visitor) {
        return visitor.visitStateNode(this);
    }

    @Override
    public String toString() {
        return "StateNode{" +
               "id=" + id +
               ", name='" + name + "\'" +
               ", isInitial=" + isInitial +
               ", annotations=" + annotations +
               ", entryActions=" + entryActions +
               ", exitActions=" + exitActions +
               ", transitions=" + transitions +
               // Add other fields (nested states, invokes) here
               "}";
    }

    // Consider adding equals() and hashCode()
} 