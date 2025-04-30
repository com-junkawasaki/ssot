package ssot_parser;

import java.util.Optional;

/** Represents an action within a state definition */
public class ActionNode implements NodeWithId {
    private final Optional<Long> id;
    public final String name; // Or could be the full action string

    public ActionNode(Optional<Long> id, String name) {
        this.id = id;
        this.name = name;
    }

    @Override
    public Optional<Long> getId() {
        return id;
    }
} 