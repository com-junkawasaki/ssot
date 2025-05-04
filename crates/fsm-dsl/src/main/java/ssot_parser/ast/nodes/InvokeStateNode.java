package ssot_parser.ast.nodes;

import ssot_parser.ast.AstNode;
import ssot_parser.NodeWithId;
import ssot_parser.ast.NodeVisitor;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.HashMap;
import java.util.ArrayList;

// Define InvokeCompletionHandler here or import if defined elsewhere
// For simplicity, defining it nested here temporarily.
// In AstBuilderVisitor, it was defined as a static nested class.
// Best practice: Define InvokeCompletionHandler as a top-level class in its own file.
// Assuming InvokeCompletionHandler exists as defined in AstBuilderVisitor.
// import ssot_parser.ast.nodes.handlers.InvokeCompletionHandler; // Example if moved

/**
 * Represents an invocation of an external service or machine within a state definition.
 * Links to an InvokeDefinitionNode and specifies completion handlers (onDone, onError).
 * Corresponds to the 'invoke ...' rule inside a state body.
 */
public class InvokeStateNode implements AstNode, NodeWithId {
    private final Optional<Long> id; // ID specific to this invocation instance in the state
    public final String invokeDefinitionRef; // Reference to the name in the machine's 'invokes' block
    public final Map<String, AstBuilderVisitor.ValueNode> inputMapping; // Simplified input mapping for now
    public final Optional<AstBuilderVisitor.InvokeCompletionHandler> onDoneHandler; // Optional handler for success
    public final Optional<AstBuilderVisitor.InvokeCompletionHandler> onErrorHandler; // Optional handler for failure
    private final List<AnnotationNode> annotations; // Annotations on the invoke line itself

    public InvokeStateNode(Optional<Long> id,
                           String invokeDefinitionRef,
                           Map<String, AstBuilderVisitor.ValueNode> inputMapping, // Use ValueNode or a dedicated mapping node
                           Optional<AstBuilderVisitor.InvokeCompletionHandler> onDoneHandler,
                           Optional<AstBuilderVisitor.InvokeCompletionHandler> onErrorHandler,
                           List<AnnotationNode> annotations) {
        this.id = id;
        this.invokeDefinitionRef = invokeDefinitionRef;
        this.inputMapping = inputMapping != null ? Collections.unmodifiableMap(new HashMap<>(inputMapping)) : Collections.emptyMap();
        this.onDoneHandler = onDoneHandler;
        this.onErrorHandler = onErrorHandler;
        this.annotations = Collections.unmodifiableList(annotations != null ? new ArrayList<>(annotations) : Collections.emptyList());
    }

    @Override
    public Optional<Long> getId() {
        return id;
    }

    public String getInvokeDefinitionRef() {
        return invokeDefinitionRef;
    }

    public Map<String, AstBuilderVisitor.ValueNode> getInputMapping() {
        return inputMapping;
    }

    public Optional<AstBuilderVisitor.InvokeCompletionHandler> getOnDoneHandler() {
        return onDoneHandler;
    }

    public Optional<AstBuilderVisitor.InvokeCompletionHandler> getOnErrorHandler() {
        return onErrorHandler;
    }

    @Override
    public Map<String, Object> getAnnotations() {
        Map<String, Object> annotationMap = new HashMap<>();
        if (this.annotations != null) {
            for (AnnotationNode annotation : this.annotations) {
                // Exclude @id if getId() is used?
                annotationMap.put(annotation.name, annotation.value);
            }
        }
        return Collections.unmodifiableMap(annotationMap);
    }

    @Override
    public <T> T accept(NodeVisitor<T> visitor) {
        return visitor.visitInvokeStateNode(this);
    }

    @Override
    public String toString() {
        return "InvokeStateNode{" +
               "id=" + id.map(String::valueOf).orElse("none") +
               ", invokeDefinitionRef='" + invokeDefinitionRef + "\'" +
               ", inputMapping=" + inputMapping +
               ", onDoneHandler=" + onDoneHandler.map(Object::toString).orElse("none") +
               ", onErrorHandler=" + onErrorHandler.map(Object::toString).orElse("none") +
               ", annotations=" + annotations +
               "}";
    }

    // Consider adding equals() and hashCode()
} 