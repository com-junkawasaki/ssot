#![allow(dead_code)] // Allow dead code for now as AST is built incrementally

// Make the Ident import public
pub use proc_macro2::Ident;

// --- Annotations ---

/// Represents an annotation attached to various elements in the `.ssot` file.
///
/// Annotations provide metadata or configuration, typically prefixed with `$`.
/// Examples: `$description("Machine description");`, `$initial`, `$id(123)`.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct Annotation {
    /// The name of the annotation (e.g., `description`, `initial`, `id`).
    pub name: Ident,
    /// The optional value associated with the annotation.
    pub value: Option<AnnotationValue>,
}

/// Represents the possible value types within an annotation.
#[derive(Debug, Clone, PartialEq, Eq)]
pub enum AnnotationValue {
    /// A string literal value, e.g., `"some text"`.
    StringLiteral(String),
    /// A boolean literal value, e.g., `true` or `false`.
    BooleanLiteral(bool),
    /// An integer literal value, e.g., `123`.
    NumberLiteral(i64),
    /// An identifier used as a value, potentially referencing another element.
    Identifier(Ident),
    /// An array of string literals, e.g., `["derive1", "derive2"]`. Used for annotations like `$derive`.
    ArrayLiteral(Vec<String>),
}

// --- Types for Payloads and Values ---

/// Defines the possible data types for fields within events or context, mirroring Cap'n Proto types.
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub enum FieldType {
    /// Represents the absence of a value (similar to `()` in Rust).
    Void,
    /// A boolean value (`true` or `false`).
    Bool,
    /// An 8-bit signed integer.
    Int8,
    /// A 16-bit signed integer.
    Int16,
    /// A 32-bit signed integer.
    Int32,
    /// A 64-bit signed integer.
    Int64,
    /// An 8-bit unsigned integer.
    UInt8,
    /// A 16-bit unsigned integer.
    UInt16,
    /// A 32-bit unsigned integer.
    UInt32,
    /// A 64-bit unsigned integer.
    UInt64,
    /// A 32-bit floating-point number.
    Float32,
    /// A 64-bit floating-point number.
    Float64,
    /// A UTF-8 encoded string.
    Text,
    /// Arbitrary binary data.
    Data,
    /// A list containing elements of the specified type.
    List(Box<FieldType>),
    /// An identifier referencing a user-defined struct or enum (requires resolution during code generation).
    Identifier(Ident),
}

// --- File Structure ---

/// Represents the root Abstract Syntax Tree (AST) node for a parsed `.ssot` file.
///
/// Contains the overall structure including file ID, package declaration,
/// top-level annotations, and state machine definitions.
#[derive(Debug, Clone, PartialEq)]
pub struct SsotFile {
    /// The Cap'n Proto schema file ID, specified like `@0x123456789abcdef0;`.
    pub file_id: u64,
    /// An optional package declaration, e.g., `package com.example.fsm;`.
    pub package_declaration: Option<String>,
    /// Annotations defined at the top level of the file, applying globally or to the generation process.
    pub top_level_annotations: Vec<Annotation>, // e.g., $rust_out, $derive
    /// The list of state machines defined within the file.
    pub state_machines: Vec<StateMachine>,
}

// --- State Machine ---

/// Represents a single `state_machine` definition within the `.ssot` file.
///
/// Defines the core components of a state machine: its name, states, events,
/// transitions, context data, and associated annotations.
#[derive(Debug, Clone, PartialEq)]
pub struct StateMachine {
    /// The name identifier of the state machine.
    pub name: Ident,
    /// Annotations specific to this state machine, like `$description` or `$initial`.
    pub annotations: Vec<Annotation>, // Includes $description, $initial etc.
    /// The set of possible states the machine can be in.
    pub states: Vec<StateItem>,
    /// The set of events (messages) that can trigger transitions or be processed by the machine.
    pub events: Vec<MessageItem>, // Renamed from messages to events
    /// The defined transitions between states, triggered by events.
    pub transitions: Vec<TransitionItem>,
    /// The data fields representing the internal context or memory of the state machine.
    pub context: Vec<FieldDef>, // Added: Context fields for the state machine
}

// --- States ---

/// Represents a single state defined within the `states` block of a state machine.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct StateItem {
    /// Annotations specific to this state.
    pub annotations: Vec<Annotation>, // Added annotations
    /// The name identifier of the state.
    pub name: Ident,
    /// The ordinal value (like `@0`) associated with the state, used for serialization/identification.
    pub ordinal: u64,
    /// A list of action identifiers to be executed when entering this state.
    pub entry_actions: Vec<Ident>, // Added: Actions to execute on entry
    /// A list of action identifiers to be executed when exiting this state.
    pub exit_actions: Vec<Ident>,  // Added: Actions to execute on exit
}

// --- Events (Messages) ---

/// Represents an event (or message) definition within the `events` block.
///
/// Events typically carry data payloads (fields) and trigger state transitions.
#[derive(Debug, Clone, PartialEq)]
pub struct MessageItem {
    /// Annotations specific to this event definition.
    pub annotations: Vec<Annotation>, // Added annotations
    /// The name identifier of the event struct.
    pub name: Ident,
    /// The ordinal value (`@N`) associated with the event struct.
    pub ordinal: u64,
    /// The data fields contained within this event.
    pub fields: Vec<FieldDef>,
}

/// Represents a single field within an event definition or the state machine's context.
#[derive(Debug, Clone, PartialEq)]
pub struct FieldDef {
    /// Annotations specific to this field.
    pub annotations: Vec<Annotation>, // Added annotations
    /// The name identifier of the field.
    pub name: Ident,
    /// The ordinal value (`@N`) associated with the field within its containing struct.
    pub ordinal: u64,
    /// The data type of the field.
    pub field_type: FieldType,
}

// --- Transitions ---

/// Represents a state transition rule defined within the `transitions` block.
///
/// Specifies how the state machine moves from one state (`from`) to another (`to`)
/// based on triggers (`on`), conditions (`guard`), and actions (`action`).
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct TransitionItem {
    /// An optional name for the transition, useful for identification or documentation.
    pub name: Option<Ident>, // Added optional name
    /// The source state identifier for this transition.
    pub from: Ident,
    /// The target state identifier for this transition.
    pub to: Ident,
    /// The components defining the transition's trigger, condition, and effect.
    pub elements: Vec<TransitionElement>, // on, guard, action
    /// Annotations specific to this transition, like `$id`.
    pub annotations: Vec<Annotation>,     // e.g., $id(...)
}

/// Represents the constituent parts of a transition definition (`on`, `guard`, `action`).
#[derive(Debug, Clone, PartialEq, Eq)]
pub enum TransitionElement {
    /// Specifies the event that triggers the transition.
    On {
        /// The ordinal of the `on` element within the transition block.
        ordinal: u64,
        /// The identifier of the triggering event.
        event: Ident
    },
    /// Specifies a condition (guard function) that must be true for the transition to occur.
    Guard {
        /// The ordinal of the `guard` element within the transition block.
        ordinal: u64,
        /// The identifier of the guard function.
        function: Ident
    },
    /// Specifies an action (function) to be executed when the transition occurs.
    Action {
        /// The ordinal of the `action` element within the transition block.
        ordinal: u64,
        /// The identifier of the action function.
        function: Ident
    },
}

// --- Utility ---

// Placeholder for unescaping string literals if needed by the parser
// pub(crate) fn unescape_string(s: &str) -> String {
//     // Basic unescaping for quotes and backslashes
//     s.trim_start_matches('"')
//      .trim_end_matches('"')
//      .replace("\\\"", "\"")
//      .replace("\\\\", "\\")
// }
