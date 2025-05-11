package ssot_parser.ast.nodes;

import ssot_parser.ast.AstNode;
import ssot_parser.NodeWithId;
import ssot_parser.ast.NodeVisitor;
import java.util.Map;
import java.util.Optional;
import java.util.List;
import java.util.Collections;
import java.util.HashMap;
import java.util.ArrayList;

// Import ParameterNode and TypeExprNode
import ssot_parser.ast.type.TypeExprNode;

/**
 * Represents an action definition within the 'actions { ... }' block of a machine.
 * Corresponds to the 'actionDefinition' rule.
 */
public class ActionDefinitionNode implements AstNode, NodeWithId {
    private final Optional<Long> id;
    public final String actionName;
    private final List<ParameterNode> parameters;
    private final Optional<TypeExprNode> returnType;
    // For now, parameters are just illustrative names in the DSL (ctx, event)
    // A more complex implementation might parse formal parameters.
    private final List<AnnotationNode> annotations; // Although not shown in DSL example, could be useful.

    public ActionDefinitionNode(Optional<Long> id, 
                              String actionName, 
                              List<ParameterNode> parameters, 
                              Optional<TypeExprNode> returnType, 
                              List<AnnotationNode> annotations) {
        this.id = id;
        this.actionName = actionName;
        this.parameters = Collections.unmodifiableList(parameters != null ? new ArrayList<>(parameters) : Collections.emptyList());
        this.returnType = returnType;
        this.annotations = Collections.unmodifiableList(annotations != null ? new ArrayList<>(annotations) : Collections.emptyList());
    }

    @Override
    public Optional<Long> getId() {
        return id;
    }

    public String getActionName() {
        return actionName;
    }

    public List<ParameterNode> getParameters() {
        return parameters;
    }

    public Optional<TypeExprNode> getReturnType() {
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
        return visitor.visitActionDefinitionNode(this);
    }

    @Override
    public String toString() {
        return "ActionDefinitionNode{" +
               "id=" + id.map(String::valueOf).orElse("none") +
               ", actionName='" + actionName + "\'" +
               ", parameters=" + parameters +
               ", returnType=" + returnType.map(Object::toString).orElse("void") +
               ", annotations=" + annotations +
               "}";
    }

    // Consider adding equals() and hashCode()
} 