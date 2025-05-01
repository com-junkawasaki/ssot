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

/**
 * Represents the 'context { ... }' block within a machine definition.
 * It holds the definitions of context variables, likely similar to fields in a struct.
 */
public class ContextNode implements AstNode, NodeWithId {

    private final Optional<Long> id;
    // Assuming context contains field-like definitions
    private final List<FieldNode> variables;
    // private final Map<String, Object> annotations; // Change to List<AnnotationNode>
    private final List<AnnotationNode> annotations;

    public ContextNode(Optional<Long> id, List<FieldNode> variables, List<AnnotationNode> annotations) { // Update constructor
        this.id = id; // Assign id
        // Add null check for variables
        this.variables = variables != null ? Collections.unmodifiableList(variables) : Collections.emptyList();
        // this.annotations = Collections.emptyMap(); // Initialize to empty for now
        this.annotations = Collections.unmodifiableList(annotations != null ? new ArrayList<>(annotations) : Collections.emptyList()); // Assign annotations list
    }

    @Override // Add Override for getId
    public Optional<Long> getId() {
        return id;
    }

    public List<FieldNode> getVariables() {
        return variables;
    }

    @Override
    // public Map<String, Object> getAnnotations() { // Change return type
    public List<AnnotationNode> getAnnotations() {
        return annotations;
    }

    @Override
    public <T> T accept(NodeVisitor<T> visitor) {
        return visitor.visitContextNode(this);
    }

    @Override
    public String toString() {
        return "ContextNode{" +
               "id=" + id.map(String::valueOf).orElse("none") + // Add id
               ", variables=" + variables +
               ", annotations=" + annotations + // Add annotations
               '}';
    }

    // Consider adding equals() and hashCode()
} 