//! TypeScript type definition generation logic.

use crate::CodegenError;
use fsm_dsl::ast::{AnnotationValue, FieldType, Ident, MessageItem, StateItem, StateMachine};
use heck::ToUpperCamelCase; // For generating PascalCase type names

// Helper function to generate JSDoc comments from annotations
fn generate_jsdoc(annotations: &[fsm_dsl::ast::Annotation]) -> String {
    let mut doc = String::new();
    if let Some(AnnotationValue::StringLiteral(desc)) =
        crate::find_annotation_value(annotations, "description")
    {
        doc.push_str("/**\n");
        // Simple wrap for lines (could be more sophisticated)
        for line in desc.lines() {
            doc.push_str(&format!(" * {}\n", line.trim()));
        }
        doc.push_str(" */"); // Remove trailing newline from doc block itself
    }
    doc
}

// Updated internal generation function
pub(crate) fn generate_typescript_types_internal(
    ast: &StateMachine,
) -> Result<String, CodegenError> {
    let mut ts_code = String::new();

    ts_code.push_str("// TypeScript types generated from .ssot
");
    ts_code.push_str(&format!("// FSM Name: {}\n\n", ast.name));

    // Add header comment from FSM annotation if present
    ts_code.push_str(&generate_jsdoc(&ast.annotations));

    // Generate State type (using union type)
    ts_code.push_str("export type State =\n");
    let state_items: Vec<String> = ast
        .states
        .iter()
        .map(|s| {
            let doc = generate_jsdoc(&s.annotations); // Generate JSDoc for state variant
            format!("{}{} | \"{}\"", "  ", doc, s.name) // Indent and add doc
        })
        .collect();
    // Adjust join logic for potentially multiline JSDoc
    ts_code.push_str(&state_items.join("\n").trim_end_matches('|').trim_end()); // Remove trailing | and whitespace
    ts_code.push_str(";\n\n");

    // --- Generate Event Payloads (Interfaces) ---
    let mut event_payload_interfaces = String::new();
    for event in &ast.events {
        if !event.fields.is_empty() {
            let type_name = event.name.to_string().to_upper_camel_case();
            let payload_interface_name = format!("{}Payload", type_name);

            // Add JSDoc for payload interfaces from event annotations
            event_payload_interfaces.push_str(&generate_jsdoc(&event.annotations));
            event_payload_interfaces.push_str(&format!(
                "export interface {} {{\n",
                payload_interface_name
            ));
            for field in &event.fields {
                // Add JSDoc for fields from field annotations
                let field_doc = generate_jsdoc(&field.annotations);
                let field_name = &field.name;
                let field_ts_type = map_field_type_to_ts_type(&field.field_type);
                // Indent doc comment correctly
                let indented_doc = field_doc.lines().map(|l| format!("  {}", l)).collect::<Vec<_>>().join("\n");
                 if !indented_doc.is_empty() {
                     event_payload_interfaces.push_str(&indented_doc);
                     event_payload_interfaces.push('\n');
                 }
                event_payload_interfaces.push_str(&format!("  {}: {};\n", field_name, field_ts_type));
            }
            event_payload_interfaces.push_str("}\n\n");
        }
    }
    if !event_payload_interfaces.is_empty() {
        ts_code.push_str(&event_payload_interfaces);
    }

    // --- Generate Event Discriminated Union ---
    // TODO: Add JSDoc for the main Event type from annotations
    ts_code.push_str("/** Discriminated union of all possible events */\n");
    ts_code.push_str("export type Event =\n");

    for event in &ast.events {
        let type_name = event.name.to_string().to_upper_camel_case();
        // Add JSDoc for individual event types from annotations
        let event_doc = generate_jsdoc(&event.annotations);
        let indented_doc = event_doc.lines().map(|l| format!("  {}", l)).collect::<Vec<_>>().join("\n");
        if !indented_doc.is_empty() {
            ts_code.push_str(&indented_doc);
            ts_code.push('\n');
        }

        if event.fields.is_empty() {
            ts_code.push_str(&format!("  | {{ type: \"{}\" }}\n", type_name));
        } else {
            let payload_interface_name = format!("{}Payload", type_name);
            ts_code.push_str(&format!(
                "  | {{ type: \"{}\", payload: {} }}\n",
                type_name,
                payload_interface_name
            ));
        }
    }
    if ast.events.is_empty() {
        ts_code.push_str("  | { type: \"__PlaceholderEvent__\" }; // No events defined\n");
    }
    ts_code.push_str(";\n");

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
            if inner_ts_type.ends_with("[]") {
                format!("{}[]", inner_ts_type)
            } else {
                format!("{}[]", inner_ts_type)
            }
        }
        FieldType::Identifier(ident) => ident.to_string(), // Assume identifier maps directly to a TS type
    }
} 