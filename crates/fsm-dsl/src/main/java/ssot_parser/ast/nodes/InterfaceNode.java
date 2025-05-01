package ssot_parser.ast.nodes;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import ssot_parser.ast.AstNode;
import ssot_parser.ast.NodeVisitor;

/**
 * Represents an interface definition.
 */
public class InterfaceNode implements AstNode {

    private final Optional<Long> id;
    private final String name;
    private final List<MethodNode> methods;
    private final Map<String, Object> annotations;

    public InterfaceNode(Optional<Long> id, String name, List<MethodNode> methods, Map<String, Object> annotations) {
        this.id = id;
        this.name = Objects.requireNonNull(name, "Interface name cannot be null");
        this.methods = Objects.requireNonNull(methods, "Interface methods cannot be null");
        this.annotations = Objects.requireNonNull(annotations, "Interface annotations cannot be null");
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
    public Map<String, Object> getAnnotations() {
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