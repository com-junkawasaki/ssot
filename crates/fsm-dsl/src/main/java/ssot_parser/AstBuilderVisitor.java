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
import ssot_parser.ast.nodes.state.StateNode; // Assuming StateNode is here
import ssot_parser.ast.nodes.state.HistoryStateNode; // Assuming HistoryStateNode is here
import ssot_parser.ast.nodes.state.StateType; // Assuming StateType enum is here
// import ssot_parser.ast.nodes.EventHandlerNode; // If this is a specific type
// import ssot_parser.ast.nodes.ConditionalTransitionNode; // If this is a specific type

/**
 * Visits the ANTLR Parse Tree and builds the Abstract Syntax Tree (AST).
 */
public class AstBuilderVisitor extends SSoTBaseVisitor<AstNode> {

    private String currentStateName = null;

    @Override
    public AstNode visitSsotDefinition(SsotDefinitionContext ctx) {
        System.out.println("Visiting SsotDefinition");
        // Corrected SsotRoot instantiation based on its likely constructor
        SsotRoot root = new SsotRoot(
                new ArrayList<>(), // imports
                new ArrayList<>(), // typeDefs
                new ArrayList<>(), // serviceDefinitions
                new ArrayList<>(), // machineNodes
                new ArrayList<>(), // actorNodes (assuming)
                // TODO: Add other top-level block lists like communication, deployment, dependencies
                Optional.empty(),  // id
                new HashMap<>()    // annotations
        );

        if (ctx.annotation() != null && !ctx.annotation().isEmpty()) {
             ctx.annotation().forEach(annoCtx -> {
                 AnnotationNode annotation = (AnnotationNode) visitAnnotation(annoCtx);
                 root.getAnnotations().put(annotation.getName(), annotation.getValue().orElse("true")); // Example storage
                 System.out.println("  Found Root Annotation: " + annotation);
             });
        }

        if (ctx.importStatement() != null) {
            for (ImportStatementContext importCtx : ctx.importStatement()) {
                root.getImports().add((ImportNode) visitImportStatement(importCtx));
            }
        }

        if (ctx.topLevelBlock() != null) {
            for (TopLevelBlockContext blockCtx : ctx.topLevelBlock()) {
                AstNode blockNode = visit(blockCtx);
                if (blockNode instanceof BlockContainerNode) {
                    BlockContainerNode container = (BlockContainerNode) blockNode;
                    switch (container.blockType) {
                        case "types":
                            container.getChildren().forEach(child -> root.getTypeDefs().add((TypeDefNode) child));
                            break;
                        case "services":
                            container.getChildren().forEach(child -> {
                                if (child instanceof ServiceDefinitionNode) {
                                    root.getServiceDefinitions().add((ServiceDefinitionNode) child);
                                } else if (child instanceof InterfaceDefinitionNode) {
                                    // Assuming ServiceDefinitions list can hold InterfaceDefinitionNode or SsotRoot needs separate list
                                    // For now, adding to service definitions as a placeholder.
                                     System.err.println("InterfaceDefinitionNode encountered in services block, needs proper handling in SsotRoot.");
                                     // root.getInterfaceDefinitions().add((InterfaceDefinitionNode) child);
                                }
                            });
                            break;
                        case "machines":
                            container.getChildren().forEach(child -> root.getMachineNodes().add((MachineNode) child));
                            break;
                        // Add cases for other block types: actors, communication, etc.
                        default:
                            System.err.println("Warning: Unhandled block container type: " + container.blockType);
                            break;
                    }
                } else if (blockNode instanceof TypeDefNode) { // Direct handling if not in container
                     root.getTypeDefs().add((TypeDefNode) blockNode);
                } else if (blockNode instanceof ServiceDefinitionNode) {
                     root.getServiceDefinitions().add((ServiceDefinitionNode) blockNode);
                } else if (blockNode instanceof MachineNode) {
                     root.getMachineNodes().add((MachineNode) child); // Corrected from blockNode to child
                }
                // Add other direct node types if necessary
            }
        }
        System.out.println("Finished Visiting SsotDefinition");
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
                // Ensure ServiceDefinitionNode is imported and cast correctly
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
                 // Ensure InterfaceDefinitionNode is imported and cast correctly
                 AstNode interfaceDef = visit(interfaceCtx);
                 if (interfaceDef instanceof InterfaceDefinitionNode) {
                    serviceDefs.add(interfaceDef);
                 } else {
                     System.err.println("Expected InterfaceDefinitionNode, got: " + (interfaceDef != null ? interfaceDef.getClass().getName() : "null"));
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
        Optional<Long> id = extractId(ctx.annotation());
        List<AnnotationNode> annotations = extractAnnotations(ctx.annotation());
        List<FieldNode> fields = new ArrayList<>();
        if (ctx.structBody() != null && ctx.structBody().fieldDefinition() != null) {
            for (FieldDefinitionContext fieldCtx : ctx.structBody().fieldDefinition()) {
                fields.add((FieldNode) visitFieldDefinition(fieldCtx));
            }
        }
        return new TypeDefNode(id, structName, TypeDefNode.TypeKind.STRUCT, fields, null, annotations);
    }

    @Override
    public AstNode visitEnumDefinition(EnumDefinitionContext ctx) {
        String enumName = ctx.ID().getText();
        Optional<Long> id = extractId(ctx.annotation());
        List<AnnotationNode> annotations = extractAnnotations(ctx.annotation());
        List<EnumVariantNode> variants = new ArrayList<>();
        if (ctx.enumBody() != null && ctx.enumBody().enumVariant() != null) {
            for (EnumVariantContext variantCtx : ctx.enumBody().enumVariant()) {
                variants.add((EnumVariantNode) visitEnumVariant(variantCtx));
            }
        }
        return new TypeDefNode(id, enumName, TypeDefNode.TypeKind.ENUM, null, variants, annotations);
    }

    @Override
    public AstNode visitFieldDefinition(FieldDefinitionContext ctx) {
        String fieldName = ctx.ID().getText();
        ssot_parser.ast.type.TypeNode type = (ssot_parser.ast.type.TypeNode) visitTypeName(ctx.typeName());
        Optional<Long> id = extractId(ctx.annotation());
        List<AnnotationNode> annotations = extractAnnotations(ctx.annotation());
        Optional<ssot_parser.ast.values.ValueNode> defaultValue = Optional.empty();
        if (ctx.defaultValueSpec() != null && ctx.defaultValueSpec().value() != null) {
            defaultValue = Optional.ofNullable((ssot_parser.ast.values.ValueNode) visitValue(ctx.defaultValueSpec().value()));
        }
        return new FieldNode(id, fieldName, type, annotations, defaultValue);
    }

     @Override
     public AstNode visitEnumVariant(EnumVariantContext ctx) {
         String variantName = ctx.ID().getText();
         Optional<Long> id = extractId(ctx.annotation());
         // Corrected: ensure annotation list is extracted only if annotations exist
         List<AnnotationNode> annotations = ctx.annotation() != null ? extractAnnotations(ctx.annotation()) : Collections.emptyList();
         return new EnumVariantNode(id, variantName, annotations);
     }

    @Override
    public AstNode visitMachineDefinition(MachineDefinitionContext ctx) {
        String machineName = ctx.machineHeader().ID().getText();
        Optional<Long> id = extractId(ctx.machineHeader().annotation());
        List<AnnotationNode> headerAnnotations = extractAnnotations(ctx.machineHeader().annotation());
        String initialStateName = ctx.machineHeader().initialStateSpec().ID().getText();

        ContextNode contextNode = null;
        if (ctx.machineBody().contextBlock() != null) {
            contextNode = (ContextNode) visitContextBlock(ctx.machineBody().contextBlock());
        }

        List<ActionDefinitionNode> actions = new ArrayList<>();
        if (ctx.machineBody().actionsBlock() != null && ctx.machineBody().actionsBlock().actionDefinition() != null) {
            for (ActionDefinitionContext actionCtx : ctx.machineBody().actionsBlock().actionDefinition()) {
                actions.add((ActionDefinitionNode) visitActionDefinition(actionCtx));
            }
        }

        List<GuardDefinitionNode> guards = new ArrayList<>();
        if (ctx.machineBody().guardsBlock() != null && ctx.machineBody().guardsBlock().guardDefinition() != null) {
            for (GuardDefinitionContext guardCtx : ctx.machineBody().guardsBlock().guardDefinition()) {
                guards.add((GuardDefinitionNode) visitGuardDefinition(guardCtx));
            }
        }

        List<InvokeDefinitionNode> invokes = new ArrayList<>();
        if (ctx.machineBody().invokesBlock() != null && ctx.machineBody().invokesBlock().invokeDefinition() != null) {
            for (InvokeDefinitionContext invokeCtx : ctx.machineBody().invokesBlock().invokeDefinition()) {
                invokes.add((InvokeDefinitionNode) visitInvokeDefinition(invokeCtx));
            }
        }

        // Changed to List<AstNode> to hold both StateNode and HistoryStateNode
        List<AstNode> topLevelStates = new ArrayList<>();
        if (ctx.machineBody().statesBlock() != null && ctx.machineBody().statesBlock().stateDefinitionOrHistoryState() != null) {
            for (StateDefinitionOrHistoryStateContext stateOrHistCtx : ctx.machineBody().statesBlock().stateDefinitionOrHistoryState()) {
                if (stateOrHistCtx.stateDefinition() != null) {
                    AstNode stateNode = visitStateDefinition(stateOrHistCtx.stateDefinition());
                    if (stateNode != null) {
                        topLevelStates.add(stateNode);
                    }
                } else if (stateOrHistCtx.historyStateDefinition() != null) {
                     AstNode historyNode = visitHistoryStateDefinition(stateOrHistCtx.historyStateDefinition());
                     if (historyNode != null) {
                        topLevelStates.add(historyNode);
                     }
                }
            }
        }
        Map<String, Object> machineAnnotationsMap = mapAnnotations(headerAnnotations);
        // Assuming MachineNode constructor is updated to accept List<AstNode> for states
        return new MachineNode(id, machineName, machineAnnotationsMap, contextNode, actions, guards, invokes, topLevelStates, initialStateName);
    }

    @Override
    public AstNode visitContextBlock(ContextBlockContext ctx) {
        List<ContextVariableNode> variables = new ArrayList<>();
        if (ctx.contextVariableDefinition() != null) {
            for (ContextVariableDefinitionContext varCtx : ctx.contextVariableDefinition()) {
                variables.add((ContextVariableNode) visitContextVariableDefinition(varCtx));
            }
        }
        // Assuming ContextNode constructor (id, variables, annotations)
        return new ContextNode(extractId(ctx.annotation()), variables, mapAnnotations(extractAnnotations(ctx.annotation())));
    }

    @Override
    public AstNode visitContextVariableDefinition(ContextVariableDefinitionContext ctx) {
        String varName = ctx.ID().getText();
        ssot_parser.ast.type.TypeNode type = (ssot_parser.ast.type.TypeNode) visitTypeName(ctx.typeName());
        Optional<Long> id = extractId(ctx.annotation());
        List<AnnotationNode> annotations = extractAnnotations(ctx.annotation());
        Optional<ssot_parser.ast.values.ValueNode> defaultValue = Optional.empty();
        if (ctx.value() != null) {
           defaultValue = Optional.ofNullable((ssot_parser.ast.values.ValueNode) visitValue(ctx.value()));
        }
        return new ContextVariableNode(id, varName, type, annotations, defaultValue);
    }

    @Override
    public AstNode visitActionsBlock(ActionsBlockContext ctx) {
        List<ActionDefinitionNode> actionDefs = new ArrayList<>();
        if (ctx.actionDefinition() != null) {
            for (ActionDefinitionContext actionDefCtx : ctx.actionDefinition()) {
                actionDefs.add((ActionDefinitionNode) visitActionDefinition(actionDefCtx));
            }
        }
        return new BlockContainerNode("actions", new ArrayList<>(actionDefs));
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
    public AstNode visitGuardsBlock(GuardsBlockContext ctx) {
        List<GuardDefinitionNode> guardDefs = new ArrayList<>();
        if (ctx.guardDefinition() != null) {
            for (GuardDefinitionContext guardDefCtx : ctx.guardDefinition()) {
                guardDefs.add((GuardDefinitionNode) visitGuardDefinition(guardDefCtx));
            }
        }
        return new BlockContainerNode("guards", new ArrayList<>(guardDefs));
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
    public AstNode visitInvokesBlock(InvokesBlockContext ctx) {
        List<InvokeDefinitionNode> invokeDefs = new ArrayList<>();
        if (ctx.invokeDefinition() != null) {
            for (InvokeDefinitionContext invokeDefCtx : ctx.invokeDefinition()) {
                invokeDefs.add((InvokeDefinitionNode) visitInvokeDefinition(invokeDefCtx));
            }
        }
        return new BlockContainerNode("invokes", new ArrayList<>(invokeDefs));
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
     public AstNode visitStatesBlock(StatesBlockContext ctx) {
         System.out.println("Visiting States Block (likely for machine's top-level states or direct nested states)");
         List<AstNode> stateNodes = new ArrayList<>();
         if (ctx.stateDefinition() != null) {
             for (StateDefinitionContext stateCtx : ctx.stateDefinition()) {
                 AstNode stateNode = visitStateDefinition(stateCtx); // This is recursive if stateDefinition calls statesBlock
                 if (stateNode != null) {
                     stateNodes.add(stateNode);
                 }
             }
         }
         // Annotations on the block itself, if any.
         // List<AnnotationNode> blockAnnotations = ctx.annotation() != null ? extractAnnotations(ctx.annotation()) : Collections.emptyList();
         // Here, we are returning a container. The caller (e.g. visitMachineDefinition or visitStateDefinition for nested)
         // will unpack this.
         return new BlockContainerNode("states", stateNodes /*, blockAnnotations */); // Assuming BlockContainerNode can take annotations
     }

    // New method based on grammar rule `statesDefinition`
    // This is very similar to visitStatesBlock. The difference is the context type.
    // If SSoT.g4 `statesDefinition` rule maps to `StatesDefinitionContext` by ANTLR
    public AstNode visitStatesDefinition(StatesDefinitionContext ctx) {
        System.out.println("Visiting StatesDefinition (likely for nested states via stateBodyElement)");
        List<AstNode> definedStates = new ArrayList<>();
        if (ctx.stateDefinition() != null) {
            for (StateDefinitionContext stateCtx : ctx.stateDefinition()) {
                AstNode stateNode = visitStateDefinition(stateCtx); // Recursive call
                if (stateNode != null) {
                    definedStates.add(stateNode);
                }
            }
        }
        // Annotations for the 'states { ... }' block itself
        List<AnnotationNode> blockAnnotations = ctx.annotation() != null ? extractAnnotations(ctx.annotation()) : Collections.emptyList();
        // Return a container; the caller (visitStateDefinition's loop) will unpack this.
        return new BlockContainerNode("states", definedStates, blockAnnotations);
    }

    @Override
    public AstNode visitStateDefinition(StateDefinitionContext ctx) {
        String stateName = ctx.ID().getText(); // Corrected: stateName is from ID token
        this.currentStateName = stateName;

        List<AnnotationNode> annotationsList = ctx.annotation() != null ? extractAnnotations(ctx.annotation()) : Collections.emptyList();
        Optional<Long> id = extractIdFromList(annotationsList); // Use helper that takes List<AnnotationNode>
        String displayName = findAnnotationValue(annotationsList, "$name").orElse(stateName);
        Optional<String> initialAnnotationValue = findAnnotationValue(annotationsList, "$initial");
        boolean isExplicitlyParallel = findAnnotationValue(annotationsList, "$type").orElse("").equalsIgnoreCase("parallel");
        boolean isExplicitlyFinal = annotationsList.stream().anyMatch(a -> "$final".equals(a.getName()) && "true".equalsIgnoreCase(String.valueOf(a.getValue().orElse("false"))));


        // Initialize lists
        List<String> entryActions = new ArrayList<>();
        List<String> exitActions = new ArrayList<>();
        List<InvokeStateNode> invokeInvocations = new ArrayList<>();
        List<TransitionNode> eventHandlers = new ArrayList<>();
        List<TransitionNode> ifTransitions = new ArrayList<>(); // Remains empty based on current grammar analysis for stateDefinition
        List<StateNode> nestedStates = new ArrayList<>();
        List<HistoryStateNode> historyStates = new ArrayList<>();
        Optional<String> resolvedInitialStateName = initialAnnotationValue;

        boolean hasNestedStatesOrHistory = false;

        // Process stateBodyElement*
        if (ctx.stateBodyElement() != null) {
            for (StateBodyElementContext itemCtx : ctx.stateBodyElement()) {
                if (itemCtx.onEntryExit() != null) {
                    OnEntryExitContext entryExitCtx = itemCtx.onEntryExit();
                    String actionName = entryExitCtx.actionReference().getText(); // Assumes actionReference().getText() is safe
                    if (entryExitCtx.ON_ENTRY() != null) {
                        entryActions.add(actionName);
                    } else if (entryExitCtx.ON_EXIT() != null) {
                        exitActions.add(actionName);
                    }
                } else if (itemCtx.invokeState() != null) {
                    AstNode invokeNode = visitInvokeState(itemCtx.invokeState());
                    if (invokeNode instanceof InvokeStateNode) {
                        invokeInvocations.add((InvokeStateNode) invokeNode);
                    }
                } else if (itemCtx.onTransition() != null) {
                    AstNode onTransNode = visitOnTransition(itemCtx.onTransition());
                    if (onTransNode instanceof TransitionNode) {
                        eventHandlers.add((TransitionNode) onTransNode);
                    }
                } else if (itemCtx.afterTransition() != null) {
                    AstNode afterTransNode = visitAfterTransition(itemCtx.afterTransition());
                    if (afterTransNode instanceof TransitionNode) {
                        eventHandlers.add((TransitionNode) afterTransNode);
                    }
                } else if (itemCtx.statesDefinition() != null) { // Grammar: statesDefinition
                    hasNestedStatesOrHistory = true;
                    AstNode visitedStatesBlock = visitStatesDefinition(itemCtx.statesDefinition());
                    if (visitedStatesBlock instanceof BlockContainerNode) {
                        ((BlockContainerNode) visitedStatesBlock).getChildren().forEach(child -> {
                            if (child instanceof StateNode) {
                                nestedStates.add((StateNode) child);
                            } else {
                                System.err.println("Warning: Unexpected node type in statesDefinition block: " + child.getClass().getName());
                            }
                        });
                    }
                } else if (itemCtx.historyDefinition() != null) { // Grammar: historyDefinition
                    hasNestedStatesOrHistory = true;
                    AstNode historyNode = visitHistoryDefinition(itemCtx.historyDefinition());
                    if (historyNode instanceof HistoryStateNode) {
                        historyStates.add((HistoryStateNode) historyNode);
                    }
                }
                // itemCtx.annotation() is ignored here, as state-level annotations are already processed.
                // Annotations for specific elements are handled by their respective visit methods.
            }
        }

        StateType resolvedType;
        if (isExplicitlyFinal) {
            resolvedType = StateType.FINAL;
        } else if (isExplicitlyParallel) {
            resolvedType = StateType.PARALLEL;
            if (nestedStates.isEmpty() && !resolvedInitialStateName.isPresent()) {
                 System.err.println("Warning: State '" + stateName + "' is $type('parallel') but has no nested states (regions) and no $initial annotation.");
            }
        } else if (hasNestedStatesOrHistory || !nestedStates.isEmpty() || !historyStates.isEmpty() || resolvedInitialStateName.isPresent()) {
            // If it has nested state definitions, history states, or an explicit initial state for children, it's compound.
            resolvedType = StateType.COMPOUND;
        } else {
            resolvedType = StateType.ATOMIC;
        }

        // If compound/parallel and no $initial, default to first child state if any
        if ((resolvedType == StateType.COMPOUND || resolvedType == StateType.PARALLEL) && !resolvedInitialStateName.isPresent() && !nestedStates.isEmpty()) {
            resolvedInitialStateName = Optional.of(nestedStates.get(0).getStateName());
            // System.out.println("Info: State '" + stateName + "' is " + resolvedType + ". Defaulting initial state to first child: '" + nestedStates.get(0).getStateName() + "' as no $initial annotation found.");
        }
         // If it's compound but has no nested states and no explicit initial, it might be an issue or an atomic state effectively.
        if ((resolvedType == StateType.COMPOUND || resolvedType == StateType.PARALLEL) && nestedStates.isEmpty() && !resolvedInitialStateName.isPresent() && !isExplicitlyParallel) {
            // If not explicitly parallel and ends up looking atomic, downgrade.
            // However, if $initial was present, it implies children are expected elsewhere or it's a forward declaration.
            // For now, if it has $initial, keep as COMPOUND. Otherwise, if no children, it's ATOMIC.
            if (!initialAnnotationValue.isPresent()) { // Check original annotation, not potentially defaulted one
                 // System.out.println("Info: State '" + stateName + "' determined as COMPOUND/PARALLEL but has no nested states and no $initial. Resolving to ATOMIC.");
                 resolvedType = StateType.ATOMIC;
            }
        }


        StateNode stateNode = new StateNode(
            id,
            stateName,
            annotationsList,
            displayName,
            resolvedType,
            entryActions,
            exitActions,
            invokeInvocations,
            eventHandlers,
            ifTransitions, // Empty for now as per grammar analysis
            nestedStates,
            historyStates,
            resolvedInitialStateName
        );

        this.currentStateName = null;
        return stateNode;
    }

    private Optional<Long> extractIdFromList(List<AnnotationNode> annotations) {
        if (annotations == null) return Optional.empty();
        for (AnnotationNode annotation : annotations) {
            if ("@id".equals(annotation.getName()) && annotation.getValue().isPresent()) {
                try {
                    // Assuming ID value is stored directly as a Long or String convertible to Long
                    Object rawValue = annotation.getValue().get();
                    if (rawValue instanceof Long) {
                        return Optional.of((Long) rawValue);
                    } else if (rawValue instanceof Number) {
                        return Optional.of(((Number) rawValue).longValue());
                    } else if (rawValue instanceof String) {
                        return Optional.of(Long.parseLong((String) rawValue));
                    }
                } catch (NumberFormatException e) {
                    System.err.println("Warning: Could not parse @id value: " + annotation.getValue().get());
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
    public AstNode visitHistoryStateDefinition(HistoryStateDefinitionContext ctx) {
        // grammar: HISTORY (SHALLOW | DEEP)? annotation* TARGET ID SEMI
        List<AnnotationNode> annotations = ctx.annotation() != null ? extractAnnotations(ctx.annotation()) : Collections.emptyList();
        Optional<Long> id = extractIdFromList(annotations); // Use helper
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
    public AstNode visitTypeName(TypeNameContext ctx) {
        if (ctx.primitiveType() != null) {
            return new BaseType(ctx.primitiveType().getText(), Optional.empty(), new HashMap<>());
        } else if (ctx.customType() != null) {
            return new CustomType(ctx.customType().getText(), Optional.empty(), Optional.empty(), new HashMap<>());
        } else if (ctx.parametrizedType() != null) {
            String baseTypeName = ctx.parametrizedType().ID().getText();
            List<ssot_parser.ast.type.TypeNode> params = new ArrayList<>();
            if (ctx.parametrizedType().typeNameList() != null) {
                for (TypeNameContext paramCtx : ctx.parametrizedType().typeNameList().typeName()) {
                    params.add((ssot_parser.ast.type.TypeNode) visitTypeName(paramCtx));
                }
            }
            return new ParametrizedType(baseTypeName, params, Optional.empty(), new HashMap<>());
        }
        System.err.println("Unknown typeName structure: " + ctx.getText());
        return new BaseType("unknown", Optional.empty(), new HashMap<>()); // Fallback
    }

    @Override
    public AstNode visitAnnotation(AnnotationContext ctx) {
        String name = ctx.annotationName().getText();
        Optional<String> value = Optional.empty();
        if (ctx.annotationValue() != null) {
            if (ctx.annotationValue().STRING() != null) {
                value = Optional.of(stripQuotes(ctx.annotationValue().STRING().getText()));
            } else if (ctx.annotationValue().NUMBER() != null) {
                value = Optional.of(ctx.annotationValue().NUMBER().getText());
            } else if (ctx.annotationValue().BOOLEAN() != null) {
                value = Optional.of(ctx.annotationValue().BOOLEAN().getText());
            } else if (ctx.annotationValue().ID() != null) { // For references like @type(MyType)
                value = Optional.of(ctx.annotationValue().ID().getText());
            }
            // TODO: Handle array and object values for annotations if grammar supports it
        }
        return new AnnotationNode(name, value);
    }

    @Override
    public AstNode visitValue(ValueContext ctx) {
        if (ctx.STRING() != null) {
            return new StringValueNode(stripQuotes(ctx.STRING().getText()));
        } else if (ctx.NUMBER() != null) {
            String numText = ctx.NUMBER().getText();
            try {
                if (numText.contains(".")) {
                    return new NumberValueNode(Double.parseDouble(numText));
                } else {
                    return new NumberValueNode(Long.parseLong(numText));
                }
            } catch (NumberFormatException e) {
                return new StringValueNode(numText); // Fallback
            }
        } else if (ctx.BOOLEAN() != null) {
            return new BooleanValueNode(Boolean.parseBoolean(ctx.BOOLEAN().getText()));
        } else if (ctx.NULL() != null) {
            return new NullValueNode();
        } else if (ctx.array() != null) {
            List<ssot_parser.ast.values.ValueNode> elements = new ArrayList<>();
            if (ctx.array().valueList() != null) {
                for (ValueContext valCtx : ctx.array().valueList().value()) {
                    elements.add((ssot_parser.ast.values.ValueNode) visitValue(valCtx));
                }
            }
            return new ArrayValueNode(elements);
        } else if (ctx.object() != null) {
            Map<String, ssot_parser.ast.values.ValueNode> properties = new HashMap<>();
            if (ctx.object().keyValuePairList() != null) {
                for (KeyValuePairContext pairCtx : ctx.object().keyValuePairList().keyValuePair()) {
                    String key = stripQuotes(pairCtx.STRING().getText());
                    properties.put(key, (ssot_parser.ast.values.ValueNode) visitValue(pairCtx.value()));
                }
            }
            return new ObjectValueNode(properties);
        }
        return new StringValueNode(ctx.getText()); // Fallback
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