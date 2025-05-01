package ssot_parser.ast.nodes;

import ssot_parser.ast.AstNode;
import ssot_parser.ast.NodeVisitor;
import ssot_parser.NodeWithId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Represents an 'invoke' declaration within a state definition.
 * This typically involves invoking a service, another machine, or an external process.
 * Corresponds to the 'invokeState' rule (or similar) in the grammar.
 */
public class InvokeStateNode implements AstNode, NodeWithId {

    private final Optional<Long> id;
    private final List<AnnotationNode> annotations;
    private final String src; // The ID/name of the service/machine/lambda to invoke (extracted from $src or grammar)
    private final Optional<String> onDoneTarget; // Optional target state for onDone
    private final Optional<String> onErrorTarget; // Optional target state for onError
    // TODO: Add fields for onDone, onError transitions, parameters/data mapping, etc. -- Added targets

    public InvokeStateNode(Optional<Long> id, List<AnnotationNode> annotations, String src,
                           Optional<String> onDoneTarget, Optional<String> onErrorTarget /* Add other params */) { // Add onDone/onError params
        this.id = id;
        this.annotations = Collections.unmodifiableList(annotations != null ? new ArrayList<>(annotations) : Collections.emptyList());
        this.src = src; // Placeholder, might need more complex parsing
        this.onDoneTarget = onDoneTarget;
        this.onErrorTarget = onErrorTarget;
        // Initialize other fields (onDone, onError) when added -- DONE for targets
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

    public Optional<String> getOnDoneTarget() { // Add getter
        return onDoneTarget;
    }

    public Optional<String> getOnErrorTarget() { // Add getter
        return onErrorTarget;
    }

    // TODO: Add getters for onDone, onError, etc. -- DONE for targets

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
               // Add other fields (onDone, onError) here
               ", onDoneTarget=" + onDoneTarget.orElse("none") +
               ", onErrorTarget=" + onErrorTarget.orElse("none") +
               '}';
    }

    // Consider adding equals() and hashCode()
} 