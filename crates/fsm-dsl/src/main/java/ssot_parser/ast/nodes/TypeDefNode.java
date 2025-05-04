package ssot_parser.ast.nodes; // Corrected package

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ArrayList; // For initializing List
import ssot_parser.ast.AstNode;
import ssot_parser.NodeWithId; // Import from parent
// import ssot_parser.ast.nodes.FieldNode; // FieldNode now in same package
import ssot_parser.ast.NodeVisitor;

// Import AnnotationNode
import ssot_parser.ast.nodes.AnnotationNode;

/** Represents a type definition */
public class TypeDefNode implements NodeWithId, AstNode {
    private final Optional<Long> id;
    public final String name;
    public final List<FieldNode> fields;
    private final Map<String, Object> annotations; // Change type to Map

    public TypeDefNode(Optional<Long> id, String name, List<FieldNode> fields, Map<String, Object> annotations) {
        this.id = id;
        this.name = name;
        this.fields = fields;
        this.annotations = annotations != null ? Collections.unmodifiableMap(new HashMap<>(annotations)) : Collections.emptyMap();
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

    /**
     * Returns the annotations associated with this node as a Map.
     * The map keys are the annotation names (without the '$'),
     * and the values are the parsed annotation values.
     */
    @Override
    public Map<String, Object> getAnnotations() { // Correct return type
        return this.annotations;
    }

    // Implementation for AstNode
    @Override
    public <T> T accept(NodeVisitor<T> visitor) {
        return visitor.visitTypeDefNode(this);
    }
} 