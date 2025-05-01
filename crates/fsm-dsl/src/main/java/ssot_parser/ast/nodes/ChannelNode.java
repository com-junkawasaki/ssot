package ssot_parser.ast.nodes;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import ssot_parser.ast.AstNode;
import ssot_parser.ast.NodeVisitor;
import ssot_parser.ast.nodes.AnnotationNode;
import ssot_parser.NodeWithId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a communication channel definition in the AST.
 */
public class ChannelNode implements AstNode, NodeWithId {

    private final Optional<Long> id;
    private final String name;
    private final List<AnnotationNode> annotations;
    // TODO: Add fields relevant to a channel (e.g., type, participants, protocol?)

    public ChannelNode(Optional<Long> id, String name, List<AnnotationNode> annotations) {
        this.id = id;
        this.name = Objects.requireNonNull(name, "Channel name cannot be null");
        this.annotations = Collections.unmodifiableList(annotations != null ? new ArrayList<>(annotations) : Collections.emptyList());
    }

    public Optional<Long> getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    @Override
    public List<AnnotationNode> getAnnotations() {
        return annotations;
    }

     @Override
    public String toString() {
        return "ChannelNode{" +
               "id=" + id.map(String::valueOf).orElse("none") +
               ", name='" + name + '\'' +
               ", annotations=" + annotations +
               '}';
    }

    // Implementation for AstNode
    @Override
    public <T> T accept(NodeVisitor<T> visitor) {
        System.err.println("Warning: NodeVisitor.visitChannelNode not implemented yet.");
        return null;
    }

    // equals/hashCode omitted
} 