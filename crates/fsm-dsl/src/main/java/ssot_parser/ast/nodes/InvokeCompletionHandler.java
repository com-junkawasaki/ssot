package ssot_parser.ast.nodes;

import java.util.Optional;
import java.util.List;
import java.util.Collections;
import java.util.ArrayList;

/**
 * Represents the result handler for an invocation (onDone or onError).
 * It can specify either a transition to a target state (potentially with actions/guard)
 * or a list of actions to execute directly.
 */
public class InvokeCompletionHandler {
    public final TransitionNode transition;
    public final List<String> actions;

    private InvokeCompletionHandler(TransitionNode transition, List<String> actions) {
        // Ensure only one of transition or actions is non-null
        if (transition != null && (actions != null && !actions.isEmpty())) {
            throw new IllegalArgumentException("InvokeCompletionHandler cannot have both transition and actions.");
        }
         if (transition == null && (actions == null || actions.isEmpty())) {
            throw new IllegalArgumentException("InvokeCompletionHandler must have either a transition or actions.");
        }
        this.transition = transition;
        this.actions = actions; // Already unmodifiable if created via factory methods
    }

    /**
     * Factory method to create a handler that performs a transition.
     */
    public static InvokeCompletionHandler fromTransition(TransitionNode transition) {
        if (transition == null) {
             throw new IllegalArgumentException("Transition cannot be null for fromTransition handler.");
        }
        return new InvokeCompletionHandler(transition, null);
    }

    /**
     * Factory method to create a handler that executes actions.
     */
    public static InvokeCompletionHandler fromActions(List<String> actions) {
        if (actions == null || actions.isEmpty()) {
             throw new IllegalArgumentException("Actions list cannot be null or empty for fromActions handler.");
        }
        // Make the list unmodifiable
        List<String> unmodifiableActions = Collections.unmodifiableList(new ArrayList<>(actions));
        return new InvokeCompletionHandler(null, unmodifiableActions);
    }

    public boolean isTransition() {
        return transition != null;
    }

    public boolean isActions() {
        return actions != null;
    }

    public Optional<TransitionNode> getTransition() {
        return Optional.ofNullable(transition);
    }

    public List<String> getActions() {
        return actions != null ? actions : Collections.emptyList();
    }

     @Override
    public String toString() {
        if (isTransition()) {
            return "InvokeCompletion(transition=" + transition + ")";
        } else if (isActions()) {
            return "InvokeCompletion(actions=" + actions + ")";
        } else {
            // This case should not happen due to constructor validation
            return "InvokeCompletion(empty)";
        }
    }

    // Consider adding equals() and hashCode()
} 