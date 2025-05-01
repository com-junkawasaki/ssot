package ssot_parser.ast.nodes;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import ssot_parser.ast.AstNode;
import ssot_parser.NodeWithId;
import ssot_parser.ast.NodeVisitor;

/** Represents a field within a type definition */
public class FieldNode implements NodeWithId {
    private final Optional<Long> id;
    public final String name;
    public final String type;
    private final Map<String, Object> annotations;

    public FieldNode(Optional<Long> id, String name, String type, Map<String, Object> annotations) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.annotations = Collections.unmodifiableMap(annotations != null ? new HashMap<>(annotations) : Collections.emptyMap());
    }

    @Override
    public Optional<Long> getId() {
        return id;
    }

    // Add getters for name and type if needed
    public String getName() { return name; }
    public String getType() { return type; }

    @Override
    public Map<String, Object> getAnnotations() {
        return annotations;
    }

    // Implementation for AstNode
    @Override
    public <T> T accept(NodeVisitor<T> visitor) {
        return visitor.visitFieldNode(this);
    }
} 