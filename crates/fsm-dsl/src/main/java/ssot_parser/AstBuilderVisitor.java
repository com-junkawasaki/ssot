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
import ssot_parser.ast.handlers.InvokeCompletionHandler;
import java.time.Duration;
import ssot_parser.ast.nodes.StateNode; // Corrected import
import ssot_parser.ast.nodes.HistoryStateNode; // Corrected import
import ssot_parser.ast.type.StateType; // Corrected import
// import ssot_parser.ast.nodes.EventHandlerNode; // If this is a specific type
// import ssot_parser.ast.nodes.ConditionalTransitionNode; // If this is a specific type

/**
 * Visits the ANTLR Parse Tree and builds the Abstract Syntax Tree (AST).
 */
public class AstBuilderVisitor extends SSoTBaseVisitor<AstNode> {

    private String currentStateName = null;

    @Override
    public AstNode visitFile(FileContext ctx) {
        System.out.println("Visiting File (was SsotDefinition)");
        SsotRoot root = new SsotRoot(
                new ArrayList<>(), // imports
                new ArrayList<>(), // typeDefinitions
                new ArrayList<>(), // serviceDefinitions
                new ArrayList<>(), // machineDefinitions
                new ArrayList<>(), // actorDefinitions
                new ArrayList<>(), // communicationDefinitions
                Optional.empty(),  // id
                new HashMap<>()    // annotations
        );

        if (ctx.annotation() != null && !ctx.annotation().isEmpty()) {
             ctx.annotation().forEach(annoCtx -> {
                 AnnotationNode annotation = (AnnotationNode) visitAnnotation(annoCtx);
                 root.getAnnotations().put(annotation.name, Optional.ofNullable(annotation.value).orElse(true));
                 System.out.println("  Found Root Annotation: " + annotation);
             });
        }

        if (ctx.importStatement() != null) {
            for (ImportStatementContext importCtx : ctx.importStatement()) {
                root.getImports().add((ImportNode) visitImportStatement(importCtx));
            }
        }

        if (ctx.definitionBlock() != null) {
            for (DefinitionBlockContext blockCtx : ctx.definitionBlock()) {
                AstNode blockNode = visit(blockCtx);
                if (blockNode instanceof BlockContainerNode) {
                    BlockContainerNode container = (BlockContainerNode) blockNode;
                    switch (container.blockType) {
                        case "types":
                            container.getChildren().forEach(child -> root.getTypeDefinitions().add((TypeDefNode) child));
                            break;
                        case "services":
                            container.getChildren().forEach(child -> {
                                if (child instanceof ServiceDefinitionNode) {
                                    root.getServiceDefinitions().add((ServiceDefinitionNode) child);
                                } else if (child instanceof InterfaceNode) {
                                     System.err.println("InterfaceNode encountered in services block, needs proper handling in SsotRoot.");
                                     // root.getInterfaceDefinitions().add((InterfaceNode) child);
                                }
                            });
                            break;
                        case "machines":
                            container.getChildren().forEach(child -> root.getMachineDefinitions().add((MachineNode) child));
                            break;
                        default:
                            System.err.println("Warning: Unhandled block container type: " + container.blockType);
                            break;
                    }
                } else if (blockNode instanceof TypeDefNode) {
                     root.getTypeDefinitions().add((TypeDefNode) blockNode);
                } else if (blockNode instanceof ServiceDefinitionNode) {
                     root.getServiceDefinitions().add((ServiceDefinitionNode) blockNode);
                } else if (blockNode instanceof MachineNode) {
                     root.getMachineDefinitions().add((MachineNode) blockNode);
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
         List<AstNode> typeDefs = new ArrayList<>();
         if (ctx.typeDefinition() != null) {
            for (TypeDefinitionContext typeCtx : ctx.typeDefinition()) {
                 TypeDefNode typeDef = (TypeDefNode) visit(typeCtx);
                 if (typeDef != null) {
                     typeDefs.add(typeDef);
                 }
            }
         }
        return new BlockContainerNode("types", typeDefs);
    }

    @Override
    public AstNode visitServicesBlock(ServicesBlockContext ctx) {
        System.out.println("Visiting Services Block");
        List<AstNode> serviceDefs = new ArrayList<>();
         if (ctx.serviceDefinition() != null) {
            for (ServiceDefinitionContext serviceCtx : ctx.serviceDefinition()) {
                AstNode serviceDef = visit(serviceCtx);
                if (serviceDef instanceof ServiceDefinitionNode) {
                     serviceDefs.add(serviceDef);
                } else {
                    System.err.println("Expected ServiceDefinitionNode, got: " + (serviceDef != null ? serviceDef.getClass().getName() : "null"));
                }
            }
         }
         if (ctx.interfaceDefinition() != null) {
             for (InterfaceDefinitionContext interfaceCtx : ctx.interfaceDefinition()) {
                 AstNode interfaceDef = visit(interfaceCtx);
                 if (interfaceDef instanceof InterfaceNode) {
                    serviceDefs.add(interfaceDef);
                 } else {
                     System.err.println("Expected InterfaceNode, got: " + (interfaceDef != null ? interfaceDef.getClass().getName() : "null"));
                 }
             }
         }
        return new BlockContainerNode("services", serviceDefs);
    }

    @Override
    public AstNode visitMachinesBlock(MachinesBlockContext ctx) {
        System.out.println("Visiting Machines Block");
        List<AstNode> machineDefs = new ArrayList<>();
         if (ctx.machineDefinition() != null) {
            for (MachineDefinitionContext machineCtx : ctx.machineDefinition()) {
                 MachineNode machineDef = (MachineNode) visit(machineCtx);
                 if (machineDef != null) {
                     machineDefs.add(machineDef);
                 }
            }
         }
        return new BlockContainerNode("machines", machineDefs);
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
        Optional<Long> id = extractId(ctx.annotation()); // Annotations directly on STRUCT ID line
        List<AnnotationNode> annotations = extractAnnotations(ctx.annotation()); // These are annotations after STRUCT ID
        List<FieldNode> fields = new ArrayList<>();
        
        // The grammar is: STRUCT ID annotation* LBRACE annotation* fieldDefinition* RBRACE
        // ctx.annotation() would typically give the first list (after ID).
        // If there are annotations inside LBRACE, these need to be accessed carefully, e.g. if the grammar labels them
        // or by inspecting all annotations and filtering. For now, assuming primary annotations are those after ID.
        // The field definitions are accessed directly from the context if they exist.
        if (ctx.fieldDefinition() != null) { // fieldDefinition is directly part of StructDefinitionContext
            for (FieldDefinitionContext fieldCtx : ctx.fieldDefinition()) {
                fields.add((FieldNode) visitFieldDefinition(fieldCtx));
            }
        }
        return new TypeDefNode(id, structName, TypeDefNode.TypeKind.STRUCT, fields, null, annotations);
    }

    @Override
    public AstNode visitEnumDefinition(EnumDefinitionContext ctx) {
        String enumName = ctx.ID().getText();
        Optional<Long> id = extractId(ctx.annotation()); // Annotations directly on ENUM ID line
        List<AnnotationNode> annotations = extractAnnotations(ctx.annotation()); // These are annotations after ENUM ID
        List<EnumVariantNode> variants = new ArrayList<>();

        // Grammar: ENUM ID annotation* LBRACE annotation* enumVariant* RBRACE
        // Similar to struct, enumVariants are accessed directly from the context.
        if (ctx.enumVariant() != null) { // enumVariant is directly part of EnumDefinitionContext
            for (EnumVariantContext variantCtx : ctx.enumVariant()) {
                variants.add((EnumVariantNode) visitEnumVariant(variantCtx));
            }
        }
        return new TypeDefNode(id, enumName, TypeDefNode.TypeKind.ENUM, null, variants, annotations);
    }

    @Override
    public AstNode visitFieldDefinition(FieldDefinitionContext ctx) {
        String fieldName = ctx.ID().getText();
        ssot_parser.ast.type.TypeExprNode type = (ssot_parser.ast.type.TypeExprNode) visitTypeExpr(ctx.typeExpr());
        Optional<Long> id = extractId(ctx.annotation());
        List<AnnotationNode> annotations = extractAnnotations(ctx.annotation());
        return new FieldNode(id, fieldName, type, annotations);
    }

     @Override
     public AstNode visitEnumVariant(EnumVariantContext ctx) {
         String variantName = ctx.ID().getText();
         Optional<Long> id = extractId(ctx.annotation());
         // Corrected: ensure annotation list is extracted only if annotations exist
         List<AnnotationNode> annotations = ctx.annotation() != null ? extractAnnotations(ctx.annotation()) : Collections.emptyList();
         return new EnumVariantNode(id, annotations, variantName);
     }

    @Override
    public AstNode visitMachineDefinition(MachineDefinitionContext ctx) {
        String machineName = ctx.ID().getText(); // Assuming machineDefinition rule starts with MACHINE ID
        List<AnnotationNode> allMachineAnnotations = new ArrayList<>();
        Optional<Long> id = Optional.empty();
        String initialStateName = null; // Must be found via $initial annotation or similar

        // Process annotations on the MACHINE ID line itself, if any in grammar
        // If machine rule is `MACHINE ID annotation* LBRACE ...`
        if (ctx.annotation() != null && !ctx.annotation().isEmpty()) {
            List<AnnotationNode> machineHeaderAnnos = extractAnnotations(ctx.annotation());
            allMachineAnnotations.addAll(machineHeaderAnnos);
            id = extractIdFromList(machineHeaderAnnos);
            // Look for $initial state annotation, e.g., $initial("SomeState")
            initialStateName = findAnnotationValue(machineHeaderAnnos, "$initial").orElse(null);
        }

        ContextNode contextNode = null;
        List<ActionDefinitionNode> actions = new ArrayList<>();
        List<GuardDefinitionNode> guards = new ArrayList<>();
        List<InvokeDefinitionNode> invokes = new ArrayList<>();
        List<AstNode> topLevelStates = new ArrayList<>(); // For StateNode and HistoryStateNode

        // Process machineBodyElement*
        if (ctx.machineBodyElement() != null) {
            for (MachineBodyElementContext bodyElCtx : ctx.machineBodyElement()) {
                if (bodyElCtx.contextDefinition() != null) {
                    contextNode = (ContextNode) visitContextDefinition(bodyElCtx.contextDefinition());
                } else if (bodyElCtx.actionsDefinition() != null) {
                    // visitActionsDefinition returns a BlockContainerNode
                    BlockContainerNode actionsContainer = (BlockContainerNode) visitActionsDefinition(bodyElCtx.actionsDefinition());
                    actionsContainer.getChildren().forEach(child -> actions.add((ActionDefinitionNode) child));
                } else if (bodyElCtx.guardsDefinition() != null) {
                    // visitGuardsDefinition returns a BlockContainerNode
                    BlockContainerNode guardsContainer = (BlockContainerNode) visitGuardsDefinition(bodyElCtx.guardsDefinition());
                    guardsContainer.getChildren().forEach(child -> guards.add((GuardDefinitionNode) child));
                } else if (bodyElCtx.invokesDefinition() != null) {
                    // visitInvokesDefinition returns a BlockContainerNode
                    BlockContainerNode invokesContainer = (BlockContainerNode) visitInvokesDefinition(bodyElCtx.invokesDefinition());
                    invokesContainer.getChildren().forEach(child -> invokes.add((InvokeDefinitionNode) child));
                } else if (bodyElCtx.statesDefinition() != null) {
                    // visitStatesDefinition is expected to return a BlockContainerNode containing StateNode/HistoryStateNode
                    BlockContainerNode statesContainer = (BlockContainerNode) visitStatesDefinition(bodyElCtx.statesDefinition());
                    if (statesContainer != null) {
                        topLevelStates.addAll(statesContainer.getChildren()); // children are AstNode (StateNode or HistoryStateNode)
                    }
                } else if (bodyElCtx.annotation() != null) {
                    // Collect annotations defined directly inside the machine body
                    allMachineAnnotations.add((AnnotationNode) visitAnnotation(bodyElCtx.annotation()));
                }
            }
        }

        if (initialStateName == null) {
            System.err.println("Warning: Machine '" + machineName + "' does not have an $initial state specified.");
            // Optionally, default to the first state if any, though explicit is better.
            if (!topLevelStates.isEmpty() && topLevelStates.get(0) instanceof StateNode) {
                initialStateName = ((StateNode)topLevelStates.get(0)).getStateName();
                 System.err.println("Warning: Defaulting initial state for machine '" + machineName + "' to first state: " + initialStateName);
            }
        }

        return new MachineNode(id, machineName, initialStateName, Optional.ofNullable(contextNode), actions, guards, invokes, topLevelStates, allMachineAnnotations);
    }

    @Override
    public AstNode visitContextDefinition(ContextDefinitionContext ctx) {
        List<AnnotationNode> contextAnnotations = new ArrayList<>(); // Annotations directly on the CONTEXT keyword line
        if (ctx.annotation() != null && !ctx.annotation().isEmpty()) {
             contextAnnotations = extractAnnotations(ctx.annotation());
        }

        List<ContextVariableNode> variables = new ArrayList<>();
        if (ctx.contextField() != null) { // Check if contextField is present
            for (ContextFieldContext varCtx : ctx.contextField()) {
                variables.add((ContextVariableNode) visitContextField(varCtx));
            }
        }
        // The ContextNode itself might not have an ID or annotations at its root in the AST model.
        // Annotations from 'context annotation* {' are stored in 'contextAnnotations'.
        // These might need to be added to the MachineNode or handled differently.
        // For now, creating ContextNode with only the variables as per its likely design.
        return new ContextNode(variables); // Passing List<ContextVariableNode>
    }

    @Override
    public AstNode visitContextField(ContextFieldContext ctx) { // Renamed from visitContextVariableDefinition
        String varName = ctx.ID().getText();
        ssot_parser.ast.type.TypeExprNode type = (ssot_parser.ast.type.TypeExprNode) visitTypeExpr(ctx.typeExpr()); // Changed TypeNode to TypeExprNode
        Optional<Long> id = extractId(ctx.annotation());
        List<AnnotationNode> annotations = ctx.annotation() != null ? extractAnnotations(ctx.annotation()) : Collections.emptyList();

        Optional<ssot_parser.ast.values.ValueNode> defaultValue = Optional.empty();
        // Grammar for contextField: ID COLON typeExpr annotation* (LBRACE annotation* RBRACE)? SEMI
        // It does not seem to have a direct value assignment for default value here.
        // Default values might be specified via an annotation if needed.

        return new ContextVariableNode(id, varName, type, annotations, defaultValue);
    }

    @Override
    public AstNode visitActionsDefinition(ActionsDefinitionContext ctx) { // Renamed from visitActionsBlock
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
        String actionName = ctx.ID().getText();
        Optional<Long> id = extractId(ctx.annotation());
        List<AnnotationNode> annotations = extractAnnotations(ctx.annotation());
        // Assuming ActionDefinitionNode constructor (id, name, annotations, params_map_if_any)
        return new ActionDefinitionNode(id, actionName, annotations, new HashMap<>());
    }

    @Override
    public AstNode visitGuardsDefinition(GuardsDefinitionContext ctx) { // Renamed from visitGuardsBlock
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
        String guardName = ctx.ID().getText();
        Optional<Long> id = extractId(ctx.annotation());
        List<AnnotationNode> annotations = extractAnnotations(ctx.annotation());
        ssot_parser.ast.type.TypeNode returnType = new ssot_parser.ast.type.BaseType("boolean"); // Default, can be overridden by annotation
        String expression = "true"; // Placeholder, should be parsed if grammar allows inline expressions
        if (ctx.inlineGuardExpression() != null) {
            expression = ctx.inlineGuardExpression().getText(); // Simple text for now
        }
         // Assuming GuardDefinitionNode constructor (id, name, annotations, returnType, expression, params_map_if_any)
        return new GuardDefinitionNode(id, guardName, annotations, returnType, expression, new HashMap<>());
    }

     @Override
    public AstNode visitInvokesDefinition(InvokesDefinitionContext ctx) { // Renamed from visitInvokesBlock
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
         String invokeIdName = ctx.ID().getText();
         Optional<Long> id = extractId(ctx.annotation());
         List<AnnotationNode> annotations = extractAnnotations(ctx.annotation());

         String invokeType = findAnnotationValue(annotations, "$type").orElse("unknownService");
         String src = null;
         ssot_parser.ast.values.ValueNode srcValueNode = null; // Store as ValueNode
         Map<String, ssot_parser.ast.values.ValueNode> inputMapping = new HashMap<>();
         Map<String, String> outputMapping = new HashMap<>(); // String to String for now
         InvokeCompletionHandler onDone = null;
         InvokeCompletionHandler onError = null;

         if (ctx.invokeDefinitionBody() != null) {
             for (InvokeAttributeContext attrCtx : ctx.invokeDefinitionBody().invokeAttribute()) {
                 if (attrCtx.invokeSrc() != null) {
                     srcValueNode = (ssot_parser.ast.values.ValueNode) visitInvokeSource(attrCtx.invokeSrc());
                     src = srcValueNode.getRawValue(); // Keep raw string for compatibility if needed
                 } else if (attrCtx.invokeInputMapping() != null && attrCtx.invokeInputMapping().keyValuePairList() != null) {
                     for (KeyValuePairContext pairCtx : attrCtx.invokeInputMapping().keyValuePairList().keyValuePair()) {
                         String key = stripQuotes(pairCtx.STRING().getText());
                         inputMapping.put(key, (ssot_parser.ast.values.ValueNode) visitValue(pairCtx.value()));
                     }
                 } else if (attrCtx.invokeOutputMapping() != null && attrCtx.invokeOutputMapping().keyValuePairList() != null) {
                     // Assuming output mapping value is an identifier for now
                     for (KeyValuePairContext pairCtx : attrCtx.invokeOutputMapping().keyValuePairList().keyValuePair()) {
                         String key = stripQuotes(pairCtx.STRING().getText());
                         // Output mapping might map to context variables or event fields.
                         // For now, assuming value is a simple string identifier.
                         outputMapping.put(key, pairCtx.value().getText());
                     }
                 } else if (attrCtx.invokeOnDone() != null) {
                     onDone = parseInvokeCompletionHandler(attrCtx.invokeOnDone().invokeCompletion());
                 } else if (attrCtx.invokeOnError() != null) {
                     onError = parseInvokeCompletionHandler(attrCtx.invokeOnError().invokeCompletion());
                 }
             }
         }
        // Assuming InvokeDefinitionNode constructor
        return new InvokeDefinitionNode(id, invokeIdName, mapAnnotations(annotations), invokeType, srcValueNode, inputMapping, outputMapping, onDone, onError);
     }

     @Override
     public AstNode visitInvokeSource(InvokeSourceContext ctx) {
         if (ctx.STRING() != null) {
             return new StringValueNode(stripQuotes(ctx.STRING().getText()));
         } else if (ctx.expressionValue() != null && ctx.expressionValue().value() != null) {
             // Delegate to the general value visitor
             return visitValue(ctx.expressionValue().value());
         }
         // Fallback or error
         System.err.println("Could not parse invokeSource: " + ctx.getText());
         return new StringValueNode("ERROR_PARSING_INVOKE_SOURCE");
     }

     @Override
     public AstNode visitStatesDefinition(StatesDefinitionContext ctx) {
         System.out.println("Visiting StatesDefinition (for machine's top-level states or nested states)");
         List<AstNode> stateNodes = new ArrayList<>();
         if (ctx.stateDefinitionOrHistoryState() != null) { // Changed from stateDefinition()
             for (StateDefinitionOrHistoryStateContext stateOrHistCtx : ctx.stateDefinitionOrHistoryState()) { // Changed context type
                 if (stateOrHistCtx.stateDefinition() != null) {
                     AstNode stateNode = visitStateDefinition(stateOrHistCtx.stateDefinition());
                     if (stateNode != null) {
                         stateNodes.add(stateNode);
                     }
                 } else if (stateOrHistCtx.historyStateDefinition() != null) {
                     AstNode historyNode = visitHistoryDefinition(stateOrHistCtx.historyStateDefinition());
                     if (historyNode != null) {
                         stateNodes.add(historyNode);
                     }
                 }
             }
         }
         List<AnnotationNode> blockAnnotations = ctx.annotation() != null ? extractAnnotations(ctx.annotation()) : Collections.emptyList();
         return new BlockContainerNode("states", stateNodes, blockAnnotations);
     }

    @Override
    public AstNode visitStateDefinition(StateDefinitionContext ctx) {
        currentStateName = ctx.ID().getText();
        System.out.println("Visiting State: " + currentStateName);

        List<AnnotationNode> stateAnnotations = new ArrayList<>();
        if (ctx.annotation() != null) {
            for (AnnotationContext annoCtx : ctx.annotation()) {
                stateAnnotations.add((AnnotationNode) visitAnnotation(annoCtx));
            }
        }
        Optional<Long> id = extractIdFromList(stateAnnotations);
        Map<String, Object> namedAnnotations = extractAnnotationsMap(stateAnnotations.stream().filter(a -> !a.isIdAnnotation()).collect(Collectors.toList()));


        boolean isInitial = false;
        boolean isFinal = false;
        String description = null;

        for (AnnotationNode a : stateAnnotations) {
            if ("initial".equals(a.name) && a.value instanceof Boolean && (Boolean)a.value) { // field access
                isInitial = true;
            }
            if ("final".equals(a.name) && a.value instanceof Boolean && (Boolean)a.value) { // field access
                isFinal = true;
            }
            if ("description".equals(a.name) && a.value instanceof String) { // field access
                description = (String) a.value; // field access
            }
        }
        // Additional check in map for safety, though direct iteration is better
        if (namedAnnotations.containsKey("initial") && Boolean.TRUE.equals(namedAnnotations.get("initial"))) isInitial = true;
        if (namedAnnotations.containsKey("final") && Boolean.TRUE.equals(namedAnnotations.get("final"))) isFinal = true;
        if (namedAnnotations.containsKey("description") && namedAnnotations.get("description") instanceof String) {
            description = (String) namedAnnotations.get("description");
        }


        StateType stateType = StateType.NORMAL; // Default
        if (isInitial) stateType = StateType.INITIAL;
        if (isFinal) stateType = StateType.FINAL;
        // TODO: Add parallel, compound, history state type detection from annotations if defined

        List<AstNode> onEntry = new ArrayList<>();
        List<AstNode> onExit = new ArrayList<>();
        List<TransitionNode> transitions = new ArrayList<>();
        List<InvokeStateNode> invokes = new ArrayList<>();
        List<EventHandlerNode> eventHandlers = new ArrayList<>(); // For event-driven transitions / internal events

        // Process stateBody elements
        if (ctx.stateBody() != null && ctx.stateBody().stateBodyElement() != null) {
            for (StateBodyElementContext elementCtx : ctx.stateBody().stateBodyElement()) {
                if (elementCtx.onEntryExit() != null) {
                    OnEntryExitContext onCtx = elementCtx.onEntryExit();
                    List<String> actionRefs = extractActionReferences(onCtx.actionReferenceList());
                    if (onCtx.ON_ENTRY() != null) {
                        onEntry.add(new ActionReferencesNode(actionRefs)); // Assuming ActionReferencesNode exists
                    } else if (onCtx.ON_EXIT() != null) {
                        onExit.add(new ActionReferencesNode(actionRefs));
                    }
                } else if (elementCtx.onTransition() != null) {
                    AstNode transitionNode = visitOnTransition(elementCtx.onTransition());
                    if (transitionNode instanceof TransitionNode) {
                        transitions.add((TransitionNode) transitionNode);
                    } else if (transitionNode instanceof ConditionalTransitionNode) {
                        // If visitOnTransition can return ConditionalTransitionNode directly
                        // transitions.add((ConditionalTransitionNode) transitionNode); // Or handle separately
                         System.err.println("ConditionalTransitionNode returned from onTransition, needs specific handling in StateNode's list or separate list.");

                    }
                } else if (elementCtx.invokeState() != null) {
                    invokes.add((InvokeStateNode) visitInvokeState(elementCtx.invokeState()));
                } else if (elementCtx.eventHandler() != null) { // Assuming eventHandler rule exists
                     eventHandlers.add((EventHandlerNode) visitEventHandler(elementCtx.eventHandler()));
                } else if (elementCtx.stateDefinition() != null || elementCtx.historyDefinition() != null) {
                    // Nested states - this indicates a compound state.
                    // The current StateNode would need a list of child StateNodes.
                    // This requires StateNode to support children, and stateType to be COMPOUND.
                    System.err.println("Nested state/history found - needs StateNode to support children and be COMPOUND.");
                    // Example: childStates.add(visit(elementCtx.stateDefinition() != null ? elementCtx.stateDefinition() : elementCtx.historyDefinition()));
                }
            }
        }
         // Temporary fix for annotations map to avoid null pointer if annotation.value is null
        Map<String, Object> finalNamedAnnotations = new HashMap<>();
        if (stateAnnotations != null) {
            for (AnnotationNode annotation : stateAnnotations) {
                if (!annotation.isIdAnnotation()) { // exclude @id
                    finalNamedAnnotations.put(annotation.name, Optional.ofNullable(annotation.value).orElse(Boolean.TRUE)); // field access
                }
            }
        }


        // Ensure StateNode constructor matches these parameters
        return new StateNode(
            id,
            currentStateName,
            finalNamedAnnotations, // Use the processed map
            stateType,
            onEntry,
            onExit,
            transitions,
            invokes,
            new ArrayList<>(), // childStates - placeholder for compound states
            eventHandlers      // eventHandlers - placeholder
        );
    }

    // New visitor method for ifTransitionStatement
    public AstNode visitIfTransitionStatement(IfTransitionStatementContext ctx) {
        List<AnnotationNode> annotations = ctx.annotation() != null ? extractAnnotations(ctx.annotation()) : Collections.emptyList();
        Optional<Long> id = extractIdFromList(annotations);

        // condition=guardReference
        String guardName = ctx.condition().getText(); // guardReference.getText() should give the full guard name e.g. myGuard or guards.myGuard

        TransitionTarget target = null;
        List<String> actions = Collections.emptyList();

        if (ctx.transitionSpec() != null) {
            TransitionSpecContext spec = ctx.transitionSpec();
            if (spec.transitionTarget() != null) {
                target = (TransitionTarget) visitTransitionTarget(spec.transitionTarget());
            }
            if (spec.actionReferenceList() != null) {
                actions = extractActionReferences(spec.actionReferenceList());
            }
            // Annotations on transitionSpec itself can be merged if necessary, or TransitionNode can hold them separately.
            // For now, using annotations from the ifTransitionStatement level.
        }

        String targetStateName = "";
        if (target instanceof StateTarget) {
            targetStateName = ((StateTarget) target).getStateName();
        } else if (target instanceof HistoryTarget) {
            targetStateName = ".history"; // Or however HistoryTarget resolves
        }

        // For if-transitions, the "event" is the condition itself, or we use a synthetic event name.
        // The guardName from the IF clause is the primary condition.
        return new TransitionNode(
                id,
                this.currentStateName, // sourceStateName
                "@IF:" + guardName,      // synthetic event name indicating a conditional transition
                targetStateName,        // targetStateName
                Optional.of(guardName), // conditionRef
                actions,
                annotations
        );
    }

    private Optional<Long> extractIdFromList(List<AnnotationNode> annotations) {
        if (annotations == null) {
            return Optional.empty();
        }
        for (AnnotationNode annotation : annotations) {
            if (annotation.isIdAnnotation()) { // Checks if it's the @id annotation
                try {
                    // Value of @id should be Long
                    return Optional.of(Long.parseLong(String.valueOf(annotation.value)));
                } catch (NumberFormatException e) {
                    System.err.println("Warning: @id annotation value is not a valid Long: " + annotation.value);
                    return Optional.empty();
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public AstNode visitOnTransition(OnTransitionContext ctx) {
        String event = ctx.ID().getText(); // The event name
        TransitionTarget target = null;
        List<String> actions = Collections.emptyList();
        String guardName = null;
        List<AnnotationNode> annotations = extractAnnotations(ctx.annotation());

        if (ctx.transitionSpec() != null) {
            TransitionSpecContext spec = ctx.transitionSpec();
            if (spec.transitionTarget() != null) {
                target = (TransitionTarget) visitTransitionTarget(spec.transitionTarget());
            }
            if (spec.actionReferenceList() != null) {
                actions = extractActionReferences(spec.actionReferenceList());
            }
            if (spec.guardReference() != null) {
                guardName = spec.guardReference().ID().getText();
            }
            // Annotations on transitionSpec itself, if any (grammar might need adjustment)
            // annotations.addAll(extractAnnotations(spec.annotation()));
        }

        return new TransitionNode(
                Optional.empty(), // id
                annotations,
                event,
                guardName,
                target,
                actions,
                TransitionNode.TransitionType.EVENT,
                null // delay
        );
    }

    // visitConditionalTransition now delegates to visitConditionalTransitionSingle
    @Override
    public AstNode visitConditionalTransition(ConditionalTransitionContext ctx) {
        // This method seems to parse a block of if/else-if/else transitions.
        // Based on SSoT.g4, this structure is not directly part of stateBodyElement.
        // Individual transitions (on, after) can have guards via transitionSpec.
        // If this method *is* used, it implies a grammar rule like:
        // conditionalTransition: IF ifTransition (ELSEIF elseIfTransition)* (ELSE elseTransition)?;
        // For now, this will likely not be hit if conditionalTransition is not in stateDefinition.
        System.err.println("AstBuilderVisitor.visitConditionalTransition called - check grammar for 'conditionalTransition' rule under stateDefinition/stateBodyElement.");

        List<TransitionNode> transitions = new ArrayList<>();
        // Logic for IF
        if (ctx.ifTransition() != null) {
            AstNode ifNode = visitIfTransition(ctx.ifTransition());
            if (ifNode instanceof TransitionNode) { // Assuming visitIfTransition returns a TransitionNode
                transitions.add((TransitionNode)ifNode);
            }
        }
        // Logic for ELSE IF
        if (ctx.elseIfTransition() != null) {
            for (ElseIfTransitionContext elseIfCtx : ctx.elseIfTransition()) {
                AstNode elseIfNode = visitElseIfTransition(elseIfCtx);
                 if (elseIfNode instanceof TransitionNode) { // Assuming visitElseIfTransition returns a TransitionNode
                    transitions.add((TransitionNode)elseIfNode);
                }
            }
        }
        // Logic for ELSE
        if (ctx.elseTransition() != null) {
            AstNode elseNode = visitElseTransition(ctx.elseTransition());
            if (elseNode instanceof TransitionNode) { // Assuming visitElseTransition returns a TransitionNode
                transitions.add((TransitionNode)elseNode);
            }
        }
        // This method is declared to return AstNode. If it's a list of transitions,
        // it should probably return a BlockContainerNode or similar if called from visitStateDefinition.
        // However, StateNode.ifTransitions expects List<TransitionNode>.
        // The original visitor added children of a BlockContainerNode to ifTransitions list.
        return new BlockContainerNode("conditionalTransitions", transitions);
    }

    public AstNode visitIfTransition(IfTransitionContext ctx) { // Made public for direct call if needed
        // Grammar: IF LPAREN condition RPAREN LBRACE transitionBody RBRACE
        // This is a specific if construct, not the general guard on a transition.
        // SSoT.g4 transitionSpec already handles guards. This rule is likely from a different grammar.
        System.err.println("AstBuilderVisitor.visitIfTransition called - this rule might not be in the current SSoT.g4 state body.");
        String guardName = ctx.condition().getText(); // Placeholder for condition parsing
        List<String> actions = new ArrayList<>();
        String targetStateName = null;
        List<AnnotationNode> annotations = Collections.emptyList();

        if (ctx.transitionBody().transitionActionList() != null) {
             actions = extractActionReferences(ctx.transitionBody().transitionActionList().actionReferenceList());
        }
        if (ctx.transitionBody().transitionTarget() != null) {
            targetStateName = ctx.transitionBody().transitionTarget().getText();
        } else {
            // Implicit self-transition? Or error.
            System.err.println("Warning: IF transition without explicit target.");
            targetStateName = this.currentStateName; // Default to self-transition if no target
        }

        // id for such transitions? Usually not specified for sub-parts of a conditional block.
        return new TransitionNode(Optional.empty(), this.currentStateName, "IF_" + guardName, targetStateName, Optional.of(guardName), actions, annotations);
    }

    public AstNode visitElseIfTransition(ElseIfTransitionContext ctx) { // Made public
        System.err.println("AstBuilderVisitor.visitElseIfTransition called - this rule might not be in the current SSoT.g4 state body.");
        String guardName = ctx.condition().getText();
        List<String> actions = new ArrayList<>();
        String targetStateName = null;
        List<AnnotationNode> annotations = Collections.emptyList();

        if (ctx.transitionBody().transitionActionList() != null) {
             actions = extractActionReferences(ctx.transitionBody().transitionActionList().actionReferenceList());
        }
        if (ctx.transitionBody().transitionTarget() != null) {
            targetStateName = ctx.transitionBody().transitionTarget().getText();
        } else {
            targetStateName = this.currentStateName;
        }
        return new TransitionNode(Optional.empty(), this.currentStateName, "ELSEIF_" + guardName, targetStateName, Optional.of(guardName), actions, annotations);
    }

    public AstNode visitElseTransition(ElseTransitionContext ctx) { // Made public
        System.err.println("AstBuilderVisitor.visitElseTransition called - this rule might not be in the current SSoT.g4 state body.");
        List<String> actions = new ArrayList<>();
        String targetStateName = null;
         List<AnnotationNode> annotations = Collections.emptyList();

        if (ctx.transitionBody().transitionActionList() != null) {
             actions = extractActionReferences(ctx.transitionBody().transitionActionList().actionReferenceList());
        }
        if (ctx.transitionBody().transitionTarget() != null) {
            targetStateName = ctx.transitionBody().transitionTarget().getText();
        } else {
            targetStateName = this.currentStateName;
        }
        return new TransitionNode(Optional.empty(), this.currentStateName, "ELSE", targetStateName, Optional.empty(), actions, annotations);
    }

    @Override
    public AstNode visitInvokeState(InvokeStateContext ctx) {
        Optional<Long> id = extractId(ctx.annotation());
        List<AnnotationNode> annotations = extractAnnotations(ctx.annotation());

        ssot_parser.ast.values.ValueNode srcValueNode = (ssot_parser.ast.values.ValueNode) visitInvokeSource(ctx.invokeSource());
        String invokeId = findAnnotationValue(annotations, "$id").orElse(null); // Optional invoke ID from annotation

        Map<String, ssot_parser.ast.values.ValueNode> inputMapping = new HashMap<>();
        if (ctx.invokeInputMapping() != null && ctx.invokeInputMapping().keyValuePairList() != null) {
            for (KeyValuePairContext pairCtx : ctx.invokeInputMapping().keyValuePairList().keyValuePair()) {
                String key = stripQuotes(pairCtx.STRING().getText());
                inputMapping.put(key, (ssot_parser.ast.values.ValueNode) visitValue(pairCtx.value()));
            }
        }
        Map<String, String> outputMapping = new HashMap<>(); // String to String for now
         if (ctx.invokeOutputMapping() != null && ctx.invokeOutputMapping().keyValuePairList() != null) {
            for (KeyValuePairContext pairCtx : ctx.invokeOutputMapping().keyValuePairList().keyValuePair()) {
                String key = stripQuotes(pairCtx.STRING().getText());
                outputMapping.put(key, pairCtx.value().getText()); // Assuming output maps to a string identifier
            }
        }


        InvokeCompletionHandler onDone = null;
        if (ctx.invokeOnDone() != null) {
            onDone = parseInvokeCompletionHandler(ctx.invokeOnDone().invokeCompletion());
        }

        InvokeCompletionHandler onError = null;
        if (ctx.invokeOnError() != null) {
            onError = parseInvokeCompletionHandler(ctx.invokeOnError().invokeCompletion());
        }

       // List<InvokeStateNode.InvokeTransition> autoForwardTransitions = new ArrayList<>(); // TODO if grammar supports $autoForward
       // boolean autoForward = annotations.stream().anyMatch(a -> "$autoForward".equals(a.getName()) && "true".equalsIgnoreCase(a.getValue().orElse("false")));


        // Assuming InvokeStateNode constructor
        String invokeDefinitionRef = (srcValueNode != null) ? srcValueNode.getRawValue() : null; // Safely get ref
        if (invokeDefinitionRef == null) {
            // Handle error: invoke source could not be resolved to a string reference
            // This might involve logging an error and returning a placeholder or throwing an exception
            // For now, let's print an error and potentially return null or a special error node.
            System.err.println("Error: Invoke source could not be resolved to a definition reference in state: " + currentStateName);
            // Depending on error strategy, you might return null or an error node:
            // return new ErrorNode("Missing invoke definition reference");
        }
        return new InvokeStateNode(id, invokeDefinitionRef, inputMapping, Optional.ofNullable(onDone), Optional.ofNullable(onError), annotations);
    }

    private InvokeCompletionHandler parseInvokeCompletionHandler(InvokeCompletionContext ctx) {
        List<AnnotationNode> handlerAnnotations = ctx.annotation() != null ? extractAnnotations(ctx.annotation()) : Collections.emptyList();
        List<String> handlerActionRefs = Collections.emptyList();
        Optional<TransitionNode> handlerTransition = Optional.empty();

        if (ctx.actionReferenceList() != null) {
            handlerActionRefs = extractActionReferences(ctx.actionReferenceList());
        } else if (ctx.transitionSpec() != null) {
            TransitionSpecContext specCtx = ctx.transitionSpec();
            // Ensure visitTransitionTarget returns a TransitionTarget or handle null if not found/applicable
            TransitionTarget target = specCtx.transitionTarget() != null ? (TransitionTarget) visitTransitionTarget(specCtx.transitionTarget()) : null;
            List<String> transitionActions = specCtx.actionReferenceList() != null ? extractActionReferences(specCtx.actionReferenceList()) : Collections.emptyList();
            List<AnnotationNode> transitionAnnotations = specCtx.annotation() != null ? extractAnnotations(specCtx.annotation()) : Collections.emptyList();

            TransitionNode specTransitionNode = new TransitionNode(
                    Optional.empty(),       // id for this specific transition
                    transitionAnnotations,  // annotations on the transition itself
                    null,                   // event (not applicable for invoke completion target)
                    null,                   // guard (not applicable for invoke completion target)
                    target,                 // the target state or self-transition
                    transitionActions,      // actions to execute ON THIS TRANSITION
                    TransitionNode.TransitionType.INTERNAL, // Default for invoke completion
                    null                    // delay (not typically used here)
            );
            handlerTransition = Optional.of(specTransitionNode);
        }
        // Handles cases where ctx might be null or empty (though grammar implies it won't be if called)
        // and also handles if it has neither actions nor transition (empty handler, but with its annotations).
        return new InvokeCompletionHandler(handlerActionRefs, handlerTransition, handlerAnnotations);
    }

    @Override
    public AstNode visitHistoryDefinition(HistoryDefinitionContext ctx) {
        System.out.println("Visiting History Definition (was HistoryStateDefinition)");
        String historyIdName = ctx.ID().getText(); // ID of the history state
        List<AnnotationNode> annotations = ctx.annotation() != null ? extractAnnotations(ctx.annotation()) : Collections.emptyList();
        Optional<Long> id = extractIdFromList(annotations);
        String targetStateName = ctx.ID().getText();
        HistoryStateNode.HistoryType type = HistoryStateNode.HistoryType.SHALLOW; // Default
        if (ctx.DEEP() != null) {
            type = HistoryStateNode.HistoryType.DEEP;
        }
        return new HistoryStateNode(id, type, targetStateName, annotations);
    }

    @Override
    public AstNode visitOnEntryExit(OnEntryExitContext ctx) {
        // This rule is: (ON_ENTRY | ON_EXIT) actionReference SEMI;
        // This method should ideally not be called if logic is handled inline in visitStateDefinition.
        // If it is called, it means it's defined as a separate visitable item by ANTLR in some contexts.
        // However, for StateNode construction, we just need the action name and type (entry/exit).
        System.err.println("Warning: AstBuilderVisitor.visitOnEntryExit was called. This logic is expected to be inline within visitStateDefinition's loop over stateBodyElement.");
        // Returning a simple wrapper if it must return an AstNode, though it won't be directly used if handled inline.
        // Could return a specific temp node if needed for some other processing.
        String actionName = ctx.actionReference().getText();
        boolean isEntry = ctx.ON_ENTRY() != null;
        // This isn't a standard AST node typically, just data for the parent.
        // For safety, if it must return an AstNode:
        // return new ActionReferenceNode(actionName, isEntry ? ActionType.ENTRY : ActionType.EXIT);
        return null; // Or throw an error: should be handled inline
    }

    @Override
    public AstNode visitTypeExpr(TypeExprContext ctx) {
        System.out.println("Visiting TypeExpr: " + ctx.getText());

        if (ctx.primitiveTypeName() != null) {
            // Handle primitive types like string, bool, u32, etc.
            // Assuming PrimitiveTypeNode or a similar class exists. Using BaseType for now as per original.
            return new BaseType(ctx.primitiveTypeName().getText(), Optional.empty(), new HashMap<>());
        } else if (ctx.referenceValue() != null) {
            // Handle references to custom types (structs, enums)
            // Assuming ReferenceTypeNode or a similar class exists. Using CustomType for now as per original.
            return new CustomType(ctx.referenceValue().getText(), Optional.empty(), Optional.empty(), new HashMap<>());
        } else if (ctx.OPTIONAL() != null) {
            // Handle Optional<T>
            if (ctx.typeExpr() != null && !ctx.typeExpr().isEmpty()) {
                ssot_parser.ast.type.TypeNode innerType = (ssot_parser.ast.type.TypeNode) visitTypeExpr(ctx.typeExpr(0));
                return new OptionalTypeNode(innerType, Optional.empty(), new HashMap<>());
            } else {
                System.err.println("Malformed Optional type: missing inner type in " + ctx.getText());
                return new CustomType("ERROR_MALFORMED_OPTIONAL", Optional.empty(), Optional.empty(), new HashMap<>());
            }
        } else if (ctx.LIST() != null) {
            // Handle List<T>
            if (ctx.typeExpr() != null && !ctx.typeExpr().isEmpty()) {
                ssot_parser.ast.type.TypeNode elementType = (ssot_parser.ast.type.TypeNode) visitTypeExpr(ctx.typeExpr(0));
                return new ListTypeNode(elementType, Optional.empty(), new HashMap<>());
            } else {
                System.err.println("Malformed List type: missing element type in " + ctx.getText());
                return new CustomType("ERROR_MALFORMED_LIST", Optional.empty(), Optional.empty(), new HashMap<>());
            }
        } else if (ctx.MAP() != null) {
            // Handle Map<K, V>
            if (ctx.typeExpr() != null && ctx.typeExpr().size() == 2) {
                ssot_parser.ast.type.TypeNode keyType = (ssot_parser.ast.type.TypeNode) visitTypeExpr(ctx.typeExpr(0));
                ssot_parser.ast.type.TypeNode valueType = (ssot_parser.ast.type.TypeNode) visitTypeExpr(ctx.typeExpr(1));
                return new MapTypeNode(keyType, valueType, Optional.empty(), new HashMap<>());
            } else {
                System.err.println("Malformed Map type: requires two type parameters in " + ctx.getText());
                return new CustomType("ERROR_MALFORMED_MAP", Optional.empty(), Optional.empty(), new HashMap<>());
            }
        }

        System.err.println("Unhandled TypeExpr variant: " + ctx.getText());
        // Return a placeholder or throw an error for unhandled cases
        return new CustomType("ERROR_UNKNOWN_TYPE_EXPR", Optional.empty(), Optional.empty(), new HashMap<>());
    }

    @Override
    public AstNode visitAnnotation(AnnotationContext ctx) {
        String name;
        Object value = null; // Default to null, indicates a flag or valueless annotation if not set
        boolean isIdAnnotation = false;

        if (ctx.AT() != null) { // @id(value)
            name = ctx.ID().getText(); // Should be "id"
            isIdAnnotation = true;
            try {
                value = Long.parseLong(ctx.INT().getText());
            } catch (NumberFormatException e) {
                System.err.println("Warning: @id annotation has non-integer value: " + ctx.INT().getText());
                value = ctx.INT().getText(); // Store as string if not parsable as long
            }
        } else if (ctx.DOLLAR() != null) { // $name(value) or $name;
            name = ctx.annotationName().getText();
            if (ctx.annotationValue() != null) { // $name(value)
                // visitAnnotationValue will return the actual Java object for the value
                AstNode valNode = visitAnnotationValue(ctx.annotationValue());
                if (valNode instanceof ValueNode) {
                    value = ((ValueNode) valNode).getValue();
                } else if (valNode != null) {
                     System.err.println("Warning: Annotation value was not a ValueNode: " + valNode.getClass().getSimpleName() + " for annotation $" + name);
                     value = valNode.toString(); // Fallback to string representation
                } else {
                    value = true; // If annotationValue is present but visit returns null, treat as flag.
                }
            } else { // $name; (a flag)
                value = Boolean.TRUE;
            }
        } else {
            throw new IllegalStateException("Unknown annotation format: " + ctx.getText());
        }
        return new AnnotationNode(name, value, isIdAnnotation);
    }

    @Override
    public AstNode visitValue(ValueContext ctx) {
        if (ctx.primitiveValue() != null) {
            return visitPrimitiveValue(ctx.primitiveValue());
        } else if (ctx.referenceValue() != null) {
            // This part needs to create a RefValueNode if that's the intent
            // For now, let's assume visitReferenceValue handles it.
            return visitReferenceValue(ctx.referenceValue());
        } else if (ctx.objectValue() != null) {
            return visitObjectValue(ctx.objectValue());
        } else if (ctx.arrayValue() != null) {
            return visitArrayValue(ctx.arrayValue());
        }
        System.err.println("Unhandled value type: " + ctx.getText());
        return new NullValueNode(); // Default or throw error
    }

    @Override
    public AstNode visitAnnotationValue(AnnotationValueContext ctx) {
        if (ctx.attributePairList() != null) {
            // This means the annotation value is an object: $name(key1: val1, key2: val2)
            Map<String, ValueNode> attributes = new HashMap<>();
            for (AttributePairContext pairCtx : ctx.attributePairList().attributePair()) {
                String key = pairCtx.ID().getText();
                // Value can be any of the ValueNode types
                ValueNode valueNode = (ValueNode) visit(pairCtx.getChild(2)); //getChild(0)=ID, getChild(1)=COLON
                attributes.put(key, valueNode);
            }
            // Assuming ObjectValueNode stores Map<String, ValueNode>
            return new ObjectValueNode(attributes, Optional.empty(), Collections.emptyMap());
        } else if (ctx.value() != null) {
            // This means the annotation value is a single primitive, reference, array
            return visitValue(ctx.value());
        }
        return new NullValueNode(); // Should not happen if grammar is correct
    }

    private String stripQuotes(String text) {
        if (text != null && text.length() >= 2 &&
            ((text.startsWith("\"") && text.endsWith("\"")) || (text.startsWith("'") && text.endsWith("'")))) {
            return text.substring(1, text.length() - 1);
        }
        return text;
    }

    private List<String> extractActionReferences(ActionReferenceListContext ctx) {
        List<String> refs = new ArrayList<>();
        if (ctx != null && ctx.ID() != null) {
            ctx.ID().forEach(idNode -> refs.add(idNode.getText()));
        }
        return refs;
    }

    private Map<String, Object> mapAnnotations(List<AnnotationNode> annotations) {
        Map<String, Object> map = new HashMap<>();
        if (annotations != null) {
            for (AnnotationNode anno : annotations) {
                // Store value if present, otherwise use Boolean.TRUE for presence
                map.put(anno.getName(), anno.getValue().orElse(Boolean.TRUE));
            }
        }
        return map;
    }

    private List<AnnotationNode> allAnnotations(List<AnnotationNode> list1, List<AnnotationNode> list2) {
        List<AnnotationNode> combined = new ArrayList<>();
        if (list1 != null) combined.addAll(list1);
        if (list2 != null) combined.addAll(list2);
        return combined;
    }

    private Optional<String> findAnnotationValue(List<AnnotationNode> annotations, String name) {
        return annotations.stream()
                .filter(a -> a.getName().equals(name))
                .findFirst()
                .flatMap(AnnotationNode::getValue);
    }

    // Utility method to extract a map of $name annotations from a list of AnnotationNode objects
    private Map<String, Object> extractAnnotationsMap(List<AnnotationNode> annotations) {
        Map<String, Object> annotationMap = new HashMap<>();
        if (annotations != null) {
            for (AnnotationNode annotation : annotations) {
                if (!annotation.isIdAnnotation()) { // Exclude @id from general annotations map
                    annotationMap.put(annotation.name, Optional.ofNullable(annotation.value).orElse(true)); // Use field access
                }
            }
        }
        return Collections.unmodifiableMap(annotationMap);
    }

    // Helper to extract @id and $annotations from a list of ANTLR AnnotationContexts
    // This is used by many visit methods.
    private Optional<Long> extractId(List<AnnotationContext> annotationCtxs) {
        if (annotationCtxs == null) return Optional.empty();
        for (AnnotationContext annoCtx : annotationCtxs) {
            AnnotationNode annotationNode = (AnnotationNode) visitAnnotation(annoCtx);
            if (annotationNode.isIdAnnotation()) {
                try {
                    return Optional.of(Long.parseLong(String.valueOf(annotationNode.value))); // Use field access
                } catch (NumberFormatException e) {
                     System.err.println("Warning: @id annotation has non-integer value: " + annotationNode.value);
                }
            }
        }
        return Optional.empty();
    }

    private List<AnnotationNode> extractAnnotations(List<AnnotationContext> annotationCtxs) {
        if (annotationCtxs == null) return Collections.emptyList();
        return annotationCtxs.stream()
            .map(annoCtx -> (AnnotationNode) visitAnnotation(annoCtx))
            .filter(Objects::nonNull)
             // Filter out @id annotations from the general list, as it's handled separately by extractId
            .filter(anno -> !anno.isIdAnnotation())
            .collect(Collectors.toList());
    }

    static class BlockContainerNode implements AstNode {
        final String blockType;
        final List<AstNode> children;
        final List<AnnotationNode> annotations; // Added field

        BlockContainerNode(String type, List<AstNode> children) {
            this(type, children, Collections.emptyList());
        }

        BlockContainerNode(String type, List<AstNode> children, List<AnnotationNode> annotations) {
            this.blockType = type;
            this.children = children != null ? Collections.unmodifiableList(new ArrayList<>(children)) : Collections.emptyList();
            this.annotations = annotations != null ? Collections.unmodifiableList(new ArrayList<>(annotations)) : Collections.emptyList();
        }

        public List<AstNode> getChildren() { return children; }
        public List<AnnotationNode> getBlockAnnotations() { return annotations; } // Getter for block's own annotations

        @Override public <T> T accept(NodeVisitor<T> visitor) { return null; } // Or visitor.visitBlockContainerNode(this)
        @Override public String toString() { return "BlockContainer[" + blockType + ", children=" + children.size() + ", annotations=" + annotations.size() + "]"; }
        @Override public Map<String, Object> getAnnotations() { // These are annotations ON the block, not its children
            Map<String, Object> annotationMap = new HashMap<>();
            for (AnnotationNode annotation : this.annotations) {
                annotationMap.put(annotation.getName(), annotation.getValue().orElse("true"));
            }
            return Collections.unmodifiableMap(annotationMap);
        }
        @Override public Optional<Long> getId() { // Blocks themselves typically don't have @id
            for (AnnotationNode annotation : this.annotations) {
                if ("@id".equals(annotation.getName()) && annotation.getValue().isPresent()) {
                     try {
                        Object rawValue = annotation.getValue().get();
                        if (rawValue instanceof Long) return Optional.of((Long) rawValue);
                        if (rawValue instanceof Number) return Optional.of(((Number) rawValue).longValue());
                        return Optional.of(Long.parseLong(String.valueOf(rawValue)));
                    } catch (NumberFormatException e) { /* ignore */ }
                }
            }
            return Optional.empty();
        }
    }
}