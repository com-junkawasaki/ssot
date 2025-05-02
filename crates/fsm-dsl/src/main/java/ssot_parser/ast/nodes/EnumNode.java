package ssot_parser.ast.nodes;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import ssot_parser.ast.AstNode;
import ssot_parser.ast.NodeVisitor;
import ssot_parser.NodeWithId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

/**
 * Represents an enum type definition in the AST.
 */
public class EnumNode implements AstNode, NodeWithId {

    private final Optional<Long> id;
    private final List<AnnotationNode> annotations;
    public final String name;
    public final List<EnumVariantNode> variants;

    public EnumNode(Optional<Long> id, List<AnnotationNode> annotations, String name, List<EnumVariantNode> variants) {
        this.id = id;
        this.annotations = Collections.unmodifiableList(annotations != null ? new ArrayList<>(annotations) : Collections.emptyList());
        this.name = name;
        this.variants = Collections.unmodifiableList(variants != null ? new ArrayList<>(variants) : Collections.emptyList());
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

    public List<EnumVariantNode> getVariants() {
        return variants;
    }

    @Override
    public String toString() {
        return "EnumNode{" +
               "id=" + id.map(String::valueOf).orElse("none") +
               ", name='" + name + '\'' +
               ", variants=" + variants +
               ", annotations=" + annotations +
               '}';
    }

    // equals and hashCode omitted for brevity

    // Implementation for AstNode
    @Override
    public <T> T accept(NodeVisitor<T> visitor) {
        return visitor.visitEnumNode(this);
    }
} 