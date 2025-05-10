package ssot_parser.ast.values;

import ssot_parser.ast.NodeVisitor;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class RefValueNode implements ValueNode {
    private final String qualifiedName;

    public RefValueNode(String qualifiedName) {
        this.qualifiedName = Objects.requireNonNull(qualifiedName, "Qualified name cannot be null");
    }

    public String getQualifiedName() {
        return qualifiedName;
    }

    @Override
    public Object getValue() { // As per current ValueNode interface
        return qualifiedName;
    }

    @Override
    public String getRawValue() { 
        return qualifiedName;
    }

    @Override
    public ValueNodeType getType() {
        return ValueNodeType.REFERENCE; // Assuming REFERENCE is a type in ValueNodeType
    }

    @Override
    public <T> T accept(NodeVisitor<T> visitor) {
        // return visitor.visitRefValueNode(this);
        return null;
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
    public String toString() {
        return qualifiedName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RefValueNode that = (RefValueNode) o;
        return Objects.equals(qualifiedName, that.qualifiedName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(qualifiedName);
    }

    @Override
    public Object getActualValue() {
        return this.qualifiedName;
    }
} 