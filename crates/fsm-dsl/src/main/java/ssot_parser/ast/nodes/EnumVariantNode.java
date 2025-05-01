package ssot_parser.ast.nodes;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import ssot_parser.ast.AstNode;
import ssot_parser.ast.NodeVisitor;

/**
 * Represents a single variant within an enum definition.
 */
public class EnumVariantNode implements AstNode { // Variants might not need full AstNode complexity

    private final Optional<Long> id; // If variants can have @id
    private final String name;
    private final Map<String, Object> annotations; // If variants can have annotations

    // Add value if enums have associated values (e.g., enum Color { RED = 1 })

    public EnumVariantNode(Optional<Long> id, String name, Map<String, Object> annotations) {
        this.id = id;
        this.name = Objects.requireNonNull(name, "Enum variant name cannot be null");
        this.annotations = Objects.requireNonNull(annotations, "Enum variant annotations cannot be null");
    }

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
    public String toString() {
        return "EnumVariantNode{" +
               "id=" + id +
               ", name='" + name + '\'' +
               ", annotations=" + annotations +
               '}';
    }

    // equals and hashCode omitted for brevity

    // Implementation for AstNode
    @Override
    public <T> T accept(NodeVisitor<T> visitor) {
        return visitor.visitEnumVariantNode(this);
    }
} 