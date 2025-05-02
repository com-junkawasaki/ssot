package ssot_parser.ast.nodes;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import ssot_parser.ast.AstNode;
import ssot_parser.ast.NodeVisitor;
import java.util.ArrayList;
import java.util.Collections;
import ssot_parser.NodeWithId;
import ssot_parser.ast.nodes.AnnotationNode;
import ssot_parser.ast.nodes.MethodNode;
import java.util.HashMap;

/**
 * Represents a service definition.
 * A service might contain methods directly or implement interfaces.
 * Adjust based on actual grammar.
 */
public class ServiceNode implements AstNode, NodeWithId {

    private final Optional<Long> id;
    private final List<AnnotationNode> annotations;
    public final String name;
    public final List<MethodNode> methods;
    public final List<String> implementedInterfaces;

    public ServiceNode(Optional<Long> id, String name, List<MethodNode> methods,
                       List<String> implementedInterfaces, List<AnnotationNode> annotations) {
        this.id = id;
        this.name = Objects.requireNonNull(name, "Service name cannot be null");
        this.methods = methods != null ? Collections.unmodifiableList(new ArrayList<>(methods)) : Collections.emptyList();
        this.implementedInterfaces = implementedInterfaces != null ? Collections.unmodifiableList(new ArrayList<>(implementedInterfaces)) : Collections.emptyList();
        this.annotations = annotations != null ? Collections.unmodifiableList(new ArrayList<>(annotations)) : Collections.emptyList();
    }

    @Override
    public Optional<Long> getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<MethodNode> getMethods() {
        return methods;
    }

    public List<String> getImplementedInterfaces() {
        return implementedInterfaces;
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
        return "ServiceNode{" +
               "id=" + id.map(String::valueOf).orElse("none") +
               ", name='" + name + '\'' +
               ", implements=" + implementedInterfaces +
               ", methods=" + methods +
               ", annotations=" + annotations +
               '}';
    }

    // Implementation for AstNode
    @Override
    public <T> T accept(NodeVisitor<T> visitor) {
        return visitor.visitServiceNode(this);
    }

    // equals/hashCode omitted
}