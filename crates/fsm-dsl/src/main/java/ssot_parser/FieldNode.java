package ssot_parser;

import java.util.Optional;

/** Represents a field within a type definition */
public class FieldNode implements NodeWithId {
    private final Optional<Long> id;
    public final String name;
    public final String type;

    public FieldNode(Optional<Long> id, String name, String type) {
        this.id = id;
        this.name = name;
        this.type = type;
    }

    @Override
    public Optional<Long> getId() {
        return id;
    }
} 