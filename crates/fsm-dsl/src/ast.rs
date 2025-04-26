use std::path::PathBuf;

// Re-export pest for convenience if needed later for spans, etc.
// extern crate pest;
// use pest::Span;

// --- Basic Building Blocks ---

#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct Identifier {
    pub name: String,
    // pub span: Span<'static>, // Consider adding spans later
}

#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct NumericId {
    pub value: u64, // Using u64 based on DSL.md examples like @0x... and @id(...)
    // pub span: Span<'static>,
}

// --- Type Specifiers (Updated) ---

#[derive(Debug, Clone, PartialEq, Eq)]
pub enum TypeSpecifier {
    Simple(Identifier),
    List(Box<TypeSpecifier>),    // Added: list<T>
    Optional(Box<TypeSpecifier>), // Added: optional<T>
    // TODO: Add Map(Box<TypeSpecifier>, Box<TypeSpecifier>)
}

// --- Annotations (Updated) ---

// Represents a value within an annotation's arguments
#[derive(Debug, Clone, PartialEq)]
pub enum AnnotationValue {
    String(String),
    Integer(i64), // Using i64 for flexibility, could use specific types
    // TODO: Add Boolean(bool), List(Vec<AnnotationValue>), Object(Vec<Argument>)
}

// Represents a single key-value argument in an annotation
#[derive(Debug, Clone, PartialEq)]
pub struct Argument {
    pub key: Identifier,
    pub value: AnnotationValue,
}

#[derive(Debug, Clone, PartialEq)]
pub enum Annotation {
    Description(String),
    // $validate(minLength: 3, pattern: "...")
    Validate(Vec<Argument>),
    // $db(table: "users", primaryKey: "id")
    Db(Vec<Argument>),
    // $meta(key: "value", other: 123)
    Meta(Vec<Argument>),
    // Generic annotations for less common/structured ones
    GenericFlag(Identifier),             // Example: $final;
    GenericKeyValue(Identifier, String), // Example: $rust_out("path") - Keep simple string value for now
    // TODO: Consider if GenericKeyValue should use AnnotationValue
}

// --- Definitions ---

#[derive(Debug, Clone, PartialEq)]
pub struct FieldDefinition {
    pub name: Identifier,
    pub type_spec: TypeSpecifier,
    pub id: NumericId,
    pub annotations: Vec<Annotation>,
    // pub span: Span<'static>,
}

#[derive(Debug, Clone, PartialEq)]
pub struct StructDefinition {
    pub name: Identifier,
    pub id: NumericId,
    pub fields: Vec<FieldDefinition>,
    pub annotations: Vec<Annotation>, // Annotations applying to the struct itself
    // pub span: Span<'static>,
}

// --- Enum Definitions (Added) ---

#[derive(Debug, Clone, PartialEq)]
pub struct EnumVariant {
    pub name: Identifier,
    pub id: NumericId,
    pub annotations: Vec<Annotation>,
    // pub span: Span<'static>,
}

#[derive(Debug, Clone, PartialEq)]
pub struct EnumDefinition {
    pub name: Identifier,
    pub id: NumericId,
    pub variants: Vec<EnumVariant>,
    pub annotations: Vec<Annotation>,
    // pub span: Span<'static>,
}

// --- Type Definitions (Updated) ---

#[derive(Debug, Clone, PartialEq)]
pub enum TypeDefinition {
    Struct(StructDefinition),
    Enum(EnumDefinition), // Added Enum variant
}


// --- Top-Level Blocks ---

#[derive(Debug, Clone, PartialEq, Default)]
pub struct TypesBlock {
    pub definitions: Vec<TypeDefinition>,
    pub annotations: Vec<Annotation>,
    // pub span: Span<'static>,
}

// TODO: Define other blocks (ServicesBlock, MachinesBlock, etc.)


// --- File Structure ---

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct FileId {
    pub value: u64, // Store the hex value as u64
    // pub span: Span<'static>,
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct ImportStatement {
    pub path: String, // The path string literal
    // pub span: Span<'static>,
}

// --- Placeholder Blocks (Added) ---

#[derive(Debug, Clone, PartialEq, Default)]
pub struct ActorsBlock {
    // TODO: Define ActorDefinition
    // pub definitions: Vec<ActorDefinition>,
    pub annotations: Vec<Annotation>,
}

#[derive(Debug, Clone, PartialEq, Default)]
pub struct CommunicationBlock {
    // TODO: Define ProtocolDefinition, ChannelDefinition, EventDefinition
    // pub definitions: Vec<CommunicationItem>,
    pub annotations: Vec<Annotation>,
}

#[derive(Debug, Clone, PartialEq, Default)]
pub struct ServicesBlock {
    // TODO: Define InterfaceDefinition, ServiceDefinition
    // pub definitions: Vec<ServiceItem>,
    pub annotations: Vec<Annotation>,
}

// --- Machine Definitions (Basic) ---

#[derive(Debug, Clone, PartialEq, Default)]
pub struct MachineDefinition {
    pub name: Identifier,
    pub id: NumericId,
    pub annotations: Vec<Annotation>,
    pub context: Option<ContextDefinition>, // Added context field
    // TODO: Add fields for states, initial, actions, guards, invokes
    // pub states: Option<StatesBlock>,
    // pub actions: Option<ActionsBlock>,
    // pub guards: Option<GuardsBlock>,
    // pub invokes: Option<InvokesBlock>,
    // pub initial_state: Option<Identifier>,
}

#[derive(Debug, Clone, PartialEq, Default)]
pub struct MachinesBlock {
    pub definitions: Vec<MachineDefinition>,
    pub annotations: Vec<Annotation>,
}

#[derive(Debug, Clone, PartialEq, Default)]
pub struct DeploymentConfigBlock {
    // TODO: Define EnvironmentDefinition, InfrastructureDefinition, DeploymentDefinition
    // pub definitions: Vec<DeploymentItem>,
    pub annotations: Vec<Annotation>,
}

// --- Machine Context Definitions (Added) ---

#[derive(Debug, Clone, PartialEq)]
pub struct ContextFieldDefinition {
    pub name: Identifier,
    pub type_spec: TypeSpecifier,
    pub id: NumericId,
    pub annotations: Vec<Annotation>,
    // TODO: Potentially add parsed default value
    // pub default_value: Option<AnnotationValue>,
}

#[derive(Debug, Clone, PartialEq)]
pub struct ContextDefinition {
    pub id: NumericId,
    pub fields: Vec<ContextFieldDefinition>,
    pub annotations: Vec<Annotation>,
}

// --- TopLevelDefinition (Updated) ---
#[derive(Debug, Clone, PartialEq)]
pub enum TopLevelDefinition {
    Types(TypesBlock),
    Actors(ActorsBlock),
    Communication(CommunicationBlock),
    Services(ServicesBlock),
    Machines(MachinesBlock),
    DeploymentConfig(DeploymentConfigBlock),
    // TODO: Potentially handle top-level annotations here too? Or within blocks?
}


/// Represents the entire parsed content of a .ssot file.
#[derive(Debug, Clone, PartialEq, Default)]
pub struct SsotAst {
    pub file_id: Option<FileId>,
    pub imports: Vec<ImportStatement>,
    pub definitions: Vec<TopLevelDefinition>, // Contains blocks like types {}, services {}, etc.
    // Store the source file path if known
    pub source_path: Option<PathBuf>,
    // Store comments or other non-semantic elements if needed
    // pub comments: Vec<CommentSpan>,
} 