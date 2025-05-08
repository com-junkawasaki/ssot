package ssot_parser.ast.handlers;

import ssot_parser.ast.AstNode;
import ssot_parser.ast.NodeVisitor;
import ssot_parser.ast.nodes.AnnotationNode;
import ssot_parser.ast.nodes.TransitionNode;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.ArrayList;

/**
 * Represents the 'onDone' or 'onError' block within an 'invoke' definition.
 * Contains optional actions and a potential target state transition.
 */
public class InvokeCompletionHandler implements AstNode {
    public final List<String> actionRefs; // References to action names
    public final Optional<TransitionNode> transition; // Optional target state transition
    public final List<AnnotationNode> annotations; // Annotations for this handler

    public InvokeCompletionHandler(List<String> actionRefs,
                                 Optional<TransitionNode> transition,
                                 List<AnnotationNode> annotations) {
        this.actionRefs = Collections.unmodifiableList(actionRefs != null ? new ArrayList<>(actionRefs) : Collections.emptyList());
        this.transition = transition;
        this.annotations = Collections.unmodifiableList(annotations != null ? new ArrayList<>(annotations) : Collections.emptyList());
    }

    public List<String> getActionRefs() {
        return actionRefs;
    }

    public Optional<TransitionNode> getTransition() {
        return transition;
    }

    public List<AnnotationNode> getAnnotations() {
        return annotations;
    }

    @Override
    public <T> T accept(NodeVisitor<T> visitor) {
        // Assuming visitor has visitInvokeCompletionHandler
        // return visitor.visitInvokeCompletionHandler(this);
        return null; // Placeholder
    }

    @Override
    public String toString() {
        return "InvokeCompletionHandler{" +
               "actionRefs=" + actionRefs +
               ", transition=" + transition.map(TransitionNode::toString).orElse("none") +
               ", annotations=" + annotations +
               "}";
    }
} 