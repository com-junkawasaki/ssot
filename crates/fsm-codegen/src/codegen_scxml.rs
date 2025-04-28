//! SCXML document generation logic.

// use crate::get_simple_ident; // Maybe remove if QualifiedIdent is gone
use crate::{find_annotation_value, CodegenError};
use fsm_dsl::ast::{
    Annotation, AnnotationValue, Argument, ContextDefinition, ContextFieldDefinition, Identifier,
    MachineDefinition, NumericId, SsotAst, StateDefinition, StatesBlock, TopLevelDefinition,
    TransitionDefinition, TransitionTarget, TypeSpecifier,
};
use std::fmt::Write;

// Helper to map DSL TypeSpecifier to SCXML data type string (approximations)
// TODO: Review SCXML data types and mapping accuracy. Maybe JSON is better?
fn map_type_specifier_to_scxml_type(type_spec: &TypeSpecifier) -> &str {
    match type_spec {
        TypeSpecifier::Simple(ident) => match ident.name.as_str() {
            "bool" => "boolean",
            "int" | "i8" | "i16" | "i32" | "i64" | "u8" | "u16" | "u32" | "u64" => "integer",
            "f32" | "f64" => "float",
            "string" | "text" => "string",
            "data" => "string", // SCXML doesn't have native binary data type
            "void" => "string", // No void type
            _ => "string", // Assume custom types are represented as strings (e.g., JSON)
        },
        TypeSpecifier::List(_) => "string", // Represent lists as JSON strings?
        TypeSpecifier::Optional(_) => "string", // Represent optionals as JSON strings or rely on expr?
        TypeSpecifier::Map(_, _) => "string", // Represent maps as JSON strings?
    }
}

// Helper to map DSL TypeSpecifier to initial value expression for SCXML expr attribute
// TODO: Review SCXML expr syntax and initial values. JSON is likely more robust.
fn map_type_specifier_to_scxml_initial_expr(type_spec: &TypeSpecifier) -> String {
    match type_spec {
        TypeSpecifier::Simple(ident) => match ident.name.as_str() {
            "bool" => "false".to_string(),
            "int" | "i8" | "i16" | "i32" | "i64" | "u8" | "u16" | "u32" | "u64" => "0".to_string(),
            "f32" | "f64" => "0.0".to_string(),
            "string" | "text" => "''".to_string(), // Empty string literal
            "data" => "null".to_string(), // Represent Data as null initially?
            "void" => "null".to_string(), // Use null for void
            _ => "null".to_string(), // Assume custom types are null initially
        },
        TypeSpecifier::List(_) => "'[]'".to_string(), // Represent List as empty array string literal
        TypeSpecifier::Optional(_) => "null".to_string(), // Optionals start as null
        TypeSpecifier::Map(_, _) => "'{}'".to_string(), // Represent Map as empty object string literal
    }
}

// Helper function to generate XML comments from annotations
fn generate_xml_comment(annotations: &[Annotation], indent: &str) -> String {
    // Find $description annotation
    let description = annotations.iter().find_map(|anno| match anno {
        Annotation::Description(desc) => Some(desc),
        _ => None,
    });

    if let Some(desc) = description {
        // Basic XML escaping for comment content (more robust escaping might be needed)
        let escaped_desc = desc.replace("--", "- -").replace(">", "&gt;").replace("<", "&lt;");
        format!("{}<!-- {} -->", indent, escaped_desc)
    } else {
        String::new()
    }
}

// Helper function to generate executable content for an action
// Updated to use ContextFieldDefinition
fn generate_scxml_action_content(
    action_ident: &Identifier,
    context_fields: &[ContextFieldDefinition],
    indent: &str,
) -> String {
    let action_name = &action_ident.name;
    let mut content = String::new();

    // Basic heuristic: Check if action name suggests assignment/increment/decrement
    // TODO: This is very primitive. A better approach would be needed for complex actions.
    let assign_prefix = "assign";
    let increment_prefix = "increment";
    let decrement_prefix = "decrement";

    let mut assigned = false;
    if let Some(suffix) = action_name.strip_prefix(assign_prefix) {
         // Try to find a field whose name matches the suffix (case-insensitive)
         if let Some(field) = context_fields.iter().find(|f| f.name.name.eq_ignore_ascii_case(suffix.trim_start_matches('_'))) {
             write!(
                 content,
                 "{indent}<assign location=\"{{}}\" expr=\"null\" />", // Placeholder value
                 field.name.name,
             ).unwrap();
             assigned = true;
         }
    } else if let Some(suffix) = action_name.strip_prefix(increment_prefix) {
         if let Some(field) = context_fields.iter().find(|f| f.name.name.eq_ignore_ascii_case(suffix.trim_start_matches('_'))) {
            // Check if the type is likely numeric before generating increment
            if let TypeSpecifier::Simple(type_ident) = &field.type_spec {
                 match type_ident.name.as_str() {
                    "int" | "i8" | "i16" | "i32" | "i64" | "u8" | "u16" | "u32" | "u64" | "f32" | "f64" => {
                        write!(
                            content,
                            "{indent}<assign location=\"{{}}\" expr=\"{{}}. + 1\" />",
                            field.name.name, field.name.name,
                        ).unwrap();
                        assigned = true;
                    }
                    _ => { /* Type mismatch, fall back to log */ }
                }
            }
         }
    } else if let Some(suffix) = action_name.strip_prefix(decrement_prefix) {
         if let Some(field) = context_fields.iter().find(|f| f.name.name.eq_ignore_ascii_case(suffix.trim_start_matches('_'))) {
             if let TypeSpecifier::Simple(type_ident) = &field.type_spec {
                 match type_ident.name.as_str() {
                    "int" | "i8" | "i16" | "i32" | "i64" | "u8" | "u16" | "u32" | "u64" | "f32" | "f64" => {
                        write!(
                             content,
                            "{indent}<assign location=\"{{}}\" expr=\"{{}}. - 1\" />",
                            field.name.name, field.name.name,
                        ).unwrap();
                        assigned = true;
                    }
                    _ => { /* Type mismatch, fall back to log */ }
                 }
             }
         }
    }

    // Fallback or default action is logging
    if !assigned {
        write!(
            content,
            "{indent}<log expr=\"'Action: {}()'\" />",
            action_name
        )
        .unwrap();
    }

    content
}

// Updated internal generation function signature and logic
pub(crate) fn generate_scxml_internal(ast: &SsotAst) -> Result<String, CodegenError> {
    // Find the machine definition within the AST
    let machine_ast = ast
        .definitions
        .iter()
        .find_map(|def| {
            if let TopLevelDefinition::Machines(m_block) = def {
                // Assuming only one machine per file for now
                m_block.definitions.first()
            } else {
                None
            }
        })
        .ok_or_else(|| {
            CodegenError::AstValidationError("No machine definition found in AST".to_string())
        })?;

    let mut output = String::new();
    let indent = "  ";

    // --- SCXML Header ---
    writeln!(output, "<?xml version=\"1.0\" encoding=\"UTF-8\"?>")?;
    // Find initial state from annotation
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

    writeln!(
        output,
        "<scxml version=\"1.0\" xmlns=\"http://www.w3.org/2005/07/scxml\" initial=\"{{}}\" name=\"{{}}\" datamodel=\"ecmascript\">",
        initial_state_name, machine_ast.name.name,
    )?;

    // --- Datamodel ---
    let context_fields = machine_ast.context.as_ref().map_or(Vec::new(), |c| c.fields.clone()); // Clone fields if context exists
    if !context_fields.is_empty() {
        writeln!(output, "{indent}<datamodel>")?;
        for field in &context_fields {
            let initial_expr = map_type_specifier_to_scxml_initial_expr(&field.type_spec);
            write!(
                output,
                "{}",
                generate_xml_comment(&field.annotations, &indent.repeat(2)) // Indent comment
            )?;
            // Add expr attribute for initial value
            writeln!(
                output,
                "{indent}{indent}<data id=\"{{}}\" expr=\"{{}}\" />", // Use expr
                field.name.name, initial_expr,
            )?;
        }
        writeln!(output, "{indent}</datamodel>")?;
    }

    // --- States ---
    if let Some(states_block) = &machine_ast.states {
         write!(
             output,
             "{}",
             generate_xml_comment(&states_block.annotations, indent) // Comment for the block
         )?;
        for state in &states_block.states {
            write!(
                output,
                "{}",
                generate_xml_comment(&state.annotations, indent) // Comment for the state
            )?;
            writeln!(output, "{indent}<state id=\"{{}}\">", state.name.name)?;

            // TODO: Initial state within compound states (if applicable later)

            // Entry Actions
            if !state.on_entry.is_empty() {
                writeln!(output, "{indent}{indent}<onentry>")?;
                for action_ident in &state.on_entry {
                    let action_content =
                        generate_scxml_action_content(action_ident, &context_fields, &indent.repeat(3));
                    writeln!(output, "{}", action_content)?;
                }
                writeln!(output, "{indent}{indent}</onentry>")?;
            }

            // Exit Actions
            if !state.on_exit.is_empty() {
                writeln!(output, "{indent}{indent}<onexit>")?;
                 for action_ident in &state.on_exit {
                    let action_content =
                        generate_scxml_action_content(action_ident, &context_fields, &indent.repeat(3));
                    writeln!(output, "{}", action_content)?;
                }
                writeln!(output, "{indent}{indent}</onexit>")?;
            }

            // Transitions originating from this state
            for transition in &state.transitions {
                let event_name = &transition.event.name;

                // Determine target state name (handle non-state targets appropriately)
                let target_name = match &transition.target {
                     TransitionTarget::State(ident) => Some(ident.name.as_str()),
                     // SCXML doesn't directly support history targets in basic transitions
                     _ => None, // Or potentially log a warning/error
                };

                // Find Guard (cond) - join multiple guards with ' && '
                 let guard_cond = if !transition.guards.is_empty() {
                     Some(transition.guards.iter().map(|g| format!("_cond.{}()", g.name)).collect::<Vec<_>>().join(" && "))
                 } else {
                     None
                 };


                 write!(output, "{indent}{indent}<transition event=\"{{}}\"", event_name)?;
                 if let Some(target) = target_name {
                     write!(output, " target=\"{{}}\"", target)?;
                 }
                 if let Some(cond) = &guard_cond {
                    // Basic escaping for condition expression
                    let escaped_cond = cond.replace('<', "&lt;").replace('&', "&amp;");
                    write!(output, " cond=\"{{}}\"", escaped_cond)?;
                 }
                 writeln!(output, ">")?;


                // Executable content (actions)
                if !transition.actions.is_empty() {
                    for action_ident in &transition.actions {
                         let action_content = generate_scxml_action_content(
                            action_ident,
                            &context_fields,
                            &indent.repeat(3), // Indent actions
                        );
                         writeln!(output, "{}", action_content)?;
                    }
                }

                writeln!(output, "{indent}{indent}</transition>")?;
            }

            // TODO: Handle nested states, parallel states, history states if needed

            writeln!(output, "{indent}</state>")?;
        }
    }

    // --- Closing Tag ---
    writeln!(output, "</scxml>")?;

    Ok(output)
}
