package ssot_parser.ast.type;

import ssot_parser.ast.NodeVisitor;
import ssot_parser.ast.nodes.AnnotationNode;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Represents a reference to another type (e.g., a struct or enum) by its name.
 */
public class RefTypeNode implements TypeExprNode {
    private final String qualifiedName;
    // Optional: private TypeDefNode resolvedType; // For a later resolution phase

    public RefTypeNode(String qualifiedName) {
        this.qualifiedName = Objects.requireNonNull(qualifiedName, "Qualified name cannot be null");
    }

    public String getQualifiedName() {
        return qualifiedName;
    }

    @Override
    public <T> T accept(NodeVisitor<T> visitor) {
        return visitor.visitRefTypeNode(this); // Visitor will need this method
    }

    @Override
    public Map<String, Object> getAnnotations() {
        return Collections.emptyMap(); // Type references themselves usually don't have annotations
    }

    @Override
    public Optional<Long> getId() {
        return Optional.empty(); // Type references themselves usually don't have IDs
    }

    @Override
    public String toString() {
        return qualifiedName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RefTypeNode that = (RefTypeNode) o;
        return Objects.equals(qualifiedName, that.qualifiedName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(qualifiedName);
    }
} 