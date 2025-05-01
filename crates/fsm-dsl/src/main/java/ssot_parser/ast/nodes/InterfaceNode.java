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
 * Represents an interface definition.
 */
public class InterfaceNode implements AstNode {

    private final Optional<Long> id;
    private final String name;
    private final List<MethodNode> methods;
    private final List<AnnotationNode> annotations;

    public InterfaceNode(Optional<Long> id, String name, List<MethodNode> methods, List<AnnotationNode> annotations) {
        this.id = id;
        this.name = Objects.requireNonNull(name, "Interface name cannot be null");
        this.methods = Objects.requireNonNull(methods, "Interface methods cannot be null");
        this.annotations = Collections.unmodifiableList(annotations != null ? new ArrayList<>(annotations) : Collections.emptyList());
    }

    public Optional<Long> getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<MethodNode> getMethods() {
        return methods;
    }

    @Override
    public List<AnnotationNode> getAnnotations() {
        return annotations;
    }

    @Override
    public String toString() {
        return "InterfaceNode{" +
               "id=" + id +
               ", name='" + name + '\'' +
               ", methods=" + methods +
               ", annotations=" + annotations +
               '}';
    }

    // Implementation for AstNode
    @Override
    public <T> T accept(NodeVisitor<T> visitor) {
        return visitor.visitInterfaceNode(this);
    }

    // equals/hashCode omitted
} 