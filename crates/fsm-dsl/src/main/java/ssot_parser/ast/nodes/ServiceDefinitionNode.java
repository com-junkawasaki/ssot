package ssot_parser.ast.nodes;

import ssot_parser.ast.AstNode;
import ssot_parser.NodeWithId;
import ssot_parser.ast.NodeVisitor;
import ssot_parser.ast.type.CustomType;
import java.util.Map;
import java.util.Optional;
import java.util.List;
import java.util.Collections;
import java.util.Objects;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Represents a service definition.
 */
public class ServiceDefinitionNode implements AstNode, NodeWithId {
    private final Optional<Long> id;
    private final String name;
    private final List<CustomType> implementedInterfaces; // Assuming interfaces are referred by CustomType
    private final List<MethodDefinitionNode> methods;
    private final List<AnnotationNode> annotations;
    private final Map<String, Object> namedAnnotationsMap;

    public ServiceDefinitionNode(Optional<Long> id,
                                 String name,
                                 List<AnnotationNode> annotations,
                                 List<CustomType> implementedInterfaces,
                                 List<MethodDefinitionNode> methods) {
        this.id = id != null ? id : Optional.empty();
        this.name = Objects.requireNonNull(name, "Service name cannot be null");
        this.annotations = annotations != null ? Collections.unmodifiableList(annotations) : Collections.emptyList();
        this.implementedInterfaces = implementedInterfaces != null ? Collections.unmodifiableList(implementedInterfaces) : Collections.emptyList();
        this.methods = methods != null ? Collections.unmodifiableList(methods) : Collections.emptyList();
        this.namedAnnotationsMap = mapAnnotations(this.annotations);
    }

    public String getName() {
        return name;
    }

    public List<CustomType> getImplementedInterfaces() {
        return implementedInterfaces;
    }

    public List<MethodDefinitionNode> getMethods() {
        return methods;
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
        return visitor.visitServiceDefinitionNode(this);
    }

    private Map<String, Object> mapAnnotations(List<AnnotationNode> annotationNodes) {
        Map<String, Object> map = new HashMap<>();
        if (annotationNodes != null) {
            for (AnnotationNode annotation : annotationNodes) {
                map.put(annotation.getName(), annotation.getValue().orElse(Boolean.TRUE));
            }
        }
        return Collections.unmodifiableMap(map);
    }

    @Override
    public String toString() {
        return "ServiceDefinitionNode{" +
               "id=" + id.map(String::valueOf).orElse("none") +
               ", name='" + name + "'" +
               ", implements=" + implementedInterfaces +
               ", methods=" + methods.size() +
               ", annotations=" + annotations +
               "}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ServiceDefinitionNode that = (ServiceDefinitionNode) o;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
} 