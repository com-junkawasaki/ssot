//! XState machine definition generation logic.

use crate::{find_annotation_value, /*get_simple_ident,*/ CodegenError}; // get_simple_ident likely unused now
use fsm_dsl::ast::{
    ActionDefinition,
    Annotation, AnnotationValue, Argument, ContextDefinition, ContextFieldDefinition, GuardDefinition,
    Identifier, InvokeDefinition, MachineDefinition, NumericId, SsotAst, StateDefinition, StatesBlock,
    TopLevelDefinition, TransitionDefinition, TransitionTarget, TypeSpecifier,
};
use heck::{ToLowerCamelCase, ToUpperCamelCase};
use std::collections::{HashMap, HashSet};
use std::fmt::Write;

// Helper to map DSL TypeSpecifier to initial JavaScript value string
fn map_type_specifier_to_js_initial_value(type_spec: &TypeSpecifier) -> String {
    match type_spec {
        TypeSpecifier::Simple(ident) => match ident.name.as_str() {
            "bool" => "false".to_string(),
            "int" | "i8" | "i16" | "i32" | "u8" | "u16" | "u32" => "0".to_string(),
            "i64" | "u64" => "0n".to_string(), // Use BigInt literal
            "f32" | "f64" => "0.0".to_string(),
            "string" | "text" => "\"\"".to_string(), // Empty string
            "data" => "new Uint8Array()".to_string(),
            "void" => "undefined".to_string(),
            _ => "null".to_string(), // Assume custom types are null initially
        },
        TypeSpecifier::List(_) => "[]".to_string(),         // Empty array
        TypeSpecifier::Optional(_) => "undefined".to_string(), // Or null?
        TypeSpecifier::Map(_, _) => "{{}}".to_string(),      // Empty object
    }
}

// Helper to generate JSDoc (copied/adapted from codegen_ts.rs)
fn generate_jsdoc(annotations: &[Annotation], indent: &str) -> String {
    let mut doc = String::new();
    let description = annotations.iter().find_map(|anno| match anno {
        Annotation::Description(desc) => Some(desc),
        _ => None,
    });
    if let Some(desc) = description {
        doc.push_str(indent);
        doc.push_str("/**\n");
        for line in desc.lines() {
            doc.push_str(indent);
            doc.push_str(&format!(" * {}\n", line.trim()));
        }
        doc.push_str(indent);
        doc.push_str(" */\n");
    }
    doc
}

// Updated internal function
pub(crate) fn generate_xstate_machine_internal(ast: &SsotAst) -> Result<String, CodegenError> {
    let mut output = String::new();
    let indent = "  "; // Using 2 spaces for indentation

    // Extract the single MachineDefinition (assuming one per file for now)
    let machine_ast = ast
        .definitions
        .iter()
        .find_map(|def| {
            if let TopLevelDefinition::Machines(m_block) = def {
                m_block.definitions.first()
            } else {
                None
            }
        })
        .ok_or_else(|| {
            CodegenError::AstValidationError("No machine definition found in AST".to_string())
        })?;

    let machine_name_str = &machine_ast.name.name;
    let machine_name_camel = machine_name_str.to_lower_camel_case(); // e.g., trafficLight

    // --- Machine Header ---
    writeln!(output, "// XState machine definition generated from .ssot")?;
    writeln!(output, "// FSM Name: {}", machine_name_str)?;
    writeln!(output, "// Generated at: {}", chrono::Utc::now())?; // Add timestamp
    write!(output, "{}", generate_jsdoc(&machine_ast.annotations, ""))?; // Machine description
    writeln!(output, "import {{ createMachine, assign }} from 'xstate';")?; // Import assign

    // --- Type Imports (from generated .types.ts) ---
    // Assume the types file is in the same directory or appropriately pathed
    let type_file_name = format!("./{}.types", machine_name_camel); // Use camel case for file name
    writeln!(
        output,
        "import type {{ State as {}State, Event as {}Event, Context as {}Context }} from '{}';\n",
        machine_name_camel, machine_name_camel, machine_name_camel, type_file_name
    )?;

    // --- createMachine call ---
    writeln!(
        output,
        "export const {}Machine = createMachine<{}Context, {}Event>({{",
        machine_name_camel, machine_name_camel, machine_name_camel
    )?;

    // --- Machine ID ---
    writeln!(output, "{indent}id: '{}',", machine_name_str)?;
    writeln!(output, "{indent}predictableActionArguments: true,")?; // Recommended for V5+
    writeln!(output, "{indent}schema: {{")?;
    writeln!(
        output,
        "{indent}{indent}context: {{}} as {}Context, // Define context schema shape",
        machine_name_camel
    )?;
    writeln!(
        output,
        "{indent}{indent}events: {{}} as {}Event, // Define event schema shape",
        machine_name_camel
    )?;
    // TODO: Add states schema if desired?
    writeln!(output, "{indent}}},",)?; // Close schema

    // --- Initial State ---
     let initial_state_name = machine_ast
        .annotations
        .iter()
        .find_map(|anno| match anno {
            Annotation::InitialState(ident) => Some(ident.name.clone()),
            _ => None,
        })
        .ok_or_else(|| {
            CodegenError::AstValidationError(
                "Missing or invalid '$initial(StateName)' annotation on machine.".to_string(),
            )
        })?;
    writeln!(output, "{indent}initial: '{}',", initial_state_name)?;

    // --- Context ---
    writeln!(output, "{indent}context: {{")?;
    if let Some(context_def) = &machine_ast.context {
        write!(output, "{}", generate_jsdoc(&context_def.annotations, indent))?;
        for field in &context_def.fields {
            write!(output, "{}", generate_jsdoc(&field.annotations, &indent.repeat(2)))?; // Field description
            let initial_value = map_type_specifier_to_js_initial_value(&field.type_spec);
            writeln!(output, "{indent}{indent}{}: {},", field.name.name, initial_value)?;
        }
    }
    writeln!(output, "{indent}}},",)?; // Close context object

    // --- States ---
    writeln!(output, "{indent}states: {{")?;
    if let Some(states_block) = &machine_ast.states {
        write!(output, "{}", generate_jsdoc(&states_block.annotations, indent))?;
        for state in &states_block.states {
            write!(output, "{}", generate_jsdoc(&state.annotations, &indent.repeat(2)))?;
            writeln!(output, "{indent}{indent}{}: {{", state.name.name)?;
            let state_indent = indent.repeat(3);

            // Entry Actions
            if !state.on_entry.is_empty() {
                let action_list = state
                    .on_entry
                    .iter()
                    .map(|a| format!("'{}'", a.name))
                    .collect::<Vec<_>>()
                    .join(", ");
                writeln!(output, "{state_indent}entry: [{}],", action_list)?;
            }

            // Exit Actions
            if !state.on_exit.is_empty() {
                let action_list = state
                    .on_exit
                    .iter()
                    .map(|a| format!("'{}'", a.name))
                    .collect::<Vec<_>>()
                    .join(", ");
                writeln!(output, "{state_indent}exit: [{}],", action_list)?;
            }

            // Transitions (on)
            if !state.transitions.is_empty() {
                writeln!(output, "{state_indent}on: {{")?;
                let trans_indent = indent.repeat(4);
                for transition in &state.transitions {
                    let event_name = &transition.event.name;
                    // Use PascalCase for event types in discriminated union
                    let event_type_name = event_name.to_upper_camel_case();

                    writeln!(output, "{trans_indent}'{}': {{", event_type_name)?;
                    let detail_indent = indent.repeat(5);

                    // Target
                    match &transition.target {
                        TransitionTarget::State(target_ident) => {
                             writeln!(output, "{detail_indent}target: '{}',", target_ident.name)?;
                        }
                        // TODO: Handle history transitions if needed in XState
                        TransitionTarget::CurrentHistory => {
                            writeln!(output, "// TODO: Handle CurrentHistory target")?;
                        }
                         TransitionTarget::QualifiedHistory(target_ident) => {
                            writeln!(output, "// TODO: Handle QualifiedHistory target: {}", target_ident.name)?;
                        }
                    }

                    // Guard (cond)
                    if !transition.guards.is_empty() {
                        // Combine multiple guards with && (assuming AND logic)
                        let guard_list = transition
                            .guards
                            .iter()
                            .map(|g| format!("'{}'", g.name))
                            .collect::<Vec<_>>()
                            .join(", ");
                        // Use object syntax for multiple guards or complex conditions
                        writeln!(output, "{detail_indent}cond: {{ type: 'and', guards: [{}] }},", guard_list)?;
                        // Or for single guard: writeln!(output, "{detail_indent}cond: '{}',", transition.guards[0].name)?;
                    }

                    // Actions
                    if !transition.actions.is_empty() {
                        let action_list = transition
                            .actions
                            .iter()
                            .map(|a| format!("'{}'", a.name))
                            .collect::<Vec<_>>()
                            .join(", ");
                        writeln!(output, "{detail_indent}actions: [{}],", action_list)?;
                    }

                    writeln!(output, "{trans_indent}}},",)?; // Close event object
                }
                writeln!(output, "{state_indent}}},",)?; // Close 'on' object
            }

            // TODO: Add invokes, after, history, etc.

            writeln!(output, "{indent}{indent}}},",)?; // Close state object
        }
    }
    writeln!(output, "{indent}}},",)?; // Close states object

    // --- Machine Options (Guards & Actions implementations - placeholders) ---
    let all_guards: HashSet<String> = machine_ast
        .states
        .iter()
        .flat_map(|s_block| {
            s_block.states.iter().flat_map(|s| {
                s.transitions
                    .iter()
                    .flat_map(|t| t.guards.iter().map(|g| g.name.clone()))
            })
        })
        .collect();

    let all_actions: HashSet<String> = machine_ast
        .states
        .iter()
        .flat_map(|s_block| {
            s_block.states.iter().flat_map(|s| {
                s.on_entry
                    .iter()
                    .map(|a| a.name.clone())
                    .chain(s.on_exit.iter().map(|a| a.name.clone()))
                    .chain(
                        s.transitions
                            .iter()
                            .flat_map(|t| t.actions.iter().map(|a| a.name.clone())),
                    )
            })
        })
        .collect();

    writeln!(output, "{indent}// --- Options (Implementations) ---")?;
    writeln!(output, "{indent}implementation: {{")?;

    // Guards
    if !all_guards.is_empty() {
        writeln!(output, "{indent}{indent}guards: {{")?;
        for guard_name in all_guards {
            // Basic placeholder implementation
            writeln!(output, "{indent}{indent}{indent}'{}': ({{ context, event }}) => {{", guard_name)?;
            writeln!(output, "{indent}{indent}{indent}  console.log('Guard check:', '{}', {{ context, event }});", guard_name)?;
            writeln!(output, "{indent}{indent}{indent}  // TODO: Implement guard logic for {}", guard_name)?;
            writeln!(output, "{indent}{indent}{indent}  return true; // Default to true
{indent}{indent}{indent}}},",)?; // Close guard function
        }
        writeln!(output, "{indent}{indent}}},",)?; // Close guards object
    }

    // Actions
    if !all_actions.is_empty() {
        writeln!(output, "{indent}{indent}actions: {{")?;
        for action_name in all_actions {
             // Basic placeholder implementation with assign example
            writeln!(output, "{indent}{indent}{indent}'{}': assign(( {{ context, event }} ) => {{", action_name)?;
            writeln!(output, "{indent}{indent}{indent}  console.log('Action executed:', '{}', {{ context, event }});", action_name)?;
            writeln!(output, "{indent}{indent}{indent}  // TODO: Implement action logic for {}", action_name)?;
            writeln!(output, "{indent}{indent}{indent}  // Example: return {{ someContextField: newValue }};
{indent}{indent}{indent}  return {{}}; // Return empty object if no context change
{indent}{indent}{indent}}}),",)?; // Close assign/action function
        }
        writeln!(output, "{indent}{indent}}},",)?; // Close actions object
    }

    // TODO: Add services (for invokes)

    writeln!(output, "{indent}}} // End of implementation object")?;

    writeln!(output, "}});")?; // Close createMachine call

    Ok(output)
}

/*
// Helper to map DSL FieldType to TypeScript type string.
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
            let inner_ts_type = map_field_type_to_ts_type(inner);
            format!("{}[]", inner_ts_type)
        }
        FieldType::Identifier(ident) => ident.to_string(), // Assume identifier is a valid TS type/interface name
    }
}

// Helper function to format event payload type for TypeScript
fn format_event_payload_type(payload_fields: &[FieldDef]) -> String {
    if payload_fields.is_empty() {
        "never".to_string() // Use never if no payload
    } else {
        let fields_str = payload_fields
            .iter()
            .map(|f| format!("  {}: {};", f.name, map_field_type_to_ts_type(&f.field_type)))
            .collect::<Vec<_>>()
            .join("\n");
        format!("{{\n{}\n}}", fields_str)
    }
}
*/
