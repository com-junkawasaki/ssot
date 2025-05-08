package ssot_parser.ast.values;

import ssot_parser.ast.NodeVisitor;
import java.util.Map;
import java.util.Optional;
import java.util.Collections;
import java.util.stream.Collectors;

// implements ValueNode (no generics)
public class ObjectValueNode implements ValueNode {
    // Keep the internal map typed with ValueNode
    private final Map<String, ValueNode> properties;

    public ObjectValueNode(Map<String, ValueNode> properties) {
        this.properties = properties != null ? Collections.unmodifiableMap(properties) : Collections.emptyMap();
    }

    // Return type is Object, but the object is a Map<String, ValueNode>
    @Override
    public Object getValue() {
        return properties;
    }

    @Override
    public String getRawValue() {
        // Use toString() which now relies on ValueNode.toString()
        return properties.entrySet().stream()
                         .map(entry -> "\"" + entry.getKey() + "\": " + entry.getValue().toString())
                         .collect(Collectors.joining(", ", "{", "}"));
    }

    // Use ValueNodeType enum
    @Override
    public ValueNodeType getType() {
        return ValueNodeType.OBJECT;
    }

    @Override
    public <T> T accept(NodeVisitor<T> visitor) {
        return null;
    }

    @Override
    public String toString() {
        return getRawValue();
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