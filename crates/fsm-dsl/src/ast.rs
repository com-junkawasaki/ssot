#![allow(dead_code)] // Allow dead code for now as AST is built incrementally

// Make the Ident import public
pub use proc_macro2::Ident;

// --- Annotations ---

/// Represents an annotation like `$name(value);`
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct Annotation {
    pub name: Ident,
    pub value: Option<AnnotationValue>,
}

/// Represents the possible values within an annotation.
#[derive(Debug, Clone, PartialEq, Eq)]
pub enum AnnotationValue {
    StringLiteral(String),
    BooleanLiteral(bool),
    NumberLiteral(i64), // Assuming integer numbers for now
    Identifier(Ident),
    ArrayLiteral(Vec<String>), // Assuming array of strings for now (e.g., for derives)
}

// --- Types for Payloads and Values ---

/// Represents a type used in event fields (Cap'n Proto style).
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub enum FieldType {
    Void,
    Bool,
    Int8,
    Int16,
    Int32,
    Int64,
    UInt8,
    UInt16,
    UInt32,
    UInt64,
    Float32,
    Float64,
    Text,                 // UTF-8 String
    Data,                 // Vec<u8>
    List(Box<FieldType>), // List(T)
    Identifier(Ident),    // Reference to a user-defined struct/enum (requires resolution later)
}

// --- File Structure ---

/// Represents the entire parsed content of a .ssot file.
#[derive(Debug, Clone, PartialEq)]
pub struct SsotFile {
    pub file_id: u64,                           // Cap'n Proto style file ID
    pub package_declaration: Option<String>,    // Keep as string for now
    pub top_level_annotations: Vec<Annotation>, // e.g., $rust_out, $derive
    pub state_machines: Vec<StateMachine>,
}

// --- State Machine ---

/// Represents a `state_machine` definition.
#[derive(Debug, Clone, PartialEq)]
pub struct StateMachine {
    pub name: Ident,
    pub annotations: Vec<Annotation>, // Includes $description, $initial etc.
    pub states: Vec<StateItem>,
    pub events: Vec<MessageItem>, // Renamed from messages to events
    pub transitions: Vec<TransitionItem>,
    pub context: Vec<FieldDef>, // Added: Context fields for the state machine
}

// --- States ---

/// Represents a state variant within the `enum State` block.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct StateItem {
    pub annotations: Vec<Annotation>, // Added annotations
    pub name: Ident,
    pub ordinal: u64,
    pub entry_actions: Vec<Ident>, // Added: Actions to execute on entry
    pub exit_actions: Vec<Ident>,  // Added: Actions to execute on exit
    // pub annotations: Vec<Annotation>, // Future: Annotations on states?
}

// --- Events (Messages) ---

/// Represents an event struct defined within the `events` block.
#[derive(Debug, Clone, PartialEq)]
pub struct MessageItem {
    pub annotations: Vec<Annotation>, // Added annotations
    pub name: Ident,
    pub ordinal: u64,
    pub fields: Vec<FieldDef>,
    // pub annotations: Vec<Annotation>, // Future: Annotations on events?
}

/// Represents a field within an event struct.
#[derive(Debug, Clone, PartialEq)]
pub struct FieldDef {
    pub annotations: Vec<Annotation>, // Added annotations
    pub name: Ident,
    pub ordinal: u64,
    pub field_type: FieldType,
    // pub annotations: Vec<Annotation>, // Future: Annotations on fields?
}

// --- Transitions ---

/// Represents a `transition` definition.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct TransitionItem {
    pub name: Option<Ident>, // Added optional name
    pub from: Ident,
    pub to: Ident,
    pub elements: Vec<TransitionElement>, // on, guard, action
    pub annotations: Vec<Annotation>,     // e.g., $id(...)
}

/// Represents elements within a transition block (`on`, `guard`, `action`).
#[derive(Debug, Clone, PartialEq, Eq)]
pub enum TransitionElement {
    On { ordinal: u64, event: Ident },
    Guard { ordinal: u64, function: Ident },
    Action { ordinal: u64, function: Ident },
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
