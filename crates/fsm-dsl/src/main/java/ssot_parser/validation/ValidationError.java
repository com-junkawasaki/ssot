package ssot_parser.validation;

import ssot_parser.ast.AstNode;
import java.util.Objects;

/**
 * Represents a validation error or warning found during AST validation.
 */
public class ValidationError {

    public enum Severity {
        ERROR,
        WARNING
    }

    private final String message;
    private final Severity severity;
    private final AstNode node; // The node where the error occurred (optional)

    public ValidationError(String message, Severity severity, AstNode node) {
        this.message = Objects.requireNonNull(message, "Validation message cannot be null");
        this.severity = Objects.requireNonNull(severity, "Severity cannot be null");
        this.node = node; // Can be null if not tied to a specific node
    }

    public ValidationError(String message, Severity severity) {
        this(message, severity, null);
    }

    public String getMessage() {
        return message;
    }

    public Severity getSeverity() {
        return severity;
    }

    public AstNode getNode() {
        return node;
    }

    @Override
    public String toString() {
        String nodeInfo = (node != null) ? " near [" + node.getClass().getSimpleName() + "]" : "";
        // Enhance nodeInfo later with name or ID if possible
        // if (node instanceof ssot_parser.NodeWithId nid && nid.getId().isPresent()) { ... }
        // if (node instanceof /* NodeWithName */ ...) { ... }
        return severity + ": " + message + nodeInfo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ValidationError that = (ValidationError) o;
        return Objects.equals(message, that.message) && severity == that.severity && Objects.equals(node, that.node);
    }

    @Override
    public int hashCode() {
        return Objects.hash(message, severity, node);
    }
} 