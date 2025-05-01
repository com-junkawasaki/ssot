package ssot_parser.ast.nodes;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import ssot_parser.ast.AstNode;
import ssot_parser.ast.NodeVisitor;
import ssot_parser.ast.nodes.AnnotationNode;
import ssot_parser.NodeWithId;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Represents an event definition, potentially similar to a struct.
 */
public class EventNode implements AstNode, NodeWithId {

    private final Optional<Long> id;
    private final String name;
    private final List<FieldNode> fields; // Assuming events have fields like structs
    private final List<AnnotationNode> annotations;

    public EventNode(Optional<Long> id, String name, List<FieldNode> fields, List<AnnotationNode> annotations) {
        this.id = id;
        this.name = Objects.requireNonNull(name, "Event name cannot be null");
        this.fields = Objects.requireNonNull(fields, "Event fields cannot be null");
        this.annotations = Collections.unmodifiableList(annotations != null ? new ArrayList<>(annotations) : Collections.emptyList());
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
    public List<AnnotationNode> getAnnotations() {
        return annotations;
    }

     @Override
    public String toString() {
        return "EventNode{" +
               "id=" + id.map(String::valueOf).orElse("none") +
               ", name='" + name + '\'' +
               ", fields=" + fields +
               ", annotations=" + annotations +
               '}';
    }

    // Implementation for AstNode
    @Override
    public <T> T accept(NodeVisitor<T> visitor) {
        System.err.println("Warning: NodeVisitor.visitEventNode not implemented yet.");
        return null;
    }

    // equals/hashCode omitted
} 