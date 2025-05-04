package ssot_parser.ast.nodes;

import ssot_parser.ast.AstNode;
import ssot_parser.NodeWithId;
import ssot_parser.ast.NodeVisitor;
import java.util.Map;
import java.util.Optional;
import java.util.List;
import java.util.Collections;
import java.util.HashMap;
import java.util.ArrayList;

/**
 * Represents an invocation definition within the 'invokes { ... }' block of a machine.
 * Defines an external call (service, promise, etc.) that can be invoked by states.
 * Corresponds to the 'invokeDefinition' rule.
 */
public class InvokeDefinitionNode implements AstNode, NodeWithId {
    private final Optional<Long> id;
    public final String invokeName;
    public final String source; // e.g., "UserProfileService.fetchProfile" or "backgroundTaskName"
    // Input mapping details might need a dedicated structure later.
    // onDone/onError definitions are part of the invoke definition in the DSL, specifying
    // the default behavior when this invoke is called, though they are specified again
    // (and potentially overridden) at the state invoke site.
    // For simplicity in the AST definition node, we might omit them here, as the crucial
    // part is what happens *when invoked by a specific state*.
    // Alternatively, we store the *default* handlers here if specified in the `invokes` block.
    // Let's omit them for now from the definition node and handle them only in the state's invoke node.
    private final List<AnnotationNode> annotations;

    public InvokeDefinitionNode(Optional<Long> id, String invokeName, String source, List<AnnotationNode> annotations) {
        this.id = id;
        this.invokeName = invokeName;
        this.source = source;
        this.annotations = Collections.unmodifiableList(annotations != null ? new ArrayList<>(annotations) : Collections.emptyList());
    }

    @Override
    public Optional<Long> getId() {
        return id;
    }

    public String getInvokeName() {
        return invokeName;
    }

    public String getSource() {
        return source;
    }

    @Override
    public Map<String, Object> getAnnotations() {
        Map<String, Object> annotationMap = new HashMap<>();
        if (this.annotations != null) {
            for (AnnotationNode annotation : this.annotations) {
                annotationMap.put(annotation.name, annotation.value);
            }
        }
        return Collections.unmodifiableMap(annotationMap);
    }

    @Override
    public <T> T accept(NodeVisitor<T> visitor) {
        return visitor.visitInvokeDefinitionNode(this);
    }

    @Override
    public String toString() {
        return "InvokeDefinitionNode{" +
               "id=" + id.map(String::valueOf).orElse("none") +
               ", invokeName='" + invokeName + "\'" +
               ", source='" + source + "\'" +
               ", annotations=" + annotations +
               "}";
    }

    // Consider adding equals() and hashCode()
} 