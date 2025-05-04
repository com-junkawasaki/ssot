package ssot_parser;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import ssot_parser.ast.AstNode;
import ssot_parser.ast.NodeVisitor;

/**
 * Represents the root node of the SSoT Abstract Syntax Tree (AST).
 * It typically holds lists of top-level definitions like types, services, machines, etc.
 */
public class SsotRoot implements AstNode {

    // Using List<AstNode> initially to handle potentially mixed content
    // before specific node types are fully defined or if the structure changes.
    // Consider changing to List<TypeDefNode>, List<ServiceDefNode>, etc., later
    // if the structure becomes fixed and validation ensures type correctness.
    private final List<AstNode> typeDefinitions;
    private final List<AstNode> serviceDefinitions; // Placeholder
    private final List<AstNode> machineDefinitions; // Placeholder
    // Add other top-level block lists as needed (imports, actors, communication, etc.)
    private final List<AstNode> actorDefinitions; // Added
    private final List<AstNode> communicationDefinitions; // Added
    private final Optional<Long> id; // Add Optional<Long> for file ID
    private final Map<String, Object> annotations; // For top-level file annotations

    // Constructor (modify as needed based on how AstBuilderVisitor works)
    public SsotRoot(
            List<AstNode> typeDefinitions,
            List<AstNode> serviceDefinitions,
            List<AstNode> machineDefinitions,
            List<AstNode> actorDefinitions, // Added
            List<AstNode> communicationDefinitions, // Added
            Optional<Long> id, // Add id parameter
            Map<String, Object> annotations
            /* Add other lists */
            ) {
        // Use unmodifiable lists for robustness
        this.typeDefinitions = Collections.unmodifiableList(typeDefinitions != null ? typeDefinitions : Collections.emptyList());
        this.serviceDefinitions = Collections.unmodifiableList(serviceDefinitions != null ? serviceDefinitions : Collections.emptyList());
        this.machineDefinitions = Collections.unmodifiableList(machineDefinitions != null ? machineDefinitions : Collections.emptyList());
        this.actorDefinitions = Collections.unmodifiableList(actorDefinitions != null ? actorDefinitions : Collections.emptyList()); // Added
        this.communicationDefinitions = Collections.unmodifiableList(communicationDefinitions != null ? communicationDefinitions : Collections.emptyList()); // Added
        this.id = id; // Store id
        this.annotations = Collections.unmodifiableMap(annotations != null ? new HashMap<>(annotations) : Collections.emptyMap());
        // Initialize other lists
    }

    // Getters for the definition lists
    public List<AstNode> getTypeDefinitions() {
        return typeDefinitions;
    }

    public List<AstNode> getServiceDefinitions() {
        return serviceDefinitions;
    }

    public List<AstNode> getMachineDefinitions() {
        return machineDefinitions;
    }

    public List<AstNode> getActorDefinitions() { // Added
        return actorDefinitions;
    }

    public List<AstNode> getCommunicationDefinitions() { // Added
        return communicationDefinitions;
    }

    // Add getter for ID if needed (consistent with NodeWithId, though SsotRoot might not implement it)
    public Optional<Long> getId() {
        return id;
    }

    @Override
    public Map<String, Object> getAnnotations() {
        return annotations;
    }

    // Implement other methods from AstNode if defined (e.g., accept, getChildren)

    @Override
    public String toString() {
        return "SsotRoot{" +
               "typeDefinitions=" + typeDefinitions + // May need better toString for AstNode lists
               ", serviceDefinitions=" + serviceDefinitions +
               ", machineDefinitions=" + machineDefinitions +
               ", actorDefinitions=" + actorDefinitions + // Added
               ", communicationDefinitions=" + communicationDefinitions + // Added
               ", id=" + id.map(String::valueOf).orElse("none") + // Add id to toString
               ", annotations=" + annotations +
               '}';
    }

    // Optional: Methods to access specific types of definitions if needed
    // e.g., public List<TypeDefNode> getParsedTypeDefs() { ... filter and cast ... }

    // Implementation for AstNode
    @Override
    public <T> T accept(NodeVisitor<T> visitor) {
        return visitor.visitSsotRoot(this);
    }
} 