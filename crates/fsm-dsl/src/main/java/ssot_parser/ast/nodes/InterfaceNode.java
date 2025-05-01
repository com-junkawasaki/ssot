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
import ssot_parser.ast.nodes.MethodNode;

/**
 * Represents an interface definition.
 */
public class InterfaceNode implements AstNode, NodeWithId {

    private final Optional<Long> id;
    private final List<AnnotationNode> annotations;
    private final String name;
    private final List<MethodNode> methods;

    public InterfaceNode(Optional<Long> id, String name, List<MethodNode> methods, List<AnnotationNode> annotations) {
        this.id = id;
        this.name = Objects.requireNonNull(name, "Interface name cannot be null");
        this.methods = methods != null ? Collections.unmodifiableList(methods) : Collections.emptyList();
        this.annotations = annotations != null ? Collections.unmodifiableList(new ArrayList<>(annotations)) : Collections.emptyList();
    }

    @Override
    public Optional<Long> getId() {
        return id;
    }

    @Override
    public List<AnnotationNode> getAnnotations() {
        return annotations;
    }

    public String getName() {
        return name;
    }

    public List<MethodNode> getMethods() {
        return methods;
    }

    @Override
    public String toString() {
        return "InterfaceNode{" +
               "id=" + id.map(String::valueOf).orElse("none") +
               ", name='" + name + '\'' +
               ", methods=" + methods +
               ", annotations=" + annotations +
               '}';
    }

    // Implementation for AstNode
    @Override
    public <T> T accept(NodeVisitor<T> visitor) {
        System.err.println("Warning: NodeVisitor.visitInterfaceNode not implemented yet.");
        return null;
    }

    // equals/hashCode omitted
} 