package ssot_parser;

import java.util.Collections;
import java.util.List;

/**
 * Represents the root node of the SSoT Abstract Syntax Tree (AST).
 * It typically holds lists of top-level definitions like types, services, machines, etc.
 */
public class SsotRoot implements AstNode {

    // Example fields - these should correspond to the top-level blocks in your DSL
    private final List<AstNode> typeDefinitions; // Replace AstNode with specific type definition node class later
    private final List<AstNode> serviceDefinitions; // Replace AstNode with specific service definition node class
    private final List<AstNode> machineDefinitions; // Replace AstNode with specific machine definition node class
    // Add other top-level block lists as needed (imports, actors, communication, etc.)

    // Constructor (modify as needed based on how AstBuilderVisitor works)
    public SsotRoot(
            List<AstNode> typeDefinitions,
            List<AstNode> serviceDefinitions,
            List<AstNode> machineDefinitions
            /* Add other lists */
            ) {
        // Use defensive copying or immutable lists for robustness
        this.typeDefinitions = typeDefinitions != null ? List.copyOf(typeDefinitions) : Collections.emptyList();
        this.serviceDefinitions = serviceDefinitions != null ? List.copyOf(serviceDefinitions) : Collections.emptyList();
        this.machineDefinitions = machineDefinitions != null ? List.copyOf(machineDefinitions) : Collections.emptyList();
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

    // Implement other methods from AstNode if defined (e.g., accept, getChildren)

    @Override
    public String toString() {
        // Basic toString for debugging
        return "SsotRoot[types=" + typeDefinitions.size() +
               ", services=" + serviceDefinitions.size() +
               ", machines=" + machineDefinitions.size() +
               /* Add other counts */
               "]";
    }
} 