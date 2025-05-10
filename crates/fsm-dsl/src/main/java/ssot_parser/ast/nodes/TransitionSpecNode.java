package ssot_parser.ast.nodes;

import ssot_parser.ast.AstNode;
import ssot_parser.ast.NodeVisitor;
import ssot_parser.ast.nodes.TargetStateNode; // Corrected import
import ssot_parser.ast.nodes.GuardReferenceNode; // Added import
import ssot_parser.ast.nodes.ActionReferenceNode; // Added import

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Collections;

public class TransitionSpecNode implements AstNode {
    private final TargetStateNode targetState;
    private final List<ActionReferenceNode> actions;
    private final List<GuardReferenceNode> guards; // Assuming GuardReferenceNode exists
    private final List<String> allowedActors;
    private final Map<String, Object> blockAnnotations; // Annotations inside the transition {} block
    private final Optional<Long> id; // If spec can have an ID from its own annotations

    public TransitionSpecNode(Optional<Long> id, TargetStateNode targetState, List<ActionReferenceNode> actions,
                              List<GuardReferenceNode> guards, List<String> allowedActors,
                              Map<String, Object> blockAnnotations) {
        this.id = id;
        this.targetState = targetState;
        this.actions = Collections.unmodifiableList(actions != null ? actions : Collections.emptyList());
        this.guards = Collections.unmodifiableList(guards != null ? guards : Collections.emptyList());
        this.allowedActors = Collections.unmodifiableList(allowedActors != null ? allowedActors : Collections.emptyList());
        this.blockAnnotations = Collections.unmodifiableMap(blockAnnotations != null ? blockAnnotations : Collections.emptyMap());
    }
     // Simpler constructor if ID is not typical for the spec itself
    public TransitionSpecNode(TargetStateNode targetState, List<ActionReferenceNode> actions,
                              List<GuardReferenceNode> guards, List<String> allowedActors,
                              Map<String, Object> blockAnnotations) {
        this(Optional.empty(), targetState, actions, guards, allowedActors, blockAnnotations);
    }

    public TargetStateNode getTargetState() {
        return targetState;
    }

    public List<ActionReferenceNode> getActions() {
        return actions;
    }

    public List<GuardReferenceNode> getGuards() {
        return guards;
    }

    public List<String> getAllowedActors() {
        return allowedActors;
    }

    public Map<String, Object> getBlockAnnotations() {
        return blockAnnotations;
    }

    @Override
    public Optional<Long> getId() {
        return id;
    }

    @Override
    public Map<String, Object> getAnnotations() {
        // These are annotations defined *within* the transition block { $anno: ... }
        // Not to be confused with annotations on the transition line (e.g., on EVENT @id(1) ...)
        return blockAnnotations;
    }

    @Override
    public <T> T accept(NodeVisitor<T> visitor) {
        // return visitor.visitTransitionSpecNode(this); // Visitor needs this method
        return null;
    }
} 