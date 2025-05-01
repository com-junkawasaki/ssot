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
import ssot_parser.ast.nodes.AnnotationNode;
import ssot_parser.ast.type.TypeExprNode;

/**
 * Represents a parameter definition within a method signature.
 * Corresponds to the 'parameter' rule (or similar) in the grammar.
 */
public class ParameterNode implements AstNode, NodeWithId {

    private final Optional<Long> id;
    private final List<AnnotationNode> annotations;
    private final String name;
    private final TypeExprNode type;

    public ParameterNode(Optional<Long> id, List<AnnotationNode> annotations, String name, TypeExprNode type) {
        this.id = id;
        this.annotations = Collections.unmodifiableList(annotations != null ? new ArrayList<>(annotations) : Collections.emptyList());
        this.name = name;
        this.type = type;
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

    public TypeExprNode getType() {
        return type;
    }

    @Override
    public String toString() {
        return "ParameterNode{" +
               "id=" + id.map(String::valueOf).orElse("none") +
               ", name='" + name + '\'' +
               ", type=" + type.toString() +
               ", annotations=" + annotations +
               '}';
    }

    // Implementation for AstNode
    @Override
    public <T> T accept(NodeVisitor<T> visitor) {
        // TODO: Add visitParameterNode method to NodeVisitor interface
        // return visitor.visitParameterNode(this);
        System.err.println("Warning: NodeVisitor.visitParameterNode not implemented yet.");
        return null; // Placeholder return
    }

    // equals/hashCode omitted
} 