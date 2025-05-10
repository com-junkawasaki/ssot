package ssot_parser.ast.type;

import ssot_parser.ast.NodeVisitor;
import ssot_parser.ast.nodes.AnnotationNode; // Required for AstNode base if it includes annotations
import java.util.List;
import java.util.Collections;
import java.util.Optional;
import java.util.Map; // For AstNode getAnnotations

/**
 * Represents a primitive type in the DSL (e.g., string, bool, u32, i64, f64, timestamp).
 */
public class PrimitiveTypeNode implements TypeExprNode {

    public enum PrimitiveType {
        U8("u8"), U16("u16"), U32("u32"), U64("u64"),
        I8("i8"), I16("i16"), I32("i32"), I64("i64"),
        F32("f32"), F64("f64"),
        BOOL("bool"), STRING("string"), TIMESTAMP("timestamp");

        private final String dslName;

        PrimitiveType(String dslName) {
            this.dslName = dslName;
        }

        public String getDslName() {
            return dslName;
        }

        public static PrimitiveType fromString(String text) {
            for (PrimitiveType b : PrimitiveType.values()) {
                if (b.dslName.equalsIgnoreCase(text)) {
                    return b;
                }
            }
            throw new IllegalArgumentException("No constant with text " + text + " found");
        }
    }

    private final PrimitiveType primitiveType;

    public PrimitiveTypeNode(PrimitiveType primitiveType) {
        this.primitiveType = primitiveType;
    }

    public PrimitiveType getPrimitiveType() {
        return primitiveType;
    }

    public String getTypeName() { // Keep for compatibility or direct DSL name access
        return primitiveType.getDslName();
    }

    @Override
    public <T> T accept(NodeVisitor<T> visitor) {
        return visitor.visitPrimitiveTypeNode(this);
    }

    @Override
    public Map<String, Object> getAnnotations() { // Changed from List<AnnotationNode>
        return Collections.emptyMap(); // Primitive types typically don't have annotations
    }

    @Override
     public Optional<Long> getId() {
         return Optional.empty(); // Primitive types typically don't have IDs
     }

    @Override
    public String toString() {
        return primitiveType.getDslName();
    }

     @Override
     public boolean equals(Object o) {
         if (this == o) return true;
         if (o == null || getClass() != o.getClass()) return false;
         PrimitiveTypeNode that = (PrimitiveTypeNode) o;
         return primitiveType == that.primitiveType;
     }

     @Override
     public int hashCode() {
         return java.util.Objects.hash(primitiveType);
     }
} 