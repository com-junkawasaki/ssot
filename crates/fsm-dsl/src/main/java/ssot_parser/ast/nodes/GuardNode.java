package ssot_parser.ast.nodes;

import ssot_parser.ast.AstNode;
import ssot_parser.ast.NodeVisitor;
import ssot_parser.NodeWithId;

import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import java.util.HashMap;

// Import AnnotationNode
import ssot_parser.ast.nodes.AnnotationNode;

/**
 * Represents a guard definition within the 'guards { ... }' block of a machine.
 * Corresponds to the 'guardDefinition' rule in the grammar (assuming similar structure to actionDefinition).
 */
public class GuardNode implements AstNode, NodeWithId {
    // ID annotation is optional for guards
    private final Optional<Long> id;
    public final String name;
    // Store other annotations ($name, $flag) in a map
    private final List<AnnotationNode> annotations;
    // Guards might have parameters or a specific return type (e.g., boolean) in a full implementation

    // Constructor including annotations
    public GuardNode(Optional<Long> id, String name, List<AnnotationNode> annotations) {
        this.id = id;
        this.name = name;
        this.annotations = annotations != null ? Collections.unmodifiableList(new ArrayList<>(annotations)) : Collections.emptyList();
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
        // Assuming a visitor pattern
        return visitor.visitGuardNode(this);
    }

    @Override
    public String toString() {
        return "GuardNode{" +
               "id=" + id +
               ", name='" + name + "\'" +
               ", annotations=" + annotations +
               "}";
    }

    // Consider adding equals() and hashCode()
} 