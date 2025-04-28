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
    #[error("Invalid numeric ID format: {0} - {1}")]
    InvalidNumericId(String, String),
    #[error("Missing required element: {0}")]
    MissingElement(String),
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
        });
    }
    Ok(Identifier {
        name: pair.as_str().to_string(),
    })
}

fn parse_numeric_id(pair: Pair<Rule>) -> Result<NumericId, SsotParserError> {
    if pair.as_rule() != Rule::numeric_id {
        return Err(SsotParserError::InvalidRule {
            expected: Rule::numeric_id,
            found: pair.as_rule(),
            rule_str: pair.as_str().to_string(),
        });
    }
    let inner_pair = pair.into_inner().next().ok_or_else(|| {
        SsotParserError::MissingRule(Rule::INTEGER_LITERAL, "in numeric_id".to_string())
    })?;
    let value_str = inner_pair.as_str();
    let value = value_str
        .parse::<u64>()
        .map_err(|e| SsotParserError::InvalidNumericId(value_str.to_string(), e.to_string()))?;
    Ok(NumericId { value })
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
        });
    }
    let mut inner = pair.into_inner();
    let name_pair = inner.next().ok_or_else(|| {
        SsotParserError::MissingRule(Rule::IDENTIFIER, "in annotation".to_string())
    })?;
    let name = name_pair.as_str();

    let args_pair = inner.next(); // Optional annotation_args

    // TODO: Implement full annotation parsing based on name and args
    match name {
        "description" => {
            let value = args_pair
                .and_then(|p| p.into_inner().next()) // Get first arg
                .and_then(|p| p.into_inner().next()) // Get annotation_value
                .and_then(|p| p.into_inner().next()) // Get STRING_LITERAL
                .map(|p| p.as_str()) // Get string slice
                .map(|s| s[1..s.len() - 1].to_string()) // Remove quotes
                .ok_or_else(|| {
                    SsotParserError::InvalidAnnotation(
                        "Missing string literal for $description".into(),
                    )
                })?;
            Ok(Annotation::Description(value))
        }
        "validate" => Ok(Annotation::Validate(vec![])), // Placeholder
        "db" => Ok(Annotation::Db(vec![])),             // Placeholder
        "meta" => Ok(Annotation::Meta(vec![])),         // Placeholder
        "initial" => {
            // Restore original logic for $initial; and $initial(StateName)
            // If no arguments, it's the simple $initial; flag
            if args_pair.is_none() {
                Ok(Annotation::Initial)
            } else {
                // If arguments exist, parse the state identifier for $initial(StateName)
                let value = args_pair
                    .and_then(|p| p.into_inner().next()) // Get first arg
                    .and_then(|p| p.into_inner().next()) // Get annotation_value
                    .and_then(|p| p.into_inner().next()) // Get IDENTIFIER
                    .map(parse_identifier)
                    .transpose()? // Convert Result<Identifier, Error> to Option<Identifier>
                    .ok_or_else(|| {
                        SsotParserError::InvalidAnnotation(
                            "Missing state identifier for $initial(<state_name>)".into(),
                        )
                    })?;
                Ok(Annotation::InitialState(value))
            }
        }
        "final" => Ok(Annotation::Final),
        "parallel" => Ok(Annotation::Parallel),
        "implements" => Ok(Annotation::Implements(Identifier {
            name: "TODO".into(),
        })), // Placeholder
        "protocol" => Ok(Annotation::Protocol(Identifier {
            name: "TODO".into(),
        })), // Placeholder
        "communicatesWith" => Ok(Annotation::CommunicatesWith(CommunicatesWithArgs {
            service: Identifier {
                name: "TODO".into(),
            },
            protocol: Identifier {
                name: "TODO".into(),
            },
        })), // Placeholder
        "publishes" => Ok(Annotation::Publishes(Identifier {
            name: "TODO".into(),
        })), // Placeholder
        "subscribes" => Ok(Annotation::Subscribes(Identifier {
            name: "TODO".into(),
        })), // Placeholder
        "route" => Ok(Annotation::Route(vec![])), // Placeholder
        "channel" => Ok(Annotation::Channel(Identifier {
            name: "TODO".into(),
        })), // Placeholder
        "allowedActors" => Ok(Annotation::AllowedActors(vec![])), // Placeholder
        _ if args_pair.is_none() => Ok(Annotation::GenericFlag(Identifier { name: name.into() })),
        _ => {
            let value = args_pair
                .and_then(|p| p.into_inner().next()) // Get first arg
                .and_then(|p| p.into_inner().next()) // Get annotation_value
                .and_then(|p| p.into_inner().next()) // Get literal
                .map(|p| p.as_str().to_string())
                .ok_or_else(|| {
                    SsotParserError::InvalidAnnotation(format!(
                        "Missing value for generic annotation ${}",
                        name
                    ))
                })?;
            Ok(Annotation::GenericKeyValue(
                Identifier { name: name.into() },
                value, // Needs proper parsing
            ))
        }
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
    let id = parse_optional_numeric_id(&mut inner_pairs, Rule::numeric_id)?
        .ok_or_else(|| SsotParserError::MissingElement("Numeric ID for struct".to_string()))?;
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
        .ok_or_else(|| SsotParserError::MissingElement("Numeric ID for enum".to_string()))?;
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
    let id = parse_optional_numeric_id(&mut inner_pairs, Rule::numeric_id)?
        .ok_or_else(|| SsotParserError::MissingElement("Numeric ID for machine".to_string()))?;

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
        .ok_or_else(|| SsotParserError::MissingElement("Numeric ID for field".to_string()))?;

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
        )),
    }
}

fn parse_enum_variant(
    pair: Pair<Rule>,
    annotations: Vec<Annotation>,
) -> Result<EnumVariant, SsotParserError> {
    let mut inner_pairs = pair.into_inner();
    let name = parse_identifier(inner_pairs.next().unwrap())?; // Expect IDENTIFIER
    let id = parse_optional_numeric_id(&mut inner_pairs, Rule::numeric_id)?
        .ok_or_else(|| SsotParserError::MissingElement("Numeric ID for variant".to_string()))?;

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
    let id = parse_optional_numeric_id(&mut inner_pairs, Rule::numeric_id)?
        .ok_or_else(|| SsotParserError::MissingElement("Numeric ID for context".to_string()))?;

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
        SsotParserError::MissingElement("Numeric ID for context field".to_string())
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
        SsotParserError::MissingElement("Numeric ID for states block".to_string())
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
        .ok_or_else(|| SsotParserError::MissingElement("Numeric ID for state".to_string()))?;

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
        SsotParserError::MissingRule(Rule::IDENTIFIER, "for event in transition".to_string())
    })?;
    let event = parse_identifier(event_pair)?;
    let id = parse_optional_numeric_id(&mut inner_pairs, Rule::numeric_id)?
        .ok_or_else(|| SsotParserError::MissingElement("Numeric ID for transition".to_string()))?;

    let target_spec_pair = inner_pairs.next().ok_or_else(|| {
        SsotParserError::MissingRule(
            Rule::transition_target_specifier,
            "in transition".to_string(),
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
    let mut inner_pairs = pair.into_inner();
    let target_ident_pair = inner_pairs.next().ok_or_else(|| {
        SsotParserError::MissingRule(Rule::target_identifier, "in target specifier".to_string())
    })?;
    let target = parse_target_identifier(target_ident_pair)?;

    let actions = Vec::new(); // TODO: Parse actions from details
    let guards = Vec::new(); // TODO: Parse guards from details

    // TODO: Parse optional details block
    if let Some(_details_pair) = inner_pairs.next() {
        // parse actions/guards
    }

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
        SsotParserError::MissingElement("Numeric ID for actions block".to_string())
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
        SsotParserError::MissingElement("Numeric ID for guards block".to_string())
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
        SsotParserError::MissingElement("Numeric ID for invokes block".to_string())
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
    let id = parse_optional_numeric_id(&mut inner_pairs, Rule::numeric_id)?
        .ok_or_else(|| SsotParserError::MissingElement("Numeric ID for invoke".to_string()))?;

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
        src: src.ok_or_else(|| SsotParserError::MissingElement("src in invoke".to_string()))?,
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
    let id = parse_optional_numeric_id(&mut inner_pairs, Rule::numeric_id)?
        .ok_or_else(|| SsotParserError::MissingElement("Numeric ID for onDone".to_string()))?;
    let target_spec_pair = inner_pairs.next().ok_or_else(|| {
        SsotParserError::MissingRule(Rule::transition_target_specifier, "in onDone".to_string())
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
    let id = parse_optional_numeric_id(&mut inner_pairs, Rule::numeric_id)?
        .ok_or_else(|| SsotParserError::MissingElement("Numeric ID for onError".to_string()))?;
    let target_spec_pair = inner_pairs.next().ok_or_else(|| {
        SsotParserError::MissingRule(Rule::transition_target_specifier, "in onError".to_string())
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
        SsotParserError::MissingElement("Numeric ID for state invoke".to_string())
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
                        SsotParserError::MissingRule(Rule::IDENTIFIER, "in state_invoke_src".to_string())
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
        .ok_or_else(|| SsotParserError::MissingElement("src in state invoke".to_string()))?;

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
    let id = parse_optional_numeric_id(&mut inner_pairs, Rule::numeric_id)?
        .ok_or_else(|| SsotParserError::MissingElement("Numeric ID for history".to_string()))?;
    let default_target_pair = inner_pairs.next().ok_or_else(|| {
        SsotParserError::MissingRule(Rule::IDENTIFIER, "for history default target".to_string())
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
    // service_item should contain either service_definition or interface_definition
    let inner_pair = pair.into_inner().next().ok_or_else(|| {
        SsotParserError::MissingRule(
            Rule::service_definition, /* or interface */
            "in service_item".to_string(),
        )
    })?;

    match inner_pair.as_rule() {
        Rule::service_definition => Ok(ServiceItem::Service(parse_service_definition(
            inner_pair,
            item_annotations,
        )?)),
        // TODO: Add case for Rule::interface_definition
        _ => Err(SsotParserError::AstConstructionError(format!(
            "Unexpected rule {:?} inside service_item ({:?})",
            inner_pair.as_rule(),
            inner_pair.as_str()
        ))),
    }
}

fn parse_communication_block(
    pair: Pair<Rule>,
    block_annotations: Vec<Annotation>,
) -> Result<CommunicationBlock, SsotParserError> {
    // TODO: Implement actual parsing based on grammar rules for communication_item
    Ok(CommunicationBlock {
        definitions: vec![], // Placeholder
        annotations: block_annotations,
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
    Ok(DeploymentConfigBlock {
        definitions: vec![], // Placeholder
        annotations: block_annotations,
    })
}

// Placeholder for parsing a service definition
fn parse_service_definition(
    pair: Pair<Rule>,
    service_annotations: Vec<Annotation>,
) -> Result<ServiceDefinition, SsotParserError> {
    let mut inner_pairs = pair.into_inner();
    let name = parse_identifier(inner_pairs.next().ok_or_else(|| {
        SsotParserError::MissingRule(Rule::IDENTIFIER, "for service name".to_string())
    })?)?;
    let id = parse_optional_numeric_id(&mut inner_pairs, Rule::numeric_id)?
        .ok_or_else(|| SsotParserError::MissingElement("Numeric ID for service".to_string()))?;

    let mut extends = None;
    let mut methods = Vec::new(); // Assuming ServiceDefinition has a 'methods' field in AST
    let mut current_element_annotations = Vec::new(); // Annotations for the next element (method, etc.)

    // Process remaining pairs directly within the service definition
    // No separate service_body rule assumed here.
    for element_pair in inner_pairs {
        match element_pair.as_rule() {
            Rule::annotation => {
                current_element_annotations.push(parse_annotation(element_pair)?);
            }
            Rule::method_definition => {
                methods.push(parse_method_definition(
                    element_pair,
                    current_element_annotations,
                )?);
                current_element_annotations = Vec::new(); // Reset for next element
            }
            // TODO: Add cases for Rule::extends_clause, etc. if needed
            Rule::COMMENT | Rule::WHITESPACE => { /* Skip */ }
            _ => {
                // If annotations were collected but no element followed, associate them with the service?
                // This might need refinement based on exact grammar spec.
                if !current_element_annotations.is_empty() {
                    // For now, let's assume they are service-level annotations inside {}
                    // We'll add them to the final_annotations list later.
                    println!("Warning: Dangling annotations in service definition: {:?}. Attaching to service.", current_element_annotations);
                } else {
                    // If no pending annotations, it's an unexpected rule
                    return Err(SsotParserError::AstConstructionError(format!(
                        "Unexpected rule {:?} inside service_definition body", // Clarified context
                        element_pair.as_rule()
                    )));
                }
            }
        }
    }

    // Combine annotations passed in (likely none if block has its own) with any collected inside {} before elements
    let final_annotations = service_annotations
        .into_iter()
        .chain(current_element_annotations)
        .collect();

    Ok(ServiceDefinition {
        name,
        id,
        annotations: final_annotations,
        extends,
        // Ensure the AST struct `ServiceDefinition` has a `methods: Vec<MethodDefinition>` field.
        // If not, this needs adjustment.
        // methods, // Placeholder until AST is confirmed/updated
    })
}

// Placeholder for parsing a method definition
fn parse_method_definition(
    pair: Pair<Rule>,
    method_annotations: Vec<Annotation>,
) -> Result<MethodDefinition, SsotParserError> {
    let mut inner_pairs = pair.into_inner();
    let name = parse_identifier(inner_pairs.next().ok_or_else(|| {
        SsotParserError::MissingRule(Rule::IDENTIFIER, "for method name".to_string())
    })?)?;
    let id = parse_optional_numeric_id(&mut inner_pairs, Rule::numeric_id)?
        .ok_or_else(|| SsotParserError::MissingElement("Numeric ID for method".to_string()))?;

    // --- Start Placeholder Logic ---
    // Skip the rest of the inner pairs for now
    // TODO: Implement full parsing for parameters, return type, body annotations
    let parameters = Vec::new();
    let return_type = None;
    let body_annotations = Vec::new();
    // --- End Placeholder Logic ---

    Ok(MethodDefinition {
        name,
        id,
        annotations: method_annotations, // Annotations before the method definition
        parameters,
        return_type,
        body_annotations, // Annotations inside the method body {}
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
