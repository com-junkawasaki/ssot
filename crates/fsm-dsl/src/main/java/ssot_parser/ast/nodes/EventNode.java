package ssot_parser.ast.nodes;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import ssot_parser.ast.AstNode;

/**
 * Represents an event definition, potentially similar to a struct.
 */
public class EventNode implements AstNode {

    private final Optional<Long> id;
    private final String name;
    private final List<FieldNode> fields; // Assuming events have fields like structs
    private final Map<String, Object> annotations;

    public EventNode(Optional<Long> id, String name, List<FieldNode> fields, Map<String, Object> annotations) {
        this.id = id;
        this.name = Objects.requireNonNull(name, "Event name cannot be null");
        this.fields = Objects.requireNonNull(fields, "Event fields cannot be null");
        this.annotations = Objects.requireNonNull(annotations, "Event annotations cannot be null");
    }

    public Optional<Long> getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<FieldNode> getFields() {
        return fields;
    }

    @Override
    public Map<String, Object> getAnnotations() {
        return annotations;
    }

     @Override
    public String toString() {
        return "EventNode{" +
               "id=" + id +
               ", name='" + name + '\'' +
               ", fields=" + fields +
               ", annotations=" + annotations +
               '}';
    }
    // equals/hashCode omitted
} 