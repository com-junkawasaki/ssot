package ssot_parser.ast.nodes;

import ssot_parser.ast.AstNode;
import ssot_parser.ast.NodeVisitor;
import java.util.Map;
import java.util.Optional;
import java.util.Collections;

public class ActionReferenceNode implements AstNode {
    public final String actionName;
    private final Map<String, Object> annotations; // For any annotations on the reference itself
    private final Optional<Long> id; // If an action reference itself can have an @id

    public ActionReferenceNode(Optional<Long> id, String actionName, Map<String, Object> annotations) {
        this.id = id;
        this.actionName = actionName;
        this.annotations = Collections.unmodifiableMap(annotations != null ? annotations : Collections.emptyMap());
    }

    // Constructor without ID, if ID is not typical for references
    public ActionReferenceNode(String actionName, Map<String, Object> annotations) {
        this(Optional.empty(), actionName, annotations);
    }

    public String getActionName() {
        return actionName;
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
        // return visitor.visitActionReferenceNode(this); // Visitor needs this method
        return null;
    }

    @Override
    public String toString() {
        return "ActionRef:" + actionName;
    }
} 