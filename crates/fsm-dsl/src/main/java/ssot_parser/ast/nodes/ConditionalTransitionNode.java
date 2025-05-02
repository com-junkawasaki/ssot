package ssot_parser.ast.nodes;

import java.util.Optional;
import ssot_parser.ast.AstNode;
import ssot_parser.ast.NodeVisitor;

/**
 * Represents a conditional transition based on a guard.
 * Corresponds to the 'if guardName { ... } else { ... }' syntax.
 */
public class ConditionalTransitionNode implements AstNode {
    public final String guardName;
    public final TransitionConfig thenTransition;
    public final Optional<TransitionConfig> elseTransition;

    public ConditionalTransitionNode(String guardName, TransitionConfig thenTransition, Optional<TransitionConfig> elseTransition) {
        this.guardName = guardName;
        this.thenTransition = thenTransition;
        this.elseTransition = elseTransition;
    }

    public String getGuardName() {
        return guardName;
    }

    public TransitionConfig getThenTransition() {
        return thenTransition;
    }

    public Optional<TransitionConfig> getElseTransition() {
        return elseTransition;
    }

    @Override
    public <T> T accept(NodeVisitor<T> visitor) {
        return visitor.visitConditionalTransitionNode(this);
    }

    @Override
    public String toString() {
        return "ConditionalTransitionNode{" +
               "guard='" + guardName + "\'" +
               ", then=" + thenTransition +
               ", else=" + elseTransition.map(Object::toString).orElse("none") +
               "}";
    }

    // Consider adding equals() and hashCode()
} 