package ssot_parser;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import ssot_parser.ast.AstNode;
import ssot_parser.ast.nodes.FieldNode;

/** Represents a type definition */
public class TypeDefNode implements NodeWithId {
    private final Optional<Long> id;
    public final String name;
    public final List<FieldNode> fields;
    private final Map<String, Object> annotations;

    public TypeDefNode(Optional<Long> id, String name, List<FieldNode> fields, Map<String, Object> annotations) {
        this.id = id;
        this.name = name;
        this.fields = fields;
        this.annotations = Collections.unmodifiableMap(annotations != null ? new HashMap<>(annotations) : Collections.emptyMap());
    }

    @Override
    public Optional<Long> getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<FieldNode> getFields() {
        return fields;
    }

    @Override
    public Map<String, Object> getAnnotations() {
        return annotations;
    }
} 