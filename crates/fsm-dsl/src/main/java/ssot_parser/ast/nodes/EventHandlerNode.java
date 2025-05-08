package ssot_parser.ast.nodes;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import ssot_parser.ast.AstNode;
import ssot_parser.ast.NodeVisitor;
import java.util.ArrayList;
import java.util.Map;

/**
 * Represents an event handler within a state (e.g., 'on EVENT { ... }').
 * It groups transitions (direct and conditional) triggered by one or more events.
 */
public class EventHandlerNode implements AstNode {
    private final List<String> eventRefs;
    private final List<TransitionNode> directTransitions;
    private final List<ConditionalTransitionNode> conditionalTransitions;
    private final List<AnnotationNode> annotations; // Annotations on the ON block itself

    public EventHandlerNode(List<String> eventRefs,
                            List<TransitionNode> directTransitions,
                            List<ConditionalTransitionNode> conditionalTransitions,
                            List<AnnotationNode> annotations) {
        this.eventRefs = Collections.unmodifiableList(eventRefs != null ? new ArrayList<>(eventRefs) : Collections.emptyList());
        this.directTransitions = Collections.unmodifiableList(directTransitions != null ? new ArrayList<>(directTransitions) : Collections.emptyList());
        this.conditionalTransitions = Collections.unmodifiableList(conditionalTransitions != null ? new ArrayList<>(conditionalTransitions) : Collections.emptyList());
        this.annotations = Collections.unmodifiableList(annotations != null ? new ArrayList<>(annotations) : Collections.emptyList());
    }

    public List<String> getEventRefs() {
        return eventRefs;
    }

    public List<TransitionNode> getDirectTransitions() {
        return directTransitions;
    }

    public List<ConditionalTransitionNode> getConditionalTransitions() {
        return conditionalTransitions;
    }

    public List<AnnotationNode> getAnnotations() {
        return annotations;
    }

    @Override
    public <T> T accept(NodeVisitor<T> visitor) {
        return visitor.visitEventHandlerNode(this);
    }

    @Override
    public String toString() {
        return "EventHandlerNode{" +
               "events=" + eventRefs +
               ", directTransitions=" + directTransitions +
               ", conditionalTransitions=" + conditionalTransitions +
               ", annotations=" + annotations +
               "}";
    }

    // Consider adding equals() and hashCode()
} 