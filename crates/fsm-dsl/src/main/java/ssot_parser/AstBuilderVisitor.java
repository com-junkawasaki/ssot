package ssot_parser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.antlr.v4.runtime.tree.ParseTree; // Import needed for context checks
import java.util.Map;
import java.util.HashMap;
import ssot_parser.SSoTParser.AnnotationContext; // Add import
import ssot_parser.ast.*; // Import common AST interfaces/classes
import ssot_parser.ast.nodes.*; // Import specific node classes like MachineNode, AnnotationNode, etc.
import ssot_parser.ast.nodes.InvokeStateNode.InvokeTransition; // Import inner record
import ssot_parser.ast.type.*;
import ssot_parser.ast.values.*; // Import ValueNode and implementations
import ssot_parser.ast.handlers.InvokeCompletionHandler; // Import InvokeCompletionHandler
import java.time.Duration; // For AFTER transitions

/**
 * Visits the ANTLR Parse Tree and builds the Abstract Syntax Tree (AST).
 * This class extends the generated SSoTBaseVisitor and overrides methods
 * for specific grammar rules to create corresponding AST nodes.
 */
// Make sure AstBuilderVisitor<T> matches SSoTBaseVisitor<T> (AstNode seems correct)
public class AstBuilderVisitor extends SSoTBaseVisitor<AstNode> {

    // Keep track of the current state name for transitions
    private String currentStateName = null;

    @Override
    public AstNode visitSsotDefinition(SSoTParser.SsotDefinitionContext ctx) {
        System.out.println("Visiting SsotDefinition");
        SsotRoot root = new SsotRoot();
        if (ctx.annotation() != null && !ctx.annotation().isEmpty()) {
             // Assuming top-level annotations apply to the whole file/root
             // This might need refinement based on how global annotations are used.
             ctx.annotation().forEach(annoCtx -> {
                 AnnotationNode annotation = (AnnotationNode) visitAnnotation(annoCtx);
                 // How to store/use root annotations? Add to SsotRoot?
                 System.out.println("  Found Root Annotation: " + annotation);
             });
        }

        if (ctx.importStatement() != null) {
            for (SSoTParser.ImportStatementContext importCtx : ctx.importStatement()) {
                root.addChild(visitImportStatement(importCtx));
            }
        }

        // Visit top-level blocks
        if (ctx.topLevelBlock() != null) {
            for (SSoTParser.TopLevelBlockContext blockCtx : ctx.topLevelBlock()) {
                AstNode blockNode = visit(blockCtx); // visitTypesBlock, visitServicesBlock, etc.
                if (blockNode != null) {
                    // Add the whole block node (e.g., TypesBlockNode) or individual items?
                    // Let's assume visitTopLevelBlock returns a container or similar
                    // For now, let's process specific blocks directly if needed or add children
                    if (blockNode instanceof TypeDefNode || blockNode instanceof ServiceDefinitionNode ||
                        blockNode instanceof MachineNode || blockNode instanceof ActorNode ||
                        blockNode instanceof CommunicationNode || blockNode instanceof DeploymentConfigNode ||
                        blockNode instanceof DependencyNode) {
                        root.addChild(blockNode);
                    } else if (blockNode instanceof BlockContainerNode) { // Hypothetical container
                        root.addChildren(((BlockContainerNode) blockNode).getChildren());
                    } else {
                         System.err.println("Warning: Unhandled top level block type: " + blockNode.getClass().getSimpleName());
                    }
                }
            }
        }


        System.out.println("Finished Visiting SsotDefinition");
        return root;
    }

    @Override
    public AstNode visitImportStatement(SSoTParser.ImportStatementContext ctx) {
        String path = stripQuotes(ctx.STRING().getText());
        return new ImportNode(path);
    }

    // --- Top Level Block Handling ---
    // visitTopLevelBlock redirects to specific block visitors

    @Override
    public AstNode visitTypesBlock(SSoTParser.TypesBlockContext ctx) {
        System.out.println("Visiting Types Block");
        // Option 1: Return a conceptual "TypesBlockNode" that contains the TypeDefNodes
        // Option 2: Visit children directly and expect the caller (visitSsotDefinition) to handle them.
        // Let's go with Option 2 for now, visitTypeDef will be called for each struct/enum
        // We might need a container if block-level annotations ($description) are important.
         if (ctx.typeDefinition() != null) {
            for (SSoTParser.TypeDefinitionContext typeCtx : ctx.typeDefinition()) {
                 // The visitSsotDefinition loop will call visit on this block,
                 // which should then call visit on typeDefinition.
                 // This direct call might be redundant depending on ANTLR visitor flow.
                 // Let's rely on the default visitor behavior to visit children.
                 // visit(typeCtx); // Likely redundant if visitSsotDefinition visits topLevelBlock
            }
        }
         // This visitor method itself doesn't need to return a single node representing the block
         // if the caller iterates through the definitions inside.
         // However, to fit the visitor pattern return type, we might need a dummy node or handle it differently.
         // Let's return null and handle children in the parent visitor method for now.
         // A better approach: Make visitSsotDefinition iterate typeDefinition directly.
         // Let's stick to returning *something* for now.
         // Return a dummy container or process children and return them?
         // For now, the main loop in visitSsotDefinition handles iterating blocks.
         // The default visitChildren(ctx) might be sufficient if we want individual nodes back.

         // Let's refine: This method *should* process the contents of the block.
         // We'll create the TypeDefNodes here and return a container or handle in parent.
         List<AstNode> typeDefs = new ArrayList<>();
         if (ctx.typeDefinition() != null) {
            for (SSoTParser.TypeDefinitionContext typeCtx : ctx.typeDefinition()) {
                 TypeDefNode typeDef = (TypeDefNode) visit(typeCtx);
                 if (typeDef != null) {
                     typeDefs.add(typeDef);
                 }
            }
         }
         // How to return multiple nodes? Use a special container or have visitSsotDefinition look inside?
         // Let's return a temporary container node.
         return new BlockContainerNode("types", typeDefs); // Assume BlockContainerNode exists
    }


    @Override
    public AstNode visitServicesBlock(SSoTParser.ServicesBlockContext ctx) {
        System.out.println("Visiting Services Block");
        List<AstNode> serviceDefs = new ArrayList<>();
         if (ctx.serviceDefinition() != null) {
            for (SSoTParser.ServiceDefinitionContext serviceCtx : ctx.serviceDefinition()) {
                ServiceDefinitionNode serviceDef = (ServiceDefinitionNode) visit(serviceCtx);
                 if (serviceDef != null) {
                     serviceDefs.add(serviceDef);
                 }
            }
         }
         // Also handle interface definitions if they can be top-level in the block
         if (ctx.interfaceDefinition() != null) {
             for (SSoTParser.InterfaceDefinitionContext interfaceCtx : ctx.interfaceDefinition()) {
                 InterfaceDefinitionNode interfaceDef = (InterfaceDefinitionNode) visit(interfaceCtx);
                  if (interfaceDef != null) {
                      serviceDefs.add(interfaceDef); // Add interfaces to the same list for now
                  }
             }
         }
        return new BlockContainerNode("services", serviceDefs);
    }

    @Override
    public AstNode visitMachinesBlock(SSoTParser.MachinesBlockContext ctx) {
        System.out.println("Visiting Machines Block");
        List<AstNode> machineDefs = new ArrayList<>();
         if (ctx.machineDefinition() != null) {
            for (SSoTParser.MachineDefinitionContext machineCtx : ctx.machineDefinition()) {
                 MachineNode machineDef = (MachineNode) visit(machineCtx);
                 if (machineDef != null) {
                     machineDefs.add(machineDef);
                 }
            }
         }
        return new BlockContainerNode("machines", machineDefs);
    }

    // Add similar visitors for visitActorsBlock, visitCommunicationBlock, etc.
    // returning BlockContainerNode or similar.

    // --- Type Definition ---
    @Override
    public AstNode visitTypeDefinition(SSoTParser.TypeDefinitionContext ctx) {
        System.out.println("Visiting TypeDefinition");
        if (ctx.structDefinition() != null) {
            return visitStructDefinition(ctx.structDefinition());
        } else if (ctx.enumDefinition() != null) {
            return visitEnumDefinition(ctx.enumDefinition());
        }
        return null; // Or throw error
    }

    @Override
    public AstNode visitStructDefinition(SSoTParser.StructDefinitionContext ctx) {
        String structName = ctx.ID().getText();
        System.out.println("  Visiting Struct: " + structName);
        Optional<Long> id = extractId(ctx.annotation());
        List<AnnotationNode> annotations = extractAnnotations(ctx.annotation());

        List<FieldNode> fields = new ArrayList<>();
        if (ctx.structBody() != null && ctx.structBody().fieldDefinition() != null) {
            for (SSoTParser.FieldDefinitionContext fieldCtx : ctx.structBody().fieldDefinition()) {
                fields.add((FieldNode) visitFieldDefinition(fieldCtx));
            }
        }
        // Decide if TypeDefNode wraps StructNode or if TypeDefNode *is* the struct/enum node.
        // Let's make TypeDefNode the container.
        return new TypeDefNode(id, structName, TypeDefNode.TypeKind.STRUCT, fields, null, annotations); // null for enum variants
    }

    @Override
    public AstNode visitEnumDefinition(SSoTParser.EnumDefinitionContext ctx) {
        String enumName = ctx.ID().getText();
         System.out.println("  Visiting Enum: " + enumName);
        Optional<Long> id = extractId(ctx.annotation());
        List<AnnotationNode> annotations = extractAnnotations(ctx.annotation());

        List<EnumVariantNode> variants = new ArrayList<>();
        if (ctx.enumBody() != null && ctx.enumBody().enumVariant() != null) {
            for (SSoTParser.EnumVariantContext variantCtx : ctx.enumBody().enumVariant()) {
                variants.add((EnumVariantNode) visitEnumVariant(variantCtx));
            }
        }
        return new TypeDefNode(id, enumName, TypeDefNode.TypeKind.ENUM, null, variants, annotations); // null for fields
    }

    @Override
    public AstNode visitFieldDefinition(SSoTParser.FieldDefinitionContext ctx) {
        String fieldName = ctx.ID().getText();
        TypeNode type = (TypeNode) visitTypeName(ctx.typeName());
        Optional<Long> id = extractId(ctx.annotation());
        List<AnnotationNode> annotations = extractAnnotations(ctx.annotation());
        // Handle default value if present
        // Optional<ValueNode> defaultValue = Optional.empty();
        // if (ctx.defaultValueSpec() != null) { ... }

        System.out.println("    Visiting Field: " + fieldName + " Type: " + type);
        return new FieldNode(id, fieldName, type, annotations);
    }

     @Override
     public AstNode visitEnumVariant(SSoTParser.EnumVariantContext ctx) {
         String variantName = ctx.ID().getText();
         Optional<Long> id = extractId(ctx.annotation());
         List<AnnotationNode> annotations = extractAnnotations(ctx.annotation());
         System.out.println("    Visiting Enum Variant: " + variantName);
         return new EnumVariantNode(id, variantName, annotations);
     }


    // --- Machine Definition ---
    @Override
    public AstNode visitMachineDefinition(SSoTParser.MachineDefinitionContext ctx) {
        String machineName = ctx.machineHeader().ID().getText();
        System.out.println("Visiting MachineDefinition: " + machineName);
        Optional<Long> id = extractId(ctx.machineHeader().annotation());
        List<AnnotationNode> headerAnnotations = extractAnnotations(ctx.machineHeader().annotation()); // Annotations on the 'machine Foo @id()...' line

        String initialStateName = ctx.machineHeader().initialStateSpec().ID().getText();

        // Process body elements
        Optional<ContextNode> contextNode = Optional.empty();
        List<ActionDefinitionNode> actions = new ArrayList<>();
        List<GuardDefinitionNode> guards = new ArrayList<>();
        List<InvokeDefinitionNode> invokes = new ArrayList<>();
        List<StateNode> states = new ArrayList<>();
        List<AnnotationNode> bodyAnnotations = new ArrayList<>(); // Annotations directly inside machine {} before blocks

        if (ctx.machineBody() != null) {
             // Extract body annotations (e.g., $description)
            if (ctx.machineBody().annotation() != null) {
                 bodyAnnotations.addAll(extractAnnotations(ctx.machineBody().annotation()));
            }

             if (ctx.machineBody().contextBlock() != null) {
                 contextNode = Optional.ofNullable((ContextNode) visitContextBlock(ctx.machineBody().contextBlock()));
             }
             if (ctx.machineBody().actionsBlock() != null) {
                 // visitActionsBlock should return a list or container
                 AstNode actionsResult = visitActionsBlock(ctx.machineBody().actionsBlock());
                 if (actionsResult instanceof BlockContainerNode) { // Assuming BlockContainerNode holds the list
                     ((BlockContainerNode) actionsResult).getChildren().forEach(node -> {
                         if (node instanceof ActionDefinitionNode) actions.add((ActionDefinitionNode) node);
                     });
                 }
             }
             if (ctx.machineBody().guardsBlock() != null) {
                  AstNode guardsResult = visitGuardsBlock(ctx.machineBody().guardsBlock());
                 if (guardsResult instanceof BlockContainerNode) {
                     ((BlockContainerNode) guardsResult).getChildren().forEach(node -> {
                         if (node instanceof GuardDefinitionNode) guards.add((GuardDefinitionNode) node);
                     });
                 }
             }
             if (ctx.machineBody().invokesBlock() != null) {
                 AstNode invokesResult = visitInvokesBlock(ctx.machineBody().invokesBlock());
                 if (invokesResult instanceof BlockContainerNode) {
                     ((BlockContainerNode) invokesResult).getChildren().forEach(node -> {
                         if (node instanceof InvokeDefinitionNode) invokes.add((InvokeDefinitionNode) node);
                     });
                 }
             }
             if (ctx.machineBody().statesBlock() != null) {
                 AstNode statesResult = visitStatesBlock(ctx.machineBody().statesBlock());
                 if (statesResult instanceof BlockContainerNode) {
                     ((BlockContainerNode) statesResult).getChildren().forEach(node -> {
                         if (node instanceof StateNode) states.add((StateNode) node);
                     });
                 }
             }
        }

        // Combine header and body annotations? Or keep separate? Let's combine for now.
        List<AnnotationNode> allAnnotations = allAnnotations(headerAnnotations, bodyAnnotations);


        return new MachineNode(id, machineName, initialStateName, contextNode, actions, guards, invokes, states, allAnnotations);
    }

    @Override
    public AstNode visitContextBlock(SSoTParser.ContextBlockContext ctx) {
        System.out.println("  Visiting Context Block");
        List<FieldNode> fields = new ArrayList<>();
        if (ctx.contextVariableDefinition() != null) {
            for (SSoTParser.ContextVariableDefinitionContext varCtx : ctx.contextVariableDefinition()) {
                fields.add((FieldNode) visitContextVariableDefinition(varCtx));
            }
        }
        // Ignore annotations on the block itself for now, handled in visitMachineDefinition
        return new ContextNode(fields);
    }

    @Override
    public AstNode visitContextVariableDefinition(SSoTParser.ContextVariableDefinitionContext ctx) {
        String varName = ctx.ID().getText();
        TypeNode type = (TypeNode) visitTypeName(ctx.typeName());
        Optional<Long> id = extractId(ctx.annotation());
        List<AnnotationNode> annotations = extractAnnotations(ctx.annotation());
        // TODO: Handle default value ctx.defaultValueSpec()
        System.out.println("    Visiting Context Variable: " + varName + " Type: " + type);
        // Using FieldNode for context variables
        return new FieldNode(id, varName, type, annotations);
    }


    @Override
    public AstNode visitActionsBlock(SSoTParser.ActionsBlockContext ctx) {
         System.out.println("  Visiting Actions Block");
         List<AstNode> actionDefs = new ArrayList<>();
         if (ctx.actionDefinition() != null) {
             for (SSoTParser.ActionDefinitionContext actionCtx : ctx.actionDefinition()) {
                 ActionDefinitionNode actionDef = (ActionDefinitionNode) visitActionDefinition(actionCtx);
                 if (actionDef != null) {
                     actionDefs.add(actionDef);
                 }
             }
         }
         return new BlockContainerNode("actions", actionDefs);
    }

    @Override
    public AstNode visitActionDefinition(SSoTParser.ActionDefinitionContext ctx) {
        String actionName = ctx.ID(0).getText(); // First ID is the name
        Optional<Long> id = extractId(ctx.annotation());
        List<AnnotationNode> annotations = extractAnnotations(ctx.annotation());
        // Ignore parameters (ctx, event) for now
        System.out.println("    Visiting Action Definition: " + actionName);
        return new ActionDefinitionNode(id, actionName, annotations);
    }

    @Override
    public AstNode visitGuardsBlock(SSoTParser.GuardsBlockContext ctx) {
         System.out.println("  Visiting Guards Block");
         List<AstNode> guardDefs = new ArrayList<>();
         if (ctx.guardDefinition() != null) {
             for (SSoTParser.GuardDefinitionContext guardCtx : ctx.guardDefinition()) {
                 GuardDefinitionNode guardDef = (GuardDefinitionNode) visitGuardDefinition(guardCtx);
                 if (guardDef != null) {
                     guardDefs.add(guardDef);
                 }
             }
         }
         return new BlockContainerNode("guards", guardDefs);
    }

    @Override
    public AstNode visitGuardDefinition(SSoTParser.GuardDefinitionContext ctx) {
        String guardName = ctx.ID(0).getText(); // First ID is the name
        Optional<Long> id = extractId(ctx.annotation());
        List<AnnotationNode> annotations = extractAnnotations(ctx.annotation());
        // DSL grammar forces return type to bool (T_BOOL)
        TypeNode returnType = new TypeNode(BaseType.BOOLEAN); // Hardcoded based on grammar rule
        // Ignore parameter (ctx) for now
        System.out.println("    Visiting Guard Definition: " + guardName);
        return new GuardDefinitionNode(id, guardName, returnType, annotations);
    }

     @Override
    public AstNode visitInvokesBlock(SSoTParser.InvokesBlockContext ctx) {
         System.out.println("  Visiting Invokes Block");
         List<AstNode> invokeDefs = new ArrayList<>();
         if (ctx.invokeDefinition() != null) {
             for (SSoTParser.InvokeDefinitionContext invokeCtx : ctx.invokeDefinition()) {
                 InvokeDefinitionNode invokeDef = (InvokeDefinitionNode) visitInvokeDefinition(invokeCtx);
                 if (invokeDef != null) {
                     invokeDefs.add(invokeDef);
                 }
             }
         }
         return new BlockContainerNode("invokes", invokeDefs);
    }

     @Override
    public AstNode visitInvokeDefinition(SSoTParser.InvokeDefinitionContext ctx) {
        String invokeName = ctx.ID().getText();
        Optional<Long> id = extractId(ctx.annotation());
        List<AnnotationNode> annotations = extractAnnotations(ctx.annotation());

        String source = null;
        // Extract 'src' attribute from invokeDefinitionBody
        if (ctx.invokeDefinitionBody() != null && ctx.invokeDefinitionBody().invokeAttribute() != null) {
            for (SSoTParser.InvokeAttributeContext attrCtx : ctx.invokeDefinitionBody().invokeAttribute()) {
                if (attrCtx.attributeAssignment() != null && attrCtx.attributeAssignment().ID().getText().equals("src")) {
                    // Assuming src value is a reference or string literal
                    AstNode valueNode = visitValue(attrCtx.attributeAssignment().value());
                     if (valueNode instanceof ValueNode) { // Assuming visitValue returns ValueNode
                        source = ((ValueNode) valueNode).getRawValue(); // Get the string representation
                        System.out.println("      Found invoke src: " + source);
                     } else {
                         System.err.println("Warning: Could not extract string value for invoke src attribute");
                     }
                    break; // Found src, assuming only one
                }
                // Grammar divergence: Original grammar used SRC keyword. This assumes 'src: value;'
                 if (attrCtx.SRC() != null && attrCtx.invokeSource() != null) {
                     // Handle SRC keyword if grammar is updated
                     AstNode sourceNode = visitInvokeSource(attrCtx.invokeSource());
                     if (sourceNode instanceof ValueNode){ // Assuming visitInvokeSource returns simple value
                         source = ((ValueNode) sourceNode).getRawValue();
                          System.out.println("      Found invoke src (SRC keyword): " + source);
                     }
                      break;
                 }

            }
        }
         if (source == null) {
             System.err.println("Warning: Invoke definition '" + invokeName + "' missing 'src' attribute.");
             // Decide how to handle missing src: return null, throw error, or create node anyway?
             // Let's create the node but log the error. It should fail validation later.
             source = "MISSING_SOURCE";
         }

        System.out.println("    Visiting Invoke Definition: " + invokeName);
        return new InvokeDefinitionNode(id, invokeName, source, annotations);
    }

     // Helper method visitInvokeSource if using SRC keyword
     @Override
     public AstNode visitInvokeSource(SSoTParser.InvokeSourceContext ctx) {
         if (ctx.referenceValue() != null) {
             // Treat reference as a string for now
             return new ValueNode<>(ctx.referenceValue().getText());
         } else if (ctx.STRING() != null) {
             return new ValueNode<>(stripQuotes(ctx.STRING().getText()));
         }
         return null; // Should not happen based on grammar
     }


     @Override
     public AstNode visitStatesBlock(SSoTParser.StatesBlockContext ctx) {
         System.out.println("  Visiting States Block (in Machine/State)");
         List<AstNode> stateDefs = new ArrayList<>();
         if (ctx.stateDefinition() != null) {
             for (SSoTParser.StateDefinitionContext stateCtx : ctx.stateDefinition()) {
                 StateNode stateDef = (StateNode) visitStateDefinition(stateCtx);
                 if (stateDef != null) {
                     stateDefs.add(stateDef);
                 }
             }
         }
          // Also handle history definitions if needed
         return new BlockContainerNode("states", stateDefs);
     }


    // --- State Definition ---
    @Override
    public AstNode visitStateDefinition(SSoTParser.StateDefinitionContext ctx) {
        String stateName = ctx.stateHeader().ID().getText();
        System.out.println("  Visiting StateDefinition: " + stateName);

        Optional<Long> id = extractId(ctx.annotation());
        List<AnnotationNode> annotations = extractAnnotations(ctx.annotation());
        Optional<String> nameAnnotation = findAnnotationValue(annotations, "$name");

        StateType type = StateType.ATOMIC;
        if (ctx.stateType() != null) {
            if (ctx.stateType().PARALLEL() != null) type = StateType.PARALLEL;
            else if (ctx.stateType().FINAL() != null) type = StateType.FINAL;
        }

        List<String> entryActions = new ArrayList<>();
        List<String> exitActions = new ArrayList<>();
        List<InvokeStateNode> invokeInvocations = new ArrayList<>();
        List<EventHandlerNode> eventHandlers = new ArrayList<>(); // For ON
        List<TransitionNode> afterTransitions = new ArrayList<>(); // Placeholder
        List<TransitionNode> alwaysTransitions = new ArrayList<>(); // Placeholder
        List<ConditionalTransitionNode> ifTransitions = new ArrayList<>(); // Placeholder
        List<StateNode> nestedStates = new ArrayList<>();
        List<HistoryStateNode> historyStates = new ArrayList<>();

        if (ctx.stateBody() != null) {
            for (SSoTParser.StateBodyItemContext itemCtx : ctx.stateBody().stateBodyItem()) {
                if (itemCtx.entryAction() != null) {
                    entryActions.addAll(extractActionReferences(itemCtx.entryAction().actionReferenceList()));
                    System.out.println("    Found Entry Actions: " + entryActions.get(entryActions.size()-1));
                } else if (itemCtx.exitAction() != null) {
                    exitActions.addAll(extractActionReferences(itemCtx.exitAction().actionReferenceList()));
                     System.out.println("    Found Exit Actions: " + exitActions.get(exitActions.size()-1));
                } else if (itemCtx.invokeState() != null) {
                    AstNode invokeNode = visitInvokeState(itemCtx.invokeState());
                    if (invokeNode instanceof InvokeStateNode) {
                        invokeInvocations.add((InvokeStateNode) invokeNode);
                        System.out.println("    Found Invoke: " + ((InvokeStateNode) invokeNode).getInvokeDefinitionRef());
                    } else {
                         System.err.println("Warning: visitInvokeState did not return an InvokeStateNode.");
                    }
                } else if (itemCtx.onTransition() != null) {
                    AstNode handlerNode = visitOnTransition(itemCtx.onTransition());
                    if (handlerNode instanceof EventHandlerNode) {
                        eventHandlers.add((EventHandlerNode) handlerNode);
                         System.out.println("    Found ON EventHandler for events: " + ((EventHandlerNode) handlerNode).getEventRefs());
                    } else {
                         System.err.println("Warning: visitOnTransition did not return an EventHandlerNode.");
                    }
                } else if (itemCtx.afterTransition() != null) {
                    AstNode transitionNode = visitAfterTransition(itemCtx.afterTransition());
                    if (transitionNode instanceof TransitionNode) {
                        afterTransitions.add((TransitionNode) transitionNode);
                        System.out.println("    Found AFTER Transition: -> " + ((TransitionNode)transitionNode).getTargetStateName() + " delay: " + ((TransitionNode)transitionNode).getDelay());
                    } else {
                        System.err.println("Warning: visitAfterTransition did not return a TransitionNode.");
                    }
                } else if (itemCtx.alwaysTransition() != null) {
                    AstNode transitionNode = visitAlwaysTransition(itemCtx.alwaysTransition());
                     if (transitionNode instanceof TransitionNode) {
                         alwaysTransitions.add((TransitionNode) transitionNode);
                         System.out.println("    Found ALWAYS Transition: -> " + ((TransitionNode)transitionNode).getTargetStateName());
                     } else {
                          System.err.println("Warning: visitAlwaysTransition did not return a TransitionNode.");
                     }
                } else if (itemCtx.ifTransition() != null) {
                    AstNode condTransitionNode = visitIfTransition(itemCtx.ifTransition());
                    if (condTransitionNode instanceof ConditionalTransitionNode) {
                        ifTransitions.add((ConditionalTransitionNode) condTransitionNode);
                        System.out.println("    Found IF Transition: IF " + ((ConditionalTransitionNode)condTransitionNode).getGuardRef() + " -> " + ((ConditionalTransitionNode)condTransitionNode).getTransition().getTargetStateName());
                    } else {
                         System.err.println("Warning: visitIfTransition did not return a ConditionalTransitionNode.");
                    }
                } else if (itemCtx.stateDefinition() != null) {
                    AstNode nestedStateNode = visitStateDefinition(itemCtx.stateDefinition());
                    if (nestedStateNode instanceof StateNode) {
                        nestedStates.add((StateNode) nestedStateNode);
                        System.out.println("    Found Nested State: " + ((StateNode) nestedStateNode).getStateName());
                    } else {
                         System.err.println("Warning: visitStateDefinition for nested state did not return a StateNode.");
                    }
                } else if (itemCtx.historyStateDefinition() != null) {
                     AstNode historyNode = visitHistoryStateDefinition(itemCtx.historyStateDefinition());
                    if (historyNode instanceof HistoryStateNode) {
                        historyStates.add((HistoryStateNode) historyNode);
                         System.out.println("    Found History State: " + ((HistoryStateNode) historyNode).getType());
                    } else {
                          System.err.println("Warning: visitHistoryStateDefinition did not return a HistoryStateNode.");
                    }
                } else {
                    System.err.println("Warning: Unhandled stateBodyItem type in state: " + stateName + " Context: " + itemCtx.getText());
                }
            }
        }

        // Create StateNode with all collected information
        // Ensure StateNode class supports these parameters
        return new StateNode(
                id,
                stateName,
                nameAnnotation.orElse(stateName),
                type,
                entryActions,
                exitActions,
                invokeInvocations,
                eventHandlers, // ON transitions
                afterTransitions, // AFTER transitions
                alwaysTransitions, // ALWAYS transitions
                ifTransitions, // Direct IF transitions
                nestedStates,
                historyStates,
                annotations
        );
    }

     // --- Helpers for StateDefinition ---

     private List<String> extractActionReferences(SSoTParser.ActionReferenceListContext ctx) {
         if (ctx == null) return Collections.emptyList();
         return ctx.actionReference().stream()
                   .map(ParseTree::getText) // Get the name directly for now
                   .collect(Collectors.toList());
     }

     @Override
     public AstNode visitOnTransition(SSoTParser.OnTransitionContext ctx) {
          System.out.println("      Visiting OnTransition");
         List<String> eventRefs = ctx.eventReferenceList().referenceValue().stream()
                                      .map(RuleContext::getText)
                                      .collect(Collectors.toList());

         List<TransitionNode> directTransitions = new ArrayList<>();
         List<ConditionalTransitionNode> conditionalTransitions = new ArrayList<>();
         List<AnnotationNode> annotations = new ArrayList<>(); // Annotations directly inside ON { ... }

         if (ctx.onTransitionOption() != null) {
             for (SSoTParser.OnTransitionOptionContext optCtx : ctx.onTransitionOption()) {
                 if (optCtx.annotation() != null) {
                     annotations.add((AnnotationNode) visitAnnotation(optCtx.annotation()));
                 } else if (optCtx.transitionSpec() != null) {
                     // Direct transition for this event
                     AstNode transitionNode = visitTransitionSpec(optCtx.transitionSpec());
                     if (transitionNode instanceof TransitionNode) {
                         // Assign the event name to the transition node if possible
                         // (TransitionNode might need an 'event' field or context)
                         ((TransitionNode) transitionNode).setEvent(eventRefs.toString()); // Store event(s) reference
                         directTransitions.add((TransitionNode) transitionNode);
                         System.out.println("        Found direct transition for event " + eventRefs + ": -> " + ((TransitionNode) transitionNode).getTargetStateName());
                     } else {
                          System.err.println("Warning: visitTransitionSpec inside ON did not return a TransitionNode.");
                     }
                 } else if (optCtx.conditionalTransition() != null) {
                     // Conditional transition for this event
                     AstNode condNode = visitConditionalTransition(optCtx.conditionalTransition());
                     if (condNode instanceof ConditionalTransitionNode) {
                         // Assign the event name to the conditional transition node's inner transition?
                         ((ConditionalTransitionNode) condNode).getTransition().setEvent(eventRefs.toString());
                         conditionalTransitions.add((ConditionalTransitionNode) condNode);
                          System.out.println("        Found conditional transition for event " + eventRefs + ": IF " + ((ConditionalTransitionNode) condNode).getGuardRef());
                     } else {
                          System.err.println("Warning: visitConditionalTransition inside ON did not return a ConditionalTransitionNode.");
                     }
                 }
             }
         }

         // Create EventHandlerNode containing event(s) and associated transitions
         // Ensure EventHandlerNode class exists and accepts these parameters
         return new EventHandlerNode(eventRefs, directTransitions, conditionalTransitions, annotations);
     }

     @Override
     public AstNode visitConditionalTransition(SSoTParser.ConditionalTransitionContext ctx) {
          System.out.println("        Visiting ConditionalTransition (inside ON)");
         String guardRef = ctx.guardReference().getText(); // Includes potential (not) prefix
         AstNode transitionNode = visitTransitionSpec(ctx.transitionSpec());

         if (!(transitionNode instanceof TransitionNode)) {
              System.err.println("Error: visitTransitionSpec inside ConditionalTransition did not return a TransitionNode.");
              // Return null or throw? Let's return null for now.
              return null;
         }

         TransitionNode transition = (TransitionNode) transitionNode;
         // Event name should be set by the calling context (visitOnTransition)

         // Assuming ConditionalTransitionNode constructor takes guard reference and the transition node
         // Ensure ConditionalTransitionNode class exists
         return new ConditionalTransitionNode(guardRef, transition);
     }

     @Override
     public AstNode visitAfterTransition(SSoTParser.AfterTransitionContext ctx) {
          System.out.println("      Visiting AfterTransition");
         String durationStr = ctx.duration().getText();
         System.out.println("        Duration: " + durationStr); // Need to parse this (e.g., "100ms", "2s")

         AstNode transitionNode = visitTransitionSpec(ctx.transitionSpec());
          if (!(transitionNode instanceof TransitionNode)) {
              System.err.println("Error: visitTransitionSpec inside AfterTransition did not return a TransitionNode.");
              return null;
         }
         TransitionNode transition = (TransitionNode) transitionNode;

         // Add delay info to the transition node
         // Ensure TransitionNode has a field and setter/constructor for delay
         transition.setDelay(durationStr); // Example setter
         transition.setEvent("AFTER_" + durationStr); // Set a synthetic event name

         return transition;
     }

     @Override
     public AstNode visitAlwaysTransition(SSoTParser.AlwaysTransitionContext ctx) {
          System.out.println("      Visiting AlwaysTransition");
         AstNode transitionNode = visitTransitionSpec(ctx.transitionSpec());
         if (!(transitionNode instanceof TransitionNode)) {
              System.err.println("Error: visitTransitionSpec inside AlwaysTransition did not return a TransitionNode.");
              return null;
         }
         TransitionNode transition = (TransitionNode) transitionNode;

         // Mark transition as always
         // Ensure TransitionNode has a field and setter/constructor for this
         transition.setAlways(true); // Example setter
          transition.setEvent("ALWAYS"); // Set a synthetic event name

         return transition;
     }

    @Override
    public AstNode visitIfTransition(SSoTParser.IfTransitionContext ctx) {
         System.out.println("      Visiting IfTransition (direct)");
        // Handles: if (Guard) { TARGET ... } else { TARGET ... }
        // Returns a ConditionalTransitionNode representing the IF-ELSE structure.
        // NOTE: This assumes the IF transition directly results in *one* ConditionalTransitionNode
        // containing the primary transition and an optional 'else' transition.
        // If grammar allows multiple transitions in THEN/ELSE, this needs adjustment.

        String guardRef = ctx.guardReference().getText();
        TransitionNode thenTransition = null;
        TransitionNode elseTransition = null;
        List<AnnotationNode> annotations = new ArrayList<>(); // Annotations on the IF line itself? Or inside blocks?
        // TODO: Clarify where annotations in IF {} ELSE {} should go.

        // --- Process 'then' block --- 
        // Find the first transitionSpec in the primary block
        SSoTParser.TransitionSpecContext thenSpecCtx = null;
        if (ctx.ifTransitionOption() != null) {
             for(SSoTParser.IfTransitionOptionContext opt : ctx.ifTransitionOption()) {
                 if (opt.transitionSpec() != null) {
                     thenSpecCtx = opt.transitionSpec();
                     // Extract annotations from this block?
                      if (opt.annotation() != null) annotations.addAll(extractAnnotations(opt.annotation()));
                     break; // Take the first transition found in the block
                 } else if (opt.annotation() != null) {
                     annotations.add((AnnotationNode) visitAnnotation(opt.annotation()));
                 }
             }
        }

        if (thenSpecCtx == null) {
            System.err.println("Error: IF transition for guard '" + guardRef + "' is missing a TARGET in its main block.");
            return null;
        }
        AstNode thenNode = visitTransitionSpec(thenSpecCtx);
        if (thenNode instanceof TransitionNode) {
            thenTransition = (TransitionNode) thenNode;
            thenTransition.setEvent("IF_" + guardRef); // Synthetic event
        } else {
            System.err.println("Error: visitTransitionSpec for IF 'then' block did not return TransitionNode.");
            return null;
        }

        // --- Process 'else' block (if present) --- Find the first transitionSpec after ELSE
        if (ctx.ELSE() != null) {
            SSoTParser.TransitionSpecContext elseSpecCtx = null;
            boolean inElseBlock = false;
            for (ParseTree child : ctx.children) {
                 if (child == ctx.ELSE()) {
                     inElseBlock = true;
                     continue;
                 }
                 if (inElseBlock && child instanceof SSoTParser.IfTransitionOptionContext) {
                      SSoTParser.IfTransitionOptionContext opt = (SSoTParser.IfTransitionOptionContext) child;
                      if (opt.transitionSpec() != null) {
                           elseSpecCtx = opt.transitionSpec();
                           // Extract annotations from else block?
                           if (opt.annotation() != null) annotations.addAll(extractAnnotations(opt.annotation())); // Add to overall annotations?
                           break; // Take first transition in else block
                      } else if (opt.annotation() != null) {
                          // Annotations in else block
                          annotations.add((AnnotationNode) visitAnnotation(opt.annotation()));
                      }
                 }
            }

            if (elseSpecCtx != null) {
                AstNode elseNode = visitTransitionSpec(elseSpecCtx);
                 if (elseNode instanceof TransitionNode) {
                     elseTransition = (TransitionNode) elseNode;
                     elseTransition.setEvent("ELSE_" + guardRef); // Synthetic event
                 } else {
                      System.err.println("Warning: visitTransitionSpec for IF 'else' block did not return TransitionNode.");
                 }
            }
        }

        // Create the ConditionalTransitionNode
        // Ensure ConditionalTransitionNode constructor/setters support else transition
        ConditionalTransitionNode conditionalNode = new ConditionalTransitionNode(guardRef, thenTransition);
        if (elseTransition != null) {
             conditionalNode.setElseTransition(elseTransition); // Assuming setter exists
        }
        conditionalNode.addAnnotations(annotations); // Assuming method exists

        return conditionalNode;
    }


     @Override
     public AstNode visitTransitionSpec(SSoTParser.TransitionSpecContext ctx) {
         System.out.println("        Visiting TransitionSpec");
         String targetStateName = ctx.targetState().getText(); // e.g., "StateName", ".history"

         String condition = null; // Guard name from GUARD option
         List<String> actions = new ArrayList<>();
         List<AnnotationNode> annotations = new ArrayList<>(); // Annotations inside {}

         if (ctx.transitionOptions() != null) {
              if (ctx.transitionOptions().annotation() != null) {
                   annotations.addAll(extractAnnotations(ctx.transitionOptions().annotation()));
              }
              if (ctx.transitionOptions().transitionOption() != null) {
                 for (SSoTParser.TransitionOptionContext optCtx : ctx.transitionOptions().transitionOption()) {
                     if (optCtx.annotation() != null) {
                          annotations.add((AnnotationNode) visitAnnotation(optCtx.annotation()));
                     } else if (optCtx.GUARD() != null) {
                         // GUARD condition for the transition itself
                         condition = optCtx.guardReferenceList().getText(); // Includes potential (not)
                     } else if (optCtx.ACTION() != null) {
                         actions.addAll(extractActionReferences(optCtx.actionReferenceList()));
                     }
                     // TODO: Handle ALLOWED_ACTORS
                 }
              }
         }

         // ID is not directly available here.
         // Source state is implicit. Event is handled by EventHandlerNode or context (AFTER/ALWAYS/IF).
         // Create TransitionNode - need to adapt constructor or use setters
         // Let's assume constructor takes: id, source, event, target, guard, actions, annotations
         return new TransitionNode(
                 Optional.empty(), // No ID parsed here
                 null, // Source state name - maybe add later during validation/linking?
                 null, // Event name - context dependent
                 targetStateName,
                 Optional.ofNullable(condition),
                 actions,
                 annotations);
     }

     @Override
     public AstNode visitInvokeState(SSoTParser.InvokeStateContext ctx) {
          System.out.println("      Visiting InvokeState");
         Optional<Long> id = extractId(ctx.annotation());
         List<AnnotationNode> annotations = extractAnnotations(ctx.annotation());
         String invokeDefinitionRef = null;
         Map<String, ValueNode> inputMapping = new HashMap<>(); // Use ValueNode from ast.values
         InvokeCompletionHandler onDone = null; // Use InvokeCompletionHandler from ast.handlers
         InvokeCompletionHandler onError = null;

         if (ctx.invokeStateBody() != null) {
             for (SSoTParser.InvokeStateOptionContext optCtx : ctx.invokeStateBody().invokeStateOption()) {
                 if (optCtx.annotation() != null) {
                     // Annotations inside the {} (apply to the invoke itself or specific option?)
                     // Let's assume they apply to the invoke overall for now.
                     annotations.add((AnnotationNode) visitAnnotation(optCtx.annotation()));
                 } else if (optCtx.SRC() != null) {
                     invokeDefinitionRef = optCtx.referenceValue().getText();
                 } else if (optCtx.INPUT() != null && optCtx.LBRACE() != null) { // Check for INPUT block
                     System.out.println("        Parsing invoke input mapping...");
                     if (optCtx.attributeAssignment() != null) {
                          for (SSoTParser.AttributeAssignmentContext assignCtx : optCtx.attributeAssignment()) {
                              String key = assignCtx.ID().getText();
                              AstNode valueAst = visitValue(assignCtx.value());
                              if (valueAst instanceof ValueNode) {
                                  inputMapping.put(key, (ValueNode) valueAst);
                                  System.out.println("          Mapped input: " + key + " = " + valueAst);
                              } else {
                                   System.err.println("Warning: Could not parse value for invoke input key: " + key + ". Raw text: " + assignCtx.value().getText());
                                   // Store raw text or a specific ErrorValueNode?
                                   inputMapping.put(key, new StringValueNode(assignCtx.value().getText())); // Fallback to StringValueNode
                              }
                          }
                     } else {
                         System.out.println("        Invoke INPUT block is empty.");
                     }
                 } else if (optCtx.ON_DONE() != null) {
                     onDone = parseInvokeCompletionHandler(optCtx.invokeCompletion());
                 } else if (optCtx.ON_ERROR() != null) {
                     onError = parseInvokeCompletionHandler(optCtx.invokeCompletion());
                 }
             }
         }

         if (invokeDefinitionRef == null) {
              System.err.println("Error: Invoke in state is missing 'src' reference to invokes definition.");
              // Consider throwing an exception or returning a specific error node
              return null; // Cannot create node without src reference
         }

         // Create InvokeStateNode using the imported classes
         return new InvokeStateNode(id, invokeDefinitionRef, inputMapping, Optional.ofNullable(onDone), Optional.ofNullable(onError), annotations);
     }

     // Helper to parse onDone/onError content - Ensure it uses imported InvokeCompletionHandler
     private InvokeCompletionHandler parseInvokeCompletionHandler(SSoTParser.InvokeCompletionContext ctx) {
         List<String> actions = new ArrayList<>();
         Optional<TransitionNode> transition = Optional.empty();
         List<AnnotationNode> annotations = new ArrayList<>(); // Assume annotations can be on the handler itself

         if (ctx.transitionSpec() != null) {
             AstNode transitionNode = visitTransitionSpec(ctx.transitionSpec());
             if (transitionNode instanceof TransitionNode) {
                  transition = Optional.of((TransitionNode) transitionNode);
                  // Add annotations from the transition spec itself
                  annotations.addAll(((TransitionNode) transitionNode).getAnnotations());
             }
         } else if (ctx.actionReferenceList() != null) {
             actions = extractActionReferences(ctx.actionReferenceList());
             // Do actions lists have annotations in the grammar?
         }

         // Ensure InvokeCompletionHandler exists in ast.handlers and has this constructor
         return new InvokeCompletionHandler(actions, transition, annotations);
     }


     @Override
     public AstNode visitHistoryStateDefinition(SSoTParser.HistoryStateDefinitionContext ctx) {
          System.out.println("      Visiting HistoryStateDefinition");
         HistoryStateType type = HistoryStateType.SHALLOW; // Default
         if (ctx.DEEP() != null) {
             type = HistoryStateType.DEEP;
         }

         List<EventHandlerNode> eventHandlers = new ArrayList<>();
         List<AnnotationNode> annotations = new ArrayList<>(); // Annotations on HISTORY line?
         // TODO: Extract annotations if grammar allows them on history definition

         // Parse history state body if present (might contain default transitions, etc.)
         if (ctx.stateBody() != null) {
              System.out.println("        Parsing history state body...");
               for (SSoTParser.StateBodyItemContext itemCtx : ctx.stateBody().stateBodyItem()) {
                    // Handle relevant items for history state, e.g., transitions
                    if (itemCtx.onTransition() != null) {
                         AstNode handlerNode = visitOnTransition(itemCtx.onTransition());
                         if (handlerNode instanceof EventHandlerNode) {
                             eventHandlers.add((EventHandlerNode) handlerNode);
                             System.out.println("          Found ON handler in history state for: " + ((EventHandlerNode) handlerNode).getEventRefs());
                         }
                    } else if (itemCtx.annotation() != null) {
                         // Annotations inside history body?
                         annotations.add((AnnotationNode)visitAnnotation(itemCtx.annotation()));
                    }
                    // Add handling for other relevant items if needed (e.g., default transition)
               }
         }

         // Create HistoryStateNode
         // Ensure HistoryStateNode class exists and accepts these parameters
         return new HistoryStateNode(type, eventHandlers, annotations);
     }


    // --- Type Name Parsing ---
    @Override
    public AstNode visitTypeName(SSoTParser.TypeNameContext ctx) {
        // Handles primitive, optional<T>, list<T>, map<K,V>, ID (custom type)
        if (ctx.primitiveTypeName() != null) {
            return new TypeNode(BaseType.fromString(ctx.primitiveTypeName().getText()));
        } else if (ctx.OPTIONAL() != null) {
            TypeNode innerType = (TypeNode) visitTypeName(ctx.typeName(0));
            return new TypeNode(BaseType.OPTIONAL, innerType);
        } else if (ctx.LIST() != null) {
             TypeNode valueType = (TypeNode) visitTypeName(ctx.typeName(0));
            return new TypeNode(BaseType.LIST, valueType);
        } else if (ctx.MAP() != null) {
             TypeNode keyType = (TypeNode) visitTypeName(ctx.typeName(0));
             TypeNode valueType = (TypeNode) visitTypeName(ctx.typeName(1));
             return new TypeNode(BaseType.MAP, keyType, valueType);
        } else if (ctx.referenceValue() != null) { // Custom type
             String typeName = ctx.referenceValue().getText();
             // Need to resolve this later during validation phase
            return new TypeNode(BaseType.CUSTOM, typeName);
        }
        // Should not happen
        System.err.println("Error: Unrecognized typeName structure: " + ctx.getText());
        return new TypeNode(BaseType.CUSTOM, "ERROR_UNKNOWN_TYPE");
    }

    // --- Annotations ---
    @Override
    public AstNode visitAnnotation(SSoTParser.AnnotationContext ctx) {
        if (ctx.idAnnotation() != null) {
            String hexId = ctx.idAnnotation().HEX_ID().getText();
            try {
                // Remove "0x" prefix and parse as Long
                long idValue = Long.parseUnsignedLong(hexId.substring(2), 16);
                return new AnnotationNode("@id", idValue); // Store ID as value
            } catch (NumberFormatException e) {
                System.err.println("Error parsing annotation ID: " + hexId + " - " + e.getMessage());
                return new AnnotationNode("@id", -1L); // Indicate error
            }
        } else if (ctx.namedAnnotation() != null) {
            String name = ctx.namedAnnotation().DOLLAR().getText() + ctx.namedAnnotation().ID().getText(); // e.g., $description
            AstNode valueNode = visitValue(ctx.namedAnnotation().value()); // value() rule handles string, bool, int, etc.
            Object value = null;
            if (valueNode instanceof ValueNode) { // Assuming visitValue returns ValueNode
                 value = ((ValueNode<?>) valueNode).getValue();
            } else {
                 System.err.println("Warning: Could not extract value for annotation: " + name);
                 value = ctx.namedAnnotation().value().getText(); // Store raw text as fallback
            }

            return new AnnotationNode(name, value);
        }
        return null; // Should not happen
    }

    // --- Value Parsing ---
    @Override
    public AstNode visitValue(SSoTParser.ValueContext ctx) {
        if (ctx.STRING() != null) {
            return new StringValueNode(stripQuotes(ctx.STRING().getText()));
        } else if (ctx.INT() != null) {
            try {
                // Prefer Long for integer literals to avoid overflow issues
                return new IntValueNode(Integer.parseInt(ctx.INT().getText())); // Changed back to int based on IntValueNode definition
            } catch (NumberFormatException e) {
                System.err.println("Warning: Could not parse INT literal: " + ctx.INT().getText());
                return new StringValueNode(ctx.INT().getText()); // Fallback to StringValueNode? Or specific ErrorValueNode?
            }
        } else if (ctx.FLOAT() != null) {
             try {
                // Prefer Double for floating-point literals
                return new FloatValueNode(Double.parseDouble(ctx.FLOAT().getText()));
            } catch (NumberFormatException e) {
                System.err.println("Warning: Could not parse FLOAT literal: " + ctx.FLOAT().getText());
                return new StringValueNode(ctx.FLOAT().getText()); // Fallback
            }
        } else if (ctx.BOOLEAN() != null) {
            return new BooleanValueNode(Boolean.parseBoolean(ctx.BOOLEAN().getText()));
        } else if (ctx.referenceValue() != null) {
            // This represents a reference to something else (context var, enum variant, etc.)
            return new RefValueNode(ctx.referenceValue().getText());
        } else if (ctx.arrayValue() != null) {
             System.err.println("Warning: Array values not fully implemented in visitValue yet.");
             // TODO: Implement array value parsing properly, returning a ListValueNode or similar
             return new StringValueNode(ctx.arrayValue().getText()); // Placeholder fallback
             /*
             List<ValueNode> elements = new ArrayList<>();
             if (ctx.arrayValue().value() != null) {
                 for (SSoTParser.ValueContext valCtx : ctx.arrayValue().value()) {
                      AstNode elementNode = visitValue(valCtx);
                      if (elementNode instanceof ValueNode) {
                          elements.add((ValueNode)elementNode);
                      } else {
                           // Handle error: Non-value node inside array?
                      }
                 }
             }
             return new ArrayValueNode(elements); // Assuming ArrayValueNode exists
             */
        } else if (ctx.objectValue() != null) {
             System.err.println("Warning: Object values not fully implemented in visitValue yet.");
              // TODO: Implement object value parsing, returning an ObjectValueNode or similar
             return new StringValueNode(ctx.objectValue().getText()); // Placeholder fallback
             /*
             Map<String, ValueNode> fields = new HashMap<>();
              if (ctx.objectValue().attributeAssignment() != null) {
                  for (SSoTParser.AttributeAssignmentContext assignCtx : ctx.objectValue().attributeAssignment()) {
                      String key = assignCtx.ID().getText();
                      AstNode valueAst = visitValue(assignCtx.value());
                      if (valueAst instanceof ValueNode) {
                          fields.put(key, (ValueNode) valueAst);
                      } else {
                          // Handle error
                      }
                  }
              }
             return new ObjectValueNode(fields); // Assuming ObjectValueNode exists
             */
        }
        System.err.println("Error: Unrecognized value structure: " + ctx.getText());
        return new StringValueNode("ERROR_UNKNOWN_VALUE"); // Error fallback
    }


    // --- Helper Methods ---

    private String stripQuotes(String text) {
        if (text != null && text.length() >= 2 && text.startsWith("\"") && text.endsWith("\"")) {
            // Basic unescaping might be needed here for \\, \", etc.
            return text.substring(1, text.length() - 1)
                       .replace("\\\\", "\\")
                       .replace("\\\"", "\""); // Add more escapes as needed
        }
        return text;
    }

    private Optional<Long> extractId(List<SSoTParser.AnnotationContext> annotations) {
        if (annotations == null) return Optional.empty();
        for (SSoTParser.AnnotationContext annoCtx : annotations) {
            if (annoCtx.idAnnotation() != null) {
                String hexId = annoCtx.idAnnotation().HEX_ID().getText();
                try {
                    return Optional.of(Long.parseUnsignedLong(hexId.substring(2), 16));
                } catch (NumberFormatException e) {
                     System.err.println("Error parsing annotation ID: " + hexId + " - " + e.getMessage());
                    return Optional.of(-1L); // Indicate error
                }
            }
        }
        return Optional.empty();
    }

    private List<AnnotationNode> extractAnnotations(List<SSoTParser.AnnotationContext> annotationContexts) {
        if (annotationContexts == null) return new ArrayList<>();
        return annotationContexts.stream()
                .map(annoCtx -> (AnnotationNode) visitAnnotation(annoCtx))
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());
    }

     private Map<String, Object> mapAnnotations(List<AnnotationNode> annotations) {
         if (annotations == null) return Collections.emptyMap();
         Map<String, Object> map = new HashMap<>();
         for (AnnotationNode node : annotations) {
              // Handle potential duplicate keys? Last one wins for now.
             map.put(node.name, node.value);
         }
         return map;
     }

     // Helper to combine annotation lists
     private List<AnnotationNode> allAnnotations(List<AnnotationNode> list1, List<AnnotationNode> list2) {
         List<AnnotationNode> combined = new ArrayList<>();
         if (list1 != null) combined.addAll(list1);
         if (list2 != null) combined.addAll(list2);
         return combined;
     }

      // Helper to find a specific annotation value
     private Optional<String> findAnnotationValue(List<AnnotationNode> annotations, String name) {
         if (annotations == null || name == null) return Optional.empty();
         String targetName = name.startsWith("$") ? name : "$" + name; // Ensure $ prefix
         for (AnnotationNode node : annotations) {
             if (targetName.equals(node.name) && node.value instanceof String) {
                 return Optional.of((String) node.value);
             }
         }
         return Optional.empty();
     }

     // Dummy Node for blocks, replace if a better structure is decided
     static class BlockContainerNode implements AstNode {
         final String blockType;
         final List<AstNode> children;
         BlockContainerNode(String type, List<AstNode> children) {
             this.blockType = type;
             this.children = children != null ? children : Collections.emptyList();
         }
         public List<AstNode> getChildren() { return children; }
         @Override public <T> T accept(NodeVisitor<T> visitor) { return null; /* Container node not visited directly */ }
         @Override public String toString() { return "BlockContainer[" + blockType + ", children=" + children.size() + "]"; }
         @Override public Map<String, Object> getAnnotations() { return Collections.emptyMap(); } // Blocks themselves might have annotations
         @Override public Optional<Long> getId() { return Optional.empty(); } // Blocks themselves might have ID
     }

     // Dummy node for values, replace if needed
     // Making it generic for type safety might be better
     static class ValueNode<V> implements AstNode {
         final V value;
         ValueNode(V value) { this.value = value; }
         public V getValue() { return value; }
         // Provide raw value for cases like invoke src where we just need the string
         public String getRawValue() { return value != null ? value.toString() : null; }
         @Override public <T> T accept(NodeVisitor<T> visitor) { return null; /* Value node not visited directly */ }
         @Override public String toString() { return "Value(" + value + ")"; }
          @Override public Map<String, Object> getAnnotations() { return Collections.emptyMap(); }
         @Override public Optional<Long> getId() { return Optional.empty(); }
     }

     // Need definition for InvokeCompletionHandler
     static class InvokeCompletionHandler {
         final TransitionNode transition;
         final List<String> actions;

         private InvokeCompletionHandler(TransitionNode transition, List<String> actions) {
             this.transition = transition;
             this.actions = actions;
         }

         static InvokeCompletionHandler fromTransition(TransitionNode transition) {
             return new InvokeCompletionHandler(transition, null);
         }

         static InvokeCompletionHandler fromActions(List<String> actions) {
             return new InvokeCompletionHandler(null, actions != null ? Collections.unmodifiableList(new ArrayList<>(actions)) : Collections.emptyList());
         }

         public boolean isTransition() { return transition != null; }
         public boolean isActions() { return actions != null; }
         public Optional<TransitionNode> getTransition() { return Optional.ofNullable(transition); }
         public List<String> getActions() { return actions != null ? actions : Collections.emptyList(); }

          @Override
         public String toString() {
             if (isTransition()) {
                 return "InvokeCompletion(transition=" + transition + ")";
             } else if (isActions()) {
                 return "InvokeCompletion(actions=" + actions + ")";
             } else {
                 return "InvokeCompletion(empty)";
             }
         }
     }

     // Add visit methods for ServiceDefinition, InterfaceDefinition, MethodDefinition, ParameterNode,
     // ActorNode, CommunicationNode, ProtocolNode, ChannelNode, EventNode,
     // DeploymentConfigNode, DependencyNode etc. as needed.

}