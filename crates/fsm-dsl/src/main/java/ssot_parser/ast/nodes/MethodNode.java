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

/**
 * Represents a method definition within an interface or service.
 */
public class MethodNode implements AstNode, NodeWithId {

    private final Optional<Long> id;
    private final List<AnnotationNode> annotations;
    private final String name;
    private final List<ParameterNode> parameters;
    private final Optional<String> returnType; // Optional if methods can be void

    public MethodNode(Optional<Long> id, String name, List<ParameterNode> parameters, Optional<String> returnType, List<AnnotationNode> annotations) {
        this.id = id;
        this.name = Objects.requireNonNull(name, "Method name cannot be null");
        this.parameters = parameters != null ? Collections.unmodifiableList(parameters) : Collections.emptyList();
        this.returnType = returnType; // Nullable/Optional
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
               "id=" + id.map(String::valueOf).orElse("none") +
               ", name='" + name + '\'' +
               ", parameters=" + parameters +
               ", returnType=" + returnType.orElse("void") +
               ", annotations=" + annotations +
               '}';
    }

    // Implementation for AstNode
    @Override
    public <T> T accept(NodeVisitor<T> visitor) {
        System.err.println("Warning: NodeVisitor.visitMethodNode not implemented yet.");
        return null;
    }

    // equals/hashCode omitted
} 