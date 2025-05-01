package ssot_parser.ast.nodes;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import ssot_parser.ast.AstNode;

/**
 * Represents a method definition within an interface or service.
 */
public class MethodNode implements AstNode {

    private final Optional<Long> id;
    private final String name;
    private final List<ParameterNode> parameters;
    private final Optional<String> returnType; // Optional if methods can be void
    private final Map<String, Object> annotations;

    public MethodNode(Optional<Long> id, String name, List<ParameterNode> parameters, Optional<String> returnType, Map<String, Object> annotations) {
        this.id = id;
        this.name = Objects.requireNonNull(name, "Method name cannot be null");
        this.parameters = Objects.requireNonNull(parameters, "Method parameters cannot be null");
        this.returnType = returnType; // Nullable/Optional
        this.annotations = Objects.requireNonNull(annotations, "Method annotations cannot be null");
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
    public Map<String, Object> getAnnotations() {
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
    // equals/hashCode omitted
} 