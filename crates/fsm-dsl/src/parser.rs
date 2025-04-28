#![allow(dead_code, unused_variables, unused_imports)] // Keep module level for now, specific ones below too
use crate::ast::{
    // --- Structs/Enums Present in ast.rs ---
    ActionsBlock,
    ActorDefinition,
    ActorsBlock,
    AfterTransitionDefinition,
    Annotation,
    AnnotationValue,
    Argument,
    AttributeDefinition,  // Added AttributeDefinition
    ChannelDefinition,    // Added ChannelDefinition
    CommunicatesWithArgs, // Added CommunicatesWithArgs
    CommunicationBlock,
    CommunicationItem, // Added CommunicationItem
    ContextDefinition,
    ContextFieldDefinition,
    DeploymentConfigBlock,
    DeploymentDefinition,
    DeploymentItem, // Added DeploymentItem
    Duration,       // Added Duration
    EnumDefinition,
    EnumVariant,
    EnvironmentDefinition, // Added EnvironmentDefinition
    EventDefinition,       // Added EventDefinition
    FieldDefinition,
    FileId,
    GuardDefinition,
    GuardsBlock, // Added GuardDefinition, GuardsBlock
    HistoryDefinition,
    HistoryType,
    Identifier,
    ImportStatement,          // Replaced Imports/ImportsEntry with ImportStatement
    InfrastructureDefinition, // Added InfrastructureDefinition
    InterfaceDefinition,      // Added InterfaceDefinition
    InvokeDefinition,
    InvokeSource,
    InvokeTransitionTarget, // Replaced InvokeSrc etc. with InvokeSource/InvokeTransitionTarget
    InvokesBlock,           // Added InvokesBlock
    MachineDefinition,
    MachinesBlock, // Added MachinesBlock
    MethodDefinition,
    NumericId,           // Added NumericId
    ParameterDefinition, // Replaced Parameter/MethodParameter
    ProtocolDefinition,  // Kept ProtocolDefinition struct
    ServiceDefinition,
    ServiceItem,
    ServicesBlock, // Added ServiceItem, ServicesBlock
    SsotAst,       // Added SsotAst (top-level)
    StateDefinition,
    StateInvokeDefinition, // Kept StateInvokeDefinition
    StatesBlock,
    StructDefinition,
    TimeUnit,           // Added TimeUnit
    TopLevelDefinition, // Added TopLevelDefinition
    TransitionDefinition,
    TransitionTarget, // Kept TransitionDefinition/Target
    TypeDefinition,
    TypeSpecifier,
    TypesBlock,
    // --- Removed/Renamed imports (Not found or different in ast.rs) ---
    // Imports, ImportsEntry, InitialAnnotation, InvokeInputMapping, InvokeOnDoneTransition,
    // InvokeOnErrorTransition, InvokeParam, InvokeSrc, ListType, MapType, MethodParameter,
    // MethodReturn, OptionalType, Parameter, ProtocolChannelDefinition,
    // ProtocolEventDefinition, ProtocolEventField, ServiceElement, ServiceExtends,
    // ServiceInterfaceDefinition, ServiceMethodRef, SimpleType, StateElement, StateInvokeSrc,
    // StateTransitionDefinition, StateMachine, TransitionAction, TransitionBlock,
    // TransitionCondition, TransitionDetail, Value,
};
use pest::iterators::{Pair, Pairs};
use pest::Parser; // Uncommented
use pest_derive::Parser;
use std::path::PathBuf;
use std::str::FromStr;

// Define potential errors during parsing
#[derive(Debug, thiserror::Error)]
pub enum SsotParserError {
    #[error("Pest parsing error: {0}")]
    PestError(#[from] pest::error::Error<Rule>),
    #[error("AST construction error: {0}")]
    AstConstructionError(String),
    #[error("Invalid rule encountered: expected {expected:?}, found {found:?}. Rule: {rule_str}")]
    InvalidRule {
        expected: Rule,
        found: Rule,
        rule_str: String,
    },
    #[error("Missing expected rule: {0:?}. Context: {1}")]
    MissingRule(Rule, String),
    #[error("Invalid identifier: {0}")]
    InvalidIdentifier(String),
    #[error("Invalid integer literal: {0} - {1}")]
    InvalidIntLiteral(String, String),
    #[error("Invalid float literal: {0} - {1}")]
    InvalidFloatLiteral(String, String),
    #[error("Invalid boolean literal: {0}")]
    InvalidBooleanLiteral(String),
    #[error("Invalid null literal: {0}")]
    InvalidNullLiteral(String),
    #[error("Invalid duration literal: {0}")]
    InvalidDurationLiteral(String),
    #[error("Unknown primitive type: {0}")]
    UnknownPrimitiveType(String),
    #[error("Invalid type reference: {0}")]
    InvalidTypeRef(String),
    #[error("Invalid annotation format: {0}")]
    InvalidAnnotation(String),
    #[error("Invalid literal value: {0}")]
    InvalidLiteralValue(String),
    #[error("Invalid state type modifier: {0}")]
    InvalidStateType(String),
    #[error("Invalid history type: {0}")]
    InvalidHistoryType(String),
    #[error("Expected exactly one inner pair for rule {0:?}, found {1}")]
    UnexpectedInnerPairCount(Rule, usize),
    #[error("Failed to parse import path: {0}")]
    InvalidImportPath(String),
    #[error("Invalid numeric ID format: {0}")]
    InvalidNumericId(String),
}

#[derive(Parser)]
#[grammar = "grammar.pest"]
pub struct SsotParser;

// --- Main Parsing Function ---
pub fn parse_ssot_content(
    content: &str,
    source_path: Option<PathBuf>,
) -> Result<SsotAst, SsotParserError> {
    let pairs = SsotParser::parse(Rule::ssot_entry_rule, content)?;

    // Expecting a single 'file' rule at the top level
    let file_pair = pairs.peek().ok_or_else(|| {
        SsotParserError::AstConstructionError("Expected 'file' rule, found nothing".to_string())
    })?;

    if file_pair.as_rule() != Rule::file {
        return Err(SsotParserError::InvalidRule {
            expected: Rule::file,
            found: file_pair.as_rule(),
            rule_str: file_pair.as_str().to_string(),
        });
    }

    let mut imports = Vec::new();
    let mut definitions = Vec::new();
    // file_id is not directly parsed from content, handle externally if needed
    let file_id = None;

    for pair in file_pair.into_inner() {
        match pair.as_rule() {
            Rule::import_statement => {
                imports.push(parse_import_statement(pair)?);
            }
            Rule::top_level_definition => {
                // Parse the top-level definition
                definitions.push(parse_top_level_definition(pair)?);
            }
            Rule::COMMENT | Rule::WHITESPACE => { /* Skip */ }
            Rule::EOI => { /* End of Input, expected */ }
            _ => {
                // This shouldn't happen if the grammar is correct for 'file'
                return Err(SsotParserError::AstConstructionError(format!(
                    "Unexpected rule {:?} inside file rule",
                    pair.as_rule()
                )));
            }
        }
    }

    Ok(SsotAst {
        file_id,
        source_path,
        imports,
        definitions,
    })
}

// --- Helper Parsing Functions ---

fn parse_identifier(pair: Pair<Rule>) -> Result<Identifier, SsotParserError> {
    if pair.as_rule() != Rule::IDENTIFIER {
        return Err(SsotParserError::InvalidRule {
            expected: Rule::IDENTIFIER,
            found: pair.as_rule(),
            rule_str: pair.as_str().to_string(),
        });
    }
    Ok(Identifier {
        name: pair.as_str().to_string(),
    })
}

fn parse_numeric_id(pair: Pair<Rule>) -> Result<NumericId, SsotParserError> {
    Ok(NumericId { value: 0 })
}

fn parse_import_statement(pair: Pair<Rule>) -> Result<ImportStatement, SsotParserError> {
    if pair.as_rule() != Rule::import_statement {
        return Err(SsotParserError::InvalidRule {
            expected: Rule::import_statement,
            found: pair.as_rule(),
            rule_str: pair.as_str().to_string(),
        });
    }

    // Find the STRING_LITERAL which represents the path
    let path_pair = pair
        .into_inner()
        .find(|p| p.as_rule() == Rule::STRING_LITERAL)
        .ok_or_else(|| {
            SsotParserError::MissingRule(Rule::STRING_LITERAL, "in import statement".to_string())
        })?;

    // Extract the string content, removing quotes
    let path_str = path_pair.as_str();
    if path_str.len() >= 2 && path_str.starts_with('"') && path_str.ends_with('"') {
        let path = path_str[1..path_str.len() - 1].to_string();
        // TODO: Handle escape sequences if needed
        Ok(ImportStatement { path })
    } else {
        Err(SsotParserError::InvalidImportPath(path_str.to_string()))
    }
}

fn parse_top_level_definition(pair: Pair<Rule>) -> Result<TopLevelDefinition, SsotParserError> {
    if pair.as_rule() != Rule::top_level_definition {
        return Err(SsotParserError::InvalidRule {
            expected: Rule::top_level_definition,
            found: pair.as_rule(),
            rule_str: pair.as_str().to_string(),
        });
    }

    // top_level_definition = _{ annotation* ~ (machine_definition | type_definition | ...) }
    let mut inner_pairs = pair.into_inner();

    // TODO: Parse annotations first if needed
    let annotations: Vec<Annotation> = vec![]; // Placeholder

    // Find the actual definition rule (machine_definition, type_definition, etc.)
    let definition_pair = inner_pairs
        .find(|p| {
            p.as_rule() != Rule::annotation
                && p.as_rule() != Rule::COMMENT
                && p.as_rule() != Rule::WHITESPACE
        })
        .ok_or_else(|| {
            SsotParserError::MissingRule(
                Rule::machine_definition, // Or any other expected definition rule
                "inside top_level_definition".to_string(),
            )
        })?;

    match definition_pair.as_rule() {
        Rule::machine_definition => {
            // Wrap MachineDefinition in MachinesBlock for TopLevelDefinition::Machines
            let machine_def = parse_machine_definition(definition_pair)?;
            // Currently, grammar doesn't define MachinesBlock explicitly at top level?
            // Let's assume a single machine can be wrapped.
            // Need to adjust AST or parser based on final design.
            let machines_block = MachinesBlock {
                definitions: vec![machine_def],
                annotations: vec![], // TODO: Handle annotations on the block if needed
            };
            Ok(TopLevelDefinition::Machines(machines_block))
        }
        Rule::type_definition => {
            // TODO: Implement parse_type_definition
            Err(SsotParserError::AstConstructionError(
                "Parsing for type_definition not yet implemented".to_string(),
            ))
        }
        Rule::actor_definition => Err(SsotParserError::AstConstructionError(
            "Parsing for actor_definition not yet implemented".to_string(),
        )),
        Rule::service_definition => Err(SsotParserError::AstConstructionError(
            "Parsing for service_definition not yet implemented".to_string(),
        )),
        Rule::communication_definition => Err(SsotParserError::AstConstructionError(
            "Parsing for communication_definition not yet implemented".to_string(),
        )),
        Rule::deployment_definition => Err(SsotParserError::AstConstructionError(
            "Parsing for deployment_definition not yet implemented".to_string(),
        )),
        Rule::infrastructure_definition => Err(SsotParserError::AstConstructionError(
            "Parsing for infrastructure_definition not yet implemented".to_string(),
        )),
        rule => Err(SsotParserError::InvalidRule {
            expected: Rule::machine_definition,
            found: rule,
            rule_str: definition_pair.as_str().to_string(),
        }),
    }
}

// TODO: Implement functions like:
// fn parse_machine_definition(pair: Pair<Rule>) -> Result<MachineDefinition, SsotParserError> { ... }
// fn parse_type_definition(pair: Pair<Rule>) -> Result<TypeDefinition, SsotParserError> { ... }
// ... and so on for all major grammar rules and their corresponding AST nodes.

fn parse_machine_definition(pair: Pair<Rule>) -> Result<MachineDefinition, SsotParserError> {
    if pair.as_rule() != Rule::machine_definition {
        return Err(SsotParserError::InvalidRule {
            expected: Rule::machine_definition,
            found: pair.as_rule(),
            rule_str: pair.as_str().to_string(),
        });
    }

    let mut inner_pairs = pair.into_inner();

    // First inner pair should be the IDENTIFIER (machine name)
    let name_pair = inner_pairs.next().ok_or_else(|| {
        SsotParserError::MissingRule(Rule::IDENTIFIER, "in machine definition name".to_string())
    })?;
    let name = parse_identifier(name_pair)?;

    // TODO: Parse optional machine_body
    let mut context: Option<ContextDefinition> = None;
    let mut initial_state: Option<Identifier> = None;
    let final_states: Vec<Identifier> = Vec::new();
    let mut states: Vec<StateDefinition> = Vec::new();
    let transitions: Vec<TransitionDefinition> = Vec::new(); // For machine-level transitions?
    let actions: Option<ActionsBlock> = None;
    let guards: Option<GuardsBlock> = None;
    let invokes: Option<InvokesBlock> = None;
    let history: Option<HistoryDefinition> = None;

    if let Some(body_pair) = inner_pairs.find(|p| p.as_rule() == Rule::machine_body) {
        for element_pair in body_pair.into_inner() {
            match element_pair.as_rule() {
                Rule::context_definition => {
                    if context.is_some() {
                        return Err(SsotParserError::AstConstructionError(
                            "Duplicate context definition found in machine".to_string(),
                        ));
                    }
                    context = Some(parse_context_definition(element_pair)?);
                }
                Rule::initial_definition => {
                    if initial_state.is_some() {
                        return Err(SsotParserError::AstConstructionError(
                            "Duplicate initial state definition found in machine".to_string(),
                        ));
                    }
                    initial_state = Some(parse_initial_definition(element_pair)?);
                }
                Rule::final_states_definition => {
                    // TODO: Implement parse_final_states_definition
                    return Err(SsotParserError::AstConstructionError(
                        "Parsing for final_states_definition not yet implemented".to_string(),
                    ));
                }
                Rule::state_definition => {
                    states.push(parse_state_definition(element_pair)?);
                }
                Rule::transition_definition => {
                    // transitions.push(parse_transition_definition(element_pair)?);
                    return Err(SsotParserError::AstConstructionError(
                        "Parsing for machine transition_definition not yet implemented".to_string(),
                    ));
                }
                Rule::actions_block => {
                    // TODO: Implement parse_actions_block
                    return Err(SsotParserError::AstConstructionError(
                        "Parsing for actions_block not yet implemented".to_string(),
                    ));
                }
                Rule::guards_block => {
                    // TODO: Implement parse_guards_block
                    return Err(SsotParserError::AstConstructionError(
                        "Parsing for guards_block not yet implemented".to_string(),
                    ));
                }
                Rule::invokes_block => {
                    // TODO: Implement parse_invokes_block
                    return Err(SsotParserError::AstConstructionError(
                        "Parsing for invokes_block not yet implemented".to_string(),
                    ));
                }
                Rule::history_definition => {
                    // TODO: Implement parse_history_definition
                    return Err(SsotParserError::AstConstructionError(
                        "Parsing for history_definition not yet implemented".to_string(),
                    ));
                }
                Rule::COMMENT | Rule::WHITESPACE | Rule::annotation => { /* Skip for now */ }
                _ => {
                    return Err(SsotParserError::AstConstructionError(format!(
                        "Unexpected rule {:?} inside machine_body",
                        element_pair.as_rule()
                    )));
                }
            }
        }
    }

    let states_block = if states.is_empty() {
        None
    } else {
        Some(StatesBlock {
            id: NumericId { value: 0 },
            states,
            annotations: vec![],
        })
    };

    Ok(MachineDefinition {
        name,
        id: NumericId { value: 0 },
        annotations: vec![],
        context,
        states: states_block,
        actions,
        guards,
        invokes,
    })
}

fn parse_context_definition(pair: Pair<Rule>) -> Result<ContextDefinition, SsotParserError> {
    if pair.as_rule() != Rule::context_definition {
        return Err(SsotParserError::InvalidRule {
            expected: Rule::context_definition,
            found: pair.as_rule(),
            rule_str: pair.as_str().to_string(),
        });
    }
    let mut fields = Vec::new();
    if let Some(body_pair) = pair
        .into_inner()
        .find(|p| p.as_rule() == Rule::context_body)
    {
        for field_pair in body_pair.into_inner() {
            match field_pair.as_rule() {
                Rule::context_field_definition => {
                    fields.push(parse_context_field_definition(field_pair)?);
                }
                Rule::COMMENT | Rule::WHITESPACE | Rule::annotation => { /* Skip */ }
                _ => {
                    return Err(SsotParserError::AstConstructionError(format!(
                        "Unexpected rule {:?} inside context_body",
                        field_pair.as_rule()
                    )));
                }
            }
        }
    }
    Ok(ContextDefinition {
        id: NumericId { value: 0 },
        fields,
        annotations: vec![],
    })
}

fn parse_context_field_definition(
    pair: Pair<Rule>,
) -> Result<ContextFieldDefinition, SsotParserError> {
    if pair.as_rule() != Rule::context_field_definition {
        return Err(SsotParserError::InvalidRule {
            expected: Rule::context_field_definition,
            found: pair.as_rule(),
            rule_str: pair.as_str().to_string(),
        });
    }
    let mut inner = pair.into_inner();
    let identifier_pair = inner.next().ok_or_else(|| {
        SsotParserError::MissingRule(Rule::IDENTIFIER, "in context field name".to_string())
    })?;
    let name = parse_identifier(identifier_pair)?;
    let type_ref_pair = inner.next().ok_or_else(|| {
        SsotParserError::MissingRule(Rule::type_ref, "in context field type".to_string())
    })?;
    let type_spec = parse_type_ref(type_ref_pair)?;
    let mut default_value: Option<AnnotationValue> = None;
    if let Some(next_pair) = inner.peek() {
        if next_pair.as_rule() == Rule::literal_value {
            let literal_pair = inner.next().unwrap();
            default_value = Some(parse_literal_value(literal_pair)?);
        }
    }
    Ok(ContextFieldDefinition {
        name,
        type_spec,
        id: NumericId { value: 0 },
        annotations: vec![],
        default_value,
    })
}

fn parse_type_ref(pair: Pair<Rule>) -> Result<TypeSpecifier, SsotParserError> {
    if pair.as_rule() != Rule::type_ref {
        return Err(SsotParserError::InvalidRule {
            expected: Rule::type_ref,
            found: pair.as_rule(),
            rule_str: pair.as_str().to_string(),
        });
    }
    let mut inner = pair.into_inner();
    let type_pair = inner.next().ok_or_else(|| {
        SsotParserError::AstConstructionError("Missing inner type in type_ref".to_string())
    })?;
    let is_optional = inner.next().is_some();
    let base_type = match type_pair.as_rule() {
        Rule::primitive_type => TypeSpecifier::Simple(Identifier {
            name: type_pair.as_str().to_string(),
        }),
        Rule::list_type => {
            let inner_type_pair = type_pair.into_inner().next().ok_or_else(|| {
                SsotParserError::MissingRule(Rule::type_ref, "in list_type".to_string())
            })?;
            let inner_type = parse_type_ref(inner_type_pair)?;
            TypeSpecifier::List(Box::new(inner_type))
        }
        Rule::map_type => {
            let mut map_inner = type_pair.into_inner();
            let key_type_pair = map_inner.next().ok_or_else(|| {
                SsotParserError::MissingRule(Rule::type_ref, "in map_type key".to_string())
            })?;
            let value_type_pair = map_inner.next().ok_or_else(|| {
                SsotParserError::MissingRule(Rule::type_ref, "in map_type value".to_string())
            })?;
            let key_type = parse_type_ref(key_type_pair)?;
            let value_type = parse_type_ref(value_type_pair)?;
            TypeSpecifier::Map(Box::new(key_type), Box::new(value_type))
        }
        Rule::IDENTIFIER => TypeSpecifier::Simple(parse_identifier(type_pair)?),
        rule => {
            return Err(SsotParserError::InvalidRule {
                expected: Rule::primitive_type,
                found: rule,
                rule_str: type_pair.as_str().to_string(),
            });
        }
    };
    if is_optional {
        Ok(TypeSpecifier::Optional(Box::new(base_type)))
    } else {
        Ok(base_type)
    }
}

fn parse_literal_value(pair: Pair<Rule>) -> Result<AnnotationValue, SsotParserError> {
    if pair.as_rule() != Rule::literal_value {
        return Err(SsotParserError::InvalidRule {
            expected: Rule::literal_value,
            found: pair.as_rule(),
            rule_str: pair.as_str().to_string(),
        });
    }
    let inner_pair = pair.into_inner().next().ok_or_else(|| {
        SsotParserError::AstConstructionError("Missing inner literal type".to_string())
    })?;
    match inner_pair.as_rule() {
        Rule::STRING_LITERAL => {
            let str_val = inner_pair.as_str();
            if str_val.len() >= 2 && str_val.starts_with('"') && str_val.ends_with('"') {
                Ok(AnnotationValue::String(
                    str_val[1..str_val.len() - 1].to_string(),
                ))
            } else {
                Err(SsotParserError::InvalidLiteralValue(
                    "Malformed string literal".to_string(),
                ))
            }
        }
        Rule::INTEGER_LITERAL => {
            let int_str = inner_pair.as_str();
            i64::from_str(int_str)
                .map(AnnotationValue::Integer)
                .map_err(|e| SsotParserError::InvalidIntLiteral(int_str.to_string(), e.to_string()))
        }
        Rule::FLOAT_LITERAL => Err(SsotParserError::AstConstructionError(
            "Float literal parsing not supported in AST AnnotationValue yet".to_string(),
        )),
        Rule::BOOLEAN_LITERAL => match inner_pair.as_str() {
            "true" => Ok(AnnotationValue::Boolean(true)),
            "false" => Ok(AnnotationValue::Boolean(false)),
            _ => Err(SsotParserError::InvalidBooleanLiteral(
                inner_pair.as_str().to_string(),
            )),
        },
        Rule::NULL_LITERAL => Err(SsotParserError::AstConstructionError(
            "Null literal parsing not supported in AST AnnotationValue yet".to_string(),
        )),
        Rule::DURATION_LITERAL => Err(SsotParserError::AstConstructionError(
            "Duration literal parsing not supported in AST AnnotationValue yet".to_string(),
        )),
        Rule::IDENTIFIER => Ok(AnnotationValue::String(inner_pair.as_str().to_string())),
        rule => Err(SsotParserError::InvalidRule {
            expected: Rule::STRING_LITERAL,
            found: rule,
            rule_str: inner_pair.as_str().to_string(),
        }),
    }
}

fn parse_initial_definition(pair: Pair<Rule>) -> Result<Identifier, SsotParserError> {
    if pair.as_rule() != Rule::initial_definition {
        return Err(SsotParserError::InvalidRule {
            expected: Rule::initial_definition,
            found: pair.as_rule(),
            rule_str: pair.as_str().to_string(),
        });
    }
    let identifier_pair = pair
        .into_inner()
        .find(|p| p.as_rule() == Rule::IDENTIFIER)
        .ok_or_else(|| {
            SsotParserError::MissingRule(Rule::IDENTIFIER, "in initial_definition".to_string())
        })?;
    parse_identifier(identifier_pair)
}

fn parse_state_definition(pair: Pair<Rule>) -> Result<StateDefinition, SsotParserError> {
    if pair.as_rule() != Rule::state_definition {
        return Err(SsotParserError::InvalidRule {
            expected: Rule::state_definition,
            found: pair.as_rule(),
            rule_str: pair.as_str().to_string(),
        });
    }
    let mut inner = pair.into_inner();

    let annotations: Vec<Annotation> = vec![];

    let type_or_name_pair = inner.next().ok_or_else(|| {
        SsotParserError::AstConstructionError("Missing state type/name".to_string())
    })?;

    let name = if type_or_name_pair.as_rule() == Rule::IDENTIFIER {
        parse_identifier(type_or_name_pair)?
    } else {
        let name_pair = inner.next().ok_or_else(|| {
            SsotParserError::MissingRule(Rule::IDENTIFIER, "in state definition name".to_string())
        })?;
        parse_identifier(name_pair)?
    };

    let mut transitions: Vec<TransitionDefinition> = vec![];
    let mut on_entry: Vec<Identifier> = vec![];
    let mut on_exit: Vec<Identifier> = vec![];
    let invokes: Vec<StateInvokeDefinition> = vec![];
    let history: Option<HistoryDefinition> = None;
    let regions: Vec<StatesBlock> = vec![];

    if let Some(body_pair) = inner.find(|p| p.as_rule() == Rule::state_body) {
        for element_pair in body_pair.into_inner() {
            match element_pair.as_rule() {
                Rule::state_definition => {
                    return Err(SsotParserError::AstConstructionError(
                        "Parsing for nested state_definition not yet implemented".to_string(),
                    ));
                }
                Rule::transition_definition => {
                    // Call the new transition parsing function
                    transitions.push(parse_transition_definition(element_pair)?);
                }
                Rule::on_entry => {
                    // Call the new action parsing function
                    on_entry.extend(parse_on_action(element_pair)?);
                }
                Rule::on_exit => {
                    // Call the new action parsing function
                    on_exit.extend(parse_on_action(element_pair)?);
                }
                Rule::state_invoke => {
                    // TODO: Implement parse_state_invoke
                    return Err(SsotParserError::AstConstructionError(
                        "Parsing for state_invoke not yet implemented".to_string(),
                    ));
                }
                Rule::history_definition => {
                    // TODO: Implement parse_history_definition
                    return Err(SsotParserError::AstConstructionError(
                        "Parsing for history_definition not yet implemented".to_string(),
                    ));
                }
                Rule::actions_block => {
                    return Err(SsotParserError::AstConstructionError(
                        "Parsing for actions_block inside state not yet implemented".to_string(),
                    ));
                }
                Rule::guards_block => {
                    return Err(SsotParserError::AstConstructionError(
                        "Parsing for guards_block inside state not yet implemented".to_string(),
                    ));
                }
                Rule::invokes_block => {
                    return Err(SsotParserError::AstConstructionError(
                        "Parsing for invokes_block inside state not yet implemented".to_string(),
                    ));
                }
                Rule::COMMENT | Rule::WHITESPACE | Rule::annotation => { /* Skip */ }
                _ => {
                    return Err(SsotParserError::AstConstructionError(format!(
                        "Unexpected rule {:?} inside state_body",
                        element_pair.as_rule()
                    )));
                }
            }
        }
    }

    let is_initial = false;
    let is_final = false;
    let is_parallel = false;

    Ok(StateDefinition {
        name,
        id: NumericId { value: 0 },
        annotations,
        transitions,
        invokes,
        on_entry,
        on_exit,
        after_transitions: vec![],
        history,
        regions,
        is_initial,
        is_final,
        is_parallel,
    })
}

// Added parse_transition_definition placeholder
fn parse_transition_definition(pair: Pair<Rule>) -> Result<TransitionDefinition, SsotParserError> {
    if pair.as_rule() != Rule::transition_definition {
        return Err(SsotParserError::InvalidRule {
            expected: Rule::transition_definition,
            found: pair.as_rule(),
            rule_str: pair.as_str().to_string(),
        });
    }

    // transition_definition = { (IDENTIFIER | STRING_LITERAL) ~ ("->" ~ IDENTIFIER)? ~ ("[" ~ IDENTIFIER ~ "]")? ~ ("/" ~ IDENTIFIER)? ~ ";"? }
    let mut inner = pair.into_inner();

    // --- Parse Event --- (Mandatory)
    let event_pair = inner.next().ok_or_else(|| {
        SsotParserError::MissingRule(Rule::IDENTIFIER, "in transition event".to_string())
        // Or STRING_LITERAL
    })?;
    let event = match event_pair.as_rule() {
        Rule::IDENTIFIER => parse_identifier(event_pair)?,
        Rule::STRING_LITERAL => {
            // Represent string literal event as Identifier for now? AST uses Identifier.
            let str_val = event_pair.as_str();
            if str_val.len() >= 2 && str_val.starts_with('"') && str_val.ends_with('"') {
                Identifier {
                    name: str_val[1..str_val.len() - 1].to_string(),
                }
            } else {
                return Err(SsotParserError::InvalidLiteralValue(
                    "Malformed string literal event".to_string(),
                ));
            }
        }
        rule => {
            return Err(SsotParserError::InvalidRule {
                expected: Rule::IDENTIFIER, // Or STRING_LITERAL
                found: rule,
                rule_str: event_pair.as_str().to_string(),
            })
        }
    };

    // --- Parse Optional Target, Guard, Action --- //
    let mut target: Option<TransitionTarget> = None;
    let mut guard: Option<Identifier> = None;
    let mut actions: Vec<Identifier> = vec![];

    for part_pair in inner {
        match part_pair.as_rule() {
            // Target: "->" ~ IDENTIFIER
            Rule::IDENTIFIER if target.is_none() => {
                // This assumes IDENTIFIER after event MUST be the target if present.
                // Grammar might need adjustment for clarity (e.g., explicit target rule).
                target = Some(TransitionTarget::State(parse_identifier(part_pair)?));
            }
            // Guard: "[" ~ IDENTIFIER ~ "]"
            Rule::IDENTIFIER if target.is_some() && guard.is_none() => {
                // This assumes IDENTIFIER after target MUST be the guard if present.
                // Grammar is ambiguous: ("[" ~ IDENTIFIER ~ "]")?
                // Need a specific rule for guard content.
                // For now, assuming this IDENTIFIER is the guard.
                guard = Some(parse_identifier(part_pair)?);
                // TODO: Fix grammar/parsing for explicit guard structure
            }
            // Action: "/" ~ IDENTIFIER
            Rule::IDENTIFIER if target.is_some() && guard.is_some() => {
                // This assumes IDENTIFIER after target and guard must be the action.
                // Grammar is ambiguous: ("/" ~ IDENTIFIER)?
                // Need a specific rule for action content.
                // For now, assuming this IDENTIFIER is the action.
                actions.push(parse_identifier(part_pair)?);
                // TODO: Fix grammar/parsing for explicit action structure
            }
            Rule::COMMENT | Rule::WHITESPACE => { /* Skip */ }
            // Handle semicolon or unexpected rules
            _ => { /* Maybe log warning or ignore */ }
        }
    }

    // --- Construct TransitionDefinition --- //
    // Default to internal transition if no target specified?
    let final_target = target.unwrap_or(TransitionTarget::State(Identifier {
        name: "#".to_string(),
    })); // Placeholder for internal?

    Ok(TransitionDefinition {
        event,
        target: final_target,
        id: NumericId { value: 0 },                // Placeholder
        annotations: vec![],                       // Placeholder
        actions, // Assign parsed actions (currently only one possible)
        guards: guard.map_or(vec![], |g| vec![g]), // Convert Option<Id> to Vec<Id>
    })
}

// Updated parse_on_action helper for on_entry / on_exit
fn parse_on_action(pair: Pair<Rule>) -> Result<Vec<Identifier>, SsotParserError> {
    if pair.as_rule() != Rule::on_entry && pair.as_rule() != Rule::on_exit {
        return Err(SsotParserError::InvalidRule {
            expected: Rule::on_entry, // or on_exit
            found: pair.as_rule(),
            rule_str: pair.as_str().to_string(),
        });
    }
    // Grammar:
    // on_entry = { "entry" ~ WHITESPACE* ~ action_block ~ ";" }
    // on_exit = { "exit" ~ WHITESPACE* ~ action_block ~ ";" }
    // action_block = { IDENTIFIER | "{" ~ any_content ~ "}" }

    // Store rule name before moving pair
    let rule_name_str = format!("{:?}", pair.as_rule());

    // Find the inner action_block
    let action_block_pair = pair
        .into_inner()
        .find(|p| p.as_rule() == Rule::action_block)
        .ok_or_else(|| {
            // Use the stored rule name string here
            SsotParserError::MissingRule(Rule::action_block, format!("inside {}", rule_name_str))
        })?;

    // Check what the action_block contains
    let inner_action = action_block_pair
        .into_inner()
        .next()
        .ok_or_else(|| SsotParserError::AstConstructionError("Empty action_block".to_string()))?;

    match inner_action.as_rule() {
        Rule::IDENTIFIER => {
            // Single action identifier
            Ok(vec![parse_identifier(inner_action)?])
        }
        Rule::any_content => {
            // Block actions { ... } - Grammar uses `any_content`
            // TODO: Implement parsing of actions within a block.
            // This requires a more specific grammar than `any_content`.
            // For now, return empty or error.
            Err(SsotParserError::AstConstructionError(
                "Parsing block actions requires a more specific grammar than 'any_content'"
                    .to_string(),
            ))
        }
        rule => Err(SsotParserError::InvalidRule {
            expected: Rule::IDENTIFIER, // or the rule for the block if grammar changes
            found: rule,
            rule_str: inner_action.as_str().to_string(),
        }),
    }
}

// TODO: Implement functions like:
// fn parse_final_states_definition(...) -> Result<Vec<Identifier>, SsotParserError> { ... }
// fn parse_state_invoke(...) -> Result<StateInvokeDefinition, SsotParserError> { ... }
// ... and so on
