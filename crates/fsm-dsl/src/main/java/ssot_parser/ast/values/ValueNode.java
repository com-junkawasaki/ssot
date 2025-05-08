package ssot_parser.ast.values;

import ssot_parser.ast.AstNode;
import ssot_parser.ast.NodeVisitor;

/**
 * Represents a value used in various parts of the DSL, like context assignments,
 * action parameters, invoke input mapping, etc.
 */
public interface ValueNode extends AstNode {
    // Common interface for all value types
}

// --- Implementation Classes ---

class StringValueNode implements ValueNode {
    public final String value;

    public StringValueNode(String value) {
        this.value = value;
    }

    @Override
    public <T> T accept(NodeVisitor<T> visitor) {
        // Assuming visitor has visitStringValueNode method
        // return visitor.visitStringValueNode(this);
        return null; // Placeholder
    }

    @Override
    public String toString() { return "\"" + value + "\""; }
}

class IntValueNode implements ValueNode {
    public final int value;

    public IntValueNode(int value) { this.value = value; }

    @Override
    public <T> T accept(NodeVisitor<T> visitor) { return null; /* Placeholder */ }

    @Override
    public String toString() { return String.valueOf(value); }
}

class FloatValueNode implements ValueNode {
    public final double value; // Use double for flexibility

    public FloatValueNode(double value) { this.value = value; }

     @Override
    public <T> T accept(NodeVisitor<T> visitor) { return null; /* Placeholder */ }

    @Override
    public String toString() { return String.valueOf(value); }
}

class BooleanValueNode implements ValueNode {
    public final boolean value;

    public BooleanValueNode(boolean value) { this.value = value; }

     @Override
    public <T> T accept(NodeVisitor<T> visitor) { return null; /* Placeholder */ }

    @Override
    public String toString() { return String.valueOf(value); }
}

/** Represents a reference to a context variable, action, guard etc. */
class RefValueNode implements ValueNode {
    public final String identifier; // e.g., "context.user.id", "Actions.logEvent"

    public RefValueNode(String identifier) { this.identifier = identifier; }

    @Override
    public <T> T accept(NodeVisitor<T> visitor) { return null; /* Placeholder */ }

    @Override
    public String toString() { return identifier; }
} 