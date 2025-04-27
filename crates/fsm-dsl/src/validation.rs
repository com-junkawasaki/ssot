use crate::ast::{
    ActionDefinition, ActionsBlock, ActorDefinition, ActorsBlock, Annotation, AnnotationValue,
    Argument, AttributeDefinition, ChannelDefinition, CommunicationBlock, CommunicationItem,
    ContextDefinition, ContextFieldDefinition, DeploymentConfigBlock, DeploymentDefinition,
    DeploymentItem, Duration, EnumDefinition, EnumVariant, EnvironmentDefinition, EventDefinition,
    FieldDefinition, FileId, GuardDefinition, GuardsBlock, HistoryDefinition, HistoryType,
    Identifier, ImportStatement, InfrastructureDefinition, InterfaceDefinition, InvokeDefinition,
    InvokeSource, InvokeTransitionTarget, InvokesBlock, MachineDefinition, MachinesBlock,
    MethodDefinition, NumericId, ParameterDefinition, ProtocolDefinition, ServiceDefinition,
    ServiceItem, ServicesBlock, SsotAst, StateDefinition, StateInvokeDefinition, StatesBlock,
    StructDefinition, TimeUnit, TopLevelDefinition, TransitionDefinition, TransitionTarget,
    TypeDefinition, TypeSpecifier, TypesBlock, // Added missing imports like DeploymentItem
};
use std::collections::{HashMap, HashSet, VecDeque};
use std::fmt::{self, Display, Formatter};
use std::path::Path;
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
#[derive(Error, Debug, Clone, PartialEq, Eq)]
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

    #[error("Duplicate identifier '{identifier}' used as {kind}")]
    DuplicateIdentifier {
        identifier: String,
        kind: SymbolKind,
    },

    #[error("Undeclared identifier '{identifier}' used as {kind}")]
    UndeclaredIdentifier {
        identifier: String,
        kind: SymbolKind,
    },

    #[error("Cyclic dependency detected in {kind}: {cycle:?}")] // Format cycle with Debug
    CyclicDependency {
        kind: String,
        cycle: Vec<String>,
    },
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

    /// Checks for unused action, guard, and invoke definitions within each machine scope.
    fn check_unused_definitions(&self) -> Vec<ValidationError> {
        let mut errors = Vec::new();

        // Iterate through all registered scopes
        for (scope_name, scope_map) in &self.scoped_symbols {
            // Skip global scope, only check machine scopes
            if scope_name == Self::GLOBAL_SCOPE {
                // Check for unused global types, services, actors etc. if needed
                for (symbol_name, symbol_info) in scope_map {
                    match symbol_info.kind {
                        SymbolKind::Type(_) => {
                            if !self.referenced_types.contains(symbol_name) {
                                // errors.push(ValidationError::UnusedDefinition { ... });
                                // Decide if unused global types are an error
                            }
                        }
                        SymbolKind::Service | SymbolKind::Operation => {
                            if !self.referenced_services.contains(symbol_name) {
                                // errors.push(ValidationError::UnusedDefinition { ... });
                            }
                        }
                        SymbolKind::Actor => {
                            if !self.referenced_actors.contains(symbol_name) {
                                // errors.push(ValidationError::UnusedDefinition { ... });
                            }
                        }
                        SymbolKind::Channel => {
                            if !self.referenced_channels.contains(symbol_name) {
                                // errors.push(ValidationError::UnusedDefinition { ... });
                            }
                        }
                        SymbolKind::Protocol => {
                            if !self.referenced_protocols.contains(symbol_name) {
                                // errors.push(ValidationError::UnusedDefinition { ... });
                            }
                        }
                        _ => {} // Ignore other global kinds for now
                    }
                }
                continue; // Move to the next scope
            }

            // Assume other scopes are machine scopes
            let machine_name = scope_name; // Scope name is the machine name

            // Check unused actions in this machine scope
            if let Some(defined_actions) = self.scoped_symbols.get(machine_name) {
                for (action_name, symbol_info) in defined_actions {
                    if symbol_info.kind == SymbolKind::Action {
                        if !self.is_action_used(machine_name, action_name) {
                            errors.push(ValidationError::UnusedDefinition {
                                kind: "Action".to_string(),
                                name: action_name.clone(),
                                scope: machine_name.clone(),
                            });
                        }
                    }
                }
            }

            // Check unused guards in this machine scope
            if let Some(defined_guards) = self.scoped_symbols.get(machine_name) {
                for (guard_name, symbol_info) in defined_guards {
                    if symbol_info.kind == SymbolKind::Guard {
                        if !self.is_guard_used(machine_name, guard_name) {
                            errors.push(ValidationError::UnusedDefinition {
                                kind: "Guard".to_string(),
                                name: guard_name.clone(),
                                scope: machine_name.clone(),
                            });
                        }
                    }
                }
            }

            // Check unused invokes in this machine scope
            if let Some(defined_invokes) = self.scoped_symbols.get(machine_name) {
                for (invoke_name, symbol_info) in defined_invokes {
                    if symbol_info.kind == SymbolKind::Invoke {
                        if !self.is_invoke_used(machine_name, invoke_name) {
                            errors.push(ValidationError::UnusedDefinition {
                                kind: "Invoke".to_string(),
                                name: invoke_name.clone(),
                                scope: machine_name.clone(),
                            });
                        }
                    }
                }
            }
        }

        errors
    }

    // Helper to check if an action is referenced within a machine scope
    fn is_action_used(&self, machine_name: &str, action: &str) -> bool {
        self.referenced_actions
            .get(machine_name)
            .map_or(false, |refs| refs.contains(action))
    }

    // Helper to check if a guard is referenced within a machine scope
    fn is_guard_used(&self, machine_name: &str, guard: &str) -> bool {
        self.referenced_guards
            .get(machine_name)
            .map_or(false, |refs| refs.contains(guard))
    }

    // Helper to check if an invoke is referenced within a machine scope
    fn is_invoke_used(&self, machine_name: &str, invoke: &str) -> bool {
        self.referenced_invokes
            .get(machine_name)
            .map_or(false, |refs| refs.contains(invoke))
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
        machine: &MachineDefinition,
    ) -> Result<(), Vec<ValidationError>> {
        let mut errors = Vec::new();

        self.check_unused_actions(machine, &mut errors)?;
        self.validate_state_refs(machine, &mut errors)?;
        self.validate_invoke_source(machine, &mut errors)?;
        self.validate_actors_block_refs(machine, &mut errors)?;

        if errors.is_empty() {
            Ok(())
        } else {
            Err(errors)
        }
    }

    fn check_unused_actions(&mut self, machine: &MachineDefinition, errors: &mut Vec<ValidationError>) {
        if let Some(actions_block) = &machine.actions {
            let machine_scope = &machine.name.name;
            for action in &actions_block.definitions {
                if !self.is_action_referenced(machine_scope, &action.name.name) {
                    errors.push(ValidationError::UnusedDefinition {
                        kind: SymbolKind::Action,
                        name: action.name.name.clone(),
                        scope: machine_scope.clone(),
                    });
                }
            }
        }
    }

    fn validate_state_refs(
        &mut self,
        state_def: &StateDefinition,
        current_path: &[String],
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) {
        // ... (rest of function) ...
        // Validate entry/exit actions
        for action_ref in &state_def.on_entry { // Use on_entry
            if self
                .lookup_action(&action_ref.name, machine_scope, errors)
                .is_some()
            {
                self.mark_action_referenced(machine_scope, &action_ref.name);
            }
        }
        for action_ref in &state_def.on_exit { // Use on_exit
            if self
                .lookup_action(&action_ref.name, machine_scope, errors)
                .is_some()
            {
                self.mark_action_referenced(machine_scope, &action_ref.name);
            }
        }

        // ... (rest of function) ...
    }

    fn validate_invoke_source(
        &mut self,
        source: &InvokeSource,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) {
        match source {
            InvokeSource::Machine(ident) => {
                if self.lookup(&ident.name, None).is_none() { // Check global scope
                    errors.push(ValidationError::UndeclaredIdentifier {
                        identifier: ident.name.clone(),
                        kind: SymbolKind::Machine,
                    });
                }
                // TODO: Mark machine referenced if needed
            }
            InvokeSource::ServiceMethod(service_ident, method_ident) => {
                if self.lookup_service_or_interface(&service_ident.name, machine_scope, errors).is_some() {
                    self.mark_service_referenced(&service_ident.name);
                    // TODO: Validate method exists within service/interface (requires AST access)
                    // self.lookup_method(&method_ident.name, &service_ident.name, machine_scope, errors);
                }
            }
            InvokeSource::Literal(_) => {
                // Assume literal sources (function names, promise names) are valid for now.
            }
            // TODO: Add cases for other InvokeSource variants if they exist.
            // InvokeSource::Actor(ident) => { ... }
        }
    }

    fn validate_actors_block_refs(
        &mut self,
        block: &ActorsBlock,
        errors: &mut Vec<ValidationError>,
    ) {
        for actor in &block.definitions {
            for annotation in &actor.annotations {
                // Example: Check for a hypothetical $implementsType annotation
                // if let Annotation::ImplementsType(type_ident) = annotation { // Assuming ImplementsType variant exists
                //     if self.lookup_type(&type_ident.name, errors).is_some() {
                //         self.mark_type_referenced(&type_ident.name);
                //     }
                // }
                // Add checks for other relevant annotations...
            }
        }
    }

    fn validate_communication_item_refs(
        item: &CommunicationItem,
        symbol_table: &mut SymbolTable,
        errors: &mut Vec<ValidationError>,
    ) {
        let (item_name, annotations, fields_opt) = match item {
            CommunicationItem::Protocol(proto) => (&proto.name.name, &proto.annotations, None),
            CommunicationItem::Channel(chan) => (
                &chan.name.name,
                &chan.annotations,
                // Assuming parameters field exists and is Vec<ParameterDefinition>
                // Some(&chan.parameters) // Need to adjust based on actual ChannelDefinition structure
                None // Placeholder
            ),
            CommunicationItem::Event(event) => (&event.name.name, &event.annotations, Some(&event.fields)),
        };

        // Validate fields/parameters type specifiers
        if let Some(fields) = fields_opt {
            for field in fields {
                validate_type_specifier(&field.type_spec, symbol_table, errors);
            }
        }

        // Validate annotations like $protocol, $actor, etc.
        for annotation in annotations {
            match annotation {
                Annotation::Protocol(proto_ident) => {
                    if symbol_table
                        .lookup_protocol(&proto_ident.name, item_name, errors)
                        .is_some()
                    {
                        symbol_table.mark_protocol_referenced(&proto_ident.name);
                    }
                }
                 Annotation::Channel(chan_ident) => { // Used for Events referencing Channels
                     if symbol_table
                         .lookup_channel(&chan_ident.name, item_name, errors)
                         .is_some()
                     {
                         symbol_table.mark_channel_referenced(&chan_ident.name);
                     }
                 }
                // Annotation::Actor(actor_ident) => { // If actors can be related here
                //     if symbol_table
                //         .lookup_actor(&actor_ident.name, item_name, errors)
                //         .is_some()
                //     {
                //         symbol_table.mark_actor_referenced(&actor_ident.name);
                //     }
                // }
                _ => {}
            }
        }
    }
}
