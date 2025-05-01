package ssot_parser.ast.nodes;

import ssot_parser.ast.AstNode;
import ssot_parser.ast.NodeVisitor;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Represents the 'context { ... }' block within a machine definition.
 * It holds the definitions of context variables, likely similar to fields in a struct.
 */
public class ContextNode implements AstNode {

    // Assuming context contains field-like definitions
    private final List<FieldNode> variables;
    private final Map<String, Object> annotations;

    public ContextNode(List<FieldNode> variables /*, Map<String, Object> annotations */) {
        // Add null check for variables
        this.variables = variables != null ? Collections.unmodifiableList(variables) : Collections.emptyList();
        this.annotations = Collections.emptyMap(); // Initialize to empty for now
    }

    public List<FieldNode> getVariables() {
        return variables;
    }

    @Override
    public Map<String, Object> getAnnotations() {
        return annotations;
    }

    @Override
    public <T> T accept(NodeVisitor<T> visitor) {
        return visitor.visitContextNode(this);
    }

    @Override
    public String toString() {
        return "ContextNode{" +
               "variables=" + variables +
               '}';
    }

    // Consider adding equals() and hashCode()
} 