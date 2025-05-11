package ssot_parser.ast.nodes;

import ssot_parser.ast.AstNode;
import ssot_parser.NodeWithId;
import ssot_parser.ast.NodeVisitor;
import ssot_parser.ast.values.ValueNode;
// import ssot_parser.ast.handlers.InvokeCompletionHandler; // Old import
import ssot_parser.ast.nodes.InvokeCompletionHandlerNode; // New import

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
    public final Map<String, ValueNode> inputMapping; // Use the imported ValueNode
    public final Optional<InvokeCompletionHandlerNode> onDoneHandler; // Use the new Node type
    public final Optional<InvokeCompletionHandlerNode> onErrorHandler; // Use the new Node type
    private final List<AnnotationNode> annotations; // Annotations on the invoke line itself

    public InvokeStateNode(Optional<Long> id,
                           String invokeDefinitionRef,
                           Map<String, ValueNode> inputMapping, // Use imported ValueNode
                           Optional<InvokeCompletionHandlerNode> onDoneHandler, // Use new Node type
                           Optional<InvokeCompletionHandlerNode> onErrorHandler, // Use new Node type
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

    public Map<String, ValueNode> getInputMapping() { // Return type uses imported ValueNode
        return inputMapping;
    }

    public Optional<InvokeCompletionHandlerNode> getOnDoneHandler() { // Return type uses new Node type
        return onDoneHandler;
    }

    public Optional<InvokeCompletionHandlerNode> getOnErrorHandler() { // Return type uses new Node type
        return onErrorHandler;
    }

    public String getSrc() {
        return this.invokeDefinitionRef;
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
               ", onDoneHandler=" + onDoneHandler.map(InvokeCompletionHandlerNode::toString).orElse("none") + // Use new Node type for toString
               ", onErrorHandler=" + onErrorHandler.map(InvokeCompletionHandlerNode::toString).orElse("none") + // Use new Node type for toString
               ", annotations=" + annotations +
               "}";
    }

    // Consider adding equals() and hashCode()
} 