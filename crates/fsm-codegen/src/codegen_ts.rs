//! TypeScript type definition generation logic.

// use crate::{find_annotation_value, CodegenError}; // find_annotation_value likely still needed
use crate::CodegenError;
use fsm_dsl::ast::{
    Annotation,
    AnnotationValue,
    Argument,
    ContextDefinition,
    ContextFieldDefinition,
    EnumDefinition,
    EnumVariant,
    FieldDefinition,
    Identifier,
    MachineDefinition,
    NumericId,
    // Needed for mapping
    PrimitiveType,
    SsotAst,
    StateDefinition,
    StatesBlock,
    StructDefinition,
    TopLevelDefinition,
    TypeDefinition,
    TypeSpecifier,
};
// use fsm_dsl::ast::{AnnotationValue, FieldType, /* MessageItem, StateItem, */ StateMachine}; // Original line commented out
use heck::ToUpperCamelCase; // For generating PascalCase type names
use std::fmt::Write; // Use write macro for better performance

// Helper function to generate JSDoc comments from annotations
fn generate_jsdoc(annotations: &[Annotation], indent_level: usize) -> String {
    let mut doc = String::new();
    // Find $description annotation
    let description = annotations.iter().find_map(|anno| match anno {
        Annotation::Description(desc) => Some(desc),
        _ => None,
    });

    if let Some(desc) = description {
        let indent = "  ".repeat(indent_level);
        doc.push_str(&indent);
        doc.push_str("/**\n");
        for line in desc.lines() {
            doc.push_str(&indent);
            doc.push_str(&format!(" * {}\n", line.trim()));
        }
        doc.push_str(&indent);
        doc.push_str(" */\n");
    }
    doc
}

// Updated internal generation function to use SsotAst
pub(crate) fn generate_typescript_types_internal(ast: &SsotAst) -> Result<String, CodegenError> {
    let mut ts_code = String::new();
    let mut zod_code = String::new(); // Buffer for Zod schemas

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

    writeln!(ts_code, "// TypeScript types generated from .ssot")?;
    writeln!(ts_code, "// FSM Name: {}\n", machine_name_str)?;
    writeln!(zod_code, "// Zod schemas generated from .ssot")?;
    writeln!(zod_code, "// FSM Name: {}\n", machine_name_str)?;
    writeln!(zod_code, "import {{ z }} from 'zod';\n")?; // Add Zod import

    // Add header comment from FSM annotation if present
    let header_doc = generate_jsdoc(&machine_ast.annotations, 0);
    write!(ts_code, "{}", header_doc)?;
    // Zod doesn't have a direct equivalent for file-level docs

    // --- Generate Types from types {} block ---
    for def in &ast.definitions {
        if let TopLevelDefinition::Types(types_block) = def {
            write!(ts_code, "{}", generate_jsdoc(&types_block.annotations, 0))?;
            for type_def in &types_block.definitions {
                match type_def {
                    TypeDefinition::Struct(struct_def) => {
                        let struct_name = &struct_def.name.name;
                        write!(ts_code, "{}", generate_jsdoc(&struct_def.annotations, 0))?;
                        writeln!(ts_code, "export interface {} {{", struct_name)?;
                        writeln!(
                            zod_code,
                            "{}",
                            generate_jsdoc(&struct_def.annotations, 0).replace(" * ", " // ")
                        )?;
                        writeln!(zod_code, "export const {}Schema = z.object({{", struct_name)?;
                        for field in &struct_def.fields {
                            write!(ts_code, "{}", generate_jsdoc(&field.annotations, 1))?;
                            let field_ts_type = map_type_specifier_to_ts_type(&field.type_spec)?;
                            writeln!(ts_code, "  {}: {};", field.name.name, field_ts_type)?;

                            write!(
                                zod_code,
                                "{}",
                                generate_jsdoc(&field.annotations, 1).replace(" * ", " // ")
                            )?;
                            let field_zod_type = map_type_specifier_to_zod_type(&field.type_spec)?;
                            writeln!(zod_code, "  {}: {},", field.name.name, field_zod_type)?;
                        }
                        writeln!(ts_code, "}}\n")?;
                        writeln!(zod_code, "}});\n")?;
                    }
                    TypeDefinition::Enum(enum_def) => {
                        let enum_name = &enum_def.name.name;
                        write!(ts_code, "{}", generate_jsdoc(&enum_def.annotations, 0))?;
                        writeln!(ts_code, "export type {} =", enum_name)?;
                        writeln!(
                            zod_code,
                            "{}",
                            generate_jsdoc(&enum_def.annotations, 0).replace(" * ", " // ")
                        )?;
                        writeln!(zod_code, "export const {}Schema = z.enum([", enum_name)?;
                        let variants_ts: Vec<String> = enum_def
                            .variants
                            .iter()
                            .map(|v| {
                                let doc = generate_jsdoc(&v.annotations, 1);
                                format!("{}{} | \"{}\"", "  ", doc, v.name.name)
                            })
                            .collect();
                        let variants_zod: Vec<String> = enum_def
                            .variants
                            .iter()
                            .map(|v| format!("  \"{}\",", v.name.name))
                            .collect();

                        ts_code.push_str(&variants_ts.join("\n").trim_end_matches('|').trim_end());
                        writeln!(ts_code, ";\n")?;
                        writeln!(zod_code, "{}", variants_zod.join("\n"))?;
                        writeln!(zod_code, "]);\n")?;
                    }
                }
            }
        }
    }

    // --- Generate Context Interface ---
    if let Some(context_def) = &machine_ast.context {
        write!(ts_code, "{}", generate_jsdoc(&context_def.annotations, 0))?;
        writeln!(ts_code, "export interface Context {{")?;
        writeln!(
            zod_code,
            "{}",
            generate_jsdoc(&context_def.annotations, 0).replace(" * ", " // ")
        )?;
        writeln!(zod_code, "export const ContextSchema = z.object({{")?;
        for field in &context_def.fields {
            let js_doc = generate_jsdoc(&field.annotations, 1);
            write!(ts_code, "{}", js_doc)?;
            let field_ts_type = map_type_specifier_to_ts_type(&field.type_spec)?;
            writeln!(ts_code, "  {}: {};", field.name.name, field_ts_type)?;

            write!(zod_code, "{}", js_doc.replace(" * ", " // "))?; // Convert JSDoc to Zod comment
            let field_zod_type = map_type_specifier_to_zod_type(&field.type_spec)?;
            writeln!(zod_code, "  {}: {},", field.name.name, field_zod_type)?;
        }
        writeln!(ts_code, "}}\n")?;
        writeln!(zod_code, "}});\n")?;
    } else {
        // Define empty context if none exists
        writeln!(ts_code, "export interface Context {{}}")?;
        writeln!(zod_code, "export const ContextSchema = z.object({{}});")?;
    }

    // --- Generate State type (Union for TS, Enum for Zod) ---
    if let Some(states_block) = &machine_ast.states {
        write!(ts_code, "{}", generate_jsod(&states_block.annotations, 0))?;
        writeln!(ts_code, "export type State =")?;
        writeln!(
            zod_code,
            "{}",
            generate_jsod(&states_block.annotations, 0).replace(" * ", " // ")
        )?;
        writeln!(zod_code, "export const StateSchema = z.enum([")?;
        let state_items_ts: Vec<String> = states_block
            .states
            .iter()
            .map(|s| {
                let doc = generate_jsod(&s.annotations, 1);
                format!("{}{} | \"{}\"", "  ", doc, s.name.name)
            })
            .collect();
        let state_items_zod: Vec<String> = states_block
            .states
            .iter()
            .map(|s| format!("  \"{}\",", s.name.name))
            .collect();

        ts_code.push_str(&state_items_ts.join("\n").trim_end_matches('|').trim_end());
        writeln!(ts_code, ";\n")?;
        writeln!(zod_code, "{}", state_items_zod.join("\n"))?;
        writeln!(zod_code, "]);\n")?;
    } else {
        // Handle case with no states
        writeln!(ts_code, "export type State = never; // No states defined")?;
        writeln!(
            zod_code,
            "export const StateSchema = z.enum([]); // No states defined"
        )?;
    }

    // --- Generate Event Payloads and Discriminated Union ---
    // Find the events block (assuming only one)
    let events_block_opt = ast.definitions.iter().find_map(|def| {
        if let TopLevelDefinition::Communication(comm_block) = def {
            // TODO: Refine this - assumes events are directly in CommunicationBlock
            // Need to find EventDefinition within CommunicationItem
            Some(comm_block)
        } else {
            None
        }
    });

    let mut event_payload_interfaces = String::new();
    let mut event_payload_schemas = String::new();
    let mut event_union_ts_parts = Vec::new();
    let mut event_union_zod_parts = Vec::new();

    if let Some(events_block) = events_block_opt {
        // TODO: Iterate through events_block.definitions (CommunicationItem)
        // and filter for CommunicationItem::Event(event_def)
        // let events = ... filter events_block.definitions ...

        // Placeholder: Assume events are directly available (needs fix based on CommunicationItem)
        let events: Vec<&fsm_dsl::ast::EventDefinition> = Vec::new(); // Placeholder

        for event in &events {
            // TODO: Extract EventDefinition details (name, fields/payload)
            // Need to adjust EventDefinition struct in ast.rs if it doesn't match expectations
            let event_name = "PlaceholderEventName"; // Replace with event.name.name
            let event_annotations = &Vec::<Annotation>::new(); // Replace with event.annotations
            let event_payload: Option<&TypeSpecifier> = None; // Replace with event.payload or similar

            let type_name = event_name.to_upper_camel_case();
            let event_doc_ts = generate_jsdoc(event_annotations, 1);

            if let Some(payload_type) = event_payload {
                let payload_interface_name = format!("{}Payload", type_name);
                let payload_schema_name = format!("{}PayloadSchema", type_name);

                // Generate Payload Interface/Schema if it has fields
                // Assuming payload_type refers to a struct defined in types block
                // We might need to look up the struct definition here.
                // For simplicity, let's assume the payload type is directly mapped.

                let payload_ts_type = map_type_specifier_to_ts_type(payload_type)?;
                let payload_zod_schema = map_type_specifier_to_zod_type(payload_type)?;

                // Check if payload type is a simple type or needs its own interface
                // If it's a complex type (struct/enum reference), we assume it was generated above.
                if let TypeSpecifier::Simple(ident) = payload_type {
                    // Assume it refers to a generated interface/schema
                    event_union_ts_parts.push(format!(
                        "{}{{ type: \"{}\", payload: {} }}",
                        event_doc_ts, type_name, ident.name
                    ));
                    event_union_zod_parts.push(format!(
                        "  z.object({{ type: z.literal(\"{}\"), payload: {}Schema }}),",
                        type_name, ident.name
                    ));
                } else {
                    // For primitive lists, optionals, maps, generate inline?
                    // Or generate separate Payload interfaces? For now, treat as any.
                    event_union_ts_parts.push(format!(
                        "{}{{ type: \"{}\", payload: {} }}",
                        event_doc_ts, type_name, payload_ts_type
                    ));
                    // Zod schema needs careful handling for complex inline types
                    event_union_zod_parts.push(format!(
                        "  z.object({{ type: z.literal(\"{}\"), payload: {} }}),",
                        type_name, payload_zod_schema
                    ));
                }
            } else {
                // Event without payload
                event_union_ts_parts.push(format!("{}{{ type: \"{}\" }}", event_doc_ts, type_name));
                event_union_zod_parts.push(format!(
                    "  z.object({{ type: z.literal(\"{}\") }}),",
                    type_name
                ));
            }
        }
    }

    // Combine event union parts
    if event_union_ts_parts.is_empty() {
        writeln!(ts_code, "/** Discriminated union of all possible events */")?;
        writeln!(ts_code, "export type Event = never; // No events defined")?;
        writeln!(
            zod_code,
            "// Discriminated union schema for all possible events"
        )?;
        writeln!(
            zod_code,
            "export const EventSchema = z.union([]); // No events defined"
        )?;
    } else {
        writeln!(ts_code, "/** Discriminated union of all possible events */")?;
        writeln!(ts_code, "export type Event =")?;
        for (i, part) in event_union_ts_parts.iter().enumerate() {
            let prefix = if i == 0 { "  " } else { "| " };
            writeln!(ts_code, "{}{}", prefix, part)?;
        }
        ts_code.pop(); // Remove last newline
        ts_code.push_str(";\n");

        writeln!(
            zod_code,
            "// Discriminated union schema for all possible events"
        )?;
        writeln!(
            zod_code,
            "export const EventSchema = z.discriminatedUnion(\"type\", ["
        )?;
        writeln!(zod_code, "{}", event_union_zod_parts.join("\n"))?;
        writeln!(zod_code, "]);\n")?;
    }

    // Combine TS and Zod code
    Ok(format!("{}\n\n{}", ts_code, zod_code))
}

/// Maps DSL TypeSpecifier to TypeScript type string.
fn map_type_specifier_to_ts_type(type_spec: &TypeSpecifier) -> Result<String, CodegenError> {
    match type_spec {
        TypeSpecifier::Simple(ident) => {
            // Map known primitive types
            match ident.name.as_str() {
                "bool" => Ok("boolean".to_string()),
                "int" | "i8" | "i16" | "i32" | "u8" | "u16" | "u32" | "f32" | "f64" => {
                    Ok("number".to_string())
                }
                "i64" | "u64" => Ok("bigint".to_string()),
                "string" | "text" => Ok("string".to_string()),
                "data" => Ok("Uint8Array".to_string()),
                "void" => Ok("undefined".to_string()),
                // Assume other simple identifiers are custom types defined elsewhere
                custom => Ok(custom.to_string()),
            }
        }
        TypeSpecifier::List(inner) => {
            let inner_ts_type = map_type_specifier_to_ts_type(inner)?;
            Ok(format!("{}[]", inner_ts_type))
        }
        TypeSpecifier::Optional(inner) => {
            let inner_ts_type = map_type_specifier_to_ts_type(inner)?;
            Ok(format!("{} | null | undefined", inner_ts_type))
        }
        TypeSpecifier::Map(key, value) => {
            let key_ts_type = map_type_specifier_to_ts_type(key)?;
            // Ensure map keys are valid types (string, number, symbol, etc.)
            let valid_key_type = match key_ts_type.as_str() {
                "string" | "number" | "boolean" => key_ts_type,
                // bigint and complex types are not typical JS Map keys
                _ => "string".to_string(), // Default to string or raise error
            };
            let value_ts_type = map_type_specifier_to_ts_type(value)?;
            // Use Record for object-like maps, or Map for Map instances
            Ok(format!("Record<{}, {}>", valid_key_type, value_ts_type))
            // Or: format!("Map<{}, {}>", valid_key_type, value_ts_type)
        }
    }
}

/// Maps DSL TypeSpecifier to Zod schema string.
fn map_type_specifier_to_zod_type(type_spec: &TypeSpecifier) -> Result<String, CodegenError> {
    match type_spec {
        TypeSpecifier::Simple(ident) => {
            match ident.name.as_str() {
                "bool" => Ok("z.boolean()".to_string()),
                "int" | "i8" | "i16" | "i32" | "u8" | "u16" | "u32" => {
                    Ok("z.number().int()".to_string())
                }
                "f32" | "f64" => Ok("z.number()".to_string()),
                "i64" | "u64" => Ok("z.bigint()".to_string()),
                "string" | "text" => Ok("z.string()".to_string()),
                "data" => Ok("z.instanceof(Uint8Array)".to_string()),
                "void" => Ok("z.undefined()".to_string()),
                // Assume other simple identifiers refer to schemas defined elsewhere
                custom => Ok(format!("{}Schema", custom)), // Assumes schema has "Schema" suffix
            }
        }
        TypeSpecifier::List(inner) => {
            let inner_zod_type = map_type_specifier_to_zod_type(inner)?;
            Ok(format!("z.array({})", inner_zod_type))
        }
        TypeSpecifier::Optional(inner) => {
            let inner_zod_type = map_type_specifier_to_zod_type(inner)?;
            Ok(format!("{}.nullable().optional()", inner_zod_type))
        }
        TypeSpecifier::Map(key, value) => {
            let key_zod_type = map_type_specifier_to_zod_type(key)?;
            // Zod record keys must be string, number, or enum
            let valid_key_zod_type = match key.as_ref() {
                TypeSpecifier::Simple(id) => match id.name.as_str() {
                    "string" | "text" => "z.string()".to_string(),
                    "int" | "i8" | "i16" | "i32" | "u8" | "u16" | "u32" => {
                        "z.number().int()".to_string()
                    } // Or z.string() if using string keys
                    _ => "z.string()".to_string(), // Default or perhaps check if it's an enum schema
                },
                _ => "z.string()".to_string(), // Default for non-simple keys
            };
            let value_zod_type = map_type_specifier_to_zod_type(value)?;
            Ok(format!(
                "z.record({}, {})",
                valid_key_zod_type, value_zod_type
            ))
        }
    }
}
