package ssot_parser.ast.nodes;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import ssot_parser.ast.AstNode;
import ssot_parser.ast.NodeVisitor;
import ssot_parser.NodeWithId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import ssot_parser.ast.nodes.AnnotationNode;
import ssot_parser.ast.type.TypeExprNode;
import java.util.HashMap;

/**
 * Represents a parameter definition within a method signature.
 * Corresponds to the 'parameter' rule (or similar) in the grammar.
 */
public class ParameterNode implements AstNode, NodeWithId {

    private final Optional<Long> id;
    private final List<AnnotationNode> annotations;
    public final String name;
    public final TypeExprNode type;

    public ParameterNode(Optional<Long> id, List<AnnotationNode> annotations, String name, TypeExprNode type) {
        this.id = id;
        this.annotations = Collections.unmodifiableList(annotations != null ? new ArrayList<>(annotations) : Collections.emptyList());
        this.name = Objects.requireNonNull(name, "Parameter name cannot be null");
        this.type = Objects.requireNonNull(type, "Parameter type cannot be null");
    }

    @Override
    public Optional<Long> getId() {
        return id;
    }

    @Override
    public Map<String, Object> getAnnotations() {
        Map<String, Object> annotationMap = new HashMap<>();
        for (AnnotationNode annotation : this.annotations) {
            annotationMap.put(annotation.name, annotation.value);
        }
        return Collections.unmodifiableMap(annotationMap);
    }

    public String getName() {
        return name;
    }

    public TypeExprNode getType() {
        return type;
    }

    @Override
    public String toString() {
        return "ParameterNode{" +
               "id=" + id.map(String::valueOf).orElse("none") +
               ", name='" + name + '\'' +
               ", type=" + type.toString() +
               ", annotations=" + annotations +
               '}';
    }

    // Implementation for AstNode
    @Override
    public <T> T accept(NodeVisitor<T> visitor) {
        return visitor.visitParameterNode(this);
    }

    // equals/hashCode omitted
} 