package ssot_parser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.antlr.v4.runtime.tree.ParseTree; // Import needed for context checks
import org.antlr.v4.runtime.tree.TerminalNode; // Added import
import java.util.Map;
import java.util.HashMap;
// import ssot_parser.SSoTParser.AnnotationContext; // Removed
import ssot_parser.SSoTParser.*; // Added for all parser contexts
import ssot_parser.ast.*;
import ssot_parser.ast.nodes.*;
import ssot_parser.ast.nodes.BlockNode; // Added import for BlockNode
// import ssot_parser.ast.nodes.InvokeStateNode.InvokeTransition; // Commented out
import ssot_parser.ast.type.*;
import ssot_parser.ast.values.ValueNode; // Explicit import
import ssot_parser.ast.values.StringValueNode;
import ssot_parser.ast.values.BooleanValueNode;
import ssot_parser.ast.values.NumberValueNode;
import ssot_parser.ast.values.NullValueNode;
import ssot_parser.ast.values.ArrayValueNode;
import ssot_parser.ast.values.ObjectValueNode;
import ssot_parser.ast.values.RefValueNode;
import ssot_parser.ast.nodes.InvokeCompletionHandlerNode; // New import
import java.time.Duration;
import ssot_parser.ast.nodes.StateNode; // Corrected import
import ssot_parser.ast.nodes.HistoryStateNode; // Corrected import
import ssot_parser.ast.type.StateType; // Corrected import
import ssot_parser.ast.type.RefTypeNode; // Added import
// import ssot_parser.ast.nodes.EventHandlerNode; // If this is a specific type
// import ssot_parser.ast.nodes.ConditionalTransitionNode; // If this is a specific type

// +++ Adding missing AST Node imports +++
import ssot_parser.ast.nodes.ActionReferenceNode;
import ssot_parser.ast.nodes.TransitionSpecNode;
import ssot_parser.ast.nodes.GuardReferenceNode;
import ssot_parser.ast.nodes.TargetStateNode;
import ssot_parser.ast.nodes.DurationNode;
// +++ End missing AST Node imports +++

import ssot_parser.ast.nodes.state.HistoryStateType; // Added import

/**
 * Visits the ANTLR Parse Tree and builds the Abstract Syntax Tree (AST).
 */
public class AstBuilderVisitor extends SSoTBaseVisitor<AstNode> {

    private String currentStateName = null;

    private String stripQuotes(String text) {
        if (text != null && text.length() >= 2 &&
            ((text.startsWith("\"") && text.endsWith("\"")) || (text.startsWith("'") && text.endsWith("'")))) {
            return text.substring(1, text.length() - 1);
        }
        return text;
    }

    private Optional<String> findAnnotationValue(List<AnnotationNode> annotations, String name) {
        if (annotations == null) return Optional.empty();
        return annotations.stream()
                .filter(a -> a.name.equals(name) && a.value instanceof String)
                .findFirst()
                .map(a -> (String) a.value);
    }

    private Optional<Long> extractIdFromList(List<AnnotationNode> annotations) {
        if (annotations == null) {
            return Optional.empty();
        }
        for (AnnotationNode annotation : annotations) {
            if ("id".equals(annotation.name)) {
                if (annotation.value instanceof Number) {
                    return Optional.of(((Number) annotation.value).longValue());
                } else if (annotation.value instanceof String) {
                    try {
                        return Optional.of(Long.parseLong((String) annotation.value));
                    } catch (NumberFormatException e) {
                        System.err.println("Warning: @id annotation string value is not a valid Long: " + annotation.value);
                    }
                } else if (annotation.value != null){
                     System.err.println("Warning: @id annotation has non-numeric, non-string value: " + annotation.value.getClass().getName() + " -> " + annotation.value);
                }
            }
        }
        return Optional.empty();
    }

    // TODO: This helper seems based on old grammar (ActionReferenceListContext missing)
    /*
    private List<ActionReferenceNode> extractActionReferenceNodes(SSoTParser.ActionReferenceListContext ctx) {
        List<ActionReferenceNode> actionRefs = new ArrayList<>();
        if (ctx == null) return actionRefs;

        // Grammar: actionReferenceList: LBRACK actionReference (COMMA actionReference)* RBRACK | actionReference;
        // actionReference: referenceValue;
        // referenceValue: ID (DOT ID)* ;

        if (ctx.actionReference() != null && !ctx.actionReference().isEmpty()) {
            for (SSoTParser.ActionReferenceContext arCtx : ctx.actionReference()) {
                if (arCtx.referenceValue() != null) {
                    String actionName = arCtx.referenceValue().getText();
                    // Assuming ActionReferenceNode constructor: (String actionName, Map<String, Object> annotations)
                    // No direct annotations on actionReference in actionReferenceList grammar, so pass empty map.
                    actionRefs.add(new ActionReferenceNode(actionName, Collections.emptyMap()));
                }
            }
        }
        return actionRefs;
    }
    */

    // TODO: This helper seems based on old grammar (InvokeCompletionContext missing)
    /*
    private InvokeCompletionHandlerNode parseInvokeCompletionHandler(InvokeCompletionContext ctx) {
        if (ctx == null) return null; // Or throw, or return an empty handler

        List<AnnotationNode> handlerAnnotations = ctx.annotation() != null ? extractAnnotations(ctx.annotation()) : Collections.emptyList();
        Optional<Long> handlerId = extractIdFromList(handlerAnnotations);

        List<ActionReferenceNode> handlerActionRefs = new ArrayList<>();
        Optional<TransitionSpecNode> handlerTransitionSpec = Optional.empty();

        if (ctx.actionReferenceList() != null) {
            // Assuming extractActionReferenceNodes returns List<ActionReferenceNode>
            // handlerActionRefs.addAll(extractActionReferenceNodes(ctx.actionReferenceList())); // extractActionReferenceNodes commented out
             System.err.println("Warning: actionReferenceList in invoke completion handler needs fixing.");
        } else if (ctx.transitionSpec() != null) {
            handlerTransitionSpec = Optional.ofNullable((TransitionSpecNode) visitTransitionSpec(ctx.transitionSpec()));
        }
        // InvokeCompletionHandlerNode(Optional<Long> id, List<ActionReferenceNode> actions, Optional<TransitionSpecNode> transitionSpec, List<AnnotationNode> annotations)
        return new InvokeCompletionHandlerNode(handlerId, handlerActionRefs, handlerTransitionSpec, handlerAnnotations);
    }
    */

    @Override
    public AstNode visitFile(FileContext ctx) {
        System.out.println("Visiting File (was SsotDefinition)");

        // TODO: fileId rule is missing from grammar snippet. Commenting out hex ID logic.
        /*
        Optional<Long> fileIdFromHex = Optional.empty();
        if (ctx.fileId() != null && ctx.fileId().HEX_ID() != null) {
            try {
                String hexIdText = ctx.fileId().HEX_ID().getText().substring(2); // Remove "0x"
                fileIdFromHex = Optional.of(Long.parseLong(hexIdText, 16));
                System.out.println("Found File ID from hex: " + fileIdFromHex.get());
            } catch (NumberFormatException e) {
                System.err.println("Error parsing HEX_ID: " + ctx.fileId().HEX_ID().getText());
            }
        }
        */

        Map<String, Object> rootAnnotationsMap = new HashMap<>();
        Optional<Long> fileIdFromAnnotation = Optional.empty();

        // TODO: Revisit how top-level annotations vs block annotations are handled.
        // Grammar puts annotations before blocks: annotation* actorsBlock etc.
        // File rule itself doesn't have annotations or imports directly in current grammar.
        List<AnnotationContext> topLevelAnnotationCtxs = new ArrayList<>();
        /*
        if (ctx.annotation() != null && !ctx.annotation().isEmpty()) {
             // This logic seems flawed based on current grammar. Annotations belong to blocks.
             // ... (Original logic to separate top-level annotations) ...
        }
        */

        // List<AnnotationNode> parsedTopLevelAnnotations = extractAnnotations(topLevelAnnotationCtxs);
        // ... (Original logic to process parsedTopLevelAnnotations for fileId/rootAnnotationsMap) ...


        // Optional<Long> finalFileId = fileIdFromHex.isPresent() ? fileIdFromHex : fileIdFromAnnotation;
        Optional<Long> finalFileId = fileIdFromAnnotation; // Use only annotation ID for now


        // Initialize local lists to store definitions
        List<ImportNode> imports = new ArrayList<>();
        List<TypeDefNode> typeDefinitions = new ArrayList<>();
        List<AstNode> serviceDefinitions = new ArrayList<>();
        List<MachineNode> machineDefinitions = new ArrayList<>();
        List<ActorNode> actorDefinitions = new ArrayList<>();
        List<AstNode> communicationDefinitions = new ArrayList<>();


        // TODO: importStatement rule missing from grammar snippet. Commenting out import processing.
        /*
        if (ctx.importStatement() != null) {
            for (ImportStatementContext importCtx : ctx.importStatement()) {
                imports.add((ImportNode) visitImportStatement(importCtx));
            }
        }
        */

        // Process definition blocks directly
        if (ctx.definitionBlock() != null) {
            for (DefinitionBlockContext blockCtx : ctx.definitionBlock()) {
                // Pass annotations from the block definition to the specific block visitor
                List<AnnotationNode> blockAnnotations = Collections.emptyList();
                 if (blockCtx.annotation() != null && !blockCtx.annotation().isEmpty()) {
                    blockAnnotations = extractAnnotations(blockCtx.annotation());
                 }

                AstNode visitedNode = visit(blockCtx.getChild(blockCtx.getChildCount() - 1)); // Visit the actual block (actorsBlock, typesBlock etc.)

                // The visitedNode should now be a BlockNode containing definitions
                // We need to associate the blockAnnotations with the BlockNode or its contents.
                // Let's assume BlockNode constructor takes annotations, or add a setter.

                if (visitedNode instanceof BlockNode) {
                    BlockNode container = (BlockNode) visitedNode;
                    // TODO: Associate blockAnnotations with the container node if needed.
                    // container.setAnnotations(blockAnnotations); // Example

                    System.out.println("Processing BlockNode: " + container.blockType + " with " + container.getDefinitions().size() + " definitions.");

                    switch (container.blockType) {
                        case "types":
                             container.getDefinitions().forEach(child -> {
                                 if (child instanceof TypeDefNode) typeDefinitions.add((TypeDefNode) child);
                                 else System.err.println("Warning: Child in types block not TypeDefNode: " + child.getClass().getName());
                             }); break;
                        case "services":
                             container.getDefinitions().forEach(child -> {
                                 if (child instanceof ServiceDefinitionNode || child instanceof InterfaceNode) serviceDefinitions.add(child);
                                 else System.err.println("Warning: Child in services block not Service/Interface: " + child.getClass().getName());
                            }); break;
                        case "machines":
                            container.getDefinitions().forEach(child -> {
                                if (child instanceof MachineNode) machineDefinitions.add((MachineNode) child);
                                else System.err.println("Warning: Child in machines block not MachineNode: " + child.getClass().getName());
                            }); break;
                        case "actors":
                             container.getDefinitions().forEach(child -> {
                                 if (child instanceof ActorNode) actorDefinitions.add((ActorNode) child);
                                 else System.err.println("Warning: Child in actors block not ActorNode: " + child.getClass().getName());
                             }); break;
                        case "communication":
                             container.getDefinitions().forEach(child -> {
                                 if (child instanceof ProtocolNode || child instanceof ChannelNode || child instanceof EventNode) communicationDefinitions.add(child);
                                 else System.err.println("Warning: Child in comms block not Protocol/Channel/Event: " + child.getClass().getName());
                             }); break;
                        default:
                             System.err.println("Warning: Unknown block type encountered: " + container.blockType);
                             break;
                    }
                } else if (visitedNode != null) {
                     System.err.println("Warning: Visiting a definitionBlock did not result in a BlockNode: " + visitedNode.getClass().getName());
                } else {
                    System.err.println("Warning: Visiting a definitionBlock resulted in null.");
                }
            }
        }

        // Create the root node with collected definitions
        return new SsotRoot(
                finalFileId,
                rootAnnotationsMap,
                imports,
                typeDefinitions,
                serviceDefinitions,
                machineDefinitions,
                actorDefinitions,
                communicationDefinitions);
    }

    // TODO: importStatement rule missing from grammar snippet. Comment out method.
    /*
    @Override // Remove @Override if superclass doesn't have it
    public AstNode visitImportStatement(ImportStatementContext ctx) {
        System.out.println("Visiting ImportStatement");
        String path = stripQuotes(ctx.STRING().getText());
        List<AnnotationNode> annotations = extractAnnotations(ctx.annotation());
        Optional<Long> id = extractIdFromList(annotations);
        return new ImportNode(path, id, mapAnnotations(annotations));
    }
    */

    // visitTypesBlock, visitServicesBlock etc. remain similar, but need to handle annotations passed down.
    @Override
    public AstNode visitTypesBlock(TypesBlockContext ctx) {
        System.out.println("Visiting TypesBlock");
        List<AstNode> definitions = new ArrayList<>();
        if (ctx.typeDefinition() != null) {
            for (TypeDefinitionContext typeCtx : ctx.typeDefinition()) {
                 AstNode node = visit(typeCtx);
                 if (node != null) definitions.add(node);
            }
        }
         // Pass annotations from parent (definitionBlock) if needed, or handle here if grammar changes.
         // BlockNode constructor: (String blockType, List<AstNode> definitions, List<AnnotationNode> annotations)
         return new BlockNode("types", definitions, Collections.emptyList()); // Pass empty for now
    }

    @Override
    public AstNode visitServicesBlock(ServicesBlockContext ctx) {
        System.out.println("Visiting ServicesBlock");
        List<AstNode> definitions = new ArrayList<>();
        if (ctx.serviceElement() != null) {
            for (ServiceElementContext elCtx : ctx.serviceElement()) {
                 AstNode node = visit(elCtx);
                 if (node != null) definitions.add(node);
            }
        }
        return new BlockNode("services", definitions, Collections.emptyList()); // Pass empty for now
    }

    // Actors Block
    @Override
    public AstNode visitActorsBlock(ActorsBlockContext ctx) {
        System.out.println("Visiting ActorsBlock");
        List<AstNode> definitions = new ArrayList<>();
        if (ctx.actorDefinition() != null) {
            for (ActorDefinitionContext actorCtx : ctx.actorDefinition()) {
                AstNode node = visitActorDefinition(actorCtx); // Call specific visitor
                if (node != null) {
                    definitions.add(node);
                }
            }
        }
        return new BlockNode("actors", definitions, Collections.emptyList());
    }


    @Override
    public AstNode visitMachinesBlock(MachinesBlockContext ctx) {
        System.out.println("Visiting MachinesBlock");
        List<AstNode> definitions = new ArrayList<>();
        if (ctx.machineDefinition() != null) {
            for (MachineDefinitionContext machCtx : ctx.machineDefinition()) {
                 AstNode node = visit(machCtx);
                 if (node != null) definitions.add(node);
            }
        }
         return new BlockNode("machines", definitions, Collections.emptyList()); // Pass empty for now
    }


    // Communication Block
    @Override
    public AstNode visitCommunicationBlock(CommunicationBlockContext ctx) {
        System.out.println("Visiting CommunicationBlock");
        List<AstNode> definitions = new ArrayList<>();
        if (ctx.communicationDefinition() != null) {
            for (CommunicationDefinitionContext commCtx : ctx.communicationDefinition()) {
                AstNode node = visit(commCtx); // Visits protocol, channel, or event def
                if (node != null) {
                    definitions.add(node);
                }
            }
        }
        return new BlockNode("communication", definitions, Collections.emptyList());
    }


    // Keep visitServiceElement, visitTypeDefinition

    // FieldDefinitionContext missing error
    // @Override // Remove @Override
    public AstNode visitFieldDefinition(StructFieldDefinitionContext ctx) { // Change context type
        System.out.println("Visiting StructFieldDefinition (was FieldDefinition)");
        List<AnnotationNode> annotations = extractAnnotations(ctx.annotation());
        Optional<Long> id = extractIdFromList(annotations);
        String name = ctx.ID().getText();
        TypeExprNode type = (TypeExprNode) visitTypeReference(ctx.typeReference()); // Use visitTypeReference

        Map<String, Object> fieldAnnotations = mapAnnotations(annotations);
        // FieldNode constructor: (String name, TypeExprNode type, Optional<Long> id, Map<String, Object> annotations)
        return new FieldNode(name, type, id, fieldAnnotations);
    }

    // EnumVariantContext missing error
    // @Override // Remove @Override
     public AstNode visitEnumVariant(EnumVariantDefinitionContext ctx) { // Change context type
        System.out.println("Visiting EnumVariantDefinition (was EnumVariant)");
        List<AnnotationNode> annotations = extractAnnotations(ctx.annotation());
        Optional<Long> id = extractIdFromList(annotations);
        String name = ctx.ID().getText();
        Map<String, Object> variantAnnotations = mapAnnotations(annotations);
        // EnumVariantNode constructor: (String name, Optional<Long> id, Map<String, Object> annotations)
        return new EnumVariantNode(name, id, variantAnnotations);
     }


    // ContextDefinition rule missing
    /*
    @Override // Remove @Override
    public AstNode visitContextDefinition(ContextDefinitionContext ctx) {
        System.out.println("Visiting ContextDefinition");
        String name = ctx.ID().getText();
        List<AnnotationNode> annotations = extractAnnotations(ctx.annotation());
        Optional<Long> id = extractIdFromList(annotations);
        List<ContextVariableNode> variables = new ArrayList<>();
        if (ctx.contextField() != null) {
            for(ContextFieldContext fieldCtx : ctx.contextField()) {
                variables.add((ContextVariableNode) visitContextField(fieldCtx));
            }
        }
        return new ContextNode(name, variables, id, mapAnnotations(annotations));
    }
    */

    // ContextField rule missing
    /*
    @Override // Remove @Override
    public AstNode visitContextField(ContextFieldContext ctx) {
        System.out.println("Visiting ContextField");
        String name = ctx.ID().getText();
        TypeExprNode type = (TypeExprNode) visitTypeExpr(ctx.typeExpr());
        List<AnnotationNode> annotations = extractAnnotations(ctx.annotation());
        Optional<Long> id = extractIdFromList(annotations);
        // ContextVariableNode constructor likely needs: name, type, id, annotations map
        return new ContextVariableNode(name, type, id, mapAnnotations(annotations));
    }
    */

    // InvokesDefinition rule missing
    /*
     @Override // Remove @Override
    public AstNode visitInvokesDefinition(InvokesDefinitionContext ctx) {
        System.out.println("Visiting InvokesDefinition");
        List<AstNode> invokes = new ArrayList<>();
        if (ctx.invokeDefinition() != null) {
            for(InvokeDefinitionContext invokeCtx : ctx.invokeDefinition()) {
                invokes.add(visitInvokeDefinition(invokeCtx));
            }
        }
        // InvokesNode constructor likely needs: invokes list, id, annotations map
        List<AnnotationNode> annotations = extractAnnotations(ctx.annotation());
        Optional<Long> id = extractIdFromList(annotations);
        return new InvokesNode(invokes, id, mapAnnotations(annotations)); // Assuming InvokesNode exists
    }
    */

    // InvokeSource rule missing
    /*
    @Override // Remove @Override
    public AstNode visitInvokeSource(InvokeSourceContext ctx) {
        // This was likely just an ID reference before, handle directly in visitInvokeDefinition
        return new RefValueNode(ctx.getText()); // Simplistic assumption
    }
    */


    // HistoryDefinitionContext missing error, rule renamed to historyStateDefinition
    // @Override // Remove @Override
    public AstNode visitHistoryStateDefinition(HistoryStateDefinitionContext ctx) { // Rename method and context type
        System.out.println("Visiting HistoryStateDefinition (was HistoryDefinition)");

        HistoryStateType type = HistoryStateType.SHALLOW; // Default
        if (ctx.historyType != null) {
            if (ctx.historyType.getType() == SSoTLexer.DEEP) {
                type = HistoryStateType.DEEP;
            }
        }

        Optional<String> targetState = Optional.empty();
         if (ctx.targetState != null) {
             targetState = Optional.of(ctx.targetState.getText());
         }

        // List<AnnotationNode> annotations = extractAnnotations(ctx.annotation()); // No annotations in grammar rule
        // Optional<Long> id = extractIdFromList(annotations);

        // HistoryStateNode constructor: (HistoryStateType type, Optional<String> defaultTransitionTarget, Optional<Long> id, Map<String, Object> annotations)
        return new HistoryStateNode(type, targetState, Optional.empty(), Collections.emptyMap()); // Pass empty id/annotations
    }


    // Overriding visit children for specific labeled alternatives
    @Override public AstNode visitIdAnnotation(IdAnnotationContext ctx) {
        String name = ctx.ID().getText(); // Should be "id"
        Long value = Long.parseLong(ctx.INT().getText());
        return new AnnotationNode(name, value);
    }

    @Override public AstNode visitValueAnnotation(ValueAnnotationContext ctx) {
        String name = ctx.annotationName().getText();
        Object value = visitAnnotationValue(ctx.annotationValue()); // Visit the value part
        return new AnnotationNode(name, value);
    }

    // Helper to visit the different kinds of annotation values
    public Object visitAnnotationValue(AnnotationValueContext ctx) {
        if (ctx.literal() != null) {
            return visitLiteral(ctx.literal()).value; // Extract value from LiteralValueNode
        } else if (ctx.ID() != null) {
             // It's a reference (e.g., $protocol(HTTP))
             return new RefValueNode(ctx.ID().getText()).value; // Return the string ID for now
        } else if (ctx.LBRACK() != null) {
             // It's an array
             List<Object> values = new ArrayList<>();
             if (ctx.literal() != null) {
                 for (LiteralContext litCtx : ctx.literal()) {
                     values.add(visitLiteral(litCtx).value); // Extract value
                 }
             }
             return values;
        }
        return null; // Should not happen
    }

    // Updated visitLiteral based on grammar
    @Override public ValueNode visitLiteral(LiteralContext ctx) {
        if (ctx.STRING() != null) {
            return new StringValueNode(stripQuotes(ctx.STRING().getText()));
        } else if (ctx.INT() != null) {
            return new NumberValueNode(Long.parseLong(ctx.INT().getText()));
        } else if (ctx.FLOAT() != null) {
            return new NumberValueNode(Double.parseDouble(ctx.FLOAT().getText()));
        } else if (ctx.BOOLEAN() != null) {
            return new BooleanValueNode(Boolean.parseBoolean(ctx.BOOLEAN().getText()));
        } else if (ctx.NULL() != null) {
            return new NullValueNode();
        }
        // Should not happen with the current grammar
        throw new IllegalArgumentException("Unknown literal type: " + ctx.getText());
    }

     // Visit Type Reference
     @Override
     public TypeExprNode visitTypeReference(TypeReferenceContext ctx) {
         if (ctx.simpleType() != null) {
             return (TypeExprNode) visit(ctx.simpleType());
         } else if (ctx.listType() != null) {
             return (TypeExprNode) visit(ctx.listType());
         } else if (ctx.mapType() != null) {
             return (TypeExprNode) visit(ctx.mapType());
         } else if (ctx.optionalType() != null) {
             return (TypeExprNode) visit(ctx.optionalType());
         } else if (ctx.ID() != null) {
             // It's a reference to a custom type (struct/enum)
             return new RefTypeNode(ctx.ID().getText()); // Assuming RefTypeNode exists
         }
         throw new RuntimeException("Unhandled type reference: " + ctx.getText());
     }

     @Override public AstNode visitSimpleType(SimpleTypeContext ctx) {
         if (ctx.PRIMITIVE_TYPE() != null) {
             return new PrimitiveTypeNode(ctx.PRIMITIVE_TYPE().getText());
         } else if (ctx.TIMESTAMP_TYPE() != null) {
             // Decide if timestamp is primitive or its own type node
             return new PrimitiveTypeNode(ctx.TIMESTAMP_TYPE().getText());
         }
         throw new RuntimeException("Unknown simple type: " + ctx.getText());
     }

     @Override public AstNode visitListType(ListTypeContext ctx) {
         TypeExprNode elementType = (TypeExprNode) visitTypeReference(ctx.typeReference());
         return new ListTypeNode(elementType);
     }

     @Override public AstNode visitMapType(MapTypeContext ctx) {
         TypeExprNode keyType = (TypeExprNode) visitTypeReference(ctx.typeReference(0));
         TypeExprNode valueType = (TypeExprNode) visitTypeReference(ctx.typeReference(1));
         return new MapTypeNode(keyType, valueType);
     }

     @Override public AstNode visitOptionalType(OptionalTypeContext ctx) {
         TypeExprNode innerType = (TypeExprNode) visitTypeReference(ctx.typeReference());
         return new OptionalTypeNode(innerType);
     }


    private List<AnnotationNode> extractAnnotations(List<AnnotationContext> annotationCtxs) {
         if (annotationCtxs == null || annotationCtxs.isEmpty()) {
             return Collections.emptyList();
         }
         List<AnnotationNode> annotations = new ArrayList<>();
         for (AnnotationContext ctx : annotationCtxs) {
             AstNode node = visit(ctx); // Uses labeled alternatives (visitIdAnnotation, visitValueAnnotation)
             if (node instanceof AnnotationNode) {
                 annotations.add((AnnotationNode) node);
             }
         }
         return annotations;
     }

     private Map<String, Object> mapAnnotations(List<AnnotationNode> annotationNodes) {
        Map<String, Object> map = new HashMap<>();
        if (annotationNodes != null) {
            for (AnnotationNode node : annotationNodes) {
                // Skip adding @id to the generic map as it's handled separately for nodes
                 if (!node.name.equals("id")) {
                     map.put(node.name, Optional.ofNullable(node.value).orElse(true)); // Store true for flag annotations
                 }
            }
        }
        return map;
    }
}

