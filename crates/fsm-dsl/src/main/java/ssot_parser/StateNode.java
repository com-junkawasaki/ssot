package ssot_parser;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import ssot_parser.ast.AstNode;
import ssot_parser.ast.NodeVisitor;

/** Represents a state definition */
public class StateNode implements NodeWithId {
    private final Optional<Long> id;
    public final String name;
    public final List<ActionNode> actions; // Combine entry/exit actions for now
    // TODO: Consider adding fields for transitions, nested states, history, etc.
    private final Map<String, Object> annotations;

    public StateNode(Optional<Long> id, String name, List<ActionNode> actions, Map<String, Object> annotations) {
        this.id = id;
        this.name = name;
        this.actions = Collections.unmodifiableList(actions != null ? actions : Collections.emptyList());
        this.annotations = Collections.unmodifiableMap(annotations != null ? new HashMap<>(annotations) : Collections.emptyMap());
    }

    @Override
    public Optional<Long> getId() {
        return id;
    }

    public String getName() { return name; }
    public List<ActionNode> getActions() { return actions; }

    @Override
    public Map<String, Object> getAnnotations() {
        return annotations;
    }
} 