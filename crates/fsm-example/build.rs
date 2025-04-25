use std::collections::HashMap;
use std::env;
use std::fs;
use std::path::{Path, PathBuf};
use std::process::Command; // To run rustfmt

use fsm_codegen::{generate_capnp_schema, generate_rust_code, generate_typescript_types};
// Use the new parser function and AST types
use fsm_dsl::ast::{
    Annotation, AnnotationValue, FieldDef, FieldType, Ident, ImportDeclaration, MessageItem,
    QualifiedIdent, SsotFile, StateItem, StateMachine, TransitionElement, TransitionItem, UseDeclaration,
};
use fsm_dsl::parser::{parse_file, ParseError as DslParseError};
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
        source: DslParseError, // Use the renamed error type
    },
    #[error("Code generation error for state machine '{machine_name}' in file {path:?}: {source}")]
    Codegen {
        path: PathBuf,
        machine_name: String,
        #[source]
        source: fsm_codegen::CodegenError,
    },
    #[error(r#"Missing '$rust_out("...")' annotation (top-level or per-machine) in {path:?}"#)]
    MissingRustOut { path: PathBuf },
    #[error("Duplicate package declaration '{package_name}' found in files {file1:?} and {file2:?}")]
    DuplicatePackage { package_name: String, file1: PathBuf, file2: PathBuf },
    #[error("Package '{package_name}' imported in {importer_file:?} not found.")]
    PackageNotFound { package_name: String, importer_file: PathBuf },
    #[error("Duplicate definition for symbol '{symbol_name}' found.")]
    DuplicateSymbol { symbol_name: String },
    #[error("Symbol '{symbol_name}' not found (referenced in {referencing_file:?} at {context_description})")]
    SymbolNotFound {
        symbol_name: String,
        referencing_file: PathBuf,
        context_description: String,
    },
    #[error("Ambiguous symbol '{symbol_name}' used in {referencing_file:?} at {context_description}.")]
    AmbiguousSymbol {
        symbol_name: String,
        referencing_file: PathBuf,
        context_description: String,
    },
    #[error("Circular dependency detected involving package '{package_name}'")]
    CircularDependency { package_name: String },
    #[error("Internal resolution error: {0}")]
    InternalResolution(String),
}

// Helper to wrap std::io::Error with path context
fn io_err(path: impl Into<PathBuf>, source: std::io::Error) -> BuildError {
    BuildError::Io {
        path: path.into(),
        source,
    }
}

// Helper to find annotation value by name from a slice
fn find_annotation_str_value<'a>(annotations: &'a [Annotation], name: &str) -> Option<&'a str> {
    annotations
        .iter()
        .find(|a| a.name == name)
        .and_then(|a| match a.value.as_ref() {
            Some(AnnotationValue::StringLiteral(s)) => Some(s.as_str()),
            _ => None,
        })
}

// Simple CamelCase to snake_case conversion
fn camel_to_snake(s: &str) -> String {
    if s.is_empty() { return String::new(); }
    let mut result = String::new();
    let mut chars = s.chars().peekable();
    let mut previous_was_uppercase = false;
    while let Some(c) = chars.next() {
        if c.is_uppercase() {
            if !result.is_empty() && !previous_was_uppercase { result.push('_'); }
            result.extend(c.to_lowercase());
            previous_was_uppercase = true;
        } else {
            result.push(c);
            previous_was_uppercase = false;
        }
    }
    result
}

fn main() -> Result<(), BuildError> {
    let crate_dir = env::var("CARGO_MANIFEST_DIR").unwrap();
    let crate_path = PathBuf::from(crate_dir);
    let spec_dir = crate_path.join("spec"); // Assuming spec files are in spec/
    let pattern = spec_dir.join("**/*.ssot");

    println!("cargo:rerun-if-changed={}", spec_dir.display());
    // Implicitly rerun if fsm-dsl or fsm-codegen changes due to dependency

    let mut parsed_files: HashMap<PathBuf, SsotFile> = HashMap::new();
    println!("Searching for FSM definitions in: {}", pattern.display());
    for entry in glob(pattern.to_str().unwrap())? {
        let ssot_path = entry?;
        println!("cargo:rerun-if-changed={}", ssot_path.display());
        println!("Found FSM definition: {}", ssot_path.display());
        let ssot_file_ast = match parse_file(&ssot_path) {
            Ok(ast) => { println!("  -> Successfully parsed: {}", ssot_path.display()); ast }
            Err(e) => { eprintln!("Error parsing file {}: {}", ssot_path.display(), e); return Err(BuildError::DslParse { path: ssot_path.clone(), source: e }); }
        };
        let canonical_path = ssot_path.canonicalize().map_err(|e| io_err(&ssot_path, e))?;
        parsed_files.insert(canonical_path, ssot_file_ast);
    }
    println!("Parsed {} .ssot file(s).", parsed_files.len());

    // 2. --- Build Package Map ---
    println!("Building package map...");
    let mut package_map: HashMap<String, PathBuf> = HashMap::new();
    for (path, ast) in &parsed_files {
        if let Some(pkg_name) = &ast.package_declaration {
            if let Some(existing_path) = package_map.get(pkg_name) {
                return Err(BuildError::DuplicatePackage { package_name: pkg_name.clone(), file1: existing_path.clone(), file2: path.clone() });
            }
            println!("  -> Mapping package '{}' to file {:?}", pkg_name, path.strip_prefix(&crate_path).unwrap_or(path));
            package_map.insert(pkg_name.clone(), path.clone());
        }
    }

    // 3. --- Build Global Symbol Table ---
    println!("Building global symbol table...");
    let mut symbol_table: HashMap<String, SymbolDefinition> = HashMap::new();
    for (file_path, file_ast) in &parsed_files {
        let package_prefix = file_ast.package_declaration.as_deref().unwrap_or("");
        for machine in &file_ast.state_machines {
            let machine_fqn = format!("{}.{}", package_prefix, machine.name);
            add_symbol(&mut symbol_table, machine_fqn.clone(), SymbolType::StateMachine, file_path, SymbolNodeRef::StateMachine(machine))?;
            for state in &machine.states {
                let state_fqn = format!("{}.{}", machine_fqn, state.name);
                add_symbol(&mut symbol_table, state_fqn.clone(), SymbolType::State, file_path, SymbolNodeRef::State(state))?;
                for action_ident in &state.entry_actions {
                    // Note: Storing actions/guards by a derived FQN including the ident string itself.
                    // This assumes actions/guards are globally unique if their names (including qualifiers) are unique.
                    let action_fqn = format!("{}.{}#entry.{}", machine_fqn, state.name, action_ident);
                    add_symbol(&mut symbol_table, action_fqn, SymbolType::Action, file_path, SymbolNodeRef::ActionIdent(action_ident))?;
                }
                for action_ident in &state.exit_actions {
                    let action_fqn = format!("{}.{}#exit.{}", machine_fqn, state.name, action_ident);
                    add_symbol(&mut symbol_table, action_fqn, SymbolType::Action, file_path, SymbolNodeRef::ActionIdent(action_ident))?;
                }
            }
            for event in &machine.events {
                let event_fqn = format!("{}.{}", machine_fqn, event.name);
                add_symbol(&mut symbol_table, event_fqn, SymbolType::Event, file_path, SymbolNodeRef::Event(event))?;
            }
            for transition in &machine.transitions {
                let trans_name = transition.name.as_ref().map(|n| n.to_string()).unwrap_or_else(|| format!("from_{}_to_{}", transition.from, transition.to));
                for element in &transition.elements {
                    match element {
                        TransitionElement::Guard { function, .. } => {
                            let guard_fqn = format!("{}#trans_{}#guard.{}", machine_fqn, trans_name, function);
                            add_symbol(&mut symbol_table, guard_fqn, SymbolType::Guard, file_path, SymbolNodeRef::GuardIdent(function))?;
                        }
                        TransitionElement::Action { function, .. } => {
                            let action_fqn = format!("{}#trans_{}#action.{}", machine_fqn, trans_name, function);
                            add_symbol(&mut symbol_table, action_fqn, SymbolType::Action, file_path, SymbolNodeRef::ActionIdent(function))?;
                        }
                        TransitionElement::On { .. } => {}
                    }
                }
            }
        }
    }
    println!("Global symbol table built. {} symbols found.", symbol_table.len());

    // --- Create Resolution Context ---
    let context = ResolutionContext {
        files: &parsed_files,
        package_map: &package_map,
        symbol_table,
    };

    // 4. --- Perform Full Resolution ---
    println!("Starting full symbol resolution...");
    for (file_path, file_ast) in context.files {
        let file_display_path = file_path.strip_prefix(&crate_path).unwrap_or(file_path);
        println!("Resolving identifiers in file: {:?}", file_display_path);

        // Build import alias map for the current file
        let import_aliases = build_import_alias_map(file_ast, file_path, &context)?;

        for machine in &file_ast.state_machines {
            println!("  Resolving identifiers in machine: {}", machine.name);
            let machine_fqn_prefix = format!(
                "{}.{}",
                file_ast.package_declaration.as_deref().unwrap_or(""),
                machine.name
            );

            // Build local resolution map from 'use' statements for this machine
            let local_resolution_map = build_local_resolution_map(
                machine,
                file_path,
                &import_aliases,
                &context,
            )?;

            // Resolve QualifiedIdents within the machine definition
            resolve_machine_identifiers(
                machine,
                file_path,
                &machine_fqn_prefix,
                &import_aliases,
                &local_resolution_map,
                &context,
            )?;
        } // End loop machines
    } // End loop files
    println!("Full symbol resolution complete.");

    // 5. --- Consolidated Code Generation ---
    println!("Placeholder: Consolidated code generation would happen here.");
    // Example: Iterate through resolved machines and generate code
    // let out_dir = crate_path.join("src/generated"); // Example output dir
    // fs::create_dir_all(&out_dir).map_err(|e| io_err(&out_dir, e))?;
    // let mut generated_mods = Vec::new();

    // for (_file_path, file_ast) in context.files {
    //     for machine in &file_ast.state_machines {
                // Here you would likely need a 'ResolvedMachine' struct or similar
                // containing the original AST plus resolved FQNs for identifiers.
                // Or, the codegen functions would need access to the `context`.

                // Assuming codegen takes context for resolution:
                // let generated_code = generate_rust_code(machine, &context)?; // Hypothetical signature
                // let machine_snake_name = camel_to_snake(&machine.name.to_string());
                // let out_file = out_dir.join(format!("{}.rs", machine_snake_name));
                // fs::write(&out_file, generated_code).map_err(|e| io_err(out_file, e))?;
                // generated_mods.push(machine_snake_name);
                // Run rustfmt if needed
    //     }
    // }
    // Generate mod.rs if needed

    println!("Build script finished successfully.");
    Ok(())
}

// --- Helper function to add symbols to the table ---
fn add_symbol<'a>(
    table: &mut HashMap<String, SymbolDefinition<'a>>, fqn: String, symbol_type: SymbolType,
    file_path: &'a PathBuf, node: SymbolNodeRef<'a>,
) -> Result<(), BuildError> {
    if table.contains_key(&fqn) { Err(BuildError::DuplicateSymbol { symbol_name: fqn }) }
    else { table.insert(fqn.clone(), SymbolDefinition { fqn, symbol_type, file_path, node }); Ok(()) }
}

// --- Helper function to build import alias map for a file ---
fn build_import_alias_map<'a, 'ctx>(
    file_ast: &'a SsotFile,
    file_path: &'a PathBuf,
    context: &'ctx ResolutionContext,
) -> Result<HashMap<String, &'a String>, BuildError> {
    let mut alias_map: HashMap<String, &String> = HashMap::new();
    for import_decl in &file_ast.imports {
        let target_pkg_name = &import_decl.package_name;
        if !context.package_map.contains_key(target_pkg_name) {
            return Err(BuildError::PackageNotFound {
                package_name: target_pkg_name.clone(),
                importer_file: file_path.clone(),
            });
        }
        let alias = target_pkg_name.split('.').last().unwrap_or(target_pkg_name).to_string();
        if let Some(existing_import) = alias_map.get(&alias) {
            return Err(BuildError::AmbiguousSymbol {
                symbol_name: alias,
                referencing_file: file_path.clone(),
                context_description: format!("Import alias conflicts with import of '{}'", existing_import),
            });
        }
        println!("    -> Import Alias Map (File: {:?}): '{}' -> '{}'", file_path.file_name().unwrap_or_default(), alias, target_pkg_name);
        alias_map.insert(alias, target_pkg_name);
    }
    Ok(alias_map)
}

// --- Helper function to build local resolution map for a machine ---
fn build_local_resolution_map<'a, 'ctx>(
    machine: &'a StateMachine,
    file_path: &'a PathBuf,
    import_aliases: &'ctx HashMap<String, &'a String>,
    context: &'ctx ResolutionContext,
) -> Result<HashMap<String, String>, BuildError> {
    let mut resolution_map: HashMap<String, String> = HashMap::new();
    let machine_name_str = machine.name.to_string();
    for use_decl in &machine.use_declarations {
        let context_desc = format!("'use' declaration in machine '{}'", machine_name_str);
        match &use_decl.target {
            QualifiedIdent::Simple(name) => {
                println!("    -> Found 'use {}' (simple) - Ambiguous, not supported yet.", name);
                return Err(BuildError::AmbiguousSymbol {
                    symbol_name: name.to_string(),
                    referencing_file: file_path.clone(),
                    context_description: context_desc,
                });
            }
            QualifiedIdent::Qualified { qualifier, name } => {
                let qualifier_str = qualifier.to_string();
                let name_str = name.to_string();
                if let Some(full_package_name) = import_aliases.get(&qualifier_str) {
                    let fqn = format!("{}.{}", full_package_name, name_str);
                    println!("    -> Use Decl Map (Machine: {}): Resolving simple name '{}' to FQN '{}'", machine_name_str, name_str, fqn);
                    if !context.symbol_table.contains_key(&fqn) {
                        return Err(BuildError::SymbolNotFound {
                            symbol_name: fqn,
                            referencing_file: file_path.clone(),
                            context_description: context_desc,
                        });
                    }
                    if let Some(existing_fqn) = resolution_map.get(&name_str) {
                        if *existing_fqn != fqn {
                            return Err(BuildError::AmbiguousSymbol {
                                symbol_name: name_str.clone(),
                                referencing_file: file_path.clone(),
                                context_description: format!("{} ('{}' already mapped to '{}')", context_desc, name_str, existing_fqn),
                            });
                        }
                    } else {
                        resolution_map.insert(name_str.clone(), fqn);
                    }
                } else {
                    return Err(BuildError::PackageNotFound {
                        package_name: qualifier_str,
                        importer_file: file_path.clone(),
                    });
                }
            }
        }
    }
    println!("    -> Local Resolution Map (Machine: {}): {:?}", machine_name_str, resolution_map);
    Ok(resolution_map)
}

// --- Function for resolving identifiers within a machine ---
fn resolve_machine_identifiers<'a, 'ctx>(
    machine: &'a StateMachine,
    file_path: &'a PathBuf,
    machine_fqn_prefix: &str,
    import_aliases: &'ctx HashMap<String, &'a String>,
    local_resolution_map: &'ctx HashMap<String, String>,
    context: &'ctx ResolutionContext,
) -> Result<(), BuildError> {
    println!("      -> Resolving identifiers for machine: {}", machine_fqn_prefix);

    // Resolve state entry/exit actions
    for state in &machine.states {
        let state_name = state.name.to_string();
        println!("        -> Resolving state: {}", state_name);
        for action_ident in &state.entry_actions {
            let context_desc = format!("state '{}' entry action", state_name);
            let fqn = resolve_identifier(action_ident, file_path, machine_fqn_prefix, import_aliases, local_resolution_map, context, &context_desc)?;
            println!("          -> Resolved entry action '{}' to '{}'", action_ident, fqn);
            // TODO: Check if resolved FQN corresponds to an Action symbol
        }
        for action_ident in &state.exit_actions {
            let context_desc = format!("state '{}' exit action", state_name);
            let fqn = resolve_identifier(action_ident, file_path, machine_fqn_prefix, import_aliases, local_resolution_map, context, &context_desc)?;
            println!("          -> Resolved exit action '{}' to '{}'", action_ident, fqn);
            // TODO: Check if resolved FQN corresponds to an Action symbol
        }
    }

    // Resolve event field types
    for event in &machine.events {
        let event_name = event.name.to_string();
        println!("        -> Resolving event: {}", event_name);
        for field in &event.fields {
            let field_name = field.name.to_string();
            println!("          -> Resolving field type: {}.{}", event_name, field_name);
            if let FieldType::Identifier(type_ident) = &field.field_type {
                let context_desc = format!("event '{}' field '{}' type", event_name, field_name);
                let fqn = resolve_identifier(type_ident, file_path, machine_fqn_prefix, import_aliases, local_resolution_map, context, &context_desc)?;
                println!("            -> Resolved type '{}' to '{}'", type_ident, fqn);
                // TODO: Check if resolved FQN corresponds to a valid Type symbol (once types are supported)
            }
        }
    }

    // Resolve transition elements (on event, guard, action)
    for transition in &machine.transitions {
        let trans_name = transition.name.as_ref().map(|n| n.to_string()).unwrap_or_else(|| format!("from_{}_to_{}", transition.from, transition.to));
        println!("        -> Resolving transition: {}", trans_name);
        for element in &transition.elements {
            match element {
                TransitionElement::On { event, .. } => {
                    let context_desc = format!("transition '{}' on event", trans_name);
                    println!("          -> Resolving 'on' event: {}", event);
                    let fqn = resolve_identifier(event, file_path, machine_fqn_prefix, import_aliases, local_resolution_map, context, &context_desc)?;
                    println!("            -> Resolved 'on' event '{}' to '{}'", event, fqn);
                    // TODO: Check if resolved FQN corresponds to an Event symbol
                }
                TransitionElement::Guard { function, .. } => {
                    let context_desc = format!("transition '{}' guard function", trans_name);
                    println!("          -> Resolving 'guard' function: {}", function);
                    let fqn = resolve_identifier(function, file_path, machine_fqn_prefix, import_aliases, local_resolution_map, context, &context_desc)?;
                    println!("            -> Resolved 'guard' function '{}' to '{}'", function, fqn);
                    // TODO: Check if resolved FQN corresponds to a Guard symbol
                }
                TransitionElement::Action { function, .. } => {
                    let context_desc = format!("transition '{}' action function", trans_name);
                    println!("          -> Resolving 'action' function: {}", function);
                    let fqn = resolve_identifier(function, file_path, machine_fqn_prefix, import_aliases, local_resolution_map, context, &context_desc)?;
                    println!("            -> Resolved 'action' function '{}' to '{}'", function, fqn);
                    // TODO: Check if resolved FQN corresponds to an Action symbol
                }
            }
        }
    }

    Ok(())
}

// --- Core Identifier Resolution Logic ---
fn resolve_identifier<'a, 'ctx>(
    ident: &'a QualifiedIdent,
    current_file: &'a PathBuf,
    current_machine_prefix: &str, // FQN prefix of the current machine (e.g., "pkg.Machine")
    import_aliases: &'ctx HashMap<String, &'a String>, // Alias -> Full Pkg Name for current file
    local_resolution_map: &'ctx HashMap<String, String>, // Simple Name -> FQN from 'use' for current machine
    context: &'ctx ResolutionContext,
    context_description: &str, // Description of where the identifier is used (for errors)
) -> Result<String, BuildError> // Returns the resolved FQN
{
    match ident {
        QualifiedIdent::Simple(name) => {
            let name_str = name.to_string();
            println!("            -> Resolving simple: '{}' (Context: {})", name_str, context_description);
            // 1. Check local 'use' map
            if let Some(fqn) = local_resolution_map.get(&name_str) {
                println!("              -> Resolved via 'use' map to: {}", fqn);
                // Symbol existence already checked when building the map
                return Ok(fqn.clone());
            }

            // 2. Check if defined within the current machine (State, Event)
            //    Or potentially actions/guards associated with this machine scope
            //    Need a reliable way to construct potential FQNs for local symbols.
            //    Using the symbol table's derived FQNs for actions/guards might be tricky.
            //    Let's prioritize checking for States and Events defined directly in the machine first.
            let potential_local_fqn_state = format!("{}.{}", current_machine_prefix, name_str);
            let potential_local_fqn_event = format!("{}.{}", current_machine_prefix, name_str);

            if let Some(def) = context.symbol_table.get(&potential_local_fqn_state) {
                if def.symbol_type == SymbolType::State {
                    println!("              -> Resolved as local state: {}", potential_local_fqn_state);
                    return Ok(potential_local_fqn_state);
                }
            }
            if let Some(def) = context.symbol_table.get(&potential_local_fqn_event) {
                if def.symbol_type == SymbolType::Event {
                    println!("              -> Resolved as local event: {}", potential_local_fqn_event);
                    return Ok(potential_local_fqn_event);
                }
            }
            // How to check for local actions/guards without knowing their exact FQN derivation?
            // Maybe iterate symbols starting with `current_machine_prefix`? Less efficient.
            // Or assume simple actions/guards are defined externally and don't need local resolution here? Needs clarification.
            // For now, we only resolve local states and events.

            // 3. Check if defined in the current *package* but outside the machine? (Future feature)

            // 4. Not found
            println!("              -> Failed to resolve simple ident '{}' locally or via 'use'.", name_str);
            Err(BuildError::SymbolNotFound {
                symbol_name: name_str,
                referencing_file: current_file.clone(),
                context_description: context_description.to_string(),
            })
        }
        QualifiedIdent::Qualified { qualifier, name } => {
            let qualifier_str = qualifier.to_string();
            let name_str = name.to_string();
            println!("            -> Resolving qualified: '{}.{}' (Context: {})", qualifier_str, name_str, context_description);

            // Qualifier must resolve to a known package alias via imports for the current file
            if let Some(full_package_name) = import_aliases.get(&qualifier_str) {
                let fqn = format!("{}.{}", full_package_name, name_str);
                println!("              -> Constructed FQN from alias: {}", fqn);
                // Now check if this FQN exists in the global symbol table
                if context.symbol_table.contains_key(&fqn) {
                    println!("              -> Found in symbol table.");
                    return Ok(fqn);
                } else {
                    // Check if the FQN might refer to an action/guard derived name
                    // This part is complex because the derived FQN for actions/guards in the symbol table includes context (#...)
                    // Simple check: iterate symbol table keys matching the base FQN? Inefficient.
                    // Alternative: Assume qualified names ONLY refer to top-level items (States, Events, Machines, Types) for now.
                    println!("              -> FQN '{}' not found in symbol table (or is not a top-level symbol).", fqn);
                    return Err(BuildError::SymbolNotFound {
                        symbol_name: fqn, // Report the expected FQN
                        referencing_file: current_file.clone(),
                        context_description: context_description.to_string(),
                    });
                }
            } else {
                // The qualifier used (e.g., `common` in `common.Start`) is not an alias defined by `import` in this file.
                println!("              -> Qualifier '{}' is not a known import alias in this file ({:?}).", qualifier_str, current_file.file_name().unwrap_or_default());
                // This implies it might be a reference to a nested structure or an error.
                // Treat as package/alias not found.
                return Err(BuildError::PackageNotFound {
                    package_name: qualifier_str, // The qualifier string used as alias/package name
                    importer_file: current_file.clone(), // The file where the reference occurs
                });
            }
        }
    }
}
