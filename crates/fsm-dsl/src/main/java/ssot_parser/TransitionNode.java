package ssot_parser;

import java.util.Optional;

/** Represents a transition between states */
public class TransitionNode implements NodeWithId {
    private final Optional<Long> id;
    public final String fromState;
    public final String toState;
    public final String event;
    public final Optional<String> condition; // Optional guard condition
    public final Optional<String> action;    // Optional action to perform

    public TransitionNode(Optional<Long> id, String fromState, String toState, String event,
                          Optional<String> condition, Optional<String> action) {
        this.id = id;
        this.fromState = fromState;
        this.toState = toState;
        this.event = event;
        this.condition = condition;
        this.action = action;
    }

    @Override
    public Optional<Long> getId() {
        return id;
    }

    // Getters for other fields might be useful later
} 