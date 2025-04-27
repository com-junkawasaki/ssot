use crate::ast::{Identifier, NumericId, SsotAst, FileId, ImportStatement, TopLevelDefinition, TypesBlock, TypeDefinition, StructDefinition, FieldDefinition, EnumDefinition, EnumVariant, TypeSpecifier, Annotation, Argument, AnnotationValue, MachineDefinition, MachinesBlock, ContextDefinition, ContextFieldDefinition, StatesBlock, StateDefinition, TransitionDefinition, ActionsBlock, ActionDefinition, GuardsBlock, GuardDefinition, InvokesBlock, InvokeDefinition, InvokeSource, StateInvokeDefinition, InvokeTransitionTarget, Duration, TimeUnit, AfterTransitionDefinition, HistoryDefinition, HistoryType, ServicesBlock, ServiceItem, InterfaceDefinition, MethodDefinition, ParameterDefinition, ServiceDefinition, CommunicationBlock, CommunicationItem, ProtocolDefinition, ChannelDefinition, EventDefinition, ActorsBlock, ActorDefinition, DeploymentConfigBlock, DeploymentItem, EnvironmentDefinition, InfrastructureDefinition, DeploymentDefinition, AttributeDefinition};
use pest::Parser;
use pest_derive::Parser;
use pest::iterators::{Pair, Pairs};
use std::path::{Path, PathBuf};
use thiserror::Error;

#[derive(Parser)]
#[grammar = "ssot.pest"] // Path relative to src
struct SsotParser;

#[derive(Error, Debug)]
pub enum ParseError {
    #[error("Pest parsing error: {0}")]
PestError(#[from] pest::error::Error<Rule>),
    #[error("Error reading file {path}: {source}")]
FileReadError {
        path: PathBuf,
        #[source]
        source: std::io::Error,
    },
    #[error("Invalid input: {message}")]
InvalidInput { message: String },
    #[error("Failed to parse integer: {0}")]
ParseIntError(#[from] std::num::ParseIntError),
    #[error("Unexpected rule: expected {expected:?}, found {found:?}")]
UnexpectedRule { expected: Rule, found: Rule },
     #[error("Missing expected rule: {expected:?}")]
    MissingRule { expected: Rule },
}

pub type ParseResult<T> = Result<T, ParseError>;

/// Parses the content of an SSOT file string into an AST.
pub fn parse_ssot_content(content: &str, source_path: Option<PathBuf>) -> ParseResult<SsotAst> {
    let pairs = SsotParser::parse(Rule::file, content)?;

    let mut ast = SsotAst {
        source_path,
        file_id: None,
        imports: Vec::new(),
        definitions: Vec::new(),
    };

    let file_pair = pairs.peek().ok_or_else(|| ParseError::InvalidInput { message: "Empty input".to_string() })?;

    if file_pair.as_rule() != Rule::file {
        return Err(ParseError::UnexpectedRule { expected: Rule::file, found: file_pair.as_rule() });
    }

    // Iterate over the inner pairs of the 'file' rule
    for pair in file_pair.into_inner() { 
        match pair.as_rule() {
            Rule::COMMENT => { /* Skip comments */ }
            Rule::EOI => { /* End of Input, expected */ }

            Rule::definition => { // Handle the PUSHed definition rule
                // Get the actual definition *inside* the 'definition' pair
                if let Some(inner_definition_pair) = pair.into_inner().next() {
                    match inner_definition_pair.as_rule() {
                        Rule::file_id => {
                            if ast.file_id.is_some() {
                                eprintln!("Warning: Duplicate file ID found, ignoring subsequent IDs.");
                            } else {
                                ast.file_id = Some(parse_file_id(inner_definition_pair)?);
                            }
                        }
                        Rule::import_statement => {
                            ast.imports.push(parse_import_statement(inner_definition_pair)?);
                        }
                        Rule::types_block => {
                            ast.definitions.push(TopLevelDefinition::Types(parse_types_block(inner_definition_pair)?));
                        }
                        Rule::machines_block => {
                            ast.definitions.push(TopLevelDefinition::Machines(parse_machines_block(inner_definition_pair)?));
                        }
                        Rule::actors_block => {
                            ast.definitions.push(TopLevelDefinition::Actors(parse_actors_block(inner_definition_pair)?));
                        }
                        Rule::communication_block => {
                            ast.definitions.push(TopLevelDefinition::Communication(parse_communication_block(inner_definition_pair)?));
                        }
                         Rule::services_block => {
                            ast.definitions.push(TopLevelDefinition::Services(parse_services_block(inner_definition_pair)?));
                        }
                         Rule::deployment_config_block => {
                            ast.definitions.push(TopLevelDefinition::DeploymentConfig(parse_deployment_config_block(inner_definition_pair)?));
                        }
                        Rule::annotation => {
                             eprintln!("Skipping top-level annotation definition for now.");
                             // TODO: Handle top-level annotations if needed (e.g., store them separately)
                        }
                        // Handle unexpected rule *inside* definition
                        inner_rule => {
                             eprintln!("Warning: Skipping unexpected rule inside definition: {:?}", inner_rule);
                        }
                    }
                } else {
                     eprintln!("Warning: Found empty definition rule.");
                }
            }

            // Catch rules that are not COMMENT, EOI, or definition
            other_rule => {
                 eprintln!("Warning: Skipping unexpected element at top level: {:?}", other_rule);
            }
        }
    }

    Ok(ast)
}

/// Parses a file_id pair into an ast::FileId.
fn parse_file_id(pair: Pair<Rule>) -> ParseResult<FileId> {
    if pair.as_rule() != Rule::file_id {
        return Err(ParseError::UnexpectedRule { expected: Rule::file_id, found: pair.as_rule() });
    }
    let hex_pair = pair.into_inner().next().ok_or(ParseError::MissingRule{ expected: Rule::hex_literal})?;
    if hex_pair.as_rule() != Rule::hex_literal {
         return Err(ParseError::UnexpectedRule { expected: Rule::hex_literal, found: hex_pair.as_rule() });
    }
    let hex_str = hex_pair.as_str().trim_start_matches("0x");
    let value = u64::from_str_radix(hex_str, 16)?;
    Ok(FileId { value })
}

/// Parses an import_statement pair into an ast::ImportStatement.
fn parse_import_statement(pair: Pair<Rule>) -> ParseResult<ImportStatement> {
    if pair.as_rule() != Rule::import_statement {
        return Err(ParseError::UnexpectedRule { expected: Rule::import_statement, found: pair.as_rule() });
    }
    let path_pair = pair.into_inner().next().ok_or(ParseError::MissingRule{ expected: Rule::string_literal})?;
     if path_pair.as_rule() != Rule::string_literal {
         return Err(ParseError::UnexpectedRule { expected: Rule::string_literal, found: path_pair.as_rule() });
    }
    // Remove quotes from the string literal
    let path = path_pair.as_str().trim_matches('"').to_string();
    Ok(ImportStatement { path })
}

// --- Helper Functions (Placeholders) ---

fn parse_identifier(pair: Pair<Rule>) -> ParseResult<Identifier> {
    if pair.as_rule() != Rule::identifier {
        return Err(ParseError::UnexpectedRule { expected: Rule::identifier, found: pair.as_rule() });
    }
    Ok(Identifier { name: pair.as_str().to_string() })
}

fn parse_numeric_id(pair: Pair<Rule>) -> ParseResult<NumericId> {
    if pair.as_rule() != Rule::integer_literal {
        return Err(ParseError::UnexpectedRule { expected: Rule::integer_literal, found: pair.as_rule() });
    }
    let value = pair.as_str().parse::<u64>()?;
     Ok(NumericId { value })
}

// --- Block Parsing --- 

fn parse_types_block(pair: Pair<Rule>) -> ParseResult<TypesBlock> {
    if pair.as_rule() != Rule::types_block {
        return Err(ParseError::UnexpectedRule { expected: Rule::types_block, found: pair.as_rule() });
    }
    println!("Parsing types block: {}", pair.as_str()); // Debug print

    let mut annotations = Vec::new();
    let mut definitions = Vec::new();

    // Grammar: types_block = { annotation* ~ "types" ~ "{" ~ type_definition* ~ "}" }
    // Iterate through the direct children provided by Pest based on the rule
    for inner_pair in pair.into_inner() {
        match inner_pair.as_rule() {
            Rule::annotation => { // Handle annotation* part
                annotations.push(parse_annotation(inner_pair)?);
            }
            // Handle struct/enum definitions directly because type_definition is silent
            Rule::struct_definition => {
                definitions.push(TypeDefinition::Struct(parse_struct_definition(inner_pair)?));
            }
            Rule::enum_definition => {
                 definitions.push(TypeDefinition::Enum(parse_enum_definition(inner_pair)?));
            }
            // Explicitly ignore the keyword based on the grammar rule structure
            Rule::identifier if inner_pair.as_str() == "types" => { /* Skip 'types' keyword */ }
            // Assuming Pest consumes `{` and `}` as part of the rule structure.
            
            // Catch truly unexpected rules
            rule => {
                eprintln!("Warning: Skipping unexpected rule within types_block: {:?} -> '{}'", rule, inner_pair.as_str());
            }
        }
    }
    Ok(TypesBlock { annotations, definitions })
}

fn parse_machines_block(pair: Pair<Rule>) -> ParseResult<MachinesBlock> {
    if pair.as_rule() != Rule::machines_block {
        return Err(ParseError::UnexpectedRule { expected: Rule::machines_block, found: pair.as_rule() });
    }
    println!("Parsing machines block: {}", pair.as_str());

    let mut annotations = Vec::new();
    let mut definitions = Vec::new();

    // Grammar: machines_block = { annotation* ~ "machines" ~ "{" ~ machine_definition* ~ "}" }
    // Iterate through the direct children provided by Pest based on the rule
    for inner_pair in pair.into_inner() {
        match inner_pair.as_rule() {
            Rule::annotation => { // Handle annotation* part
                annotations.push(parse_annotation(inner_pair)?);
            }
            Rule::machine_definition => { // Handle machine_definition* part
                definitions.push(parse_machine_definition(inner_pair)?);
            }
            // Explicitly ignore the keyword based on the grammar rule structure
            Rule::identifier if inner_pair.as_str() == "machines" => { /* Skip 'machines' keyword */ }
            // Assuming Pest consumes `{` and `}` as part of the rule structure.
            
            // Catch truly unexpected rules
            rule => {
                eprintln!("Warning: Skipping unexpected rule within machines_block: {:?} -> '{}'", rule, inner_pair.as_str());
            }
        }
    }
    Ok(MachinesBlock { annotations, definitions })
}

// --- Definition Parsing (Implementations & Stubs) --- 

fn parse_struct_definition(pair: Pair<Rule>) -> ParseResult<StructDefinition> {
     if pair.as_rule() != Rule::struct_definition {
        return Err(ParseError::UnexpectedRule { expected: Rule::struct_definition, found: pair.as_rule() });
    }
    println!("Parsing struct definition: {}", pair.as_str()); // Debug print

    let mut inner_pairs = pair.into_inner();
    let mut annotations = Vec::new();
    let mut name: Option<Identifier> = None;
    let mut id: Option<NumericId> = None;
    let mut fields = Vec::new();

    // Order based on grammar: annotation*, "struct", identifier, "@id(", integer_literal, ")", "{", field_definition*, "}" 
    while let Some(p) = inner_pairs.peek() {
        match p.as_rule() {
             Rule::annotation => annotations.push(parse_annotation(inner_pairs.next().unwrap())?),
             Rule::identifier => {
                 name = Some(parse_identifier(inner_pairs.next().unwrap())?);
                 // Now expect the ID
                 let id_pair = inner_pairs.next().ok_or(ParseError::MissingRule { expected: Rule::integer_literal })?;
                 id = Some(parse_numeric_id(id_pair)?);
             }
             Rule::field_definition => fields.push(parse_field_definition(inner_pairs.next().unwrap())?),
             // Catch unexpected rules based on grammar structure
             rule => return Err(ParseError::UnexpectedRule { expected: Rule::annotation /* or identifier or field_definition */, found: rule })
        }
    }

    Ok(StructDefinition {
        name: name.ok_or(ParseError::MissingRule { expected: Rule::identifier })?,
        id: id.ok_or(ParseError::MissingRule { expected: Rule::integer_literal })?,
        fields,
        annotations,
    })
}

fn parse_field_definition(pair: Pair<Rule>) -> ParseResult<FieldDefinition> {
     if pair.as_rule() != Rule::field_definition {
        return Err(ParseError::UnexpectedRule { expected: Rule::field_definition, found: pair.as_rule() });
    }
    println!("Parsing field definition: {}", pair.as_str()); // Debug print

    let mut inner_pairs = pair.into_inner();
    let mut annotations = Vec::new();
    let mut leading_annotations = Vec::new(); // Annotations before the name
    let mut name: Option<Identifier> = None;
    let mut type_spec: Option<TypeSpecifier> = None;
    let mut id: Option<NumericId> = None;

     // Order: annotation*, identifier, ":", type_specifier, "@id(", integer_literal, ")", annotation*, ";"
     // Parse leading annotations
     while let Some(p) = inner_pairs.peek() {
         if p.as_rule() == Rule::annotation {
             leading_annotations.push(parse_annotation(inner_pairs.next().unwrap())?);
         } else {
             break;
         }
     }

    // Parse required parts: identifier, type_specifier, integer_literal (for ID)
    let name_pair = inner_pairs.next().ok_or(ParseError::MissingRule { expected: Rule::identifier })?;
    if name_pair.as_rule() != Rule::identifier {
        return Err(ParseError::UnexpectedRule { expected: Rule::identifier, found: name_pair.as_rule() });
    }
    name = Some(parse_identifier(name_pair)?);

    let type_spec_pair = inner_pairs.next().ok_or(ParseError::MissingRule { expected: Rule::type_specifier })?;
     if type_spec_pair.as_rule() != Rule::type_specifier {
        return Err(ParseError::UnexpectedRule { expected: Rule::type_specifier, found: type_spec_pair.as_rule() });
    }
    type_spec = Some(parse_type_specifier(type_spec_pair)?);

    let id_pair = inner_pairs.next().ok_or(ParseError::MissingRule { expected: Rule::integer_literal })?;
     if id_pair.as_rule() != Rule::integer_literal {
        return Err(ParseError::UnexpectedRule { expected: Rule::integer_literal, found: id_pair.as_rule() });
    }
    id = Some(parse_numeric_id(id_pair)?);

    // Parse trailing annotations
    while let Some(p) = inner_pairs.next() { // Consumes the rest
        if p.as_rule() == Rule::annotation {
            annotations.push(parse_annotation(p)?);
        } else {
            // Should ideally only find trailing annotations before the implicit semicolon handled by pest
            return Err(ParseError::UnexpectedRule { expected: Rule::annotation, found: p.as_rule() })
        }
    }

    Ok(FieldDefinition {
        name: name.unwrap(), // Checked above
        type_spec: type_spec.unwrap(), // Checked above
        id: id.unwrap(), // Checked above
        annotations: [leading_annotations, annotations].concat(),
    })
}

fn parse_enum_definition(pair: Pair<Rule>) -> ParseResult<EnumDefinition> {
     if pair.as_rule() != Rule::enum_definition {
        return Err(ParseError::UnexpectedRule { expected: Rule::enum_definition, found: pair.as_rule() });
    }
    println!("Parsing enum definition: {}", pair.as_str()); // Debug print

    let mut inner_pairs = pair.into_inner();
    let mut annotations = Vec::new();
    let mut name: Option<Identifier> = None;
    let mut id: Option<NumericId> = None;
    let mut variants = Vec::new();

    // Order: annotation*, "enum", identifier, "@id(", integer_literal, ")", "{", enum_variant*, "}"
    while let Some(p) = inner_pairs.peek() {
        match p.as_rule() {
             Rule::annotation => annotations.push(parse_annotation(inner_pairs.next().unwrap())?),
             Rule::identifier => {
                 name = Some(parse_identifier(inner_pairs.next().unwrap())?);
                 // Expect ID
                 let id_pair = inner_pairs.next().ok_or(ParseError::MissingRule { expected: Rule::integer_literal })?;
                 id = Some(parse_numeric_id(id_pair)?);
             }
             Rule::enum_variant => variants.push(parse_enum_variant(inner_pairs.next().unwrap())?),
             rule => return Err(ParseError::UnexpectedRule { expected: Rule::annotation /* or identifier or enum_variant */, found: rule })
        }
    }

    Ok(EnumDefinition {
        name: name.ok_or(ParseError::MissingRule { expected: Rule::identifier })?,
        id: id.ok_or(ParseError::MissingRule { expected: Rule::integer_literal })?,
        variants,
        annotations,
    })
}

fn parse_enum_variant(pair: Pair<Rule>) -> ParseResult<EnumVariant> {
     if pair.as_rule() != Rule::enum_variant {
        return Err(ParseError::UnexpectedRule { expected: Rule::enum_variant, found: pair.as_rule() });
    }
     println!("Parsing enum variant: {}", pair.as_str()); // Debug print

    let mut inner_pairs = pair.into_inner();
    let mut annotations = Vec::new();
    let mut leading_annotations = Vec::new();
    let mut name: Option<Identifier> = None;
    let mut id: Option<NumericId> = None;

     // Order: annotation*, identifier, "@id(", integer_literal, ")", annotation*, ";"
     // Leading annotations
     while let Some(p) = inner_pairs.peek() {
         if p.as_rule() == Rule::annotation {
             leading_annotations.push(parse_annotation(inner_pairs.next().unwrap())?);
         } else {
             break;
         }
     }

     // Required parts
    let name_pair = inner_pairs.next().ok_or(ParseError::MissingRule { expected: Rule::identifier })?;
    if name_pair.as_rule() != Rule::identifier {
        return Err(ParseError::UnexpectedRule { expected: Rule::identifier, found: name_pair.as_rule() });
    }
    name = Some(parse_identifier(name_pair)?);

    let id_pair = inner_pairs.next().ok_or(ParseError::MissingRule { expected: Rule::integer_literal })?;
    if id_pair.as_rule() != Rule::integer_literal {
        return Err(ParseError::UnexpectedRule { expected: Rule::integer_literal, found: id_pair.as_rule() });
    }
    id = Some(parse_numeric_id(id_pair)?);

    // Trailing annotations
    while let Some(p) = inner_pairs.next() { // Consumes the rest
        if p.as_rule() == Rule::annotation {
            annotations.push(parse_annotation(p)?);
        } else {
            return Err(ParseError::UnexpectedRule { expected: Rule::annotation, found: p.as_rule() })
        }
    }

    Ok(EnumVariant {
        name: name.unwrap(), // Checked
        id: id.unwrap(),     // Checked
        annotations: [leading_annotations, annotations].concat(),
    })
}

fn parse_machine_definition(pair: Pair<Rule>) -> ParseResult<MachineDefinition> {
    if pair.as_rule() != Rule::machine_definition {
        return Err(ParseError::UnexpectedRule { expected: Rule::machine_definition, found: pair.as_rule() });
    }
    println!("Parsing machine definition: {}", pair.as_str());

    let mut inner_pairs = pair.into_inner();
    let mut annotations = Vec::new();
    let mut name: Option<Identifier> = None;
    let mut id: Option<NumericId> = None;
    let mut context: Option<ContextDefinition> = None;
    let mut states: Option<StatesBlock> = None;
    let mut actions: Option<ActionsBlock> = None;
    let mut guards: Option<GuardsBlock> = None;
    let mut invokes: Option<InvokesBlock> = None;

    // Order based on grammar: annotation*, "machine", identifier, "@id(", int, ")", "{", machine_element*, "}"
    while let Some(p) = inner_pairs.peek() {
        match p.as_rule() {
            Rule::annotation => annotations.push(parse_annotation(inner_pairs.next().unwrap())?),
            Rule::identifier => {
                if name.is_none() { // First identifier is the name
                     name = Some(parse_identifier(inner_pairs.next().unwrap())?);
                } else {
                     // Skip keywords like "machine"
                     if inner_pairs.peek().unwrap().as_str() != "machine" {
                         eprintln!("Warning: Skipping unexpected identifier inside machine definition: {:?}", inner_pairs.peek().unwrap().as_str());
                     }
                     inner_pairs.next(); // Consume the keyword or unexpected identifier
                }
            },
            Rule::integer_literal => {
                 id = Some(parse_numeric_id(inner_pairs.next().unwrap())?);
            },
            Rule::machine_element => {
                let element_pair = inner_pairs.next().unwrap().into_inner().next().ok_or_else(|| ParseError::InvalidInput { message: "Empty machine_element rule".to_string() })?;
                 match element_pair.as_rule() {
                     Rule::context_definition => context = Some(parse_context_definition(element_pair)?),
                     Rule::states_definition => states = Some(parse_states_definition(element_pair)?),
                     Rule::actions_definition => actions = Some(parse_actions_block(element_pair)?),
                     Rule::guards_definition => guards = Some(parse_guards_block(element_pair)?),
                     Rule::invokes_definition => invokes = Some(parse_invokes_block(element_pair)?),
                     rule => {
                        eprintln!("Warning: Skipping unexpected rule within machine_element: {:?}", rule);
                    }
                 }
            }
             // Skip keywords and braces implicitly handled by Pest structure
            _ => { inner_pairs.next(); } // Consume other pairs like keywords, braces, @id(,)
        }
    }


    Ok(MachineDefinition {
        name: name.ok_or_else(|| ParseError::InvalidInput { message: "Missing machine name".to_string() })?,
        id: id.ok_or_else(|| ParseError::InvalidInput { message: "Missing machine id".to_string() })?,
        annotations,
        context,
        states,
        actions,
        guards,
        invokes,
    })
}

fn parse_context_definition(pair: Pair<Rule>) -> ParseResult<ContextDefinition> {
     if pair.as_rule() != Rule::context_definition {
        return Err(ParseError::UnexpectedRule { expected: Rule::context_definition, found: pair.as_rule() });
    }
    println!("Parsing context definition: {}", pair.as_str());

    let mut inner_pairs = pair.into_inner();
    let mut annotations = Vec::new();
    let mut id: Option<NumericId> = None;
    let mut fields = Vec::new();

    // Order: annotation*, "context", "@id(", integer_literal, ")", "{", context_field_definition*, "}" 
    while let Some(p) = inner_pairs.peek() {
        match p.as_rule() {
             Rule::annotation => annotations.push(parse_annotation(inner_pairs.next().unwrap())?),
             Rule::integer_literal => { // ID follows annotations and "context" keyword implicitly 
                 id = Some(parse_numeric_id(inner_pairs.next().unwrap())?);
             }
             Rule::context_field_definition => {
                 fields.push(parse_context_field_definition(inner_pairs.next().unwrap())?);
             }
            rule => return Err(ParseError::UnexpectedRule { expected: Rule::annotation /* or integer_literal or context_field_definition */, found: rule })
        }
    }

    Ok(ContextDefinition {
        id: id.ok_or(ParseError::MissingRule { expected: Rule::integer_literal })?,
        fields,
        annotations,
    })
}

fn parse_context_field_definition(pair: Pair<Rule>) -> ParseResult<ContextFieldDefinition> {
    if pair.as_rule() != Rule::context_field_definition {
        return Err(ParseError::UnexpectedRule { expected: Rule::context_field_definition, found: pair.as_rule() });
    }
    println!("Parsing context field definition: {}", pair.as_str());

     // Reuses much of parse_field_definition logic
     let mut inner_pairs = pair.into_inner();
     let mut annotations = Vec::new();
     let mut leading_annotations = Vec::new();
     let mut name: Option<Identifier> = None;
     let mut type_spec: Option<TypeSpecifier> = None;
     let mut id: Option<NumericId> = None;
    // TODO: Parse default value from annotation if present

     while let Some(p) = inner_pairs.peek() {
         if p.as_rule() == Rule::annotation {
             leading_annotations.push(parse_annotation(inner_pairs.next().unwrap())?);
         } else {
             break;
         }
     }

    let name_pair = inner_pairs.next().ok_or(ParseError::MissingRule { expected: Rule::identifier })?;
    name = Some(parse_identifier(name_pair)?);

    let type_spec_pair = inner_pairs.next().ok_or(ParseError::MissingRule { expected: Rule::type_specifier })?;
    type_spec = Some(parse_type_specifier(type_spec_pair)?);

    let id_pair = inner_pairs.next().ok_or(ParseError::MissingRule { expected: Rule::integer_literal })?;
    id = Some(parse_numeric_id(id_pair)?);

    while let Some(p) = inner_pairs.next() {
        if p.as_rule() == Rule::annotation {
            annotations.push(parse_annotation(p)?);
            // TODO: Check if it's a $default annotation and store its value
        } else {
            return Err(ParseError::UnexpectedRule { expected: Rule::annotation, found: p.as_rule() })
        }
    }

    Ok(ContextFieldDefinition {
        name: name.unwrap(),
        type_spec: type_spec.unwrap(),
        id: id.unwrap(),
        annotations: [leading_annotations, annotations].concat(),
    })
}

fn parse_states_definition(pair: Pair<Rule>) -> ParseResult<StatesBlock> {
    if pair.as_rule() != Rule::states_definition {
        return Err(ParseError::UnexpectedRule { expected: Rule::states_definition, found: pair.as_rule() });
    }
    println!("Parsing states definition: {}", pair.as_str());

    let mut inner_pairs = pair.into_inner();
    let mut annotations = Vec::new();
    let mut id: Option<NumericId> = None;
    let mut states = Vec::new();

    // Order: annotation*, "states", "@id(", integer_literal, ")", "{", state_definition*, "}" 
    while let Some(p) = inner_pairs.peek() {
        match p.as_rule() {
            Rule::annotation => annotations.push(parse_annotation(inner_pairs.next().unwrap())?),
            Rule::integer_literal => { // ID follows annotations and "states" keyword
                 id = Some(parse_numeric_id(inner_pairs.next().unwrap())?);
            }
            Rule::state_definition => {
                 states.push(parse_state_definition(inner_pairs.next().unwrap())?);
            }
            rule => return Err(ParseError::UnexpectedRule { expected: Rule::annotation /* or int or state */, found: rule })
        }
    }

     Ok(StatesBlock {
        id: id.ok_or(ParseError::MissingRule { expected: Rule::integer_literal })?,
        states,
        annotations,
    })
}

fn parse_state_definition(pair: Pair<Rule>) -> ParseResult<StateDefinition> {
    if pair.as_rule() != Rule::state_definition {
        return Err(ParseError::UnexpectedRule { expected: Rule::state_definition, found: pair.as_rule() });
    }
    println!("Parsing state definition: {}", pair.as_str());

    let mut inner_pairs = pair.into_inner();
    let mut annotations = Vec::new();
    let mut name: Option<Identifier> = None;
    let mut id: Option<NumericId> = None;
    let mut transitions = Vec::new();
    let mut invokes = Vec::new();
    let mut on_entry_actions = Vec::new();
    let mut on_exit_actions = Vec::new();
    let mut after_transitions = Vec::new();
    let mut nested_states: Option<Box<StatesBlock>> = None;
    let mut history: Option<HistoryDefinition> = None;
    let mut is_initial = false;
    let mut is_final = false;
    let mut is_parallel = false;

    // Order: annotation*, "state", identifier, "@id(", int, ")", "{", state_element*, "}"
     while let Some(p) = inner_pairs.peek() {
        match p.as_rule() {
            Rule::annotation => {
                 let annotation = parse_annotation(inner_pairs.next().unwrap())?;
                 // Check for specific state flags within general annotations
                 match annotation {
                     Annotation::Initial => is_initial = true,
                     Annotation::Final => is_final = true,
                     Annotation::Parallel => is_parallel = true,
                     _ => {} // Keep other annotations as well
                 }
                 annotations.push(annotation);
            }
             Rule::identifier => {
                 if name.is_none() { // First identifier is the name
                      name = Some(parse_identifier(inner_pairs.next().unwrap())?);
                 } else {
                     // Skip keyword "state"
                     if inner_pairs.peek().unwrap().as_str() != "state" {
                          eprintln!("Warning: Skipping unexpected identifier inside state definition: {:?}", inner_pairs.peek().unwrap().as_str());
                     }
                     inner_pairs.next();
                 }
            },
            Rule::integer_literal => {
                id = Some(parse_numeric_id(inner_pairs.next().unwrap())?);
            },
            Rule::state_element => {
                 let element_pair = inner_pairs.next().unwrap().into_inner().next().ok_or_else(|| ParseError::InvalidInput { message: "Empty state_element rule".to_string() })?;
                 match element_pair.as_rule() {
                    Rule::initial_annotation => { is_initial = true; annotations.push(Annotation::Initial); }
                    Rule::final_annotation => { is_final = true; annotations.push(Annotation::Final); }
                    Rule::parallel_annotation => { is_parallel = true; annotations.push(Annotation::Parallel); }
                    Rule::on_entry => on_entry_actions = parse_on_entry(element_pair)?,
                    Rule::on_exit => on_exit_actions = parse_on_exit(element_pair)?,
                    Rule::on_transition => transitions.push(parse_on_transition(element_pair)?),
                    Rule::after_transition => after_transitions.push(parse_after_transition(element_pair)?),
                    Rule::state_invoke => invokes.push(parse_state_invoke(element_pair)?),
                    Rule::states_definition => {
                        if nested_states.is_some() {
                            eprintln!("Warning: Duplicate nested states definition found within state '{}', ignoring subsequent.", name.as_ref().map(|n| n.name.as_str()).unwrap_or("unknown"));
                        } else {
                            nested_states = Some(Box::new(parse_states_definition(element_pair)?));
                        }
                    }
                    Rule::history_definition => {
                        if history.is_some() {
                            eprintln!("Warning: Duplicate history definition found within state '{}', ignoring subsequent.", name.as_ref().map(|n| n.name.as_str()).unwrap_or("unknown"));
                        } else {
                            history = Some(parse_history_definition(element_pair)?);
                        }
                    }
                     // TODO: Handle history etc.
                     rule => {
                         eprintln!("Warning: Skipping unexpected rule within state_element: {:?}", rule);
                     }
                 }
            }
            _ => { inner_pairs.next(); } // Consume other pairs
        }
     }


    Ok(StateDefinition {
        name: name.ok_or_else(|| ParseError::InvalidInput { message: "Missing state name".to_string() })?,
        id: id.ok_or_else(|| ParseError::InvalidInput { message: "Missing state id".to_string() })?,
        annotations,
        transitions,
        invokes,
        on_entry: on_entry_actions,
        on_exit: on_exit_actions,
        after_transitions,
        nested_states,
        history,
        is_initial,
        is_final,
        is_parallel,
    })
}

fn parse_on_transition(pair: Pair<Rule>) -> ParseResult<TransitionDefinition> {
    if pair.as_rule() != Rule::on_transition {
        return Err(ParseError::UnexpectedRule { expected: Rule::on_transition, found: pair.as_rule() });
    }
     println!("Parsing on transition: {}", pair.as_str());

    let mut inner_pairs = pair.into_inner();
    let mut event: Option<Identifier> = None;
    let mut id: Option<NumericId> = None;
    let mut target: Option<Identifier> = None;
    let mut actions = Vec::new(); // Added
    let mut guards = Vec::new();  // Added

    // Order: "on", identifier(event), "@id(", int, ")", transition_details, ";"
    // transition_details: "transition", identifier(target), transition_block?
    // transition_block: "{", (action_ref | guard_ref)*, "}"

    while let Some(p) = inner_pairs.peek() {
        match p.as_rule() {
             Rule::identifier => {
                let ident_pair = inner_pairs.next().unwrap();
                 if event.is_none() { // First identifier is the event
                     event = Some(parse_identifier(ident_pair)?);
                 } else {
                      // Skip keywords "on", "transition"
                     if ident_pair.as_str() != "on" && ident_pair.as_str() != "transition" {
                         eprintln!("Warning: Skipping unexpected identifier inside on_transition: {:?}", ident_pair.as_str());
                     }
                 }
            },
            Rule::integer_literal => {
                 id = Some(parse_numeric_id(inner_pairs.next().unwrap())?);
            },
            Rule::transition_details => {
                let mut details_pairs = inner_pairs.next().unwrap().into_inner();
                while let Some(dp) = details_pairs.peek() {
                    match dp.as_rule() {
                        Rule::identifier => { // This must be the target state
                            if target.is_none() {
                                target = Some(parse_identifier(details_pairs.next().unwrap())?);
                            } else {
                                // Skip keyword "transition"
                                if details_pairs.peek().unwrap().as_str() != "transition" {
                                    eprintln!("Warning: Skipping unexpected identifier inside transition_details: {:?}", details_pairs.peek().unwrap().as_str());
                                }
                                details_pairs.next();
                            }
                        }
                        Rule::transition_block => { // Parse actions and guards
                            let block_pair = details_pairs.next().unwrap();
                             println!("Parsing transition block: {}", block_pair.as_str());
                            for item_pair in block_pair.into_inner() {
                                match item_pair.as_rule() {
                                    Rule::action_ref => {
                                        let action_name = item_pair.into_inner().find(|p| p.as_rule() == Rule::identifier)
                                            .ok_or_else(|| ParseError::InvalidInput { message: "Missing action name in action_ref".to_string() })?;
                                        actions.push(parse_identifier(action_name)?);
                                         println!("  Found action_ref: {}", actions.last().unwrap().name);
                                    }
                                    Rule::guard_ref => {
                                        let guard_name = item_pair.into_inner().find(|p| p.as_rule() == Rule::identifier)
                                             .ok_or_else(|| ParseError::InvalidInput { message: "Missing guard name in guard_ref".to_string() })?;
                                        guards.push(parse_identifier(guard_name)?);
                                        println!("  Found guard_ref: {}", guards.last().unwrap().name);
                                    }
                                    _ => { /* Skip other inner elements like braces */ }
                                }
                            }
                        }
                         _ => { details_pairs.next(); } // Consume other pairs like keywords
                    }
                }
            }
             _ => { inner_pairs.next(); } // Consume other pairs like @id(,) and ;
        }
    }


    Ok(TransitionDefinition {
        event: event.ok_or_else(|| ParseError::InvalidInput { message: "Missing event name in transition".to_string() })?,
        target: target.ok_or_else(|| ParseError::InvalidInput { message: "Missing target state name in transition".to_string() })?,
        id: id.ok_or_else(|| ParseError::InvalidInput { message: "Missing id in transition".to_string() })?,
        annotations: Vec::new(), // TODO: Parse annotations on transitions if grammar allows
        actions, // Added
        guards,  // Added
    })
}

// --- Helper Functions (Implementations) ---

fn parse_annotation(pair: Pair<Rule>) -> ParseResult<Annotation> {
     if pair.as_rule() != Rule::annotation {
        return Err(ParseError::UnexpectedRule { expected: Rule::annotation, found: pair.as_rule() });
    }
    println!("Parsing annotation: {}", pair.as_str());

    let inner_pair = pair.into_inner().next().ok_or_else(|| ParseError::InvalidInput { message: "Empty annotation rule".to_string() })?;

    match inner_pair.as_rule() {
        Rule::description_annotation => {
            let value = parse_string_literal(inner_pair.into_inner().next().unwrap())?;
            Ok(Annotation::Description(value))
        }
        Rule::validate_annotation => {
            let args = parse_annotation_args(inner_pair.into_inner())?;
            Ok(Annotation::Validate(args))
        }
         Rule::db_annotation => {
            let args = parse_annotation_args(inner_pair.into_inner())?;
            Ok(Annotation::Db(args))
        }
         Rule::meta_annotation => {
            let args = parse_annotation_args(inner_pair.into_inner())?;
            Ok(Annotation::Meta(args))
        }
        Rule::generic_flag_annotation => {
             let ident = parse_identifier(inner_pair.into_inner().next().unwrap())?;
            Ok(Annotation::GenericFlag(ident))
        }
        Rule::generic_kv_annotation => {
            let mut kv_pairs = inner_pair.into_inner();
            let ident = parse_identifier(kv_pairs.next().unwrap())?;
            let value_pair = kv_pairs.next().unwrap(); // This is annotation_value
            let value = parse_annotation_value(value_pair)?;
             // For now, only handle string value for GenericKeyValue as per AST definition
            if let AnnotationValue::String(s) = value {
                 Ok(Annotation::GenericKeyValue(ident, s))
            } else {
                Err(ParseError::InvalidInput { message: format!("Expected string value for generic key-value annotation '{}', found {:?}", ident.name, value) })
            }
        }
        // Handle specific flags added for states
        Rule::initial_annotation => Ok(Annotation::Initial),
        Rule::final_annotation => Ok(Annotation::Final),
        Rule::parallel_annotation => Ok(Annotation::Parallel),
        Rule::implements_annotation => { 
             let ident = parse_identifier(inner_pair.into_inner().next().unwrap())?;
             Ok(Annotation::Implements(ident))
        }
        Rule::channel_annotation => { // Added case for $channel
             let ident = parse_identifier(inner_pair.into_inner().next().unwrap())?;
             Ok(Annotation::Channel(ident))
        }
        rule => Err(ParseError::UnexpectedRule { expected: Rule::description_annotation /* or others */, found: rule }),
    }
}

// Helper to parse annotation arguments (key: value, ...)
fn parse_annotation_args(pairs: Pairs<Rule>) -> ParseResult<Vec<Argument>> {
    let mut args = Vec::new();
     // The pairs might be wrapped in an optional annotation_args rule
     for pair in pairs { // Iterate through the inner parts, skipping the top-level rule if present
        if pair.as_rule() == Rule::annotation_args {
             for arg_pair in pair.into_inner() {
                 if arg_pair.as_rule() == Rule::annotation_arg {
                    args.push(parse_annotation_arg(arg_pair)?);
                 } else {
                      return Err(ParseError::UnexpectedRule { expected: Rule::annotation_arg, found: arg_pair.as_rule() });
                 }
            }
        } else if pair.as_rule() == Rule::annotation_arg { // Handle case where args isn't wrapped (e.g., single arg) 
             args.push(parse_annotation_arg(pair)?);
        } else { 
             // This case might occur if the annotation call is empty: $validate()
             // Or if there's an unexpected token inside the parenthesis
            eprintln!("Warning: Unexpected rule while parsing annotation args: {:?}", pair.as_rule());
            // Depending on strictness, could return error or ignore.
            // return Err(ParseError::UnexpectedRule { expected: Rule::annotation_args, found: pair.as_rule() });
         }
    }
    Ok(args)
}

// Helper to parse a single key: value argument
fn parse_annotation_arg(pair: Pair<Rule>) -> ParseResult<Argument> {
    if pair.as_rule() != Rule::annotation_arg {
        return Err(ParseError::UnexpectedRule { expected: Rule::annotation_arg, found: pair.as_rule() });
    }
    let mut inner = pair.into_inner();
    let key_pair = inner.next().ok_or(ParseError::MissingRule { expected: Rule::identifier })?;
    let value_pair = inner.next().ok_or(ParseError::MissingRule { expected: Rule::annotation_value })?;

    let key = parse_identifier(key_pair)?;
    let value = parse_annotation_value(value_pair)?;

    Ok(Argument { key, value })
}

// Helper to parse an annotation value
fn parse_annotation_value(pair: Pair<Rule>) -> ParseResult<AnnotationValue> {
    if pair.as_rule() != Rule::annotation_value {
        return Err(ParseError::UnexpectedRule { expected: Rule::annotation_value, found: pair.as_rule() });
    }
    // annotation_value is silent (_)
    let inner = pair.into_inner().next().ok_or_else(|| ParseError::InvalidInput { message: "Empty annotation_value rule".to_string() })?;
    match inner.as_rule() {
        Rule::string_literal => Ok(AnnotationValue::String(parse_string_literal(inner)?)),
        Rule::integer_literal => {
            let int_val = inner.as_str().parse::<i64>()?;
            Ok(AnnotationValue::Integer(int_val))
        }
        Rule::boolean_literal => { // Added boolean case
            let bool_val = inner.as_str().parse::<bool>().map_err(|e| ParseError::InvalidInput { message: format!("Invalid boolean value: {}", e) })?;
            Ok(AnnotationValue::Boolean(bool_val))
        }
        // TODO: Add list_literal, object_literal when grammar/AST support them
        rule => Err(ParseError::UnexpectedRule { expected: Rule::string_literal /* or others */, found: rule })
    }
}

// Helper to parse string literal and remove quotes
fn parse_string_literal(pair: Pair<Rule>) -> ParseResult<String> {
     if pair.as_rule() != Rule::string_literal {
        return Err(ParseError::UnexpectedRule { expected: Rule::string_literal, found: pair.as_rule() });
    }
    Ok(pair.as_str().trim_matches('"').to_string())
}

fn parse_type_specifier(pair: Pair<Rule>) -> ParseResult<TypeSpecifier> {
     // The input pair IS the actual rule (simple_type, list_type, optional_type) because type_specifier is silent
    let inner_pair = pair; // No need to call into_inner() for the top level
    println!("Parsing specific type_specifier: {:?} - {}", inner_pair.as_rule(), inner_pair.as_str()); // Debug print

    match inner_pair.as_rule() {
        Rule::simple_type => {
            let ident_pair = inner_pair.into_inner().next().ok_or(ParseError::MissingRule{ expected: Rule::identifier })?;
            Ok(TypeSpecifier::Simple(parse_identifier(ident_pair)?))
        }
         Rule::list_type => {
             // Inner is type_specifier
             let inner_type_pair = inner_pair.into_inner().next().ok_or(ParseError::MissingRule { expected: Rule::type_specifier })?;
             let inner_type = parse_type_specifier(inner_type_pair)?;
             Ok(TypeSpecifier::List(Box::new(inner_type)))
         }
         Rule::optional_type => {
             // Inner is type_specifier
             let inner_type_pair = inner_pair.into_inner().next().ok_or(ParseError::MissingRule { expected: Rule::type_specifier })?;
             let inner_type = parse_type_specifier(inner_type_pair)?;
             Ok(TypeSpecifier::Optional(Box::new(inner_type)))
         }
         Rule::map_type => { // Added map case
            let mut inner_pairs = inner_pair.into_inner();
            let key_type_pair = inner_pairs.next().ok_or(ParseError::MissingRule { expected: Rule::type_specifier })?;
            let value_type_pair = inner_pairs.next().ok_or(ParseError::MissingRule { expected: Rule::type_specifier })?;
            let key_type = parse_type_specifier(key_type_pair)?;
            let value_type = parse_type_specifier(value_type_pair)?;
            Ok(TypeSpecifier::Map(Box::new(key_type), Box::new(value_type)))
         }
        // TODO: Add map_type when grammar supports it
        rule => Err(ParseError::UnexpectedRule { expected: Rule::simple_type /* or list/optional */, found: rule })
    }
}

// --- History State Parsing (Added) ---

fn parse_history_definition(pair: Pair<Rule>) -> ParseResult<HistoryDefinition> {
    if pair.as_rule() != Rule::history_definition {
        return Err(ParseError::UnexpectedRule { expected: Rule::history_definition, found: pair.as_rule() });
    }
    println!("Parsing history definition: {}", pair.as_str());

    let mut inner_pairs = pair.into_inner();
    let mut history_type: Option<HistoryType> = None;
    let mut id: Option<NumericId> = None;
    let mut target: Option<Identifier> = None;

    // Order: "history", history_type, "@id(", int, ")", "target", identifier, ";"
    while let Some(p) = inner_pairs.peek() {
        match p.as_rule() {
            Rule::history_type => {
                let type_str = inner_pairs.next().unwrap().as_str();
                history_type = Some(match type_str {
                    "shallow" => HistoryType::Shallow,
                    "deep" => HistoryType::Deep,
                    _ => return Err(ParseError::InvalidInput{ message: format!("Invalid history type: {}", type_str)}),
                });
            }
            Rule::integer_literal => id = Some(parse_numeric_id(inner_pairs.next().unwrap())?),
            Rule::identifier => target = Some(parse_identifier(inner_pairs.next().unwrap())?),
            _ => { inner_pairs.next(); } // Consume keywords "history", "target", @id, etc.
        }
    }

    Ok(HistoryDefinition {
        history_type: history_type.ok_or_else(|| ParseError::InvalidInput { message: "Missing history type".to_string() })?,
        id: id.ok_or_else(|| ParseError::InvalidInput { message: "Missing history id".to_string() })?,
        default_target: target.ok_or_else(|| ParseError::InvalidInput { message: "Missing history default target".to_string() })?,
    })
}

// --- Service Block Parsing (Added) ---

fn parse_services_block(pair: Pair<Rule>) -> ParseResult<ServicesBlock> {
    if pair.as_rule() != Rule::services_block {
        return Err(ParseError::UnexpectedRule { expected: Rule::services_block, found: pair.as_rule() });
    }
    println!("Parsing services block: {}", pair.as_str());

    let mut annotations = Vec::new();
    let mut definitions = Vec::new();

    // Order: annotation*, "services", "{", service_item*, "}"
    for inner_pair in pair.into_inner() {
        match inner_pair.as_rule() {
            Rule::annotation => annotations.push(parse_annotation(inner_pair)?),
            Rule::service_item => {
                let item_pair = inner_pair.into_inner().next().unwrap();
                match item_pair.as_rule() {
                    Rule::interface_definition => definitions.push(ServiceItem::Interface(parse_interface_definition(item_pair)?)),
                    Rule::service_definition => definitions.push(ServiceItem::Service(parse_service_definition(item_pair)?)),
                    rule => return Err(ParseError::UnexpectedRule { expected: Rule::interface_definition /* or service */, found: rule })
                }
            }
            _ => { /* Skip keyword 'services', braces */ }
        }
    }

    Ok(ServicesBlock { annotations, definitions })
}

fn parse_interface_definition(pair: Pair<Rule>) -> ParseResult<InterfaceDefinition> {
    if pair.as_rule() != Rule::interface_definition {
        return Err(ParseError::UnexpectedRule { expected: Rule::interface_definition, found: pair.as_rule() });
    }
    println!("Parsing interface definition: {}", pair.as_str());

    let mut annotations = Vec::new();
    let mut name: Option<Identifier> = None;
    let mut id: Option<NumericId> = None;
    let mut methods = Vec::new();

    // Order: annotation*, "interface", identifier, "@id(", int, ")", "{", method_definition*, "}"
    for inner_pair in pair.into_inner() {
        match inner_pair.as_rule() {
            Rule::annotation => annotations.push(parse_annotation(inner_pair)?),
            Rule::identifier => name = Some(parse_identifier(inner_pair)?),
            Rule::integer_literal => id = Some(parse_numeric_id(inner_pair)?),
            Rule::method_definition => methods.push(parse_method_definition(inner_pair)?),
            _ => { /* Skip keywords, braces, @id */ }
        }
    }

    Ok(InterfaceDefinition {
        name: name.ok_or_else(|| ParseError::InvalidInput { message: "Missing interface name".to_string() })?,
        id: id.ok_or_else(|| ParseError::InvalidInput { message: "Missing interface id".to_string() })?,
        annotations,
        methods,
    })
}

fn parse_method_definition(pair: Pair<Rule>) -> ParseResult<MethodDefinition> {
    if pair.as_rule() != Rule::method_definition {
        return Err(ParseError::UnexpectedRule { expected: Rule::method_definition, found: pair.as_rule() });
    }
     println!("Parsing method definition: {}", pair.as_str());

    let mut annotations = Vec::new();
    let mut name: Option<Identifier> = None;
    let mut id: Option<NumericId> = None;
    let mut parameters = Vec::new();
    let mut return_type: Option<TypeSpecifier> = None;
    let mut body_annotations = Vec::new();

    // Order: annotation*, identifier, "@id(", int, ")", "(", param_list?, ")", return?, body?, ";"
    for inner_pair in pair.into_inner() {
        match inner_pair.as_rule() {
            Rule::annotation => annotations.push(parse_annotation(inner_pair)?),
            Rule::identifier => name = Some(parse_identifier(inner_pair)?),
            Rule::integer_literal => id = Some(parse_numeric_id(inner_pair)?),
            Rule::parameter_list => {
                for param_pair in inner_pair.into_inner() {
                     if param_pair.as_rule() == Rule::parameter {
                        parameters.push(parse_parameter(param_pair)?);
                     }
                }
            }
            Rule::method_return => {
                let type_pair = inner_pair.into_inner().next().unwrap(); // Skip "->"
                 return_type = Some(parse_type_specifier(type_pair)?);
            }
            Rule::method_body => {
                 for body_item in inner_pair.into_inner() {
                    if body_item.as_rule() == Rule::annotation {
                         body_annotations.push(parse_annotation(body_item)?);
                    }
                 }
            }
             _ => { /* Skip keywords, parens, braces, @id, etc. */ }
        }
    }

     Ok(MethodDefinition {
        name: name.ok_or_else(|| ParseError::InvalidInput { message: "Missing method name".to_string() })?,
        id: id.ok_or_else(|| ParseError::InvalidInput { message: "Missing method id".to_string() })?,
        annotations,
        parameters,
        return_type,
        body_annotations,
    })
}

fn parse_parameter(pair: Pair<Rule>) -> ParseResult<ParameterDefinition> {
     if pair.as_rule() != Rule::parameter {
        return Err(ParseError::UnexpectedRule { expected: Rule::parameter, found: pair.as_rule() });
    }
     println!("Parsing parameter: {}", pair.as_str());

     let mut name: Option<Identifier> = None;
     let mut type_spec: Option<TypeSpecifier> = None;
     let mut id: Option<NumericId> = None;
     // TODO: Parse annotations on parameters if grammar allows

     // Order: identifier, ":", type_specifier, ("@id(", int, ")")?
     for inner_pair in pair.into_inner() {
         match inner_pair.as_rule() {
            Rule::identifier => name = Some(parse_identifier(inner_pair)?),
            Rule::type_specifier => type_spec = Some(parse_type_specifier(inner_pair)?),
            Rule::integer_literal => id = Some(parse_numeric_id(inner_pair)?),
            _ => { /* Skip : @id() */ }
         }
     }

     Ok(ParameterDefinition {
         name: name.ok_or_else(|| ParseError::InvalidInput { message: "Missing parameter name".to_string() })?,
         type_spec: type_spec.ok_or_else(|| ParseError::InvalidInput { message: "Missing parameter type".to_string() })?,
         id,
         annotations: Vec::new(), // TODO
     })
}

fn parse_service_definition(pair: Pair<Rule>) -> ParseResult<ServiceDefinition> {
     if pair.as_rule() != Rule::service_definition {
        return Err(ParseError::UnexpectedRule { expected: Rule::service_definition, found: pair.as_rule() });
    }
     println!("Parsing service definition: {}", pair.as_str());

    let mut annotations = Vec::new();
    let mut name: Option<Identifier> = None;
    let mut id: Option<NumericId> = None;
    let mut extends: Option<Identifier> = None;

    // Order: annotation*, "service", identifier, "@id(", int, ")", extends?, body
     for inner_pair in pair.into_inner() {
         match inner_pair.as_rule() {
            Rule::annotation => annotations.push(parse_annotation(inner_pair)?),
            Rule::identifier => name = Some(parse_identifier(inner_pair)?),
            Rule::integer_literal => id = Some(parse_numeric_id(inner_pair)?),
            Rule::service_extends => {
                extends = Some(parse_identifier(inner_pair.into_inner().next().unwrap())?); // Skip "extends"
            }
            Rule::service_body => {
                 // Body only contains annotations for now
                 for body_item in inner_pair.into_inner() {
                     if body_item.as_rule() == Rule::annotation {
                         annotations.push(parse_annotation(body_item)?);
                     } else if body_item.as_rule() != Rule::service_element { // service_element is just annotation now
                        eprintln!("Warning: Skipping unexpected element in service body: {:?}", body_item.as_rule());
                     }
                 }
            }
             _ => { /* Skip keywords, @id, braces etc. */ }
         }
     }

     Ok(ServiceDefinition {
        name: name.ok_or_else(|| ParseError::InvalidInput { message: "Missing service name".to_string() })?,
        id: id.ok_or_else(|| ParseError::InvalidInput { message: "Missing service id".to_string() })?,
        annotations, // Note: $implements etc. are parsed into here
        extends,
    })
}

// --- Communication Block Parsing (Added) ---

fn parse_communication_block(pair: Pair<Rule>) -> ParseResult<CommunicationBlock> {
    if pair.as_rule() != Rule::communication_block {
        return Err(ParseError::UnexpectedRule { expected: Rule::communication_block, found: pair.as_rule() });
    }
    println!("Parsing communication block: {}", pair.as_str());

    let mut annotations = Vec::new();
    let mut definitions = Vec::new();

    // Order: annotation*, "communication", "{", communication_item*, "}"
    for inner_pair in pair.into_inner() {
        match inner_pair.as_rule() {
            Rule::annotation => annotations.push(parse_annotation(inner_pair)?),
            Rule::communication_item => {
                let item_pair = inner_pair.into_inner().next().unwrap();
                match item_pair.as_rule() {
                    Rule::protocol_definition => definitions.push(CommunicationItem::Protocol(parse_protocol_definition(item_pair)?)),
                    Rule::channel_definition => definitions.push(CommunicationItem::Channel(parse_channel_definition(item_pair)?)),
                    Rule::event_definition => definitions.push(CommunicationItem::Event(parse_event_definition(item_pair)?)),
                    rule => return Err(ParseError::UnexpectedRule { expected: Rule::protocol_definition /* or others */, found: rule })
                }
            }
            _ => { /* Skip keyword 'communication', braces */ }
        }
    }
    Ok(CommunicationBlock { annotations, definitions })
}

fn parse_protocol_definition(pair: Pair<Rule>) -> ParseResult<ProtocolDefinition> {
    if pair.as_rule() != Rule::protocol_definition {
        return Err(ParseError::UnexpectedRule { expected: Rule::protocol_definition, found: pair.as_rule() });
    }
    println!("Parsing protocol definition: {}", pair.as_str());

    let mut annotations = Vec::new();
    let mut name: Option<Identifier> = None;
    let mut id: Option<NumericId> = None;

    // Order: annotation*, "protocol", identifier, "@id(", int, ")", body, ";"
    for inner_pair in pair.into_inner() {
        match inner_pair.as_rule() {
            Rule::annotation => annotations.push(parse_annotation(inner_pair)?),
            Rule::identifier => name = Some(parse_identifier(inner_pair)?),
            Rule::integer_literal => id = Some(parse_numeric_id(inner_pair)?),
            Rule::protocol_body => {
                 // Body only contains annotations for now
                 for body_item in inner_pair.into_inner() {
                     if body_item.as_rule() == Rule::annotation {
                         annotations.push(parse_annotation(body_item)?);
                     }
                 }
            }
             _ => { /* Skip keywords, @id, braces etc. */ }
        }
    }

    Ok(ProtocolDefinition {
        name: name.ok_or_else(|| ParseError::InvalidInput { message: "Missing protocol name".to_string() })?,
        id: id.ok_or_else(|| ParseError::InvalidInput { message: "Missing protocol id".to_string() })?,
        annotations,
    })
}

fn parse_channel_definition(pair: Pair<Rule>) -> ParseResult<ChannelDefinition> {
    if pair.as_rule() != Rule::channel_definition {
        return Err(ParseError::UnexpectedRule { expected: Rule::channel_definition, found: pair.as_rule() });
    }
    println!("Parsing channel definition: {}", pair.as_str());

    let mut annotations = Vec::new();
    let mut name: Option<Identifier> = None;
    let mut id: Option<NumericId> = None;
    let mut description: Option<String> = None;
    let mut parameters: Option<Vec<Argument>> = None;

    // Order: annotation*, "channel", identifier, "@id(", int, ")", body, ";"
    for inner_pair in pair.into_inner() {
        match inner_pair.as_rule() {
            Rule::annotation => annotations.push(parse_annotation(inner_pair)?),
            Rule::identifier => name = Some(parse_identifier(inner_pair)?),
            Rule::integer_literal => id = Some(parse_numeric_id(inner_pair)?),
            Rule::channel_body => {
                 for element in inner_pair.into_inner() {
                    match element.as_rule() {
                        Rule::description_element => {
                            let desc_val = element.into_inner().find(|p| p.as_rule() == Rule::string_literal).unwrap();
                            description = Some(parse_string_literal(desc_val)?);
                        }
                        Rule::parameters_element => {
                            let params_obj = element.into_inner().find(|p| p.as_rule() == Rule::object_literal).unwrap();
                            // Reuse annotation args parser for { key: value } structure
                            // Note: Value is currently parsed as String/Int, not type identifier
                            parameters = Some(parse_annotation_args(params_obj.into_inner())?);
                        }
                        Rule::annotation => annotations.push(parse_annotation(element)?),
                         _ => { /* Skip braces etc. */ }
                    }
                 }
            }
             _ => { /* Skip keywords, @id, etc. */ }
        }
    }

    Ok(ChannelDefinition {
        name: name.ok_or_else(|| ParseError::InvalidInput { message: "Missing channel name".to_string() })?,
        id: id.ok_or_else(|| ParseError::InvalidInput { message: "Missing channel id".to_string() })?,
        annotations,
        description,
        parameters,
    })
}

fn parse_event_definition(pair: Pair<Rule>) -> ParseResult<EventDefinition> {
    if pair.as_rule() != Rule::event_definition {
        return Err(ParseError::UnexpectedRule { expected: Rule::event_definition, found: pair.as_rule() });
    }
    println!("Parsing event definition: {}", pair.as_str());

    let mut annotations = Vec::new();
    let mut name: Option<Identifier> = None;
    let mut id: Option<NumericId> = None;
    let mut fields = Vec::new();

    // Order: annotation*, "event", identifier, "@id(", int, ")", "{", event_field_definition*, "}"
    for inner_pair in pair.into_inner() {
        match inner_pair.as_rule() {
            Rule::annotation => annotations.push(parse_annotation(inner_pair)?),
            Rule::identifier => name = Some(parse_identifier(inner_pair)?),
            Rule::integer_literal => id = Some(parse_numeric_id(inner_pair)?),
            Rule::event_field_definition => {
                // event_field_definition just wraps field_definition
                let field_pair = inner_pair.into_inner().next().unwrap();
                fields.push(parse_field_definition(field_pair)?);
            }
            _ => { /* Skip keywords, braces, @id etc. */ }
        }
    }

    Ok(EventDefinition {
        name: name.ok_or_else(|| ParseError::InvalidInput { message: "Missing event name".to_string() })?,
        id: id.ok_or_else(|| ParseError::InvalidInput { message: "Missing event id".to_string() })?,
        annotations, // Includes $channel
        fields,
    })
}

// --- Actor Block Parsing (Added) ---

fn parse_actors_block(pair: Pair<Rule>) -> ParseResult<ActorsBlock> {
    if pair.as_rule() != Rule::actors_block {
        return Err(ParseError::UnexpectedRule { expected: Rule::actors_block, found: pair.as_rule() });
    }
    println!("Parsing actors block: {}", pair.as_str());

    let mut annotations = Vec::new();
    let mut definitions = Vec::new();

    // Order: annotation*, "actors", "{", actor_definition*, "}"
    for inner_pair in pair.into_inner() {
        match inner_pair.as_rule() {
            Rule::annotation => annotations.push(parse_annotation(inner_pair)?),
            Rule::actor_definition => definitions.push(parse_actor_definition(inner_pair)?),
            _ => { /* Skip keyword 'actors', braces */ }
        }
    }

    Ok(ActorsBlock { annotations, definitions })
}

fn parse_actor_definition(pair: Pair<Rule>) -> ParseResult<ActorDefinition> {
     if pair.as_rule() != Rule::actor_definition {
        return Err(ParseError::UnexpectedRule { expected: Rule::actor_definition, found: pair.as_rule() });
    }
    println!("Parsing actor definition: {}", pair.as_str());

    let mut annotations = Vec::new();
    let mut name: Option<Identifier> = None;
    let mut id: Option<NumericId> = None;

     // Order: annotation*, "actor", identifier, "@id(", int, ")", body, ";"
    for inner_pair in pair.into_inner() {
        match inner_pair.as_rule() {
            Rule::annotation => annotations.push(parse_annotation(inner_pair)?),
            Rule::identifier => name = Some(parse_identifier(inner_pair)?),
            Rule::integer_literal => id = Some(parse_numeric_id(inner_pair)?),
            Rule::actor_body => {
                 // Body only contains annotations for now
                 for body_item in inner_pair.into_inner() {
                     if body_item.as_rule() == Rule::annotation {
                         annotations.push(parse_annotation(body_item)?);
                     }
                 }
            }
             _ => { /* Skip keywords, @id, braces etc. */ }
        }
    }

     Ok(ActorDefinition {
        name: name.ok_or_else(|| ParseError::InvalidInput { message: "Missing actor name".to_string() })?,
        id: id.ok_or_else(|| ParseError::InvalidInput { message: "Missing actor id".to_string() })?,
        annotations,
    })
}

// --- Deployment Config Block Parsing (Added) ---

fn parse_deployment_config_block(pair: Pair<Rule>) -> ParseResult<DeploymentConfigBlock> {
    if pair.as_rule() != Rule::deployment_config_block {
        return Err(ParseError::UnexpectedRule { expected: Rule::deployment_config_block, found: pair.as_rule() });
    }
    println!("Parsing deployment_config block: {}", pair.as_str());

    let mut annotations = Vec::new();
    let mut definitions = Vec::new();

    // Order: annotation*, "deployment_config", "{", deployment_item*, "}"
    for inner_pair in pair.into_inner() {
        match inner_pair.as_rule() {
            Rule::annotation => annotations.push(parse_annotation(inner_pair)?),
            Rule::deployment_item => {
                let item_pair = inner_pair.into_inner().next().unwrap();
                match item_pair.as_rule() {
                    Rule::environment_definition => definitions.push(DeploymentItem::Environment(parse_environment_definition(item_pair)?)),
                    Rule::infrastructure_definition => definitions.push(DeploymentItem::Infrastructure(parse_infrastructure_definition(item_pair)?)),
                    Rule::deployment_definition => definitions.push(DeploymentItem::Deployment(parse_deployment_definition(item_pair)?)),
                    rule => return Err(ParseError::UnexpectedRule { expected: Rule::environment_definition /* or others */, found: rule })
                }
            }
            _ => { /* Skip keyword, braces */ }
        }
    }
    Ok(DeploymentConfigBlock { annotations, definitions })
}

fn parse_environment_definition(pair: Pair<Rule>) -> ParseResult<EnvironmentDefinition> {
    if pair.as_rule() != Rule::environment_definition {
        return Err(ParseError::UnexpectedRule { expected: Rule::environment_definition, found: pair.as_rule() });
    }
     println!("Parsing environment definition: {}", pair.as_str());

    let mut annotations = Vec::new();
    let mut name: Option<Identifier> = None;
    let mut id: Option<NumericId> = None;
    let mut extends: Option<Identifier> = None;
    let mut variables: Option<Vec<Argument>> = None;

     // Order: annotation*, "environment", identifier, "@id(", int, ")", extends?, body, ";"
     for inner_pair in pair.into_inner() {
         match inner_pair.as_rule() {
            Rule::annotation => annotations.push(parse_annotation(inner_pair)?),
            Rule::identifier => name = Some(parse_identifier(inner_pair)?),
            Rule::integer_literal => id = Some(parse_numeric_id(inner_pair)?),
            Rule::environment_extends => {
                extends = Some(parse_identifier(inner_pair.into_inner().next().unwrap())?);
            }
            Rule::environment_body => {
                 for element in inner_pair.into_inner() {
                    match element.as_rule() {
                        Rule::variables_element => {
                            let vars_obj = element.into_inner().find(|p| p.as_rule() == Rule::object_literal).unwrap();
                            variables = Some(parse_annotation_args(vars_obj.into_inner())?);
                        }
                        Rule::annotation => annotations.push(parse_annotation(element)?),
                         _ => { /* Skip braces etc. */ }
                    }
                 }
            }
             _ => { /* Skip keywords, @id, etc. */ }
         }
     }

     Ok(EnvironmentDefinition {
        name: name.ok_or_else(|| ParseError::InvalidInput { message: "Missing environment name".to_string() })?,
        id: id.ok_or_else(|| ParseError::InvalidInput { message: "Missing environment id".to_string() })?,
        annotations,
        extends,
        variables,
    })
}

fn parse_infrastructure_definition(pair: Pair<Rule>) -> ParseResult<InfrastructureDefinition> {
    if pair.as_rule() != Rule::infrastructure_definition {
        return Err(ParseError::UnexpectedRule { expected: Rule::infrastructure_definition, found: pair.as_rule() });
    }
    println!("Parsing infrastructure definition: {}", pair.as_str());

    let mut annotations = Vec::new();
    let mut name: Option<Identifier> = None;
    let mut id: Option<NumericId> = None;
    let mut extends: Option<Identifier> = None;
    let mut attributes = Vec::new();

    // Order: annotation*, "infrastructure", identifier, "@id(", int, ")", extends?, body, ";"
    for inner_pair in pair.into_inner() {
         match inner_pair.as_rule() {
            Rule::annotation => annotations.push(parse_annotation(inner_pair)?),
            Rule::identifier => name = Some(parse_identifier(inner_pair)?),
            Rule::integer_literal => id = Some(parse_numeric_id(inner_pair)?),
            Rule::infrastructure_extends => {
                extends = Some(parse_identifier(inner_pair.into_inner().next().unwrap())?);
            }
            Rule::infrastructure_body => {
                 for element in inner_pair.into_inner() {
                    match element.as_rule() {
                         Rule::attribute_kv_pair => attributes.push(parse_attribute_kv_pair(element)?),
                         Rule::annotation => annotations.push(parse_annotation(element)?),
                         _ => { /* Skip braces etc. */ }
                    }
                 }
            }
             _ => { /* Skip keywords, @id, etc. */ }
         }
     }

     Ok(InfrastructureDefinition {
        name: name.ok_or_else(|| ParseError::InvalidInput { message: "Missing infrastructure name".to_string() })?,
        id: id.ok_or_else(|| ParseError::InvalidInput { message: "Missing infrastructure id".to_string() })?,
        annotations,
        extends,
        attributes,
    })
}

fn parse_attribute_kv_pair(pair: Pair<Rule>) -> ParseResult<AttributeDefinition> {
     if pair.as_rule() != Rule::attribute_kv_pair {
        return Err(ParseError::UnexpectedRule { expected: Rule::attribute_kv_pair, found: pair.as_rule() });
    }
    let mut inner = pair.into_inner();
    let key = parse_identifier(inner.next().unwrap())?;
    let value = parse_annotation_value(inner.next().unwrap())?;
    Ok(AttributeDefinition { key, value })
}

fn parse_deployment_definition(pair: Pair<Rule>) -> ParseResult<DeploymentDefinition> {
    if pair.as_rule() != Rule::deployment_definition {
        return Err(ParseError::UnexpectedRule { expected: Rule::deployment_definition, found: pair.as_rule() });
    }
    println!("Parsing deployment definition: {}", pair.as_str());

    let mut annotations = Vec::new();
    let mut name: Option<Identifier> = None;
    let mut id: Option<NumericId> = None;
    let mut target_environment: Option<Identifier> = None;
    let mut target_infrastructure: Option<Vec<Argument>> = None;
    let mut deployable: Option<Identifier> = None;
    let mut config: Option<Vec<Argument>> = None;
    let mut other_attributes = Vec::new();

     // Order: annotation*, "deployment", identifier, "@id(", int, ")", body, ";"
     for inner_pair in pair.into_inner() {
         match inner_pair.as_rule() {
            Rule::annotation => annotations.push(parse_annotation(inner_pair)?),
            Rule::identifier => name = Some(parse_identifier(inner_pair)?),
            Rule::integer_literal => id = Some(parse_numeric_id(inner_pair)?),
            Rule::deployment_body => {
                 for element in inner_pair.into_inner() {
                    match element.as_rule() {
                         Rule::target_env_element => {
                             target_environment = Some(parse_identifier(element.into_inner().find(|p| p.as_rule() == Rule::identifier).unwrap())?);
                         }
                         Rule::target_infra_element => {
                             let infra_obj = element.into_inner().find(|p| p.as_rule() == Rule::object_literal).unwrap();
                             target_infrastructure = Some(parse_annotation_args(infra_obj.into_inner())?);
                         }
                         Rule::deployable_element => {
                             deployable = Some(parse_identifier(element.into_inner().find(|p| p.as_rule() == Rule::identifier).unwrap())?);
                         }
                         Rule::config_element => {
                             let config_obj = element.into_inner().find(|p| p.as_rule() == Rule::object_literal).unwrap();
                             config = Some(parse_annotation_args(config_obj.into_inner())?);
                         }
                         Rule::attribute_kv_pair => other_attributes.push(parse_attribute_kv_pair(element)?),
                         Rule::annotation => annotations.push(parse_annotation(element)?),
                         _ => { /* Skip braces etc. */ }
                    }
                 }
            }
             _ => { /* Skip keywords, @id, etc. */ }
         }
     }

     Ok(DeploymentDefinition {
        name: name.ok_or_else(|| ParseError::InvalidInput { message: "Missing deployment name".to_string() })?,
        id: id.ok_or_else(|| ParseError::InvalidInput { message: "Missing deployment id".to_string() })?,
        annotations,
        target_environment,
        target_infrastructure,
        deployable,
        config,
        other_attributes,
    })
}

// --- Public API ---

/// Reads and parses an SSOT file from the given path.
pub fn parse_ssot_file<P: AsRef<Path>>(path: P) -> ParseResult<SsotAst> {
    let path = path.as_ref();
    let content = std::fs::read_to_string(path).map_err(|e| ParseError::FileReadError {
        path: path.to_path_buf(),
        source: e,
    })?;
    parse_ssot_content(&content, Some(path.to_path_buf()))
}

#[cfg(test)]
mod tests {
    use super::*;
    
    use crate::ast::{TypeDefinition, Annotation, TypeSpecifier, InvokeSource, Duration, TimeUnit, HistoryType, ServiceItem, CommunicationItem, ActorsBlock, ActorDefinition, DeploymentItem, DeploymentConfigBlock}; // Ensure needed types are imported
    use pretty_assertions::assert_eq;

    // Helper to create simple Identifier
    fn ident(name: &str) -> Identifier {
        Identifier { name: name.to_string() }
    }

    // Helper to create simple NumericId
    fn num_id(value: u64) -> NumericId {
        NumericId { value }
    }

    #[test]
    fn test_parse_basic_file_structure() {
        let content = r#"
            file_id: 0x1234abcd5678ef90; // Corrected syntax
            import "/path/to/common.ssot";
            // Add empty blocks to ensure they parse okay at top level
            types {}
            machines {}
        "#;
        let result = parse_ssot_content(content, None);
        assert!(result.is_ok(), "Basic parsing failed: {:?}", result.err()); // Add message
        let ast = result.unwrap();

        assert_eq!(ast.file_id, Some(FileId { value: 0x1234abcd5678ef90 }));
        assert_eq!(ast.imports.len(), 1);
        assert_eq!(ast.imports[0], ImportStatement { path: "/path/to/common.ssot".to_string() });
        // Check that definitions for the empty blocks were NOT created
        assert!(ast.definitions.is_empty(), "Definitions should be empty for empty blocks");
    }

    #[test]
    fn test_parse_simple_types_block() {
        let content = r#"
            $description("Simple types");
            types {
                struct SimpleStruct @id(1) {
                    field_a: string @id(10);
                    field_b: map<string, i32> @id(11); // Added map type field
                }
                enum SimpleEnum @id(2) {
                    VARIANT_A @id(20);
                }
            }
        "#;
        let result = parse_ssot_content(content, None);
        println!("Types Block Parse Result: {:?}", result); // Debug print
        if let Err(e) = &result {
            if let ParseError::PestError(pe) = e {
                eprintln!("Pest Error Details:\n{}", pe);
            }
        }

        assert!(result.is_ok(), "Parsing failed: {:?}", result.err());

        let ast = result.unwrap();
        assert_eq!(ast.definitions.len(), 1, "Expected one top-level definition (types block)");

        let types_def = match &ast.definitions[0] {
            TopLevelDefinition::Types(block) => block,
            _ => panic!("Expected TypesBlock"),
        };

        assert_eq!(types_def.annotations.len(), 1, "Expected one annotation on the types block");
        // Add more specific assertions about the parsed content if needed
         assert_eq!(types_def.definitions.len(), 2, "Expected two type definitions (struct and enum)");

         // Assert Struct
         match &types_def.definitions[0] {
            TypeDefinition::Struct(s) => {
                assert_eq!(s.name.name, "SimpleStruct");
                assert_eq!(s.id.value, 1);
                assert_eq!(s.fields.len(), 2);
                assert_eq!(s.fields[0].name.name, "field_a");
                assert_eq!(s.fields[0].id.value, 10);
                 if let TypeSpecifier::Simple(ts) = &s.fields[0].type_spec {
                      assert_eq!(ts.name, "string");
                 } else {
                     panic!("Expected simple type specifier for field_a");
                 }
                 // Check field_b (map type)
                 assert_eq!(s.fields[1].name.name, "field_b");
                 assert_eq!(s.fields[1].id.value, 11);
                 if let TypeSpecifier::Map(key_type, value_type) = &s.fields[1].type_spec {
                     if let TypeSpecifier::Simple(k) = &**key_type {
                         assert_eq!(k.name, "string");
                     } else { panic!("Expected simple key type in map"); }
                     if let TypeSpecifier::Simple(v) = &**value_type {
                          assert_eq!(v.name, "i32");
                     } else { panic!("Expected simple value type in map"); }
                 } else {
                      panic!("Expected map type specifier for field_b");
                 }
            },
            _ => panic!("Expected StructDefinition")
         }

          // Assert Enum
         match &types_def.definitions[1] {
             TypeDefinition::Enum(e) => {
                 assert_eq!(e.name.name, "SimpleEnum");
                 assert_eq!(e.id.value, 2);
                 assert_eq!(e.variants.len(), 1);
                 assert_eq!(e.variants[0].name.name, "VARIANT_A");
                 assert_eq!(e.variants[0].id.value, 20);
             },
             _ => panic!("Expected EnumDefinition")
         }
    }

    #[test]
    fn test_parse_simple_machines_block() {
        let content = r#"
            machines {
                machine SimpleMachine @id(0) {
                    $description("A basic machine");
                    context @id(0) {
                        counter: u32 @id(0) { $default(0); };
                        userId: string @id(1);
                    }
                    actions @id(1) {
                        increment @id(0);
                        logEntry @id(1);
                    }
                    guards @id(2) {
                        isZero @id(0);
                    }
                    invokes @id(3) {
                        invoke fetchUser @id(0) { src: UserService.fetchProfile; };
                        invoke backgroundTask @id(1) { 
                            src: "someTask";
                            onDone: Done;
                            onError: Failed;
                        };
                    }
                    states @id(4) {
                        $initial;
                        state Idle @id(0) {
                            on INCREMENT @id(0) transition Processing { action: increment; };
                        }
                        state Loading @id(1) {
                           invoke LoadData @id(0) {
                                src: invokes.fetchUser;
                                input: { id: "ctx.userId" };
                                onDone: Idle;
                                onError: Failed;
                           };
                        }
                        state Processing @id(2) {
                            $final;
                            on COMPLETE @id(0) transition Idle { guard: isZero; action: logEntry; };
                        }
                         state Failed @id(3) { }
                         state Done @id(4) { }
                    }
                }
            }
        "#;
        let result = parse_ssot_content(content, None);
        println!("Parse result: {:?}", result);
        if let Err(e) = &result {
            if let ParseError::PestError(pe) = e {
                eprintln!("Pest Error Details:\n{}", pe);
            }
        }
        assert!(result.is_ok());
        let ast = result.unwrap();

        assert_eq!(ast.definitions.len(), 1);
        match &ast.definitions[0] {
            TopLevelDefinition::Machines(machines_block) => {
                 assert_eq!(machines_block.definitions.len(), 1);
                 let machine = &machines_block.definitions[0];
                 assert_eq!(machine.name.name, "SimpleMachine");
                 assert!(machine.context.is_some());
                 assert!(machine.actions.is_some());
                 assert!(machine.guards.is_some());
                 assert!(machine.invokes.is_some());
                 assert!(machine.states.is_some());

                 let invokes = machine.invokes.as_ref().unwrap();
                 assert_eq!(invokes.definitions.len(), 2);
                 assert_eq!(invokes.definitions[0].name.name, "fetchUser");
                 match &invokes.definitions[0].src {
                     InvokeSource::ServiceMethod(s, m) => {
                         assert_eq!(s.name, "UserService");
                         assert_eq!(m.name, "fetchProfile");
                     }
                     _ => panic!("Expected ServiceMethod source")
                 }
                  assert_eq!(invokes.definitions[1].name.name, "backgroundTask");
                 match &invokes.definitions[1].src {
                     InvokeSource::Literal(s) => assert_eq!(s, "someTask"),
                     _ => panic!("Expected Literal source")
                 }
                 assert!(invokes.definitions[1].on_done.is_some());
                 assert_eq!(invokes.definitions[1].on_done.as_ref().unwrap().target_state.name, "Done");
                 assert!(invokes.definitions[1].on_error.is_some());
                 assert_eq!(invokes.definitions[1].on_error.as_ref().unwrap().target_state.name, "Failed");

                 let states_block = machine.states.as_ref().unwrap();
                 assert_eq!(states_block.states.len(), 5);

                 let idle_state = &states_block.states[0];
                 assert!(idle_state.invokes.is_empty());

                  let loading_state = &states_block.states[1];
                  assert!(!loading_state.is_initial);
                  assert!(!loading_state.is_final);
                  assert_eq!(loading_state.transitions.len(), 0);
                  assert_eq!(loading_state.invokes.len(), 1);
                  let state_invoke = &loading_state.invokes[0];
                  assert_eq!(state_invoke.name.name, "LoadData");
                  assert_eq!(state_invoke.src_ref.name, "fetchUser");
                  assert!(state_invoke.input_mapping.is_some());
                  assert_eq!(state_invoke.input_mapping.as_ref().unwrap().len(), 1);
                  assert_eq!(state_invoke.input_mapping.as_ref().unwrap()[0].key.name, "id");
                  if let AnnotationValue::String(s) = &state_invoke.input_mapping.as_ref().unwrap()[0].value {
                      assert_eq!(s, "ctx.userId");
                  } else {
                       panic!("Expected string value for input mapping");
                  }
                  assert!(state_invoke.on_done.is_some());
                  assert_eq!(state_invoke.on_done.as_ref().unwrap().target_state.name, "Idle");
                  assert!(state_invoke.on_error.is_some());
                  assert_eq!(state_invoke.on_error.as_ref().unwrap().target_state.name, "Failed");

                  let processing_state = &states_block.states[2];
                   assert!(processing_state.is_final);

                  assert!(states_block.states.iter().any(|s| s.name.name == "Failed"));
                  assert!(states_block.states.iter().any(|s| s.name.name == "Done"));
            }
            _ => panic!("Expected Machines block"),
        }
    }

    #[test]
    fn test_parse_error_handling() {
        // Example of invalid syntax (missing semicolon)
        let content = r#"
            @0xbbbbbbbbbbbbbbbb;
            types {
                struct Invalid @id(0) {
                    field: string @id(0)
                }
            }
        "#;
        let result = parse_ssot_content(content, None);
        assert!(result.is_err());
        // More specific error checking could be added here if needed
        // e.g., assert!(matches!(result.err().unwrap(), ParseError::PestError(_)));
    }

    #[test]
    fn test_parse_parallel_state() {
        let content = r#"
            machines {
                machine ParallelMachine @id(0) {
                    $description("A machine with parallel states");
                    context @id(0) {
                        counter: u32 @id(0) { $default(0); };
                        userId: string @id(1);
                    }
                    actions @id(1) {
                        increment @id(0);
                        logEntry @id(1);
                    }
                    guards @id(2) {
                        isZero @id(0);
                    }
                    invokes @id(3) {
                        invoke fetchUser @id(0) { src: UserService.fetchProfile; };
                        invoke backgroundTask @id(1) { 
                            src: "someTask";
                            onDone: Done;
                            onError: Failed;
                        };
                    }
                    states @id(4) {
                        $initial;
                        state Idle @id(0) {
                            on INCREMENT @id(0) transition Processing { action: increment; };
                        }
                        state Loading @id(1) {
                           invoke LoadData @id(0) {
                                src: invokes.fetchUser;
                                input: { id: "ctx.userId" };
                                onDone: Idle;
                                onError: Failed;
                           };
                        }
                        state Processing @id(2) {
                            $final;
                            on COMPLETE @id(0) transition Idle { guard: isZero; action: logEntry; };
                        }
                         state Failed @id(3) { }
                         state Done @id(4) { }
                    }
                }
            }
        "#;
        let result = parse_ssot_content(content, None);
        println!("Parse result: {:?}", result);
        if let Err(e) = &result {
            if let ParseError::PestError(pe) = e {
                eprintln!("Pest Error Details:\n{}", pe);
            }
        }
        assert!(result.is_ok());
        let ast = result.unwrap();

        assert_eq!(ast.definitions.len(), 1);
        match &ast.definitions[0] {
            TopLevelDefinition::Machines(machines_block) => {
                 assert_eq!(machines_block.definitions.len(), 1);
                 let machine = &machines_block.definitions[0];
                 assert_eq!(machine.name.name, "ParallelMachine");
                 assert!(machine.context.is_some());
                 assert!(machine.actions.is_some());
                 assert!(machine.guards.is_some());
                 assert!(machine.invokes.is_some());
                 assert!(machine.states.is_some());

                 let invokes = machine.invokes.as_ref().unwrap();
                 assert_eq!(invokes.definitions.len(), 2);
                 assert_eq!(invokes.definitions[0].name.name, "fetchUser");
                 match &invokes.definitions[0].src {
                     InvokeSource::ServiceMethod(s, m) => {
                         assert_eq!(s.name, "UserService");
                         assert_eq!(m.name, "fetchProfile");
                     }
                     _ => panic!("Expected ServiceMethod source")
                 }
                  assert_eq!(invokes.definitions[1].name.name, "backgroundTask");
                 match &invokes.definitions[1].src {
                     InvokeSource::Literal(s) => assert_eq!(s, "someTask"),
                     _ => panic!("Expected Literal source")
                 }
                 assert!(invokes.definitions[1].on_done.is_some());
                 assert_eq!(invokes.definitions[1].on_done.as_ref().unwrap().target_state.name, "Done");
                 assert!(invokes.definitions[1].on_error.is_some());
                 assert_eq!(invokes.definitions[1].on_error.as_ref().unwrap().target_state.name, "Failed");

                 let states_block = machine.states.as_ref().unwrap();
                 assert_eq!(states_block.states.len(), 5);

                 let idle_state = &states_block.states[0];
                 assert!(idle_state.invokes.is_empty());

                  let loading_state = &states_block.states[1];
                  assert!(!loading_state.is_initial);
                  assert!(!loading_state.is_final);
                  assert_eq!(loading_state.transitions.len(), 0);
                  assert_eq!(loading_state.invokes.len(), 1);
                  let state_invoke = &loading_state.invokes[0];
                  assert_eq!(state_invoke.name.name, "LoadData");
                  assert_eq!(state_invoke.src_ref.name, "fetchUser");
                  assert!(state_invoke.input_mapping.is_some());
                  assert_eq!(state_invoke.input_mapping.as_ref().unwrap().len(), 1);
                  assert_eq!(state_invoke.input_mapping.as_ref().unwrap()[0].key.name, "id");
                  if let AnnotationValue::String(s) = &state_invoke.input_mapping.as_ref().unwrap()[0].value {
                      assert_eq!(s, "ctx.userId");
                  } else {
                       panic!("Expected string value for input mapping");
                  }
                  assert!(state_invoke.on_done.is_some());
                  assert_eq!(state_invoke.on_done.as_ref().unwrap().target_state.name, "Idle");
                  assert!(state_invoke.on_error.is_some());
                  assert_eq!(state_invoke.on_error.as_ref().unwrap().target_state.name, "Failed");

                  let processing_state = &states_block.states[2];
                   assert!(processing_state.is_final);

                  assert!(states_block.states.iter().any(|s| s.name.name == "Failed"));
                  assert!(states_block.states.iter().any(|s| s.name.name == "Done"));
            }
            _ => panic!("Expected Machines block"),
        }
    }

    #[test]
    fn test_parse_services_block() {
         let content = r#"
            file_id: 0xcccccccccccccccc;
            types { // Need some types for parameters/return
                struct Request @id(0) { data: string @id(0); }
                struct Response @id(1) { result: string @id(0); }
                enum Status @id(2) { OK @id(0); ERR @id(1); }
            }
            services {
                $description("Core services");
                interface MyInterface @id(0) {
                     $protocol(CapnpRPC); // Example annotation
                     methodA @id(0) ( req: Request @id(0) ) -> Response { $route(path: "/a"); }; // Param ID optional in grammar
                     methodB @id(1) () -> optional<Status>; // No params, optional return
                }

                service MyService @id(1) extends BaseService {
                     $implements(MyInterface);
                     $route(basePath: "/api");
                     $meta(version: "1.0");
                }
            }
        "#;
        let result = parse_ssot_content(content, None);
        println!("Parse result (services): {:?}", result);
        if let Err(e) = &result {
            if let ParseError::PestError(pe) = e {
                eprintln!("Pest Error Details:\n{}", pe);
            }
        }
        assert!(result.is_ok());
        let ast = result.unwrap();

        let services_block = ast.definitions.iter().find_map(|def| match def {
            TopLevelDefinition::Services(block) => Some(block),
            _ => None,
        }).expect("Services block not found");

        assert_eq!(services_block.annotations.len(), 1);
         if let Annotation::Description(d) = &services_block.annotations[0] {
             assert_eq!(d, "Core services");
         } else { panic!("Expected description annotation"); }

         assert_eq!(services_block.definitions.len(), 2);

         // Check Interface
         let interface = match &services_block.definitions[0] {
             ServiceItem::Interface(iface) => iface,
             _ => panic!("Expected InterfaceDefinition"),
         };
         assert_eq!(interface.name.name, "MyInterface");
         assert_eq!(interface.id.value, 0);
         assert_eq!(interface.annotations.len(), 1);
         // TODO: Check protocol annotation if parsed specifically or as GenericKeyValue

         assert_eq!(interface.methods.len(), 2);
         let method_a = &interface.methods[0];
         assert_eq!(method_a.name.name, "methodA");
         assert_eq!(method_a.id.value, 0);
         assert_eq!(method_a.parameters.len(), 1);
         assert_eq!(method_a.parameters[0].name.name, "req");
         assert_eq!(method_a.parameters[0].id, Some(NumericId{ value: 0})); // Check optional ID parsed
         if let TypeSpecifier::Simple(ts) = &method_a.parameters[0].type_spec {
            assert_eq!(ts.name, "Request");
         } else { panic!("Expected simple type"); }
         assert!(method_a.return_type.is_some());
         if let TypeSpecifier::Simple(ts) = method_a.return_type.as_ref().unwrap() {
             assert_eq!(ts.name, "Response");
         } else { panic!("Expected simple return type"); }
         assert_eq!(method_a.body_annotations.len(), 1); // Check annotation in body

         let method_b = &interface.methods[1];
         assert_eq!(method_b.name.name, "methodB");
         assert_eq!(method_b.parameters.len(), 0);
         assert!(method_b.return_type.is_some());
         if let TypeSpecifier::Optional(ot) = method_b.return_type.as_ref().unwrap() {
            if let TypeSpecifier::Simple(ts) = &**ot {
                 assert_eq!(ts.name, "Status");
            } else { panic!("Expected simple type inside optional"); }
         } else { panic!("Expected optional return type"); }
         assert!(method_b.body_annotations.is_empty());

        // Check Service
        let service = match &services_block.definitions[1] {
            ServiceItem::Service(serv) => serv,
            _ => panic!("Expected ServiceDefinition"),
        };
        assert_eq!(service.name.name, "MyService");
        assert_eq!(service.id.value, 1);
        assert!(service.extends.is_some());
        assert_eq!(service.extends.as_ref().unwrap().name, "BaseService");
        assert_eq!(service.annotations.len(), 3); // $implements, $route, $meta
        assert!(service.annotations.iter().any(|a| matches!(a, Annotation::Implements(id) if id.name == "MyInterface")));

    }

    #[test]
    fn test_parse_communication_block() {
        let content = r#"
            file_id: 0xdddddddddddddddd;
             types { // Need string type
                 struct Dummy @id(0) {}
             }
             communication {
                 $description("System messaging");
                 protocol MyProto @id(0) { $meta(standard: "custom"); };

                 channel UserEvents @id(1) {
                     description: "Events for specific users";
                     parameters: { userId: string }; // Using string directly for now
                 };

                 event UserLoggedIn @id(2) {
                     $channel(UserEvents);
                     userId: string @id(0);
                     timestamp: u64 @id(1);
                 }
             }
        "#;
        let result = parse_ssot_content(content, None);
        println!("Parse result (communication): {:?}", result);
        if let Err(e) = &result {
            if let ParseError::PestError(pe) = e {
                eprintln!("Pest Error Details:\n{}", pe);
            }
        }
        assert!(result.is_ok());
        let ast = result.unwrap();

         let comm_block = ast.definitions.iter().find_map(|def| match def {
            TopLevelDefinition::Communication(block) => Some(block),
            _ => None,
        }).expect("Communication block not found");

        assert_eq!(comm_block.annotations.len(), 1);
         if let Annotation::Description(d) = &comm_block.annotations[0] {
             assert_eq!(d, "System messaging");
         } else { panic!("Expected description annotation"); }

         assert_eq!(comm_block.definitions.len(), 3);

         // Check Protocol
         let proto = match &comm_block.definitions[0] {
             CommunicationItem::Protocol(p) => p,
             _ => panic!("Expected ProtocolDefinition")
         };
         assert_eq!(proto.name.name, "MyProto");
         assert_eq!(proto.id.value, 0);
         assert_eq!(proto.annotations.len(), 1);
         assert!(matches!(proto.annotations[0], Annotation::Meta(_)));

         // Check Channel
          let channel = match &comm_block.definitions[1] {
             CommunicationItem::Channel(c) => c,
             _ => panic!("Expected ChannelDefinition")
         };
          assert_eq!(channel.name.name, "UserEvents");
          assert_eq!(channel.id.value, 1);
          assert!(channel.description.is_some());
          assert_eq!(channel.description.as_ref().unwrap(), "Events for specific users");
          assert!(channel.parameters.is_some());
          let params = channel.parameters.as_ref().unwrap();
          assert_eq!(params.len(), 1);
          assert_eq!(params[0].key.name, "userId");
          // Note: Value is parsed as string/int, not type. Grammar/parser needs update for type parsing here.
          if let AnnotationValue::String(s) = &params[0].value {
              assert_eq!(s, "string");
          } else { panic!("Expected string value for parameter type placeholder"); }

          // Check Event
          let event = match &comm_block.definitions[2] {
              CommunicationItem::Event(e) => e,
              _ => panic!("Expected EventDefinition")
          };
          assert_eq!(event.name.name, "UserLoggedIn");
          assert_eq!(event.id.value, 2);
          assert_eq!(event.annotations.len(), 1);
          assert!(matches!(event.annotations[0], Annotation::Channel(id) if id.name == "UserEvents"));
          assert_eq!(event.fields.len(), 2);
          assert_eq!(event.fields[0].name.name, "userId");
          assert_eq!(event.fields[1].name.name, "timestamp");
    }

    #[test]
    fn test_parse_actors_block() {
        let content = r#"
            file_id: 0xeeeeeeeeeeeeeeee;
            actors {
                $description("System actors");
                actor User @id(0) {
                    $type("role");
                };
                actor ExternalApi @id(1) {
                    $type("system");
                    $description("Third-party API");
                }
            }
        "#;
        let result = parse_ssot_content(content, None);
        println!("Parse result (actors): {:?}", result);
         if let Err(e) = &result {
            if let ParseError::PestError(pe) = e {
                eprintln!("Pest Error Details:\n{}", pe);
            }
        }
        assert!(result.is_ok());
        let ast = result.unwrap();

        let actors_block = ast.definitions.iter().find_map(|def| match def {
            TopLevelDefinition::Actors(block) => Some(block),
            _ => None,
        }).expect("Actors block not found");

        assert_eq!(actors_block.annotations.len(), 1);
        assert!(matches!(actors_block.annotations[0], Annotation::Description(_)));
        assert_eq!(actors_block.definitions.len(), 2);

        // Check Actor User
        let actor_user = &actors_block.definitions[0];
        assert_eq!(actor_user.name.name, "User");
        assert_eq!(actor_user.id.value, 0);
        assert_eq!(actor_user.annotations.len(), 1);
        assert!(matches!(actor_user.annotations[0], Annotation::GenericKeyValue(id, _) if id.name == "type"));

        // Check Actor ExternalApi
        let actor_api = &actors_block.definitions[1];
        assert_eq!(actor_api.name.name, "ExternalApi");
        assert_eq!(actor_api.id.value, 1);
        // Order of annotations might vary
        assert_eq!(actor_api.annotations.len(), 2);
        assert!(actor_api.annotations.iter().any(|a| matches!(a, Annotation::GenericKeyValue(id, _) if id.name == "type")));
        assert!(actor_api.annotations.iter().any(|a| matches!(a, Annotation::Description(_))));
    }

    #[test]
    fn test_parse_deployment_config_block() {
        let content = r#"
            file_id: 0xffffffffffffffff;
            actors { actor DummyActor @id(0) {}; }
            services { service DummyService @id(0) {}; }

            deployment_config {
                $description("Deployment settings");

                environment Production @id(0) {
                    variables: { logLevel: "info", apiEndpoint: "https://prod.example.com" };
                    $provider("aws");
                }

                environment Staging @id(1) extends Production {
                    variables: { logLevel: "debug" }; // Override variable
                }

                infrastructure Compute @id(0) {
                    type: "kubernetes";
                    instanceType: "t3.large";
                }

                deployment DeployLive @id(0) {
                    targetEnvironment: Production;
                    targetInfrastructure: { cluster: Compute }; // Simple reference for now
                    deployable: DummyService;
                    replicas: 3;
                    config: { secretKey: "$env(PROD_SECRET)" };
                    $strategy("blue-green");
                }
            }
        "#;
        let result = parse_ssot_content(content, None);
        println!("Parse result (deployment_config): {:?}", result);
        if let Err(e) = &result {
            if let ParseError::PestError(pe) = e {
                eprintln!("Pest Error Details:\n{}", pe);
            }
        }
        assert!(result.is_ok());
        let ast = result.unwrap();

        let dep_block = ast.definitions.iter().find_map(|def| match def {
            TopLevelDefinition::DeploymentConfig(block) => Some(block),
            _ => None,
        }).expect("DeploymentConfig block not found");

         assert_eq!(dep_block.annotations.len(), 1);
         assert!(matches!(dep_block.annotations[0], Annotation::Description(_)));
         assert_eq!(dep_block.definitions.len(), 4);

         // Check Environment Production
         let env_prod = match &dep_block.definitions[0] {
             DeploymentItem::Environment(env) => env,
             _ => panic!("Expected EnvironmentDefinition")
         };
         assert_eq!(env_prod.name.name, "Production");
         assert_eq!(env_prod.id.value, 0);
         assert!(env_prod.extends.is_none());
         assert!(env_prod.variables.is_some());
         assert_eq!(env_prod.variables.as_ref().unwrap().len(), 2);
         assert_eq!(env_prod.annotations.len(), 1);
         assert!(matches!(env_prod.annotations[0], Annotation::GenericKeyValue(id, _) if id.name == "provider"));

         // Check Environment Staging
          let env_staging = match &dep_block.definitions[1] {
             DeploymentItem::Environment(env) => env,
             _ => panic!("Expected EnvironmentDefinition")
         };
         assert_eq!(env_staging.name.name, "Staging");
         assert!(env_staging.extends.is_some());
         assert_eq!(env_staging.extends.as_ref().unwrap().name, "Production");
         assert!(env_staging.variables.is_some());
         assert_eq!(env_staging.variables.as_ref().unwrap().len(), 1); // Check override

         // Check Infrastructure Compute
         let infra_compute = match &dep_block.definitions[2] {
             DeploymentItem::Infrastructure(infra) => infra,
             _ => panic!("Expected InfrastructureDefinition")
         };
         assert_eq!(infra_compute.name.name, "Compute");
         assert_eq!(infra_compute.attributes.len(), 2);
         assert_eq!(infra_compute.attributes[0].key.name, "type");
         assert_eq!(infra_compute.attributes[1].key.name, "instanceType");

         // Check Deployment DeployLive
         let dep_live = match &dep_block.definitions[3] {
             DeploymentItem::Deployment(dep) => dep,
             _ => panic!("Expected DeploymentDefinition")
         };
         assert_eq!(dep_live.name.name, "DeployLive");
         assert!(dep_live.target_environment.is_some());
         assert_eq!(dep_live.target_environment.as_ref().unwrap().name, "Production");
         assert!(dep_live.target_infrastructure.is_some()); // Basic check
         assert!(dep_live.deployable.is_some());
         assert_eq!(dep_live.deployable.as_ref().unwrap().name, "DummyService");
         assert!(dep_live.config.is_some());
         assert_eq!(dep_live.other_attributes.len(), 1);
         assert_eq!(dep_live.other_attributes[0].key.name, "replicas");
         assert_eq!(dep_live.annotations.len(), 1);
         assert!(matches!(dep_live.annotations[0], Annotation::GenericKeyValue(id, _) if id.name == "strategy"));
    }

    #[test]
    fn test_parse_annotations() { // New test specific to annotations
        let content = r#"
            file_id: 0xaaaaaaaaaaaaaaaa;
            types {
                 struct AnnotatedStruct @id(0) {
                     $description("A struct with various annotations.");
                     field1: string @id(0) { $validate(required: true, maxLength: 100); };
                     field2: i32 @id(1) { $db(index: true); };
                     field3: bool @id(2) { $meta(defaultValue: false, uiHint: "toggle"); };
                     $customFlag; // Generic flag
                     $outputDir("/generated"); // Generic KV
                 }
            }
        "#;
        let result = parse_ssot_content(content, None);
        println!("Parse result (annotations): {:?}", result);
        if let Err(e) = &result {
            if let ParseError::PestError(pe) = e {
                eprintln!("Pest Error Details:\n{}", pe);
            }
        }
        assert!(result.is_ok());
        let ast = result.unwrap();

        let types_block = ast.definitions.iter().find_map(|def| match def {
            TopLevelDefinition::Types(block) => Some(block),
            _ => None,
        }).expect("Types block not found");

        let struct_def = match &types_block.definitions[0] {
            TypeDefinition::Struct(s) => s,
            _ => panic!("Expected StructDefinition")
        };

        assert_eq!(struct_def.annotations.len(), 3); // $description, $customFlag, $outputDir
        assert!(matches!(struct_def.annotations[0], Annotation::Description(_)));
        assert!(matches!(struct_def.annotations[1], Annotation::GenericFlag(id) if id.name == "customFlag"));
        assert!(matches!(struct_def.annotations[2], Annotation::GenericKeyValue(id, val) if id.name == "outputDir" && val == "/generated"));

        // Check field1 annotations ($validate)
        let field1 = &struct_def.fields[0];
        assert_eq!(field1.annotations.len(), 1);
        if let Annotation::Validate(args) = &field1.annotations[0] {
             assert_eq!(args.len(), 2);
             assert_eq!(args[0].key.name, "required");
             assert_eq!(args[0].value, AnnotationValue::Boolean(true)); // Check boolean true
             assert_eq!(args[1].key.name, "maxLength");
             assert_eq!(args[1].value, AnnotationValue::Integer(100));
        } else { panic!("Expected Validate annotation"); }

        // Check field2 annotations ($db)
        let field2 = &struct_def.fields[1];
        assert_eq!(field2.annotations.len(), 1);
        if let Annotation::Db(args) = &field2.annotations[0] {
             assert_eq!(args.len(), 1);
             assert_eq!(args[0].key.name, "index");
             assert_eq!(args[0].value, AnnotationValue::Boolean(true)); // Check boolean true
        } else { panic!("Expected Db annotation"); }

        // Check field3 annotations ($meta)
        let field3 = &struct_def.fields[2];
        assert_eq!(field3.annotations.len(), 1);
        if let Annotation::Meta(args) = &field3.annotations[0] {
            assert_eq!(args.len(), 2);
            assert_eq!(args[0].key.name, "defaultValue");
            assert_eq!(args[0].value, AnnotationValue::Boolean(false)); // Check boolean false
             assert_eq!(args[1].key.name, "uiHint");
             assert!(matches!(args[1].value, AnnotationValue::String(_)));
        } else { panic!("Expected Meta annotation"); }
    }

} 