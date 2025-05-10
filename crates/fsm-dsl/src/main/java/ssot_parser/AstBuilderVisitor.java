package ssot_parser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.antlr.v4.runtime.tree.ParseTree; // Import needed for context checks
import java.util.Map;
import java.util.HashMap;
// import ssot_parser.SSoTParser.AnnotationContext; // Removed
import ssot_parser.SSoTParser.*; // Added for all parser contexts
import ssot_parser.ast.*;
import ssot_parser.ast.nodes.*;
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
import ssot_parser.ast.handlers.InvokeCompletionHandler;
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

    private InvokeCompletionHandler parseInvokeCompletionHandler(InvokeCompletionContext ctx) {
        if (ctx == null) return null; // Or throw, or return an empty handler

        List<AnnotationNode> handlerAnnotations = ctx.annotation() != null ? extractAnnotations(ctx.annotation()) : Collections.emptyList();
        List<ActionReferenceNode> handlerActionRefs = new ArrayList<>();
        Optional<TransitionSpecNode> handlerTransitionSpec = Optional.empty();

        if (ctx.actionReferenceList() != null) {
            // Assuming extractActionReferenceNodes returns List<ActionReferenceNode>
            handlerActionRefs.addAll(extractActionReferenceNodes(ctx.actionReferenceList()));
        } else if (ctx.transitionSpec() != null) {
            handlerTransitionSpec = Optional.ofNullable((TransitionSpecNode) visitTransitionSpec(ctx.transitionSpec()));
        }
        // InvokeCompletionHandler(List<ActionReferenceNode> actions, Optional<TransitionSpecNode> transitionSpec, List<AnnotationNode> annotations)
        return new InvokeCompletionHandler(handlerActionRefs, handlerTransitionSpec, handlerAnnotations);
    }

    @Override
    public AstNode visitFile(FileContext ctx) {
        System.out.println("Visiting File (was SsotDefinition)");

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

        Map<String, Object> rootAnnotationsMap = new HashMap<>();
        Optional<Long> fileIdFromAnnotation = Optional.empty();

        List<AnnotationContext> topLevelAnnotationCtxs = new ArrayList<>();
        if (ctx.annotation() != null && !ctx.annotation().isEmpty()) {
            boolean importsOrBlocksStarted = false;
            for (AnnotationContext annoCtx : ctx.annotation()) {
                boolean isAfterImports = ctx.importStatement(0) != null && annoCtx.getStart().getTokenIndex() > ctx.importStatement(0).getStart().getTokenIndex();
                boolean isAfterBlocks = ctx.definitionBlock(0) != null && annoCtx.getStart().getTokenIndex() > ctx.definitionBlock(0).getStart().getTokenIndex();
                importsOrBlocksStarted = isAfterImports || isAfterBlocks;

                if (!importsOrBlocksStarted) {
                    topLevelAnnotationCtxs.add(annoCtx);
                } else {
                    System.out.println("Found annotation after imports/blocks started, not treating as top-level file annotation: " + annoCtx.getText());
                }
            }
        }

        List<AnnotationNode> parsedTopLevelAnnotations = extractAnnotations(topLevelAnnotationCtxs);
        for (AnnotationNode annotation : parsedTopLevelAnnotations) {
            if (annotation.name.equals("id") && annotation.value instanceof Number) {
                fileIdFromAnnotation = Optional.of(((Number) annotation.value).longValue());
                System.out.println("  Set Root ID from @id annotation: " + annotation.value);
            } else if (annotation.name.equals("id") && annotation.value instanceof String && ((String)annotation.value).matches("\\d+")) {
                 try {
                    fileIdFromAnnotation = Optional.of(Long.parseLong((String)annotation.value));
                    System.out.println("  Set Root ID from @id annotation (string parsed to long): " + annotation.value);
                 } catch (NumberFormatException e) {
                    System.err.println("Error parsing @id string value: " + annotation.value);
                 }
            }else {
                rootAnnotationsMap.put(annotation.name, Optional.ofNullable(annotation.value).orElse(true));
                System.out.println("  Found Root Annotation: " + annotation);
            }
        }

        Optional<Long> finalFileId = fileIdFromHex.isPresent() ? fileIdFromHex : fileIdFromAnnotation;
        if (fileIdFromHex.isPresent() && fileIdFromAnnotation.isPresent()) {
            System.err.println("Warning: Both fileId (e.g. @0x123) and @id annotation found for file. Using fileId (@0x...). Found @id: " + fileIdFromAnnotation.get());
        }

        SsotRoot root = new SsotRoot(
                new ArrayList<>(), // imports
                new ArrayList<>(), // typeDefinitions
                new ArrayList<>(), // serviceDefinitions
                new ArrayList<>(), // machineDefinitions
                new ArrayList<>(), // actorDefinitions
                new ArrayList<>(), // communicationDefinitions
                finalFileId,       // ID for the SsotRoot
                rootAnnotationsMap // annotations for the SsotRoot
        );

        if (ctx.importStatement() != null) {
            for (ImportStatementContext importCtx : ctx.importStatement()) {
                root.getImports().add((ImportNode) visitImportStatement(importCtx));
            }
        }

        if (ctx.definitionBlock() != null) {
            for (DefinitionBlockContext blockCtx : ctx.definitionBlock()) {
                AstNode visitedNode = visit(blockCtx); 
                if (visitedNode instanceof BlockContainerNode) {
                    BlockContainerNode container = (BlockContainerNode) visitedNode;
                    System.out.println("Processing BlockContainer: " + container.blockType + " with " + container.getChildren().size() + " children.");

                    switch (container.blockType) {
                        case "types":
                            container.getChildren().forEach(child -> {
                                if (child instanceof TypeDefNode) { 
                                    root.getTypeDefinitions().add((TypeDefNode) child);
                                } else {
                                    System.err.println("Warning: Child in types block is not TypeDefNode: " + child.getClass().getName());
                                }
                            });
                            break;
                        case "services":
                            container.getChildren().forEach(child -> {
                                if (child instanceof ServiceDefinitionNode) {
                                    root.getServiceDefinitions().add((ServiceDefinitionNode) child);
                                } else if (child instanceof InterfaceNode) {
                                    System.err.println("InterfaceNode cannot be directly added to List<ServiceDefinitionNode>. Needs SsotRoot modification or different handling for interfaces.");
                                } else {
                                     System.err.println("Warning: Child in services block is not ServiceDefinitionNode or InterfaceNode: " + child.getClass().getName());
                                }
                            });
                            break;
                        case "machines":
                            container.getChildren().forEach(child -> {
                                if (child instanceof MachineNode) { 
                                    root.getMachineDefinitions().add((MachineNode) child);
                                } else {
                                     System.err.println("Warning: Child in machines block is not MachineNode: " + child.getClass().getName());
                                }
                            });
                            break;
                        case "actors":
                             container.getChildren().forEach(child -> {
                                 if (child instanceof ActorNode) {
                                     root.getActorDefinitions().add((ActorNode) child);
                                 } else {
                                     System.err.println("Warning: Child in actors block is not ActorNode: " + child.getClass().getName());
                                 }
                             });
                             break;
                        case "communication":
                            container.getChildren().forEach(child -> {
                                if (child instanceof ProtocolNode || child instanceof ChannelNode || child instanceof EventNode) {
                                    root.getCommunicationDefinitions().add(child); 
                                } else {
                                    System.err.println("Warning: Child in communication block is not a known comm element: " + child.getClass().getName());
                                }
                            });
                            break;
                        default:
                            System.err.println("Warning: Unhandled block container type in visitFile: " + container.blockType);
                            break;
                    }
                } else if (visitedNode != null) {
                    System.err.println("Warning: Visited definition block did not return a BlockContainerNode. Node type: " + visitedNode.getClass().getName());
                }
            }
        }
        System.out.println("Finished Visiting File (was SsotDefinition)");
        return root;
    }

    @Override
    public AstNode visitImportStatement(ImportStatementContext ctx) {
        String path = stripQuotes(ctx.STRING().getText());
        // Assuming ImportNode has a constructor like this, adjust as necessary
        return new ImportNode(path, Optional.empty(), new HashMap<>());
    }

    @Override
    public AstNode visitTypesBlock(TypesBlockContext ctx) {
        System.out.println("Visiting Types Block");
        List<AnnotationNode> blockAnnotations = extractAnnotations(ctx.annotation());
        List<AstNode> typeDefs = new ArrayList<>();
        if (ctx.typeDefinition() != null) {
            for (TypeDefinitionContext typeCtx : ctx.typeDefinition()) {
                TypeDefNode typeDef = (TypeDefNode) visit(typeCtx);
                if (typeDef != null) {
                    typeDefs.add(typeDef);
                }
            }
        }
        return new BlockContainerNode("types", typeDefs, blockAnnotations);
    }

    @Override
    public AstNode visitServicesBlock(ServicesBlockContext ctx) {
        System.out.println("Visiting Services Block");
        List<AnnotationNode> blockAnnotations = extractAnnotations(ctx.annotation());
        List<AstNode> serviceElements = new ArrayList<>(); // Changed from serviceDefs to serviceElements

        // Grammar: servicesBlock: SERVICES LBRACE annotation* serviceElement* RBRACE;
        // serviceElement: interfaceDefinition | serviceDefinition ;
        if (ctx.serviceElement() != null && !ctx.serviceElement().isEmpty()) {
            for (ServiceElementContext elementCtx : ctx.serviceElement()) {
                AstNode visitedElement = visit(elementCtx); // Visit the serviceElement itself
                if (visitedElement instanceof ServiceDefinitionNode || visitedElement instanceof InterfaceNode) {
                    serviceElements.add(visitedElement);
                } else if (visitedElement != null) {
                    System.err.println("Expected ServiceDefinitionNode or InterfaceNode from serviceElement, got: " + visitedElement.getClass().getName());
                } else {
                     System.err.println("Warning: visitServiceElement returned null for: " + elementCtx.getText());
                }
            }
        } else {
            System.out.println("No service elements found in services block.");
        }
        return new BlockContainerNode("services", serviceElements, blockAnnotations);
    }

    @Override
    public AstNode visitMachinesBlock(MachinesBlockContext ctx) {
        System.out.println("Visiting Machines Block");
        List<AnnotationNode> blockAnnotations = extractAnnotations(ctx.annotation());
        List<AstNode> machineDefs = new ArrayList<>();
        if (ctx.machineDefinition() != null) {
            for (MachineDefinitionContext machineCtx : ctx.machineDefinition()) {
                MachineNode machineDef = (MachineNode) visit(machineCtx);
                if (machineDef != null) {
                    machineDefs.add(machineDef);
                }
            }
        }
        return new BlockContainerNode("machines", machineDefs, blockAnnotations);
    }

    // Add visitServiceElement if not present (it should be generated by ANTLR if serviceElement is a rule)
    // If serviceElement is a labeled alternative in the grammar, ANTLR might generate visitInterfaceDefinition and visitServiceDefinition directly.
    // Let's assume SSoTBaseVisitor will have visitServiceElement or we can rely on visit(elementCtx) dispatching correctly.
    // If not, we might need:
    @Override
    public AstNode visitServiceElement(ServiceElementContext ctx) {
        if (ctx.interfaceDefinition() != null) {
            return visitInterfaceDefinition(ctx.interfaceDefinition());
        } else if (ctx.serviceDefinition() != null) {
            return visitServiceDefinition(ctx.serviceDefinition());
        }
        System.err.println("Unknown service element type: " + ctx.getText());
        return null;
    }


    @Override
    public AstNode visitTypeDefinition(TypeDefinitionContext ctx) {
        if (ctx.structDefinition() != null) {
            return visitStructDefinition(ctx.structDefinition());
        } else if (ctx.enumDefinition() != null) {
            return visitEnumDefinition(ctx.enumDefinition());
        }
        return null;
    }

    @Override
    public AstNode visitStructDefinition(StructDefinitionContext ctx) {
        String structName = ctx.ID().getText();
        List<AnnotationContext> structLevelAnnoCtxs = new ArrayList<>();
        List<AnnotationContext> bodyLevelAnnoCtxs = new ArrayList<>();

        if (ctx.annotation() != null && !ctx.annotation().isEmpty()) {
            for (AnnotationContext annoCtx : ctx.annotation()) {
                if (ctx.LBRACE() != null && annoCtx.getSourceInterval().a < ctx.LBRACE().getSymbol().getTokenIndex()) {
                    structLevelAnnoCtxs.add(annoCtx);
                } else {
                    bodyLevelAnnoCtxs.add(annoCtx);
                }
            }
        }
        
        List<AnnotationNode> structAnnotations = extractAnnotations(structLevelAnnoCtxs);
        Optional<Long> id = extractIdFromList(structAnnotations);

        List<AnnotationNode> structBodyAnnotations = extractAnnotations(bodyLevelAnnoCtxs);
        structAnnotations.addAll(structBodyAnnotations);

        List<FieldNode> fields = new ArrayList<>();
        if (ctx.fieldDefinition() != null) {
            for (FieldDefinitionContext fieldCtx : ctx.fieldDefinition()) {
                fields.add((FieldNode) visitFieldDefinition(fieldCtx));
            }
        }
        // Ensure TypeDefNode constructor matches: id, name, kind, fields, variants, annotations
        return new TypeDefNode(id, structName, TypeDefNode.TypeKind.STRUCT, fields, null /*variants*/, structAnnotations);
    }

    @Override
    public AstNode visitEnumDefinition(EnumDefinitionContext ctx) {
        String enumName = ctx.ID().getText();
        List<AnnotationContext> enumLevelAnnoCtxs = new ArrayList<>();
        List<AnnotationContext> bodyLevelAnnoCtxs = new ArrayList<>();

        if (ctx.annotation() != null && !ctx.annotation().isEmpty()) {
            for (AnnotationContext annoCtx : ctx.annotation()) {
                if (ctx.LBRACE() != null && annoCtx.getSourceInterval().a < ctx.LBRACE().getSymbol().getTokenIndex()) {
                    enumLevelAnnoCtxs.add(annoCtx);
                } else {
                    bodyLevelAnnoCtxs.add(annoCtx);
                }
            }
        }

        List<AnnotationNode> enumAnnotations = extractAnnotations(enumLevelAnnoCtxs);
        Optional<Long> id = extractIdFromList(enumAnnotations);

        List<AnnotationNode> enumBodyAnnotations = extractAnnotations(bodyLevelAnnoCtxs);
        enumAnnotations.addAll(enumBodyAnnotations);

        List<EnumVariantNode> variants = new ArrayList<>();
        if (ctx.enumVariant() != null) {
            for (EnumVariantContext variantCtx : ctx.enumVariant()) {
                variants.add((EnumVariantNode) visitEnumVariant(variantCtx));
            }
        }
        // Ensure TypeDefNode constructor matches: id, name, kind, fields, variants, annotations
        return new TypeDefNode(id, enumName, TypeDefNode.TypeKind.ENUM, null /*fields*/, variants, enumAnnotations);
    }

    @Override
    public AstNode visitFieldDefinition(FieldDefinitionContext ctx) {
        String fieldName = ctx.ID().getText();
        ssot_parser.ast.type.TypeExprNode type = (ssot_parser.ast.type.TypeExprNode) visitTypeExpr(ctx.typeExpr());
        List<AnnotationNode> annotations = extractAnnotations(ctx.annotation());
        return new FieldNode(extractIdFromList(annotations), fieldName, type, annotations);
    }

     @Override
     public AstNode visitEnumVariant(EnumVariantContext ctx) {
         String variantName = ctx.ID().getText();
         List<AnnotationNode> annotations = ctx.annotation() != null ? extractAnnotations(ctx.annotation()) : Collections.emptyList();
         return new EnumVariantNode(extractIdFromList(annotations), annotations, variantName);
     }

    @Override
    public AstNode visitMachineDefinition(MachineDefinitionContext ctx) {
        String machineName = ctx.ID().getText(); 
        List<AnnotationNode> allMachineAnnotations = new ArrayList<>();
        Optional<Long> id = Optional.empty();
        String initialStateName = null; 
        if (ctx.annotation() != null && !ctx.annotation().isEmpty()) {
            List<AnnotationNode> machineHeaderAnnos = extractAnnotations(ctx.annotation());
            allMachineAnnotations.addAll(machineHeaderAnnos);
            id = extractIdFromList(machineHeaderAnnos);
            initialStateName = findAnnotationValue(machineHeaderAnnos, "$initial").orElse(null);
        }
        ContextNode contextNode = null;
        List<ActionDefinitionNode> actions = new ArrayList<>();
        List<GuardDefinitionNode> guards = new ArrayList<>();
        List<InvokeDefinitionNode> invokes = new ArrayList<>();
        List<AstNode> topLevelStates = new ArrayList<>(); 
        if (ctx.machineBodyElement() != null) {
            for (MachineBodyElementContext bodyElCtx : ctx.machineBodyElement()) {
                if (bodyElCtx.contextDefinition() != null) {
                    contextNode = (ContextNode) visitContextDefinition(bodyElCtx.contextDefinition());
                } else if (bodyElCtx.actionsDefinition() != null) {
                    BlockContainerNode actionsContainer = (BlockContainerNode) visitActionsDefinition(bodyElCtx.actionsDefinition());
                    actionsContainer.getChildren().forEach(child -> actions.add((ActionDefinitionNode) child));
                } else if (bodyElCtx.guardsDefinition() != null) {
                    BlockContainerNode guardsContainer = (BlockContainerNode) visitGuardsDefinition(bodyElCtx.guardsDefinition());
                    guardsContainer.getChildren().forEach(child -> guards.add((GuardDefinitionNode) child));
                } else if (bodyElCtx.invokesDefinition() != null) {
                    BlockContainerNode invokesContainer = (BlockContainerNode) visitInvokesDefinition(bodyElCtx.invokesDefinition());
                    invokesContainer.getChildren().forEach(child -> invokes.add((InvokeDefinitionNode) child));
                } else if (bodyElCtx.statesDefinition() != null) {
                    BlockContainerNode statesContainer = (BlockContainerNode) visitStatesDefinition(bodyElCtx.statesDefinition());
                    if (statesContainer != null) {
                        topLevelStates.addAll(statesContainer.getChildren()); 
                    }
                } else if (bodyElCtx.annotation() != null) {
                    allMachineAnnotations.add((AnnotationNode) visitAnnotation(bodyElCtx.annotation()));
                }
            }
        }
        if (initialStateName == null) {
            System.err.println("Warning: Machine '" + machineName + "' does not have an $initial state specified.");
            if (!topLevelStates.isEmpty() && topLevelStates.get(0) instanceof StateNode) {
                initialStateName = ((StateNode)topLevelStates.get(0)).getStateName();
                 System.err.println("Warning: Defaulting initial state for machine '" + machineName + "' to first state: " + initialStateName);
            }
        }
        return new MachineNode(id, machineName, initialStateName, Optional.ofNullable(contextNode), actions, guards, invokes, topLevelStates, allMachineAnnotations);
    }

    @Override
    public AstNode visitContextDefinition(ContextDefinitionContext ctx) {
        List<AnnotationNode> contextAnnotations = new ArrayList<>();
        if (ctx.annotation() != null && !ctx.annotation().isEmpty()) {
             contextAnnotations = extractAnnotations(ctx.annotation());
        }
        List<ContextVariableNode> variables = new ArrayList<>();
        if (ctx.contextField() != null) { 
            for (ContextFieldContext varCtx : ctx.contextField()) {
                variables.add((ContextVariableNode) visitContextField(varCtx));
            }
        }
        // The ContextNode itself might not have an ID or annotations at its root in the AST model.
        // Annotations from 'context annotation* {' are stored in 'contextAnnotations'.
        // These might need to be added to the MachineNode or handled differently.
        // For now, creating ContextNode with only the variables as per its likely design.
        return new ContextNode(variables, contextAnnotations); // Pass annotations to ContextNode
    }

    @Override
    public AstNode visitContextField(ContextFieldContext ctx) { 
        String varName = ctx.ID().getText();
        ssot_parser.ast.type.TypeExprNode type = (ssot_parser.ast.type.TypeExprNode) visitTypeExpr(ctx.typeExpr());
        List<AnnotationNode> annotationsList = extractAnnotations(ctx.annotation());
        Optional<Long> id = extractIdFromList(annotationsList);
        Optional<ValueNode> defaultValue = Optional.empty();
        // TODO: Parse default value if grammar allows, e.g. from an annotation like $default(...)
        // For now, ContextVariableNode constructor takes Optional<ValueNode> defaultValue
        return new ContextVariableNode(id, varName, type, annotationsList, defaultValue);
    }

    @Override
    public AstNode visitActionsDefinition(ActionsDefinitionContext ctx) { 
        List<AstNode> actionDefs = new ArrayList<>();
        if (ctx.actionDefinition() != null) {
            for (ActionDefinitionContext actionDefCtx : ctx.actionDefinition()) {
                actionDefs.add(visitActionDefinition(actionDefCtx));
            }
        }
        List<AnnotationNode> blockAnnotations = ctx.annotation() != null ? extractAnnotations(ctx.annotation()) : Collections.emptyList();
        return new BlockContainerNode("actions", actionDefs, blockAnnotations);
    }

    @Override
    public AstNode visitActionDefinition(ActionDefinitionContext ctx) {
        String name = ctx.ID(0).getText(); 
        List<AnnotationNode> allAnnotations = extractAnnotations(ctx.annotation()); 
        Optional<Long> id = extractIdFromList(allAnnotations);
        List<AnnotationNode> nonIdAnnotations = allAnnotations.stream()
                                                    .filter(a -> !(a.name.equals("id") && a.value instanceof Number))
                                                    .collect(Collectors.toList());
        Optional<String> eventParam = Optional.empty();
        Optional<String> contextParam = Optional.empty();
        Optional<TypeExprNode> returnType = Optional.empty();
        int idIndex = 1; 
        if (ctx.LPAREN() != null) {
            if (ctx.ID().size() > idIndex && ctx.ID(idIndex) != null) { 
                eventParam = Optional.of(ctx.ID(idIndex).getText());
                idIndex++;
                if (ctx.COMMA() != null && ctx.ID().size() > idIndex && ctx.ID(idIndex) != null) { 
                    contextParam = Optional.of(ctx.ID(idIndex).getText());
                    idIndex++;
                }
            }
        }
        if (ctx.typeExpr() != null) {
            returnType = Optional.of((TypeExprNode) visitTypeExpr(ctx.typeExpr()));
        }
        System.out.println("Creating ActionDefinitionNode for: " + name + " with id: " + id + ". Params/returnType not stored in AST yet.");
        return new ActionDefinitionNode(id, name, nonIdAnnotations);
    }

    @Override
    public AstNode visitGuardsDefinition(GuardsDefinitionContext ctx) { 
        List<AstNode> guardDefs = new ArrayList<>();
        if (ctx.guardDefinition() != null) {
            for (GuardDefinitionContext guardDefCtx : ctx.guardDefinition()) {
                guardDefs.add(visitGuardDefinition(guardDefCtx));
            }
        }
        List<AnnotationNode> blockAnnotations = ctx.annotation() != null ? extractAnnotations(ctx.annotation()) : Collections.emptyList();
        return new BlockContainerNode("guards", guardDefs, blockAnnotations);
    }

    @Override
    public AstNode visitGuardDefinition(GuardDefinitionContext ctx) {
        String name = ctx.ID(0).getText(); 
        List<AnnotationNode> allAnnotations = extractAnnotations(ctx.annotation());
        Optional<Long> id = extractIdFromList(allAnnotations);
        
        List<AnnotationNode> nonIdAnnotations = allAnnotations.stream()
                                                .filter(a -> !(a.name.equals("id") && a.value instanceof Number))
                                                .collect(Collectors.toList());

        Optional<String> eventParamName = Optional.empty();
        if (ctx.LPAREN() != null && ctx.ID().size() > 1) { 
            eventParamName = Optional.of(ctx.ID(1).getText()); 
        }

        // GuardDefinitionNode(Optional<Long> id, String guardName, List<AnnotationNode> annotations, TypeExprNode returnType, String expression, Map<String, TypeExprNode> parameters)
        TypeExprNode returnType = new PrimitiveTypeNode(PrimitiveTypeNode.PrimitiveType.BOOLEAN);
        String expression = name + "_expression_placeholder"; // Placeholder
        Map<String, TypeExprNode> parameters = new HashMap<>();
        if(eventParamName.isPresent()) {
            // Type of event param is unknown from grammar, using a placeholder TypeExprNode (e.g. any/object or a specific event type if known)
            parameters.put(eventParamName.get(), new RefTypeNode("Event")); // Placeholder for event type
        }

        System.out.println("Creating GuardDefinitionNode for: " + name + ". Expression/params are simplified.");
        return new GuardDefinitionNode(id, name, nonIdAnnotations, returnType, expression, parameters);
    }

     @Override
    public AstNode visitInvokesDefinition(InvokesDefinitionContext ctx) { 
        List<AstNode> invokeDefs = new ArrayList<>();
        if (ctx.invokeDefinition() != null) {
            for (InvokeDefinitionContext invokeDefCtx : ctx.invokeDefinition()) {
                invokeDefs.add(visitInvokeDefinition(invokeDefCtx));
            }
        }
        List<AnnotationNode> blockAnnotations = ctx.annotation() != null ? extractAnnotations(ctx.annotation()) : Collections.emptyList();
        return new BlockContainerNode("invokes", invokeDefs, blockAnnotations);
    }

     @Override
     public AstNode visitInvokeDefinition(InvokeDefinitionContext ctx) {
        String name = ctx.ID().getText();
        System.out.println("Visiting Invoke Definition: " + name);
        List<AnnotationNode> allInvokeAnnotations = extractAnnotations(ctx.annotation()); 
        Optional<Long> id = extractIdFromList(allInvokeAnnotations);
        List<AnnotationNode> nonIdAnnotations = allInvokeAnnotations.stream()
                                                    .filter(a -> !(a.name.equals("id") && a.value instanceof Number))
                                                    .collect(Collectors.toList());

        Optional<ValueNode> srcOpt = Optional.empty();
        Map<String, ValueNode> inputMapping = new HashMap<>(); // Parsed, but not used by current InvokeDefinitionNode constructor
        Map<String, ValueNode> outputMapping = new HashMap<>(); // Parsed, but not used
        Optional<InvokeCompletionHandler> onDoneOpt = Optional.empty(); // Parsed, but not used
        Optional<InvokeCompletionHandler> onErrorOpt = Optional.empty(); // Parsed, but not used

        if (ctx.invokeDefinitionBody() != null) {
            for (InvokeAttributeContext attrCtx : ctx.invokeDefinitionBody().invokeAttribute()) {
                if (attrCtx.invokeSrc() != null) {
                     System.err.println("Warning: 'src' defined in invokeState body for " + name + ". 'src' is usually part of invoke definition.");
                     srcOpt = Optional.ofNullable((ValueNode) visitInvokeSource(attrCtx.invokeSrc().invokeSource()));
                } else if (attrCtx.invokeInputMapping() != null && attrCtx.invokeInputMapping().keyValuePairList() != null) {
                    for (KeyValuePairContext pairCtx : attrCtx.invokeInputMapping().keyValuePairList().keyValuePair()) {
                        String key = stripQuotes(pairCtx.STRING().getText());
                        ValueNode value = (ValueNode) visitValue(pairCtx.value());
                        inputMapping.put(key, value);
                    }
                } else if (attrCtx.invokeOutputMapping() != null && attrCtx.invokeOutputMapping().keyValuePairList() != null) {
                     for (KeyValuePairContext pairCtx : attrCtx.invokeOutputMapping().keyValuePairList().keyValuePair()) {
                        String key = stripQuotes(pairCtx.STRING().getText());
                        ValueNode value = (ValueNode) visitValue(pairCtx.value());
                        outputMapping.put(key, value);
                    }
                } else if (attrCtx.invokeOnDone() != null) {
                    onDoneOpt = Optional.ofNullable(parseInvokeCompletionHandler(attrCtx.invokeOnDone().invokeCompletion()));
                } else if (attrCtx.invokeOnError() != null) {
                    onErrorOpt = Optional.ofNullable(parseInvokeCompletionHandler(attrCtx.invokeOnError().invokeCompletion()));
                } else if (attrCtx.annotation() != null) {
                    // These are annotations from within the LBRACE RBRACE block.
                    // They are already part of `allInvokeAnnotations` due to how `ctx.annotation()` works for the rule structure.
                    // So they are also in `nonIdAnnotations`.
                     System.out.println("Invoke body annotation (already captured): " + ((AnnotationNode)visitAnnotation(attrCtx.annotation())).name );
                }
            }
        }
        
        String srcAsString = "";
        if (srcOpt.isPresent()) {
            ValueNode srcValue = srcOpt.get();
            if (srcValue instanceof StringValueNode) {
                srcAsString = ((StringValueNode) srcValue).getRawValue();
            } else if (srcValue instanceof RefValueNode) {
                srcAsString = ((RefValueNode) srcValue).getQualifiedName();
            } else {
                System.err.println("Warning: Invoke source for '" + name + "' is not a direct string or reference. Using raw toString(). Value type: " + srcValue.getClass().getSimpleName());
                srcAsString = srcValue.toString(); // Fallback, might not be ideal
            }
        } else {
            System.err.println("Error: Invoke '" + name + "' is missing a 'src' attribute. Cannot create InvokeDefinitionNode.");
            // Return a placeholder or throw, as 'src' is mandatory for InvokeDefinitionNode constructor.
            // For now, let's use a placeholder to allow compilation, but this is an error state.
            srcAsString = "__MISSING_SRC__"; 
        }

        System.out.println("Creating InvokeDefinitionNode for: " + name + " with src: " + srcAsString + ". Mappings/handlers not stored in AST constructor yet.");
        // Constructor: InvokeDefinitionNode(Optional<Long> id, String invokeName, String source, List<AnnotationNode> annotations)
        return new InvokeDefinitionNode(id, name, srcAsString, nonIdAnnotations);
    }

    @Override
    public AstNode visitInvokeSource(InvokeSourceContext ctx) {
        // Grammar: invokeSource: STRING | expressionValue ;
        // expressionValue: value ;
        if (ctx.STRING() != null) {
            return new StringValueNode(stripQuotes(ctx.STRING().getText()));
        } else if (ctx.expressionValue() != null && ctx.expressionValue().value() != null) {
            return visitValue(ctx.expressionValue().value());
        }
        System.err.println("Unknown invoke source: " + ctx.getText());
        return new NullValueNode(); // Or throw an error
    }

    @Override
    public AstNode visitStatesDefinition(StatesDefinitionContext ctx) {
        System.out.println("Visiting States Definition block");
        List<AnnotationNode> blockAnnotations = ctx.annotation() != null ? extractAnnotations(ctx.annotation()) : Collections.emptyList();
        List<AstNode> statesAndHistories = new ArrayList<>();

        if (ctx.stateDefinitionOrHistoryState() != null && !ctx.stateDefinitionOrHistoryState().isEmpty()) {
            for (StateDefinitionOrHistoryStateContext itemCtx : ctx.stateDefinitionOrHistoryState()) {
                if (itemCtx.stateDefinition() != null) {
                    statesAndHistories.add(visitStateDefinition(itemCtx.stateDefinition()));
                } else if (itemCtx.historyDefinition() != null) { // Corrected from historyStateDefinition()
                    statesAndHistories.add(visitHistoryDefinition(itemCtx.historyDefinition()));
                } else {
                    System.err.println("Unknown item in stateDefinitionOrHistoryState: " + itemCtx.getText());
                }
            }
        }
        return new BlockContainerNode("states", statesAndHistories, blockAnnotations);
    }

    @Override
    public AstNode visitStateDefinition(StateDefinitionContext ctx) {
        currentStateName = ctx.stateName.getText(); 
        System.out.println("Visiting State Definition: " + currentStateName);

        List<AnnotationNode> directAnnotations = extractAnnotations(ctx.annotation());
        Optional<Long> id = extractIdFromList(directAnnotations);
        // Map<String, Object> annotationMap = mapAnnotations(directAnnotations); // Will be constructed by StateNode or used differently

        String displayNameValue = currentStateName; // Default display name
        Optional<String> nameAnnotationOpt = findAnnotationValue(directAnnotations, "name");
        if (nameAnnotationOpt.isPresent()) {
            displayNameValue = nameAnnotationOpt.get();
        } else {
            Optional<String> displayNameAnnotationOpt = findAnnotationValue(directAnnotations, "displayName");
            if (displayNameAnnotationOpt.isPresent()) {
                displayNameValue = displayNameAnnotationOpt.get();
            }
        }

        StateType stateType = StateType.NORMAL; 
        boolean isInitial = findAnnotationValue(directAnnotations, "initial").map(Boolean::parseBoolean).orElse(false);
        boolean isFinal = findAnnotationValue(directAnnotations, "final").map(Boolean::parseBoolean).orElse(false);

        if (isInitial) stateType = StateType.INITIAL;
        if (isFinal) stateType = StateType.FINAL; // 'final' takes precedence if both are true, or define rules
        
        List<ActionReferenceNode> onEntryActionNodes = new ArrayList<>();
        List<ActionReferenceNode> onExitActionNodes = new ArrayList<>();
        List<TransitionNode> onEventTransitions = new ArrayList<>();
        List<TransitionNode> afterEventTransitions = new ArrayList<>();
        List<TransitionNode> ifConditionTransitions = new ArrayList<>(); 
        List<InvokeStateNode> invokeInvocations = new ArrayList<>();
        List<AstNode> nestedStateElements = new ArrayList<>(); 
        Optional<String> nestedInitialStateName = Optional.empty();

        if (ctx.stateBodyElement() != null) {
            for (StateBodyElementContext bodyElementCtx : ctx.stateBodyElement()) {
                if (bodyElementCtx.onEntryExit() != null) {
                    OnEntryExitContext oeeCtx = bodyElementCtx.onEntryExit();
                    ActionReferenceNode actionRef = (ActionReferenceNode) visitActionReference(oeeCtx.actionReference());
                    if (oeeCtx.ON_ENTRY() != null) {
                        onEntryActionNodes.add(actionRef);
                    } else if (oeeCtx.ON_EXIT() != null) {
                        onExitActionNodes.add(actionRef);
                    }
                } else if (bodyElementCtx.invokeState() != null) {
                    invokeInvocations.add((InvokeStateNode) visitInvokeState(bodyElementCtx.invokeState()));
                } else if (bodyElementCtx.onTransition() != null) {
                    onEventTransitions.add((TransitionNode) visitOnTransition(bodyElementCtx.onTransition()));
                } else if (bodyElementCtx.afterTransition() != null) {
                    AstNode afterNode = visit(bodyElementCtx.afterTransition());
                     if (afterNode instanceof TransitionNode) {
                        afterEventTransitions.add((TransitionNode) afterNode);
                    } else {
                        System.err.println("Expected TransitionNode from afterTransition, got: " + (afterNode != null ? afterNode.getClass().getName() : "null"));
                    }
                } else if (bodyElementCtx.ifTransitionStatement() != null) {
                    AstNode ifNode = visitIfTransitionStatement(bodyElementCtx.ifTransitionStatement());
                    if (ifNode instanceof TransitionNode) {
                        ifConditionTransitions.add((TransitionNode) ifNode);
                    } else {
                         System.err.println("Expected TransitionNode from ifTransitionStatement, got: " + (ifNode != null ? ifNode.getClass().getName() : "null"));
                    }
                } else if (bodyElementCtx.statesDefinition() != null) {
                    BlockContainerNode nestedStatesContainer = (BlockContainerNode) visitStatesDefinition(bodyElementCtx.statesDefinition());
                    if (nestedStatesContainer != null && nestedStatesContainer.getChildren() != null) {
                        nestedStateElements.addAll(nestedStatesContainer.getChildren());
                        // Check for $initial on the nested states block itself
                        nestedInitialStateName = findAnnotationValue(nestedStatesContainer.getBlockAnnotations(), "initial"); 
                    }
                } else if (bodyElementCtx.historyDefinition() != null) {
                    HistoryStateNode historyNode = (HistoryStateNode) visitHistoryDefinition(bodyElementCtx.historyDefinition());
                    if (historyNode != null) {
                        nestedStateElements.add(historyNode); 
                    }
                } else if (bodyElementCtx.annotation() != null) {
                    // Body annotations are part of directAnnotations if they are on the state line,
                    // or part of specific elements like transitions. General free-floating annotations in state body
                    // might need a separate collection in StateNode or be added to `directAnnotations` if appropriate.
                    // For now, assuming `directAnnotations` captures the main ones for the state itself.
                    AnnotationNode bodyAnnotation = (AnnotationNode) visitAnnotation(bodyElementCtx.annotation());
                    directAnnotations.add(bodyAnnotation); // Add to the main list for the state
                     System.out.println("Found body annotation in state " + currentStateName + ": " + bodyAnnotation + ". Added to state's direct annotations.");
                }
            }
        }
        
        List<StateNode> childStateNodes = nestedStateElements.stream()
                                               .filter(n -> n instanceof StateNode)
                                               .map(n -> (StateNode)n)
                                               .collect(Collectors.toList());
        List<HistoryStateNode> historyStateNodes = nestedStateElements.stream()
                                                   .filter(n -> n instanceof HistoryStateNode)
                                                   .map(n -> (HistoryStateNode)n)
                                                   .collect(Collectors.toList());

        if (!childStateNodes.isEmpty() && stateType == StateType.NORMAL) { // Only upgrade normal to compound
            stateType = StateType.COMPOUND; 
        }
        if (stateType == StateType.COMPOUND && !nestedInitialStateName.isPresent() && !childStateNodes.isEmpty()) {
            // If compound and no $initial on states block, take first child state's name as initial
            nestedInitialStateName = Optional.of(childStateNodes.get(0).getStateName());
        }

        List<String> entryActionNames = onEntryActionNodes.stream().map(ActionReferenceNode::getActionName).collect(Collectors.toList());
        List<String> exitActionNames = onExitActionNodes.stream().map(ActionReferenceNode::getActionName).collect(Collectors.toList());

        List<TransitionNode> eventAndAfterTransitions = new ArrayList<>(onEventTransitions);
        eventAndAfterTransitions.addAll(afterEventTransitions);

        // Constructor: StateNode(Optional<Long> id, String stateName, List<AnnotationNode> annotations, 
        //                        String displayNameValue, StateType type, 
        //                        List<String> entryActions, List<String> exitActions, 
        //                        List<InvokeStateNode> invokeInvocations, 
        //                        List<TransitionNode> eventHandlers, List<TransitionNode> ifTransitions, 
        //                        List<StateNode> nestedStates, List<HistoryStateNode> historyStates, 
        //                        Optional<String> initialStateNameValue)
        StateNode resultNode = new StateNode(
                id,
                currentStateName,
                directAnnotations, 
                displayNameValue,
                stateType,
                entryActionNames, 
                exitActionNames,  
                invokeInvocations,
                eventAndAfterTransitions, // eventHandlers
                ifConditionTransitions, // ifTransitions
                childStateNodes,    
                historyStateNodes,  
                nestedInitialStateName // initialStateNameValue for compound states
        );
        currentStateName = null; 
        return resultNode;
    }
     public AstNode visitIfTransitionStatement(IfTransitionStatementContext ctx) {
        System.out.println("Visiting IfTransitionStatement");
        GuardReferenceNode condition = (GuardReferenceNode) visitGuardReference(ctx.condition);
        List<AnnotationNode> transitionLineAnnotations = extractAnnotations(ctx.annotation());
        Map<String, Object> annotationMap = mapAnnotations(transitionLineAnnotations); 
        Optional<Long> transitionId = extractIdFromList(transitionLineAnnotations);

        TransitionSpecNode specNode = (TransitionSpecNode) visitTransitionSpec(ctx.transitionSpec());
        String syntheticEventName = "@IF:" + condition.getGuardName();
        if (condition.isNegated()) {
            syntheticEventName += "(not)";
        }
        
        // New TransitionNode constructor:
        // Optional<Long> id, String event, TargetStateNode targetState, Optional<GuardReferenceNode> condition,
        // List<ActionReferenceNode> actions, List<GuardReferenceNode> guards, List<String> allowedActors,
        // Optional<DurationNode> delay, TransitionType type, Map<String, Object> annotationsMap
        return new TransitionNode(
            transitionId, 
            syntheticEventName,         // event
            specNode.getTargetState(),  // targetState
            Optional.of(condition),     // condition (the main one for IF)
            specNode.getActions(),      // actions from spec
            specNode.getGuards(),       // guards from spec
            specNode.getAllowedActors(),// allowedActors from spec
            Optional.<DurationNode>empty(), // delay (none for IF transitions), explicitly typed
            TransitionNode.TransitionType.CONDITIONAL, // type
            annotationMap               // annotations from the IF line
        );
    }

    @Override
    public AstNode visitInvokeState(InvokeStateContext ctx) {
        // Grammar: INVOKE ID annotation* LBRACE annotation* invokeStateBody? RBRACE SEMI;
        // invokeStateBody: invokeAttribute (SEMI? invokeAttribute)* ;
        // invokeAttribute is same as for invokeDefinition
        String invokeIdToRef = ctx.ID().getText(); // This is a reference to an InvokeDefinition by its ID/name
        System.out.println("Visiting Invoke State, referencing: " + invokeIdToRef);

        List<AnnotationNode> invokeLineAnnotations = new ArrayList<>();
        List<AnnotationNode> invokeBodyAnnotations = new ArrayList<>(); // Annotations inside the LBRACE RBRACE

        // Separate annotations on the INVOKE ID line from those inside the body
        if (ctx.annotation() != null && !ctx.annotation().isEmpty()) {
            for (AnnotationContext annoCtx : ctx.annotation()) {
                 // Heuristic: if annotation is before LBRACE token
                if (ctx.LBRACE() != null && annoCtx.getSourceInterval().a < ctx.LBRACE().getSymbol().getTokenIndex()) {
                    invokeLineAnnotations.add((AnnotationNode) visitAnnotation(annoCtx));
                } else {
                    // These are annotations just after LBRACE, before specific attributes, or general body annotations
                    // The grammar `LBRACE annotation* invokeStateBody? RBRACE` means `annotation*` are directly in body.
                    // And `invokeStateBody` can also have `annotation`.
                    // Let's collect all from `invokeStateBody` explicitly.
                    // The ones from `LBRACE annotation*` are trickier if not part of `invokeStateBody` rule itself.
                    // For now, assume `ctx.annotation()` gives all of them.
                    // This is problematic if we need to distinguish.
                    //
                    // Let's refine: First list of annotations are for the invoke *instance*.
                    // Annotations inside LBRACE are for *configuring* this instance (overrides, etc.)
                }
            }
        }
        // Correctly extract annotations for the invoke *instance* (those on the `INVOKE ID annotation*` part)
        // This requires knowing which `annotation*` in the rule `ctx.annotation()` refers to.
        // ANTLR usually provides `annotation(0)`, `annotation(1)` if there are multiple distinct lists.
        // If it's just one list, we have to use token indices.
        // Assume `extractAnnotations(ctx.annotation())` gets all annotations at the `invokeState` rule level.
        // This is likely fine, as they all describe this specific invocation.
        
        List<AnnotationNode> allInvokeInstanceAnnotations = extractAnnotations(ctx.annotation());
        Optional<Long> instanceId = extractIdFromList(allInvokeInstanceAnnotations); // If this specific invoke can have @id
        Map<String, Object> instanceAnnotationMap = mapAnnotations(allInvokeInstanceAnnotations);


        // These are attributes that can override or specify details for this particular invocation
        // Default values come from the InvokeDefinitionNode this `invokeIdToRef` points to.
        Optional<ValueNode> srcOverride = Optional.empty(); // Not usually overridden here, src is part of definition
        Map<String, ValueNode> inputMappingOverride = new HashMap<>();
        Map<String, ValueNode> outputMappingOverride = new HashMap<>();
        Optional<InvokeCompletionHandler> onDoneOverride = Optional.empty();
        Optional<InvokeCompletionHandler> onErrorOverride = Optional.empty();


        if (ctx.invokeStateBody() != null) {
            for (InvokeAttributeContext attrCtx : ctx.invokeStateBody().invokeAttribute()) {
                if (attrCtx.invokeSrc() != null) {
                     System.err.println("Warning: 'src' defined in invokeState body for " + invokeIdToRef + ". 'src' is usually part of invoke definition.");
                     srcOverride = Optional.ofNullable((ValueNode) visitInvokeSource(attrCtx.invokeSrc().invokeSource()));
                } else if (attrCtx.invokeInputMapping() != null && attrCtx.invokeInputMapping().keyValuePairList() != null) {
                    for (KeyValuePairContext pairCtx : attrCtx.invokeInputMapping().keyValuePairList().keyValuePair()) {
                        String key = stripQuotes(pairCtx.STRING().getText());
                        ValueNode value = (ValueNode) visitValue(pairCtx.value());
                        inputMappingOverride.put(key, value);
                    }
                } else if (attrCtx.invokeOutputMapping() != null && attrCtx.invokeOutputMapping().keyValuePairList() != null) {
                     for (KeyValuePairContext pairCtx : attrCtx.invokeOutputMapping().keyValuePairList().keyValuePair()) {
                        String key = stripQuotes(pairCtx.STRING().getText());
                        ValueNode value = (ValueNode) visitValue(pairCtx.value());
                        outputMappingOverride.put(key, value);
                    }
                } else if (attrCtx.invokeOnDone() != null) {
                    onDoneOverride = Optional.ofNullable(parseInvokeCompletionHandler(attrCtx.invokeOnDone().invokeCompletion()));
                } else if (attrCtx.invokeOnError() != null) {
                    onErrorOverride = Optional.ofNullable(parseInvokeCompletionHandler(attrCtx.invokeOnError().invokeCompletion()));
                } else if (attrCtx.annotation() != null) {
                    // Annotations inside the invokeState body, add to instanceAnnotationMap
                    AnnotationNode bodyAnn = (AnnotationNode) visitAnnotation(attrCtx.annotation());
                    instanceAnnotationMap.put("body_" + bodyAnn.name, Optional.ofNullable(bodyAnn.value).orElse(true));
                }
            }
        }

        // Constructor: InvokeStateNode(Optional<Long> id, String invokeDefinitionRefName,
        //                              Map<String, ValueNode> inputMappingOverrides,
        //                              Map<String, ValueNode> outputMappingOverrides, // Added
        //                              Optional<InvokeCompletionHandler> onDoneOverride,
        //                              Optional<InvokeCompletionHandler> onErrorOverride,
        //                              Map<String, Object> instanceAnnotations)
        return new InvokeStateNode(
                instanceId,
                invokeIdToRef,
                inputMappingOverride,
                outputMappingOverride, // Pass the new map
                onDoneOverride,
                onErrorOverride,
                instanceAnnotationMap
        );
    }

    public AstNode visitHistoryDefinition(HistoryDefinitionContext ctx) {
        // Grammar: HISTORY historyType=(SHALLOW | DEEP)? annotation* (TARGET targetRef=ID)? transitionSpec? SEMI
        System.out.println("Visiting History State Definition");
        String historyId = "history"; // Default name, or could be from an annotation like $name
        HistoryStateNode.HistoryType type = HistoryStateNode.HistoryType.SHALLOW; // Default
        if (ctx.historyType != null) {
            if (ctx.historyType.getText().equals("deep")) { // Safer check
                type = HistoryStateNode.HistoryType.DEEP;
            }
        }

        List<AnnotationNode> annotations = extractAnnotations(ctx.annotation());
        Optional<Long> id = extractIdFromList(annotations); // @id on the history line
        Map<String, Object> annotationMap = mapAnnotations(annotations);
        
        if (annotationMap.containsKey("name")) { // Allow $name annotation to set the ID/name
            historyId = String.valueOf(annotationMap.get("name"));
        }


        Optional<TransitionSpecNode> defaultTransition = Optional.empty();
        if (ctx.transitionSpec() != null) {
            defaultTransition = Optional.of((TransitionSpecNode) visitTransitionSpec(ctx.transitionSpec()));
        }
        
        Optional<String> targetStateRef = Optional.empty();
        if (ctx.targetRef != null) {
            targetStateRef = Optional.of(ctx.targetRef.getText());
            // This targetRef seems to conflict with transitionSpec's target.
            // The grammar `(TARGET targetRef=ID)? transitionSpec?` implies that if transitionSpec is present, it defines the target.
            // If `TARGET targetRef=ID` is present, it might be a simpler way to define a direct target
            // without actions/guards, OR it's an alternative to transitionSpec.
            // For now, if transitionSpec is present, it takes precedence.
            // If $target annotation is used, it should be picked up by annotationMap.
            if (defaultTransition.isPresent() && targetStateRef.isPresent()) {
                System.err.println("Warning: Both TARGET attribute and transitionSpec found for history state " + historyId + ". transitionSpec will take precedence.");
            }
        }


        // Constructor: HistoryStateNode(Optional<Long> id, String name, HistoryType type, Optional<TransitionSpecNode> defaultTransition, Map<String, Object> annotations)
        // The targetStateRef from `TARGET targetRef=ID` is not directly in this constructor.
        // If it's meant to be the *only* way to specify a simple target, and transitionSpec is for complex ones,
        // then we need to conditionally create TransitionSpecNode.
        // For now, let's prioritize transitionSpec if present.
        // If only targetRef is present, we might need to create a simple TransitionSpecNode.

        if (!defaultTransition.isPresent() && targetStateRef.isPresent()) {
            // Create a simple TransitionSpecNode if only TARGET targetRef is given
            StateTargetNode simpleTarget = new StateTargetNode(targetStateRef.get());
            defaultTransition = Optional.of(new TransitionSpecNode(simpleTarget, Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyMap()));
        }


        return new HistoryStateNode(id, historyId, type, defaultTransition, annotationMap);
    }

    @Override
    public AstNode visitTypeExpr(TypeExprContext ctx) {
        // System.out.println("Visiting TypeExpr: " + ctx.getText());
        if (ctx.primitiveTypeName() != null) {
            String typeName = ctx.primitiveTypeName().getText();
            // System.out.println("  Primitive type: " + typeName);
            // return new PrimitiveTypeNode(typeName); // PrimitiveTypeNode should take the string name
            // Need to map string to PrimitiveTypeNode.PrimitiveType enum
            try {
                PrimitiveTypeNode.PrimitiveType pType = PrimitiveTypeNode.PrimitiveType.fromString(typeName);
                return new PrimitiveTypeNode(pType);
            } catch (IllegalArgumentException e) {
                System.err.println("Unknown primitive type name: " + typeName + " in TypeExpr");
                return new PrimitiveTypeNode(PrimitiveTypeNode.PrimitiveType.STRING); // Default or throw
            }

        } else if (ctx.referenceValue() != null) {
            // System.out.println("  Reference type: " + ctx.referenceValue().getText());
            String qualifiedName = ctx.referenceValue().getText(); // Get full text like "MyType" or "my.module.MyType"
            return new RefTypeNode(qualifiedName);
        } else if (ctx.OPTIONAL() != null) {
            // System.out.println("  Optional type");
            TypeExprNode innerType = (TypeExprNode) visitTypeExpr(ctx.typeExpr(0)); // typeExpr(0) as there's one typeExpr inside optional
            return new OptionalTypeNode(innerType);
        } else if (ctx.LIST() != null) {
            // System.out.println("  List type");
            TypeExprNode valueType = (TypeExprNode) visitTypeExpr(ctx.typeExpr(0)); // typeExpr(0) for list value type
            return new ListTypeNode(valueType);
        } else if (ctx.MAP() != null) {
            // System.out.println("  Map type");
            TypeExprNode keyType = (TypeExprNode) visitTypeExpr(ctx.typeExpr(0));   // typeExpr(0) for map key type
            TypeExprNode valueType = (TypeExprNode) visitTypeExpr(ctx.typeExpr(1)); // typeExpr(1) for map value type
            return new MapTypeNode(keyType, valueType);
        }
        System.err.println("Unknown TypeExpr: " + ctx.getText());
        return new PrimitiveTypeNode(PrimitiveTypeNode.PrimitiveType.STRING); // Default or throw error
    }

    @Override
    public AstNode visitAnnotation(AnnotationContext ctx) {
        // Grammar: AT ID LPAREN INT RPAREN | DOLLAR annotationName LPAREN annotationValue? RPAREN | DOLLAR annotationName SEMI
        if (ctx.AT() != null) { // @id(integer)
            String name = ctx.ID().getText();
            Long value = Long.parseLong(ctx.INT().getText());
            return new AnnotationNode(name, value);
        } else if (ctx.DOLLAR() != null) {
            String name = ctx.annotationName().getText();
            Object value = true; // Default for flag-style like $final;
            if (ctx.LPAREN() != null) { // $name(...) or $name()
                if (ctx.annotationValue() != null) {
                    // visitAnnotationValue returns a ValueNode or a list of AttributePairNodes (which should be mapped to a Map)
                    AstNode rawValue = visitAnnotationValue(ctx.annotationValue());
                    if (rawValue instanceof ValueNode) {
                        value = ((ValueNode) rawValue).getActualValue(); // Get the actual Java value from ValueNode
                    } else if (rawValue instanceof ObjectValueNode) { // If visitAnnotationValue returns an ObjectValueNode for attributePairList
                         value = ((ObjectValueNode)rawValue).getFields(); // This would be Map<String, ValueNode>
                    } else {
                        // This case might occur if attributePairList is handled differently by visitAnnotationValue
                        // For now, if it's not a simple ValueNode, keep it as AstNode or string for debugging
                        System.err.println("Annotation " + name + " has complex value of type: " + rawValue.getClass().getName() + ". Storing as raw AST node.");
                        value = rawValue; // Or convert to a map if it represents key-value pairs
                    }
                } else {
                    value = true; // For $name() - represents presence, no specific value needed beyond true or an empty map
                                  // Or, could be an empty map if $name() is expected to be distinct from $name(someVal)
                                  // Let's use an empty map for $name() to distinguish from $name(value='foo')
                    value = Collections.emptyMap();
                }
            }
            // For $flag;, value remains true.
            return new AnnotationNode(name, value);
        }
        return null; // Should not happen
    }

    @Override
    public AstNode visitAnnotationValue(AnnotationValueContext ctx) {
        // Grammar: {_input.LA(2) == COLON}? attributePairList | value
        // The predicate means if the second token ahead is a COLON, it's an attributePairList.
        // Otherwise, it's a single value.
        // ANTLR handles this by making either attributePairList() or value() non-null.
        if (ctx.attributePairList() != null) {
            // System.out.println("Visiting AnnotationValue as attributePairList");
            Map<String, ValueNode> attributes = new HashMap<>();
            for (AttributePairContext pairCtx : ctx.attributePairList().attributePair()) {
                String key = pairCtx.ID().getText();
                ValueNode valueNode = (ValueNode) visit(pairCtx.getChild(2)); //getChild(0)=ID, getChild(1)=COLON, getChild(2)=value alternative
                attributes.put(key, valueNode);
            }
            // Return as an ObjectValueNode for consistency with other object structures in AST
            return new ObjectValueNode(attributes);
        } else if (ctx.value() != null) {
            // System.out.println("Visiting AnnotationValue as single value");
            return visitValue(ctx.value()); // Returns a ValueNode (e.g., StringValueNode, NumberValueNode)
        }
        System.err.println("Unknown annotation value structure: " + ctx.getText());
        return new NullValueNode(); // Or throw
    }

    // Make visitReferenceValue public if it needs to be called from other helpers directly
    @Override
    public AstNode visitReferenceValue(ReferenceValueContext ctx) {
        // Grammar: ID (DOT ID)*
        String qualifiedName = ctx.ID().stream().map(ParseTree::getText).collect(Collectors.joining("."));
        return new RefValueNode(qualifiedName); // Assuming RefValueNode exists for representing references as values
    }


    private List<String> extractActionReferences(ActionReferenceListContext ctx) {
        List<String> refs = new ArrayList<>();
        if (ctx.actionReference() != null) {
            for (ActionReferenceContext arCtx : ctx.actionReference()) {
                // Assuming actionReference rule is just 'referenceValue'
                refs.add(arCtx.referenceValue().getText());
            }
        }
        return refs;
    }

    private Map<String, Object> mapAnnotations(List<AnnotationNode> annotations) {
        Map<String, Object> map = new HashMap<>();
        if (annotations != null) {
            for (AnnotationNode ann : annotations) {
                map.put(ann.name, Optional.ofNullable(ann.value).orElse(true));
            }
        }
        return map;
    }

    private List<AnnotationNode> extractAnnotations(List<AnnotationContext> annotationCtxs) {
        List<AnnotationNode> annotations = new ArrayList<>();
        if (annotationCtxs != null) {
            for (AnnotationContext annotationCtx : annotationCtxs) {
                AnnotationNode annNode = (AnnotationNode) visitAnnotation(annotationCtx);
                if (annNode != null) { // visitAnnotation can return null if grammar is ambiguous or error
                    annotations.add(annNode);
                }
            }
        }
        return annotations;
    }

    // Inner class BlockContainerNode
    static class BlockContainerNode implements AstNode {
        final String blockType;
        final List<AstNode> children;
        final List<AnnotationNode> annotations; // Annotations ON THE BLOCK itself (e.g. $description for `types { ... }`)

        BlockContainerNode(String type, List<AstNode> children) {
            this(type, children, Collections.emptyList());
        }

        BlockContainerNode(String type, List<AstNode> children, List<AnnotationNode> annotations) {
            this.blockType = type;
            this.children = children != null ? children : Collections.emptyList();
            this.annotations = annotations != null ? annotations : Collections.emptyList();
        }

        public String getBlockType() { return blockType; } // Getter for blockType
        public List<AstNode> getChildren() { return children; }
        public List<AnnotationNode> getBlockAnnotations() { return annotations; }

        @Override public <T> T accept(NodeVisitor<T> visitor) { return null; } // Or visitor.visitBlockContainerNode(this)
        @Override public String toString() { return "BlockContainer[" + blockType + ", children=" + children.size() + ", annotations=" + annotations.size() + "]"; }
        
        @Override public Map<String, Object> getAnnotations() { // These are annotations ON the block, not its children
            Map<String, Object> map = new HashMap<>();
            for (AnnotationNode ann : this.annotations) {
                map.put(ann.name, Optional.ofNullable(ann.value).orElse(true));
            }
            return map;
        }
        @Override public Optional<Long> getId() { // Blocks themselves typically don't have @id
            for (AnnotationNode ann : this.annotations) {
                if ("id".equals(ann.name) && ann.value instanceof Number) {
                    return Optional.of(((Number) ann.value).longValue());
                }
            }
            return Optional.empty();
        }
    }
}
}
