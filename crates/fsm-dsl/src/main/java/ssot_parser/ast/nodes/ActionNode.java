package ssot_parser.ast.nodes;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.List;
import java.util.ArrayList;
import ssot_parser.ast.AstNode;
import ssot_parser.NodeWithId;
import ssot_parser.ast.NodeVisitor;
import ssot_parser.ast.nodes.AnnotationNode;

/**
 * Represents an action definition within the 'actions { ... }' block of a machine.
 * Corresponds to the 'actionDefinition' rule in the grammar.
 */
public class ActionNode implements AstNode, NodeWithId {
    // ID annotation is optional for actions
    private final Optional<Long> id;
    public final String name;
    // Store other annotations ($name, $flag) in a map
    private final List<AnnotationNode> annotations;

    // Constructor including annotations
    public ActionNode(Optional<Long> id, String name, List<AnnotationNode> annotations) {
        this.id = id;
        this.name = name;
        this.annotations = Collections.unmodifiableList(annotations != null ? new ArrayList<>(annotations) : Collections.emptyList());
    }

    @Override
    public Optional<Long> getId() {
        return id;
    }

    public String getName() {
        return name;
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
        return visitor.visitActionNode(this);
    }

    @Override
    public String toString() {
        return "ActionNode{" +
               "id=" + id +
               ", name='" + name + "\'" +
               ", annotations=" + annotations +
               "}";
    }

    // Consider adding equals() and hashCode()
} 