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
    public final String invokedStateMachineName; // Renamed from src for clarity
    public final Optional<InvokeTransition> onDoneTransition;
    public final Optional<InvokeTransition> onErrorTransition;
    // Convenience list for transitions, parsed from onDone/onError
    public final List<TransitionNode> transitions;

    public InvokeStateNode(Optional<Long> id, List<AnnotationNode> annotations, String invokedStateMachineName,
                           Optional<InvokeTransition> onDoneTransition,
                           Optional<InvokeTransition> onErrorTransition,
                           List<TransitionNode> transitions) {
        this.id = id;
        this.annotations = Collections.unmodifiableList(annotations != null ? new ArrayList<>(annotations) : Collections.emptyList());
        this.invokedStateMachineName = Objects.requireNonNull(invokedStateMachineName, "Invoked state machine name cannot be null");
        this.onDoneTransition = Objects.requireNonNull(onDoneTransition, "onDoneTransition cannot be null");
        this.onErrorTransition = Objects.requireNonNull(onErrorTransition, "onErrorTransition cannot be null");
        this.transitions = Collections.unmodifiableList(transitions != null ? new ArrayList<>(transitions) : Collections.emptyList());
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

    public String getInvokedStateMachineName() {
        return invokedStateMachineName;
    }

    public Optional<InvokeTransition> getOnDoneTransition() {
        return onDoneTransition;
    }

    public Optional<InvokeTransition> getOnErrorTransition() {
        return onErrorTransition;
    }

    public List<TransitionNode> getTransitions() {
        return transitions;
    }

    @Override
    public <T> T accept(NodeVisitor<T> visitor) {
        return visitor.visitInvokeStateNode(this);
    }

    @Override
    public String toString() {
        return "InvokeStateNode{" +
               "id=" + id.map(String::valueOf).orElse("none") +
               ", invokedStateMachineName='" + invokedStateMachineName + "\'" +
               ", annotations=" + annotations +
               ", onDone=" + onDoneTransition.map(InvokeTransition::toString).orElse("none") +
               ", onError=" + onErrorTransition.map(InvokeTransition::toString).orElse("none") +
               ", transitions=" + transitions +
               '}';
    }

    // Consider adding equals() and hashCode()
} 