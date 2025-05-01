package ssot_parser.ast.nodes;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import ssot_parser.ast.AstNode;
import ssot_parser.ast.NodeVisitor;

/**
 * Represents a communication channel definition in the AST.
 */
public class ChannelNode implements AstNode {

    private final Optional<Long> id;
    private final String name;
    private final Map<String, Object> annotations;
    // TODO: Add fields relevant to a channel (e.g., type, participants, protocol?)

    public ChannelNode(Optional<Long> id, String name, Map<String, Object> annotations) {
        this.id = id;
        this.name = Objects.requireNonNull(name, "Channel name cannot be null");
        this.annotations = Objects.requireNonNull(annotations, "Channel annotations cannot be null");
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
        return "ChannelNode{" +
               "id=" + id +
               ", name='" + name + '\'' +
               ", annotations=" + annotations +
               '}';
    }

    // Implementation for AstNode
    @Override
    public <T> T accept(NodeVisitor<T> visitor) {
        return visitor.visitChannelNode(this);
    }

    // equals/hashCode omitted
} 