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
    private final List<AnnotationNode> annotations; // Keep internal representation as List

    public TypeDefNode(Optional<Long> id, String name, List<FieldNode> fields, List<AnnotationNode> annotations) {
        this.id = id;
        this.name = name;
        this.fields = fields;
        this.annotations = Collections.unmodifiableList(annotations != null ? new ArrayList<>(annotations) : Collections.emptyList()); // Use unmodifiableList
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
        Map<String, Object> annotationMap = new HashMap<>();
        for (AnnotationNode annotation : this.annotations) {
            annotationMap.put(annotation.name, annotation.value); // Use name and value from AnnotationNode
        }
        return Collections.unmodifiableMap(annotationMap); // Return unmodifiable map
    }

    // Implementation for AstNode
    @Override
    public <T> T accept(NodeVisitor<T> visitor) {
        return visitor.visitTypeDefNode(this);
    }
} 