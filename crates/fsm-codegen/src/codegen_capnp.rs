//! Cap'n Proto schema generation logic.

use crate::CodegenError;
use fsm_dsl::ast::{FieldType, Ident, StateMachine, SsotFile};

// Function to map DSL FieldType to Cap'n Proto type string
fn map_field_type_to_capnp_type(field_type: &FieldType) -> String {
    match field_type {
        FieldType::Void => "Void".to_string(),
        FieldType::Bool => "Bool".to_string(),
        FieldType::Int8 => "Int8".to_string(),
        FieldType::Int16 => "Int16".to_string(),
        FieldType::Int32 => "Int32".to_string(),
        FieldType::Int64 => "Int64".to_string(),
        FieldType::UInt8 => "UInt8".to_string(),
        FieldType::UInt16 => "UInt16".to_string(),
        FieldType::UInt32 => "UInt32".to_string(),
        FieldType::UInt64 => "UInt64".to_string(),
        FieldType::Float32 => "Float32".to_string(),
        FieldType::Float64 => "Float64".to_string(),
        FieldType::Text => "Text".to_string(),
        FieldType::Data => "Data".to_string(),
        FieldType::List(inner) => {
            let inner_capnp_type = map_field_type_to_capnp_type(inner);
            // Cap'n Proto requires List(T) where T is a known type (primitive, struct, enum, list, data, text)
            // Assuming List can contain primitives, structs, text, data, or other lists directly.
            format!("List({})", inner_capnp_type)
        }
        FieldType::Identifier(ident) => ident.to_string(), // Assume identifier is a valid Cap'n Proto struct/enum name
    }
}

// Updated internal generation function
pub(crate) fn generate_capnp_schema_internal(
    file_ast: &SsotFile,
    machine_ast: &StateMachine,
) -> Result<String, CodegenError> {
    let mut capnp_code = String::new();

    // --- File ID ---
    capnp_code.push_str(&format!("@0x{:x};\n\n", file_ast.file_id));

    // --- File Header Comment ---
    capnp_code.push_str(&format!(
        "# Cap'n Proto schema generated from .ssot for {}
",
        machine_ast.name
    ));
    // Add package declaration if present (using annotation for now)
    // TODO: Use file_ast.package_declaration when available
    if let Some(AnnotationValue::StringLiteral(pkg)) =
        find_annotation_value(&file_ast.top_level_annotations, "capnpPackage")
    {
        capnp_code.push_str(&format!("# package: {}\n", pkg));
    }
    capnp_code.push_str("\n");

    // --- State Enum ---
    capnp_code.push_str("enum State @0 {\n");
    for state in &machine_ast.states {
        capnp_code.push_str(&format!("  {} @{};\n", state.name, state.ordinal));
    }
    capnp_code.push_str("}\n\n");

    // --- Event Payloads (Structs) ---
    let mut event_payload_structs = String::new();
    for event in &machine_ast.events {
        if !event.fields.is_empty() {
            let struct_name = format!("{}Payload", event.name); // Use PascalCase? Cap'n Proto uses camelCase generally
            event_payload_structs.push_str(&format!("struct {} @{} {{\n", struct_name, event.ordinal)); // Use event ordinal? Need unique IDs for structs.
            for field in &event.fields {
                let field_capnp_type = map_field_type_to_capnp_type(&field.field_type);
                event_payload_structs.push_str(&format!(
                    "  {} @{} :{};
",
                    field.name,
                    field.ordinal,
                    field_capnp_type
                ));
            }
            event_payload_structs.push_str("}\n\n");
        }
    }
    if !event_payload_structs.is_empty() {
        capnp_code.push_str(&event_payload_structs);
    }

    // --- Event Union ---
    capnp_code.push_str("union Event @1 {\n"); // Assign next available ID
    for event in &machine_ast.events {
        let event_name_capnp = &event.name; // Use original name or convert case?
        if event.fields.is_empty() {
            capnp_code.push_str(&format!("  {} @{} :Void;\n", event_name_capnp, event.ordinal));
        } else {
            let payload_struct_name = format!("{}Payload", event.name);
            capnp_code.push_str(&format!(
                "  {} @{} :{};\n",
                event_name_capnp,
                event.ordinal,
                payload_struct_name
            ));
        }
    }
    capnp_code.push_str("}\n\n");

    // --- Optional: StateMachine Definition Struct ---
    // Can add this later if needed

    // TODO:
    // - Need a robust way to assign unique @ IDs for structs (Payloads).
    // - Handle annotations ($description -> # comments).
    // - Decide on casing conventions (camelCase vs PascalCase for structs/enums/fields).
    // - Validate FieldType::Identifier references against generated structs/enums.
    // - Add support for `using` declarations if needed.

    Ok(capnp_code)
}

// Helper to find annotation value by name (copied from lib.rs or import?)
// This should ideally be shared or imported.
use fsm_dsl::ast::{Annotation, AnnotationValue};
fn find_annotation_value<'a>(
    annotations: &'a [Annotation],
    name: &str,
) -> Option<&'a AnnotationValue> {
    annotations
        .iter()
        .find(|a| a.name == name)
        .and_then(|a| a.value.as_ref())
} 