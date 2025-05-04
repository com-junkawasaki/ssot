package ssot_parser.ast.nodes;

import java.util.*;
import ssot_parser.ast.AstNode;
import ssot_parser.NodeWithId;
import ssot_parser.ast.NodeVisitor;
import ssot_parser.ast.nodes.ContextNode;
import ssot_parser.ast.nodes.ActionNode;
import ssot_parser.ast.nodes.GuardNode;
import ssot_parser.ast.nodes.InvokeServiceNode; // Assuming InvokeNode was meant to be this
import ssot_parser.ast.nodes.StateNode;

/**
 * Represents a complete state machine definition ('machine Name { ... }').
 */
public class MachineDefinitionNode implements AstNode, NodeWithId {
    private final Optional<Long> id;
    public final String machineName;
    private final Map<String, Object> annotations;
    public final ContextNode context; // Assuming Optional<ContextNode> might be better, but using direct for now
    public final List<ActionNode> actions;
    public final List<GuardNode> guards;
    public final List<InvokeServiceNode> invokes;
    public final List<StateNode> states;
    public final String initialStateName; // Name of the initial state

    public MachineDefinitionNode(Optional<Long> id,
                                 String machineName,
                                 Map<String, Object> annotations,
                                 ContextNode context,
                                 List<ActionNode> actions,
                                 List<GuardNode> guards,
                                 List<InvokeServiceNode> invokes,
                                 List<StateNode> states,
                                 String initialStateName) {
        this.id = id;
        this.machineName = Objects.requireNonNull(machineName, "Machine name cannot be null");
        this.annotations = annotations != null ? Collections.unmodifiableMap(new HashMap<>(annotations)) : Collections.emptyMap();
        this.context = context; // Nullable?
        this.actions = Collections.unmodifiableList(actions != null ? new ArrayList<>(actions) : Collections.emptyList());
        this.guards = Collections.unmodifiableList(guards != null ? new ArrayList<>(guards) : Collections.emptyList());
        this.invokes = Collections.unmodifiableList(invokes != null ? new ArrayList<>(invokes) : Collections.emptyList());
        this.states = Collections.unmodifiableList(states != null ? new ArrayList<>(states) : Collections.emptyList());
        this.initialStateName = initialStateName; // Can be null if no states? Validation needed.

        // Basic validation: Initial state must exist if states are defined
        if (initialStateName != null && !states.isEmpty()) {
            boolean initialStateExists = states.stream().anyMatch(s -> s.stateName.equals(initialStateName));
            if (!initialStateExists) {
                // Consider throwing a specific exception or handling differently
                System.err.println("Error: Initial state '" + initialStateName + "' defined in machine '" + machineName + "' does not match any defined state.");
            }
        } else if (initialStateName == null && !states.isEmpty()) {
             System.err.println("Warning: Machine '" + machineName + "' has states but no initial state defined.");
        }

    }

    @Override
    public Optional<Long> getId() {
        return id;
    }

    @Override
    public Map<String, Object> getAnnotations() {
        return annotations;
    }

    // Getters for other fields...
    public String getMachineName() { return machineName; }
    public ContextNode getContext() { return context; }
    public List<ActionNode> getActions() { return actions; }
    public List<GuardNode> getGuards() { return guards; }
    public List<InvokeServiceNode> getInvokes() { return invokes; }
    public List<StateNode> getStates() { return states; }
    public String getInitialStateName() { return initialStateName; }


    @Override
    public <T> T accept(NodeVisitor<T> visitor) {
        // return visitor.visitMachineDefinitionNode(this); // Add to visitor
        System.err.println("Warning: NodeVisitor.visitMachineDefinitionNode not implemented yet.");
        return null;
    }

    @Override
    public String toString() {
        return "MachineDefinitionNode{" +
               "id=" + id.map(String::valueOf).orElse("none") +
               ", machineName='" + machineName + ''' +
               ", initialStateName='" + initialStateName + ''' +
               ", annotations=" + annotations +
               ", context=" + context +
               ", actions=" + actions +
               ", guards=" + guards +
               ", invokes=" + invokes +
               ", states=" + states +
               '}';
    }

    // Consider adding equals() and hashCode()
} 