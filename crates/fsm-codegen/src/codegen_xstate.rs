//! XState machine definition generation logic.

use crate::{find_annotation_value, get_simple_ident, CodegenError};
use fsm_dsl::ast::{AnnotationValue, SsotAst, MachineDefinition, TopLevelDefinition, TypeDefinition, StructDefinition, EnumDefinition, EnumVariant, FieldDefinition, TypeSpecifier, Annotation, Argument, NumericId, Identifier, ContextDefinition, ContextFieldDefinition, StatesBlock, StateDefinition, TransitionDefinition};
// use fsm_dsl::ast::{AnnotationValue, FieldDef, FieldType, StateMachine, TransitionElement}; // Original line commented out
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
pub(crate) fn generate_xstate_machine_internal(ast: &StateMachine) -> Result<String, CodegenError> {
    let mut output = String::new();
    let indent = "  "; // Using 2 spaces for indentation
    let machine_name_str = ast.name.to_string();

    // --- Machine Header ---
    writeln!(output, "// XState machine definition generated from .ssot")?;
    writeln!(output, "// FSM Name: {}", machine_name_str)?;
    writeln!(
        output,
        "// Generated at: {}\
",
        chrono::Utc::now()
    )?; // Add timestamp
    write!(output, "{}", generate_jsdoc(&ast.annotations, ""))?; // Machine description
    writeln!(output, "import {{ createMachine }} from 'xstate';")?;

    // --- Type Imports (from generated .types.ts) ---
    // Assume the types file is in the same directory
    let type_file_name = format!("./{}.types", machine_name_str); // Simple naming convention
                                                                  // TODO: Make the Context type generation happen in codegen_ts.rs
    writeln!(
        output,
        "import type {{ State as {}State, Event as {}Event, Context as {}Context }} from '{}';\n",
        machine_name_str, machine_name_str, machine_name_str, type_file_name
    )?;

    // --- createMachine call ---
    // Add generic types
    writeln!(
        output,
        "export const {}Machine = createMachine<{}Context, {}Event>({{",
        machine_name_str, machine_name_str, machine_name_str
    )?;

    // --- Machine ID ---
    writeln!(output, "{indent}id: '{}',", machine_name_str)?;
    // Add type predicate for state matching (improves type safety in use)
    writeln!(output, "{indent}predictableActionArguments: true,")?; // Recommended for V5+
    writeln!(output, "{indent}schema: {{")?;
    writeln!(
        output,
        "{indent}{indent}context: {{}} as {}Context, // Define context schema shape",
        machine_name_str
    )?;
    writeln!(
        output,
        "{indent}{indent}events: {{}} as {}Event, // Define event schema shape",
        machine_name_str
    )?;
    // Optional: Add states schema if needed, but often inferred
    // writeln!(output, "{indent}{indent}states: {{}} as {{ [K in {}State]: {{}}; }},", machine_name_str)?;
    writeln!(output, "{indent}}},",)?; // Close schema

    // --- Initial State ---
    let initial_state = find_annotation_value(&ast.annotations, "initial")
        .and_then(|v| match v {
            AnnotationValue::Identifier(ident) => Some(ident.to_string()),
            _ => None,
        })
        .ok_or_else(|| {
            CodegenError::AstValidationError(
                "Missing or invalid '$initial(StateName)' annotation on stateMachine.".to_string(),
            )
        })?;
    writeln!(output, "{indent}initial: '{}',", initial_state)?;

    // --- Context ---
    writeln!(output, "{indent}context: {{")?;
    for field in &ast.context {
        write!(
            output,
            "{}",
            generate_jsdoc(&field.annotations, indent.repeat(2).as_str())
        )?; // Field description
        let initial_value = map_field_type_to_js_initial_value(&field.field_type);
        writeln!(output, "{indent}{indent}{}: {},", field.name, initial_value)?;
    }
    writeln!(output, "{indent}}},",)?; // Close context object

    // --- States ---
    writeln!(output, "{indent}states: {{")?;
    for state in &ast.states {
        write!(
            output,
            "{}",
            generate_jsdoc(&state.annotations, indent.repeat(2).as_str())
        )?; // State description
        writeln!(output, "{indent}{indent}{}: {{", state.name)?;

        // Entry Actions
        if !state.entry_actions.is_empty() {
            let action_list = state
                .entry_actions
                .iter()
                .map(|a| format!("'{}'", a))
                .collect::<Vec<_>>()
                .join(", ");
            writeln!(output, "{indent}{indent}{indent}entry: [{}],", action_list)?;
        }

        // Exit Actions
        if !state.exit_actions.is_empty() {
            let action_list = state
                .exit_actions
                .iter()
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

                writeln!(
                    output,
                    "{indent}{indent}{indent}{indent}'{}': {{",
                    event_name_camel_case
                )?; // Event Name as key

                // Target
                writeln!(
                    output,
                    "{indent}{indent}{indent}{indent}{indent}target: '{}',",
                    transition.to
                )?;

                // Guard (cond)
                if let Some(guard_fn) = transition.elements.iter().find_map(|el| match el {
                    TransitionElement::Guard { function, .. } => Some(function),
                    _ => None,
                }) {
                    writeln!(
                        output,
                        "{indent}{indent}{indent}{indent}{indent}cond: '{}',",
                        guard_fn
                    )?;
                }

                // Action
                if let Some(action_fn) = transition.elements.iter().find_map(|el| match el {
                    TransitionElement::Action { function, .. } => Some(function),
                    _ => None,
                }) {
                    // XState actions are typically arrays, even for one action
                    writeln!(
                        output,
                        "{indent}{indent}{indent}{indent}{indent}actions: ['{}'],",
                        action_fn
                    )?;
                }

                writeln!(output, "{indent}{indent}{indent}{indent}}},",)?; // Close event object
            }
        }
        writeln!(output, "{indent}{indent}{indent}}},",)?; // Close 'on' object

        writeln!(output, "{indent}{indent}}},",)?; // Close state object
    }
    writeln!(output, "{indent}}},",)?; // Close states object

    // --- Machine Options (Guards & Actions implementations - improved placeholders) ---
    let all_guards: std::collections::HashMap<String, bool> = ast
        .transitions
        .iter()
        .filter_map(|t| {
            let guard_fn = t.elements.iter().find_map(|el| match el {
                TransitionElement::Guard { function, .. } => Some(function),
                _ => None,
            });
            let event_name = t.elements.iter().find_map(|el| match el {
                TransitionElement::On { event, .. } => Some(event),
                _ => None,
            });
            if let (Some(g), Some(e_qident)) = (guard_fn, event_name) {
                let event_has_payload = ast
                    .events
                    .iter()
                    // Compare simple names
                    .any(|evt| &evt.name == get_simple_ident(e_qident) && !evt.fields.is_empty());
                Some((g.to_string(), event_has_payload))
            } else {
                None
            }
        })
        .collect(); // Collect into HashMap<GuardName, HasPayload>
                    // For actions, consider entry/exit (no payload) and transition actions (payload possible)
    let mut all_actions: std::collections::HashMap<String, bool> = std::collections::HashMap::new();
    // Entry/Exit actions (no payload context)
    for state in &ast.states {
        for action in &state.entry_actions {
            all_actions.entry(action.to_string()).or_insert(false);
        }
        for action in &state.exit_actions {
            all_actions.entry(action.to_string()).or_insert(false);
        }
    }
    // Transition actions (payload context)
    for transition in &ast.transitions {
        if let Some(action_fn) = transition.elements.iter().find_map(|el| match el {
            TransitionElement::Action { function, .. } => Some(function),
            _ => None,
        }) {
            if let Some(event_name_qident) = transition.elements.iter().find_map(|el| match el {
                TransitionElement::On { event, .. } => Some(event),
                _ => None,
            }) {
                let event_has_payload = ast
                    .events
                    .iter()
                    // Compare simple names
                    .any(|evt| {
                        &evt.name == get_simple_ident(event_name_qident) && !evt.fields.is_empty()
                    });
                // If the action is already present, update payload flag only if true
                all_actions
                    .entry(action_fn.to_string())
                    .and_modify(|p| *p = *p || event_has_payload)
                    .or_insert(event_has_payload);
            }
        }
    }

    if !all_guards.is_empty() || !all_actions.is_empty() {
        writeln!(output, "{indent}options: {{")?;
        if !all_guards.is_empty() {
            writeln!(output, "{indent}{indent}guards: {{")?;
            // Sort guards for consistent output
            let mut sorted_guards: Vec<_> = all_guards.into_iter().collect();
            sorted_guards.sort_by(|a, b| a.0.cmp(&b.0));
            for (guard_name, has_payload) in sorted_guards {
                writeln!(
                    output,
                    "{indent}{indent}{indent}// TODO: Implement guard '{}'",
                    guard_name
                )?;
                write!(
                    output,
                    "{indent}{indent}{indent}'{}': (context, event) => {{",
                    guard_name
                )?;
                if has_payload {
                    write!(output, "\n{indent}{indent}{indent}{indent}// This guard might receive events with payloads ('payload' in event ? event.payload : undefined)")?;
                }
                write!(output, "\n{indent}{indent}{indent}{indent}console.warn('Guard \'{}\' not implemented, returning true');", guard_name)?;
                write!(
                    output,
                    "\n{indent}{indent}{indent}{indent}return true;\n{indent}{indent}{indent}}},\n",
                )?; // Close guard func
            }
            writeln!(output, "{indent}{indent}}},",)?; // Close guards
        }
        if !all_actions.is_empty() {
            writeln!(output, "{indent}{indent}actions: {{")?;
            // Sort actions for consistent output
            let mut sorted_actions: Vec<_> = all_actions.into_iter().collect();
            sorted_actions.sort_by(|a, b| a.0.cmp(&b.0));
            for (action_name, has_payload) in sorted_actions {
                writeln!(
                    output,
                    "{indent}{indent}{indent}// TODO: Implement action '{}'",
                    action_name
                )?;
                write!(
                    output,
                    "{indent}{indent}{indent}'{}': (context, event) => {{",
                    action_name
                )?;
                if has_payload {
                    write!(output, "\n{indent}{indent}{indent}{indent}// This action might receive events with payloads ('payload' in event ? event.payload : undefined)")?;
                }
                write!(output, "\n{indent}{indent}{indent}{indent}console.warn('Action \'{}\' not implemented');", action_name)?;
                write!(output, "\n{indent}{indent}{indent}}},\n",)?; // Close action func
            }
            writeln!(output, "{indent}{indent}}},",)?; // Close actions
        }
        writeln!(output, "{indent}}},",)?; // Close options
    }

    // --- Close createMachine call ---
    writeln!(output, "}});")?;

    // Format using prettier (optional, requires prettier installed and in PATH)
    // format_with_prettier(&output)

    Ok(output)
}

// Map DSL field types to TS types
#[allow(dead_code)]
fn map_field_type_to_ts_type(field_type: &fsm_dsl::ast::FieldType) -> String {
    match field_type {
        FieldType::Void => "void".to_string(), // Use void for Void
        FieldType::Bool => "boolean".to_string(),
        FieldType::Int8
        | FieldType::Int16
        | FieldType::Int32
        | FieldType::Int64 // Represent all integers as number
        | FieldType::UInt8
        | FieldType::UInt16
        | FieldType::UInt32
        | FieldType::UInt64
        | FieldType::Float32
        | FieldType::Float64 => "number".to_string(),
        FieldType::Text => "string".to_string(),
        FieldType::Data => "Uint8Array".to_string(), // Represent Data as Uint8Array
        FieldType::List(inner) => {
            format!("{}[]", map_field_type_to_ts_type(inner))
        }
        FieldType::Identifier(ident) => ident.to_string(), // Assume identifier is a valid TS type/interface
    }
}

// Format event payload type string for TS interface
#[allow(dead_code)]
fn format_event_payload_type(payload_fields: &[FieldDef]) -> String {
    if payload_fields.is_empty() {
        return "never".to_string(); // No payload means type is never expected
    }
    let fields_str = payload_fields
        .iter()
        .map(|field| {
            let ts_type = map_field_type_to_ts_type(&field.field_type);
            // Optional: Add JSDoc based on field annotations here if needed
            format!("  {}: {};", field.name, ts_type)
        })
        .collect::<Vec<_>>()
        .join("\n");
    format!("{{\n{}\n}}", fields_str)
}

// Removed helper: Moved to lib.rs
/*
fn get_simple_ident(qident: &fsm_dsl::ast::QualifiedIdent) -> &fsm_dsl::ast::Ident {
    match qident {
        fsm_dsl::ast::QualifiedIdent::Simple(id) => id,
        fsm_dsl::ast::QualifiedIdent::Qualified { name, .. } => name,
    }
}
*/
