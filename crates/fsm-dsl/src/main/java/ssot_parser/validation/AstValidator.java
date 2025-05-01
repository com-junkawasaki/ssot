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

/**
 * Performs validation checks on the SSoT AST (Abstract Syntax Tree).
 * Includes checks like reference resolution, type consistency, state machine logic, etc.
 */
public class AstValidator {

    private final SsotRoot astRoot;
    private final List<ValidationError> errors;

    public AstValidator(SsotRoot astRoot) {
        this.astRoot = astRoot;
        this.errors = new ArrayList<>();
    }

    /**
     * Runs all validation checks.
     *
     * @return A list of validation errors and warnings. Returns an empty list if the AST is valid.
     */
    public List<ValidationError> validate() {
        errors.clear(); // Clear previous errors if called multiple times

        if (astRoot == null) {
            errors.add(new ValidationError("AST Root cannot be null.", ValidationError.Severity.ERROR));
            return Collections.unmodifiableList(errors);
        }

        // --- Run validation checks --- 
        validateMachines();
        validateTypes();
        validateServices();
        validateActors();
        validateCommunication();
        validateCrossCuttingConcerns(); // e.g., ID uniqueness across different blocks?

        return Collections.unmodifiableList(errors);
    }

    // --- Placeholder validation methods for different blocks --- 

    private void validateMachines() {
        if (astRoot.getMachineDefinitions() == null) return;
        for (AstNode node : astRoot.getMachineDefinitions()) {
            if (node instanceof MachineNode machine) {
                 validateSingleMachine(machine);
            }
            // else: Error? Should only contain MachineNode
        }
    }

    private void validateSingleMachine(MachineNode machine) {
        System.out.println("Validating machine: " + machine.getName());

        // Collect all defined state names within this machine (including nested? For now, top-level only)
        Set<String> definedStateNames = machine.getStates().stream()
                                            .filter(s -> s instanceof StateNode) // Ensure it's a StateNode
                                            .map(s -> ((StateNode)s).getName())
                                            .collect(Collectors.toSet());

        // Collect defined action and guard names
        Set<String> definedActionNames = machine.getActions().stream()
                                             .filter(a -> a instanceof ActionNode)
                                             .map(a -> ((ActionNode)a).getName())
                                             .collect(Collectors.toSet());
        Set<String> definedGuardNames = machine.getGuards().stream()
                                            .filter(g -> g instanceof GuardNode)
                                            .map(g -> ((GuardNode)g).getName())
                                            .collect(Collectors.toSet());
        // TODO: Consider adding invokes to a resolvable map/set as well?

        // 1. Validate Initial State
        if (machine.getInitialState().isPresent()) {
            String initialStateName = machine.getInitialState().get();
            if (!definedStateNames.contains(initialStateName)) {
                addError("Initial state '" + initialStateName + "' is not defined.", machine);
            }
        } else {
            // If initial state is required, add error. Visitor currently defaults to first state.
            if (definedStateNames.isEmpty()) {
                 addError("Machine has no states defined.", machine);
             } else {
                 // Optionally add a warning if explicit initial state is preferred
                 addWarning("No explicit initial state defined. Defaulting to first state: '" + ((StateNode)machine.getStates().get(0)).getName() + "'", machine);
             }
        }

        // 2. Validate Transitions (Target State Existence + Action/Guard Existence)
        if (machine.getTransitions() != null) {
            for (AstNode transitionNode : machine.getTransitions()) {
                 if (!(transitionNode instanceof TransitionNode)) continue; // Should not happen
                 TransitionNode transition = (TransitionNode) transitionNode;
                String targetStateName = transition.getTargetState();
                if (!definedStateNames.contains(targetStateName)) {
                    // Try to find the source state node for better error reporting
                    AstNode sourceNode = machine.getStates().stream()
                        .filter(s -> s instanceof StateNode && ((StateNode)s).getName().equals(transition.getSourceState()))
                        .findFirst().orElse(machine); // Default to machine node if source state not found
                    addError("Transition target state '" + targetStateName + "' for event '" + transition.getEvent() + "' from state '" + transition.getSourceState() + "' is not defined.", sourceNode);
                }

                // Validate action reference
                transition.getAction().ifPresent(actionName -> {
                    if (!definedActionNames.contains(actionName)) {
                        addError("Transition action '" + actionName + "' is not defined.", transition);
                    }
                });

                // Validate guard reference
                transition.getCondition().ifPresent(guardName -> {
                    if (!definedGuardNames.contains(guardName)) {
                        addError("Transition guard '" + guardName + "' is not defined.", transition);
                    }
                });
            }
        }

         // 3. Validate Invokes (Target State Existence in onDone/onError + Action/Guard Existence)
         if (machine.getStates() != null) {
             for (AstNode stateNodeAst : machine.getStates()) {
                 if (!(stateNodeAst instanceof StateNode)) continue;
                 StateNode stateNode = (StateNode) stateNodeAst;

                 // Validate onEntry/onExit actions
                 validateActionReferences(stateNode.getEntryActions(), definedActionNames, "onEntry", stateNode);
                 validateActionReferences(stateNode.getExitActions(), definedActionNames, "onExit", stateNode);

                 if (stateNode.getInvokes() != null) {
                     for (InvokeStateNode invoke : stateNode.getInvokes()) {
                         validateInvokeTransition(invoke.getOnDoneTransition(), definedStateNames, definedActionNames, definedGuardNames, "onDone", invoke);
                         validateInvokeTransition(invoke.getOnErrorTransition(), definedStateNames, definedActionNames, definedGuardNames, "onError", invoke);
                         // TODO: Validate invoke.getSrc() resolves to a service/machine?
                     }
                 }
                 // TODO: Recursively validate nested states?
             }
         }

        // TODO: Add more checks: unreachable states etc.
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
        for (AstNode node : astRoot.getTypeDefinitions()) {
            // TODO: Validate struct/enum definitions (e.g., unique variant names)
             if (node instanceof EnumNode enumNode) {
                 validateEnum(enumNode);
             } else if (node instanceof TypeDefNode structNode) {
                 // Assuming TypeDefNode is struct for now
                 validateStruct(structNode);
             }
        }
    }
    private void validateEnum(EnumNode node) {
        System.out.println("Validating enum: " + node.getName());
        // Check for duplicate variant names
        Set<String> variantNames = new HashSet<>();
        if (node.getVariants() != null) {
            for (EnumVariantNode variant : node.getVariants()) {
                if (!variantNames.add(variant.getName())) {
                    // Found a duplicate
                    addError("Duplicate enum variant name '" + variant.getName() + "' defined.", variant); // Point error to the duplicate variant node
                }
                // TODO: Validate annotations on variant?
            }
        }
        // TODO: Check for duplicate variant names
    }
    private void validateStruct(TypeDefNode node) {
        System.out.println("Validating struct: " + node.getName());
        // Check for duplicate field names
        Set<String> fieldNames = new HashSet<>();
        if (node.getFields() != null) {
            for (FieldNode field : node.getFields()) {
                if (!fieldNames.add(field.getName())) {
                    // Found a duplicate
                    addError("Duplicate struct field name '" + field.getName() + "' defined.", field); // Point error to the duplicate field node
                }
                // TODO: Validate field type reference (later step)
                // TODO: Validate annotations on field?
            }
        }
    }

    private void validateServices() {
        if (astRoot.getServiceDefinitions() == null) return;
        // Collect all defined interface names for implements check
        Set<String> definedInterfaceNames = astRoot.getServiceDefinitions().stream()
                                                 .filter(n -> n instanceof InterfaceNode)
                                                 .map(n -> ((InterfaceNode)n).getName())
                                                 .collect(Collectors.toSet());

        for (AstNode node : astRoot.getServiceDefinitions()) {
            // TODO: Validate service/interface definitions
            if (node instanceof ServiceNode service) {
                 validateService(service, definedInterfaceNames); // Pass defined interfaces
             } else if (node instanceof InterfaceNode iface) {
                 validateInterface(iface);
             }
        }
    }
    private void validateService(ServiceNode node, Set<String> definedInterfaceNames) {
        System.out.println("Validating service: " + node.getName());
        // Check for duplicate method names (including implemented ones potentially)
        // Simple check for now: only check methods directly defined in the service
        Set<String> methodNames = new HashSet<>();
        if (node.getMethods() != null) {
            for (MethodNode method : node.getMethods()) {
                if (!methodNames.add(method.getName())) {
                    addError("Duplicate method name '" + method.getName() + "' in service '" + node.getName() + "'.", method);
                }
                 // TODO: Validate parameter types, return type reference?
            }
        }
        // TODO: Check for clashes with implemented interface methods

        // Check if implemented interfaces exist
        if (node.getImplementedInterfaces() != null) {
             for (String interfaceName : node.getImplementedInterfaces()) {
                 if (!definedInterfaceNames.contains(interfaceName)) {
                     addError("Service '" + node.getName() + "' implements undefined interface '" + interfaceName + "'.", node);
                 }
             }
        }

        // TODO: Check if all methods from implemented interfaces are present (if required)
    }
    private void validateInterface(InterfaceNode node) {
        System.out.println("Validating interface: " + node.getName());
        // Check for duplicate method names
        Set<String> methodNames = new HashSet<>();
        if (node.getMethods() != null) {
            for (MethodNode method : node.getMethods()) {
                if (!methodNames.add(method.getName())) {
                    addError("Duplicate method name '" + method.getName() + "' in interface '" + node.getName() + "'.", method);
                }
                // TODO: Validate parameter types, return type reference?
            }
        }
    }

    private void validateActors() {
        if (astRoot.getActorDefinitions() == null) return;
        for (AstNode node : astRoot.getActorDefinitions()) {
            // TODO: Validate actor definitions
        }
    }

    private void validateCommunication() {
        if (astRoot.getCommunicationDefinitions() == null) return;
        for (AstNode node : astRoot.getCommunicationDefinitions()) {
            // TODO: Validate protocol/channel/event definitions
        }
    }

    private void validateCrossCuttingConcerns() {
        // TODO: Implement checks that span multiple blocks (e.g., unique IDs globally?)
    }

    // Helper method to add an error
    private void addError(String message, AstNode node) {
        errors.add(new ValidationError(message, ValidationError.Severity.ERROR, node));
    }

    // Helper method to add a warning
    private void addWarning(String message, AstNode node) {
        errors.add(new ValidationError(message, ValidationError.Severity.WARNING, node));
    }
} 