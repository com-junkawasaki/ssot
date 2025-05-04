package ssot_parser.ast.nodes;

import ssot_parser.ast.AstNode;
import ssot_parser.ast.NodeVisitor;

import java.util.Collections;
import java.util.List;
import java.util.ArrayList;

/**
 * Represents the 'context { ... }' block within a machine definition.
 * Contains the fields defining the machine's extended state.
 */
public class ContextNode implements AstNode {
    public final List<FieldNode> fields;

    public ContextNode(List<FieldNode> fields) {
        this.fields = fields != null ? Collections.unmodifiableList(new ArrayList<>(fields)) : Collections.emptyList();
    }

    public List<FieldNode> getFields() {
        return fields;
    }

    @Override
    public <T> T accept(NodeVisitor<T> visitor) {
        return visitor.visitContextNode(this);
    }

    @Override
    public String toString() {
        return "ContextNode{" +
               "fields=" + fields +
               '}';
    }

    // Consider adding equals() and hashCode()
}