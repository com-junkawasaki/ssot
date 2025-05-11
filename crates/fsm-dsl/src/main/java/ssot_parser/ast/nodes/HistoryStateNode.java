package ssot_parser.ast.nodes;

import ssot_parser.ast.AstNode;
import ssot_parser.ast.NodeVisitor;
import ssot_parser.ast.nodes.state.HistoryStateType;
import ssot_parser.NodeWithId;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.ArrayList;
import java.util.Map;

/**
 * Represents a history pseudo-state (shallow or deep) within a state machine.
 * Can contain a default target transition if no history exists.
 */
public class HistoryStateNode implements AstNode, NodeWithId {
    private final Optional<Long> id;
    private final String name;
    private final HistoryStateType type;
    private final Optional<TransitionSpecNode> defaultTransition;
    private final List<AnnotationNode> annotations;

    public HistoryStateNode(Optional<Long> id,
                            String name,
                            HistoryStateType type,
                            Optional<TransitionSpecNode> defaultTransition,
                            List<AnnotationNode> annotations) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.defaultTransition = defaultTransition;
        this.annotations = Collections.unmodifiableList(annotations != null ? new ArrayList<>(annotations) : Collections.emptyList());
    }

    @Override
    public Optional<Long> getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public HistoryStateType getType() {
        return type;
    }

    public Optional<TransitionSpecNode> getDefaultTransition() {
        return defaultTransition;
    }

    @Override
    public Map<String, Object> getAnnotations() {
        Map<String, Object> annotationMap = new java.util.HashMap<>();
        if (this.annotations != null) {
            for (AnnotationNode annotation : this.annotations) {
                annotationMap.put(annotation.name, annotation.value);
            }
        }
        return java.util.Collections.unmodifiableMap(annotationMap);
    }

    @Override
    public <T> T accept(NodeVisitor<T> visitor) {
        return visitor.visitHistoryStateNode(this);
    }

    @Override
    public String toString() {
        return "HistoryStateNode{" +
               "id=" + id.map(String::valueOf).orElse("none") +
               ", name='" + name + '\'' +
               ", type=" + type +
               ", defaultTransition=" + defaultTransition.map(TransitionSpecNode::toString).orElse("none") +
               ", annotations=" + annotations +
               '}';
    }

    // Consider adding equals() and hashCode()
}
