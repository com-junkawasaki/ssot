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
    private TransitionNode elseTransition; // Optional transition if the guard evaluates to false
    private final List<AnnotationNode> annotations; // Annotations associated with the IF/ELSE structure

    public ConditionalTransitionNode(String guardName, TransitionNode transition) {
        this.guardName = guardName;
         if (transition == null) {
             System.err.println("Error: ConditionalTransitionNode created with null transition for guard: " + guardName);
             this.transition = new TransitionNode(
                Optional.empty(), // id
                null, // sourceStateName
                "ERROR_EVENT", // event
                "ERROR_NULL_TRANSITION", // targetStateName
                Optional.empty(), // conditionRef
                Collections.emptyList(), // actionRefs
                Collections.emptyList(), // annotations
                new TargetStateNode("ERROR_NULL_TRANSITION"), // targetState
                Optional.empty(), // condition
                Collections.emptyList(), // actions
                Collections.emptyList(), // guards
                Collections.emptyList(), // allowedActors
                Optional.empty(), // delay
                TransitionNode.TransitionType.EVENT, // type
                Collections.emptyMap() // annotationsMap
             );
        } else {
             this.transition = transition;
        }
        this.elseTransition = null; // Initialize else transition as null
        this.annotations = new ArrayList<>(); // Initialize annotations list
    }

    // Constructor allowing else transition initially
    public ConditionalTransitionNode(String guardName, TransitionNode transition, TransitionNode elseTransition, List<AnnotationNode> annotations) {
        this.guardName = guardName;
        this.transition = transition;
        this.elseTransition = elseTransition;
        this.annotations = new ArrayList<>(annotations != null ? annotations : Collections.emptyList());
    }

    // Getters
    public String getGuardName() {
        return guardName;
    }

    public TransitionNode getTransition() {
        return transition;
    }

    public Optional<TransitionNode> getElseTransition() {
        return Optional.ofNullable(elseTransition);
    }

    public void setElseTransition(TransitionNode elseTransition) {
        this.elseTransition = elseTransition;
    }

    public List<AnnotationNode> getRawAnnotations() {
        return Collections.unmodifiableList(annotations);
    }

    public void addAnnotations(List<AnnotationNode> annotations) {
        if (annotations != null) {
            this.annotations.addAll(annotations);
        }
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
               (elseTransition != null ? ", else=" + elseTransition : "") +
               ", annotations=" + annotations +
               "}";
    }

    // No ID or annotations directly on this node, they are within the TransitionNode
     @Override public Map<String, Object> getAnnotations() { return transition != null ? transition.getAnnotations() : Collections.emptyMap(); }
     @Override public Optional<Long> getId() { return transition != null ? transition.getId() : Optional.empty(); }
} 