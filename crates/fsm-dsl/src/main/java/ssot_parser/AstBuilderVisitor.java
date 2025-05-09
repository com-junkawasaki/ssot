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
         List<AstNode> stateNodes = new ArrayList<>(); // Changed to List<AstNode>
         if (ctx.stateDefinitionOrHistoryState() != null) {
             for (StateDefinitionOrHistoryStateContext stateOrHistCtx : ctx.stateDefinitionOrHistoryState()) {
                 if (stateOrHistCtx.stateDefinition() != null) {
                     AstNode visitedNode = visitStateDefinition(stateOrHistCtx.stateDefinition());
                     if (visitedNode != null) stateNodes.add(visitedNode);
                 } else if (stateOrHistCtx.historyStateDefinition() != null) {
                     AstNode historyNode = visitHistoryStateDefinition(stateOrHistCtx.historyStateDefinition());
                     if (historyNode != null) stateNodes.add(historyNode);
                 }
             }
         }
         return new BlockContainerNode("states", stateNodes); // stateNodes is already List<AstNode>
     }

    @Override
    public AstNode visitStateDefinition(StateDefinitionContext ctx) {
        String stateName = ctx.ID().getText();
        this.currentStateName = stateName; // Set current state name for context in deeper calls if needed

        Optional<Long> id = extractId(ctx.annotation());
        List<AnnotationNode> annotationsList = extractAnnotations(ctx.annotation());
        String displayName = findAnnotationValue(annotationsList, "$name").orElse(stateName);

        StateType resolvedType;
        // Determine StateType based on structure and $final annotation
        // Order: PARALLEL -> COMPOUND -> FINAL -> ATOMIC as per user request
        if (ctx.parallelStateBlock() != null) {
            resolvedType = StateType.PARALLEL;
        } else if (ctx.statesBlock() != null || (ctx.stateBodyItem() != null && ctx.stateBodyItem().stream().anyMatch(item -> item.statesBlock()!=null || item.historyStateDefinition()!=null ))) {
            // Check for statesBlock or historyStateDefinition if stateBodyItem is used
            // For now, assuming direct ctx.statesBlock() is the primary indicator for COMPOUND if not PARALLEL
            resolvedType = StateType.COMPOUND;
        } else if (annotationsList.stream().anyMatch(a -> "$final".equals(a.getName()))) {
            resolvedType = StateType.FINAL;
        } else {
            resolvedType = StateType.ATOMIC;
        }
        // If grammar has specific initialStateSpec for compound/parallel states directly under stateDefinition:
        // Optional<String> initialStateName = (resolvedType == StateType.COMPOUND || resolvedType == StateType.PARALLEL) && ctx.initialStateSpec() != null ?
        //                                   Optional.of(ctx.initialStateSpec().ID().getText()) : Optional.empty();
        // Assuming initialStateSpec might be within stateBodyItem or a specific field:
        Optional<String> initialStateName = Optional.empty();
        if ((resolvedType == StateType.COMPOUND || resolvedType == StateType.PARALLEL)) {
            if (ctx.initialStateSpec() != null) { // Direct child
                 initialStateName = Optional.of(ctx.initialStateSpec().ID().getText());
            } else if (ctx.stateBodyItem() != null) { // Check within stateBodyItem if applicable
                initialStateName = ctx.stateBodyItem().stream()
                    .filter(item -> item.initialStateSpec() != null)
                    .findFirst()
                    .map(item -> item.initialStateSpec().ID().getText());
            }
        }


        List<String> entryActions = new ArrayList<>();
        if (ctx.entryActions() != null && ctx.entryActions().actionReferenceList() != null) {
            entryActions = extractActionReferences(ctx.entryActions().actionReferenceList());
        }
        // Also check stateBodyItem if entryActions can be there
         else if (ctx.stateBodyItem() != null) {
            ctx.stateBodyItem().stream()
                .filter(item -> item.entryActions() != null && item.entryActions().actionReferenceList() != null)
                .findFirst().ifPresent(item -> entryActions.addAll(extractActionReferences(item.entryActions().actionReferenceList())));
        }


        List<String> exitActions = new ArrayList<>();
        if (ctx.exitActions() != null && ctx.exitActions().actionReferenceList() != null) {
            exitActions = extractActionReferences(ctx.exitActions().actionReferenceList());
        }
        // Also check stateBodyItem
        else if (ctx.stateBodyItem() != null) {
             ctx.stateBodyItem().stream()
                .filter(item -> item.exitActions() != null && item.exitActions().actionReferenceList() != null)
                .findFirst().ifPresent(item -> exitActions.addAll(extractActionReferences(item.exitActions().actionReferenceList())));
        }


        List<InvokeStateNode> invokeInvocations = new ArrayList<>();
        List<TransitionNode> eventHandlers = new ArrayList<>(); // Assuming EventHandlerNode is TransitionNode
        List<TransitionNode> ifTransitions = new ArrayList<>(); // Assuming ConditionalTransitionNode is TransitionNode
        List<StateNode> nestedStates = new ArrayList<>();
        List<HistoryStateNode> historyStates = new ArrayList<>();

        // Process single invokeState if present (current grammar structure)
        if (ctx.invokeState() != null) {
            AstNode invokeNode = visitInvokeState(ctx.invokeState());
            if (invokeNode instanceof InvokeStateNode) {
                invokeInvocations.add((InvokeStateNode) invokeNode);
            }
        }

        // Process transitions
        if (ctx.onTransition() != null) { // Singular in current structure
            AstNode onTransNode = visitOnTransition(ctx.onTransition());
            if (onTransNode instanceof TransitionNode) {
                 eventHandlers.add((TransitionNode) onTransNode);
            }
        }
        if (ctx.conditionalTransition() != null) { // Singular block in current structure
            AstNode condTransBlockNode = visitConditionalTransition(ctx.conditionalTransition());
            if (condTransBlockNode instanceof BlockContainerNode) {
                ((BlockContainerNode) condTransBlockNode).getChildren().forEach(tNode -> {
                    if (tNode instanceof TransitionNode) ifTransitions.add((TransitionNode) tNode);
                });
            }
        }
        if (ctx.afterTransition() != null) { // List in current structure
            for (AfterTransitionContext afterCtx : ctx.afterTransition()) {
                AstNode afterTransNode = visitAfterTransition(afterCtx);
                if (afterTransNode instanceof TransitionNode) {
                     eventHandlers.add((TransitionNode) afterTransNode);
                }
            }
        }
         if (ctx.alwaysTransition() != null) { // List in current structure
             for (AlwaysTransitionContext alwaysCtx : ctx.alwaysTransition()) {
                 AstNode alwaysTransNode = visitAlwaysTransition(alwaysCtx);
                 if (alwaysTransNode instanceof TransitionNode) {
                    eventHandlers.add((TransitionNode) alwaysTransNode);
                 }
             }
         }

        // If stateBodyItem list exists and contains invokes/transitions, iterate it:
        // This part is speculative based on "ctx.stateBodyItem()" in prompt.
        // If your grammar strictly uses the direct fields (e.g. ctx.invokeState(), ctx.onTransition()),
        // then the above direct processing is sufficient.
        // If stateBodyItem() IS the way these are structured:
        if (ctx.stateBodyItem() != null) {
            for (StateBodyItemContext itemCtx : ctx.stateBodyItem()) {
                if (itemCtx.invokeState() != null) {
                    // If multiple invokes were added here, ensure not to duplicate with single ctx.invokeState() above
                    // This logic assumes either single ctx.invokeState() OR items in stateBodyItem list, not both for invokes.
                    // For this refactor, let's assume the single ctx.invokeState() is the current definite source.
                    // If stateBodyItem can also contain invokes, that needs clarification on precedence/combination.
                } else if (itemCtx.onTransition() != null) {
                    AstNode itemOnTrans = visitOnTransition(itemCtx.onTransition());
                    if (itemOnTrans instanceof TransitionNode) eventHandlers.add((TransitionNode)itemOnTrans);
                } else if (itemCtx.afterTransition() != null) { // afterTransition in body item would be singular
                    AstNode itemAfterTrans = visitAfterTransition(itemCtx.afterTransition());
                     if (itemAfterTrans instanceof TransitionNode) eventHandlers.add((TransitionNode)itemAfterTrans);
                } else if (itemCtx.alwaysTransition() != null) { // alwaysTransition in body item would be singular
                     AstNode itemAlwaysTrans = visitAlwaysTransition(itemCtx.alwaysTransition());
                     if (itemAlwaysTrans instanceof TransitionNode) eventHandlers.add((TransitionNode)itemAlwaysTrans);
                } else if (itemCtx.conditionalTransition() != null) {
                    AstNode itemCondTrans = visitConditionalTransition(itemCtx.conditionalTransition());
                     if (itemCondTrans instanceof BlockContainerNode) {
                        ((BlockContainerNode) itemCondTrans).getChildren().forEach(t -> {
                            if (t instanceof TransitionNode) ifTransitions.add((TransitionNode)t);
                        });
                    }
                }
                // nested states and history states might also be inside stateBodyItem if grammar allows
                else if (itemCtx.statesBlock() != null && resolvedType == StateType.COMPOUND) {
                     AstNode visitedStatesBlock = visitStatesBlock(itemCtx.statesBlock());
                     if (visitedStatesBlock instanceof BlockContainerNode) {
                        ((BlockContainerNode) visitedStatesBlock).getChildren().forEach(child -> {
                            if (child instanceof StateNode) nestedStates.add((StateNode) child);
                            else if (child instanceof HistoryStateNode) historyStates.add((HistoryStateNode) child);
                        });
                    }
                }
                 else if (itemCtx.historyStateDefinition() != null && resolvedType == StateType.COMPOUND) {
                    AstNode historyNode = visitHistoryStateDefinition(itemCtx.historyStateDefinition());
                    if (historyNode instanceof HistoryStateNode) {
                        historyStates.add((HistoryStateNode) historyNode);
                    }
                }
            }
        }


        // Populate nestedStates and historyStates for COMPOUND and PARALLEL types
        // This is primary handling if statesBlock/parallelStateBlock are direct children:
        if (resolvedType == StateType.COMPOUND && ctx.statesBlock() != null) {
            AstNode visitedStatesBlock = visitStatesBlock(ctx.statesBlock());
            if (visitedStatesBlock instanceof BlockContainerNode) {
                ((BlockContainerNode) visitedStatesBlock).getChildren().forEach(child -> {
                    if (child instanceof StateNode) nestedStates.add((StateNode) child);
                    else if (child instanceof HistoryStateNode) historyStates.add((HistoryStateNode) child);
                });
            }
        } else if (resolvedType == StateType.PARALLEL && ctx.parallelStateBlock() != null) {
            if (ctx.parallelStateBlock().regionDefinition() != null) {
                for (RegionDefinitionContext regionCtx : ctx.parallelStateBlock().regionDefinition()) {
                    // String regionName = regionCtx.ID() != null ? regionCtx.ID().getText() : null; // If needed
                    if (regionCtx.statesBlock() != null) {
                        AstNode visitedRegionStates = visitStatesBlock(regionCtx.statesBlock());
                        if (visitedRegionStates instanceof BlockContainerNode) {
                            ((BlockContainerNode) visitedRegionStates).getChildren().forEach(child -> {
                                if (child instanceof StateNode) nestedStates.add((StateNode) child);
                                // History states typically not per-region but per compound parent.
                            });
                        }
                    }
                }
            }
        }
        
        // Create the StateNode using assumed new constructor
        // Ensure StateNode.java constructor matches these parameters and types
        StateNode stateNode = new StateNode(
            id,
            stateName, // The actual name/identifier
            annotationsList, // Pass the raw list of AnnotationNode
            displayName, // User-friendly display name
            resolvedType,
            entryActions,
            exitActions,
            invokeInvocations,
            eventHandlers, // Assuming List<TransitionNode> is compatible with StateNode's eventHandlers field
            ifTransitions,  // Assuming List<TransitionNode> is compatible with StateNode's ifTransitions field
            nestedStates,
            historyStates,
            initialStateName
        );

        this.currentStateName = null; // Reset for next state
        return stateNode;
    }

    private List<String> extractActionReferences(ActionReferenceListContext ctx) {
        List<String> refs = new ArrayList<>();
        if (ctx != null && ctx.ID() != null) {
            ctx.ID().forEach(idNode -> refs.add(idNode.getText()));
        }
        return refs;
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
        // System.out.println("Visiting ConditionalTransition (delegating to singles)");
        List<TransitionNode> allConditionalTransitions = new ArrayList<>();
        if (ctx.conditionalTransitionSingle() != null) {
            for (ConditionalTransitionSingleContext singleCtx : ctx.conditionalTransitionSingle()) {
                AstNode node = visitConditionalTransitionSingle(singleCtx);
                if (node instanceof TransitionNode) {
                    allConditionalTransitions.add((TransitionNode) node);
                } else if (node instanceof BlockContainerNode) { 
                     System.err.println("BlockContainerNode from visitConditionalTransitionSingle, unexpected if it should return one TransitionNode.");
                     // If visitConditionalTransitionSingle can return a list in a container, handle it:
                     ((BlockContainerNode) node).getChildren().forEach(item -> {
                         if (item instanceof TransitionNode) allConditionalTransitions.add((TransitionNode)item);
                     });
                } else if (node != null) {
                    System.err.println("Unexpected node type from visitConditionalTransitionSingle: " + node.getClass().getName());
                }
            }
        }
        return new BlockContainerNode("conditionalTransitionsList", new ArrayList<>(allConditionalTransitions));
    }


    public AstNode visitConditionalTransitionSingle(ConditionalTransitionSingleContext ctx) {
        if (ctx.ifTransition() != null) {
            return visitIfTransition(ctx.ifTransition());
        } else if (ctx.elseIfTransition() != null) {
            return visitElseIfTransition(ctx.elseIfTransition());
        } else if (ctx.elseTransition() != null) {
            return visitElseTransition(ctx.elseTransition());
        }
        return null;
    }


     @Override
     public AstNode visitAfterTransition(AfterTransitionContext ctx) {
         Duration delay = Duration.parse("PT" + ctx.DURATION().getText()); // Example: PT10S for 10 seconds
         TransitionTarget target = (TransitionTarget) visitTransitionTarget(ctx.transitionTarget());
         List<String> actions = ctx.actionReferenceList() != null ? extractActionReferences(ctx.actionReferenceList()) : Collections.emptyList();
         List<AnnotationNode> annotations = extractAnnotations(ctx.annotation());

         return new TransitionNode(
                 Optional.empty(),
                 annotations,
                 null, // event (implicit from 'after')
                 null, // cond
                 target,
                 actions,
                 TransitionNode.TransitionType.AFTER,
                 delay
         );
     }

     @Override
     public AstNode visitAlwaysTransition(AlwaysTransitionContext ctx) {
        TransitionTarget target = (TransitionTarget) visitTransitionTarget(ctx.transitionTarget());
        List<String> actions = ctx.actionReferenceList() != null ? extractActionReferences(ctx.actionReferenceList()) : Collections.emptyList();
        List<AnnotationNode> annotations = extractAnnotations(ctx.annotation());
        return new TransitionNode(
                Optional.empty(),
                annotations,
                null, // event
                null, // cond
                target,
                actions,
                TransitionNode.TransitionType.ALWAYS,
                null // delay
        );
    }

    public AstNode visitIfTransition(IfTransitionContext ctx) { // Made public for direct call if needed
        String guardName = null;
        if (ctx.guardReference() != null && ctx.guardReference().ID() != null) {
            guardName = ctx.guardReference().ID().getText();
        } else if (ctx.inlineGuardExpression() != null) {
            guardName = "$inline_guard: " + ctx.inlineGuardExpression().getText();
        }

        TransitionTarget target = null;
        List<String> actions = Collections.emptyList();
        List<AnnotationNode> annotations = Collections.emptyList();

        if (ctx.transitionSpec() != null) {
            TransitionSpecContext spec = ctx.transitionSpec();
            if (spec.transitionTarget() != null) {
                 target = (TransitionTarget) visitTransitionTarget(spec.transitionTarget());
            }
            if (spec.actionReferenceList() != null) {
                actions = extractActionReferences(spec.actionReferenceList());
            }
            if (spec.annotation() != null && !spec.annotation().isEmpty()) { // Check if annotation() is available on spec
                annotations = extractAnnotations(spec.annotation());
            }
        } else if (ctx.transitionActionOnly() != null) {
             if (ctx.transitionActionOnly().actionReferenceList() != null) {
                actions = extractActionReferences(ctx.transitionActionOnly().actionReferenceList());
            }
            // Check if annotation() is available on transitionActionOnly
            // if (ctx.transitionActionOnly().annotation() != null && !ctx.transitionActionOnly().annotation().isEmpty()) {
            // annotations = extractAnnotations(ctx.transitionActionOnly().annotation());
            // }
            // For now, target remains null.
        }

        if (guardName == null) {
             System.err.println("Error: If transition without a guard at line " + ctx.start.getLine());
             return new TransitionNode(Optional.empty(), annotations, null, null, target, actions, TransitionNode.TransitionType.CONDITIONAL, null);
        }
         if (target == null && actions.isEmpty()) {
             System.err.println("Error: If transition without target or actions for guard '" + guardName + "' at line " + ctx.start.getLine());
             return new TransitionNode(Optional.empty(), annotations, null, guardName, new TransitionTarget(this.currentStateName, TransitionTarget.Type.INTERNAL), Collections.emptyList(), TransitionNode.TransitionType.CONDITIONAL, null);
         }

        return new TransitionNode(
                Optional.empty(), annotations, null, guardName, target, actions,
                TransitionNode.TransitionType.CONDITIONAL, null
        );
    }

    public AstNode visitElseIfTransition(ElseIfTransitionContext ctx) { // Made public
        String guardName = null;
        if (ctx.guardReference() != null && ctx.guardReference().ID() != null) {
            guardName = ctx.guardReference().ID().getText();
        } else if (ctx.inlineGuardExpression() != null) {
            guardName = "$inline_guard: " + ctx.inlineGuardExpression().getText();
        }

        TransitionTarget target = null;
        List<String> actions = Collections.emptyList();
        List<AnnotationNode> annotations = Collections.emptyList(); // Assuming annotations can exist on else if

        if (ctx.transitionSpec() != null) {
            TransitionSpecContext spec = ctx.transitionSpec();
            if (spec.transitionTarget() != null) {
                 target = (TransitionTarget) visitTransitionTarget(spec.transitionTarget());
            }
            if (spec.actionReferenceList() != null) {
                actions = extractActionReferences(spec.actionReferenceList());
            }
             if (spec.annotation() != null && !spec.annotation().isEmpty()) {
                annotations = extractAnnotations(spec.annotation());
            }
        } else if (ctx.transitionActionOnly() != null) {
            if (ctx.transitionActionOnly().actionReferenceList() != null) {
                actions = extractActionReferences(ctx.transitionActionOnly().actionReferenceList());
            }
        }


        if (guardName == null) {
            System.err.println("Error: Else If transition without a guard at line " + ctx.start.getLine());
            return new TransitionNode(Optional.empty(), annotations, null, null, target, actions, TransitionNode.TransitionType.CONDITIONAL, null);
        }
        if (target == null && actions.isEmpty()) {
            System.err.println("Error: Else If transition without target or actions for guard '" + guardName + "' at line " + ctx.start.getLine());
            return new TransitionNode(Optional.empty(), annotations, null, guardName, new TransitionTarget(this.currentStateName, TransitionTarget.Type.INTERNAL), Collections.emptyList(), TransitionNode.TransitionType.CONDITIONAL, null);
        }


        return new TransitionNode(
            Optional.empty(), annotations, null, guardName, target, actions,
            TransitionNode.TransitionType.CONDITIONAL, null
        );
    }

    public AstNode visitElseTransition(ElseTransitionContext ctx) { // Made public
        TransitionTarget target = null;
        List<String> actions = Collections.emptyList();
        List<AnnotationNode> annotations = Collections.emptyList(); // Assuming annotations can exist on else

        if (ctx.transitionSpec() != null) {
            TransitionSpecContext spec = ctx.transitionSpec();
            if (spec.transitionTarget() != null) {
                 target = (TransitionTarget) visitTransitionTarget(spec.transitionTarget());
            }
            if (spec.actionReferenceList() != null) {
                actions = extractActionReferences(spec.actionReferenceList());
            }
             if (spec.annotation() != null && !spec.annotation().isEmpty()) {
                annotations = extractAnnotations(spec.annotation());
            }
        } else if (ctx.transitionActionOnly() != null) {
             if (ctx.transitionActionOnly().actionReferenceList() != null) {
                actions = extractActionReferences(ctx.transitionActionOnly().actionReferenceList());
            }
        }


        if (target == null && actions.isEmpty()) {
            System.err.println("Error: Else transition without target or actions at line " + ctx.start.getLine());
            // Create a "do nothing" transition for else, or it should be a validation error
            return new TransitionNode(Optional.empty(), annotations, null, null, new TransitionTarget(this.currentStateName, TransitionTarget.Type.INTERNAL), Collections.emptyList(), TransitionNode.TransitionType.CONDITIONAL, null);
        }

        return new TransitionNode(
            Optional.empty(), annotations, null, null, /* No guard for else */
            target, actions, TransitionNode.TransitionType.CONDITIONAL, null
        );
    }


     @Override
     public AstNode visitTransitionTarget(TransitionTargetContext ctx) {
         String targetName = ctx.ID().getText();
         TransitionTarget.Type type = TransitionTarget.Type.INTERNAL; // Default
         if (ctx.EXTERNAL_TRANSITION() != null) {
             type = TransitionTarget.Type.EXTERNAL;
         }
         // Annotations on target? Not in current grammar.
         return new TransitionTarget(targetName, type);
     }

     @Override
     public AstNode visitTransitionSpec(TransitionSpecContext ctx) {
         // This method might not be directly called if its components are visited by parent rules.
         // However, if called, it should aggregate parts into a TransitionNode like object
         // For now, let specific transition visitors (on, if, after, always) handle this.
         System.out.println("Visiting TransitionSpec (should be handled by specific transition type visitors)");
         // If it needs to return a full TransitionNode, it would need context for event, guard, type, delay.
         // This suggests that visitTransitionSpec itself might not be the right place to build a full TransitionNode
         // without more context from the parent (e.g. OnTransitionContext provides the event).
         // Let's assume it returns a "partial" transition spec or is handled by callers.
         // For now, return null as it's likely handled by its callers.
         return null;
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
         String historyStateName = ctx.ID().getText();
         Optional<Long> id = extractId(ctx.annotation());
         List<AnnotationNode> annotations = extractAnnotations(ctx.annotation());
         boolean isDeep = ctx.DEEP() != null;
         
         HistoryStateType historyType = isDeep ? HistoryStateType.DEEP : HistoryStateType.SHALLOW;

         Optional<TransitionNode> defaultTransition = Optional.empty();
         if (ctx.transitionSpec() != null) {
             TransitionSpecContext spec = ctx.transitionSpec();
             TransitionTarget target = spec.transitionTarget() != null ? (TransitionTarget) visitTransitionTarget(spec.transitionTarget()) : null;
             List<String> actions = spec.actionReferenceList() != null ? extractActionReferences(spec.actionReferenceList()) : Collections.emptyList();
             List<AnnotationNode> transitionAnnotations = spec.annotation() != null ? extractAnnotations(spec.annotation()) : Collections.emptyList();
             
             // Create the TransitionNode for the default transition
             TransitionNode transition = new TransitionNode(
                     Optional.empty(), // id for the transition itself
                     transitionAnnotations, // annotations on the transition spec
                     null, // event (none for default history transition)
                     null, // guard (none for default history transition)
                     target, // target state
                     actions, // actions for this transition
                     TransitionNode.TransitionType.INTERNAL, // Or other appropriate type
                     null // delay
             );
             defaultTransition = Optional.of(transition);
         }

        return new HistoryStateNode(id, historyStateName, historyType, defaultTransition, annotations);
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

    private Optional<Long> extractId(List<AnnotationContext> annotations) {
        if (annotations == null) return Optional.empty();
        for (AnnotationContext annoCtx : annotations) {
            if ("@id".equals(annoCtx.annotationName().getText())) {
                if (annoCtx.annotationValue() != null && annoCtx.annotationValue().NUMBER() != null) {
                    try {
                        return Optional.of(Long.parseLong(annoCtx.annotationValue().NUMBER().getText()));
                    } catch (NumberFormatException e) {
                        System.err.println("Error parsing @id value: " + annoCtx.annotationValue().NUMBER().getText());
                        return Optional.empty();
                    }
                } else if (annoCtx.annotationValue() == null) { // For @id()
                    // Generate or assign a unique ID if necessary, or treat as error/ignore
                    // For now, returning empty, as this implies a semantic action beyond parsing.
                    System.err.println("@id annotation found without a value. Auto-generation not implemented.");
                    return Optional.empty();
                }
            }
        }
        return Optional.empty();
    }

    private List<AnnotationNode> extractAnnotations(List<AnnotationContext> annotationContexts) {
        if (annotationContexts == null || annotationContexts.isEmpty()) {
            return Collections.emptyList();
        }
        return annotationContexts.stream()
                                 .map(this::visitAnnotation) // 'this::visitAnnotation' already returns AnnotationNode
                                 .map(astNode -> (AnnotationNode) astNode) // Cast needed if visitAnnotation returns AstNode
                                 .collect(Collectors.toList());
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
        BlockContainerNode(String type, List<AstNode> children) {
            this.blockType = type;
            this.children = children != null ? new ArrayList<>(children) : Collections.emptyList();
        }
        public List<AstNode> getChildren() { return children; }
        @Override public <T> T accept(NodeVisitor<T> visitor) { return null; }
        @Override public String toString() { return "BlockContainer[" + blockType + ", children=" + children.size() + "]"; }
        @Override public Map<String, Object> getAnnotations() { return Collections.emptyMap(); }
        @Override public Optional<Long> getId() { return Optional.empty(); }
    }
}