package ssot_parser;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import ssot_parser.ast.AstNode;
import ssot_parser.ast.NodeVisitor;

/** Represents a transition between states */
public class TransitionNode implements NodeWithId {
    private final Optional<Long> id;
    public final String fromState;
    public final String toState;
    public final String event;
    public final Optional<String> condition; // Optional guard condition
    public final Optional<String> action;    // Optional action to perform (Simplified: just a string name/ref)
    // TODO: Action should perhaps be List<ActionNode> or similar
    private final Map<String, Object> annotations;


    public TransitionNode(Optional<Long> id, String fromState, String toState, String event,
                          Optional<String> condition, Optional<String> action,
                          Map<String, Object> annotations) {
        this.id = id;
        this.fromState = fromState;
        this.toState = toState;
        this.event = event;
        this.condition = condition;
        this.action = action;
        this.annotations = Collections.unmodifiableMap(annotations != null ? new HashMap<>(annotations) : Collections.emptyMap());
    }

    @Override
    public Optional<Long> getId() {
        return id;
    }

    // Getters for other fields might be useful later
    public String getFromState() { return fromState; }
    public String getToState() { return toState; }
    public String getEvent() { return event; }
    public Optional<String> getCondition() { return condition; }
    public Optional<String> getAction() { return action; }

    @Override
    public Map<String, Object> getAnnotations() {
        return annotations;
    }
} 