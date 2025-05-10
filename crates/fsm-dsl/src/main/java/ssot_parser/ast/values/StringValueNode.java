package ssot_parser.ast.values;

import ssot_parser.ast.NodeVisitor;
import java.util.Optional;
import java.util.Map;
import java.util.Collections;

// implements ValueNode (no generics)
public class StringValueNode implements ValueNode {
    private final String value;

    public StringValueNode(String value) {
        this.value = value;
    }

    // Return type is Object
    @Override
    public Object getValue() {
        return value;
    }

    @Override
    public String getRawValue() {
        return value;
    }

    // Use ValueNodeType enum
    @Override
    public ValueNodeType getType() {
        return ValueNodeType.STRING;
    }

    @Override
    public <T> T accept(NodeVisitor<T> visitor) {
        // Typically ValueNodes are not visited directly by the main AST visitor
        // but this could be used by a more specific expression evaluator visitor.
        // return visitor.visitStringValueNode(this); // If NodeVisitor has such a method
        return null;
    }

    @Override
    public String toString() {
        // Keep quotes for string representation if desired
        return "\"" + value + "\"";
    }

    @Override
    public Optional<Long> getId() {
        return Optional.empty(); // Value nodes typically don't have IDs
    }

    @Override
    public Map<String, Object> getAnnotations() {
        return Collections.emptyMap(); // Value nodes typically don't have annotations
    }

    @Override
    public Object getActualValue() {
        return this.value;
    }
} 