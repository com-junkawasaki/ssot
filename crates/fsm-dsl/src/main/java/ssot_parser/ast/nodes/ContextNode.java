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
    // Potentially add annotations specific to the context block if the grammar allows
    // private final List<AnnotationNode> annotations;

    public ContextNode(List<FieldNode> variables) {
        // Add null check for variables
        this.variables = variables != null ? Collections.unmodifiableList(variables) : Collections.emptyList();
        // Initialize annotations if added
    }

    public List<FieldNode> getVariables() {
        return variables;
    }

    // public List<AnnotationNode> getAnnotations() { return annotations; }

    @Override
    public <T> T accept(NodeVisitor<T> visitor) {
        // Assuming a visitor pattern
        // return visitor.visitContextNode(this);
        throw new UnsupportedOperationException("Visitor pattern not yet implemented for ContextNode");
    }

    @Override
    public String toString() {
        return "ContextNode{" +
               "variables=" + variables +
               // ", annotations=" + annotations +
               '}';
    }

    // Consider adding equals() and hashCode()
} 