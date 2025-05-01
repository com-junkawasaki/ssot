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

/**
 * Represents a variant (member) within an enum definition.
 * Corresponds to the 'enumVariant' rule in the grammar.
 */
public class EnumVariantNode implements AstNode, NodeWithId {

    private final Optional<Long> id;
    private final List<AnnotationNode> annotations;
    private final String name;

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
    public List<AnnotationNode> getAnnotations() {
        return annotations;
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
        // TODO: Add visitEnumVariantNode method to NodeVisitor interface
        // return visitor.visitEnumVariantNode(this);
        System.err.println("Warning: NodeVisitor.visitEnumVariantNode not implemented yet.");
        return null; // Placeholder return
    }
} 