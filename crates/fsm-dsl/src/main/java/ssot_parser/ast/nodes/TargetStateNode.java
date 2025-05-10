package ssot_parser.ast.nodes;

import ssot_parser.ast.AstNode;
import ssot_parser.ast.NodeVisitor;
import java.util.Map;
import java.util.Optional;
import java.util.Collections;

public class TargetStateNode implements AstNode {
    public enum TargetType {
        STATE_REFERENCE, // e.g., "StateName" or "Parent.Child"
        HISTORY_REFERENCE // e.g., ".history"
    }

    private final TargetType type;
    private final Optional<String> stateName; // Present if type is STATE_REFERENCE
    private final boolean isDeepHistory; // Relevant if type is HISTORY_REFERENCE, from historyDefinition
    private final Optional<Long> id; // If target spec itself can have @id, unlikely but for completeness
    private final Map<String, Object> annotations; // Annotations on the target spec, also unlikely

    // Constructor for named state target
    public TargetStateNode(Optional<Long> id, String stateName, Map<String, Object> annotations) {
        this.id = id;
        this.type = TargetType.STATE_REFERENCE;
        this.stateName = Optional.ofNullable(stateName);
        this.isDeepHistory = false;
        this.annotations = Collections.unmodifiableMap(annotations != null ? annotations : Collections.emptyMap());
    }
    // Simpler constructor for named state target
    public TargetStateNode(String stateName) {
        this(Optional.empty(), stateName, Collections.emptyMap());
    }

    // Constructor for history target
    public TargetStateNode(Optional<Long> id, boolean isDeepHistory, Map<String, Object> annotations) {
        this.id = id;
        this.type = TargetType.HISTORY_REFERENCE;
        this.stateName = Optional.empty();
        this.isDeepHistory = isDeepHistory;
        this.annotations = Collections.unmodifiableMap(annotations != null ? annotations : Collections.emptyMap());
    }
    // Simpler constructor for history target
    public TargetStateNode(boolean isDeepHistory) {
        this(Optional.empty(), isDeepHistory, Collections.emptyMap());
    }

    public TargetType getType() {
        return type;
    }

    public Optional<String> getStateName() {
        return stateName;
    }

    public boolean isDeepHistory() {
        return isDeepHistory;
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
        // return visitor.visitTargetStateNode(this); // Visitor needs this method
        return null;
    }

    @Override
    public String toString() {
        if (type == TargetType.STATE_REFERENCE) {
            return "TargetState:" + stateName.orElse("UNNAMED_STATE_REF");
        } else {
            return "TargetState:" + (isDeepHistory ? ".historyDeep" : ".history");
        }
    }
} 