package ssot_parser;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import ssot_parser.ast.AstNode;
import ssot_parser.ast.NodeVisitor;
import ssot_parser.ast.nodes.TransitionNode;

/** Represents a state definition */
public class StateNode implements NodeWithId {
    private final Optional<Long> id;
    public final String name;
    private final List<String> entryActions;
    private final List<String> exitActions;
    private final List<TransitionNode> transitions;
    private final boolean isInitial;
    private final Map<String, Object> annotations;

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

    public String getName() { return name; }

    public List<String> getEntryActions() { return entryActions; }
    public List<String> getExitActions() { return exitActions; }
    public List<TransitionNode> getTransitions() { return transitions; }
    public boolean isInitial() { return isInitial; }

    @Override
    public Map<String, Object> getAnnotations() {
        return annotations;
    }

    // Consider adding equals() and hashCode()

    // Implementation for AstNode
    @Override
    public <T> T accept(NodeVisitor<T> visitor) {
        return visitor.visitStateNode(this);
    }
} 