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

/**
 * Represents a state definition within the 'states { ... }' block of a machine.
 * Corresponds to the 'stateDefinition' rule.
 */
public class StateNode implements NodeWithId {
    private final Optional<Long> id; // Optional @id annotation
    public final String name;
    private final List<AnnotationNode> annotations; // Changed type
    private final List<String> entryActions; // List of action names referenced in ON_ENTRY
    private final List<String> exitActions;  // List of action names referenced in ON_EXIT
    private final List<TransitionNode> transitions; // Transitions defined within this state using ON
    private final List<InvokeStateNode> invokes; // Add list for invokes
    private final List<StateNode> nestedStates; // Add list for nested states
    // TODO: Add fields for nested states (List<StateNode>), invokes (List<InvokeNode>), history, etc. based on full grammar

    // Constructor - without isInitial flag
    public StateNode(Optional<Long> id, String name, List<AnnotationNode> annotations,
                     List<String> entryActions, List<String> exitActions,
                     List<TransitionNode> transitions,
                     List<InvokeStateNode> invokes,
                     List<StateNode> nestedStates /* Add other fields like history */) { // Add nestedStates parameter
        this.id = id;
        this.name = name;
        this.annotations = Collections.unmodifiableList(annotations != null ? new ArrayList<>(annotations) : Collections.emptyList());
        this.entryActions = entryActions != null ? Collections.unmodifiableList(entryActions) : Collections.emptyList();
        this.exitActions = exitActions != null ? Collections.unmodifiableList(exitActions) : Collections.emptyList();
        this.transitions = transitions != null ? Collections.unmodifiableList(transitions) : Collections.emptyList();
        this.invokes = invokes != null ? Collections.unmodifiableList(invokes) : Collections.emptyList(); // Assign invokes
        this.nestedStates = nestedStates != null ? Collections.unmodifiableList(nestedStates) : Collections.emptyList(); // Assign nestedStates
    }

    @Override
    public Optional<Long> getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    @Override
    public List<AnnotationNode> getAnnotations() {
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

    public List<InvokeStateNode> getInvokes() { // Add getter for invokes
        return invokes;
    }

    public List<StateNode> getNestedStates() { // Add getter for nestedStates
        return nestedStates;
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
               ", annotations=" + annotations +
               ", entryActions=" + entryActions +
               ", exitActions=" + exitActions +
               ", transitions=" + transitions +
               ", invokes=" + invokes +
               // Add other fields (nested states, invokes) here
               ", nestedStates=" + nestedStates +
               "}";
    }

    // Consider adding equals() and hashCode()
} 