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

    private InvokeCompletionHandlerNode parseInvokeCompletionHandler(InvokeCompletionContext ctx) {
        if (ctx == null) return null; // Or throw, or return an empty handler

        List<AnnotationNode> handlerAnnotations = ctx.annotation() != null ? extractAnnotations(ctx.annotation()) : Collections.emptyList();
        Optional<Long> handlerId = extractIdFromList(handlerAnnotations);

        List<ActionReferenceNode> handlerActionRefs = new ArrayList<>();
        Optional<TransitionSpecNode> handlerTransitionSpec = Optional.empty();

        if (ctx.actionReferenceList() != null) {
            // Assuming extractActionReferenceNodes returns List<ActionReferenceNode>
            handlerActionRefs.addAll(extractActionReferenceNodes(ctx.actionReferenceList()));
        } else if (ctx.transitionSpec() != null) {
            handlerTransitionSpec = Optional.ofNullable((TransitionSpecNode) visitTransitionSpec(ctx.transitionSpec()));
        }
        // InvokeCompletionHandlerNode(Optional<Long> id, List<ActionReferenceNode> actions, Optional<TransitionSpecNode> transitionSpec, List<AnnotationNode> annotations)
        return new InvokeCompletionHandlerNode(handlerId, handlerActionRefs, handlerTransitionSpec, handlerAnnotations);
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
            // Determine the start index of the first import or definition block if they exist
            int firstImportOrBlockIndex = Integer.MAX_VALUE;
            if (ctx.importStatement() != null && !ctx.importStatement().isEmpty()) {
                firstImportOrBlockIndex = Math.min(firstImportOrBlockIndex, ctx.importStatement(0).getStart().getTokenIndex());
            }
            if (ctx.definitionBlock() != null && !ctx.definitionBlock().isEmpty()) {
                firstImportOrBlockIndex = Math.min(firstImportOrBlockIndex, ctx.definitionBlock(0).getStart().getTokenIndex());
            }

            for (AnnotationContext annoCtx : ctx.annotation()) {
                // Only consider annotations that appear before any import or definition block
                if (annoCtx.getStart().getTokenIndex() < firstImportOrBlockIndex) {
                    topLevelAnnotationCtxs.add(annoCtx);
                } else {
                    // This logic might misclassify annotations if they are interleaved in a way not handled by this simple check
                    System.out.println("Found annotation after imports/blocks started or no imports/blocks present, not treating as top-level file annotation: " + annoCtx.getText());
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

        // Initialize local lists to store definitions
        List<ImportNode> imports = new ArrayList<>();
        List<TypeDefNode> typeDefinitions = new ArrayList<>();
        List<AstNode> serviceDefinitions = new ArrayList<>(); // Stays AstNode (holds Service/Interface)
        List<MachineNode> machineDefinitions = new ArrayList<>(); // Use specific type
        List<ActorNode> actorDefinitions = new ArrayList<>(); // Use specific type
        List<AstNode> communicationDefinitions = new ArrayList<>(); // Stays AstNode (holds Event/Channel/Protocol)
        // Add other lists as needed, e.g., for deploymentConfig, dependencies

        if (ctx.importStatement() != null) {
            for (ImportStatementContext importCtx : ctx.importStatement()) {
                imports.add((ImportNode) visitImportStatement(importCtx));
            }
        }

        if (ctx.definitionBlock() != null) {
            for (DefinitionBlockContext blockCtx : ctx.definitionBlock()) {
                AstNode visitedNode = visit(blockCtx);
                if (visitedNode instanceof BlockNode) {
                    BlockNode container = (BlockNode) visitedNode;
                    System.out.println("Processing BlockNode: " + container.blockType + " with " + container.getDefinitions().size() + " definitions.");

                    switch (container.blockType) {
                        case "types":
                            container.getDefinitions().forEach(child -> {
                                if (child instanceof TypeDefNode) {
                                    typeDefinitions.add((TypeDefNode) child); // Add to local list
                                } else {
                                    System.err.println("Warning: Child in types block is not TypeDefNode: " + child.getClass().getName());
                                }
                            });
                            break;
                        case "services":
                            container.getDefinitions().forEach(child -> {
                                // ServiceDefinitionNode and InterfaceNode are both valid children
                                if (child instanceof ServiceDefinitionNode || child instanceof InterfaceNode) {
                                    serviceDefinitions.add(child); // Add to local list
                                } else {
                                     System.err.println("Warning: Child in services block is not ServiceDefinitionNode or InterfaceNode: " + child.getClass().getName());
                                }
                            });
                            break;
                        case "machines":
                            container.getDefinitions().forEach(child -> {
                                if (child instanceof MachineNode) {
                                    machineDefinitions.add((MachineNode) child); // Add to local list
                                } else {
                                     System.err.println("Warning: Child in machines block is not MachineNode: " + child.getClass().getName());
                                }
                            });
                            break;
                        case "actors":
                             container.getDefinitions().forEach(child -> {
                                 if (child instanceof ActorNode) {
                                     actorDefinitions.add((ActorNode) child); // Add to local list
                                 } else {
                                     System.err.println("Warning: Child in actors block is not ActorNode: " + child.getClass().getName());
                                 }
                             });
                             break;
                        case "communication":
                            container.getDefinitions().forEach(child -> {
                                if (child instanceof ProtocolNode || child instanceof ChannelNode || child instanceof EventNode) {
                                    communicationDefinitions.add(child); // Add to local list
                                } else {
                                    System.err.println("Warning: Child in communication block is not a known comm element: " + child.getClass().getName());
                                }
                            });
                            break;
                        // TODO: Add cases for deploymentConfigBlock and dependenciesBlock
                        // case "deploymentConfig": ...
                        // case "dependencies": ...
                        default:
                            System.err.println("Warning: Unknown block type: " + container.blockType);
                    }
                } else {
                     System.err.println("Warning: Visited node from DefinitionBlockContext is not a BlockNode: " + (visitedNode != null ? visitedNode.getClass().getName() : "null"));
                }
            }
        }

        // Return the fully populated SsotRoot node
        return new SsotRoot(
            finalFileId,
            rootAnnotationsMap,
            imports,
            typeDefinitions,
            serviceDefinitions,
            machineDefinitions,
            actorDefinitions,
            communicationDefinitions
        );
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
        return new BlockNode(Optional.empty(), "types", typeDefs, mapAnnotations(blockAnnotations));
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
        return new BlockNode(Optional.empty(), "services", serviceElements, mapAnnotations(blockAnnotations));
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
        return new BlockNode(Optional.empty(), "machines", machineDefs, mapAnnotations(blockAnnotations));
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
                    BlockNode actionsContainer = (BlockNode) visitActionsDefinition(bodyElCtx.actionsDefinition());
                    actionsContainer.getDefinitions().forEach(child -> actions.add((ActionDefinitionNode) child));
                } else if (bodyElCtx.guardsDefinition() != null) {
                    BlockNode guardsContainer = (BlockNode) visitGuardsDefinition(bodyElCtx.guardsDefinition());
                    guardsContainer.getDefinitions().forEach(child -> guards.add((GuardDefinitionNode) child));
                } else if (bodyElCtx.invokesDefinition() != null) {
                    BlockNode invokesContainer = (BlockNode) visitInvokesDefinition(bodyElCtx.invokesDefinition());
                    invokesContainer.getDefinitions().forEach(child -> invokes.add((InvokeDefinitionNode) child));
                } else if (bodyElCtx.statesDefinition() != null) {
                    BlockNode statesContainer = (BlockNode) visitStatesDefinition(bodyElCtx.statesDefinition());
                    if (statesContainer != null) {
                        topLevelStates.addAll(statesContainer.getDefinitions()); 
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
        return new BlockNode(Optional.empty(), "actions", actionDefs, mapAnnotations(blockAnnotations));
    }

    @Override
    public AstNode visitActionDefinition(SSoTParser.ActionDefinitionContext ctx) {
        String name = ctx.ID().getText(); // Action name from the ID token
        List<AnnotationNode> allAnnotations = extractAnnotations(ctx.annotation()); 
        Optional<Long> id = extractIdFromList(allAnnotations);
        List<AnnotationNode> nonIdAnnotations = allAnnotations.stream()
                                                    .filter(a -> !(a.name.equals("id") && a.value instanceof Number))
                                                    .collect(Collectors.toList());

        List<ParameterNode> parameters = new ArrayList<>();
        if (ctx.parameterList() != null) {
            for (SSoTParser.ParameterContext paramCtx : ctx.parameterList().parameter()) {
                String paramName = paramCtx.ID().getText(); // Name from ParameterContext
                TypeExprNode paramType = (TypeExprNode) visitTypeExpr(paramCtx.typeExpr());
                // ParameterNode(Optional<Long> id, List<AnnotationNode> annotations, String name, TypeExprNode type)
                parameters.add(new ParameterNode(Optional.empty(), Collections.emptyList(), paramName, paramType));
            }
        }
        
        Optional<TypeExprNode> returnType = Optional.empty();
        if (ctx.typeExpr() != null) { // Return type for the action
            returnType = Optional.of((TypeExprNode) visitTypeExpr(ctx.typeExpr()));
        }
        
        System.out.println("Creating ActionDefinitionNode for: " + name + " with id: " + id + ", params: " + parameters.size() + ", returnType: " + returnType.isPresent());
        
        return new ActionDefinitionNode(id, name, parameters, returnType, nonIdAnnotations);
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
        return new BlockNode(Optional.empty(), "guards", guardDefs, mapAnnotations(blockAnnotations));
    }

    @Override
    public AstNode visitGuardDefinition(SSoTParser.GuardDefinitionContext ctx) {
        // System.out.println("Visiting GuardDefinition: " + ctx.getText());
        List<AnnotationNode> annotations = extractAnnotations(ctx.annotation());
        Optional<Long> id = extractIdFromList(annotations);
        // Map<String, Object> annotationMap = mapAnnotations(annotations); // GuardNode takes List<AnnotationNode>

        String guardName = "UnknownGuard"; // Default value
        if (ctx.ID() != null && !ctx.ID().isEmpty()) { // ID() is a list here
            guardName = ctx.ID(0).getText(); // First ID is the guard name
        } else {
            System.err.println("Error: Guard definition is missing a name. Context: " + ctx.getText());
        }

        // Parameter and returnType are not currently stored in GuardNode.
        // The logic below is for future reference if GuardNode is extended.
        /*
        Optional<String> parameter = Optional.empty();
        // Grammar: guardDefinition: ID annotation* (LPAREN ID? RPAREN)? ARROW T_BOOL SEMI;
        // The ID for the parameter is the second ID token if present and if LPAREN exists.
        if (ctx.LPAREN() != null && ctx.ID().size() > 1) {
            // This assumes the parameter ID is the next ID token after the guard name ID.
            // This might be fragile if annotations can contain ID tokens.
            // A more robust approach would be to label the parameter ID in the grammar
            // or inspect children tokens between LPAREN and RPAREN.
            // Example: If rule was `guardName=ID ... (LPAREN paramName=ID? RPAREN)? ...`
            // then ctx.paramName would be directly accessible.
            // For now, if LPAREN exists, check if there's a second ID token overall.
            // This is a simplification and might need refinement.
            // Check if the second ID is actually within the parentheses.
            // A simple way: check if the text of the token after LPAREN is not RPAREN.
            ParseTree tokenAfterLparen = null;
            ParseTree tokenBeforeRparen = null;
            boolean foundLparen = false;
            for (int i = 0; i < ctx.getChildCount(); i++) {
                ParseTree child = ctx.getChild(i);
                if (child instanceof TerminalNode) {
                    TerminalNode tn = (TerminalNode) child;
                    if (tn.getSymbol().getType() == SSoTParser.LPAREN) {
                        foundLparen = true;
                        if (i + 1 < ctx.getChildCount()) {
                            tokenAfterLparen = ctx.getChild(i + 1);
                        }
                        continue;
                    }
                    if (foundLparen && tn.getSymbol().getType() == SSoTParser.ID) {
                         // This ID is a candidate for parameter
                         if ( (i + 1 < ctx.getChildCount() && ctx.getChild(i+1) instanceof TerminalNode && ((TerminalNode)ctx.getChild(i+1)).getSymbol().getType() == SSoTParser.RPAREN) ) {
                            parameter = Optional.of(tn.getText());
                            break; // found parameter
                         }
                    }
                    if (tn.getSymbol().getType() == SSoTParser.RPAREN) {
                        break; // Past potential parameter
                    }
                }
            }
        }
        // Type for a guard is always boolean by grammar definition (ARROW T_BOOL SEMI).
        PrimitiveTypeNode returnType = new PrimitiveTypeNode(PrimitiveTypeNode.PrimitiveType.BOOL);
        */

        // Constructor: GuardNode(Optional<Long> id, String name, List<AnnotationNode> annotations)
        return new GuardNode(id, guardName, annotations);
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
        return new BlockNode(Optional.empty(), "invokes", invokeDefs, mapAnnotations(blockAnnotations));
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
        Optional<InvokeCompletionHandlerNode> onDoneOpt = Optional.empty(); // Parsed, but not used
        Optional<InvokeCompletionHandlerNode> onErrorOpt = Optional.empty(); // Parsed, but not used

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
        return new BlockNode(Optional.empty(), "states", statesAndHistories, mapAnnotations(blockAnnotations));
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
                    BlockNode nestedStatesContainer = (BlockNode) visitStatesDefinition(bodyElementCtx.statesDefinition());
                    if (nestedStatesContainer != null && nestedStatesContainer.getDefinitions() != null) {
                        nestedStateElements.addAll(nestedStatesContainer.getDefinitions());
                        // Check for $initial on the nested states block itself
                        Map<String, Object> blockAnnots = nestedStatesContainer.getAnnotations();
                        Object initialValue = blockAnnots.get("initial");
                        if (initialValue instanceof String) {
                            nestedInitialStateName = Optional.of((String) initialValue);
                        } else {
                            nestedInitialStateName = Optional.empty();
                        }
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
        
        // Corrected TransitionNode constructor call to match the 15-argument version
        return new TransitionNode(
            transitionId,                                                               // id
            this.currentStateName,                                                      // sourceStateName
            syntheticEventName,                                                         // event
            specNode.getTargetState().getStateName().orElseThrow(() -> new IllegalStateException("Target name missing for IF transition")), // targetStateName (String)
            Optional.of(condition.getGuardName()),                                      // conditionRef (Optional<String>)
            specNode.getActions().stream().map(ActionReferenceNode::getActionName).collect(Collectors.toList()), // actionRefs (List<String>)
            transitionLineAnnotations,                                                  // annotations (List<AnnotationNode> for the transition line)
            specNode.getTargetState(),                                                  // targetState (TargetStateNode)
            Optional.of(condition),                                                     // condition (Optional<GuardReferenceNode> - the main one for IF)
            specNode.getActions(),                                                      // actions (List<ActionReferenceNode> from spec)
            specNode.getGuards(),                                                       // guards (List<GuardReferenceNode> from spec)
            specNode.getAllowedActors(),                                                // allowedActors (List<String> from spec)
            Optional.<DurationNode>empty(),                                             // delay (Optional<DurationNode>)
            TransitionNode.TransitionType.CONDITIONAL,                                  // type
            annotationMap                                                               // annotationsMap (Map<String, Object> from IF line)
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
                    // For now, assume `extractAnnotations(ctx.annotation())` gets all annotations at the `invokeState` rule level.
                    // This is likely fine, as they all describe this specific invocation.
                    
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
        Optional<InvokeCompletionHandlerNode> onDoneOverride = Optional.empty();
        Optional<InvokeCompletionHandlerNode> onErrorOverride = Optional.empty();


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
        //                              Optional<InvokeCompletionHandler> onDoneOverride,
        //                              Optional<InvokeCompletionHandler> onErrorOverride,
        //                              List<AnnotationNode> instanceAnnotations)
        // Corrected to match the actual 6-argument constructor of InvokeStateNode
        return new InvokeStateNode(
                instanceId,
                invokeIdToRef,
                inputMappingOverride,
                onDoneOverride,
                onErrorOverride,
                allInvokeInstanceAnnotations // Pass the List<AnnotationNode>
        );
    }

    public AstNode visitHistoryDefinition(HistoryDefinitionContext ctx) {
        // Grammar: HISTORY historyType=(SHALLOW | DEEP)? annotation* (TARGET targetRef=ID)? transitionSpec? SEMI
        System.out.println("Visiting History State Definition");
        String historyId = "history"; // Default name, or could be from an annotation like $name
        HistoryStateType type = HistoryStateType.SHALLOW; // Default - Corrected
        if (ctx.historyType != null) {
            if (ctx.historyType.getText().equals("deep")) { // Safer check
                type = HistoryStateType.DEEP; // Corrected
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
            TargetStateNode simpleTarget = new TargetStateNode(targetStateRef.get()); // Corrected from StateTargetNode
            defaultTransition = Optional.<TransitionSpecNode>of(new TransitionSpecNode(simpleTarget, Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyMap()));
        }

        return new HistoryStateNode(id, historyId, type, defaultTransition, annotations);
    }

    // Added helper method to extract and visit annotations
    private List<AnnotationNode> extractAnnotations(List<SSoTParser.AnnotationContext> annotationCtxs) {
        if (annotationCtxs == null || annotationCtxs.isEmpty()) {
            return Collections.emptyList();
        }
        List<AnnotationNode> annotationNodes = new ArrayList<>();
        for (SSoTParser.AnnotationContext annotationCtx : annotationCtxs) {
            AnnotationNode annotationNode = (AnnotationNode) visitAnnotation(annotationCtx);
            if (annotationNode != null) {
                annotationNodes.add(annotationNode);
            }
        }
        return annotationNodes;
    }

    // Added helper method to map a list of AnnotationNode to Map<String, Object>
    private Map<String, Object> mapAnnotations(List<AnnotationNode> annotationNodes) {
        if (annotationNodes == null || annotationNodes.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, Object> map = new HashMap<>();
        for (AnnotationNode annotation : annotationNodes) {
            map.put(annotation.name, Optional.ofNullable(annotation.value).orElse(Boolean.TRUE));
        }
        return Collections.unmodifiableMap(map);
    }

}

