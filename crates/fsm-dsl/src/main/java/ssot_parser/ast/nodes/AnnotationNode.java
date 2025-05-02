package ssot_parser.ast.nodes;

import java.util.Map;
import java.util.Objects;
import ssot_parser.ast.AstNode;
import ssot_parser.ast.NodeVisitor;

/**
 * Represents a single annotation in the AST (e.g., $name(value) or @id(value)).
 */
public class AnnotationNode implements AstNode {

    public final String name; // e.g., "id" for @id or "myAnnotation" for $myAnnotation
    public final Object value; // The value associated with the annotation (can be Boolean, String, Number, List, Map)
    private final boolean isIdAnnotation; // Flag to distinguish @id

    public AnnotationNode(String name, Object value, boolean isIdAnnotation) {
        this.name = Objects.requireNonNull(name, "Annotation name cannot be null");
        this.value = value; // Value can be null for flags like $flag; represented as true
        this.isIdAnnotation = isIdAnnotation;
    }

    public boolean isIdAnnotation() {
        return isIdAnnotation;
    }

    // Since annotations don't have their own sub-annotations in this model,
    // getAnnotations returns an empty map. File/Machine/State nodes hold their annotations.
    @Override
    public Map<String, Object> getAnnotations() {
        return Map.of(); // An annotation itself doesn't have annotations
    }

    @Override
    public String toString() {
        String prefix = isIdAnnotation ? "@" : "$";
        if (value instanceof Boolean && (Boolean)value) {
            // Handle boolean flags ($flag;)
            return prefix + name;
        } else if (value != null) {
            // Handle annotations with values ($description("text"), @id(123))
            String valueStr = (value instanceof String) ? "\"" + value + "\"" : value.toString();
            return prefix + name + "(" + valueStr + ")";
        } else {
            // Should not happen frequently, maybe $name() without value?
            return prefix + name + "(null)";
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AnnotationNode that = (AnnotationNode) o;
        return isIdAnnotation == that.isIdAnnotation &&
               name.equals(that.name) &&
               Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, value, isIdAnnotation);
    }

    // Implementation for AstNode
    @Override
    public <T> T accept(NodeVisitor<T> visitor) {
        System.err.println("Warning: NodeVisitor.visitAnnotationNode called, but might not be intended.");
        return null;
    }
} 