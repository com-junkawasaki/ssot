package ssot_parser;

import java.util.List;
import java.util.Optional;

/** Represents a state definition */
public class StateNode implements NodeWithId {
    private final Optional<Long> id;
    public final String name;
    public final List<ActionNode> actions;

    public StateNode(Optional<Long> id, String name, List<ActionNode> actions) {
        this.id = id;
        this.name = name;
        this.actions = actions;
    }

    @Override
    public Optional<Long> getId() {
        return id;
    }
} 