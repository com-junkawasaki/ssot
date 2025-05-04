package ssot_parser.ast.nodes;

import ssot_parser.ast.AstNode;
import ssot_parser.NodeWithId;
import ssot_parser.ast.NodeVisitor;
import java.util.Map;
import java.util.Optional;
import java.util.List;
import java.util.Collections;
import java.util.HashMap;
import java.util.ArrayList;

/**
 * Represents the top-level 'machine { ... }' definition.
 */
public class MachineNode implements AstNode, NodeWithId {
    private final Optional<Long> id;
    public final String machineName;
    public final String initialStateName;
    public final Optional<ContextNode> context; // Context is optional
    public final List<ActionDefinitionNode> actions; // List of action definitions
    public final List<GuardDefinitionNode> guards;   // List of guard definitions
    public final List<InvokeDefinitionNode> invokes; // List of invoke definitions
    public final List<StateNode> states;        // List of top-level states
    private final List<AnnotationNode> annotations;

    public MachineNode(Optional<Long> id, String machineName, String initialStateName,
                       Optional<ContextNode> context, List<ActionDefinitionNode> actions,
                       List<GuardDefinitionNode> guards, List<InvokeDefinitionNode> invokes,
                       List<StateNode> states, List<AnnotationNode> annotations) {
        this.id = id;
        this.machineName = machineName;
        this.initialStateName = initialStateName;
        this.context = context;
        this.actions = actions != null ? Collections.unmodifiableList(new ArrayList<>(actions)) : Collections.emptyList();
        this.guards = guards != null ? Collections.unmodifiableList(new ArrayList<>(guards)) : Collections.emptyList();
        this.invokes = invokes != null ? Collections.unmodifiableList(new ArrayList<>(invokes)) : Collections.emptyList();
        this.states = states != null ? Collections.unmodifiableList(new ArrayList<>(states)) : Collections.emptyList();
        this.annotations = Collections.unmodifiableList(annotations != null ? new ArrayList<>(annotations) : Collections.emptyList());
    }

    @Override
    public Optional<Long> getId() {
        return id;
    }

    public String getMachineName() {
        return machineName;
    }

    public String getInitialStateName() {
        return initialStateName;
    }

    public Optional<ContextNode> getContext() {
        return context;
    }

    public List<ActionDefinitionNode> getActions() {
        return actions;
    }

    public List<GuardDefinitionNode> getGuards() {
        return guards;
    }

    public List<InvokeDefinitionNode> getInvokes() {
        return invokes;
    }

    public List<StateNode> getStates() {
        return states;
    }

    @Override
    public Map<String, Object> getAnnotations() {
        Map<String, Object> annotationMap = new HashMap<>();
        if (this.annotations != null) {
            for (AnnotationNode annotation : this.annotations) {
                annotationMap.put(annotation.name, annotation.value);
            }
        }
        return Collections.unmodifiableMap(annotationMap);
    }

    @Override
    public <T> T accept(NodeVisitor<T> visitor) {
        return visitor.visitMachineNode(this);
    }

    @Override
    public String toString() {
        return "MachineNode{" +
               "id=" + id.map(String::valueOf).orElse("none") +
               ", machineName='" + machineName + "\'" +
               ", initialStateName='" + initialStateName + "\'" +
               ", context=" + context.map(ContextNode::toString).orElse("none") +
               ", actions=" + actions +
               ", guards=" + guards +
               ", invokes=" + invokes +
               ", states=" + states +
               ", annotations=" + annotations +
               "}";
    }

    // Consider adding equals() and hashCode()
} 