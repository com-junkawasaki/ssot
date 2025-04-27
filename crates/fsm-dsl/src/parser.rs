use crate::ast::{self, Identifier, NumericId, SsotAst, FileId, ImportStatement, TopLevelDefinition, TypesBlock, TypeDefinition, StructDefinition, FieldDefinition, EnumDefinition, EnumVariant, TypeSpecifier, Annotation, Argument, AnnotationValue, MachineDefinition, MachinesBlock, ContextDefinition, ContextFieldDefinition, StatesBlock, StateDefinition, TransitionDefinition};
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

    for pair in file_pair.into_inner() {
        match pair.as_rule() {
            Rule::file_id => {
                if ast.file_id.is_some() {
                    eprintln!("Warning: Duplicate file ID found, ignoring subsequent IDs.");
                } else {
                    ast.file_id = Some(parse_file_id(pair)?);
                }
            }
            Rule::import_statement => {
                ast.imports.push(parse_import_statement(pair)?);
            }
            Rule::types_block => {
                ast.definitions.push(TopLevelDefinition::Types(parse_types_block(pair)?));
            }
            Rule::machines_block => {
                ast.definitions.push(TopLevelDefinition::Machines(parse_machines_block(pair)?));
            }
            Rule::actors_block => {
                eprintln!("Skipping actors_block for now.");
                 // ast.definitions.push(TopLevelDefinition::Actors(parse_actors_block(pair)?));
            }
            Rule::communication_block => {
                eprintln!("Skipping communication_block for now.");
                // ast.definitions.push(TopLevelDefinition::Communication(parse_communication_block(pair)?));
            }
             Rule::services_block => {
                eprintln!("Skipping services_block for now.");
                // ast.definitions.push(TopLevelDefinition::Services(parse_services_block(pair)?));
            }
             Rule::deployment_config_block => {
                eprintln!("Skipping deployment_config_block for now.");
                 // ast.definitions.push(TopLevelDefinition::DeploymentConfig(parse_deployment_config_block(pair)?));
            }
            Rule::annotation => {
                 eprintln!("Skipping top-level annotation for now.");
                 // TODO: Handle top-level annotations if needed
            }
            Rule::COMMENT => { /* Skip comments */ }
            Rule::EOI => { /* End of Input, expected */ }
            other_rule => {
                 eprintln!("Warning: Skipping unexpected top-level rule: {:?}", other_rule);
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

    for inner_pair in pair.into_inner() {
        match inner_pair.as_rule() {
            Rule::annotation => {
                annotations.push(parse_annotation(inner_pair)?);
            }
            Rule::type_definition => {
                 let definition_pair = inner_pair.into_inner().next().ok_or_else(|| ParseError::InvalidInput { message: "Empty type_definition rule".to_string() })?;
                 match definition_pair.as_rule() {
                     Rule::struct_definition => {
                         definitions.push(TypeDefinition::Struct(parse_struct_definition(definition_pair)?));
                     }
                     Rule::enum_definition => {
                         definitions.push(TypeDefinition::Enum(parse_enum_definition(definition_pair)?));
                     }
                     rule => return Err(ParseError::UnexpectedRule { expected: Rule::struct_definition /* or enum */, found: rule })
                 }
            }
             // Handle unexpected rules within the types block if necessary
            rule => {
                eprintln!("Warning: Skipping unexpected rule within types_block: {:?}", rule);
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

    for inner_pair in pair.into_inner() {
        match inner_pair.as_rule() {
            Rule::annotation => {
                annotations.push(parse_annotation(inner_pair)?);
            }
            Rule::machine_definition => {
                definitions.push(parse_machine_definition(inner_pair)?);
            }
            rule => {
                eprintln!("Warning: Skipping unexpected rule within machines_block: {:?}", rule);
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
    // TODO: Add other machine elements (states, actions, etc.)

    while let Some(p) = inner_pairs.peek() {
        match p.as_rule() {
            Rule::annotation => annotations.push(parse_annotation(inner_pairs.next().unwrap())?),
            Rule::identifier => {
                name = Some(parse_identifier(inner_pairs.next().unwrap())?);
                let id_pair = inner_pairs.next().ok_or(ParseError::MissingRule { expected: Rule::integer_literal })?;
                id = Some(parse_numeric_id(id_pair)?);
            }
            Rule::machine_element => {
                let element_pair = inner_pairs.next().unwrap();
                let element_inner = element_pair.into_inner().next().ok_or_else(|| ParseError::InvalidInput { message: "Empty machine_element".to_string() })?;
                match element_inner.as_rule() {
                    Rule::context_definition => {
                        if context.is_some() {
                             eprintln!("Warning: Duplicate context definition found, ignoring subsequent.");
                         } else {
                             context = Some(parse_context_definition(element_inner)?);
                         }
                    }
                    Rule::states_definition => {
                        if states.is_some() {
                             eprintln!("Warning: Duplicate states definition found, ignoring subsequent.");
                         } else {
                             states = Some(parse_states_definition(element_inner)?);
                         }
                    }
                    // TODO: Add cases for actions_definition, etc.
                    rule => {
                        eprintln!("Skipping unimplemented machine_element: {:?}", rule);
                    }
                }
            }
            rule => return Err(ParseError::UnexpectedRule { expected: Rule::annotation /* or identifier or machine_element */, found: rule })
        }
    }

    Ok(MachineDefinition {
        name: name.ok_or(ParseError::MissingRule { expected: Rule::identifier })?,
        id: id.ok_or(ParseError::MissingRule { expected: Rule::integer_literal })?,
        annotations,
        context,
        states,
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
    // TODO: Parse other state elements

    // Order: annotation*, "state", identifier, "@id(", integer_literal, ")", "{", state_element*, "}"
    while let Some(p) = inner_pairs.peek() {
         match p.as_rule() {
             Rule::annotation => annotations.push(parse_annotation(inner_pairs.next().unwrap())?),
             Rule::identifier => { // Name follows annotations and "state"
                 name = Some(parse_identifier(inner_pairs.next().unwrap())?);
                 // Expect ID
                 let id_pair = inner_pairs.next().ok_or(ParseError::MissingRule { expected: Rule::integer_literal })?;
                 id = Some(parse_numeric_id(id_pair)?);
             }
             Rule::state_element => {
                let element_pair = inner_pairs.next().unwrap();
                let element_inner = element_pair.into_inner().next().ok_or_else(|| ParseError::InvalidInput { message: "Empty state_element".to_string() })?;
                 match element_inner.as_rule() {
                     Rule::on_transition => {
                         transitions.push(parse_on_transition(element_inner)?);
                     }
                     // TODO: Add cases for on_entry, on_exit, invoke, etc.
                     rule => {
                          eprintln!("Skipping unimplemented state_element: {:?}", rule);
                     }
                 }
             }
             rule => return Err(ParseError::UnexpectedRule { expected: Rule::annotation /* or ident or state_element */, found: rule })
         }
    }

    Ok(StateDefinition {
        name: name.ok_or(ParseError::MissingRule { expected: Rule::identifier })?,
        id: id.ok_or(ParseError::MissingRule { expected: Rule::integer_literal })?,
        annotations,
        transitions,
    })
}

fn parse_on_transition(pair: Pair<Rule>) -> ParseResult<TransitionDefinition> {
    if pair.as_rule() != Rule::on_transition {
        return Err(ParseError::UnexpectedRule { expected: Rule::on_transition, found: pair.as_rule() });
    }
    println!("Parsing on transition: {}", pair.as_str()); // Debug print

    let mut inner_pairs = pair.into_inner();
    let event = parse_identifier(inner_pairs.next().ok_or(ParseError::MissingRule { expected: Rule::identifier })?)?;
    let id = parse_numeric_id(inner_pairs.next().ok_or(ParseError::MissingRule { expected: Rule::integer_literal })?)?;

    let details_pair = inner_pairs.next().ok_or(ParseError::MissingRule { expected: Rule::transition_details })?;
    if details_pair.as_rule() != Rule::transition_details {
         return Err(ParseError::UnexpectedRule { expected: Rule::transition_details, found: details_pair.as_rule() });
    }

    let mut details_inner = details_pair.into_inner();
    let target = parse_identifier(details_inner.next().ok_or(ParseError::MissingRule { expected: Rule::identifier })?)?; // Expect 'identifier' for target

    // --- Start parsing optional transition block ---
    let mut actions = Vec::new();
    let mut guards = Vec::new();

    // Check if transition_block exists
    if let Some(block_pair) = details_inner.next() {
        if block_pair.as_rule() != Rule::transition_block {
            return Err(ParseError::UnexpectedRule { expected: Rule::transition_block, found: block_pair.as_rule() });
        }
        // Iterate inside the block { ... }
        for item_pair in block_pair.into_inner() {
            match item_pair.as_rule() {
                Rule::action_ref => {
                    // action_ref = { "action" ~ identifier ~ ";" }
                    let action_id = parse_identifier(item_pair.into_inner().next().ok_or(ParseError::MissingRule { expected: Rule::identifier })?)?;
                    actions.push(action_id);
                }
                Rule::guard_ref => {
                     // guard_ref = { "guard" ~ identifier ~ ";" }
                     let guard_id = parse_identifier(item_pair.into_inner().next().ok_or(ParseError::MissingRule { expected: Rule::identifier })?)?;
                    guards.push(guard_id);
                }
                rule => return Err(ParseError::UnexpectedRule { expected: Rule::action_ref /* or guard_ref */, found: rule }),
            }
        }
    }
    // --- End parsing optional transition block ---

    // Check if there are any unexpected pairs left in on_transition
    if inner_pairs.next().is_some() {
         eprintln!("Warning: Unexpected extra tokens found after transition details in 'on {}'", event.name);
    }


    Ok(TransitionDefinition {
        event,
        target,
        id,
        annotations: vec![], // TODO: Parse annotations if they become possible here
        actions, // Assign parsed actions
        guards,  // Assign parsed guards
    })
}

// --- Helper Functions (Implementations) ---

fn parse_annotation(pair: Pair<Rule>) -> ParseResult<Annotation> {
     if pair.as_rule() != Rule::annotation {
         return Err(ParseError::UnexpectedRule { expected: Rule::annotation, found: pair.as_rule() });
     }
    let inner_pair = pair.into_inner().next().ok_or_else(|| ParseError::InvalidInput { message: "Empty annotation rule".to_string() })?;

    println!("Parsing specific annotation: {:?} - {}", inner_pair.as_rule(), inner_pair.as_str()); // Debug print

    match inner_pair.as_rule() {
        Rule::description_annotation => {
            let value_pair = inner_pair.into_inner().next().ok_or(ParseError::MissingRule { expected: Rule::string_literal })?;
            let value = parse_string_literal(value_pair)?;
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
            let key_pair = inner_pair.into_inner().next().ok_or(ParseError::MissingRule { expected: Rule::identifier })?;
            let key = parse_identifier(key_pair)?;
            Ok(Annotation::GenericFlag(key))
        }
         Rule::generic_kv_annotation => {
             let mut kv_pairs = inner_pair.into_inner();
             let key_pair = kv_pairs.next().ok_or(ParseError::MissingRule { expected: Rule::identifier })?;
             let key = parse_identifier(key_pair)?;
             let value_pair = kv_pairs.next().ok_or(ParseError::MissingRule { expected: Rule::annotation_value })?;
             let value_inner = value_pair.into_inner().next().ok_or_else(|| ParseError::InvalidInput { message: "Empty annotation_value rule in generic_kv".to_string() })?;
              // For generic KV, keep the value as string for now
              let value = match value_inner.as_rule() {
                  Rule::string_literal => parse_string_literal(value_inner)?,
                  Rule::integer_literal => value_inner.as_str().to_string(), // Keep as string
                  _ => return Err(ParseError::UnexpectedRule { expected: Rule::string_literal /* or int */, found: value_inner.as_rule() })
              };
             Ok(Annotation::GenericKeyValue(key, value))
         }
        rule => Err(ParseError::UnexpectedRule { expected: Rule::description_annotation /* or others */, found: rule })
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
        // TODO: Add boolean_literal, list_literal, object_literal when grammar/AST support them
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
     if pair.as_rule() != Rule::type_specifier {
         return Err(ParseError::UnexpectedRule { expected: Rule::type_specifier, found: pair.as_rule() });
     }
     // type_specifier is silent (_)
    let inner_pair = pair.into_inner().next().ok_or_else(|| ParseError::InvalidInput{ message: "Empty type_specifier rule".to_string() })?;
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
        // TODO: Add map_type when grammar supports it
        rule => Err(ParseError::UnexpectedRule { expected: Rule::simple_type /* or list/optional */, found: rule })
    }
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
    use super::Rule;
    use crate::ast::{self, TypeDefinition, Annotation, TypeSpecifier, StatesBlock, StateDefinition, ContextDefinition, MachineDefinition};
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
            @0x1234abcd5678ef90;
            import "/path/to/common.ssot";
        "#;
        let result = parse_ssot_content(content, None);
        assert!(result.is_ok());
        let ast = result.unwrap();

        assert_eq!(ast.file_id, Some(FileId { value: 0x1234abcd5678ef90 }));
        assert_eq!(ast.imports.len(), 1);
        assert_eq!(ast.imports[0], ImportStatement { path: "/path/to/common.ssot".to_string() });
        assert!(ast.definitions.is_empty());
    }

    #[test]
    fn test_parse_simple_types_block() {
        let content = r#"
            @0xaaaaaaaaaaaaaaaa;

            types {
                $description("Simple types");

                struct User @id(0) {
                    $description("User structure");
                    userId: string @id(0);
                    email: optional<string> @id(1);
                    tags: list<string> @id(2);
                    $validate(minLength: 1);
                }

                enum Status @id(1) {
                    $meta(kind: "state");
                    PENDING @id(0);
                    ACTIVE @id(1);
                    INACTIVE @id(2) { $description("User is inactive"); };
                }
            }
        "#;

        let result = parse_ssot_content(content, None);
        assert!(result.is_ok(), "Parsing failed: {:?}", result.err());
        let ast = result.unwrap();

        assert_eq!(ast.file_id, Some(FileId { value: 0xaaaaaaaaaaaaaaaa }));
        assert_eq!(ast.definitions.len(), 1);

        match &ast.definitions[0] {
            TopLevelDefinition::Types(types_block) => {
                assert_eq!(types_block.annotations.len(), 1);
                assert_eq!(types_block.annotations[0], Annotation::Description("Simple types".to_string()));

                assert_eq!(types_block.definitions.len(), 2);

                // Check Struct
                match &types_block.definitions[0] {
                    TypeDefinition::Struct(s) => {
                        assert_eq!(s.name, ident("User"));
                        assert_eq!(s.id, num_id(0));
                        assert_eq!(s.annotations.len(), 1);
                        assert_eq!(s.annotations[0], Annotation::Description("User structure".to_string()));
                        assert_eq!(s.fields.len(), 3);
                        
                        // Field 0: userId
                        assert_eq!(s.fields[0].name, ident("userId"));
                        assert_eq!(s.fields[0].id, num_id(0));
                        assert_eq!(s.fields[0].type_spec, TypeSpecifier::Simple(ident("string")));
                        assert!(s.fields[0].annotations.is_empty());
                        
                        // Field 1: email
                        assert_eq!(s.fields[1].name, ident("email"));
                        assert_eq!(s.fields[1].id, num_id(1));
                        assert_eq!(s.fields[1].type_spec, TypeSpecifier::Optional(Box::new(TypeSpecifier::Simple(ident("string")))));
                        assert!(s.fields[1].annotations.is_empty());

                        // Field 2: tags
                        assert_eq!(s.fields[2].name, ident("tags"));
                        assert_eq!(s.fields[2].id, num_id(2));
                        assert_eq!(s.fields[2].type_spec, TypeSpecifier::List(Box::new(TypeSpecifier::Simple(ident("string")))));
                         assert_eq!(s.fields[2].annotations.len(), 1);
                         assert_eq!(s.fields[2].annotations[0], Annotation::Validate(vec![ 
                            Argument { key: ident("minLength"), value: AnnotationValue::Integer(1) }
                         ]));
                    }
                    _ => panic!("Expected StructDefinition"),
                }

                 // Check Enum
                match &types_block.definitions[1] {
                     TypeDefinition::Enum(e) => {
                        assert_eq!(e.name, ident("Status"));
                        assert_eq!(e.id, num_id(1));
                        assert_eq!(e.annotations.len(), 1);
                        assert_eq!(e.annotations[0], Annotation::Meta(vec![
                            Argument { key: ident("kind"), value: AnnotationValue::String("state".to_string()) }
                        ]));
                        assert_eq!(e.variants.len(), 3);
                        assert_eq!(e.variants[0].name, ident("PENDING"));
                        assert_eq!(e.variants[0].id, num_id(0));
                        assert!(e.variants[0].annotations.is_empty());
                        assert_eq!(e.variants[1].name, ident("ACTIVE"));
                        assert_eq!(e.variants[1].id, num_id(1));
                         assert!(e.variants[1].annotations.is_empty());
                        assert_eq!(e.variants[2].name, ident("INACTIVE"));
                        assert_eq!(e.variants[2].id, num_id(2));
                         assert_eq!(e.variants[2].annotations.len(), 1);
                         assert_eq!(e.variants[2].annotations[0], Annotation::Description("User is inactive".to_string()));
                     }
                    _ => panic!("Expected EnumDefinition"),
                }
            }
            _ => panic!("Expected TopLevelDefinition::Types"),
        }
    }

    #[test]
    fn test_parse_simple_machines_block() {
        let content = r#"
            @0x1;
            machines {
                machine MyMachine @id(10) {
                    context @id(11) {
                        counter: int @id(12);
                    }
                    states @id(13) {
                        state Idle @id(14) {
                            on START @id(15) transition Active; # Simple transition
                        }
                        state Active @id(16) {
                            on STOP @id(17) transition Idle {
                                action resetCounter;
                                guard canStop;
                                action notifyStop; # Multiple actions/guards
                            };
                            on INTERNAL @id(18) transition Active {
                                guard isCounterHigh; # Only guard
                            };
                             on ANOTHER @id(19) transition Idle {
                                action doSomething; # Only action
                            };
                        }
                    }
                }
            }
        "#;

        let result = parse_ssot_content(content, None);
        assert!(result.is_ok(), "Parsing failed: {:?}", result.err());
        let ast = result.unwrap();

        assert_eq!(ast.definitions.len(), 1);

        match &ast.definitions[0] {
            TopLevelDefinition::Machines(machines_block) => {
                assert_eq!(machines_block.definitions.len(), 1);
                let machine = &machines_block.definitions[0];

                assert_eq!(machine.name, ident("MyMachine"));
                assert_eq!(machine.id, num_id(10));

                // Check context (basic check)
                assert!(machine.context.is_some());
                if let Some(ctx) = &machine.context {
                     assert_eq!(ctx.id, num_id(11));
                     assert_eq!(ctx.fields.len(), 1);
                     assert_eq!(ctx.fields[0].name, ident("counter"));
                }


                // Check states block
                assert!(machine.states.is_some());
                if let Some(states_block) = &machine.states {
                    assert_eq!(states_block.id, num_id(13));
                    assert_eq!(states_block.states.len(), 2);

                    // State: Idle
                    let idle_state = &states_block.states[0];
                    assert_eq!(idle_state.name, ident("Idle"));
                    assert_eq!(idle_state.id, num_id(14));
                    assert_eq!(idle_state.transitions.len(), 1);
                    let idle_trans = &idle_state.transitions[0];
                    assert_eq!(idle_trans.event, ident("START"));
                    assert_eq!(idle_trans.id, num_id(15));
                    assert_eq!(idle_trans.target, ident("Active"));
                    assert!(idle_trans.actions.is_empty()); // No actions/guards
                    assert!(idle_trans.guards.is_empty());

                    // State: Active
                    let active_state = &states_block.states[1];
                    assert_eq!(active_state.name, ident("Active"));
                    assert_eq!(active_state.id, num_id(16));
                    assert_eq!(active_state.transitions.len(), 3);

                    // Transition 1: STOP
                    let stop_trans = &active_state.transitions[0];
                    assert_eq!(stop_trans.event, ident("STOP"));
                     assert_eq!(stop_trans.id, num_id(17));
                    assert_eq!(stop_trans.target, ident("Idle"));
                    assert_eq!(stop_trans.actions, vec![ident("resetCounter"), ident("notifyStop")]);
                    assert_eq!(stop_trans.guards, vec![ident("canStop")]);

                    // Transition 2: INTERNAL
                     let internal_trans = &active_state.transitions[1];
                    assert_eq!(internal_trans.event, ident("INTERNAL"));
                    assert_eq!(internal_trans.id, num_id(18));
                    assert_eq!(internal_trans.target, ident("Active"));
                    assert!(internal_trans.actions.is_empty());
                    assert_eq!(internal_trans.guards, vec![ident("isCounterHigh")]);

                     // Transition 3: ANOTHER
                     let another_trans = &active_state.transitions[2];
                    assert_eq!(another_trans.event, ident("ANOTHER"));
                    assert_eq!(another_trans.id, num_id(19));
                    assert_eq!(another_trans.target, ident("Idle"));
                    assert_eq!(another_trans.actions, vec![ident("doSomething")]);
                    assert!(another_trans.guards.is_empty());
                }
            }
            _ => panic!("Expected TopLevelDefinition::Machines"),
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
} 