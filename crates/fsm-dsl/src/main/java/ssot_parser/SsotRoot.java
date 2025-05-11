package ssot_parser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import ssot_parser.ast.AstNode;
import ssot_parser.ast.NodeVisitor;
import ssot_parser.ast.nodes.ImportNode;
import ssot_parser.ast.nodes.TypeDefNode;
import ssot_parser.ast.nodes.ServiceDefinitionNode;
import ssot_parser.ast.nodes.MachineNode;
import ssot_parser.ast.nodes.InterfaceNode;
import java.util.stream.Collectors;

/**
 * Represents the root node of the SSoT Abstract Syntax Tree (AST).
 * It typically holds lists of top-level definitions like types, services, machines, etc.
 */
public class SsotRoot implements AstNode {

    private final List<ImportNode> imports;
    private final List<AstNode> typeDefinitions;
    private final List<AstNode> serviceDefinitions;
    private final List<AstNode> machineDefinitions;
    private final List<AstNode> actorDefinitions;
    private final List<AstNode> communicationDefinitions;
    private final Optional<Long> id;
    private final Map<String, Object> annotations;

    public SsotRoot(
            List<ImportNode> imports,
            List<AstNode> typeDefinitions,
            List<AstNode> serviceDefinitions,
            List<AstNode> machineDefinitions,
            List<AstNode> actorDefinitions,
            List<AstNode> communicationDefinitions,
            Optional<Long> id,
            Map<String, Object> annotations
            ) {
        this.imports = Collections.unmodifiableList(imports != null ? imports : new ArrayList<>());
        this.typeDefinitions = Collections.unmodifiableList(typeDefinitions != null ? typeDefinitions : Collections.emptyList());
        this.serviceDefinitions = Collections.unmodifiableList(serviceDefinitions != null ? serviceDefinitions : Collections.emptyList());
        this.machineDefinitions = Collections.unmodifiableList(machineDefinitions != null ? machineDefinitions : Collections.emptyList());
        this.actorDefinitions = Collections.unmodifiableList(actorDefinitions != null ? actorDefinitions : Collections.emptyList());
        this.communicationDefinitions = Collections.unmodifiableList(communicationDefinitions != null ? communicationDefinitions : Collections.emptyList());
        this.id = id;
        this.annotations = Collections.unmodifiableMap(annotations != null ? new HashMap<>(annotations) : Collections.emptyMap());
    }

    public List<ImportNode> getImports() {
        return imports;
    }

    public List<TypeDefNode> getTypeDefs() {
        return typeDefinitions.stream()
                              .filter(TypeDefNode.class::isInstance)
                              .map(TypeDefNode.class::cast)
                              .collect(Collectors.toList());
    }

    public List<ServiceDefinitionNode> getServiceDefinitions() {
        return serviceDefinitions.stream()
                                 .filter(ServiceDefinitionNode.class::isInstance)
                                 .map(ServiceDefinitionNode.class::cast)
                                 .collect(Collectors.toList());
    }

    public List<MachineNode> getMachineNodes() {
        return machineDefinitions.stream()
                               .filter(MachineNode.class::isInstance)
                               .map(MachineNode.class::cast)
                               .collect(Collectors.toList());
    }

    public List<InterfaceNode> getInterfaceNodes() {
        return serviceDefinitions.stream()
                                .filter(InterfaceNode.class::isInstance)
                                .map(InterfaceNode.class::cast)
                                .collect(Collectors.toList());
    }

    public List<AstNode> getActorDefinitions() {
        return actorDefinitions;
    }

    public List<AstNode> getCommunicationDefinitions() {
        return communicationDefinitions;
    }

    public Optional<Long> getId() {
        return id;
    }

    @Override
    public Map<String, Object> getAnnotations() {
        return annotations;
    }

    @Override
    public String toString() {
        return "SsotRoot{" +
               "imports=" + imports +
               ", typeDefinitions=" + typeDefinitions +
               ", serviceDefinitions=" + serviceDefinitions +
               ", machineDefinitions=" + machineDefinitions +
               ", actorDefinitions=" + actorDefinitions +
               ", communicationDefinitions=" + communicationDefinitions +
               ", id=" + id.map(String::valueOf).orElse("none") +
               ", annotations=" + annotations +
               '}';
    }

    @Override
    public <T> T accept(NodeVisitor<T> visitor) {
        return visitor.visitSsotRoot(this);
    }
} 