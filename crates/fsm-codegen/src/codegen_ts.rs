//! TypeScript type definition generation logic.

use crate::{find_annotation_value, CodegenError};
use fsm_dsl::ast::{AnnotationValue, FieldType, StateMachine};
use heck::ToUpperCamelCase; // For generating PascalCase type names
use std::fmt::Write; // Use write macro for better performance

// Helper function to generate JSDoc comments from annotations
fn generate_jsdoc(annotations: &[fsm_dsl::ast::Annotation], indent_level: usize) -> String {
    let mut doc = String::new();
    if let Some(AnnotationValue::StringLiteral(desc)) =
        find_annotation_value(annotations, "description")
    {
        let indent = "  ".repeat(indent_level);
        doc.push_str(&indent);
        doc.push_str("/**\n");
        // Simple wrap for lines (could be more sophisticated)
        for line in desc.lines() {
            doc.push_str(&indent);
            doc.push_str(&format!(" * {}\n", line.trim()));
        }
        doc.push_str(&indent);
        doc.push_str(" */\n"); // Add newline after doc block
    }
    doc
}

// Updated internal generation function
pub(crate) fn generate_typescript_types_internal(
    ast: &StateMachine,
) -> Result<String, CodegenError> {
    let mut ts_code = String::new();
    let machine_name_str = ast.name.to_string();

    writeln!(ts_code, "// TypeScript types generated from .ssot")?;
    writeln!(ts_code, "// FSM Name: {}\n", machine_name_str)?;

    // Add header comment from FSM annotation if present
    write!(ts_code, "{}", generate_jsdoc(&ast.annotations, 0))?;

    // --- Generate Context Interface --- (Moved before State/Event)
    writeln!(ts_code, "export interface Context {{")?;
    for field in &ast.context {
        write!(ts_code, "{}", generate_jsdoc(&field.annotations, 1))?; // Indent level 1
        let field_ts_type = map_field_type_to_ts_type(&field.field_type);
        writeln!(ts_code, "  {}: {};", field.name, field_ts_type)?;
    }
    writeln!(ts_code, "}}\n")?; // Close Context interface

    // --- Generate State type (Union) ---
    write!(ts_code, "{}", generate_jsdoc(&[], 0))?; // Placeholder for potential State type doc
    writeln!(ts_code, "export type State =")?;
    let state_items: Vec<String> = ast
        .states
        .iter()
        .map(|s| {
            // JSDoc indent level 1
            let doc = generate_jsdoc(&s.annotations, 1);
            format!("{}{} | \"{}\"", "  ", doc, s.name)
        })
        .collect();
    // Adjust join logic for potentially multiline JSDoc
    ts_code.push_str(state_items.join("\n").trim_end_matches('|').trim_end()); // Remove trailing | and whitespace
    writeln!(ts_code, ";\n")?;

    // --- Generate Event Payloads (Interfaces) ---
    let mut event_payload_interfaces = String::new();
    for event in &ast.events {
        if !event.fields.is_empty() {
            let type_name = event.name.to_string().to_upper_camel_case();
            let payload_interface_name = format!("{}Payload", type_name);

            // JSDoc indent level 0
            write!(
                event_payload_interfaces,
                "{}",
                generate_jsdoc(&event.annotations, 0)
            )?;
            writeln!(
                event_payload_interfaces,
                "export interface {} {{",
                payload_interface_name
            )?;
            for field in &event.fields {
                // JSDoc indent level 1
                write!(
                    event_payload_interfaces,
                    "{}",
                    generate_jsdoc(&field.annotations, 1)
                )?;
                let field_name = &field.name;
                let field_ts_type = map_field_type_to_ts_type(&field.field_type);
                writeln!(
                    event_payload_interfaces,
                    "  {}: {};",
                    field_name, field_ts_type
                )?;
            }
            writeln!(event_payload_interfaces, "}}\n")?;
        }
    }
    if !event_payload_interfaces.is_empty() {
        ts_code.push_str(&event_payload_interfaces);
    }

    // --- Generate Event Discriminated Union ---
    // TODO: Add JSDoc for the main Event type from annotations
    writeln!(ts_code, "/** Discriminated union of all possible events */")?;
    writeln!(ts_code, "export type Event =")?;

    for (index, event) in ast.events.iter().enumerate() {
        let type_name = event.name.to_string().to_upper_camel_case();
        // JSDoc indent level 1
        write!(ts_code, "{}", generate_jsdoc(&event.annotations, 1))?;

        let prefix = if index == 0 { "  " } else { "| " };

        if event.fields.is_empty() {
            writeln!(ts_code, "{} {{ type: \"{}\" }}", prefix, type_name)?;
        } else {
            let payload_interface_name = format!("{}Payload", type_name);
            writeln!(
                ts_code,
                "{} {{ type: \"{}\", payload: {} }}",
                prefix, type_name, payload_interface_name
            )?;
        }
    }
    if ast.events.is_empty() {
        writeln!(
            ts_code,
            "    {{ type: \"__PlaceholderEvent__\" }}; // No events defined"
        )?;
    } else {
        // Add semicolon only if there are events
        ts_code.pop(); // Remove last newline
        ts_code.push_str(";\n");
    }

    // TODO:
    // - Optionally generate types for Guards and Actions.
    // - Handle FieldType::Identifier more robustly (imports?).

    Ok(ts_code)
}

/// Maps DSL FieldType to TypeScript type string.
fn map_field_type_to_ts_type(field_type: &FieldType) -> String {
    match field_type {
        FieldType::Void => "undefined".to_string(), // Consistent with `void` functions returning undefined
        FieldType::Bool => "boolean".to_string(),
        FieldType::Int8
        | FieldType::Int16
        | FieldType::Int32
        | FieldType::UInt8
        | FieldType::UInt16
        | FieldType::UInt32
        | FieldType::Float32
        | FieldType::Float64 => "number".to_string(),
        // Use bigint for 64-bit integers if strictness is required, otherwise number might suffice
        FieldType::Int64 | FieldType::UInt64 => "bigint".to_string(),
        FieldType::Text => "string".to_string(),
        FieldType::Data => "Uint8Array".to_string(),
        FieldType::List(inner) => {
            let inner_ts_type = map_field_type_to_ts_type(inner);
            // Handle potential nested lists correctly
            format!("{}[]", inner_ts_type) // Simplified array type generation
        }
        FieldType::Identifier(ident) => ident.to_string(), // Assume identifier maps directly to a TS type
    }
}
