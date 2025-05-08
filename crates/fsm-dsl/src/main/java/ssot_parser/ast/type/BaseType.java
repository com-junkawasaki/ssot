package ssot_parser.ast.type;

import ssot_parser.ast.NodeVisitor;
import java.util.Map;
import java.util.Optional;
import java.util.Collections;
import java.util.Objects;
import java.util.HashMap;

/**
 * Represents a base (primitive) type like string, u32, boolean.
 */
public class BaseType implements TypeExprNode {
    private final Optional<Long> id; // ID, if annotated
    private final String typeName; // e.g., "string", "u32", "boolean"
    private final Map<String, Object> annotations;

    public BaseType(String typeName, Optional<Long> id, Map<String, Object> annotations) {
        this.typeName = Objects.requireNonNull(typeName, "Type name cannot be null");
        this.id = id != null ? id : Optional.empty();
        this.annotations = annotations != null ? Collections.unmodifiableMap(new HashMap<>(annotations)) : Collections.emptyMap();
    }

    public String getTypeName() {
        return typeName;
    }

    @Override
    public Optional<Long> getId() {
        return id;
    }

    @Override
    public Map<String, Object> getAnnotations() {
        return annotations;
    }

    @Override
    public <T> T accept(NodeVisitor<T> visitor) {
        // Visitor might handle base types specifically or generally via TypeExprNode
        // return visitor.visitBaseType(this); // If visitor has this method
        return null; // Or throw unsupported operation
    }

    @Override
    public String toString() {
        return "BaseType{" +
               "typeName='" + typeName + "'" +
               ", id=" + id.map(String::valueOf).orElse("none") +
               ", annotations=" + annotations +
               "}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BaseType baseType = (BaseType) o;
        return Objects.equals(typeName, baseType.typeName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(typeName);
    }
} 