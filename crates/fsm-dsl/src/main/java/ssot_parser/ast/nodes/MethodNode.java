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

/**
 * Represents a method definition within an interface or service.
 */
public class MethodNode implements AstNode {

    private final Optional<Long> id;
    private final String name;
    private final List<ParameterNode> parameters;
    private final Optional<String> returnType; // Optional if methods can be void
    private final List<AnnotationNode> annotations;

    public MethodNode(Optional<Long> id, String name, List<ParameterNode> parameters, Optional<String> returnType, List<AnnotationNode> annotations) {
        this.id = id;
        this.name = Objects.requireNonNull(name, "Method name cannot be null");
        this.parameters = Objects.requireNonNull(parameters, "Method parameters cannot be null");
        this.returnType = returnType; // Nullable/Optional
        this.annotations = Collections.unmodifiableList(annotations != null ? new ArrayList<>(annotations) : Collections.emptyList());
    }

    public Optional<Long> getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<ParameterNode> getParameters() {
        return parameters;
    }

    public Optional<String> getReturnType() {
        return returnType;
    }

    @Override
    public List<AnnotationNode> getAnnotations() {
        return annotations;
    }

     @Override
    public String toString() {
        return "MethodNode{" +
               "id=" + id +
               ", name='" + name + '\'' +
               ", parameters=" + parameters +
               ", returnType=" + returnType +
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