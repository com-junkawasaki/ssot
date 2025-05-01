package ssot_parser.ast.nodes;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import ssot_parser.ast.AstNode;
import ssot_parser.NodeWithId;
import ssot_parser.ast.NodeVisitor;

/**
 * Represents an action definition within the 'actions { ... }' block of a machine.
 * Corresponds to the 'actionDefinition' rule in the grammar.
 */
public class ActionNode implements NodeWithId {
    // ID annotation is optional for actions
    private final Optional<Long> id;
    public final String name;
    // Store other annotations ($name, $flag) in a map
    private final Map<String, Object> annotations;

    // Constructor including annotations
    public ActionNode(Optional<Long> id, String name, Map<String, Object> annotations) {
        this.id = id;
        this.name = name;
        this.annotations = Collections.unmodifiableMap(annotations != null ? new HashMap<>(annotations) : Collections.emptyMap());
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

    @Override
    public <T> T accept(NodeVisitor<T> visitor) {
        return visitor.visitActionNode(this);
    }

    @Override
    public String toString() {
        return "ActionNode{" +
               "id=" + id +
               ", name='" + name + "\'" +
               ", annotations=" + annotations +
               "}";
    }

    // Consider adding equals() and hashCode()
} 