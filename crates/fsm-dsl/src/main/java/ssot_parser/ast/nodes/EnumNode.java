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

/**
 * Represents an enum type definition in the AST.
 */
public class EnumNode implements AstNode, TypeDefNode, NodeWithId { // Implement TypeDefNode for consistency?

    private final Optional<Long> id;
    private final List<AnnotationNode> annotations;
    private final String name;
    private final List<EnumVariantNode> variants;

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
    public List<AnnotationNode> getAnnotations() {
        return annotations;
    }

    @Override
    public String getName() {
        return name;
    }

    // Method required by TypeDefNode if implemented
    @Override
    public List<FieldNode> getFields() {
        return Collections.emptyList(); // Enums don't have fields like structs
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
        // TODO: Add visitEnumNode method to NodeVisitor interface
        // return visitor.visitEnumNode(this);
        System.err.println("Warning: NodeVisitor.visitEnumNode not implemented yet.");
        return null; // Placeholder return
    }
} 