package ssot_parser.ast.nodes;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import ssot_parser.ast.AstNode;
import ssot_parser.ast.NodeVisitor;

/**
 * Represents an enum type definition in the AST.
 */
public class EnumNode implements AstNode { // Could potentially extend a common TypeDefNode base class

    private final Optional<Long> id;
    private final String name;
    private final List<EnumVariantNode> variants;
    private final Map<String, Object> annotations;

    public EnumNode(Optional<Long> id, String name, List<EnumVariantNode> variants, Map<String, Object> annotations) {
        this.id = id;
        this.name = Objects.requireNonNull(name, "Enum name cannot be null");
        this.variants = Objects.requireNonNull(variants, "Enum variants cannot be null");
        this.annotations = Objects.requireNonNull(annotations, "Enum annotations cannot be null");
    }

    public Optional<Long> getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<EnumVariantNode> getVariants() {
        return variants;
    }

    @Override
    public Map<String, Object> getAnnotations() {
        return annotations;
    }

    @Override
    public String toString() {
        return "EnumNode{" +
               "id=" + id +
               ", name='" + name + '\'' +
               ", variants=" + variants +
               ", annotations=" + annotations +
               '}';
    }

    // equals and hashCode omitted for brevity

    // Implementation for AstNode
    @Override
    public <T> T accept(NodeVisitor<T> visitor) {
        return visitor.visitEnumNode(this);
    }
} 