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
use pest::iterators::{Pair, Pairs};
use pest::Parser;
use pest_derive::Parser;
use std::path::{Path, PathBuf};
use thiserror::Error;

#[derive(Parser)]
#[grammar = "ssot.pest"] // Path relative to src
struct SsotParser;

// Bring Rule into scope
use self::Rule;

/// Represents errors that can occur during the parsing of an SSOT file or content.
#[derive(Error, Debug)]
pub enum ParseError {
    /// An error occurred within the Pest parser.
    #[error("Pest parsing error: {0}")]
    PestError(#[from] pest::error::Error<SsotParser::Rule>),
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
        expected: SsotParser::Rule,
        found: SsotParser::Rule,
    },
    /// A required grammar rule was missing from the input.
    #[error("Missing expected rule: {expected:?}")]
    MissingRule { expected: SsotParser::Rule },
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
    let pairs = SsotParser::parse(SsotParser::Rule::file, content)?;

    let mut ast = SsotAst {
        source_path,
        file_id: None,
        imports: Vec::new(),
        definitions: Vec::new(),
    };

    let file_pair = pairs.peek().ok_or_else(|| ParseError::InvalidInput {
        message: "Empty input".to_string(),
    })?;

    if file_pair.as_rule() != SsotParser::Rule::file {
        return Err(ParseError::UnexpectedRule {
            expected: SsotParser::Rule::file,
            found: file_pair.as_rule(),
        });
    }

    // Iterate over the inner pairs of the 'file' rule
    for pair in file_pair.into_inner() {
        match pair.as_rule() {
            SsotParser::Rule::COMMENT => { /* Skip comments */ }
            SsotParser::Rule::EOI => { /* End of Input, expected */ }

            SsotParser::Rule::definition => {
                // Handle the PUSHed definition rule
                // Get the actual definition *inside* the 'definition' pair
                if let Some(inner_definition_pair) = pair.into_inner().next() {
                    match inner_definition_pair.as_rule() {
                        SsotParser::Rule::file_id => {
                            if ast.file_id.is_some() {
                                eprintln!(
                                    "Warning: Duplicate file ID found, ignoring subsequent IDs."
                                );
                            } else {
                                ast.file_id = Some(parse_file_id(inner_definition_pair)?);
                            }
                        }
                        SsotParser::Rule::import_statement => {
                            ast.imports
                                .push(parse_import_statement(inner_definition_pair)?);
                        }
                        SsotParser::Rule::types_block => {
                            ast.definitions
                                .push(TopLevelDefinition::Types(parse_types_block(
                                    inner_definition_pair,
                                )?));
                        }
                        SsotParser::Rule::machines_block => {
                            ast.definitions.push(TopLevelDefinition::Machines(
                                parse_machines_block(inner_definition_pair)?,
                            ));
                        }
                        SsotParser::Rule::actors_block => {
                            ast.definitions
                                .push(TopLevelDefinition::Actors(parse_actors_block(
                                    inner_definition_pair,
                                )?));
                        }
                        SsotParser::Rule::communication_block => {
                            ast.definitions.push(TopLevelDefinition::Communication(
                                parse_communication_block(inner_definition_pair)?,
                            ));
                        }
                        SsotParser::Rule::services_block => {
                            ast.definitions.push(TopLevelDefinition::Services(
                                parse_services_block(inner_definition_pair)?,
                            ));
                        }
                        SsotParser::Rule::deployment_config_block => {
                            ast.definitions.push(TopLevelDefinition::DeploymentConfig(
                                parse_deployment_config_block(inner_definition_pair)?,
                            ));
                        }
                        SsotParser::Rule::annotation => {
                            eprintln!("Skipping top-level annotation definition for now.");
                            // TODO: Handle top-level annotations if needed (e.g., store them separately)
                        }
                        // Handle unexpected rule *inside* definition
                        inner_rule => {
                            eprintln!(
                                "Warning: Skipping unexpected rule inside definition: {:?}",
                                inner_rule
                            );
                        }
                    }
                } else {
                    eprintln!("Warning: Found empty definition rule.");
                }
            }

            // Catch rules that are not COMMENT, EOI, or definition
            other_rule => {
                eprintln!(
                    "Warning: Skipping unexpected element at top level: {:?}",
                    other_rule
                );
            }
        }
    }

    Ok(ast)
}

// Helper function to extract text from a pair
fn get_text(pair: Pair<SsotParser::Rule>) -> &str {
    pair.as_str()
}

fn parse_file_id(pair: Pair<SsotParser::Rule>) -> ParseResult<FileId> {
    if pair.as_rule() != SsotParser::Rule::file_id {
        return Err(ParseError::UnexpectedRule {
            expected: SsotParser::Rule::file_id,
            found: pair.as_rule(),
        });
    }
    let mut inner_pairs = pair.into_inner();
    let hex_pair = inner_pairs
        .find(|p| p.as_rule() == SsotParser::Rule::hex_literal)
        .ok_or_else(|| ParseError::MissingRule {
            expected: SsotParser::Rule::hex_literal,
        })?;
    Ok(FileId {
        value: u64::from_str_radix(hex_pair.as_str().trim_start_matches("0x"), 16)?,
    })
}

fn parse_import_statement(pair: Pair<SsotParser::Rule>) -> ParseResult<ImportStatement> {
    if pair.as_rule() != SsotParser::Rule::import_statement {
        return Err(ParseError::UnexpectedRule {
            expected: SsotParser::Rule::import_statement,
            found: pair.as_rule(),
        });
    }
    let mut inner_pairs = pair.into_inner();
    let path_pair = inner_pairs
        .find(|p| p.as_rule() == SsotParser::Rule::string_literal)
        .ok_or_else(|| ParseError::MissingRule {
            expected: SsotParser::Rule::string_literal,
        })?;

    // Remove quotes from the string literal
    let path_str = path_pair.as_str();
    let path = path_str[1..path_str.len() - 1].to_string();
    Ok(ImportStatement(path))
}

fn parse_identifier(pair: Pair<SsotParser::Rule>) -> ParseResult<Identifier> {
    if pair.as_rule() != SsotParser::Rule::identifier {
        return Err(ParseError::UnexpectedRule {
            expected: SsotParser::Rule::identifier,
            found: pair.as_rule(),
        });
    }
    Ok(Identifier {
        name: pair.as_str().to_string(),
    })
}

fn parse_numeric_id(pair: Pair<SsotParser::Rule>) -> ParseResult<NumericId> {
    if pair.as_rule() != SsotParser::Rule::integer_literal {
        return Err(ParseError::UnexpectedRule {
            expected: SsotParser::Rule::integer_literal,
            found: pair.as_rule(),
        });
    }
    let value = pair.as_str().parse::<u64>()?;
    Ok(NumericId { value })
}

fn parse_types_block(pair: Pair<SsotParser::Rule>) -> ParseResult<TypesBlock> {
    if pair.as_rule() != SsotParser::Rule::types_block {
        return Err(ParseError::UnexpectedRule {
            expected: SsotParser::Rule::types_block,
            found: pair.as_rule(),
        });
    }

    let mut annotations = Vec::new();
    let mut definitions = Vec::new();

    for inner_pair in pair.into_inner() {
        match inner_pair.as_rule() {
            SsotParser::Rule::annotation => annotations.push(parse_annotation(inner_pair)?),
            SsotParser::Rule::type_definition => {
                // type_definition is silent (_), need to go one level deeper
                if let Some(definition_pair) = inner_pair.into_inner().next() {
                    match definition_pair.as_rule() {
                        SsotParser::Rule::struct_definition => {
                            definitions.push(TypeDefinition::Struct(parse_struct_definition(
                                definition_pair,
                            )?));
                        }
                        SsotParser::Rule::enum_definition => {
                            definitions.push(TypeDefinition::Enum(parse_enum_definition(
                                definition_pair,
                            )?));
                        }
                        _ => {
                            return Err(ParseError::UnexpectedRule {
                                expected: SsotParser::Rule::struct_definition, // Or enum
                                found: definition_pair.as_rule(),
                            });
                        }
                    }
                } else {
                    // Handle case where type_definition is empty (shouldn't happen with non-empty grammar)
                    eprintln!("Warning: Empty type_definition encountered.");
                }
            }
            _ => {
                return Err(ParseError::UnexpectedRule {
                    expected: SsotParser::Rule::annotation, // Or type_definition
                    found: inner_pair.as_rule(),
                });
            }
        }
    }

    Ok(TypesBlock {
        annotations,
        definitions,
    })
}

fn parse_machines_block(pair: Pair<SsotParser::Rule>) -> ParseResult<MachinesBlock> {
    if pair.as_rule() != SsotParser::Rule::machines_block {
        return Err(ParseError::UnexpectedRule {
            expected: SsotParser::Rule::machines_block,
            found: pair.as_rule(),
        });
    }

    let mut annotations = Vec::new();
    let mut definitions = Vec::new();

    for inner_pair in pair.into_inner() {
        match inner_pair.as_rule() {
            SsotParser::Rule::annotation => annotations.push(parse_annotation(inner_pair)?),
            SsotParser::Rule::machine_definition => {
                definitions.push(parse_machine_definition(inner_pair)?);
            }
            _ => {
                return Err(ParseError::UnexpectedRule {
                    expected: SsotParser::Rule::annotation, // Or machine_definition
                    found: inner_pair.as_rule(),
                });
            }
        }
    }

    Ok(MachinesBlock {
        annotations,
        definitions,
    })
}

fn parse_struct_definition(pair: Pair<SsotParser::Rule>) -> ParseResult<StructDefinition> {
    if pair.as_rule() != SsotParser::Rule::struct_definition {
        return Err(ParseError::UnexpectedRule {
            expected: SsotParser::Rule::struct_definition,
            found: pair.as_rule(),
        });
    }

    let mut annotations = Vec::new();
    let mut name = None;
    let mut id = None;
    let mut fields = Vec::new();

    for inner_pair in pair.into_inner() {
        match inner_pair.as_rule() {
            SsotParser::Rule::annotation => annotations.push(parse_annotation(inner_pair)?),
            SsotParser::Rule::identifier => name = Some(parse_identifier(inner_pair)?),
            SsotParser::Rule::integer_literal => id = Some(parse_numeric_id(inner_pair)?),
            SsotParser::Rule::field_definition => fields.push(parse_field_definition(inner_pair)?),
            _ => {
                return Err(ParseError::UnexpectedRule {
                    expected: SsotParser::Rule::identifier, // Or others
                    found: inner_pair.as_rule(),
                });
            }
        }
    }

    Ok(StructDefinition {
        annotations,
        name: name.ok_or_else(|| ParseError::MissingRule {
            expected: SsotParser::Rule::identifier,
        })?,
        id: id.ok_or_else(|| ParseError::MissingRule {
            expected: SsotParser::Rule::integer_literal,
        })?,
        fields,
    })
}

fn parse_field_definition(pair: Pair<SsotParser::Rule>) -> ParseResult<FieldDefinition> {
    if pair.as_rule() != SsotParser::Rule::field_definition {
        return Err(ParseError::UnexpectedRule {
            expected: SsotParser::Rule::field_definition,
            found: pair.as_rule(),
        });
    }

    let mut annotations = Vec::new();
    let mut name = None;
    let mut type_spec = None;
    let mut id = None;

    // The rule itself might have annotations before the identifier
    // And potentially annotations after the id (as per grammar `annotation* ;`)
    let mut main_annotations = Vec::new(); // Annotations directly modifying the field

    for inner_pair in pair.into_inner() {
        match inner_pair.as_rule() {
            SsotParser::Rule::annotation => {
                // Decide if it's a pre-identifier or post-id annotation
                if name.is_none() {
                    annotations.push(parse_annotation(inner_pair)?);
                } else {
                    main_annotations.push(parse_annotation(inner_pair)?);
                }
            }
            SsotParser::Rule::identifier => name = Some(parse_identifier(inner_pair)?),
            SsotParser::Rule::type_specifier => type_spec = Some(parse_type_specifier(inner_pair)?),
            SsotParser::Rule::integer_literal => id = Some(parse_numeric_id(inner_pair)?),
            _ => {
                return Err(ParseError::UnexpectedRule {
                    expected: SsotParser::Rule::identifier, // Or others
                    found: inner_pair.as_rule(),
                });
            }
        }
    }

    Ok(FieldDefinition {
        annotations, // Annotations found before the field identifier
        name: name.ok_or_else(|| ParseError::MissingRule {
            expected: SsotParser::Rule::identifier,
        })?,
        type_specifier: type_spec.ok_or_else(|| ParseError::MissingRule {
            expected: SsotParser::Rule::type_specifier,
        })?,
        id: id.ok_or_else(|| ParseError::MissingRule {
            expected: SsotParser::Rule::integer_literal,
        })?,
        field_annotations: main_annotations, // Annotations found after the ID
    })
}

fn parse_enum_definition(pair: Pair<SsotParser::Rule>) -> ParseResult<EnumDefinition> {
    if pair.as_rule() != SsotParser::Rule::enum_definition {
        return Err(ParseError::UnexpectedRule {
            expected: SsotParser::Rule::enum_definition,
            found: pair.as_rule(),
        });
    }

    let mut annotations = Vec::new();
    let mut name = None;
    let mut id = None;
    let mut variants = Vec::new();

    for inner_pair in pair.into_inner() {
        match inner_pair.as_rule() {
            SsotParser::Rule::annotation => annotations.push(parse_annotation(inner_pair)?),
            SsotParser::Rule::identifier => name = Some(parse_identifier(inner_pair)?),
            SsotParser::Rule::integer_literal => id = Some(parse_numeric_id(inner_pair)?),
            SsotParser::Rule::enum_variant => variants.push(parse_enum_variant(inner_pair)?),
            _ => {
                return Err(ParseError::UnexpectedRule {
                    expected: SsotParser::Rule::identifier, // Or others
                    found: inner_pair.as_rule(),
                });
            }
        }
    }

    Ok(EnumDefinition {
        annotations,
        name: name.ok_or_else(|| ParseError::MissingRule {
            expected: SsotParser::Rule::identifier,
        })?,
        id: id.ok_or_else(|| ParseError::MissingRule {
            expected: SsotParser::Rule::integer_literal,
        })?,
        variants,
    })
}

fn parse_enum_variant(pair: Pair<SsotParser::Rule>) -> ParseResult<EnumVariant> {
    if pair.as_rule() != SsotParser::Rule::enum_variant {
        return Err(ParseError::UnexpectedRule {
            expected: SsotParser::Rule::enum_variant,
            found: pair.as_rule(),
        });
    }

    let mut annotations = Vec::new();
    let mut name = None;
    let mut id = None;
    let mut variant_annotations = Vec::new(); // Annotations appearing after the ID

    for inner_pair in pair.into_inner() {
        match inner_pair.as_rule() {
            SsotParser::Rule::annotation => {
                if name.is_none() {
                    annotations.push(parse_annotation(inner_pair)?);
                } else {
                    variant_annotations.push(parse_annotation(inner_pair)?);
                }
            }
            SsotParser::Rule::identifier => name = Some(parse_identifier(inner_pair)?),
            SsotParser::Rule::integer_literal => id = Some(parse_numeric_id(inner_pair)?),
            _ => {
                return Err(ParseError::UnexpectedRule {
                    expected: SsotParser::Rule::identifier, // Or others
                    found: inner_pair.as_rule(),
                });
            }
        }
    }

    Ok(EnumVariant {
        annotations, // Annotations before the variant name
        name: name.ok_or_else(|| ParseError::MissingRule {
            expected: SsotParser::Rule::identifier,
        })?,
        id: id.ok_or_else(|| ParseError::MissingRule {
            expected: SsotParser::Rule::integer_literal,
        })?,
        variant_annotations, // Annotations after the ID
    })
}

fn parse_machine_definition(pair: Pair<SsotParser::Rule>) -> ParseResult<MachineDefinition> {
    if pair.as_rule() != SsotParser::Rule::machine_definition {
        return Err(ParseError::UnexpectedRule {
            expected: SsotParser::Rule::machine_definition,
            found: pair.as_rule(),
        });
    }

    let mut annotations = Vec::new();
    let mut name = None;
    let mut id = None;
    let mut context = None;
    let mut states_block = None;
    let mut actions = None; // Initialize Optional fields
    let mut guards = None;
    let mut invokes = None;

    for inner_pair in pair.into_inner() {
        match inner_pair.as_rule() {
            SsotParser::Rule::annotation => annotations.push(parse_annotation(inner_pair)?),
            SsotParser::Rule::identifier => name = Some(parse_identifier(inner_pair)?),
            SsotParser::Rule::integer_literal => id = Some(parse_numeric_id(inner_pair)?),
            SsotParser::Rule::machine_element => {
                // machine_element is silent, look inside
                if let Some(element_pair) = inner_pair.into_inner().next() {
                    match element_pair.as_rule() {
                        SsotParser::Rule::context_definition => {
                            context = Some(parse_context_definition(element_pair)?);
                        }
                        SsotParser::Rule::states_definition => {
                            states_block = Some(parse_states_definition(element_pair)?);
                        }
                        // --- TODO: Add parsing for actions, guards, invokes blocks ---
                        SsotParser::Rule::actions_definition => {
                            actions = Some(parse_actions_block(element_pair)?)
                        }
                        SsotParser::Rule::guards_definition => {
                            guards = Some(parse_guards_block(element_pair)?)
                        }
                        SsotParser::Rule::invokes_definition => {
                            invokes = Some(parse_invokes_block(element_pair)?)
                        }
                        _ => {
                            return Err(ParseError::UnexpectedRule {
                                expected: SsotParser::Rule::context_definition, // Or states_definition, etc.
                                found: element_pair.as_rule(),
                            });
                        }
                    }
                } else {
                    eprintln!("Warning: Empty machine_element encountered.");
                }
            }
            _ => {
                return Err(ParseError::UnexpectedRule {
                    expected: SsotParser::Rule::identifier, // Or others
                    found: inner_pair.as_rule(),
                });
            }
        }
    }

    Ok(MachineDefinition {
        annotations,
        name: name.ok_or_else(|| ParseError::MissingRule {
            expected: SsotParser::Rule::identifier,
        })?,
        id: id.ok_or_else(|| ParseError::MissingRule {
            expected: SsotParser::Rule::integer_literal,
        })?,
        context,
        states: states_block.ok_or_else(|| ParseError::MissingRule {
            expected: SsotParser::Rule::states_definition,
        })?,
        actions,
        guards,
        invokes,
        // Initialize other fields as needed
        // events: None, // Example, if events were parsed separately
    })
}

fn parse_context_definition(pair: Pair<SsotParser::Rule>) -> ParseResult<ContextDefinition> {
    if pair.as_rule() != SsotParser::Rule::context_definition {
        return Err(ParseError::UnexpectedRule {
            expected: SsotParser::Rule::context_definition,
            found: pair.as_rule(),
        });
    }

    let mut annotations = Vec::new();
    let mut id = None;
    let mut fields = Vec::new();

    for inner_pair in pair.into_inner() {
        match inner_pair.as_rule() {
            SsotParser::Rule::annotation => annotations.push(parse_annotation(inner_pair)?),
            SsotParser::Rule::integer_literal => id = Some(parse_numeric_id(inner_pair)?),
            SsotParser::Rule::context_field_definition => {
                fields.push(parse_context_field_definition(inner_pair)?);
            }
            _ => {
                return Err(ParseError::UnexpectedRule {
                    expected: SsotParser::Rule::integer_literal, // Or context_field_definition
                    found: inner_pair.as_rule(),
                });
            }
        }
    }

    Ok(ContextDefinition {
        annotations,
        id: id.ok_or_else(|| ParseError::MissingRule {
            expected: SsotParser::Rule::integer_literal,
        })?,
        fields,
    })
}

fn parse_context_field_definition(
    pair: Pair<SsotParser::Rule>,
) -> ParseResult<ContextFieldDefinition> {
    if pair.as_rule() != SsotParser::Rule::context_field_definition {
        return Err(ParseError::UnexpectedRule {
            expected: SsotParser::Rule::context_field_definition,
            found: pair.as_rule(),
        });
    }

    let mut annotations = Vec::new(); // Annotations before the identifier
    let mut name = None;
    let mut type_spec = None;
    let mut id = None;
    let mut field_annotations = Vec::new(); // Annotations after the ID (e.g., $default)

    for inner_pair in pair.into_inner() {
        match inner_pair.as_rule() {
            SsotParser::Rule::annotation => {
                // Annotations can appear before the identifier or after the ID
                if name.is_none() {
                    annotations.push(parse_annotation(inner_pair)?);
                } else {
                    field_annotations.push(parse_annotation(inner_pair)?);
                }
            }
            SsotParser::Rule::identifier => name = Some(parse_identifier(inner_pair)?),
            SsotParser::Rule::type_specifier => type_spec = Some(parse_type_specifier(inner_pair)?),
            SsotParser::Rule::integer_literal => id = Some(parse_numeric_id(inner_pair)?),
            _ => {
                return Err(ParseError::UnexpectedRule {
                    expected: SsotParser::Rule::identifier, // Or others
                    found: inner_pair.as_rule(),
                });
            }
        }
    }

    Ok(ContextFieldDefinition {
        annotations, // Annotations before the name
        name: name.ok_or_else(|| ParseError::MissingRule {
            expected: SsotParser::Rule::identifier,
        })?,
        type_specifier: type_spec.ok_or_else(|| ParseError::MissingRule {
            expected: SsotParser::Rule::type_specifier,
        })?,
        id: id.ok_or_else(|| ParseError::MissingRule {
            expected: SsotParser::Rule::integer_literal,
        })?,
        field_annotations, // Annotations after the id (like $default)
    })
}

fn parse_states_definition(pair: Pair<SsotParser::Rule>) -> ParseResult<StatesBlock> {
    if pair.as_rule() != SsotParser::Rule::states_definition {
        return Err(ParseError::UnexpectedRule {
            expected: SsotParser::Rule::states_definition,
            found: pair.as_rule(),
        });
    }

    let mut annotations = Vec::new();
    let mut id = None;
    let mut states = Vec::new();

    for inner_pair in pair.into_inner() {
        match inner_pair.as_rule() {
            SsotParser::Rule::annotation => annotations.push(parse_annotation(inner_pair)?),
            SsotParser::Rule::integer_literal => id = Some(parse_numeric_id(inner_pair)?),
            SsotParser::Rule::state_definition => states.push(parse_state_definition(inner_pair)?),
            _ => {
                return Err(ParseError::UnexpectedRule {
                    expected: SsotParser::Rule::integer_literal, // Or state_definition
                    found: inner_pair.as_rule(),
                });
            }
        }
    }

    Ok(StatesBlock {
        annotations,
        id: id.ok_or_else(|| ParseError::MissingRule {
            expected: SsotParser::Rule::integer_literal,
        })?,
        states,
    })
}

fn parse_state_definition(pair: Pair<SsotParser::Rule>) -> ParseResult<StateDefinition> {
    if pair.as_rule() != SsotParser::Rule::state_definition {
        return Err(ParseError::UnexpectedRule {
            expected: SsotParser::Rule::state_definition,
            found: pair.as_rule(),
        });
    }

    let mut annotations = Vec::new();
    let mut name = None;
    let mut id = None;
    let mut on_entry_actions = Vec::new();
    let mut on_exit_actions = Vec::new();
    let mut on_transitions = Vec::new();
    let mut after_transitions = Vec::new();
    let mut invokes = Vec::new();
    let mut nested_states = None;
    let mut history = None;
    let mut is_initial = false;
    let mut is_final = false;
    let mut is_parallel = false;

    for inner_pair in pair.into_inner() {
        match inner_pair.as_rule() {
            SsotParser::Rule::annotation => {
                // Check for special state annotations ($initial, $final, $parallel)
                let ann = parse_annotation(inner_pair.clone())?;
                match ann.name.name.as_str() {
                    "initial" => is_initial = true, // Handled via $initial annotation on machine/parent state
                    "final" => is_final = true,
                    "parallel" => is_parallel = true,
                    _ => annotations.push(ann),
                }
            }
            SsotParser::Rule::identifier => name = Some(parse_identifier(inner_pair)?),
            SsotParser::Rule::integer_literal => id = Some(parse_numeric_id(inner_pair)?),
            SsotParser::Rule::state_element => {
                // state_element is silent, look inside
                if let Some(element_pair) = inner_pair.into_inner().next() {
                    match element_pair.as_rule() {
                        SsotParser::Rule::on_entry => {
                            on_entry_actions = parse_on_entry(element_pair)?
                        }
                        SsotParser::Rule::on_exit => on_exit_actions = parse_on_exit(element_pair)?,
                        SsotParser::Rule::on_transition => {
                            on_transitions.push(parse_on_transition(element_pair)?)
                        }
                        SsotParser::Rule::after_transition => {
                            after_transitions.push(parse_after_transition(element_pair)?)
                        }
                        SsotParser::Rule::state_invoke => {
                            invokes.push(parse_state_invoke(element_pair)?)
                        }
                        SsotParser::Rule::states_definition => {
                            nested_states = Some(parse_states_definition(element_pair)?);
                        }
                        SsotParser::Rule::history_definition => {
                            history = Some(parse_history_definition(element_pair)?);
                        }
                        // Handle special annotations that might appear inside the body too
                        SsotParser::Rule::initial_annotation => { /* Handled by machine/parent */ }
                        SsotParser::Rule::final_annotation => is_final = true,
                        SsotParser::Rule::parallel_annotation => is_parallel = true,

                        _ => {
                            return Err(ParseError::UnexpectedRule {
                                expected: SsotParser::Rule::on_entry, // Or others
                                found: element_pair.as_rule(),
                            });
                        }
                    }
                } else {
                    eprintln!("Warning: Empty state_element encountered.");
                }
            }
            _ => {
                return Err(ParseError::UnexpectedRule {
                    expected: SsotParser::Rule::identifier, // Or others
                    found: inner_pair.as_rule(),
                });
            }
        }
    }

    Ok(StateDefinition {
        annotations,
        name: name.ok_or_else(|| ParseError::MissingRule {
            expected: SsotParser::Rule::identifier,
        })?,
        id: id.ok_or_else(|| ParseError::MissingRule {
            expected: SsotParser::Rule::integer_literal,
        })?,
        entry: on_entry_actions,
        exit: on_exit_actions,
        transitions: on_transitions,
        after: after_transitions,
        invokes,
        states: nested_states,
        history,
        is_final,
        is_parallel,
        // is_initial is typically determined by the $initial annotation on the parent machine/state
    })
}

fn parse_on_transition(pair: Pair<SsotParser::Rule>) -> ParseResult<TransitionDefinition> {
    if pair.as_rule() != SsotParser::Rule::on_transition {
        return Err(ParseError::UnexpectedRule {
            expected: SsotParser::Rule::on_transition,
            found: pair.as_rule(),
        });
    }

    let mut event = None;
    let mut id = None;
    let mut target = None;
    let mut actions = Vec::new();
    let mut guard = None;
    let mut annotations = Vec::new(); // For $allowedActors etc.

    // Inner rules: identifier (event), integer_literal (id), transition_details
    for inner_pair in pair.into_inner() {
        match inner_pair.as_rule() {
            SsotParser::Rule::identifier => event = Some(parse_identifier(inner_pair)?),
            SsotParser::Rule::integer_literal => id = Some(parse_numeric_id(inner_pair)?),
            SsotParser::Rule::transition_details => {
                // Look inside transition_details: transition_target, transition_block?
                let mut details_pairs = inner_pair.into_inner();
                target = Some(parse_transition_target(details_pairs.next().unwrap())?);

                if let Some(block_pair) = details_pairs.next() {
                    // transition_block is optional
                    if block_pair.as_rule() == SsotParser::Rule::transition_block {
                        for block_inner in block_pair.into_inner() {
                            match block_inner.as_rule() {
                                SsotParser::Rule::action_ref => {
                                    actions.push(parse_identifier(
                                        block_inner.into_inner().next().unwrap(),
                                    )?);
                                }
                                SsotParser::Rule::guard_ref => {
                                    guard = Some(parse_identifier(
                                        block_inner.into_inner().next().unwrap(),
                                    )?);
                                }
                                SsotParser::Rule::annotation => {
                                    // Handle $allowedActors
                                    annotations.push(parse_annotation(block_inner)?);
                                }
                                _ => { /* Ignore unexpected inside block */ }
                            }
                        }
                    }
                }
            }
            _ => { /* Ignore unexpected */ }
        }
    }

    Ok(TransitionDefinition {
        event: event.ok_or_else(|| ParseError::MissingRule {
            expected: SsotParser::Rule::identifier,
        })?,
        id: id.ok_or_else(|| ParseError::MissingRule {
            expected: SsotParser::Rule::integer_literal,
        })?,
        target,
        actions,
        guard,
        invoke: None, // Invoke is not part of 'on EVENT' syntax directly
        annotations,
    })
}

// --- TODO: Implement missing parsing functions ---
// Placeholder implementations returning dummy data or errors

fn parse_actions_block(_pair: Pair<SsotParser::Rule>) -> ParseResult<ActionsBlock> {
    // TODO: Implement actual parsing logic
    println!("TODO: Implement parse_actions_block");
    Ok(ActionsBlock {
        annotations: vec![],
        id: NumericId { value: 0 },
        actions: vec![],
    })
}

fn parse_guards_block(_pair: Pair<SsotParser::Rule>) -> ParseResult<GuardsBlock> {
    // TODO: Implement actual parsing logic
    println!("TODO: Implement parse_guards_block");
    Ok(GuardsBlock {
        annotations: vec![],
        id: NumericId { value: 0 },
        guards: vec![],
    })
}

fn parse_invokes_block(pair: Pair<SsotParser::Rule>) -> ParseResult<InvokesBlock> {
    if pair.as_rule() != SsotParser::Rule::invokes_definition {
        return Err(ParseError::UnexpectedRule {
            expected: SsotParser::Rule::invokes_definition,
            found: pair.as_rule(),
        });
    }

    let mut annotations = Vec::new();
    let mut id = NumericId { value: 0 };
    let mut invokes = Vec::new();

    for item in pair.into_inner() {
        match item.as_rule() {
            SsotParser::Rule::annotation => {
                annotations.push(parse_annotation(item)?);
            }
            SsotParser::Rule::numeric_id => {
                id = parse_numeric_id(item)?;
            }
            SsotParser::Rule::invoke_definition => {
                invokes.push(parse_invoke_definition(item)?);
            }
            _ => {
                return Err(ParseError::UnexpectedRule {
                    expected: SsotParser::Rule::annotation,
                    found: item.as_rule(),
                });
            }
        }
    }

    Ok(InvokesBlock {
        annotations,
        id,
        invokes,
    })
}

fn parse_invoke_definition(pair: Pair<SsotParser::Rule>) -> ParseResult<InvokeDefinition> {
    if pair.as_rule() != SsotParser::Rule::invoke_definition {
        return Err(ParseError::UnexpectedRule {
            expected: SsotParser::Rule::invoke_definition,
            found: pair.as_rule(),
        });
    }

    let mut annotations = Vec::new();
    let mut name = None;
    let mut id = None;
    let mut src = None;
    let mut input_mapping = None;
    let mut on_done = None;
    let mut on_error = None;

    for item in pair.into_inner() {
        match item.as_rule() {
            SsotParser::Rule::annotation => {
                annotations.push(parse_annotation(item)?);
            }
            SsotParser::Rule::identifier => {
                name = Some(parse_identifier(item)?);
            }
            SsotParser::Rule::numeric_id => {
                id = Some(parse_numeric_id(item)?);
            }
            SsotParser::Rule::invoke_body => {
                let body_result = parse_invoke_body(item)?;
                src = Some(body_result.0);
                input_mapping = body_result.1;
                on_done = body_result.2;
                on_error = body_result.3;
            }
            _ => {
                return Err(ParseError::UnexpectedRule {
                    expected: SsotParser::Rule::annotation,
                    found: item.as_rule(),
                });
            }
        }
    }

    let name = name.ok_or_else(|| ParseError::InvalidInput {
        message: "Missing invoke name".to_string(),
    })?;

    let id = id.ok_or_else(|| ParseError::InvalidInput {
        message: "Missing invoke id".to_string(),
    })?;

    let src = src.ok_or_else(|| ParseError::InvalidInput {
        message: "Missing invoke src".to_string(),
    })?;

    Ok(InvokeDefinition {
        annotations,
        name,
        id,
        src,
        input_mapping,
        on_done,
        on_error,
    })
}

fn parse_invoke_body(
    pair: Pair<SsotParser::Rule>,
) -> ParseResult<(
    InvokeSource,
    Option<ObjectLiteral>,
    Option<TransitionDefinition>,
    Option<TransitionDefinition>,
)> {
    if pair.as_rule() != SsotParser::Rule::invoke_body {
        return Err(ParseError::UnexpectedRule {
            expected: SsotParser::Rule::invoke_body,
            found: pair.as_rule(),
        });
    }

    let mut src = None;
    let mut input_mapping = None;
    let mut on_done = None;
    let mut on_error = None;

    for item in pair.into_inner() {
        match item.as_rule() {
            SsotParser::Rule::invoke_src => {
                src = Some(parse_invoke_src(item)?);
            }
            SsotParser::Rule::input_mapping => {
                let object_literal =
                    item.into_inner()
                        .next()
                        .ok_or_else(|| ParseError::InvalidInput {
                            message: "Empty input mapping".to_string(),
                        })?;
                input_mapping = Some(parse_object_literal(object_literal)?);
            }
            SsotParser::Rule::on_done => {
                let transition_details =
                    item.into_inner()
                        .next()
                        .ok_or_else(|| ParseError::InvalidInput {
                            message: "Empty onDone transition".to_string(),
                        })?;
                on_done = Some(parse_invoke_transition_details(transition_details)?);
            }
            SsotParser::Rule::on_error => {
                let transition_details =
                    item.into_inner()
                        .next()
                        .ok_or_else(|| ParseError::InvalidInput {
                            message: "Empty onError transition".to_string(),
                        })?;
                on_error = Some(parse_invoke_transition_details(transition_details)?);
            }
            _ => {
                return Err(ParseError::UnexpectedRule {
                    expected: SsotParser::Rule::invoke_src,
                    found: item.as_rule(),
                });
            }
        }
    }

    let src = src.ok_or_else(|| ParseError::InvalidInput {
        message: "Missing invoke src".to_string(),
    })?;

    Ok((src, input_mapping, on_done, on_error))
}

fn parse_invoke_src(pair: Pair<SsotParser::Rule>) -> ParseResult<InvokeSource> {
    if pair.as_rule() != SsotParser::Rule::invoke_src {
        return Err(ParseError::UnexpectedRule {
            expected: SsotParser::Rule::invoke_src,
            found: pair.as_rule(),
        });
    }

    let src_pair = pair
        .into_inner()
        .next()
        .ok_or_else(|| ParseError::InvalidInput {
            message: "Empty invoke src".to_string(),
        })?;

    match src_pair.as_rule() {
        SsotParser::Rule::service_method_ref => {
            let mut parts = src_pair.into_inner();
            let service = parse_identifier(parts.next().unwrap())?;
            let method = parse_identifier(parts.next().unwrap())?;
            Ok(InvokeSource::ServiceMethod(service, method))
        }
        SsotParser::Rule::string_literal => {
            Ok(InvokeSource::StringLiteral(parse_string_literal(src_pair)?))
        }
        SsotParser::Rule::identifier => Ok(InvokeSource::MachineName(parse_identifier(src_pair)?)),
        _ => Err(ParseError::UnexpectedRule {
            expected: SsotParser::Rule::service_method_ref,
            found: src_pair.as_rule(),
        }),
    }
}

fn parse_invoke_transition_details(
    pair: Pair<SsotParser::Rule>,
) -> ParseResult<TransitionDefinition> {
    if pair.as_rule() != SsotParser::Rule::invoke_transition_details {
        return Err(ParseError::UnexpectedRule {
            expected: SsotParser::Rule::invoke_transition_details,
            found: pair.as_rule(),
        });
    }

    let mut target = None;
    let mut actions = Vec::new();
    let mut guard = None;

    for item in pair.into_inner() {
        match item.as_rule() {
            SsotParser::Rule::transition_target => {
                target = Some(parse_transition_target(item)?);
            }
            SsotParser::Rule::transition_block => {
                for block_item in item.into_inner() {
                    match block_item.as_rule() {
                        SsotParser::Rule::action_ref => {
                            let action_id = block_item.into_inner().next().unwrap();
                            actions.push(parse_identifier(action_id)?);
                        }
                        SsotParser::Rule::guard_ref => {
                            let guard_id = block_item.into_inner().next().unwrap();
                            guard = Some(parse_identifier(guard_id)?);
                        }
                        _ => {
                            return Err(ParseError::UnexpectedRule {
                                expected: SsotParser::Rule::action_ref,
                                found: block_item.as_rule(),
                            });
                        }
                    }
                }
            }
            _ => {
                return Err(ParseError::UnexpectedRule {
                    expected: SsotParser::Rule::transition_target,
                    found: item.as_rule(),
                });
            }
        }
    }

    let target = target.ok_or_else(|| ParseError::InvalidInput {
        message: "Missing transition target".to_string(),
    })?;

    Ok(TransitionDefinition {
        event: None,                // Not used in invoke transitions
        id: NumericId { value: 0 }, // Not used in invoke transitions
        target: Some(target),
        actions,
        guard,
        annotations: Vec::new(), // Not used in invoke transitions
    })
}

fn parse_object_literal(pair: Pair<SsotParser::Rule>) -> ParseResult<ObjectLiteral> {
    if pair.as_rule() != SsotParser::Rule::object_literal {
        return Err(ParseError::UnexpectedRule {
            expected: SsotParser::Rule::object_literal,
            found: pair.as_rule(),
        });
    }

    let mut fields = Vec::new();

    if let Some(args_pair) = pair.into_inner().next() {
        fields = parse_annotation_args(args_pair.into_inner())?;
    }

    Ok(ObjectLiteral { fields })
}

fn parse_on_entry(_pair: Pair<SsotParser::Rule>) -> ParseResult<Vec<Identifier>> {
    // TODO: Implement actual parsing logic based on grammar `on_entry = { "onEntry" ~ "{" ~ action_ref* ~ "}" ~ ";" }`
    println!("TODO: Implement parse_on_entry");
    Ok(vec![]) // Return empty vec for now
}

fn parse_on_exit(_pair: Pair<SsotParser::Rule>) -> ParseResult<Vec<Identifier>> {
    // TODO: Implement actual parsing logic based on grammar `on_exit = { "onExit" ~ "{" ~ action_ref* ~ "}" ~ ";" }`
    println!("TODO: Implement parse_on_exit");
    Ok(vec![]) // Return empty vec for now
}

fn parse_after_transition(_pair: Pair<SsotParser::Rule>) -> ParseResult<AfterTransitionDefinition> {
    // TODO: Implement actual parsing logic
    println!("TODO: Implement parse_after_transition");
    // Dummy implementation
    Ok(AfterTransitionDefinition {
        delay: Duration {
            value: 1,
            unit: TimeUnit::Seconds,
        },
        id: NumericId { value: 0 },
        target: None,
        actions: vec![],
        guard: None,
        annotations: vec![],
    })
}

fn parse_state_invoke(_pair: Pair<SsotParser::Rule>) -> ParseResult<StateInvokeDefinition> {
    // TODO: Implement actual parsing logic
    println!("TODO: Implement parse_state_invoke");
    // Dummy implementation
    Ok(StateInvokeDefinition {
        name: Identifier {
            name: "dummy_invoke".to_string(),
        },
        id: NumericId { value: 0 },
        src: Identifier {
            name: "dummy_src".to_string(),
        }, // Should parse invoke_ref
        input_mapping: None,
        on_done: None,
        on_error: None,
        annotations: vec![],
    })
}

fn parse_transition_target(pair: Pair<SsotParser::Rule>) -> ParseResult<TransitionTarget> {
    // TODO: Implement actual parsing logic
    println!("TODO: Implement parse_transition_target");
    // Based on grammar: qualified_history_target | current_history_target | state_name_target
    if let Some(inner) = pair.into_inner().next() {
        // transition_target is silent
        match inner.as_rule() {
            SsotParser::Rule::state_name_target => Ok(TransitionTarget::State(parse_identifier(
                inner.into_inner().next().unwrap(),
            )?)),
            SsotParser::Rule::current_history_target => Ok(TransitionTarget::CurrentHistory),
            SsotParser::Rule::qualified_history_target => Ok(TransitionTarget::ParentHistory(
                parse_identifier(inner.into_inner().next().unwrap())?,
            )),
            _ => Err(ParseError::UnexpectedRule {
                expected: SsotParser::Rule::state_name_target,
                found: inner.as_rule(),
            }),
        }
    } else {
        Err(ParseError::MissingRule {
            expected: SsotParser::Rule::transition_target,
        })
    }
}

// --- End of TODO section for missing functions ---

fn parse_identifier_list(pair: Pair<SsotParser::Rule>) -> ParseResult<Vec<Identifier>> {
    if pair.as_rule() != SsotParser::Rule::identifier_list {
        return Err(ParseError::UnexpectedRule {
            expected: SsotParser::Rule::identifier_list,
            found: pair.as_rule(),
        });
    }
    let mut identifiers = Vec::new();
    for id_pair in pair.into_inner() {
        if id_pair.as_rule() == SsotParser::Rule::identifier {
            identifiers.push(parse_identifier(id_pair)?);
        }
    }
    Ok(identifiers)
}

fn parse_annotation(pair: Pair<SsotParser::Rule>) -> ParseResult<Annotation> {
    if pair.as_rule() != SsotParser::Rule::annotation {
        return Err(ParseError::UnexpectedRule {
            expected: SsotParser::Rule::annotation,
            found: pair.as_rule(),
        });
    }

    // Annotation rule is silent, look inside
    let inner_pair = pair
        .into_inner()
        .next()
        .ok_or_else(|| ParseError::InvalidInput {
            message: "Empty annotation rule".to_string(),
        })?;

    match inner_pair.as_rule() {
        SsotParser::Rule::description_annotation
        | SsotParser::Rule::validate_annotation
        | SsotParser::Rule::db_annotation
        | SsotParser::Rule::meta_annotation
        | SsotParser::Rule::protocol_annotation
        | SsotParser::Rule::communicates_with_annotation
        | SsotParser::Rule::publishes_annotation
        | SsotParser::Rule::subscribes_annotation
        | SsotParser::Rule::route_annotation
        | SsotParser::Rule::implements_annotation
        | SsotParser::Rule::channel_annotation
        | SsotParser::Rule::initial_annotation
        | SsotParser::Rule::allowed_actors_annotation => {
            let mut kv_pairs = inner_pair.into_inner();
            let name_ident = kv_pairs.next().unwrap(); // The annotation name like 'description'
            let name = Identifier {
                name: name_ident.as_str().to_string(),
            };

            let args = match kv_pairs.next() {
                // Optional annotation_args or identifier_list or identifier or string_literal
                Some(args_pair) => {
                    match args_pair.as_rule() {
                        SsotParser::Rule::string_literal => {
                            // For description
                            vec![Argument {
                                key: Identifier {
                                    name: "value".to_string(),
                                },
                                value: parse_annotation_value(args_pair)?,
                            }]
                        }
                        SsotParser::Rule::annotation_args => {
                            parse_annotation_args(args_pair.into_inner())?
                        }
                        SsotParser::Rule::identifier_list => {
                            // For allowedActors
                            let ids = parse_identifier_list(args_pair)?;
                            vec![Argument {
                                key: Identifier {
                                    name: "actors".to_string(),
                                },
                                value: AnnotationValue::List(
                                    ids.into_iter()
                                        .map(|id| AnnotationValue::String(id.name))
                                        .collect(),
                                ),
                            }]
                        }
                        SsotParser::Rule::identifier => {
                            // For protocol, implements, channel, initial, publishes, subscribes
                            vec![Argument {
                                key: Identifier {
                                    name: "name".to_string(),
                                },
                                value: AnnotationValue::String(parse_identifier(args_pair)?.name),
                            }]
                        }
                        _ => {
                            return Err(ParseError::UnexpectedRule {
                                expected: SsotParser::Rule::annotation_args,
                                found: args_pair.as_rule(),
                            })
                        }
                    }
                }
                None => Vec::new(), // No arguments for flags like $final
            };
            Ok(Annotation {
                name,
                arguments: args,
            })
        }
        SsotParser::Rule::generic_kv_annotation => {
            let mut kv_pairs = inner_pair.into_inner();
            let name = parse_identifier(kv_pairs.next().unwrap())?;
            let value = parse_annotation_value(kv_pairs.next().unwrap())?;
            Ok(Annotation {
                name,
                arguments: vec![Argument {
                    key: Identifier {
                        name: "value".to_string(),
                    },
                    value,
                }],
            })
        }
        SsotParser::Rule::generic_flag_annotation
        | SsotParser::Rule::final_annotation
        | SsotParser::Rule::parallel_annotation => {
            let name = parse_identifier(inner_pair.into_inner().next().unwrap())?;
            Ok(Annotation {
                name,
                arguments: Vec::new(),
            })
        }
        SsotParser::Rule::output_directive_annotation => {
            let mut kv_pairs = inner_pair.into_inner();
            let name_ident = kv_pairs.next().unwrap(); // e.g., rust_out
            let name = Identifier {
                name: name_ident.as_str().to_string(),
            };
            let value_pair = kv_pairs.next().unwrap();
            let value = parse_string_literal(value_pair)?;
            Ok(Annotation {
                name,
                arguments: vec![Argument {
                    key: Identifier {
                        name: "path".to_string(),
                    },
                    value: AnnotationValue::String(value),
                }],
            })
        }
        rule => Err(ParseError::UnexpectedRule {
            expected: SsotParser::Rule::description_annotation, // Or others
            found: rule,
        }),
    }
}

fn parse_annotation_args(pairs: Pairs<SsotParser::Rule>) -> ParseResult<Vec<Argument>> {
    let mut args = Vec::new();
    for arg_pair in pairs {
        if arg_pair.as_rule() == SsotParser::Rule::annotation_arg {
            args.push(parse_annotation_arg(arg_pair)?);
        }
    }
    Ok(args)
}

fn parse_annotation_arg(pair: Pair<SsotParser::Rule>) -> ParseResult<Argument> {
    if pair.as_rule() != SsotParser::Rule::annotation_arg {
        return Err(ParseError::UnexpectedRule {
            expected: SsotParser::Rule::annotation_arg,
            found: pair.as_rule(),
        });
    }
    let mut inner = pair.into_inner();
    let key = parse_identifier(inner.next().unwrap())?;
    let value = parse_annotation_value(inner.next().unwrap())?;
    Ok(Argument { key, value })
}

fn parse_annotation_value(pair: Pair<SsotParser::Rule>) -> ParseResult<AnnotationValue> {
    // Value rule is silent, look inside
    let inner_pair = pair
        .into_inner()
        .next()
        .ok_or_else(|| ParseError::InvalidInput {
            message: "Empty annotation_value rule".to_string(),
        })?;

    match inner_pair.as_rule() {
        SsotParser::Rule::string_literal => {
            Ok(AnnotationValue::String(parse_string_literal(inner_pair)?))
        }
        SsotParser::Rule::integer_literal => Ok(AnnotationValue::Integer(
            inner_pair.as_str().parse::<i64>()?,
        )),
        SsotParser::Rule::boolean_literal => {
            Ok(AnnotationValue::Boolean(inner_pair.as_str() == "true"))
        }
        SsotParser::Rule::list_literal => {
            let items = inner_pair
                .into_inner()
                .map(|item_pair| parse_annotation_value(item_pair))
                .collect::<ParseResult<Vec<_>>>()?;
            Ok(AnnotationValue::List(items))
        }
        SsotParser::Rule::object_literal => {
            let args = match inner_pair.into_inner().next() {
                // object_literal contains optional annotation_args
                Some(args_pair) if args_pair.as_rule() == SsotParser::Rule::annotation_args => {
                    parse_annotation_args(args_pair.into_inner())?
                }
                _ => vec![], // Empty object or no args
            };
            Ok(AnnotationValue::Object(args))
        }
        rule => Err(ParseError::UnexpectedRule {
            expected: SsotParser::Rule::string_literal, /* or others */
            found: rule,
        }),
    }
}

fn parse_string_literal(pair: Pair<SsotParser::Rule>) -> ParseResult<String> {
    if pair.as_rule() != SsotParser::Rule::string_literal {
        return Err(ParseError::UnexpectedRule {
            expected: SsotParser::Rule::string_literal,
            found: pair.as_rule(),
        });
    }
    let s = pair.as_str();
    Ok(s[1..s.len() - 1].to_string()) // Remove quotes
}

fn parse_type_specifier(pair: Pair<SsotParser::Rule>) -> ParseResult<TypeSpecifier> {
    // type_specifier rule is silent, look inside
    let inner_pair = pair
        .into_inner()
        .next()
        .ok_or_else(|| ParseError::InvalidInput {
            message: "Empty type_specifier rule".to_string(),
        })?;

    match inner_pair.as_rule() {
        SsotParser::Rule::simple_type => {
            let ident_pair =
                inner_pair
                    .into_inner()
                    .next()
                    .ok_or_else(|| ParseError::MissingRule {
                        expected: SsotParser::Rule::identifier,
                    })?;
            Ok(TypeSpecifier::Simple(parse_identifier(ident_pair)?))
        }
        SsotParser::Rule::list_type => {
            let inner_type_pair =
                inner_pair
                    .into_inner()
                    .next()
                    .ok_or_else(|| ParseError::MissingRule {
                        expected: SsotParser::Rule::type_specifier,
                    })?;
            let inner_type = parse_type_specifier(inner_type_pair)?;
            Ok(TypeSpecifier::List(Box::new(inner_type)))
        }
        SsotParser::Rule::optional_type => {
            let inner_type_pair =
                inner_pair
                    .into_inner()
                    .next()
                    .ok_or_else(|| ParseError::MissingRule {
                        expected: SsotParser::Rule::type_specifier,
                    })?;
            let inner_type = parse_type_specifier(inner_type_pair)?;
            Ok(TypeSpecifier::Optional(Box::new(inner_type)))
        }
        SsotParser::Rule::map_type => {
            let mut map_inner = inner_pair.into_inner();
            let key_type_pair = map_inner.next().ok_or_else(|| ParseError::MissingRule {
                expected: SsotParser::Rule::type_specifier,
            })?;
            let value_type_pair = map_inner.next().ok_or_else(|| ParseError::MissingRule {
                expected: SsotParser::Rule::type_specifier,
            })?;
            let key_type = parse_type_specifier(key_type_pair)?;
            let value_type = parse_type_specifier(value_type_pair)?;
            Ok(TypeSpecifier::Map(Box::new(key_type), Box::new(value_type)))
        }
        rule => Err(ParseError::UnexpectedRule {
            expected: SsotParser::Rule::simple_type, // or list/optional/map
            found: rule,
        }),
    }
}

fn parse_history_definition(pair: Pair<SsotParser::Rule>) -> ParseResult<HistoryDefinition> {
    if pair.as_rule() != SsotParser::Rule::history_definition {
        return Err(ParseError::UnexpectedRule {
            expected: SsotParser::Rule::history_definition,
            found: pair.as_rule(),
        });
    }

    let mut history_type = None;
    let mut id = None;
    let mut target = None;

    for inner_pair in pair.into_inner() {
        match inner_pair.as_rule() {
            SsotParser::Rule::history_type => {
                history_type = Some(match inner_pair.as_str() {
                    "shallow" => HistoryType::Shallow,
                    "deep" => HistoryType::Deep,
                    _ => {
                        return Err(ParseError::InvalidInput {
                            message: format!("Invalid history type: {}", inner_pair.as_str()),
                        })
                    }
                });
            }
            SsotParser::Rule::integer_literal => id = Some(parse_numeric_id(inner_pair)?),
            SsotParser::Rule::identifier => target = Some(parse_identifier(inner_pair)?),
            _ => {
                // Handle unexpected inner rules if necessary
            }
        }
    }

    Ok(HistoryDefinition {
        history_type: history_type.ok_or_else(|| ParseError::MissingRule {
            expected: SsotParser::Rule::history_type,
        })?,
        id: id.ok_or_else(|| ParseError::MissingRule {
            expected: SsotParser::Rule::integer_literal,
        })?,
        target: target.ok_or_else(|| ParseError::MissingRule {
            expected: SsotParser::Rule::identifier,
        })?,
    })
}

fn parse_identifier_list_in_parens(pair: Pair<SsotParser::Rule>) -> ParseResult<Vec<Identifier>> {
    // Rule might be silent, e.g., `allowed_actors_annotation = { ... "(" ~ identifier_list ~ ")" ... }`
    // Need to find the actual identifier_list pair within the current pair.
    let id_list_pair = pair
        .into_inner()
        .find(|p| p.as_rule() == SsotParser::Rule::identifier_list)
        .ok_or_else(|| ParseError::MissingRule {
            expected: SsotParser::Rule::identifier_list,
        })?;
    parse_identifier_list(id_list_pair)
}

fn parse_services_block(pair: Pair<SsotParser::Rule>) -> ParseResult<ServicesBlock> {
    if pair.as_rule() != SsotParser::Rule::services_block {
        return Err(ParseError::UnexpectedRule {
            expected: SsotParser::Rule::services_block,
            found: pair.as_rule(),
        });
    }

    let mut annotations = Vec::new();
    let mut definitions = Vec::new();

    for inner_pair in pair.into_inner() {
        match inner_pair.as_rule() {
            SsotParser::Rule::annotation => annotations.push(parse_annotation(inner_pair)?),
            SsotParser::Rule::service_item => {
                // service_item is silent, look inside
                if let Some(item_pair) = inner_pair.into_inner().next() {
                    match item_pair.as_rule() {
                        SsotParser::Rule::interface_definition => definitions.push(
                            ServiceItem::Interface(parse_interface_definition(item_pair)?),
                        ),
                        SsotParser::Rule::service_definition => {
                            definitions
                                .push(ServiceItem::Service(parse_service_definition(item_pair)?));
                        }
                        _ => {
                            return Err(ParseError::UnexpectedRule {
                                expected: SsotParser::Rule::interface_definition, // or service
                                found: item_pair.as_rule(),
                            });
                        }
                    }
                } else {
                    eprintln!("Warning: Empty service_item encountered.");
                }
            }
            _ => {
                return Err(ParseError::UnexpectedRule {
                    expected: SsotParser::Rule::annotation, // Or service_item
                    found: inner_pair.as_rule(),
                });
            }
        }
    }

    Ok(ServicesBlock {
        annotations,
        definitions,
    })
}

fn parse_interface_definition(pair: Pair<SsotParser::Rule>) -> ParseResult<InterfaceDefinition> {
    if pair.as_rule() != SsotParser::Rule::interface_definition {
        return Err(ParseError::UnexpectedRule {
            expected: SsotParser::Rule::interface_definition,
            found: pair.as_rule(),
        });
    }

    let mut annotations = Vec::new();
    let mut name = None;
    let mut id = None;
    let mut methods = Vec::new();

    for inner_pair in pair.into_inner() {
        match inner_pair.as_rule() {
            SsotParser::Rule::annotation => annotations.push(parse_annotation(inner_pair)?),
            SsotParser::Rule::identifier => name = Some(parse_identifier(inner_pair)?),
            SsotParser::Rule::integer_literal => id = Some(parse_numeric_id(inner_pair)?),
            SsotParser::Rule::method_definition => {
                methods.push(parse_method_definition(inner_pair)?)
            }
            _ => {
                return Err(ParseError::UnexpectedRule {
                    expected: SsotParser::Rule::identifier, // Or others
                    found: inner_pair.as_rule(),
                });
            }
        }
    }

    Ok(InterfaceDefinition {
        annotations,
        name: name.ok_or_else(|| ParseError::MissingRule {
            expected: SsotParser::Rule::identifier,
        })?,
        id: id.ok_or_else(|| ParseError::MissingRule {
            expected: SsotParser::Rule::integer_literal,
        })?,
        methods,
    })
}

fn parse_method_definition(pair: Pair<SsotParser::Rule>) -> ParseResult<MethodDefinition> {
    if pair.as_rule() != SsotParser::Rule::method_definition {
        return Err(ParseError::UnexpectedRule {
            expected: SsotParser::Rule::method_definition,
            found: pair.as_rule(),
        });
    }

    let mut annotations = Vec::new();
    let mut name = None;
    let mut id = None;
    let mut parameters = Vec::new();
    let mut return_type = None;
    let mut body_annotations = Vec::new();

    for inner_pair in pair.into_inner() {
        match inner_pair.as_rule() {
            SsotParser::Rule::annotation => annotations.push(parse_annotation(inner_pair)?),
            SsotParser::Rule::identifier => name = Some(parse_identifier(inner_pair)?),
            SsotParser::Rule::integer_literal => id = Some(parse_numeric_id(inner_pair)?),
            SsotParser::Rule::parameter_list => {
                for param_pair in inner_pair.into_inner() {
                    if param_pair.as_rule() == SsotParser::Rule::parameter {
                        parameters.push(parse_parameter(param_pair)?);
                    }
                }
            }
            SsotParser::Rule::method_return => {
                // method_return is silent, look inside for type_specifier
                if let Some(ts_pair) = inner_pair.into_inner().next() {
                    return_type = Some(parse_type_specifier(ts_pair)?);
                }
            }
            SsotParser::Rule::method_body => {
                for body_item in inner_pair.into_inner() {
                    if body_item.as_rule() == SsotParser::Rule::annotation {
                        body_annotations.push(parse_annotation(body_item)?);
                    }
                }
            }
            _ => {
                // Ignore unexpected inner rules like ',' in parameter_list
            }
        }
    }

    Ok(MethodDefinition {
        annotations,
        name: name.ok_or_else(|| ParseError::MissingRule {
            expected: SsotParser::Rule::identifier,
        })?,
        id: id.ok_or_else(|| ParseError::MissingRule {
            expected: SsotParser::Rule::integer_literal,
        })?,
        parameters,
        return_type,
        body_annotations,
    })
}

fn parse_parameter(pair: Pair<SsotParser::Rule>) -> ParseResult<ParameterDefinition> {
    if pair.as_rule() != SsotParser::Rule::parameter {
        return Err(ParseError::UnexpectedRule {
            expected: SsotParser::Rule::parameter,
            found: pair.as_rule(),
        });
    }

    let mut name = None;
    let mut type_spec = None;
    let mut id = None;

    for inner_pair in pair.into_inner() {
        match inner_pair.as_rule() {
            SsotParser::Rule::identifier => name = Some(parse_identifier(inner_pair)?),
            SsotParser::Rule::type_specifier => type_spec = Some(parse_type_specifier(inner_pair)?),
            SsotParser::Rule::integer_literal => id = Some(parse_numeric_id(inner_pair)?),
            _ => {
                // Ignore unexpected rules (like @id parts if handled differently)
            }
        }
    }

    Ok(ParameterDefinition {
        name: name.ok_or_else(|| ParseError::MissingRule {
            expected: SsotParser::Rule::identifier,
        })?,
        type_specifier: type_spec.ok_or_else(|| ParseError::MissingRule {
            expected: SsotParser::Rule::type_specifier,
        })?,
        id,
    })
}

fn parse_service_definition(pair: Pair<SsotParser::Rule>) -> ParseResult<ServiceDefinition> {
    if pair.as_rule() != SsotParser::Rule::service_definition {
        return Err(ParseError::UnexpectedRule {
            expected: SsotParser::Rule::service_definition,
            found: pair.as_rule(),
        });
    }

    let mut annotations = Vec::new();
    let mut name = None;
    let mut id = None;
    let mut extends = None;
    let mut body_annotations = Vec::new(); // Annotations within the service body {}

    for inner_pair in pair.into_inner() {
        match inner_pair.as_rule() {
            SsotParser::Rule::annotation => annotations.push(parse_annotation(inner_pair)?),
            SsotParser::Rule::identifier => name = Some(parse_identifier(inner_pair)?),
            SsotParser::Rule::integer_literal => id = Some(parse_numeric_id(inner_pair)?),
            SsotParser::Rule::service_extends => {
                // service_extends is silent, look inside for identifier
                if let Some(id_pair) = inner_pair.into_inner().next() {
                    extends = Some(parse_identifier(id_pair)?);
                }
            }
            SsotParser::Rule::service_body => {
                for body_item in inner_pair.into_inner() {
                    // service_element is silent, check its inner rule
                    if let Some(element_pair) = body_item.into_inner().next() {
                        if element_pair.as_rule() == SsotParser::Rule::annotation {
                            body_annotations.push(parse_annotation(element_pair)?);
                        }
                    } else if body_item.as_rule() != SsotParser::Rule::service_element {
                        // Check top level pair if not silent
                        // If service_element itself could be non-silent annotation
                        if body_item.as_rule() == SsotParser::Rule::annotation {
                            body_annotations.push(parse_annotation(body_item)?);
                        }
                    }
                }
            }
            _ => { /* Ignore unexpected */ }
        }
    }

    // Combine top-level and body annotations for the final struct
    annotations.extend(body_annotations);

    Ok(ServiceDefinition {
        annotations,
        name: name.ok_or_else(|| ParseError::MissingRule {
            expected: SsotParser::Rule::identifier,
        })?,
        id: id.ok_or_else(|| ParseError::MissingRule {
            expected: SsotParser::Rule::integer_literal,
        })?,
        extends,
        // elements: body_annotations, // Store body annotations here or merge
    })
}

fn parse_communication_block(pair: Pair<SsotParser::Rule>) -> ParseResult<CommunicationBlock> {
    if pair.as_rule() != SsotParser::Rule::communication_block {
        return Err(ParseError::UnexpectedRule {
            expected: SsotParser::Rule::communication_block,
            found: pair.as_rule(),
        });
    }

    let mut annotations = Vec::new();
    let mut definitions = Vec::new();

    for inner_pair in pair.into_inner() {
        match inner_pair.as_rule() {
            SsotParser::Rule::annotation => annotations.push(parse_annotation(inner_pair)?),
            SsotParser::Rule::communication_item => {
                // communication_item is silent, look inside
                if let Some(item_pair) = inner_pair.into_inner().next() {
                    match item_pair.as_rule() {
                        SsotParser::Rule::protocol_definition => definitions.push(
                            CommunicationItem::Protocol(parse_protocol_definition(item_pair)?),
                        ),
                        SsotParser::Rule::channel_definition => definitions.push(
                            CommunicationItem::Channel(parse_channel_definition(item_pair)?),
                        ),
                        SsotParser::Rule::event_definition => definitions
                            .push(CommunicationItem::Event(parse_event_definition(item_pair)?)),
                        _ => {
                            return Err(ParseError::UnexpectedRule {
                                expected: SsotParser::Rule::protocol_definition, // or others
                                found: item_pair.as_rule(),
                            });
                        }
                    }
                } else {
                    eprintln!("Warning: Empty communication_item encountered.");
                }
            }
            _ => {
                return Err(ParseError::UnexpectedRule {
                    expected: SsotParser::Rule::annotation, // Or communication_item
                    found: inner_pair.as_rule(),
                });
            }
        }
    }

    Ok(CommunicationBlock {
        annotations,
        definitions,
    })
}

fn parse_protocol_definition(pair: Pair<SsotParser::Rule>) -> ParseResult<ProtocolDefinition> {
    if pair.as_rule() != SsotParser::Rule::protocol_definition {
        return Err(ParseError::UnexpectedRule {
            expected: SsotParser::Rule::protocol_definition,
            found: pair.as_rule(),
        });
    }

    let mut annotations = Vec::new();
    let mut name = None;
    let mut id = None;
    let mut body_annotations = Vec::new();

    for inner_pair in pair.into_inner() {
        match inner_pair.as_rule() {
            SsotParser::Rule::annotation => annotations.push(parse_annotation(inner_pair)?),
            SsotParser::Rule::identifier => name = Some(parse_identifier(inner_pair)?),
            SsotParser::Rule::integer_literal => id = Some(parse_numeric_id(inner_pair)?),
            SsotParser::Rule::protocol_body => {
                for body_item in inner_pair.into_inner() {
                    if body_item.as_rule() == SsotParser::Rule::annotation {
                        body_annotations.push(parse_annotation(body_item)?);
                    }
                }
            }
            _ => { /* Ignore unexpected */ }
        }
    }

    annotations.extend(body_annotations);

    Ok(ProtocolDefinition {
        annotations,
        name: name.ok_or_else(|| ParseError::MissingRule {
            expected: SsotParser::Rule::identifier,
        })?,
        id: id.ok_or_else(|| ParseError::MissingRule {
            expected: SsotParser::Rule::integer_literal,
        })?,
        // Body annotations are merged into main annotations
    })
}

fn parse_channel_definition(pair: Pair<SsotParser::Rule>) -> ParseResult<ChannelDefinition> {
    if pair.as_rule() != SsotParser::Rule::channel_definition {
        return Err(ParseError::UnexpectedRule {
            expected: SsotParser::Rule::channel_definition,
            found: pair.as_rule(),
        });
    }

    let mut annotations = Vec::new();
    let mut name = None;
    let mut id = None;
    let mut description = None;
    let mut parameters = None;

    for inner_pair in pair.into_inner() {
        match inner_pair.as_rule() {
            SsotParser::Rule::annotation => annotations.push(parse_annotation(inner_pair)?),
            SsotParser::Rule::identifier => name = Some(parse_identifier(inner_pair)?),
            SsotParser::Rule::integer_literal => id = Some(parse_numeric_id(inner_pair)?),
            SsotParser::Rule::channel_body => {
                for element in inner_pair.into_inner() {
                    // channel_element is silent, look inside
                    if let Some(element_pair) = element.into_inner().next() {
                        match element_pair.as_rule() {
                            SsotParser::Rule::description_element => {
                                let desc_val = element_pair
                                    .into_inner()
                                    .find(|p| p.as_rule() == SsotParser::Rule::string_literal)
                                    .ok_or(ParseError::MissingRule {
                                        expected: SsotParser::Rule::string_literal,
                                    })?;
                                description = Some(parse_string_literal(desc_val)?);
                            }
                            SsotParser::Rule::parameters_element => {
                                let params_val = element_pair
                                    .into_inner()
                                    .find(|p| p.as_rule() == SsotParser::Rule::object_literal)
                                    .ok_or(ParseError::MissingRule {
                                        expected: SsotParser::Rule::object_literal,
                                    })?;
                                if let AnnotationValue::Object(params_obj) =
                                    parse_annotation_value(params_val)?
                                {
                                    parameters = Some(params_obj);
                                }
                            }
                            SsotParser::Rule::annotation => {
                                annotations.push(parse_annotation(element_pair)?)
                            }
                            _ => { /* Unexpected inside channel_element */ }
                        }
                    } else if element.as_rule() == SsotParser::Rule::annotation {
                        // Handle annotation directly under channel_body
                        annotations.push(parse_annotation(element)?);
                    }
                }
            }
            _ => { /* Ignore unexpected */ }
        }
    }

    Ok(ChannelDefinition {
        annotations,
        name: name.ok_or_else(|| ParseError::MissingRule {
            expected: SsotParser::Rule::identifier,
        })?,
        id: id.ok_or_else(|| ParseError::MissingRule {
            expected: SsotParser::Rule::integer_literal,
        })?,
        description,
        parameters,
    })
}

fn parse_event_definition(pair: Pair<SsotParser::Rule>) -> ParseResult<EventDefinition> {
    if pair.as_rule() != SsotParser::Rule::event_definition {
        return Err(ParseError::UnexpectedRule {
            expected: SsotParser::Rule::event_definition,
            found: pair.as_rule(),
        });
    }

    let mut annotations = Vec::new();
    let mut name = None;
    let mut id = None;
    let mut fields = Vec::new();

    for inner_pair in pair.into_inner() {
        match inner_pair.as_rule() {
            SsotParser::Rule::annotation => annotations.push(parse_annotation(inner_pair)?),
            SsotParser::Rule::identifier => name = Some(parse_identifier(inner_pair)?),
            SsotParser::Rule::integer_literal => id = Some(parse_numeric_id(inner_pair)?),
            SsotParser::Rule::event_field_definition => {
                // event_field_definition is silent, contains field_definition
                if let Some(field_pair) = inner_pair.into_inner().next() {
                    if field_pair.as_rule() == SsotParser::Rule::field_definition {
                        fields.push(parse_field_definition(field_pair)?);
                    }
                }
            }
            _ => { /* Ignore unexpected */ }
        }
    }

    Ok(EventDefinition {
        annotations,
        name: name.ok_or_else(|| ParseError::MissingRule {
            expected: SsotParser::Rule::identifier,
        })?,
        id: id.ok_or_else(|| ParseError::MissingRule {
            expected: SsotParser::Rule::integer_literal,
        })?,
        fields,
    })
}

fn parse_actors_block(pair: Pair<SsotParser::Rule>) -> ParseResult<ActorsBlock> {
    if pair.as_rule() != SsotParser::Rule::actors_block {
        return Err(ParseError::UnexpectedRule {
            expected: SsotParser::Rule::actors_block,
            found: pair.as_rule(),
        });
    }

    let mut annotations = Vec::new();
    let mut definitions = Vec::new();

    for inner_pair in pair.into_inner() {
        match inner_pair.as_rule() {
            SsotParser::Rule::annotation => annotations.push(parse_annotation(inner_pair)?),
            SsotParser::Rule::actor_definition => {
                definitions.push(parse_actor_definition(inner_pair)?)
            }
            _ => {
                return Err(ParseError::UnexpectedRule {
                    expected: SsotParser::Rule::annotation, // Or actor_definition
                    found: inner_pair.as_rule(),
                });
            }
        }
    }

    Ok(ActorsBlock {
        annotations,
        definitions,
    })
}

fn parse_actor_definition(pair: Pair<SsotParser::Rule>) -> ParseResult<ActorDefinition> {
    if pair.as_rule() != SsotParser::Rule::actor_definition {
        return Err(ParseError::UnexpectedRule {
            expected: SsotParser::Rule::actor_definition,
            found: pair.as_rule(),
        });
    }

    let mut annotations = Vec::new();
    let mut name = None;
    let mut id = None;
    let mut body_annotations = Vec::new();

    for inner_pair in pair.into_inner() {
        match inner_pair.as_rule() {
            SsotParser::Rule::annotation => annotations.push(parse_annotation(inner_pair)?),
            SsotParser::Rule::identifier => name = Some(parse_identifier(inner_pair)?),
            SsotParser::Rule::integer_literal => id = Some(parse_numeric_id(inner_pair)?),
            SsotParser::Rule::actor_body => {
                for body_item in inner_pair.into_inner() {
                    if body_item.as_rule() == SsotParser::Rule::annotation {
                        body_annotations.push(parse_annotation(body_item)?);
                    }
                }
            }
            _ => { /* Ignore unexpected */ }
        }
    }

    annotations.extend(body_annotations);

    Ok(ActorDefinition {
        annotations,
        name: name.ok_or_else(|| ParseError::MissingRule {
            expected: SsotParser::Rule::identifier,
        })?,
        id: id.ok_or_else(|| ParseError::MissingRule {
            expected: SsotParser::Rule::integer_literal,
        })?,
    })
}

fn parse_deployment_config_block(
    pair: Pair<SsotParser::Rule>,
) -> ParseResult<DeploymentConfigBlock> {
    if pair.as_rule() != SsotParser::Rule::deployment_config_block {
        return Err(ParseError::UnexpectedRule {
            expected: SsotParser::Rule::deployment_config_block,
            found: pair.as_rule(),
        });
    }

    let mut annotations = Vec::new();
    let mut definitions = Vec::new();

    for inner_pair in pair.into_inner() {
        match inner_pair.as_rule() {
            SsotParser::Rule::annotation => annotations.push(parse_annotation(inner_pair)?),
            SsotParser::Rule::deployment_item => {
                // deployment_item is silent, look inside
                if let Some(item_pair) = inner_pair.into_inner().next() {
                    match item_pair.as_rule() {
                        SsotParser::Rule::environment_definition => definitions.push(
                            DeploymentItem::Environment(parse_environment_definition(item_pair)?),
                        ),
                        SsotParser::Rule::infrastructure_definition => {
                            definitions.push(DeploymentItem::Infrastructure(
                                parse_infrastructure_definition(item_pair)?,
                            ))
                        }
                        SsotParser::Rule::deployment_definition => definitions.push(
                            DeploymentItem::Deployment(parse_deployment_definition(item_pair)?),
                        ),
                        _ => {
                            return Err(ParseError::UnexpectedRule {
                                expected: SsotParser::Rule::environment_definition, // or others
                                found: item_pair.as_rule(),
                            });
                        }
                    }
                } else {
                    eprintln!("Warning: Empty deployment_item encountered.");
                }
            }
            _ => {
                return Err(ParseError::UnexpectedRule {
                    expected: SsotParser::Rule::annotation, // Or deployment_item
                    found: inner_pair.as_rule(),
                });
            }
        }
    }

    Ok(DeploymentConfigBlock {
        annotations,
        definitions,
    })
}

fn parse_environment_definition(
    pair: Pair<SsotParser::Rule>,
) -> ParseResult<EnvironmentDefinition> {
    if pair.as_rule() != SsotParser::Rule::environment_definition {
        return Err(ParseError::UnexpectedRule {
            expected: SsotParser::Rule::environment_definition,
            found: pair.as_rule(),
        });
    }

    let mut annotations = Vec::new();
    let mut name = None;
    let mut id = None;
    let mut extends = None;
    let mut variables = None;

    for inner_pair in pair.into_inner() {
        match inner_pair.as_rule() {
            SsotParser::Rule::annotation => annotations.push(parse_annotation(inner_pair)?),
            SsotParser::Rule::identifier => name = Some(parse_identifier(inner_pair)?),
            SsotParser::Rule::integer_literal => id = Some(parse_numeric_id(inner_pair)?),
            SsotParser::Rule::environment_extends => {
                if let Some(id_pair) = inner_pair.into_inner().next() {
                    extends = Some(parse_identifier(id_pair)?);
                }
            }
            SsotParser::Rule::environment_body => {
                for element in inner_pair.into_inner() {
                    // environment_element is silent
                    if let Some(element_pair) = element.into_inner().next() {
                        match element_pair.as_rule() {
                            SsotParser::Rule::variables_element => {
                                let vars_val = element_pair
                                    .into_inner()
                                    .find(|p| p.as_rule() == SsotParser::Rule::object_literal)
                                    .ok_or(ParseError::MissingRule {
                                        expected: SsotParser::Rule::object_literal,
                                    })?;
                                if let AnnotationValue::Object(vars_obj) =
                                    parse_annotation_value(vars_val)?
                                {
                                    variables = Some(vars_obj);
                                }
                            }
                            SsotParser::Rule::annotation => {
                                annotations.push(parse_annotation(element_pair)?)
                            }
                            _ => { /* Unexpected inside element */ }
                        }
                    } else if element.as_rule() == SsotParser::Rule::annotation {
                        // Annotation directly under body
                        annotations.push(parse_annotation(element)?);
                    }
                }
            }
            _ => { /* Ignore unexpected */ }
        }
    }

    Ok(EnvironmentDefinition {
        annotations,
        name: name.ok_or_else(|| ParseError::MissingRule {
            expected: SsotParser::Rule::identifier,
        })?,
        id: id.ok_or_else(|| ParseError::MissingRule {
            expected: SsotParser::Rule::integer_literal,
        })?,
        extends,
        variables,
    })
}

fn parse_infrastructure_definition(
    pair: Pair<SsotParser::Rule>,
) -> ParseResult<InfrastructureDefinition> {
    if pair.as_rule() != SsotParser::Rule::infrastructure_definition {
        return Err(ParseError::UnexpectedRule {
            expected: SsotParser::Rule::infrastructure_definition,
            found: pair.as_rule(),
        });
    }

    let mut annotations = Vec::new();
    let mut name = None;
    let mut id = None;
    let mut extends = None;
    let mut attributes = Vec::new();

    for inner_pair in pair.into_inner() {
        match inner_pair.as_rule() {
            SsotParser::Rule::annotation => annotations.push(parse_annotation(inner_pair)?),
            SsotParser::Rule::identifier => name = Some(parse_identifier(inner_pair)?),
            SsotParser::Rule::integer_literal => id = Some(parse_numeric_id(inner_pair)?),
            SsotParser::Rule::infrastructure_extends => {
                if let Some(id_pair) = inner_pair.into_inner().next() {
                    extends = Some(parse_identifier(id_pair)?);
                }
            }
            SsotParser::Rule::infrastructure_body => {
                for element in inner_pair.into_inner() {
                    // infrastructure_element is silent
                    if let Some(element_pair) = element.into_inner().next() {
                        match element_pair.as_rule() {
                            SsotParser::Rule::attribute_kv_pair => {
                                attributes.push(parse_attribute_kv_pair(element_pair)?);
                            }
                            SsotParser::Rule::annotation => {
                                annotations.push(parse_annotation(element_pair)?)
                            }
                            _ => { /* Unexpected inside element */ }
                        }
                    } else if element.as_rule() == SsotParser::Rule::annotation {
                        // Annotation directly under body
                        annotations.push(parse_annotation(element)?);
                    }
                }
            }
            _ => { /* Ignore unexpected */ }
        }
    }

    Ok(InfrastructureDefinition {
        annotations,
        name: name.ok_or_else(|| ParseError::MissingRule {
            expected: SsotParser::Rule::identifier,
        })?,
        id: id.ok_or_else(|| ParseError::MissingRule {
            expected: SsotParser::Rule::integer_literal,
        })?,
        extends,
        attributes,
    })
}

fn parse_attribute_kv_pair(pair: Pair<SsotParser::Rule>) -> ParseResult<AttributeDefinition> {
    if pair.as_rule() != SsotParser::Rule::attribute_kv_pair {
        return Err(ParseError::UnexpectedRule {
            expected: SsotParser::Rule::attribute_kv_pair,
            found: pair.as_rule(),
        });
    }
    let mut inner = pair.into_inner();
    let key = parse_identifier(inner.next().unwrap())?;
    let value = parse_annotation_value(inner.next().unwrap())?; // Reuse annotation value parsing
    Ok(AttributeDefinition { key, value })
}

fn parse_deployment_definition(pair: Pair<SsotParser::Rule>) -> ParseResult<DeploymentDefinition> {
    if pair.as_rule() != SsotParser::Rule::deployment_definition {
        return Err(ParseError::UnexpectedRule {
            expected: SsotParser::Rule::deployment_definition,
            found: pair.as_rule(),
        });
    }

    let mut annotations = Vec::new();
    let mut name = None;
    let mut id = None;
    let mut target_environment = None;
    let mut target_infrastructure = None;
    let mut deployable = None;
    let mut config = None;
    let mut other_attributes = Vec::new(); // For direct attributes like replicas, strategy

    for inner_pair in pair.into_inner() {
        match inner_pair.as_rule() {
            SsotParser::Rule::annotation => annotations.push(parse_annotation(inner_pair)?),
            SsotParser::Rule::identifier => name = Some(parse_identifier(inner_pair)?),
            SsotParser::Rule::integer_literal => id = Some(parse_numeric_id(inner_pair)?),
            SsotParser::Rule::deployment_body => {
                for element in inner_pair.into_inner() {
                    // deployment_element is silent
                    if let Some(element_pair) = element.into_inner().next() {
                        match element_pair.as_rule() {
                            SsotParser::Rule::target_env_element => {
                                let env_id = element_pair
                                    .into_inner()
                                    .find(|p| p.as_rule() == SsotParser::Rule::identifier)
                                    .ok_or(ParseError::MissingRule {
                                        expected: SsotParser::Rule::identifier,
                                    })?;
                                target_environment = Some(parse_identifier(env_id)?);
                            }
                            SsotParser::Rule::target_infra_element => {
                                let infra_obj = element_pair
                                    .into_inner()
                                    .find(|p| p.as_rule() == SsotParser::Rule::object_literal)
                                    .ok_or(ParseError::MissingRule {
                                        expected: SsotParser::Rule::object_literal,
                                    })?;
                                if let AnnotationValue::Object(infra_map) =
                                    parse_annotation_value(infra_obj)?
                                {
                                    target_infrastructure = Some(infra_map);
                                }
                            }
                            SsotParser::Rule::deployable_element => {
                                let dep_id = element_pair
                                    .into_inner()
                                    .find(|p| p.as_rule() == SsotParser::Rule::identifier)
                                    .ok_or(ParseError::MissingRule {
                                        expected: SsotParser::Rule::identifier,
                                    })?;
                                deployable = Some(parse_identifier(dep_id)?);
                            }
                            SsotParser::Rule::config_element => {
                                let conf_obj = element_pair
                                    .into_inner()
                                    .find(|p| p.as_rule() == SsotParser::Rule::object_literal)
                                    .ok_or(ParseError::MissingRule {
                                        expected: SsotParser::Rule::object_literal,
                                    })?;
                                if let AnnotationValue::Object(conf_map) =
                                    parse_annotation_value(conf_obj)?
                                {
                                    config = Some(conf_map);
                                }
                            }
                            SsotParser::Rule::attribute_kv_pair => {
                                other_attributes.push(parse_attribute_kv_pair(element_pair)?);
                            }
                            SsotParser::Rule::annotation => {
                                annotations.push(parse_annotation(element_pair)?)
                            }
                            _ => { /* Unexpected inside element */ }
                        }
                    } else if element.as_rule() == SsotParser::Rule::annotation {
                        // Annotation directly under body
                        annotations.push(parse_annotation(element)?);
                    }
                }
            }
            _ => { /* Ignore unexpected */ }
        }
    }

    Ok(DeploymentDefinition {
        annotations,
        name: name.ok_or_else(|| ParseError::MissingRule {
            expected: SsotParser::Rule::identifier,
        })?,
        id: id.ok_or_else(|| ParseError::MissingRule {
            expected: SsotParser::Rule::integer_literal,
        })?,
        target_environment: target_environment.ok_or_else(|| ParseError::MissingRule {
            expected: SsotParser::Rule::target_env_element,
        })?,
        target_infrastructure: target_infrastructure.ok_or_else(|| ParseError::MissingRule {
            expected: SsotParser::Rule::target_infra_element,
        })?,
        deployable: deployable.ok_or_else(|| ParseError::MissingRule {
            expected: SsotParser::Rule::deployable_element,
        })?,
        config,
        other_attributes,
    })
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
        ActorDefinition,
        ActorsBlock,
        ActorsBlock,
        Annotation,
        Annotation,
        AnnotationValue,
        Argument,
        AttributeDefinition,
        ChannelDefinition,
        CommunicationBlock,
        CommunicationItem,
        CommunicationItem,
        ContextDefinition,
        ContextFieldDefinition,
        DeploymentConfigBlock,
        DeploymentConfigBlock,
        DeploymentDefinition,
        DeploymentItem,
        DeploymentItem,
        Duration,
        EnumDefinition,
        EnumVariant,
        EnvironmentDefinition,
        EventDefinition,
        FieldDefinition,
        FileId,
        GuardDefinition,
        GuardsBlock,
        HistoryDefinition,
        HistoryType,
        HistoryType,
        Identifier,
        ImportStatement,
        InfrastructureDefinition,
        InterfaceDefinition,
        InvokeDefinition,
        InvokeSource,
        InvokeSource,
        InvokesBlock,
        MachineDefinition,
        MachinesBlock,
        MethodDefinition,
        NumericId,
        ParameterDefinition,
        ProtocolDefinition,
        ServiceDefinition,
        ServiceItem,
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
        TransitionTarget,
        TypeDefinition,
        TypeDefinition,
        TypeSpecifier,
        TypeSpecifier,
        TypesBlock,
    };
    use pretty_assertions::assert_eq;
    use std::io::Write;
    use std::path::PathBuf;
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
            import \"/path/to/another.ssot\";
            # This is a comment
        "#;
        let ast = parse_ssot_content(content, None).unwrap();

        assert_eq!(ast.file_id, Some(FileId { value: 0xabcdef1234567890 }));
        assert_eq!(ast.imports.len(), 1);
        assert_eq!(ast.imports[0], ImportStatement{ path: "/path/to/another.ssot".to_string() });
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
                assert!(matches!(types_block.annotations[0], Annotation::Description(s) if s == "Some types"));
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
                        assert!(matches!(&s.fields[1].annotations[0], Annotation::Meta(args) if \
                            args.len() == 1 && \
                            args[0].key.name == "unit" && \
                            matches!(args[0].value, AnnotationValue::String(ref s) if s == "pixels") // Corrected: Compare with AnnotationValue::String
                        )); // Added borrow
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
                        assert!(matches!(&e.variants[2].annotations[0], Annotation::Description(s) if s == "Primary blue"));
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
                $description("State machines");

                machine SimpleMachine @id(0) {
                    $initial(Idle);

                    context @id(0) {
                        counter: u32 @id(0) { $default(0); };
                    }

                    states @id(1) {
                        Idle @id(0) {
                            on START @id(0) transition Running;
                        }
                        Running @id(1) {
                             $parallel;
                             on STOP @id(0) transition Idle;
                             after 5s @id(1) transition Idle { action StopAction; };
                             invoke TimerInvoke @id(0) { src: invokes.Timer; };
                             history deep @id(1) target SubIdle;
                             states @id(0) { // Nested states for parallel
                                SubIdle @id(0) { $final; }
                             }
                        }
                    }
                    // TODO: Add actions, guards, invokes blocks once parsing is implemented
                    actions @id(2) { action StopAction @id(0); }
                    guards @id(3) { guard IsRunning @id(0); }
                    invokes @id(4) { invoke Timer @id(0) { src: "timerService.start"; }; }
                }
            }
        "#;
        let ast = parse_ssot_content(content, None).unwrap();
        assert_eq!(ast.definitions.len(), 1);

        match &ast.definitions[0] {
            TopLevelDefinition::Machines(machines_block) => {
                assert_eq!(machines_block.annotations.len(), 1);
                assert!(matches!(machines_block.annotations[0], Annotation::Description(s) if s == "State machines"));
                assert_eq!(machines_block.definitions.len(), 1);
                let machine = &machines_block.definitions[0];
                assert_eq!(machine.name.name, "SimpleMachine");
                assert_eq!(machine.id.value, 0);
                assert!(machine.annotations.iter().any(|a| matches!(a, Annotation::Initial(id) if id.name == "Idle")));

                // Check context
                assert!(machine.context.is_some());
                let context = machine.context.as_ref().unwrap();
                assert_eq!(context.id.value, 0);
                assert_eq!(context.fields.len(), 1);
                assert_eq!(context.fields[0].name.name, "counter");
                assert_eq!(context.fields[0].field_annotations.len(), 1); // $default
                assert!(matches!(context.fields[0].field_annotations[0], Annotation::Default(val) if val == "0"));

                // Check states block
                assert_eq!(machine.states.id.value, 1);
                assert_eq!(machine.states.states.len(), 2);

                // Check Idle state
                let idle_state = &machine.states.states[0];
                assert_eq!(idle_state.name.name, "Idle");
                assert_eq!(idle_state.id.value, 0);
                assert_eq!(idle_state.transitions.len(), 1);
                assert_eq!(idle_state.transitions[0].event.name, "START");
                assert!(
                    matches!(idle_state.transitions[0].target, Some(TransitionTarget::State(ref id)) if id.name == "Running")
                );

                // Check Running state
                let running_state = &machine.states.states[1];
                assert_eq!(running_state.name.name, "Running");
                assert_eq!(running_state.id.value, 1);
                assert!(running_state.is_parallel);
                assert_eq!(running_state.transitions.len(), 1);
                assert_eq!(running_state.transitions[0].event.name, "STOP");
                assert!(
                    matches!(running_state.transitions[0].target, Some(TransitionTarget::State(ref id)) if id.name == "Idle")
                );
                assert_eq!(running_state.after.len(), 1);
                assert_eq!(running_state.after[0].delay.value, 5);
                assert_eq!(running_state.after[0].actions.len(), 1);
                assert_eq!(running_state.after[0].actions[0].name, "StopAction");
                assert_eq!(running_state.invokes.len(), 1);
                // assert_eq!(running_state.invokes[0].name.name, "TimerInvoke"); // Need to implement invoke parsing
                assert!(running_state.history.is_some());
                assert_eq!(
                    running_state.history.as_ref().unwrap().history_type,
                    HistoryType::Deep
                );
                assert!(running_state.states.is_some());
                assert_eq!(running_state.states.as_ref().unwrap().states.len(), 1);
                assert_eq!(
                    running_state.states.as_ref().unwrap().states[0].name.name,
                    "SubIdle"
                );
                assert!(running_state.states.as_ref().unwrap().states[0].is_final);

                // Check Actions, Guards, Invokes (basic check based on TODO placeholders)
                assert!(machine.actions.is_some());
                // assert_eq!(machine.actions.as_ref().unwrap().actions.len(), 1); // Need impl
                assert!(machine.guards.is_some());
                // assert_eq!(machine.guards.as_ref().unwrap().guards.len(), 1); // Need impl
                assert!(machine.invokes.is_some());
                // assert_eq!(machine.invokes.as_ref().unwrap().invokes.len(), 1); // Need impl
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
            file_id: 0x4;
            services {
                $description("API Services");

                interface Greeter @id(0) {
                    sayHello @id(0) (name: string @id(0)) -> string { $route(method: "GET", path: "/hello/{name}"); };
                }

                service MyGreeter @id(1) extends BaseService {
                    $implements(Greeter);
                    $route(basePath: "/api");
                }
            }
        "#;
        let ast = parse_ssot_content(content, None).unwrap();
        assert_eq!(ast.definitions.len(), 1);
        match &ast.definitions[0] {
            TopLevelDefinition::Services(services_block) => {
                assert_eq!(services_block.annotations.len(), 1);
                assert!(matches!(services_block.annotations[0], Annotation::Description(s) if s == "API Services"));
                assert_eq!(services_block.definitions.len(), 2);

                // Check Interface
                match &services_block.definitions[0] {
                    ServiceItem::Interface(i) => {
                        assert_eq!(i.name.name, "Greeter");
                        assert_eq!(i.id.value, 0);
                        assert_eq!(i.methods.len(), 1);
                        let method = &i.methods[0];
                        assert_eq!(method.name.name, "sayHello");
                        assert_eq!(method.parameters.len(), 1);
                        assert_eq!(method.parameters[0].name.name, "name");
                        assert!(
                            matches!(method.parameters[0].type_spec, TypeSpecifier::Simple(ref id) if id.name == "string")
                        );
                        assert_eq!(method.parameters[0].id.unwrap().value, 0);
                        assert!(
                            matches!(method.return_type, Some(TypeSpecifier::Simple(ref id)) if id.name == "string")
                        );
                        assert_eq!(method.body_annotations.len(), 1);
                        assert!(matches!(&method.body_annotations[0], Annotation::Route(args) if args.len() == 2));
                    }
                    _ => panic!("Expected Interface"),
                }

                // Check Service
                match &services_block.definitions[1] {
                    ServiceItem::Service(s) => {
                        assert_eq!(s.name.name, "MyGreeter");
                        assert_eq!(s.id.value, 1);
                        assert!(s.extends.is_some());
                        assert_eq!(s.extends.as_ref().unwrap().name, "BaseService");
                        assert_eq!(s.annotations.len(), 2);
                        assert!(s.annotations.iter().any(|a| matches!(a, Annotation::Implements(id) if id.name == "Greeter")));
                        assert!(s.annotations.iter().any(|a| matches!(a, Annotation::Route(_))));
                    }
                    _ => panic!("Expected Service"),
                }
            }
            _ => panic!("Expected Services block"),
        }
    }

    #[test]
    fn test_parse_communication_block() {
        let content = r#"
            file_id: 0x5;
            communication {
                protocol CapnpRPC @id(0);

                channel UserEvents @id(1) {
                    description: "Events for users";
                    parameters: { userId: string };
                };

                event UserLoggedIn @id(2) {
                    $channel(UserEvents);
                    userId: string @id(0);
                    timestamp: timestamp @id(1);
                }
            }
        "#;
        let ast = parse_ssot_content(content, None).unwrap();
        assert_eq!(ast.definitions.len(), 1);
        match &ast.definitions[0] {
            TopLevelDefinition::Communication(comm_block) => {
                assert_eq!(comm_block.definitions.len(), 3);

                // Check Protocol
                match &comm_block.definitions[0] {
                    CommunicationItem::Protocol(p) => {
                        assert_eq!(p.name.name, "CapnpRPC");
                        assert_eq!(p.id.value, 0);
                    }
                    _ => panic!("Expected Protocol"),
                }

                // Check Channel
                match &comm_block.definitions[1] {
                    CommunicationItem::Channel(c) => {
                        assert_eq!(c.name.name, "UserEvents");
                        assert_eq!(c.id.value, 1);
                        assert!(c.description.is_some());
                        assert!(c.parameters.is_some());
                        assert_eq!(c.parameters.as_ref().unwrap().len(), 1);
                        assert_eq!(c.parameters.as_ref().unwrap()[0].key.name, "userId");
                    }
                    _ => panic!("Expected Channel"),
                }

                // Check Event
                match &comm_block.definitions[2] {
                    CommunicationItem::Event(e) => {
                        assert_eq!(e.name.name, "UserLoggedIn");
                        assert_eq!(e.id.value, 2);
                        assert_eq!(e.annotations.len(), 1);
                        assert!(matches!(&e.annotations[0], Annotation::Channel(id) if id.name == "UserEvents"));
                        assert_eq!(e.fields.len(), 2);
                        assert_eq!(e.fields[0].name.name, "userId");
                        assert_eq!(e.fields[1].name.name, "timestamp");
                    }
                    _ => panic!("Expected Event"),
                }
            }
            _ => panic!("Expected Communication block"),
        }
    }

    #[test]
    fn test_parse_actors_block() {
        let content = r#"
            file_id: 0x6;
            actors {
                actor AdminUser @id(0) { $type("role"); };
                actor PaymentGateway @id(1) { $description("External system"); };
            }
        "#;
        let ast = parse_ssot_content(content, None).unwrap();
        assert_eq!(ast.definitions.len(), 1);
        match &ast.definitions[0] {
            TopLevelDefinition::Actors(actors_block) => {
                assert_eq!(actors_block.definitions.len(), 2);
                match &actors_block.definitions[0] {
                    ActorDefinition {
                        name,
                        id,
                        annotations,
                    } => {
                        assert_eq!(name.name, "AdminUser");
                        assert_eq!(id.value, 0);
                        assert_eq!(annotations.len(), 1);
                        assert!(matches!(&annotations[0], Annotation::GenericKeyValue(id, val) if id.name == "type" && val == "role"));
                    } // _ => panic!("Expected ActorDefinition")
                }
                match &actors_block.definitions[1] {
                    ActorDefinition {
                        name,
                        id,
                        annotations,
                    } => {
                        assert_eq!(name.name, "PaymentGateway");
                        assert_eq!(id.value, 1);
                        assert_eq!(annotations.len(), 1);
                        assert!(matches!(&annotations[0], Annotation::Description(s) if s == "External system"));
                    } // _ => panic!("Expected ActorDefinition")
                }
            }
            _ => panic!("Expected Actors block"),
        }
    }

    #[test]
    fn test_parse_deployment_config_block() {
        let content = r#"
            file_id: 0x7;
            deployment_config {
                environment Production @id(0) {
                    variables: { logLevel: "info" };
                    $provider("aws");
                }
                infrastructure Compute @id(0) {
                     type: "kubernetes";
                     size: "large";
                }
                deployment WebApp @id(0) {
                    targetEnvironment: Production;
                    targetInfrastructure: { cluster: Compute };
                    deployable: MyWebAppService;
                    replicas: 3;
                }
            }
        "#;
        let ast = parse_ssot_content(content, None).unwrap();
        assert_eq!(ast.definitions.len(), 1);
        match &ast.definitions[0] {
            TopLevelDefinition::DeploymentConfig(dep_block) => {
                assert_eq!(dep_block.definitions.len(), 3);

                // Check Environment
                match &dep_block.definitions[0] {
                    DeploymentItem::Environment(e) => {
                        assert_eq!(e.name.name, "Production");
                        assert!(e.variables.is_some());
                        // Check for the $provider annotation specifically
                        assert!(e.annotations.iter().any(|a| matches!(a, Annotation::GenericKeyValue(id, val) if id.name == "provider" && val == "aws")));
                    }
                    _ => panic!("Expected Environment"),
                }
                // Check Infrastructure
                match &dep_block.definitions[1] {
                    DeploymentItem::Infrastructure(i) => {
                        assert_eq!(i.name.name, "Compute");
                        assert_eq!(i.attributes.len(), 2);
                        assert!(i.attributes.iter().any(|a| a.key.name == "type"));
                        assert!(i.attributes.iter().any(|a| a.key.name == "size"));
                    }
                    _ => panic!("Expected Infrastructure"),
                }
                // Check Deployment
                match &dep_block.definitions[2] {
                    DeploymentItem::Deployment(d) => {
                        assert_eq!(d.name.name, "WebApp");
                        assert_eq!(d.target_environment.as_ref().unwrap().name, "Production"); // Use as_ref().unwrap()
                        assert!(d.target_infrastructure.is_some());
                        assert_eq!(d.deployable.as_ref().unwrap().name, "MyWebAppService"); // Use as_ref().unwrap()
                        // Check other_attributes instead of attributes
                        assert!(d
                            .other_attributes
                            .iter()
                            .any(|a| a.key.name == "replicas"));
                    }
                    _ => panic!("Expected Deployment"),
                }
            }
            _ => panic!("Expected DeploymentConfig block"),
        }
    }

    #[test]
    fn test_parse_annotations() {
        let content = r#"
            file_id: 0x8;
            types {
                struct Annotated @id(0) {
                    $description("Hello");
                    $validate(min: 0, max: 100, required: true);
                    $db(table: "users", index: ["name", "email"]);
                    $meta(flag: true, list: [1, "a", false], obj: { k: "v" });
                    $customFlag;
                    $customKV("some_value");
                    field: string @id(0);
                }
            }
        "#;
        let ast = parse_ssot_content(content, None).unwrap();
        let types_block = match &ast.definitions[0] {
            TopLevelDefinition::Types(t) => t,
            _ => panic!(),
        };
        let struct_def = match &types_block.definitions[0] {
            TypeDefinition::Struct(s) => s,
            _ => panic!(),
        };
        assert_eq!(struct_def.annotations.len(), 6);
        // Basic checks
        assert!(struct_def
            .annotations
            .iter()
            .any(|a| matches!(a, Annotation::Description(s) if s == "Hello")));
        assert!(struct_def
            .annotations
            .iter()
            .any(|a| matches!(a, Annotation::Validate(_))));
        assert!(struct_def.annotations.iter().any(|a| matches!(a, Annotation::Db(_))));
        assert!(struct_def.annotations.iter().any(|a| matches!(a, Annotation::Meta(_))));
        assert!(struct_def
            .annotations
            .iter()
            .any(|a| matches!(a, Annotation::GenericFlag(id) if id.name == "customFlag")));
        assert!(struct_def
            .annotations
            .iter()
            .any(|a| matches!(a, Annotation::GenericKeyValue(id, val) if id.name == "customKV" && val == "some_value")));

        // Optionally, check details of a specific annotation like $meta
        let meta_annotation = struct_def.annotations.iter().find(|a| matches!(a, Annotation::Meta(_))).unwrap();
        if let Annotation::Meta(args) = meta_annotation {
            assert_eq!(args.len(), 3);
            // Further checks on args if needed
        } else {
            panic!("Expected Meta annotation");
        }
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
