package ssot_parser.ast.nodes;

import ssot_parser.ast.AstNode;
import ssot_parser.ast.NodeVisitor;
import java.util.Map;
import java.util.Optional;
import java.util.Collections;

public class GuardReferenceNode implements AstNode {
    public final String guardName;
    public final boolean isNegated;
    private final Map<String, Object> annotations; // For any annotations on the reference itself
    private final Optional<Long> id; // If a guard reference itself can have an @id

    public GuardReferenceNode(Optional<Long> id, String guardName, boolean isNegated, Map<String, Object> annotations) {
        this.id = id;
        this.guardName = guardName;
        this.isNegated = isNegated;
        this.annotations = Collections.unmodifiableMap(annotations != null ? annotations : Collections.emptyMap());
    }

    // Constructor without ID
    public GuardReferenceNode(String guardName, boolean isNegated, Map<String, Object> annotations) {
        this(Optional.empty(), guardName, isNegated, annotations);
    }

    public String getGuardName() {
        return guardName;
    }

    public boolean isNegated() {
        return isNegated;
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
        // return visitor.visitGuardReferenceNode(this); // Visitor needs this method
        return null;
    }

    @Override
    public String toString() {
        return "GuardRef:" + guardName + (isNegated ? "(not)" : "");
    }
} 