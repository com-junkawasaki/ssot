package ssot_parser.ast.nodes;

import ssot_parser.ast.AstNode;
import ssot_parser.ast.NodeVisitor;
import java.util.Map;
import java.util.Optional;
import java.util.Collections;

public class DurationNode implements AstNode {
    private final long value;
    private final String unit; // "ms", "s", "m", "h"
    private final Optional<Long> id;
    private final Map<String, Object> annotations;

    public DurationNode(Optional<Long> id, long value, String unit, Map<String, Object> annotations) {
        this.id = id;
        this.value = value;
        this.unit = unit;
        this.annotations = Collections.unmodifiableMap(annotations != null ? annotations : Collections.emptyMap());
    }

    public DurationNode(long value, String unit) {
        this(Optional.empty(), value, unit, Collections.emptyMap());
    }

    public long getValue() {
        return value;
    }

    public String getUnit() {
        return unit;
    }

    @Override
    public Optional<Long> getId() {
        return id;
    }

    @Override
    public Map<String, Object> getAnnotations() {
        return annotations;
    }

    @Override
    public <T> T accept(NodeVisitor<T> visitor) {
        // return visitor.visitDurationNode(this); // Visitor may need this method
        return null;
    }

    @Override
    public String toString() {
        return "Duration:" + value + unit;
    }
} 