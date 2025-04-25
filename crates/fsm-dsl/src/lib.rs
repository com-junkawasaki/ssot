// crates/fsm-dsl/src/lib.rs
// This file should only expose the necessary modules.

//! # Finite State Machine DSL (`fsm-dsl`)
//!
//! This crate provides the parser and Abstract Syntax Tree (AST) definitions
//! for the `.ssot` (Single Source of Truth) file format, designed for defining
//! state machines.
//!
//! The primary goal is to parse `.ssot` files into a structured Rust representation (`ast::SsotFile`)
//! which can then be used by code generation tools (like `fsm-codegen`) to produce
//! executable state machine code in various languages.
//!
//! ## Core Components
//!
//! *   **`ast` module:** Defines the Rust structs and enums representing the parsed elements
//!     of a `.ssot` file (state machines, states, events, transitions, annotations, etc.).
//! *   **`parser` module:** Contains the `pest`-based parser logic (`parser::parse_file`,
//!     `parser::parse_str`) and the `ParseError` enum for handling parsing failures.
//! *   **`ssot.pest` file:** (Not directly in this module, but crucial) Defines the formal
//!     grammar used by the parser.
//!
//! ## Usage
//!
//! The main way to use this crate is to call the parsing functions:
//!
//! ```no_run
//! use fsm_dsl::parser::{parse_file, ParseError};
//! use fsm_dsl::ast::SsotFile;
//!
//! fn main() -> Result<(), ParseError> {
//!     let ssot_ast: SsotFile = parse_file("path/to/your/machine.ssot")?;
//!     println!("Successfully parsed state machine: {}", ssot_ast.state_machines[0].name);
//!     // ... further processing or code generation ...
//!     Ok(())
//! }
//! ```
//! Or using the re-exported items:
//! ```no_run
//! use fsm_dsl::{parse_str, SsotFile, ParseError};
//!
//! fn main() -> Result<(), ParseError> {
//!     let content = "@0x...; state_machine M { ... }"; // Simplified content
//!     let ssot_ast: SsotFile = parse_str(content)?;
//!     println!("Parsed machine: {}", ssot_ast.state_machines[0].name);
//!     Ok(())
//! }
//! ```

pub mod ast;
pub mod parser;

// Re-export key items for convenience
pub use ast::{
    Annotation, AnnotationValue, FieldDef, FieldType, MessageItem, SsotFile, StateItem,
    StateMachine, TransitionElement, TransitionItem,
};
pub use parser::{parse_file, parse_str, ParseError};
