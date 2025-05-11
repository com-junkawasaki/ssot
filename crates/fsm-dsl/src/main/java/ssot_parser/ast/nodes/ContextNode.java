package ssot_parser.ast.nodes;

import ssot_parser.ast.AstNode;
import ssot_parser.ast.NodeVisitor;

import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import java.util.HashMap;

/**
 * Represents the 'context { ... }' block within a machine definition.
 * Contains the fields defining the machine's extended state.
 */
public class ContextNode implements AstNode {
    public final List<ContextVariableNode> fields;
    private final List<AnnotationNode> annotations;
    private final Map<String, Object> namedAnnotationsMap;
    private final Optional<Long> id = Optional.empty();

    public ContextNode(List<ContextVariableNode> fields, List<AnnotationNode> annotations) {
        this.fields = fields != null ? Collections.unmodifiableList(new ArrayList<>(fields)) : Collections.emptyList();
        this.annotations = annotations != null ? Collections.unmodifiableList(new ArrayList<>(annotations)) : Collections.emptyList();
        this.namedAnnotationsMap = mapBlockAnnotations(this.annotations);
    }

    public List<ContextVariableNode> getFields() {
        return fields;
    }

    public List<ContextVariableNode> getVariables() {
        return fields;
    }

    @Override
    public Optional<Long> getId() {
        return id;
    }

    @Override
    public Map<String, Object> getAnnotations() {
        return namedAnnotationsMap;
    }

    private Map<String, Object> mapBlockAnnotations(List<AnnotationNode> annotationNodes) {
        Map<String, Object> map = new HashMap<>();
        if (annotationNodes != null) {
            for (AnnotationNode annotation : annotationNodes) {
                map.put(annotation.name, Optional.ofNullable(annotation.value).orElse(Boolean.TRUE));
            }
        }
        return Collections.unmodifiableMap(map);
    }

    @Override
    public <T> T accept(NodeVisitor<T> visitor) {
        return visitor.visitContextNode(this);
    }

    @Override
    public String toString() {
        return "ContextNode{" +
               "fields=" + fields +
               ", annotations=" + annotations +
               '}';
    }

    // Consider adding equals() and hashCode()
}