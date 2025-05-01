package ssot_parser.ast.nodes;

import ssot_parser.ast.AstNode;
import ssot_parser.ast.NodeVisitor;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Represents a guard definition within the 'guards { ... }' block of a machine.
 * Corresponds to the 'guardDefinition' rule in the grammar (assuming similar structure to actionDefinition).
 */
public class GuardNode implements AstNode {
    // ID annotation is optional for guards
    private final Optional<Long> id;
    private final String name;
    // Store other annotations ($name, $flag) in a map
    private final Map<String, Object> annotations;
    // Guards might have parameters or a specific return type (e.g., boolean) in a full implementation

    // Constructor including annotations
    public GuardNode(Optional<Long> id, String name, Map<String, Object> annotations) {
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
        // return visitor.visitGuardNode(this);
        throw new UnsupportedOperationException("Visitor pattern not yet implemented for GuardNode");
    }

    @Override
    public String toString() {
        return "GuardNode{" +
               "id=" + id +
               ", name='" + name + "\'" +
               ", annotations=" + annotations +
               "}";
    }

    // Consider adding equals() and hashCode()
} 