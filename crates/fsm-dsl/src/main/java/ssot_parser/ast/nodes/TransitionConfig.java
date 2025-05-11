package ssot_parser.ast.nodes;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import ssot_parser.ast.AstNode;
import ssot_parser.ast.NodeVisitor;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import ssot_parser.NodeWithId;

/**
 * Represents the configuration details for a single transition target,
 * including target state, actions, and an optional guard condition.
 * This is often used within EventHandlerNode or ConditionalTransitionNode.
 */
public class TransitionConfig implements NodeWithId {
    public final String targetStateName;
    public final List<String> actions;
    public final Optional<String> condition; // Guard name

    public TransitionConfig(String targetStateName, List<String> actions, Optional<String> condition) {
        this.targetStateName = targetStateName;
        this.actions = actions != null ? Collections.unmodifiableList(new ArrayList<>(actions)) : Collections.emptyList();
        this.condition = condition;
    }

    public String getTargetStateName() {
        return targetStateName;
    }

    public List<String> getActions() {
        return actions;
    }

    public Optional<String> getCondition() {
        return condition;
    }

    @Override
    public Optional<Long> getId() {
        // TransitionConfig nodes do not have their own @id
        return Optional.empty();
    }

    @Override
    public Map<String, Object> getAnnotations() {
        // This node type currently does not support annotations directly
        return Collections.emptyMap();
    }

    @Override
    public <T> T accept(NodeVisitor<T> visitor) {
        // Note: This node might not need its own visit method if handled within its parent.
        // If needed, add visitor.visitTransitionConfig(this);
        return null; // Or throw an exception if direct visit is not intended.
    }

    @Override
    public String toString() {
        return "TransitionConfig{" +
               "target='" + targetStateName + "\'" +
               ", actions=" + actions +
               ", condition=" + condition.orElse("none") +
               "}";
    }

    // Consider adding equals() and hashCode()
} 