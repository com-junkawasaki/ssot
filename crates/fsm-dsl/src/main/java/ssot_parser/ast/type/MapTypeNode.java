package ssot_parser.ast.type;

import ssot_parser.ast.NodeVisitor;
import ssot_parser.ast.nodes.AnnotationNode;
import java.util.List;
import java.util.Collections;
import java.util.Optional;
import java.util.Objects;

/**
 * Represents a map type 'map<K, V>'.
 */
public class MapTypeNode implements TypeExprNode {

    private final TypeExprNode keyType;
    private final TypeExprNode valueType;

    public MapTypeNode(TypeExprNode keyType, TypeExprNode valueType) {
        this.keyType = Objects.requireNonNull(keyType, "Key type for map cannot be null");
        this.valueType = Objects.requireNonNull(valueType, "Value type for map cannot be null");
    }

    public TypeExprNode getKeyType() {
        return keyType;
    }

    public TypeExprNode getValueType() {
        return valueType;
    }

    @Override
    public <T> T accept(NodeVisitor<T> visitor) {
        // TODO: Add visitMapTypeNode to NodeVisitor
        // return visitor.visitMapTypeNode(this);
        System.err.println("Warning: NodeVisitor.visitMapTypeNode not implemented yet.");
        return null;
    }

    // AstNode requirements
    @Override
    public java.util.Map<String, Object> getAnnotations() {
        return Collections.emptyMap();
    }
    @Override
     public Optional<Long> getId() {
         return Optional.empty();
     }

    @Override
    public String toString() {
        return "map<" + keyType.toString() + ", " + valueType.toString() + ">";
    }

     @Override
     public boolean equals(Object o) {
         if (this == o) return true;
         if (o == null || getClass() != o.getClass()) return false;
         MapTypeNode that = (MapTypeNode) o;
         return Objects.equals(keyType, that.keyType) && Objects.equals(valueType, that.valueType);
     }

     @Override
     public int hashCode() {
         return Objects.hash(keyType, valueType);
     }
} 