package ssot_parser.ast.values;

import ssot_parser.ast.AstNode;
import ssot_parser.ast.NodeVisitor;
import java.util.Optional;
import java.util.Map;
import java.util.Collections;

// Ensure this class is public
public class StringValueNode implements ValueNode<String> {
    private final String value;

    public StringValueNode(String value) {
        this.value = value;
    }

    @Override
    public String getValue() {
        return value;
    }

    @Override
    public String getRawValue() {
        return value;
    }

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
} 