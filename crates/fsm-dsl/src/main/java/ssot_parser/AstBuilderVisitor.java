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
    private record ProcessedAnnotations(Optional<Long> id, List<AnnotationNode> annotations) {}

    @Override
    public Object visitFile(SSoTParser.FileContext ctx) { // Return type changed to Object
        System.out.println("Visiting File node...");

        List<AstNode> typeDefs = new ArrayList<>();
        List<AstNode> serviceDefs = new ArrayList<>();
        List<AstNode> machineDefs = new ArrayList<>();
        List<AstNode> actorDefs = new ArrayList<>(); // Added
        List<AstNode> communicationDefs = new ArrayList<>(); // Added
        // TODO: Handle imports, fileId, annotations
        // Process file-level annotations
        ProcessedAnnotations fileAnnotationsResult = processAnnotations(ctx.annotation());
        // TODO: Handle fileId if present in grammar

        // Iterate through definition blocks based on grammar: definitionBlock*
        if (ctx.definitionBlock() != null) {
            for (SSoTParser.DefinitionBlockContext blockCtx : ctx.definitionBlock()) {
                if (blockCtx.typesBlock() != null) {
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
        // Map<String, Object> fileAnnotations = new HashMap<>(); // Placeholder for file annotations
        // return new SsotRoot(typeDefs, serviceDefs, machineDefs, fileAnnotations); // Add annotations map - OLD CALL
        // Pass all collected lists and the processed annotations map to the constructor
        return new SsotRoot(
            typeDefs,
            serviceDefs,
            machineDefs,
            actorDefs, // Added missing argument
            communicationDefs, // Added missing argument
            fileAnnotationsResult.annotations() // Use the list from ProcessedAnnotations
        );
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
        List<AnnotationNode> annotationNodes = new ArrayList<>(); // Changed from Map to List<AnnotationNode>

        if (annotationCtxs != null) {
            for (AnnotationContext ctx : annotationCtxs) {
                Object result = visitAnnotation(ctx); // Returns Long for @id, or AnnotationNode for $name

                if (result instanceof Long) {
                    if (id.isPresent()) {
                        System.err.println("Warning: Duplicate @id annotation found. Ignoring subsequent IDs. First ID was: " + id.get());
                    } else {
                        id = Optional.of((Long) result);
                    }
                } else if (result instanceof AnnotationNode) {
                    annotationNodes.add((AnnotationNode) result);
                    // Check for duplicates if needed (requires iterating annotationNodes)
                    // Example: Find if an annotation with the same name already exists
                    // boolean exists = annotationNodes.stream().anyMatch(node -> node.getName().equals(((AnnotationNode) result).getName()));
                    // Handle duplicates based on project requirements (e.g., log warning, error, allow)
                }
                // Ignore null results from visitAnnotation (e.g., parse errors)
            }
        }
        // Modify the ProcessedAnnotations record or return type if needed to hold List<AnnotationNode>
        // For now, let's assume ProcessedAnnotations record is updated or we create a new return structure.
        // We need to update the record definition first.
        // return new ProcessedAnnotations(id, annotationNodes);

        // TEMPORARY: Return original record structure with an empty map until record is updated
        // This will likely cause errors downstream until all usages are updated.
        //return new ProcessedAnnotations(id, new HashMap<>());
        // Let's redefine ProcessedAnnotations record to hold the list
        // Remove the old record definition near the top and replace with:
        // private record ProcessedAnnotationsResult(Optional<Long> id, List<AnnotationNode> annotations) {}
        // Then return:
         return new ProcessedAnnotations(id, annotationNodes); // Assuming record is updated
    }

    // Visitor for a single annotation rule: handles @id, $name(value), $flag;
    // Returns Long for @id, or AnnotationNode for $name annotations.
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

             // Create and return AnnotationNode
             return new AnnotationNode(name, value, false); // false because it's not @id

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
                if (fieldResult instanceof FieldNode) {
                    fields.add((FieldNode) fieldResult);
                }
            }
        }
        // Pass processed ID and annotations map to constructor
        return new TypeDefNode(processed.id(), name, fields, processed.annotations());
    }

    @Override
    public Object visitFieldDefinition(SSoTParser.FieldDefinitionContext ctx) { // Return Object
        System.out.println("Visiting FieldDefinition...");
        ProcessedAnnotations processed = processAnnotations(ctx.annotation());
        String name = ctx.IDENTIFIER().getText();
        TypeExprNode type = null; // Initialize to null
        if (ctx.typeExpr() != null) {
            Object typeResult = visit(ctx.typeExpr()); // Call visit on typeExpr
            if (typeResult instanceof TypeExprNode) {
                type = (TypeExprNode) typeResult;
            } else {
                 System.err.println("Warning: visitTypeExpr did not return a TypeExprNode for field '" + name + "'. Got: " + (typeResult != null ? typeResult.getClass().getName() : "null"));
                 // Decide how to handle error: return null, throw, create default TypeExprNode?
                 return null; // Return null to indicate error in field definition
            }
        }
        if (type == null) {
             System.err.println("Error: Type expression missing for field '" + name + "'.");
             return null; // Cannot create FieldNode without type
        }

        // Default value handling - depends on grammar for defaultValue
        // Optional<Object> defaultValue = Optional.empty();
        // if (ctx.defaultValue() != null) { ... visit defaultValue ... }

        return new FieldNode(processed.id(), name, type, processed.annotations());
    }

    // Visitor for Enum Variant
    public Object visitEnumVariant(SSoTParser.EnumVariantContext ctx) {
        System.out.println("Visiting EnumVariant: " + ctx.ID().getText());
        String name = ctx.ID().getText();
        ProcessedAnnotations processed = processAnnotations(ctx.annotation());
        return new EnumVariantNode(processed.id(), processed.annotations(), name);
    }

    // Assuming visitEnumDefinition is needed
    @Override
    public Object visitEnumDefinition(SSoTParser.EnumDefinitionContext ctx) { // Return Object
        String name = ctx.ID().getText();
        System.out.println("Visiting EnumDefinition: " + name);

        // Process annotations on the enum itself
        List<AnnotationContext> allAnnotationCtxs = new ArrayList<>();
        if (ctx.annotation() != null) {
            allAnnotationCtxs.addAll(ctx.annotation()); // Before and after LBRACE
        }
        ProcessedAnnotations enumAnnotations = processAnnotations(allAnnotationCtxs);

        List<EnumVariantNode> variants = new ArrayList<>();
        if (ctx.enumVariant() != null) {
            for (SSoTParser.EnumVariantContext variantCtx : ctx.enumVariant()) {
                Object variantResult = visitEnumVariant(variantCtx);
                if (variantResult instanceof EnumVariantNode) {
                    variants.add((EnumVariantNode) variantResult);
                } else if (variantResult != null) {
                    System.err.println("Warning: visitEnumVariant did not return EnumVariantNode. Got: " + variantResult.getClass().getName());
                }
            }
        }

        // TODO: Implement EnumNode creation (needs EnumNode class) -- Implementing now
        // Extract name ctx.ID()
        // Iterate ctx.enumVariant()
        // System.err.println("Warning: visitEnumDefinition not fully implemented.");
        // return null;
        return new EnumNode(enumAnnotations.id(), enumAnnotations.annotations(), name, variants);
    }

    // --- Machine related visitors ---

    @Override
    public Object visitMachineDefinition(SSoTParser.MachineDefinitionContext ctx) { // Return Object, should be MachineNode
        String name = ctx.ID().getText();
        System.out.println("Visiting MachineDefinition: " + name);

        // Process annotations specific to this machine definition
        ProcessedAnnotations processed = processAnnotations(ctx.annotation());
        List<AnnotationNode> annotations = new ArrayList<>(); // Placeholder for actual AnnotationNode objects
        // TODO: Convert processed.annotations() and potentially processed.id() into AnnotationNode list

        // Initialize placeholders for machine elements
        Optional<AstNode> contextNode = Optional.empty(); // Expecting ContextNode later
        List<AstNode> actions = new ArrayList<>();     // Expecting List<ActionNode> later
        List<AstNode> guards = new ArrayList<>();      // Expecting List<GuardNode> later
        List<AstNode> invokes = new ArrayList<>();     // Expecting List<InvokeNode> later
        List<AstNode> states = new ArrayList<>();      // Expecting List<StateNode> or Map<String, StateNode> later
        Optional<String> initialState = Optional.empty(); // Reset here, will be set by annotation or fallback
        List<AstNode> transitions = new ArrayList<>(); // Expecting List<TransitionNode> later

        // --- Process $initial annotation ---
        if (processed.annotations().stream().anyMatch(node -> node instanceof AnnotationNode && ((AnnotationNode) node).getName().equals("initial"))) {
            Optional<AnnotationNode> initialAnnotation = processed.annotations().stream()
                .filter(node -> node instanceof AnnotationNode && ((AnnotationNode) node).getName().equals("initial"))
                .findFirst();
            if (initialAnnotation.isPresent()) {
                Object initialValue = initialAnnotation.get().getValue();
                if (initialValue instanceof String) {
                    initialState = Optional.of((String) initialValue);
                    System.out.println("Found $initial annotation, setting initial state to: " + initialState.get());
                } else {
                    System.err.println("Warning: $initial annotation value is not a String for machine '" + name + "'. Ignoring.");
                }
                // Remove $initial from the list so it doesn't become a generic AnnotationNode
                processed.annotations().remove(initialAnnotation.get());
            }
        }
        // --- End of $initial processing ---

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
                 // --- Fallback for initialState if not set by $initial annotation ---
                 // If we processed a StatesInfo block and the machine's initialState is still empty,
                 // use the initial state identified within that block (usually the first one).
                 if (elementResult instanceof StatesInfo) {
                     StatesInfo statesResult = (StatesInfo) elementResult;
                     if (initialState.isEmpty() && statesResult.initialStatename().isPresent()) {
                         initialState = statesResult.initialStatename();
                         System.out.println("Info: Using initial state '" + initialState.get() + "' identified from states block (no $initial annotation found).");
                     }
                 }
                 // --- End Fallback for initialState ---

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
            processed.id(),
            processed.annotations(),
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

     public ContextNode visitContextDefinition(SSoTParser.ContextDefinitionContext ctx) { // Changed return type
        System.out.println("Visiting ContextDefinition...");
        ProcessedAnnotations processed = processAnnotations(ctx.annotation());

        List<FieldNode> variables = new ArrayList<>();
        if (ctx.contextBody() != null && ctx.contextBody().contextEntry() != null) {
            for (SSoTParser.ContextEntryContext entry : ctx.contextBody().contextEntry()) {
                if (entry.fieldDefinition() != null) { // Check if fieldDefinition exists in entry
                    Object fieldResult = visit(entry.fieldDefinition()); // Visit the field definition
                    if (fieldResult instanceof FieldNode) {
                        variables.add((FieldNode) fieldResult);
                    } else if (fieldResult != null) {
                        System.err.println("Warning: visitFieldDefinition within context did not return FieldNode. Got: " + fieldResult.getClass().getName());
                    }
                } else {
                     // Handle other possible context entries if grammar allows (e.g., nested context?)
                }
            }
        }

        return new ContextNode(processed.id(), variables, processed.annotations());
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
         List<StateNode> states = new ArrayList<>();
         String initialStatename = null; // Changed from Optional<String>

         // Check for explicit initial state declaration
         if (ctx.initialStateClause() != null && ctx.initialStateClause().IDENTIFIER() != null) {
             initialStatename = ctx.initialStateClause().IDENTIFIER().getText();
             System.out.println("Found explicit initial state: " + initialStatename);
         }

         if (ctx.stateDefinition() != null) {
             for (SSoTParser.StateDefinitionContext stateCtx : ctx.stateDefinition()) {
                 Object stateResult = visit(stateCtx); // visitStateDefinition returns StateNode
                 if (stateResult instanceof StateNode) {
                     states.add((StateNode) stateResult);
                 } else if (stateResult != null) {
                     System.err.println("Warning: visitStateDefinition did not return StateNode. Got: " + stateResult.getClass().getName());
                 }
             }
         }

         // If no explicit initial state, default to the first state defined (if any)
         if (initialStatename == null && !states.isEmpty()) {
             initialStatename = states.get(0).stateName; // Use stateName
             System.out.println("Defaulting initial state to first defined: " + initialStatename);
         }

         // Instead of StatesInfo, maybe just return the list and let the caller (visitMachineDefinition) handle initial state?
         // For now, let's return a simple Map as a placeholder for StatesInfo
         Map<String, Object> result = new HashMap<>();
         result.put("states", states);
         result.put("initialStateName", initialStatename); // Pass the determined name
         return result; // Returning Map, caller needs to cast and extract
     }

     @Override
     public Object visitStateDefinition(SSoTParser.StateDefinitionContext ctx) { // Return Object, should be StateNode
          String name = ctx.stateName.getText(); // Use label
          currentStateName = name; // Set current state name for transitions
          System.out.println("Visiting StateDefinition: " + name);

          // Process annotations
          List<AnnotationContext> allAnnotationsCtx = new ArrayList<>();
          if (ctx.annotation() != null) allAnnotationsCtx.addAll(ctx.annotation());
          ProcessedAnnotations processedAnnotations = processAnnotations(allAnnotationsCtx);

          List<String> entryActions = new ArrayList<>();
          List<String> exitActions = new ArrayList<>();
          List<EventHandlerNode> eventHandlers = new ArrayList<>(); // New
          List<ConditionalTransitionNode> conditionalTransitions = new ArrayList<>(); // New (but unused for now)
          Optional<InvokeStateNode> invoke = Optional.empty();
          List<StateNode> nestedStates = new ArrayList<>();
          StateType type = StateType.ATOMIC; // Default to ATOMIC
          String initialStateName = null;

          // Process state body elements
          if (ctx.stateBody() != null && ctx.stateBody().stateBodyElement() != null) {
              for (SSoTParser.StateBodyElementContext elementCtx : ctx.stateBody().stateBodyElement()) {
                  if (elementCtx.onEntryExit() != null && elementCtx.onEntryExit().ON_ENTRY() != null) {
                      if (elementCtx.onEntryExit().actionReference() != null && elementCtx.onEntryExit().actionReference().referenceValue() != null) {
                          entryActions.add(elementCtx.onEntryExit().actionReference().referenceValue().getText());
                      }
                  } else if (elementCtx.onEntryExit() != null && elementCtx.onEntryExit().ON_EXIT() != null) {
                      if (elementCtx.onEntryExit().actionReference() != null && elementCtx.onEntryExit().actionReference().referenceValue() != null) {
                          exitActions.add(elementCtx.onEntryExit().actionReference().referenceValue().getText());
                      }
                  } else if (elementCtx.onTransition() != null) {
                      Object handlerResult = visitOnTransition(elementCtx.onTransition()); // Call specific visitor
                      if (handlerResult instanceof EventHandlerNode) {
                          eventHandlers.add((EventHandlerNode) handlerResult);
                      } else if (handlerResult != null) {
                           System.err.println("Warning: visitOnTransition did not return EventHandlerNode. Got: " + handlerResult.getClass().getName());
                      }
                  } else if (elementCtx.invokeState() != null) {
                      Object invokeResult = visitInvokeState(elementCtx.invokeState()); // Call specific visitor
                      if (invokeResult instanceof InvokeStateNode) {
                          if (invoke.isPresent()) {
                              System.err.println("Warning: Multiple invoke declarations found in state '" + name + "'. Only the last one will be used.");
                          }
                          invoke = Optional.of((InvokeStateNode) invokeResult);
                      } else if (invokeResult != null) {
                            System.err.println("Warning: visitInvokeState did not return InvokeStateNode. Got: " + invokeResult.getClass().getName());
                      }
                  } else if (elementCtx.statesDefinition() != null) { // Changed from nestedStateDefinition
                      // Nested states are now handled by visiting the statesDefinition block directly
                       Object nestedStatesResult = visitStatesDefinition(elementCtx.statesDefinition());
                       // visitStatesDefinition should return StatesInfo or similar containing the list
                       if (nestedStatesResult instanceof StatesInfo) { // Assuming StatesInfo helper record exists
                            nestedStates.addAll(((StatesInfo) nestedStatesResult).states);
                            // Handle initial state from nested block if needed
                            if (initialStateName == null && ((StatesInfo) nestedStatesResult).initialStateName != null) {
                                 initialStateName = ((StatesInfo) nestedStatesResult).initialStateName;
                            }
                       } else if (nestedStatesResult != null) {
                            System.err.println("Warning: visitStatesDefinition for nested states did not return StatesInfo. Got: " + nestedStatesResult.getClass().getName());
                       }
                  } else if (elementCtx.annotation() != null) {
                        System.out.println("Info: Annotation found inside state body: " + elementCtx.annotation().getText() + " - Currently ignored.");
                  } else if (elementCtx.afterTransition() != null) {
                        // TODO: Handle afterTransition when needed
                        System.out.println("Info: afterTransition found but not currently processed into AST: " + elementCtx.afterTransition().getText());
                  }
              }
          }

          // Determine state type based on content
          if (!nestedStates.isEmpty()) {
              // TODO: Differentiate between COMPOUND and PARALLEL if grammar supports parallel states
              type = StateType.COMPOUND;
          } else if (invoke.isPresent()) {
              // States with invokes might be considered atomic or compound depending on definition
              // Let's keep it ATOMIC unless nested states exist.
          } else {
              // Check if it's explicitly marked as final (requires grammar support or specific annotation)
              boolean isFinal = processedAnnotations.annotations().stream()
                                   .anyMatch(a -> a.name.equals("final") && "true".equals(String.valueOf(a.value).toLowerCase())); // Example check
              if (isFinal) {
                  type = StateType.FINAL;
              } else if (eventHandlers.isEmpty() && conditionalTransitions.isEmpty() && !invoke.isPresent()) {
                  // Potentially FINAL if no transitions out and no invoke?
                  // This inference is risky, prefer explicit marking.
                  // type = StateType.FINAL; // Commented out - prefer explicit
              } else {
                  type = StateType.ATOMIC;
              }
          }

          // Determine initial state for compound states
          if ((type == StateType.COMPOUND || type == StateType.PARALLEL) && initialStateName == null && !nestedStates.isEmpty()) {
              initialStateName = nestedStates.get(0).getStateName();
              System.out.println("Info: Inferring initial state for compound state '" + name + "' to be '" + initialStateName + "'");
          }

          // Reset current state name after processing this state
          currentStateName = null;

          return new StateNode(
                  processedAnnotations.id(),
                  name,
                  processedAnnotations.annotations(),
                  entryActions,
                  exitActions,
                  eventHandlers, // Use new list
                  conditionalTransitions, // Use new list (empty for now)
                  invoke,
                  nestedStates,
                  type,
                  initialStateName != null ? initialStateName : "" // Pass empty string if null for constructor
          );
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
         System.out.println("Visiting ServiceDefinition: " + name);

         // Process annotations attached to the service definition
         List<AnnotationContext> allAnnotationCtxs = new ArrayList<>();
         if (ctx.annotation() != null) {
             allAnnotationCtxs.addAll(ctx.annotation()); // Collect before and after LBRACE
         }
         ProcessedAnnotations serviceAnnotations = processAnnotations(allAnnotationCtxs);

         List<MethodNode> methods = new ArrayList<>();
         List<String> implementedInterfaces = new ArrayList<>();

         // Iterate through service body elements based on grammar
         if (ctx.serviceBody() != null && ctx.serviceBody().serviceEntry() != null) { // Check serviceBody and serviceEntry
            for (SSoTParser.ServiceEntryContext entryCtx : ctx.serviceBody().serviceEntry()) { // Iterate over serviceEntry
                // Check which element is present within the entry
                if (entryCtx.methodDefinition() != null) {
                    Object methodResult = visitMethodDefinition(entryCtx.methodDefinition());
                    if (methodResult instanceof MethodNode) {
                        methods.add((MethodNode) methodResult);
                    } else if (methodResult != null) {
                         System.err.println("Warning: visitMethodDefinition did not return MethodNode for service: " + name + ". Got: " + methodResult.getClass().getName());
                    }
                } else if (entryCtx.implementsDeclaration() != null) {
                    // Parse implemented interfaces
                    SSoTParser.ImplementsDeclarationContext implCtx = entryCtx.implementsDeclaration();
                    if (implCtx.typeExprList() != null && implCtx.typeExprList().typeExpr() != null) { // Check typeExprList and its content
                        for (SSoTParser.TypeExprContext typeCtx : implCtx.typeExprList().typeExpr()) {
                            // Assuming typeExpr directly gives the interface name for now
                            // A more robust approach might involve visiting typeExpr and ensuring it's a simple reference
                            Object typeResult = visit(typeCtx); // Visit the type expression
                            if (typeResult instanceof ReferenceTypeNode) { // Check if it's a ReferenceTypeNode
                                implementedInterfaces.add(((ReferenceTypeNode) typeResult).getReferencedTypeName());
                            } else {
                                 System.err.println("Warning: Implemented type is not a simple reference: " + typeCtx.getText());
                                 implementedInterfaces.add(typeCtx.getText()); // Fallback to getText?
                            }
                        }
                    }
                } else if (entryCtx.annotation() != null) {
                     // Annotations inside the body are currently ignored, but could be attached to the service
                     System.out.println("Info: Annotation found inside service body: " + entryCtx.annotation().getText() + " - Currently ignored.");
                }
            }
         }

         return new ServiceNode(
             serviceAnnotations.id(),
             name,
             methods,
             implementedInterfaces,
             serviceAnnotations.annotations()
         );
     }

     @Override
     public Object visitInterfaceDefinition(SSoTParser.InterfaceDefinitionContext ctx) { // Return Object
          String name = ctx.ID() != null ? ctx.ID().getText() : "UNKNOWN_INTERFACE";
          System.out.println("Visiting InterfaceDefinition: " + name);

          // Process annotations attached to the interface definition
          List<AnnotationContext> allAnnotationCtxs = new ArrayList<>();
          if (ctx.annotation() != null) {
              allAnnotationCtxs.addAll(ctx.annotation()); // Collect before and after LBRACE
          }
          ProcessedAnnotations interfaceAnnotations = processAnnotations(allAnnotationCtxs);

          List<MethodNode> methods = new ArrayList<>();
          if (ctx.methodDefinition() != null) { // Assuming methods are direct children
              for (SSoTParser.MethodDefinitionContext methodCtx : ctx.methodDefinition()) {
                  Object methodResult = visitMethodDefinition(methodCtx);
                  if (methodResult instanceof MethodNode) {
                      methods.add((MethodNode) methodResult);
                  } else if (methodResult != null) {
                       System.err.println("Warning: visitMethodDefinition did not return MethodNode for interface: " + name + ". Got: " + methodResult.getClass().getName());
                  }
              }
          }

          // TODO: Handle annotations inside LBRACE if they have specific meaning

          return new InterfaceNode(
              interfaceAnnotations.id(),
              name,
              methods,
              interfaceAnnotations.annotations()
          );
     }

      @Override
      public Object visitMethodDefinition(SSoTParser.MethodDefinitionContext ctx) { // Return Object
           System.out.println("Visiting MethodDefinition...");
           ProcessedAnnotations processed = processAnnotations(ctx.annotation());
           String name = ctx.IDENTIFIER().getText();
           List<ParameterNode> parameters = new ArrayList<>();
           if (ctx.parameterList() != null) {
               parameters = visitParameterList(ctx.parameterList()); // Use helper
           }

           Optional<TypeExprNode> returnType = Optional.empty();
           if (ctx.typeExpr() != null) {
               Object typeResult = visit(ctx.typeExpr()); // Visit the return type expression
               if (typeResult instanceof TypeExprNode) {
                    returnType = Optional.of((TypeExprNode) typeResult);
               } else {
                    System.err.println("Warning: visitTypeExpr did not return a TypeExprNode for return type of method '" + name + "'. Got: " + (typeResult != null ? typeResult.getClass().getName() : "null"));
                    // Decide handling: Optional.empty() or placeholder?
               }
           }

           return new MethodNode(processed.id(), name, parameters, returnType, processed.annotations());
      }

     // --- Actor related visitors ---
      @Override
      public Object visitActorDefinition(SSoTParser.ActorDefinitionContext ctx) { // Return Object
           String name = ctx.ID() != null ? ctx.ID().getText() : "UNKNOWN_ACTOR";
           System.out.println("Visiting ActorDefinition: " + name);

           // Process annotations
           ProcessedAnnotations actorAnnotations = processAnnotations(ctx.annotation());

           // TODO: Visit actor body elements if the grammar defines them

           return new ActorNode(actorAnnotations.id(), name, actorAnnotations.annotations());
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
            System.out.println("Visiting ProtocolDefinition: " + name);
            ProcessedAnnotations protocolAnnotations = processAnnotations(ctx.annotation());

            // TODO: Visit protocol body elements based on grammar

            return new ProtocolNode(protocolAnnotations.id(), name, protocolAnnotations.annotations());
      }
       @Override
       public Object visitChannelDefinition(SSoTParser.ChannelDefinitionContext ctx) { // Return Object
            String name = ctx.ID() != null ? ctx.ID().getText() : "UNKNOWN_CHANNEL";
            System.out.println("Visiting ChannelDefinition: " + name);
            ProcessedAnnotations channelAnnotations = processAnnotations(ctx.annotation());

            // TODO: Visit channel body elements based on grammar

            return new ChannelNode(channelAnnotations.id(), name, channelAnnotations.annotations());
       }
        @Override
        public Object visitEventDefinition(SSoTParser.EventDefinitionContext ctx) { // Return Object
             String name = ctx.ID() != null ? ctx.ID().getText() : "UNKNOWN_EVENT";
             System.out.println("Visiting EventDefinition: " + name);
             ProcessedAnnotations eventAnnotations = processAnnotations(ctx.annotation());

             List<FieldNode> fields = new ArrayList<>();
             // Assuming event definition grammar is similar to struct: event ID { fieldDefinition* }
             if (ctx.fieldDefinition() != null) {
                 for (SSoTParser.FieldDefinitionContext fieldCtx : ctx.fieldDefinition()) {
                     Object fieldResult = visitFieldDefinition(fieldCtx);
                     if (fieldResult instanceof FieldNode) {
                         fields.add((FieldNode) fieldResult);
                     }
                 }
             }

             return new EventNode(eventAnnotations.id(), name, fields, eventAnnotations.annotations());
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

    // Placeholder for visitParameter - Needs implementation based on grammar
    public Object visitParameter(SSoTParser.ParameterContext ctx) {
        System.out.println("Visiting Parameter...");
        ProcessedAnnotations processed = processAnnotations(ctx.annotation());
        String name = ctx.IDENTIFIER().getText();
        TypeExprNode type = null;
        if (ctx.typeExpr() != null) {
            Object typeResult = visit(ctx.typeExpr());
            if (typeResult instanceof TypeExprNode) {
                type = (TypeExprNode) typeResult;
            } else {
                 System.err.println("Warning: visitTypeExpr did not return a TypeExprNode for parameter '" + name + "'. Got: " + (typeResult != null ? typeResult.getClass().getName() : "null"));
                 return null; // Indicate error
            }
        } else {
             System.err.println("Error: Type expression missing for parameter '" + name + "'.");
             return null; // Indicate error
        }

        return new ParameterNode(processed.id(), processed.annotations(), name, type);
    }

    // Placeholder for visitParameterList - Needs implementation based on grammar
    public List<ParameterNode> visitParameterList(SSoTParser.ParameterListContext ctx) {
        System.out.println("Visiting ParameterList...");
        List<ParameterNode> parameters = new ArrayList<>();
        if (ctx != null && ctx.parameter() != null) {
            for (SSoTParser.ParameterContext paramCtx : ctx.parameter()) {
                Object result = visitParameter(paramCtx);
                if (result instanceof ParameterNode) {
                    parameters.add((ParameterNode) result);
                } else if (result != null) {
                    System.err.println("Warning: visitParameter did not return ParameterNode. Got: " + result.getClass().getName());
                }
            }
        }
        return parameters;
    }

    // --- Type Expression Visitor ---
    // Returns TypeExprNode based on the parsed type expression
    @Override
    public Object visitTypeExpr(SSoTParser.TypeExprContext ctx) {
        if (ctx.primitiveTypeName() != null) {
            return new PrimitiveTypeNode(ctx.primitiveTypeName().getText());
        } else if (ctx.referenceValue() != null) {
            return new ReferenceTypeNode(ctx.referenceValue().getText());
        } else if (ctx.OPTIONAL() != null && ctx.typeExpr(0) != null) {
            Object innerType = visitTypeExpr(ctx.typeExpr(0));
            if (innerType instanceof TypeExprNode) {
                return new OptionalTypeNode((TypeExprNode) innerType);
            } else {
                System.err.println("Warning: Could not parse inner type for optional: " + ctx.typeExpr(0).getText());
                return null; // Or a placeholder error node
            }
        } else if (ctx.LIST() != null && ctx.typeExpr(0) != null) {
            Object elementType = visitTypeExpr(ctx.typeExpr(0));
            if (elementType instanceof TypeExprNode) {
                return new ListTypeNode((TypeExprNode) elementType);
            } else {
                System.err.println("Warning: Could not parse element type for list: " + ctx.typeExpr(0).getText());
                return null;
            }
        } else if (ctx.MAP() != null && ctx.typeExpr(0) != null && ctx.typeExpr(1) != null) {
            Object keyType = visitTypeExpr(ctx.typeExpr(0));
            Object valueType = visitTypeExpr(ctx.typeExpr(1));
            if (keyType instanceof TypeExprNode && valueType instanceof TypeExprNode) {
                return new MapTypeNode((TypeExprNode) keyType, (TypeExprNode) valueType);
            } else {
                System.err.println("Warning: Could not parse key/value types for map: " + ctx.getText());
                return null;
            }
        } else {
             System.err.println("Warning: Unrecognized type expression: " + ctx.getText());
             return null; // Or a specific UnknownTypeNode
        }
    }

    // Helper to parse transitionSpec rule and create TransitionConfig
    // Returns TransitionConfig based on target, actions, and guards found.
    private TransitionConfig visitTransitionSpec(SSoTParser.TransitionSpecContext ctx) {
        if (ctx == null) {
            System.err.println("Error: null TransitionSpecContext provided.");
            return new TransitionConfig("UNKNOWN_TARGET", Collections.emptyList(), Optional.empty());
        }

        // 1. Parse Target State
        String targetState = "UNKNOWN_TARGET";
        if (ctx.targetState() != null) {
            SSoTParser.TargetStateContext targetCtx = ctx.targetState();
            if (targetCtx.referenceValue() != null) {
                targetState = targetCtx.referenceValue().getText();
            } else if (targetCtx.HISTORY() != null) {
                targetState = ".history"; // Special marker for history target
            } else {
                 System.err.println("Warning: Unknown targetState structure: " + targetCtx.getText());
            }
        } else {
             System.err.println("Error: Missing targetState in transitionSpec: " + ctx.getText());
        }

        List<String> actionNames = new ArrayList<>();
        Optional<String> guardName = Optional.empty(); // Assuming only one guard based on common usage, though grammar allows list
        boolean guardNegated = false;

        // 2. Parse Transition Options Block {}
        if (ctx.LBRACE() != null && ctx.RBRACE() != null && ctx.transitionOptions() != null) {
             SSoTParser.TransitionOptionsContext optionsCtx = ctx.transitionOptions();
             if (optionsCtx.transitionOption() != null) {
                 for (SSoTParser.TransitionOptionContext optionCtx : optionsCtx.transitionOption()) {
                     if (optionCtx.ACTION() != null && optionCtx.actionReferenceList() != null) {
                         // Parse actionReferenceList
                         SSoTParser.ActionReferenceListContext actionListCtx = optionCtx.actionReferenceList();
                         if (actionListCtx.actionReference() != null) {
                             if (actionListCtx.LBRACK() != null) { // List format: [ref1, ref2]
                                for (SSoTParser.ActionReferenceContext actionRefCtx : actionListCtx.actionReference()) {
                                     if (actionRefCtx.referenceValue() != null) {
                                         actionNames.add(actionRefCtx.referenceValue().getText());
                                     }
                                }
                             } else { // Single reference format: ref1
                                  if (actionListCtx.actionReference(0) != null && actionListCtx.actionReference(0).referenceValue() != null) {
                                      actionNames.add(actionListCtx.actionReference(0).referenceValue().getText());
                                  }
                             }
                         }
                     } else if (optionCtx.GUARD() != null && optionCtx.guardReferenceList() != null) {
                         // Parse guardReferenceList (taking the first one if multiple are defined, as TransitionConfig expects Optional<String>)
                         SSoTParser.GuardReferenceListContext guardListCtx = optionCtx.guardReferenceList();
                         if (guardListCtx.guardReference() != null && !guardListCtx.guardReference().isEmpty()) {
                             SSoTParser.GuardReferenceContext firstGuardCtx = guardListCtx.guardReference(0);
                             if (firstGuardCtx.referenceValue() != null) {
                                 if (guardName.isPresent()) {
                                     System.err.println("Warning: Multiple guards specified in transition, using the first one: " + firstGuardCtx.referenceValue().getText());
                                 } else {
                                     guardName = Optional.of(firstGuardCtx.referenceValue().getText());
                                     guardNegated = (firstGuardCtx.NOT() != null); // Check for negation
                                 }
                             }
                             if (guardListCtx.guardReference().size() > 1) {
                                 System.err.println("Warning: Multiple guards specified in guardReferenceList, only the first one is considered by current AST structure.");
                             }
                         }
                     } else if (optionCtx.ALLOWED_ACTORS() != null) {
                         // TODO: Handle allowedActors if needed in AST
                         System.out.println("Info: allowedActors option found but not currently processed into AST: " + optionCtx.getText());
                     } else if (optionCtx.annotation() != null) {
                         // TODO: Handle annotations attached to options if needed
                         System.out.println("Info: Annotation found within transitionOptions: " + optionCtx.annotation().getText() + " - Currently ignored.");
                     }
                 }
             }
        }

        // TODO: Handle guard negation if required by AST/Validation later.
        // Currently, only the guard name is stored in TransitionConfig.
        if (guardNegated) {
             System.out.println("Info: Guard '" + guardName.orElse("") + "' has negation, but negation is not stored in TransitionConfig yet.");
        }

        // TODO: Process annotations attached directly to transitionSpec if grammar allows.

        return new TransitionConfig(targetState, actionNames, guardName);
    }

    @Override
    public Object visitOnTransition(SSoTParser.OnTransitionContext ctx) { // Return Object
        System.out.println("Visiting OnTransition...");
        String event = ctx.event.getText(); // Use the label 'event'

        // Process annotations attached to the 'on Event' line itself
        ProcessedAnnotations processedAnnotations = processAnnotations(ctx.annotation());

        // Visit the transitionSpec using the new helper method
        TransitionConfig config = visitTransitionSpec(ctx.transitionSpec());

        // TODO: Attach annotations from processedAnnotations to EventHandlerNode if needed.
        // Currently EventHandlerNode doesn't store annotations directly.
        if (!processedAnnotations.annotations().isEmpty()) {
             System.out.println("Info: Annotations found on 'onTransition' line but not stored in EventHandlerNode: " + processedAnnotations.annotations());
        }
        if (processedAnnotations.id().isPresent()) {
            System.out.println("Warning: @id found on 'onTransition' line but not stored in EventHandlerNode: " + processedAnnotations.id().get());
        }

        return new EventHandlerNode(
            Optional.of(event),
            Collections.singletonList(config) // Wrap the single config in a list
        );
    }

    // Visitor for invoke declarations within a state
    @Override // Added @Override assuming it overrides a method in BaseVisitor
    public Object visitInvokeState(SSoTParser.InvokeStateContext ctx) {
        System.out.println("Visiting InvokeState...");
        // Process annotations attached to the invoke itself
        ProcessedAnnotations invokeAnnotations = processAnnotations(ctx.annotation());

        // Grammar: INVOKE ID annotation* LBRACE annotation* invokeBody? RBRACE SEMI;
        // Need to get the invoked source name (likely an ID referring to an invokesDefinition entry)
        String invokedSourceName = "UNKNOWN_INVOKE_SOURCE";
        if (ctx.ID() != null) { // Assuming the ID after INVOKE is the reference
            invokedSourceName = ctx.ID().getText();
        } else {
             System.err.println("Error: Missing identifier for invoke source reference.");
             // Maybe return null or a dummy node?
             return null; // Returning null for now
        }

        Optional<TransitionConfig> onDoneConfig = Optional.empty();
        Optional<TransitionConfig> onErrorConfig = Optional.empty();

        // Process onDone and onError clauses within invokeBody
        if (ctx.invokeBody() != null && ctx.invokeBody().invokeTransition() != null) {
            for(SSoTParser.InvokeTransitionContext transitionCtx : ctx.invokeBody().invokeTransition()) {
                 SSoTParser.TransitionSpecContext specCtx = transitionCtx.transitionSpec();
                 if (specCtx != null) {
                     TransitionConfig parsedConfig = visitTransitionSpec(specCtx);
                     if (transitionCtx.ON_DONE() != null) {
                         if (onDoneConfig.isPresent()) {
                            System.err.println("Warning: Multiple onDone transitions found for invoke '" + invokedSourceName + "'. Using the last one.");
                         }
                         onDoneConfig = Optional.of(parsedConfig);
                     } else if (transitionCtx.ON_ERROR() != null) {
                          if (onErrorConfig.isPresent()) {
                            System.err.println("Warning: Multiple onError transitions found for invoke '" + invokedSourceName + "'. Using the last one.");
                         }
                         onErrorConfig = Optional.of(parsedConfig);
                     }
                 } else {
                      System.err.println("Warning: Found onDone/onError keyword but missing transitionSpec in invoke block for: " + invokedSourceName);
                 }
            }
        }

        return new InvokeStateNode(
            invokeAnnotations.id(),
            invokeAnnotations.annotations(),
            invokedSourceName,
            onDoneConfig, // Pass Optional<TransitionConfig>
            onErrorConfig // Pass Optional<TransitionConfig>
            // Removed the old TransitionNode list argument
        );
    }

    // ... other visit methods as needed ...

}