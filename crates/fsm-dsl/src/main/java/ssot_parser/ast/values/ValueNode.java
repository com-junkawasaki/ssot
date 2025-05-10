package ssot_parser.ast.values;

import ssot_parser.ast.AstNode;
import ssot_parser.ast.NodeVisitor;

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
}

// --- Implementation Classes ---

class IntValueNode implements ValueNode {
    public final int value;

    public IntValueNode(int value) { this.value = value; }

    @Override
    public <T> T accept(NodeVisitor<T> visitor) { return null; /* Placeholder */ }

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
}

class FloatValueNode implements ValueNode {
    public final double value; // Use double for flexibility

    public FloatValueNode(double value) { this.value = value; }

     @Override
    public <T> T accept(NodeVisitor<T> visitor) { return null; /* Placeholder */ }

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
}

/** Represents a reference to a context variable, action, guard etc. */
class RefValueNode implements ValueNode {
    public final String identifier; // e.g., "context.user.id", "Actions.logEvent"

    public RefValueNode(String identifier) { this.identifier = identifier; }

    @Override
    public <T> T accept(NodeVisitor<T> visitor) { return null; /* Placeholder */ }

    @Override
    public String toString() { return identifier; }

    @Override
    public Object getValue() {
        return identifier;
    }

    @Override
    public String getRawValue() {
        return identifier;
    }

    @Override
    public ValueNodeType getType() {
        return ValueNodeType.REFERENCE;
    }
} 