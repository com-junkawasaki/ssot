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
import ssot_parser.ast.type.BaseType; // Added import for BaseType
import ssot_parser.ast.type.PrimitiveTypeNode; // Ensure this is imported
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
            if (annotation.isIdAnnotation()) {
                if (annotation.value instanceof Long) {
                     return Optional.of((Long) annotation.value);
                } else if (annotation.value instanceof Number) { // Handle potential other Number types
                    return Optional.of(((Number) annotation.value).longValue());
                } else {
                     System.err.println("Warning: @id annotation has non-Long value: " + annotation.value.getClass().getName() + " -> " + annotation.value);
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
        System.out.println("Visiting File");

        List<ImportNode> imports = new ArrayList<>();
        List<TypeDefNode> typeDefinitions = new ArrayList<>();
        List<AstNode> serviceDefinitions = new ArrayList<>();
        List<MachineNode> machineDefinitions = new ArrayList<>();
        List<ActorNode> actorDefinitions = new ArrayList<>();
        List<AstNode> communicationDefinitions = new ArrayList<>();

        if (ctx.definitionBlock() != null) {
            for (DefinitionBlockContext blockCtx : ctx.definitionBlock()) {
                List<AnnotationNode> blockAnnotations = Collections.emptyList();
                ParseTree contentNode = null;
                AstNode visitedNode = null;
                Optional<Long> blockId = Optional.empty();
                Map<String, Object> blockAnnotationMap = Collections.emptyMap();

                // Handle annotations based on the specific DefinitionBlockContext subtype
                if (blockCtx instanceof ActorsBlockDefinitionContext) {
                    ActorsBlockDefinitionContext specificCtx = (ActorsBlockDefinitionContext) blockCtx;
                    if (specificCtx.annotation() != null && !specificCtx.annotation().isEmpty()) {
                        blockAnnotations = extractAnnotations(specificCtx.annotation());
                    }
                    contentNode = specificCtx.actorsBlock();
                } else if (blockCtx instanceof TypesBlockDefinitionContext) {
                    TypesBlockDefinitionContext specificCtx = (TypesBlockDefinitionContext) blockCtx;
                    if (specificCtx.annotation() != null && !specificCtx.annotation().isEmpty()) {
                        blockAnnotations = extractAnnotations(specificCtx.annotation());
                    }
                    contentNode = specificCtx.typesBlock();
                } else if (blockCtx instanceof ServicesBlockDefinitionContext) {
                    ServicesBlockDefinitionContext specificCtx = (ServicesBlockDefinitionContext) blockCtx;
                    if (specificCtx.annotation() != null && !specificCtx.annotation().isEmpty()) {
                        blockAnnotations = extractAnnotations(specificCtx.annotation());
                    }
                    contentNode = specificCtx.servicesBlock();
                } else if (blockCtx instanceof CommunicationBlockDefinitionContext) {
                    CommunicationBlockDefinitionContext specificCtx = (CommunicationBlockDefinitionContext) blockCtx;
                    if (specificCtx.annotation() != null && !specificCtx.annotation().isEmpty()) {
                        blockAnnotations = extractAnnotations(specificCtx.annotation());
                    }
                    contentNode = specificCtx.communicationBlock();
                } else if (blockCtx instanceof MachinesBlockDefinitionContext) {
                    MachinesBlockDefinitionContext specificCtx = (MachinesBlockDefinitionContext) blockCtx;
                    if (specificCtx.annotation() != null && !specificCtx.annotation().isEmpty()) {
                        blockAnnotations = extractAnnotations(specificCtx.annotation());
                    }
                    contentNode = specificCtx.machinesBlock();
                }

                blockId = extractIdFromList(blockAnnotations);
                blockAnnotationMap = mapAnnotations(blockAnnotations.stream()
                                                                        .filter(a -> !a.isIdAnnotation())
                                                                        .collect(Collectors.toList()));

                // Visit the content node with the extracted ID and annotations
                if (contentNode instanceof ActorsBlockContext) {
                    visitedNode = visitActorsBlock((ActorsBlockContext)contentNode, blockId, blockAnnotationMap);
                } else if (contentNode instanceof TypesBlockContext) {
                    visitedNode = visitTypesBlock((TypesBlockContext)contentNode, blockId, blockAnnotationMap);
                } else if (contentNode instanceof ServicesBlockContext) {
                    visitedNode = visitServicesBlock((ServicesBlockContext)contentNode, blockId, blockAnnotationMap);
                } else if (contentNode instanceof CommunicationBlockContext) {
                    visitedNode = visitCommunicationBlock((CommunicationBlockContext)contentNode, blockId, blockAnnotationMap);
                } else if (contentNode instanceof MachinesBlockContext) {
                    visitedNode = visitMachinesBlock((MachinesBlockContext)contentNode, blockId, blockAnnotationMap);
                }


                if (visitedNode != null) {
                    if (visitedNode instanceof BlockNode) {
                        BlockNode container = (BlockNode) visitedNode;
                        System.out.println("Processing BlockNode: " + container.blockType + " with ID: " + container.getId().orElse(null) + " and " + container.getDefinitions().size() + " definitions.");

                        switch (container.blockType) {
                            case "types":
                                 container.getDefinitions().forEach(child -> {
                                     if (child instanceof TypeDefNode) typeDefinitions.add((TypeDefNode) child);
                                     else System.err.println("Warning: Child in types block not TypeDefNode: " + child.getClass().getName());
                                 }); break;
                            case "services":
                                  container.getDefinitions().forEach(child -> serviceDefinitions.add(child));
                                  break;
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
                                 container.getDefinitions().forEach(child -> communicationDefinitions.add(child));
                                 break;
                            default:
                                 System.err.println("Warning: Unknown block type encountered: " + container.blockType);
                                 break;
                        }
                    }
                } else if (contentNode != null) {
                    System.err.println("Warning: Visiting a block content node ("+ contentNode.getClass().getSimpleName() +") returned null: " + contentNode.getText());
                } else if (blockCtx != null) { // blockCtx itself might be of an unexpected type if grammar changes
                     System.err.println("Warning: Could not determine content node or specific block type for DefinitionBlockContext: " + blockCtx.getText());
                }
            }
        }

        return new SsotRoot(
            Optional.empty(),
            Collections.emptyMap(),
            imports,
            typeDefinitions,
            serviceDefinitions,
            machineDefinitions,
            actorDefinitions,
            communicationDefinitions
        );
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

    // Modified to accept ID and annotations from parent DefinitionBlockContext
    public AstNode visitTypesBlock(TypesBlockContext ctx, Optional<Long> id, Map<String, Object> annotations) {
        List<AstNode> definitions = new ArrayList<>();
        if (ctx.typeDefinition() != null) {
            for (TypeDefinitionContext typeCtx : ctx.typeDefinition()) {
                definitions.add(visit(typeCtx));
            }
        }
         // Use the passed ID and annotations in the constructor
        return new BlockNode(id, "types", definitions, annotations);
    }

    // Modified to accept ID and annotations
    public AstNode visitServicesBlock(ServicesBlockContext ctx, Optional<Long> id, Map<String, Object> annotations) {
        List<AstNode> definitions = new ArrayList<>();
         if (ctx.serviceElement() != null) {
             for (ServiceElementContext elementCtx : ctx.serviceElement()) {
                 definitions.add(visit(elementCtx));
             }
         }
         return new BlockNode(id, "services", definitions, annotations);
    }

    // Modified to accept ID and annotations
    public AstNode visitActorsBlock(ActorsBlockContext ctx, Optional<Long> id, Map<String, Object> annotations) {
        List<AstNode> definitions = new ArrayList<>();
        if (ctx.actorDefinition() != null) {
            for (ActorDefinitionContext actorCtx : ctx.actorDefinition()) {
                definitions.add(visit(actorCtx));
            }
        }
        return new BlockNode(id, "actors", definitions, annotations);
    }

    // Modified to accept ID and annotations
    public AstNode visitMachinesBlock(MachinesBlockContext ctx, Optional<Long> id, Map<String, Object> annotations) {
        List<AstNode> definitions = new ArrayList<>();
        if (ctx.machineDefinition() != null) {
            for (MachineDefinitionContext machineCtx : ctx.machineDefinition()) {
                definitions.add(visit(machineCtx));
            }
        }
        return new BlockNode(id, "machines", definitions, annotations);
    }

    // Modified to accept ID and annotations
    public AstNode visitCommunicationBlock(CommunicationBlockContext ctx, Optional<Long> id, Map<String, Object> annotations) {
        List<AstNode> definitions = new ArrayList<>();
         if (ctx.communicationDefinition() != null) {
             for (CommunicationDefinitionContext commCtx : ctx.communicationDefinition()) {
                 definitions.add(visit(commCtx));
             }
         }
        return new BlockNode(id, "communication", definitions, annotations);
    }


    // FieldDefinitionContext missing error
    // @Override // Remove @Override
    @Override
    public AstNode visitStructFieldDefinition(StructFieldDefinitionContext ctx) { // Corrected context type
        List<AnnotationNode> annotations = Collections.emptyList();
        if (ctx.annotation() != null && !ctx.annotation().isEmpty()) {
            annotations = extractAnnotations(ctx.annotation());
        }
        Optional<Long> id = extractIdFromList(annotations);
        String name = ctx.ID().getText(); // Reverted: Use ID()
        TypeExprNode type = (TypeExprNode) visit(ctx.typeReference());
        // TODO: Extract annotations within braces after type if needed ctx.annotation(1)
        return new FieldNode(id, name, type, annotations);
    }

    // EnumVariantContext missing error
    // @Override // Remove @Override
     @Override
     public AstNode visitEnumVariantDefinition(EnumVariantDefinitionContext ctx) { // Corrected context type
         List<AnnotationNode> annotations = Collections.emptyList();
          if (ctx.annotation() != null && !ctx.annotation().isEmpty()) {
             annotations = extractAnnotations(ctx.annotation());
          }
         Optional<Long> id = extractIdFromList(annotations);
         // Map<String, Object> annotationMap = mapAnnotations(annotations.stream().filter(a -> !a.isIdAnnotation()).collect(Collectors.toList())); // Not needed for constructor

         String name = ctx.ID().getText();
         // Optional<ValueNode> value = Optional.empty(); // Not needed for constructor

         // Constructor is: EnumVariantNode(Optional<Long> id, List<AnnotationNode> annotations, String name)
         return new EnumVariantNode(id, annotations, name);
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
    public AstNode visitHistoryStateDefinition(HistoryStateDefinitionContext ctx) {
        // History states don't have their own annotations or IDs in this grammar
        // They belong to the parent state.
        Optional<Long> id = Optional.empty();
        Map<String, Object> annotationMap = Collections.emptyMap();

        HistoryStateType type = HistoryStateType.SHALLOW; // Default
        if (ctx.historyType != null && ctx.historyType.getType() == SSoTLexer.DEEP) {
            type = HistoryStateType.DEEP;
        }

        // Name is implicitly '$historyShallow' or '$historyDeep'
        String name = (type == HistoryStateType.DEEP) ? "$historyDeep" : "$historyShallow";

        Optional<TransitionSpecNode> defaultTransition = Optional.empty();
        TransitionSpecNode createdTransitionSpec = null;

        if (ctx.targetState != null) {
            String targetStateName = ctx.targetState.getText();
            TargetStateNode targetNode = new TargetStateNode(targetStateName);
            // TransitionSpecNode constructor does not take TransitionType.
            // Actions, guards, allowedActors, blockAnnotations are empty for this simple default history transition.
            createdTransitionSpec = new TransitionSpecNode(Optional.empty(), targetNode, Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyMap());
        }
        defaultTransition = Optional.ofNullable(createdTransitionSpec);

        // HistoryStateNode constructor expects List<AnnotationNode>, not Map<String, Object>
        // Grammar for historyStateDefinition does not include annotations.
        return new HistoryStateNode(id, name, type, defaultTransition, Collections.emptyList());
    }


    // Overriding visit children for specific labeled alternatives
    @Override public AstNode visitIdAnnotation(IdAnnotationContext ctx) {
        // Grammar: AT ID LPAREN INT RPAREN
        String name = ctx.ID().getText(); // Should be 'id'
        Long idValue = -1L; // Default error value
        if (ctx.INT() != null) {
            try {
                idValue = Long.parseLong(ctx.INT().getText());
            } catch (NumberFormatException e) {
                 System.err.println("Error: Could not parse @id integer value: " + ctx.INT().getText());
            }
        } else {
             System.err.println("Error: @id annotation missing integer value.");
        }
        // Constructor: AnnotationNode(String name, Object value, boolean isId)
        return new AnnotationNode(name, idValue, true);
    }

    @Override public AstNode visitValueAnnotation(ValueAnnotationContext ctx) {
        // Grammar: DOLLAR annotationName annotationValue
        String name = "";
        if (ctx.annotationName() != null && ctx.annotationName().ID() != null) {
            name = ctx.annotationName().ID().getText();
        }

        ValueNode valueNode = new NullValueNode(); // Default
        if (ctx.annotationValue() != null) {
             valueNode = visitAnnotationValue(ctx.annotationValue());
        }

        // Constructor: AnnotationNode(String name, Object value, boolean isId)
        return new AnnotationNode(name, valueNode.getActualValue(), false);
    }


    // Change return type from Object to ValueNode
    @Override
    public ValueNode visitAnnotationValue(AnnotationValueContext ctx) {
        // LPAREN literal RPAREN
        if (ctx.literal() != null && ctx.literal().size() == 1) {
            return visitLiteral(ctx.literal(0)); // Visit the single literal
        }
        // LPAREN ID RPAREN
        else if (ctx.ID() != null) {
            return new RefValueNode(ctx.ID().getText()); // Create RefValueNode for the ID
        }
        // LPAREN LBRACK (literal (COMMA literal)*)? RBRACK RPAREN
        else if (ctx.LBRACK() != null) {
            List<ValueNode> elements = new ArrayList<>();
            if (ctx.literal() != null) { // literal() returns List<LiteralContext> here
                for (LiteralContext literalCtx : ctx.literal()) {
                    elements.add(visitLiteral(literalCtx));
                }
            }
            return new ArrayValueNode(elements);
        }
        // LPAREN annotationObject RPAREN -> Added this case if grammar supports object literals in annotations
        // else if (ctx.annotationObject() != null) { ... handle object ... }

        System.err.println("Warning: Unhandled annotation value structure: " + ctx.getText());
        return new NullValueNode();
    }


    @Override public ValueNode visitLiteral(LiteralContext ctx) {
        if (ctx.STRING() != null) {
            return new StringValueNode(stripQuotes(ctx.STRING().getText()));
        } else if (ctx.INT() != null) {
             try {
                 return new NumberValueNode(Long.parseLong(ctx.INT().getText()));
             } catch (NumberFormatException e) {
                 System.err.println("Warning: Could not parse INT literal: " + ctx.INT().getText());
                 return new NullValueNode();
             }
        } else if (ctx.FLOAT() != null) {
             try {
                 return new NumberValueNode(Double.parseDouble(ctx.FLOAT().getText()));
             } catch (NumberFormatException e) {
                 System.err.println("Warning: Could not parse FLOAT literal: " + ctx.FLOAT().getText());
                 return new NullValueNode();
             }
        } else if (ctx.BOOLEAN() != null) {
            return new BooleanValueNode(Boolean.parseBoolean(ctx.BOOLEAN().getText()));
        } else if (ctx.NULL() != null) {
            return new NullValueNode();
        }

        System.err.println("Warning: Unknown literal type: " + ctx.getText());
        return new NullValueNode();
    }

     @Override
     public TypeExprNode visitTypeReference(TypeReferenceContext ctx) {
        if (ctx.simpleType() != null) {
            return (TypeExprNode) visitSimpleType(ctx.simpleType());
        } else if (ctx.listType() != null) {
            return (TypeExprNode) visitListType(ctx.listType());
        } else if (ctx.mapType() != null) {
            return (TypeExprNode) visitMapType(ctx.mapType());
        } else if (ctx.optionalType() != null) {
            return (TypeExprNode) visitOptionalType(ctx.optionalType());
        } else if (ctx.ID() != null) {
            return new RefTypeNode(ctx.ID().getText());
        }
        // Should not happen based on grammar
        throw new IllegalStateException("Invalid TypeReference context: " + ctx.getText());
     }

      @Override public AstNode visitSimpleType(SimpleTypeContext ctx) {
          // Grammar: simpleType: PRIMITIVE_TYPE | TIMESTAMP_TYPE ;
          String typeName = ctx.getText(); // Get the full text (e.g., "string", "int", "timestamp")
          try {
              // Attempt to parse as a known primitive type
              // Convert text to uppercase to match enum constants (STRING, INT, BOOLEAN, etc.)
              PrimitiveTypeNode.PrimitiveType primitive = PrimitiveTypeNode.PrimitiveType.valueOf(typeName.toUpperCase());
              return new PrimitiveTypeNode(primitive);
          } catch (IllegalArgumentException e) {
              // If not a primitive, handle other cases like TIMESTAMP_TYPE or error
              if ("timestamp".equalsIgnoreCase(typeName)) {
                    // Assuming TIMESTAMP is a distinct type or represented differently
                    // For now, maybe treat as RefTypeNode or a specific TimestampTypeNode if it exists
                    // return new TimestampTypeNode(); // If TimestampTypeNode exists
                    System.err.println("Warning: Timestamp type handling needs specific AST node.");
                    return new RefTypeNode(typeName); // Fallback
              } else {
                  System.err.println("Error: Unknown simple type: " + typeName);
                  return new RefTypeNode("ERROR_UNKNOWN_SIMPLE_TYPE"); // Error node
              }
          }
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


    // Helper to extract annotations from a list of AnnotationContexts
    private List<AnnotationNode> extractAnnotations(List<AnnotationContext> annotationCtxs) {
        List<AnnotationNode> annotations = new ArrayList<>();
        if (annotationCtxs != null) {
            for (AnnotationContext annotationCtx : annotationCtxs) {
                // Visit should return AnnotationNode based on visitIdAnnotation/visitValueAnnotation
                AstNode visitedNode = visit(annotationCtx);
                if (visitedNode instanceof AnnotationNode) {
                    annotations.add((AnnotationNode) visitedNode);
                } else if (visitedNode != null) {
                     System.err.println("Warning: Visiting AnnotationContext did not yield AnnotationNode: " + visitedNode.getClass().getName());
                } else {
                     System.err.println("Warning: Visiting AnnotationContext returned null: " + annotationCtx.getText());
                }
            }
        }
        return annotations;
    }

     // Helper to convert a list of AnnotationNodes into a map for AST nodes
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

      // Helper to extract annotations from various contexts that might have them
      // This helper is largely superseded by direct access in visitFile using specific context types.
      // Keeping it for now in case it's used by other methods for non-DefinitionBlock contexts.
      private List<AnnotationNode> extractAnnotationsFromContext(ParseTree ctx) {
          if (ctx instanceof StructFieldDefinitionContext) { // Example: field annotations
               StructFieldDefinitionContext sfCtx = (StructFieldDefinitionContext) ctx;
               if (sfCtx.annotation() != null && !sfCtx.annotation().isEmpty()) {
                   return extractAnnotations(sfCtx.annotation());
               }
          } else if (ctx instanceof EnumVariantDefinitionContext) { // Example: enum variant annotations
              EnumVariantDefinitionContext evCtx = (EnumVariantDefinitionContext) ctx;
              if (evCtx.annotation() != null && !evCtx.annotation().isEmpty()) {
                  return extractAnnotations(evCtx.annotation());
              }
          } else if (ctx instanceof StateBodyContext) { // Annotations within a state body's {} block
              StateBodyContext sbCtx = (StateBodyContext) ctx;
              if (sbCtx.annotation() != null && !sbCtx.annotation().isEmpty()) {
                  return extractAnnotations(sbCtx.annotation());
              }
          }
          // DefinitionBlockContext annotations are handled directly in visitFile.
          return Collections.emptyList();
      }

    // --- Implementation for Block Content Definitions ---

    // Types Block visitors
    @Override
    public AstNode visitStructDefinition(StructDefinitionContext ctx) {
        // Annotations are handled by the parent TypeDefinition or DefinitionBlock
        String name = ctx.ID().getText();
        List<FieldNode> fields = new ArrayList<>();
        if (ctx.structFieldDefinition() != null) {
            for (StructFieldDefinitionContext fieldCtx : ctx.structFieldDefinition()) {
                 AstNode visitedField = visit(fieldCtx);
                 if (visitedField instanceof FieldNode) {
                    fields.add((FieldNode) visitedField);
                 } else {
                     System.err.println("Warning: Visiting struct field did not yield FieldNode: " + fieldCtx.getText());
                 }
            }
        }
        // Constructor: TypeDefNode(Optional<Long> id, String name, TypeKind kind, List<FieldNode> fields, List<EnumVariantNode> variants, List<AnnotationNode> annotationsList)
        // ID and annotations come from parent. Pass empty for annotationsList for now.
        return new TypeDefNode(Optional.empty(), name, TypeDefNode.TypeKind.STRUCT, fields, null, Collections.emptyList());
    }

    @Override
    public AstNode visitEnumDefinition(EnumDefinitionContext ctx) {
        // Annotations are handled by the parent TypeDefinition or DefinitionBlock
        String name = ctx.ID().getText();
        List<EnumVariantNode> variants = new ArrayList<>();
        if (ctx.enumVariantDefinition() != null) {
            for (EnumVariantDefinitionContext variantCtx : ctx.enumVariantDefinition()) {
                 AstNode visitedVariant = visit(variantCtx);
                 if (visitedVariant instanceof EnumVariantNode) {
                    variants.add((EnumVariantNode) visitedVariant);
                 } else {
                      System.err.println("Warning: Visiting enum variant did not yield EnumVariantNode: " + variantCtx.getText());
                 }
            }
        }
         // Constructor: TypeDefNode(Optional<Long> id, String name, TypeKind kind, List<FieldNode> fields, List<EnumVariantNode> variants, List<AnnotationNode> annotationsList)
         // ID and annotations come from parent. Pass empty for annotationsList for now.
        return new TypeDefNode(Optional.empty(), name, TypeDefNode.TypeKind.ENUM, null, variants, Collections.emptyList());
    }

     // Service Block visitors
    @Override
    public AstNode visitInterfaceDefinition(InterfaceDefinitionContext ctx) {
        // ID/Annotations are handled by parent DefinitionBlock
        String name = ctx.ID().getText();
         List<MethodNode> methods = new ArrayList<>();
         List<AnnotationNode> innerAnnotations = new ArrayList<>(); // Annotations inside interface {} block

         if (ctx.annotation() != null) {
              innerAnnotations.addAll(extractAnnotations(ctx.annotation()));
         }
         if (ctx.methodDefinition() != null) {
             for (MethodDefinitionContext methodCtx : ctx.methodDefinition()) {
                 AstNode visitedMethod = visit(methodCtx);
                 if (visitedMethod instanceof MethodNode) {
                     methods.add((MethodNode)visitedMethod);
                 } else {
                      System.err.println("Warning: Visiting method definition did not yield MethodNode: " + methodCtx.getText());
                 }
             }
         }
         // Constructor: InterfaceNode(Optional<Long> id, String name, List<MethodNode> methods, List<AnnotationNode> innerAnnotations)
         // ID comes from parent block, pass placeholder. Pass extracted inner annotations.
        return new InterfaceNode(Optional.empty(), name, methods, innerAnnotations);
    }

    @Override
    public AstNode visitServiceDefinition(ServiceDefinitionContext ctx) {
         // ID/Annotations are handled by parent DefinitionBlock
         String name = ctx.ID().getText();
         List<AnnotationNode> innerAnnotations = new ArrayList<>(); // Annotations inside service {} block (e.g., $implements)
         if (ctx.annotation() != null) {
             innerAnnotations.addAll(extractAnnotations(ctx.annotation()));
         }
         // Constructor: ServiceDefinitionNode(Optional<Long> id, String name, List<AnnotationNode> annotations, List<InterfaceNode> implementedInterfaces, List<MethodNode> methods)
         // ID comes from parent block, pass placeholder. Pass empty lists for implementedInterfaces and methods.
        return new ServiceDefinitionNode(Optional.empty(), name, innerAnnotations, Collections.emptyList(), Collections.emptyList());
    }

    // Actor Block visitor
    @Override
    public AstNode visitActorDefinition(ActorDefinitionContext ctx) {
        // Outer annotations are handled by parent DefinitionBlock
        String name = ctx.ID().getText();
        List<AnnotationNode> innerAnnotations = new ArrayList<>();
         if (ctx.annotation() != null) {
             innerAnnotations.addAll(extractAnnotations(ctx.annotation()));
         }
         // Constructor: ActorNode(Optional<Long> id, String name, List<AnnotationNode> innerAnnotations)
         // ID comes from parent block, pass placeholder.
        return new ActorNode(Optional.empty(), name, innerAnnotations);
    }

    // Machine Block visitor
    @Override
    public AstNode visitMachineDefinition(MachineDefinitionContext ctx) {
         // ID/Annotations are handled by parent DefinitionBlock
        String name = ctx.ID().getText();
        // Process machine body elements (states, actions, guards)
         ContextNode context = null; // TODO: Implement context parsing
         List<ActionDefinitionNode> actions = new ArrayList<>();
         List<GuardDefinitionNode> guards = new ArrayList<>();
         List<InvokeDefinitionNode> invokes = new ArrayList<>(); // Assuming invokes are defined here? Grammar shows them in states.
         List<StateNode> states = new ArrayList<>(); // This list should contain all states, including history states
         Optional<String> initialStateNameOpt = Optional.empty(); // Corrected from initialState
         // List<HistoryStateNode> historyStates = new ArrayList<>(); // History states will be part of the states list

         // Iterate through machineBodyElement
         if (ctx.machineBodyElement() != null) {
             for (MachineBodyElementContext elementCtx : ctx.machineBodyElement()) {
                 if (elementCtx.statesDefinition() != null) {
                     // visitStatesDefinition populates the 'states' list (which includes history states)
                     visitStatesDefinition(elementCtx.statesDefinition(), states /*, historyStates */); // historyStates removed as it's merged
                     // Extract initial state if defined within statesDefinition
                     if (elementCtx.statesDefinition().initialStateDefinition() != null) {
                         initialStateNameOpt = Optional.of(elementCtx.statesDefinition().initialStateDefinition().ID().getText());
                     }
                 } else if (elementCtx.actionsDefinition() != null) {
                     actions.addAll(visitActionsDefinitionHelper(elementCtx.actionsDefinition())); // Helper needed
                 } else if (elementCtx.guardsDefinition() != null) {
                      guards.addAll(visitGuardsDefinitionHelper(elementCtx.guardsDefinition())); // Helper needed
                 } else {
                      System.err.println("Warning: Unknown machine body element: " + elementCtx.getText());
                 }
             }
         }

        // Constructor: MachineDefinitionNode(Optional<Long> id, String machineName, Map<String, Object> annotations, ContextNode context, List<ActionDefinitionNode> actions, List<GuardDefinitionNode> guards, List<InvokeDefinitionNode> invokes, List<StateNode> states, String initialStateName)
        // ID from parent block. Pass emptyMap for annotations for now.
        return new MachineDefinitionNode(Optional.empty(), name, Collections.emptyMap(), context, actions, guards, invokes, states, initialStateNameOpt.orElse(null));
    }

     // Communication Block visitors
    @Override
    public AstNode visitProtocolDefinition(ProtocolDefinitionContext ctx) {
         // ID/Annotations are handled by parent DefinitionBlock
         String name = ctx.ID().getText();
         List<AnnotationNode> innerAnnotations = new ArrayList<>();
         if (ctx.annotation() != null) {
             innerAnnotations.addAll(extractAnnotations(ctx.annotation()));
         }
         // Constructor: ProtocolNode(Optional<Long> id, String name, List<AnnotationNode> innerAnnotations)
         // ID comes from parent block, pass placeholder.
        return new ProtocolNode(Optional.empty(), name, innerAnnotations);
    }

    @Override
    public AstNode visitChannelDefinition(ChannelDefinitionContext ctx) {
        // ID/Annotations are handled by parent DefinitionBlock
        String name = ctx.ID().getText();
        List<AnnotationNode> innerAnnotations = new ArrayList<>();
        if (ctx.annotation() != null) {
            innerAnnotations.addAll(extractAnnotations(ctx.annotation()));
        }
        // Constructor: ChannelNode(Optional<Long> id, String name, List<AnnotationNode> innerAnnotations)
        // ID comes from parent block, pass placeholder.
        return new ChannelNode(Optional.empty(), name, innerAnnotations);
    }

    @Override
    public AstNode visitEventDefinition(EventDefinitionContext ctx) {
        // ID/Annotations are handled by parent DefinitionBlock
        String name = ctx.ID().getText();
        List<AnnotationNode> innerAnnotations = new ArrayList<>();
        List<FieldNode> fields = new ArrayList<>();

        if (ctx.annotation() != null) {
            innerAnnotations.addAll(extractAnnotations(ctx.annotation()));
        }
        if (ctx.eventFieldDefinition() != null) {
            for (EventFieldDefinitionContext fieldCtx : ctx.eventFieldDefinition()) {
                 // Assuming eventFieldDefinition is similar to structFieldDefinition
                 // Need visitEventFieldDefinition method
                 AstNode visitedField = visitEventFieldDefinition(fieldCtx); // Call specific visitor
                 if (visitedField instanceof FieldNode) {
                    fields.add((FieldNode) visitedField);
                 } else {
                      System.err.println("Warning: Visiting event field did not yield FieldNode: " + fieldCtx.getText());
                 }
            }
        }
        // Constructor: EventNode(Optional<Long> id, String name, List<FieldNode> fields, List<AnnotationNode> innerAnnotations)
        // ID comes from parent block, pass placeholder.
        return new EventNode(Optional.empty(), name, fields, innerAnnotations);
    }

    // Helper visitor for event fields (similar to struct fields)
    @Override
    public AstNode visitEventFieldDefinition(EventFieldDefinitionContext ctx) {
        List<AnnotationNode> annotations = Collections.emptyList();
        if (ctx.annotation() != null && !ctx.annotation().isEmpty()) {
            annotations = extractAnnotations(ctx.annotation());
        }
        Optional<Long> id = extractIdFromList(annotations);
        String name = ctx.ID().getText(); // Reverted: Use ID()
        TypeExprNode type = (TypeExprNode) visit(ctx.typeReference());
        return new FieldNode(id, name, type, annotations);
    }

     // --- Helper visitors for Machine Definition Body ---

    public List<ActionDefinitionNode> visitActionsDefinitionHelper(ActionsDefinitionContext ctx) {
        List<ActionDefinitionNode> actions = new ArrayList<>();
        if (ctx.actionDefinition() != null) {
            for(ActionDefinitionContext actionCtx : ctx.actionDefinition()) {
                AstNode visitedAction = visitActionDefinition(actionCtx); // Need visitActionDefinition
                if (visitedAction instanceof ActionDefinitionNode) {
                    actions.add((ActionDefinitionNode)visitedAction);
                } else {
                     System.err.println("Warning: Visiting action definition did not yield ActionDefinitionNode: " + actionCtx.getText());
                }
            }
        }
        return actions;
    }

     public List<GuardDefinitionNode> visitGuardsDefinitionHelper(GuardsDefinitionContext ctx) {
        List<GuardDefinitionNode> guards = new ArrayList<>();
        if (ctx.guardDefinition() != null) {
            for(GuardDefinitionContext guardCtx : ctx.guardDefinition()) {
                 AstNode visitedGuard = visitGuardDefinition(guardCtx); // Need visitGuardDefinition
                 if (visitedGuard instanceof GuardDefinitionNode) {
                    guards.add((GuardDefinitionNode)visitedGuard);
                 } else {
                     System.err.println("Warning: Visiting guard definition did not yield GuardDefinitionNode: " + guardCtx.getText());
                 }
            }
        }
        return guards;
     }

     // Need to implement visitActionDefinition, visitGuardDefinition
     public AstNode visitActionDefinition(ActionDefinitionContext ctx) {
         // Grammar: ID LPAREN paramList? RPAREN SEMI
         String name = ctx.ID().getText();
         List<ParameterNode> params = new ArrayList<>();
         if (ctx.paramList() != null) {
             params.addAll(visitParamListHelper(ctx.paramList())); // Use Helper suffix
         }
         // ID/Annotations handled by parent block?
         // Constructor: ActionDefinitionNode(Optional<Long> id, String actionName, List<ParameterNode> parameters, Optional<TypeExprNode> returnType, List<AnnotationNode> annotations)
         return new ActionDefinitionNode(Optional.empty(), name, params, Optional.empty(), Collections.emptyList());
     }

     public AstNode visitGuardDefinition(GuardDefinitionContext ctx) {
         // Grammar: ID LPAREN paramList? RPAREN (COLON typeReference)? SEMI
         String name = ctx.ID().getText();
         List<ParameterNode> params = new ArrayList<>();
         if (ctx.paramList() != null) {
             params.addAll(visitParamListHelper(ctx.paramList())); // Use Helper suffix
         }
         Optional<TypeExprNode> returnTypeOpt = Optional.empty();
         if (ctx.typeReference() != null) {
             returnTypeOpt = Optional.of((TypeExprNode) visit(ctx.typeReference()));
         }
          // ID/Annotations handled by parent block?
          // Constructor: GuardDefinitionNode(Optional<Long> id, String guardName, List<AnnotationNode> annotations, TypeExprNode returnType, String expression, Map<String, TypeExprNode> parameters)
         Map<String, TypeExprNode> paramMap = new HashMap<>();
         for (ParameterNode p : params) {
             paramMap.put(p.getName(), p.getType());
         }
         // Guards must return boolean. If not specified, assume boolean.
         // If specified and not boolean, GuardDefinitionNode constructor logs a warning and sets it to boolean.
         TypeExprNode effectiveReturnType = returnTypeOpt.orElse(new PrimitiveTypeNode(PrimitiveTypeNode.PrimitiveType.BOOL));


         return new GuardDefinitionNode(Optional.empty(), name, Collections.emptyList(), effectiveReturnType, null, paramMap);
     }

     public List<ParameterNode> visitParamListHelper(ParamListContext ctx) { // Rename definition
         List<ParameterNode> params = new ArrayList<>();
         if (ctx.parameter() != null) {
             for (ParameterContext paramCtx : ctx.parameter()) {
                 AstNode visitedParam = visitParameter(paramCtx); // Need visitParameter
                 if (visitedParam instanceof ParameterNode) {
                     params.add((ParameterNode)visitedParam);
                 } else {
                     System.err.println("Warning: Visiting parameter did not yield ParameterNode: " + paramCtx.getText());
                 }
             }
         }
         return params;
     }

      public AstNode visitParameter(ParameterContext ctx) {
         // Grammar: ID COLON typeReference
         String name = ctx.ID().getText(); // Reverted: Use ID()
         TypeExprNode type = (TypeExprNode) visit(ctx.typeReference());
         return new ParameterNode(name, type);
      }


    public void visitStatesDefinition(StatesDefinitionContext ctx, List<StateNode> states /*, List<HistoryStateNode> historyStates Removed */) {
        // Handles initialStateDefinition? stateDefinitionOrHistoryState*
        // Initial state name is extracted in visitMachineDefinition
        if (ctx.stateDefinitionOrHistoryState() != null) {
            for (StateDefinitionOrHistoryStateContext stateOrHistCtx : ctx.stateDefinitionOrHistoryState()) {
                AstNode visitedNode = visit(stateOrHistCtx);
                if (visitedNode instanceof StateNode) { // This includes HistoryStateNode if it extends StateNode
                    states.add((StateNode) visitedNode);
                // } else if (visitedNode instanceof HistoryStateNode) { // This case might be redundant if HistoryStateNode is a StateNode
                // historyStates.add((HistoryStateNode) visitedNode);
                } else if (visitedNode != null) {
                     System.err.println("Warning: Visiting stateDefinitionOrHistoryState did not yield StateNode: " + visitedNode.getClass().getName() + " for text: " + stateOrHistCtx.getText());
                } else {
                    System.err.println("Warning: Visiting stateDefinitionOrHistoryState returned null for text: " + stateOrHistCtx.getText());
                }
            }
        }
    }

} // End of class AstBuilderVisitor

