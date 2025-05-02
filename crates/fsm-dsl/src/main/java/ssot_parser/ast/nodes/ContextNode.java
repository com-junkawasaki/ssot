package ssot_parser.ast.nodes;

import ssot_parser.ast.AstNode;
import ssot_parser.ast.NodeVisitor;
import ssot_parser.NodeWithId;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ArrayList;
import ssot_parser.ast.nodes.AnnotationNode;
import java.util.HashMap;

/**
 * Represents the 'context { ... }' block within a machine definition.
 * It holds the definitions of context variables, likely similar to fields in a struct.
 */
public class ContextNode implements AstNode, NodeWithId {

    private final Optional<Long> id;
    // Assuming context contains field-like definitions
    public final List<FieldNode> variables;
    private final List<AnnotationNode> annotations;

    public ContextNode(Optional<Long> id, List<FieldNode> variables, List<AnnotationNode> annotations) {
        this.id = id;
        this.variables = variables != null ? Collections.unmodifiableList(new ArrayList<>(variables)) : Collections.emptyList();
        this.annotations = Collections.unmodifiableList(annotations != null ? new ArrayList<>(annotations) : Collections.emptyList());
    }

    @Override
    public Optional<Long> getId() {
        return id;
    }

    public List<FieldNode> getVariables() {
        return variables;
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
    public <T> T accept(NodeVisitor<T> visitor) {
        return visitor.visitContextNode(this);
    }

    @Override
    public String toString() {
        return "ContextNode{" +
               "id=" + id.map(String::valueOf).orElse("none") +
               ", variables=" + variables +
               ", annotations=" + annotations +
               '}';
    }

    // Consider adding equals() and hashCode()
} 