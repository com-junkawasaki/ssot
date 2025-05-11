package ssot_parser.ast.nodes;

import ssot_parser.ast.AstNode;
import ssot_parser.NodeWithId;
import ssot_parser.ast.NodeVisitor;
import ssot_parser.ast.type.TypeExprNode;
import ssot_parser.ast.values.ValueNode;
import java.util.Map;
import java.util.Optional;
import java.util.List;
import java.util.Collections;
import java.util.Objects;
import java.util.HashMap;

/**
 * Represents a variable definition within a context block.
 */
public class ContextVariableNode implements AstNode, NodeWithId {
    private final Optional<Long> id;
    private final String variableName;
    private final TypeExprNode type;
    private final Optional<ValueNode> defaultValue;
    private final List<AnnotationNode> annotations;
    private final Map<String, Object> namedAnnotationsMap;

    public ContextVariableNode(Optional<Long> id,
                               String variableName,
                               TypeExprNode type,
                               List<AnnotationNode> annotations,
                               Optional<ValueNode> defaultValue) {
        this.id = id != null ? id : Optional.empty();
        this.variableName = Objects.requireNonNull(variableName, "Variable name cannot be null");
        this.type = Objects.requireNonNull(type, "Variable type cannot be null");
        this.annotations = annotations != null ? Collections.unmodifiableList(annotations) : Collections.emptyList();
        this.defaultValue = defaultValue != null ? defaultValue : Optional.empty();
        this.namedAnnotationsMap = mapAnnotations(this.annotations);
    }

    public String getVariableName() {
        return variableName;
    }

    public TypeExprNode getType() {
        return type;
    }

    public Optional<ValueNode> getDefaultValue() {
        return defaultValue;
    }

    @Override
    public Optional<Long> getId() {
        return id;
    }

    @Override
    public Map<String, Object> getAnnotations() {
        return namedAnnotationsMap;
    }

    @Override
    public <T> T accept(NodeVisitor<T> visitor) {
        return visitor.visitContextVariableNode(this);
    }

    private Map<String, Object> mapAnnotations(List<AnnotationNode> annotationNodes) {
        Map<String, Object> map = new HashMap<>();
        if (annotationNodes != null) {
            for (AnnotationNode annotation : annotationNodes) {
                map.put(annotation.getName(), annotation.getValue());
            }
        }
        return Collections.unmodifiableMap(map);
    }

    @Override
    public String toString() {
        return "ContextVariableNode{" +
               "id=" + id.map(String::valueOf).orElse("none") +
               ", variableName='" + variableName + "'" +
               ", type=" + type +
               ", defaultValue=" + defaultValue.map(Object::toString).orElse("none") +
               ", annotations=" + annotations +
               "}";
    }

    // equals and hashCode based on name?
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ContextVariableNode that = (ContextVariableNode) o;
        return Objects.equals(variableName, that.variableName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(variableName);
    }
} 