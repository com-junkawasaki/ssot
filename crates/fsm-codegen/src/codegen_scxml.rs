//! SCXML document generation logic.

use crate::{find_annotation_value, CodegenError};
use fsm_dsl::ast::{AnnotationValue, FieldDef, FieldType, StateMachine, TransitionElement};
use std::fmt::Write;

// Helper to map DSL FieldType to SCXML data type string (approximations)
fn map_field_type_to_scxml_type(field_type: &FieldType) -> &'static str {
    match field_type {
        FieldType::Void => "", // No direct equivalent, maybe omit or use custom type
        FieldType::Bool => "boolean",
        FieldType::Int8
        | FieldType::Int16
        | FieldType::Int32
        | FieldType::Int64
        | FieldType::UInt8
        | FieldType::UInt16
        | FieldType::UInt32
        | FieldType::UInt64 => "integer", // SCXML has limited numeric types
        FieldType::Float32 | FieldType::Float64 => "float",
        FieldType::Text => "string",
        FieldType::Data => "string",    // Base64 encode? Or custom type?
        FieldType::List(_) => "",       // SCXML datamodel is flat, maybe JSON string?
        FieldType::Identifier(_) => "", // Custom struct, maybe JSON string?
    }
}

// Helper to map DSL FieldType to initial value expression for SCXML expr attribute
fn map_field_type_to_scxml_initial_expr(field_type: &FieldType) -> String {
    match field_type {
        FieldType::Void => "null".to_string(), // Use null for void
        FieldType::Bool => "false".to_string(),
        FieldType::Int8
        | FieldType::Int16
        | FieldType::Int32
        | FieldType::Int64 // SCXML doesn't distinguish sizes well
        | FieldType::UInt8
        | FieldType::UInt16
        | FieldType::UInt32
        | FieldType::UInt64 => "0".to_string(),
        FieldType::Float32 | FieldType::Float64 => "0.0".to_string(),
        FieldType::Text => "''".to_string(), // Empty string literal
        FieldType::Data => "null".to_string(), // Represent Data as null initially?
        FieldType::List(_) => "[]".to_string(), // Represent List as empty array literal (JSON-like)
        FieldType::Identifier(_) => "null".to_string(), // Represent custom structs as null initially?
    }
}

// Helper function to generate XML comments from annotations
fn generate_xml_comment(annotations: &[fsm_dsl::ast::Annotation], indent: &str) -> String {
    let mut comment = String::new();
    if let Some(AnnotationValue::StringLiteral(desc)) =
        find_annotation_value(annotations, "description")
    {
        // Simple comment formatting
        write!(
            comment,
            "{}<!-- {} -->\n",
            indent,
            desc.lines()
                .map(|l| l.trim())
                .filter(|l| !l.is_empty())
                .collect::<Vec<_>>()
                .join(" ") // Join lines into a single comment line
        )
        .unwrap(); // Use unwrap for simplicity in helper
    }
    comment
}

// Helper function to generate executable content for an action
fn generate_scxml_action_content(
    action_ident: &fsm_dsl::ast::Ident,
    context_fields: &[FieldDef],
    indent: &str,
) -> String {
    let action_name = action_ident.to_string();
    let mut content = String::new();

    // Very basic heuristic: Check if action name suggests assignment/increment/decrement
    let assign_prefix = "assign";
    let increment_prefix = "increment";
    let decrement_prefix = "decrement";

    let mut assigned = false;
    if action_name.starts_with(assign_prefix) {
        let field_name = &action_name[assign_prefix.len()..];
        if context_fields.iter().any(|f| f.name == field_name) {
            // Assume assigning a default/null value for now, DSL needs more info for actual value
            write!(
                content,
                "{indent}<assign location=\"{}\" expr=\"null\" />",
                field_name
            )
            .unwrap();
            assigned = true;
        }
    } else if action_name.starts_with(increment_prefix) {
        let field_name_maybe_camel = &action_name[increment_prefix.len()..];
        // Attempt to find matching field (case-insensitive?)
        if let Some(field) = context_fields.iter().find(|f| {
            f.name
                .to_string()
                .eq_ignore_ascii_case(field_name_maybe_camel)
        }) {
            // Check if the type is likely numeric before generating increment
            match field.field_type {
                FieldType::Int8
                | FieldType::Int16
                | FieldType::Int32
                | FieldType::Int64
                | FieldType::UInt8
                | FieldType::UInt16
                | FieldType::UInt32
                | FieldType::UInt64 => {
                    write!(
                        content,
                        "{indent}<assign location=\"{}\" expr=\"{}. + 1\" />",
                        field.name, field.name
                    )
                    .unwrap();
                    assigned = true;
                }
                _ => { /* Type mismatch, fall back to log */ }
            }
        }
    } else if action_name.starts_with(decrement_prefix) {
        let field_name_maybe_camel = &action_name[decrement_prefix.len()..];
        if let Some(field) = context_fields.iter().find(|f| {
            f.name
                .to_string()
                .eq_ignore_ascii_case(field_name_maybe_camel)
        }) {
            match field.field_type {
                FieldType::Int8
                | FieldType::Int16
                | FieldType::Int32
                | FieldType::Int64
                | FieldType::UInt8
                | FieldType::UInt16
                | FieldType::UInt32
                | FieldType::UInt64 => {
                    write!(
                        content,
                        "{indent}<assign location=\"{}\" expr=\"{}. - 1\" />",
                        field.name, field.name
                    )
                    .unwrap();
                    assigned = true;
                }
                _ => { /* Type mismatch, fall back to log */ }
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

pub(crate) fn generate_scxml_internal(ast: &StateMachine) -> Result<String, CodegenError> {
    let mut output = String::new();
    let indent = "  ";

    // --- SCXML Header ---
    writeln!(output, "<?xml version=\"1.0\" encoding=\"UTF-8\"?>")?;
    let initial_state_name = find_annotation_value(&ast.annotations, "initial")
        .and_then(|v| match v {
            AnnotationValue::Identifier(ident) => Some(ident.to_string()),
            _ => None,
        })
        .ok_or_else(|| {
            CodegenError::AstValidationError(
                "Missing or invalid '$initial(StateName)' annotation on stateMachine.".to_string(),
            )
        })?;
    writeln!(output,
        "<scxml version=\"1.0\" xmlns=\"http://www.w3.org/2005/07/scxml\" initial=\"{}\" name=\"{}\" datamodel=\"ecmascript\">", // Added datamodel="ecmascript"
        initial_state_name,
        ast.name
    )?;

    // --- Datamodel ---
    if !ast.context.is_empty() {
        writeln!(output, "{indent}<datamodel>")?;
        for field in &ast.context {
            let initial_expr = map_field_type_to_scxml_initial_expr(&field.field_type);
            write!(
                output,
                "{}",
                generate_xml_comment(&field.annotations, &indent.repeat(2))
            )?;
            // Add expr attribute for initial value
            writeln!(
                output,
                "{indent}{indent}<data id=\"{}\" expr=\"{}\" />",
                field.name, initial_expr
            )?;
        }
        writeln!(output, "{indent}</datamodel>")?;
    }

    // --- States ---
    for state in &ast.states {
        write!(
            output,
            "{}",
            generate_xml_comment(&state.annotations, &indent.repeat(1))
        )?;
        writeln!(output, "{indent}<state id=\"{}\">", state.name)?;

        // Initial state within compound states (if applicable later)

        // Entry Actions
        if !state.entry_actions.is_empty() {
            writeln!(output, "{indent}{indent}<onentry>")?;
            for action_ident in &state.entry_actions {
                let action_content =
                    generate_scxml_action_content(action_ident, &ast.context, &indent.repeat(3));
                writeln!(output, "{}", action_content)?; // Write the generated <assign> or <log>
            }
            writeln!(output, "{indent}{indent}</onentry>")?;
        }

        // Exit Actions
        if !state.exit_actions.is_empty() {
            writeln!(output, "{indent}{indent}<onexit>")?;
            for action_ident in &state.exit_actions {
                let action_content =
                    generate_scxml_action_content(action_ident, &ast.context, &indent.repeat(3));
                writeln!(output, "{}", action_content)?; // Write the generated <assign> or <log>
            }
            writeln!(output, "{indent}{indent}</onexit>")?;
        }

        // Transitions originating from this state
        for transition in ast.transitions.iter().filter(|t| t.from == state.name) {
            // Find the 'On' element for the event
            let event_name = transition.elements.iter().find_map(|el| match el {
                TransitionElement::On { event, .. } => Some(event.to_string()),
                _ => None,
            });

            if let Some(event) = event_name {
                // Find Guard (cond)
                let guard_cond = transition.elements.iter().find_map(|el| match el {
                    TransitionElement::Guard { function, .. } => Some(function.to_string()),
                    _ => None,
                });

                // Find Actions (executable content)
                let actions: Vec<_> = transition
                    .elements
                    .iter()
                    .filter_map(|el| match el {
                        TransitionElement::Action { function, .. } => Some(function.clone()),
                        _ => None,
                    })
                    .collect();

                write!(
                    output,
                    "{}",
                    generate_xml_comment(&transition.annotations, &indent.repeat(2))
                )?;
                write!(
                    output,
                    "{indent}{indent}<transition event=\"{}\" target=\"{}\"",
                    event, transition.to
                )?;
                if let Some(cond) = guard_cond {
                    // Assume guard function name directly maps to a condition expression for now
                    write!(output, " cond=\"guard_{}()\"", cond)?; // Wrap in placeholder function call
                }
                writeln!(output, ">")?;

                // Add actions as executable content within <script>
                for action_ident in actions {
                    let action_content = generate_scxml_action_content(
                        &action_ident,
                        &ast.context,
                        &indent.repeat(3),
                    );
                    writeln!(output, "{}", action_content)?; // Write the generated <assign> or <log>
                }

                writeln!(output, "{indent}{indent}</transition>")?;
            }
        }

        writeln!(output, "{indent}</state>")?;
    }

    // --- SCXML Footer ---
    writeln!(output, "</scxml>")?;

    Ok(output)
}
