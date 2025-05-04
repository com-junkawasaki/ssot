package ssot_parser.ast.nodes;

import java.util.Optional;
import ssot_parser.ast.AstNode;
import ssot_parser.ast.NodeVisitor;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

/**
 * Represents a conditional transition within a state, triggered by a guard condition.
 * Corresponds to the 'if guardName { transition ... }' or 'if guardName transition ...' syntax.
 */
public class ConditionalTransitionNode implements AstNode {
    public final String guardName; // Name of the referenced guard, potentially with "(not)"
    public final TransitionNode transition;

    public ConditionalTransitionNode(String guardName, TransitionNode transition) {
        this.guardName = guardName;
         if (transition == null) {
             System.err.println("Error: ConditionalTransitionNode created with null transition for guard: " + guardName);
             this.transition = new TransitionNode("ERROR_NULL_TRANSITION", Optional.empty(), Collections.emptyList(), Collections.emptyList());
        } else {
             this.transition = transition;
        }
    }

    // Getters
    public String getGuardName() {
        return guardName;
    }

    public TransitionNode getTransition() {
        return transition;
    }

    @Override
    public <T> T accept(NodeVisitor<T> visitor) {
        return visitor.visitConditionalTransitionNode(this);
    }

    @Override
    public String toString() {
        return "ConditionalTransitionNode{" +
               "guardName='" + guardName + "\'" +
               ", transition=" + transition +
               "}";
    }

    // No ID or annotations directly on this node, they are within the TransitionNode
     @Override public Map<String, Object> getAnnotations() { return transition != null ? transition.getAnnotations() : Collections.emptyMap(); }
     @Override public Optional<Long> getId() { return transition != null ? transition.getId() : Optional.empty(); }
} 