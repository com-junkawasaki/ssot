#![allow(dead_code)] // Allow dead code for now as AST is built incrementally

// Make the Ident import public
pub use proc_macro2::Ident;

// --- New: Qualified Identifier ---

/// Represents an identifier, potentially qualified with a namespace/package alias.
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub enum QualifiedIdent {
    /// A simple, unqualified identifier (e.g., `MyEvent`).
    Simple(Ident),
    /// A qualified identifier (e.g., `common.MyEvent`).
    Qualified {
        /// The namespace or package alias (e.g., `common`).
        qualifier: Ident,
        /// The actual identifier name (e.g., `MyEvent`).
        name: Ident,
    },
}

// Implement Display for easier use in messages/debugging
impl std::fmt::Display for QualifiedIdent {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        match self {
            QualifiedIdent::Simple(ident) => write!(f, "{}", ident),
            QualifiedIdent::Qualified { qualifier, name } => write!(f, "{}.{}", qualifier, name),
        }
    }
}

// --- Annotations ---

/// Represents an annotation attached to various elements in the `.ssot` file.
///
/// Annotations provide metadata or configuration, typically prefixed with `$`.
/// Examples: `$description("Machine description");`, `$initial`, `$id(123)`.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct Annotation {
    /// The name of the annotation (e.g., `description`, `initial`, `id`).
    /// This corresponds to the part after the `$` symbol.
    pub name: Ident,
    /// The optional value associated with the annotation, enclosed in parentheses.
    /// If the parentheses are omitted, the value is `None`.
    pub value: Option<AnnotationValue>,
}

/// Represents the possible value types within an annotation's parentheses.
#[derive(Debug, Clone, PartialEq, Eq)]
pub enum AnnotationValue {
    /// A string literal value, enclosed in double quotes (e.g., `"some text"`).
    StringLiteral(String),
    /// A boolean literal value (`true` or `false`).
    BooleanLiteral(bool),
    /// An integer literal value (e.g., `123`, `-45`).
    NumberLiteral(i64),
    /// An identifier used as a value, potentially referencing another definition (e.g., a state name in `$initial(StateName)`).
    Identifier(Ident),
    /// An array of string literals, enclosed in square brackets (e.g., `["derive1", "derive2"]`).
    /// Typically used for annotations like `$derive`.
    ArrayLiteral(Vec<String>),
}

// --- Types for Payloads and Values ---

/// Defines the possible data types for fields within events or context.
///
/// These types directly correspond to the primitive types available in Cap'n Proto,
/// ensuring compatibility when generating Cap'n Proto schemas.
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub enum FieldType {
    /// Represents the absence of a value (similar to `()` in Rust or `Void` in Cap'n Proto).
    Void,
    /// A boolean value (`true` or `false`). Corresponds to `Bool` in Cap'n Proto.
    Bool,
    /// An 8-bit signed integer. Corresponds to `Int8` in Cap'n Proto.
    Int8,
    /// A 16-bit signed integer. Corresponds to `Int16` in Cap'n Proto.
    Int16,
    /// A 32-bit signed integer. Corresponds to `Int32` in Cap'n Proto.
    Int32,
    /// A 64-bit signed integer. Corresponds to `Int64` in Cap'n Proto.
    Int64,
    /// An 8-bit unsigned integer. Corresponds to `UInt8` in Cap'n Proto.
    UInt8,
    /// A 16-bit unsigned integer. Corresponds to `UInt16` in Cap'n Proto.
    UInt16,
    /// A 32-bit unsigned integer. Corresponds to `UInt32` in Cap'n Proto.
    UInt32,
    /// A 64-bit unsigned integer. Corresponds to `UInt64` in Cap'n Proto.
    UInt64,
    /// A 32-bit floating-point number. Corresponds to `Float32` in Cap'n Proto.
    Float32,
    /// A 64-bit floating-point number. Corresponds to `Float64` in Cap'n Proto.
    Float64,
    /// A UTF-8 encoded string. Corresponds to `Text` in Cap'n Proto.
    Text,
    /// Arbitrary binary data. Corresponds to `Data` in Cap'n Proto.
    Data,
    /// A list containing elements of the specified inner type. Corresponds to `List(T)` in Cap'n Proto.
    /// (Note: DSL syntax for lists might still be under development).
    List(Box<FieldType>),
    /// An identifier referencing a user-defined struct or enum defined elsewhere.
    /// This could be simple or qualified (e.g., `MyStruct`, `common.OtherStruct`).
    /// Requires resolution during code generation.
    Identifier(QualifiedIdent),
}

// --- File Structure ---

/// Represents a single import declaration at the top level of an .ssot file.
/// e.g., `import my.package.name;`
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct ImportDeclaration {
    /// The full name of the package being imported (e.g., "my.package.name").
    /// Parsing needs to handle the dot-separated structure.
    pub package_name: String, // Using String to easily store dot-separated names
}

/// Represents the root Abstract Syntax Tree (AST) node for a parsed `.ssot` file.
///
/// This is the top-level container holding all information parsed from a single file.
#[derive(Debug, Clone, PartialEq)]
pub struct SsotFile {
    /// The unique Cap'n Proto schema file ID, specified at the top of the file (e.g., `@0x123456789abcdef0;`).
    /// Required for Cap'n Proto schema generation.
    pub file_id: u64,
    /// An optional package declaration (e.g., `package com.example.fsm;`).
    /// Primarily relevant for organizing generated code in some target languages.
    pub package_declaration: Option<String>,
    /// Import declarations at the top level (e.g., `import other.package;`)
    pub imports: Vec<ImportDeclaration>,
    /// Annotations defined at the top level of the file, before any `stateMachine` definitions.
    /// These often provide global configuration for code generation (e.g., `$rust_out`).
    pub top_level_annotations: Vec<Annotation>,
    /// A list of all `stateMachine` blocks defined within the file.
    pub state_machines: Vec<StateMachine>,
}

// --- State Machine ---

/// Represents a single `use` declaration within a scope (e.g., events block).
/// e.g., `use common.MyEvent;`
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct UseDeclaration {
    /// The identifier being brought into scope, potentially qualified.
    /// e.g., `common.MyEvent` would be stored here. Resolution happens later.
    pub target: QualifiedIdent,
}

/// Represents a single `stateMachine` definition, encapsulating its logic and structure.
///
/// This is the core unit of definition in an `.ssot` file.
#[derive(Debug, Clone, PartialEq)]
pub struct StateMachine {
    /// The name identifier of the state machine (e.g., `LightSwitch`).
    pub name: Ident,
    /// Annotations specific to this state machine definition, such as `$description`, `$initial`,
    /// or overrides for output directories (e.g., `$rust_out`).
    pub annotations: Vec<Annotation>,
    /// `use` declarations within the state machine scope (e.g., `use common.Type;`).
    pub use_declarations: Vec<UseDeclaration>,
    /// The complete set of possible states defined for this machine within the `states { ... }` block.
    pub states: Vec<StateItem>,
    /// The complete set of events (messages) defined for this machine within the `events { ... }` block.
    pub events: Vec<MessageItem>,
    /// The complete set of transition rules defined for this machine within the `transitions { ... }` block.
    pub transitions: Vec<TransitionItem>,
    /// Data fields representing the internal context or memory of the state machine.
    /// (Note: DSL syntax for context might still be under development or implicitly defined).
    pub context: Vec<FieldDef>,
}

// --- States ---

/// Represents a single state defined within the `states { ... }` block.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct StateItem {
    /// Annotations specific to this state definition (e.g., `$description`).
    pub annotations: Vec<Annotation>,
    /// The name identifier of the state (e.g., `On`, `Off`).
    pub name: Ident,
    /// The unique non-negative integer (`@N`) associated with the state.
    /// Used for serialization and identification, especially in Cap'n Proto.
    pub ordinal: u64,
    /// A list of action identifiers to be executed upon entering this state.
    /// Defined using `entry: actionName;` or `entry: ns.actionName;`.
    pub entry_actions: Vec<QualifiedIdent>,
    /// A list of action identifiers to be executed upon exiting this state.
    /// Defined using `exit: actionName;` or `exit: ns.actionName;`.
    pub exit_actions: Vec<QualifiedIdent>,
}

// --- Events (Messages) ---

/// Represents an event (or message) definition within the `events { ... }` block.
///
/// Events are the triggers for state transitions and can carry data payloads.
#[derive(Debug, Clone, PartialEq)]
pub struct MessageItem {
    /// Annotations specific to this event definition (e.g., `$description`).
    pub annotations: Vec<Annotation>,
    /// The name identifier of the event struct (e.g., `Toggle`, `TurnOn`).
    pub name: Ident,
    /// The unique non-negative integer (`@N`) associated with the event struct.
    /// Used for serialization and identification.
    pub ordinal: u64,
    /// The data fields contained within this event's payload, defined within the event's braces `{ ... }`.
    /// If the event has no payload, this list is empty.
    pub fields: Vec<FieldDef>,
}

/// Represents a single data field within an event's payload or the state machine's context.
#[derive(Debug, Clone, PartialEq)]
pub struct FieldDef {
    /// Annotations specific to this field definition (e.g., `$description`).
    pub annotations: Vec<Annotation>,
    /// The name identifier of the field (e.g., `brightness`).
    pub name: Ident,
    /// The unique non-negative integer (`@N`) associated with the field within its containing struct.
    /// Used for serialization and identification.
    pub ordinal: u64,
    /// The data type of the field (e.g., `UInt8`, `Text`).
    pub field_type: FieldType,
}

// --- Transitions ---

/// Represents a state transition rule defined within the `transitions { ... }` block.
///
/// Describes how the machine moves from a source state to a target state.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct TransitionItem {
    /// An optional name identifier for the transition definition (e.g., `transition MyTransition from ...`).
    /// Useful for identification, documentation, or specific referencing.
    pub name: Option<Ident>,
    /// The identifier of the state from which this transition originates.
    pub from: Ident,
    /// The identifier of the state to which this transition leads.
    pub to: Ident,
    /// The core components defining the transition's trigger (`on`), condition (`guard`), and effect (`action`).
    /// There must be exactly one `on` element, and zero or one `guard` and `action` elements.
    pub elements: Vec<TransitionElement>,
    /// Annotations specific to this transition definition (e.g., `$description`).
    pub annotations: Vec<Annotation>,
}

/// Represents the distinct parts that define a transition's behavior: `on`, `guard`, `action`.
#[derive(Debug, Clone, PartialEq, Eq)]
pub enum TransitionElement {
    /// Specifies the event that triggers the transition.
    /// Syntax: `on @Ordinal EventName;` or `on @Ordinal ns.EventName;`
    On {
        /// The unique non-negative integer ordinal (`@N`) for this `on` declaration within the state machine.
        ordinal: u64,
        /// The identifier of the triggering event (potentially qualified).
        event: QualifiedIdent,
    },
    /// Specifies a condition (guard function) that must evaluate to true for the transition to be taken.
    /// Syntax: `guard @Ordinal guardFunctionName;` or `guard @Ordinal ns.guardFunctionName;`
    Guard {
        /// The unique non-negative integer ordinal (`@N`) for this `guard` declaration within the state machine.
        ordinal: u64,
        /// The identifier of the guard function (potentially qualified).
        function: QualifiedIdent,
    },
    /// Specifies an action (function) to be executed when the transition is taken.
    /// Syntax: `action @Ordinal actionFunctionName;` or `action @Ordinal ns.actionFunctionName;`
    Action {
        /// The unique non-negative integer ordinal (`@N`) for this `action` declaration within the state machine.
        ordinal: u64,
        /// The identifier of the action function (potentially qualified).
        function: QualifiedIdent,
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
