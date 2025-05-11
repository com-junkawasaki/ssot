package ssot_parser.ast.nodes;

import ssot_parser.ast.AstNode;
import ssot_parser.NodeWithId;
import ssot_parser.ast.NodeVisitor;
import ssot_parser.ast.type.TypeExprNode;
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
    public final TypeExprNode returnType;
    private final List<AnnotationNode> annotations;
    private final Map<String, Object> namedAnnotationsMap;
    public final String expression;
    public final Map<String, TypeExprNode> parameters;

    public GuardDefinitionNode(Optional<Long> id, 
                               String guardName, 
                               List<AnnotationNode> annotations,
                               TypeExprNode returnType,
                               String expression, 
                               Map<String, TypeExprNode> parameters) {
        this.id = id;
        this.guardName = guardName;
        this.annotations = Collections.unmodifiableList(annotations != null ? new ArrayList<>(annotations) : Collections.emptyList());
        this.namedAnnotationsMap = mapAnnotations(this.annotations);
        this.expression = expression;
        this.parameters = parameters != null ? Collections.unmodifiableMap(new HashMap<>(parameters)) : Collections.emptyMap();

        if (returnType instanceof BaseType) {
            BaseType baseReturnType = (BaseType) returnType;
            if (!"boolean".equalsIgnoreCase(baseReturnType.getTypeName())) {
                System.err.println("Warning: Guard '" + guardName + "' must return bool. Received: " + baseReturnType.getTypeName());
                this.returnType = new BaseType("boolean", Optional.empty(), Collections.emptyMap());
            } else {
                this.returnType = returnType;
            }
        } else {
            System.err.println("Warning: Guard '" + guardName + "' return type is not a BaseType ('" + returnType.getClass().getSimpleName() + "'). Validation needed to ensure it resolves to boolean.");
            this.returnType = returnType;
        }
    }

    @Override
    public Optional<Long> getId() {
        return id;
    }

    public String getGuardName() {
        return guardName;
    }

    public TypeExprNode getReturnType() {
        return returnType;
    }
    
    public String getExpression() {
        return expression;
    }

    public Map<String, TypeExprNode> getParameters() {
        return parameters;
    }

    @Override
    public Map<String, Object> getAnnotations() {
        return this.namedAnnotationsMap;
    }

    private Map<String, Object> mapAnnotations(List<AnnotationNode> annotationNodes) {
        Map<String, Object> map = new HashMap<>();
        if (annotationNodes != null) {
            for (AnnotationNode annotation : annotationNodes) {
                map.put(annotation.getName(), annotation.getValue());
            }
        }
        return Collections.unmodifiableMap(map);
    }

    @Override
    public <T> T accept(NodeVisitor<T> visitor) {
        return visitor.visitGuardDefinitionNode(this);
    }

    @Override
    public String toString() {
        return "GuardDefinitionNode{" +
               "id=" + id.map(String::valueOf).orElse("none") +
               ", guardName='" + guardName + "'" +
               ", returnType=" + returnType +
               ", expression='" + expression + "'" +
               ", parameters=" + parameters +
               ", annotations=" + annotations +
               "}";
    }

    // Consider adding equals() and hashCode()
} 