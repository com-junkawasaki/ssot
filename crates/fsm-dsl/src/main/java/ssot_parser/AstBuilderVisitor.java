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

/**
 * Visits the ANTLR Parse Tree and builds the Abstract Syntax Tree (AST).
 * This class extends the generated SSoTBaseVisitor and overrides methods
 * for specific grammar rules to create corresponding AST nodes.
 */
// Make sure AstBuilderVisitor<T> matches SSoTBaseVisitor<T> (AstNode seems correct)
public class AstBuilderVisitor extends SSoTBaseVisitor<Object> {

    // Keep track of the current state name for transitions
    private String currentStateName = null;

    // Record to hold results from processing a list of annotations
    private record ProcessedAnnotations(Optional<Long> id, Map<String, Object> annotationMap) {}

    // Record to hold results from visiting statesDefinition
    private record StatesInfo(List<StateNode> states, Optional<String> initialStatename) {}

    @Override
    public Object visitFile(SSoTParser.FileContext ctx) { // Return type changed to Object
        System.out.println("Visiting File node...");

        List<AstNode> typeDefs = new ArrayList<>();
        List<AstNode> serviceDefs = new ArrayList<>();
        List<AstNode> machineDefs = new ArrayList<>();
        List<AstNode> actorDefs = new ArrayList<>(); // Added
        List<AstNode> communicationDefs = new ArrayList<>(); // Added
        // TODO: Handle imports, fileId, annotations

        // Iterate through definition blocks based on grammar: definitionBlock*
        if (ctx.definitionBlock() != null) {
            for (SSoTParser.DefinitionBlockContext blockCtx : ctx.definitionBlock()) {
                if (blockCtx.typesBlock() != null) {
                    // Use helper method, add results to list
                    typeDefs.addAll(visitTypesBlockHelper(blockCtx.typesBlock()));
                } else if (blockCtx.servicesBlock() != null) {
                    serviceDefs.addAll(visitServicesBlockHelper(blockCtx.servicesBlock()));
                } else if (blockCtx.machinesBlock() != null) {
                    machineDefs.addAll(visitMachinesBlockHelper(blockCtx.machinesBlock()));
                 } else if (blockCtx.actorsBlock() != null) {
                     actorDefs.addAll(visitActorsBlockHelper(blockCtx.actorsBlock()));
                 } else if (blockCtx.communicationBlock() != null) {
                     communicationDefs.addAll(visitCommunicationBlockHelper(blockCtx.communicationBlock()));
                }
                // TODO: Add other block types (deployment, dependencies)
            }
        }

        // Create SsotRoot with collected lists (adjust constructor if needed)
        // Assuming SsotRoot needs more lists now. Let's keep it simple for now.
        // TODO: Update SsotRoot constructor to accept all definition types.
        // TODO: Process file-level annotations
        Map<String, Object> fileAnnotations = new HashMap<>(); // Placeholder for file annotations
        return new SsotRoot(typeDefs, serviceDefs, machineDefs, fileAnnotations); // Add annotations map
    }

    // --- Helper methods for visiting blocks (NOT overriding BaseVisitor) ---

    // These helpers collect specific node types (AstNode), but visit() returns Object.
    // The return type should be List<AstNode> as they filter for specific types.

    // Helper for Types Block
    private List<AstNode> visitTypesBlockHelper(SSoTParser.TypesBlockContext ctx) {
        System.out.println("Visiting TypesBlock node...");
        List<AstNode> typeDefs = new ArrayList<>();
        if (ctx.typeDefinition() != null) {
            for (SSoTParser.TypeDefinitionContext typeCtx : ctx.typeDefinition()) {
                Object result = visit(typeCtx); // visit() returns Object
                if (result instanceof AstNode) { // Cast to AstNode before adding
                    typeDefs.add((AstNode) result);
                }
            }
        }
        return typeDefs;
    }

    // Helper for Machines Block
     private List<AstNode> visitMachinesBlockHelper(SSoTParser.MachinesBlockContext ctx) {
         System.out.println("Visiting MachinesBlock node...");
         List<AstNode> machineDefs = new ArrayList<>();
         if (ctx.machineDefinition() != null) {
             for (SSoTParser.MachineDefinitionContext machineCtx : ctx.machineDefinition()) {
                 Object result = visit(machineCtx); // visit() returns Object
                 // Ensure the result is actually a MachineNode before casting/adding
                 if (result instanceof MachineNode) {
                     machineDefs.add((MachineNode) result);
                 } else if (result != null) {
                     // Log error if visitMachineDefinition returns something unexpected but not null
                      System.err.println("Warning: visitMachineDefinition did not return a MachineNode. Got: " + result.getClass().getName());
                 }
                 // Null result might indicate an error during visit, already logged
             }
         }
         return machineDefs;
     }

     // Helper for Services Block
      private List<AstNode> visitServicesBlockHelper(SSoTParser.ServicesBlockContext ctx) {
          System.out.println("Visiting ServicesBlock node...");
          List<AstNode> serviceDefs = new ArrayList<>();
          if (ctx.serviceElement() != null) { // Grammar uses serviceElement*
              for (SSoTParser.ServiceElementContext elementCtx : ctx.serviceElement()) {
                  Object result = visit(elementCtx); // visit() returns Object
                  if (result instanceof AstNode) { // Cast to AstNode
                      serviceDefs.add((AstNode) result);
                  }
              }
          }
          return serviceDefs;
      }

      // Helper for Actors Block (New)
       private List<AstNode> visitActorsBlockHelper(SSoTParser.ActorsBlockContext ctx) {
           System.out.println("Visiting ActorsBlock node...");
           List<AstNode> actorDefs = new ArrayList<>();
           if (ctx.actorDefinition() != null) {
                for(SSoTParser.ActorDefinitionContext actorCtx : ctx.actorDefinition()) {
                    Object result = visit(actorCtx); // visit() returns Object
                    if (result instanceof AstNode) { // Cast to AstNode
                        actorDefs.add((AstNode) result);
                    }
                }
           }
           return actorDefs;
       }

       // Helper for Communication Block (New)
       private List<AstNode> visitCommunicationBlockHelper(SSoTParser.CommunicationBlockContext ctx) {
            System.out.println("Visiting CommunicationBlock node...");
            List<AstNode> commDefs = new ArrayList<>();
            if (ctx.communicationDefinition() != null) {
                for(SSoTParser.CommunicationDefinitionContext commCtx : ctx.communicationDefinition()) {
                     Object result = visit(commCtx); // visit() returns Object
                     if (result instanceof AstNode) { // Cast to AstNode
                         commDefs.add((AstNode) result);
                     }
                }
            }
            return commDefs;
       }


    // --- Annotation Processing Logic ---

    // Helper method to process a list of annotation contexts
    private ProcessedAnnotations processAnnotations(List<AnnotationContext> annotationCtxs) {
        // Restore original logic
        Optional<Long> id = Optional.empty();
        Map<String, Object> annotationMap = new HashMap<>();

        if (annotationCtxs != null) {
            for (AnnotationContext ctx : annotationCtxs) {
                Object result = visitAnnotation(ctx); // Returns Long for @id, or Map.Entry<String, Object> for $name

                if (result instanceof Long) {
                    if (id.isPresent()) {
                        System.err.println("Warning: Duplicate @id annotation found. Ignoring subsequent IDs. First ID was: " + id.get());
                    } else {
                        id = Optional.of((Long) result);
                    }
                } else if (result instanceof Map.Entry) {
                    try {
                         @SuppressWarnings("unchecked")
                         Map.Entry<String, Object> entry = (Map.Entry<String, Object>) result;
                         if (annotationMap.containsKey(entry.getKey())) {
                             // Handle duplicate annotation names if necessary (e.g., merge lists, overwrite, error)
                             System.err.println("Warning: Duplicate annotation name found: $" + entry.getKey() + ". Overwriting previous value.");
                         }
                         annotationMap.put(entry.getKey(), entry.getValue());
                     } catch(ClassCastException e){
                          System.err.println("Warning: visitAnnotation for $name did not return Map.Entry<String, Object>");
                     }
                }
                // Ignore null results from visitAnnotation (e.g., parse errors)
            }
        }
        return new ProcessedAnnotations(id, annotationMap);
    }

    // Visitor for a single annotation rule: handles @id, $name(value), $flag;
    // Returns Long for @id, or Map.Entry<String, Object> for $name annotations.
    public Object visitAnnotation(SSoTParser.AnnotationContext ctx) { // Removed @Override
         // Restore original implementation
         if (ctx.AT() != null && ctx.ID() != null && ctx.INT() != null) {
             // @id(integer) annotation
             try {
                 long idValue = Long.parseLong(ctx.INT().getText());
                 // We return the Long value directly. processAnnotations will wrap it in Optional.
                 return idValue;
             } catch (NumberFormatException e) {
                 System.err.println("Warning: Could not parse @id value: " + ctx.INT().getText());
                 return null; // Indicate error
             }
         } else if (ctx.DOLLAR() != null && ctx.annotationName() != null) {
             String name = ctx.annotationName().getText();
             Object value = null;

             if (ctx.LPAREN() != null && ctx.RPAREN() != null) {
                 // $name(value) annotation
                 if (ctx.annotationValue() != null) {
                     value = visitAnnotationValue(ctx.annotationValue());
                 } else {
                     // $name() - empty value, might represent true or an empty structure depending on convention
                     value = true; // Defaulting to true for now, could be null or empty map/list
                      System.out.println("Info: Annotation $" + name + " has empty parentheses.");
                 }
             } else if (ctx.SEMI() != null) {
                 // $flag; annotation - Treat as boolean true
                 value = true;
             } else {
                  System.err.println("Warning: Malformed $ annotation rule: " + ctx.getText());
                  return null;
             }

             // Return as a Map.Entry
             return Map.entry(name, value);

         } else {
             System.err.println("Warning: Unrecognized annotation format: " + ctx.getText());
             return null; // Indicate error or unrecognized format
         }
    }

    // Add visitAnnotationName - Although simple, good practice to have it.
    public Object visitAnnotationName(SSoTParser.AnnotationNameContext ctx) { // Removed @Override
        // Restore original implementation
        // This visitor might not be strictly necessary if we just use getText(),
        // but useful if we needed to validate the name against allowed keywords later.
        return ctx.getText(); // Just return the name string
    }

    // --- Implementations for individual definition visitors (Overriding BaseVisitor where appropriate) ---

    @Override
    public Object visitTypeDefinition(SSoTParser.TypeDefinitionContext ctx) { // Return Object
        // This method IS useful if TypeDefinition has alternatives like struct | enum
        if (ctx.structDefinition() != null) {
            return visitStructDefinition(ctx.structDefinition());
        } else if (ctx.enumDefinition() != null) {
            return visitEnumDefinition(ctx.enumDefinition());
        }
        System.err.println("Warning: Unsupported type definition encountered: " + ctx.getText());
        return null;
    }

    @Override
    public Object visitStructDefinition(SSoTParser.StructDefinitionContext ctx) { // Return Object
        // Process annotations first
        ProcessedAnnotations processed = processAnnotations(ctx.annotation()); // Pass the list of annotation contexts

        // Extract name (handle potential list access based on previous findings)
        String name = "UNKNOWN_STRUCT";
        if (ctx.ID() != null) { // Assuming ID() returns TerminalNode based on grammar
             name = ctx.ID().getText();
        } else {
             System.err.println("Warning: No ID found for struct definition: " + ctx.getText());
        }
        System.out.println("Visiting StructDefinition: " + name);

        List<FieldNode> fields = new ArrayList<>();
        if (ctx.fieldDefinition() != null) {
            for (SSoTParser.FieldDefinitionContext fieldCtx : ctx.fieldDefinition()) {
                Object fieldResult = visitFieldDefinition(fieldCtx);
                if (fieldResult instanceof FieldNode) { // Check if it's a FieldNode
                    fields.add((FieldNode) fieldResult);
                }
            }
        }
        // Pass processed ID and annotations map to constructor
        return new TypeDefNode(processed.id(), name, fields, processed.annotationMap());
    }

    @Override
    public Object visitFieldDefinition(SSoTParser.FieldDefinitionContext ctx) { // Return Object
         // Grammar: ID COLON typeExpr annotation* (LBRACE annotation* RBRACE)? SEMI
         String name = ctx.ID().getText();
         String type = ctx.typeExpr().getText();
         System.out.println("Visiting FieldDefinition: " + name + " (" + type + ")");

         // Process annotations associated with the field
         ProcessedAnnotations processed = processAnnotations(ctx.annotation());

        // TODO: Handle annotations inside braces: (LBRACE annotation* RBRACE)?

         // Pass processed ID and annotations map to constructor
         return new FieldNode(processed.id(), name, type, processed.annotationMap());
     }

     // Assuming visitEnumDefinition is needed
     @Override
     public Object visitEnumDefinition(SSoTParser.EnumDefinitionContext ctx) { // Return Object
        System.out.println("Visiting EnumDefinition: " + ctx.ID().getText());
        // TODO: Implement EnumNode creation (needs EnumNode class)
        // Extract name ctx.ID()
        // Iterate ctx.enumVariant()
         System.err.println("Warning: visitEnumDefinition not fully implemented.");
        return null;
     }

    // --- Machine related visitors ---

    @Override
    public Object visitMachineDefinition(SSoTParser.MachineDefinitionContext ctx) { // Return Object, should be MachineNode
        String name = ctx.IDENTIFIER().getText();
        System.out.println("Visiting MachineDefinition: " + name);

        // Process annotations specific to this machine definition
        ProcessedAnnotations processed = processAnnotations(ctx.annotation());
        List<AnnotationNode> annotations = new ArrayList<>(); // Placeholder for actual AnnotationNode objects
        // TODO: Convert processed.annotationMap() and potentially processed.id() into AnnotationNode list

        // Initialize placeholders for machine elements
        Optional<AstNode> contextNode = Optional.empty(); // Expecting ContextNode later
        List<AstNode> actions = new ArrayList<>();     // Expecting List<ActionNode> later
        List<AstNode> guards = new ArrayList<>();      // Expecting List<GuardNode> later
        List<AstNode> invokes = new ArrayList<>();     // Expecting List<InvokeNode> later
        List<AstNode> states = new ArrayList<>();      // Expecting List<StateNode> or Map<String, StateNode> later
        Optional<String> initialState = Optional.empty();
        List<AstNode> transitions = new ArrayList<>(); // Expecting List<TransitionNode> later

        // Iterate through the machine body elements
        if (ctx.machineBodyElement() != null) {
            for (SSoTParser.MachineBodyElementContext elementCtx : ctx.machineBodyElement()) {
                // Use a helper or direct visits to populate the lists/optionals above
                 Object elementResult = visit(elementCtx); // Visit the specific element rule

                 System.out.println("Processing MachineBodyElement, result type: " + (elementResult != null ? elementResult.getClass().getName() : "null"));

                 // Handle the result based on its type
                 if (elementResult instanceof ContextNode) {
                    if (contextNode.isPresent()) {
                        // Handle multiple context blocks if necessary (e.g., error or merge)
                        System.err.println("Warning: Multiple context definitions found in machine '" + name + "'. Using the last one defined.");
                    }
                    contextNode = Optional.of((ContextNode) elementResult);
                 } else if (elementResult instanceof List) {
                    // Assuming actions, guards, invokes, states might return Lists of nodes
                    // Check the type of nodes in the list based on the visitor that produced it.
                    try {
                        @SuppressWarnings("unchecked")
                        List<?> nodes = (List<?>) elementResult;
                        if (!nodes.isEmpty()) {
                             Object firstNode = nodes.get(0);
                             // Check node type and assign to the correct list in MachineNode
                             if (firstNode instanceof ActionNode) {
                                 // Cast the whole list to List<ActionNode> and add all
                                 @SuppressWarnings("unchecked")
                                 List<ActionNode> actionResultList = (List<ActionNode>) nodes;
                                 actions.addAll(actionResultList);
                             } else if (firstNode instanceof StateNode) { // Assuming StateNode exists
                                 // Cast the whole list to List<StateNode> when implemented
                                  @SuppressWarnings("unchecked")
                                  List<AstNode> stateResultList = (List<AstNode>) nodes; // Keep as AstNode for now
                                  states.addAll(stateResultList); // Assuming visitStatesDefinition returns List<StateNode>
                             }
                             // Check for GuardNode
                             else if (firstNode instanceof GuardNode) {
                                @SuppressWarnings("unchecked")
                                List<GuardNode> guardResultList = (List<GuardNode>) nodes;
                                guards.addAll(guardResultList);
                             }
                             // Check for InvokeNode
                             else if (firstNode instanceof InvokeNode) {
                                @SuppressWarnings("unchecked")
                                List<InvokeNode> invokeResultList = (List<InvokeNode>) nodes;
                                invokes.addAll(invokeResultList);
                             }
                             else {
                                System.err.println("Warning: Unexpected node type in list from MachineBodyElement visitor: " + firstNode.getClass().getName());
                             }
                         }
                     } catch (ClassCastException e) {
                         System.err.println("Warning: MachineBodyElement visitor returned a List, but it doesn't contain expected node types: " + e.getMessage());
                     }
                 } else if (elementResult instanceof StatesInfo) {
                    // Handle the result from visitStatesDefinition
                    StatesInfo statesResult = (StatesInfo) elementResult;
                    states.addAll(statesResult.states()); // Add all StateNode objects
                    if (initialState.isPresent() && statesResult.initialStatename().isPresent()) {
                         // Handle case where initial state might be defined multiple times (e.g., in multiple states blocks)
                         System.err.println("Warning: Initial state potentially redefined. Using value from last 'states' block: " + statesResult.initialStatename().get());
                     }
                    // Set the initial state name for the machine
                    if (statesResult.initialStatename().isPresent()) {
                        initialState = statesResult.initialStatename();
                    }

                    // Collect transitions from all states within this block
                    for (StateNode stateNode : statesResult.states()) {
                        transitions.addAll(stateNode.getTransitions());
                    }

                 }
                 // TODO: Add handling for other potential return types from visitMachineBodyElement
                 // (e.g., a dedicated StatesBlockNode containing states and initial state)
                 else if (elementResult != null){
                     System.err.println("Warning: Unhandled result type from MachineBodyElement visitor: " + elementResult.getClass().getName());
                 }

                 // Handle initial state marker if found within states definition or body element
                 // Handle transitions if defined within states or directly in the body
            }
        }

         // TODO: Extract initial state logic if defined separately or within states block.
         // The grammar for initial state might be part of stateDefinition or a top-level element.

        // Create and return the MachineNode
        // Using placeholders for unimplemented parts
        System.out.println("Creating MachineNode for: " + name);
        return new MachineNode(
            annotations,
            name,
            contextNode,
            actions,
            guards,
            invokes,
            states,
            initialState,
            transitions
        );
    }

    // Visitor for elements within the machine body (context, actions, states, etc.)
    // This might need to return different types depending on the element,
    // or a generic container, or the main visitMachineDefinition handles the results.
     @Override
     public Object visitMachineBodyElement(SSoTParser.MachineBodyElementContext ctx) {
         System.out.println("Visiting MachineBodyElement...");
         if (ctx.contextDefinition() != null) {
             return visitContextDefinition(ctx.contextDefinition());
         } else if (ctx.actionsDefinition() != null) {
             return visitActionsDefinition(ctx.actionsDefinition());
         } else if (ctx.guardsDefinition() != null) {
             return visitGuardsDefinition(ctx.guardsDefinition());
         } else if (ctx.invokesDefinition() != null) {
             return visitInvokesDefinition(ctx.invokesDefinition());
         } else if (ctx.statesDefinition() != null) {
             // This might return a list of StateNode or a specific StatesBlockNode
             return visitStatesDefinition(ctx.statesDefinition());
         }
         // TODO: Add other possible machine body elements if defined in the grammar
         System.err.println("Warning: Unsupported machine body element: " + ctx.getText());
         return null;
     }


     // Placeholder visitors for machine elements - These need implementation
     // They should return appropriate AST Node types (e.g., ContextNode, List<ActionNode>, etc.)

     public Object visitContextDefinition(SSoTParser.ContextDefinitionContext ctx) { // Removed @Override if not in BaseVisitor
        System.out.println("Visiting ContextDefinition...");
        // Assuming context block contains field definitions similar to struct
        List<FieldNode> variables = new ArrayList<>();
        if (ctx.fieldDefinition() != null) {
            for (SSoTParser.FieldDefinitionContext fieldCtx : ctx.fieldDefinition()) {
                Object fieldResult = visitFieldDefinition(fieldCtx);
                if (fieldResult instanceof FieldNode) {
                    variables.add((FieldNode) fieldResult);
                } else if (fieldResult != null) {
                     System.err.println("Warning: visitFieldDefinition inside context did not return a FieldNode. Got: " + fieldResult.getClass().getName());
                }
            }
        }
        // TODO: Process annotations specific to the context block itself if the grammar allows
        return new ContextNode(variables);
     }

     public Object visitActionsDefinition(SSoTParser.ActionsDefinitionContext ctx) { // Removed @Override if not in BaseVisitor
        System.out.println("Visiting ActionsDefinition...");
        List<ActionNode> actionNodes = new ArrayList<>(); // Changed type to List<ActionNode>
        if (ctx.actionDefinition() != null) {
            for (SSoTParser.ActionDefinitionContext actionCtx : ctx.actionDefinition()) {
                Object result = visitActionDefinition(actionCtx); // Should return ActionNode
                if (result instanceof ActionNode) { // Check if the result is ActionNode
                    actionNodes.add((ActionNode) result);
                } else if (result != null) {
                     System.err.println("Warning: visitActionDefinition did not return an ActionNode. Got: " + result.getClass().getName());
                }
            }
        }
        // Return the list of ActionNodes
        // System.err.println("Warning: visitActionsDefinition returning potentially incomplete list."); // Warning might not be needed anymore
        return actionNodes;
     }

      public Object visitGuardsDefinition(SSoTParser.GuardsDefinitionContext ctx) { // Removed @Override if not in BaseVisitor
         System.out.println("Visiting GuardsDefinition...");
         List<GuardNode> guardNodes = new ArrayList<>(); // Changed type to List<GuardNode>
         if (ctx.guardDefinition() != null) {
             for (SSoTParser.GuardDefinitionContext guardCtx : ctx.guardDefinition()) {
                 Object result = visitGuardDefinition(guardCtx); // Call the specific visitor
                 if (result instanceof GuardNode) {
                     guardNodes.add((GuardNode) result);
                 } else if (result != null) {
                     System.err.println("Warning: visitGuardDefinition did not return a GuardNode. Got: " + result.getClass().getName());
                 }
             }
         }
         // Return the list of GuardNodes
         return guardNodes;
      }

      public Object visitInvokesDefinition(SSoTParser.InvokesDefinitionContext ctx) { // Removed @Override if not in BaseVisitor
          System.out.println("Visiting InvokesDefinition...");
          List<InvokeNode> invokeNodes = new ArrayList<>(); // Changed type to List<InvokeNode>
          if (ctx.invokeDefinition() != null) {
              for (SSoTParser.InvokeDefinitionContext invokeCtx : ctx.invokeDefinition()) {
                  Object result = visitInvokeDefinition(invokeCtx); // Call the specific visitor
                  if (result instanceof InvokeNode) {
                      invokeNodes.add((InvokeNode) result);
                  } else if (result != null) {
                      System.err.println("Warning: visitInvokeDefinition did not return an InvokeNode. Got: " + result.getClass().getName());
                  }
              }
          }
          // Return the list of InvokeNodes
          return invokeNodes;
      }


     @Override
     public Object visitStatesDefinition(SSoTParser.StatesDefinitionContext ctx) { // Return Object, should be StatesInfo
         System.out.println("Visiting StatesDefinition...");
         List<StateNode> stateNodes = new ArrayList<>();
         Optional<String> initialStateName = Optional.empty();

         if (ctx.stateDefinition() != null) {
             for (SSoTParser.StateDefinitionContext stateCtx : ctx.stateDefinition()) {
                 Object result = visitStateDefinition(stateCtx); // Should return StateNode
                 if (result instanceof StateNode) {
                     StateNode stateNode = (StateNode) result;
                     stateNodes.add(stateNode);
                     // Check if this state is marked as initial
                     if (stateNode.isInitial()) {
                        if (initialStateName.isPresent()) {
                           // Handle multiple initial states - error or use last one?
                           System.err.println("Warning: Multiple initial states defined or marked. Using state: " + stateNode.getName() + " as initial, overwriting previous: " + initialStateName.get());
                        }
                        initialStateName = Optional.of(stateNode.getName());
                     }
                 } else if (result != null) {
                    System.err.println("Warning: visitStateDefinition did not return a StateNode. Got: " + result.getClass().getName());
                 }
             }
         }

         // TODO: If initial state is marked by a keyword *outside* stateDefinition (e.g., initial = ID;), handle it here.

         if (initialStateName.isEmpty() && !stateNodes.isEmpty()) {
            // Convention: If no initial state is explicitly marked, use the first state defined.
            initialStateName = Optional.of(stateNodes.get(0).getName());
            System.out.println("Info: No initial state explicitly marked. Using first defined state '" + initialStateName.get() + "' as initial.");
            // Note: We might need to update the isInitial flag on the actual StateNode object if desired.
            // stateNodes.get(0).setInitial(true); // Requires StateNode to be mutable or recreated
         }

         // Return the collected states and the determined initial state name
         return new StatesInfo(stateNodes, initialStateName);
     }

     @Override
     public Object visitStateDefinition(SSoTParser.StateDefinitionContext ctx) { // Return Object, should be StateNode
         // Grammar: stateName=ID annotation* LBRACE annotation* stateBodyElement* RBRACE
         String name = ctx.stateName.getText(); // Use label stateName
         System.out.println("Visiting StateDefinition: " + name);
         this.currentStateName = name; // Store current state name for transitions

         List<String> entryActions = new ArrayList<>(); // Store action names
         List<String> exitActions = new ArrayList<>();  // Store action names
         List<TransitionNode> transitions = new ArrayList<>();
         boolean isInitialState = false; // Default to false, logic needs grammar info

         // TODO: Check grammar for how initial state is marked (e.g., INITIAL keyword before ID? specific annotation?)
         // if (ctx.INITIAL() != null) { isInitialState = true; }
         // Or check annotations processed below

         // Process state-level annotations (e.g., @id, maybe $initial)
         ProcessedAnnotations stateAnnotations = processAnnotations(ctx.annotation());
         // Example check for a potential $initial annotation:
         // if (stateAnnotations.annotationMap().containsKey("initial") && Boolean.TRUE.equals(stateAnnotations.annotationMap().get("initial"))) {
         //     isInitialState = true;
         // }

         // Process annotations inside braces (if grammar allows, currently seems redundant with outer annotations)
         // List<AnnotationContext> innerAnnotations = ctx.innerAnnotations != null ? ctx.innerAnnotations : Collections.emptyList();
         // ProcessedAnnotations innerProcessed = processAnnotations(innerAnnotations);
         // Merge or handle innerProcessed.id() and innerProcessed.annotationMap() if needed

         // Iterate through state body elements
         if (ctx.stateBodyElement() != null) {
            for (SSoTParser.StateBodyElementContext bodyElement : ctx.stateBodyElement()) {
                if (bodyElement.onEntryExit() != null) {
                    // Grammar: onEntryExit : (ON_ENTRY | ON_EXIT) actionReference SEMI;
                    SSoTParser.OnEntryExitContext entryExitCtx = bodyElement.onEntryExit();
                    // Assuming actionReference directly gives the name
                    String actionName = entryExitCtx.actionReference().getText();
                    if (entryExitCtx.ON_ENTRY() != null) {
                        entryActions.add(actionName);
                    } else if (entryExitCtx.ON_EXIT() != null) {
                        exitActions.add(actionName);
                    }
                } else if (bodyElement.onTransition() != null) {
                     // Delegate to visitOnTransition, which now returns TransitionNode
                     Object transitionResult = visitOnTransition(bodyElement.onTransition());
                     if (transitionResult instanceof TransitionNode) {
                         transitions.add((TransitionNode) transitionResult);
                     } else if (transitionResult != null) {
                         System.err.println("Warning: visitOnTransition did not return a TransitionNode. Got: " + transitionResult.getClass().getName());
                     }
                }
                // TODO: Handle invokeState, afterTransition, nested statesDefinition, historyDefinition
            }
         }

         this.currentStateName = null; // Clear current state name after visiting

         // Create and return the StateNode
         return new StateNode(stateAnnotations.id(), name, stateAnnotations.annotationMap(),
                            entryActions, exitActions, transitions, isInitialState);
     }

     // This method corresponds to the 'onTransition' rule in the grammar
     @Override
     public Object visitOnTransition(SSoTParser.OnTransitionContext ctx) { // Return Object
         // Grammar: ON event=ID annotation* transitionSpec SEMI
         String event = ctx.event.getText(); // Use label 'event'
         System.out.println("Visiting OnTransition: on " + event);

         // Process the transitionSpec part
         if (ctx.transitionSpec() != null) {
             SSoTParser.TransitionSpecContext specCtx = ctx.transitionSpec();
             String targetState = specCtx.targetState().getText(); // Get target state text
             String condition = null; // Placeholder - needs parsing from specCtx.transitionOptions() -> GUARD
             Optional<String> action = Optional.empty(); // Placeholder - needs parsing from specCtx.transitionOptions() -> ACTION

             if (specCtx.transitionOptions() != null) {
                 // TODO: Implement parsing logic for transitionOptions to find ACTION and GUARD
                 // Example (simplified):
                 for (SSoTParser.TransitionOptionContext option : specCtx.transitionOptions().transitionOption()) {
                     if (option.ACTION() != null && option.actionReferenceList() != null) {
                         action = Optional.of(option.actionReferenceList().getText()); // Get action text
                     } else if (option.GUARD() != null && option.guardReferenceList() != null) {
                         condition = option.guardReferenceList().getText(); // Get condition text
                     }
                 }
             }

             // Process annotations for the transition itself
             ProcessedAnnotations transitionAnnotations = processAnnotations(ctx.annotation());

             // Constructor: TransitionNode(Optional<Long> id, String fromState, String toState, String event, Optional<String> condition, Optional<String> action)
             // Use the stored currentStateName as fromState
             String fromState = (this.currentStateName != null) ? this.currentStateName : "UNKNOWN_SOURCE";
             // Add transitionAnnotations map to constructor
             return new TransitionNode(Optional.empty(), fromState, targetState, event, Optional.ofNullable(condition), action, transitionAnnotations.annotationMap());

         } else {
             System.err.println("Warning: onTransition rule missing transitionSpec for event: " + event);
             return null;
         }
     }


     // This method handles the 'actionDefinition' rule inside an 'actions' block
     @Override
     public Object visitActionDefinition(SSoTParser.ActionDefinitionContext ctx) { // Return Object, should be ActionNode
        String name = "UNKNOWN_ACTION";
         // Check if ID exists (grammar might allow actions without explicit names? unlikely)
         if (ctx.ID() != null) { // Assuming ID() returns a single TerminalNode now
            name = ctx.ID().getText();
         } else {
            System.err.println("Warning: No ID found for action definition: " + ctx.getText());
         }
        System.out.println("Visiting ActionDefinition: " + name);

        // Process annotations for the action definition
        ProcessedAnnotations processed = processAnnotations(ctx.annotation());

        // TODO: Parse parameters and return type if needed for a more detailed ActionNode based on grammar

        // Constructor: ActionNode(Optional<Long> id, String name, Map<String, Object> annotations)
        return new ActionNode(processed.id(), name, processed.annotationMap());
     }

    // Visitor for the 'guardDefinition' rule (assuming similar structure to actionDefinition)
    public Object visitGuardDefinition(SSoTParser.GuardDefinitionContext ctx) {
        String name = "UNKNOWN_GUARD";
        if (ctx.ID() != null) {
            name = ctx.ID().getText();
        } else {
            System.err.println("Warning: No ID found for guard definition: " + ctx.getText());
        }
        System.out.println("Visiting GuardDefinition: " + name);

        // Process annotations
        ProcessedAnnotations processed = processAnnotations(ctx.annotation());

        // TODO: Add parsing for parameters/return type if grammar supports them

        // Constructor: GuardNode(Optional<Long> id, String name, Map<String, Object> annotations)
        return new GuardNode(processed.id(), name, processed.annotationMap());
    }

    // Visitor for the 'invokeDefinition' rule
    public Object visitInvokeDefinition(SSoTParser.InvokeDefinitionContext ctx) {
        String name = "UNKNOWN_INVOKE";
        // Assuming the grammar has an ID for the invocation target
        if (ctx.ID() != null) {
            name = ctx.ID().getText();
        } else {
            // The grammar might be different, e.g., referring to a service directly?
            // Adjust parsing based on the actual SSoT.g4 rule for invokeDefinition
            System.err.println("Warning: No ID found for invoke definition: " + ctx.getText() + ". Grammar needs checking.");
            // For now, use the full text as a placeholder name if ID is missing
            name = ctx.getText(); 
        }
        System.out.println("Visiting InvokeDefinition: " + name);

        // Process annotations
        ProcessedAnnotations processed = processAnnotations(ctx.annotation());

        // TODO: Add parsing for other details like src, data, onDone, onError based on grammar

        // Constructor: InvokeNode(Optional<Long> id, String name, Map<String, Object> annotations)
        return new InvokeNode(processed.id(), name, processed.annotationMap());
    }

    // --- Service related visitors ---
    @Override
    public Object visitServiceElement(SSoTParser.ServiceElementContext ctx) { // Return Object
        // Handles alternatives within servicesBlock
        if (ctx.interfaceDefinition() != null) {
            return visitInterfaceDefinition(ctx.interfaceDefinition());
        } else if (ctx.serviceDefinition() != null) {
            return visitServiceDefinition(ctx.serviceDefinition());
        }
        return null;
    }

     @Override
     public Object visitServiceDefinition(SSoTParser.ServiceDefinitionContext ctx) { // Return Object
         String name = ctx.ID() != null ? ctx.ID().getText() : "UNKNOWN_SERVICE";
         System.out.println("Visiting ServiceDefinition (placeholder): " + name);
         // TODO: Implement based on Service AST Node (needs definition) and grammar
         System.err.println("Warning: ServiceDefinition visitor not implemented.");
         return null; // Placeholder
     }

     @Override
     public Object visitInterfaceDefinition(SSoTParser.InterfaceDefinitionContext ctx) { // Return Object
          String name = ctx.ID() != null ? ctx.ID().getText() : "UNKNOWN_INTERFACE";
          System.out.println("Visiting InterfaceDefinition (placeholder): " + name);
          // TODO: Implement based on Interface AST Node
          System.err.println("Warning: InterfaceDefinition visitor not implemented.");
          return null;
     }

      @Override
      public Object visitMethodDefinition(SSoTParser.MethodDefinitionContext ctx) { // Return Object
           String name = ctx.ID() != null ? ctx.ID().getText() : "UNKNOWN_METHOD";
           System.out.println("Visiting MethodDefinition (placeholder): " + name);
           // TODO: Implement based on Method AST Node
           System.err.println("Warning: MethodDefinition visitor not implemented.");
           return null;
      }

     // --- Actor related visitors ---
      @Override
      public Object visitActorDefinition(SSoTParser.ActorDefinitionContext ctx) { // Return Object
           String name = ctx.ID() != null ? ctx.ID().getText() : "UNKNOWN_ACTOR";
           System.out.println("Visiting ActorDefinition (placeholder): " + name);
           // TODO: Implement based on Actor AST Node
           System.err.println("Warning: ActorDefinition visitor not implemented.");
           return null;
      }

     // --- Communication related visitors ---
      @Override
      public Object visitCommunicationDefinition(SSoTParser.CommunicationDefinitionContext ctx) { // Return Object
          // Handles alternatives protocol | channel | event
          if (ctx.protocolDefinition() != null) {
              return visitProtocolDefinition(ctx.protocolDefinition());
          } else if (ctx.channelDefinition() != null) {
              return visitChannelDefinition(ctx.channelDefinition());
          } else if (ctx.eventDefinition() != null) {
              return visitEventDefinition(ctx.eventDefinition());
          }
          return null;
      }

      @Override
      public Object visitProtocolDefinition(SSoTParser.ProtocolDefinitionContext ctx) { // Return Object
            String name = ctx.ID() != null ? ctx.ID().getText() : "UNKNOWN_PROTOCOL";
            System.out.println("Visiting ProtocolDefinition (placeholder): " + name);
            // TODO: Implement based on Protocol AST Node
            System.err.println("Warning: ProtocolDefinition visitor not implemented.");
            return null;
      }
       @Override
       public Object visitChannelDefinition(SSoTParser.ChannelDefinitionContext ctx) { // Return Object
            String name = ctx.ID() != null ? ctx.ID().getText() : "UNKNOWN_CHANNEL";
            System.out.println("Visiting ChannelDefinition (placeholder): " + name);
            // TODO: Implement based on Channel AST Node
            System.err.println("Warning: ChannelDefinition visitor not implemented.");
            return null;
       }
        @Override
        public Object visitEventDefinition(SSoTParser.EventDefinitionContext ctx) { // Return Object
             String name = ctx.ID() != null ? ctx.ID().getText() : "UNKNOWN_EVENT";
             System.out.println("Visiting EventDefinition (placeholder): " + name);
             // TODO: Implement based on Event AST Node (similar to Struct?)
             System.err.println("Warning: EventDefinition visitor not implemented.");
             return null;
        }

    // --- Annotation Value Parsing Helpers ---

    // Helper method to strip quotes from STRING literals
    private String stripQuotes(String text) {
        if (text != null && text.length() >= 2 && text.startsWith("\"") && text.endsWith("\"")) {
            // TODO: Handle potential escape sequences inside the string if necessary
            return text.substring(1, text.length() - 1);
        }
        return text;
    }

    @Override
    public Object visitValue(SSoTParser.ValueContext ctx) { // Removed @Override
        // Restore original implementation
        if (ctx.primitiveValue() != null) {
            return visitPrimitiveValue(ctx.primitiveValue());
        } else if (ctx.referenceValue() != null) {
            // For now, just return the reference text. Validation/resolution happens later.
            return ctx.referenceValue().getText();
        } else if (ctx.objectValue() != null) {
            return visitObjectValue(ctx.objectValue());
        } else if (ctx.arrayValue() != null) {
            return visitArrayValue(ctx.arrayValue());
        }
        System.err.println("Warning: Unknown value type encountered: " + ctx.getText());
        return null; // Or throw exception
    }

    @Override
    public Object visitPrimitiveValue(SSoTParser.PrimitiveValueContext ctx) { // Removed @Override
        // Restore original implementation
        if (ctx.STRING() != null) {
            return stripQuotes(ctx.STRING().getText());
        } else if (ctx.INT() != null) {
            try {
                return Long.parseLong(ctx.INT().getText()); // Use Long for wider range
            } catch (NumberFormatException e) {
                System.err.println("Warning: Could not parse INT: " + ctx.INT().getText());
                return 0L; // Default or throw
            }
        } else if (ctx.FLOAT() != null) {
            try {
                return Double.parseDouble(ctx.FLOAT().getText()); // Use Double
            } catch (NumberFormatException e) {
                System.err.println("Warning: Could not parse FLOAT: " + ctx.FLOAT().getText());
                return 0.0; // Default or throw
            }
        } else if (ctx.BOOLEAN() != null) {
            return Boolean.parseBoolean(ctx.BOOLEAN().getText());
        }
        return null;
    }

    // Returns Map<String, Object>
    @Override
    public Object visitObjectValue(SSoTParser.ObjectValueContext ctx) { // Removed @Override
        // Restore original implementation
        Map<String, Object> objectMap = new HashMap<>();
        if (ctx.attributePairList() != null) {
            // visitAttributePairList should return a Map
            Object result = visitAttributePairList(ctx.attributePairList());
            if (result instanceof Map) {
                // Need to cast carefully, assuming Map<String, Object>
                try {
                     @SuppressWarnings("unchecked") // Suppress warning, but be cautious
                     Map<String, Object> resultMap = (Map<String, Object>) result;
                     objectMap.putAll(resultMap);
                } catch (ClassCastException e) {
                     System.err.println("Warning: visitAttributePairList did not return Map<String, Object>");
                }
            } else {
                 System.err.println("Warning: visitAttributePairList did not return a Map for objectValue");
            }
        }
        return objectMap; // Return the map representing the object
    }

    // Returns List<Object>
    @Override
    public Object visitArrayValue(SSoTParser.ArrayValueContext ctx) { // Removed @Override
        // Restore original implementation
        List<Object> list = new ArrayList<>();
        if (ctx.valueList() != null) {
            for (SSoTParser.ValueContext valueCtx : ctx.valueList().value()) {
                Object val = visitValue(valueCtx);
                if (val != null) {
                    list.add(val);
                }
            }
        }
        return list;
    }

    // Returns Map<String, Object> representing the list of pairs
    @Override
    public Object visitAttributePairList(SSoTParser.AttributePairListContext ctx) { // Removed @Override
        // Restore original implementation
        Map<String, Object> map = new HashMap<>();
        for (SSoTParser.AttributePairContext pairCtx : ctx.attributePair()) {
            Object pairResult = visitAttributePair(pairCtx);
            // visitAttributePair should ideally return a Map.Entry or similar,
            // but returning a single-entry Map might be simpler here.
            if (pairResult instanceof Map) {
                 try {
                     @SuppressWarnings("unchecked")
                     Map<String, Object> singleEntryMap = (Map<String, Object>) pairResult;
                     map.putAll(singleEntryMap); // Add the entry from the pair
                 } catch (ClassCastException e) {
                      System.err.println("Warning: visitAttributePair did not return Map<String, Object>");
                 }

            } else {
                System.err.println("Warning: visitAttributePair did not return a Map");
            }
        }
        return map;
    }

    // Returns a single-entry Map<String, Object>
    @Override
    public Object visitAttributePair(SSoTParser.AttributePairContext ctx) { // Removed @Override
        // Restore original implementation
        String key = ctx.ID().getText();
        // Grammar: ID COLON (primitiveValue | referenceValue | objectValue | arrayValue)
        Object value = null;
         ParseTree valueTree = null;
         if (ctx.primitiveValue() != null) valueTree = ctx.primitiveValue();
         else if (ctx.referenceValue() != null) valueTree = ctx.referenceValue();
         else if (ctx.objectValue() != null) valueTree = ctx.objectValue();
         else if (ctx.arrayValue() != null) valueTree = ctx.arrayValue();

        if (valueTree != null) {
             value = visit(valueTree); // Use generic visit to dispatch correctly
        } else {
            System.err.println("Warning: Could not determine value type in attributePair for key: " + key);
        }

        // Return a map containing this single key-value pair
        Map<String, Object> pairMap = new HashMap<>();
        pairMap.put(key, value);
        return pairMap;
    }

    @Override
    public Object visitAnnotationValue(SSoTParser.AnnotationValueContext ctx) { // Removed @Override
        // Restore original implementation
        if (ctx.attributePairList() != null) {
            // Returns Map<String, Object>
            return visitAttributePairList(ctx.attributePairList());
        } else if (ctx.value() != null) {
            // Returns the parsed value object (String, Long, List, Map, etc.)
            return visitValue(ctx.value());
        }
        System.err.println("Warning: No value or attribute list found in annotationValue: " + ctx.getText());
        return null;
    }

    // ... other visit methods as needed ...

}