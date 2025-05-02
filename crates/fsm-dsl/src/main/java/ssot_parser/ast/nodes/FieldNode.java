package ssot_parser.ast.nodes;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.List;
import java.util.ArrayList;
import ssot_parser.ast.AstNode;
import ssot_parser.NodeWithId;
import ssot_parser.ast.NodeVisitor;
import ssot_parser.ast.nodes.AnnotationNode;
import ssot_parser.ast.type.TypeExprNode;

/** Represents a field within a type definition */
public class FieldNode implements NodeWithId, AstNode {
    private final Optional<Long> id;
    public final String name;
    public final TypeExprNode type;
    private final List<AnnotationNode> annotations;

    public FieldNode(Optional<Long> id, String name, TypeExprNode type, List<AnnotationNode> annotations) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.annotations = Collections.unmodifiableList(annotations != null ? new ArrayList<>(annotations) : Collections.emptyList());
    }

    @Override
    public Optional<Long> getId() {
        return id;
    }

    // Add getters for name and type if needed
    public String getName() { return name; }
    public TypeExprNode getType() { return type; }

    @Override
    public Map<String, Object> getAnnotations() {
        Map<String, Object> annotationMap = new HashMap<>();
        for (AnnotationNode annotation : this.annotations) {
            annotationMap.put(annotation.name, annotation.value);
        }
        return Collections.unmodifiableMap(annotationMap);
    }

    // Implementation for AstNode
    @Override
    public <T> T accept(NodeVisitor<T> visitor) {
        return visitor.visitFieldNode(this);
    }

    // Optional: Add toString, equals, hashCode if needed
    @Override
    public String toString() {
        return "FieldNode{" +
               "id=" + id.map(String::valueOf).orElse("none") +
               ", name='" + name + '\'' +
               ", type=" + type.toString() +
               ", annotations=" + annotations +
               '}';
    }
} 