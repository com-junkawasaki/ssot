package ssot_parser.ast.nodes;

import ssot_parser.ast.AstNode;
import ssot_parser.ast.NodeVisitor; // Assuming you have or will have a visitor pattern

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

// Basic placeholder for Machine Definition Node
public class MachineNode implements AstNode {
    // private final List<AnnotationNode> annotations; // Assuming AnnotationNode exists or will be created
    private final Map<String, Object> annotations; // Changed to Map
    private final String name;
    private final Optional<ContextNode> context; // Changed from Optional<AstNode>
    private final List<ActionNode> actions;     // Changed from List<AstNode>
    private final List<GuardNode> guards;      // Changed from List<AstNode>
    private final List<InvokeNode> invokes;     // Changed from List<AstNode>
    private final List<StateNode> states;      // Changed from List<AstNode>
    private final Optional<String> initialState; // Changed from Optional<String> (was correct, just clarifying)
    // Transitions are now collected and stored here
    private final List<TransitionNode> transitions; // Changed from List<AstNode>

    // Constructor - updated states, initialState, and transitions types
    public MachineNode(Map<String, Object> annotations, // Changed type
                       String name, Optional<ContextNode> context,
                       List<ActionNode> actions, List<GuardNode> guards, List<InvokeNode> invokes,
                       List<StateNode> states, Optional<String> initialState,
                       List<TransitionNode> transitions) {
        this.annotations = annotations != null ? Collections.unmodifiableMap(annotations) : Collections.emptyMap(); // Assign map
        this.name = name;
        this.context = context; // Assign the Optional<ContextNode>
        this.actions = actions != null ? Collections.unmodifiableList(actions) : Collections.emptyList();
        this.guards = guards != null ? Collections.unmodifiableList(guards) : Collections.emptyList();
        this.invokes = invokes != null ? Collections.unmodifiableList(invokes) : Collections.emptyList();
        this.states = states != null ? Collections.unmodifiableList(states) : Collections.emptyList();
        this.initialState = initialState;
        this.transitions = transitions != null ? Collections.unmodifiableList(transitions) : Collections.emptyList();
        // TODO: Add validation or further initialization if needed
        // - Validate initialState refers to an actual state name in the list?
        // - Validate transition sources/targets refer to actual state names?
    }

    // public List<AnnotationNode> getAnnotations() {
    //     return annotations;
    // }
    @Override // Added Override
    public Map<String, Object> getAnnotations() { // Changed return type to Map
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

    public List<GuardNode> getGuards() { // Changed return type
        return guards;
    }

    public List<InvokeNode> getInvokes() { // Changed return type
        return invokes;
    }

    public List<StateNode> getStates() { // Changed return type
        return states;
    }

    public Optional<String> getInitialState() { // Return type was already Optional<String>
        return initialState;
    }

    public List<TransitionNode> getTransitions() { // Changed return type
        return transitions;
    }

    @Override
    public <T> T accept(NodeVisitor<T> visitor) {
        // Assuming a visitor pattern for processing the AST
        return visitor.visitMachineNode(this);
    }

    @Override
    public String toString() {
        return "MachineNode{" +
               "name='" + name + "\'" +
               ", annotations=" + annotations +
               ", context=" + context +
               ", actions=" + actions +
               ", guards=" + guards +
               ", invokes=" + invokes +
               ", states=" + states +
               ", initialState=" + initialState +
               ", transitions=" + transitions +
               "}";
    }

    // Potentially add equals() and hashCode() methods
} 