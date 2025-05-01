package ssot_parser.ast.nodes;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import ssot_parser.ast.AstNode;
import ssot_parser.ast.NodeVisitor;

/**
 * Represents an actor definition in the AST.
 */
public class ActorNode implements AstNode {

    private final Optional<Long> id;
    private final String name;
    private final Map<String, Object> annotations;
    // TODO: Add more fields based on actor grammar (e.g., properties, interfaces implemented?)

    public ActorNode(Optional<Long> id, String name, Map<String, Object> annotations) {
        this.id = id;
        this.name = Objects.requireNonNull(name, "Actor name cannot be null");
        this.annotations = Objects.requireNonNull(annotations, "Actor annotations cannot be null");
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