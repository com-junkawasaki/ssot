package ssot_parser.ast.nodes; // Corrected package

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import ssot_parser.ast.AstNode;
import ssot_parser.NodeWithId; // Import from parent
import ssot_parser.ast.NodeVisitor;

/** Represents a generic block (e.g., types {}, machines {}) containing definitions */
public class BlockNode implements NodeWithId {
    private final Optional<Long> id;
    public final String blockType; // e.g., "types", "machines"
    public final List<AstNode> definitions; // Definitions can be various node types
    private final Map<String, Object> annotations;


    public BlockNode(Optional<Long> id, String blockType, List<AstNode> definitions, Map<String, Object> annotations) {
        this.id = id;
        this.blockType = blockType;
        // Ensure definitions list is unmodifiable
        this.definitions = Collections.unmodifiableList(definitions != null ? definitions : Collections.emptyList());
        this.annotations = Collections.unmodifiableMap(annotations != null ? new HashMap<>(annotations) : Collections.emptyMap());
    }

    @Override
    public Optional<Long> getId() {
        return id;
    }

    public String getBlockType() {
        return blockType;
    }

    public List<AstNode> getDefinitions() {
        return definitions;
    }

    @Override
    public Map<String, Object> getAnnotations() {
        return annotations;
    }

    @Override
    public String toString() {
        return "BlockNode{" +
                "id=" + id +
                ", blockType='" + blockType + "\'" + // Escaped quote
                ", definitions=" + definitions +
                ", annotations=" + annotations +
                "}";
    }

    // Implementation for AstNode
    @Override
    public <T> T accept(NodeVisitor<T> visitor) {
        // How to visit a generic block?
        // Maybe delegate to children or have specific visitBlockNode?
        // Consider adding visitBlockNode to NodeVisitor interface if needed.
        throw new UnsupportedOperationException("Accept not implemented for BlockNode yet.");
    }
} 