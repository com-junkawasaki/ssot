package ssot_parser.ast.handlers;

import ssot_parser.ast.AstNode;
import ssot_parser.ast.NodeVisitor;
import ssot_parser.ast.nodes.AnnotationNode;
import ssot_parser.ast.nodes.TransitionSpecNode;
import ssot_parser.ast.nodes.ActionReferenceNode;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.ArrayList;
import java.util.Map;

/**
 * Represents the 'onDone' or 'onError' block within an 'invoke' definition.
 * Contains optional actions and a potential target state transition specification.
 */
public class InvokeCompletionHandler implements AstNode {
    public final List<ActionReferenceNode> actions;
    public final Optional<TransitionSpecNode> transitionSpec;
    public final List<AnnotationNode> annotations;
    private final Optional<Long> id;

    public InvokeCompletionHandler(List<ActionReferenceNode> actions,
                                 Optional<TransitionSpecNode> transitionSpec,
                                 List<AnnotationNode> annotations) {
        this(actions, transitionSpec, annotations, Optional.empty());
    }

    public InvokeCompletionHandler(List<ActionReferenceNode> actions,
                                 Optional<TransitionSpecNode> transitionSpec,
                                 List<AnnotationNode> annotations,
                                 Optional<Long> id) {
        this.actions = Collections.unmodifiableList(actions != null ? new ArrayList<>(actions) : Collections.emptyList());
        this.transitionSpec = transitionSpec;
        this.annotations = Collections.unmodifiableList(annotations != null ? new ArrayList<>(annotations) : Collections.emptyList());
        this.id = id;
    }

    public List<ActionReferenceNode> getActions() {
        return actions;
    }

    public Optional<TransitionSpecNode> getTransitionSpec() {
        return transitionSpec;
    }

    @Override
    public Optional<Long> getId() {
        return id;
    }

    @Override
    public Map<String, Object> getAnnotations() {
        Map<String, Object> map = new java.util.HashMap<>();
        for (AnnotationNode ann : this.annotations) {
            map.put(ann.name, Optional.ofNullable(ann.value).orElse(true));
        }
        return Collections.unmodifiableMap(map);
    }
    
    public List<AnnotationNode> getRawAnnotations() {
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
               "actions=" + actions +
               ", transitionSpec=" + transitionSpec.map(TransitionSpecNode::toString).orElse("none") +
               ", annotations=" + annotations +
               "}";
    }
} 