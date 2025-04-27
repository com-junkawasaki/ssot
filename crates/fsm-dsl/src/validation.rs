use crate::ast::{
    Annotation, AnnotationValue, Argument, CommunicationBlock as ChannelsBlock, Identifier,
    MachineDefinition as StateMachine, MachinesBlock, SsotAst, TopLevelDefinition, TypeDefinition,
    TypeSpecifier,
};
use std::collections::{HashMap, HashSet};
use std::fmt::{self, Display, Formatter};
use thiserror::Error; // Import necessary AST nodes

// TODO: Define Symbol Table structure to store definitions and scopes.
// struct SymbolTable { ... }

// TODO: Define Validator structure or functions.
// struct Validator {
//     symbol_table: SymbolTable,
//     errors: Vec<ValidationError>,
// }
// impl Validator {
//     fn validate(&mut self, ast: &SsotAst) { ... }
// }

/// Represents semantic errors found during validation.
#[derive(Error, Debug, Clone, PartialEq)]
pub enum ValidationError {
    #[error("Duplicate ID 0x{id:x} found for {kind} '{name}'. First seen for {first_kind} '{first_name}'")]
    DuplicateId {
        id: u64,
        kind: String, // e.g., "state", "field", "action"
        name: String,
        first_kind: String,
        first_name: String,
    },

    #[error("Name '{name}' not found in the current scope (expected {expected_kind})")]
    NameNotFound {
        name: String,
        expected_kind: String, // e.g., "Type", "State", "Action"
    },

    #[error("Duplicate name '{name}' found for {kind} within the same scope")]
    DuplicateName { name: String, kind: String },

    #[error("Machine '{machine_name}' must have exactly one initial state marker ($initial)")]
    MissingOrMultipleInitialStates { machine_name: String, count: usize },

    #[error("Invalid transition target '{target_name}' in state '{state_name}' for event '{event_name}'")]
    InvalidTransitionTarget {
        state_name: String,
        event_name: String, // Or "after delay"
        target_name: String,
    },

    #[error(
        "History transition target '{target_name}' is invalid or refers to a non-existent state"
    )]
    InvalidHistoryTarget {
        target_name: String, // .history or Parent.history
        state_name: String,  // State containing the transition
    },

    #[error("History state requires a default target state")]
    MissingHistoryDefaultTarget {
        state_name: String, // Composite state containing the history definition
    },

    #[error("Parallel state '{state_name}' must contain multiple regions (states blocks)")]
    ParallelStateRequiresRegions { state_name: String },

    #[error("Annotation value for {annotation_name} on {element_name} is missing or invalid")]
    InvalidAnnotationValue {
        annotation_name: String,
        element_name: String,
        // TODO: Add expected format/value details?
    },

    #[error("Unused action definition '{action_name}' in machine '{machine_name}'")]
    UnusedAction {
        machine_name: String,
        action_name: String,
    },

    #[error("Unused guard definition '{guard_name}' in machine '{machine_name}'")]
    UnusedGuard {
        machine_name: String,
        guard_name: String,
    },

    #[error("History target '{target_name}' used outside of a composite state in machine '{machine_name}'")]
    InvalidHistoryTargetUsage {
        target_name: String, // e.g., ".history" or "Parent.history"
        current_state_name: String,
        machine_name: String,
    },

    #[error("Interface '{name}' referenced by '{element_name}' not found")]
    InterfaceNotFound { name: String, element_name: String },

    #[error("Service '{name}' referenced by '{element_name}' not found")]
    ServiceNotFound { name: String, element_name: String },

    #[error("Actor '{name}' referenced by '{element_name}' not found")]
    ActorNotFound { name: String, element_name: String },

    #[error("Channel '{name}' referenced by '{element_name}' not found")]
    ChannelNotFound { name: String, element_name: String },

    #[error("Protocol '{name}' referenced by '{element_name}' not found")]
    ProtocolNotFound { name: String, element_name: String },

    // --- Added Errors ---
    #[error("Type mismatch for {element_name} in {location}: expected {expected_type}, found {found_type}")]
    TypeMismatch {
        element_name: String, // e.g., "context field 'count'"
        expected_type: String,
        found_type: String,
        location: String, // e.g., "Machine 'timer'"
    },

    #[error("Unused {kind} definition '{name}' in scope '{scope}'")]
    UnusedDefinition {
        kind: String,
        name: String,
        scope: String,
    },

    #[error("Import error for path '{path}': {reason}")]
    ImportError { path: String, reason: String },

    // TODO: Add more specific validation errors as needed.
    // - Import resolution errors (Specific cases?)
    #[error("Unused {kind} definition '{name}' in scope '{scope}'")]
    DuplicateDefinition {
        kind: String,
        name: String,
        scope: String,
    },

    #[error("Undefined {kind} '{name}' referenced in scope '{scope}'")]
    UndefinedReference {
        kind: String,
        name: String,
        scope: String,
    },

    #[error(
        "Invalid state transition from '{from_state}' to '{to_state}' in machine '{machine_name}'"
    )]
    InvalidStateTransition {
        machine_name: String,
        from_state: String,
        to_state: String,
    },

    #[error("Invalid event type for '{event_name}' in machine '{machine_name}', expected {expected_type}")]
    InvalidEventType {
        machine_name: String,
        event_name: String,
        expected_type: String,
    },

    #[error("Invalid context type for field '{field_name}' in machine '{machine_name}', expected {expected_type}")]
    InvalidContextType {
        machine_name: String,
        field_name: String,
        expected_type: String,
    },

    #[error("Cyclic dependency detected in {kind}: {cycle}")]
    CyclicDependency { kind: String, cycle: Vec<String> },
}

// TODO: Add more specific validation errors as needed.
// - Import resolution errors (Specific cases?)
/*
impl Display for ValidationError {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        match self {
            ValidationError::UnusedDefinition { kind, name, scope } => {
                write!(f, "Unused {} '{}' defined in {}", kind, name, scope)
            }
            ValidationError::UnusedAction {
                machine_name,
                action_name,
            } => {
                write!(
                    f,
                    "Unused action '{}' in machine '{}'",
                    action_name, machine_name
                )
            }
            ValidationError::UnusedGuard {
                machine_name,
                guard_name,
            } => {
                write!(
                    f,
                    "Unused guard '{}' in machine '{}'",
                    guard_name, machine_name
                )
            }
            ValidationError::DuplicateDefinition { kind, name, scope } => {
                write!(f, "Duplicate {} '{}' in scope '{}'", kind, name, scope)
            }
            ValidationError::UndefinedReference { kind, name, scope } => {
                write!(
                    f,
                    "Undefined {} '{}' referenced in scope '{}'",
                    kind, name, scope
                )
            }
            ValidationError::InvalidStateTransition {
                machine_name,
                from_state,
                to_state,
            } => {
                write!(
                    f,
                    "Invalid state transition from '{}' to '{}' in machine '{}'",
                    from_state, to_state, machine_name
                )
            }
            ValidationError::InvalidEventType {
                machine_name,
                event_name,
                expected_type,
            } => {
                write!(
                    f,
                    "Invalid event type for '{}' in machine '{}', expected {}",
                    event_name, machine_name, expected_type
                )
            }
            ValidationError::InvalidContextType {
                machine_name,
                field_name,
                expected_type,
            } => {
                write!(
                    f,
                    "Invalid context type for field '{}' in machine '{}', expected {}",
                    field_name, machine_name, expected_type
                )
            }
            ValidationError::CyclicDependency { kind, cycle } => {
                write!(
                    f,
                    "Cyclic dependency detected in {}: {}",
                    kind,
                    cycle.join(" -> ")
                )
            } // ... existing error cases ...
        }
    }
}
*/

// Helper structure to store information about the first occurrence of an ID
#[derive(Debug, Clone)]
struct IdInfo {
    kind: String,
    name: String,
}

/// Represents the kind of a symbol in the symbol table.
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
enum SymbolKind {
    Type(TypeDefinitionKind), // Struct, Enum, Alias
    Machine,
    State,
    Action,
    Guard,
    Invoke,
    Service,
    Operation,
    Protocol,
    Channel,
    Actor,
    DeploymentTarget,
    // Add other kinds as needed
}

/// Represents the kind of a type definition.
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
enum TypeDefinitionKind {
    Struct,
    Enum,
    Alias,
}

/// Information about a defined symbol.
#[derive(Debug, Clone)]
struct SymbolInfo {
    id: Option<u64>,
    kind: SymbolKind,
    name: String,
    defined_in_scope: Option<String>, // e.g., Machine name for states/actions
                                      // Add other relevant info, e.g., reference to the AST node itself if needed later
}

/// Stores symbols defined in the SSOT AST for validation purposes.
#[derive(Debug, Default)]
struct SymbolTable {
    /// Maps names to their definitions. Handles global scope and nested scopes (e.g., machine-specific symbols).
    /// Key: Scope name (e.g., "global", "MachineName"), Value: HashMap<SymbolName, SymbolInfo>
    scoped_symbols: HashMap<String, HashMap<String, SymbolInfo>>,
    /// Keep track of all registered IDs for uniqueness check.
    ids: HashMap<u64, IdInfo>,

    /// Tracks referenced actions per machine scope.
    /// Key: Machine Name, Value: Set of action names referenced.
    referenced_actions: HashMap<String, HashSet<String>>,
    /// Tracks referenced guards per machine scope.
    /// Key: Machine Name, Value: Set of guard names referenced.
    referenced_guards: HashMap<String, HashSet<String>>,
    /// Tracks referenced invokes per machine scope.
    /// Key: Machine Name, Value: Set of invoke names referenced.
    referenced_invokes: HashMap<String, HashSet<String>>,

    /// Tracks referenced types globally
    referenced_types: HashSet<String>,
    /// Tracks referenced services globally
    referenced_services: HashSet<String>,
    /// Tracks referenced actors globally
    referenced_actors: HashSet<String>,
    /// Tracks referenced channels globally
    referenced_channels: HashSet<String>,
    /// Tracks referenced protocols globally
    referenced_protocols: HashSet<String>,
}

impl SymbolTable {
    const GLOBAL_SCOPE: &'static str = "global";

    /// Registers a symbol in the appropriate scope.
    /// Checks for duplicate names within the same scope and kind.
    /// Checks for duplicate IDs globally.
    fn register(
        &mut self,
        name: String,
        kind: SymbolKind,
        id: Option<u64>,
        scope: Option<&str>,
        errors: &mut Vec<ValidationError>,
    ) {
        let scope_name = scope.unwrap_or(Self::GLOBAL_SCOPE).to_string();
        let scope_map = self.scoped_symbols.entry(scope_name.clone()).or_default();

        // 1. Check for duplicate names within the scope
        if let Some(existing_symbol) = scope_map.get(&name) {
            // Allow states and actions/guards/invokes to potentially share names within a machine scope?
            // For now, enforce uniqueness strictly within the scope for the same *kind*.
            // More nuanced rules might be needed later (e.g., state vs action name collision).
            if existing_symbol.kind == kind {
                errors.push(ValidationError::DuplicateName {
                    name: name.clone(),
                    kind: format!("{:?}", kind), // Simple kind representation for now
                });
            }
            // Consider if different kinds can share names (e.g., State 'X' and Action 'X')
        }

        // 2. Check for duplicate IDs globally (using the separate id_map)
        if let Some(id_val) = id {
            let id_info = IdInfo {
                kind: format!("{:?}", kind),
                name: name.clone(),
            };
            if let Some(existing_id_info) = self.ids.get(&id_val) {
                errors.push(ValidationError::DuplicateId {
                    id: id_val,
                    kind: id_info.kind.clone(),
                    name: id_info.name.clone(),
                    first_kind: existing_id_info.kind.clone(),
                    first_name: existing_id_info.name.clone(),
                });
            } else {
                self.ids.insert(id_val, id_info);
            }
        }

        // 3. Register the symbol
        let symbol_info = SymbolInfo {
            id,
            kind,
            name: name.clone(),
            defined_in_scope: if scope_name == Self::GLOBAL_SCOPE {
                None
            } else {
                Some(scope_name)
            },
        };
        scope_map.insert(name, symbol_info); // Overwrites if name collision with *different* kind allowed
    }

    /// Looks up a symbol by name, optionally within a specific scope.
    fn lookup(&self, name: &str, scope: Option<&str>) -> Option<&SymbolInfo> {
        let scope_name = scope.unwrap_or(Self::GLOBAL_SCOPE);
        self.scoped_symbols.get(scope_name)?.get(name)
    }

    /// Looks up a symbol and checks if it matches the expected kind.
    /// Generates a NameNotFound error if the symbol doesn't exist or the kind mismatches.
    fn lookup_with_kind(
        &self,
        name: &str,
        expected_kind_str: &str, // For error message
        scope: Option<&str>,
        match_kind_fn: impl FnOnce(&SymbolKind) -> bool,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        match self.lookup(name, scope) {
            Some(symbol_info) => {
                if match_kind_fn(&symbol_info.kind) {
                    Some(symbol_info)
                } else {
                    // Found the name, but it's the wrong kind of symbol
                    errors.push(ValidationError::NameNotFound {
                        name: name.to_string(),
                        expected_kind: expected_kind_str.to_string(),
                        // TODO: Could add a "found_kind" field to the error
                    });
                    None
                }
            }
            None => {
                // Name not found in the specified scope
                errors.push(ValidationError::NameNotFound {
                    name: name.to_string(),
                    expected_kind: expected_kind_str.to_string(),
                });
                None
            }
        }
    }

    /// Looks up a type definition (struct, enum, alias) in the global scope.
    fn lookup_type(&self, name: &str, errors: &mut Vec<ValidationError>) -> Option<&SymbolInfo> {
        self.lookup_with_kind(
            name,
            "Type",
            None,
            |k| matches!(k, SymbolKind::Type(_)),
            errors,
        )
    }

    /// Looks up a state within a specific machine scope.
    fn lookup_state(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(
            name,
            "State",
            Some(machine_scope),
            |k| *k == SymbolKind::State,
            errors,
        )
    }

    /// Looks up an action within a specific machine scope.
    fn lookup_action(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(
            name,
            "Action",
            Some(machine_scope),
            |k| *k == SymbolKind::Action,
            errors,
        )
    }

    /// Looks up a guard within a specific machine scope.
    fn lookup_guard(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(
            name,
            "Guard",
            Some(machine_scope),
            |k| *k == SymbolKind::Guard,
            errors,
        )
    }

    /// Looks up an invoke definition within a specific machine scope.
    fn lookup_invoke(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(
            name,
            "Invoke",
            Some(machine_scope),
            |k| *k == SymbolKind::Invoke,
            errors,
        )
    }

    /// Looks up a service or interface in the global scope.
    fn lookup_service_or_interface(
        &self,
        name: &str,
        element_name: &str, // For error reporting
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        match self.lookup_with_kind(
            name,
            "Service or Interface",
            None,
            |k| *k == SymbolKind::Service,
            errors,
        ) {
            Some(s) => Some(s),
            None => {
                // Replace generic NameNotFound with specific error if possible
                errors.pop(); // Remove the generic error added by lookup_with_kind
                errors.push(ValidationError::ServiceNotFound {
                    name: name.to_string(),
                    element_name: element_name.to_string(),
                });
                None
            }
        }
    }

    /// Looks up an Actor in the global scope.
    fn lookup_actor(
        &self,
        name: &str,
        element_name: &str, // For error reporting
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        match self.lookup_with_kind(name, "Actor", None, |k| *k == SymbolKind::Actor, errors) {
            Some(a) => Some(a),
            None => {
                errors.pop();
                errors.push(ValidationError::ActorNotFound {
                    name: name.to_string(),
                    element_name: element_name.to_string(),
                });
                None
            }
        }
    }

    /// Looks up a Channel in the global scope.
    fn lookup_channel(
        &self,
        name: &str,
        element_name: &str, // For error reporting
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        match self.lookup_with_kind(name, "Channel", None, |k| *k == SymbolKind::Channel, errors) {
            Some(c) => Some(c),
            None => {
                errors.pop();
                errors.push(ValidationError::ChannelNotFound {
                    name: name.to_string(),
                    element_name: element_name.to_string(),
                });
                None
            }
        }
    }

    /// Looks up a Protocol in the global scope.
    fn lookup_protocol(
        &self,
        name: &str,
        element_name: &str, // For error reporting
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        match self.lookup_with_kind(
            name,
            "Protocol",
            None,
            |k| *k == SymbolKind::Protocol,
            errors,
        ) {
            Some(p) => Some(p),
            None => {
                errors.pop();
                errors.push(ValidationError::ProtocolNotFound {
                    name: name.to_string(),
                    element_name: element_name.to_string(),
                });
                None
            }
        }
    }

    /// Helper to register a type definition (Struct or Enum).
    fn register_type_definition(
        &mut self,
        type_def: &TypeDefinition,
        errors: &mut Vec<ValidationError>,
    ) {
        match type_def {
            TypeDefinition::Struct(s) => {
                self.register(
                    s.name.name.clone(),
                    SymbolKind::Type(TypeDefinitionKind::Struct),
                    Some(s.id.value),
                    None, // Types are global
                    errors,
                );
                // Optionally register fields if needed for some validation later
            }
            TypeDefinition::Enum(e) => {
                self.register(
                    e.name.name.clone(),
                    SymbolKind::Type(TypeDefinitionKind::Enum),
                    Some(e.id.value),
                    None, // Types are global
                    errors,
                );
                // Optionally register variants
            }
        }
    }

    /// Helper to register definitions within a machine's scope.
    fn register_machine_member<F>(
        &mut self,
        name: String,
        kind: SymbolKind,
        id_value: u64,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
        check_duplicate_fn: F, // Function to check for specific kind duplicates
    ) where
        F: FnOnce(&SymbolKind) -> bool,
    {
        // Check for duplicates using the generic lookup before registering
        // This allows finer control (e.g., allow state 'X' and action 'X') if needed
        // though current `register` also does a basic kind check.
        if let Some(existing) = self.lookup(&name, Some(machine_scope)) {
            if check_duplicate_fn(&existing.kind) {
                errors.push(ValidationError::DuplicateName {
                    name: name.clone(),
                    kind: format!("{:?}", kind),
                });
                return; // Don't register if specific duplicate found
            }
        }

        self.register(name, kind, Some(id_value), Some(machine_scope), errors);
    }

    /// Checks for unused definitions in the symbol table and reports them as validation errors.
    fn check_unused_definitions(&self) -> Vec<ValidationError> {
        let mut errors = Vec::new();

        // Check each machine's definitions
        for (machine_name, machine) in &self.machines {
            // Check unused actions
            for action in &machine.actions {
                if !self.is_action_used(machine_name, action) {
                    errors.push(ValidationError::UnusedDefinition {
                        kind: "action".to_string(),
                        name: action.clone(),
                        scope: machine_name.clone(),
                    });
                }
            }

            // Check unused guards
            for guard in &machine.guards {
                if !self.is_guard_used(machine_name, guard) {
                    errors.push(ValidationError::UnusedDefinition {
                        kind: "guard".to_string(),
                        name: guard.clone(),
                        scope: machine_name.clone(),
                    });
                }
            }

            // Check unused invokes
            for invoke in &machine.invokes {
                if !self.is_invoke_used(machine_name, invoke) {
                    errors.push(ValidationError::UnusedDefinition {
                        kind: "invoke".to_string(),
                        name: invoke.clone(),
                        scope: machine_name.clone(),
                    });
                }
            }
        }

        errors
    }

    fn is_action_used(&self, machine_name: &str, action: &str) -> bool {
        if let Some(machine) = self.machines.get(machine_name) {
            for state in &machine.states {
                // Check entry actions
                if state.entry.contains(action) {
                    return true;
                }
                // Check exit actions
                if state.exit.contains(action) {
                    return true;
                }
                // Check transition actions
                for transition in &state.transitions {
                    if transition.actions.contains(action) {
                        return true;
                    }
                }
            }
        }
        false
    }

    fn is_guard_used(&self, machine_name: &str, guard: &str) -> bool {
        if let Some(machine) = self.machines.get(machine_name) {
            for state in &machine.states {
                for transition in &state.transitions {
                    if transition.guard.as_ref().map_or(false, |g| g == guard) {
                        return true;
                    }
                }
            }
        }
        false
    }

    fn is_invoke_used(&self, machine_name: &str, invoke: &str) -> bool {
        if let Some(machine) = self.machines.get(machine_name) {
            for state in &machine.states {
                if state.invoke.as_ref().map_or(false, |i| i == invoke) {
                    return true;
                }
            }
        }
        false
    }

    /// Marks a type as referenced
    fn mark_type_referenced(&mut self, type_name: &str) {
        self.referenced_types.insert(type_name.to_string());
    }

    /// Marks a service as referenced
    fn mark_service_referenced(&mut self, service_name: &str) {
        self.referenced_services.insert(service_name.to_string());
    }

    /// Marks an actor as referenced
    fn mark_actor_referenced(&mut self, actor_name: &str) {
        self.referenced_actors.insert(actor_name.to_string());
    }

    /// Marks a channel as referenced
    fn mark_channel_referenced(&mut self, channel_name: &str) {
        self.referenced_channels.insert(channel_name.to_string());
    }

    /// Marks a protocol as referenced
    fn mark_protocol_referenced(&mut self, protocol_name: &str) {
        self.referenced_protocols.insert(protocol_name.to_string());
    }

    pub fn mark_action_referenced(&mut self, machine_name: &str, action_name: &str) {
        if let Some(machine_refs) = self.referenced_actions.get_mut(machine_name) {
            machine_refs.insert(action_name.to_string());
        } else {
            let mut machine_refs = HashSet::new();
            machine_refs.insert(action_name.to_string());
            self.referenced_actions
                .insert(machine_name.to_string(), machine_refs);
        }
    }

    pub fn mark_guard_referenced(&mut self, machine_name: &str, guard_name: &str) {
        if let Some(machine_refs) = self.referenced_guards.get_mut(machine_name) {
            machine_refs.insert(guard_name.to_string());
        } else {
            let mut machine_refs = HashSet::new();
            machine_refs.insert(guard_name.to_string());
            self.referenced_guards
                .insert(machine_name.to_string(), machine_refs);
        }
    }

    pub fn mark_invoke_referenced(&mut self, machine_name: &str, invoke_name: &str) {
        if let Some(machine_refs) = self.referenced_invokes.get_mut(machine_name) {
            machine_refs.insert(invoke_name.to_string());
        } else {
            let mut machine_refs = HashSet::new();
            machine_refs.insert(invoke_name.to_string());
            self.referenced_invokes
                .insert(machine_name.to_string(), machine_refs);
        }
    }

    pub fn validate_state_machine(
        &mut self,
        machine: &StateMachine,
    ) -> Result<(), Vec<ValidationError>> {
        let mut errors = Vec::new();

        // Validate states
        self.validate_states(machine, &mut errors);

        // Validate transitions
        self.validate_transitions(machine, &mut errors);

        // Validate actions
        self.validate_actions(machine, &mut errors);

        // Validate guards
        self.validate_guards(machine, &mut errors);

        // Validate invokes
        self.validate_invokes(machine, &mut errors);

        // Check for unused definitions
        self.check_unused_definitions();

        if errors.is_empty() {
            Ok(())
        } else {
            Err(errors)
        }
    }

    fn validate_states(&self, machine: &StateMachine, errors: &mut Vec<ValidationError>) {
        let mut visited_states = HashSet::new();
        let mut state_stack = vec![&machine.initial_state];

        while let Some(state) = state_stack.pop() {
            if !visited_states.insert(state) {
                continue;
            }

            // Validate transitions from this state
            if let Some(transitions) = machine.transitions.get(state) {
                for transition in transitions {
                    if !machine.states.contains(&transition.target) {
                        errors.push(ValidationError::UndefinedReference {
                            kind: "State".to_string(),
                            name: transition.target.clone(),
                            scope: format!("Machine '{}'", machine.name),
                        });
                    } else {
                        state_stack.push(&transition.target);
                    }
                }
            }
        }
    }

    fn validate_transitions(&self, machine: &StateMachine, errors: &mut Vec<ValidationError>) {
        for (from_state, transitions) in &machine.transitions {
            for transition in transitions {
                // Validate that the target state exists
                if !machine.states.contains(&transition.target) {
                    errors.push(ValidationError::InvalidStateTransition {
                        machine_name: machine.name.clone(),
                        from_state: from_state.clone(),
                        to_state: transition.target.clone(),
                    });
                }

                // Validate event type if specified
                if let Some(ref event_type) = transition.event_type {
                    if !self.is_valid_event_type(event_type) {
                        errors.push(ValidationError::InvalidEventType {
                            machine_name: machine.name.clone(),
                            event_name: transition.event.clone(),
                            expected_type: event_type.clone(),
                        });
                    }
                }

                // Validate guards
                for guard in &transition.guards {
                    if !self.is_guard_defined(guard, &machine.name) {
                        errors.push(ValidationError::UndefinedReference {
                            kind: "Guard".to_string(),
                            name: guard.clone(),
                            scope: format!("Machine '{}'", machine.name),
                        });
                    }
                }

                // Validate actions
                for action in &transition.actions {
                    if !self.is_action_defined(action, &machine.name) {
                        errors.push(ValidationError::UndefinedReference {
                            kind: "Action".to_string(),
                            name: action.clone(),
                            scope: format!("Machine '{}'", machine.name),
                        });
                    }
                }
            }
        }
    }

    fn validate_actions(&mut self, machine: &StateMachine, errors: &mut Vec<ValidationError>) {
        for action in &machine.actions {
            if self.is_action_defined(action, &machine.name) {
                errors.push(ValidationError::DuplicateDefinition {
                    kind: "Action".to_string(),
                    name: action.clone(),
                    scope: format!("Machine '{}'", machine.name),
                });
            }
        }
    }

    fn validate_guards(&mut self, machine: &StateMachine, errors: &mut Vec<ValidationError>) {
        for guard in &machine.guards {
            if self.is_guard_defined(guard, &machine.name) {
                errors.push(ValidationError::DuplicateDefinition {
                    kind: "Guard".to_string(),
                    name: guard.clone(),
                    scope: format!("Machine '{}'", machine.name),
                });
            }
        }
    }

    fn validate_invokes(&mut self, machine: &StateMachine, errors: &mut Vec<ValidationError>) {
        let mut visited = HashSet::new();
        for invoke in &machine.invokes {
            if !visited.insert(invoke) {
                errors.push(ValidationError::DuplicateDefinition {
                    kind: "Invoke".to_string(),
                    name: invoke.clone(),
                    scope: format!("Machine '{}'", machine.name),
                });
            }

            if !self.is_invoke_defined(invoke, &machine.name) {
                errors.push(ValidationError::UndefinedReference {
                    kind: "Invoke".to_string(),
                    name: invoke.clone(),
                    scope: format!("Machine '{}'", machine.name),
                });
            }
        }
    }

    fn is_valid_event_type(&self, event_type: &str) -> bool {
        // Add your event type validation logic here
        // For example, check if it's a valid type in your type system
        true // Placeholder implementation
    }

    fn is_guard_defined(&self, guard: &str, machine_name: &str) -> bool {
        self.referenced_guards
            .get(machine_name)
            .map_or(false, |guards| guards.contains(guard))
    }

    fn is_action_defined(&self, action: &str, machine_name: &str) -> bool {
        self.referenced_actions
            .get(machine_name)
            .map_or(false, |actions| actions.contains(action))
    }

    fn is_invoke_defined(&self, invoke: &str, machine_name: &str) -> bool {
        self.referenced_invokes
            .get(machine_name)
            .map_or(false, |invokes| invokes.contains(invoke))
    }
}

/// Main validation function.
pub fn validate_ast(ast: &SsotAst) -> Result<(), Vec<ValidationError>> {
    let mut errors = Vec::new();
    let mut symbol_table = SymbolTable::default();

    // --- Pass 1: Collect all definitions ---
    collect_definitions(ast, &mut symbol_table, &mut errors);

    // --- Pass 2: Validate references and structure ---
    validate_references(ast, &symbol_table, &mut errors);

    // --- Pass 3: Check for unused definitions ---
    symbol_table.check_unused_definitions();

    if errors.is_empty() {
        Ok(())
    } else {
        Err(errors)
    }
}

/// Pass 1: Traverses the AST and registers all definitions in the symbol table.
fn collect_definitions(
    ast: &SsotAst,
    symbol_table: &mut SymbolTable,
    errors: &mut Vec<ValidationError>,
) {
    // TODO: Handle imports - requires parsing imported files potentially
    // For now, validation is within a single file.

    for definition in &ast.definitions {
        match definition {
            TopLevelDefinition::Types(block) => {
                for type_def in &block.definitions {
                    symbol_table.register_type_definition(type_def, errors);
                }
            }
            TopLevelDefinition::Services(block) => {
                for item in &block.definitions {
                    match item {
                        ServiceItem::Interface(iface) => {
                            symbol_table.register(
                                iface.name.name.clone(),
                                SymbolKind::Service, // Treat Interface as a Service kind for now
                                Some(iface.id.value),
                                None, // Services/Interfaces are global
                                errors,
                            );
                            // TODO: Register methods?
                        }
                        ServiceItem::Service(svc) => {
                            symbol_table.register(
                                svc.name.name.clone(),
                                SymbolKind::Service,
                                Some(svc.id.value),
                                None, // Services are global
                                errors,
                            );
                        }
                    }
                }
            }
            TopLevelDefinition::Actors(block) => {
                for actor_def in &block.definitions {
                    symbol_table.register(
                        actor_def.name.name.clone(),
                        SymbolKind::Actor,
                        Some(actor_def.id.value),
                        None, // Actors are global
                        errors,
                    );
                }
            }
            TopLevelDefinition::Communication(block) => {
                for item in &block.definitions {
                    match item {
                        CommunicationItem::Protocol(proto) => {
                            // Added Protocol registration
                            symbol_table.register(
                                proto.name.name.clone(),
                                SymbolKind::Protocol, // Fixed kind
                                Some(proto.id.value),
                                None, // Protocols are global
                                errors,
                            );
                        }
                        CommunicationItem::Channel(chan) => {
                            symbol_table.register(
                                chan.name.name.clone(),
                                SymbolKind::Channel,
                                Some(chan.id.value),
                                None, // Channels are global
                                errors,
                            );
                        }
                        CommunicationItem::Event(_) => { /* Register if needed */ }
                    }
                }
            }
            TopLevelDefinition::Machines(block) => {
                for machine_def in &block.definitions {
                    collect_machine_definitions(machine_def, symbol_table, errors);
                }
            }
            TopLevelDefinition::DeploymentConfig(block) => {
                for item in &block.definitions {
                    match item {
                        DeploymentItem::Environment(env) => {
                            symbol_table.register(
                                env.name.name.clone(),
                                SymbolKind::DeploymentTarget, // Generic kind for now
                                Some(env.id.value),
                                None, // Global
                                errors,
                            );
                        }
                        DeploymentItem::Infrastructure(inf) => {
                            symbol_table.register(
                                inf.name.name.clone(),
                                SymbolKind::DeploymentTarget,
                                Some(inf.id.value),
                                None,
                                errors,
                            );
                        }
                        DeploymentItem::Deployment(dep) => {
                            symbol_table.register(
                                dep.name.name.clone(),
                                SymbolKind::DeploymentTarget,
                                Some(dep.id.value),
                                None,
                                errors,
                            );
                        }
                    }
                }
            }
        }
    }
}

/// Collects definitions within a single machine.
fn collect_machine_definitions(
    machine_def: &MachineDefinition,
    symbol_table: &mut SymbolTable,
    errors: &mut Vec<ValidationError>,
) {
    let machine_name = machine_def.name.name.clone();
    let machine_scope = &machine_name;

    // Register the machine itself (in global scope)
    symbol_table.register(
        machine_name.clone(),
        SymbolKind::Machine,
        Some(machine_def.id.value),
        None,
        errors,
    );

    // Register actions
    if let Some(actions_block) = &machine_def.actions {
        for action_def in &actions_block.definitions {
            symbol_table.register_machine_member(
                action_def.name.name.clone(),
                SymbolKind::Action,
                action_def.id.value,
                machine_scope,
                errors,
                |k| matches!(k, SymbolKind::Action), // Check only for duplicate Actions
            );
        }
    }

    // Register guards
    if let Some(guards_block) = &machine_def.guards {
        for guard_def in &guards_block.definitions {
            symbol_table.register_machine_member(
                guard_def.name.name.clone(),
                SymbolKind::Guard,
                guard_def.id.value,
                machine_scope,
                errors,
                |k| matches!(k, SymbolKind::Guard), // Check only for duplicate Guards
            );
        }
    }

    // Register invokes
    if let Some(invokes_block) = &machine_def.invokes {
        for invoke_def in &invokes_block.definitions {
            symbol_table.register_machine_member(
                invoke_def.name.name.clone(),
                SymbolKind::Invoke,
                invoke_def.id.value,
                machine_scope,
                errors,
                |k| matches!(k, SymbolKind::Invoke), // Check only for duplicate Invokes
            );
        }
    }

    // Register states (recursively)
    if let Some(states_block) = &machine_def.states {
        collect_states_definitions(states_block, machine_scope, symbol_table, errors);
    }

    // Register context fields? (If needed for type checking later)
    // if let Some(context_def) = &machine_def.context {
    //     // ... register context fields under a specific sub-scope? ...
    // }
}

/// Recursively collects state definitions within a states block.
fn collect_states_definitions(
    states_block: &StatesBlock,
    machine_scope: &str,
    symbol_table: &mut SymbolTable,
    errors: &mut Vec<ValidationError>,
) {
    for state_def in &states_block.states {
        symbol_table.register_machine_member(
            state_def.name.name.clone(),
            SymbolKind::State,
            state_def.id.value,
            machine_scope, // States are scoped to the machine
            errors,
            |k| matches!(k, SymbolKind::State), // Check only for duplicate States
        );

        // Recursively collect nested states within parallel regions
        for region in &state_def.regions {
            collect_states_definitions(region, machine_scope, symbol_table, errors);
        }

        // TODO: Maybe register state-specific invokes if they have unique IDs/names?
        // for state_invoke in &state_def.invokes {
        //     // Need careful scope handling here...
        // }
    }
}

// --- Pass 2: Reference and Structure Validation ---

/// Pass 2: Traverses the AST again, validating references against the symbol table.
fn validate_references(
    ast: &SsotAst,
    symbol_table: &SymbolTable,
    errors: &mut Vec<ValidationError>,
) {
    // TODO: Validate imports if/when implemented

    for definition in &ast.definitions {
        match definition {
            TopLevelDefinition::Types(block) => {
                validate_types_block_refs(block, symbol_table, errors)
            }
            TopLevelDefinition::Services(block) => {
                validate_services_block_refs(block, symbol_table, errors)
            }
            TopLevelDefinition::Actors(_) => { /* No references to validate within Actors block itself */
            }
            TopLevelDefinition::Communication(block) => {
                validate_communication_block_refs(block, symbol_table, errors)
            }
            TopLevelDefinition::Machines(block) => {
                validate_machines_block_refs(block, symbol_table, errors)
            }
            TopLevelDefinition::DeploymentConfig(block) => {
                validate_deployment_block_refs(block, symbol_table, errors)
            }
        }
    }
}

fn validate_types_block_refs(
    block: &TypesBlock,
    symbol_table: &SymbolTable,
    errors: &mut Vec<ValidationError>,
) {
    for type_def in &block.definitions {
        match type_def {
            TypeDefinition::Struct(struct_def) => {
                for field in &struct_def.fields {
                    validate_type_specifier(&field.type_spec, symbol_table, errors);
                    // TODO: Validate annotations ($validate, $db)
                }
                // TODO: Validate struct-level annotations
            }
            TypeDefinition::Enum(enum_def) => {
                // Validate types within variants if they exist later
                // TODO: Validate enum/variant annotations
            }
        }
    }
}

/// Recursively validates type specifiers.
fn validate_type_specifier(
    type_spec: &TypeSpecifier,
    symbol_table: &mut SymbolTable,
    errors: &mut Vec<ValidationError>,
) {
    match type_spec {
        TypeSpecifier::Simple(ident) => {
            // Check if it's a built-in type or a defined type
            if !is_primitive_type(&ident.name) {
                if symbol_table.lookup_type(&ident.name, errors).is_some() {
                    symbol_table.mark_type_referenced(&ident.name);
                }
            }
        }
        TypeSpecifier::List(inner) | TypeSpecifier::Optional(inner) => {
            validate_type_specifier(inner, symbol_table, errors);
        }
        TypeSpecifier::Map(key_type, value_type) => {
            validate_type_specifier(key_type, symbol_table, errors);
            validate_type_specifier(value_type, symbol_table, errors);
        }
    }
}

/// Helper to check for known primitive types (adjust as needed).
fn is_primitive_type(name: &str) -> bool {
    matches!(
        name,
        "string"
            | "u8"
            | "u16"
            | "u32"
            | "u64"
            | "i8"
            | "i16"
            | "i32"
            | "i64"
            | "f32"
            | "f64"
            | "bool"
            | "timestamp"
    )
    // Add other built-ins like 'any', 'void' if they exist
}

fn validate_services_block_refs(
    block: &ServicesBlock,
    symbol_table: &mut SymbolTable,
    errors: &mut Vec<ValidationError>,
) {
    for item in &block.definitions {
        match item {
            ServiceItem::Interface(iface) => {
                // Validate method parameter/return types
                for method in &iface.methods {
                    for param in &method.parameters {
                        validate_type_specifier(&param.type_spec, symbol_table, errors);
                        // TODO: Validate parameter annotations
                    }
                    if let Some(return_type) = &method.return_type {
                        validate_type_specifier(return_type, symbol_table, errors);
                    }
                    // TODO: Validate method body annotations
                }

                for annotation in &iface.annotations {
                    if let Annotation::Protocol(proto_ident) = annotation {
                        if symbol_table
                            .lookup_protocol(&proto_ident.name, &iface.name.name, errors)
                            .is_some()
                        {
                            symbol_table.mark_protocol_referenced(&proto_ident.name);
                        }
                    }
                }
            }
            ServiceItem::Service(svc) => {
                let service_name = &svc.name.name;
                if let Some(base_service_ident) = &svc.extends {
                    if symbol_table
                        .lookup_service_or_interface(&base_service_ident.name, service_name, errors)
                        .is_some()
                    {
                        symbol_table.mark_service_referenced(&base_service_ident.name);
                    }
                }

                for annotation in &svc.annotations {
                    match annotation {
                        Annotation::Implements(iface_ident) => {
                            if symbol_table
                                .lookup_service_or_interface(
                                    &iface_ident.name,
                                    service_name,
                                    errors,
                                )
                                .is_some()
                            {
                                symbol_table.mark_service_referenced(&iface_ident.name);
                            }
                        }
                        // Annotation::CommunicatesWith(comm_ident, proto_ident) => { // Requires Annotation update
                        //     symbol_table.lookup_service_or_interface(&comm_ident.name, service_name, errors);
                        //     symbol_table.lookup_protocol(&proto_ident.name, service_name, errors);
                        // }
                        // Annotation::Protocol(proto_ident) => {
                        //     symbol_table.lookup_protocol(&proto_ident.name, service_name, errors);
                        // }
                        // Annotation::Publishes(chan_ident) => {
                        //      symbol_table.lookup_channel(&chan_ident.name, service_name, errors);
                        // }
                        // Annotation::Subscribes(chan_ident) => {
                        //      symbol_table.lookup_channel(&chan_ident.name, service_name, errors);
                        // }
                        _ => {}
                    }
                }
            }
        }
    }
}

fn validate_communication_block_refs(
    block: &CommunicationBlock,
    symbol_table: &SymbolTable,
    errors: &mut Vec<ValidationError>,
) {
    for item in &block.definitions {
        match item {
            CommunicationItem::Protocol(proto) => {
                // Validate annotations?
            }
            CommunicationItem::Channel(chan) => {
                // TODO: Validate types in parameters map
                // TODO: Validate annotations
            }
            CommunicationItem::Event(event) => {
                for field in &event.fields {
                    validate_type_specifier(&field.type_spec, symbol_table, errors);
                    // TODO: Validate field annotations
                }
                // TODO: Validate $channel annotation reference
                let event_name = &event.name.name;
                for annotation in &event.annotations {
                    if let Annotation::Channel(chan_ident) = annotation {
                        symbol_table.lookup_channel(&chan_ident.name, event_name, errors);
                    }
                }
            }
        }
    }
}

fn validate_machines_block_refs(
    block: &MachinesBlock,
    symbol_table: &SymbolTable,
    errors: &mut Vec<ValidationError>,
) {
    for machine_def in &block.definitions {
        let mut current_path: Vec<String> = Vec::new(); // Start with empty path for machine top-level
        validate_machine_refs(machine_def, &current_path, symbol_table, errors);
    }
}

fn validate_machine_refs(
    machine_def: &MachineDefinition,
    current_path: &[String],
    symbol_table: &SymbolTable,
    errors: &mut Vec<ValidationError>,
) {
    let machine_scope = &machine_def.name.name;

    // Validate context types
    if let Some(context) = &machine_def.context {
        for field in &context.fields {
            validate_type_specifier(&field.type_spec, symbol_table, errors);
            // TODO: Validate context field annotations ($default)
        }
    }

    // Validate states, transitions, actions, guards, invokes recursively
    if let Some(states_block) = &machine_def.states {
        validate_states_block_refs(
            states_block,
            current_path,
            machine_scope,
            symbol_table,
            errors,
        );
    }

    // TODO: Validate machine-level annotations ($initial)
    // Integrate initial state check here:
    validate_initial_state_marker(machine_def, symbol_table, errors);

    // TODO: Validate invoke sources (Service.Method, MachineName, etc.)
    // TODO: Validate invoke input/onDone/onError references
}

fn validate_states_block_refs(
    states_block: &StatesBlock,
    current_path: &[String],
    machine_scope: &str,
    symbol_table: &SymbolTable,
    errors: &mut Vec<ValidationError>,
) {
    for state_def in &states_block.states {
        validate_state_refs(state_def, current_path, machine_scope, symbol_table, errors);
    }
}

fn validate_state_refs(
    state_def: &StateDefinition,
    current_path: &[String],
    machine_scope: &str,
    symbol_table: &SymbolTable,
    errors: &mut Vec<ValidationError>,
) {
    let mut new_path = current_path.to_vec();
    new_path.push(state_def.name.name.clone());
    let current_path = &new_path; // Shadow with the new extended path

    // Validate transitions
    for transition in &state_def.transitions {
        validate_transition_refs(
            transition,
            current_path,
            machine_scope,
            symbol_table,
            errors,
        );
    }

    // Validate onEntry/onExit actions
    for action_ident in &state_def.on_entry {
        symbol_table.lookup_action(&action_ident.name, machine_scope, errors);
    }
    for action_ident in &state_def.on_exit {
        symbol_table.lookup_action(&action_ident.name, machine_scope, errors);
    }

    // Validate state-local invokes
    for invoke in &state_def.invokes {
        // Validate the referenced invoke definition
        symbol_table.lookup_invoke(&invoke.src_ref.name, machine_scope, errors);
        // TODO: Validate input mapping types
        // TODO: Validate onDone/onError transition targets/actions/guards
        if let Some(target) = &invoke.on_done {
            validate_invoke_transition_target(
                target,
                current_path,
                machine_scope,
                symbol_table,
                errors,
            );
        }
        if let Some(target) = &invoke.on_error {
            validate_invoke_transition_target(
                target,
                current_path,
                machine_scope,
                symbol_table,
                errors,
            );
        }
    }

    // Validate after transitions
    for after_transition in &state_def.after_transitions {
        validate_after_transition_refs(
            after_transition,
            current_path,
            machine_scope,
            symbol_table,
            errors,
        );
    }

    // Validate history state definition
    if let Some(history) = &state_def.history {
        // Default target must be a valid state in the *current* composite state's scope
        // This requires knowing the parent state, which isn't easily available here.
        // Might need to pass down scope information or do this check differently.
        // For now, just check if it exists in the machine scope.
        symbol_table.lookup_state(&history.default_target.name, machine_scope, errors);
    }

    // Recursively validate nested states/regions
    for region in &state_def.regions {
        validate_states_block_refs(region, current_path, machine_scope, symbol_table, errors);
    }

    // TODO: Validate state annotations ($initial, $final, $parallel)
    // Integrate structural checks like parallel state requires regions?
}

fn validate_transition_refs(
    transition: &TransitionDefinition,
    current_path: &[String],
    machine_scope: &str,
    symbol_table: &SymbolTable,
    errors: &mut Vec<ValidationError>,
) {
    let current_state_name = current_path.last().cloned().unwrap_or_default(); // Get current state name for error reporting

    // Validate target state
    match &transition.target {
        TransitionTarget::State(target_ident) => {
            symbol_table.lookup_state(&target_ident.name, machine_scope, errors);
        }
        TransitionTarget::CurrentHistory => {
            if current_path.len() <= 1 {
                // Only machine scope, not inside a state
                errors.push(ValidationError::InvalidHistoryTargetUsage {
                    target_name: ".history".to_string(),
                    current_state_name: current_state_name,
                    machine_name: machine_scope.to_string(),
                });
            }
            // TODO: Further check if the *parent* state actually defines a history state.
        }
        TransitionTarget::QualifiedHistory(parent_ident) => {
            if current_path.len() <= 1 || !current_path.contains(&parent_ident.name) {
                errors.push(ValidationError::InvalidHistoryTargetUsage {
                    target_name: format!("{}.history", parent_ident.name),
                    current_state_name: current_state_name,
                    machine_name: machine_scope.to_string(),
                });
            }
            // TODO: Check if the state identified by parent_ident actually defines a history state.
        }
    }

    // Validate actions
    for action_ident in &transition.actions {
        symbol_table.lookup_action(&action_ident.name, machine_scope, errors);
    }

    // Validate guards
    for guard_ident in &transition.guards {
        symbol_table.lookup_guard(&guard_ident.name, machine_scope, errors);
    }

    // Validate $allowedActors references
    let element_name = format!(
        "Transition on event '{}' in state '{}'",
        transition.event.name,
        current_path.last().cloned().unwrap_or_default()
    );
    for annotation in &transition.annotations {
        // TODO: Need specific Annotation variants like AllowedActors(Vec<Identifier>)
        // if let Annotation::AllowedActors(actor_idents) = annotation {
        //     for actor_ident in actor_idents {
        //          symbol_table.lookup_actor(&actor_ident.name, &element_name, errors);
        //     }
        // }
    }
}

fn validate_after_transition_refs(
    after_transition: &AfterTransitionDefinition,
    current_path: &[String],
    machine_scope: &str,
    symbol_table: &SymbolTable,
    errors: &mut Vec<ValidationError>,
) {
    let current_state_name = current_path.last().cloned().unwrap_or_default();
    // Validate target state (similar to regular transition)
    match &after_transition.target {
        TransitionTarget::State(target_ident) => {
            symbol_table.lookup_state(&target_ident.name, machine_scope, errors);
        }
        TransitionTarget::CurrentHistory => {
            if current_path.len() <= 1 {
                errors.push(ValidationError::InvalidHistoryTargetUsage {
                    target_name: ".history".to_string(),
                    current_state_name: current_state_name,
                    machine_name: machine_scope.to_string(),
                });
            }
            // TODO: Check parent history definition
        }
        TransitionTarget::QualifiedHistory(parent_ident) => {
            if current_path.len() <= 1 || !current_path.contains(&parent_ident.name) {
                errors.push(ValidationError::InvalidHistoryTargetUsage {
                    target_name: format!("{}.history", parent_ident.name),
                    current_state_name: current_state_name,
                    machine_name: machine_scope.to_string(),
                });
            }
            // TODO: Check parent history definition
        }
    }
    // Validate actions
    for action_ident in &after_transition.actions {
        symbol_table.lookup_action(&action_ident.name, machine_scope, errors);
    }
    // Validate guards
    for guard_ident in &after_transition.guards {
        symbol_table.lookup_guard(&guard_ident.name, machine_scope, errors);
    }

    // TODO: Validate $allowedActors if applicable to after transitions?
}

fn validate_invoke_transition_target(
    target: &InvokeTransitionTarget,
    current_path: &[String],
    machine_scope: &str,
    symbol_table: &SymbolTable,
    errors: &mut Vec<ValidationError>,
) {
    let current_state_name = current_path.last().cloned().unwrap_or_default();
    // Validate target state (similar to regular transition)
    match &target.target {
        TransitionTarget::State(target_ident) => {
            symbol_table.lookup_state(&target_ident.name, machine_scope, errors);
        }
        TransitionTarget::CurrentHistory => {
            if current_path.len() <= 1 {
                errors.push(ValidationError::InvalidHistoryTargetUsage {
                    target_name: ".history".to_string(),
                    current_state_name: current_state_name,
                    machine_name: machine_scope.to_string(),
                });
            }
            // TODO: Check parent history definition
        }
        TransitionTarget::QualifiedHistory(parent_ident) => {
            if current_path.len() <= 1 || !current_path.contains(&parent_ident.name) {
                errors.push(ValidationError::InvalidHistoryTargetUsage {
                    target_name: format!("{}.history", parent_ident.name),
                    current_state_name: current_state_name,
                    machine_name: machine_scope.to_string(),
                });
            }
            // TODO: Check parent history definition
        }
    }
    // Validate actions
    for action_ident in &target.actions {
        symbol_table.lookup_action(&action_ident.name, machine_scope, errors);
    }
    // Validate guards
    for guard_ident in &target.guards {
        symbol_table.lookup_guard(&guard_ident.name, machine_scope, errors);
    }

    // TODO: Validate $allowedActors if applicable to invoke transitions?
}

fn validate_deployment_block_refs(
    block: &DeploymentConfigBlock,
    symbol_table: &SymbolTable,
    errors: &mut Vec<ValidationError>,
) {
    for item in &block.definitions {
        match item {
            DeploymentItem::Environment(env) => {
                // TODO: Validate extends reference
            }
            DeploymentItem::Infrastructure(inf) => {
                // TODO: Validate extends reference
            }
            DeploymentItem::Deployment(dep) => {
                // TODO: Validate targetEnvironment reference
                // TODO: Validate targetInfrastructure references (name and type)
                // TODO: Validate deployable reference (Service or Machine)
            }
        }
    }
}

// Helper function to integrate initial state check into Pass 2
fn validate_initial_state_marker(
    machine_def: &MachineDefinition,
    symbol_table: &SymbolTable,
    errors: &mut Vec<ValidationError>,
) {
    let machine_name = &machine_def.name.name;
    let machine_scope = machine_name;
    let mut initial_state_name: Option<String> = None;
    let mut initial_state_count = 0;

    // Find $initial annotation on the machine block
    for annotation in &machine_def.annotations {
        if let Annotation::Initial = annotation {
            // The DSL spec says $initial(StateName), need parser adjustment?
            // For now, assume $initial on the machine means we look for it on child states.
            // Or better: $initial(StateName) on machine points directly.
            // Let's assume the grammar puts $initial(StateName) on machine.
            // *** TODO: This part needs clarification based on parser implementation ***
            // If $initial(StateName) exists on machine, validate that StateName exists.
        }
    }

    // Find $initial annotation on direct child states
    if let Some(states_block) = &machine_def.states {
        for state_def in &states_block.states {
            if state_def.is_initial {
                initial_state_count += 1;
                initial_state_name = Some(state_def.name.name.clone());
                // Check if this state actually exists in the symbol table for this scope
                // (Should always exist if Pass 1 worked, but good sanity check)
                symbol_table.lookup_state(&state_def.name.name, machine_scope, errors);
            }
        }
    }

    if initial_state_count != 1 {
        errors.push(ValidationError::MissingOrMultipleInitialStates {
            machine_name: machine_name.clone(),
            count: initial_state_count,
        });
    }

    // TODO: Similar checks for initial states within composite states.
}

fn validate_actors_block_refs(
    block: &ActorsBlock,
    symbol_table: &mut SymbolTable,
    errors: &mut Vec<ValidationError>,
) {
    for actor in &block.definitions {
        // Validate actor service reference
        if let Some(service_ident) = &actor.service {
            if symbol_table
                .lookup_service_or_interface(&service_ident.name, &actor.name.name, errors)
                .is_some()
            {
                symbol_table.mark_service_referenced(&service_ident.name);
            }
        }

        // Validate actor annotations
        for annotation in &actor.annotations {
            match annotation {
                Annotation::CommunicatesWith(target_actor, protocol) => {
                    if symbol_table
                        .lookup_actor(&target_actor.name, &actor.name.name, errors)
                        .is_some()
                    {
                        symbol_table.mark_actor_referenced(&target_actor.name);
                    }
                    if symbol_table
                        .lookup_protocol(&protocol.name, &actor.name.name, errors)
                        .is_some()
                    {
                        symbol_table.mark_protocol_referenced(&protocol.name);
                    }
                }
                _ => {}
            }
        }
    }
}

fn validate_channels_block_refs(
    block: &ChannelsBlock,
    symbol_table: &mut SymbolTable,
    errors: &mut Vec<ValidationError>,
) {
    for channel in &block.definitions {
        // Validate source actor reference
        if symbol_table
            .lookup_actor(&channel.source.name, &channel.name.name, errors)
            .is_some()
        {
            symbol_table.mark_actor_referenced(&channel.source.name);
        }

        // Validate target actor reference
        if symbol_table
            .lookup_actor(&channel.target.name, &channel.name.name, errors)
            .is_some()
        {
            symbol_table.mark_actor_referenced(&channel.target.name);
        }

        // Validate protocol reference
        if let Some(protocol_ident) = &channel.protocol {
            if symbol_table
                .lookup_protocol(&protocol_ident.name, &channel.name.name, errors)
                .is_some()
            {
                symbol_table.mark_protocol_referenced(&protocol_ident.name);
            }
        }

        // Validate message types
        for message in &channel.messages {
            validate_type_specifier(&message.type_spec, symbol_table, errors);
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::ast::{
        ActionDefinition, GuardDefinition, Identifier, InvokeDefinition, MachineDefinition,
        NumericId, StateDefinition, StatesBlock, TransitionDefinition,
    };

    fn create_test_machine(name: &str) -> MachineDefinition {
        MachineDefinition {
            name: Identifier {
                name: name.to_string(),
            },
            id: NumericId { value: 0 },
            annotations: vec![],
            states: None,
            actions: vec![],
            guards: vec![],
            invokes: vec![],
            context: None,
        }
    }

    #[test]
    fn test_unused_action_definition() {
        let mut symbol_table = SymbolTable::default();
        let mut machine = create_test_machine("test_machine");

        // Add an unused action
        let unused_action = ActionDefinition {
            name: Identifier {
                name: "unused_action".to_string(),
            },
            id: NumericId { value: 1 },
            annotations: vec![],
        };
        machine.actions.push(unused_action.name.name.clone());

        // Add a used action
        let used_action = ActionDefinition {
            name: Identifier {
                name: "used_action".to_string(),
            },
            id: NumericId { value: 2 },
            annotations: vec![],
        };
        machine.actions.push(used_action.name.name.clone());

        // Create a state that uses one action
        let state = StateDefinition {
            name: Identifier {
                name: "state1".to_string(),
            },
            id: NumericId { value: 3 },
            annotations: vec![],
            on_entry: vec![used_action.name.clone()],
            on_exit: vec![],
            invokes: vec![],
            regions: vec![],
            history: None,
            is_initial: false,
        };

        let states_block = StatesBlock {
            states: vec![state],
        };
        machine.states = Some(states_block);

        // Register the machine in the symbol table
        symbol_table.register_machine(&machine).unwrap();

        // Check for unused definitions
        let errors = symbol_table.check_unused_definitions();

        // Verify that only the unused action is reported
        assert_eq!(errors.len(), 1);
        match &errors[0] {
            ValidationError::UnusedDefinition { kind, name, scope } => {
                assert_eq!(kind, "action");
                assert_eq!(name, "unused_action");
                assert_eq!(scope, "test_machine");
            }
            _ => panic!("Expected UnusedDefinition error"),
        }
    }

    #[test]
    fn test_unused_guard_definition() {
        let mut symbol_table = SymbolTable::default();
        let mut machine = create_test_machine("test_machine");

        // Add an unused guard
        let unused_guard = GuardDefinition {
            name: Identifier {
                name: "unused_guard".to_string(),
            },
            id: NumericId { value: 1 },
            annotations: vec![],
        };
        machine.guards.push(unused_guard.name.name.clone());

        // Add a used guard
        let used_guard = GuardDefinition {
            name: Identifier {
                name: "used_guard".to_string(),
            },
            id: NumericId { value: 2 },
            annotations: vec![],
        };
        machine.guards.push(used_guard.name.name.clone());

        // Create a state with a transition that uses one guard
        let state = StateDefinition {
            name: Identifier {
                name: "state1".to_string(),
            },
            id: NumericId { value: 3 },
            annotations: vec![],
            on_entry: vec![],
            on_exit: vec![],
            invokes: vec![],
            regions: vec![],
            history: None,
            is_initial: false,
        };

        let transition = TransitionDefinition {
            event: Identifier {
                name: "event".to_string(),
            },
            target: Identifier {
                name: "target".to_string(),
            },
            guards: vec![used_guard.name.clone()],
            actions: vec![],
            annotations: vec![],
        };

        let states_block = StatesBlock {
            states: vec![state],
            transitions: vec![transition],
        };
        machine.states = Some(states_block);

        // Register the machine in the symbol table
        symbol_table.register_machine(&machine).unwrap();

        // Check for unused definitions
        let errors = symbol_table.check_unused_definitions();

        // Verify that only the unused guard is reported
        assert_eq!(errors.len(), 1);
        match &errors[0] {
            ValidationError::UnusedDefinition { kind, name, scope } => {
                assert_eq!(kind, "guard");
                assert_eq!(name, "unused_guard");
                assert_eq!(scope, "test_machine");
            }
            _ => panic!("Expected UnusedDefinition error"),
        }
    }

    #[test]
    fn test_unused_invoke_definition() {
        let mut symbol_table = SymbolTable::default();
        let mut machine = create_test_machine("test_machine");

        // Add an unused invoke
        let unused_invoke = InvokeDefinition {
            name: Identifier {
                name: "unused_invoke".to_string(),
            },
            id: NumericId { value: 1 },
            annotations: vec![],
            src: None,
        };
        machine.invokes.push(unused_invoke.name.name.clone());

        // Add a used invoke
        let used_invoke = InvokeDefinition {
            name: Identifier {
                name: "used_invoke".to_string(),
            },
            id: NumericId { value: 2 },
            annotations: vec![],
            src: None,
        };
        machine.invokes.push(used_invoke.name.name.clone());

        // Create a state that uses one invoke
        let state = StateDefinition {
            name: Identifier {
                name: "state1".to_string(),
            },
            id: NumericId { value: 3 },
            annotations: vec![],
            on_entry: vec![],
            on_exit: vec![],
            invokes: vec![used_invoke.name.clone()],
            regions: vec![],
            history: None,
            is_initial: false,
        };

        let states_block = StatesBlock {
            states: vec![state],
        };
        machine.states = Some(states_block);

        // Register the machine in the symbol table
        symbol_table.register_machine(&machine).unwrap();

        // Check for unused definitions
        let errors = symbol_table.check_unused_definitions();

        // Verify that only the unused invoke is reported
        assert_eq!(errors.len(), 1);
        match &errors[0] {
            ValidationError::UnusedDefinition { kind, name, scope } => {
                assert_eq!(kind, "invoke");
                assert_eq!(name, "unused_invoke");
                assert_eq!(scope, "test_machine");
            }
            _ => panic!("Expected UnusedDefinition error"),
        }
    }

    #[test]
    fn test_multiple_unused_definitions() {
        let mut symbol_table = SymbolTable::default();
        let mut machine = create_test_machine("test_machine");

        // Add unused definitions
        machine.actions.push("unused_action".to_string());
        machine.guards.push("unused_guard".to_string());
        machine.invokes.push("unused_invoke".to_string());

        // Create a state with no uses
        let state = StateDefinition {
            name: Identifier {
                name: "state1".to_string(),
            },
            id: NumericId { value: 1 },
            annotations: vec![],
            on_entry: vec![],
            on_exit: vec![],
            invokes: vec![],
            regions: vec![],
            history: None,
            is_initial: false,
        };

        let states_block = StatesBlock {
            states: vec![state],
        };
        machine.states = Some(states_block);

        // Register the machine in the symbol table
        symbol_table.register_machine(&machine).unwrap();

        // Check for unused definitions
        let errors = symbol_table.check_unused_definitions();

        // Verify that all unused definitions are reported
        assert_eq!(errors.len(), 3);

        // Sort errors by kind to make testing order-independent
        let mut sorted_errors = errors.clone();
        sorted_errors.sort_by(|a, b| match (a, b) {
            (
                ValidationError::UnusedDefinition { kind: k1, .. },
                ValidationError::UnusedDefinition { kind: k2, .. },
            ) => k1.cmp(k2),
            _ => panic!("Expected UnusedDefinition errors"),
        });

        match &sorted_errors[0] {
            ValidationError::UnusedDefinition { kind, name, scope } => {
                assert_eq!(kind, "action");
                assert_eq!(name, "unused_action");
                assert_eq!(scope, "test_machine");
            }
            _ => panic!("Expected UnusedDefinition error"),
        }

        match &sorted_errors[1] {
            ValidationError::UnusedDefinition { kind, name, scope } => {
                assert_eq!(kind, "guard");
                assert_eq!(name, "unused_guard");
                assert_eq!(scope, "test_machine");
            }
            _ => panic!("Expected UnusedDefinition error"),
        }

        match &sorted_errors[2] {
            ValidationError::UnusedDefinition { kind, name, scope } => {
                assert_eq!(kind, "invoke");
                assert_eq!(name, "unused_invoke");
                assert_eq!(scope, "test_machine");
            }
            _ => panic!("Expected UnusedDefinition error"),
        }
    }
}
