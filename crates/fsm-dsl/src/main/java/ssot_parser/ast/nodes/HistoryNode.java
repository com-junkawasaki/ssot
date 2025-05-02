package ssot_parser.ast.nodes;

import ssot_parser.ast.AstNode;
import ssot_parser.ast.NodeVisitor;
import ssot_parser.NodeWithId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.HashMap;

/**
 * Represents a history state definition within a state.
 * Corresponds to the 'historyDefinition' rule in the grammar.
 */
public class HistoryNode implements AstNode, NodeWithId {

    public enum HistoryType { SHALLOW, DEEP }

    private final Optional<Long> id;
    private final List<AnnotationNode> annotations;
    public final HistoryType historyType;
    public final String historyStateName;
    public final boolean isDeep;

    public HistoryNode(Optional<Long> id, List<AnnotationNode> annotations, String historyStateName, HistoryType historyType) {
        this.id = id;
        this.annotations = Collections.unmodifiableList(annotations != null ? new ArrayList<>(annotations) : Collections.emptyList());
        this.historyStateName = historyStateName;
        this.historyType = historyType;
        this.isDeep = (historyType == HistoryType.DEEP);
    }

    @Override
    public Optional<Long> getId() {
        return id;
    }

    @Override
    public Map<String, Object> getAnnotations() {
        Map<String, Object> annotationMap = new HashMap<>();
        for (AnnotationNode annotation : this.annotations) {
            annotationMap.put(annotation.name, annotation.value);
        }
        return Collections.unmodifiableMap(annotationMap);
    }

    public HistoryType getHistoryType() {
        return historyType;
    }

    public String getHistoryStateName() {
        return historyStateName;
    }

    public boolean isDeep() {
        return isDeep;
    }

    @Override
    public <T> T accept(NodeVisitor<T> visitor) {
        return visitor.visitHistoryNode(this);
    }

    @Override
    public String toString() {
        return "HistoryNode{" +
               "id=" + id.map(String::valueOf).orElse("none") +
               ", name='" + historyStateName + "\'" +
               ", type=" + historyType +
               ", annotations=" + annotations +
               '}';
    }

    // Consider adding equals() and hashCode()
} 