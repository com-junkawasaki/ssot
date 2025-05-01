package ssot_parser.ast.nodes;

import ssot_parser.ast.AstNode;
import ssot_parser.ast.NodeVisitor;
import ssot_parser.NodeWithId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Represents a history state definition within a state.
 * Corresponds to the 'historyDefinition' rule in the grammar.
 */
public class HistoryNode implements AstNode, NodeWithId {

    public enum HistoryType { SHALLOW, DEEP }

    private final Optional<Long> id;
    private final List<AnnotationNode> annotations;
    private final HistoryType historyType;

    public HistoryNode(Optional<Long> id, List<AnnotationNode> annotations, HistoryType historyType) {
        this.id = id;
        this.annotations = Collections.unmodifiableList(annotations != null ? new ArrayList<>(annotations) : Collections.emptyList());
        this.historyType = historyType;
    }

    @Override
    public Optional<Long> getId() {
        return id;
    }

    @Override
    public List<AnnotationNode> getAnnotations() {
        return annotations;
    }

    public HistoryType getHistoryType() {
        return historyType;
    }

    @Override
    public <T> T accept(NodeVisitor<T> visitor) {
        // TODO: Add visitHistoryNode method to NodeVisitor interface
        // return visitor.visitHistoryNode(this);
        System.err.println("Warning: NodeVisitor.visitHistoryNode not implemented yet.");
        return null; // Placeholder return
    }

    @Override
    public String toString() {
        return "HistoryNode{" +
               "id=" + id.map(String::valueOf).orElse("none") +
               ", type=" + historyType +
               ", annotations=" + annotations +
               '}';
    }

    // Consider adding equals() and hashCode()
} 