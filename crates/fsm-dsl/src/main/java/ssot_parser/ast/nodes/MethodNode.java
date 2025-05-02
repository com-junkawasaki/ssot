package ssot_parser.ast.nodes;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import ssot_parser.ast.AstNode;
import ssot_parser.ast.NodeVisitor;
import java.util.ArrayList;
import java.util.Collections;
import ssot_parser.ast.nodes.AnnotationNode;
import ssot_parser.NodeWithId;
import ssot_parser.ast.nodes.ParameterNode;
import ssot_parser.ast.type.TypeExprNode;
import java.util.HashMap;

/**
 * Represents a method definition within an interface or service.
 */
public class MethodNode implements AstNode, NodeWithId {

    private final Optional<Long> id;
    private final List<AnnotationNode> annotations;
    public final String name;
    public final List<ParameterNode> parameters;
    public final Optional<TypeExprNode> returnType;

    public MethodNode(Optional<Long> id, String name, List<ParameterNode> parameters, Optional<TypeExprNode> returnType, List<AnnotationNode> annotations) {
        this.id = id;
        this.name = Objects.requireNonNull(name, "Method name cannot be null");
        this.parameters = parameters != null ? Collections.unmodifiableList(new ArrayList<>(parameters)) : Collections.emptyList();
        this.returnType = returnType;
        this.annotations = annotations != null ? Collections.unmodifiableList(new ArrayList<>(annotations)) : Collections.emptyList();
    }

    @Override
    public Optional<Long> getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<ParameterNode> getParameters() {
        return parameters;
    }

    public Optional<TypeExprNode> getReturnType() {
        return returnType;
    }

    @Override
    public Map<String, Object> getAnnotations() {
        Map<String, Object> annotationMap = new HashMap<>();
        for (AnnotationNode annotation : this.annotations) {
            annotationMap.put(annotation.name, annotation.value);
        }
        return Collections.unmodifiableMap(annotationMap);
    }

    @Override
    public String toString() {
        return "MethodNode{" +
               "id=" + id.map(String::valueOf).orElse("none") +
               ", name='" + name + '\'' +
               ", parameters=" + parameters +
               ", returnType=" + returnType.map(TypeExprNode::toString).orElse("void") +
               ", annotations=" + annotations +
               '}';
    }

    // Implementation for AstNode
    @Override
    public <T> T accept(NodeVisitor<T> visitor) {
        return visitor.visitMethodNode(this);
    }

    // equals/hashCode omitted
} 