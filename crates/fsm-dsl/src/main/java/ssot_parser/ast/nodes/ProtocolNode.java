package ssot_parser.ast.nodes;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import ssot_parser.ast.AstNode;

/**
 * Represents a protocol definition in the AST.
 */
public class ProtocolNode implements AstNode {

    private final Optional<Long> id;
    private final String name;
    private final Map<String, Object> annotations;
    // TODO: Add fields relevant to a protocol (e.g., message types, direction?)

    public ProtocolNode(Optional<Long> id, String name, Map<String, Object> annotations) {
        this.id = id;
        this.name = Objects.requireNonNull(name, "Protocol name cannot be null");
        this.annotations = Objects.requireNonNull(annotations, "Protocol annotations cannot be null");
    }

    public Optional<Long> getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    @Override
    public Map<String, Object> getAnnotations() {
        return annotations;
    }

    @Override
    public String toString() {
        return "ProtocolNode{" +
               "id=" + id +
               ", name='" + name + '\'' +
               ", annotations=" + annotations +
               '}';
    }
    // equals/hashCode omitted
} 