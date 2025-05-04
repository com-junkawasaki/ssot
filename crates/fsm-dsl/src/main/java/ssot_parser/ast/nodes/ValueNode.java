package ssot_parser.ast.nodes;

import ssot_parser.ast.AstNode;
import ssot_parser.ast.NodeVisitor;
import java.util.Map;
import java.util.Optional;
import java.util.Collections;

/**
 * A generic wrapper for literal values (String, Long, Boolean, List, Map) encountered
 * during parsing, primarily used within annotations or default value specifications.
 */
public class ValueNode<V> implements AstNode {
    public final V value;

    public ValueNode(V value) {
        this.value = value;
    }

    public V getValue() {
        return value;
    }

    /**
     * Provides the raw string representation of the value.
     * Useful for cases like invoke src where the exact text is needed before resolution.
     */
    public String getRawValue() {
        return value != null ? value.toString() : null;
    }

    @Override
    public <T> T accept(NodeVisitor<T> visitor) {
        // Values are typically not visited directly in the main traversal,
        // but handled when visiting the node containing them (e.g., AnnotationNode).
        // If direct visiting is needed, add a visitValueNode method to NodeVisitor.
        return null;
    }

    @Override
    public String toString() {
        String type = value != null ? value.getClass().getSimpleName() : "null";
        return "Value<" + type + ">(" + value + ")";
    }

    // Value nodes themselves don't typically have IDs or annotations.
    @Override public Map<String, Object> getAnnotations() { return Collections.emptyMap(); }
    @Override public Optional<Long> getId() { return Optional.empty(); }

    // Consider adding equals() and hashCode()
} 