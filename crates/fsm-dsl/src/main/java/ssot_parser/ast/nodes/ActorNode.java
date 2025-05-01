package ssot_parser.ast.nodes;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import ssot_parser.ast.AstNode;
import ssot_parser.ast.NodeVisitor;
import ssot_parser.ast.nodes.AnnotationNode;

/**
 * Represents an actor definition in the AST.
 */
public class ActorNode implements AstNode {

    private final Optional<Long> id;
    private final String name;
    private final List<AnnotationNode> annotations;
    // TODO: Add more fields based on actor grammar (e.g., properties, interfaces implemented?)

    public ActorNode(Optional<Long> id, String name, List<AnnotationNode> annotations) {
        this.id = id;
        this.name = Objects.requireNonNull(name, "Actor name cannot be null");
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
        return "ActorNode{" +
               "id=" + id +
               ", name='" + name + '\'' +
               ", annotations=" + annotations +
               '}';
    }

    // equals and hashCode omitted for brevity

    // Implementation for AstNode
    @Override
    public <T> T accept(NodeVisitor<T> visitor) {
        return visitor.visitActorNode(this);
    }
} 