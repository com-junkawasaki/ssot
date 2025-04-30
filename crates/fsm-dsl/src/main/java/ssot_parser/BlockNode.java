package ssot_parser;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Represents a generic block (e.g., types {}, machines {}) containing definitions */
public class BlockNode implements NodeWithId {
    private final Optional<Long> id;
    public final String blockType; // e.g., "types", "machines"
    public final List<AstNode> definitions;
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
} 