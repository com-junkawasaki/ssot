package ssot_parser.ast.nodes; // Corrected package

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.ArrayList; // For initializing List
import ssot_parser.ast.AstNode;
import ssot_parser.NodeWithId; // Import from parent
// import ssot_parser.ast.nodes.FieldNode; // FieldNode now in same package
import ssot_parser.ast.NodeVisitor;

// Import AnnotationNode
import ssot_parser.ast.nodes.AnnotationNode;
import ssot_parser.ast.nodes.EnumVariantNode; // Import EnumVariantNode
import ssot_parser.ast.nodes.FieldNode; // Import FieldNode

/** Represents a type definition (struct or enum) */
public class TypeDefNode implements NodeWithId, AstNode {
    public enum TypeKind {
        STRUCT,
        ENUM
    }

    private final Optional<Long> id;
    public final String name;
    public final TypeKind kind;
    public final List<FieldNode> fields; // Null if kind is ENUM
    public final List<EnumVariantNode> variants; // Null if kind is STRUCT
    private final Map<String, Object> annotations;

    // Constructor for Structs
    public TypeDefNode(Optional<Long> id, String name, List<FieldNode> fields, List<AnnotationNode> annotationsList) {
        this.id = id;
        this.name = Objects.requireNonNull(name, "Type name cannot be null");
        this.kind = TypeKind.STRUCT;
        this.fields = fields != null ? Collections.unmodifiableList(new ArrayList<>(fields)) : Collections.emptyList();
        this.variants = null; // Structs don't have variants
        this.annotations = mapAnnotations(annotationsList);
    }

    // Constructor for Enums
    public TypeDefNode(Optional<Long> id, String name, List<EnumVariantNode> variants, List<AnnotationNode> annotationsList, boolean isEnum /* dummy for signature diff */) {
        this.id = id;
        this.name = Objects.requireNonNull(name, "Type name cannot be null");
        this.kind = TypeKind.ENUM;
        this.fields = null; // Enums don't have fields in this model
        this.variants = variants != null ? Collections.unmodifiableList(new ArrayList<>(variants)) : Collections.emptyList();
        this.annotations = mapAnnotations(annotationsList);
    }
    
    // Common constructor used by AstBuilderVisitor, matching its call signature
    public TypeDefNode(Optional<Long> id, String name, TypeKind kind, List<FieldNode> fields, List<EnumVariantNode> variants, List<AnnotationNode> annotationsList) {
        this.id = id;
        this.name = Objects.requireNonNull(name, "Type name cannot be null");
        this.kind = Objects.requireNonNull(kind, "TypeKind cannot be null");
        
        if (kind == TypeKind.STRUCT) {
            this.fields = fields != null ? Collections.unmodifiableList(new ArrayList<>(fields)) : Collections.emptyList();
            this.variants = null;
        } else { // ENUM
            this.fields = null;
            this.variants = variants != null ? Collections.unmodifiableList(new ArrayList<>(variants)) : Collections.emptyList();
        }
        this.annotations = mapAnnotations(annotationsList);
    }

    private Map<String, Object> mapAnnotations(List<AnnotationNode> annotationNodes) {
        Map<String, Object> map = new HashMap<>();
        if (annotationNodes != null) {
            for (AnnotationNode annotation : annotationNodes) {
                 // Assuming AnnotationNode has public fields 'name' and 'value', and 'isIdAnnotation' method
                if (!annotation.isIdAnnotation()) { // Use the getter method
                    map.put(annotation.name, Optional.ofNullable(annotation.value).orElse(Boolean.TRUE));
                }
            }
        }
        return Collections.unmodifiableMap(map);
    }

    @Override
    public Optional<Long> getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public TypeKind getKind() {
        return kind;
    }

    public List<FieldNode> getFields() {
        if (kind != TypeKind.STRUCT) {
            // Or throw exception, or return empty list. Depends on desired strictness.
            System.err.println("Warning: Tried to getFields on a non-STRUCT TypeDefNode: " + name);
            return Collections.emptyList(); 
        }
        return fields;
    }

    public List<EnumVariantNode> getVariants() {
        if (kind != TypeKind.ENUM) {
            System.err.println("Warning: Tried to getVariants on a non-ENUM TypeDefNode: " + name);
            return Collections.emptyList();
        }
        return variants;
    }

    /**
     * Returns the annotations associated with this node as a Map.
     * The map keys are the annotation names (without the '$'),
     * and the values are the parsed annotation values.
     */
    @Override
    public Map<String, Object> getAnnotations() {
        return this.annotations;
    }

    // Implementation for AstNode
    @Override
    public <T> T accept(NodeVisitor<T> visitor) {
        return visitor.visitTypeDefNode(this);
    }

    @Override
    public String toString() {
        return "TypeDefNode{" +
               "id=" + id.map(String::valueOf).orElse("none") +
               ", name='" + name + "'" +
               ", kind=" + kind +
               (kind == TypeKind.STRUCT ? ", fields=" + fields.size() : ", variants=" + variants.size()) +
               ", annotations=" + annotations.size() +
               '}';
    }
} 