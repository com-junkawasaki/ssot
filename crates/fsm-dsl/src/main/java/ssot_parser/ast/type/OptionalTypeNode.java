package ssot_parser.ast.type;

import ssot_parser.ast.NodeVisitor;
import ssot_parser.ast.nodes.AnnotationNode;
import java.util.List;
import java.util.Collections;
import java.util.Optional;
import java.util.Objects;

/**
 * Represents an optional type 'optional<T>'.
 */
public class OptionalTypeNode implements TypeExprNode {

    private final TypeExprNode innerType;

    public OptionalTypeNode(TypeExprNode innerType) {
        this.innerType = Objects.requireNonNull(innerType, "Inner type for optional cannot be null");
    }

    public TypeExprNode getInnerType() {
        return innerType;
    }

    @Override
    public <T> T accept(NodeVisitor<T> visitor) {
        // TODO: Add visitOptionalTypeNode to NodeVisitor
        // return visitor.visitOptionalTypeNode(this);
        System.err.println("Warning: NodeVisitor.visitOptionalTypeNode not implemented yet.");
        return null;
    }

    // AstNode requirements
    @Override
    public List<AnnotationNode> getAnnotations() {
        return Collections.emptyList();
    }
     @Override
     public Optional<Long> getId() {
         return Optional.empty();
     }

    @Override
    public String toString() {
        return "optional<" + innerType.toString() + ">";
    }

     @Override
     public boolean equals(Object o) {
         if (this == o) return true;
         if (o == null || getClass() != o.getClass()) return false;
         OptionalTypeNode that = (OptionalTypeNode) o;
         return Objects.equals(innerType, that.innerType);
     }

     @Override
     public int hashCode() {
         return Objects.hash(innerType);
     }
} 