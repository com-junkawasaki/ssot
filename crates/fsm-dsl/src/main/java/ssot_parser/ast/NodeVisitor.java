package ssot_parser.ast;

import ssot_parser.ast.nodes.*;
import ssot_parser.SsotRoot;
import ssot_parser.TypeDefNode;
import ssot_parser.FieldNode;
import ssot_parser.ActionNode;
import ssot_parser.StateNode;
import ssot_parser.TransitionNode;
import ssot_parser.BlockNode;

/**
 * Defines the Visitor pattern interface for traversing the AST.
 * Implementations will define how to handle each node type.
 *
 * @param <T> The return type of the visit methods.
 */
public interface NodeVisitor<T> {

    // Root node
    T visitSsotRoot(SsotRoot node);

    // Type Definition Nodes
    T visitTypeDefNode(TypeDefNode node);
    T visitFieldNode(FieldNode node);
    T visitEnumNode(EnumNode node);
    T visitEnumVariantNode(EnumVariantNode node);

    // State Machine Nodes
    T visitMachineNode(MachineNode node);
    T visitContextNode(ContextNode node);
    T visitActionNode(ActionNode node);
    T visitGuardNode(GuardNode node);
    T visitInvokeNode(InvokeNode node);
    T visitStateNode(StateNode node);
    T visitTransitionNode(TransitionNode node);

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
    T visitAnnotationNode(AnnotationNode node);

    // Add other node types as they are created
    // T visitBlockNode(BlockNode node); // If BlockNode becomes part of traversable AST

}
