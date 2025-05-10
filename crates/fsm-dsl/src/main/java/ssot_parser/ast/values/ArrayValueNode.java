package ssot_parser.ast.values;

import ssot_parser.ast.NodeVisitor;
import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.Collections;
import java.util.stream.Collectors;

// implements ValueNode (no generics)
public class ArrayValueNode implements ValueNode {
    // Keep the internal list typed with ValueNode
    private final List<ValueNode> values;

    public ArrayValueNode(List<ValueNode> values) {
        this.values = values != null ? Collections.unmodifiableList(values) : Collections.emptyList();
    }

    // Return type is Object, but the object is a List<ValueNode>
    @Override
    public Object getValue() {
        return values;
    }

    @Override
    public String getRawValue() {
        // Use toString() which now relies on ValueNode.toString()
        return values.stream().map(Object::toString).collect(Collectors.joining(", ", "[", "]"));
    }

    // Use ValueNodeType enum
    @Override
    public ValueNodeType getType() {
        return ValueNodeType.ARRAY;
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

    @Override
    public Object getActualValue() {
        return this.values.stream().map(ValueNode::getActualValue).collect(Collectors.toList());
    }
} 