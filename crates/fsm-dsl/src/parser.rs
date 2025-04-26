use crate::ast::{
    self, Annotation, AnnotationValue, FieldDef, FieldType, Ident, ImportDeclaration, MessageItem,
    QualifiedIdent, SsotFile, StateItem, StateMachine, StructDef, TopLevelItem, TransitionElement,
    TransitionItem, UseDeclaration,
};
use pest::iterators::Pair;
use pest::Parser;
use pest_derive::Parser;
use std::fs;
use std::path::Path;
use thiserror::Error;

#[derive(Parser)]
#[grammar = "ssot.pest"]
pub struct SsotParser;

#[derive(Error, Debug)]
pub enum ParseError {
    #[error("Failed to read file: {0}")]
    FileReadError(#[from] std::io::Error),
    #[error("Pest parsing error: {0}")]
    PestError(#[from] Box<pest::error::Error<Rule>>),
    #[error("Invalid identifier: '{0}'")]
    InvalidIdentifier(String),
    #[error("Invalid number format: '{0}' - {1}")]
    InvalidNumber(String, String),
    #[error("Invalid file ID format: '{0}'")]
    InvalidFileId(String),
    #[error("Invalid type string: '{0}'")]
    InvalidTypeString(String),
    #[error("Missing required element: {0}")]
    MissingElement(String),
    #[error("Duplicate definition: {0}")]
    DuplicateDefinition(String),
    #[error("Internal parser error: Unexpected rule '{rule:?}' encountered in '{context}'")]
    UnexpectedRule { rule: Rule, context: String },
    #[error("Semantic error: {0}")]
    SemanticError(String),
}

pub fn parse_file<P: AsRef<Path>>(path: P) -> Result<SsotFile, ParseError> {
    let content = fs::read_to_string(path)?;
    parse_str(&content)
}

pub fn parse_str(input: &str) -> Result<SsotFile, ParseError> {
    let mut pairs = SsotParser::parse(Rule::ssot_file, input)
        .map_err(|e| ParseError::PestError(Box::new(e)))?;

    // Get the single top-level ssot_file pair
    let ssot_file_pair = pairs.next().ok_or_else(|| {
        ParseError::SemanticError("Parser did not return a top-level ssot_file rule".to_string())
    })?;
    if ssot_file_pair.as_rule() != Rule::ssot_file {
        return Err(ParseError::SemanticError(format!(
            "Expected ssot_file rule, got {:?}",
            ssot_file_pair.as_rule()
        )));
    }

    let mut file_id: Option<u64> = None;
    let mut package_declaration: Option<String> = None;
    let mut imports: Vec<ImportDeclaration> = Vec::new();
    let mut items: Vec<TopLevelItem> = Vec::new();

    // Iterate over the *inner* pairs of the ssot_file rule
    for pair in ssot_file_pair.into_inner() {
        match pair.as_rule() {
            Rule::file_id => {
                file_id = Some(parse_hex_literal(pair.into_inner().next().unwrap())?);
            }
            Rule::package_decl => {
                package_declaration = Some(parse_package_name(pair.into_inner().next().unwrap())?);
            }
            Rule::import_decl => {
                let package_name = parse_package_name(pair.into_inner().next().unwrap())?;
                imports.push(ImportDeclaration { package_name });
            }
            Rule::top_level_item => {
                let inner_item = pair.into_inner().next().ok_or_else(|| {
                    ParseError::SemanticError("Empty top_level_item encountered".to_string())
                })?;
                match inner_item.as_rule() {
                    Rule::annotation => {
                        items.push(TopLevelItem::Annotation(parse_annotation(inner_item)?));
                    }
                    Rule::state_machine => {
                        items.push(TopLevelItem::StateMachine(parse_state_machine(inner_item)?));
                    }
                    Rule::struct_def => {
                        items.push(TopLevelItem::StructDefinition(parse_struct_def(
                            inner_item,
                        )?));
                    }
                    _ => {
                        return Err(ParseError::UnexpectedRule {
                            rule: inner_item.as_rule(),
                            context: "inside top_level_item".to_string(),
                        });
                    }
                }
            }
            Rule::WHITESPACE | Rule::COMMENT => { /* ignore */ }
            Rule::EOI => break, // EOI marks the end of the inner pairs for ssot_file
            _ => {
                return Err(ParseError::UnexpectedRule {
                    rule: pair.as_rule(),
                    context: "ssot_file inner elements".to_string(),
                });
            }
        }
    }

    // Check for required file_id after parsing all inner elements
    if file_id.is_none() {
        return Err(ParseError::MissingElement(
            "file ID (@0x...;) required at top level".to_string(),
        ));
    }

    Ok(SsotFile {
        file_id: file_id.unwrap(), // Safe now after check
        package_declaration,
        imports,
        items,
    })
}

fn parse_annotation(pair: Pair<Rule>) -> Result<Annotation, ParseError> {
    let mut inner = pair.into_inner();
    let name = parse_ident(inner.next().unwrap())?;

    // Check if the optional annotation_value is present
    let value = if let Some(value_pair) = inner.peek() {
        // If the next pair is annotation_value, parse it
        if value_pair.as_rule() == Rule::annotation_value {
            // Consume the pair and parse it
            let parsed_value = parse_annotation_value(inner.next().unwrap())?;
            Some(parsed_value)
        } else {
            // Next pair is not annotation_value (likely the semicolon), so no value
            None
        }
    } else {
        // No more pairs after identifier, so no value
        None
    };

    // We don't need to consume the semicolon explicitly as it's part of the grammar rule
    // and handled by Pest moving to the next item.

    Ok(Annotation { name, value })
}

fn parse_annotation_value(pair: Pair<Rule>) -> Result<AnnotationValue, ParseError> {
    // pair matches annotation_value rule: "(" ~ annotation_value_inner? ~ ")"
    if let Some(annotation_value_inner_pair) = pair.into_inner().next() {
        // Check the rule of the content INSIDE the parentheses
        let actual_content_pair = annotation_value_inner_pair.into_inner().next().unwrap();
        match actual_content_pair.as_rule() {
            Rule::ident => Ok(AnnotationValue::Identifier(parse_ident(
                actual_content_pair,
            )?)),
            Rule::array_literal => {
                let strings = actual_content_pair
                    .into_inner()
                    .map(|p| unescape_string(p.as_str()))
                    .collect();
                Ok(AnnotationValue::ArrayLiteral(strings))
            }
            Rule::literal => {
                if let Some(specific_literal_pair) = actual_content_pair.into_inner().next() {
                    match specific_literal_pair.as_rule() {
                        Rule::string_literal => Ok(AnnotationValue::StringLiteral(
                            unescape_string(specific_literal_pair.as_str()),
                        )),
                        Rule::boolean_literal => Ok(AnnotationValue::BooleanLiteral(
                            specific_literal_pair.as_str() == "true",
                        )),
                        Rule::number_literal => {
                            let num_str = specific_literal_pair.as_str();
                            let num = num_str.parse::<i64>().map_err(|e| {
                                ParseError::InvalidNumber(num_str.to_string(), e.to_string())
                            })?;
                            Ok(AnnotationValue::NumberLiteral(num))
                        }
                        _ => Err(ParseError::UnexpectedRule {
                            rule: specific_literal_pair.as_rule(),
                            context: "specific literal value inside literal".to_string(),
                        }),
                    }
                } else {
                    Err(ParseError::MissingElement(
                        "specific literal content inside literal rule".to_string(),
                    ))
                }
            }
            Rule::ident_list => {
                let idents = actual_content_pair
                    .into_inner()
                    .filter(|p| p.as_rule() == Rule::ident)
                    .map(parse_ident)
                    .collect::<Result<Vec<Ident>, _>>()?;
                Ok(AnnotationValue::IdentifierList(idents))
            }
            _ => Err(ParseError::UnexpectedRule {
                rule: actual_content_pair.as_rule(),
                context: "annotation value content (expected ident, array, literal, or ident_list)"
                    .to_string(),
            }),
        }
    } else {
        // No content inside parentheses - this might be valid for some annotations?
        // For now, let's assume it's an error or needs specific handling if needed.
        // If annotations like `@foo()` without value are allowed, return None or a specific variant.
        Err(ParseError::MissingElement(
            "annotation value content (parentheses cannot be empty)".to_string(),
        ))
        // Or potentially: Ok(AnnotationValue::Empty) if you add such a variant to the enum
    }
}

fn parse_state_machine(pair: Pair<Rule>) -> Result<StateMachine, ParseError> {
    debug_assert_eq!(pair.as_rule(), Rule::state_machine);
    let mut inner = pair.into_inner();
    let name = parse_ident(inner.next().unwrap())?;

    let mut annotations = Vec::new();
    let mut use_declarations = Vec::new();
    let mut states = Vec::new();
    let mut events = Vec::new();
    let mut transitions = Vec::new();
    let mut context_fields = Vec::new();

    // Iterate over items inside the stateMachine block {}
    for machine_item_pair in inner {
        // This pair *is* the machine_item
        // Skip silent rules that might sneak in
        if machine_item_pair.as_rule() == Rule::WHITESPACE
            || machine_item_pair.as_rule() == Rule::COMMENT
        {
            continue;
        }

        // Expect machine_item rule here, then look inside it
        if machine_item_pair.as_rule() != Rule::machine_item {
            return Err(ParseError::UnexpectedRule {
                rule: machine_item_pair.as_rule(),
                context: format!("Expected machine_item inside stateMachine '{}'", name),
            });
        }

        // Get the actual specific item *inside* machine_item
        if let Some(specific_item_pair) = machine_item_pair.into_inner().next() {
            match specific_item_pair.as_rule() {
                // Match on the specific rule
                Rule::annotation => {
                    annotations.push(parse_annotation(specific_item_pair)?);
                }
                Rule::states_block => {
                    states = parse_states_block(specific_item_pair)?;
                }
                Rule::events_block => {
                    let (parsed_events, event_use_decls) = parse_events_block(specific_item_pair)?;
                    events = parsed_events;
                    use_declarations.extend(event_use_decls);
                }
                Rule::transitions_block => {
                    transitions = parse_transitions_block(specific_item_pair)?;
                }
                Rule::context_block => {
                    context_fields = parse_context_block(specific_item_pair)?;
                }
                Rule::use_decl => {
                    use_declarations.push(parse_use_declaration(specific_item_pair)?);
                }
                _ => {
                    return Err(ParseError::UnexpectedRule {
                        rule: specific_item_pair.as_rule(),
                        context: format!("inside stateMachine '{}' machine_item", name),
                    });
                }
            }
        } else {
            // This can happen if machine_item matched something empty, which shouldn't occur with the current grammar.
            return Err(ParseError::SemanticError(format!(
                "Empty machine_item found in stateMachine '{}'",
                name
            )));
        }
    }

    // Basic validation (e.g., ensure initial state is valid)
    // ... (validation logic can be added here or in a separate step) ...

    Ok(StateMachine {
        name,
        annotations,
        use_declarations,
        states,
        events,
        transitions,
        context: context_fields,
    })
}

fn parse_use_declaration(pair: Pair<Rule>) -> Result<UseDeclaration, ParseError> {
    debug_assert_eq!(pair.as_rule(), Rule::use_decl);
    let qident_pair = pair.into_inner().next().unwrap();
    if qident_pair.as_rule() != Rule::qualified_ident {
        return Err(ParseError::UnexpectedRule {
            rule: qident_pair.as_rule(),
            context: "parsing use_decl: expected qualified_ident".to_string(),
        });
    }
    let package_name = parse_qualified_ident(qident_pair)?;
    Ok(UseDeclaration {
        target: package_name,
    })
}

fn parse_states_block(pair: Pair<Rule>) -> Result<Vec<StateItem>, ParseError> {
    debug_assert_eq!(pair.as_rule(), Rule::states_block);
    pair.into_inner()
        .filter(|p| p.as_rule() == Rule::state_item)
        .map(parse_state_item)
        .collect()
}

fn parse_state_item(pair: Pair<Rule>) -> Result<ast::StateItem, ParseError> {
    debug_assert_eq!(pair.as_rule(), Rule::state_item);
    let inner = pair.into_inner();
    let mut annotations = Vec::new();
    let mut name: Option<Ident> = None;
    let mut ordinal: Option<u64> = None;
    let mut entry_actions: Vec<QualifiedIdent> = Vec::new();
    let mut exit_actions: Vec<QualifiedIdent> = Vec::new();

    for item_pair in inner {
        match item_pair.as_rule() {
            Rule::annotation => annotations.push(parse_annotation(item_pair)?),
            Rule::ident => name = Some(parse_ident(item_pair)?),
            Rule::state_ordinal => {
                ordinal = Some(parse_integer_literal(
                    item_pair.into_inner().next().unwrap(),
                )?)
            }
            Rule::state_body => {
                // Body is optional, find the list rule if it exists
                if let Some(list_pair) = item_pair
                    .into_inner()
                    .find(|p| p.as_rule() == Rule::state_element_list)
                {
                    // Iterate through elements in the list
                    for state_element_pair in list_pair.into_inner() {
                        match state_element_pair.as_rule() {
                            Rule::WHITESPACE | Rule::COMMENT => continue, // Skip silent rules within the list iteration
                            Rule::state_element => {
                                // Process the actual element nested inside state_element
                                if let Some(specific_element_pair) =
                                    state_element_pair.into_inner().next()
                                {
                                    match specific_element_pair.as_rule() {
                                        Rule::entry_action => {
                                            entry_actions.push(parse_qualified_ident(
                                                specific_element_pair.into_inner().next().unwrap(),
                                            )?);
                                        }
                                        Rule::exit_action => {
                                            exit_actions.push(parse_qualified_ident(
                                                specific_element_pair.into_inner().next().unwrap(),
                                            )?);
                                        }
                                        Rule::annotation => {
                                            annotations
                                                .push(parse_annotation(specific_element_pair)?);
                                        }
                                        _ => {
                                            return Err(ParseError::UnexpectedRule {
                                                rule: specific_element_pair.as_rule(),
                                                context: "inside state_element".to_string(),
                                            });
                                        }
                                    }
                                } else {
                                    return Err(ParseError::SemanticError(format!(
                                        "Empty state_element found in state '{}'",
                                        name.as_ref().map(|n| n.to_string()).unwrap_or_default()
                                    )));
                                }
                            }
                            // This case should ideally not be reached if grammar is correct
                            _ => {
                                return Err(ParseError::UnexpectedRule {
                                     rule: state_element_pair.as_rule(),
                                     context: "inside state_element_list (expected state_element or silent)".to_string(),
                                });
                            }
                        }
                    }
                }
                // If state_body existed but state_element_list wasn't found, it was empty {}. No action needed.
            }
            Rule::WHITESPACE | Rule::COMMENT => { /* ignore */ }
            _ => {
                return Err(ParseError::UnexpectedRule {
                    rule: item_pair.as_rule(),
                    context: "state_item definition".to_string(),
                });
            }
        }
    }

    Ok(ast::StateItem {
        annotations,
        name: name.ok_or_else(|| ParseError::MissingElement("state name".to_string()))?,
        ordinal: ordinal
            .ok_or_else(|| ParseError::MissingElement("state ordinal (@N)".to_string()))?,
        entry_actions,
        exit_actions,
    })
}

fn parse_events_block(
    pair: Pair<Rule>,
) -> Result<(Vec<MessageItem>, Vec<UseDeclaration>), ParseError> {
    debug_assert_eq!(pair.as_rule(), Rule::events_block);
    let mut events = Vec::new();
    let mut use_declarations = Vec::new();

    for event_item_pair in pair.into_inner() {
        // This pair *is* the event_item
        // Skip silent rules
        if event_item_pair.as_rule() == Rule::WHITESPACE
            || event_item_pair.as_rule() == Rule::COMMENT
        {
            continue;
        }

        // Expect event_item rule here, then look inside it
        if event_item_pair.as_rule() != Rule::event_item {
            return Err(ParseError::UnexpectedRule {
                rule: event_item_pair.as_rule(),
                context: "Expected event_item inside events_block".to_string(),
            });
        }

        // Get the actual specific item *inside* event_item
        if let Some(specific_item_pair) = event_item_pair.into_inner().next() {
            match specific_item_pair.as_rule() {
                Rule::annotation => {
                    // TODO: Decide how to handle annotations directly in events_block
                    // For now, maybe ignore or collect separately?
                    // Let's ignore for now, assuming annotations belong ON events.
                }
                Rule::event_struct => {
                    events.push(parse_event_struct(specific_item_pair)?);
                }
                Rule::use_decl => {
                    use_declarations.push(parse_use_declaration(specific_item_pair)?);
                }
                _ => {
                    return Err(ParseError::UnexpectedRule {
                        rule: specific_item_pair.as_rule(),
                        context:
                            "inside event_item (expected annotation, event_struct, or use_decl)"
                                .to_string(),
                    });
                }
            }
        } else {
            return Err(ParseError::SemanticError(
                "Empty event_item found in events_block".to_string(),
            ));
        }
    }
    Ok((events, use_declarations))
}

fn parse_event_struct(pair: Pair<Rule>) -> Result<MessageItem, ParseError> {
    debug_assert_eq!(pair.as_rule(), Rule::event_struct);
    let inner = pair.into_inner();
    let mut annotations = Vec::new(); // Annotations for the event itself
    let mut name: Option<Ident> = None;
    let mut ordinal: Option<u64> = None;
    let mut fields = Vec::new();

    for item_pair in inner {
        match item_pair.as_rule() {
            Rule::annotation => annotations.push(parse_annotation(item_pair)?),
            Rule::ident => name = Some(parse_ident(item_pair)?),
            Rule::event_ordinal => {
                ordinal = Some(parse_integer_literal(
                    item_pair.into_inner().next().unwrap(),
                )?)
            }
            Rule::event_body => {
                let mut current_field_annotations = Vec::new();
                // Body is optional, find the content rule if it exists
                if let Some(content_pair) = item_pair
                    .into_inner()
                    .find(|p| p.as_rule() == Rule::event_body_content)
                {
                    // Iterate through elements in the content list
                    for body_item_pair in content_pair.into_inner() {
                        match body_item_pair.as_rule() {
                            Rule::WHITESPACE | Rule::COMMENT => continue, // Skip silent rules
                            Rule::annotation => {
                                current_field_annotations.push(parse_annotation(body_item_pair)?);
                            }
                            Rule::event_field => {
                                // Annotation applies to this field
                                fields.push(parse_event_field(
                                    body_item_pair,
                                    current_field_annotations,
                                )?);
                                current_field_annotations = Vec::new(); // Reset for next field
                            }
                            // This case should ideally not be reached
                            _ => {
                                return Err(ParseError::UnexpectedRule {
                                    rule: body_item_pair.as_rule(),
                                    context: "inside event_body_content (expected annotation, event_field, or silent)".to_string(),
                                });
                            }
                        }
                    }
                }
                // If event_body existed but event_body_content wasn't found, it was empty {}.
                // Check for dangling annotations AFTER processing the optional content.
                if !current_field_annotations.is_empty() {
                    return Err(ParseError::SemanticError(
                        "Dangling annotations found at end of event body".to_string(),
                    ));
                }
            }
            Rule::WHITESPACE | Rule::COMMENT => { /* ignore */ }
            _ => {
                return Err(ParseError::UnexpectedRule {
                    rule: item_pair.as_rule(),
                    context: "event_struct definition".to_string(),
                });
            }
        }
    }

    Ok(MessageItem {
        annotations, // These are the event's annotations
        name: name.ok_or_else(|| ParseError::MissingElement("event name".to_string()))?,
        ordinal: ordinal
            .ok_or_else(|| ParseError::MissingElement("event ordinal (@N)".to_string()))?,
        fields, // Fields include their own annotations
    })
}

fn parse_event_field(
    pair: Pair<Rule>,
    annotations: Vec<Annotation>,
) -> Result<FieldDef, ParseError> {
    debug_assert_eq!(pair.as_rule(), Rule::event_field);
    let mut inner_event_field = pair.into_inner();

    // Consume identifier (mandatory)
    let name_pair = inner_event_field.next().ok_or_else(|| {
        ParseError::MissingElement("field name identifier in event_field".to_string())
    })?;
    let name = parse_ident(name_pair)?;

    // Consume the next part which is either field_with_ordinal or field_without_ordinal
    let field_details_pair = inner_event_field.next().ok_or_else(|| {
        ParseError::MissingElement("field details (with/without ordinal) after colon".to_string())
    })?;

    let (field_type, ordinal) = match field_details_pair.as_rule() {
        Rule::field_with_ordinal => {
            let mut inner_details = field_details_pair.into_inner();
            // First is field_type
            let type_pair = inner_details.next().ok_or_else(|| {
                ParseError::MissingElement("field_type inside field_with_ordinal".to_string())
            })?;
            let specific_type_pair = type_pair.into_inner().next().ok_or_else(|| {
                ParseError::MissingElement("Specific type inside field_type rule".to_string())
            })?;
            let parsed_field_type = parse_field_type(specific_type_pair)?;

            // Second is field_ordinal
            let ordinal_wrapper_pair = inner_details.next().ok_or_else(|| {
                ParseError::MissingElement("field_ordinal inside field_with_ordinal".to_string())
            })?;
            let ordinal_pair = ordinal_wrapper_pair.into_inner().next().unwrap(); // Get integer_literal
            let parsed_ordinal = Some(parse_integer_literal(ordinal_pair)?);

            (parsed_field_type, parsed_ordinal)
        }
        Rule::field_without_ordinal => {
            let mut inner_details = field_details_pair.into_inner();
            // Only contains field_type
            let type_pair = inner_details.next().ok_or_else(|| {
                ParseError::MissingElement("field_type inside field_without_ordinal".to_string())
            })?;
            let specific_type_pair = type_pair.into_inner().next().ok_or_else(|| {
                ParseError::MissingElement("Specific type inside field_type rule".to_string())
            })?;
            let parsed_field_type = parse_field_type(specific_type_pair)?;

            (parsed_field_type, None)
        }
        _ => {
            return Err(ParseError::UnexpectedRule {
                rule: field_details_pair.as_rule(),
                context: "Expected field_with_ordinal or field_without_ordinal".to_string(),
            });
        }
    };

    Ok(FieldDef {
        annotations,
        name,
        ordinal,
        field_type,
    })
}

fn parse_field_type(pair: Pair<Rule>) -> Result<FieldType, ParseError> {
    // Pair should be primitive_type, list_type, or type_identifier
    // debug_assert_eq!(pair.as_rule(), Rule::field_type); // No longer true
    // let specific_type_pair = pair.into_inner().next()... // No longer needed

    match pair.as_rule() {
        Rule::primitive_type => match pair.as_str() {
            // primitive_type now has @, so use pair.as_str()
            "Void" => Ok(FieldType::Void),
            "Bool" => Ok(FieldType::Bool),
            "Int8" => Ok(FieldType::Int8),
            "Int16" => Ok(FieldType::Int16),
            "Int32" => Ok(FieldType::Int32),
            "Int64" => Ok(FieldType::Int64),
            "UInt8" => Ok(FieldType::UInt8),
            "UInt16" => Ok(FieldType::UInt16),
            "UInt32" => Ok(FieldType::UInt32),
            "UInt64" => Ok(FieldType::UInt64),
            "Float32" => Ok(FieldType::Float32),
            "Float64" => Ok(FieldType::Float64),
            "Text" => Ok(FieldType::Text),
            "Data" => Ok(FieldType::Data),
            _ => Err(ParseError::InvalidTypeString(pair.as_str().to_string())), // Use pair directly
        },
        Rule::list_type => {
            // pair matches list_type = { "List" ~ "<" ~ field_type ~ ">" }
            let inner_field_type_pair = pair.into_inner().next().ok_or_else(|| {
                ParseError::MissingElement("field_type inside list_type".to_string())
            })?;
            // The inner pair IS field_type rule, need to get specific type inside it
            let specific_inner_type_pair =
                inner_field_type_pair.into_inner().next().ok_or_else(|| {
                    ParseError::MissingElement("Specific type inside nested field_type".to_string())
                })?;
            Ok(FieldType::List(Box::new(parse_field_type(
                specific_inner_type_pair,
            )?)))
        }
        Rule::type_identifier => {
            // pair matches type_identifier = { qualified_ident }
            let qident_pair = pair.into_inner().next().ok_or_else(|| {
                ParseError::MissingElement("qualified_ident inside type_identifier".to_string())
            })?;
            Ok(FieldType::Identifier(parse_qualified_ident(qident_pair)?))
        }
        _ => Err(ParseError::UnexpectedRule {
            rule: pair.as_rule(), // Report the rule received
            context: "parsing field_type inner rule (expected primitive, list, or identifier)"
                .to_string(),
        }),
    }
}

fn parse_struct_def(pair: Pair<Rule>) -> Result<StructDef, ParseError> {
    debug_assert_eq!(pair.as_rule(), Rule::struct_def);
    let inner = pair.into_inner();
    let mut annotations = Vec::new();
    let mut name: Option<Ident> = None;
    let mut fields = Vec::new();

    for item in inner {
        match item.as_rule() {
            Rule::annotation => {
                annotations.push(parse_annotation(item)?);
            }
            Rule::ident => {
                name = Some(parse_ident(item)?);
            }
            Rule::struct_body => {
                // Parse fields inside the body
                for field_pair in item.into_inner() {
                    if field_pair.as_rule() == Rule::struct_field {
                        // Unwrap the struct_field to get the actual event_field
                        let event_field_pair = field_pair.into_inner().next().unwrap();
                        if event_field_pair.as_rule() == Rule::event_field {
                            // Pass an empty Vec for annotations for struct fields
                            fields.push(parse_event_field(event_field_pair, Vec::new())?);
                        } else {
                            return Err(ParseError::UnexpectedRule {
                                rule: event_field_pair.as_rule(),
                                context: "inside struct_field".to_string(),
                            });
                        }
                    } else if field_pair.as_rule() != Rule::WHITESPACE
                        && field_pair.as_rule() != Rule::COMMENT
                    {
                        return Err(ParseError::UnexpectedRule {
                            rule: field_pair.as_rule(),
                            context: "inside struct_body".to_string(),
                        });
                    }
                }
            }
            Rule::WHITESPACE | Rule::COMMENT => { /* ignore */ }
            _ => {
                return Err(ParseError::UnexpectedRule {
                    rule: item.as_rule(),
                    context: "struct_def inner elements".to_string(),
                });
            }
        }
    }

    Ok(StructDef {
        annotations,
        name: name.ok_or_else(|| ParseError::MissingElement("struct name".to_string()))?,
        fields,
    })
}

fn parse_transitions_block(pair: Pair<Rule>) -> Result<Vec<TransitionItem>, ParseError> {
    debug_assert_eq!(pair.as_rule(), Rule::transitions_block);
    pair.into_inner()
        .filter(|p| p.as_rule() == Rule::transition_item)
        .map(parse_transition_item)
        .collect()
}

fn parse_transition_header(pair: Pair<Rule>) -> Result<(Option<Ident>, Ident, Ident), ParseError> {
    debug_assert_eq!(pair.as_rule(), Rule::transition_header);
    let mut inner = pair.into_inner(); // Gets pairs matching the sequence inside {}

    let mut transition_name: Option<Ident> = None;

    // Peek at the first element inside the header rule sequence
    let first_element = inner.peek().ok_or_else(|| {
        ParseError::MissingElement("Content inside transition_header".to_string())
    })?;

    // Check if the optional ("transition" ~ ident) part is present
    // The grammar implies if the first rule matched within the header is `ident`, it must be the name.
    if first_element.as_rule() == Rule::ident {
        // Consume and parse the identifier
        transition_name = Some(parse_ident(inner.next().unwrap())?);
    }
    // If the first element was not ident, then the optional part was skipped.

    // Next must be the "from" state identifier
    let from_ident_pair = inner.next().ok_or_else(|| {
        ParseError::MissingElement("'from' state in transition_header".to_string())
    })?;
    // Grammar guarantees 'from' keyword then 'ident'. Pest consumes 'from', inner.next() gives the ident.
    let from_state = parse_ident(from_ident_pair)?;

    // Next must be the "to" state identifier
    let to_ident_pair = inner
        .next()
        .ok_or_else(|| ParseError::MissingElement("'to' state in transition_header".to_string()))?;
    // Grammar guarantees 'to' keyword then 'ident'. Pest consumes 'to', inner.next() gives the ident.
    let to_state = parse_ident(to_ident_pair)?;

    Ok((transition_name, from_state, to_state))
}

fn parse_transition_item(pair: Pair<Rule>) -> Result<TransitionItem, ParseError> {
    debug_assert_eq!(pair.as_rule(), Rule::transition_item);
    let inner = pair.into_inner(); // annotation*, transition_header, transition_body, ";"
    let mut annotations = Vec::new();
    let mut header_result: Option<(Option<Ident>, Ident, Ident)> = None;
    let mut elements = Vec::new();

    for item_pair in inner {
        match item_pair.as_rule() {
            Rule::annotation => annotations.push(parse_annotation(item_pair)?),
            Rule::transition_header => {
                header_result = Some(parse_transition_header(item_pair)?);
            }
            Rule::transition_body => {
                // Parse elements within the body
                elements = item_pair
                    .into_inner()
                    .filter(|p| p.as_rule() == Rule::transition_element)
                    .map(parse_transition_element)
                    .collect::<Result<Vec<_>, _>>()?;
            }
            // Ignore semicolon, handled by grammar
            Rule::COMMENT | Rule::WHITESPACE => {} // Ignore
            _ => {
                return Err(ParseError::UnexpectedRule {
                    rule: item_pair.as_rule(),
                    context: "parsing transition_item".to_string(),
                })
            }
        }
    }

    let (name, from, to) = header_result.ok_or_else(|| {
        ParseError::MissingElement("transition header parsing failed or missing".to_string())
    })?;

    // TODO: Validate transition elements (exactly one 'on', max one 'guard'/'action')
    // let on_count = elements.iter().filter(|e| matches!(e, TransitionElement::On { .. })).count();
    // if on_count != 1 {
    //     return Err(ParseError::SemanticError(format!("Transition must have exactly one 'on' trigger, found {}", on_count)));
    // }
    // ... more validation ...

    Ok(TransitionItem {
        name,
        from,
        to,
        elements,
        annotations,
    })
}

fn parse_transition_element(pair: Pair<Rule>) -> Result<TransitionElement, ParseError> {
    debug_assert_eq!(pair.as_rule(), Rule::transition_element);
    let inner_pair = pair.into_inner().next().unwrap();
    match inner_pair.as_rule() {
        Rule::on_trigger => {
            let mut trigger_inner = inner_pair.into_inner();
            let ordinal_pair = trigger_inner.next().unwrap(); // transition_ordinal = { "@" ~ integer_literal }
            let ordinal = parse_integer_literal(ordinal_pair.into_inner().next().unwrap())?;

            let qident_pair = trigger_inner.next().unwrap();
            if qident_pair.as_rule() != Rule::qualified_ident {
                return Err(ParseError::UnexpectedRule {
                    rule: qident_pair.as_rule(),
                    context: "parsing on_trigger: expected qualified_ident".to_string(),
                });
            }
            let target_event = parse_qualified_ident(qident_pair)?;
            Ok(TransitionElement::On {
                ordinal,
                event: target_event,
            })
        }
        Rule::guard_condition => {
            let mut guard_inner = inner_pair.into_inner();
            let ordinal_pair = guard_inner.next().unwrap();
            let ordinal = parse_integer_literal(ordinal_pair.into_inner().next().unwrap())?;

            let qident_pair = guard_inner.next().unwrap();
            if qident_pair.as_rule() != Rule::qualified_ident {
                return Err(ParseError::UnexpectedRule {
                    rule: qident_pair.as_rule(),
                    context: "parsing guard_condition: expected qualified_ident".to_string(),
                });
            }
            let condition = parse_qualified_ident(qident_pair)?;
            Ok(TransitionElement::Guard {
                ordinal,
                function: condition,
            })
        }
        Rule::action_effect => {
            let mut action_inner = inner_pair.into_inner();
            let ordinal_pair = action_inner.next().unwrap();
            let ordinal = parse_integer_literal(ordinal_pair.into_inner().next().unwrap())?;

            let qident_pair = action_inner.next().unwrap();
            if qident_pair.as_rule() != Rule::qualified_ident {
                return Err(ParseError::UnexpectedRule {
                    rule: qident_pair.as_rule(),
                    context: "parsing action_effect: expected qualified_ident".to_string(),
                });
            }
            let effect = parse_qualified_ident(qident_pair)?;
            Ok(TransitionElement::Action {
                ordinal,
                function: effect,
            })
        }
        _ => Err(ParseError::UnexpectedRule {
            rule: inner_pair.as_rule(),
            context: "parsing transition_element (expected on, guard, or action)".to_string(),
        }),
    }
}

fn parse_context_block(pair: Pair<Rule>) -> Result<Vec<FieldDef>, ParseError> {
    debug_assert_eq!(pair.as_rule(), Rule::context_block);
    let mut fields = Vec::new();
    for item_pair in pair.into_inner() {
        if item_pair.as_rule() == Rule::context_field {
            // context_field reuses event_field rule
            // Pass an empty Vec for annotations for context fields
            fields.push(parse_event_field(
                item_pair.into_inner().next().unwrap(),
                Vec::new(),
            )?);
        } else if item_pair.as_rule() != Rule::WHITESPACE && item_pair.as_rule() != Rule::COMMENT {
            return Err(ParseError::UnexpectedRule {
                rule: item_pair.as_rule(),
                context: "context_block definition".to_string(),
            });
        }
    }
    Ok(fields)
}

fn parse_ident(pair: Pair<Rule>) -> Result<Ident, ParseError> {
    if pair.as_rule() != Rule::ident {
        return Err(ParseError::UnexpectedRule {
            rule: pair.as_rule(),
            context: "parse_ident called with wrong rule".to_string(),
        });
    }
    let ident_str = pair.as_str();
    // Use Span::call_site() as we can't directly convert pest::Span
    // Ok(proc_macro2::Ident::new(ident_str, pair.as_span().into()))
    Ok(proc_macro2::Ident::new(
        ident_str,
        proc_macro2::Span::call_site(),
    ))
}

fn parse_qualified_ident(pair: Pair<Rule>) -> Result<QualifiedIdent, ParseError> {
    // Check the top-level rule passed
    if pair.as_rule() != Rule::qualified_ident {
        return Err(ParseError::UnexpectedRule {
            rule: pair.as_rule(),
            context: "parse_qualified_ident called with wrong rule".to_string(),
        });
    }

    // Grammar: qualified_ident = { ident ~ ("." ~ ident)? } // No @ capture
    let mut inner = pair.into_inner(); // Inner now yields the sequence

    // Expect the first ident
    let first_ident_pair = inner
        .next()
        .ok_or_else(|| ParseError::MissingElement("First ident in qual. ident".to_string()))?;
    let first_ident = parse_ident(first_ident_pair)?;

    // Check if there's a second ident (meaning a '.' was matched implicitly)
    if let Some(second_ident_pair) = inner.next() {
        // Pest implicitly consumes the '.' between the idents based on the grammar.
        // The next pair yielded by inner will be the second ident.
        let second_ident = parse_ident(second_ident_pair)?;
        Ok(QualifiedIdent::Qualified {
            qualifier: first_ident,
            name: second_ident,
        })
    } else {
        // No second ident pair, so it's a simple identifier
        Ok(QualifiedIdent::Simple(first_ident))
    }
}

fn parse_package_name(pair: Pair<Rule>) -> Result<String, ParseError> {
    debug_assert_eq!(pair.as_rule(), Rule::package_name);
    Ok(pair.as_str().to_string())
}

fn parse_integer_literal(pair: Pair<Rule>) -> Result<u64, ParseError> {
    debug_assert_eq!(pair.as_rule(), Rule::integer_literal);
    let num_str = pair.as_str();
    num_str
        .parse::<u64>()
        .map_err(|e| ParseError::InvalidNumber(num_str.to_string(), e.to_string()))
}

fn parse_hex_literal(pair: Pair<Rule>) -> Result<u64, ParseError> {
    debug_assert_eq!(pair.as_rule(), Rule::hex_literal);
    let hex_str = pair.as_str().trim_start_matches("0x");
    u64::from_str_radix(hex_str, 16)
        .map_err(|e| ParseError::InvalidNumber(hex_str.to_string(), e.to_string()))
}

/// Unescapes a string literal parsed by Pest.
fn unescape_string(s: &str) -> String {
    // Based on Pest documentation for string literal unescaping
    // Assumes the grammar correctly handles escape sequences.
    // This might need adjustment based on the exact grammar rules for escapes.
    let content = &s[1..s.len() - 1]; // Remove outer quotes
    content.replace("\\\"", "\"").replace("\\\\", "\\") // Basic unescape
                                                        // Add more complex escape sequence handling here if needed (\n, \t, \uXXXX)
}

#[cfg(test)]
mod tests {
    use super::*;

    use pretty_assertions::assert_eq;
    use proc_macro2::Ident;
    use proc_macro2::Span;

    // Helper to create Ident for tests
    fn ident(s: &str) -> Ident {
        Ident::new(s, Span::call_site())
    }

    #[test]
    fn test_parse_valid_input() {
        let input = r#"
            @0x1;
            package my_pkg;
            import another_pkg.utils;

            $top_level_annotation("Info");
            stateMachine MyFSM {
                $machine_annotation("Details");

                states {
                    $state_ann("Initial");
                    Initial @0;

                    Active @1 {
                        entry : action1;
                        exit : action3;
                        $state_element_ann("Note");
                    }
                    Done @2;
                }

                events {
                    $description("Event to start processing.");
                    event Start @0 {
                        $description("User ID initiating the start.");
                        userId : UInt64 @0;

                        $description("Optional config string.");
                        config : Text @1;
                    }

                    $description("Event without payload.");
                    event Stop @1 {}
                }

                transitions {
                    transition from Initial to Active { on @0 Start; }
                    transition from Active to Done { on @1 Stop; }
                }
            }
        "#;
        let result = parse_str(input);
        assert!(result.is_ok(), "Parse failed: {:?}", result.err());
        let ssot_file = result.unwrap();

        assert_eq!(ssot_file.file_id, 0x1);
        assert_eq!(ssot_file.package_declaration, Some("my_pkg".to_string()));

        // Find the $top_level_annotation annotation and check its value
        let derive_annotation = ssot_file
            .items
            .iter()
            .find_map(|item| match item {
                TopLevelItem::Annotation(ann) if ann.name == "top_level_annotation" => Some(ann),
                _ => None,
            })
            .expect("Expected $top_level_annotation annotation");

        assert_eq!(
            derive_annotation.value,
            Some(AnnotationValue::IdentifierList(vec![ident("Info")]))
        );

        let top_level_annotations_count = ssot_file
            .items
            .iter()
            .filter(|item| matches!(item, TopLevelItem::Annotation(_)))
            .count();
        assert!(
            top_level_annotations_count > 0,
            "Expected at least one top-level annotation"
        );

        let state_machines: Vec<&StateMachine> = ssot_file
            .items
            .iter()
            .filter_map(|item| match item {
                TopLevelItem::StateMachine(sm) => Some(sm),
                _ => None,
            })
            .collect();
        assert_eq!(
            state_machines.len(),
            1,
            "Expected exactly one state machine"
        );

        let machine = state_machines[0];
        assert_eq!(machine.name.to_string(), "MyFSM");
        assert!(!machine.annotations.is_empty());

        // Verify Context
        assert_eq!(machine.context.len(), 2);
        assert_eq!(machine.context[0].name.to_string(), "counter");
        assert_eq!(machine.context[0].field_type, FieldType::Int32);
        assert_eq!(machine.context[0].ordinal, None);
        assert_eq!(machine.context[1].name.to_string(), "description");
        assert_eq!(machine.context[1].field_type, FieldType::Text);
        assert_eq!(machine.context[1].ordinal, Some(1));

        assert_eq!(machine.states.len(), 3);
        assert_eq!(machine.states[0].name.to_string(), "Initial");
        assert!(machine.states[0].entry_actions.is_empty());
        assert!(machine.states[0].exit_actions.is_empty());
        assert_eq!(machine.states[1].name.to_string(), "Active");
        assert_eq!(machine.states[1].entry_actions.len(), 1);
        assert_eq!(machine.states[1].entry_actions[0].to_string(), "action1");
        assert_eq!(machine.states[1].exit_actions.len(), 1);
        assert_eq!(machine.states[1].exit_actions[0].to_string(), "action3");
        assert_eq!(machine.states[2].name.to_string(), "Done");
        assert!(machine.states[2].entry_actions.is_empty());
        assert!(machine.states[2].exit_actions.is_empty());

        assert_eq!(machine.events.len(), 2);
        assert_eq!(machine.transitions.len(), 2);
    }

    #[test]
    fn test_missing_file_id() {
        // This input lacks the mandatory file_id
        let input = r#"
            package my_pkg;
            stateMachine Test { states { A@0;} events { event E@0{} } transition from A to A { on @0 E;} }
            "#;
        let result = parse_str(input);
        assert!(result.is_err());
        match result.unwrap_err() {
            // The grammar now requires file_id, so absence is a PestError
            ParseError::PestError(_) => {} // Expect PestError now
            e => panic!("Expected PestError for missing file_id, got {:?}", e),
        }
    }

    #[test]
    fn test_missing_on_in_transition() {
        // Input is now grammatically correct except for the missing 'on' clause
        let input = r#"
             @0x1;
             package my_pkg;
             stateMachine Test {
                 states { A@0; B@1; }
                 events { event E@0{} }
                 // Missing 'on' clause
                 transition from A to B { action @0 some_action; }
             }
             "#;
        let result = parse_str(input);
        assert!(result.is_err());
        // This should ideally be a custom MissingElement error, but might be PestError
        // if the grammar rule structurally requires 'on' before 'action'.
        match result.unwrap_err() {
            ParseError::PestError(_) => {} // Accept PestError for now
            // TODO: Enhance parser logic to detect missing required transition elements like 'on'
            // ParseError::MissingElement(s) => assert!(s.contains("on clause")),
            e => panic!(
                "Expected PestError or MissingElement for missing 'on', got {:?}",
                e
            ),
        }
    }

    #[test]
    fn test_duplicate_state() {
        // Correct syntax for the duplicate check
        let input = r#"
            @0x1;
            package my_pkg;
            stateMachine Test {
                states { A@0; A@1; } // Duplicate state name 'A'
                events { event E@0{} }
                transition from A to A { on @0 E; }
            }
            "#;
        let result = parse_str(input);
        // Expect an error, likely PestError as duplicates aren't specifically handled yet
        assert!(result.is_err());
        match result.unwrap_err() {
            ParseError::PestError(_) => {} // Accept PestError
            // TODO: Enhance parser (`parse_state_enum`) to detect duplicate state names/ordinals
            //       and return ParseError::DuplicateDefinition.
            e => panic!("Expected PestError for duplicate state, got {:?}", e),
        }
    }

    #[test]
    fn test_duplicate_event() {
        // Correct syntax for the duplicate check
        let input = r#"
            @0x1;
            package my_pkg;
            stateMachine Test {
                states { A@0; B@1; }
                events { event E@0{}; event E@1{}; } // Duplicate event name 'E'
                transition from A to B { on @0 E; }
            }
            "#;
        let result = parse_str(input);
        // Expect an error, likely PestError as duplicates aren't specifically handled yet
        assert!(result.is_err());
        match result.unwrap_err() {
            ParseError::PestError(_) => {} // Accept PestError
            // TODO: Enhance parser (`parse_event_definitions`) to detect duplicate event names/ordinals
            //       and return ParseError::DuplicateDefinition.
            e => panic!("Expected PestError for duplicate event, got {:?}", e),
        }
    }

    #[test]
    fn test_parse_annotations_on_items() {
        let input = r#"
        @0xcafe0001;
        stateMachine AnnMachine {
            states {
                $description("Initial idle state.");
                Idle @0;

                $description("Machine is actively running.");
                Running @1;
            }
            events {
                $description("Event to start processing.");
                event Start @0 {
                    $description("User ID initiating the start.");
                    userId : UInt64 @0;

                    $description("Optional config string.");
                    config : Text @1;
                }

                $description("Event without payload.");
                event Stop @1 {}
            }
            transitions {
                transition from Idle to Running { on @0 Start; }
            }
        }
        "#;
        let result = parse_str(input);
        assert!(result.is_ok(), "Parsing failed: {:?}", result.err());
        let file_ast = result.unwrap();
        let machine = file_ast
            .items
            .iter()
            .find_map(|item| match item {
                TopLevelItem::StateMachine(sm) => Some(sm),
                _ => None,
            })
            .expect("State machine not found in parsed items");

        // Check State annotations
        let idle_state = machine.states.iter().find(|s| s.name == "Idle").unwrap();
        assert_eq!(idle_state.annotations.len(), 1);
        assert_eq!(idle_state.annotations[0].name, "description");
        assert_eq!(
            idle_state.annotations[0].value,
            Some(AnnotationValue::StringLiteral(
                "Initial idle state.".to_string()
            ))
        );

        let running_state = machine.states.iter().find(|s| s.name == "Running").unwrap();
        assert_eq!(running_state.annotations.len(), 1);
        assert_eq!(running_state.annotations[0].name, "description");
        assert_eq!(
            running_state.annotations[0].value,
            Some(AnnotationValue::StringLiteral(
                "Machine is actively running.".to_string()
            ))
        );

        // Check Event and Field annotations
        let start_event = machine.events.iter().find(|e| e.name == "Start").unwrap();
        assert_eq!(start_event.annotations.len(), 1);
        assert_eq!(start_event.annotations[0].name, "description");
        assert_eq!(
            start_event.annotations[0].value,
            Some(AnnotationValue::StringLiteral(
                "Event to start processing.".to_string()
            ))
        );

        let userid_field = start_event
            .fields
            .iter()
            .find(|f| f.name == "userId")
            .unwrap();
        assert_eq!(userid_field.annotations.len(), 1);
        assert_eq!(userid_field.annotations[0].name, "description");
        assert_eq!(
            userid_field.annotations[0].value,
            Some(AnnotationValue::StringLiteral(
                "User ID initiating the start.".to_string()
            ))
        );

        let config_field = start_event
            .fields
            .iter()
            .find(|f| f.name == "config")
            .unwrap();
        assert_eq!(config_field.annotations.len(), 1);
        assert_eq!(config_field.annotations[0].name, "description");
        assert_eq!(
            config_field.annotations[0].value,
            Some(AnnotationValue::StringLiteral(
                "Optional config string.".to_string()
            ))
        );

        let stop_event = machine.events.iter().find(|e| e.name == "Stop").unwrap();
        assert_eq!(stop_event.annotations.len(), 1);
        assert_eq!(stop_event.annotations[0].name, "description");
        assert_eq!(
            stop_event.annotations[0].value,
            Some(AnnotationValue::StringLiteral(
                "Event without payload.".to_string()
            ))
        );
    }

    // #[test]
    // fn test_parse_from_file() {
    //     // Create a temporary directory
    //     let dir = tempdir().unwrap();
    //     let file_path = dir.path().join("test.ssot");
    //     let mut file = File::create(&file_path).unwrap();
    //
    //     // Read the content of the actual spec file relative to the workspace root
    //     // Ensure the test runner executes from a context where this relative path is valid,
    //     // typically the workspace root or the crate root.
    //     let input_content = fs::read_to_string("../../fsm-example/spec/my_fsm.ssot")
    //         .expect("Failed to read spec file. Ensure test is run from workspace/crate root.");
    //
    //     // Write the content to the temporary file
    //     writeln!(file, "{}", input_content).unwrap();
    //
    //     // Use parse_file which handles reading the file content
    //     let result = parse_file(&file_path);
    //
    //     // Check if parsing was successful
    //     assert!(result.is_ok(), "{:?}", result.unwrap_err());
    //
    //     // Further assertions on the parsed AST (file_ast)
    //     let file_ast = result.unwrap();
    //     assert_eq!(file_ast.state_machines.len(), 1);
    //     let machine = file_ast.state_machines.first().unwrap();
    //     assert_eq!(machine.name.to_string(), "LightSwitch");
    //     assert_eq!(machine.states.len(), 2);
    //     assert_eq!(machine.events.len(), 3);
    //     assert_eq!(machine.transitions.len(), 4);
    // }
}
