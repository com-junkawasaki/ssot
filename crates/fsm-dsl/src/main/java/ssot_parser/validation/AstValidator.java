package ssot_parser.validation;

import ssot_parser.ast.SsotRoot;
import ssot_parser.ast.AstNode;
import ssot_parser.ast.nodes.*;
import ssot_parser.ast.nodes.InterfaceNode;
import ssot_parser.NodeWithId;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.Optional;
import java.util.HashSet;
import ssot_parser.ast.type.*; // Import type nodes
import java.util.HashMap; // Import HashMap
import java.util.Map; // Import Map
import java.util.ArrayDeque; // Import ArrayDeque
import java.util.Deque; // Import Deque
import java.util.Objects; // Import Objects
import ssot_parser.ast.nodes.EventNode;

/**
 * Performs validation checks on the SSoT AST (Abstract Syntax Tree).
 * Includes checks like reference resolution, type consistency, state machine logic, etc.
 */
public class AstValidator {

    private static final Set<String> KNOWN_PRIMITIVE_TYPES = Set.of(
        "string", "bool", 
        "u8", "u16", "u32", "u64", 
        "i8", "i16", "i32", "i64", 
        "f32", "f64", 
        "timestamp", "void", "any", "error" // Added any, error as common primitives
    );

    private final SsotRoot astRoot;
    private final List<ValidationError> errors;
    private final Set<String> definedTypeNames; // For type reference validation
    private final Map<String, InterfaceNode> interfaceDefinitionsMap; // Add map
    private final Set<String> definedServiceNames; // For invoke source validation
    private final Set<String> definedMachineNames; // For invoke source validation
    private final Set<String> definedEventNames; // New field
    private final Set<String> definedProtocolNames; // For protocol name uniqueness
    private final Set<String> definedChannelNames;  // For channel name uniqueness
    private final Set<String> definedActorNames;    // For actor name uniqueness
    private final Map<Long, AstNode> globalSeenIds; // For global ID uniqueness
    // Add maps to store definitions for faster lookup if needed elsewhere
    // private final Map<String, TypeDefNode> typeDefinitionsMap;
    // private final Map<String, InterfaceNode> interfaceDefinitionsMap;
    // ... etc

    public AstValidator(SsotRoot astRoot) {
        this.astRoot = astRoot;
        this.errors = new ArrayList<>();
        this.definedTypeNames = new HashSet<>();
        this.interfaceDefinitionsMap = new HashMap<>(); // Initialize map
        this.definedServiceNames = new HashSet<>(); // Initialize
        this.definedMachineNames = new HashSet<>(); // Initialize
        this.definedEventNames = new HashSet<>(); // Initialize
        this.definedProtocolNames = new HashSet<>(); // Initialize
        this.definedChannelNames = new HashSet<>(); // Initialize
        this.definedActorNames = new HashSet<>();   // Initialize
        this.globalSeenIds = new HashMap<>(); // Initialize global ID map
        // this.typeDefinitionsMap = new HashMap<>();
        // this.interfaceDefinitionsMap = new HashMap<>();
    }

    /**
     * Runs all validation checks.
     *
     * @return A list of validation errors and warnings. Returns an empty list if the AST is valid.
     */
    public List<ValidationError> validate() {
        errors.clear();
        definedTypeNames.clear();
        interfaceDefinitionsMap.clear(); // Clear map
        definedServiceNames.clear(); // Clear
        definedMachineNames.clear(); // Clear
        definedEventNames.clear(); // Clear
        definedProtocolNames.clear(); // Clear
        definedChannelNames.clear(); // Clear
        definedActorNames.clear();   // Clear
        globalSeenIds.clear(); // Clear global ID map

        if (astRoot == null) {
            errors.add(new ValidationError("AST Root cannot be null.", ValidationError.Severity.ERROR));
            return Collections.unmodifiableList(errors);
        }

        // --- Pre-collect definitions for reference checks ---
        collectDefinedTypeNames();
        collectInterfaceDefinitions(); // Collect interfaces
        collectServiceAndMachineNames(); // Collect invokable names
        collectDefinedEventNames(); // Call new collection method
        collectDefinedProtocolNames();
        collectDefinedChannelNames();
        collectDefinedActorNames();

        // --- Run validation checks --- 
        validateMachines();
        validateTypes();
        validateServices();
        validateActors();
        validateCommunication();
        validateCrossCuttingConcerns(); // e.g., ID uniqueness across different blocks?

        return Collections.unmodifiableList(errors);
    }

    public List<ValidationError> getErrors() {
        return Collections.unmodifiableList(errors);
    }

    // --- Pre-collection methods ---
    private void collectDefinedTypeNames() {
        if (astRoot.getTypeDefinitions() != null) {
            for (TypeDefNode typeDef : astRoot.getTypeDefinitions()) {
                if (!definedTypeNames.add(typeDef.getName())) {
                    addError("Duplicate type definition name '" + typeDef.getName() + "'.", typeDef);
                }
            }
        }
        System.out.println("Collected defined type names: " + definedTypeNames);
    }

    private void collectInterfaceDefinitions() {
        if (astRoot.getServiceDefinitions() != null) {
            for (AstNode serviceOrInterface : astRoot.getServiceDefinitions()) {
                if (serviceOrInterface instanceof InterfaceNode) {
                    InterfaceNode interfaceNode = (InterfaceNode) serviceOrInterface;
                    if (interfaceNode.getName() == null || interfaceNode.getName().isEmpty()) {
                        addError("Interface definition has no name.", interfaceNode);
                    }
                    if (interfaceDefinitionsMap.put(interfaceNode.getName(), interfaceNode) != null) {
                        addError("Duplicate interface definition name '" + interfaceNode.getName() + "'.", interfaceNode);
                    }
                }
            }
        }
         System.out.println("Collected interface definitions: " + interfaceDefinitionsMap.keySet());
    }

    private void collectServiceAndMachineNames() {
        if (astRoot.getServiceDefinitions() != null) {
            for (AstNode serviceElement : astRoot.getServiceDefinitions()) {
                 if (!(serviceElement instanceof InterfaceNode)) {
                    if (serviceElement instanceof ServiceNode) {
                        ServiceNode service = (ServiceNode) serviceElement;
                        if (!definedServiceNames.add(service.getName())) {
                             addError("Duplicate service definition name '" + service.getName() + "'.", service);
                         }
                     } else if (serviceElement instanceof ServiceDefinitionNode) {
                        ServiceDefinitionNode service = (ServiceDefinitionNode) serviceElement;
                        if (!definedServiceNames.add(service.getName())) {
                            addError("Duplicate service definition name '" + service.getName() + "'.", service);
                        }
                     }
                 }
            }
        }
        if (astRoot.getMachineDefinitions() != null) {
             for (MachineNode machine : astRoot.getMachineDefinitions()) {
                 if (!definedMachineNames.add(machine.getMachineName())) {
                      addError("Duplicate machine definition name '" + machine.getMachineName() + "'.", machine);
                 }
             }
        }
         System.out.println("Collected service names: " + definedServiceNames);
         System.out.println("Collected machine names: " + definedMachineNames);
    }

    // New method
    private void collectDefinedEventNames() {
        if (astRoot.getCommunicationDefinitions() != null) {
            for (AstNode commNode : astRoot.getCommunicationDefinitions()) {
                if (commNode instanceof EventNode) {
                    EventNode eventNode = (EventNode) commNode;
                    if (!definedEventNames.add(eventNode.getName())) {
                        addError("Duplicate event definition name '" + eventNode.getName() + "'.", eventNode);
                    }
                }
            }
        }
        System.out.println("Collected defined event names: " + definedEventNames);
    }

    private void collectDefinedProtocolNames() {
        if (astRoot.getCommunicationDefinitions() != null) {
            for (AstNode commNode : astRoot.getCommunicationDefinitions()) {
                if (commNode instanceof ProtocolNode) {
                    ProtocolNode protocolNode = (ProtocolNode) commNode;
                    if (!definedProtocolNames.add(protocolNode.getName())) {
                        addError("Duplicate protocol definition name '" + protocolNode.getName() + "'.", protocolNode);
                    }
                }
            }
        }
        System.out.println("Collected defined protocol names: " + definedProtocolNames);
    }

    private void collectDefinedChannelNames() {
        if (astRoot.getCommunicationDefinitions() != null) {
            for (AstNode commNode : astRoot.getCommunicationDefinitions()) {
                if (commNode instanceof ChannelNode) {
                    ChannelNode channelNode = (ChannelNode) commNode;
                    if (!definedChannelNames.add(channelNode.getName())) {
                        addError("Duplicate channel definition name '" + channelNode.getName() + "'.", channelNode);
                    }
                }
            }
        }
        System.out.println("Collected defined channel names: " + definedChannelNames);
    }

    private void collectDefinedActorNames() {
        if (astRoot.getActorDefinitions() != null) {
            for (AstNode actorAstNode : astRoot.getActorDefinitions()) {
                if (actorAstNode instanceof ActorNode) {
                    ActorNode actorNode = (ActorNode) actorAstNode;
                    if (!definedActorNames.add(actorNode.getName())) {
                        addError("Duplicate actor definition name '" + actorNode.getName() + "'.", actorNode);
                    }
                } else {
                    // This case should ideally not happen if the parser ensures correct types in this list
                    addError("Invalid node type found in actor definitions list: " + actorAstNode.getClass().getSimpleName(), actorAstNode);
                }
            }
        }
        System.out.println("Collected defined actor names: " + definedActorNames);
    }

    // --- Placeholder validation methods for different blocks --- 

    private void validateMachines() {
        if (astRoot.getMachineDefinitions() != null) {
            Map<Long, AstNode> seenIds = new HashMap<>();

            for (MachineNode machine : astRoot.getMachineDefinitions()) {
                 Map<Long, AstNode> seenIdsInMachine = new HashMap<>();
                 checkAndRegisterId(machine, seenIds);
                 checkAndRegisterId(machine, seenIdsInMachine);

                 Set<String> definedActionNames = machine.getActions().stream()
                                                     .map(ActionDefinitionNode::getActionName)
                                                     .collect(Collectors.toSet());
                 Set<String> definedGuardNames = machine.getGuards().stream()
                                                    .map(GuardDefinitionNode::getGuardName)
                                                    .collect(Collectors.toSet());

                 // Context validation
                 machine.getContext().ifPresent(contextNode -> validateContext(contextNode, seenIdsInMachine));

                 validateMachineStateHierarchy(machine, null, definedActionNames, definedGuardNames, seenIdsInMachine);
                 Set<String> allDefinedStateNames = collectAllStateNames(machine);
                 validateUnreachableStates(machine, allDefinedStateNames);
            }
        }
    }

    // Updated signature and logic for hierarchy validation
    private void validateMachineStateHierarchy(MachineNode machine, StateNode parentState,
                                             Set<String> definedActionNames, Set<String> definedGuardNames,
                                             Map<Long, AstNode> seenIdsInScope) {
        String scopeName = (parentState == null) ? machine.getMachineName() : parentState.getStateName();
        System.out.println("Validating states within scope: " + scopeName);

        List<StateNode> currentLevelStates;
        if (parentState == null) {
            currentLevelStates = machine.getStates().stream()
                                     .filter(s -> s instanceof StateNode)
                                     .map(s -> (StateNode)s)
                                     .collect(Collectors.toList());
        } else {
            currentLevelStates = parentState.getNestedStates(); // Simplified: getNestedStates() returns List<StateNode>
        }

        Set<String> definedStateNamesInScope = new HashSet<>();
        // IDs are checked cumulatively within the machine scope passed down
        if(parentState != null) checkAndRegisterId(parentState, seenIdsInScope);

        // First pass: Collect names and check IDs/duplicates at this level
        for (StateNode state : currentLevelStates) {
             checkAndRegisterId(state, seenIdsInScope);
             if (!definedStateNamesInScope.add(state.getStateName())) {
                 addError("Duplicate state name '" + state.getStateName() + "' defined within scope '" + scopeName + "'.", state);
             }
             // Check IDs of children immediately within this state's definition
             state.getInvokeInvocations().forEach(inv -> checkAndRegisterId(inv, seenIdsInScope));
             state.getTransitions().forEach(t -> checkAndRegisterId(t, seenIdsInScope));
             state.getHistory().ifPresent(h -> checkAndRegisterId(h, seenIdsInScope));
        }

        // Validate initial state for the current scope
        validateInitialStateInScope(machine, parentState, currentLevelStates, definedStateNamesInScope);

        // Second pass: Validate transitions, invokes, actions for each state at this level
        Set<String> allStateNamesInMachine = collectAllStateNames(machine); // Get all state names for target validation
        for (StateNode state : currentLevelStates) {
            // Pass the machine and current parentState (which is parentOfState for 'state')
            validateStateContent(state, machine, parentState, allStateNamesInMachine, definedActionNames, definedGuardNames);

            // Recursively validate nested states
            if (state.getNestedStates() != null && !state.getNestedStates().isEmpty()) {
                 // When recursing, 'state' becomes the parentState for its children
                 validateMachineStateHierarchy(machine, state, definedActionNames, definedGuardNames, seenIdsInScope);
            }
        }
    }

    // Helper to validate content of a single state (transitions, invokes, entry/exit)
    // Added MachineNode machine, StateNode parentOfState parameters
    private void validateStateContent(StateNode state, MachineNode machine, StateNode parentOfState, Set<String> allStateNames, Set<String> definedActionNames, Set<String> definedGuardNames) {
         // Validate onEntry/onExit actions
         validateActionReferences(state.getEntryActions(), definedActionNames, "onEntry", state);
         validateActionReferences(state.getExitActions(), definedActionNames, "onExit", state);

         // Validate Invokes
         if (state.getInvokeInvocations() != null) {
            for (InvokeStateNode invokeNode : state.getInvokeInvocations()) {
                checkAndRegisterId(invokeNode, new HashMap<>()); // Check ID for invoke node itself, new scope for its internals if any
                String invokeSourceName = invokeNode.getInvokeDefinitionRef();
                if (invokeSourceName != null && !invokeSourceName.isEmpty()) {
                    boolean sourceFound = this.definedServiceNames.contains(invokeSourceName) || 
                                          this.definedMachineNames.contains(invokeSourceName);
                    if (!sourceFound) {
                        addError("Invoked source '" + invokeSourceName + "' is not a defined service or machine.", invokeNode);
                    }
                } else {
                    addError("Invoke node is missing a source name (invokeDefinitionRef).", invokeNode);
                }

                // Validate onDone and onError handlers within the invoke node
                validateInvokeCompletionHandler(invokeNode.getOnDoneHandler(), "onDone", allStateNames, definedActionNames, definedGuardNames, invokeNode);
                validateInvokeCompletionHandler(invokeNode.getOnErrorHandler(), "onError", allStateNames, definedActionNames, definedGuardNames, invokeNode);
            }
        }

         // Validate Transitions
         if (state.getTransitions() != null) {
             for (TransitionNode transition : state.getTransitions()) {
                // Validate event
                String eventName = transition.getEvent();
                // Only validate event name for actual EVENT type transitions
                if (transition.getType() == TransitionNode.TransitionType.EVENT) {
                    if (eventName != null && !eventName.isEmpty()) {
                        if (!this.definedEventNames.contains(eventName)) {
                            addError("Event '" + eventName + "' referenced in transition from state '" + state.getStateName() + "' is not defined.", transition);
                        }
                    } else { // eventName is null or empty for an EVENT type transition
                         addError("EVENT type transition from state '" + state.getStateName() + "' has a null or empty event name.", transition);
                    }
                }

                 // Validate target state existence (against all states in the machine)
                 TargetStateNode targetNode = transition.getTargetState();
                 if (targetNode != null && targetNode.getType() == TargetStateNode.TargetType.STATE_REFERENCE) {
                     String targetName = targetNode.getStateName().orElse(null);
                     boolean isValidTarget = false;

                     if (targetName != null && !targetName.isEmpty()) {
                         if (allStateNames.contains(targetName)) { // Check if it's a regular defined state
                             isValidTarget = true;
                         } else if (targetName.equals("H")) { // Convention for history pseudo-state
                             boolean historyFound = false;
                             if (parentOfState != null) { // 'state' is a nested state, check its parent for history
                                 if (parentOfState.getHistory().isPresent()) {
                                     historyFound = true;
                                 }
                             } else { // 'state' is a top-level state in the machine, check machine for top-level history
                                 // AstBuilderVisitor adds HistoryStateNode to the machine's list of states.
                                 if (machine.getStates().stream().anyMatch(s -> s instanceof HistoryStateNode)) {
                                     historyFound = true;
                                 }
                             }
                             if (historyFound) {
                                 isValidTarget = true;
                             }
                         }
                     }

                     if (!isValidTarget) {
                         addError("Transition target state '" + (targetName != null ? targetName : "<empty_or_null>") + "' for event '" + transition.getEvent() + "' from state '" + state.getStateName() + "' is not defined or empty.", transition);
                     }
                 } else if (targetNode == null) {
                     addError("Transition target state is null for event '" + transition.getEvent() + "' from state '" + state.getStateName() + "'.", transition);
                 }
                 // History targets (".history") are handled differently, usually validated by structure not by name in allStateNames.

                 // Validate action/guard references
                 transition.getAction().ifPresent(actionRefNode -> {
                     if (!definedActionNames.contains(actionRefNode.getActionName())) {
                         addError("Transition action '" + actionRefNode.getActionName() + "' is not defined.", transition);
                     }
                 });
                 transition.getCondition().ifPresent(guardRefNode -> {
                     if (!definedGuardNames.contains(guardRefNode.getGuardName())) {
                         addError("Transition guard '" + guardRefNode.getGuardName() + "' in state '" + state.getStateName() + "' is not defined in machine.", transition);
                     }
                 });
             }
         }

         // New: Validate transition consistency (check for duplicate events without distinct guards)
         if (state.getTransitions() != null && state.getTransitions().size() > 1) {
             Map<String, List<TransitionNode>> transitionsByEvent = state.getTransitions().stream()
                 .filter(t -> t.getType() == TransitionNode.TransitionType.EVENT && t.getEvent() != null && !t.getEvent().isEmpty())
                 .collect(Collectors.groupingBy(TransitionNode::getEvent));

             for (Map.Entry<String, List<TransitionNode>> entry : transitionsByEvent.entrySet()) {
                 String eventName = entry.getKey();
                 List<TransitionNode> eventTransitions = entry.getValue();

                 if (eventTransitions.size() > 1) {
                     // Found multiple transitions for the same event
                     boolean allGuarded = true;
                     List<TransitionNode> unguardedTransitions = new ArrayList<>();
                     for (TransitionNode etn : eventTransitions) {
                         boolean isGuarded = etn.getCondition().isPresent() || (etn.getGuards() != null && !etn.getGuards().isEmpty());
                         if (!isGuarded) {
                             allGuarded = false;
                             unguardedTransitions.add(etn);
                         }
                     }

                     if (!allGuarded) {
                         // If not all are guarded, it means at least one (or more) is unconditional for this event.
                         // If there's more than one unconditional, or one unconditional and some conditional, it's ambiguous.
                         // For simplicity, if >1 transition for an event, and ANY are unguarded, flag it.
                         // More precise: if count(unguarded) > 1, or if count(unguarded)==1 AND count(total for event) > 1
                         // The current logic: if eventTransitions.size() > 1 AND any is unguarded, then error.
                         addError("State '" + state.getStateName() + "' has multiple transitions for event '" + eventName + "', and at least one is not guarded, causing ambiguity.",
                                  unguardedTransitions.isEmpty() ? state : unguardedTransitions.get(0)); // Report error on state or first unguarded transition
                     }
                 }
             }
         }

        // New: Validations specific to FINAL states
        if (state.getType() == StateType.FINAL) {
            if (state.getTransitions() != null && !state.getTransitions().isEmpty()) {
                addError("Final state '" + state.getStateName() + "' must not have any outgoing transitions.", state);
            }
            if (state.getInvokeInvocations() != null && !state.getInvokeInvocations().isEmpty()) {
                addError("Final state '" + state.getStateName() + "' must not have invoke definitions.", state);
            }
            if (state.getInitialStateName().isPresent()) {
                addError("Final state '" + state.getStateName() + "' cannot define an initial nested state.", state);
            }
            if (state.getNestedStates() != null && !state.getNestedStates().isEmpty()) {
                addError("Final state '" + state.getStateName() + "' cannot have nested states.", state);
            }
            // Note: onEntry might be permissible for a final action. onExit is less likely to be useful.
            // For now, not adding errors for onEntry/onExit on final states.
        }
    }

     // Helper to collect all state names recursively
     private Set<String> collectAllStateNames(MachineNode machine) {
         Set<String> names = new HashSet<>();
         collectStateNamesRecursive(
             machine.getStates().stream()
                 .filter(StateNode.class::isInstance)
                 .map(StateNode.class::cast)
                 .collect(Collectors.toList()), 
             names
         );
         return names;
     }

     private void collectStateNamesRecursive(List<StateNode> states, Set<String> names) {
         if (states == null) return;
         for (StateNode state : states) {
             names.add(state.getStateName());
             if (state.getNestedStates() != null && !state.getNestedStates().isEmpty()) {
                 collectStateNamesRecursive(state.getNestedStates(), names);
             }
         }
     }

     // Helper to validate initial state within a scope
     private void validateInitialStateInScope(MachineNode machine, StateNode parentState, List<StateNode> currentLevelStates, Set<String> definedStateNamesInScope) {
          if (parentState != null) { // Validating initial state for a compound/parallel state
              // Ensure currentLevelStates accurately reflects children of parentState that are StateNodes
              // definedStateNamesInScope should also contain names of these currentLevelStates

              if (parentState.getNestedStates().isEmpty()) {
                  // A state that is declared as COMPOUND or PARALLEL should have nested states.
                  // ATOMIC and FINAL states are expected to have no nested states.
                  if (parentState.getType() == StateType.COMPOUND || parentState.getType() == StateType.PARALLEL) {
                     addError("State '" + parentState.getStateName() + "' is defined as " + parentState.getType() + " but has no nested states.", parentState);
                  }
                  // No initial state validation needed if no nested states (e.g., for ATOMIC, FINAL, or empty COMPOUND/PARALLEL which is an error itself)
              } else { // parentState has nested states defined
                  Optional<String> explicitInitialStateNameOpt = parentState.getInitialStateName();

                  if (explicitInitialStateNameOpt.isPresent()) {
                      String explicitInitialName = explicitInitialStateNameOpt.get();
                      if (!definedStateNamesInScope.contains(explicitInitialName)) {
                          addError("Explicitly defined initial nested state '$initial(" + explicitInitialName + ")' for state '" + parentState.getStateName() + "' refers to a non-existent or invalid nested state.", parentState);
                      }
                      // If present and found in definedStateNamesInScope, it's valid.
                  } else {
                      // No explicit $initial annotation was on the parentState.
                      // Default to the first state in the current level's definition order.
                      if (!currentLevelStates.isEmpty()) {
                          addWarning("No explicit initial state (e.g., $initial(ChildState)) defined for " + parentState.getType() + " state '" + parentState.getStateName() + "'. Defaulting to first nested state: '" + currentLevelStates.get(0).getStateName() + "'.", parentState);
                      } else {
                          // This case should ideally not be reached if parentState.getNestedStates() was not empty.
                          // It implies currentLevelStates might be empty due to filtering or other issues.
                          addError("State '" + parentState.getStateName() + "' has nested state definitions, but no valid first state could be determined for default initial state.", parentState);
                      }
                  }
              }
          } else {
              // Top-level machine: Check machine's initial state
              if (machine.getInitialState().isPresent()) {
                  String initialStateName = machine.getInitialState().get();
                  if (!definedStateNamesInScope.contains(initialStateName)) {
                      addError("Initial state '" + initialStateName + "' for machine '" + machine.getMachineName() + "' is not defined at the top level.", machine);
                  }
              } else { // No explicit $initial on machine
                   if (currentLevelStates.isEmpty()) {
                       addError("Machine '" + machine.getMachineName() + "' has no states defined.", machine);
                   } else {
                       addWarning("No explicit initial state (e.g. $initial(SomeState)) defined for machine '" + machine.getMachineName() + "'. Defaulting to first state: '" + currentLevelStates.get(0).getStateName() + "'.", machine);
                   }
              }
          }
     }

    // --- Context Validation ---
    private void validateContext(ContextNode node, Map<Long, AstNode> seenIdsInScope) { // Pass scope IDs
        if (node == null) return;
        System.out.println("Validating machine context...");
        Set<String> fieldNames = new HashSet<>();
        // Map<Long, AstNode> seenFieldIds = new HashMap<>(); // Use passed scope
        checkAndRegisterId(node, seenIdsInScope); // Check context block ID itself?

        if (node.getVariables() != null) {
            for (ContextVariableNode field : node.getVariables()) {
                checkAndRegisterId(field, seenIdsInScope);
                if (!fieldNames.add(field.getVariableName())) {
                    addError("Duplicate context variable name '" + field.getVariableName() + "' defined.", field);
                }
                validateTypeReference(field.getType(), field); // Validate type ref as well
            }
        }
    }

    // --- State Machine Specific Validations ---

    private void validateUnreachableStates(MachineNode machine, Set<String> definedStateNames) {
        if (machine.getStates().isEmpty()) return;

        Set<String> reachableStates = new HashSet<>();
        Deque<String> queue = new ArrayDeque<>();

        // Start with the initial state of the machine
        String initialMachineState = machine.getInitialStateName();
        if (initialMachineState != null && definedStateNames.contains(initialMachineState)) {
            queue.add(initialMachineState);
            reachableStates.add(initialMachineState);
        } else if (initialMachineState == null && !machine.getStates().isEmpty()) {
            // This case should ideally be caught by initial state validation, but as a fallback:
            AstNode firstNode = machine.getStates().get(0);
            if (firstNode instanceof StateNode) {
                String firstDefinedState = ((StateNode) firstNode).getStateName();
                queue.add(firstDefinedState);
                reachableStates.add(firstDefinedState);
                addWarning("Machine '" + machine.getMachineName() + "' has no explicit initial state for reachability analysis. Starting with first defined state: " + firstDefinedState, machine);
            } else {
                // Handle cases where the first node isn't a StateNode, though less likely for a valid machine
                addError("Machine '" + machine.getMachineName() + "' has no explicit initial state and the first defined element is not a state. Cannot perform reachability analysis.", machine);
                return;
            }
        } else if (initialMachineState != null) {
             addError("Initial state '" + initialMachineState + "' for machine '" + machine.getMachineName() + "' is not defined. Cannot perform reachability analysis.", machine);
             return;
        }


        Map<String, StateNode> stateMap = machine.getStates().stream()
                .filter(StateNode.class::isInstance)
                .map(StateNode.class::cast)
                .collect(Collectors.toMap(StateNode::getStateName, s -> s, (s1, s2) -> {
                    addWarning("Duplicate state name '" + s1.getStateName() + "' found when building state map for reachability. Check for issues in state name collection or definition.", s1);
                    return s1; // Keep the first encountered
                }));

        while (!queue.isEmpty()) {
            String currentStateName = queue.remove();
            StateNode currentState = stateMap.get(currentStateName);

            if (currentState == null) continue; // Should not happen if definedStateNames is accurate

            // Check transitions from the current state
            for (TransitionNode transition : currentState.getTransitions()) {
                String targetStateName = transition.getTargetState().getStateName().orElse(null);
                if (targetStateName != null && definedStateNames.contains(targetStateName) && reachableStates.add(targetStateName)) {
                    queue.add(targetStateName);
                }
            }

            // Check transitions from invoke handlers
            currentState.getInvoke().ifPresent(invoke -> {
                checkAndEnqueueTargetFromHandler(invoke.getOnDoneHandler(), definedStateNames, reachableStates, queue);
                checkAndEnqueueTargetFromHandler(invoke.getOnErrorHandler(), definedStateNames, reachableStates, queue);
            });
            
            // Check transitions from history state defaults
            currentState.getHistory().ifPresent(historyNode -> {
                historyNode.getDefaultTransition().ifPresent(transitionSpec -> {
                    TargetStateNode targetNode = transitionSpec.getTargetState();
                    // Only consider named state targets for reachability from history default
                    if (targetNode != null && targetNode.getType() == TargetStateNode.TargetType.STATE_REFERENCE) {
                        targetNode.getStateName().ifPresent(targetName -> {
                            if (definedStateNames.contains(targetName) && reachableStates.add(targetName)) {
                                queue.add(targetName);
                            }
                        });
                    }
                });
            });
        }

        for (String stateName : definedStateNames) {
            if (!reachableStates.contains(stateName)) {
                // Find the StateNode to attach the warning to
                StateNode unreachableStateNode = stateMap.get(stateName);
                if (unreachableStateNode != null) {
                    addWarning("State '" + stateName + "' in machine '" + machine.getMachineName() + "' is unreachable.", unreachableStateNode);
                } else {
                    // Should not happen if stateMap is built correctly from definedStateNames
                    addWarning("State '" + stateName + "' in machine '" + machine.getMachineName() + "' is unreachable (node not found for warning attachment).", machine);
                }
            }
        }
    }

    private void checkAndEnqueueTargetFromHandler(Optional<InvokeCompletionHandlerNode> handlerOpt, Set<String> definedStates, Set<String> reachableStates, Deque<String> queue) {
        if (handlerOpt.isPresent()) {
            InvokeCompletionHandlerNode handler = handlerOpt.get();
            handler.getTransitionSpec().ifPresent(spec -> {
                TargetStateNode targetNode = spec.getTargetState();
                if (targetNode != null && targetNode.getStateName().isPresent()) {
                    String targetName = targetNode.getStateName().get();
                    if (definedStates.contains(targetName) && reachableStates.add(targetName)) {
                        queue.add(targetName);
                    }
                } else if (targetNode != null && targetNode.getType() == TargetStateNode.TargetType.HISTORY_REFERENCE) {
                    // TODO: How to handle history targets in reachability? For now, assume they are valid if present.
                    // String owningStateName = getNodeName(handler); // This is problematic, handler doesn't have a direct parent state name
                    // Need a way to get the context of the current state for '.history'
                }
            });
        }
    }

    private void validateActionReferences(List<String> actionNames, Set<String> definedActionNames, String context, AstNode ownerNode) {
        if (actionNames == null) return;
        for (String actionName : actionNames) {
            if (!definedActionNames.contains(actionName)) {
                addError("Action '" + actionName + "' referenced in " + context + " of '" + getNodeName(ownerNode) + "' is not defined in the current machine scope.", ownerNode);
            }
        }
    }

    private void validateInvokeCompletionHandler(Optional<InvokeCompletionHandlerNode> handlerOpt, String handlerType, Set<String> definedStateNames, Set<String> definedActionNames, Set<String> definedGuardNames, InvokeStateNode invokeNode) {
        handlerOpt.ifPresent(handler -> {
            checkAndRegisterId(handler, new HashMap<>()); // Check ID for handler node itself
            
            // Validate target state, actions, and guard from TransitionSpecNode
            handler.getTransitionSpec().ifPresent(spec -> {
                // Validate target state if present in spec
                TargetStateNode targetNode = spec.getTargetState(); // Assuming TransitionSpecNode has getTargetState() -> TargetStateNode
                if (targetNode != null && targetNode.getType() == TargetStateNode.TargetType.STATE_REFERENCE) {
                    targetNode.getStateName().ifPresent(targetStateName -> {
                        if (!definedStateNames.contains(targetStateName)) {
                            addError(handlerType + " handler for invoke '" + invokeNode.getInvokeDefinitionRef() + "' targets non-existent state '" + targetStateName + "'.", handler);
                        }
                    });
                } else if (targetNode == null && spec.getActions().isEmpty()) { // Only error if target is null AND no actions (it might be an action-only handler)
                     addError(handlerType + " handler for invoke '" + invokeNode.getInvokeDefinitionRef() + "' has no target state and no actions.", handler);
                }


                // Validate guards in the transition spec
                List<GuardReferenceNode> guardNodes = spec.getGuards(); // Corrected: getGuards() returns List<GuardReferenceNode>
                if (guardNodes != null) {
                    for (GuardReferenceNode guardRefNode : guardNodes) {
                        String guardName = guardRefNode.getGuardName(); // Assuming GuardReferenceNode has getGuardName()
                        if (!definedGuardNames.contains(guardName)) {
                            addError(handlerType + " handler guard '" + guardName + "' for invoke '" + invokeNode.getInvokeDefinitionRef() + "' is not a defined guard.", handler);
                        }
                    }
                }

                // Validate actions specified in the transition spec itself
                // These are different from handler.getActions() which are at the handler's top level.
                List<String> specActionNames = spec.getActions().stream() // Assuming TransitionSpecNode has getActions() -> List<ActionReferenceNode>
                                                   .map(ActionReferenceNode::getActionName)
                                                   .collect(Collectors.toList());
                validateActionReferences(specActionNames, definedActionNames, handlerType + " handler transition spec for invoke '" + invokeNode.getInvokeDefinitionRef() + "'", handler);
            });

            // Validate top-level actions directly on the handler (these execute regardless of the transition spec's guard)
            List<String> handlerActionNames = handler.getActions().stream()
                                                     .map(ActionReferenceNode::getActionName)
                                                     .collect(Collectors.toList());
            validateActionReferences(handlerActionNames, definedActionNames, handlerType + " handler for invoke '" + invokeNode.getInvokeDefinitionRef() + "'", handler);
        });
    }

    private void validateTypes() {
        if (astRoot.getTypeDefinitions() != null) {
            for (TypeDefNode typeDef : astRoot.getTypeDefinitions()) {
                checkAndRegisterId(typeDef, new HashMap<>());
                 if (typeDef.getKind() == TypeDefNode.TypeKind.ENUM) {
                     validateEnum(typeDef);
                 } else if (typeDef.getKind() == TypeDefNode.TypeKind.STRUCT) {
                     validateStruct(typeDef);
                 } else {
                     addError("TypeDefNode '" + typeDef.getName() + "' has unknown kind: " + typeDef.getKind(), typeDef);
                 }
            }
        }
    }
    private void validateEnum(TypeDefNode node) {
        System.out.println("Validating enum: " + node.getName());
        Set<String> variantNames = new HashSet<>();
        Map<Long, AstNode> seenVariantIds = new HashMap<>(); // IDs within enum variants
        checkAndRegisterId(node, seenVariantIds); // Check enum ID itself (relative to inner scope?)

        if (node.getVariants() != null) {
            for (EnumVariantNode variant : node.getVariants()) {
                checkAndRegisterId(variant, seenVariantIds);
                if (!variantNames.add(variant.getName())) {
                    addError("Duplicate enum variant name '" + variant.getName() + "' defined in enum '" + node.getName() + "'.", variant);
                }
            }
        }
    }
    private void validateStruct(TypeDefNode node) {
        System.out.println("Validating struct: " + node.getName());
        Set<String> fieldNames = new HashSet<>();
        Map<Long, AstNode> seenFieldIds = new HashMap<>(); // IDs within struct fields
        checkAndRegisterId(node, seenFieldIds); // Check struct ID itself (relative to inner scope?)

        if (node.getFields() != null) {
            for (FieldNode field : node.getFields()) {
                checkAndRegisterId(field, seenFieldIds);
                if (!fieldNames.add(field.getName())) {
                    addError("Duplicate struct field name '" + field.getName() + "' defined.", field);
                }
                validateTypeReference(field.getType(), field);
            }
        }
    }

    private void validateServices() {
        if (astRoot.getServiceDefinitions() == null) return;
        Map<Long, AstNode> seenIds = new HashMap<>();

        for (AstNode node : astRoot.getServiceDefinitions()) {
            checkAndRegisterId(node, seenIds);
            if (node instanceof ServiceNode service) {
                 validateService(service, interfaceDefinitionsMap);
             } else if (node instanceof InterfaceNode iface) {
                 validateInterface(iface);
             } else {
                 addError("Invalid node type found in services block: " + node.getClass().getSimpleName(), node);
             }
        }
    }
    private void validateService(ServiceNode node, Map<String, InterfaceNode> definedInterfaces) {
        System.out.println("Validating service: " + node.getName());
        Map<Long, AstNode> seenMethodIds = new HashMap<>();
        checkAndRegisterId(node, seenMethodIds);

        // Collect methods defined directly in the service for faster lookup and duplicate check
        Map<String, MethodNode> serviceMethods = new HashMap<>();
         if (node.getMethods() != null) {
             for (MethodNode method : node.getMethods()) {
                 checkAndRegisterId(method, seenMethodIds);
                 // Check for duplicates among directly defined methods
                 if (serviceMethods.put(method.getName(), method) != null) {
                     addError("Duplicate method name '" + method.getName() + "' directly defined in service '" + node.getName() + "'.", method);
                 }
                 // Validate parameter/return types
                 if (method.getParameters() != null) {
                     for (ParameterNode param : method.getParameters()) {
                         checkAndRegisterId(param, seenMethodIds);
                         validateTypeReference(param.getType(), param);
                     }
                 }
                 method.getReturnType().ifPresent(rt -> validateTypeReference(rt, method));
             }
         }


        // Check implemented interfaces
        if (node.getImplementedInterfaces() != null) {
             for (String interfaceName : node.getImplementedInterfaces()) {
                 InterfaceNode implementedInterface = definedInterfaces.get(interfaceName);
                 if (implementedInterface == null) {
                     addError("Service '" + node.getName() + "' implements undefined interface '" + interfaceName + "'.", node);
                 } else {
                     // Check if all methods from the interface are implemented correctly
                     if (implementedInterface.getMethods() != null) {
                        for (MethodNode interfaceMethod : implementedInterface.getMethods()) {
                            MethodNode serviceMethod = serviceMethods.get(interfaceMethod.getName());
                            if (serviceMethod == null) {
                                 addError("Service '" + node.getName() + "' is missing implementation for method '" + interfaceMethod.getName() + "' from interface '" + interfaceName + "'.", node);
                            } else {
                                 // Check signature match (parameter types and return type)
                                 if (!compareParameterLists(interfaceMethod.getParameters(), serviceMethod.getParameters())) {
                                     addError("Method signature mismatch for '" + interfaceMethod.getName() + "' in service '" + node.getName() + "'. Parameter types do not match interface '" + interfaceName + "'.", serviceMethod);
                                 }
                                 if (!compareReturnTypes(interfaceMethod.getReturnType(), serviceMethod.getReturnType())) {
                                      addError("Method signature mismatch for '" + interfaceMethod.getName() + "' in service '" + node.getName() + "'. Return type does not match interface '" + interfaceName + "'.", serviceMethod);
                                 }
                            }
                        }
                     }
                 }
             }
        }
    }
    private void validateInterface(InterfaceNode node) {
        System.out.println("Validating interface: " + node.getName());
        Set<String> methodNames = new HashSet<>();
        Map<Long, AstNode> seenMethodIds = new HashMap<>(); // IDs within interface methods/params
        checkAndRegisterId(node, seenMethodIds);

        if (node.getMethods() != null) {
            for (MethodNode method : node.getMethods()) {
                checkAndRegisterId(method, seenMethodIds);
                if (!methodNames.add(method.getName())) {
                    addError("Duplicate method name '" + method.getName() + "' in interface '" + node.getName() + "'.", method);
                }
                 if (method.getParameters() != null) {
                     for (ParameterNode param : method.getParameters()) {
                         checkAndRegisterId(param, seenMethodIds);
                         validateTypeReference(param.getType(), param);
                     }
                 }
                 method.getReturnType().ifPresent(returnType -> validateTypeReference(returnType, method));
            }
        }
    }

    private void validateActors() {
        if (astRoot.getActorDefinitions() == null) return;
         Map<Long, AstNode> seenIds = new HashMap<>(); // Track IDs within this block
        for (AstNode node : astRoot.getActorDefinitions()) {
            checkAndRegisterId(node, seenIds);
            if (!(node instanceof ActorNode)) {
                 addError("Invalid node type found in actors block: " + node.getClass().getSimpleName(), node);
            }
            // TODO: Validate actor definitions further if needed
        }
    }

    private void validateCommunication() {
        if (astRoot.getCommunicationDefinitions() == null) return;
        Map<Long, AstNode> seenIds = new HashMap<>(); // Track IDs within this block
        for (AstNode node : astRoot.getCommunicationDefinitions()) {
            checkAndRegisterId(node, seenIds);
            if (node instanceof EventNode eventNode) {
                validateEvent(eventNode); // Add specific event validation if needed
            } else if (!(node instanceof ProtocolNode || node instanceof ChannelNode)) {
                 addError("Invalid node type found in communication block: " + node.getClass().getSimpleName(), node);
            }
            // TODO: Validate protocol/channel definitions further if needed
        }
    }
     private void validateEvent(EventNode node) {
         System.out.println("Validating event: " + node.getName());
         Set<String> fieldNames = new HashSet<>();
         Map<Long, AstNode> seenFieldIds = new HashMap<>(); // IDs within event fields
         checkAndRegisterId(node, seenFieldIds); // Check event ID itself (relative to inner scope?)

         if (node.getFields() != null) {
             for (FieldNode field : node.getFields()) {
                 checkAndRegisterId(field, seenFieldIds);
                 if (!fieldNames.add(field.getName())) {
                     addError("Duplicate event field name '" + field.getName() + "' defined.", field);
                 }
                 validateTypeReference(field.getType(), field);
             }
         }
     }

    private void validateCrossCuttingConcerns() {
        // --- Global ID Uniqueness Check ---
        // Types
        if (astRoot.getTypeDefinitions() != null) {
            for (TypeDefNode typeDef : astRoot.getTypeDefinitions()) {
                checkAndRegisterGlobalId(typeDef);
            }
        }
        // Services (Interfaces and ServiceDefinitions)
        if (astRoot.getServiceDefinitions() != null) {
            for (AstNode serviceOrInterface : astRoot.getServiceDefinitions()) {
                 if (serviceOrInterface instanceof ServiceDefinitionNode) {
                      checkAndRegisterGlobalId((ServiceDefinitionNode)serviceOrInterface);
                 } else if (serviceOrInterface instanceof InterfaceNode) {
                     checkAndRegisterGlobalId((InterfaceNode)serviceOrInterface);
                 }
            }
        }
        // Machines
        if (astRoot.getMachineDefinitions() != null) {
            for (MachineNode machine : astRoot.getMachineDefinitions()) {
                checkAndRegisterGlobalId(machine);
                // IDs within machines (states, transitions, etc.) are handled by checkAndRegisterId
                // with a scoped map. We are primarily concerned with top-level @id annotations here.
                // However, if @id can appear on nested elements and needs to be globally unique,
                // those checks would also need to use or contribute to globalSeenIds.
                // For now, assuming @id on MachineNode itself needs to be globally unique.
            }
        }
        // Actors
        if (astRoot.getActorDefinitions() != null) { // getActorDefinitions() returns List<AstNode>
            for (AstNode actorNode : astRoot.getActorDefinitions()) { // Corrected loop variable to AstNode
                checkAndRegisterGlobalId(actorNode);
            }
        }
        // Communication (Protocols, Channels, Events)
        if (astRoot.getCommunicationDefinitions() != null) { // getCommunicationDefinitions() returns List<AstNode>
            for (AstNode commElement : astRoot.getCommunicationDefinitions()) { // Corrected loop variable to AstNode
                checkAndRegisterGlobalId(commElement); // Covers ProtocolNode, ChannelNode, EventNode
            }
        }

        // Add other cross-cutting concerns here, e.g.:
        // - Name collision checks between different types of definitions if necessary
        // - Overall complexity metrics, etc.
    }

    /**
     * Checks if the node has an ID and if it's already registered in the global ID map.
     * Adds an error if the ID is duplicated.
     * This method is for global ID checks. For scoped ID checks (e.g., within a machine),
     * use {@code checkAndRegisterId(AstNode node, Map<Long, AstNode> seenIds)}.
     *
     * @param node The AST node to check.
     */
    private void checkAndRegisterGlobalId(AstNode node) {
        if (node instanceof NodeWithId) {
            NodeWithId nodeWithId = (NodeWithId) node;
            Optional<Long> idOpt = nodeWithId.getId();

            if (idOpt.isPresent()) {
                Long id = idOpt.get();
                if (globalSeenIds.containsKey(id)) {
                    AstNode existingNode = globalSeenIds.get(id);
                    addError("Duplicate global ID #" + id + " used by " + getNodeName(node) +
                             ". It was already defined by " + getNodeName(existingNode) +
                             " at " + getNodeLocation(existingNode) + ".", node);
                } else {
                    globalSeenIds.put(id, node);
                }
            }
        }
    }

    // Helper to check and register @id
    private void checkAndRegisterId(AstNode node, Map<Long, AstNode> seenIds) {
        if (node instanceof NodeWithId) {
            NodeWithId nodeWithId = (NodeWithId) node;
            Optional<Long> idOpt = nodeWithId.getId();
            if (idOpt.isPresent()) {
                Long id = idOpt.get();
                if (seenIds.containsKey(id)) {
                    AstNode existingNode = seenIds.get(id);
                    addError("Duplicate ID #" + id + " found. Previously defined by '" +
                                    getNodeName(existingNode) + "' (" + existingNode.getClass().getSimpleName() + ") at " + getNodeLocation(existingNode) +
                                    ". Now seen on '" + getNodeName(node) + "' (" + node.getClass().getSimpleName() +").",
                             node);
                } else {
                    seenIds.put(id, node);
                }
            }
        }
        // Recursively check children if applicable, or delegate to specific validators
    }

    // Helper method to add an error
    private void addError(String message, AstNode node) {
        errors.add(new ValidationError(message + " (at " + getNodeLocation(node) + ")", ValidationError.Severity.ERROR));
    }

    // Helper method to add a warning
    private void addWarning(String message, AstNode node) {
        errors.add(new ValidationError(message + " (at " + getNodeLocation(node) + ")", ValidationError.Severity.WARNING));
    }

    private String getNodeLocation(AstNode node) {
        if (node != null) {
            if (node instanceof NodeWithId && ((NodeWithId) node).getId().isPresent()) {
                 return node.getClass().getSimpleName() + "[id=" + ((NodeWithId) node).getId().get() + "]";
            }
            return node.getClass().getSimpleName();
        }
        return "unknown_location";
    }

    // Helper method to recursively validate type expressions
    private void validateTypeReference(TypeExprNode typeNode, AstNode ownerNode) {
        if (typeNode == null) {
            addError("Type expression is missing or could not be parsed.", ownerNode);
            return;
        }

        if (typeNode instanceof ReferenceTypeNode refNode) {
            String typeName = refNode.getReferencedTypeName();
            if (!definedTypeNames.contains(typeName)) {
                 // Check if it's a known primitive before declaring error
                 if (!KNOWN_PRIMITIVE_TYPES.contains(typeName.toLowerCase())) { // Use constant and toLowerCase for robustness
                    addError("Referenced type '" + typeName + "' is not defined and not a known primitive type.", ownerNode != null ? ownerNode : typeNode);
                 }
            }
        } else if (typeNode instanceof OptionalTypeNode optNode) {
            validateTypeReference(optNode.getInnerType(), ownerNode); // Validate inner type
        } else if (typeNode instanceof ListTypeNode listNode) {
            validateTypeReference(listNode.getElementType(), ownerNode); // Validate element type
        } else if (typeNode instanceof MapTypeNode mapNode) {
            validateTypeReference(mapNode.getKeyType(), ownerNode); // Validate key type
            validateTypeReference(mapNode.getValueType(), ownerNode); // Validate value type
        } else if (typeNode instanceof PrimitiveTypeNode primNode) {
            // Optionally validate if primitive type name is known/allowed
             if (!KNOWN_PRIMITIVE_TYPES.contains(primNode.getTypeName().toLowerCase())) { // Use constant and toLowerCase
                  addWarning("Unknown primitive type name '" + primNode.getTypeName() + "'.", ownerNode != null ? ownerNode : typeNode);
             }
        }
        // No action needed for PrimitiveTypeNode if any string is allowed
    }

    // --- Helper methods for signature comparison ---
    private boolean compareParameterLists(List<ParameterNode> params1, List<ParameterNode> params2) {
        if (params1.size() != params2.size()) {
            return false;
        }
        for (int i = 0; i < params1.size(); i++) {
            if (!Objects.equals(params1.get(i).getType(), params2.get(i).getType())) {
                return false;
            }
        }
        return true;
    }

    private boolean compareReturnTypes(Optional<TypeExprNode> type1, Optional<TypeExprNode> type2) {
        return Objects.equals(type1, type2);
    }

    private String getNodeName(AstNode node) {
        if (node instanceof StateNode) return ((StateNode) node).getStateName();
        if (node instanceof MachineNode) return ((MachineNode) node).getMachineName();
        if (node instanceof TypeDefNode) return ((TypeDefNode) node).getName();
        // Add more node types as needed
        return node.getClass().getSimpleName();
    }
} 