package ssot_parser.ast.nodes;

import ssot_parser.ast.AstNode;
import ssot_parser.ast.NodeVisitor;
import ssot_parser.NodeWithId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Objects;

/**
 * Represents an 'invoke' declaration within a state definition.
 * This typically involves invoking a service, another machine, or an external process.
 * Corresponds to the 'invokeState' rule (or similar) in the grammar.
 */
public class InvokeStateNode implements AstNode, NodeWithId {

    /**
     * Represents the details of a transition triggered by invoke completion (onDone) or error (onError).
     */
    public record InvokeTransition(
        Optional<String> target, // Target state name (optional if staying in same state? Depends on semantic)
        Optional<String> action, // Action to execute
        Optional<String> guard, // Guard condition name
        List<AnnotationNode> annotations // Annotations specific to this transition
    ) {
         public InvokeTransition {
             // Ensure lists are unmodifiable
             annotations = Collections.unmodifiableList(annotations != null ? new ArrayList<>(annotations) : Collections.emptyList());
         }
         // Default constructor with empty optionals/list
          public InvokeTransition() {
             this(Optional.empty(), Optional.empty(), Optional.empty(), Collections.emptyList());
          }
    }

    private final Optional<Long> id;
    private final List<AnnotationNode> annotations;
    private final String src; // The ID/name of the service/machine/lambda to invoke (extracted from $src or grammar)
    private final Optional<InvokeTransition> onDoneTransition;
    private final Optional<InvokeTransition> onErrorTransition;
    // TODO: Add fields for parameters/data mapping, etc.

    public InvokeStateNode(Optional<Long> id, List<AnnotationNode> annotations, String src,
                           Optional<InvokeTransition> onDoneTransition,
                           Optional<InvokeTransition> onErrorTransition
                           /* Add other params */) {
        this.id = id;
        this.annotations = Collections.unmodifiableList(annotations != null ? new ArrayList<>(annotations) : Collections.emptyList());
        this.src = src;
        this.onDoneTransition = Objects.requireNonNull(onDoneTransition, "onDoneTransition cannot be null");
        this.onErrorTransition = Objects.requireNonNull(onErrorTransition, "onErrorTransition cannot be null");
    }

    @Override
    public Optional<Long> getId() {
        return id;
    }

    @Override
    public List<AnnotationNode> getAnnotations() {
        return annotations;
    }

    public String getSrc() {
        return src;
    }

    public Optional<InvokeTransition> getOnDoneTransition() {
        return onDoneTransition;
    }

    public Optional<InvokeTransition> getOnErrorTransition() {
        return onErrorTransition;
    }

    // TODO: Add getters for other fields (data mapping etc.)

    @Override
    public <T> T accept(NodeVisitor<T> visitor) {
        // TODO: Add visitInvokeStateNode method to NodeVisitor interface
        // return visitor.visitInvokeStateNode(this);
        System.err.println("Warning: NodeVisitor.visitInvokeStateNode not implemented yet.");
        return null; // Placeholder return
    }

    @Override
    public String toString() {
        return "InvokeStateNode{" +
               "id=" + id.map(String::valueOf).orElse("none") +
               ", src='" + src + '\'' +
               ", annotations=" + annotations +
               ", onDone=" + onDoneTransition.map(InvokeTransition::toString).orElse("none") +
               ", onError=" + onErrorTransition.map(InvokeTransition::toString).orElse("none") +
               '}';
    }

    // Consider adding equals() and hashCode()
} 