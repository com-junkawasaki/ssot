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

// New: Represents a top-level struct definition
#[derive(Debug, Clone, PartialEq)]
pub struct StructDef {
    /// Annotations specific to this struct definition (e.g., `$description`).
    pub annotations: Vec<Annotation>,
    /// The name identifier of the struct (e.g., `TrafficLightContext`).
    pub name: Ident,
    /// The data fields contained within this struct.
    pub fields: Vec<FieldDef>,
    // Note: No top-level ordinal for now, unlike MessageItem
}

/// Represents a single import declaration at the top level of an .ssot file.
/// e.g., `import my.package.name;`
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct ImportDeclaration {
    pub package_declaration: Option<String>,
    /// Import declarations at the top level (e.g., `import other.package;`)
    pub imports: Vec<ImportDeclaration>,
    // Updated: Use TopLevelItem enum instead of separate lists
    /// A list of all top-level items (state machines, structs, annotations) defined within the file.
    pub items: Vec<TopLevelItem>,
    /*
    /// Annotations defined at the top level of the file, before any `stateMachine` definitions.
    /// These often provide global configuration for code generation (e.g., `$rust_out`).
    pub top_level_annotations: Vec<Annotation>,
    /// A list of all `stateMachine` blocks defined within the file.
    pub state_machines: Vec<StateMachine>,
    */
}

// New: Enum to represent different kinds of top-level items
#[derive(Debug, Clone, PartialEq)]
pub enum TopLevelItem {
    StateMachine(StateMachine),
    StructDefinition(StructDef),
    Annotation(Annotation),
    // Add other potential top-level items here in the future (e.g., Enums)
}

// --- State Machine ---

/// Represents a `