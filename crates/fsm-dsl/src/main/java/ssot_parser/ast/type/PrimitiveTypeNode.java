package ssot_parser.ast.type;

import ssot_parser.ast.NodeVisitor;
import ssot_parser.ast.nodes.AnnotationNode; // Required for AstNode base if it includes annotations
import java.util.List;
import java.util.Collections;
import java.util.Optional;

/**
 * Represents a primitive type in the DSL (e.g., string, bool, u32, i64, f64, timestamp).
 */
public class PrimitiveTypeNode implements TypeExprNode {

    // Consider using an enum for strictness, but String allows flexibility for unknown/future primitives.
    private final String typeName;

    public PrimitiveTypeNode(String typeName) {
        // Basic validation or normalization could happen here
        this.typeName = typeName;
    }

    public String getTypeName() {
        return typeName;
    }

    @Override
    public <T> T accept(NodeVisitor<T> visitor) {
        // TODO: Add visitPrimitiveTypeNode to NodeVisitor
        // return visitor.visitPrimitiveTypeNode(this);
         System.err.println("Warning: NodeVisitor.visitPrimitiveTypeNode not implemented yet.");
         return null;
    }

    // AstNode requires getAnnotations() if defined in the interface
    // If AstNode doesn't require it, these can be removed.
    @Override
    public List<AnnotationNode> getAnnotations() {
        return Collections.emptyList(); // Primitive types typically don't have annotations
    }
    @Override
     public Optional<Long> getId() {
         return Optional.empty(); // Primitive types typically don't have IDs
     }

    @Override
    public String toString() {
        return typeName;
    }

     @Override
     public boolean equals(Object o) {
         if (this == o) return true;
         if (o == null || getClass() != o.getClass()) return false;
         PrimitiveTypeNode that = (PrimitiveTypeNode) o;
         return java.util.Objects.equals(typeName, that.typeName);
     }

     @Override
     public int hashCode() {
         return java.util.Objects.hash(typeName);
     }
} 