package ssot_parser.validation;

import ssot_parser.ast.SsotRoot;
import ssot_parser.ast.nodes.*;
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

/**
 * Performs validation checks on the SSoT AST (Abstract Syntax Tree).
 * Includes checks like reference resolution, type consistency, state machine logic, etc.
 */
public class AstValidator {

    private final SsotRoot astRoot;
    private final List<ValidationError> errors;
    private final Set<String> definedTypeNames; // For type reference validation
    private final Map<String, InterfaceNode> interfaceDefinitionsMap; // Add map
    private final Set<String> definedServiceNames; // For invoke source validation
    private final Set<String> definedMachineNames; // For invoke source validation
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

        if (astRoot == null) {
            errors.add(new ValidationError("AST Root cannot be null.", ValidationError.Severity.ERROR));
            return Collections.unmodifiableList(errors);
        }

        // --- Pre-collect definitions for reference checks ---
        collectDefinedTypeNames();
        collectInterfaceDefinitions(); // Collect interfaces
        collectServiceAndMachineNames(); // Collect invokable names

        // --- Run validation checks --- 
        validateMachines();
        validateTypes();
        validateServices();
        validateActors();
        validateCommunication();
        validateCrossCuttingConcerns(); // e.g., ID uniqueness across different blocks?

        return Collections.unmodifiableList(errors);
    }

    // --- Pre-collection methods ---
    private void collectDefinedTypeNames() {
        if (astRoot.getTypeDefinitions() != null) {
            for (AstNode node : astRoot.getTypeDefinitions()) {
                if (node instanceof TypeDefNode typeDef) {
                    // Assuming TypeDefNode provides getName()
                    if (!definedTypeNames.add(typeDef.getName())) {
                        // This check might be better placed in validateTypes to point to the duplicate node
                        addError("Duplicate type definition name '" + typeDef.getName() + "'.", typeDef);
                    }
                    // Optionally populate a map: typeDefinitionsMap.put(typeDef.getName(), typeDef);
                }
            }
        }
        System.out.println("Collected defined type names: " + definedTypeNames);
    }

    private void collectInterfaceDefinitions() {
        if (astRoot.getServiceDefinitions() != null) {
            for (AstNode node : astRoot.getServiceDefinitions()) {
                if (node instanceof InterfaceNode iface) {
                    if (interfaceDefinitionsMap.put(iface.getName(), iface) != null) {
                        addError("Duplicate interface definition name '" + iface.getName() + "'.", iface);
                    }
                }
            }
        }
         System.out.println("Collected interface definitions: " + interfaceDefinitionsMap.keySet());
    }

    private void collectServiceAndMachineNames() {
        // Collect Service Names
        if (astRoot.getServiceDefinitions() != null) {
            for (AstNode node : astRoot.getServiceDefinitions()) {
                if (node instanceof ServiceNode service) {
                     if (!definedServiceNames.add(service.getName())) {
                          // Duplicate service name error (might be handled elsewhere)
                     }
                }
                // Interfaces are collected separately
            }
        }
        // Collect Machine Names
        if (astRoot.getMachineDefinitions() != null) {
             for (AstNode node : astRoot.getMachineDefinitions()) {
                 if (node instanceof MachineNode machine) {
                     if (!definedMachineNames.add(machine.getName())) {
                          addError("Duplicate machine definition name '" + machine.getName() + "'.", machine);
                     }
                 }
             }
        }
         System.out.println("Collected service names: " + definedServiceNames);
         System.out.println("Collected machine names: " + definedMachineNames);
    }

    // --- Placeholder validation methods for different blocks --- 

    private void validateMachines() {
        if (astRoot.getMachineDefinitions() == null) return;
        Map<Long, AstNode> seenIds = new HashMap<>(); // Track IDs within this block

        for (AstNode node : astRoot.getMachineDefinitions()) {
            if (node instanceof MachineNode machine) {
                 Map<Long, AstNode> seenIdsInMachine = new HashMap<>(); // Track IDs within THIS machine
                 checkAndRegisterId(machine, seenIds); // Check top-level machine ID
                 checkAndRegisterId(machine, seenIdsInMachine); // Also add machine ID to its internal scope

                 // Collect actions/guards defined at machine level
                 Set<String> definedActionNames = machine.getActions().stream()
                                                     .filter(a -> a instanceof ActionNode)
                                                     .map(a -> ((ActionNode)a).getName())
                                                     .collect(Collectors.toSet());
                 Set<String> definedGuardNames = machine.getGuards().stream()
                                                    .filter(g -> g instanceof GuardNode)
                                                    .map(g -> ((GuardNode)g).getName())
                                                    .collect(Collectors.toSet());
                // Collect all state names (including nested) for target validation later?
                // Set<String> allStateNamesInMachine = collectAllStateNames(machine); // Requires helper

                 // Validate context block first
                 machine.getContext().ifPresent(context -> validateContext(context, seenIdsInMachine));

                 // Validate hierarchy starting from top level (null parent)
                 validateMachineStateHierarchy(machine, null, definedActionNames, definedGuardNames, seenIdsInMachine);

                 // Post-hierarchy check: Reachability (needs all states collected)
                 Set<String> allDefinedStateNames = collectAllStateNames(machine); // Collect all names after hierarchy validation
                 validateUnreachableStates(machine, allDefinedStateNames);

            } else {
                 addError("Invalid node type found in machines block: " + node.getClass().getSimpleName(), node);
            }
        }
    }

    // Updated signature and logic for hierarchy validation
    private void validateMachineStateHierarchy(MachineNode machine, StateNode parentState,
                                             Set<String> definedActionNames, Set<String> definedGuardNames,
                                             Map<Long, ssot_parser.ast.AstNode> seenIdsInScope) {
        String scopeName = (parentState == null) ? machine.getName() : parentState.getName();
        System.out.println("Validating states within scope: " + scopeName);

        List<StateNode> currentLevelStates;
        if (parentState == null) {
            currentLevelStates = machine.getStates().stream()
                                     .filter(s -> s instanceof StateNode)
                                     .map(s -> (StateNode)s)
                                     .collect(Collectors.toList());
        } else {
            currentLevelStates = parentState.getNestedStates().stream()
                                         .filter(s -> s instanceof StateNode)
                                         .map(s -> (StateNode)s)
                                         .collect(Collectors.toList());
        }

        Set<String> definedStateNamesInScope = new HashSet<>();
        // IDs are checked cumulatively within the machine scope passed down
        if(parentState != null) checkAndRegisterId(parentState, seenIdsInScope);

        // First pass: Collect names and check IDs/duplicates at this level
        for (StateNode state : currentLevelStates) {
             checkAndRegisterId(state, seenIdsInScope);
             if (!definedStateNamesInScope.add(state.getName())) {
                 addError("Duplicate state name '" + state.getName() + "' defined within scope '" + scopeName + "'.", state);
             }
             // Check IDs of children immediately within this state's definition
             state.getInvokes().forEach(inv -> checkAndRegisterId(inv, seenIdsInScope));
             state.getTransitions().forEach(t -> checkAndRegisterId(t, seenIdsInScope));
             state.getHistory().ifPresent(h -> checkAndRegisterId(h, seenIdsInScope));
        }

        // Validate initial state for the current scope
        validateInitialStateInScope(machine, parentState, currentLevelStates, definedStateNamesInScope);

        // Second pass: Validate transitions, invokes, actions for each state at this level
        Set<String> allStateNamesInMachine = collectAllStateNames(machine); // Get all state names for target validation
        for (StateNode state : currentLevelStates) {
            validateStateContent(state, allStateNamesInMachine, definedActionNames, definedGuardNames);

            // Recursively validate nested states
            if (state.getNestedStates() != null && !state.getNestedStates().isEmpty()) {
                 validateMachineStateHierarchy(machine, state, definedActionNames, definedGuardNames, seenIdsInScope); // Pass same maps down
            }
        }
    }

    // Helper to validate content of a single state (transitions, invokes, entry/exit)
    private void validateStateContent(StateNode state, Set<String> allStateNames, Set<String> definedActionNames, Set<String> definedGuardNames) {
         // Validate onEntry/onExit actions
         validateActionReferences(state.getEntryActions(), definedActionNames, "onEntry", state);
         validateActionReferences(state.getExitActions(), definedActionNames, "onExit", state);

         // Validate Transitions
         if (state.getTransitions() != null) {
             for (TransitionNode transition : state.getTransitions()) {
                 // Validate target state existence (against all states in the machine)
                 if (!allStateNames.contains(transition.getTargetState())) {
                     addError("Transition target state '" + transition.getTargetState() + "' for event '" + transition.getEvent() + "' from state '" + state.getName() + "' is not defined within the machine.", transition);
                 }
                 // Validate action/guard references
                 transition.getAction().ifPresent(actionName -> {
                     if (!definedActionNames.contains(actionName)) {
                         addError("Transition action '" + actionName + "' is not defined.", transition);
                     }
                 });
                 transition.getCondition().ifPresent(guardName -> {
                     if (!definedGuardNames.contains(guardName)) {
                         addError("Transition guard '" + guardName + "' is not defined.", transition);
                     }
                 });
             }
         }

         // Validate Invokes
         if (state.getInvokes() != null) {
             for (InvokeStateNode invoke : state.getInvokes()) {
                 // Validate invoke source
                 String srcName = invoke.getSrc();
                 if (!definedServiceNames.contains(srcName) && !definedMachineNames.contains(srcName)) {
                     addError("Invoke source '" + srcName + "' does not resolve to a defined service or machine.", invoke);
                 }
                 // Validate onDone/onError transitions (target state, action, guard)
                 validateInvokeTransition(invoke.getOnDoneTransition(), allStateNames, definedActionNames, definedGuardNames, "onDone", invoke);
                 validateInvokeTransition(invoke.getOnErrorTransition(), allStateNames, definedActionNames, definedGuardNames, "onError", invoke);
             }
         }
    }

     // Helper to collect all state names recursively
     private Set<String> collectAllStateNames(MachineNode machine) {
         Set<String> names = new HashSet<>();
         collectStateNamesRecursive(machine.getStates(), names);
         return names;
     }

     private void collectStateNamesRecursive(List<AstNode> states, Set<String> names) {
         if (states == null) return;
         for (AstNode node : states) {
             if (node instanceof StateNode state) {
                 names.add(state.getName());
                 collectStateNamesRecursive(state.getNestedStates(), names);
             }
         }
     }

     // Helper to validate initial state within a scope
     private void validateInitialStateInScope(MachineNode machine, StateNode parentState, List<StateNode> currentLevelStates, Set<String> definedStateNamesInScope) {
          if (parentState != null && !parentState.getNestedStates().isEmpty()) {
              // Compound state: Check for initial state among children
              Optional<String> initialName = findInitialStateName(parentState, currentLevelStates); // TODO: Implement findInitial based on grammar/annotations
              if (initialName.isPresent()) {
                  if (!definedStateNamesInScope.contains(initialName.get())) {
                      addError("Explicit or implicit initial nested state '" + initialName.get() + "' not found in compound state '" + parentState.getName() + "'.", parentState);
                  }
              } else if (currentLevelStates.isEmpty()){
                   addError("Compound state '" + parentState.getName() + "' has no nested states defined.", parentState);
              } else {
                   // Convention: default to first if no explicit marker found
                   addWarning("No explicit initial state defined for compound state '" + parentState.getName() + "'. Defaulting to first nested state: '" + currentLevelStates.get(0).getName() + "'.", parentState);
              }
          } else if (parentState == null) {
              // Top-level machine: Check machine's initial state
              if (machine.getInitialState().isPresent()) {
                  String initialStateName = machine.getInitialState().get();
                  if (!definedStateNamesInScope.contains(initialStateName)) {
                      addError("Initial state '" + initialStateName + "' is not defined at the top level of machine '" + machine.getName() + "'.", machine);
                  }
              } else { // No explicit $initial on machine
                   if (currentLevelStates.isEmpty()) {
                       addError("Machine '" + machine.getName() + "' has no states defined.", machine);
                   } else {
                       addWarning("No explicit initial state defined for machine '" + machine.getName() + "'. Defaulting to first state: '" + currentLevelStates.get(0).getName() + "'.", machine);
                   }
              }
          }
     }

     // Placeholder helper to find initial state marker (needs grammar detail)
     private Optional<String> findInitialStateName(StateNode parentState, List<StateNode> children) {
         // TODO: Implement logic based on how initial state is marked:
         // 1. Check for $initial annotation on parentState?
         // 2. Check for $initial annotation on one of the children?
         // 3. Rely on parser setting an 'isInitial' flag on a child StateNode?
         // For now, return empty to test default convention / missing marker warning.
         return Optional.empty();
     }

    // --- Context Validation ---
    private void validateContext(ContextNode node, Map<Long, ssot_parser.ast.AstNode> seenIdsInScope) { // Pass scope IDs
        if (node == null) return;
        System.out.println("Validating machine context...");
        Set<String> fieldNames = new HashSet<>();
        // Map<Long, AstNode> seenFieldIds = new HashMap<>(); // Use passed scope
        checkAndRegisterId(node, seenIdsInScope); // Check context block ID itself?

        if (node.getVariables() != null) {
            for (FieldNode field : node.getVariables()) {
                checkAndRegisterId(field, seenIdsInScope);
                if (!fieldNames.add(field.getName())) {
                    addError("Duplicate context variable name '" + field.getName() + "' defined.", field);
                }
                validateTypeReference(field.getType(), field); // Validate type ref as well
            }
        }
    }

    // --- State Machine Specific Validations ---

    private void validateUnreachableStates(MachineNode machine, Set<String> definedStateNames) {
        if (machine.getInitialState().isEmpty() || definedStateNames.isEmpty()) {
            // Cannot determine reachability without an initial state or any states
            // Errors for these cases are likely handled elsewhere
            return;
        }

        Set<String> reachableStates = new HashSet<>();
        Deque<String> queue = new ArrayDeque<>();
        Map<String, StateNode> stateMap = machine.getStates().stream()
                                             .filter(s -> s instanceof StateNode)
                                             .map(s -> (StateNode)s)
                                             .collect(Collectors.toMap(StateNode::getName, s -> s, (s1, s2) -> s1)); // Handle potential duplicates earlier

        String initialStateName = machine.getInitialState().get();
        if (stateMap.containsKey(initialStateName)) {
             queue.add(initialStateName);
             reachableStates.add(initialStateName);
        } else {
            // Initial state itself is not defined, error handled elsewhere
            return;
        }

        while (!queue.isEmpty()) {
            String currentStateName = queue.poll();
            StateNode currentState = stateMap.get(currentStateName);
            if (currentState == null) continue; // Should not happen if initial check passed

            // Check regular transitions
            if (currentState.getTransitions() != null) {
                for (TransitionNode transition : currentState.getTransitions()) {
                    String target = transition.getTargetState();
                    if (definedStateNames.contains(target) && reachableStates.add(target)) {
                        queue.add(target);
                    }
                }
            }

            // Check invoke transitions
             if (currentState.getInvokes() != null) {
                 for (InvokeStateNode invoke : currentState.getInvokes()) {
                     checkAndEnqueueTarget(invoke.getOnDoneTransition(), definedStateNames, reachableStates, queue);
                     checkAndEnqueueTarget(invoke.getOnErrorTransition(), definedStateNames, reachableStates, queue);
                 }
             }

            // TODO: Consider transitions from nested states if validating hierarchical reachability
        }

        // Find unreachable states
        Set<String> unreachableStates = new HashSet<>(definedStateNames);
        unreachableStates.removeAll(reachableStates);

        for (String unreachable : unreachableStates) {
            StateNode unreachableNode = stateMap.get(unreachable);
            addWarning("State '" + unreachable + "' is unreachable.", unreachableNode); // Warning for unreachable state
        }
    }

    // Helper to check invoke transition target and add to queue if reachable and new
    private void checkAndEnqueueTarget(Optional<InvokeStateNode.InvokeTransition> transitionOpt, Set<String> definedStates, Set<String> reachableStates, Deque<String> queue) {
         transitionOpt.flatMap(InvokeStateNode.InvokeTransition::target)
                      .filter(definedStates::contains)
                      .filter(reachableStates::add)
                      .ifPresent(queue::add);
    }

    // Helper to validate a list of action references
    private void validateActionReferences(List<String> actionNames, Set<String> definedActionNames, String context, StateNode stateNode) {
        if (actionNames != null) {
            for (String actionName : actionNames) {
                if (!definedActionNames.contains(actionName)) {
                    addError("State " + context + " action '" + actionName + "' is not defined.", stateNode);
                }
            }
        }
    }

    // Helper to validate InvokeTransition (target, action, guard)
    private void validateInvokeTransition(Optional<InvokeStateNode.InvokeTransition> transitionOpt, Set<String> definedStateNames, Set<String> definedActionNames, Set<String> definedGuardNames, String transitionType, InvokeStateNode invokeNode) {
        if (transitionOpt.isPresent()) {
            InvokeStateNode.InvokeTransition transition = transitionOpt.get();

            // Validate Target
            if (transition.target().isPresent()) {
                String targetName = transition.target().get();
                if (!definedStateNames.contains(targetName)) {
                    addError("Invoke " + transitionType + " transition target state '" + targetName + "' is not defined.", invokeNode);
                }
            }
            // Validate Action
            transition.action().ifPresent(actionName -> {
                if (!definedActionNames.contains(actionName)) {
                    addError("Invoke " + transitionType + " transition action '" + actionName + "' is not defined.", invokeNode);
                }
            });
            // Validate Guard
            transition.guard().ifPresent(guardName -> {
                if (!definedGuardNames.contains(guardName)) {
                    addError("Invoke " + transitionType + " transition guard '" + guardName + "' is not defined.", invokeNode);
                }
            });
        }
    }

    private void validateTypes() {
        if (astRoot.getTypeDefinitions() == null) return;
        Map<Long, AstNode> seenIds = new HashMap<>(); // Track IDs within this block

        for (AstNode node : astRoot.getTypeDefinitions()) {
            checkAndRegisterId(node, seenIds);
             if (node instanceof EnumNode enumNode) {
                 validateEnum(enumNode);
             } else if (node instanceof TypeDefNode structNode) {
                 validateStruct(structNode);
             } else {
                 addError("Invalid node type found in types block: " + node.getClass().getSimpleName(), node);
             }
        }
    }
    private void validateEnum(EnumNode node) {
        System.out.println("Validating enum: " + node.getName());
        Set<String> variantNames = new HashSet<>();
        Map<Long, AstNode> seenVariantIds = new HashMap<>(); // IDs within enum variants
        checkAndRegisterId(node, seenVariantIds); // Check enum ID itself (relative to inner scope?)

        if (node.getVariants() != null) {
            for (EnumVariantNode variant : node.getVariants()) {
                checkAndRegisterId(variant, seenVariantIds);
                if (!variantNames.add(variant.getName())) {
                    addError("Duplicate enum variant name '" + variant.getName() + "' defined.", variant);
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
        // Example: Check for ID uniqueness across the entire file
        // Map<Long, AstNode> allSeenIds = new HashMap<>();
        // collectAndCheckAllIds(astRoot, allSeenIds);
    }

    // Helper to check and register @id
    private void checkAndRegisterId(ssot_parser.ast.AstNode node, Map<Long, ssot_parser.ast.AstNode> seenIds) {
        if (node instanceof NodeWithId nodeWithId) {
            Optional<Long> idOpt = nodeWithId.getId();
            if (idOpt.isPresent()) {
                Long id = idOpt.get();
                if (seenIds.containsKey(id)) {
                    ssot_parser.ast.AstNode firstNode = seenIds.get(id);
                    addError("Duplicate @id(" + id + ") defined. First used near "
                             + firstNode.getClass().getSimpleName()
                             + (firstNode instanceof NodeWithName ? (" '" + ((NodeWithName)firstNode).getName() + "'") : "") // Add name if possible
                             + ".", node);
                } else {
                    seenIds.put(id, node);
                }
            }
        }
    }

    // Helper method to add an error
    private void addError(String message, ssot_parser.ast.AstNode node) {
        errors.add(new ValidationError(message, ValidationError.Severity.ERROR, node));
    }

    // Helper method to add a warning
    private void addWarning(String message, ssot_parser.ast.AstNode node) {
        errors.add(new ValidationError(message, ValidationError.Severity.WARNING, node));
    }

    // Helper method to recursively validate type expressions
    private void validateTypeReference(TypeExprNode typeNode, ssot_parser.ast.AstNode ownerNode) {
        if (typeNode == null) {
            addError("Type expression is missing or could not be parsed.", ownerNode);
            return;
        }

        if (typeNode instanceof ReferenceTypeNode refNode) {
            String typeName = refNode.getReferencedTypeName();
            if (!definedTypeNames.contains(typeName)) {
                 // Check if it's a known primitive before declaring error
                 // TODO: Define known primitive types centrally
                 Set<String> primitives = Set.of("string", "bool", "u8", "u16", "u32", "u64", "i8", "i16", "i32", "i64", "f32", "f64", "timestamp", "void");
                 if (!primitives.contains(typeName)) {
                    addError("Referenced type '" + typeName + "' is not defined.", ownerNode != null ? ownerNode : typeNode);
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
             Set<String> primitives = Set.of("string", "bool", "u8", "u16", "u32", "u64", "i8", "i16", "i32", "i64", "f32", "f64", "timestamp", "void");
             if (!primitives.contains(primNode.getTypeName())) {
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
} 