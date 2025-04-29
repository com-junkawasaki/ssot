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
    PestError(#[from] Box<pest::error::Error<Rule>>),
    #[error("AST construction error: {0}")]
    AstConstructionError(String),
    #[error("Invalid rule encountered: expected {expected:?}, found {found:?}. Rule: {rule_str}")]
    InvalidRule {
        expected: Rule,
        found: Rule,
        rule_str: String,
        span: Option<SpanInfo>,
    },
    #[error("Missing expected rule: {0:?}. Context: {1}")]
    MissingRule(Rule, String, Option<SpanInfo>),
    #[error("Invalid identifier: {0}")]
    InvalidIdentifier(String, Option<SpanInfo>),
    #[error("Invalid integer literal: {0} - {1}")]
    InvalidIntLiteral(String, String, Option<SpanInfo>),
    #[error("Invalid float literal: {0} - {1}")]
    InvalidFloatLiteral(String, String, Option<SpanInfo>),
    #[error("Invalid boolean literal: {0}")]
    InvalidBooleanLiteral(String, Option<SpanInfo>),
    #[error("Invalid null literal: {0}")]
    InvalidNullLiteral(String, Option<SpanInfo>),
    #[error("Invalid duration literal: {0}")]
    InvalidDurationLiteral(String, Option<SpanInfo>),
    #[error("Unknown primitive type: {0}")]
    UnknownPrimitiveType(String, Option<SpanInfo>),
    #[error("Invalid type reference: {0}")]
    InvalidTypeRef(String, Option<SpanInfo>),
    #[error("Invalid annotation format: {0}")]
    InvalidAnnotation(String, Option<SpanInfo>),
    #[error("Invalid literal value: {0}")]
    InvalidLiteralValue(String, Option<SpanInfo>),
    #[error("Invalid state type modifier: {0}")]
    InvalidStateType(String, Option<SpanInfo>),
    #[error("Invalid history type: {0}")]
    InvalidHistoryType(String, Option<SpanInfo>),
    #[error("Expected exactly one inner pair for rule {0:?}, found {1}")]
    UnexpectedInnerPairCount(Rule, usize, Option<SpanInfo>),
    #[error("Failed to parse import path: {0}")]
    InvalidImportPath(String, Option<SpanInfo>),
    #[error("Invalid numeric ID format: {0} - {1}")]
    InvalidNumericId(String, String, Option<SpanInfo>),
    #[error("Missing required element: {0}")]
    MissingElement(String, Option<SpanInfo>),
}

// Helper function to create SpanInfo from pest::Span
fn span_info_from_pest(span: pest::Span) -> SpanInfo {
    SpanInfo {
        start: span.start(),
        end: span.end(),
    }
}

#[derive(Parser)]
#[grammar = "grammar.pest"]
pub struct SsotParser;

// --- Main Parsing Function ---
pub fn parse_ssot_content(
    content: &str,
    source_path: Option<PathBuf>,
) -> Result<SsotAst, SsotParserError> {
    let pairs = SsotParser::parse(Rule::ssot_entry_rule, content).map_err(Box::new)?;

    let file_pair = pairs.peek().ok_or_else(|| {
        SsotParserError::AstConstructionError("Expected 'file' rule, found nothing".to_string())
    })?;

    if file_pair.as_rule() != Rule::file {
        return Err(SsotParserError::InvalidRule {
            expected: Rule::file,
            found: file_pair.as_rule(),
            rule_str: file_pair.as_str().to_string(),
            span: Some(span_info_from_pest(file_pair.as_span())),
        });
    }

    let mut imports = Vec::new();
    let mut definitions = Vec::new();
    let file_id = None; // TODO: Handle file ID (@...) if needed

    for pair in file_pair.into_inner() {
        match pair.as_rule() {
            Rule::import_statement => {
                imports.push(parse_import_statement(pair)?);
            }
            // Directly match the specific block rules produced by the silent top_level_block
            Rule::types_block
            | Rule::machines_block
            | Rule::services_block
            | Rule::communication_block
            | Rule::actors_block
            | Rule::deployment_config_block => {
                // Pass the specific block pair to parse_top_level_block
                // which will then delegate based on the rule.
                definitions.push(parse_top_level_block_wrapper(pair)?);
            }
            Rule::COMMENT | Rule::WHITESPACE => { /* Skip */ }
            Rule::EOI => { /* End of Input, expected */ }
            _ => {
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

// Wrapper function to handle delegation based on the actual block rule found
fn parse_top_level_block_wrapper(
    block_pair: Pair<Rule>,
) -> Result<TopLevelDefinition, SsotParserError> {
    // Since top_level_block rule was silent, annotations are not directly attached here.
    // We assume block-level annotations aren't the primary focus yet or need grammar adjustment.
    let annotations = vec![]; // Assume no block annotations for now

    match block_pair.as_rule() {
        Rule::types_block => Ok(TopLevelDefinition::Types(parse_types_block(
            block_pair,
            annotations,
        )?)),
        Rule::machines_block => Ok(TopLevelDefinition::Machines(parse_machines_block(
            block_pair,
            annotations,
        )?)),
        Rule::services_block => Ok(TopLevelDefinition::Services(parse_services_block(
            block_pair,
            annotations,
        )?)),
        Rule::communication_block => Ok(TopLevelDefinition::Communication(
            parse_communication_block(block_pair, annotations)?,
        )),
        Rule::actors_block => Ok(TopLevelDefinition::Actors(parse_actors_block(
            block_pair,
            annotations,
        )?)),
        Rule::deployment_config_block => Ok(TopLevelDefinition::DeploymentConfig(
            parse_deployment_config_block(block_pair, annotations)?,
        )),
        // This case should ideally not be reached if the main loop logic is correct
        _ => Err(SsotParserError::AstConstructionError(format!(
            "Unexpected rule {:?} passed to parse_top_level_block_wrapper",
            block_pair.as_rule()
        ))),
    }
}

// --- Helper Parsing Functions ---

fn parse_identifier(pair: Pair<Rule>) -> Result<Identifier, SsotParserError> {
    if pair.as_rule() != Rule::IDENTIFIER {
        return Err(SsotParserError::InvalidRule {
            expected: Rule::IDENTIFIER,
            found: pair.as_rule(),
            rule_str: pair.as_str().to_string(),
            span: Some(span_info_from_pest(pair.as_span())),
        });
    }
    let span = pair.as_span();
    Ok(Identifier {
        name: pair.as_str().to_string(),
        span: Some(SpanInfo {
            start: span.start(),
            end: span.end(),
        }), // Capture span
    })
}

fn parse_numeric_id(pair: Pair<Rule>) -> Result<NumericId, SsotParserError> {
    if pair.as_rule() != Rule::numeric_id {
        return Err(SsotParserError::InvalidRule {
            expected: Rule::numeric_id,
            found: pair.as_rule(),
            rule_str: pair.as_str().to_string(),
            span: Some(span_info_from_pest(pair.as_span())),
        });
    }
    let outer_span = pair.as_span(); // Span of the whole @id(...) rule
    let inner_pair = pair.into_inner().next().ok_or_else(|| {
        SsotParserError::MissingRule(Rule::INTEGER_LITERAL, "in numeric_id".to_string(), None)
    })?;
    let value_str = inner_pair.as_str();
    let value = value_str.parse::<u64>().map_err(|e| {
        SsotParserError::InvalidNumericId(value_str.to_string(), e.to_string(), None)
    })?;
    Ok(NumericId {
        value,
        span: Some(SpanInfo {
            start: outer_span.start(),
            end: outer_span.end(),
        }),
    }) // Capture span
}

fn parse_optional_numeric_id(
    pairs: &mut Pairs<Rule>,
    rule_to_find: Rule,
) -> Result<Option<NumericId>, SsotParserError> {
    if let Some(pair) = pairs.peek() {
        if pair.as_rule() == rule_to_find {
            // Use .clone() on the pair before calling next() to avoid moving from the iterator peek references
            return Ok(Some(parse_numeric_id(pairs.next().unwrap())?));
        }
    }
    Ok(None)
}

fn parse_import_statement(pair: Pair<Rule>) -> Result<ImportStatement, SsotParserError> {
    if pair.as_rule() != Rule::import_statement {
        return Err(SsotParserError::InvalidRule {
            expected: Rule::import_statement,
            found: pair.as_rule(),
            rule_str: pair.as_str().to_string(),
            span: Some(span_info_from_pest(pair.as_span())),
        });
    }

    // Find the STRING_LITERAL which represents the path
    let path_pair = pair
        .into_inner()
        .find(|p| p.as_rule() == Rule::STRING_LITERAL)
        .ok_or_else(|| {
            SsotParserError::MissingRule(
                Rule::STRING_LITERAL,
                "in import statement".to_string(),
                None,
            )
        })?;

    // Extract the string content, removing quotes
    let path_str = path_pair.as_str();
    if path_str.len() >= 2 && path_str.starts_with('"') && path_str.ends_with('"') {
        let path = path_str[1..path_str.len() - 1].to_string();
        // TODO: Handle escape sequences if needed
        Ok(ImportStatement { path })
    } else {
        Err(SsotParserError::InvalidImportPath(
            path_str.to_string(),
            None,
        ))
    }
}

// --- Annotation Argument Parsing Helpers ---

fn parse_annotation_args(pair: Pair<Rule>) -> Result<Vec<Argument>, SsotParserError> {
    if pair.as_rule() != Rule::annotation_args {
        return Err(SsotParserError::InvalidRule {
            expected: Rule::annotation_args,
            found: pair.as_rule(),
            rule_str: pair.as_str().to_string(),
            span: Some(span_info_from_pest(pair.as_span())),
        });
    }
    let mut args = Vec::new();
    for arg_pair in pair.into_inner() {
        if arg_pair.as_rule() == Rule::annotation_arg {
            args.push(parse_annotation_arg(arg_pair)?);
        }
    }
    Ok(args)
}

fn parse_annotation_arg(pair: Pair<Rule>) -> Result<Argument, SsotParserError> {
    if pair.as_rule() != Rule::annotation_arg {
        return Err(SsotParserError::InvalidRule {
            expected: Rule::annotation_arg,
            found: pair.as_rule(),
            rule_str: pair.as_str().to_string(),
            span: Some(span_info_from_pest(pair.as_span())),
        });
    }
    let mut inner = pair.into_inner();
    let key_pair = inner.next().unwrap(); // Must have at least value

    // Check if the first part is an IDENTIFIER (which implies a key)
    if key_pair.as_rule() == Rule::IDENTIFIER {
        let key = parse_identifier(key_pair)?;
        // The next part must be the value
        let value_pair = inner.next().ok_or_else(|| {
            SsotParserError::MissingRule(
                Rule::annotation_value,
                "after annotation key".to_string(),
                None,
            )
        })?;
        let value = parse_annotation_value(value_pair)?;
        Ok(Argument { key, value })
    } else {
        // If the first part wasn't an IDENTIFIER, it must be the value itself (unkeyed argument)
        // We need a way to represent unkeyed arguments, perhaps using a default key?
        // For now, let's error or use a dummy key.
        // Using a dummy key "_" for unkeyed arguments.
        let key = Identifier {
            name: "_".to_string(),
        };
        let value = parse_annotation_value(key_pair)?; // The first pair was the value
        Ok(Argument { key, value })
        // OR return Err(SsotParserError::InvalidAnnotation(
        //     "Found unkeyed annotation argument where key was expected".to_string(),
        // ));
    }
}

fn parse_annotation_value(pair: Pair<Rule>) -> Result<AnnotationValue, SsotParserError> {
    if pair.as_rule() != Rule::annotation_value {
        return Err(SsotParserError::InvalidRule {
            expected: Rule::annotation_value,
            found: pair.as_rule(),
            rule_str: pair.as_str().to_string(),
            span: Some(span_info_from_pest(pair.as_span())),
        });
    }
    let inner = pair.into_inner().next().unwrap(); // Should have one inner rule
    match inner.as_rule() {
        Rule::literal_value => parse_literal_value(inner),
        Rule::list_literal => parse_list_literal(inner),
        Rule::object_literal => parse_object_literal(inner),
        _ => Err(SsotParserError::AstConstructionError(format!(
            "Unexpected rule {:?} inside annotation_value",
            inner.as_rule()
        ))),
    }
}

fn parse_list_literal(pair: Pair<Rule>) -> Result<AnnotationValue, SsotParserError> {
    if pair.as_rule() != Rule::list_literal {
        return Err(SsotParserError::InvalidRule {
            expected: Rule::list_literal,
            found: pair.as_rule(),
            rule_str: pair.as_str().to_string(),
            span: Some(span_info_from_pest(pair.as_span())),
        });
    }
    let mut values = Vec::new();
    for value_pair in pair.into_inner() {
        // value_pair here is actually annotation_value rule
        if value_pair.as_rule() == Rule::annotation_value {
            values.push(parse_annotation_value(value_pair)?);
        }
    }
    Ok(AnnotationValue::List(values))
}

fn parse_object_literal(pair: Pair<Rule>) -> Result<AnnotationValue, SsotParserError> {
    if pair.as_rule() != Rule::object_literal {
        return Err(SsotParserError::InvalidRule {
            expected: Rule::object_literal,
            found: pair.as_rule(),
            rule_str: pair.as_str().to_string(),
            span: Some(span_info_from_pest(pair.as_span())),
        });
    }
    let mut args = Vec::new();
    for arg_pair_outer in pair.into_inner() {
        // arg_pair_outer here is argument_pair rule
        if arg_pair_outer.as_rule() == Rule::argument_pair {
            let mut inner = arg_pair_outer.into_inner();
            let key = parse_identifier(inner.next().unwrap())?;
            let value = parse_annotation_value(inner.next().unwrap())?;
            args.push(Argument { key, value });
        }
    }
    Ok(AnnotationValue::Object(args))
}

fn parse_literal_value(pair: Pair<Rule>) -> Result<AnnotationValue, SsotParserError> {
    if pair.as_rule() != Rule::literal_value {
        return Err(SsotParserError::InvalidRule {
            expected: Rule::literal_value,
            found: pair.as_rule(),
            rule_str: pair.as_str().to_string(),
            span: Some(span_info_from_pest(pair.as_span())),
        });
    }
    let inner = pair.into_inner().next().unwrap(); // Should have one inner literal rule
    match inner.as_rule() {
        Rule::STRING_LITERAL => {
            let s = inner.as_str();
            Ok(AnnotationValue::String(s[1..s.len() - 1].to_string())) // Remove quotes
        }
        Rule::INTEGER_LITERAL => {
            let value_str = inner.as_str();
            let value = value_str.parse::<i64>().map_err(|e| {
                SsotParserError::InvalidIntLiteral(value_str.to_string(), e.to_string(), None)
            })?;
            Ok(AnnotationValue::Integer(value))
        }
        Rule::BOOLEAN_LITERAL => {
            let value = inner.as_str().parse::<bool>().map_err(|_|
                // This should not happen if grammar is correct
                SsotParserError::InvalidBooleanLiteral(inner.as_str().to_string(), None))?;
            Ok(AnnotationValue::Boolean(value))
        }
        Rule::IDENTIFIER => {
            // Treat bare identifiers as strings for now, could represent enums/constants
            Ok(AnnotationValue::String(inner.as_str().to_string()))
        }
        // TODO: Add FLOAT_LITERAL, DURATION_LITERAL, NULL_LITERAL if needed
        _ => Err(SsotParserError::InvalidLiteralValue(
            inner.as_str().to_string(),
            None,
        )),
    }
}

// TODO: Restore or ensure parse_annotations exists correctly
fn parse_annotations(pairs: &mut Pairs<Rule>) -> Result<Vec<Annotation>, SsotParserError> {
    let mut annotations = Vec::new();
    while let Some(pair) = pairs.peek() {
        if pair.as_rule() == Rule::annotation {
            annotations.push(parse_annotation(pairs.next().unwrap())?); // Ensure parse_annotation exists
        } else {
            break;
        }
    }
    Ok(annotations)
}

// TODO: Ensure parse_annotation exists and is correct
fn parse_annotation(pair: Pair<Rule>) -> Result<Annotation, SsotParserError> {
    if pair.as_rule() != Rule::annotation {
        return Err(SsotParserError::InvalidRule {
            expected: Rule::annotation,
            found: pair.as_rule(),
            rule_str: pair.as_str().to_string(),
            span: Some(span_info_from_pest(pair.as_span())),
        });
    }
    let mut inner = pair.into_inner();
    let name_pair = inner.next().ok_or_else(|| {
        SsotParserError::MissingRule(Rule::IDENTIFIER, "in annotation".to_string(), None)
    })?;
    let name = name_pair.as_str();

    let args_pair = inner.next(); // Optional annotation_args

    // Parse arguments if they exist
    let args = if let Some(pair) = args_pair {
        parse_annotation_args(pair)?
    } else {
        Vec::new() // No arguments
    };

    match name {
        "description" => {
            // Expect exactly one unkeyed string literal argument
            if args.len() == 1 && args[0].key.name == "_" {
                if let AnnotationValue::String(value) = &args[0].value {
                    Ok(Annotation::Description(value.clone()))
                } else {
                    Err(SsotParserError::InvalidAnnotation(
                        "$description expects a single string literal argument".to_string(),
                        None,
                    ))
                }
            } else {
                Err(SsotParserError::InvalidAnnotation(
                    "$description expects a single string literal argument".to_string(),
                    None,
                ))
            }
        }
        "validate" => Ok(Annotation::Validate(args)),
        "db" => Ok(Annotation::Db(args)),
        "meta" => Ok(Annotation::Meta(args)),
        "initial" => {
            if args.is_empty() {
                Ok(Annotation::Initial) // $initial;
            } else if args.len() == 1 && args[0].key.name == "_" {
                // $initial(StateName) - Expects unkeyed IDENTIFIER treated as String
                if let AnnotationValue::String(state_name) = &args[0].value {
                    Ok(Annotation::InitialState(Identifier {
                        name: state_name.clone(),
                    }))
                } else {
                    Err(SsotParserError::InvalidAnnotation(
                        "$initial(StateName) expects a state name identifier".to_string(),
                        None,
                    ))
                }
            } else {
                Err(SsotParserError::InvalidAnnotation(
                    "Invalid arguments for $initial. Use $initial; or $initial(StateName)"
                        .to_string(),
                    None,
                ))
            }
        }
        "final" if args.is_empty() => Ok(Annotation::Final),
        "parallel" if args.is_empty() => Ok(Annotation::Parallel),
        "implements" => {
            // Expects single unkeyed Identifier $implements(InterfaceName)
            if args.len() == 1 && args[0].key.name == "_" {
                if let AnnotationValue::String(iface_name) = &args[0].value {
                    Ok(Annotation::Implements(Identifier {
                        name: iface_name.clone(),
                    }))
                } else {
                    Err(SsotParserError::InvalidAnnotation(
                        "$implements expects an interface name".to_string(),
                        None,
                    ))
                }
            } else {
                Err(SsotParserError::InvalidAnnotation(
                    "$implements expects a single interface name argument".to_string(),
                    None,
                ))
            }
        }
        "protocol" => {
            // Expects single unkeyed Identifier $protocol(ProtoName)
            if args.len() == 1 && args[0].key.name == "_" {
                if let AnnotationValue::String(proto_name) = &args[0].value {
                    Ok(Annotation::Protocol(Identifier {
                        name: proto_name.clone(),
                    }))
                } else {
                    Err(SsotParserError::InvalidAnnotation(
                        "$protocol expects a protocol name".to_string(),
                        None,
                    ))
                }
            } else {
                Err(SsotParserError::InvalidAnnotation(
                    "$protocol expects a single protocol name argument".to_string(),
                    None,
                ))
            }
        }
        "communicatesWith" => {
            // $communicatesWith(ServiceName using ProtocolName)
            // This is tricky with the current generic arg parser. Assume specific format or require key-value.
            // Let's assume key-value for now: $communicatesWith(service: ServiceName, protocol: ProtocolName)
            if args.len() == 2 {
                let service_arg = args.iter().find(|a| a.key.name == "service");
                let protocol_arg = args.iter().find(|a| a.key.name == "protocol");
                if let (Some(s_arg), Some(p_arg)) = (service_arg, protocol_arg) {
                    if let (AnnotationValue::String(s_name), AnnotationValue::String(p_name)) =
                        (&s_arg.value, &p_arg.value)
                    {
                        Ok(Annotation::CommunicatesWith(CommunicatesWithArgs {
                            service: Identifier {
                                name: s_name.clone(),
                            },
                            protocol: Identifier {
                                name: p_name.clone(),
                            },
                        }))
                    } else {
                        Err(SsotParserError::InvalidAnnotation(
                            "$communicatesWith requires service and protocol names as strings"
                                .to_string(),
                            None,
                        ))
                    }
                } else {
                    Err(SsotParserError::InvalidAnnotation(
                        "$communicatesWith requires 'service:' and 'protocol:' arguments"
                            .to_string(),
                        None,
                    ))
                }
            } else {
                Err(SsotParserError::InvalidAnnotation(
                    "$communicatesWith expects exactly two arguments: service and protocol"
                        .to_string(),
                    None,
                ))
            }
        }
        "publishes" | "subscribes" | "channel" => {
            // Expects single unkeyed Identifier $publishes(ChannelName)
            if args.len() == 1 && args[0].key.name == "_" {
                if let AnnotationValue::String(channel_name) = &args[0].value {
                    let ident = Identifier {
                        name: channel_name.clone(),
                    };
                    match name {
                        "publishes" => Ok(Annotation::Publishes(ident)),
                        "subscribes" => Ok(Annotation::Subscribes(ident)),
                        "channel" => Ok(Annotation::Channel(ident)),
                        _ => unreachable!(),
                    }
                } else {
                    Err(SsotParserError::InvalidAnnotation(
                        format!("${} expects a channel name", name),
                        None,
                    ))
                }
            } else {
                Err(SsotParserError::InvalidAnnotation(
                    format!("${} expects a single channel name argument", name),
                    None,
                ))
            }
        }
        "route" => Ok(Annotation::Route(args)),
        "allowedActors" => {
            // Expects $allowedActors(Actor) or $allowedActors([Actor1, Actor2])
            if args.len() == 1 && args[0].key.name == "_" {
                match &args[0].value {
                    AnnotationValue::String(actor_name) => {
                        Ok(Annotation::AllowedActors(vec![Identifier {
                            name: actor_name.clone(),
                        }]))
                    }
                    AnnotationValue::List(actor_list) => {
                        let mut actors = Vec::new();
                        for val in actor_list {
                            if let AnnotationValue::String(actor_name) = val {
                                actors.push(Identifier {
                                    name: actor_name.clone(),
                                });
                            } else {
                                return Err(SsotParserError::InvalidAnnotation(
                                    "$allowedActors list must contain only actor names".to_string(),
                                    None,
                                ));
                            }
                        }
                        Ok(Annotation::AllowedActors(actors))
                    }
                    _ => Err(SsotParserError::InvalidAnnotation(
                        "$allowedActors expects an actor name or a list of actor names".to_string(),
                        None,
                    )),
                }
            } else {
                Err(SsotParserError::InvalidAnnotation(
                    "$allowedActors expects a single argument (actor name or list)".to_string(),
                    None,
                ))
            }
        }
        // Handle generic output directives $xxx_out("path")
        name if name.ends_with("_out") => {
            if args.len() == 1 && args[0].key.name == "_" {
                if let AnnotationValue::String(path) = &args[0].value {
                    Ok(Annotation::OutputDirective {
                        directive: name.to_string(),
                        path: path.clone(),
                    })
                } else {
                    Err(SsotParserError::InvalidAnnotation(
                        format!("${} directive expects a single string path argument", name),
                        None,
                    ))
                }
            } else {
                Err(SsotParserError::InvalidAnnotation(
                    format!("${} directive expects a single string path argument", name),
                    None,
                ))
            }
        }
        // Generic flags (no args) and KeyValue (single simple arg)
        _ if args.is_empty() => Ok(Annotation::GenericFlag(Identifier { name: name.into() })),
        _ if args.len() == 1 && args[0].key.name == "_" => {
            // For generic key-value like $someKey("value"), treat value as string for now
            match &args[0].value {
                AnnotationValue::String(s) => Ok(Annotation::GenericKeyValue(
                    Identifier { name: name.into() },
                    s.clone(),
                )),
                AnnotationValue::Integer(i) => Ok(Annotation::GenericKeyValue(
                    Identifier { name: name.into() },
                    i.to_string(),
                )),
                AnnotationValue::Boolean(b) => Ok(Annotation::GenericKeyValue(
                    Identifier { name: name.into() },
                    b.to_string(),
                )),
                _ => Err(SsotParserError::InvalidAnnotation(
                    format!(
                        "Generic annotation ${} has unsupported complex value type",
                        name
                    ),
                    None,
                )),
            }
        }
        // Catch-all for invalid argument structures for known annotations or unknown annotations with args
        _ => Err(SsotParserError::InvalidAnnotation(
            format!("Unsupported or invalid arguments for annotation ${}", name),
            None,
        )),
    }
}

fn parse_types_block(
    pair: Pair<Rule>,
    block_annotations: Vec<Annotation>,
) -> Result<TypesBlock, SsotParserError> {
    let mut definitions = Vec::new();
    let mut current_annotations = Vec::new();
    for inner_pair in pair.into_inner() {
        match inner_pair.as_rule() {
            Rule::annotation => current_annotations.push(parse_annotation(inner_pair)?),
            Rule::struct_definition => {
                // Correct rule
                definitions.push(TypeDefinition::Struct(parse_struct_definition(
                    inner_pair,
                    current_annotations,
                )?));
                current_annotations = Vec::new();
            }
            Rule::enum_definition => {
                // Correct rule
                definitions.push(TypeDefinition::Enum(parse_enum_definition(
                    inner_pair,
                    current_annotations,
                )?));
                current_annotations = Vec::new();
            }
            Rule::COMMENT | Rule::WHITESPACE => { /* Skip */ }
            _ => {
                if !current_annotations.is_empty() {
                    return Err(SsotParserError::AstConstructionError(format!(
                        "Annotations found but not attached to a definition: {:?}",
                        current_annotations
                    )));
                }
                return Err(SsotParserError::AstConstructionError(format!(
                    "Unexpected rule {:?} inside types_block",
                    inner_pair.as_rule()
                )));
            }
        }
    }
    if !current_annotations.is_empty() {
        return Err(SsotParserError::AstConstructionError(
            "Trailing annotations found at end of types block".to_string(),
        ));
    }
    Ok(TypesBlock {
        definitions,
        annotations: block_annotations,
    })
}

// TODO: Ensure parse_struct_definition exists and takes annotations
fn parse_struct_definition(
    pair: Pair<Rule>,
    struct_annotations: Vec<Annotation>,
) -> Result<StructDefinition, SsotParserError> {
    let mut inner_pairs = pair.into_inner();
    let name = parse_identifier(inner_pairs.next().unwrap())?;
    let id = parse_optional_numeric_id(&mut inner_pairs, Rule::numeric_id)?.ok_or_else(|| {
        SsotParserError::MissingElement("Numeric ID for struct".to_string(), None)
    })?;
    let mut fields = Vec::new();
    let mut field_annotations = Vec::new();
    for field_pair in inner_pairs {
        match field_pair.as_rule() {
            Rule::annotation => field_annotations.push(parse_annotation(field_pair)?),
            Rule::field_definition => {
                fields.push(parse_field_definition(field_pair, field_annotations)?);
                field_annotations = Vec::new();
            }
            Rule::COMMENT | Rule::WHITESPACE => { /* Skip */ }
            _ => {
                return Err(SsotParserError::AstConstructionError(format!(
                    "Unexpected rule {:?} inside struct_definition",
                    field_pair.as_rule()
                )));
            }
        }
    }
    Ok(StructDefinition {
        name,
        id,
        fields,
        annotations: struct_annotations,
    })
}

// TODO: Ensure parse_enum_definition exists and takes annotations
fn parse_enum_definition(
    pair: Pair<Rule>,
    enum_annotations: Vec<Annotation>,
) -> Result<EnumDefinition, SsotParserError> {
    let mut inner_pairs = pair.into_inner();
    let name = parse_identifier(inner_pairs.next().unwrap())?;
    let id = parse_optional_numeric_id(&mut inner_pairs, Rule::numeric_id)?
        .ok_or_else(|| SsotParserError::MissingElement("Numeric ID for enum".to_string(), None))?;
    let mut variants = Vec::new();
    let mut variant_annotations = Vec::new();
    for variant_pair in inner_pairs {
        match variant_pair.as_rule() {
            Rule::annotation => variant_annotations.push(parse_annotation(variant_pair)?),
            Rule::enum_variant => {
                variants.push(parse_enum_variant(variant_pair, variant_annotations)?);
                variant_annotations = Vec::new();
            }
            Rule::COMMENT | Rule::WHITESPACE => { /* Skip */ }
            _ => {
                return Err(SsotParserError::AstConstructionError(format!(
                    "Unexpected rule {:?} inside enum_definition",
                    variant_pair.as_rule()
                )));
            }
        }
    }
    Ok(EnumDefinition {
        name,
        id,
        variants,
        annotations: enum_annotations,
    })
}

fn parse_machines_block(
    pair: Pair<Rule>,
    block_annotations: Vec<Annotation>,
) -> Result<MachinesBlock, SsotParserError> {
    let mut definitions = Vec::new();
    let mut current_annotations = Vec::new();
    for inner_pair in pair.into_inner() {
        match inner_pair.as_rule() {
            Rule::annotation => current_annotations.push(parse_annotation(inner_pair)?),
            Rule::machine_definition => {
                // Correct rule
                definitions.push(parse_machine_definition(inner_pair, current_annotations)?);
                current_annotations = Vec::new();
            }
            Rule::COMMENT | Rule::WHITESPACE => { /* Skip */ }
            _ => {
                if !current_annotations.is_empty() {
                    return Err(SsotParserError::AstConstructionError(format!(
                        "Annotations found but not attached to a definition: {:?}",
                        current_annotations
                    )));
                }
                return Err(SsotParserError::AstConstructionError(format!(
                    "Unexpected rule {:?} inside machines_block",
                    inner_pair.as_rule()
                )));
            }
        }
    }
    if !current_annotations.is_empty() {
        return Err(SsotParserError::AstConstructionError(
            "Trailing annotations found at end of machines block".to_string(),
        ));
    }
    Ok(MachinesBlock {
        definitions,
        annotations: block_annotations,
    })
}

// Update signature to accept annotations
fn parse_machine_definition(
    pair: Pair<Rule>,
    machine_annotations: Vec<Annotation>,
) -> Result<MachineDefinition, SsotParserError> {
    let mut inner_pairs = pair.into_inner();
    let name = parse_identifier(inner_pairs.next().unwrap())?;
    let id = parse_optional_numeric_id(&mut inner_pairs, Rule::numeric_id)?.ok_or_else(|| {
        SsotParserError::MissingElement("Numeric ID for machine".to_string(), None)
    })?;

    let mut context = None;
    let mut states = None;
    let mut actions = None;
    let mut guards = None;
    let mut invokes = None;
    let mut element_annotations = Vec::new();

    // Find the optional machine_body pair
    if let Some(body_pair) = inner_pairs.find(|p| p.as_rule() == Rule::machine_body) {
        // Iterate inside the machine_body
        for element_pair in body_pair.into_inner() {
            match element_pair.as_rule() {
                Rule::annotation => element_annotations.push(parse_annotation(element_pair)?),
                Rule::context_definition => {
                    context = Some(parse_context_definition(element_pair, element_annotations)?);
                    element_annotations = Vec::new();
                }
                Rule::states_block => {
                    states = Some(parse_states_block(element_pair, element_annotations)?);
                    element_annotations = Vec::new();
                }
                Rule::actions_block => {
                    actions = Some(parse_actions_block(element_pair, element_annotations)?);
                    element_annotations = Vec::new();
                }
                Rule::guards_block => {
                    guards = Some(parse_guards_block(element_pair, element_annotations)?);
                    element_annotations = Vec::new();
                }
                Rule::invokes_block => {
                    invokes = Some(parse_invokes_block(element_pair, element_annotations)?);
                    element_annotations = Vec::new();
                }
                Rule::COMMENT | Rule::WHITESPACE => { /* Skip */ }
                _ => {
                    return Err(SsotParserError::AstConstructionError(format!(
                        "Unexpected rule {:?} inside machine_body", // Changed context
                        element_pair.as_rule()
                    )));
                }
            }
        }
    } // If no machine_body pair, loops zero times, elements remain None.

    // Handle annotations that might precede the machine_body or be the only content
    // These should likely be associated with the machine itself?
    let final_annotations = machine_annotations
        .into_iter()
        .chain(element_annotations)
        .collect();

    Ok(MachineDefinition {
        name,
        id,
        context,
        states,
        actions,
        guards,
        invokes,
        annotations: final_annotations, // Use combined annotations
    })
}

// --- Field, Type Specifier, Enum Variant Parsers ---

fn parse_field_definition(
    pair: Pair<Rule>,
    mut annotations: Vec<Annotation>,
) -> Result<FieldDefinition, SsotParserError> {
    let mut inner_pairs = pair.into_inner();
    let name = parse_identifier(inner_pairs.next().unwrap())?; // Expect IDENTIFIER
    let type_spec = parse_type_specifier(inner_pairs.next().unwrap())?; // Expect type_specifier
    let id = parse_optional_numeric_id(&mut inner_pairs, Rule::numeric_id)?
        .ok_or_else(|| SsotParserError::MissingElement("Numeric ID for field".to_string(), None))?;

    // Parse any remaining annotations directly attached to the field
    annotations.extend(parse_annotations(&mut inner_pairs)?);

    Ok(FieldDefinition {
        name,
        type_spec,
        id,
        annotations,
    })
}

fn parse_type_specifier(pair: Pair<Rule>) -> Result<TypeSpecifier, SsotParserError> {
    if pair.as_rule() != Rule::type_specifier {
        return Err(SsotParserError::InvalidRule {
            expected: Rule::type_specifier,
            found: pair.as_rule(),
            rule_str: pair.as_str().to_string(),
            span: Some(span_info_from_pest(pair.as_span())),
        });
    }
    let inner_pair = pair.into_inner().next().unwrap(); // Should have one inner rule
    match inner_pair.as_rule() {
        Rule::simple_specifier => {
            let ident_pair = inner_pair.into_inner().next().unwrap();
            Ok(TypeSpecifier::Simple(parse_identifier(ident_pair)?))
        }
        Rule::list_specifier => {
            let type_pair = inner_pair.into_inner().next().unwrap();
            Ok(TypeSpecifier::List(Box::new(parse_type_specifier(
                type_pair,
            )?)))
        }
        Rule::optional_specifier => {
            let type_pair = inner_pair.into_inner().next().unwrap();
            Ok(TypeSpecifier::Optional(Box::new(parse_type_specifier(
                type_pair,
            )?)))
        }
        Rule::map_specifier => {
            let mut map_inner = inner_pair.into_inner();
            let key_pair = map_inner.next().unwrap();
            let value_pair = map_inner.next().unwrap();
            Ok(TypeSpecifier::Map(
                Box::new(parse_type_specifier(key_pair)?),
                Box::new(parse_type_specifier(value_pair)?),
            ))
        }
        _ => Err(SsotParserError::InvalidTypeRef(
            inner_pair.as_str().to_string(),
            Some(span_info_from_pest(inner_pair.as_span())),
        )),
    }
}

fn parse_enum_variant(
    pair: Pair<Rule>,
    annotations: Vec<Annotation>,
) -> Result<EnumVariant, SsotParserError> {
    let mut inner_pairs = pair.into_inner();
    let name = parse_identifier(inner_pairs.next().unwrap())?; // Expect IDENTIFIER
    let id = parse_optional_numeric_id(&mut inner_pairs, Rule::numeric_id)?.ok_or_else(|| {
        SsotParserError::MissingElement("Numeric ID for variant".to_string(), None)
    })?;

    let variant_annotations = if let Some(annotation_block) = inner_pairs.next() {
        let mut block_pairs = annotation_block.into_inner();
        parse_annotations(&mut block_pairs)?
    } else {
        annotations // Use annotations passed if no {} block
    };

    Ok(EnumVariant {
        name,
        id,
        annotations: variant_annotations,
    })
}

// --- Machine Element Parsers ---

fn parse_context_definition(
    pair: Pair<Rule>,
    block_annotations: Vec<Annotation>,
) -> Result<ContextDefinition, SsotParserError> {
    let mut inner_pairs = pair.into_inner();
    let id = parse_optional_numeric_id(&mut inner_pairs, Rule::numeric_id)?.ok_or_else(|| {
        SsotParserError::MissingElement("Numeric ID for context".to_string(), None)
    })?;

    let mut fields = Vec::new();
    let mut field_annotations = Vec::new();

    for field_pair in inner_pairs {
        match field_pair.as_rule() {
            Rule::annotation => field_annotations.push(parse_annotation(field_pair)?),
            Rule::context_field_definition => {
                fields.push(parse_context_field_definition(
                    field_pair,
                    field_annotations,
                )?);
                field_annotations = Vec::new();
            }
            Rule::COMMENT | Rule::WHITESPACE => { /* Skip */ }
            _ => {
                return Err(SsotParserError::AstConstructionError(format!(
                    "Unexpected rule {:?} inside context_definition",
                    field_pair.as_rule()
                )));
            }
        }
    }

    Ok(ContextDefinition {
        id,
        fields,
        annotations: block_annotations,
    })
}

fn parse_context_field_definition(
    pair: Pair<Rule>,
    mut annotations: Vec<Annotation>,
) -> Result<ContextFieldDefinition, SsotParserError> {
    let mut inner_pairs = pair.into_inner();
    let name = parse_identifier(inner_pairs.next().unwrap())?;
    let type_spec = parse_type_specifier(inner_pairs.next().unwrap())?;
    let id = parse_optional_numeric_id(&mut inner_pairs, Rule::numeric_id)?.ok_or_else(|| {
        SsotParserError::MissingElement("Numeric ID for context field".to_string(), None)
    })?;

    annotations.extend(parse_annotations(&mut inner_pairs)?);

    let default_value = None; // TODO: Parse default value if grammar supports it

    Ok(ContextFieldDefinition {
        name,
        type_spec,
        id,
        annotations,
        default_value,
    })
}

fn parse_states_block(
    pair: Pair<Rule>,
    block_annotations: Vec<Annotation>,
) -> Result<StatesBlock, SsotParserError> {
    let mut inner_pairs = pair.into_inner();
    let id = parse_optional_numeric_id(&mut inner_pairs, Rule::numeric_id)?.ok_or_else(|| {
        SsotParserError::MissingElement("Numeric ID for states block".to_string(), None)
    })?;

    let mut states = Vec::new();
    let mut state_annotations = Vec::new();

    for state_pair in inner_pairs {
        match state_pair.as_rule() {
            Rule::annotation => state_annotations.push(parse_annotation(state_pair)?),
            Rule::state_definition => {
                states.push(parse_state_definition(state_pair, state_annotations)?);
                state_annotations = Vec::new();
            }
            Rule::COMMENT | Rule::WHITESPACE => { /* Skip */ }
            _ => {
                return Err(SsotParserError::AstConstructionError(format!(
                    "Unexpected rule {:?} inside states_block",
                    state_pair.as_rule()
                )));
            }
        }
    }

    Ok(StatesBlock {
        id,
        states,
        annotations: block_annotations,
    })
}

fn parse_state_definition(
    pair: Pair<Rule>,
    mut annotations: Vec<Annotation>,
) -> Result<StateDefinition, SsotParserError> {
    let mut inner_pairs = pair.into_inner();
    let name = parse_identifier(inner_pairs.next().unwrap())?;
    let id = parse_optional_numeric_id(&mut inner_pairs, Rule::numeric_id)?
        .ok_or_else(|| SsotParserError::MissingElement("Numeric ID for state".to_string(), None))?;

    let mut transitions = Vec::new();
    let mut invokes = Vec::new();
    let mut regions = Vec::new(); // For parallel states
    let mut history = None;
    let mut element_annotations = Vec::new(); // Annotations found inside the state body {}
    let on_entry = Vec::new(); // TODO: Parse
    let on_exit = Vec::new(); // TODO: Parse
    let after_transitions = Vec::new(); // TODO: Parse

    // Check for state body or semicolon
    if let Some(body_pair) = inner_pairs.next() {
        if body_pair.as_rule() == Rule::state_body {
            for element_pair in body_pair.into_inner() {
                match element_pair.as_rule() {
                    // Collect annotations found *inside* the body separately
                    Rule::annotation => {
                        element_annotations.push(parse_annotation(element_pair)?);
                    }
                    Rule::transition_definition => {
                        // Pass annotations collected *just before* this element
                        transitions.push(parse_transition_definition(
                            element_pair,
                            element_annotations,
                        )?);
                        element_annotations = Vec::new(); // Clear for next element
                    }
                    Rule::state_invoke => {
                        invokes.push(parse_state_invoke(element_pair, element_annotations)?);
                        element_annotations = Vec::new();
                    }
                    Rule::states_block => {
                        regions.push(parse_states_block(element_pair, element_annotations)?);
                        element_annotations = Vec::new();
                    }
                    Rule::history_definition => {
                        history =
                            Some(parse_history_definition(element_pair, element_annotations)?);
                        element_annotations = Vec::new();
                    }
                    // TODO: Parse on_entry, on_exit, after_transitions
                    Rule::COMMENT | Rule::WHITESPACE => { /* Skip */ }
                    _ => {
                        // If we collected annotations but didn't find a valid element, error?
                        if !element_annotations.is_empty() {
                            return Err(SsotParserError::AstConstructionError(format!(
                                "Dangling annotations inside state body: {:?}",
                                element_annotations
                            )));
                        }
                        return Err(SsotParserError::AstConstructionError(format!(
                            "Unexpected rule {:?} inside state_body",
                            element_pair.as_rule()
                        )));
                    }
                }
            }
        } // else it was a semicolon, no body
    }

    // Add any remaining annotations collected inside the body to the state's main list
    // This ensures flags like $initial; defined inside {} are captured for the state itself.
    annotations.extend(element_annotations);

    // Process collected top-level annotations for the state
    let is_initial = annotations
        .iter()
        .any(|a| matches!(a, Annotation::Initial | Annotation::InitialState(_)));
    let is_final = annotations.iter().any(|a| matches!(a, Annotation::Final));
    let is_parallel = annotations
        .iter()
        .any(|a| matches!(a, Annotation::Parallel));

    Ok(StateDefinition {
        name,
        id,
        annotations,
        transitions,
        invokes,
        regions,
        history,
        on_entry,
        on_exit,
        after_transitions,
        is_initial,
        is_final,
        is_parallel,
    })
}

fn parse_transition_definition(
    pair: Pair<Rule>,
    annotations: Vec<Annotation>,
) -> Result<TransitionDefinition, SsotParserError> {
    let mut inner_pairs = pair.into_inner();

    // Optional 'on' keyword is handled by the grammar, first element is event IDENTIFIER
    let event_pair = inner_pairs.next().ok_or_else(|| {
        SsotParserError::MissingRule(
            Rule::IDENTIFIER,
            "for event in transition".to_string(),
            None,
        )
    })?;
    let event = parse_identifier(event_pair)?;
    let id = parse_optional_numeric_id(&mut inner_pairs, Rule::numeric_id)?.ok_or_else(|| {
        SsotParserError::MissingElement("Numeric ID for transition".to_string(), None)
    })?;

    let target_spec_pair = inner_pairs.next().ok_or_else(|| {
        SsotParserError::MissingRule(
            Rule::transition_target_specifier,
            "in transition".to_string(),
            None,
        )
    })?;

    let (target, actions, guards) = parse_transition_target_specifier(target_spec_pair)?;

    Ok(TransitionDefinition {
        event,
        target,
        id,
        annotations,
        actions,
        guards,
    })
}

fn parse_transition_target_specifier(
    pair: Pair<Rule>,
) -> Result<(TransitionTarget, Vec<Identifier>, Vec<Identifier>), SsotParserError> {
    if pair.as_rule() != Rule::transition_target_specifier {
        return Err(SsotParserError::InvalidRule {
            expected: Rule::transition_target_specifier,
            found: pair.as_rule(),
            rule_str: pair.as_str().to_string(),
            span: Some(span_info_from_pest(pair.as_span())),
        });
    }

    let mut inner_pairs = pair.into_inner();
    let target_pair = inner_pairs.next().ok_or_else(|| {
        SsotParserError::MissingRule(
            Rule::target_identifier,
            "in transition_target_specifier".to_string(),
            None,
        )
    })?;

    let target = parse_target_identifier(target_pair)?;

    let mut actions = vec![];
    let mut guards = vec![];

    // Check for optional transition_details block
    if let Some(details_pair) = inner_pairs.next() {
        if details_pair.as_rule() != Rule::transition_details {
            return Err(SsotParserError::InvalidRule {
                expected: Rule::transition_details,
                found: details_pair.as_rule(),
                rule_str: details_pair.as_str().to_string(),
                span: Some(span_info_from_pest(details_pair.as_span())),
            });
        }
        // Iterate through items inside the details block ({ ... })
        for detail_item_pair in details_pair.into_inner() {
            match detail_item_pair.as_rule() {
                Rule::transition_action => {
                    let action_content_pair =
                        detail_item_pair.into_inner().next().ok_or_else(|| {
                            SsotParserError::AstConstructionError(
                                "Expected content inside transition_action".to_string(),
                                None,
                            )
                        })?;
                    match action_content_pair.as_rule() {
                        Rule::IDENTIFIER => {
                            actions.push(parse_identifier(action_content_pair)?);
                        }
                        Rule::action_list => {
                            for action_ident_pair in action_content_pair.into_inner() {
                                if action_ident_pair.as_rule() == Rule::IDENTIFIER {
                                    actions.push(parse_identifier(action_ident_pair)?);
                                }
                            }
                        }
                        _ => {
                            return Err(SsotParserError::AstConstructionError(
                                format!(
                                    "Unexpected rule {:?} inside transition_action",
                                    action_content_pair.as_rule()
                                ),
                                None,
                            ))
                        }
                    }
                }
                Rule::transition_guard => {
                    let guard_content_pair =
                        detail_item_pair.into_inner().next().ok_or_else(|| {
                            SsotParserError::AstConstructionError(
                                "Expected content inside transition_guard".to_string(),
                                None,
                            )
                        })?;
                    match guard_content_pair.as_rule() {
                        Rule::guard_specifier => {
                            // Handle guard negation (myGuard(not)) by capturing the full specifier text
                            let full_specifier_str = guard_content_pair.as_str().trim().to_string();
                            // Create an Identifier using the full string
                            guards.push(Identifier {
                                name: full_specifier_str,
                            });
                        }
                        Rule::guard_list => {
                            for guard_spec_pair in guard_content_pair.into_inner() {
                                if guard_spec_pair.as_rule() == Rule::guard_specifier {
                                    // Handle guard negation (myGuard(not)) by capturing the full specifier text
                                    let full_specifier_str =
                                        guard_spec_pair.as_str().trim().to_string();
                                    // Create an Identifier using the full string
                                    guards.push(Identifier {
                                        name: full_specifier_str,
                                    });
                                }
                            }
                        }
                        _ => {
                            return Err(SsotParserError::AstConstructionError(
                                format!(
                                    "Unexpected rule {:?} inside transition_guard",
                                    guard_content_pair.as_rule()
                                ),
                                None,
                            ))
                        }
                    }
                }
                Rule::COMMENT | Rule::WHITESPACE => { /* Skip */ }
                _ => {
                    return Err(SsotParserError::AstConstructionError(
                        format!(
                            "Unexpected rule {:?} inside transition_details",
                            detail_item_pair.as_rule()
                        ),
                        None,
                    ))
                }
            }
        }
    }
    // Ensure we consumed all pairs if details were present
    // if inner_pairs.next().is_some() {
    //     return Err(SsotParserError::AstConstructionError(
    //         "Unexpected extra pairs after transition_details".to_string(),
    //     ));
    // }

    Ok((target, actions, guards))
}

fn parse_target_identifier(pair: Pair<Rule>) -> Result<TransitionTarget, SsotParserError> {
    let target_str = pair.as_str();
    if target_str == ".history" {
        Ok(TransitionTarget::CurrentHistory)
    // Clone pair before consuming it with into_inner
    } else if let Some(captures) = pair.clone().into_inner().next() {
        // Check if it's QualifiedHistory (IDENTIFIER . history)
        let mut parts = captures.clone().into_inner();
        if let (Some(ident), Some(hist)) = (parts.next(), parts.next()) {
            if hist.as_str() == "history" {
                return Ok(TransitionTarget::QualifiedHistory(parse_identifier(ident)?));
            }
        }
        // If not qualified history, treat as simple state identifier using the original captures pair
        Ok(TransitionTarget::State(parse_identifier(captures)?))
    } else {
        // Should be simple IDENTIFIER if no inner pairs matched the qualified rule
        // Re-parse the original pair as an identifier
        Ok(TransitionTarget::State(parse_identifier(pair)?))
    }
}

fn parse_actions_block(
    pair: Pair<Rule>,
    block_annotations: Vec<Annotation>,
) -> Result<ActionsBlock, SsotParserError> {
    let mut inner_pairs = pair.into_inner();
    let id = parse_optional_numeric_id(&mut inner_pairs, Rule::numeric_id)?.ok_or_else(|| {
        SsotParserError::MissingElement("Numeric ID for actions block".to_string(), None)
    })?;
    let definitions = Vec::new(); // TODO: Parse action definitions
    Ok(ActionsBlock {
        id,
        definitions,
        annotations: block_annotations,
    })
}

fn parse_guards_block(
    pair: Pair<Rule>,
    block_annotations: Vec<Annotation>,
) -> Result<GuardsBlock, SsotParserError> {
    let mut inner_pairs = pair.into_inner();
    let id = parse_optional_numeric_id(&mut inner_pairs, Rule::numeric_id)?.ok_or_else(|| {
        SsotParserError::MissingElement("Numeric ID for guards block".to_string(), None)
    })?;
    let definitions = Vec::new(); // TODO: Parse guard definitions
    Ok(GuardsBlock {
        id,
        definitions,
        annotations: block_annotations,
    })
}

fn parse_invokes_block(
    pair: Pair<Rule>,
    block_annotations: Vec<Annotation>,
) -> Result<InvokesBlock, SsotParserError> {
    let mut inner_pairs = pair.into_inner();
    let id = parse_optional_numeric_id(&mut inner_pairs, Rule::numeric_id)?.ok_or_else(|| {
        SsotParserError::MissingElement("Numeric ID for invokes block".to_string(), None)
    })?;
    let mut definitions = Vec::new();
    let mut invoke_annotations = Vec::new();

    for invoke_pair in inner_pairs {
        match invoke_pair.as_rule() {
            Rule::annotation => invoke_annotations.push(parse_annotation(invoke_pair)?),
            Rule::invoke_definition => {
                definitions.push(parse_invoke_definition(invoke_pair, invoke_annotations)?);
                invoke_annotations = Vec::new();
            }
            Rule::COMMENT | Rule::WHITESPACE => { /* Skip */ }
            _ => {
                return Err(SsotParserError::AstConstructionError(format!(
                    "Unexpected rule {:?} inside invokes_block",
                    invoke_pair.as_rule()
                )));
            }
        }
    }

    Ok(InvokesBlock {
        id,
        definitions,
        annotations: block_annotations,
    })
}

fn parse_invoke_definition(
    pair: Pair<Rule>,
    annotations: Vec<Annotation>,
) -> Result<InvokeDefinition, SsotParserError> {
    let mut inner_pairs = pair.into_inner();
    let name = parse_identifier(inner_pairs.next().unwrap())?;
    let id = parse_optional_numeric_id(&mut inner_pairs, Rule::numeric_id)?.ok_or_else(|| {
        SsotParserError::MissingElement("Numeric ID for invoke".to_string(), None)
    })?;

    let mut src = None;
    let input_mapping = None; // TODO: Parse
    let mut on_done = None;
    let mut on_error = None;
    let mut property_annotations = Vec::new();

    for prop_pair in inner_pairs {
        match prop_pair.as_rule() {
            Rule::annotation => property_annotations.push(parse_annotation(prop_pair)?),
            Rule::invoke_src => {
                src = Some(parse_invoke_src(prop_pair)?);
                property_annotations = Vec::new();
            }
            Rule::invoke_input => {
                // input_mapping = Some(parse_invoke_input(prop_pair)?); // TODO
                property_annotations = Vec::new();
            }
            Rule::invoke_on_done => {
                on_done = Some(parse_invoke_on_done(prop_pair, property_annotations)?);
                property_annotations = Vec::new();
            }
            Rule::invoke_on_error => {
                on_error = Some(parse_invoke_on_error(prop_pair, property_annotations)?);
                property_annotations = Vec::new();
            }
            Rule::COMMENT | Rule::WHITESPACE => { /* Skip */ }
            _ => {
                return Err(SsotParserError::AstConstructionError(format!(
                    "Unexpected rule {:?} inside invoke_definition",
                    prop_pair.as_rule()
                )));
            }
        }
    }

    Ok(InvokeDefinition {
        name,
        id,
        annotations,
        src: src
            .ok_or_else(|| SsotParserError::MissingElement("src in invoke".to_string(), None))?,
        input_mapping,
        on_done,
        on_error,
    })
}

fn parse_invoke_src(pair: Pair<Rule>) -> Result<InvokeSource, SsotParserError> {
    let value_pair = pair.into_inner().next().unwrap(); // invoke_source_value
    let mut inner = value_pair.into_inner();
    let first = inner.next().unwrap();
    match first.as_rule() {
        Rule::STRING_LITERAL => {
            let s = first.as_str();
            Ok(InvokeSource::Literal(s[1..s.len() - 1].to_string()))
        }
        Rule::IDENTIFIER => {
            if let Some(second) = inner.next() {
                // Dot indicates Service.Method
                if second.as_rule() == Rule::IDENTIFIER {
                    Ok(InvokeSource::ServiceMethod(
                        parse_identifier(first)?,
                        parse_identifier(second)?,
                    ))
                } else {
                    Err(SsotParserError::InvalidRule {
                        expected: Rule::IDENTIFIER,
                        found: second.as_rule(),
                        rule_str: second.as_str().to_string(),
                        span: Some(span_info_from_pest(second.as_span())),
                    })
                }
            } else {
                // Single identifier is MachineName
                Ok(InvokeSource::Machine(parse_identifier(first)?))
            }
        }
        _ => Err(SsotParserError::AstConstructionError(format!(
            "Unexpected rule {:?} in invoke_source_value",
            first.as_rule()
        ))),
    }
}

fn parse_invoke_on_done(
    pair: Pair<Rule>,
    annotations: Vec<Annotation>,
) -> Result<InvokeTransitionTarget, SsotParserError> {
    let mut inner_pairs = pair.into_inner();
    let id = parse_optional_numeric_id(&mut inner_pairs, Rule::numeric_id)?.ok_or_else(|| {
        SsotParserError::MissingElement("Numeric ID for onDone".to_string(), None)
    })?;
    let target_spec_pair = inner_pairs.next().ok_or_else(|| {
        SsotParserError::MissingRule(
            Rule::transition_target_specifier,
            "in onDone".to_string(),
            None,
        )
    })?;
    let (target, actions, guards) = parse_transition_target_specifier(target_spec_pair)?;
    Ok(InvokeTransitionTarget {
        target,
        actions,
        guards,
        annotations, // Pass annotations associated with onDone
    })
}

fn parse_invoke_on_error(
    pair: Pair<Rule>,
    annotations: Vec<Annotation>,
) -> Result<InvokeTransitionTarget, SsotParserError> {
    let mut inner_pairs = pair.into_inner();
    let id = parse_optional_numeric_id(&mut inner_pairs, Rule::numeric_id)?.ok_or_else(|| {
        SsotParserError::MissingElement("Numeric ID for onError".to_string(), None)
    })?;
    let target_spec_pair = inner_pairs.next().ok_or_else(|| {
        SsotParserError::MissingRule(
            Rule::transition_target_specifier,
            "in onError".to_string(),
            None,
        )
    })?;
    let (target, actions, guards) = parse_transition_target_specifier(target_spec_pair)?;
    Ok(InvokeTransitionTarget {
        target,
        actions,
        guards,
        annotations, // Pass annotations associated with onError
    })
}

fn parse_state_invoke(
    pair: Pair<Rule>,
    annotations: Vec<Annotation>,
) -> Result<StateInvokeDefinition, SsotParserError> {
    let mut inner_pairs = pair.into_inner();
    let id = parse_optional_numeric_id(&mut inner_pairs, Rule::numeric_id)?.ok_or_else(|| {
        SsotParserError::MissingElement("Numeric ID for state invoke".to_string(), None)
    })?;

    let mut src_ref = None;
    let input_mapping = None; // TODO: Parse
    let mut on_done = None;
    let mut on_error = None;
    let mut property_annotations = Vec::new();

    for prop_pair in inner_pairs {
        match prop_pair.as_rule() {
            Rule::annotation => property_annotations.push(parse_annotation(prop_pair)?),
            Rule::state_invoke_src => {
                // state_invoke_src now matches `("invokes" ~ ".")? ~ IDENTIFIER`
                let mut inner_src_pairs = prop_pair.into_inner();
                // Skip "invokes." if present, find the IDENTIFIER
                let identifier_pair = inner_src_pairs
                    .find(|p| p.as_rule() == Rule::IDENTIFIER)
                    .ok_or_else(|| {
                        SsotParserError::MissingRule(
                            Rule::IDENTIFIER,
                            "in state_invoke_src".to_string(),
                            None,
                        )
                    })?;
                src_ref = Some(parse_identifier(identifier_pair)?);
                property_annotations = Vec::new(); // Reset annotations after processing the element
            }
            Rule::invoke_input => {
                // input_mapping = Some(parse_invoke_input(prop_pair)?);
                property_annotations = Vec::new();
            }
            Rule::invoke_on_done => {
                on_done = Some(parse_invoke_on_done(prop_pair, property_annotations)?);
                property_annotations = Vec::new();
            }
            Rule::invoke_on_error => {
                on_error = Some(parse_invoke_on_error(prop_pair, property_annotations)?);
                property_annotations = Vec::new();
            }
            Rule::COMMENT | Rule::WHITESPACE => { /* Skip */ }
            _ => {
                return Err(SsotParserError::AstConstructionError(format!(
                    "Unexpected rule {:?} inside state_invoke",
                    prop_pair.as_rule()
                )));
            }
        }
    }

    let name = src_ref
        .clone()
        .ok_or_else(|| SsotParserError::MissingElement("src in state invoke".to_string(), None))?;

    Ok(StateInvokeDefinition {
        name,
        id,
        annotations,
        src_ref: src_ref.unwrap(),
        input_mapping,
        on_done,
        on_error,
    })
}

fn parse_history_definition(
    pair: Pair<Rule>,
    annotations: Vec<Annotation>,
) -> Result<HistoryDefinition, SsotParserError> {
    let mut inner_pairs = pair.into_inner();
    let history_type = match inner_pairs.peek().map(|p| p.as_rule()) {
        Some(Rule::history_type) => {
            let type_pair = inner_pairs.next().unwrap();
            match type_pair.as_str() {
                "shallow" => HistoryType::Shallow,
                "deep" => HistoryType::Deep,
                _ => unreachable!(), // Grammar ensures this
            }
        }
        _ => HistoryType::Shallow, // Default to shallow if type not specified
    };
    let id = parse_optional_numeric_id(&mut inner_pairs, Rule::numeric_id)?.ok_or_else(|| {
        SsotParserError::MissingElement("Numeric ID for history".to_string(), None)
    })?;
    let default_target_pair = inner_pairs.next().ok_or_else(|| {
        SsotParserError::MissingRule(
            Rule::IDENTIFIER,
            "for history default target".to_string(),
            None,
        )
    })?;
    let default_target = parse_identifier(default_target_pair)?;

    // TODO: Handle annotations passed in `annotations` parameter?
    Ok(HistoryDefinition {
        history_type,
        id,
        default_target,
    })
}

// --- Placeholder Block Parsers (Ensure these exist) ---

fn parse_services_block(
    pair: Pair<Rule>,
    block_annotations: Vec<Annotation>,
) -> Result<ServicesBlock, SsotParserError> {
    let mut definitions = Vec::new();
    let mut current_annotations = Vec::new();

    for inner_pair in pair.into_inner() {
        match inner_pair.as_rule() {
            Rule::annotation => current_annotations.push(parse_annotation(inner_pair)?),
            // Assuming service_item is the rule containing service/interface defs
            Rule::service_item => {
                definitions.push(parse_service_item(inner_pair, current_annotations)?);
                current_annotations = Vec::new();
            }
            Rule::COMMENT | Rule::WHITESPACE => { /* Skip */ }
            _ => {
                if !current_annotations.is_empty() {
                    return Err(SsotParserError::AstConstructionError(format!(
                        "Annotations found but not attached to a definition: {:?}",
                        current_annotations
                    )));
                }
                return Err(SsotParserError::AstConstructionError(format!(
                    "Unexpected rule {:?} inside services_block",
                    inner_pair.as_rule()
                )));
            }
        }
    }
    if !current_annotations.is_empty() {
        return Err(SsotParserError::AstConstructionError(
            "Trailing annotations found at end of services block".to_string(),
        ));
    }

    Ok(ServicesBlock {
        definitions,
        annotations: block_annotations,
    })
}

// Placeholder for parsing an item within the services block (service or interface)
fn parse_service_item(
    pair: Pair<Rule>,
    item_annotations: Vec<Annotation>,
) -> Result<ServiceItem, SsotParserError> {
    let inner_pair = pair
        .into_inner()
        .next()
        .ok_or_else(|| SsotParserError::UnexpectedInnerPairCount(Rule::service_item, 0, None))?;
    match inner_pair.as_rule() {
        Rule::interface_definition => Ok(ServiceItem::Interface(parse_interface_definition(
            inner_pair,
            item_annotations,
        )?)),
        Rule::service_definition => Ok(ServiceItem::Service(parse_service_definition(
            inner_pair,
            item_annotations,
        )?)),
        _ => Err(SsotParserError::AstConstructionError(format!(
            "Unexpected rule {:?} inside service_item",
            inner_pair.as_rule()
        ))),
    }
}

fn parse_communication_block(
    pair: Pair<Rule>,
    block_annotations: Vec<Annotation>,
) -> Result<CommunicationBlock, SsotParserError> {
    let mut definitions = Vec::new();
    let mut current_annotations = Vec::new();
    for inner_pair in pair.into_inner() {
        match inner_pair.as_rule() {
            Rule::annotation => current_annotations.push(parse_annotation(inner_pair)?),
            Rule::communication_item => {
                // Parse the specific item (protocol, channel, event)
                definitions.push(parse_communication_item(inner_pair, current_annotations)?);
                current_annotations = Vec::new();
            }
            Rule::COMMENT | Rule::WHITESPACE => { /* Skip */ }
            _ => {
                if !current_annotations.is_empty() {
                    return Err(SsotParserError::AstConstructionError(format!(
                        "Annotations found but not attached to a definition: {:?}",
                        current_annotations
                    )));
                }
                return Err(SsotParserError::AstConstructionError(format!(
                    "Unexpected rule {:?} inside communication_block",
                    inner_pair.as_rule()
                )));
            }
        }
    }
    if !current_annotations.is_empty() {
        return Err(SsotParserError::AstConstructionError(
            "Trailing annotations found at end of communication block".to_string(),
        ));
    }
    Ok(CommunicationBlock {
        definitions,
        annotations: block_annotations,
    })
}

// New function to parse the inner communication item
fn parse_communication_item(
    pair: Pair<Rule>,
    item_annotations: Vec<Annotation>,
) -> Result<CommunicationItem, SsotParserError> {
    let inner_pair = pair.into_inner().next().ok_or_else(|| {
        SsotParserError::UnexpectedInnerPairCount(Rule::communication_item, 0, None)
    })?;
    match inner_pair.as_rule() {
        Rule::protocol_definition => Ok(CommunicationItem::Protocol(parse_protocol_definition(
            inner_pair,
            item_annotations,
        )?)),
        Rule::channel_definition => Ok(CommunicationItem::Channel(parse_channel_definition(
            inner_pair,
            item_annotations,
        )?)),
        Rule::event_definition => Ok(CommunicationItem::Event(parse_event_definition(
            inner_pair,
            item_annotations,
        )?)),
        _ => Err(SsotParserError::AstConstructionError(format!(
            "Unexpected rule {:?} inside communication_item",
            inner_pair.as_rule()
        ))),
    }
}

// New function to parse protocol definition
fn parse_protocol_definition(
    pair: Pair<Rule>,
    annotations: Vec<Annotation>,
) -> Result<ProtocolDefinition, SsotParserError> {
    let mut inner_pairs = pair.into_inner();
    let name = parse_identifier(inner_pairs.next().unwrap())?;
    let id = parse_optional_numeric_id(&mut inner_pairs, Rule::numeric_id)?.ok_or_else(|| {
        SsotParserError::MissingElement("Numeric ID for protocol".to_string(), None)
    })?;
    // Protocol definition is simple: protocol NAME @id(ID);
    // No inner elements expected besides name and ID
    Ok(ProtocolDefinition {
        name,
        id,
        annotations,
    })
}

// New function to parse channel definition
fn parse_channel_definition(
    pair: Pair<Rule>,
    mut annotations: Vec<Annotation>,
) -> Result<ChannelDefinition, SsotParserError> {
    let mut inner_pairs = pair.into_inner();
    let name = parse_identifier(inner_pairs.next().unwrap())?;
    let id = parse_optional_numeric_id(&mut inner_pairs, Rule::numeric_id)?.ok_or_else(|| {
        SsotParserError::MissingElement("Numeric ID for channel".to_string(), None)
    })?;

    let mut description = None;
    let mut parameters = Vec::new();
    let mut property_annotations = Vec::new(); // Annotations preceding a property

    // Iterate inside the channel body {}
    for prop_pair in inner_pairs {
        match prop_pair.as_rule() {
            Rule::annotation => {
                // Collect annotations that might apply to the channel itself if inside {}
                // or annotations that precede a property
                property_annotations.push(parse_annotation(prop_pair)?);
            }
            Rule::channel_description => {
                let desc_pair = prop_pair.into_inner().next().ok_or_else(|| {
                    SsotParserError::MissingRule(
                        Rule::STRING_LITERAL,
                        "in channel_description".to_string(),
                        None,
                    )
                })?;
                let desc_str = desc_pair.as_str();
                description = Some(desc_str[1..desc_str.len() - 1].to_string()); // Remove quotes
                                                                                 // TODO: Decide how to handle property_annotations here. Associate with description?
                property_annotations = Vec::new(); // Clear after processing
            }
            Rule::channel_parameters => {
                let params_block = prop_pair.into_inner().next().ok_or_else(|| {
                    SsotParserError::AstConstructionError(
                        "Expected parameter list block {}".to_string(),
                        None,
                    )
                })?;
                // params_block should contain parameter_definition pairs
                for param_pair in params_block.into_inner() {
                    if param_pair.as_rule() == Rule::parameter_definition {
                        // TODO: Need to implement or reuse parse_parameter_definition
                        // parameters.push(parse_parameter_definition(param_pair)?);
                        parameters.push(parse_parameter_definition(param_pair)?);
                    // Use the new function
                    } else if param_pair.as_rule() != Rule::WHITESPACE
                        && param_pair.as_rule() != Rule::COMMENT
                    {
                        return Err(SsotParserError::AstConstructionError(
                            format!(
                                "Unexpected rule {:?} inside channel_parameters block",
                                param_pair.as_rule()
                            ),
                            None,
                        ));
                    }
                }
                // TODO: Decide how to handle property_annotations here. Associate with parameters block?
                property_annotations = Vec::new(); // Clear after processing
            }
            Rule::COMMENT | Rule::WHITESPACE => { /* Skip */ }
            _ => {
                return Err(SsotParserError::AstConstructionError(format!(
                    "Unexpected rule {:?} inside channel_definition body",
                    prop_pair.as_rule()
                )));
            }
        }
    }

    // Combine annotations found before the definition and any potentially dangling ones inside
    annotations.extend(property_annotations);

    Ok(ChannelDefinition {
        name,
        id,
        annotations,
        description,
        parameters, // Placeholder until parse_parameter_definition is handled
        parameters, // Now populated
    })
}

// New function to parse event definition
fn parse_event_definition(
    pair: Pair<Rule>,
    mut annotations: Vec<Annotation>,
) -> Result<EventDefinition, SsotParserError> {
    let mut inner_pairs = pair.into_inner();
    let name = parse_identifier(inner_pairs.next().unwrap())?;
    let id = parse_optional_numeric_id(&mut inner_pairs, Rule::numeric_id)?
        .ok_or_else(|| SsotParserError::MissingElement("Numeric ID for event".to_string(), None))?;
    // Grammar: event IDENTIFIER numeric_id? { (annotation | field_definition)* }
    let mut fields = Vec::new();
    let mut field_annotations = Vec::new();
    for field_pair in inner_pairs {
        // Iterate within {}
        match field_pair.as_rule() {
            Rule::annotation => field_annotations.push(parse_annotation(field_pair)?),
            Rule::field_definition => {
                fields.push(parse_field_definition(field_pair, field_annotations)?);
                field_annotations = Vec::new();
            }
            Rule::COMMENT | Rule::WHITESPACE => { /* Skip */ }
            _ => {
                return Err(SsotParserError::AstConstructionError(format!(
                    "Unexpected rule {:?} inside event_definition body",
                    field_pair.as_rule()
                )));
            }
        }
    }
    // Combine annotations defined before the event and those potentially before the first field
    annotations.extend(field_annotations);

    Ok(EventDefinition {
        name,
        id,
        fields,
        annotations,
    })
}

fn parse_actors_block(
    pair: Pair<Rule>,
    block_annotations: Vec<Annotation>,
) -> Result<ActorsBlock, SsotParserError> {
    // TODO: Implement actual parsing based on grammar rules for actor_definition
    Ok(ActorsBlock {
        definitions: vec![], // Placeholder
        annotations: block_annotations,
    })
}

fn parse_deployment_config_block(
    pair: Pair<Rule>,
    block_annotations: Vec<Annotation>,
) -> Result<DeploymentConfigBlock, SsotParserError> {
    // TODO: Implement actual parsing based on grammar rules for deployment_item
    // Ok(DeploymentConfigBlock {
    //     definitions: vec![], // Placeholder
    //     annotations: block_annotations,
    // })
    let mut definitions = Vec::new();
    let mut current_annotations = Vec::new();

    for inner_pair in pair.into_inner() {
        match inner_pair.as_rule() {
            Rule::annotation => current_annotations.push(parse_annotation(inner_pair)?),
            Rule::deployment_item => {
                definitions.push(parse_deployment_item(inner_pair, current_annotations)?);
                current_annotations = Vec::new();
            }
            Rule::COMMENT | Rule::WHITESPACE => { /* Skip */ }
            _ => {
                if !current_annotations.is_empty() {
                    return Err(SsotParserError::AstConstructionError(format!(
                        "Annotations found but not attached to a deployment item: {:?}",
                        current_annotations
                    )));
                }
                return Err(SsotParserError::AstConstructionError(format!(
                    "Unexpected rule {:?} inside deployment_config_block",
                    inner_pair.as_rule()
                )));
            }
        }
    }
    if !current_annotations.is_empty() {
        return Err(SsotParserError::AstConstructionError(
            "Trailing annotations found at end of deployment_config block".to_string(),
        ));
    }

    Ok(DeploymentConfigBlock {
        definitions,
        annotations: block_annotations,
    })
}

fn parse_deployment_item(
    pair: Pair<Rule>,
    item_annotations: Vec<Annotation>,
) -> Result<DeploymentItem, SsotParserError> {
    let inner_pair = pair
        .into_inner()
        .next()
        .ok_or_else(|| SsotParserError::UnexpectedInnerPairCount(Rule::deployment_item, 0, None))?;
    match inner_pair.as_rule() {
        Rule::environment_definition => Ok(DeploymentItem::Environment(
            parse_environment_definition(inner_pair, item_annotations)?,
        )),
        Rule::infrastructure_definition => Ok(DeploymentItem::Infrastructure(
            parse_infrastructure_definition(inner_pair, item_annotations)?, // TODO: Implement
        )),
        Rule::deployment_definition => Ok(DeploymentItem::Deployment(
            parse_deployment_definition(inner_pair, item_annotations)?, // TODO: Implement
        )),
        _ => Err(SsotParserError::AstConstructionError(format!(
            "Unexpected rule {:?} inside deployment_item",
            inner_pair.as_rule()
        ))),
    }
}

fn parse_environment_definition(
    pair: Pair<Rule>,
    annotations: Vec<Annotation>,
) -> Result<EnvironmentDefinition, SsotParserError> {
    let mut inner_pairs = pair.into_inner();
    let name = parse_identifier(inner_pairs.next().ok_or_else(|| {
        SsotParserError::MissingRule(
            Rule::IDENTIFIER,
            "in parameter_definition".to_string(),
            None,
        )
    })?)?;
    let type_spec = parse_type_specifier(inner_pairs.next().ok_or_else(|| {
        SsotParserError::MissingRule(
            Rule::type_specifier,
            "in parameter_definition".to_string(),
            None,
        )
    })?)?;
    let id = parse_optional_numeric_id(&mut inner_pairs, Rule::numeric_id)?;
    let annotations = parse_annotations(&mut inner_pairs)?;

    Ok(ParameterDefinition {
        name,
        type_spec,
        id,
        annotations,
    })
}

fn parse_infrastructure_definition(
    pair: Pair<Rule>,
    annotations: Vec<Annotation>,
) -> Result<InfrastructureDefinition, SsotParserError> {
    let mut inner_pairs = pair.into_inner();
    let name = parse_identifier(inner_pairs.next().ok_or_else(|| {
        SsotParserError::MissingRule(
            Rule::IDENTIFIER,
            "for infrastructure name".to_string(),
            None,
        )
    })?)?;
    let id = parse_optional_numeric_id(&mut inner_pairs, Rule::numeric_id)?.ok_or_else(|| {
        SsotParserError::MissingElement("Numeric ID for infrastructure".to_string(), None)
    })?;

    let mut extends = None;
    if let Some(maybe_extends) = inner_pairs.peek() {
        if maybe_extends.as_rule() == Rule::infrastructure_extends {
            let extends_pair = inner_pairs.next().unwrap();
            extends = Some(parse_identifier(extends_pair.into_inner().next().unwrap())?);
        }
    }

    let mut attributes = Vec::new();
    let mut body_annotations = Vec::new(); // Annotations inside {}

    for prop_pair in inner_pairs {
        // Iterate remaining items (should be inside {})
        match prop_pair.as_rule() {
            Rule::annotation => {
                body_annotations.push(parse_annotation(prop_pair)?);
            }
            Rule::attribute_definition => {
                let mut attr_inner = prop_pair.into_inner();
                let key = parse_identifier(attr_inner.next().unwrap())?;
                let value_pair = attr_inner.next().unwrap(); // This is literal_value
                let value = parse_literal_value(value_pair)?;
                attributes.push(AttributeDefinition { key, value });
                // TODO: Associate body_annotations?
                body_annotations = Vec::new(); // Clear after processing property
            }
            Rule::COMMENT | Rule::WHITESPACE => { /* Skip */ }
            _ => {
                return Err(SsotParserError::AstConstructionError(format!(
                    "Unexpected rule {:?} inside infrastructure_definition body",
                    prop_pair.as_rule()
                )));
            }
        }
    }

    // Combine annotations from before the definition and inside the body
    let final_annotations = annotations.into_iter().chain(body_annotations).collect();

    Ok(InfrastructureDefinition {
        name,
        id,
        annotations: final_annotations,
        extends,
        attributes,
    })
}

// Placeholder for parsing an interface definition
fn parse_interface_definition(
    pair: Pair<Rule>,
    item_annotations: Vec<Annotation>,
) -> Result<InterfaceDefinition, SsotParserError> {
    // Implement parsing logic for interface definition
    // This is a placeholder and should be replaced with actual parsing logic
    Ok(InterfaceDefinition {
        name: Identifier {
            name: "PlaceholderInterface".to_string(),
        },
        id: NumericId { value: 0 },
        annotations: vec![],
        extends: None,
        methods: vec![],
    })
}

fn parse_deployment_definition(
    pair: Pair<Rule>,
    annotations: Vec<Annotation>,
) -> Result<DeploymentDefinition, SsotParserError> {
    let mut inner_pairs = pair.into_inner();
    let name = parse_identifier(inner_pairs.next().ok_or_else(|| {
        SsotParserError::MissingRule(Rule::IDENTIFIER, "for deployment name".to_string(), None)
    })?)?;
    let id = parse_optional_numeric_id(&mut inner_pairs, Rule::numeric_id)?.ok_or_else(|| {
        SsotParserError::MissingElement("Numeric ID for deployment".to_string(), None)
    })?;

    let mut target_environment = None;
    let mut target_infrastructure = None;
    let mut deployable = None;
    let mut config = None;
    let mut other_attributes = Vec::new();
    let mut body_annotations = Vec::new(); // Annotations inside {}

    for prop_pair in inner_pairs {
        // Iterate remaining items (should be inside {})
        match prop_pair.as_rule() {
            Rule::annotation => {
                body_annotations.push(parse_annotation(prop_pair)?);
            }
            Rule::dep_target_env => {
                target_environment =
                    Some(parse_identifier(prop_pair.into_inner().next().unwrap())?);
                body_annotations = Vec::new();
            }
            Rule::dep_target_infra => {
                let obj_lit_pair = prop_pair.into_inner().next().unwrap();
                if let AnnotationValue::Object(args) = parse_object_literal(obj_lit_pair)? {
                    target_infrastructure = Some(args);
                } else {
                    return Err(SsotParserError::AstConstructionError(
                        "Expected object literal for targetInfrastructure".to_string(),
                        None,
                    ));
                }
                body_annotations = Vec::new();
            }
            Rule::dep_deployable => {
                deployable = Some(parse_identifier(prop_pair.into_inner().next().unwrap())?);
                body_annotations = Vec::new();
            }
            // Note: replicas and strategy are parsed as dep_attribute for now
            // Rule::dep_replicas => { ... }
            // Rule::dep_strategy => { ... }
            Rule::dep_config => {
                let obj_lit_pair = prop_pair.into_inner().next().unwrap();
                if let AnnotationValue::Object(args) = parse_object_literal(obj_lit_pair)? {
                    config = Some(args);
                } else {
                    return Err(SsotParserError::AstConstructionError(
                        "Expected object literal for config".to_string(),
                        None,
                    ));
                }
                body_annotations = Vec::new();
            }
            Rule::dep_attribute => {
                // This rule expands to attribute_definition
                let mut attr_inner = prop_pair.into_inner().next().unwrap().into_inner();
                let key = parse_identifier(attr_inner.next().unwrap())?;
                let value_pair = attr_inner.next().unwrap(); // This is literal_value
                let value = parse_literal_value(value_pair)?;
                other_attributes.push(AttributeDefinition { key, value });
                body_annotations = Vec::new();
            }
            Rule::COMMENT | Rule::WHITESPACE => { /* Skip */ }
            _ => {
                return Err(SsotParserError::AstConstructionError(format!(
                    "Unexpected rule {:?} inside deployment_definition body",
                    prop_pair.as_rule()
                )));
            }
        }
    }

    // Combine annotations from before the definition and inside the body
    let final_annotations = annotations.into_iter().chain(body_annotations).collect();

    Ok(DeploymentDefinition {
        name,
        id,
        annotations: final_annotations,
        target_environment,
        target_infrastructure,
        deployable,
        config,
        other_attributes,
    })
}

#[test]
fn test_parse_invoke_block() {
    let content = r#"
machines {
    machine MyMachine @id(0) {
        invokes @id(1) {
            invoke checkCallback @id(1) {
                src: MyMachine.check;
                onDone @id(2) target Success;
                onError @id(3) target Failure;
            }
        }
    }
}
"#;

    let result = parse_ssot_content(content, None);
    assert!(result.is_ok());
    let ast = result.unwrap();

    let invokes_block = ast.definitions.iter().find_map(|def| {
        if let TopLevelDefinition::Machines(MachinesBlock { definitions, .. }) = def {
            definitions.iter().find_map(|def| {
                if let MachineDefinition { invokes, .. } = def {
                    invokes.as_ref()
                } else {
                    None
                }
            })
        } else {
            None
        }
    });

    assert!(invokes_block.is_some());
    let invokes_block = invokes_block.unwrap();

    assert_eq!(invokes_block.definitions.len(), 1);
    let invoke_def = &invokes_block.definitions[0];
    assert_eq!(invoke_def.name.name, "checkCallback");
    assert_eq!(invoke_def.id.value, 1);
}
