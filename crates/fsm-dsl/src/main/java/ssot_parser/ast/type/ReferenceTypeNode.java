package ssot_parser.ast.type;

import ssot_parser.ast.NodeVisitor;
import ssot_parser.ast.nodes.AnnotationNode;
import java.util.List;
import java.util.Collections;
import java.util.Optional;
import java.util.Map;

/**
 * Represents a reference to another defined type (e.g., a struct or enum name).
 * This might be a simple ID or a qualified path like 'common.types.UserId'.
 */
public class ReferenceTypeNode implements TypeExprNode {

    private final String referencedTypeName; // Store the full name/path as parsed

    public ReferenceTypeNode(String referencedTypeName) {
        this.referencedTypeName = referencedTypeName;
    }

    public String getReferencedTypeName() {
        return referencedTypeName;
    }

    @Override
    public <T> T accept(NodeVisitor<T> visitor) {
        // TODO: Add visitReferenceTypeNode to NodeVisitor
        // return visitor.visitReferenceTypeNode(this);
        System.err.println("Warning: NodeVisitor.visitReferenceTypeNode not implemented yet.");
        return null;
    }

    // AstNode requirements
    @Override
    public Map<String, Object> getAnnotations() {
        return Collections.emptyMap();
    }
    @Override
     public Optional<Long> getId() {
         return Optional.empty();
     }

    @Override
    public String toString() {
        return referencedTypeName;
    }

     @Override
     public boolean equals(Object o) {
         if (this == o) return true;
         if (o == null || getClass() != o.getClass()) return false;
         ReferenceTypeNode that = (ReferenceTypeNode) o;
         return java.util.Objects.equals(referencedTypeName, that.referencedTypeName);
     }

     @Override
     public int hashCode() {
         return java.util.Objects.hash(referencedTypeName);
     }
} 