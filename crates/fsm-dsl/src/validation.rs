use std::collections::{HashMap, HashSet};
use thiserror::Error;
use crate::ast::*; // Import necessary AST nodes

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
    DuplicateName {
        name: String,
        kind: String,
    },
    
    #[error("Machine '{machine_name}' must have exactly one initial state marker ($initial)")]
    MissingOrMultipleInitialStates {
        machine_name: String,
        count: usize,
    },

    #[error("Invalid transition target '{target_name}' in state '{state_name}' for event '{event_name}'")]
    InvalidTransitionTarget {
        state_name: String,
        event_name: String, // Or "after delay"
        target_name: String,
    },

    #[error("History transition target '{target_name}' is invalid or refers to a non-existent state")]
    InvalidHistoryTarget {
        target_name: String, // .history or Parent.history
        state_name: String, // State containing the transition
    },

    #[error("History state requires a default target state")]
    MissingHistoryDefaultTarget {
        state_name: String, // Composite state containing the history definition
    },
    
    #[error("Parallel state '{state_name}' must contain multiple regions (states blocks)")]
    ParallelStateRequiresRegions {
        state_name: String,
    },

    #[error("Annotation value for {annotation_name} on {element_name} is missing or invalid")]
    InvalidAnnotationValue {
        annotation_name: String,
        element_name: String,
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

    // TODO: Add more specific validation errors as needed.
    // - Type mismatch errors
    // - Unused definition warnings
    // - Import resolution errors
}

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
            defined_in_scope: if scope_name == Self::GLOBAL_SCOPE { None } else { Some(scope_name) },
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
    fn lookup_type(
        &self,
        name: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Type", None, |k| matches!(k, SymbolKind::Type(_)), errors)
    }

     /// Looks up a state within a specific machine scope.
    fn lookup_state(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "State", Some(machine_scope), |k| *k == SymbolKind::State, errors)
    }

     /// Looks up an action within a specific machine scope.
    fn lookup_action(
        &mut self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        let result = self.lookup_with_kind(name, "Action", Some(machine_scope), |k| *k == SymbolKind::Action, errors);
        if result.is_some() {
             self.referenced_actions.entry(machine_scope.to_string()).or_default().insert(name.to_string());
        }
        result
    }

    /// Looks up a guard within a specific machine scope.
    fn lookup_guard(
        &mut self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        let result = self.lookup_with_kind(name, "Guard", Some(machine_scope), |k| *k == SymbolKind::Guard, errors);
         if result.is_some() {
             self.referenced_guards.entry(machine_scope.to_string()).or_default().insert(name.to_string());
        }
        result
    }

    /// Looks up an invoke definition within a specific machine scope.
    fn lookup_invoke(
        &mut self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
         let result = self.lookup_with_kind(name, "Invoke", Some(machine_scope), |k| *k == SymbolKind::Invoke, errors);
          if result.is_some() {
             self.referenced_invokes.entry(machine_scope.to_string()).or_default().insert(name.to_string());
        }
         result
    }

    /// Looks up a type definition (struct, enum, alias) in the global scope.
    fn lookup_type(
        &self,
        name: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Type", None, |k| matches!(k, SymbolKind::Type(_)), errors)
    }

     /// Looks up a state within a specific machine scope.
    fn lookup_state(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "State", Some(machine_scope), |k| *k == SymbolKind::State, errors)
    }

    /// Looks up an action within a specific machine scope.
    fn lookup_action(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Action", Some(machine_scope), |k| *k == SymbolKind::Action, errors)
    }

    /// Looks up a guard within a specific machine scope.
    fn lookup_guard(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Guard", Some(machine_scope), |k| *k == SymbolKind::Guard, errors)
    }

    /// Looks up an invoke definition within a specific machine scope.
    fn lookup_invoke(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
         self.lookup_with_kind(name, "Invoke", Some(machine_scope), |k| *k == SymbolKind::Invoke, errors)
    }

    /// Looks up a type definition (struct, enum, alias) in the global scope.
    fn lookup_type(
        &self,
        name: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Type", None, |k| matches!(k, SymbolKind::Type(_)), errors)
    }

     /// Looks up a state within a specific machine scope.
    fn lookup_state(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "State", Some(machine_scope), |k| *k == SymbolKind::State, errors)
    }

    /// Looks up an action within a specific machine scope.
    fn lookup_action(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Action", Some(machine_scope), |k| *k == SymbolKind::Action, errors)
    }

    /// Looks up a guard within a specific machine scope.
    fn lookup_guard(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Guard", Some(machine_scope), |k| *k == SymbolKind::Guard, errors)
    }

    /// Looks up an invoke definition within a specific machine scope.
    fn lookup_invoke(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
         self.lookup_with_kind(name, "Invoke", Some(machine_scope), |k| *k == SymbolKind::Invoke, errors)
    }

    /// Looks up a type definition (struct, enum, alias) in the global scope.
    fn lookup_type(
        &self,
        name: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Type", None, |k| matches!(k, SymbolKind::Type(_)), errors)
    }

     /// Looks up a state within a specific machine scope.
    fn lookup_state(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "State", Some(machine_scope), |k| *k == SymbolKind::State, errors)
    }

    /// Looks up an action within a specific machine scope.
    fn lookup_action(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Action", Some(machine_scope), |k| *k == SymbolKind::Action, errors)
    }

    /// Looks up a guard within a specific machine scope.
    fn lookup_guard(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Guard", Some(machine_scope), |k| *k == SymbolKind::Guard, errors)
    }

    /// Looks up an invoke definition within a specific machine scope.
    fn lookup_invoke(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
         self.lookup_with_kind(name, "Invoke", Some(machine_scope), |k| *k == SymbolKind::Invoke, errors)
    }

    /// Looks up a type definition (struct, enum, alias) in the global scope.
    fn lookup_type(
        &self,
        name: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Type", None, |k| matches!(k, SymbolKind::Type(_)), errors)
    }

     /// Looks up a state within a specific machine scope.
    fn lookup_state(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "State", Some(machine_scope), |k| *k == SymbolKind::State, errors)
    }

    /// Looks up an action within a specific machine scope.
    fn lookup_action(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Action", Some(machine_scope), |k| *k == SymbolKind::Action, errors)
    }

    /// Looks up a guard within a specific machine scope.
    fn lookup_guard(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Guard", Some(machine_scope), |k| *k == SymbolKind::Guard, errors)
    }

    /// Looks up an invoke definition within a specific machine scope.
    fn lookup_invoke(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
         self.lookup_with_kind(name, "Invoke", Some(machine_scope), |k| *k == SymbolKind::Invoke, errors)
    }

    /// Looks up a type definition (struct, enum, alias) in the global scope.
    fn lookup_type(
        &self,
        name: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Type", None, |k| matches!(k, SymbolKind::Type(_)), errors)
    }

     /// Looks up a state within a specific machine scope.
    fn lookup_state(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "State", Some(machine_scope), |k| *k == SymbolKind::State, errors)
    }

    /// Looks up an action within a specific machine scope.
    fn lookup_action(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Action", Some(machine_scope), |k| *k == SymbolKind::Action, errors)
    }

    /// Looks up a guard within a specific machine scope.
    fn lookup_guard(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Guard", Some(machine_scope), |k| *k == SymbolKind::Guard, errors)
    }

    /// Looks up an invoke definition within a specific machine scope.
    fn lookup_invoke(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
         self.lookup_with_kind(name, "Invoke", Some(machine_scope), |k| *k == SymbolKind::Invoke, errors)
    }

    /// Looks up a type definition (struct, enum, alias) in the global scope.
    fn lookup_type(
        &self,
        name: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Type", None, |k| matches!(k, SymbolKind::Type(_)), errors)
    }

     /// Looks up a state within a specific machine scope.
    fn lookup_state(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "State", Some(machine_scope), |k| *k == SymbolKind::State, errors)
    }

    /// Looks up an action within a specific machine scope.
    fn lookup_action(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Action", Some(machine_scope), |k| *k == SymbolKind::Action, errors)
    }

    /// Looks up a guard within a specific machine scope.
    fn lookup_guard(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Guard", Some(machine_scope), |k| *k == SymbolKind::Guard, errors)
    }

    /// Looks up an invoke definition within a specific machine scope.
    fn lookup_invoke(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
         self.lookup_with_kind(name, "Invoke", Some(machine_scope), |k| *k == SymbolKind::Invoke, errors)
    }

    /// Looks up a type definition (struct, enum, alias) in the global scope.
    fn lookup_type(
        &self,
        name: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Type", None, |k| matches!(k, SymbolKind::Type(_)), errors)
    }

     /// Looks up a state within a specific machine scope.
    fn lookup_state(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "State", Some(machine_scope), |k| *k == SymbolKind::State, errors)
    }

    /// Looks up an action within a specific machine scope.
    fn lookup_action(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Action", Some(machine_scope), |k| *k == SymbolKind::Action, errors)
    }

    /// Looks up a guard within a specific machine scope.
    fn lookup_guard(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Guard", Some(machine_scope), |k| *k == SymbolKind::Guard, errors)
    }

    /// Looks up an invoke definition within a specific machine scope.
    fn lookup_invoke(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
         self.lookup_with_kind(name, "Invoke", Some(machine_scope), |k| *k == SymbolKind::Invoke, errors)
    }

    /// Looks up a type definition (struct, enum, alias) in the global scope.
    fn lookup_type(
        &self,
        name: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Type", None, |k| matches!(k, SymbolKind::Type(_)), errors)
    }

     /// Looks up a state within a specific machine scope.
    fn lookup_state(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "State", Some(machine_scope), |k| *k == SymbolKind::State, errors)
    }

    /// Looks up an action within a specific machine scope.
    fn lookup_action(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Action", Some(machine_scope), |k| *k == SymbolKind::Action, errors)
    }

    /// Looks up a guard within a specific machine scope.
    fn lookup_guard(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Guard", Some(machine_scope), |k| *k == SymbolKind::Guard, errors)
    }

    /// Looks up an invoke definition within a specific machine scope.
    fn lookup_invoke(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
         self.lookup_with_kind(name, "Invoke", Some(machine_scope), |k| *k == SymbolKind::Invoke, errors)
    }

    /// Looks up a type definition (struct, enum, alias) in the global scope.
    fn lookup_type(
        &self,
        name: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Type", None, |k| matches!(k, SymbolKind::Type(_)), errors)
    }

     /// Looks up a state within a specific machine scope.
    fn lookup_state(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "State", Some(machine_scope), |k| *k == SymbolKind::State, errors)
    }

    /// Looks up an action within a specific machine scope.
    fn lookup_action(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Action", Some(machine_scope), |k| *k == SymbolKind::Action, errors)
    }

    /// Looks up a guard within a specific machine scope.
    fn lookup_guard(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Guard", Some(machine_scope), |k| *k == SymbolKind::Guard, errors)
    }

    /// Looks up an invoke definition within a specific machine scope.
    fn lookup_invoke(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
         self.lookup_with_kind(name, "Invoke", Some(machine_scope), |k| *k == SymbolKind::Invoke, errors)
    }

    /// Looks up a type definition (struct, enum, alias) in the global scope.
    fn lookup_type(
        &self,
        name: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Type", None, |k| matches!(k, SymbolKind::Type(_)), errors)
    }

     /// Looks up a state within a specific machine scope.
    fn lookup_state(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "State", Some(machine_scope), |k| *k == SymbolKind::State, errors)
    }

    /// Looks up an action within a specific machine scope.
    fn lookup_action(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Action", Some(machine_scope), |k| *k == SymbolKind::Action, errors)
    }

    /// Looks up a guard within a specific machine scope.
    fn lookup_guard(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Guard", Some(machine_scope), |k| *k == SymbolKind::Guard, errors)
    }

    /// Looks up an invoke definition within a specific machine scope.
    fn lookup_invoke(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
         self.lookup_with_kind(name, "Invoke", Some(machine_scope), |k| *k == SymbolKind::Invoke, errors)
    }

    /// Looks up a type definition (struct, enum, alias) in the global scope.
    fn lookup_type(
        &self,
        name: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Type", None, |k| matches!(k, SymbolKind::Type(_)), errors)
    }

     /// Looks up a state within a specific machine scope.
    fn lookup_state(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "State", Some(machine_scope), |k| *k == SymbolKind::State, errors)
    }

    /// Looks up an action within a specific machine scope.
    fn lookup_action(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Action", Some(machine_scope), |k| *k == SymbolKind::Action, errors)
    }

    /// Looks up a guard within a specific machine scope.
    fn lookup_guard(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Guard", Some(machine_scope), |k| *k == SymbolKind::Guard, errors)
    }

    /// Looks up an invoke definition within a specific machine scope.
    fn lookup_invoke(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
         self.lookup_with_kind(name, "Invoke", Some(machine_scope), |k| *k == SymbolKind::Invoke, errors)
    }

    /// Looks up a type definition (struct, enum, alias) in the global scope.
    fn lookup_type(
        &self,
        name: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Type", None, |k| matches!(k, SymbolKind::Type(_)), errors)
    }

     /// Looks up a state within a specific machine scope.
    fn lookup_state(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "State", Some(machine_scope), |k| *k == SymbolKind::State, errors)
    }

    /// Looks up an action within a specific machine scope.
    fn lookup_action(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Action", Some(machine_scope), |k| *k == SymbolKind::Action, errors)
    }

    /// Looks up a guard within a specific machine scope.
    fn lookup_guard(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Guard", Some(machine_scope), |k| *k == SymbolKind::Guard, errors)
    }

    /// Looks up an invoke definition within a specific machine scope.
    fn lookup_invoke(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
         self.lookup_with_kind(name, "Invoke", Some(machine_scope), |k| *k == SymbolKind::Invoke, errors)
    }

    /// Looks up a type definition (struct, enum, alias) in the global scope.
    fn lookup_type(
        &self,
        name: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Type", None, |k| matches!(k, SymbolKind::Type(_)), errors)
    }

     /// Looks up a state within a specific machine scope.
    fn lookup_state(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "State", Some(machine_scope), |k| *k == SymbolKind::State, errors)
    }

    /// Looks up an action within a specific machine scope.
    fn lookup_action(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Action", Some(machine_scope), |k| *k == SymbolKind::Action, errors)
    }

    /// Looks up a guard within a specific machine scope.
    fn lookup_guard(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Guard", Some(machine_scope), |k| *k == SymbolKind::Guard, errors)
    }

    /// Looks up an invoke definition within a specific machine scope.
    fn lookup_invoke(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
         self.lookup_with_kind(name, "Invoke", Some(machine_scope), |k| *k == SymbolKind::Invoke, errors)
    }

    /// Looks up a type definition (struct, enum, alias) in the global scope.
    fn lookup_type(
        &self,
        name: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Type", None, |k| matches!(k, SymbolKind::Type(_)), errors)
    }

     /// Looks up a state within a specific machine scope.
    fn lookup_state(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "State", Some(machine_scope), |k| *k == SymbolKind::State, errors)
    }

    /// Looks up an action within a specific machine scope.
    fn lookup_action(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Action", Some(machine_scope), |k| *k == SymbolKind::Action, errors)
    }

    /// Looks up a guard within a specific machine scope.
    fn lookup_guard(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Guard", Some(machine_scope), |k| *k == SymbolKind::Guard, errors)
    }

    /// Looks up an invoke definition within a specific machine scope.
    fn lookup_invoke(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
         self.lookup_with_kind(name, "Invoke", Some(machine_scope), |k| *k == SymbolKind::Invoke, errors)
    }

    /// Looks up a type definition (struct, enum, alias) in the global scope.
    fn lookup_type(
        &self,
        name: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Type", None, |k| matches!(k, SymbolKind::Type(_)), errors)
    }

     /// Looks up a state within a specific machine scope.
    fn lookup_state(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "State", Some(machine_scope), |k| *k == SymbolKind::State, errors)
    }

    /// Looks up an action within a specific machine scope.
    fn lookup_action(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Action", Some(machine_scope), |k| *k == SymbolKind::Action, errors)
    }

    /// Looks up a guard within a specific machine scope.
    fn lookup_guard(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Guard", Some(machine_scope), |k| *k == SymbolKind::Guard, errors)
    }

    /// Looks up an invoke definition within a specific machine scope.
    fn lookup_invoke(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
         self.lookup_with_kind(name, "Invoke", Some(machine_scope), |k| *k == SymbolKind::Invoke, errors)
    }

    /// Looks up a type definition (struct, enum, alias) in the global scope.
    fn lookup_type(
        &self,
        name: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Type", None, |k| matches!(k, SymbolKind::Type(_)), errors)
    }

     /// Looks up a state within a specific machine scope.
    fn lookup_state(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "State", Some(machine_scope), |k| *k == SymbolKind::State, errors)
    }

    /// Looks up an action within a specific machine scope.
    fn lookup_action(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Action", Some(machine_scope), |k| *k == SymbolKind::Action, errors)
    }

    /// Looks up a guard within a specific machine scope.
    fn lookup_guard(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Guard", Some(machine_scope), |k| *k == SymbolKind::Guard, errors)
    }

    /// Looks up an invoke definition within a specific machine scope.
    fn lookup_invoke(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
         self.lookup_with_kind(name, "Invoke", Some(machine_scope), |k| *k == SymbolKind::Invoke, errors)
    }

    /// Looks up a type definition (struct, enum, alias) in the global scope.
    fn lookup_type(
        &self,
        name: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Type", None, |k| matches!(k, SymbolKind::Type(_)), errors)
    }

     /// Looks up a state within a specific machine scope.
    fn lookup_state(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "State", Some(machine_scope), |k| *k == SymbolKind::State, errors)
    }

    /// Looks up an action within a specific machine scope.
    fn lookup_action(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Action", Some(machine_scope), |k| *k == SymbolKind::Action, errors)
    }

    /// Looks up a guard within a specific machine scope.
    fn lookup_guard(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Guard", Some(machine_scope), |k| *k == SymbolKind::Guard, errors)
    }

    /// Looks up an invoke definition within a specific machine scope.
    fn lookup_invoke(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
         self.lookup_with_kind(name, "Invoke", Some(machine_scope), |k| *k == SymbolKind::Invoke, errors)
    }

    /// Looks up a type definition (struct, enum, alias) in the global scope.
    fn lookup_type(
        &self,
        name: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Type", None, |k| matches!(k, SymbolKind::Type(_)), errors)
    }

     /// Looks up a state within a specific machine scope.
    fn lookup_state(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "State", Some(machine_scope), |k| *k == SymbolKind::State, errors)
    }

    /// Looks up an action within a specific machine scope.
    fn lookup_action(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Action", Some(machine_scope), |k| *k == SymbolKind::Action, errors)
    }

    /// Looks up a guard within a specific machine scope.
    fn lookup_guard(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Guard", Some(machine_scope), |k| *k == SymbolKind::Guard, errors)
    }

    /// Looks up an invoke definition within a specific machine scope.
    fn lookup_invoke(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
         self.lookup_with_kind(name, "Invoke", Some(machine_scope), |k| *k == SymbolKind::Invoke, errors)
    }

    /// Looks up a type definition (struct, enum, alias) in the global scope.
    fn lookup_type(
        &self,
        name: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Type", None, |k| matches!(k, SymbolKind::Type(_)), errors)
    }

     /// Looks up a state within a specific machine scope.
    fn lookup_state(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "State", Some(machine_scope), |k| *k == SymbolKind::State, errors)
    }

    /// Looks up an action within a specific machine scope.
    fn lookup_action(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Action", Some(machine_scope), |k| *k == SymbolKind::Action, errors)
    }

    /// Looks up a guard within a specific machine scope.
    fn lookup_guard(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Guard", Some(machine_scope), |k| *k == SymbolKind::Guard, errors)
    }

    /// Looks up an invoke definition within a specific machine scope.
    fn lookup_invoke(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
         self.lookup_with_kind(name, "Invoke", Some(machine_scope), |k| *k == SymbolKind::Invoke, errors)
    }

    /// Looks up a type definition (struct, enum, alias) in the global scope.
    fn lookup_type(
        &self,
        name: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Type", None, |k| matches!(k, SymbolKind::Type(_)), errors)
    }

     /// Looks up a state within a specific machine scope.
    fn lookup_state(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "State", Some(machine_scope), |k| *k == SymbolKind::State, errors)
    }

    /// Looks up an action within a specific machine scope.
    fn lookup_action(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Action", Some(machine_scope), |k| *k == SymbolKind::Action, errors)
    }

    /// Looks up a guard within a specific machine scope.
    fn lookup_guard(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Guard", Some(machine_scope), |k| *k == SymbolKind::Guard, errors)
    }

    /// Looks up an invoke definition within a specific machine scope.
    fn lookup_invoke(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
         self.lookup_with_kind(name, "Invoke", Some(machine_scope), |k| *k == SymbolKind::Invoke, errors)
    }

    /// Looks up a type definition (struct, enum, alias) in the global scope.
    fn lookup_type(
        &self,
        name: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Type", None, |k| matches!(k, SymbolKind::Type(_)), errors)
    }

     /// Looks up a state within a specific machine scope.
    fn lookup_state(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "State", Some(machine_scope), |k| *k == SymbolKind::State, errors)
    }

    /// Looks up an action within a specific machine scope.
    fn lookup_action(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Action", Some(machine_scope), |k| *k == SymbolKind::Action, errors)
    }

    /// Looks up a guard within a specific machine scope.
    fn lookup_guard(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Guard", Some(machine_scope), |k| *k == SymbolKind::Guard, errors)
    }

    /// Looks up an invoke definition within a specific machine scope.
    fn lookup_invoke(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
         self.lookup_with_kind(name, "Invoke", Some(machine_scope), |k| *k == SymbolKind::Invoke, errors)
    }

    /// Looks up a type definition (struct, enum, alias) in the global scope.
    fn lookup_type(
        &self,
        name: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Type", None, |k| matches!(k, SymbolKind::Type(_)), errors)
    }

     /// Looks up a state within a specific machine scope.
    fn lookup_state(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "State", Some(machine_scope), |k| *k == SymbolKind::State, errors)
    }

    /// Looks up an action within a specific machine scope.
    fn lookup_action(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Action", Some(machine_scope), |k| *k == SymbolKind::Action, errors)
    }

    /// Looks up a guard within a specific machine scope.
    fn lookup_guard(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Guard", Some(machine_scope), |k| *k == SymbolKind::Guard, errors)
    }

    /// Looks up an invoke definition within a specific machine scope.
    fn lookup_invoke(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
         self.lookup_with_kind(name, "Invoke", Some(machine_scope), |k| *k == SymbolKind::Invoke, errors)
    }

    /// Looks up a type definition (struct, enum, alias) in the global scope.
    fn lookup_type(
        &self,
        name: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Type", None, |k| matches!(k, SymbolKind::Type(_)), errors)
    }

     /// Looks up a state within a specific machine scope.
    fn lookup_state(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "State", Some(machine_scope), |k| *k == SymbolKind::State, errors)
    }

    /// Looks up an action within a specific machine scope.
    fn lookup_action(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Action", Some(machine_scope), |k| *k == SymbolKind::Action, errors)
    }

    /// Looks up a guard within a specific machine scope.
    fn lookup_guard(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Guard", Some(machine_scope), |k| *k == SymbolKind::Guard, errors)
    }

    /// Looks up an invoke definition within a specific machine scope.
    fn lookup_invoke(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
         self.lookup_with_kind(name, "Invoke", Some(machine_scope), |k| *k == SymbolKind::Invoke, errors)
    }

    /// Looks up a type definition (struct, enum, alias) in the global scope.
    fn lookup_type(
        &self,
        name: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Type", None, |k| matches!(k, SymbolKind::Type(_)), errors)
    }

     /// Looks up a state within a specific machine scope.
    fn lookup_state(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "State", Some(machine_scope), |k| *k == SymbolKind::State, errors)
    }

    /// Looks up an action within a specific machine scope.
    fn lookup_action(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Action", Some(machine_scope), |k| *k == SymbolKind::Action, errors)
    }

    /// Looks up a guard within a specific machine scope.
    fn lookup_guard(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Guard", Some(machine_scope), |k| *k == SymbolKind::Guard, errors)
    }

    /// Looks up an invoke definition within a specific machine scope.
    fn lookup_invoke(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
         self.lookup_with_kind(name, "Invoke", Some(machine_scope), |k| *k == SymbolKind::Invoke, errors)
    }

    /// Looks up a type definition (struct, enum, alias) in the global scope.
    fn lookup_type(
        &self,
        name: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Type", None, |k| matches!(k, SymbolKind::Type(_)), errors)
    }

     /// Looks up a state within a specific machine scope.
    fn lookup_state(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "State", Some(machine_scope), |k| *k == SymbolKind::State, errors)
    }

    /// Looks up an action within a specific machine scope.
    fn lookup_action(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Action", Some(machine_scope), |k| *k == SymbolKind::Action, errors)
    }

    /// Looks up a guard within a specific machine scope.
    fn lookup_guard(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Guard", Some(machine_scope), |k| *k == SymbolKind::Guard, errors)
    }

    /// Looks up an invoke definition within a specific machine scope.
    fn lookup_invoke(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
         self.lookup_with_kind(name, "Invoke", Some(machine_scope), |k| *k == SymbolKind::Invoke, errors)
    }

    /// Looks up a type definition (struct, enum, alias) in the global scope.
    fn lookup_type(
        &self,
        name: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Type", None, |k| matches!(k, SymbolKind::Type(_)), errors)
    }

     /// Looks up a state within a specific machine scope.
    fn lookup_state(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "State", Some(machine_scope), |k| *k == SymbolKind::State, errors)
    }

    /// Looks up an action within a specific machine scope.
    fn lookup_action(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Action", Some(machine_scope), |k| *k == SymbolKind::Action, errors)
    }

    /// Looks up a guard within a specific machine scope.
    fn lookup_guard(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Guard", Some(machine_scope), |k| *k == SymbolKind::Guard, errors)
    }

    /// Looks up an invoke definition within a specific machine scope.
    fn lookup_invoke(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
         self.lookup_with_kind(name, "Invoke", Some(machine_scope), |k| *k == SymbolKind::Invoke, errors)
    }

    /// Looks up a type definition (struct, enum, alias) in the global scope.
    fn lookup_type(
        &self,
        name: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Type", None, |k| matches!(k, SymbolKind::Type(_)), errors)
    }

     /// Looks up a state within a specific machine scope.
    fn lookup_state(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "State", Some(machine_scope), |k| *k == SymbolKind::State, errors)
    }

    /// Looks up an action within a specific machine scope.
    fn lookup_action(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Action", Some(machine_scope), |k| *k == SymbolKind::Action, errors)
    }

    /// Looks up a guard within a specific machine scope.
    fn lookup_guard(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Guard", Some(machine_scope), |k| *k == SymbolKind::Guard, errors)
    }

    /// Looks up an invoke definition within a specific machine scope.
    fn lookup_invoke(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
         self.lookup_with_kind(name, "Invoke", Some(machine_scope), |k| *k == SymbolKind::Invoke, errors)
    }

    /// Looks up a type definition (struct, enum, alias) in the global scope.
    fn lookup_type(
        &self,
        name: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Type", None, |k| matches!(k, SymbolKind::Type(_)), errors)
    }

     /// Looks up a state within a specific machine scope.
    fn lookup_state(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "State", Some(machine_scope), |k| *k == SymbolKind::State, errors)
    }

    /// Looks up an action within a specific machine scope.
    fn lookup_action(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Action", Some(machine_scope), |k| *k == SymbolKind::Action, errors)
    }

    /// Looks up a guard within a specific machine scope.
    fn lookup_guard(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Guard", Some(machine_scope), |k| *k == SymbolKind::Guard, errors)
    }

    /// Looks up an invoke definition within a specific machine scope.
    fn lookup_invoke(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
         self.lookup_with_kind(name, "Invoke", Some(machine_scope), |k| *k == SymbolKind::Invoke, errors)
    }

    /// Looks up a type definition (struct, enum, alias) in the global scope.
    fn lookup_type(
        &self,
        name: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Type", None, |k| matches!(k, SymbolKind::Type(_)), errors)
    }

     /// Looks up a state within a specific machine scope.
    fn lookup_state(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "State", Some(machine_scope), |k| *k == SymbolKind::State, errors)
    }

    /// Looks up an action within a specific machine scope.
    fn lookup_action(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Action", Some(machine_scope), |k| *k == SymbolKind::Action, errors)
    }

    /// Looks up a guard within a specific machine scope.
    fn lookup_guard(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Guard", Some(machine_scope), |k| *k == SymbolKind::Guard, errors)
    }

    /// Looks up an invoke definition within a specific machine scope.
    fn lookup_invoke(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
         self.lookup_with_kind(name, "Invoke", Some(machine_scope), |k| *k == SymbolKind::Invoke, errors)
    }

    /// Looks up a type definition (struct, enum, alias) in the global scope.
    fn lookup_type(
        &self,
        name: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Type", None, |k| matches!(k, SymbolKind::Type(_)), errors)
    }

     /// Looks up a state within a specific machine scope.
    fn lookup_state(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "State", Some(machine_scope), |k| *k == SymbolKind::State, errors)
    }

    /// Looks up an action within a specific machine scope.
    fn lookup_action(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Action", Some(machine_scope), |k| *k == SymbolKind::Action, errors)
    }

    /// Looks up a guard within a specific machine scope.
    fn lookup_guard(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Guard", Some(machine_scope), |k| *k == SymbolKind::Guard, errors)
    }

    /// Looks up an invoke definition within a specific machine scope.
    fn lookup_invoke(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
         self.lookup_with_kind(name, "Invoke", Some(machine_scope), |k| *k == SymbolKind::Invoke, errors)
    }

    /// Looks up a type definition (struct, enum, alias) in the global scope.
    fn lookup_type(
        &self,
        name: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Type", None, |k| matches!(k, SymbolKind::Type(_)), errors)
    }

     /// Looks up a state within a specific machine scope.
    fn lookup_state(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "State", Some(machine_scope), |k| *k == SymbolKind::State, errors)
    }

    /// Looks up an action within a specific machine scope.
    fn lookup_action(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Action", Some(machine_scope), |k| *k == SymbolKind::Action, errors)
    }

    /// Looks up a guard within a specific machine scope.
    fn lookup_guard(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Guard", Some(machine_scope), |k| *k == SymbolKind::Guard, errors)
    }

    /// Looks up an invoke definition within a specific machine scope.
    fn lookup_invoke(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
         self.lookup_with_kind(name, "Invoke", Some(machine_scope), |k| *k == SymbolKind::Invoke, errors)
    }

    /// Looks up a type definition (struct, enum, alias) in the global scope.
    fn lookup_type(
        &self,
        name: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Type", None, |k| matches!(k, SymbolKind::Type(_)), errors)
    }

     /// Looks up a state within a specific machine scope.
    fn lookup_state(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "State", Some(machine_scope), |k| *k == SymbolKind::State, errors)
    }

    /// Looks up an action within a specific machine scope.
    fn lookup_action(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Action", Some(machine_scope), |k| *k == SymbolKind::Action, errors)
    }

    /// Looks up a guard within a specific machine scope.
    fn lookup_guard(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Guard", Some(machine_scope), |k| *k == SymbolKind::Guard, errors)
    }

    /// Looks up an invoke definition within a specific machine scope.
    fn lookup_invoke(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
         self.lookup_with_kind(name, "Invoke", Some(machine_scope), |k| *k == SymbolKind::Invoke, errors)
    }

    /// Looks up a type definition (struct, enum, alias) in the global scope.
    fn lookup_type(
        &self,
        name: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Type", None, |k| matches!(k, SymbolKind::Type(_)), errors)
    }

     /// Looks up a state within a specific machine scope.
    fn lookup_state(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "State", Some(machine_scope), |k| *k == SymbolKind::State, errors)
    }

    /// Looks up an action within a specific machine scope.
    fn lookup_action(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Action", Some(machine_scope), |k| *k == SymbolKind::Action, errors)
    }

    /// Looks up a guard within a specific machine scope.
    fn lookup_guard(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Guard", Some(machine_scope), |k| *k == SymbolKind::Guard, errors)
    }

    /// Looks up an invoke definition within a specific machine scope.
    fn lookup_invoke(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
         self.lookup_with_kind(name, "Invoke", Some(machine_scope), |k| *k == SymbolKind::Invoke, errors)
    }

    /// Looks up a type definition (struct, enum, alias) in the global scope.
    fn lookup_type(
        &self,
        name: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Type", None, |k| matches!(k, SymbolKind::Type(_)), errors)
    }

     /// Looks up a state within a specific machine scope.
    fn lookup_state(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "State", Some(machine_scope), |k| *k == SymbolKind::State, errors)
    }

    /// Looks up an action within a specific machine scope.
    fn lookup_action(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Action", Some(machine_scope), |k| *k == SymbolKind::Action, errors)
    }

    /// Looks up a guard within a specific machine scope.
    fn lookup_guard(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Guard", Some(machine_scope), |k| *k == SymbolKind::Guard, errors)
    }

    /// Looks up an invoke definition within a specific machine scope.
    fn lookup_invoke(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
         self.lookup_with_kind(name, "Invoke", Some(machine_scope), |k| *k == SymbolKind::Invoke, errors)
    }

    /// Looks up a type definition (struct, enum, alias) in the global scope.
    fn lookup_type(
        &self,
        name: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Type", None, |k| matches!(k, SymbolKind::Type(_)), errors)
    }

     /// Looks up a state within a specific machine scope.
    fn lookup_state(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "State", Some(machine_scope), |k| *k == SymbolKind::State, errors)
    }

    /// Looks up an action within a specific machine scope.
    fn lookup_action(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Action", Some(machine_scope), |k| *k == SymbolKind::Action, errors)
    }

    /// Looks up a guard within a specific machine scope.
    fn lookup_guard(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Guard", Some(machine_scope), |k| *k == SymbolKind::Guard, errors)
    }

    /// Looks up an invoke definition within a specific machine scope.
    fn lookup_invoke(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
         self.lookup_with_kind(name, "Invoke", Some(machine_scope), |k| *k == SymbolKind::Invoke, errors)
    }

    /// Looks up a type definition (struct, enum, alias) in the global scope.
    fn lookup_type(
        &self,
        name: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Type", None, |k| matches!(k, SymbolKind::Type(_)), errors)
    }

     /// Looks up a state within a specific machine scope.
    fn lookup_state(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "State", Some(machine_scope), |k| *k == SymbolKind::State, errors)
    }

    /// Looks up an action within a specific machine scope.
    fn lookup_action(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Action", Some(machine_scope), |k| *k == SymbolKind::Action, errors)
    }

    /// Looks up a guard within a specific machine scope.
    fn lookup_guard(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Guard", Some(machine_scope), |k| *k == SymbolKind::Guard, errors)
    }

    /// Looks up an invoke definition within a specific machine scope.
    fn lookup_invoke(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
         self.lookup_with_kind(name, "Invoke", Some(machine_scope), |k| *k == SymbolKind::Invoke, errors)
    }

    /// Looks up a type definition (struct, enum, alias) in the global scope.
    fn lookup_type(
        &self,
        name: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Type", None, |k| matches!(k, SymbolKind::Type(_)), errors)
    }

     /// Looks up a state within a specific machine scope.
    fn lookup_state(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "State", Some(machine_scope), |k| *k == SymbolKind::State, errors)
    }

    /// Looks up an action within a specific machine scope.
    fn lookup_action(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Action", Some(machine_scope), |k| *k == SymbolKind::Action, errors)
    }

    /// Looks up a guard within a specific machine scope.
    fn lookup_guard(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Guard", Some(machine_scope), |k| *k == SymbolKind::Guard, errors)
    }

    /// Looks up an invoke definition within a specific machine scope.
    fn lookup_invoke(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
         self.lookup_with_kind(name, "Invoke", Some(machine_scope), |k| *k == SymbolKind::Invoke, errors)
    }

    /// Looks up a type definition (struct, enum, alias) in the global scope.
    fn lookup_type(
        &self,
        name: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Type", None, |k| matches!(k, SymbolKind::Type(_)), errors)
    }

     /// Looks up a state within a specific machine scope.
    fn lookup_state(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "State", Some(machine_scope), |k| *k == SymbolKind::State, errors)
    }

    /// Looks up an action within a specific machine scope.
    fn lookup_action(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Action", Some(machine_scope), |k| *k == SymbolKind::Action, errors)
    }

    /// Looks up a guard within a specific machine scope.
    fn lookup_guard(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Guard", Some(machine_scope), |k| *k == SymbolKind::Guard, errors)
    }

    /// Looks up an invoke definition within a specific machine scope.
    fn lookup_invoke(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
         self.lookup_with_kind(name, "Invoke", Some(machine_scope), |k| *k == SymbolKind::Invoke, errors)
    }

    /// Looks up a type definition (struct, enum, alias) in the global scope.
    fn lookup_type(
        &self,
        name: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Type", None, |k| matches!(k, SymbolKind::Type(_)), errors)
    }

     /// Looks up a state within a specific machine scope.
    fn lookup_state(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "State", Some(machine_scope), |k| *k == SymbolKind::State, errors)
    }

    /// Looks up an action within a specific machine scope.
    fn lookup_action(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Action", Some(machine_scope), |k| *k == SymbolKind::Action, errors)
    }

    /// Looks up a guard within a specific machine scope.
    fn lookup_guard(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Guard", Some(machine_scope), |k| *k == SymbolKind::Guard, errors)
    }

    /// Looks up an invoke definition within a specific machine scope.
    fn lookup_invoke(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
         self.lookup_with_kind(name, "Invoke", Some(machine_scope), |k| *k == SymbolKind::Invoke, errors)
    }

    /// Looks up a type definition (struct, enum, alias) in the global scope.
    fn lookup_type(
        &self,
        name: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Type", None, |k| matches!(k, SymbolKind::Type(_)), errors)
    }

     /// Looks up a state within a specific machine scope.
    fn lookup_state(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "State", Some(machine_scope), |k| *k == SymbolKind::State, errors)
    }

    /// Looks up an action within a specific machine scope.
    fn lookup_action(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Action", Some(machine_scope), |k| *k == SymbolKind::Action, errors)
    }

    /// Looks up a guard within a specific machine scope.
    fn lookup_guard(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Guard", Some(machine_scope), |k| *k == SymbolKind::Guard, errors)
    }

    /// Looks up an invoke definition within a specific machine scope.
    fn lookup_invoke(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
         self.lookup_with_kind(name, "Invoke", Some(machine_scope), |k| *k == SymbolKind::Invoke, errors)
    }

    /// Looks up a type definition (struct, enum, alias) in the global scope.
    fn lookup_type(
        &self,
        name: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Type", None, |k| matches!(k, SymbolKind::Type(_)), errors)
    }

     /// Looks up a state within a specific machine scope.
    fn lookup_state(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "State", Some(machine_scope), |k| *k == SymbolKind::State, errors)
    }

    /// Looks up an action within a specific machine scope.
    fn lookup_action(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Action", Some(machine_scope), |k| *k == SymbolKind::Action, errors)
    }

    /// Looks up a guard within a specific machine scope.
    fn lookup_guard(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Guard", Some(machine_scope), |k| *k == SymbolKind::Guard, errors)
    }

    /// Looks up an invoke definition within a specific machine scope.
    fn lookup_invoke(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
         self.lookup_with_kind(name, "Invoke", Some(machine_scope), |k| *k == SymbolKind::Invoke, errors)
    }

    /// Looks up a type definition (struct, enum, alias) in the global scope.
    fn lookup_type(
        &self,
        name: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Type", None, |k| matches!(k, SymbolKind::Type(_)), errors)
    }

     /// Looks up a state within a specific machine scope.
    fn lookup_state(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "State", Some(machine_scope), |k| *k == SymbolKind::State, errors)
    }

    /// Looks up an action within a specific machine scope.
    fn lookup_action(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Action", Some(machine_scope), |k| *k == SymbolKind::Action, errors)
    }

    /// Looks up a guard within a specific machine scope.
    fn lookup_guard(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Guard", Some(machine_scope), |k| *k == SymbolKind::Guard, errors)
    }

    /// Looks up an invoke definition within a specific machine scope.
    fn lookup_invoke(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
         self.lookup_with_kind(name, "Invoke", Some(machine_scope), |k| *k == SymbolKind::Invoke, errors)
    }

    /// Looks up a type definition (struct, enum, alias) in the global scope.
    fn lookup_type(
        &self,
        name: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Type", None, |k| matches!(k, SymbolKind::Type(_)), errors)
    }

     /// Looks up a state within a specific machine scope.
    fn lookup_state(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "State", Some(machine_scope), |k| *k == SymbolKind::State, errors)
    }

    /// Looks up an action within a specific machine scope.
    fn lookup_action(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Action", Some(machine_scope), |k| *k == SymbolKind::Action, errors)
    }

    /// Looks up a guard within a specific machine scope.
    fn lookup_guard(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Guard", Some(machine_scope), |k| *k == SymbolKind::Guard, errors)
    }

    /// Looks up an invoke definition within a specific machine scope.
    fn lookup_invoke(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
         self.lookup_with_kind(name, "Invoke", Some(machine_scope), |k| *k == SymbolKind::Invoke, errors)
    }

    /// Looks up a type definition (struct, enum, alias) in the global scope.
    fn lookup_type(
        &self,
        name: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Type", None, |k| matches!(k, SymbolKind::Type(_)), errors)
    }

     /// Looks up a state within a specific machine scope.
    fn lookup_state(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "State", Some(machine_scope), |k| *k == SymbolKind::State, errors)
    }

    /// Looks up an action within a specific machine scope.
    fn lookup_action(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Action", Some(machine_scope), |k| *k == SymbolKind::Action, errors)
    }

    /// Looks up a guard within a specific machine scope.
    fn lookup_guard(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Guard", Some(machine_scope), |k| *k == SymbolKind::Guard, errors)
    }

    /// Looks up an invoke definition within a specific machine scope.
    fn lookup_invoke(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
         self.lookup_with_kind(name, "Invoke", Some(machine_scope), |k| *k == SymbolKind::Invoke, errors)
    }

    /// Looks up a type definition (struct, enum, alias) in the global scope.
    fn lookup_type(
        &self,
        name: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Type", None, |k| matches!(k, SymbolKind::Type(_)), errors)
    }

     /// Looks up a state within a specific machine scope.
    fn lookup_state(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "State", Some(machine_scope), |k| *k == SymbolKind::State, errors)
    }

    /// Looks up an action within a specific machine scope.
    fn lookup_action(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Action", Some(machine_scope), |k| *k == SymbolKind::Action, errors)
    }

    /// Looks up a guard within a specific machine scope.
    fn lookup_guard(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Guard", Some(machine_scope), |k| *k == SymbolKind::Guard, errors)
    }

    /// Looks up an invoke definition within a specific machine scope.
    fn lookup_invoke(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
         self.lookup_with_kind(name, "Invoke", Some(machine_scope), |k| *k == SymbolKind::Invoke, errors)
    }

    /// Looks up a type definition (struct, enum, alias) in the global scope.
    fn lookup_type(
        &self,
        name: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Type", None, |k| matches!(k, SymbolKind::Type(_)), errors)
    }

     /// Looks up a state within a specific machine scope.
    fn lookup_state(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "State", Some(machine_scope), |k| *k == SymbolKind::State, errors)
    }

    /// Looks up an action within a specific machine scope.
    fn lookup_action(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Action", Some(machine_scope), |k| *k == SymbolKind::Action, errors)
    }

    /// Looks up a guard within a specific machine scope.
    fn lookup_guard(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Guard", Some(machine_scope), |k| *k == SymbolKind::Guard, errors)
    }

    /// Looks up an invoke definition within a specific machine scope.
    fn lookup_invoke(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
         self.lookup_with_kind(name, "Invoke", Some(machine_scope), |k| *k == SymbolKind::Invoke, errors)
    }

    /// Looks up a type definition (struct, enum, alias) in the global scope.
    fn lookup_type(
        &self,
        name: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Type", None, |k| matches!(k, SymbolKind::Type(_)), errors)
    }

     /// Looks up a state within a specific machine scope.
    fn lookup_state(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "State", Some(machine_scope), |k| *k == SymbolKind::State, errors)
    }

    /// Looks up an action within a specific machine scope.
    fn lookup_action(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Action", Some(machine_scope), |k| *k == SymbolKind::Action, errors)
    }

    /// Looks up a guard within a specific machine scope.
    fn lookup_guard(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Guard", Some(machine_scope), |k| *k == SymbolKind::Guard, errors)
    }

    /// Looks up an invoke definition within a specific machine scope.
    fn lookup_invoke(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
         self.lookup_with_kind(name, "Invoke", Some(machine_scope), |k| *k == SymbolKind::Invoke, errors)
    }

    /// Looks up a type definition (struct, enum, alias) in the global scope.
    fn lookup_type(
        &self,
        name: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Type", None, |k| matches!(k, SymbolKind::Type(_)), errors)
    }

     /// Looks up a state within a specific machine scope.
    fn lookup_state(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "State", Some(machine_scope), |k| *k == SymbolKind::State, errors)
    }

    /// Looks up an action within a specific machine scope.
    fn lookup_action(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Action", Some(machine_scope), |k| *k == SymbolKind::Action, errors)
    }

    /// Looks up a guard within a specific machine scope.
    fn lookup_guard(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Guard", Some(machine_scope), |k| *k == SymbolKind::Guard, errors)
    }

    /// Looks up an invoke definition within a specific machine scope.
    fn lookup_invoke(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
         self.lookup_with_kind(name, "Invoke", Some(machine_scope), |k| *k == SymbolKind::Invoke, errors)
    }

    /// Looks up a type definition (struct, enum, alias) in the global scope.
    fn lookup_type(
        &self,
        name: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Type", None, |k| matches!(k, SymbolKind::Type(_)), errors)
    }

     /// Looks up a state within a specific machine scope.
    fn lookup_state(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "State", Some(machine_scope), |k| *k == SymbolKind::State, errors)
    }

    /// Looks up an action within a specific machine scope.
    fn lookup_action(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Action", Some(machine_scope), |k| *k == SymbolKind::Action, errors)
    }

    /// Looks up a guard within a specific machine scope.
    fn lookup_guard(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Guard", Some(machine_scope), |k| *k == SymbolKind::Guard, errors)
    }

    /// Looks up an invoke definition within a specific machine scope.
    fn lookup_invoke(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
         self.lookup_with_kind(name, "Invoke", Some(machine_scope), |k| *k == SymbolKind::Invoke, errors)
    }

    /// Looks up a type definition (struct, enum, alias) in the global scope.
    fn lookup_type(
        &self,
        name: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Type", None, |k| matches!(k, SymbolKind::Type(_)), errors)
    }

     /// Looks up a state within a specific machine scope.
    fn lookup_state(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "State", Some(machine_scope), |k| *k == SymbolKind::State, errors)
    }

    /// Looks up an action within a specific machine scope.
    fn lookup_action(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Action", Some(machine_scope), |k| *k == SymbolKind::Action, errors)
    }

    /// Looks up a guard within a specific machine scope.
    fn lookup_guard(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Guard", Some(machine_scope), |k| *k == SymbolKind::Guard, errors)
    }

    /// Looks up an invoke definition within a specific machine scope.
    fn lookup_invoke(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
         self.lookup_with_kind(name, "Invoke", Some(machine_scope), |k| *k == SymbolKind::Invoke, errors)
    }

    /// Looks up a type definition (struct, enum, alias) in the global scope.
    fn lookup_type(
        &self,
        name: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Type", None, |k| matches!(k, SymbolKind::Type(_)), errors)
    }

     /// Looks up a state within a specific machine scope.
    fn lookup_state(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "State", Some(machine_scope), |k| *k == SymbolKind::State, errors)
    }

    /// Looks up an action within a specific machine scope.
    fn lookup_action(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Action", Some(machine_scope), |k| *k == SymbolKind::Action, errors)
    }

    /// Looks up a guard within a specific machine scope.
    fn lookup_guard(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Guard", Some(machine_scope), |k| *k == SymbolKind::Guard, errors)
    }

    /// Looks up an invoke definition within a specific machine scope.
    fn lookup_invoke(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
         self.lookup_with_kind(name, "Invoke", Some(machine_scope), |k| *k == SymbolKind::Invoke, errors)
    }

    /// Looks up a type definition (struct, enum, alias) in the global scope.
    fn lookup_type(
        &self,
        name: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Type", None, |k| matches!(k, SymbolKind::Type(_)), errors)
    }

     /// Looks up a state within a specific machine scope.
    fn lookup_state(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "State", Some(machine_scope), |k| *k == SymbolKind::State, errors)
    }

    /// Looks up an action within a specific machine scope.
    fn lookup_action(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Action", Some(machine_scope), |k| *k == SymbolKind::Action, errors)
    }

    /// Looks up a guard within a specific machine scope.
    fn lookup_guard(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Guard", Some(machine_scope), |k| *k == SymbolKind::Guard, errors)
    }

    /// Looks up an invoke definition within a specific machine scope.
    fn lookup_invoke(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
         self.lookup_with_kind(name, "Invoke", Some(machine_scope), |k| *k == SymbolKind::Invoke, errors)
    }

    /// Looks up a type definition (struct, enum, alias) in the global scope.
    fn lookup_type(
        &self,
        name: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Type", None, |k| matches!(k, SymbolKind::Type(_)), errors)
    }

     /// Looks up a state within a specific machine scope.
    fn lookup_state(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "State", Some(machine_scope), |k| *k == SymbolKind::State, errors)
    }

    /// Looks up an action within a specific machine scope.
    fn lookup_action(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Action", Some(machine_scope), |k| *k == SymbolKind::Action, errors)
    }

    /// Looks up a guard within a specific machine scope.
    fn lookup_guard(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Guard", Some(machine_scope), |k| *k == SymbolKind::Guard, errors)
    }

    /// Looks up an invoke definition within a specific machine scope.
    fn lookup_invoke(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
         self.lookup_with_kind(name, "Invoke", Some(machine_scope), |k| *k == SymbolKind::Invoke, errors)
    }

    /// Looks up a type definition (struct, enum, alias) in the global scope.
    fn lookup_type(
        &self,
        name: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Type", None, |k| matches!(k, SymbolKind::Type(_)), errors)
    }

     /// Looks up a state within a specific machine scope.
    fn lookup_state(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "State", Some(machine_scope), |k| *k == SymbolKind::State, errors)
    }

    /// Looks up an action within a specific machine scope.
    fn lookup_action(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Action", Some(machine_scope), |k| *k == SymbolKind::Action, errors)
    }

    /// Looks up a guard within a specific machine scope.
    fn lookup_guard(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Guard", Some(machine_scope), |k| *k == SymbolKind::Guard, errors)
    }

    /// Looks up an invoke definition within a specific machine scope.
    fn lookup_invoke(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
         self.lookup_with_kind(name, "Invoke", Some(machine_scope), |k| *k == SymbolKind::Invoke, errors)
    }

    /// Looks up a type definition (struct, enum, alias) in the global scope.
    fn lookup_type(
        &self,
        name: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Type", None, |k| matches!(k, SymbolKind::Type(_)), errors)
    }

     /// Looks up a state within a specific machine scope.
    fn lookup_state(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "State", Some(machine_scope), |k| *k == SymbolKind::State, errors)
    }

    /// Looks up an action within a specific machine scope.
    fn lookup_action(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Action", Some(machine_scope), |k| *k == SymbolKind::Action, errors)
    }

    /// Looks up a guard within a specific machine scope.
    fn lookup_guard(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Guard", Some(machine_scope), |k| *k == SymbolKind::Guard, errors)
    }

    /// Looks up an invoke definition within a specific machine scope.
    fn lookup_invoke(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
         self.lookup_with_kind(name, "Invoke", Some(machine_scope), |k| *k == SymbolKind::Invoke, errors)
    }

    /// Looks up a type definition (struct, enum, alias) in the global scope.
    fn lookup_type(
        &self,
        name: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Type", None, |k| matches!(k, SymbolKind::Type(_)), errors)
    }

     /// Looks up a state within a specific machine scope.
    fn lookup_state(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "State", Some(machine_scope), |k| *k == SymbolKind::State, errors)
    }

    /// Looks up an action within a specific machine scope.
    fn lookup_action(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Action", Some(machine_scope), |k| *k == SymbolKind::Action, errors)
    }

    /// Looks up a guard within a specific machine scope.
    fn lookup_guard(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Guard", Some(machine_scope), |k| *k == SymbolKind::Guard, errors)
    }

    /// Looks up an invoke definition within a specific machine scope.
    fn lookup_invoke(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
         self.lookup_with_kind(name, "Invoke", Some(machine_scope), |k| *k == SymbolKind::Invoke, errors)
    }

    /// Looks up a type definition (struct, enum, alias) in the global scope.
    fn lookup_type(
        &self,
        name: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Type", None, |k| matches!(k, SymbolKind::Type(_)), errors)
    }

     /// Looks up a state within a specific machine scope.
    fn lookup_state(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "State", Some(machine_scope), |k| *k == SymbolKind::State, errors)
    }

    /// Looks up an action within a specific machine scope.
    fn lookup_action(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Action", Some(machine_scope), |k| *k == SymbolKind::Action, errors)
    }

    /// Looks up a guard within a specific machine scope.
    fn lookup_guard(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Guard", Some(machine_scope), |k| *k == SymbolKind::Guard, errors)
    }

    /// Looks up an invoke definition within a specific machine scope.
    fn lookup_invoke(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
         self.lookup_with_kind(name, "Invoke", Some(machine_scope), |k| *k == SymbolKind::Invoke, errors)
    }

    /// Looks up a type definition (struct, enum, alias) in the global scope.
    fn lookup_type(
        &self,
        name: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Type", None, |k| matches!(k, SymbolKind::Type(_)), errors)
    }

     /// Looks up a state within a specific machine scope.
    fn lookup_state(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "State", Some(machine_scope), |k| *k == SymbolKind::State, errors)
    }

    /// Looks up an action within a specific machine scope.
    fn lookup_action(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Action", Some(machine_scope), |k| *k == SymbolKind::Action, errors)
    }

    /// Looks up a guard within a specific machine scope.
    fn lookup_guard(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Guard", Some(machine_scope), |k| *k == SymbolKind::Guard, errors)
    }

    /// Looks up an invoke definition within a specific machine scope.
    fn lookup_invoke(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
         self.lookup_with_kind(name, "Invoke", Some(machine_scope), |k| *k == SymbolKind::Invoke, errors)
    }

    /// Looks up a type definition (struct, enum, alias) in the global scope.
    fn lookup_type(
        &self,
        name: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Type", None, |k| matches!(k, SymbolKind::Type(_)), errors)
    }

     /// Looks up a state within a specific machine scope.
    fn lookup_state(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "State", Some(machine_scope), |k| *k == SymbolKind::State, errors)
    }

    /// Looks up an action within a specific machine scope.
    fn lookup_action(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Action", Some(machine_scope), |k| *k == SymbolKind::Action, errors)
    }

    /// Looks up a guard within a specific machine scope.
    fn lookup_guard(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Guard", Some(machine_scope), |k| *k == SymbolKind::Guard, errors)
    }

    /// Looks up an invoke definition within a specific machine scope.
    fn lookup_invoke(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
         self.lookup_with_kind(name, "Invoke", Some(machine_scope), |k| *k == SymbolKind::Invoke, errors)
    }

    /// Looks up a type definition (struct, enum, alias) in the global scope.
    fn lookup_type(
        &self,
        name: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Type", None, |k| matches!(k, SymbolKind::Type(_)), errors)
    }

     /// Looks up a state within a specific machine scope.
    fn lookup_state(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "State", Some(machine_scope), |k| *k == SymbolKind::State, errors)
    }

    /// Looks up an action within a specific machine scope.
    fn lookup_action(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Action", Some(machine_scope), |k| *k == SymbolKind::Action, errors)
    }

    /// Looks up a guard within a specific machine scope.
    fn lookup_guard(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Guard", Some(machine_scope), |k| *k == SymbolKind::Guard, errors)
    }

    /// Looks up an invoke definition within a specific machine scope.
    fn lookup_invoke(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
         self.lookup_with_kind(name, "Invoke", Some(machine_scope), |k| *k == SymbolKind::Invoke, errors)
    }

    /// Looks up a type definition (struct, enum, alias) in the global scope.
    fn lookup_type(
        &self,
        name: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Type", None, |k| matches!(k, SymbolKind::Type(_)), errors)
    }

     /// Looks up a state within a specific machine scope.
    fn lookup_state(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "State", Some(machine_scope), |k| *k == SymbolKind::State, errors)
    }

    /// Looks up an action within a specific machine scope.
    fn lookup_action(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Action", Some(machine_scope), |k| *k == SymbolKind::Action, errors)
    }

    /// Looks up a guard within a specific machine scope.
    fn lookup_guard(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Guard", Some(machine_scope), |k| *k == SymbolKind::Guard, errors)
    }

    /// Looks up an invoke definition within a specific machine scope.
    fn lookup_invoke(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
         self.lookup_with_kind(name, "Invoke", Some(machine_scope), |k| *k == SymbolKind::Invoke, errors)
    }

    /// Looks up a type definition (struct, enum, alias) in the global scope.
    fn lookup_type(
        &self,
        name: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Type", None, |k| matches!(k, SymbolKind::Type(_)), errors)
    }

     /// Looks up a state within a specific machine scope.
    fn lookup_state(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "State", Some(machine_scope), |k| *k == SymbolKind::State, errors)
    }

    /// Looks up an action within a specific machine scope.
    fn lookup_action(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Action", Some(machine_scope), |k| *k == SymbolKind::Action, errors)
    }

    /// Looks up a guard within a specific machine scope.
    fn lookup_guard(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Guard", Some(machine_scope), |k| *k == SymbolKind::Guard, errors)
    }

    /// Looks up an invoke definition within a specific machine scope.
    fn lookup_invoke(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
         self.lookup_with_kind(name, "Invoke", Some(machine_scope), |k| *k == SymbolKind::Invoke, errors)
    }

    /// Looks up a type definition (struct, enum, alias) in the global scope.
    fn lookup_type(
        &self,
        name: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Type", None, |k| matches!(k, SymbolKind::Type(_)), errors)
    }

     /// Looks up a state within a specific machine scope.
    fn lookup_state(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "State", Some(machine_scope), |k| *k == SymbolKind::State, errors)
    }

    /// Looks up an action within a specific machine scope.
    fn lookup_action(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Action", Some(machine_scope), |k| *k == SymbolKind::Action, errors)
    }

    /// Looks up a guard within a specific machine scope.
    fn lookup_guard(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Guard", Some(machine_scope), |k| *k == SymbolKind::Guard, errors)
    }

    /// Looks up an invoke definition within a specific machine scope.
    fn lookup_invoke(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
         self.lookup_with_kind(name, "Invoke", Some(machine_scope), |k| *k == SymbolKind::Invoke, errors)
    }

    /// Looks up a type definition (struct, enum, alias) in the global scope.
    fn lookup_type(
        &self,
        name: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Type", None, |k| matches!(k, SymbolKind::Type(_)), errors)
    }

     /// Looks up a state within a specific machine scope.
    fn lookup_state(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "State", Some(machine_scope), |k| *k == SymbolKind::State, errors)
    }

    /// Looks up an action within a specific machine scope.
    fn lookup_action(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Action", Some(machine_scope), |k| *k == SymbolKind::Action, errors)
    }

    /// Looks up a guard within a specific machine scope.
    fn lookup_guard(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Guard", Some(machine_scope), |k| *k == SymbolKind::Guard, errors)
    }

    /// Looks up an invoke definition within a specific machine scope.
    fn lookup_invoke(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
         self.lookup_with_kind(name, "Invoke", Some(machine_scope), |k| *k == SymbolKind::Invoke, errors)
    }

    /// Looks up a type definition (struct, enum, alias) in the global scope.
    fn lookup_type(
        &self,
        name: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Type", None, |k| matches!(k, SymbolKind::Type(_)), errors)
    }

     /// Looks up a state within a specific machine scope.
    fn lookup_state(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "State", Some(machine_scope), |k| *k == SymbolKind::State, errors)
    }

    /// Looks up an action within a specific machine scope.
    fn lookup_action(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Action", Some(machine_scope), |k| *k == SymbolKind::Action, errors)
    }

    /// Looks up a guard within a specific machine scope.
    fn lookup_guard(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Guard", Some(machine_scope), |k| *k == SymbolKind::Guard, errors)
    }

    /// Looks up an invoke definition within a specific machine scope.
    fn lookup_invoke(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
         self.lookup_with_kind(name, "Invoke", Some(machine_scope), |k| *k == SymbolKind::Invoke, errors)
    }

    /// Looks up a type definition (struct, enum, alias) in the global scope.
    fn lookup_type(
        &self,
        name: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Type", None, |k| matches!(k, SymbolKind::Type(_)), errors)
    }

     /// Looks up a state within a specific machine scope.
    fn lookup_state(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "State", Some(machine_scope), |k| *k == SymbolKind::State, errors)
    }

    /// Looks up an action within a specific machine scope.
    fn lookup_action(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Action", Some(machine_scope), |k| *k == SymbolKind::Action, errors)
    }

    /// Looks up a guard within a specific machine scope.
    fn lookup_guard(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Guard", Some(machine_scope), |k| *k == SymbolKind::Guard, errors)
    }

    /// Looks up an invoke definition within a specific machine scope.
    fn lookup_invoke(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
         self.lookup_with_kind(name, "Invoke", Some(machine_scope), |k| *k == SymbolKind::Invoke, errors)
    }

    /// Looks up a type definition (struct, enum, alias) in the global scope.
    fn lookup_type(
        &self,
        name: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Type", None, |k| matches!(k, SymbolKind::Type(_)), errors)
    }

     /// Looks up a state within a specific machine scope.
    fn lookup_state(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "State", Some(machine_scope), |k| *k == SymbolKind::State, errors)
    }

    /// Looks up an action within a specific machine scope.
    fn lookup_action(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Action", Some(machine_scope), |k| *k == SymbolKind::Action, errors)
    }

    /// Looks up a guard within a specific machine scope.
    fn lookup_guard(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Guard", Some(machine_scope), |k| *k == SymbolKind::Guard, errors)
    }

    /// Looks up an invoke definition within a specific machine scope.
    fn lookup_invoke(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
         self.lookup_with_kind(name, "Invoke", Some(machine_scope), |k| *k == SymbolKind::Invoke, errors)
    }

    /// Looks up a type definition (struct, enum, alias) in the global scope.
    fn lookup_type(
        &self,
        name: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Type", None, |k| matches!(k, SymbolKind::Type(_)), errors)
    }

     /// Looks up a state within a specific machine scope.
    fn lookup_state(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "State", Some(machine_scope), |k| *k == SymbolKind::State, errors)
    }

    /// Looks up an action within a specific machine scope.
    fn lookup_action(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Action", Some(machine_scope), |k| *k == SymbolKind::Action, errors)
    }

    /// Looks up a guard within a specific machine scope.
    fn lookup_guard(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Guard", Some(machine_scope), |k| *k == SymbolKind::Guard, errors)
    }

    /// Looks up an invoke definition within a specific machine scope.
    fn lookup_invoke(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
         self.lookup_with_kind(name, "Invoke", Some(machine_scope), |k| *k == SymbolKind::Invoke, errors)
    }

    /// Looks up a type definition (struct, enum, alias) in the global scope.
    fn lookup_type(
        &self,
        name: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Type", None, |k| matches!(k, SymbolKind::Type(_)), errors)
    }

     /// Looks up a state within a specific machine scope.
    fn lookup_state(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "State", Some(machine_scope), |k| *k == SymbolKind::State, errors)
    }

    /// Looks up an action within a specific machine scope.
    fn lookup_action(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Action", Some(machine_scope), |k| *k == SymbolKind::Action, errors)
    }

    /// Looks up a guard within a specific machine scope.
    fn lookup_guard(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Guard", Some(machine_scope), |k| *k == SymbolKind::Guard, errors)
    }

    /// Looks up an invoke definition within a specific machine scope.
    fn lookup_invoke(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
         self.lookup_with_kind(name, "Invoke", Some(machine_scope), |k| *k == SymbolKind::Invoke, errors)
    }

    /// Looks up a type definition (struct, enum, alias) in the global scope.
    fn lookup_type(
        &self,
        name: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Type", None, |k| matches!(k, SymbolKind::Type(_)), errors)
    }

     /// Looks up a state within a specific machine scope.
    fn lookup_state(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "State", Some(machine_scope), |k| *k == SymbolKind::State, errors)
    }

    /// Looks up an action within a specific machine scope.
    fn lookup_action(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Action", Some(machine_scope), |k| *k == SymbolKind::Action, errors)
    }

    /// Looks up a guard within a specific machine scope.
    fn lookup_guard(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Guard", Some(machine_scope), |k| *k == SymbolKind::Guard, errors)
    }

    /// Looks up an invoke definition within a specific machine scope.
    fn lookup_invoke(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
         self.lookup_with_kind(name, "Invoke", Some(machine_scope), |k| *k == SymbolKind::Invoke, errors)
    }

    /// Looks up a type definition (struct, enum, alias) in the global scope.
    fn lookup_type(
        &self,
        name: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Type", None, |k| matches!(k, SymbolKind::Type(_)), errors)
    }

     /// Looks up a state within a specific machine scope.
    fn lookup_state(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "State", Some(machine_scope), |k| *k == SymbolKind::State, errors)
    }

    /// Looks up an action within a specific machine scope.
    fn lookup_action(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Action", Some(machine_scope), |k| *k == SymbolKind::Action, errors)
    }

    /// Looks up a guard within a specific machine scope.
    fn lookup_guard(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Guard", Some(machine_scope), |k| *k == SymbolKind::Guard, errors)
    }

    /// Looks up an invoke definition within a specific machine scope.
    fn lookup_invoke(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
         self.lookup_with_kind(name, "Invoke", Some(machine_scope), |k| *k == SymbolKind::Invoke, errors)
    }

    /// Looks up a type definition (struct, enum, alias) in the global scope.
    fn lookup_type(
        &self,
        name: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Type", None, |k| matches!(k, SymbolKind::Type(_)), errors)
    }

     /// Looks up a state within a specific machine scope.
    fn lookup_state(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "State", Some(machine_scope), |k| *k == SymbolKind::State, errors)
    }

    /// Looks up an action within a specific machine scope.
    fn lookup_action(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Action", Some(machine_scope), |k| *k == SymbolKind::Action, errors)
    }

    /// Looks up a guard within a specific machine scope.
    fn lookup_guard(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Guard", Some(machine_scope), |k| *k == SymbolKind::Guard, errors)
    }

    /// Looks up an invoke definition within a specific machine scope.
    fn lookup_invoke(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
         self.lookup_with_kind(name, "Invoke", Some(machine_scope), |k| *k == SymbolKind::Invoke, errors)
    }

    /// Looks up a type definition (struct, enum, alias) in the global scope.
    fn lookup_type(
        &self,
        name: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Type", None, |k| matches!(k, SymbolKind::Type(_)), errors)
    }

     /// Looks up a state within a specific machine scope.
    fn lookup_state(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "State", Some(machine_scope), |k| *k == SymbolKind::State, errors)
    }

    /// Looks up an action within a specific machine scope.
    fn lookup_action(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Action", Some(machine_scope), |k| *k == SymbolKind::Action, errors)
    }

    /// Looks up a guard within a specific machine scope.
    fn lookup_guard(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Guard", Some(machine_scope), |k| *k == SymbolKind::Guard, errors)
    }

    /// Looks up an invoke definition within a specific machine scope.
    fn lookup_invoke(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
         self.lookup_with_kind(name, "Invoke", Some(machine_scope), |k| *k == SymbolKind::Invoke, errors)
    }

    /// Looks up a type definition (struct, enum, alias) in the global scope.
    fn lookup_type(
        &self,
        name: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Type", None, |k| matches!(k, SymbolKind::Type(_)), errors)
    }

     /// Looks up a state within a specific machine scope.
    fn lookup_state(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "State", Some(machine_scope), |k| *k == SymbolKind::State, errors)
    }

    /// Looks up an action within a specific machine scope.
    fn lookup_action(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Action", Some(machine_scope), |k| *k == SymbolKind::Action, errors)
    }

    /// Looks up a guard within a specific machine scope.
    fn lookup_guard(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Guard", Some(machine_scope), |k| *k == SymbolKind::Guard, errors)
    }

    /// Looks up an invoke definition within a specific machine scope.
    fn lookup_invoke(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
         self.lookup_with_kind(name, "Invoke", Some(machine_scope), |k| *k == SymbolKind::Invoke, errors)
    }

    /// Looks up a type definition (struct, enum, alias) in the global scope.
    fn lookup_type(
        &self,
        name: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Type", None, |k| matches!(k, SymbolKind::Type(_)), errors)
    }

     /// Looks up a state within a specific machine scope.
    fn lookup_state(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "State", Some(machine_scope), |k| *k == SymbolKind::State, errors)
    }

    /// Looks up an action within a specific machine scope.
    fn lookup_action(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Action", Some(machine_scope), |k| *k == SymbolKind::Action, errors)
    }

    /// Looks up a guard within a specific machine scope.
    fn lookup_guard(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Guard", Some(machine_scope), |k| *k == SymbolKind::Guard, errors)
    }

    /// Looks up an invoke definition within a specific machine scope.
    fn lookup_invoke(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
         self.lookup_with_kind(name, "Invoke", Some(machine_scope), |k| *k == SymbolKind::Invoke, errors)
    }

    /// Looks up a type definition (struct, enum, alias) in the global scope.
    fn lookup_type(
        &self,
        name: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Type", None, |k| matches!(k, SymbolKind::Type(_)), errors)
    }

     /// Looks up a state within a specific machine scope.
    fn lookup_state(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "State", Some(machine_scope), |k| *k == SymbolKind::State, errors)
    }

    /// Looks up an action within a specific machine scope.
    fn lookup_action(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Action", Some(machine_scope), |k| *k == SymbolKind::Action, errors)
    }

    /// Looks up a guard within a specific machine scope.
    fn lookup_guard(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Guard", Some(machine_scope), |k| *k == SymbolKind::Guard, errors)
    }

    /// Looks up an invoke definition within a specific machine scope.
    fn lookup_invoke(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
         self.lookup_with_kind(name, "Invoke", Some(machine_scope), |k| *k == SymbolKind::Invoke, errors)
    }

    /// Looks up a type definition (struct, enum, alias) in the global scope.
    fn lookup_type(
        &self,
        name: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Type", None, |k| matches!(k, SymbolKind::Type(_)), errors)
    }

     /// Looks up a state within a specific machine scope.
    fn lookup_state(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "State", Some(machine_scope), |k| *k == SymbolKind::State, errors)
    }

    /// Looks up an action within a specific machine scope.
    fn lookup_action(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Action", Some(machine_scope), |k| *k == SymbolKind::Action, errors)
    }

    /// Looks up a guard within a specific machine scope.
    fn lookup_guard(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Guard", Some(machine_scope), |k| *k == SymbolKind::Guard, errors)
    }

    /// Looks up an invoke definition within a specific machine scope.
    fn lookup_invoke(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
         self.lookup_with_kind(name, "Invoke", Some(machine_scope), |k| *k == SymbolKind::Invoke, errors)
    }

    /// Looks up a type definition (struct, enum, alias) in the global scope.
    fn lookup_type(
        &self,
        name: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Type", None, |k| matches!(k, SymbolKind::Type(_)), errors)
    }

     /// Looks up a state within a specific machine scope.
    fn lookup_state(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "State", Some(machine_scope), |k| *k == SymbolKind::State, errors)
    }

    /// Looks up an action within a specific machine scope.
    fn lookup_action(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Action", Some(machine_scope), |k| *k == SymbolKind::Action, errors)
    }

    /// Looks up a guard within a specific machine scope.
    fn lookup_guard(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Guard", Some(machine_scope), |k| *k == SymbolKind::Guard, errors)
    }

    /// Looks up an invoke definition within a specific machine scope.
    fn lookup_invoke(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
         self.lookup_with_kind(name, "Invoke", Some(machine_scope), |k| *k == SymbolKind::Invoke, errors)
    }

    /// Looks up a type definition (struct, enum, alias) in the global scope.
    fn lookup_type(
        &self,
        name: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Type", None, |k| matches!(k, SymbolKind::Type(_)), errors)
    }

     /// Looks up a state within a specific machine scope.
    fn lookup_state(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "State", Some(machine_scope), |k| *k == SymbolKind::State, errors)
    }

    /// Looks up an action within a specific machine scope.
    fn lookup_action(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Action", Some(machine_scope), |k| *k == SymbolKind::Action, errors)
    }

    /// Looks up a guard within a specific machine scope.
    fn lookup_guard(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Guard", Some(machine_scope), |k| *k == SymbolKind::Guard, errors)
    }

    /// Looks up an invoke definition within a specific machine scope.
    fn lookup_invoke(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
         self.lookup_with_kind(name, "Invoke", Some(machine_scope), |k| *k == SymbolKind::Invoke, errors)
    }

    /// Looks up a type definition (struct, enum, alias) in the global scope.
    fn lookup_type(
        &self,
        name: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Type", None, |k| matches!(k, SymbolKind::Type(_)), errors)
    }

     /// Looks up a state within a specific machine scope.
    fn lookup_state(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "State", Some(machine_scope), |k| *k == SymbolKind::State, errors)
    }

    /// Looks up an action within a specific machine scope.
    fn lookup_action(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Action", Some(machine_scope), |k| *k == SymbolKind::Action, errors)
    }

    /// Looks up a guard within a specific machine scope.
    fn lookup_guard(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Guard", Some(machine_scope), |k| *k == SymbolKind::Guard, errors)
    }

    /// Looks up an invoke definition within a specific machine scope.
    fn lookup_invoke(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
         self.lookup_with_kind(name, "Invoke", Some(machine_scope), |k| *k == SymbolKind::Invoke, errors)
    }

    /// Looks up a type definition (struct, enum, alias) in the global scope.
    fn lookup_type(
        &self,
        name: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Type", None, |k| matches!(k, SymbolKind::Type(_)), errors)
    }

     /// Looks up a state within a specific machine scope.
    fn lookup_state(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "State", Some(machine_scope), |k| *k == SymbolKind::State, errors)
    }

    /// Looks up an action within a specific machine scope.
    fn lookup_action(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Action", Some(machine_scope), |k| *k == SymbolKind::Action, errors)
    }

    /// Looks up a guard within a specific machine scope.
    fn lookup_guard(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Guard", Some(machine_scope), |k| *k == SymbolKind::Guard, errors)
    }

    /// Looks up an invoke definition within a specific machine scope.
    fn lookup_invoke(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
         self.lookup_with_kind(name, "Invoke", Some(machine_scope), |k| *k == SymbolKind::Invoke, errors)
    }

    /// Looks up a type definition (struct, enum, alias) in the global scope.
    fn lookup_type(
        &self,
        name: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Type", None, |k| matches!(k, SymbolKind::Type(_)), errors)
    }

     /// Looks up a state within a specific machine scope.
    fn lookup_state(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "State", Some(machine_scope), |k| *k == SymbolKind::State, errors)
    }

    /// Looks up an action within a specific machine scope.
    fn lookup_action(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Action", Some(machine_scope), |k| *k == SymbolKind::Action, errors)
    }

    /// Looks up a guard within a specific machine scope.
    fn lookup_guard(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Guard", Some(machine_scope), |k| *k == SymbolKind::Guard, errors)
    }

    /// Looks up an invoke definition within a specific machine scope.
    fn lookup_invoke(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
         self.lookup_with_kind(name, "Invoke", Some(machine_scope), |k| *k == SymbolKind::Invoke, errors)
    }

    /// Looks up a type definition (struct, enum, alias) in the global scope.
    fn lookup_type(
        &self,
        name: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Type", None, |k| matches!(k, SymbolKind::Type(_)), errors)
    }

     /// Looks up a state within a specific machine scope.
    fn lookup_state(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "State", Some(machine_scope), |k| *k == SymbolKind::State, errors)
    }

    /// Looks up an action within a specific machine scope.
    fn lookup_action(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Action", Some(machine_scope), |k| *k == SymbolKind::Action, errors)
    }

    /// Looks up a guard within a specific machine scope.
    fn lookup_guard(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Guard", Some(machine_scope), |k| *k == SymbolKind::Guard, errors)
    }

    /// Looks up an invoke definition within a specific machine scope.
    fn lookup_invoke(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
         self.lookup_with_kind(name, "Invoke", Some(machine_scope), |k| *k == SymbolKind::Invoke, errors)
    }

    /// Looks up a type definition (struct, enum, alias) in the global scope.
    fn lookup_type(
        &self,
        name: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Type", None, |k| matches!(k, SymbolKind::Type(_)), errors)
    }

     /// Looks up a state within a specific machine scope.
    fn lookup_state(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "State", Some(machine_scope), |k| *k == SymbolKind::State, errors)
    }

    /// Looks up an action within a specific machine scope.
    fn lookup_action(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Action", Some(machine_scope), |k| *k == SymbolKind::Action, errors)
    }

    /// Looks up a guard within a specific machine scope.
    fn lookup_guard(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Guard", Some(machine_scope), |k| *k == SymbolKind::Guard, errors)
    }

    /// Looks up an invoke definition within a specific machine scope.
    fn lookup_invoke(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
         self.lookup_with_kind(name, "Invoke", Some(machine_scope), |k| *k == SymbolKind::Invoke, errors)
    }

    /// Looks up a type definition (struct, enum, alias) in the global scope.
    fn lookup_type(
        &self,
        name: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Type", None, |k| matches!(k, SymbolKind::Type(_)), errors)
    }

     /// Looks up a state within a specific machine scope.
    fn lookup_state(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "State", Some(machine_scope), |k| *k == SymbolKind::State, errors)
    }

    /// Looks up an action within a specific machine scope.
    fn lookup_action(
        &self,
        name: &str,
        machine_scope: &str,
        errors: &mut Vec<ValidationError>,
    ) -> Option<&SymbolInfo> {
        self.lookup_with_kind(name, "Action", Some(machine_scope), |k| *k == SymbolKind::Action, errors)
    }

    ///