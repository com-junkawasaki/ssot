package ssot_parser.ast.nodes;

import ssot_parser.ast.AstNode;
import ssot_parser.ast.NodeVisitor;
import ssot_parser.NodeWithId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Objects;
import java.util.HashMap;

/**
 * Represents an 'invoke' declaration within a state definition.
 * This typically involves invoking a service, another machine, or an external process.
 * Corresponds to the 'invokeState' rule (or similar) in the grammar.
 */
public class InvokeStateNode implements AstNode, NodeWithId {

    /* // Inner record InvokeTransition removed, using TransitionConfig instead
    public record InvokeTransition(...) {
        ...
    }
    */

    private final Optional<Long> id;
    private final List<AnnotationNode> annotations;
    public final String invokedSource; // Name of the invoked source (service.method, task name, etc.)
    public final Optional<TransitionConfig> onDoneTransitionConfig;
    public final Optional<TransitionConfig> onErrorTransitionConfig;
    // No longer storing separate TransitionNode list here, handled by StateNode's structure
    // public final List<TransitionNode> transitions;

    public InvokeStateNode(Optional<Long> id, List<AnnotationNode> annotations, String invokedSource,
                           Optional<TransitionConfig> onDoneTransitionConfig,
                           Optional<TransitionConfig> onErrorTransitionConfig) {
        this.id = id;
        this.annotations = Collections.unmodifiableList(annotations != null ? new ArrayList<>(annotations) : Collections.emptyList());
        this.invokedSource = Objects.requireNonNull(invokedSource, "Invoked source name cannot be null");
        this.onDoneTransitionConfig = Objects.requireNonNull(onDoneTransitionConfig, "onDoneTransitionConfig cannot be null");
        this.onErrorTransitionConfig = Objects.requireNonNull(onErrorTransitionConfig, "onErrorTransitionConfig cannot be null");
        // this.transitions = Collections.unmodifiableList(transitions != null ? new ArrayList<>(transitions) : Collections.emptyList()); // Removed
    }

    @Override
    public Optional<Long> getId() {
        return id;
    }

    @Override
    public Map<String, Object> getAnnotations() {
        Map<String, Object> annotationMap = new HashMap<>();
        for (AnnotationNode annotation : this.annotations) {
            annotationMap.put(annotation.name, annotation.value);
        }
        return Collections.unmodifiableMap(annotationMap);
    }

    public String getInvokedSource() {
        return invokedSource;
    }

    public Optional<TransitionConfig> getOnDoneTransitionConfig() {
        return onDoneTransitionConfig;
    }

    public Optional<TransitionConfig> getOnErrorTransitionConfig() {
        return onErrorTransitionConfig;
    }

    /* // Removed getter for old transitions list
    public List<TransitionNode> getTransitions() {
        return transitions;
    }
    */

    @Override
    public <T> T accept(NodeVisitor<T> visitor) {
        return visitor.visitInvokeStateNode(this);
    }

    @Override
    public String toString() {
        return "InvokeStateNode{" +
               "id=" + id.map(String::valueOf).orElse("none") +
               ", invokedSource='" + invokedSource + "\'" +
               ", annotations=" + annotations +
               ", onDone=" + onDoneTransitionConfig.map(TransitionConfig::toString).orElse("none") +
               ", onError=" + onErrorTransitionConfig.map(TransitionConfig::toString).orElse("none") +
               // ", transitions=" + transitions +
               '}';
    }

    // Consider adding equals() and hashCode()
} 