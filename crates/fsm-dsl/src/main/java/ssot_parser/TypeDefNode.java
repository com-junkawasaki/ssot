package ssot_parser;

import java.util.List;
import java.util.Optional;

/** Represents a type definition */
public class TypeDefNode implements NodeWithId {
    private final Optional<Long> id;
    public final String name;
    public final List<FieldNode> fields;

    public TypeDefNode(Optional<Long> id, String name, List<FieldNode> fields) {
        this.id = id;
        this.name = name;
        this.fields = fields;
    }

    @Override
    public Optional<Long> getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<FieldNode> getFields() {
        return fields;
    }
} 