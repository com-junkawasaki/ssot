package ssot_parser.ast.nodes;

import ssot_parser.ast.AstNode;
import ssot_parser.ast.NodeVisitor; // Assuming you have or will have a visitor pattern

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

// Basic placeholder for Machine Definition Node
public class MachineNode implements AstNode {
    private final List<AnnotationNode> annotations; // Assuming AnnotationNode exists or will be created
    private final String name;
    // Placeholders for other machine elements - their types might need refinement
    private final Optional<ContextNode> context; // Changed from Optional<AstNode>
    private final List<ActionNode> actions;     // Changed from List<AstNode>
    private final List<AstNode> guards;      // Type might be List<GuardNode>
    private final List<AstNode> invokes;     // Type might be List<InvokeNode>
    private final List<AstNode> states;      // Type might be List<StateNode> or Map<String, StateNode>
    private final Optional<String> initialState; // Or Optional<StateNode>
    // Transitions might be complex, represented differently
    private final List<AstNode> transitions; // Type might be List<TransitionNode>

    // Constructor - updated context and actions types
    public MachineNode(List<AnnotationNode> annotations, String name, Optional<ContextNode> context, List<ActionNode> actions, List<AstNode> guards, List<AstNode> invokes, List<AstNode> states, Optional<String> initialState, List<AstNode> transitions) {
        this.annotations = annotations != null ? Collections.unmodifiableList(annotations) : Collections.emptyList();
        this.name = name;
        this.context = context; // Assign the Optional<ContextNode>
        this.actions = actions != null ? Collections.unmodifiableList(actions) : Collections.emptyList();
        this.guards = guards != null ? Collections.unmodifiableList(guards) : Collections.emptyList();
        this.invokes = invokes != null ? Collections.unmodifiableList(invokes) : Collections.emptyList();
        this.states = states != null ? Collections.unmodifiableList(states) : Collections.emptyList();
        this.initialState = initialState;
        this.transitions = transitions != null ? Collections.unmodifiableList(transitions) : Collections.emptyList();
        // TODO: Add validation or further initialization if needed
    }

    public List<AnnotationNode> getAnnotations() {
        return annotations;
    }

    public String getName() {
        return name;
    }

    public Optional<ContextNode> getContext() { // Changed return type
        return context;
    }

    public List<ActionNode> getActions() { // Changed return type
        return actions;
    }

    public List<AstNode> getGuards() {
        return guards;
    }

    public List<AstNode> getInvokes() {
        return invokes;
    }

    public List<AstNode> getStates() {
        return states;
    }

    public Optional<String> getInitialState() {
        return initialState;
    }

    public List<AstNode> getTransitions() {
        return transitions;
    }

    @Override
    public <T> T accept(NodeVisitor<T> visitor) {
        // Assuming a visitor pattern for processing the AST
        // return visitor.visitMachineNode(this); // Example call
        throw new UnsupportedOperationException("Visitor pattern not yet implemented for MachineNode");
    }

    @Override
    public String toString() {
        return "MachineNode{" +
               "name='" + name + ''' +
               ", annotations=" + annotations +
               ", context=" + context +
               ", actions=" + actions +
               ", guards=" + guards +
               ", invokes=" + invokes +
               ", states=" + states +
               ", initialState=" + initialState +
               ", transitions=" + transitions +
               '}';
    }

    // Potentially add equals() and hashCode() methods
} 