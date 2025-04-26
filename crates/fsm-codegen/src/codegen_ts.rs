//! TypeScript type definition generation logic.

use crate::{find_annotation_value, CodegenError};
use fsm_dsl::ast::{AnnotationValue, FieldType, /* MessageItem, StateItem, */ StateMachine};
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
        doc.push_str("/**\\n");
        // Simple wrap for lines (could be more sophisticated)
        for line in desc.lines() {
            doc.push_str(&indent);
            doc.push_str(&format!(" * {}\\n", line.trim()));
        }
        doc.push_str(&indent);
        doc.push_str(" */\\n"); // Add newline after doc block
    }
    doc
}

// Updated internal generation function
pub(crate) fn generate_typescript_types_internal(
    ast: &StateMachine,
) -> Result<String, CodegenError> {
    let mut ts_code = String::new();
    let mut zod_code = String::new(); // Buffer for Zod schemas
    let machine_name_str = ast.name.to_string();

    writeln!(ts_code, "// TypeScript types generated from .ssot")?;
    writeln!(ts_code, "// FSM Name: {}\\n", machine_name_str)?;
    writeln!(zod_code, "// Zod schemas generated from .ssot")?;
    writeln!(zod_code, "// FSM Name: {}\\n", machine_name_str)?;
    writeln!(zod_code, "import {{ z }} from 'zod';\\n")?; // Add Zod import

    // Add header comment from FSM annotation if present
    let header_doc = generate_jsdoc(&ast.annotations, 0);
    write!(ts_code, "{}", header_doc)?;
    // Zod doesn't have a direct equivalent for file-level docs

    // --- Generate Context Interface ---
    writeln!(ts_code, "export interface Context {{")?;
    writeln!(zod_code, "export const ContextSchema = z.object({{")?;
    for field in &ast.context {
        let js_doc = generate_jsdoc(&field.annotations, 1); // Indent level 1
        write!(ts_code, "{}", js_doc)?;
        let field_ts_type = map_field_type_to_ts_type(&field.field_type);
        writeln!(ts_code, "  {}: {};", field.name, field_ts_type)?;

        // Add to Zod schema
        write!(zod_code, "{}", js_doc.replace(" * ", " // "))?; // Convert JSDoc to Zod comment
        let field_zod_type = map_field_type_to_zod_type(&field.field_type);
        writeln!(zod_code, "  {}: {},", field.name, field_zod_type)?;
    }
    writeln!(ts_code, "}}\\n")?; // Close Context interface
    writeln!(zod_code, "}});\\n")?; // Close Context schema

    // --- Generate State type (Union for TS, Enum for Zod) ---
    write!(ts_code, "{}", generate_jsdoc(&[], 0))?; // Placeholder for potential State type doc
    writeln!(ts_code, "export type State =")?;
    writeln!(zod_code, "// Represents the possible states of the machine")?;
    writeln!(zod_code, "export const StateSchema = z.enum([")?;
    let state_items_ts: Vec<String> = ast
        .states
        .iter()
        .map(|s| {
            // JSDoc indent level 1
            let doc = generate_jsdoc(&s.annotations, 1);
            format!("{}{} | \\\"{}\\\"", "  ", doc, s.name)
        })
        .collect();
    let state_items_zod: Vec<String> = ast
        .states
        .iter()
        .map(|s| format!("  \"{}\",", s.name))
        .collect();
    // Adjust join logic for potentially multiline JSDoc
    ts_code.push_str(state_items_ts.join("\n").trim_end_matches('|').trim_end()); // Remove trailing | or space
    writeln!(ts_code, ";\\n")?;
    writeln!(zod_code, "{}", state_items_zod.join("\n"))?;
    writeln!(zod_code, "]);\\n")?;

    // --- Generate Event Payloads (Interfaces for TS, Schemas for Zod) ---
    let mut event_payload_interfaces = String::new();
    let mut event_payload_schemas = String::new();
    for event in &ast.events {
        if !event.fields.is_empty() {
            let type_name = event.name.to_string().to_upper_camel_case();
            let payload_interface_name = format!("{}Payload", type_name);
            let payload_schema_name = format!("{}PayloadSchema", type_name);

            // JSDoc indent level 0
            let event_doc = generate_jsdoc(&event.annotations, 0);
            write!(event_payload_interfaces, "{}", event_doc)?;
            writeln!(
                event_payload_schemas,
                "// Schema for {}",
                payload_interface_name
            )?;

            writeln!(
                event_payload_interfaces,
                "export interface {} {{",
                payload_interface_name
            )?;
            writeln!(
                event_payload_schemas,
                "export const {} = z.object({{",
                payload_schema_name
            )?;

            for field in &event.fields {
                // JSDoc indent level 1
                let field_doc_ts = generate_jsdoc(&field.annotations, 1);
                let field_doc_zod = field_doc_ts.replace(" * ", " // "); // Convert JSDoc to Zod comment
                write!(event_payload_interfaces, "{}", field_doc_ts)?;
                write!(event_payload_schemas, "{}", field_doc_zod)?;

                let field_name = &field.name;
                let field_ts_type = map_field_type_to_ts_type(&field.field_type);
                let field_zod_type = map_field_type_to_zod_type(&field.field_type);

                writeln!(
                    event_payload_interfaces,
                    "  {}: {};",
                    field_name, field_ts_type
                )?;
                writeln!(
                    event_payload_schemas,
                    "  {}: {},",
                    field_name, field_zod_type
                )?;
            }
            writeln!(event_payload_interfaces, "}}\\n")?;
            writeln!(event_payload_schemas, "}});\\n")?; // Close Zod object
        }
    }
    if !event_payload_interfaces.is_empty() {
        ts_code.push_str(&event_payload_interfaces);
        zod_code.push_str(&event_payload_schemas);
    }

    // --- Generate Event Discriminated Union (TS & Zod) ---
    writeln!(ts_code, "/** Discriminated union of all possible events */")?;
    writeln!(ts_code, "export type Event =")?;
    writeln!(
        zod_code,
        "// Discriminated union schema for all possible events"
    )?;
    writeln!(
        zod_code,
        "export const EventSchema = z.discriminatedUnion(\"type\", ["
    )?;

    for (index, event) in ast.events.iter().enumerate() {
        let type_name = event.name.to_string().to_upper_camel_case();
        // JSDoc indent level 1 for TS
        let event_doc_ts = generate_jsdoc(&event.annotations, 1);
        write!(ts_code, "{}", event_doc_ts)?;

        let prefix_ts = if index == 0 { "  " } else { "| " };

        if event.fields.is_empty() {
            writeln!(ts_code, "{} {{ type: \\\"{}\\\" }}", prefix_ts, type_name)?;
            // Zod part for event without payload
            writeln!(
                zod_code,
                "  z.object({{ type: z.literal(\"{}\") }}),",
                type_name
            )?;
        } else {
            let payload_interface_name = format!("{}Payload", type_name);
            let payload_schema_name = format!("{}PayloadSchema", type_name);
            writeln!(
                ts_code,
                "{} {{ type: \\\"{}\\\", payload: {} }}",
                prefix_ts, type_name, payload_interface_name
            )?;
            // Zod part for event with payload
            writeln!(
                zod_code,
                "  z.object({{ type: z.literal(\"{}\"), payload: {} }}),",
                type_name, payload_schema_name
            )?;
        }
    }
    if ast.events.is_empty() {
        writeln!(
            ts_code,
            "    {{ type: \\\"__PlaceholderEvent__\\\" }}; // No events defined"
        )?;
        // Handle empty events for Zod (might need a placeholder or different structure)
        writeln!(
            zod_code,
            "  // No events defined, Zod schema might be empty or require a placeholder"
        )?;
    } else {
        // Add semicolon only if there are events for TS
        ts_code.pop(); // Remove last newline
        ts_code.push_str(";\\n");
    }
    writeln!(zod_code, "]);\\n")?; // Close Zod discriminated union

    // TODO:\n    // - Optionally generate types/schemas for Guards and Actions.\n    // - Handle FieldType::Identifier more robustly (imports?).\n
    // Combine TS and Zod code
    Ok(format!("{}\n\n{}", ts_code, zod_code))
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

/// Maps DSL FieldType to Zod schema string.
fn map_field_type_to_zod_type(field_type: &FieldType) -> String {
    match field_type {
        FieldType::Void => "z.undefined()".to_string(),
        FieldType::Bool => "z.boolean()".to_string(),
        FieldType::Int8
        | FieldType::Int16
        | FieldType::Int32
        | FieldType::UInt8
        | FieldType::UInt16
        | FieldType::UInt32 => "z.number().int()".to_string(), // Add .int() for integer types
        FieldType::Float32 | FieldType::Float64 => "z.number()".to_string(),
        FieldType::Int64 | FieldType::UInt64 => "z.bigint()".to_string(),
        FieldType::Text => "z.string()".to_string(),
        FieldType::Data => "z.instanceof(Uint8Array)".to_string(), // Use instanceof for specific types
        FieldType::List(inner) => {
            let inner_zod_type = map_field_type_to_zod_type(inner);
            format!("z.array({})", inner_zod_type)
        }
        FieldType::Identifier(ident) => ident.to_string(), // Assume identifier maps to an existing Zod schema constant
    }
}
