package ssot_parser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.antlr.v4.runtime.tree.ParseTree; // Import needed for context checks

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
    public AstNode visitFile(SSoTParser.FileContext ctx) { // Changed from visitSsotFile to match grammar rule 'file'
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
        return new SsotRoot(typeDefs, serviceDefs, machineDefs);
    }

    // --- Helper methods for visiting blocks (NOT overriding BaseVisitor) ---

    // Helper for Types Block
    private List<AstNode> visitTypesBlockHelper(SSoTParser.TypesBlockContext ctx) {
        System.out.println("Visiting TypesBlock node...");
        List<AstNode> typeDefs = new ArrayList<>();
        if (ctx.typeDefinition() != null) {
            for (SSoTParser.TypeDefinitionContext typeCtx : ctx.typeDefinition()) {
                AstNode typeNode = visit(typeCtx); // This will call visitStructDefinition or visitEnumDefinition
                if (typeNode != null) {
                    typeDefs.add(typeNode);
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
                 AstNode machineNode = visit(machineCtx); // Calls visitMachineDefinition
                 if (machineNode != null) {
                     machineDefs.add(machineNode);
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
                  AstNode serviceNode = visit(elementCtx); // Visit interface or service definition
                  if (serviceNode != null) {
                      serviceDefs.add(serviceNode);
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
                    AstNode actorNode = visit(actorCtx); // Calls visitActorDefinition
                    if (actorNode != null) {
                        actorDefs.add(actorNode);
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
                     AstNode commNode = visit(commCtx); // Calls visitProtocolDefinition, etc.
                     if (commNode != null) {
                         commDefs.add(commNode);
                     }
                }
            }
            return commDefs;
       }


    // --- Implementations for individual definition visitors (Overriding BaseVisitor where appropriate) ---

    @Override
    public AstNode visitTypeDefinition(SSoTParser.TypeDefinitionContext ctx) {
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
    public AstNode visitStructDefinition(SSoTParser.StructDefinitionContext ctx) {
        // Revert to simple access, assuming ID() returns TerminalNode
        String name = "UNKNOWN_STRUCT";
        if (ctx.ID() != null) { // Check for null TerminalNode
             name = ctx.ID().getText();
        } else {
             System.err.println("Warning: No ID found for struct definition: " + ctx.getText());
        }
        System.out.println("Visiting StructDefinition: " + name);
        List<FieldNode> fields = new ArrayList<>();
        if (ctx.fieldDefinition() != null) {
            for (SSoTParser.FieldDefinitionContext fieldCtx : ctx.fieldDefinition()) {
                AstNode fieldNode = visitFieldDefinition(fieldCtx); // Use specific visitor
                if (fieldNode instanceof FieldNode) {
                    fields.add((FieldNode) fieldNode);
                }
            }
        }
        // Use the TypeDefNode constructor
         return new TypeDefNode(Optional.empty(), name, fields);
         // TODO: Revisit if TypeDefNode should store "struct" explicitly
    }

    @Override
    public AstNode visitFieldDefinition(SSoTParser.FieldDefinitionContext ctx) {
         // Grammar: ID COLON typeExpr annotation* (LBRACE annotation* RBRACE)? SEMI
         String name = ctx.ID().getText(); // Use ID()
         String type = ctx.typeExpr().getText(); // Use typeExpr()
         System.out.println("Visiting FieldDefinition: " + name + " (" + type + ")");
         // No default value expression in grammar rule. Remove that logic.
         // Constructor: FieldNode(Optional<Long> id, String name, String type)
         return new FieldNode(Optional.empty(), name, type); // Pass type, not default value
     }

     // Assuming visitEnumDefinition is needed
     @Override
     public AstNode visitEnumDefinition(SSoTParser.EnumDefinitionContext ctx) {
        System.out.println("Visiting EnumDefinition: " + ctx.ID().getText());
        // TODO: Implement EnumNode creation (needs EnumNode class)
        // Extract name ctx.ID()
        // Iterate ctx.enumVariant()
         System.err.println("Warning: visitEnumDefinition not fully implemented.");
        return null;
     }

    // --- Machine related visitors ---

    @Override
    public AstNode visitMachineDefinition(SSoTParser.MachineDefinitionContext ctx) {
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
     public AstNode visitStatesDefinition(SSoTParser.StatesDefinitionContext ctx) {
        System.out.println("Visiting StatesDefinition block...");
        // This visitor might just collect StateNodes and return a list or a wrapper node.
        // For now, just iterate and visit children. The actual collection might happen
        // within visitMachineDefinition when it encounters a statesDefinition element.
        List<AstNode> stateNodes = new ArrayList<>();
         if(ctx.stateDefinition() != null) {
            for(SSoTParser.StateDefinitionContext stateCtx : ctx.stateDefinition()){
                AstNode stateNode = visitStateDefinition(stateCtx);
                if (stateNode != null) {
                    stateNodes.add(stateNode);
                }
            }
         }
         // Returning null because the list should be processed by the caller (visitMachineDefinition)
         // Or return a dedicated StatesBlockNode if needed. Returning null for simplicity now.
         System.err.println("Warning: visitStatesDefinition returning null, caller needs to handle children.");
         return null;
     }


     @Override
     public AstNode visitStateDefinition(SSoTParser.StateDefinitionContext ctx) {
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
                    ActionNode action = new ActionNode(Optional.empty(), actionRefText); // Use constructor
                    if (entryExitCtx.ON_ENTRY() != null) {
                        entryActions.add(action);
                    } else if (entryExitCtx.ON_EXIT() != null) {
                        exitActions.add(action);
                    }
                } else if (bodyElement.onTransition() != null) {
                     // Delegate to visitOnTransition
                     AstNode transitionNode = visitOnTransition(bodyElement.onTransition());
                     if (transitionNode instanceof TransitionNode) {
                         transitions.add((TransitionNode) transitionNode);
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

         this.currentStateName = null; // Clear current state name after visiting
         return new StateNode(Optional.empty(), name, combinedActions);
     }

     // This method corresponds to the 'onTransition' rule in the grammar
     @Override
     public AstNode visitOnTransition(SSoTParser.OnTransitionContext ctx) {
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

             // Constructor: TransitionNode(Optional<Long> id, String fromState, String toState, String event, Optional<String> condition, Optional<String> action)
             // Use the stored currentStateName as fromState
             String fromState = (this.currentStateName != null) ? this.currentStateName : "UNKNOWN_SOURCE";
             return new TransitionNode(Optional.empty(), fromState, targetState, event, Optional.ofNullable(condition), action);

         } else {
             System.err.println("Warning: onTransition rule missing transitionSpec for event: " + event);
             return null;
         }
     }


     // This method handles the 'actionDefinition' rule inside an 'actions' block
     @Override
     public AstNode visitActionDefinition(SSoTParser.ActionDefinitionContext ctx) {
        String name = "UNKNOWN_ACTION";
         // Assuming ID() returns a List here based on compiler error
         if (ctx.ID() != null && !ctx.ID().isEmpty()) { // Check list not empty
            name = ctx.ID(0).getText(); // Get text from first element
         } else {
            System.err.println("Warning: No ID found for action definition: " + ctx.getText());
         }
        System.out.println("Visiting ActionDefinition: " + name);
        // TODO: Parse parameters and return type if needed for a more detailed ActionNode
        // Constructor: ActionNode(Optional<Long> id, String name)
        return new ActionNode(Optional.empty(), name);
     }

     // This method might be called by visitActionList (if used) or directly if grammar changes.
     // Let's remove @Override as 'visitAction' is likely not in BaseVisitor if the rule is 'actionDefinition'
     // public AstNode visitAction(SSoTParser.ActionContext ctx) { ... }
     // Let's assume action references are resolved elsewhere for now.

    // --- Service related visitors ---
    @Override
    public AstNode visitServiceElement(SSoTParser.ServiceElementContext ctx) {
        // Handles alternatives within servicesBlock
        if (ctx.interfaceDefinition() != null) {
            return visitInterfaceDefinition(ctx.interfaceDefinition());
        } else if (ctx.serviceDefinition() != null) {
            return visitServiceDefinition(ctx.serviceDefinition());
        }
        return null;
    }

     @Override
     public AstNode visitServiceDefinition(SSoTParser.ServiceDefinitionContext ctx) {
         String name = ctx.ID() != null ? ctx.ID().getText() : "UNKNOWN_SERVICE";
         System.out.println("Visiting ServiceDefinition (placeholder): " + name);
         // TODO: Implement based on Service AST Node (needs definition) and grammar
         System.err.println("Warning: ServiceDefinition visitor not implemented.");
         return null; // Placeholder
     }

     @Override
     public AstNode visitInterfaceDefinition(SSoTParser.InterfaceDefinitionContext ctx) {
          String name = ctx.ID() != null ? ctx.ID().getText() : "UNKNOWN_INTERFACE";
          System.out.println("Visiting InterfaceDefinition (placeholder): " + name);
          // TODO: Implement based on Interface AST Node
          System.err.println("Warning: InterfaceDefinition visitor not implemented.");
          return null;
     }

      @Override
      public AstNode visitMethodDefinition(SSoTParser.MethodDefinitionContext ctx) {
           String name = ctx.ID() != null ? ctx.ID().getText() : "UNKNOWN_METHOD";
           System.out.println("Visiting MethodDefinition (placeholder): " + name);
           // TODO: Implement based on Method AST Node
           System.err.println("Warning: MethodDefinition visitor not implemented.");
           return null;
      }

     // --- Actor related visitors ---
      @Override
      public AstNode visitActorDefinition(SSoTParser.ActorDefinitionContext ctx) {
           String name = ctx.ID() != null ? ctx.ID().getText() : "UNKNOWN_ACTOR";
           System.out.println("Visiting ActorDefinition (placeholder): " + name);
           // TODO: Implement based on Actor AST Node
           System.err.println("Warning: ActorDefinition visitor not implemented.");
           return null;
      }

     // --- Communication related visitors ---
      @Override
      public AstNode visitCommunicationDefinition(SSoTParser.CommunicationDefinitionContext ctx) {
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
      public AstNode visitProtocolDefinition(SSoTParser.ProtocolDefinitionContext ctx) {
            String name = ctx.ID() != null ? ctx.ID().getText() : "UNKNOWN_PROTOCOL";
            System.out.println("Visiting ProtocolDefinition (placeholder): " + name);
            // TODO: Implement based on Protocol AST Node
            System.err.println("Warning: ProtocolDefinition visitor not implemented.");
            return null;
      }
       @Override
       public AstNode visitChannelDefinition(SSoTParser.ChannelDefinitionContext ctx) {
            String name = ctx.ID() != null ? ctx.ID().getText() : "UNKNOWN_CHANNEL";
            System.out.println("Visiting ChannelDefinition (placeholder): " + name);
            // TODO: Implement based on Channel AST Node
            System.err.println("Warning: ChannelDefinition visitor not implemented.");
            return null;
       }
        @Override
        public AstNode visitEventDefinition(SSoTParser.EventDefinitionContext ctx) {
             String name = ctx.ID() != null ? ctx.ID().getText() : "UNKNOWN_EVENT";
             System.out.println("Visiting EventDefinition (placeholder): " + name);
             // TODO: Implement based on Event AST Node (similar to Struct?)
             System.err.println("Warning: EventDefinition visitor not implemented.");
             return null;
        }

    // ... other visit methods as needed ...

}