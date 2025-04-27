use crate::ast::{
    ActionDefinition, ActionsBlock, ActorDefinition, ActorsBlock, AfterTransitionDefinition,
    Annotation, AnnotationValue, Argument, AttributeDefinition, ChannelDefinition,
    CommunicatesWithArgs, CommunicationBlock, CommunicationItem, ContextDefinition,
    ContextFieldDefinition, DeploymentConfigBlock, DeploymentDefinition, DeploymentItem, Duration,
    EnumDefinition, EnumVariant, EnvironmentDefinition, EventDefinition, FieldDefinition, FileId,
    GuardDefinition, GuardsBlock, HistoryDefinition, HistoryType, Identifier, ImportStatement,
    InfrastructureDefinition, InterfaceDefinition, InvokeDefinition, InvokeSource,
    InvokeTransitionTarget, InvokesBlock, MachineDefinition, MachinesBlock, MethodDefinition,
    NumericId, ParameterDefinition, ProtocolDefinition, ServiceDefinition, ServiceItem,
    ServicesBlock, SsotAst, StateDefinition, StateInvokeDefinition, StatesBlock, StructDefinition,
    TimeUnit, TopLevelDefinition, TransitionDefinition, TransitionTarget, TypeDefinition,
    TypeSpecifier, TypesBlock,
};
use crate::error::{ErrorKind, ParserError};
use pest::iterators::{Pair, Pairs};
use pest::Parser;
use pest_derive::Parser;
use std::collections::HashMap;
use std::path::{Path, PathBuf};
use thiserror::Error;

#[derive(Parser)]
#[grammar = "ssot.pest"]
struct SsotParser;

// Bring Rule into scope
use self::Rule;

// Span structure to hold location info
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct Span {
    pub file_path: PathBuf,
    pub start: usize,
    pub end: usize,
}

impl Span {
    fn from_pest_span(pest_span: &pest::Span<'_>, file_path: &Path) -> Self {
        Span {
            file_path: file_path.to_path_buf(),
            start: pest_span.start(),
            end: pest_span.end(),
        }
    }
}

/// Represents errors that can occur during the parsing of an SSOT file or content.
#[derive(Error, Debug)]
pub enum ParseError {
    /// An error occurred within the Pest parser.
    #[error("Pest parsing error: {0}")]
    PestError(#[from] pest::error::Error<<SsotParser as pest::Parser>::Rule>),
    /// An error occurred while trying to read an SSOT file.
    #[error("Error reading file {path}: {source}")]
    FileReadError {
        /// The path of the file that failed to read.
        path: PathBuf,
        /// The underlying I/O error.
        #[source]
        source: std::io::Error,
    },
    /// The input content was invalid or unexpected.
    #[error("Invalid input: {message}")]
    InvalidInput { message: String },
    /// Failed to parse an integer value from the input.
    #[error("Failed to parse integer: {0}")]
    ParseIntError(#[from] std::num::ParseIntError),
    /// An unexpected grammar rule was encountered during parsing.
    #[error("Unexpected rule: expected {expected:?}, found {found:?}")]
    UnexpectedRule {
        expected: <SsotParser as pest::Parser>::Rule,
        found: <SsotParser as pest::Parser>::Rule,
    },
    /// A required grammar rule was missing from the input.
    #[error("Missing rule: expected {expected:?}")]
    MissingRule {
        expected: <SsotParser as pest::Parser>::Rule,
    },
    /// Missing identifier.
    #[error("Missing identifier")]
    MissingIdentifier,
}

/// A type alias for `Result<T, ParseError>`.
pub type ParseResult<T> = Result<T, ParseError>;

/// Parses the content of an SSOT string into an Abstract Syntax Tree (AST).
///
/// This function takes the string content of an SSOT definition and attempts
/// to parse it according to the grammar defined in `ssot.pest`.
///
/// # Arguments
///
/// * `content` - A string slice (`&str`) containing the SSOT definition.
/// * `source_path` - An optional `PathBuf` indicating the original file path of the content.
///                   This is stored in the resulting AST but not used during parsing itself.
///
/// # Returns
///
/// * `ParseResult<SsotAst>` - A `Result` which is `Ok(SsotAst)` containing the parsed AST
///   on success, or `Err(ParseError)` if parsing fails.
///
/// # Errors
///
/// This function can return various `ParseError` variants if the input content
/// is invalid, contains syntax errors, or if unexpected parsing issues occur.
///
/// # Example
///
/// ```rust
/// use fsm_dsl::parser::{parse_ssot_content, ParseError};
/// use fsm_dsl::ast::SsotAst;
/// use std::path::PathBuf;
///
/// let ssot_content = r#"
///     @0x1;
///     types {
///         struct MyData @id(0) { field: string @id(0); }
///     }
/// "#;
///
/// match parse_ssot_content(ssot_content, Some(PathBuf::from("example.ssot"))) {
///     Ok(ast) => println!("Parsed AST successfully! File ID: {:?}", ast.file_id),
///     Err(e) => eprintln!("Failed to parse: {}", e),
/// }
/// ```
pub fn parse_ssot_content(content: &str, source_path: Option<PathBuf>) -> ParseResult<SsotAst> {
    let pairs = SsotParser::parse(<SsotParser as pest::Parser>::Rule::file, content)?;

    let mut ast = SsotAst {
        source_path,
        file_id: None,
        imports: Vec::new(),
        definitions: Vec::new(),
    };

    let file_pair = pairs.peek().ok_or_else(|| ParseError::InvalidInput {
        message: "Empty input".to_string(),
    })?;

    if file_pair.as_rule() != <SsotParser as pest::Parser>::Rule::file {
        return Err(ParseError::UnexpectedRule {
            expected: <SsotParser as pest::Parser>::Rule::file,
            found: file_pair.as_rule(),
        });
    }

    let mut definitions = Vec::new();
    for pair in file_pair.into_inner() {
        match pair.as_rule() {
            <SsotParser as pest::Parser>::Rule::COMMENT => { /* Skip comments */ }
            <SsotParser as pest::Parser>::Rule::EOI => { /* End of Input, expected */ }
            // Top-level definitions
            <SsotParser as pest::Parser>::Rule::definition => {
                // A definition can contain various blocks or statements
                if let Some(inner_pair) = pair.into_inner().next() {
                    match inner_pair.as_rule() {
                        <SsotParser as pest::Parser>::Rule::file_id => {
                            ast.file_id = Some(parse_file_id(inner_pair)?);
                        }
                        <SsotParser as pest::Parser>::Rule::import_statement => {
                            ast.imports.push(parse_import_statement(pair)?);
                        }
                        <SsotParser as pest::Parser>::Rule::types_block => {
                            ast.definitions
                                .push(TopLevelDefinition::Types(parse_types_block(pair)?));
                        }
                        <SsotParser as pest::Parser>::Rule::machines_block => {
                            ast.definitions
                                .push(TopLevelDefinition::Machines(parse_machines_block(pair)?));
                        }
                        <SsotParser as pest::Parser>::Rule::services_block => {
                            ast.definitions
                                .push(TopLevelDefinition::Services(parse_services_block(pair)?));
                        }
                        <SsotParser as pest::Parser>::Rule::communication_block => {
                            ast.definitions.push(TopLevelDefinition::Communication(
                                parse_communication_block(pair)?,
                            ));
                        }
                        <SsotParser as pest::Parser>::Rule::actors_block => {
                            ast.definitions
                                .push(TopLevelDefinition::Actors(parse_actors_block(pair)?));
                        }
                        <SsotParser as pest::Parser>::Rule::deployment_config_block => {
                            ast.definitions.push(TopLevelDefinition::DeploymentConfig(
                                parse_deployment_config_block(pair)?,
                            ));
                        }
                        <SsotParser as pest::Parser>::Rule::annotation => {
                            // annotations at the top level might relate to the file itself
                            // For now, we might ignore them or associate them with the file scope
                            // definitions.push(Definition::Annotation(parse_annotation(pair)?));
                        }
                        _ => {
                            // Optionally log or handle unexpected top-level pairs
                            // eprintln!("Unexpected top-level pair: {:?}", pair.as_rule());
                        }
                    }
                }
            }
            _ => {
                // Optionally log or handle unexpected top-level pairs
                // eprintln!("Unexpected top-level pair: {:?}", pair.as_rule());
            }
        }
    }

    Ok(ast)
}

// --- Helper Functions ---

fn get_text(pair: Pair<<SsotParser as pest::Parser>::Rule>) -> &str {
    pair.as_str().trim()
}

fn parse_file_id(pair: Pair<<SsotParser as pest::Parser>::Rule>) -> ParseResult<FileId> {
    if pair.as_rule() != <SsotParser as pest::Parser>::Rule::file_id {
        return Err(ParseError::UnexpectedRule {
            expected: <SsotParser as pest::Parser>::Rule::file_id,
            found: pair.as_rule(),
        });
    }
    let hex_literal_pair = pair
        .into_inner()
        .find(|p| p.as_rule() == <SsotParser as pest::Parser>::Rule::hex_literal)
        .ok_or(ParseError::MissingRule {
            expected: <SsotParser as pest::Parser>::Rule::hex_literal,
        })?;

    let hex_str = get_text(hex_literal_pair);
    let value_str = hex_str.strip_prefix("0x").ok_or(ParseError::InvalidInput {
        message: format!("Invalid hex literal format: {}", hex_str),
    })?;
    let value = u64::from_str_radix(value_str, 16)?;

    Ok(FileId { value })
}

fn parse_import_statement(
    pair: Pair<<SsotParser as pest::Parser>::Rule>,
) -> ParseResult<ImportStatement> {
    if pair.as_rule() != <SsotParser as pest::Parser>::Rule::import_statement {
        return Err(ParseError::UnexpectedRule {
            expected: <SsotParser as pest::Parser>::Rule::import_statement,
            found: pair.as_rule(),
        });
    }
    let path_pair = pair
        .into_inner()
        .find(|p| p.as_rule() == <SsotParser as pest::Parser>::Rule::string_literal)
        .ok_or(ParseError::MissingRule {
            expected: <SsotParser as pest::Parser>::Rule::string_literal,
        })?;

    // Remove quotes from the string literal
    let path_str = get_text(path_pair);
    let path = path_str.trim_matches('"').to_string();

    Ok(ImportStatement { path })
}

fn parse_identifier(pair: Pair<<SsotParser as pest::Parser>::Rule>) -> ParseResult<Identifier> {
    if pair.as_rule() != <SsotParser as pest::Parser>::Rule::identifier {
        return Err(ParseError::UnexpectedRule {
            expected: <SsotParser as pest::Parser>::Rule::identifier,
            found: pair.as_rule(),
        });
    }
    Ok(Identifier {
        name: get_text(pair).to_string(),
    })
}

fn parse_numeric_id(pair: Pair<<SsotParser as pest::Parser>::Rule>) -> ParseResult<NumericId> {
    let text = get_text(pair);
    let id = text.parse::<u64>()?;
    Ok(NumericId { value: id })
}

fn parse_types_block(pair: Pair<<SsotParser as pest::Parser>::Rule>) -> ParseResult<TypesBlock> {
    if pair.as_rule() != <SsotParser as pest::Parser>::Rule::types_block {
        return Err(ParseError::UnexpectedRule {
            expected: <SsotParser as pest::Parser>::Rule::types_block,
            found: pair.as_rule(),
        });
    }

    let mut types = Vec::new();
    let mut annotations = Vec::new();

    for inner_pair in pair.into_inner() {
        match inner_pair.as_rule() {
            <SsotParser as pest::Parser>::Rule::annotation => {
                annotations.push(parse_annotation(inner_pair)?)
            }
            <SsotParser as pest::Parser>::Rule::type_definition => {
                // A type_definition wraps either a struct or an enum
                if let Some(def_pair) = inner_pair.into_inner().next() {
                    match def_pair.as_rule() {
                        <SsotParser as pest::Parser>::Rule::struct_definition => {
                            let mut struct_def = parse_struct_definition(def_pair)?;
                            struct_def.annotations.extend(annotations.drain(..)); // Add preceding annotations
                            types.push(TypeDefinition::Struct(struct_def));
                        }
                        <SsotParser as pest::Parser>::Rule::enum_definition => {
                            let mut enum_def = parse_enum_definition(def_pair)?;
                            enum_def.annotations.extend(annotations.drain(..)); // Add preceding annotations
                            types.push(TypeDefinition::Enum(enum_def));
                        }
                        _ => {
                            return Err(ParseError::UnexpectedRule {
                                expected: <SsotParser as pest::Parser>::Rule::struct_definition, // Or enum
                                found: def_pair.as_rule(),
                            });
                        }
                    }
                }
            }
            _ => {
                return Err(ParseError::UnexpectedRule {
                    expected: <SsotParser as pest::Parser>::Rule::annotation, // Or type_definition
                    found: inner_pair.as_rule(),
                });
            }
        }
    }

    Ok(TypesBlock { types })
}

// Add similar parse functions for machines, actors, communication, services, deployment_config blocks

fn parse_machines_block(
    pair: Pair<<SsotParser as pest::Parser>::Rule>,
) -> ParseResult<MachinesBlock> {
    if pair.as_rule() != <SsotParser as pest::Parser>::Rule::machines_block {
        return Err(ParseError::UnexpectedRule {
            expected: <SsotParser as pest::Parser>::Rule::machines_block,
            found: pair.as_rule(),
        });
    }

    let mut machines = Vec::new();
    let mut annotations = Vec::new();

    for inner_pair in pair.into_inner() {
        match inner_pair.as_rule() {
            <SsotParser as pest::Parser>::Rule::annotation => {
                annotations.push(parse_annotation(inner_pair)?)
            }
            <SsotParser as pest::Parser>::Rule::machine_definition => {
                let mut machine_def = parse_machine_definition(inner_pair)?;
                machine_def.annotations.extend(annotations.drain(..)); // Add preceding annotations
                machines.push(machine_def);
            }
            _ => {
                return Err(ParseError::UnexpectedRule {
                    expected: <SsotParser as pest::Parser>::Rule::annotation, // Or machine_definition
                    found: inner_pair.as_rule(),
                });
            }
        }
    }

    Ok(MachinesBlock { machines })
}

fn parse_struct_definition(
    pair: Pair<<SsotParser as pest::Parser>::Rule>,
) -> ParseResult<StructDefinition> {
    if pair.as_rule() != <SsotParser as pest::Parser>::Rule::struct_definition {
        return Err(ParseError::UnexpectedRule {
            expected: <SsotParser as pest::Parser>::Rule::struct_definition,
            found: pair.as_rule(),
        });
    }

    let mut name: Option<Identifier> = None;
    let mut id: Option<NumericId> = None;
    let mut fields = Vec::new();
    let mut annotations = Vec::new(); // Annotations specific to this struct

    for inner_pair in pair.into_inner() {
        match inner_pair.as_rule() {
            <SsotParser as pest::Parser>::Rule::annotation => {
                annotations.push(parse_annotation(inner_pair)?);
            }
            <SsotParser as pest::Parser>::Rule::identifier => {
                name = Some(parse_identifier(inner_pair)?)
            }
            <SsotParser as pest::Parser>::Rule::integer_literal => {
                id = Some(parse_numeric_id(inner_pair)?)
            }
            <SsotParser as pest::Parser>::Rule::field_definition => {
                fields.push(parse_field_definition(inner_pair)?)
            }
            _ => {
                return Err(ParseError::UnexpectedRule {
                    expected: <SsotParser as pest::Parser>::Rule::identifier, // Or others
                    found: inner_pair.as_rule(),
                });
            }
        }
    }

    Ok(StructDefinition {
        name: name.ok_or(ParseError::MissingRule {
            expected: <SsotParser as pest::Parser>::Rule::identifier,
        })?,
        id: id.ok_or(ParseError::MissingRule {
            expected: <SsotParser as pest::Parser>::Rule::integer_literal,
        })?,
        fields,
        annotations,
    })
}

fn parse_field_definition(
    pair: Pair<<SsotParser as pest::Parser>::Rule>,
) -> ParseResult<FieldDefinition> {
    if pair.as_rule() != <SsotParser as pest::Parser>::Rule::field_definition {
        return Err(ParseError::UnexpectedRule {
            expected: <SsotParser as pest::Parser>::Rule::field_definition,
            found: pair.as_rule(),
        });
    }

    let mut name: Option<Identifier> = None;
    let mut type_spec: Option<TypeSpecifier> = None;
    let mut id: Option<NumericId> = None;
    let mut is_optional = false;
    let mut is_list = false;
    let mut annotations = Vec::new(); // Annotations specific to this field

    for inner_pair in pair.into_inner() {
        match inner_pair.as_rule() {
            <SsotParser as pest::Parser>::Rule::annotation => {
                annotations.push(parse_annotation(inner_pair)?);
            }
            <SsotParser as pest::Parser>::Rule::field_modifier => {
                // TODO: Handle modifiers like optional (?) and list ([])
                // For now, just note their presence if needed, or parse them
                // let modifier_text = get_text(inner_pair);
                // if modifier_text == "?" { is_optional = true; }
                // if modifier_text == "[]" { is_list = true; }
            }
            <SsotParser as pest::Parser>::Rule::identifier => {
                name = Some(parse_identifier(inner_pair)?)
            }
            <SsotParser as pest::Parser>::Rule::type_specifier => {
                type_spec = Some(parse_type_specifier(inner_pair)?)
            }
            <SsotParser as pest::Parser>::Rule::integer_literal => {
                id = Some(parse_numeric_id(inner_pair)?)
            }
            _ => {
                return Err(ParseError::UnexpectedRule {
                    expected: <SsotParser as pest::Parser>::Rule::identifier, // Or others
                    found: inner_pair.as_rule(),
                });
            }
        }
    }

    Ok(FieldDefinition {
        name: name.ok_or(ParseError::MissingRule {
            expected: <SsotParser as pest::Parser>::Rule::identifier,
        })?,
        type_specifier: type_spec.ok_or(ParseError::MissingRule {
            expected: <SsotParser as pest::Parser>::Rule::type_specifier,
        })?,
        id: id.ok_or(ParseError::MissingRule {
            expected: <SsotParser as pest::Parser>::Rule::integer_literal,
        })?,
        // is_optional, // Add these fields to FieldDefinition struct
        // is_list,
        annotations,
    })
}

fn parse_enum_definition(
    pair: Pair<<SsotParser as pest::Parser>::Rule>,
) -> ParseResult<EnumDefinition> {
    if pair.as_rule() != <SsotParser as pest::Parser>::Rule::enum_definition {
        return Err(ParseError::UnexpectedRule {
            expected: <SsotParser as pest::Parser>::Rule::enum_definition,
            found: pair.as_rule(),
        });
    }

    let mut name: Option<Identifier> = None;
    let mut id: Option<NumericId> = None;
    let mut variants = Vec::new();
    let mut annotations = Vec::new(); // Annotations specific to this enum

    for inner_pair in pair.into_inner() {
        match inner_pair.as_rule() {
            <SsotParser as pest::Parser>::Rule::annotation => {
                annotations.push(parse_annotation(inner_pair)?)
            }
            <SsotParser as pest::Parser>::Rule::identifier => {
                name = Some(parse_identifier(inner_pair)?)
            }
            <SsotParser as pest::Parser>::Rule::integer_literal => {
                id = Some(parse_numeric_id(inner_pair)?)
            }
            <SsotParser as pest::Parser>::Rule::enum_variant => {
                variants.push(parse_enum_variant(inner_pair)?)
            }
            _ => {
                return Err(ParseError::UnexpectedRule {
                    expected: <SsotParser as pest::Parser>::Rule::identifier, // Or others
                    found: inner_pair.as_rule(),
                });
            }
        }
    }

    Ok(EnumDefinition {
        name: name.ok_or(ParseError::MissingRule {
            expected: <SsotParser as pest::Parser>::Rule::identifier,
        })?,
        id: id.ok_or(ParseError::MissingRule {
            expected: <SsotParser as pest::Parser>::Rule::integer_literal,
        })?,
        variants,
        annotations,
    })
}

fn parse_enum_variant(pair: Pair<<SsotParser as pest::Parser>::Rule>) -> ParseResult<EnumVariant> {
    if pair.as_rule() != <SsotParser as pest::Parser>::Rule::enum_variant {
        return Err(ParseError::UnexpectedRule {
            expected: <SsotParser as pest::Parser>::Rule::enum_variant,
            found: pair.as_rule(),
        });
    }

    let mut name: Option<Identifier> = None;
    let mut id: Option<NumericId> = None;
    // TODO: Handle associated data/types if the grammar supports it
    let mut annotations = Vec::new(); // Annotations specific to this variant

    for inner_pair in pair.into_inner() {
        match inner_pair.as_rule() {
            <SsotParser as pest::Parser>::Rule::annotation => {
                annotations.push(parse_annotation(inner_pair)?);
                // TODO: Decide if annotations apply to the variant itself
            }
            <SsotParser as pest::Parser>::Rule::identifier => {
                name = Some(parse_identifier(inner_pair)?)
            }
            <SsotParser as pest::Parser>::Rule::integer_literal => {
                id = Some(parse_numeric_id(inner_pair)?)
            }
            _ => {
                return Err(ParseError::UnexpectedRule {
                    expected: <SsotParser as pest::Parser>::Rule::identifier, // Or others
                    found: inner_pair.as_rule(),
                });
            }
        }
    }

    Ok(EnumVariant {
        name: name.ok_or(ParseError::MissingRule {
            expected: <SsotParser as pest::Parser>::Rule::identifier,
        })?,
        id: id.ok_or(ParseError::MissingRule {
            expected: <SsotParser as pest::Parser>::Rule::integer_literal,
        })?,
        // associated_type: Option<TypeSpecifier>, // Add if needed
        annotations,
    })
}

fn parse_machine_definition(
    pair: Pair<<SsotParser as pest::Parser>::Rule>,
) -> ParseResult<MachineDefinition> {
    if pair.as_rule() != <SsotParser as pest::Parser>::Rule::machine_definition {
        return Err(ParseError::UnexpectedRule {
            expected: <SsotParser as pest::Parser>::Rule::machine_definition,
            found: pair.as_rule(),
        });
    }

    let mut name: Option<Identifier> = None;
    let mut id: Option<NumericId> = None;
    let mut context: Option<ContextDefinition> = None;
    let mut states: Option<StatesBlock> = None;
    let mut actions: Option<ActionsBlock> = None;
    let mut guards: Option<GuardsBlock> = None;
    let mut invokes: Option<InvokesBlock> = None;
    let mut annotations = Vec::new();

    for inner_pair in pair.into_inner() {
        match inner_pair.as_rule() {
            <SsotParser as pest::Parser>::Rule::annotation => {
                annotations.push(parse_annotation(inner_pair)?)
            }
            <SsotParser as pest::Parser>::Rule::identifier => {
                name = Some(parse_identifier(inner_pair)?)
            }
            <SsotParser as pest::Parser>::Rule::integer_literal => {
                id = Some(parse_numeric_id(inner_pair)?)
            }
            <SsotParser as pest::Parser>::Rule::machine_element => {
                // machine_element is silent, look inside
                if let Some(element_pair) = inner_pair.into_inner().next() {
                    match element_pair.as_rule() {
                        <SsotParser as pest::Parser>::Rule::context_definition => {
                            context = Some(parse_context_definition(element_pair)?);
                        }
                        <SsotParser as pest::Parser>::Rule::states_definition => {
                            states = Some(parse_states_definition(element_pair)?);
                        }
                        <SsotParser as pest::Parser>::Rule::actions_definition => {
                            actions = Some(parse_actions_block(element_pair)?);
                        }
                        <SsotParser as pest::Parser>::Rule::guards_definition => {
                            guards = Some(parse_guards_block(element_pair)?);
                        }
                        <SsotParser as pest::Parser>::Rule::invokes_definition => {
                            invokes = Some(parse_invokes_block(element_pair)?);
                        }
                        _ => {
                            eprintln!(
                                "Warning: Unexpected rule inside machine_element: {:?}",
                                element_pair.as_rule()
                            );
                        }
                    }
                } else {
                    eprintln!("Warning: Empty machine_element encountered.");
                }
            }
            _ => {
                eprintln!(
                    "Warning: Unexpected rule inside machine_definition: {:?}",
                    inner_pair.as_rule()
                );
            }
        }
    }

    Ok(MachineDefinition {
        annotations,
        name: name.ok_or(ParseError::MissingRule {
            expected: <SsotParser as pest::Parser>::Rule::identifier,
        })?,
        id: id.ok_or(ParseError::MissingRule {
            expected: <SsotParser as pest::Parser>::Rule::integer_literal,
        })?,
        context,
        states,
        actions,
        guards,
        invokes,
    })
}

fn parse_annotation(pair: Pair<<SsotParser as pest::Parser>::Rule>) -> ParseResult<Annotation> {
    // Assuming annotation format like `@key(value)` or `@key`
    // Example: @description("This is a type")
    if pair.as_rule() != <SsotParser as pest::Parser>::Rule::annotation {
        return Err(ParseError::UnexpectedRule {
            expected: <SsotParser as pest::Parser>::Rule::annotation,
            found: pair.as_rule(),
        });
    }

    let mut inner = pair.into_inner();
    let key_pair = inner.next().ok_or(ParseError::MissingRule {
        expected: <SsotParser as pest::Parser>::Rule::identifier, /* or specific key rule */
    })?;
    let key = get_text(key_pair).to_string(); // Assuming the key is an identifier

    let value = if let Some(value_pair) = inner.next() {
        // Assuming value is within parentheses and is a string_literal
        if value_pair.as_rule() == <SsotParser as pest::Parser>::Rule::string_literal {
            Some(get_text(value_pair).trim_matches('"').to_string())
        } else {
            // Handle other potential value types or error
            None // Or return an error
        }
    } else {
        None // Annotation without value, e.g., `@deprecated`
    };

    Ok(Annotation { key, value })
}

fn parse_type_specifier(
    pair: Pair<<SsotParser as pest::Parser>::Rule>,
) -> ParseResult<TypeSpecifier> {
    if pair.as_rule() != <SsotParser as pest::Parser>::Rule::type_specifier {
        return Err(ParseError::UnexpectedRule {
            expected: <SsotParser as pest::Parser>::Rule::type_specifier,
            found: pair.as_rule(),
        });
    }

    let inner = pair.into_inner().next().ok_or(ParseError::MissingRule {
        expected: <SsotParser as pest::Parser>::Rule::identifier, // or list_type etc.
    })?;

    match inner.as_rule() {
        <SsotParser as pest::Parser>::Rule::identifier => {
            let base_type = Identifier {
                name: get_text(inner).to_string(),
            };
            Ok(TypeSpecifier::Simple(base_type))
        }
        <SsotParser as pest::Parser>::Rule::list_type => {
            // list_type = { "list" ~ "<" ~ type_specifier ~ ">" }
            let inner_type_pair = inner.into_inner().next().ok_or(ParseError::MissingRule {
                expected: <SsotParser as pest::Parser>::Rule::type_specifier,
            })?;
            let inner_type = parse_type_specifier(inner_type_pair)?;
            Ok(TypeSpecifier::List(Box::new(inner_type)))
        }
        <SsotParser as pest::Parser>::Rule::optional_type => {
            // optional_type = { "optional" ~ "<" ~ type_specifier ~ ">" }
            let inner_type_pair = inner.into_inner().next().ok_or(ParseError::MissingRule {
                expected: <SsotParser as pest::Parser>::Rule::type_specifier,
            })?;
            let inner_type = parse_type_specifier(inner_type_pair)?;
            Ok(TypeSpecifier::Optional(Box::new(inner_type)))
        }
        <SsotParser as pest::Parser>::Rule::map_type => {
            // map_type = { "map" ~ "<" ~ type_specifier ~ "," ~ type_specifier ~ ">" }
            let mut inner_pairs = inner.into_inner();
            let key_type_pair = inner_pairs.next().ok_or(ParseError::MissingRule {
                expected: <SsotParser as pest::Parser>::Rule::type_specifier,
            })?;
            let value_type_pair = inner_pairs.next().ok_or(ParseError::MissingRule {
                expected: <SsotParser as pest::Parser>::Rule::type_specifier,
            })?;
            let key_type = parse_type_specifier(key_type_pair)?;
            let value_type = parse_type_specifier(value_type_pair)?;
            Ok(TypeSpecifier::Map(Box::new(key_type), Box::new(value_type)))
        }
        rule => Err(ParseError::UnexpectedRule {
            expected: <SsotParser as pest::Parser>::Rule::identifier, // Simplified expected rule
            found: rule,
        }),
    }
}

// --- Machine Element Parsers ---

fn parse_context_definition(
    pair: Pair<<SsotParser as pest::Parser>::Rule>,
) -> ParseResult<ContextDefinition> {
    if pair.as_rule() != <SsotParser as pest::Parser>::Rule::context_definition {
        return Err(ParseError::UnexpectedRule {
            expected: <SsotParser as pest::Parser>::Rule::context_definition,
            found: pair.as_rule(),
        });
    }
    // Grammar: annotation* "context" "@id(" integer_literal ")" "{" context_field_definition* "}"
    let mut id: Option<NumericId> = None;
    let mut fields = Vec::new();
    let mut annotations = Vec::new();

    for inner_pair in pair.into_inner() {
        match inner_pair.as_rule() {
            <SsotParser as pest::Parser>::Rule::annotation => {
                annotations.push(parse_annotation(inner_pair)?)
            }
            <SsotParser as pest::Parser>::Rule::integer_literal => {
                id = Some(parse_numeric_id(inner_pair)?)
            }
            <SsotParser as pest::Parser>::Rule::context_field_definition => {
                fields.push(parse_context_field_definition(inner_pair)?);
            }
            _ => { /* Ignore other rules like keywords */ }
        }
    }

    Ok(ContextDefinition {
        id: id.ok_or(ParseError::MissingRule {
            expected: <SsotParser as pest::Parser>::Rule::integer_literal,
        })?,
        fields,
        annotations,
    })
}

fn parse_context_field_definition(
    pair: Pair<<SsotParser as pest::Parser>::Rule>,
) -> ParseResult<ContextFieldDefinition> {
    // Grammar: annotation* identifier ":" type_specifier "@id(" integer_literal ")" annotation* ";"
    if pair.as_rule() != <SsotParser as pest::Parser>::Rule::context_field_definition {
        return Err(ParseError::UnexpectedRule {
            expected: <SsotParser as pest::Parser>::Rule::context_field_definition,
            found: pair.as_rule(),
        });
    }

    let mut name: Option<Identifier> = None;
    let mut type_spec: Option<TypeSpecifier> = None;
    let mut id: Option<NumericId> = None;
    let mut annotations = Vec::new(); // Collect all annotations together

    for inner_pair in pair.into_inner() {
        match inner_pair.as_rule() {
            <SsotParser as pest::Parser>::Rule::annotation => {
                annotations.push(parse_annotation(inner_pair)?);
            }
            <SsotParser as pest::Parser>::Rule::identifier => {
                name = Some(parse_identifier(inner_pair)?)
            }
            <SsotParser as pest::Parser>::Rule::type_specifier => {
                type_spec = Some(parse_type_specifier(inner_pair)?)
            }
            <SsotParser as pest::Parser>::Rule::integer_literal => {
                id = Some(parse_numeric_id(inner_pair)?)
            }
            _ => { /* Ignore other rules */ }
        }
    }

    Ok(ContextFieldDefinition {
        name: name.ok_or(ParseError::MissingRule {
            expected: <SsotParser as pest::Parser>::Rule::identifier,
        })?,
        type_spec: type_spec.ok_or(ParseError::MissingRule {
            expected: <SsotParser as pest::Parser>::Rule::type_specifier,
        })?,
        id: id.ok_or(ParseError::MissingRule {
            expected: <SsotParser as pest::Parser>::Rule::integer_literal,
        })?,
        annotations,
    })
}

fn parse_states_definition(
    pair: Pair<<SsotParser as pest::Parser>::Rule>,
) -> ParseResult<StatesBlock> {
    // Changed return type
    if pair.as_rule() != <SsotParser as pest::Parser>::Rule::states_definition {
        return Err(ParseError::UnexpectedRule {
            expected: <SsotParser as pest::Parser>::Rule::states_definition,
            found: pair.as_rule(),
        });
    }
    // Grammar: annotation* "states" "@id(" integer_literal ")" "{" state_definition* "}"
    let mut id: Option<NumericId> = None;
    let mut states = Vec::new();
    let mut annotations = Vec::new();

    for inner_pair in pair.into_inner() {
        match inner_pair.as_rule() {
            <SsotParser as pest::Parser>::Rule::annotation => {
                annotations.push(parse_annotation(inner_pair)?)
            }
            <SsotParser as pest::Parser>::Rule::integer_literal => {
                id = Some(parse_numeric_id(inner_pair)?)
            }
            <SsotParser as pest::Parser>::Rule::state_definition => {
                states.push(parse_state_definition(inner_pair)?);
            }
            _ => { /* Ignore other rules like keywords */ }
        }
    }

    Ok(StatesBlock {
        // Changed struct name
        id: id.ok_or(ParseError::MissingRule {
            expected: <SsotParser as pest::Parser>::Rule::integer_literal,
        })?,
        states,
        annotations,
    })
}

fn parse_state_definition(
    pair: Pair<<SsotParser as pest::Parser>::Rule>,
) -> ParseResult<StateDefinition> {
    if pair.as_rule() != <SsotParser as pest::Parser>::Rule::state_definition {
        return Err(ParseError::UnexpectedRule {
            expected: <SsotParser as pest::Parser>::Rule::state_definition,
            found: pair.as_rule(),
        });
    }
    // Grammar: annotation* "state" identifier "@id(" integer_literal ")" "{" state_element* "}"
    let mut name: Option<Identifier> = None;
    let mut id: Option<NumericId> = None;
    let mut transitions = Vec::new();
    let mut entry_actions = Vec::new();
    let mut exit_actions = Vec::new();
    let mut annotations = Vec::new();
    let mut invokes = Vec::new();
    let mut after_transitions = Vec::new();
    let mut history: Option<HistoryDefinition> = None;
    let mut regions: Vec<StatesBlock> = Vec::new();

    for inner_pair in pair.into_inner() {
        match inner_pair.as_rule() {
            <SsotParser as pest::Parser>::Rule::annotation => {
                annotations.push(parse_annotation(inner_pair)?)
            }
            <SsotParser as pest::Parser>::Rule::identifier => {
                name = Some(parse_identifier(inner_pair)?)
            }
            <SsotParser as pest::Parser>::Rule::integer_literal => {
                id = Some(parse_numeric_id(inner_pair)?)
            }
            <SsotParser as pest::Parser>::Rule::state_element => {
                // state_element is silent, look inside
                if let Some(element_pair) = inner_pair.into_inner().next() {
                    match element_pair.as_rule() {
                        <SsotParser as pest::Parser>::Rule::on_entry => {
                            // on_entry = { "onEntry" ~ "{" ~ action_ref* ~ "}" ~ ";" }
                            for action_pair in element_pair.into_inner().filter(|p| {
                                p.as_rule() == <SsotParser as pest::Parser>::Rule::action_ref
                            }) {
                                let id_pair = action_pair
                                    .into_inner()
                                    .find(|p| {
                                        p.as_rule()
                                            == <SsotParser as pest::Parser>::Rule::identifier
                                    })
                                    .unwrap();
                                entry_actions.push(parse_identifier(id_pair)?);
                            }
                        }
                        <SsotParser as pest::Parser>::Rule::on_exit => {
                            for action_pair in element_pair.into_inner().filter(|p| {
                                p.as_rule() == <SsotParser as pest::Parser>::Rule::action_ref
                            }) {
                                let id_pair = action_pair
                                    .into_inner()
                                    .find(|p| {
                                        p.as_rule()
                                            == <SsotParser as pest::Parser>::Rule::identifier
                                    })
                                    .unwrap();
                                exit_actions.push(parse_identifier(id_pair)?);
                            }
                        }
                        <SsotParser as pest::Parser>::Rule::on_transition => {
                            transitions.push(parse_on_transition(element_pair)?);
                            // Renamed function
                        }
                        <SsotParser as pest::Parser>::Rule::after_transition => {
                            after_transitions.push(parse_after_transition(element_pair)?);
                        }
                        <SsotParser as pest::Parser>::Rule::state_invoke => {
                            invokes.push(parse_state_invoke(element_pair)?);
                        }
                        <SsotParser as pest::Parser>::Rule::states_definition => {
                            // Nested states = region
                            regions.push(parse_states_definition(element_pair)?);
                        }
                        <SsotParser as pest::Parser>::Rule::history_definition => {
                            history = Some(parse_history_definition(element_pair)?);
                        }
                        // Handle annotation rules specifically if needed, otherwise parse_annotation handles them
                        <SsotParser as pest::Parser>::Rule::initial_annotation
                        | <SsotParser as pest::Parser>::Rule::final_annotation
                        | <SsotParser as pest::Parser>::Rule::parallel_annotation => {
                            // These are handled by parse_annotation and collected in the main `annotations` vec
                            // We derive flags from the `annotations` vec later.
                        }
                        _ => {
                            eprintln!(
                                "Warning: Unexpected rule inside state_element: {:?}",
                                element_pair.as_rule()
                            );
                        }
                    }
                } else {
                    eprintln!("Warning: Empty state_element encountered.");
                }
            }
            _ => { /* Ignore rules like 'state' keyword */ }
        }
    }

    // Derive flags from annotations
    let is_final = annotations.iter().any(|a| matches!(a, Annotation::Final));
    let is_parallel = annotations
        .iter()
        .any(|a| matches!(a, Annotation::Parallel))
        || !regions.is_empty(); // Parallel if annotation or regions exist
                                // Initial state marker is usually on the *parent* machine or state's annotation list referencing this state's name
                                // Or potentially an annotation directly on the state itself - check grammar
    let is_initial = annotations
        .iter()
        .any(|a| matches!(a, Annotation::InitialState(_))); // Simpler check if $initial is allowed directly on state

    Ok(StateDefinition {
        name: name.ok_or(ParseError::MissingRule {
            expected: <SsotParser as pest::Parser>::Rule::identifier,
        })?,
        id: id.ok_or(ParseError::MissingRule {
            expected: <SsotParser as pest::Parser>::Rule::integer_literal,
        })?,
        transitions,   // This should be Vec<TransitionDefinition>
        entry_actions, // Changed name
        exit_actions,  // Changed name
        annotations,
        invokes,
        after_transitions, // Added field
        history,           // Added field
        regions,           // Added field
        is_initial,
        is_final,    // Added field
        is_parallel, // Added field
    })
}

// Renamed from parse_transition_definition to avoid conflict with AST struct name
fn parse_on_transition(
    pair: Pair<<SsotParser as pest::Parser>::Rule>,
) -> ParseResult<TransitionDefinition> {
    // Grammar: "on" identifier "@id(" integer_literal ")" transition_details ";"
    if pair.as_rule() != <SsotParser as pest::Parser>::Rule::on_transition {
        return Err(ParseError::UnexpectedRule {
            expected: <SsotParser as pest::Parser>::Rule::on_transition,
            found: pair.as_rule(),
        });
    }

    let mut event: Option<Identifier> = None;
    let mut id: Option<NumericId> = None;
    let mut target: Option<TransitionTarget> = None;
    let mut guard: Option<Identifier> = None;
    let mut actions = Vec::new();
    let mut annotations = Vec::new(); // Parse specific annotations if needed

    for inner_pair in pair.into_inner() {
        match inner_pair.as_rule() {
            <SsotParser as pest::Parser>::Rule::annotation => {
                // TODO: Handle specific transition annotations like $allowedActors
                annotations.push(parse_annotation(inner_pair)?);
            }
            <SsotParser as pest::Parser>::Rule::identifier => {
                event = Some(parse_identifier(inner_pair)?)
            }
            <SsotParser as pest::Parser>::Rule::integer_literal => {
                id = Some(parse_numeric_id(inner_pair)?)
            }
            <SsotParser as pest::Parser>::Rule::transition_details => {
                // transition_details = { "transition" ~ transition_target ~ transition_block? }
                let mut details_pairs = inner_pair.into_inner();
                let target_pair = details_pairs.next().ok_or(ParseError::MissingRule {
                    expected: <SsotParser as pest::Parser>::Rule::transition_target,
                })?;
                target = Some(parse_transition_target(target_pair)?);

                if let Some(block_pair) = details_pairs.next() {
                    if block_pair.as_rule() == <SsotParser as pest::Parser>::Rule::transition_block
                    {
                        for block_inner in block_pair.into_inner() {
                            match block_inner.as_rule() {
                                <SsotParser as pest::Parser>::Rule::action_ref => {
                                    let id_pair = block_inner
                                        .into_inner()
                                        .find(|p| {
                                            p.as_rule()
                                                == <SsotParser as pest::Parser>::Rule::identifier
                                        })
                                        .unwrap();
                                    actions.push(parse_identifier(id_pair)?);
                                }
                                <SsotParser as pest::Parser>::Rule::guard_ref => {
                                    let id_pair = block_inner
                                        .into_inner()
                                        .find(|p| {
                                            p.as_rule()
                                                == <SsotParser as pest::Parser>::Rule::identifier
                                        })
                                        .unwrap();
                                    guard = Some(parse_identifier(id_pair)?);
                                }
                                // Handle annotations inside the block if needed
                                <SsotParser as pest::Parser>::Rule::annotation => {
                                    annotations.push(parse_annotation(block_inner)?);
                                }
                                _ => { /* Ignore unexpected inside block */ }
                            }
                        }
                    }
                }
            }
            _ => { /* Ignore 'on' keyword */ }
        }
    }

    Ok(TransitionDefinition {
        event: event.ok_or(ParseError::MissingRule {
            expected: <SsotParser as pest::Parser>::Rule::identifier,
        })?,
        id: id.ok_or(ParseError::MissingRule {
            expected: <SsotParser as pest::Parser>::Rule::integer_literal,
        })?,
        target: target.ok_or(ParseError::MissingRule {
            expected: <SsotParser as pest::Parser>::Rule::transition_target,
        })?,
        guards: guard.map_or(vec![], |g| vec![g]), // AST expects Vec<Identifier>
        actions,                                   // Already Vec<Identifier>
        annotations,                               // Keep annotations Vec
    })
}

// Added parser for TransitionTarget enum
fn parse_transition_target(
    pair: Pair<<SsotParser as pest::Parser>::Rule>,
) -> ParseResult<TransitionTarget> {
    // transition_target = { qualified_history_target | current_history_target | state_name_target }
    if pair.as_rule() != <SsotParser as pest::Parser>::Rule::transition_target {
        return Err(ParseError::UnexpectedRule {
            expected: <SsotParser as pest::Parser>::Rule::transition_target,
            found: pair.as_rule(),
        });
    }
    // transition_target is silent, look inside
    let inner = pair.into_inner().next().ok_or(ParseError::MissingRule {
        expected: <SsotParser as pest::Parser>::Rule::state_name_target, /* or others */
    })?;
    match inner.as_rule() {
        <SsotParser as pest::Parser>::Rule::state_name_target => {
            // state_name_target = { identifier }
            let ident_pair = inner.into_inner().next().unwrap();
            Ok(TransitionTarget::State(parse_identifier(ident_pair)?))
        }
        <SsotParser as pest::Parser>::Rule::current_history_target => {
            Ok(TransitionTarget::CurrentHistory)
        }
        <SsotParser as pest::Parser>::Rule::qualified_history_target => {
            // qualified_history_target = { identifier ~ "." ~ "history" }
            let ident_pair = inner
                .into_inner()
                .find(|p| p.as_rule() == <SsotParser as pest::Parser>::Rule::identifier)
                .ok_or(ParseError::MissingIdentifier)?;
            Ok(TransitionTarget::QualifiedHistory(parse_identifier(
                ident_pair,
            )?))
        }
    }
}

/// Parses a single key-value argument within an annotation.
fn parse_annotation_arg(pair: Pair<<SsotParser as pest::Parser>::Rule>) -> ParseResult<Argument> {
    if pair.as_rule() != <SsotParser as pest::Parser>::Rule::annotation_arg {
        return Err(ParseError::UnexpectedRule {
            expected: <SsotParser as pest::Parser>::Rule::annotation_arg,
            found: pair.as_rule(),
        });
    }
    let mut inner = pair.into_inner();
    let key = parse_identifier(inner.next().ok_or(ParseError::MissingIdentifier)?)?;
    let value_pair = inner.next().ok_or(ParseError::InvalidInput {
        message: "Missing value for annotation argument".to_string(),
    })?;

    // The value_pair's *inner* rule determines the type (string_literal, etc.)
    let value_inner_pair = value_pair
        .into_inner()
        .next()
        .ok_or(ParseError::InvalidInput {
            message: "Empty value for annotation argument".to_string(),
        })?;
    let value = parse_annotation_value(value_inner_pair)?; // Parse the actual value based on its type

    Ok(Argument { key, value })
}

/// Parses an SSOT file from the given path.
///
/// Reads the file content and then calls `parse_ssot_content`.
///
/// # Arguments
///
/// * `path` - A path reference that can be converted into a `Path`.
///
/// # Returns
///
/// * `ParseResult<SsotAst>` - The result of parsing the file content.
///
/// # Errors
///
/// Can return `ParseError::FileReadError` if the file cannot be read, or
/// any error from `parse_ssot_content` if the content is invalid.
pub fn parse_ssot_file<P: AsRef<Path>>(path: P) -> ParseResult<SsotAst> {
    let path_buf = path.as_ref().to_path_buf();
    let content = std::fs::read_to_string(&path_buf).map_err(|e| ParseError::FileReadError {
        path: path_buf.clone(),
        source: e,
    })?;
    parse_ssot_content(&content, Some(path_buf))
}

// --- Tests ---
#[cfg(test)]
mod tests {
    use super::*;
    use crate::ast::{
        // Import necessary AST nodes for comparison
        ActionDefinition,
        ActionsBlock,
        ActorDefinition,
        ActorsBlock,
        Annotation,
        AnnotationValue,
        Argument,
        AttributeDefinition,
        CommunicationBlock,
        CommunicationItem,
        ContextDefinition,
        ContextFieldDefinition,
        DeploymentConfigBlock,
        DeploymentDefinition,
        DeploymentItem,
        Duration,
        EnumDefinition,
        EnumVariant,
        EnvironmentDefinition,
        FieldDefinition,
        FileId,
        GuardDefinition,
        GuardsBlock,
        HistoryDefinition,
        HistoryType,
        Identifier,
        ImportStatement,
        InfrastructureDefinition,
        InterfaceDefinition,
        InvokeDefinition,
        InvokeSource,
        InvokesBlock,
        MachineDefinition,
        MachinesBlock,
        MethodDefinition,
        NumericId,
        ParameterDefinition,
        ServiceDefinition,
        ServiceItem,
        ServicesBlock,
        SsotAst,
        StateDefinition,
        StateInvokeDefinition,
        StatesBlock,
        StructDefinition,
        TimeUnit,
        TopLevelDefinition,
        TransitionDefinition,
        TransitionTarget,
        TypeDefinition,
        TypeSpecifier,
        TypesBlock,
    };
    use pretty_assertions::assert_eq;
    use std::io::Write;
    use tempfile::NamedTempFile;

    // Helper to create Identifier
    fn ident(name: &str) -> Identifier {
        Identifier {
            name: name.to_string(),
        }
    }

    // Helper to create NumericId
    fn num_id(value: u64) -> NumericId {
        NumericId { value }
    }

    #[test]
    fn test_parse_basic_file_structure() {
        let content = r#"
            file_id: 0xabcdef1234567890;
            import "/path/to/another.ssot";
            # This is a comment
        "#;
        let ast = parse_ssot_content(content, None).unwrap();

        assert_eq!(
            ast.file_id,
            Some(FileId {
                value: 0xabcdef1234567890
            })
        );
        assert_eq!(ast.imports.len(), 1);
        assert_eq!(
            ast.imports[0],
            ImportStatement {
                path: "/path/to/another.ssot".to_string()
            }
        );
        assert!(ast.definitions.is_empty());
    }

    #[test]
    fn test_parse_simple_types_block() {
        let content = r#"
            file_id: 0x1;
            types {
                $description("Some types");

                struct Point @id(0) {
                    x: i32 @id(0);
                    y: i32 @id(1) { $meta(unit: "pixels"); };
                }

                enum Color @id(1) {
                    RED @id(0);
                    GREEN @id(1);
                    BLUE @id(2) { $description("Primary blue"); };
                }
            }
        "#;
        let ast = parse_ssot_content(content, None).unwrap();
        assert_eq!(ast.definitions.len(), 1);

        match &ast.definitions[0] {
            TopLevelDefinition::Types(types_block) => {
                assert_eq!(types_block.annotations.len(), 1);
                assert!(
                    matches!(&types_block.annotations[0], Annotation::Description(s) if s == "Some types")
                );
                assert_eq!(types_block.definitions.len(), 2);

                // Check Struct
                match &types_block.definitions[0] {
                    TypeDefinition::Struct(s) => {
                        assert_eq!(s.name.name, "Point");
                        assert_eq!(s.id.value, 0);
                        assert_eq!(s.fields.len(), 2);
                        assert_eq!(s.fields[0].name.name, "x");
                        assert_eq!(s.fields[0].id.value, 0);
                        assert!(
                            matches!(s.fields[0].type_spec, TypeSpecifier::Simple(ref id) if id.name == "i32")
                        );
                        assert_eq!(s.fields[1].name.name, "y");
                        assert_eq!(s.fields[1].id.value, 1);
                        assert!(
                            matches!(s.fields[1].type_spec, TypeSpecifier::Simple(ref id) if id.name == "i32")
                        );
                        assert_eq!(s.fields[1].annotations.len(), 1);
                        assert!(
                            matches!(&s.fields[1].annotations[0], Annotation::Meta(args) if
                                args.len() == 1 &&
                                args[0].key.name == "unit" &&
                                matches!(args[0].value, AnnotationValue::String(ref s) if s == "pixels")
                            )
                        ); // Added borrow
                    }
                    _ => panic!("Expected StructDefinition"),
                }

                // Check Enum
                match &types_block.definitions[1] {
                    TypeDefinition::Enum(e) => {
                        assert_eq!(e.name.name, "Color");
                        assert_eq!(e.id.value, 1);
                        assert_eq!(e.variants.len(), 3);
                        assert_eq!(e.variants[0].name.name, "RED");
                        assert_eq!(e.variants[0].id.value, 0);
                        assert_eq!(e.variants[1].name.name, "GREEN");
                        assert_eq!(e.variants[1].id.value, 1);
                        assert_eq!(e.variants[2].name.name, "BLUE");
                        assert_eq!(e.variants[2].id.value, 2);
                        assert_eq!(e.variants[2].annotations.len(), 1);
                        assert!(
                            matches!(&e.variants[2].annotations[0], Annotation::Description(s) if s == "Primary blue")
                        );
                    }
                    _ => panic!("Expected EnumDefinition"),
                }
            }
            _ => panic!("Expected Types block"),
        }
    }

    #[test]
    fn test_parse_simple_machines_block() {
        let content = r#"
            file_id: 0x2;
            machines {
                machine SimpleMachine @id(0) {
                    context @id(0) {
                        counter: i32 @id(0) { $default(0); }; // Use AnnotationValue for default
                    }
                    states @id(1) {
                        $initial(Idle); // Annotation determines initial state
                        state Idle @id(0) {
                            on START -> Running;
                        }
                        state Running @id(1) {
                            on STOP -> Idle;
                            after 5 seconds { action StopAction; }
                            invoke TimerInvoke @id(0) (src: invokes.Timer);
                            history deep @id(0) target SubIdle;
                            $parallel;
                            states SubRegion @id(0) {
                                $final(SubIdle); // Added final state marker
                                state SubIdle @id(0){ $final; }
                            }
                        }
                    }
                    actions @id(2) {
                        action StopAction @id(0);
                    }
                    guards @id(3) {
                        guard CheckGuard @id(0);
                    }
                    invokes @id(4) {
                        invoke Timer @id(0) (src: "timerService.startTimer");
                    }
                }
            }
        "#;
        // Note: Parsing for actions, guards, invokes, after, history, invoke in state is NOT fully implemented yet
        let ast = parse_ssot_content(content, None).expect("Parse failed");
        assert_eq!(ast.definitions.len(), 1);

        match &ast.definitions[0] {
            TopLevelDefinition::Machines(machine_block) => {
                assert_eq!(machine_block.definitions.len(), 1);
                let machine = &machine_block.definitions[0];
                assert_eq!(machine.name.name, "SimpleMachine");
                assert_eq!(machine.id.value, 0);

                // Check context (assuming basic parsing works)
                assert!(machine.context.is_some());
                let context = machine.context.as_ref().unwrap();
                assert_eq!(context.id.value, 0);
                assert_eq!(context.fields.len(), 1);
                assert_eq!(context.fields[0].name.name, "counter");
                // Check for $default annotation (requires Annotation parsing)
                // assert!(context.fields[0].annotations.iter()
                //     .any(|a| matches!(a, Annotation::Default(val) if matches!(*val, AnnotationValue::Integer(0)))));

                // Check states block (basic structure)
                assert!(machine.states.is_some());
                let states_block = machine.states.as_ref().unwrap();
                assert_eq!(states_block.id.value, 1);
                assert_eq!(states_block.states.len(), 2);
                // Check for $initial annotation
                assert!(states_block
                    .annotations
                    .iter()
                    .any(|a| matches!(a, Annotation::InitialState(id) if id.name == "Idle")));

                // Check Idle state
                let idle_state = &states_block.states[0];
                assert_eq!(idle_state.name.name, "Idle");
                assert_eq!(idle_state.id.value, 0);
                assert_eq!(idle_state.transitions.len(), 1);
                assert_eq!(idle_state.transitions[0].event.name, "START");
                assert!(
                    matches!(idle_state.transitions[0].target, TransitionTarget::State(ref id) if id.name == "Running")
                );

                // Check Running state
                let running_state = &states_block.states[1];
                assert_eq!(running_state.name.name, "Running");
                assert_eq!(running_state.id.value, 1);
                assert!(running_state.is_parallel);
                assert_eq!(running_state.transitions.len(), 1);
                assert_eq!(running_state.transitions[0].event.name, "STOP");
                assert!(
                    matches!(running_state.transitions[0].target, TransitionTarget::State(ref id) if id.name == "Idle")
                );
                // assert_eq!(running_state.after.len(), 1); // Need parse_after_transition
                // assert_eq!(running_state.after[0].delay.value, 5);
                // assert_eq!(running_state.after[0].actions.len(), 1);
                // assert_eq!(running_state.after[0].actions[0].name, "StopAction");
                // assert_eq!(running_state.invokes.len(), 1); // Need parse_state_invoke
                // assert_eq!(running_state.invokes[0].name.name, "TimerInvoke");
                // assert!(running_state.history.is_some()); // Need parse_history_definition
                // assert_eq!(
                //     running_state.history.as_ref().unwrap().history_type,
                //     HistoryType::Deep
                // );
                assert_eq!(running_state.regions.len(), 1);
                assert_eq!(running_state.regions[0].id.value, 0);
                assert!(running_state.regions[0]
                    .annotations
                    .iter()
                    .any(|a| matches!(a, Annotation::FinalState(id) if id.name == "SubIdle"))); // Updated Check for $final annotation on region
                assert_eq!(running_state.regions[0].states.len(), 1);
                assert_eq!(running_state.regions[0].states[0].name.name, "SubIdle");
                assert!(running_state.regions[0].states[0].is_final);

                // Check Actions, Guards, Invokes (basic check based on TODO placeholders)
                // assert!(machine.actions.is_some()); // Need parse_actions_block
                // assert_eq!(machine.actions.as_ref().unwrap().actions.len(), 1);
                // assert!(machine.guards.is_some()); // Need parse_guards_block
                // assert_eq!(machine.guards.as_ref().unwrap().guards.len(), 1);
                // assert!(machine.invokes.is_some()); // Need parse_invokes_block
                // assert_eq!(machine.invokes.as_ref().unwrap().invokes.len(), 1);
            }
            _ => panic!("Expected Machines block"),
        }
    }

    #[test]
    fn test_parse_error_handling() {
        let invalid_content = "file_id: 0x1 types { struct Oops }"; // Missing semicolon, closing brace
        let result = parse_ssot_content(invalid_content, None);
        assert!(result.is_err());
        if let Err(ParseError::PestError(e)) = result {
            // Check that the error points roughly to the right place
            // Specific error messages depend on the grammar details
            println!("Pest Error: {}", e);
        } else {
            panic!("Expected PestError, got {:?}", result);
        }
    }

    #[test]
    fn test_parse_parallel_state() {
        let content = r#"
            file_id: 0x10; # Example ID
            machines {
                machine ParallelMachine @id(0) {
                    states @id(0) {
                        state ParallelState @id(0) { # State marked parallel implicitly by having regions
                            $parallel;
                            states RegionA @id(0) { # Region 1
                                $initial(A1);
                                state A1 @id(0);
                                state A2 @id(1);
                            }
                            states RegionB @id(1) { # Region 2
                                $initial(B1);
                                state B1 @id(0);
                                state B2 @id(1);
                            }
                        }
                    }
                }
            }
        "#;
        let ast = parse_ssot_content(content, None).unwrap();
        match &ast.definitions[0] {
            TopLevelDefinition::Machines(machine_block) => {
                assert_eq!(machine_block.definitions.len(), 1);
                let machine = &machine_block.definitions[0];
                assert!(machine.states.is_some());
                let states_block = machine.states.as_ref().unwrap(); // Added unwrap
                assert_eq!(states_block.states.len(), 1);
                let parallel_state = &states_block.states[0]; // Added unwrap
                assert_eq!(parallel_state.name.name, "ParallelState");
                assert!(parallel_state.is_parallel); // Check derived flag
                assert_eq!(parallel_state.regions.len(), 2);
                assert_eq!(parallel_state.regions[0].id.value, 0);
                assert_eq!(parallel_state.regions[0].states.len(), 2);
                assert_eq!(parallel_state.regions[1].id.value, 1);
                assert_eq!(parallel_state.regions[1].states.len(), 2);
            }
            _ => panic!("Expected Machines block"),
        }
    }

    #[test]
    fn test_parse_services_block() {
        let content = r#"
            file_id: 0xa;
            services {
                interface Greeter @id(0) {
                    method SayHello @id(0) (name: string @id(0)) -> string @id(0);
                }
                service MyGreeter @id(1) {
                    $implements(Greeter);
                    $description("A simple greeter service");
                }
            }
        "#;
        let ast = parse_ssot_content(content, None).unwrap();
        assert_eq!(ast.definitions.len(), 1);
        match &ast.definitions[0] {
            TopLevelDefinition::Services(services_block) => {
                assert_eq!(services_block.definitions.len(), 2);

                // Check Interface
                match &services_block.definitions[0] {
                    ServiceItem::Interface(i) => {
                        assert_eq!(i.name.name, "Greeter");
                        assert_eq!(i.id.value, 0);
                        assert_eq!(i.methods.len(), 1);
                        assert_eq!(i.methods[0].name.name, "SayHello");
                    }
                    _ => panic!("Expected Interface"),
                }

                // Check Service
                match &services_block.definitions[1] {
                    ServiceItem::Service(s) => {
                        assert_eq!(s.name.name, "MyGreeter");
                        assert_eq!(s.id.value, 1);
                        assert_eq!(s.annotations.len(), 2); // $implements and $description
                        assert!(s.annotations.iter().any(
                            |a| matches!(a, Annotation::Implements(id) if id.name == "Greeter")
                        ));
                        assert!(s.annotations.iter().any(|a| matches!(a, Annotation::Description(d) if d == "A simple greeter service")));
                    }
                    _ => panic!("Expected Service"),
                }
            }
            _ => panic!("Expected Services block"),
        }
    }

    #[test]
    fn test_parse_error_missing_brace() {
        let content = r#"
            file_id: 0xb;
            types {
                struct Point @id(0) {
                    x: i32 @id(0);
                // Missing closing brace for struct and types block
        "#;
        let result = parse_ssot_content(content, None);
        assert!(result.is_err());
        // Optionally check the specific error type if needed
        // assert!(matches!(result.unwrap_err(), ParseError::PestError(_)));
    }

    #[test]
    fn test_parse_from_file() {
        let content = "file_id: 0x9;";
        let mut temp_file = NamedTempFile::new().unwrap();
        writeln!(temp_file, "{}", content).unwrap();

        let ast_from_file = parse_ssot_file(temp_file.path()).unwrap();
        let ast_from_content =
            parse_ssot_content(content, Some(temp_file.path().to_path_buf())).unwrap();

        assert_eq!(ast_from_file.file_id, Some(FileId { value: 0x9 }));
        assert_eq!(
            ast_from_file.source_path,
            Some(temp_file.path().to_path_buf())
        );
        // Compare relevant fields (ignore source_path potentially)
        assert_eq!(ast_from_file.file_id, ast_from_content.file_id);
        assert_eq!(ast_from_file.imports, ast_from_content.imports);
        assert_eq!(ast_from_file.definitions, ast_from_content.definitions);
    }

    #[test]
    fn test_parse_file_not_found() {
        let result = parse_ssot_file("non_existent_file.ssot");
        assert!(result.is_err());
        assert!(matches!(result, Err(ParseError::FileReadError { .. })));
    }
}

// --- Stub Implementations for Missing Parsers ---

fn parse_services_block(_pair: Pair<<SsotParser as pest::Parser>::Rule>) -> ParseResult<ServicesBlock> {
    Err(ParseError::InvalidInput { message: "Parsing for services_block not yet implemented".to_string() })
}

fn parse_communication_block(_pair: Pair<<SsotParser as pest::Parser>::Rule>) -> ParseResult<CommunicationBlock> {
    Err(ParseError::InvalidInput { message: "Parsing for communication_block not yet implemented".to_string() })
}

fn parse_actors_block(_pair: Pair<<SsotParser as pest::Parser>::Rule>) -> ParseResult<ActorsBlock> {
    Err(ParseError::InvalidInput { message: "Parsing for actors_block not yet implemented".to_string() })
}

fn parse_deployment_config_block(_pair: Pair<<SsotParser as pest::Parser>::Rule>) -> ParseResult<DeploymentConfigBlock> {
    Err(ParseError::InvalidInput { message: "Parsing for deployment_config_block not yet implemented".to_string() })
}

fn parse_actions_block(_pair: Pair<<SsotParser as pest::Parser>::Rule>) -> ParseResult<ActionsBlock> {
    Err(ParseError::InvalidInput { message: "Parsing for actions_block not yet implemented".to_string() })
}
