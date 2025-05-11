package ssot_parser.ast.nodes;

import ssot_parser.ast.AstNode;
import ssot_parser.NodeWithId;
import ssot_parser.ast.NodeVisitor;
import ssot_parser.ast.nodes.AnnotationNode;
import ssot_parser.ast.nodes.TransitionSpecNode;
import ssot_parser.ast.nodes.ActionReferenceNode;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

/**
 * Represents the 'onDone' or 'onError' block within an 'invoke' definition as an AST node.
 * Contains optional actions and a potential target state transition specification.
 */
public class InvokeCompletionHandlerNode implements AstNode, NodeWithId {
    private final Optional<Long> id; // Optional @id annotation for the handler itself
    public final List<ActionReferenceNode> actions;
    public final Optional<TransitionSpecNode> transitionSpec;
    public final List<AnnotationNode> annotations;

    public InvokeCompletionHandlerNode(Optional<Long> id,
                                       List<ActionReferenceNode> actions,
                                       Optional<TransitionSpecNode> transitionSpec,
                                       List<AnnotationNode> annotations) {
        this.id = id;
        this.actions = Collections.unmodifiableList(actions != null ? new ArrayList<>(actions) : Collections.emptyList());
        this.transitionSpec = transitionSpec;
        this.annotations = Collections.unmodifiableList(annotations != null ? new ArrayList<>(annotations) : Collections.emptyList());
    }

    @Override
    public Optional<Long> getId() {
        return id;
    }

    public List<ActionReferenceNode> getActions() {
        return actions;
    }

    public Optional<TransitionSpecNode> getTransitionSpec() {
        return transitionSpec;
    }

    @Override
    public Map<String, Object> getAnnotations() {
        Map<String, Object> map = new HashMap<>();
        if (this.annotations != null) {
            for (AnnotationNode ann : this.annotations) {
                // Exclude @id if it's already covered by getId()
                if (!"@id".equals(ann.name) || !id.isPresent()) {
                    map.put(ann.name, Optional.ofNullable(ann.value).orElse(true));
                }
            }
        }
        return Collections.unmodifiableMap(map);
    }

    public List<AnnotationNode> getRawAnnotations() {
        return annotations;
    }

    @Override
    public <T> T accept(NodeVisitor<T> visitor) {
        return visitor.visitInvokeCompletionHandlerNode(this); // Assuming visitor will have this method
    }

    @Override
    public String toString() {
        return "InvokeCompletionHandlerNode{" +
               "id=" + id.map(String::valueOf).orElse("none") +
               ", actions=" + actions +
               ", transitionSpec=" + transitionSpec.map(TransitionSpecNode::toString).orElse("none") +
               ", annotations=" + annotations +
               "}";
    }
} 