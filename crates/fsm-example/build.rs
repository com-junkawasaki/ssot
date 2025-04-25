use std::collections::HashMap;
use std::env;
use std::fs;
use std::path::PathBuf;

use fsm_codegen::{
    generate_capnp_schema, generate_rust_code, generate_scxml, generate_typescript_types,
    generate_xstate_machine, CodegenError,
};
use fsm_dsl::ast::{Annotation, AnnotationValue, SsotFile, StateMachine, TopLevelItem};
use fsm_dsl::parser::{parse_file, ParseError as DslParseError};
use thiserror::Error;

#[derive(Error, Debug)]
enum BuildError {
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
    #[error("Code generation error for state machine \'{machine_name}\' in file {path:?}: {source}")]
    Codegen {
        path: PathBuf,
        machine_name: String,
        #[source]
        source: CodegenError,
    },
    #[error(
        r#"Missing generation annotation ('$rust_out', '$capnp_out', etc.) in {path:?}"#
    )]
    MissingAnnotation { path: PathBuf, annotation_name: String },
    #[error("State machine definition not found in file {path:?}")]
    NoStateMachineFound { path: PathBuf },
    #[error("Multiple state machines found in file {path:?}. Example only supports one.")]
    MultipleStateMachines { path: PathBuf },
}

fn io_err(path: impl Into<PathBuf>, source: std::io::Error) -> BuildError {
    BuildError::Io {
        path: path.into(),
        source,
    }
}

fn find_annotation_str_value<'a>(annotations: &'a [Annotation], name: &str) -> Option<&'a str> {
    annotations
        .iter()
        .find(|a| a.name == name)
        .and_then(|a| match a.value.as_ref() {
            Some(AnnotationValue::StringLiteral(s)) => Some(s.as_str()),
            _ => None,
        })
}

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

fn main() -> Result<(), BuildError> {
    let crate_dir = env::var("CARGO_MANIFEST_DIR").unwrap();
    let crate_path = PathBuf::from(crate_dir);
    let out_dir = PathBuf::from(env::var("OUT_DIR").unwrap());

    let ssot_path = crate_path.join("spec").join("traffic_light.ssot");
    println!("cargo:rerun-if-changed={}", ssot_path.display());

    println!("Parsing FSM definition: {}", ssot_path.display());
    let file_ast = parse_file(&ssot_path).map_err(|e| BuildError::DslParse {
        path: ssot_path.clone(),
        source: e,
    })?;
    println!("  -> Successfully parsed: {}", ssot_path.display());

    let mut global_annotations: Vec<&Annotation> = Vec::new();
    let mut machine_ast: Option<&StateMachine> = None;

    for item in &file_ast.items {
        match item {
            TopLevelItem::Annotation(anno) => global_annotations.push(anno),
            TopLevelItem::StateMachine(machine) => {
                if machine_ast.is_some() {
                    return Err(BuildError::MultipleStateMachines {
                        path: ssot_path.clone(),
                    });
                }
                machine_ast = Some(machine);
            }
            TopLevelItem::StructDefinition(_) => {
                println!("  -> Ignoring top-level struct definition.");
            }
        }
    }

    let machine = machine_ast.ok_or(BuildError::NoStateMachineFound {
        path: ssot_path.clone(),
    })?;
    let machine_name_str = machine.name.to_string();
    println!("Found state machine: {}", machine_name_str);

    let combined_annotations: Vec<&Annotation> = global_annotations
        .iter()
        .cloned()
        .chain(machine.annotations.iter())
        .collect();

    let get_output_dir = |annotation_name: &str| {
        find_annotation_str_value(&combined_annotations, annotation_name).map(PathBuf::from)
    };

    let rust_out_dir = get_output_dir("rust_out").ok_or_else(|| BuildError::MissingAnnotation {
        path: ssot_path.clone(),
        annotation_name: "rust_out".to_string(),
    })?;
    let rust_code = generate_rust_code(machine).map_err(|e| BuildError::Codegen {
        path: ssot_path.clone(),
        machine_name: machine_name_str.clone(),
        source: e,
    })?;
    let rust_file_name = format!("{}.rs", camel_to_snake(&machine_name_str));
    let rust_out_path = out_dir.join(rust_file_name);
    fs::write(&rust_out_path, rust_code).map_err(|e| io_err(&rust_out_path, e))?;
    println!(
        "  -> Generated Rust code: {}",
        rust_out_path.strip_prefix(&out_dir).unwrap_or(&rust_out_path).display()
    );

    if rust_out_dir != out_dir {
        fs::create_dir_all(&rust_out_dir).map_err(|e| io_err(&rust_out_dir, e))?;
        let source_rust_out_path = rust_out_dir.join(format!("{}.rs", camel_to_snake(&machine_name_str)));
         fs::copy(&rust_out_path, &source_rust_out_path).map_err(|e| io_err(&source_rust_out_path, e))?;
         println!(
             "  -> Copied Rust code to: {}",
             source_rust_out_path.strip_prefix(&crate_path).unwrap_or(&source_rust_out_path).display()
         );
    }

    if let Some(capnp_out_dir) = get_output_dir("capnp_out") {
        fs::create_dir_all(&capnp_out_dir).map_err(|e| io_err(&capnp_out_dir, e))?;
        let capnp_schema =
            generate_capnp_schema(&file_ast, machine).map_err(|e| BuildError::Codegen {
                path: ssot_path.clone(),
                machine_name: machine_name_str.clone(),
                source: e,
            })?;
        let capnp_out_path = capnp_out_dir.join(format!("{}.capnp", machine_name_str));
        fs::write(&capnp_out_path, capnp_schema).map_err(|e| io_err(&capnp_out_path, e))?;
        println!(
            "  -> Generated Cap'n Proto schema: {}",
            capnp_out_path.strip_prefix(&crate_path).unwrap_or(&capnp_out_path).display()
        );
    }

    if let Some(ts_out_dir) = get_output_dir("ts_out") {
        fs::create_dir_all(&ts_out_dir).map_err(|e| io_err(&ts_out_dir, e))?;
        let ts_types =
            generate_typescript_types(machine).map_err(|e| BuildError::Codegen {
                path: ssot_path.clone(),
                machine_name: machine_name_str.clone(),
                source: e,
            })?;
        let ts_out_path = ts_out_dir.join(format!("{}.types.ts", machine_name_str));
        fs::write(&ts_out_path, ts_types).map_err(|e| io_err(&ts_out_path, e))?;
        println!(
            "  -> Generated TypeScript types: {}",
            ts_out_path.strip_prefix(&crate_path).unwrap_or(&ts_out_path).display()
        );
    }

     if let Some(xstate_out_dir) = get_output_dir("xstate_out") {
        fs::create_dir_all(&xstate_out_dir).map_err(|e| io_err(&xstate_out_dir, e))?;
        let xstate_config =
            generate_xstate_machine(machine).map_err(|e| BuildError::Codegen {
                path: ssot_path.clone(),
                machine_name: machine_name_str.clone(),
                source: e,
            })?;
        let xstate_out_path = xstate_out_dir.join(format!("{}.xstate.ts", machine_name_str));
        fs::write(&xstate_out_path, xstate_config).map_err(|e| io_err(&xstate_out_path, e))?;
        println!(
            "  -> Generated XState config: {}",
            xstate_out_path.strip_prefix(&crate_path).unwrap_or(&xstate_out_path).display()
        );
    }

    if let Some(scxml_out_dir) = get_output_dir("scxml_out") {
        fs::create_dir_all(&scxml_out_dir).map_err(|e| io_err(&scxml_out_dir, e))?;
        let scxml_doc = generate_scxml(machine).map_err(|e| BuildError::Codegen {
            path: ssot_path.clone(),
            machine_name: machine_name_str.clone(),
            source: e,
        })?;
        let scxml_out_path = scxml_out_dir.join(format!("{}.scxml", machine_name_str));
        fs::write(&scxml_out_path, scxml_doc).map_err(|e| io_err(&scxml_out_path, e))?;
        println!(
            "  -> Generated SCXML document: {}",
            scxml_out_path.strip_prefix(&crate_path).unwrap_or(&scxml_out_path).display()
        );
    }

    println!("Finished processing state machine: {}", machine_name_str);
    Ok(())
}
