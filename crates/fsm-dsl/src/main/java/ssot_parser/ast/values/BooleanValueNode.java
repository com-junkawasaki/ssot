package ssot_parser.ast.values;

import ssot_parser.ast.NodeVisitor;
import java.util.Optional;
import java.util.Map;
import java.util.Collections;

// Ensure this class is public
public class BooleanValueNode implements ValueNode<Boolean> {
    private final Boolean value;

    public BooleanValueNode(Boolean value) {
        this.value = value;
    }

    @Override
    public Boolean getValue() {
        return value;
    }

    @Override
    public String getRawValue() {
        return value.toString();
    }

    @Override
    public ValueNodeType getType() {
        return ValueNodeType.BOOLEAN;
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