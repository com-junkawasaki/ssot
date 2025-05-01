package ssot_parser.ast.type;

import ssot_parser.ast.NodeVisitor;
import ssot_parser.ast.nodes.AnnotationNode;
import java.util.List;
import java.util.Collections;
import java.util.Optional;
import java.util.Objects;

/**
 * Represents a list type 'list<T>'.
 */
public class ListTypeNode implements TypeExprNode {

    private final TypeExprNode elementType;

    public ListTypeNode(TypeExprNode elementType) {
        this.elementType = Objects.requireNonNull(elementType, "Element type for list cannot be null");
    }

    public TypeExprNode getElementType() {
        return elementType;
    }

    @Override
    public <T> T accept(NodeVisitor<T> visitor) {
        // TODO: Add visitListTypeNode to NodeVisitor
        // return visitor.visitListTypeNode(this);
         System.err.println("Warning: NodeVisitor.visitListTypeNode not implemented yet.");
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
        return "list<" + elementType.toString() + ">";
    }

     @Override
     public boolean equals(Object o) {
         if (this == o) return true;
         if (o == null || getClass() != o.getClass()) return false;
         ListTypeNode that = (ListTypeNode) o;
         return Objects.equals(elementType, that.elementType);
     }

     @Override
     public int hashCode() {
         return Objects.hash(elementType);
     }
} 