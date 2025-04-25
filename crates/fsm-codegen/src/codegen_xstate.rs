//! XState machine definition generation logic.

use crate::{find_annotation_value, CodegenError};
use fsm_dsl::ast::{AnnotationValue, FieldDef, FieldType, StateMachine, TransitionElement};
use heck::ToUpperCamelCase; // For event type casing if needed
use std::fmt::Write; // For efficient string building

// Helper to map DSL FieldType to initial JavaScript value string
fn map_field_type_to_js_initial_value(field_type: &FieldType) -> String {
    match field_type {
        FieldType::Void => "undefined".to_string(),
        FieldType::Bool => "false".to_string(),
        FieldType::Int8
        | FieldType::Int16
        | FieldType::Int32
        | FieldType::UInt8
        | FieldType::UInt16
        | FieldType::UInt32 => "0".to_string(),
        FieldType::Int64 | FieldType::UInt64 => "0n".to_string(), // Use BigInt literal
        FieldType::Float32 | FieldType::Float64 => "0.0".to_string(),
        FieldType::Text => "\"\"".to_string(), // Empty string
        FieldType::Data => "new Uint8Array()".to_string(),
        FieldType::List(_) => "[]".to_string(), // Empty array
        FieldType::Identifier(_) => "null".to_string(), // Or maybe throw error? Placeholder for now.
    }
}

// Helper to generate JSDoc (can be copied/adapted from codegen_ts.rs if needed)
fn generate_jsdoc(annotations: &[fsm_dsl::ast::Annotation], indent: &str) -> String {
    let mut doc = String::new();
    if let Some(AnnotationValue::StringLiteral(desc)) =
        find_annotation_value(annotations, "description")
    {
        doc.push_str(indent);
        doc.push_str("/**\n");
        for line in desc.lines() {
            doc.push_str(indent);
            doc.push_str(&format!(" * {}\n", line.trim()));
        }
        doc.push_str(indent);
        doc.push_str(" */\n"); // Add newline after doc block
    }
    doc
}


// TODO: Implement XState machine generation
pub(crate) fn generate_xstate_machine_internal(
    ast: &StateMachine,
) -> Result<String, CodegenError> {
    let mut output = String::new();
    let indent = "  "; // Using 2 spaces for indentation
    let machine_name_str = ast.name.to_string();

    // --- Machine Header ---
    writeln!(output, "// XState machine definition generated from .ssot")?;
    writeln!(output, "// FSM Name: {}", machine_name_str)?;
    writeln!(output, "// Generated at: {}\
", chrono::Utc::now())?; // Add timestamp
    write!(output, "{}", generate_jsdoc(&ast.annotations, ""))?; // Machine description
    writeln!(output, "import {{ createMachine }} from 'xstate';")?;

    // --- Type Imports (from generated .types.ts) ---
    // Assume the types file is in the same directory
    let type_file_name = format!("./{}.types", machine_name_str); // Simple naming convention
    // TODO: Make the Context type generation happen in codegen_ts.rs
    writeln!(output, "import type {{ State as {}State, Event as {}Event, Context as {}Context }} from '{}';\n",
             machine_name_str, machine_name_str, machine_name_str, type_file_name)?;


    // --- createMachine call ---
    // Add generic types
    writeln!(output, "export const {}Machine = createMachine<{}Context, {}Event>({{",
             machine_name_str, machine_name_str, machine_name_str)?;


    // --- Machine ID ---
    writeln!(output, "{indent}id: '{}',", machine_name_str)?;
    // Add type predicate for state matching (improves type safety in use)
    writeln!(output, "{indent}predictableActionArguments: true,")?; // Recommended for V5+
    writeln!(output, "{indent}schema: {{")?;
    writeln!(output, "{indent}{indent}context: {{}} as {}Context, // Define context schema shape", machine_name_str)?;
    writeln!(output, "{indent}{indent}events: {{}} as {}Event, // Define event schema shape", machine_name_str)?;
    // Optional: Add states schema if needed, but often inferred
    // writeln!(output, "{indent}{indent}states: {{}} as {{ [K in {}State]: {{}}; }},", machine_name_str)?;
    writeln!(output, "{indent}}},",)?; // Close schema


    // --- Initial State ---
    let initial_state = find_annotation_value(&ast.annotations, "initial")
        .and_then(|v| match v {
            AnnotationValue::Identifier(ident) => Some(ident.to_string()),
            _ => None,
        })
        .ok_or_else(|| CodegenError::AstValidationError(
            "Missing or invalid '$initial(StateName)' annotation on stateMachine.".to_string()
        ))?;
    writeln!(output, "{indent}initial: '{}',", initial_state)?;


    // --- Context ---
    writeln!(output, "{indent}context: {{")?;
    for field in &ast.context {
        write!(output, "{}", generate_jsdoc(&field.annotations, indent.repeat(2).as_str()))?; // Field description
        let initial_value = map_field_type_to_js_initial_value(&field.field_type);
        writeln!(output, "{indent}{indent}{}: {},", field.name, initial_value)?;
    }
    writeln!(output, "{indent}}},",)?; // Close context object


    // --- States ---
    writeln!(output, "{indent}states: {{")?;
    for state in &ast.states {
        write!(output, "{}", generate_jsdoc(&state.annotations, indent.repeat(2).as_str()))?; // State description
        writeln!(output, "{indent}{indent}{}: {{", state.name)?;

        // Entry Actions
        if !state.entry_actions.is_empty() {
            let action_list = state.entry_actions.iter()
                               .map(|a| format!("'{}'", a))
                               .collect::<Vec<_>>()
                               .join(", ");
            writeln!(output, "{indent}{indent}{indent}entry: [{}],", action_list)?;
        }

        // Exit Actions
        if !state.exit_actions.is_empty() {
            let action_list = state.exit_actions.iter()
                               .map(|a| format!("'{}'", a))
                               .collect::<Vec<_>>()
                               .join(", ");
            writeln!(output, "{indent}{indent}{indent}exit: [{}],", action_list)?;
        }


        // Transitions (on)
        writeln!(output, "{indent}{indent}{indent}on: {{")?;
        for transition in ast.transitions.iter().filter(|t| t.from == state.name) {
            // Find the 'On' element
            let on_element = transition.elements.iter().find_map(|el| match el {
                 TransitionElement::On { event, .. } => Some(event),
                 _ => None,
            });
             if let Some(event_ident) = on_element {
                 // Find the event definition to check for payload (for potential type hints later)
                 // let event_def = ast.events.iter().find(|e| &e.name == event_ident);
                 let event_name_camel_case = event_ident.to_string().to_upper_camel_case(); // Use PascalCase for event types

                 writeln!(output, "{indent}{indent}{indent}{indent}'{}': {{", event_name_camel_case)?; // Event Name as key

                 // Target
                 writeln!(output, "{indent}{indent}{indent}{indent}{indent}target: '{}',", transition.to)?;

                 // Guard (cond)
                 if let Some(guard_fn) = transition.elements.iter().find_map(|el| match el {
                     TransitionElement::Guard { function, .. } => Some(function),
                     _ => None,
                 }) {
                     writeln!(output, "{indent}{indent}{indent}{indent}{indent}cond: '{}',", guard_fn)?;
                 }

                 // Action
                 if let Some(action_fn) = transition.elements.iter().find_map(|el| match el {
                    TransitionElement::Action { function, .. } => Some(function),
                    _ => None,
                 }) {
                     // XState actions are typically arrays, even for one action
                     writeln!(output, "{indent}{indent}{indent}{indent}{indent}actions: ['{}'],", action_fn)?;
                 }

                 writeln!(output, "{indent}{indent}{indent}{indent}}},",)?; // Close event object
             }
        }
        writeln!(output, "{indent}{indent}{indent}}},",)?; // Close 'on' object

        writeln!(output, "{indent}{indent}}},",)?; // Close state object
    }
    writeln!(output, "{indent}}},",)?; // Close states object


    // --- Machine Options (Guards & Actions implementations - placeholders) ---
    // For type safety, these should ideally reference implementations provided elsewhere.
    // Generating placeholders for now.
    let all_guards: std::collections::HashSet<_> = ast.transitions.iter().flat_map(|t| t.elements.iter().filter_map(|el| match el {
        TransitionElement::Guard { function, .. } => Some(function.to_string()),
        _ => None
    })).collect();

     let all_actions: std::collections::HashSet<_> = ast.states.iter().flat_map(|s| s.entry_actions.iter().chain(s.exit_actions.iter()).map(|a| a.to_string()))
         .chain(ast.transitions.iter().flat_map(|t| t.elements.iter().filter_map(|el| match el {
             TransitionElement::Action { function, .. } => Some(function.to_string()),
             _ => None
         })))
         .collect();


    if !all_guards.is_empty() || !all_actions.is_empty() {
        writeln!(output, "{indent}options: {{")?;
        if !all_guards.is_empty() {
            writeln!(output, "{indent}{indent}guards: {{")?;
            for guard in all_guards {
                 writeln!(output, "{indent}{indent}{indent}// TODO: Implement guard '{}'", guard)?;
                 writeln!(output, "{indent}{indent}{indent}'{}': (context, event) => {{ console.warn('Guard \'{}\' not implemented'); return true; }},", guard, guard)?;
            }
             writeln!(output, "{indent}{indent}}},",)?; // Close guards
        }
        if !all_actions.is_empty() {
            writeln!(output, "{indent}{indent}actions: {{")?;
             for action in all_actions {
                 writeln!(output, "{indent}{indent}{indent}// TODO: Implement action '{}'", action)?;
                 writeln!(output, "{indent}{indent}{indent}'{}': (context, event) => {{ console.warn('Action \'{}\' not implemented'); }},", action, action)?;
             }
             writeln!(output, "{indent}{indent}}},",)?; // Close actions
        }
         writeln!(output, "{indent}}},",)?; // Close options
    }


    // --- Close createMachine call ---
    writeln!(output, "}});")?;

    Ok(output)
}


// Helper to handle potential Write errors, converting them to CodegenError
impl From<std::fmt::Error> for CodegenError {
    fn from(err: std::fmt::Error) -> Self {
        CodegenError::GenerationError(format!("Failed to write to string: {}", err))
    }
} 