package ssot_parser.ast.values;

import ssot_parser.ast.NodeVisitor;
import java.util.Optional;
import java.util.Map;
import java.util.Collections;

// implements ValueNode (no generics)
public class NullValueNode implements ValueNode {
    public NullValueNode() {}

    // Return type is Object
    @Override
    public Object getValue() {
        return null;
    }

    @Override
    public String getRawValue() {
        return "null";
    }

    // Use ValueNodeType enum
    @Override
    public ValueNodeType getType() {
        return ValueNodeType.NULL;
    }

    @Override
    public <T> T accept(NodeVisitor<T> visitor) {
        return null;
    }

    @Override
    public String toString() {
        return "null";
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