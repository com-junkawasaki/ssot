package ssot_parser.ast.nodes;

import ssot_parser.ast.AstNode;
import ssot_parser.NodeWithId;
import ssot_parser.ast.NodeVisitor;
import ssot_parser.ast.type.TypeNode;
import ssot_parser.ast.type.BaseType;
import java.util.Map;
import java.util.Optional;
import java.util.List;
import java.util.Collections;
import java.util.HashMap;
import java.util.ArrayList;

/**
 * Represents a guard definition within the 'guards { ... }' block of a machine.
 * Corresponds to the 'guardDefinition' rule.
 */
public class GuardDefinitionNode implements AstNode, NodeWithId {
    private final Optional<Long> id;
    public final String guardName;
    // For now, parameters are just illustrative names in the DSL (ctx)
    // A more complex implementation might parse formal parameters.
    public final TypeNode returnType; // DSL specifies "-> bool"
    private final List<AnnotationNode> annotations; // Although not shown in DSL example.

    public GuardDefinitionNode(Optional<Long> id, String guardName, TypeNode returnType, List<AnnotationNode> annotations) {
        this.id = id;
        this.guardName = guardName;
        // Basic validation: Ensure the specified return type is boolean
        if (returnType == null || returnType.getBaseType() != BaseType.BOOLEAN) {
            // In a real implementation, this should throw a specific semantic validation error
            // during the validation phase, not necessarily at construction.
            // For now, we can assign a default boolean type or log a warning.
            System.err.println("Warning: Guard '" + guardName + "' must return bool. Defaulting return type.");
            this.returnType = new TypeNode(BaseType.BOOLEAN);
        } else {
            this.returnType = returnType;
        }
        this.annotations = Collections.unmodifiableList(annotations != null ? new ArrayList<>(annotations) : Collections.emptyList());
    }

    @Override
    public Optional<Long> getId() {
        return id;
    }

    public String getGuardName() {
        return guardName;
    }

    public TypeNode getReturnType() {
        return returnType;
    }

    @Override
    public Map<String, Object> getAnnotations() {
        Map<String, Object> annotationMap = new HashMap<>();
        if (this.annotations != null) {
            for (AnnotationNode annotation : this.annotations) {
                annotationMap.put(annotation.name, annotation.value);
            }
        }
        return Collections.unmodifiableMap(annotationMap);
    }

    @Override
    public <T> T accept(NodeVisitor<T> visitor) {
        return visitor.visitGuardDefinitionNode(this);
    }

    @Override
    public String toString() {
        return "GuardDefinitionNode{" +
               "id=" + id.map(String::valueOf).orElse("none") +
               ", guardName='" + guardName + "\'" +
               ", returnType=" + returnType +
               ", annotations=" + annotations +
               "}";
    }

    // Consider adding equals() and hashCode()
} 