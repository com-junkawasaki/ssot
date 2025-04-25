use std::collections::HashMap; // Use HashMap to store ASTs by path
use std::env;
use std::fs;
use std::path::{Path, PathBuf};
// Remove unused Command import for now
// use std::process::Command;

// Keep codegen imports for now, might be needed later for the integrated generation
use fsm_codegen::{generate_capnp_schema, generate_rust_code, generate_typescript_types};
// Use the parser function and AST types
use fsm_dsl::ast::{Annotation, AnnotationValue, SsotFile, ImportDeclaration, MessageItem, QualifiedIdent, StateMachine, UseDeclaration}; // Keep AST import
use fsm_dsl::parser::{parse_file, ParseError as DslParseError}; // Keep parser imports
use glob::glob;
use thiserror::Error;

#[derive(Error, Debug)]
enum BuildError {
    #[error("Glob pattern error: {0}")]
    Glob(#[from] glob::PatternError),
    #[error("Glob iteration error: {0}")]
    GlobIteration(#[from] glob::GlobError),
    #[error("I/O error processing path {path:?}: {source}")]
    Io {
        path: PathBuf,
        source: std::io::Error,
    },
    #[error("DSL parsing error in file {path:?}: {source}")]
    DslParse {
        path: PathBuf,
        #[source]
        source: DslParseError,
    },
    // Remove Codegen error for now, as generation is deferred
    // #[error("Code generation error for state machine '{machine_name}' in file {path:?}: {source}")]
    // Codegen {
    //     path: PathBuf,
    //     machine_name: String,
    //     #[source]
    //     source: fsm_codegen::CodegenError,
    // },
    // #[error("Missing '$rust_out(\"...")' annotation (top-level or per-machine) in {path:?}")]
    // MissingRustOut { path: PathBuf },
    #[error("Duplicate package declaration '{package_name}' found in files {file1:?} and {file2:?}")]
    DuplicatePackage {
        package_name: String,
        file1: PathBuf,
        file2: PathBuf,
    },
    #[error("Package '{package_name}' imported in {importer_file:?} not found.")]
    PackageNotFound {
        package_name: String,
        importer_file: PathBuf,
    },
    #[error("Symbol '{symbol_name}' not found (referenced in {referencing_file:?})")]
    SymbolNotFound {
        symbol_name: String, // Fully qualified name or name used in context
        referencing_file: PathBuf, // File where the reference occurs
    },
    #[error("Ambiguous symbol '{symbol_name}' used in {referencing_file:?}. Could refer to multiple definitions.")]
    AmbiguousSymbol {
        symbol_name: String,
        referencing_file: PathBuf,
        // Optional: Add locations of conflicting definitions
    },
    #[error("Circular dependency detected involving package '{package_name}'")]
    CircularDependency { package_name: String },
}

// Helper to wrap std::io::Error with path context (keep)
fn io_err(path: impl Into<PathBuf>, source: std::io::Error) -> BuildError {
    BuildError::Io {
        path: path.into(),
        source,
    }
}

// Helper to find annotation value by name from a slice (keep, might be useful later)
fn find_annotation_str_value<'a>(annotations: &'a [Annotation], name: &str) -> Option<&'a str> {
    annotations
        .iter()
        .find(|a| a.name == name)
        .and_then(|a| match a.value.as_ref() {
            Some(AnnotationValue::StringLiteral(s)) => Some(s.as_str()),
            _ => None,
        })
}

// Helper to convert CamelCase to snake_case (keep, might be useful later)
fn camel_to_snake(s: &str) -> String {
    if s.is_empty() {
        return String::new();
    }

    let mut result = String::new();
    let mut chars = s.chars().peekable();
    let mut previous_was_uppercase = false;

    while let Some(c) = chars.next() {
        if c.is_uppercase() {
            if !result.is_empty() && !previous_was_uppercase {
                result.push('_');
            }
            result.extend(c.to_lowercase());
            previous_was_uppercase = true;
        } else {
            result.push(c);
            previous_was_uppercase = false;
        }
    }
    result
}

/// Holds references to all parsed ASTs and mappings for resolution.
struct ParsedContext<'a> {
    /// Maps canonical file path to the parsed SsotFile AST.
    files: &'a HashMap<PathBuf, SsotFile>,
    /// Maps package name (String) to the canonical path of the file declaring it.
    package_map: HashMap<String, PathBuf>,
    // /// Maps fully qualified symbol name (e.g., "pkg.common.EventA") to its definition.
    // /// Might need a more complex structure like an enum SymbolDefinition { Event(MessageItem), State(...), ... }
    // symbol_table: HashMap<String, SymbolDefinition<'a>>, // To be built
}

fn main() -> Result<(), BuildError> {
    let crate_dir = env::var("CARGO_MANIFEST_DIR").unwrap();
    let crate_path = PathBuf::from(crate_dir);
    let spec_dir = crate_path.join("spec");
    let pattern = spec_dir.join("**/*.ssot");

    println!("cargo:rerun-if-changed={}", spec_dir.display());

    // Store parsed ASTs mapped by their path
    let mut parsed_files: HashMap<PathBuf, SsotFile> = HashMap::new();

    println!("Searching for FSM definitions in: {}", pattern.display());

    for entry in glob(pattern.to_str().unwrap())? {
        let ssot_path = entry?;
        println!("cargo:rerun-if-changed={}", ssot_path.display());
        println!("Found FSM definition: {}", ssot_path.display());

        // Parse the .ssot file
        let ssot_file_ast = match parse_file(&ssot_path) {
             Ok(ast) => {
                 println!("  -> Successfully parsed: {}", ssot_path.display());
                 ast
             }
             Err(e) => {
                 // Report the parsing error but continue to try parsing other files
                 eprintln!("Error parsing file {}: {}", ssot_path.display(), e);
                 // Convert the error and return to stop the build process
                 return Err(BuildError::DslParse {
                     path: ssot_path.clone(),
                     source: e,
                 });
                 // Or, if you want to allow the build to continue despite errors:
                 // continue;
             }
        };


        // --- Store the parsed AST ---
        // Use canonicalize to get an absolute, normalized path as the key
        let canonical_path = ssot_path.canonicalize().map_err(|e| io_err(&ssot_path, e))?;
        parsed_files.insert(canonical_path, ssot_file_ast);

        // --- Removed code generation logic for individual files ---
        // The logic for generating rust, capnp, ts, formatting, and mod.rs
        // for each machine within this file has been removed.
        // It will be replaced by a consolidated generation step after parsing all files.

    } // End of loop iterating through .ssot files

    println!(
        "Parsed {} .ssot file(s). Next steps: Resolve imports and generate code.",
        parsed_files.len()
    );

    // 2. --- Build Package Map ---
    println!("Building package map...");
    let mut package_map: HashMap<String, PathBuf> = HashMap::new();
    for (path, ast) in &parsed_files {
        if let Some(pkg_name) = &ast.package_declaration {
            if let Some(existing_path) = package_map.get(pkg_name) {
                // Found duplicate package declaration
                return Err(BuildError::DuplicatePackage {
                    package_name: pkg_name.clone(),
                    file1: existing_path.clone(),
                    file2: path.clone(),
                });
            }
            println!("  -> Mapping package '{}' to file {:?}", pkg_name, path.strip_prefix(&crate_path).unwrap_or(path));
            package_map.insert(pkg_name.clone(), path.clone());
        }
    }

    let context = ParsedContext {
        files: &parsed_files,
        package_map,
        // symbol_table: HashMap::new(), // Initialize later
    };

    println!("Package map built. Starting import resolution...");

    // 3. --- Resolve Imports and Build Symbol Table (Conceptual) ---
    //    This is where the main resolution logic will go.
    //    We need to iterate through machines, resolve 'use' statements based on imports,
    //    and then traverse the AST to resolve all QualifiedIdents.

    // Placeholder for resolved machines
    // let mut resolved_machines: Vec<ResolvedStateMachine> = Vec::new();

    for (file_path, file_ast) in context.files {
        let file_display_path = file_path.strip_prefix(&crate_path).unwrap_or(file_path);
        println!("Resolving symbols in file: {:?}", file_display_path);

        // Process imports for this file
        let mut imported_packages: HashMap<String, &SsotFile> = HashMap::new(); // Map alias/last part -> imported AST
        for import_decl in &file_ast.imports {
            let target_pkg_name = &import_decl.package_name;
            if let Some(target_file_path) = context.package_map.get(target_pkg_name) {
                 if let Some(target_ast) = context.files.get(target_file_path) {
                     // Simple mapping for now: use the last part of the package name as alias
                     // e.g., import test.fsm.common; -> alias "common" maps to target_ast
                     let alias = target_pkg_name.split('.').last().unwrap_or(target_pkg_name).to_string();
                     println!("  -> Imported package '{}' as alias '{}' from {:?}", target_pkg_name, alias, target_file_path.strip_prefix(&crate_path).unwrap_or(target_file_path));
                     imported_packages.insert(alias, target_ast);
                 } else {
                     // Should not happen if package_map is consistent with files
                      eprintln!("Internal error: File path from package_map not found in parsed_files.");
                 }
            } else {
                 return Err(BuildError::PackageNotFound {
                    package_name: target_pkg_name.clone(),
                    importer_file: file_path.clone(),
                });
            }
        }


        for machine in &file_ast.state_machines {
            println!("  Resolving machine: {}", machine.name);

            // Build local name resolution map based on 'use' statements
            let mut local_resolution_map: HashMap<String, String> = HashMap::new(); // SimpleName -> FullyQualifiedName
            for use_decl in &machine.use_declarations {
                match &use_decl.target {
                    QualifiedIdent::Simple(name) => {
                        // 'use SimpleName;' - needs searching through imports or local defs later
                        println!("    -> Found 'use {}' (simple) - Resolution deferred", name);
                        // Potentially ambiguous, handle later during symbol resolution
                    }
                    QualifiedIdent::Qualified { qualifier, name } => {
                        // 'use qualifier.Name;'
                        let qualifier_str = qualifier.to_string();
                        let name_str = name.to_string();
                        println!("    -> Found 'use {}.{}'", qualifier_str, name_str);

                        // Check if qualifier matches an imported package alias
                        if let Some(imported_ast) = imported_packages.get(&qualifier_str) {
                           // Construct the fully qualified name assuming the qualifier is the last part of the imported package
                            let full_qualifier = imported_ast.package_declaration.as_deref().unwrap_or("");
                            let fqn = format!("{}.{}", full_qualifier, name_str);
                             println!("      -> Resolved '{}' to FQN '{}'", name_str, fqn);
                             // Check for local name collision before inserting
                            if local_resolution_map.contains_key(&name_str) {
                                // Handle ambiguity/error
                                return Err(BuildError::AmbiguousSymbol { symbol_name: name_str.clone(), referencing_file: file_path.clone() });
                            }
                            local_resolution_map.insert(name_str, fqn);

                        } else {
                            // Qualifier doesn't match an import alias - potentially error or local nested structure?
                             println!("      -> Warning: Qualifier '{}' in 'use' statement does not match any imported package alias.", qualifier_str);
                             // For now, treat as unresolved
                        }
                    }
                }
            }
            println!("    -> Local Resolution Map: {:?}", local_resolution_map);


            // --- Placeholder: Traverse Machine AST (states, events, transitions) ---
            // For each QualifiedIdent:
            //   - If Qualified: Use the full name (qualifier.Name) - need to ensure qualifier maps to a valid package
            //   - If Simple:
            //     - Try resolving using local_resolution_map.
            //     - If not found locally, check if defined within the current machine (e.g., state name, local event).
            //     - If still not found, check imports without explicit 'use' (maybe?) or error.
            // Need to build the global symbol table first or resolve on the fly.

        } // End loop machines
    } // End loop files

    // --- Placeholder for Consolidated Code Generation ---
    // This step will now use the resolved information (e.g., `resolved_machines` or resolved ASTs)
    // to generate the final code.

    println!("Import resolution phase complete (basic structure). Next: Full symbol resolution and code generation.");


    // --- Removed mod.rs generation for now ---
    // Will be added back as part of the consolidated code generation step.


    Ok(())
}
