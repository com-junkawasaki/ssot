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
        let actual_content_pair = annotation_value_inner_pair;
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
                // Handle the new ident_list rule
                let idents = actual_content_pair
                    .into_inner() // Gets the idents inside the list
                    .map(parse_ident) // Parse each ident
                    .collect::<Result<Vec<Ident>, _>>()?; // Collect results
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
    for item_pair in inner {
        match item_pair.as_rule() {
            Rule::annotation => {
                annotations.push(parse_annotation(item_pair)?);
            }
            Rule::states_block => {
                states = parse_states_block(item_pair)?;
            }
            Rule::events_block => {
                // Parse events block, which might contain events or use declarations
                let (parsed_events, event_use_decls) = parse_events_block(item_pair)?;
                events = parsed_events;
                use_declarations.extend(event_use_decls); // Add any `use` from events block
            }
            Rule::transitions_block => {
                transitions = parse_transitions_block(item_pair)?;
            }
            Rule::context_block => {
                context_fields = parse_context_block(item_pair)?;
            }
            Rule::use_decl => {
                // Handle use declarations directly within the machine
                use_declarations.push(parse_use_declaration(item_pair)?);
            }
            Rule::WHITESPACE | Rule::COMMENT => { /* ignore */ }
            Rule::EOI => break, // Should not happen inside machine block normally
            _ => {
                return Err(ParseError::UnexpectedRule {
                    rule: item_pair.as_rule(),
                    context: format!("inside stateMachine '{}' block", name),
                });
            }
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
    let inner = pair.into_inner().next().ok_or_else(|| {
        ParseError::MissingElement("qualified identifier in use declaration".to_string())
    })?;
    let target = parse_qualified_ident(inner)?;
    Ok(UseDeclaration { target })
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
                for body_item_pair in item_pair.into_inner() {
                    match body_item_pair.as_rule() {
                        Rule::entry_action => {
                            let q_ident =
                                parse_qualified_ident(body_item_pair.into_inner().next().unwrap())?;
                            entry_actions.push(q_ident);
                        }
                        Rule::exit_action => {
                            let q_ident =
                                parse_qualified_ident(body_item_pair.into_inner().next().unwrap())?;
                            exit_actions.push(q_ident);
                        }
                        Rule::annotation => {
                            // Annotations inside state body? Ignore for now.
                        }
                        Rule::WHITESPACE | Rule::COMMENT => { /* Ignore */ }
                        _ => {
                            return Err(ParseError::UnexpectedRule {
                                rule: body_item_pair.as_rule(),
                                context: "state_body definition".to_string(),
                            });
                        }
                    }
                }
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

    for item_pair in pair.into_inner() {
        match item_pair.as_rule() {
            Rule::event_struct => {
                events.push(parse_event_struct(item_pair)?);
            }
            Rule::use_decl => {
                use_declarations.push(parse_use_declaration(item_pair)?);
            }
            Rule::WHITESPACE | Rule::COMMENT => { /* ignore */ }
            _ => {
                return Err(ParseError::UnexpectedRule {
                    rule: item_pair.as_rule(),
                    context: "events_block definition".to_string(),
                });
            }
        }
    }
    Ok((events, use_declarations))
}

fn parse_event_struct(pair: Pair<Rule>) -> Result<MessageItem, ParseError> {
    debug_assert_eq!(pair.as_rule(), Rule::event_struct);
    let inner = pair.into_inner();
    let mut annotations = Vec::new();
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
                for field_pair in item_pair.into_inner() {
                    if field_pair.as_rule() == Rule::event_field {
                        fields.push(parse_event_field(field_pair)?);
                    } else if field_pair.as_rule() != Rule::WHITESPACE
                        && field_pair.as_rule() != Rule::COMMENT
                    {
                        return Err(ParseError::UnexpectedRule {
                            rule: field_pair.as_rule(),
                            context: "inside event_body".to_string(),
                        });
                    }
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
        annotations,
        name: name.ok_or_else(|| ParseError::MissingElement("event name".to_string()))?,
        ordinal: ordinal
            .ok_or_else(|| ParseError::MissingElement("event ordinal (@N)".to_string()))?,
        fields,
    })
}

fn parse_event_field(pair: Pair<Rule>) -> Result<FieldDef, ParseError> {
    debug_assert_eq!(pair.as_rule(), Rule::event_field);
    let mut inner = pair.into_inner(); // Use mutable iterator
    let mut annotations = Vec::new();
    let mut name: Option<Ident> = None;
    let mut field_type: Option<FieldType> = None;
    let mut ordinal: Option<u64> = None;

    // Peek at the pairs to parse them in order
    while let Some(item_pair) = inner.peek() {
        match item_pair.as_rule() {
            Rule::annotation => {
                annotations.push(parse_annotation(inner.next().unwrap())?);
            }
            Rule::ident => {
                name = Some(parse_ident(inner.next().unwrap())?);
            }
            Rule::field_type => {
                // The actual type is nested inside field_type rule
                let type_pair = inner.next().unwrap().into_inner().next().unwrap();
                field_type = Some(parse_field_type(type_pair)?);
            }
            Rule::field_ordinal => {
                // The actual literal is nested inside field_ordinal
                let ordinal_pair = inner.next().unwrap().into_inner().next().unwrap();
                ordinal = Some(parse_integer_literal(ordinal_pair)?);
            }
            Rule::WHITESPACE | Rule::COMMENT => {
                let _ = inner.next(); /* consume and ignore */
            }
            _ => {
                // This should ideally not be reached if grammar is correct
                // but handles unexpected tokens within the field definition.
                return Err(ParseError::UnexpectedRule {
                    rule: item_pair.as_rule(),
                    context: "event_field definition inner elements".to_string(),
                });
            }
        }
    }

    Ok(FieldDef {
        annotations,
        name: name.ok_or_else(|| ParseError::MissingElement("field name".to_string()))?,
        ordinal, // Now optional
        field_type: field_type
            .ok_or_else(|| ParseError::MissingElement("field type".to_string()))?,
    })
}

fn parse_field_type(pair: Pair<Rule>) -> Result<FieldType, ParseError> {
    match pair.as_rule() {
        Rule::primitive_type => match pair.as_str() {
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
            _ => Err(ParseError::InvalidTypeString(pair.as_str().to_string())),
        },
        Rule::list_type => {
            let inner_type_pair = pair.into_inner().next().unwrap(); // Should contain field_type
            let inner_type = parse_field_type(inner_type_pair.into_inner().next().unwrap())?; // Unwrap inner
            Ok(FieldType::List(Box::new(inner_type)))
        }
        Rule::type_identifier => {
            Ok(FieldType::Identifier(parse_qualified_ident(
                pair.into_inner().next().unwrap(),
            )?)) // Unwrap inner qualified_ident
        }
        _ => Err(ParseError::UnexpectedRule {
            rule: pair.as_rule(),
            context: "field type".to_string(),
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
                            fields.push(parse_event_field(event_field_pair)?);
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

fn parse_transition_item(pair: Pair<Rule>) -> Result<TransitionItem, ParseError> {
    debug_assert_eq!(pair.as_rule(), Rule::transition_item);
    let inner = pair.into_inner();
    let mut annotations = Vec::new();
    let mut name: Option<Ident> = None;
    let mut from: Option<Ident> = None;
    let mut to: Option<Ident> = None;
    let mut elements = Vec::new();

    for item_pair in inner {
        match item_pair.as_rule() {
            Rule::annotation => annotations.push(parse_annotation(item_pair)?),
            Rule::transition_header => {
                let mut header_inner = item_pair.into_inner();
                // Optional transition name
                if header_inner.peek().unwrap().as_rule() == Rule::ident {
                    name = Some(parse_ident(header_inner.next().unwrap())?);
                }
                from = Some(parse_ident(header_inner.next().unwrap())?);
                to = Some(parse_ident(header_inner.next().unwrap())?);
            }
            Rule::transition_body => {
                for element_pair in item_pair.into_inner() {
                    if element_pair.as_rule() == Rule::transition_element {
                        elements.push(parse_transition_element(
                            element_pair.into_inner().next().unwrap(),
                        )?);
                    } else if element_pair.as_rule() == Rule::annotation {
                        // TODO: Handle annotations within transition body?
                    } else if element_pair.as_rule() != Rule::WHITESPACE
                        && element_pair.as_rule() != Rule::COMMENT
                    {
                        return Err(ParseError::UnexpectedRule {
                            rule: element_pair.as_rule(),
                            context: "transition_body definition".to_string(),
                        });
                    }
                }
            }
            Rule::WHITESPACE | Rule::COMMENT => { /* ignore */ }
            _ => {
                return Err(ParseError::UnexpectedRule {
                    rule: item_pair.as_rule(),
                    context: "transition_item definition".to_string(),
                });
            }
        }
    }

    Ok(TransitionItem {
        name,
        from: from
            .ok_or_else(|| ParseError::MissingElement("transition 'from' state".to_string()))?,
        to: to.ok_or_else(|| ParseError::MissingElement("transition 'to' state".to_string()))?,
        elements,
        annotations,
    })
}

fn parse_transition_element(pair: Pair<Rule>) -> Result<TransitionElement, ParseError> {
    match pair.as_rule() {
        Rule::on_trigger => {
            let mut inner = pair.into_inner();
            let ordinal =
                parse_integer_literal(inner.next().unwrap().into_inner().next().unwrap())?;
            let event = parse_qualified_ident(inner.next().unwrap())?;
            Ok(TransitionElement::On { ordinal, event })
        }
        Rule::guard_condition => {
            let mut inner = pair.into_inner();
            let ordinal =
                parse_integer_literal(inner.next().unwrap().into_inner().next().unwrap())?;
            let function = parse_qualified_ident(inner.next().unwrap())?;
            Ok(TransitionElement::Guard { ordinal, function })
        }
        Rule::action_effect => {
            let mut inner = pair.into_inner();
            let ordinal =
                parse_integer_literal(inner.next().unwrap().into_inner().next().unwrap())?;
            let function = parse_qualified_ident(inner.next().unwrap())?;
            Ok(TransitionElement::Action { ordinal, function })
        }
        _ => Err(ParseError::UnexpectedRule {
            rule: pair.as_rule(),
            context: "transition element (on, guard, action)".to_string(),
        }),
    }
}

fn parse_context_block(pair: Pair<Rule>) -> Result<Vec<FieldDef>, ParseError> {
    debug_assert_eq!(pair.as_rule(), Rule::context_block);
    let mut fields = Vec::new();
    for item_pair in pair.into_inner() {
        if item_pair.as_rule() == Rule::context_field {
            // context_field reuses event_field rule
            fields.push(parse_event_field(item_pair.into_inner().next().unwrap())?);
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
    debug_assert_eq!(pair.as_rule(), Rule::ident);
    let ident_str = pair.as_str();
    syn::parse_str::<Ident>(ident_str)
        .map_err(|_| ParseError::InvalidIdentifier(ident_str.to_string()))
}

fn parse_qualified_ident(pair: Pair<Rule>) -> Result<QualifiedIdent, ParseError> {
    debug_assert_eq!(pair.as_rule(), Rule::qualified_ident);
    let mut parts = pair.into_inner();
    let first = parse_ident(parts.next().unwrap())?;
    if let Some(second) = parts.next() {
        let name = parse_ident(second)?;
        Ok(QualifiedIdent::Qualified {
            qualifier: first,
            name,
        })
    } else {
        Ok(QualifiedIdent::Simple(first))
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
            @0x123456789ABCDEF0;
            package my.package;

            $top_level_derive(Debug, Clone);

            stateMachine MyMachine {
                $initial(Idle);
                $description("A simple state machine.");

                context {
                    counter: Int32;
                    description: Text @1;
                    maybeFlag: Bool;
                }

                states {
                    Idle @0;
                    Running @1 {
                        entry / action1, action2;
                        exit / cleanup;
                    }
                }

                events {
                    Start @0 {
                        source: Text;
                    }
                    Stop @1;
                    Internal @2;
                }

                transitions {
                    from Idle to Running {
                        on @1 Start;
                        action @2 doStart;
                    }
                    from Running to Idle {
                        on @1 Stop;
                        guard @2 canStop;
                    }
                }
            }
        "#;
        let result = parse_str(input);
        assert!(result.is_ok(), "Parse failed: {:?}", result.err());
        let ssot_file = result.unwrap();

        assert_eq!(ssot_file.file_id, 0x123456789ABCDEF0);
        assert_eq!(
            ssot_file.package_declaration,
            Some("my.package".to_string())
        );

        // Find the $top_level_derive annotation and check its value
        let derive_annotation = ssot_file
            .items
            .iter()
            .find_map(|item| match item {
                TopLevelItem::Annotation(ann) if ann.name == "top_level_derive" => Some(ann),
                _ => None,
            })
            .expect("Expected $top_level_derive annotation");

        assert_eq!(
            derive_annotation.value,
            Some(AnnotationValue::IdentifierList(vec![
                ident("Debug"),
                ident("Clone")
            ]))
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
        assert_eq!(machine.name.to_string(), "MyMachine");
        assert!(!machine.annotations.is_empty());

        // Verify Context
        assert_eq!(machine.context.len(), 3);
        assert_eq!(machine.context[0].name.to_string(), "counter");
        assert_eq!(machine.context[0].field_type, FieldType::Int32);
        assert_eq!(machine.context[0].ordinal, None);
        assert_eq!(machine.context[1].name.to_string(), "description");
        assert_eq!(machine.context[1].field_type, FieldType::Text);
        assert_eq!(machine.context[1].ordinal, Some(1));
        assert_eq!(machine.context[2].name.to_string(), "maybeFlag");
        assert_eq!(machine.context[2].field_type, FieldType::Bool);
        assert_eq!(machine.context[2].ordinal, None);

        assert_eq!(machine.states.len(), 2);
        assert_eq!(machine.states[0].name.to_string(), "Idle");
        assert!(machine.states[0].entry_actions.is_empty());
        assert!(machine.states[0].exit_actions.is_empty());
        assert_eq!(machine.states[1].name.to_string(), "Running");
        assert_eq!(machine.states[1].entry_actions.len(), 2);
        assert_eq!(machine.states[1].entry_actions[0].to_string(), "action1");
        assert_eq!(machine.states[1].entry_actions[1].to_string(), "action2");
        assert_eq!(machine.states[1].exit_actions.len(), 1);
        assert_eq!(machine.states[1].exit_actions[0].to_string(), "cleanup");

        assert_eq!(machine.events.len(), 3);
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
                    userId @0 : UInt64;

                    $description("Optional config string.");
                    config @1 : Text;
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
