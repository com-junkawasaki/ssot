package ssot_parser.ast.nodes;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import ssot_parser.ast.AstNode;
import ssot_parser.ast.NodeVisitor;
import ssot_parser.NodeWithId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.HashMap;

/**
 * Represents a variant (member) within an enum definition.
 * Corresponds to the 'enumVariant' rule in the grammar.
 */
public class EnumVariantNode implements AstNode, NodeWithId {

    private final Optional<Long> id;
    private final List<AnnotationNode> annotations;
    public final String name;

    public EnumVariantNode(Optional<Long> id, List<AnnotationNode> annotations, String name) {
        this.id = id;
        this.annotations = Collections.unmodifiableList(annotations != null ? new ArrayList<>(annotations) : Collections.emptyList());
        this.name = name;
    }

    @Override
    public Optional<Long> getId() {
        return id;
    }

    @Override
    public Map<String, Object> getAnnotations() {
        Map<String, Object> annotationMap = new HashMap<>();
        for (AnnotationNode annotation : this.annotations) {
            annotationMap.put(annotation.name, annotation.value);
        }
        return Collections.unmodifiableMap(annotationMap);
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "EnumVariantNode{" +
               "id=" + id.map(String::valueOf).orElse("none") +
               ", name='" + name + '\'' +
               ", annotations=" + annotations +
               '}';
    }

    // Consider adding equals() and hashCode()

    // Implementation for AstNode
    @Override
    public <T> T accept(NodeVisitor<T> visitor) {
        return visitor.visitEnumVariantNode(this);
    }
} 