package ssot_parser.ast.nodes;

import ssot_parser.ast.AstNode;
import ssot_parser.ast.NodeVisitor;

import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;

// Import AnnotationNode
import ssot_parser.ast.nodes.AnnotationNode;

/**
 * Represents an invoke definition within the 'invokes { ... }' block of a machine.
 * Corresponds to the 'invokeDefinition' rule in the grammar (assuming similar structure to action/guardDefinition).
 */
public class InvokeNode implements AstNode {
    // ID annotation is optional
    private final Optional<Long> id;
    private final String name; // Name of the invoked service/actor/function
    // Store other annotations ($name, $flag) in a map
    private final List<AnnotationNode> annotations;
    // Invokes might have details like src, data, onDone, onError in a full implementation

    // Constructor including annotations
    public InvokeNode(Optional<Long> id, String name, List<AnnotationNode> annotations) {
        this.id = id;
        this.name = name;
        this.annotations = annotations != null ? Collections.unmodifiableList(new ArrayList<>(annotations)) : Collections.emptyList();
    }

    public Optional<Long> getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<AnnotationNode> getAnnotations() {
        return annotations;
    }

    @Override
    public <T> T accept(NodeVisitor<T> visitor) {
        // Assuming a visitor pattern
        // return visitor.visitInvokeNode(this);
        throw new UnsupportedOperationException("Visitor pattern not yet implemented for InvokeNode");
    }

    @Override
    public String toString() {
        return "InvokeNode{" +
               "id=" + id +
               ", name='" + name + "\'" +
               ", annotations=" + annotations +
               "}";
    }

    // Consider adding equals() and hashCode()
} 