package ssot_parser.ast;

import ssot_parser.ast.nodes.*;
import ssot_parser.SsotRoot;
import ssot_parser.ast.nodes.ImportNode;
import ssot_parser.ast.nodes.ContextVariableNode;
// import ssot_parser.TypeDefNode; // Removed
// import ssot_parser.ActionNode; // Removed
// import ssot_parser.StateNode; // Removed
// import ssot_parser.TransitionNode; // Removed
// import ssot_parser.BlockNode; // Removed
// No more imports needed from ssot_parser directly for nodes

/**
 * Defines the Visitor pattern interface for traversing the AST.
 * Implementations will define how to handle each node type.
 *
 * @param <T> The return type of the visit methods.
 */
public interface NodeVisitor<T> {

    // Root node for the entire file
    T visitSsotRoot(SsotRoot node);
    T visitImportNode(ImportNode node);

    // Type Definition Nodes
    T visitTypeDefNode(TypeDefNode node);
    T visitFieldNode(FieldNode node);
    T visitEnumVariantNode(EnumVariantNode node);

    // State Machine Nodes
    T visitMachineDefinitionNode(MachineDefinitionNode node);
    T visitContextNode(ContextNode node);
    T visitContextVariableNode(ContextVariableNode node);
    T visitActionDefinitionNode(ActionDefinitionNode node);
    T visitGuardDefinitionNode(GuardDefinitionNode node);
    T visitInvokeDefinitionNode(InvokeDefinitionNode node);
    T visitStateNode(StateNode node);
    T visitHistoryStateNode(HistoryStateNode node);
    T visitTransitionNode(TransitionNode node);
    T visitInvokeStateNode(InvokeStateNode node);
    T visitEventHandlerNode(EventHandlerNode node);
    T visitConditionalTransitionNode(ConditionalTransitionNode node);

    // Service Nodes
    T visitServiceDefinitionNode(ServiceDefinitionNode node);
    T visitInterfaceNode(InterfaceNode node);
    T visitMethodNode(MethodNode node);
    T visitParameterNode(ParameterNode node);

    // Actor Node
    T visitActorNode(ActorNode node);

    // Communication Nodes
    // T visitCommunicationNode(CommunicationNode node); // Commented out for now
    T visitProtocolNode(ProtocolNode node);
    T visitChannelNode(ChannelNode node);
    T visitEventNode(EventNode node);

    // Deployment and Dependency Nodes
    // T visitDeploymentConfigNode(DeploymentConfigNode node); // Commented out for now
    // T visitDependencyNode(DependencyNode node); // Commented out for now

    // Annotation Node (might not be visited directly, but included for completeness)
    // T visitAnnotationNode(AnnotationNode node);

    // Add other node types as they are created
    // T visitBlockNode(BlockNode node); // If BlockNode becomes part of traversable AST

    // Value Nodes
    T visitValueNode(ssot_parser.ast.values.ValueNode node);

}
