use crate::ast::{self, Identifier, NumericId, SsotAst, FileId, ImportStatement, TopLevelDefinition, TypesBlock, TypeDefinition, StructDefinition, FieldDefinition, EnumDefinition, EnumVariant, TypeSpecifier, Annotation, Argument, AnnotationValue, MachineDefinition, MachinesBlock};
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
        ..Default::default()
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

    let mut block = TypesBlock::default();
    for inner_pair in pair.into_inner() {
        match inner_pair.as_rule() {
            Rule::annotation => {
                block.annotations.push(parse_annotation(inner_pair)?);
            }
            Rule::type_definition => {
                 // The type_definition rule itself is silent (_), so we need to look at its inner content.
                 let definition_pair = inner_pair.into_inner().next().ok_or_else(|| ParseError::InvalidInput { message: "Empty type_definition rule".to_string() })?;
                 match definition_pair.as_rule() {
                     Rule::struct_definition => {
                         block.definitions.push(TypeDefinition::Struct(parse_struct_definition(definition_pair)?));
                     }
                     Rule::enum_definition => {
                         block.definitions.push(TypeDefinition::Enum(parse_enum_definition(definition_pair)?));
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
    Ok(block)
}

fn parse_machines_block(pair: Pair<Rule>) -> ParseResult<MachinesBlock> {
    if pair.as_rule() != Rule::machines_block {
        return Err(ParseError::UnexpectedRule { expected: Rule::machines_block, found: pair.as_rule() });
    }
    println!("Parsing machines block: {}", pair.as_str());

    let mut block = MachinesBlock::default();
    for inner_pair in pair.into_inner() {
        match inner_pair.as_rule() {
            Rule::annotation => {
                block.annotations.push(parse_annotation(inner_pair)?);
            }
            Rule::machine_definition => {
                block.definitions.push(parse_machine_definition(inner_pair)?);
            }
            rule => {
                eprintln!("Warning: Skipping unexpected rule within machines_block: {:?}", rule);
            }
        }
    }
    Ok(block)
}

// --- Definition Parsing (Implementations) --- 

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
    // TODO: Parse machine elements (context, states, etc.)

    while let Some(p) = inner_pairs.peek() {
        match p.as_rule() {
            Rule::annotation => annotations.push(parse_annotation(inner_pairs.next().unwrap())?),
            Rule::identifier => {
                name = Some(parse_identifier(inner_pairs.next().unwrap())?);
                // Expect ID
                let id_pair = inner_pairs.next().ok_or(ParseError::MissingRule { expected: Rule::integer_literal })?;
                id = Some(parse_numeric_id(id_pair)?);
            }
            Rule::machine_element => {
                // TODO: Implement parsing for machine elements
                eprintln!("Skipping machine_element for now: {}", inner_pairs.next().unwrap().as_str());
            }
            rule => return Err(ParseError::UnexpectedRule { expected: Rule::annotation /* or identifier or machine_element */, found: rule })
        }
    }

    Ok(MachineDefinition {
        name: name.ok_or(ParseError::MissingRule { expected: Rule::identifier })?,
        id: id.ok_or(ParseError::MissingRule { expected: Rule::integer_literal })?,
        annotations,
        ..Default::default() // Rest are TODOs
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
    use crate::ast::{self, TypeDefinition, Annotation, TypeSpecifier};
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