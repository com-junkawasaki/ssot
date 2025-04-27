use std::collections::HashMap;
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

/// Performs validation checks on the AST.
pub fn validate_ast(ast: &SsotAst) -> Result<(), Vec<ValidationError>> {
    let mut errors: Vec<ValidationError> = Vec::new();
    let mut id_map: HashMap<u64, IdInfo> = HashMap::new();

    // Check File ID itself (if present)
    if let Some(file_id) = ast.file_id {
         check_and_register_id(file_id, "file".to_string(), "top-level".to_string(), &mut id_map, &mut errors);
    }

    // Validate top-level blocks
    for item in &ast.items {
        match item {
            TopLevelItem::Types(types_block) => validate_types_block(types_block, &mut id_map, &mut errors),
            TopLevelItem::Machines(machines_block) => validate_machines_block(machines_block, &mut id_map, &mut errors),
            TopLevelItem::Services(services_block) => validate_services_block(services_block, &mut id_map, &mut errors),
            TopLevelItem::Communication(comm_block) => validate_communication_block(comm_block, &mut id_map, &mut errors),
            TopLevelItem::Actors(actors_block) => validate_actors_block(actors_block, &mut id_map, &mut errors),
            TopLevelItem::Deployment(deployment_config) => validate_deployment_config(deployment_config, &mut id_map, &mut errors),
            TopLevelItem::Import(_) => { /* TODO: Handle imports */ },
        }
    }

    if errors.is_empty() {
        Ok(())
    } else {
        Err(errors)
    }
}

// --- Helper Function for ID Checking ---

fn check_and_register_id(
    id: u64,
    kind: String,
    name: String,
    id_map: &mut HashMap<u64, IdInfo>,
    errors: &mut Vec<ValidationError>,
) {
    if let Some(existing_info) = id_map.get(&id) {
        errors.push(ValidationError::DuplicateId {
            id,
            kind: kind.clone(),
            name: name.clone(),
            first_kind: existing_info.kind.clone(),
            first_name: existing_info.name.clone(),
        });
    } else {
        id_map.insert(id, IdInfo { kind, name });
    }
}


// --- Traversal and Validation Functions ---

fn validate_types_block(block: &TypesBlock, id_map: &mut HashMap<u64, IdInfo>, errors: &mut Vec<ValidationError>) {
    if let Some(id) = block.id {
        check_and_register_id(id, "types_block".to_string(), "types".to_string(), id_map, errors);
    }
    for def in &block.definitions {
        match def {
            TypeDef::Struct(s_def) => validate_struct_def(s_def, id_map, errors),
            TypeDef::Enum(e_def) => validate_enum_def(e_def, id_map, errors),
            TypeDef::Alias(a_def) => validate_type_alias(a_def, id_map, errors),
        }
    }
}

fn validate_struct_def(def: &StructDef, id_map: &mut HashMap<u64, IdInfo>, errors: &mut Vec<ValidationError>) {
    if let Some(id) = def.id {
        check_and_register_id(id, "struct".to_string(), def.name.clone(), id_map, errors);
    }
    for field in &def.fields {
        validate_field_def(field, id_map, errors);
    }
}

fn validate_enum_def(def: &EnumDef, id_map: &mut HashMap<u64, IdInfo>, errors: &mut Vec<ValidationError>) {
    if let Some(id) = def.id {
        check_and_register_id(id, "enum".to_string(), def.name.clone(), id_map, errors);
    }
    for variant in &def.variants {
        validate_enum_variant(variant, id_map, errors);
    }
}

fn validate_enum_variant(variant: &EnumVariant, id_map: &mut HashMap<u64, IdInfo>, errors: &mut Vec<ValidationError>) {
     if let Some(id) = variant.id {
        check_and_register_id(id, "enum_variant".to_string(), variant.name.clone(), id_map, errors);
    }
    // TODO: Validate variant data types if needed
}

fn validate_type_alias(alias: &TypeAlias, id_map: &mut HashMap<u64, IdInfo>, errors: &mut Vec<ValidationError>) {
    if let Some(id) = alias.id {
        check_and_register_id(id, "type_alias".to_string(), alias.name.clone(), id_map, errors);
    }
    // TODO: Validate original type specifier
}

fn validate_field_def(field: &FieldDef, id_map: &mut HashMap<u64, IdInfo>, errors: &mut Vec<ValidationError>) {
    if let Some(id) = field.id {
        check_and_register_id(id, "field".to_string(), field.name.clone(), id_map, errors);
    }
    // TODO: Validate field type specifier
}

fn validate_machines_block(block: &MachinesBlock, id_map: &mut HashMap<u64, IdInfo>, errors: &mut Vec<ValidationError>) {
    if let Some(id) = block.id {
        check_and_register_id(id, "machines_block".to_string(), "machines".to_string(), id_map, errors);
    }
    for machine in &block.machines {
        validate_machine_def(machine, id_map, errors);
    }
}

fn validate_machine_def(machine: &MachineDef, id_map: &mut HashMap<u64, IdInfo>, errors: &mut Vec<ValidationError>) {
    if let Some(id) = machine.id {
        check_and_register_id(id, "machine".to_string(), machine.name.clone(), id_map, errors);
    }
    // TODO: Validate context type, etc.
    if let Some(states_block) = &machine.states {
        validate_states_block(states_block, id_map, errors);
    }
    for action in &machine.actions {
        validate_action_def(action, id_map, errors);
    }
    for guard in &machine.guards {
        validate_guard_def(guard, id_map, errors);
    }
     for invoke in &machine.invokes {
        validate_invoke_def(invoke, id_map, errors);
    }
}

fn validate_states_block(block: &StatesBlock, id_map: &mut HashMap<u64, IdInfo>, errors: &mut Vec<ValidationError>) {
     if let Some(id) = block.id {
        check_and_register_id(id, "states_block".to_string(), "states".to_string(), id_map, errors);
    }
    for state in &block.states {
        validate_state(state, id_map, errors);
    }
}

fn validate_state(state: &State, id_map: &mut HashMap<u64, IdInfo>, errors: &mut Vec<ValidationError>) {
    if let Some(id) = state.id {
        check_and_register_id(id, "state".to_string(), state.name.clone(), id_map, errors);
    }
     // Validate nested states (regions in parallel states, children in compound states)
    if let Some(nested_states_block) = &state.states {
        validate_states_block(nested_states_block, id_map, errors);
    }
    // TODO: Validate state details (type, initial, final, history, transitions, etc.)
    for entry_action in &state.entry_actions {
        validate_action_ref(entry_action, id_map, errors);
    }
    for exit_action in &state.exit_actions {
         validate_action_ref(exit_action, id_map, errors);
    }
    for transition in &state.transitions {
        validate_transition(transition, id_map, errors);
    }
     if let Some(invoke) = &state.invoke {
        validate_state_invoke(invoke, id_map, errors);
    }
     if let Some(on_done) = &state.on_done {
        validate_transition_details(on_done, id_map, errors);
    }
     if let Some(on_error) = &state.on_error {
        validate_transition_details(on_error, id_map, errors);
    }
}

fn validate_action_def(action: &ActionDef, id_map: &mut HashMap<u64, IdInfo>, errors: &mut Vec<ValidationError>) {
    if let Some(id) = action.id {
        check_and_register_id(id, "action".to_string(), action.name.clone(), id_map, errors);
    }
    // TODO: Validate action parameters if needed
}

fn validate_guard_def(guard: &GuardDef, id_map: &mut HashMap<u64, IdInfo>, errors: &mut Vec<ValidationError>) {
    if let Some(id) = guard.id {
        check_and_register_id(id, "guard".to_string(), guard.name.clone(), id_map, errors);
    }
    // TODO: Validate guard parameters if needed
}

fn validate_invoke_def(invoke: &InvokeDef, id_map: &mut HashMap<u64, IdInfo>, errors: &mut Vec<ValidationError>) {
    if let Some(id) = invoke.id {
        check_and_register_id(id, "invoke".to_string(), invoke.name.clone(), id_map, errors);
    }
    // TODO: Validate invoke src, etc.
}


fn validate_action_ref(_action_ref: &ActionRef, _id_map: &mut HashMap<u64, IdInfo>, _errors: &mut Vec<ValidationError>) {
   // TODO: Implement ID check if ActionRef gets an ID
}

fn validate_transition(_transition: &Transition, _id_map: &mut HashMap<u64, IdInfo>, _errors: &mut Vec<ValidationError>) {
   // TODO: Implement ID check if Transition gets an ID
   // Need to validate targets, actions, guards within the transition
}

fn validate_state_invoke(_invoke: &StateInvoke, _id_map: &mut HashMap<u64, IdInfo>, _errors: &mut Vec<ValidationError>) {
    // TODO: Implement ID check if StateInvoke gets an ID
}

fn validate_transition_details(_details: &TransitionDetails, _id_map: &mut HashMap<u64, IdInfo>, _errors: &mut Vec<ValidationError>) {
    // TODO: Implement ID check if TransitionDetails gets an ID
}


// --- Placeholder functions for other blocks ---
// TODO: Implement similar validation logic for other blocks (Services, Communication, Actors, Deployment)

fn validate_services_block(_block: &ServicesBlock, _id_map: &mut HashMap<u64, IdInfo>, _errors: &mut Vec<ValidationError>) {
    // Placeholder
}

fn validate_communication_block(_block: &CommunicationBlock, _id_map: &mut HashMap<u64, IdInfo>, _errors: &mut Vec<ValidationError>) {
    // Placeholder
}

fn validate_actors_block(_block: &ActorsBlock, _id_map: &mut HashMap<u64, IdInfo>, _errors: &mut Vec<ValidationError>) {
    // Placeholder
}

fn validate_deployment_config(_config: &DeploymentConfig, _id_map: &mut HashMap<u64, IdInfo>, _errors: &mut Vec<ValidationError>) {
    // Placeholder
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
    fn test_placeholder_validation_passes() {
        // Basic valid input (should eventually pass more complex checks)
        let content = r#"
            file_id: 0x1;
            types { 
                struct T @id(0x10) {} 
                enum E @id(0x11) { V @id(0x12) }
            }
            machines { 
                machine M @id(0x20) { 
                    states @id(0x21) { 
                        $initial; 
                        state S @id(0x22) {} 
                    } 
                    actions { action A @id(0x30) {} }
                } 
            }
        "#;
        let result = parse_and_validate(content);
        assert!(result.is_ok(), "Validation failed: {:?}", result.err());
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
            if kind == "struct" && name == "T2" && first_kind == "struct" && first_name == "T1"
        ));
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
            if kind == "machine" && name == "M" && first_kind == "struct" && first_name == "T1"
        ));
    }
     #[test]
    fn test_duplicate_id_error_within_machine() {
         let content = r#"
            file_id: 0x4;
            machines {
                machine M @id(0x50) {
                     states { 
                         $initial; 
                         state S1 @id(0x51) {} 
                         state S2 @id(0x51) {} // Duplicate ID with S1
                     }
                     actions { action A @id(0x52) {} }
                }
            }
        "#;
        let result = parse_and_validate(content);
        assert!(result.is_err());
        let errors = result.err().unwrap();
        assert_eq!(errors.len(), 1);
         assert!(matches!(errors[0], ValidationError::DuplicateId { id: 0x51, kind, name, first_kind, first_name } 
            if kind == "state" && name == "S2" && first_kind == "state" && first_name == "S1"
        ));
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
            if kind == "struct" && name == "T1" && first_kind == "file" && first_name == "top-level"
        ));
    }

    // TODO: Add more comprehensive tests covering all elements with IDs
} 