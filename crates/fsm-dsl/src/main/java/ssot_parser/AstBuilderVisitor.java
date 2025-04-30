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

/**
 * Visits the ANTLR Parse Tree and builds the Abstract Syntax Tree (AST).
 * This class extends the generated SSoTBaseVisitor and overrides methods
 * for specific grammar rules to create corresponding AST nodes.
 */
// Make sure AstBuilderVisitor<T> matches SSoTBaseVisitor<T> (AstNode seems correct)
public class AstBuilderVisitor extends SSoTBaseVisitor<Object> {

    // Keep track of the current state name for transitions
    private String currentStateName = null;

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
                 if (result instanceof AstNode) { // Cast to AstNode
                     machineDefs.add((AstNode) result);
                 }
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

    // Record to hold results from processing a list of annotations
    private record ProcessedAnnotations(Optional<Long> id, Map<String, Object> annotationMap) {}

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
    public Object visitMachineDefinition(SSoTParser.MachineDefinitionContext ctx) { // Return Object
         String name = ctx.ID().getText();
         System.out.println("Visiting MachineDefinition: " + name);
         // TODO: Implement proper MachineNode creation.
         // Needs to parse ctx.machineBodyElement* to find context, actions, states etc.
         // Create and return a MachineNode(Optional.empty(), name, context, states, transitions, etc.)
         System.err.println("Warning: MachineDefinition visitor not fully implemented.");
        return null; // Placeholder
    }

     // This should visit the rule 'statesDefinition' if it exists, or handle states within machineBodyElement
     // Let's assume we call visitStateDefinition for each state inside 'statesDefinition' block

     @Override
     public Object visitStatesDefinition(SSoTParser.StatesDefinitionContext ctx) { // Return Object
        System.out.println("Visiting StatesDefinition block...");
        // This visitor might just collect StateNodes and return a list or a wrapper node.
        // For now, just iterate and visit children. The actual collection might happen
        // within visitMachineDefinition when it encounters a statesDefinition element.
        List<AstNode> stateNodes = new ArrayList<>();
         if(ctx.stateDefinition() != null) {
            for(SSoTParser.StateDefinitionContext stateCtx : ctx.stateDefinition()){
                Object stateResult = visitStateDefinition(stateCtx);
                if (stateResult instanceof AstNode) { // Check and cast
                    stateNodes.add((AstNode) stateResult);
                }
            }
         }
         // Returning null because the list should be processed by the caller (visitMachineDefinition)
         // Or return a dedicated StatesBlockNode if needed. Returning null for simplicity now.
         System.err.println("Warning: visitStatesDefinition returning null, caller needs to handle children.");
         return null;
     }


     @Override
     public Object visitStateDefinition(SSoTParser.StateDefinitionContext ctx) { // Return Object
         // Grammar: stateName=ID annotation* LBRACE annotation* stateBodyElement* RBRACE
         String name = ctx.stateName.getText(); // Use label stateName
         System.out.println("Visiting StateDefinition: " + name);
         this.currentStateName = name; // Store current state name for transitions

         List<ActionNode> entryActions = new ArrayList<>();
         List<ActionNode> exitActions = new ArrayList<>();
         List<TransitionNode> transitions = new ArrayList<>();

         // Iterate through state body elements
         if (ctx.stateBodyElement() != null) {
            for (SSoTParser.StateBodyElementContext bodyElement : ctx.stateBodyElement()) {
                if (bodyElement.onEntryExit() != null) {
                    // Grammar: onEntryExit : (ON_ENTRY | ON_EXIT) actionReference SEMI;
                    SSoTParser.OnEntryExitContext entryExitCtx = bodyElement.onEntryExit();
                    String actionRefText = entryExitCtx.actionReference().getText(); // Get reference text
                    // Need to resolve actionReference later, for now create basic ActionNode
                    // TODO: Process annotations for entry/exit actions if grammar allows
                    ProcessedAnnotations entryExitAnnotations = processAnnotations(Collections.emptyList()); // Placeholder
                    ActionNode action = new ActionNode(Optional.empty(), actionRefText, entryExitAnnotations.annotationMap()); // Add annotations map
                    if (entryExitCtx.ON_ENTRY() != null) {
                        entryActions.add(action);
                    } else if (entryExitCtx.ON_EXIT() != null) {
                        exitActions.add(action);
                    }
                } else if (bodyElement.onTransition() != null) {
                     // Delegate to visitOnTransition
                     Object transitionResult = visitOnTransition(bodyElement.onTransition());
                     if (transitionResult instanceof TransitionNode) { // Check and cast
                         transitions.add((TransitionNode) transitionResult);
                     }
                }
                // TODO: Handle invokeState, afterTransition, nested statesDefinition, historyDefinition, annotation
            }
         }

         // Constructor: StateNode(Optional<Long> id, String name, List<ActionNode> actions)
         // Combine entry and exit actions for now, as constructor only takes one list.
         // This needs revisiting based on desired AST structure.
         List<ActionNode> combinedActions = new ArrayList<>(entryActions);
         combinedActions.addAll(exitActions);
         // Transitions are not part of StateNode constructor currently.
         // TODO: Process annotations for the state itself
         ProcessedAnnotations stateAnnotations = processAnnotations(ctx.annotation()); // Process state annotations

         this.currentStateName = null; // Clear current state name after visiting
         // Add stateAnnotations map to constructor
         return new StateNode(Optional.empty(), name, combinedActions, stateAnnotations.annotationMap());
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
     public Object visitActionDefinition(SSoTParser.ActionDefinitionContext ctx) { // Return Object
        String name = "UNKNOWN_ACTION";
         // Assuming ID() returns a List here based on compiler error
         if (ctx.ID() != null && !ctx.ID().isEmpty()) { // Check list not empty
            name = ctx.ID(0).getText(); // Get text from first element
         } else {
            System.err.println("Warning: No ID found for action definition: " + ctx.getText());
         }
        System.out.println("Visiting ActionDefinition: " + name);
        // TODO: Parse parameters and return type if needed for a more detailed ActionNode
        // TODO: Process annotations for the action definition
        ProcessedAnnotations actionAnnotations = processAnnotations(ctx.annotation()); // Process action annotations
        // Constructor: ActionNode(Optional<Long> id, String name)
        // Add actionAnnotations map to constructor
        return new ActionNode(Optional.empty(), name, actionAnnotations.annotationMap());
     }

     // This method might be called by visitActionList (if used) or directly if grammar changes.
     // Let's remove @Override as 'visitAction' is likely not in BaseVisitor if the rule is 'actionDefinition'
     // public AstNode visitAction(SSoTParser.ActionContext ctx) { ... }
     // Let's assume action references are resolved elsewhere for now.

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