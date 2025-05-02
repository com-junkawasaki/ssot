package ssot_parser.ast;

import ssot_parser.ast.nodes.*;
import ssot_parser.SsotRoot;
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

    // Root node
    T visitRootNode(RootNode node);

    // Type Definition Nodes
    T visitTypeDefNode(TypeDefNode node);
    T visitFieldNode(FieldNode node);
    T visitEnumNode(EnumNode node);
    T visitEnumVariantNode(EnumVariantNode node);

    // State Machine Nodes
    T visitStateMachineNode(StateMachineNode node);
    T visitContextNode(ContextNode node);
    T visitActionNode(ActionNode node);
    T visitGuardNode(GuardNode node);
    T visitStateNode(StateNode node);
    T visitTransitionNode(TransitionNode node);
    T visitInvokeStateNode(InvokeStateNode node);
    T visitEventHandlerNode(EventHandlerNode node);
    T visitConditionalTransitionNode(ConditionalTransitionNode node);

    // Service Nodes
    T visitServiceNode(ServiceNode node);
    T visitInterfaceNode(InterfaceNode node);
    T visitMethodNode(MethodNode node);
    T visitParameterNode(ParameterNode node);

    // Actor Node
    T visitActorNode(ActorNode node);

    // Communication Nodes
    T visitProtocolNode(ProtocolNode node);
    T visitChannelNode(ChannelNode node);
    T visitEventNode(EventNode node);

    // Annotation Node (might not be visited directly, but included for completeness)
    // T visitAnnotationNode(AnnotationNode node);

    // Add other node types as they are created
    // T visitBlockNode(BlockNode node); // If BlockNode becomes part of traversable AST

}
