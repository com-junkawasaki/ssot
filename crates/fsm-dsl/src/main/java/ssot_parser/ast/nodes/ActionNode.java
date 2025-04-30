package ssot_parser.ast.nodes;

import ssot_parser.ast.AstNode;
import ssot_parser.ast.NodeVisitor;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Represents an action definition within the 'actions { ... }' block of a machine.
 * Corresponds to the 'actionDefinition' rule in the grammar.
 */
public class ActionNode implements AstNode {
    // ID annotation is optional for actions
    private final Optional<Long> id;
    private final String name;
    // Store other annotations ($name, $flag) in a map
    private final Map<String, Object> annotations;

    // Constructor including annotations
    public ActionNode(Optional<Long> id, String name, Map<String, Object> annotations) {
        this.id = id;
        this.name = name;
        this.annotations = annotations != null ? Collections.unmodifiableMap(annotations) : Collections.emptyMap();
    }

    public Optional<Long> getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Map<String, Object> getAnnotations() {
        return annotations;
    }

    @Override
    public <T> T accept(NodeVisitor<T> visitor) {
        // Assuming a visitor pattern
        // return visitor.visitActionNode(this);
        throw new UnsupportedOperationException("Visitor pattern not yet implemented for ActionNode");
    }

    @Override
    public String toString() {
        return "ActionNode{" +
               "id=" + id +
               ", name='" + name + ''' +
               ", annotations=" + annotations +
               '}';
    }

    // Consider adding equals() and hashCode()
} 