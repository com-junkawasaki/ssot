package ssot_parser.ast.values;

import ssot_parser.ast.AstNode;
import ssot_parser.ast.NodeVisitor;
import java.util.Optional;
import java.util.Map;

/**
 * Base interface for nodes representing literal values in the AST.
 * Implementations hold specific types (String, Number, Boolean, etc.).
 */
public interface ValueNode extends AstNode {
    /**
     * Gets the underlying Java value.
     * The type depends on the specific implementation (e.g., String, Long, Double, Boolean, List<ValueNode>, Map<String, ValueNode>).
     */
    Object getValue();

    /**
     * Gets a raw string representation of the value, useful for simple cases.
     */
    String getRawValue();

    /**
     * Gets the type of the value represented by this node.
     */
    ValueNodeType getType();

    Optional<Long> getId();

    Map<String, Object> getAnnotations();

    Object getActualValue();
}

// --- Implementation Classes ---

class IntValueNode implements ValueNode {
    public final int value;
    private final Optional<Long> id = Optional.empty(); // No specific ID for a literal int
    private final Map<String, Object> annotations = java.util.Collections.emptyMap(); // No annotations for a literal int

    public IntValueNode(int value) { this.value = value; }

    @Override
    public Optional<Long> getId() {
        return id;
    }

    @Override
    public Map<String, Object> getAnnotations() {
        return annotations;
    }

    @Override
    public <T> T accept(NodeVisitor<T> visitor) { return visitor.visitValueNode(this); /* Placeholder for specific visitor method if any */ }

    @Override
    public String toString() { return String.valueOf(value); }

    @Override
    public Object getValue() {
        return value;
    }

    @Override
    public String getRawValue() {
        return String.valueOf(value);
    }

    @Override
    public ValueNodeType getType() {
        return ValueNodeType.INTEGER;
    }

    @Override
    public Object getActualValue() {
        return value;
    }
}

class FloatValueNode implements ValueNode {
    public final double value; // Use double for flexibility
    private final Optional<Long> id = Optional.empty(); // No specific ID for a literal float
    private final Map<String, Object> annotations = java.util.Collections.emptyMap(); // No annotations for a literal float

    public FloatValueNode(double value) { this.value = value; }

    @Override
    public Optional<Long> getId() {
        return id;
    }

    @Override
    public Map<String, Object> getAnnotations() {
        return annotations;
    }

    @Override
    public <T> T accept(NodeVisitor<T> visitor) { return visitor.visitValueNode(this); /* Placeholder for specific visitor method if any */ }

    @Override
    public String toString() { return String.valueOf(value); }

    @Override
    public Object getValue() {
        return value;
    }

    @Override
    public String getRawValue() {
        return String.valueOf(value);
    }

    @Override
    public ValueNodeType getType() {
        return ValueNodeType.FLOAT;
    }

    @Override
    public Object getActualValue() {
        return value;
    }
}