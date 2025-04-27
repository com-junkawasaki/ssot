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
}

/// Performs validation checks on the AST.
pub fn validate_ast(ast: &SsotAst) -> Result<(), Vec<ValidationError>> {
    let mut errors: Vec<ValidationError> = Vec::new();
    let mut symbol_table = SymbolTable::default();

    // --- First Pass: Collect symbols and check immediate duplicates --- 
    collect_symbols(ast, &mut symbol_table, &mut errors);

    // --- Second Pass: Resolve names and perform structural checks --- 
    // Only proceed to second pass if the first pass didn't find critical errors?
    // For now, always run it, but errors might cascade.
    resolve_names(ast, &symbol_table, &mut errors);

    if errors.is_empty() {
        Ok(())
    } else {
        Err(errors)
    }
}

// --- First Pass: Symbol Collection --- 

fn collect_symbols(ast: &SsotAst, symbol_table: &mut SymbolTable, errors: &mut Vec<ValidationError>) {
     // Register File ID (if present)
    if let Some(file_id) = ast.file_id {
        // We use a placeholder kind/name as file_id doesn't have a specific symbol representation yet.
        // The ID uniqueness is checked here.
        if let Some(existing_id_info) = symbol_table.ids.get(&file_id) {
             errors.push(ValidationError::DuplicateId {
                id: file_id,
                kind: "file".to_string(),
                name: "top-level".to_string(),
                first_kind: existing_id_info.kind.clone(),
                first_name: existing_id_info.name.clone(),
            });
        } else {
            symbol_table.ids.insert(file_id, IdInfo { kind: "file".to_string(), name: "top-level".to_string() });
        }
    }
    // TODO: Consider if blocks themselves should be registered as symbols?
    // if let Some(id) = block.id { ... }

    for item in &ast.items {
        match item {
            TopLevelItem::Types(types_block) => collect_types_symbols(types_block, symbol_table, errors),
            TopLevelItem::Machines(machines_block) => collect_machines_symbols(machines_block, symbol_table, errors),
            // TODO: Implement symbol collection for other top-level items
            TopLevelItem::Services(_) => { /* Placeholder */ },
            TopLevelItem::Communication(_) => { /* Placeholder */ },
            TopLevelItem::Actors(_) => { /* Placeholder */ },
            TopLevelItem::Deployment(_) => { /* Placeholder */ },
            TopLevelItem::Import(_) => { /* Placeholder */ },
        }
    }
}

fn collect_types_symbols(block: &TypesBlock, symbol_table: &mut SymbolTable, errors: &mut Vec<ValidationError>) {
    for def in &block.definitions {
        match def {
            TypeDef::Struct(s_def) => {
                symbol_table.register(s_def.name.clone(), SymbolKind::Type(TypeDefinitionKind::Struct), s_def.id, None, errors);
                // Collect field symbols (potentially useful later, but maybe not needed in symbol table directly)
            }
            TypeDef::Enum(e_def) => {
                symbol_table.register(e_def.name.clone(), SymbolKind::Type(TypeDefinitionKind::Enum), e_def.id, None, errors);
                // Collect enum variants
                for variant in &e_def.variants {
                     // Register variants? Usually accessed via Enum.Variant, maybe not needed globally.
                     // symbol_table.register(...) 
                     // Need to check variant ID uniqueness though.
                     if let Some(variant_id) = variant.id {
                         let id_info = IdInfo { kind: "enum_variant".to_string(), name: format!("{}.{}", e_def.name, variant.name) };
                         if let Some(existing) = symbol_table.ids.get(&variant_id) {
                             errors.push(ValidationError::DuplicateId { 
                                 id: variant_id, 
                                 kind: id_info.kind, name: id_info.name, 
                                 first_kind: existing.kind.clone(), first_name: existing.name.clone() 
                             });
                         } else {
                             symbol_table.ids.insert(variant_id, id_info);
                         }
                     }
                }
            }
            TypeDef::Alias(a_def) => {
                symbol_table.register(a_def.name.clone(), SymbolKind::Type(TypeDefinitionKind::Alias), a_def.id, None, errors);
            }
        }
    }
}

fn collect_machines_symbols(block: &MachinesBlock, symbol_table: &mut SymbolTable, errors: &mut Vec<ValidationError>) {
    for machine in &block.machines {
        symbol_table.register(machine.name.clone(), SymbolKind::Machine, machine.id, None, errors);
        let machine_scope = Some(machine.name.as_str());

        // Collect states within the machine's scope
        if let Some(states_block) = &machine.states {
            collect_states_symbols(states_block, machine_scope, symbol_table, errors);
        }

        // Collect actions, guards, invokes within the machine's scope
        for action in &machine.actions {
            symbol_table.register(action.name.clone(), SymbolKind::Action, action.id, machine_scope, errors);
        }
        for guard in &machine.guards {
             symbol_table.register(guard.name.clone(), SymbolKind::Guard, guard.id, machine_scope, errors);
        }
        for invoke in &machine.invokes {
             symbol_table.register(invoke.name.clone(), SymbolKind::Invoke, invoke.id, machine_scope, errors);
        }
    }
}

fn collect_states_symbols(block: &StatesBlock, scope: Option<&str>, symbol_table: &mut SymbolTable, errors: &mut Vec<ValidationError>) {
     for state in &block.states {
        symbol_table.register(state.name.clone(), SymbolKind::State, state.id, scope, errors);
        // Recursively collect nested states
        if let Some(nested_states_block) = &state.states {
            // Nested states inherit the same scope (machine name)
            collect_states_symbols(nested_states_block, scope, symbol_table, errors);
        }
        // We don't register transitions, actions refs, etc. as symbols here, they are *references*
    }
}

// --- Second Pass: Name Resolution --- 

fn resolve_names(ast: &SsotAst, symbol_table: &SymbolTable, errors: &mut Vec<ValidationError>) {
    for item in &ast.items {
        match item {
            TopLevelItem::Types(types_block) => resolve_types_names(types_block, symbol_table, errors),
            TopLevelItem::Machines(machines_block) => resolve_machines_names(machines_block, symbol_table, errors),
            // TODO: Implement resolution for other top-level items
            TopLevelItem::Services(_) => { /* Placeholder */ },
            TopLevelItem::Communication(_) => { /* Placeholder */ },
            TopLevelItem::Actors(_) => { /* Placeholder */ },
            TopLevelItem::Deployment(_) => { /* Placeholder */ },
            TopLevelItem::Import(_) => { /* Placeholder */ },
        }
    }
}

fn resolve_types_names(block: &TypesBlock, symbol_table: &SymbolTable, errors: &mut Vec<ValidationError>) {
    for def in &block.definitions {
        match def {
            TypeDef::Struct(s_def) => {
                for field in &s_def.fields {
                    // Check field type exists
                    resolve_type_specifier(&field.type_specifier, symbol_table, errors);
                }
            }
            TypeDef::Enum(e_def) => {
                // TODO: Validate variant data types if they exist
                for variant in &e_def.variants {
                    if let Some(ts) = &variant.data_type {
                         resolve_type_specifier(ts, symbol_table, errors);
                    }
                }
            },
            TypeDef::Alias(a_def) => {
                // Check original type exists
                 resolve_type_specifier(&a_def.original_type, symbol_table, errors);
            }
        }
    }
}

fn resolve_type_specifier(ts: &TypeSpecifier, symbol_table: &SymbolTable, errors: &mut Vec<ValidationError>) {
     match ts {
        TypeSpecifier::Simple(name) => {
            // Check built-in types first, then lookup user-defined types
            if !is_builtin_type(name) {
                symbol_table.lookup_type(name, errors);
            }
        },
        TypeSpecifier::Generic(name, params) => {
             // Check the base type (e.g., Vec, Option, HashMap)
             // TODO: Add more robust check for built-in generics vs user-defined generic types
             if !is_builtin_generic(name) {
                 symbol_table.lookup_type(name, errors);
             }
             // Recursively check type parameters
            for param in params {
                resolve_type_specifier(param, symbol_table, errors);
            }
        }
        // TODO: Handle other TypeSpecifier variants if added (e.g., Function, Tuple)
    }
}

// Simple helper for built-in types (expand as needed)
fn is_builtin_type(name: &str) -> bool {
    matches!(name, "string" | "i8" | "u8" | "i16" | "u16" | "i32" | "u32" | "i64" | "u64" | "isize" | "usize" | "f32" | "f64" | "bool" | "unit" | "any" | "json")
}

// Simple helper for built-in generic types (expand as needed)
fn is_builtin_generic(name: &str) -> bool {
     matches!(name, "Vec" | "Option" | "Result" | "HashMap" | "HashSet" | "Box") // TODO: Adjust based on actual supported generics
}

fn resolve_machines_names(block: &MachinesBlock, symbol_table: &SymbolTable, errors: &mut Vec<ValidationError>) {
     for machine in &block.machines {
         let machine_scope = machine.name.as_str();
         // Check machine context type
         if let Some(ctx_type_spec) = &machine.context_type {
             resolve_type_specifier(ctx_type_spec, symbol_table, errors);
         }

         // Check for exactly one initial state
         let mut initial_state_count = 0;
         if let Some(states_block) = &machine.states {
             for state in &states_block.states {
                 if state.initial {
                     initial_state_count += 1;
                 }
             }
             if initial_state_count != 1 {
                errors.push(ValidationError::MissingOrMultipleInitialStates {
                    machine_name: machine.name.clone(),
                    count: initial_state_count,
                });
             }
             
             // Proceed to resolve names within states
             resolve_states_names(states_block, machine_scope, symbol_table, errors);
         } else {
             // Machine has no states block at all, which implies no initial state.
             errors.push(ValidationError::MissingOrMultipleInitialStates {
                machine_name: machine.name.clone(),
                count: 0,
            });
         }
         
         // Check if invoked services/actors exist (if defined elsewhere)
         // TODO: Need symbol table support for services/actors for this check
         // for invoke in &machine.invokes {
         //     if let Some(src_name) = &invoke.src { ... check src_name ... }
         // }
     }
}

fn resolve_states_names(block: &StatesBlock, machine_scope: &str, symbol_table: &SymbolTable, errors: &mut Vec<ValidationError>) {
     for state in &block.states {
         // Check Parallel state structure
         if state.state_type == StateType::Parallel {
             if state.states.is_none() {
                 // According to XState, parallel states must have regions defined within a states block.
                 errors.push(ValidationError::ParallelStateRequiresRegions {
                     state_name: state.name.clone(),
                 });
             } 
             // Optional stricter check: Ensure there's more than one region (direct child state)?
             // else if state.states.as_ref().map_or(0, |sb| sb.states.len()) < 2 {
             //     errors.push(ValidationError::ParallelStateRequiresRegions {
             //         state_name: state.name.clone(),
             //         // message: "Parallel state must contain at least two regions (child states)".to_string(), 
             //     });
             // }
         }

         // Check History state structure (requires default target)
         if state.state_type == StateType::HistoryShallow || state.state_type == StateType::HistoryDeep {
            let mut has_valid_default_transition = false;
            if state.transitions.len() == 1 {
                let transition = &state.transitions[0];
                if transition.event.is_none() && transition.target.is_some() && !transition.target.as_ref().unwrap().is_empty() {
                     has_valid_default_transition = true;
                     // Also resolve this default target
                     resolve_transition(transition, machine_scope, symbol_table, errors);
                }
            }
            
            if !has_valid_default_transition {
                 errors.push(ValidationError::MissingHistoryDefaultTarget {
                     state_name: state.name.clone(),
                 });
            }
            // Skip further checks for History states as they mainly define the default transition
            continue; // Go to the next state in the loop
         }

         // Resolve entry/exit actions
         for action_ref in &state.entry_actions {
             symbol_table.lookup_action(&action_ref.name, machine_scope, errors);
         }
          for action_ref in &state.exit_actions {
             symbol_table.lookup_action(&action_ref.name, machine_scope, errors);
         }
         
         // Resolve transitions
         for transition in &state.transitions {
             resolve_transition(transition, machine_scope, symbol_table, errors);
         }

         // Resolve invoke
         if let Some(invoke) = &state.invoke {
             resolve_state_invoke(invoke, machine_scope, symbol_table, errors);
         }

         // Resolve on_done / on_error transitions
         if let Some(on_done) = &state.on_done {
             resolve_transition_details(on_done, machine_scope, symbol_table, errors);
         }
         if let Some(on_error) = &state.on_error {
             resolve_transition_details(on_error, machine_scope, symbol_table, errors);
         }
         
         // Recursively resolve nested states
         if let Some(nested_states) = &state.states {
             resolve_states_names(nested_states, machine_scope, symbol_table, errors);
         }
     }
}

fn resolve_transition(transition: &Transition, machine_scope: &str, symbol_table: &SymbolTable, errors: &mut Vec<ValidationError>) {
    // Resolve target state(s)
    if let Some(targets) = &transition.target {
        for target_name in targets {
            // TODO: Handle special targets like .history, Parent.history later
            if !target_name.starts_with('.') { // Simple check to ignore history for now
                symbol_table.lookup_state(target_name, machine_scope, errors);
            }
        }
    }
    
    resolve_transition_details(&transition.details, machine_scope, symbol_table, errors);
}

fn resolve_state_invoke(invoke: &StateInvoke, machine_scope: &str, symbol_table: &SymbolTable, errors: &mut Vec<ValidationError>) {
    symbol_table.lookup_invoke(&invoke.invoke_ref, machine_scope, errors);
    // TODO: Resolve on_done / on_error within the invoke if they exist?
    // Currently they are top-level on the state
}

fn resolve_transition_details(details: &TransitionDetails, machine_scope: &str, symbol_table: &SymbolTable, errors: &mut Vec<ValidationError>) {
    // Resolve actions
    if let Some(actions) = &details.actions {
        for action_ref in actions {
            symbol_table.lookup_action(&action_ref.name, machine_scope, errors);
        }
    }
    // Resolve guard
    if let Some(guard_ref) = &details.guard {
         symbol_table.lookup_guard(&guard_ref.name, machine_scope, errors);
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::parser::parse_ssot_content;

    fn parse_and_validate(content: &str) -> Result<(), Vec<ValidationError>> {
        let ast = parse_ssot_content(content, None).expect("Parsing failed in test");
        validate_ast(&ast)
    }


    #[test]
    fn test_valid_structure_no_errors() {
        let content = r#"
            file_id: 0x1;
            types { 
                struct T @id(0x10) {} 
                enum E @id(0x11) { V @id(0x12) }
                alias A @id(0x13) = T;
            }
            machines { 
                machine M @id(0x20) { 
                    context: T @id(0x2A);
                    states @id(0x21) { 
                        $initial;
                        state S @id(0x22) { 
                            entry: Act1;
                            on E1 -> S2 @guard(G1);
                        }
                        state S2 @id(0x23) {}
                    } 
                    actions { 
                        action Act1 @id(0x30) {} 
                        action Act2 @id(0x31) {}
                    }
                    guards { guard G1 @id(0x35) {}}
                    invokes { invoke I1 @id(0x38) { src: "some_service"} }
                } 
            }
        "#;
        let result = parse_and_validate(content);
        // Expect Ok(()) because we haven't implemented the second pass (name resolution) yet.
        // Duplicate checks are handled in the first pass.
        assert!(result.is_ok(), "Validation failed unexpectedly: {:?}", result.err()); 
    }

    #[test]
    fn test_duplicate_id_error_in_types() {
        let content = r#"
            file_id: 0x2;
            types { 
                struct T1 @id(0x10) {}
                struct T2 @id(0x10) {} // Duplicate ID
            }
        "#;
        let result = parse_and_validate(content);
        assert!(result.is_err());
        let errors = result.err().unwrap();
        assert_eq!(errors.len(), 1);
        assert!(matches!(errors[0], ValidationError::DuplicateId { id: 0x10, kind, name, first_kind, first_name } 
             if kind == "Type(Struct)" && name == "T2" && first_kind == "Type(Struct)" && first_name == "T1"
        ), "Unexpected error: {:?}", errors[0]);
    }

    #[test]
    fn test_duplicate_id_error_across_blocks() {
         let content = r#"
            file_id: 0x3;
            types { 
                struct T1 @id(0x40) {} 
            }
            machines {
                machine M @id(0x40) { // Duplicate ID with T1
                     states { $initial; state S @id(0x41) {} }
                }
            }
        "#;
        let result = parse_and_validate(content);
        assert!(result.is_err());
        let errors = result.err().unwrap();
        assert_eq!(errors.len(), 1);
         assert!(matches!(errors[0], ValidationError::DuplicateId { id: 0x40, kind, name, first_kind, first_name } 
            if kind == "Machine" && name == "M" && first_kind == "Type(Struct)" && first_name == "T1"
        ), "Unexpected error: {:?}", errors[0]);
    }
    
    #[test]
    fn test_duplicate_id_error_within_machine_states() {
         let content = r#"
            file_id: 0x4;
            machines {
                machine M @id(0x50) {
                     states { 
                         $initial; 
                         state S1 @id(0x51) {} 
                         state S2 @id(0x51) {} // Duplicate ID with S1
                     }
                }
            }
        "#;
        let result = parse_and_validate(content);
        assert!(result.is_err());
        let errors = result.err().unwrap();
        assert_eq!(errors.len(), 1);
         assert!(matches!(errors[0], ValidationError::DuplicateId { id: 0x51, kind, name, first_kind, first_name } 
            if kind == "State" && name == "S2" && first_kind == "State" && first_name == "S1"
        ), "Unexpected error: {:?}", errors[0]);
    }
    
     #[test]
    fn test_duplicate_id_error_within_machine_actions() {
         let content = r#"
            file_id: 0x5;
            machines {
                machine M @id(0x60) {
                     states { $initial; state S1 @id(0x61) {} } 
                     actions { 
                         action A1 @id(0x62) {} 
                         action A2 @id(0x62) {} // Duplicate ID with A1
                     }
                }
            }
        "#;
        let result = parse_and_validate(content);
        assert!(result.is_err());
        let errors = result.err().unwrap();
        assert_eq!(errors.len(), 1);
         assert!(matches!(errors[0], ValidationError::DuplicateId { id: 0x62, kind, name, first_kind, first_name } 
            if kind == "Action" && name == "A2" && first_kind == "Action" && first_name == "A1"
        ), "Unexpected error: {:?}", errors[0]);
    }

     #[test]
    fn test_duplicate_id_with_file_id() {
         let content = r#"
            file_id: 0x60; 
            types { 
                struct T1 @id(0x60) {} // Duplicate with file_id
            }
        "#;
        let result = parse_and_validate(content);
        assert!(result.is_err());
        let errors = result.err().unwrap();
        assert_eq!(errors.len(), 1);
        assert!(matches!(errors[0], ValidationError::DuplicateId { id: 0x60, kind, name, first_kind, first_name }
            if kind == "Type(Struct)" && name == "T1" && first_kind == "file" && first_name == "top-level"
        ), "Unexpected error: {:?}", errors[0]);
    }

    #[test]
    fn test_duplicate_name_error_global_types() {
         let content = r#"
            file_id: 0x70; 
            types { 
                struct MyType @id(0x71) {} 
                enum MyType @id(0x72) {} // Duplicate name
            }
        "#;
        let result = parse_and_validate(content);
        assert!(result.is_err());
        let errors = result.err().unwrap();
        assert_eq!(errors.len(), 1);
        // Note: Kind check in register prevents this specific error if kinds are different.
        // If we allow different kinds to share names, this test would pass the first pass.
        // Currently, it should fail because both are `SymbolKind::Type`.
        assert!(matches!(errors[0], ValidationError::DuplicateName { name, kind } 
            if name == "MyType" && (kind == "Type(Enum)" || kind == "Type(Struct)") // Order might vary
        ), "Unexpected error: {:?}", errors[0]);
    }

    #[test]
    fn test_duplicate_name_error_within_machine_scope_states() {
        let content = r#"
            file_id: 0x80;
            machines {
                machine M @id(0x81) {
                    states {
                        state Active @id(0x82) {}
                        state Active @id(0x83) {} // Duplicate state name
                    }
                }
            }
        "#;
        let result = parse_and_validate(content);
        assert!(result.is_err());
        let errors = result.err().unwrap();
        assert_eq!(errors.len(), 1);
        assert!(matches!(errors[0], ValidationError::DuplicateName { name, kind } 
            if name == "Active" && kind == "State"
        ), "Unexpected error: {:?}", errors[0]);
    }

    #[test]
    fn test_duplicate_name_error_within_machine_scope_actions() {
         let content = r#"
            file_id: 0x90;
            machines {
                machine M @id(0x91) {
                    states { $initial; state S @id(0x92) {} }
                    actions {
                        action log @id(0x93) {}
                        action log @id(0x94) {} // Duplicate action name
                    }
                }
            }
        "#;
        let result = parse_and_validate(content);
        assert!(result.is_err());
        let errors = result.err().unwrap();
        assert_eq!(errors.len(), 1);
        assert!(matches!(errors[0], ValidationError::DuplicateName { name, kind } 
            if name == "log" && kind == "Action"
        ), "Unexpected error: {:?}", errors[0]);
    }

    #[test]
    fn test_name_not_found_error_type() {
        let content = r#"
            file_id: 0xB0;
            types {
                struct S1 @id(0xB1) { f1: NonExistentType; }
            }
        "#;
        let result = parse_and_validate(content);
        assert!(result.is_err());
        let errors = result.err().unwrap();
        assert!(errors.iter().any(|e| matches!(e, ValidationError::NameNotFound { name, expected_kind } if name == "NonExistentType" && expected_kind == "Type")), "Expected NameNotFound error for NonExistentType, got: {:?}", errors);
    }
    
     #[test]
    fn test_name_not_found_error_alias() {
        let content = r#"
            file_id: 0xB1;
            types {
                 alias A1 @id(0xB2) = NonExistentType;
            }
        "#;
        let result = parse_and_validate(content);
        assert!(result.is_err());
        let errors = result.err().unwrap();
         assert!(errors.iter().any(|e| matches!(e, ValidationError::NameNotFound { name, expected_kind } if name == "NonExistentType" && expected_kind == "Type")), "Expected NameNotFound error for NonExistentType, got: {:?}", errors);
    }

     #[test]
    fn test_name_not_found_error_machine_context() {
        let content = r#"
            file_id: 0xC0;
            machines {
                machine M @id(0xC1) {
                    context: NonExistentType;
                    states { $initial; state S @id(0xC2) {} }
                }
            }
        "#;
        let result = parse_and_validate(content);
        assert!(result.is_err());
        let errors = result.err().unwrap();
        assert!(errors.iter().any(|e| matches!(e, ValidationError::NameNotFound { name, expected_kind } if name == "NonExistentType" && expected_kind == "Type")), "Expected NameNotFound error for NonExistentType, got: {:?}", errors);
    }

     #[test]
    fn test_name_not_found_error_transition_target() {
        let content = r#"
            file_id: 0xD0;
            machines {
                machine M @id(0xD1) {
                    states {
                        $initial;
                        state S1 @id(0xD2) { on E -> NonExistentState; }
                    }
                }
            }
        "#;
        let result = parse_and_validate(content);
        assert!(result.is_err());
        let errors = result.err().unwrap();
        assert!(errors.iter().any(|e| matches!(e, ValidationError::NameNotFound { name, expected_kind } if name == "NonExistentState" && expected_kind == "State")), "Expected NameNotFound error for NonExistentState, got: {:?}", errors);
    }

    #[test]
    fn test_name_not_found_error_entry_action() {
        let content = r#"
            file_id: 0xE0;
            machines {
                machine M @id(0xE1) {
                    states {
                        $initial;
                        state S1 @id(0xE2) { entry: NonExistentAction; }
                    }
                    actions { action RealAction @id(0xE3) {} }
                }
            }
        "#;
        let result = parse_and_validate(content);
        assert!(result.is_err());
        let errors = result.err().unwrap();
         assert!(errors.iter().any(|e| matches!(e, ValidationError::NameNotFound { name, expected_kind } if name == "NonExistentAction" && expected_kind == "Action")), "Expected NameNotFound error for NonExistentAction, got: {:?}", errors);
    }
    
    #[test]
    fn test_name_not_found_error_exit_action() {
        let content = r#"
            file_id: 0xE4;
            machines {
                machine M @id(0xE5) {
                    states {
                        $initial;
                        state S1 @id(0xE6) { exit: NonExistentAction; }
                    }
                }
            }
        "#;
        let result = parse_and_validate(content);
        assert!(result.is_err());
        let errors = result.err().unwrap();
         assert!(errors.iter().any(|e| matches!(e, ValidationError::NameNotFound { name, expected_kind } if name == "NonExistentAction" && expected_kind == "Action")), "Expected NameNotFound error for NonExistentAction, got: {:?}", errors);
    }

    #[test]
    fn test_name_not_found_error_transition_action() {
         let content = r#"
            file_id: 0xF0;
            machines {
                machine M @id(0xF1) {
                    states {
                        $initial;
                        state S1 @id(0xF2) { on E -> S1 / NonExistentAction; }
                    }
                }
            }
        "#;
        let result = parse_and_validate(content);
        assert!(result.is_err());
        let errors = result.err().unwrap();
        assert!(errors.iter().any(|e| matches!(e, ValidationError::NameNotFound { name, expected_kind } if name == "NonExistentAction" && expected_kind == "Action")), "Expected NameNotFound error for NonExistentAction, got: {:?}", errors);
    }

    #[test]
    fn test_name_not_found_error_transition_guard() {
         let content = r#"
            file_id: 0x100;
            machines {
                machine M @id(0x101) {
                    states {
                        $initial;
                        state S1 @id(0x102) { on E -> S1 @guard(NonExistentGuard); }
                    }
                    guards { guard RealGuard @id(0x103) {} }
                }
            }
        "#;
        let result = parse_and_validate(content);
        assert!(result.is_err());
        let errors = result.err().unwrap();
        assert!(errors.iter().any(|e| matches!(e, ValidationError::NameNotFound { name, expected_kind } if name == "NonExistentGuard" && expected_kind == "Guard")), "Expected NameNotFound error for NonExistentGuard, got: {:?}", errors);
    }

    #[test]
    fn test_name_not_found_error_invoke() {
         let content = r#"
            file_id: 0x110;
            machines {
                machine M @id(0x111) {
                    states {
                        $initial;
                        state S1 @id(0x112) { invoke: NonExistentInvoke; }
                    }
                    invokes { invoke RealInvoke @id(0x113) { src: "test"} }
                }
            }
        "#;
        let result = parse_and_validate(content);
        assert!(result.is_err());
        let errors = result.err().unwrap();
        assert!(errors.iter().any(|e| matches!(e, ValidationError::NameNotFound { name, expected_kind } if name == "NonExistentInvoke" && expected_kind == "Invoke")), "Expected NameNotFound error for NonExistentInvoke, got: {:?}", errors);
    }

    #[test]
    fn test_type_found_but_wrong_kind_action() {
         let content = r#"
            file_id: 0x120;
            types { struct S @id(0x121) {} }
            machines {
                machine M @id(0x122) {
                    states {
                        $initial;
                        state S1 @id(0x123) { entry: S; } // S is a Type, not an Action
                    }
                }
            }
        "#;
        let result = parse_and_validate(content);
        assert!(result.is_err());
        let errors = result.err().unwrap();
        // Expect NameNotFound because lookup_action filters by kind
        assert!(errors.iter().any(|e| matches!(e, ValidationError::NameNotFound { name, expected_kind } if name == "S" && expected_kind == "Action")), "Expected NameNotFound error for S (wrong kind), got: {:?}", errors);
    }
     #[test]
    fn test_state_found_but_wrong_kind_type() {
         let content = r#"
            file_id: 0x130;
            machines {
                machine M @id(0x131) {
                     states { $initial; state S @id(0x132) {} }
                }
            }
             types {
                struct MyData @id(0x133) { field: S; } // S is a State, not a Type
            }
        "#;
        let result = parse_and_validate(content);
        assert!(result.is_err());
        let errors = result.err().unwrap();
        // Expect NameNotFound because lookup_type filters by kind
        assert!(errors.iter().any(|e| matches!(e, ValidationError::NameNotFound { name, expected_kind } if name == "S" && expected_kind == "Type")), "Expected NameNotFound error for S (wrong kind), got: {:?}", errors);
    }

    #[test]
    fn test_missing_initial_state() {
        let content = r#"
            file_id: 0x140;
            machines {
                machine M @id(0x141) {
                    states {
                        // No $initial marker
                        state S1 @id(0x142) {}
                        state S2 @id(0x143) {}
                    }
                }
            }
        "#;
        let result = parse_and_validate(content);
        assert!(result.is_err());
        let errors = result.err().unwrap();
        assert!(errors.iter().any(|e| matches!(e, ValidationError::MissingOrMultipleInitialStates { machine_name, count } if machine_name == "M" && *count == 0)), "Expected MissingOrMultipleInitialStates error with count 0, got: {:?}", errors);
    }

    #[test]
    fn test_multiple_initial_states() {
        let content = r#"
            file_id: 0x150;
            machines {
                machine M @id(0x151) {
                    states {
                        $initial;
                        state S1 @id(0x152) {}
                        $initial; // Second initial marker
                        state S2 @id(0x153) {}
                    }
                }
            }
        "#;
        let result = parse_and_validate(content);
        assert!(result.is_err());
        let errors = result.err().unwrap();
        assert!(errors.iter().any(|e| matches!(e, ValidationError::MissingOrMultipleInitialStates { machine_name, count } if machine_name == "M" && *count == 2)), "Expected MissingOrMultipleInitialStates error with count 2, got: {:?}", errors);
    }
    
    #[test]
    fn test_no_states_block_implies_missing_initial_state() {
         let content = r#"
            file_id: 0x160;
            machines {
                machine M @id(0x161) {
                    // No states block
                    actions { action A @id(0x162) {} }
                }
            }
        "#;
        let result = parse_and_validate(content);
        assert!(result.is_err());
        let errors = result.err().unwrap();
        assert!(errors.iter().any(|e| matches!(e, ValidationError::MissingOrMultipleInitialStates { machine_name, count } if machine_name == "M" && *count == 0)), "Expected MissingOrMultipleInitialStates error with count 0 for missing states block, got: {:?}", errors);
    }

    #[test]
    fn test_parallel_state_missing_regions() {
        let content = r#"
            file_id: 0x170;
            machines {
                machine M @id(0x171) {
                    states {
                        $initial;
                        parallel state P @id(0x172) {
                            // No nested states block defining regions
                            entry: A1;
                        }
                    }
                    actions { action A1 @id(0x173) {} }
                }
            }
        "#;
        let result = parse_and_validate(content);
        assert!(result.is_err());
        let errors = result.err().unwrap();
        assert!(errors.iter().any(|e| matches!(e, ValidationError::ParallelStateRequiresRegions { state_name } if state_name == "P")), "Expected ParallelStateRequiresRegions error for P, got: {:?}", errors);
    }

    #[test]
    fn test_parallel_state_with_regions_is_valid() {
        // This test also implicitly checks that the nested states are resolved correctly.
        let content = r#"
            file_id: 0x180;
             types { struct Ctx @id(0x18F) {} }
            machines {
                machine M @id(0x181) {
                    context: Ctx;
                    states {
                        $initial;
                        parallel state P @id(0x182) {
                            states { // Regions defined here
                                state R1 @id(0x183) {
                                    $initial;
                                    state R1S1 @id(0x184) { on E1 -> R1S2 @guard(G1); }
                                    state R1S2 @id(0x185) {}
                                }
                                state R2 @id(0x186) {
                                    $initial;
                                     state R2S1 @id(0x187) { entry: A1; }
                                }
                            }
                        }
                    }
                    actions { action A1 @id(0x18A) {} }
                    guards { guard G1 @id(0x18B) {} }
                }
            }
        "#;
        let result = parse_and_validate(content);
        assert!(result.is_ok(), "Validation failed unexpectedly for valid parallel state: {:?}", result.err());
    }
    
     #[test]
    fn test_compound_state_with_nested_states_is_valid() {
        // Ensure non-parallel states with nested states don't trigger the error
        let content = r#"
            file_id: 0x190;
            machines {
                machine M @id(0x191) {
                    states {
                        $initial;
                        state C @id(0x192) { // Compound, not parallel
                            states {
                                $initial;
                                state C1 @id(0x193) {}
                            }
                        }
                    }
                }
            }
        "#;
        let result = parse_and_validate(content);
         assert!(result.is_ok(), "Validation failed unexpectedly for compound state with children: {:?}", result.err());
    }

    #[test]
    fn test_history_state_missing_transition() {
        let content = r#"
            file_id: 0x1A0;
            machines {
                machine M @id(0x1A1) {
                    states {
                        $initial;
                        state Parent @id(0x1A2) {
                             $initial;
                             state Child @id(0x1A3) {};
                             history state H @id(0x1A4) {}
                        }
                    }
                }
            }
        "#;
        let result = parse_and_validate(content);
        assert!(result.is_err());
        let errors = result.err().unwrap();
        assert!(errors.iter().any(|e| matches!(e, ValidationError::MissingHistoryDefaultTarget { state_name } if state_name == "H")), "Expected MissingHistoryDefaultTarget error for H, got: {:?}", errors);
    }

    #[test]
    fn test_history_state_with_event_transition() {
        let content = r#"
            file_id: 0x1B0;
            machines {
                machine M @id(0x1B1) {
                    states {
                        $initial;
                        state Parent @id(0x1B2) {
                             $initial;
                             state Child @id(0x1B3) {};
                             history state H @id(0x1B4) { on EV -> Child; }
                        }
                    }
                }
            }
        "#;
        let result = parse_and_validate(content);
        assert!(result.is_err());
        let errors = result.err().unwrap();
         assert!(errors.iter().any(|e| matches!(e, ValidationError::MissingHistoryDefaultTarget { state_name } if state_name == "H")), "Expected MissingHistoryDefaultTarget error for H (has event), got: {:?}", errors);
    }

    #[test]
    fn test_history_state_with_eventless_no_target_transition() {
         let content = r#"
            file_id: 0x1C0;
            machines {
                machine M @id(0x1C1) {
                    states {
                        $initial;
                        state Parent @id(0x1C2) {
                             $initial;
                             state Child @id(0x1C3) {};
                             history state H @id(0x1C4) { -> ; }
                        }
                    }
                }
            }
        "#;
        // Note: The parser might reject "-> ;" earlier. If it passes, validation should catch it.
        let result = parse_and_validate(content);
        assert!(result.is_err());
        let errors = result.err().unwrap();
        assert!(errors.iter().any(|e| matches!(e, ValidationError::MissingHistoryDefaultTarget { state_name } if state_name == "H")), "Expected MissingHistoryDefaultTarget error for H (no target), got: {:?}", errors);
    }
    
    #[test]
    fn test_history_state_with_multiple_transitions() {
         let content = r#"
            file_id: 0x1D0;
            machines {
                machine M @id(0x1D1) {
                    states {
                        $initial;
                        state Parent @id(0x1D2) {
                             $initial;
                             state Child1 @id(0x1D3) {};
                             state Child2 @id(0x1D4) {};
                             history state H @id(0x1D5) {
                                 -> Child1;
                                 -> Child2; // Invalid: multiple transitions
                             }
                        }
                    }
                }
            }
        "#;
        let result = parse_and_validate(content);
        assert!(result.is_err());
        let errors = result.err().unwrap();
        assert!(errors.iter().any(|e| matches!(e, ValidationError::MissingHistoryDefaultTarget { state_name } if state_name == "H")), "Expected MissingHistoryDefaultTarget error for H (multiple transitions), got: {:?}", errors);
    }

    #[test]
    fn test_valid_history_state() {
         let content = r#"
            file_id: 0x1E0;
            machines {
                machine M @id(0x1E1) {
                    states {
                        $initial;
                        state Parent @id(0x1E2) {
                             $initial;
                             state Child @id(0x1E3) {};
                             history state H @id(0x1E4) { -> Child; }
                        }
                    }
                }
            }
        "#;
        let result = parse_and_validate(content);
        assert!(result.is_ok(), "Validation failed unexpectedly for valid history state: {:?}", result.err());
    }
    
    #[test]
    fn test_valid_deep_history_state() {
         let content = r#"
            file_id: 0x1F0;
            machines {
                machine M @id(0x1F1) {
                    states {
                        $initial;
                        state Parent @id(0x1F2) {
                             $initial;
                             state Child @id(0x1F3) {};
                             history deep state H @id(0x1F4) { -> Child; }
                        }
                    }
                }
            }
        "#;
        let result = parse_and_validate(content);
        assert!(result.is_ok(), "Validation failed unexpectedly for valid deep history state: {:?}", result.err());
    }

} 