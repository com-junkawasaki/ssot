package ssot_parser.ast.nodes;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import ssot_parser.ast.AstNode;
import ssot_parser.ast.NodeVisitor;

/**
 * Represents a parameter in a method definition.
 */
public class ParameterNode implements AstNode {

    private final String name;
    private final String type;
    private final Map<String, Object> annotations;
    private final Optional<Long> id; // If parameters can have @id

    public ParameterNode(Optional<Long> id, String name, String type, Map<String, Object> annotations) {
        this.id = id;
        this.name = Objects.requireNonNull(name, "Parameter name cannot be null");
        this.type = Objects.requireNonNull(type, "Parameter type cannot be null");
        this.annotations = Objects.requireNonNull(annotations, "Parameter annotations cannot be null");
    }

    public Optional<Long> getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    @Override
    public Map<String, Object> getAnnotations() {
        return annotations;
    }

    @Override
    public String toString() {
        return "ParameterNode{" +
               "id=" + id +
               ", name='" + name + '\'' +
               ", type='" + type + '\'' +
               ", annotations=" + annotations +
               '}';
    }

    // Implementation for AstNode
    @Override
    public <T> T accept(NodeVisitor<T> visitor) {
        return visitor.visitParameterNode(this);
    }

    // equals/hashCode omitted
} 