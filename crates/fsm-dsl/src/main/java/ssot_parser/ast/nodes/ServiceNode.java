package ssot_parser.ast.nodes;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import ssot_parser.ast.AstNode;

/**
 * Represents a service definition.
 * A service might contain methods directly or implement interfaces.
 * Adjust based on actual grammar.
 */
public class ServiceNode implements AstNode { // Renamed from ServiceDefinitionNode for consistency

    private final Optional<Long> id;
    private final String name;
    private final List<MethodNode> methods; // Direct methods
    private final List<String> implementedInterfaces; // Names of implemented interfaces
    private final Map<String, Object> annotations;

    public ServiceNode(Optional<Long> id, String name, List<MethodNode> methods, List<String> implementedInterfaces, Map<String, Object> annotations) {
        this.id = id;
        this.name = Objects.requireNonNull(name, "Service name cannot be null");
        this.methods = Objects.requireNonNull(methods, "Service methods cannot be null");
        this.implementedInterfaces = Objects.requireNonNull(implementedInterfaces, "Implemented interfaces list cannot be null");
        this.annotations = Objects.requireNonNull(annotations, "Service annotations cannot be null");
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

    public List<String> getImplementedInterfaces() {
        return implementedInterfaces;
    }

    @Override
    public Map<String, Object> getAnnotations() {
        return annotations;
    }

    @Override
    public String toString() {
        return "ServiceNode{" +
               "id=" + id +
               ", name='" + name + '\'' +
               ", methods=" + methods +
               ", implementedInterfaces=" + implementedInterfaces +
               ", annotations=" + annotations +
               '}';
    }
     // equals/hashCode omitted
} 