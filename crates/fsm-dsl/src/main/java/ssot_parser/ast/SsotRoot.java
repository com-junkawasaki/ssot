package ssot_parser.ast;

import ssot_parser.ast.nodes.*;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Collections;
import ssot_parser.NodeWithId;

/**
 * Represents the root node of the entire parsed SSoT file.
 * Contains imports and top-level definition blocks.
 */
public class SsotRoot implements AstNode, NodeWithId {

    private final Optional<Long> fileId;
    private final Map<String, Object> annotations;
    private final List<ImportNode> imports;
    private final List<TypeDefNode> typeDefinitions; // Structs, Enums
    private final List<AstNode> serviceDefinitions; // Services, Interfaces
    private final List<MachineNode> machineDefinitions;
    private final List<ActorNode> actorDefinitions;
    private final List<AstNode> communicationDefinitions; // Events, Channels, Protocols
    // Add other definition lists as needed

    public SsotRoot(Optional<Long> fileId,
                    Map<String, Object> annotations,
                    List<ImportNode> imports,
                    List<TypeDefNode> typeDefinitions,
                    List<AstNode> serviceDefinitions,
                    List<MachineNode> machineDefinitions,
                    List<ActorNode> actorDefinitions,
                    List<AstNode> communicationDefinitions) {
        this.fileId = fileId;
        this.annotations = Collections.unmodifiableMap(annotations != null ? annotations : Collections.emptyMap());
        this.imports = Collections.unmodifiableList(imports != null ? imports : Collections.emptyList());
        this.typeDefinitions = Collections.unmodifiableList(typeDefinitions != null ? typeDefinitions : Collections.emptyList());
        this.serviceDefinitions = Collections.unmodifiableList(serviceDefinitions != null ? serviceDefinitions : Collections.emptyList());
        this.machineDefinitions = Collections.unmodifiableList(machineDefinitions != null ? machineDefinitions : Collections.emptyList());
        this.actorDefinitions = Collections.unmodifiableList(actorDefinitions != null ? actorDefinitions : Collections.emptyList());
        this.communicationDefinitions = Collections.unmodifiableList(communicationDefinitions != null ? communicationDefinitions : Collections.emptyList());
    }

    @Override
    public Optional<Long> getId() {
        return fileId;
    }

    public Map<String, Object> getAnnotations() {
        return annotations;
    }

    public List<ImportNode> getImports() {
        return imports;
    }

    public List<TypeDefNode> getTypeDefinitions() {
        return typeDefinitions;
    }

    public List<AstNode> getServiceDefinitions() {
        return serviceDefinitions;
    }

    public List<MachineNode> getMachineDefinitions() {
        return machineDefinitions;
    }

    public List<ActorNode> getActorDefinitions() {
        return actorDefinitions;
    }

    public List<AstNode> getCommunicationDefinitions() {
        return communicationDefinitions;
    }

    @Override
    public <T> T accept(NodeVisitor<T> visitor) {
        // Might need a specific visit method on the visitor interface
        // return visitor.visitSsotRoot(this);
        throw new UnsupportedOperationException("Visiting SsotRoot not implemented yet.");
    }

     @Override
    public String toString() {
        return "SsotRoot{" +
               "fileId=" + fileId.map(String::valueOf).orElse("none") +
               ", annotations=" + annotations.size() +
               ", imports=" + imports.size() +
               ", typeDefs=" + typeDefinitions.size() +
               ", serviceDefs=" + serviceDefinitions.size() +
               ", machineDefs=" + machineDefinitions.size() +
               ", actorDefs=" + actorDefinitions.size() +
               ", commDefs=" + communicationDefinitions.size() +
               '}';
    }
} 