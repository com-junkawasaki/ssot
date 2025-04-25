use std::env;
use std::fs;
use std::path::PathBuf;
use std::process::Command; // To run rustfmt

use fsm_codegen::{generate_capnp_schema, generate_rust_code, generate_typescript_types};
// Use the new parser function and AST types
use fsm_dsl::ast::{Annotation, AnnotationValue, SsotFile};
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

fn main() -> Result<(), BuildError> {
    let crate_dir = env::var("CARGO_MANIFEST_DIR").unwrap();
    let crate_path = PathBuf::from(crate_dir);
    let spec_dir = crate_path.join("spec"); // Assuming spec files are in spec/
    let pattern = spec_dir.join("**/*.ssot");

    println!("cargo:rerun-if-changed={}", spec_dir.display());
    // Implicitly rerun if fsm-dsl or fsm-codegen changes due to dependency

    let mut generated_mod_files = Vec::new();

    for entry in glob(pattern.to_str().unwrap())? {
        let ssot_path = entry?;
        println!("cargo:rerun-if-changed={}", ssot_path.display());
        println!("Processing FSM definition: {}", ssot_path.display());

        // Parse the entire .ssot file using the new parser
        let ssot_file_ast: SsotFile = parse_file(&ssot_path).map_err(|e| BuildError::DslParse {
            path: ssot_path.clone(),
            source: e,
        })?;

        // Determine the base output directory (can be overridden per machine)
        let top_level_rust_out =
            find_annotation_str_value(&ssot_file_ast.top_level_annotations, "rust_out");
        // Look for top-level Cap'n Proto and TS output annotations
        let top_level_capnp_out =
            find_annotation_str_value(&ssot_file_ast.top_level_annotations, "capnp_out");
        let top_level_ts_out =
            find_annotation_str_value(&ssot_file_ast.top_level_annotations, "ts_out");

        // Process each state machine defined in the file
        for machine_ast in &ssot_file_ast.state_machines {
            let machine_name_str = machine_ast.name.to_string();

            // Determine output dir: use machine-specific $rust_out or fallback to top-level
            let rust_out_dir_str = find_annotation_str_value(&machine_ast.annotations, "rust_out")
                .or(top_level_rust_out)
                .ok_or_else(|| BuildError::MissingRustOut {
                    path: ssot_path.clone(),
                })?;

            let out_dir_path = crate_path.join(rust_out_dir_str);
            fs::create_dir_all(&out_dir_path).map_err(|e| io_err(&out_dir_path, e))?;

            // Generate filename based on state machine name (snake_case)
            let machine_name_snake = camel_to_snake(&machine_name_str);
            let out_file_path = out_dir_path.join(format!("{}.rs", machine_name_snake));

            println!(
                "Generating Rust code for '{}' to: {}",
                machine_name_str,
                out_file_path.display()
            );

            // Generate code for the current state machine
            let generated_code =
                generate_rust_code(machine_ast).map_err(|e| BuildError::Codegen {
                    path: ssot_path.clone(),
                    machine_name: machine_name_str.clone(),
                    source: e,
                })?;

            fs::write(&out_file_path, &generated_code).map_err(|e| io_err(&out_file_path, e))?;

            // Store the stem for mod.rs generation
            if out_dir_path == crate_path.join("src/generated") {
                // Only auto-gen mod.rs for standard path
                generated_mod_files.push(machine_name_snake.clone());
            }

            // Attempt to format the generated code using rustfmt
            match Command::new("rustfmt").arg(&out_file_path).output() {
                Ok(output) => {
                    if !output.status.success() {
                        eprintln!(
                            "warning: Failed to format generated code {}: {}",
                            out_file_path.display(),
                            String::from_utf8_lossy(&output.stderr)
                        );
                        // Optionally treat as hard error
                    }
                }
                Err(e) => {
                    eprintln!(
                         "warning: Failed to run rustfmt for {}: {}. Ensure rustfmt is installed and in PATH.",
                         out_file_path.display(),
                         e
                     );
                }
            }

            // --- Cap'n Proto Schema Generation (Optional) ---
            let capnp_out_dir_str =
                find_annotation_str_value(&machine_ast.annotations, "capnp_out")
                    .or(top_level_capnp_out);

            if let Some(capnp_out_dir_str) = capnp_out_dir_str {
                let out_dir_path = crate_path.join(capnp_out_dir_str);
                fs::create_dir_all(&out_dir_path).map_err(|e| io_err(&out_dir_path, e))?;
                // Use original machine name for capnp file
                let out_file_path = out_dir_path.join(format!("{}.capnp", machine_name_str));

                println!(
                    "Generating Cap'n Proto schema for '{}' to: {}",
                    machine_name_str,
                    out_file_path.display()
                );

                let generated_schema =
                    generate_capnp_schema(&ssot_file_ast, machine_ast).map_err(|e| {
                        BuildError::Codegen {
                            path: ssot_path.clone(),
                            machine_name: machine_name_str.clone(),
                            source: e,
                        }
                    })?;

                fs::write(&out_file_path, &generated_schema)
                    .map_err(|e| io_err(&out_file_path, e))?;
                // No formatting needed for .capnp usually
            }

            // --- TypeScript Type Generation (Optional) ---
            let ts_out_dir_str =
                find_annotation_str_value(&machine_ast.annotations, "ts_out").or(top_level_ts_out);

            if let Some(ts_out_dir_str) = ts_out_dir_str {
                let out_dir_path = crate_path.join(ts_out_dir_str);
                fs::create_dir_all(&out_dir_path).map_err(|e| io_err(&out_dir_path, e))?;
                // Use original machine name + .types.ts convention
                let out_file_path = out_dir_path.join(format!("{}.types.ts", machine_name_str));

                println!(
                    "Generating TypeScript types for '{}' to: {}",
                    machine_name_str,
                    out_file_path.display()
                );

                let generated_types =
                    generate_typescript_types(machine_ast).map_err(|e| BuildError::Codegen {
                        path: ssot_path.clone(),
                        machine_name: machine_name_str.clone(),
                        source: e,
                    })?;

                fs::write(&out_file_path, &generated_types)
                    .map_err(|e| io_err(&out_file_path, e))?;

                // Optional: Run Prettier or other TS formatter
                match Command::new("prettier")
                    .arg("--write")
                    .arg(&out_file_path)
                    .output()
                {
                    Ok(output) => {
                        if !output.status.success() {
                            eprintln!(
                                "warning: Failed to format generated TypeScript {}: {}",
                                out_file_path.display(),
                                String::from_utf8_lossy(&output.stderr)
                            );
                        }
                    }
                    Err(e) => {
                        eprintln!(
                              "warning: Failed to run prettier for {}: {}. Ensure prettier is installed and in PATH.",
                              out_file_path.display(),
                              e
                          );
                    }
                }
            }
        }
    }

    // Generate src/generated/mod.rs if needed
    if !generated_mod_files.is_empty() {
        let generated_mod_path = crate_path.join("src/generated/mod.rs");
        let mod_content = generated_mod_files
            .iter()
            .map(|mod_name| format!("pub mod {};", mod_name))
            .collect::<Vec<_>>()
            .join("\n");

        fs::write(&generated_mod_path, mod_content).map_err(|e| io_err(generated_mod_path, e))?;
        println!("Generated src/generated/mod.rs");
    }

    Ok(())
}

// Simple CamelCase to snake_case conversion
fn camel_to_snake(s: &str) -> String {
    let mut snake = String::new();
    let mut prev_is_underscore = true; // Avoid leading underscore
    for ch in s.chars() {
        if ch.is_uppercase() {
            if !prev_is_underscore {
                snake.push('_');
            }
            snake.push(ch.to_ascii_lowercase());
            prev_is_underscore = true;
        } else {
            snake.push(ch);
            prev_is_underscore = ch == '_';
        }
    }
    snake
}
