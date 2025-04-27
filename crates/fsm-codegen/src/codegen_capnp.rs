//! Cap'n Proto schema generation logic.

use crate::CodegenError;
use fsm_dsl::ast::{AnnotationValue, SsotAst, MachineDefinition, TypeDefinition, StructDefinition, EnumDefinition, EnumVariant, FieldDefinition, TypeSpecifier, Annotation, Identifier, ContextDefinition, ContextFieldDefinition, StatesBlock, StateDefinition, TransitionDefinition, PrimitiveType};
// use std::fmt::Write; // Removed unused import

// Function to map DSL TypeSpecifier to Cap'n Proto type string
fn map_type_specifier_to_capnp_type(type_spec: &TypeSpecifier) -> String {
    match type_spec {
        TypeSpecifier::Primitive(prim) => match prim {
            PrimitiveType::Void => "Void".to_string(),
            PrimitiveType::Bool => "Bool".to_string(),
            PrimitiveType::Int8 => "Int8".to_string(),
            PrimitiveType::Int16 => "Int16".to_string(),
            PrimitiveType::Int32 => "Int32".to_string(),
            PrimitiveType::Int64 => "Int64".to_string(),
            PrimitiveType::UInt8 => "UInt8".to_string(),
            PrimitiveType::UInt16 => "UInt16".to_string(),
            PrimitiveType::UInt32 => "UInt32".to_string(),
            PrimitiveType::UInt64 => "UInt64".to_string(),
            PrimitiveType::Float32 => "Float32".to_string(),
            PrimitiveType::Float64 => "Float64".to_string(),
            PrimitiveType::String => "Text".to_string(), // Map DSL String to Cap'n Proto Text
            PrimitiveType::Bytes => "Data".to_string(),  // Map DSL Bytes to Cap'n Proto Data
        },
        TypeSpecifier::Identifier(ident) => ident.name.to_string(), // Assume identifier is a defined struct/enum
        TypeSpecifier::List(inner) => {
            let inner_capnp_type = map_type_specifier_to_capnp_type(inner);
            // Cap'n Proto requires List(T) where T is a known type (primitive, struct, enum, list, data, text)
            format!("List({})", inner_capnp_type)
        }
        // Add other TypeSpecifier variants if necessary (e.g., Map, Tuple)
    }
}

// Helper function to generate Cap'n Proto comments from annotations
fn generate_capnp_comment(annotations: &[Annotation], indent: &str) -> String {
    let mut comment_str = String::new();
    // Use the find_annotation_value helper defined below
    if let Some(AnnotationValue::String(desc)) = // Changed StringLiteral to String
        find_annotation_value(annotations, "description")
    {
        for line in desc.lines() {
            comment_str.push_str(&format!("{}# {}\n", indent, line.trim()));
        }
    }
    // Add other annotation processing here if needed
    comment_str
}

// Updated internal generation function
pub(crate) fn generate_capnp_schema_internal(
    ast: &SsotAst, // Use the new top-level AST type
    // machine_ast: &StateMachine, // Remove old machine type
) -> Result<String, CodegenError> {
    // Find the machine definition within the AST
    let machine_ast = ast.definitions.iter().find_map(|def| {
        if let TypeDefinition::Machine(m) = def {
            Some(m)
        } else {
            None
        }
    }).ok_or_else(|| CodegenError::AstValidationError("No machine definition found in AST".to_string()))?;

    let mut capnp_code = String::new();
    let mut type_id_counter = 0u64; // Counter for unique Cap'n Proto type IDs (@0, @1, ...)

    // --- File ID (Placeholder) ---
    // TODO: Implement a stable File ID generation strategy (e.g., hash-based)
    let file_id = 0xCAFEBABECAFED00D; // Placeholder ID
    capnp_code.push_str(&format!("@0x{:x};\n\n", file_id));

    // --- File Header Comment ---
    capnp_code.push_str(&generate_capnp_comment(&machine_ast.annotations, ""));
    capnp_code.push_str(&format!(
        "# Cap'n Proto schema generated from .ssot for {}\n\n",
        machine_ast.name.name
    ));

    // --- Using Declarations (Optional - for imports) ---
    // TODO: Generate 'using import "other_schema.capnp".*;' if needed

    // --- Generate Structs/Enums defined in the SsotAst ---
    let mut defined_types_code = String::new();
    for def in &ast.definitions {
        match def {
            TypeDefinition::Struct(struct_def) => {
                defined_types_code.push_str(&generate_capnp_comment(&struct_def.annotations, ""));
                defined_types_code.push_str(&format!("struct {} @{} {{\n", struct_def.name.name, type_id_counter));
                type_id_counter += 1;
                let mut field_ordinal = 0u16;
                for field in &struct_def.fields {
                     defined_types_code.push_str(&generate_capnp_comment(&field.annotations, "  "));
                     let field_capnp_type = map_type_specifier_to_capnp_type(&field.type_spec);
                     defined_types_code.push_str(&format!(
                        "  {} @{} :{};\n",
                        field.name.name,
                        field_ordinal, // Assign sequential ordinals within the struct
                        field_capnp_type
                    ));
                    field_ordinal += 1;
                }
                defined_types_code.push_str("}\n\n");
            }
            TypeDefinition::Enum(enum_def) => {
                 defined_types_code.push_str(&generate_capnp_comment(&enum_def.annotations, ""));
                defined_types_code.push_str(&format!("enum {} @{} {{\n", enum_def.name.name, type_id_counter));
                type_id_counter += 1;
                let mut variant_ordinal = 0u16;
                 for variant in &enum_def.variants {
                     defined_types_code.push_str(&generate_capnp_comment(&variant.annotations, "  "));
                     // TODO: Handle potential associated types/payloads for enum variants if DSL supports it
                     defined_types_code.push_str(&format!(
                        "  {} @{};\n",
                        variant.name.name,
                        variant_ordinal // Assign sequential ordinals within the enum
                    ));
                    variant_ordinal += 1;
                }
                defined_types_code.push_str("}\n\n");
            }
            TypeDefinition::Machine(_) => { /* Handled separately */ }
        }
    }
    capnp_code.push_str(&defined_types_code);

    // --- State Enum ---
    let states_block = machine_ast.states.as_ref()
        .ok_or_else(|| CodegenError::AstValidationError("Machine definition requires a 'states' block".to_string()))?;
    let state_enum_name = format!("{}State", machine_ast.name.name); // e.g., TrafficLightState
    capnp_code.push_str(&generate_capnp_comment(&states_block.annotations, ""));
    capnp_code.push_str(&format!("enum {} @{} {{\n", state_enum_name, type_id_counter));
    let state_enum_id = type_id_counter;
    type_id_counter += 1;
    let mut state_ordinal = 0u16;
    for state in &states_block.states {
        capnp_code.push_str(&generate_capnp_comment(&state.annotations, "  "));
        capnp_code.push_str(&format!("  {} @{};\n", state.name.name, state_ordinal));
        state_ordinal += 1;
    }
    capnp_code.push_str("}\n\n");

    // --- Event Payloads (Structs) ---
    // Event payloads should ideally be defined as separate structs in the SsotAst
    // and referenced by the event definition. We assume this convention.

    // --- Event Union ---
    let events_block = machine_ast.events.as_ref()
        .ok_or_else(|| CodegenError::AstValidationError("Machine definition requires an 'events' block".to_string()))?;
    let event_union_name = format!("{}Event", machine_ast.name.name); // e.g., TrafficLightEvent
    capnp_code.push_str(&generate_capnp_comment(&events_block.annotations, ""));
    capnp_code.push_str(&format!("union {} @{} {{\n", event_union_name, type_id_counter));
    let event_union_id = type_id_counter;
    type_id_counter += 1;
    let mut event_ordinal = 0u16;
    for event_def in &events_block.events {
         capnp_code.push_str(&generate_capnp_comment(&event_def.annotations, "  "));
         match &event_def.payload {
             Some(payload_type_spec) => {
                let payload_capnp_type = map_type_specifier_to_capnp_type(payload_type_spec);
                // Assume payload_capnp_type is a struct defined elsewhere in the AST
                capnp_code.push_str(&format!(
                    "  {} @{} :{};\n",
                    event_def.name.name,
                    event_ordinal,
                    payload_capnp_type
                ));
            }
            None => {
                 capnp_code.push_str(&format!(
                    "  {} @{} :Void;\n",
                    event_def.name.name,
                    event_ordinal
                ));
            }
        }
        event_ordinal += 1;
    }
    capnp_code.push_str("}\n\n");

    // --- Context Struct ---
    let context_struct_name = format!("{}Context", machine_ast.name.name); // e.g., TrafficLightContext
    if let Some(context_block) = &machine_ast.context {
        capnp_code.push_str(&generate_capnp_comment(&context_block.annotations, ""));
        capnp_code.push_str(&format!("struct {} @{} {{\n", context_struct_name, type_id_counter));
        let context_struct_id = type_id_counter;
        type_id_counter += 1;
        let mut context_field_ordinal = 0u16;
        for field in &context_block.fields {
            capnp_code.push_str(&generate_capnp_comment(&field.annotations, "  "));
            let field_capnp_type = map_type_specifier_to_capnp_type(&field.type_spec);
            capnp_code.push_str(&format!(
                "  {} @{} :{};\n",
                field.name.name,
                context_field_ordinal,
                field_capnp_type
            ));
            context_field_ordinal += 1;
        }
        capnp_code.push_str("}\n\n");

        // --- Machine State Struct ---
        let machine_state_struct_name = format!("{}MachineState", machine_ast.name.name);
        capnp_code.push_str(&format!("# Represents the current state and context of the machine.\n"));
        capnp_code.push_str(&format!("struct {} @{} {{\n", machine_state_struct_name, type_id_counter));
        type_id_counter += 1;
        capnp_code.push_str(&format!("  currentState @0 :{};\n", state_enum_name));
        capnp_code.push_str(&format!("  context @1 :{};\n", context_struct_name));
        capnp_code.push_str("}\n\n");

    } else {
        // If no context, maybe just define the State enum? Or a simpler MachineState?
        // For now, we assume context is usually present for a meaningful machine state.
         capnp_code.push_str(&format!("# Machine has no context defined.\n"));
         // Define a simple state wrapper
         let machine_state_struct_name = format!("{}MachineState", machine_ast.name.name);
         capnp_code.push_str(&format!("# Represents the current state of the machine.\n"));
         capnp_code.push_str(&format!("struct {} @{} {{\n", machine_state_struct_name, type_id_counter));
         type_id_counter += 1;
         capnp_code.push_str(&format!("  currentState @0 :{};\n", state_enum_name));
         capnp_code.push_str("}\n\n");
    }


    // TODO:
    // - Assign unique @ IDs robustly.
    // - Validate Identifier references against defined types.
    // - Add support for `using` declarations.
    // - Consider casing conventions.

    Ok(capnp_code)
}

// Helper to find annotation value by name
// Moved the import inside the function where it's needed, or keep it global if used elsewhere
fn find_annotation_value<'a>(
    annotations: &'a [Annotation],
    name: &str,
) -> Option<&'a AnnotationValue> {
    annotations
        .iter()
        .find(|a| a.name.name == name) // Compare with Identifier's name field
        .and_then(|a| a.value.as_ref())
}
