package ssot_parser.ast.values;

import ssot_parser.ast.NodeVisitor;
import java.util.Optional;
import java.util.Map;
import java.util.Collections;

// implements ValueNode (no generics)
public class NumberValueNode implements ValueNode {
    private final Number value;

    public NumberValueNode(Number value) {
        this.value = value;
    }

    // Return type is Object
    @Override
    public Object getValue() {
        return value;
    }

    @Override
    public String getRawValue() {
        return value.toString();
    }

    // Use ValueNodeType enum
    @Override
    public ValueNodeType getType() {
        return ValueNodeType.NUMBER;
    }

    @Override
    public <T> T accept(NodeVisitor<T> visitor) {
        return null;
    }

    @Override
    public String toString() {
        return value.toString();
    }

    @Override
    public Optional<Long> getId() {
        return Optional.empty();
    }

    @Override
    public Map<String, Object> getAnnotations() {
        return Collections.emptyMap();
    }
} 