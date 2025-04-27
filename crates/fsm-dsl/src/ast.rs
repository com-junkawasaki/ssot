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
    // Add specific annotations related to state machine structure
    Initial, // $initial;
    Final,   // $final;
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

#[derive(Debug, Clone, PartialEq)]
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

#[derive(Debug, Clone, PartialEq)]
pub struct ActorsBlock {
    // TODO: Define ActorDefinition
    // pub definitions: Vec<ActorDefinition>,
    pub annotations: Vec<Annotation>,
}

#[derive(Debug, Clone, PartialEq)]
pub struct CommunicationBlock {
    // TODO: Define ProtocolDefinition, ChannelDefinition, EventDefinition
    // pub definitions: Vec<CommunicationItem>,
    pub annotations: Vec<Annotation>,
}

#[derive(Debug, Clone, PartialEq)]
pub struct ServicesBlock {
    // TODO: Define InterfaceDefinition, ServiceDefinition
    // pub definitions: Vec<ServiceItem>,
    pub annotations: Vec<Annotation>,
}

// --- Machine Definitions (Basic) ---

#[derive(Debug, Clone, PartialEq)]
pub struct MachineDefinition {
    pub name: Identifier,
    pub id: NumericId,
    pub annotations: Vec<Annotation>,
    pub context: Option<ContextDefinition>,
    pub states: Option<StatesBlock>, // Added states field
    // TODO: Add fields for initial, actions, guards, invokes
    pub actions: Option<ActionsBlock>, // Added actions block
    pub guards: Option<GuardsBlock>,   // Added guards block
    pub invokes: Option<InvokesBlock>, // Added invokes block
    // TODO: Add fields for initial_state: Option<Identifier>,
}

#[derive(Debug, Clone, PartialEq)]
pub struct MachinesBlock {
    pub definitions: Vec<MachineDefinition>,
    pub annotations: Vec<Annotation>,
}

#[derive(Debug, Clone, PartialEq)]
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

// --- State Machine States Definitions (Added) ---

#[derive(Debug, Clone, PartialEq)]
pub struct TransitionDefinition {
    pub event: Identifier,
    pub target: Identifier, // Simple target state name for now
    pub id: NumericId,
    pub annotations: Vec<Annotation>, // Although grammar doesn't explicitly show annotations here yet
    // TODO: Add fields for actions, guards, allowed_actors
    pub actions: Vec<Identifier>, // Added: List of action identifiers
    pub guards: Vec<Identifier>,  // Added: List of guard identifiers
    // pub allowed_actors: Vec<Identifier>,
}

#[derive(Debug, Clone, PartialEq)]
pub struct StateDefinition {
    pub name: Identifier,
    pub id: NumericId,
    pub annotations: Vec<Annotation>, // Contains general annotations + $initial/$final
    pub transitions: Vec<TransitionDefinition>,
    // TODO: Add fields for on_entry, on_exit, invokes, nested_states, history, initial, final, parallel
    pub invokes: Vec<StateInvokeDefinition>, // Added invokes field
    // pub on_entry: Vec<Identifier>,
    pub on_entry: Vec<Identifier>, // Added onEntry actions
    pub on_exit: Vec<Identifier>,  // Added onExit actions
    pub after_transitions: Vec<AfterTransitionDefinition>, // Added after transitions
    // pub on_exit: Vec<Identifier>,
    // pub invokes: Vec<InvokeDefinition>,
    // pub nested_states: Option<StatesBlock>,
    // pub history: Option<HistoryDefinition>,
    // pub initial_state: Option<Identifier>, // Redundant if using $initial annotation
    // pub is_final: bool, // Redundant if using $final annotation
    // pub is_parallel: bool,
    // Helper flags derived from annotations for easier access
    pub is_initial: bool,
    pub is_final: bool,
}

#[derive(Debug, Clone, PartialEq)]
pub struct StatesBlock {
    pub id: NumericId,
    pub states: Vec<StateDefinition>,
    pub annotations: Vec<Annotation>,
}

// --- Action/Guard Definitions (Added) ---

#[derive(Debug, Clone, PartialEq)]
pub struct ActionDefinition {
    pub name: Identifier,
    pub id: NumericId,
    pub annotations: Vec<Annotation>,
    // TODO: Add parameters or implementation details later
}

#[derive(Debug, Clone, PartialEq)]
pub struct ActionsBlock {
    pub id: NumericId,
    pub definitions: Vec<ActionDefinition>,
    pub annotations: Vec<Annotation>,
}

#[derive(Debug, Clone, PartialEq)]
pub struct GuardDefinition {
    pub name: Identifier,
    pub id: NumericId,
    pub annotations: Vec<Annotation>,
    // TODO: Add expression or condition details later
}

#[derive(Debug, Clone, PartialEq)]
pub struct GuardsBlock {
    pub id: NumericId,
    pub definitions: Vec<GuardDefinition>,
    pub annotations: Vec<Annotation>,
}

// --- Invoke Definitions (Added) ---

#[derive(Debug, Clone, PartialEq)]
pub enum InvokeSource {
    ServiceMethod(Identifier, Identifier), // ServiceName, MethodName
    Literal(String), // String literal for function/promise name
    Machine(Identifier), // MachineName
    // TODO: Potentially DatabaseOperation, etc.
}

// Placeholder for input mapping, onDone/onError transitions
// For now, just storing the target state identifier
#[derive(Debug, Clone, PartialEq)]
pub struct InvokeTransitionTarget {
    pub target_state: Identifier, 
    // TODO: Add actions, guards later if needed for invoke transitions
}

#[derive(Debug, Clone, PartialEq)]
pub struct InvokeDefinition {
    pub name: Identifier,
    pub id: NumericId,
    pub annotations: Vec<Annotation>,
    pub src: InvokeSource,
    pub input_mapping: Option<Vec<Argument>>, // Using Argument similar to annotations for now
    pub on_done: Option<InvokeTransitionTarget>, // Simplified target
    pub on_error: Option<InvokeTransitionTarget>, // Simplified target
}

#[derive(Debug, Clone, PartialEq)]
pub struct InvokesBlock {
    pub id: NumericId,
    pub definitions: Vec<InvokeDefinition>,
    pub annotations: Vec<Annotation>,
}

// Represents an invoke call within a state, referencing a definition
#[derive(Debug, Clone, PartialEq)]
pub struct StateInvokeDefinition {
     pub name: Identifier, // Name of the invoke instance within the state
     pub id: NumericId,
     pub annotations: Vec<Annotation>,
     pub src_ref: Identifier, // Name of the InvokeDefinition being referenced (from invokes block)
     pub input_mapping: Option<Vec<Argument>>, // Specific input for this instance
     pub on_done: Option<InvokeTransitionTarget>, // Specific onDone for this instance
     pub on_error: Option<InvokeTransitionTarget>, // Specific onError for this instance
}

// --- Delayed Transitions (Added) ---

#[derive(Debug, Clone, PartialEq, Eq)]
pub enum TimeUnit {
    Milliseconds,
    Seconds,
    Minutes,
    Hours,
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct Duration {
    pub value: u64,
    pub unit: TimeUnit,
}

#[derive(Debug, Clone, PartialEq)]
pub struct AfterTransitionDefinition {
    pub delay: Duration,
    pub id: NumericId,
    pub target: Identifier,
    pub annotations: Vec<Annotation>, // Add if grammar allows annotations here later
    pub actions: Vec<Identifier>,
    pub guards: Vec<Identifier>,
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
#[derive(Debug, Clone, PartialEq)]
pub struct SsotAst {
    pub file_id: Option<FileId>,
    pub imports: Vec<ImportStatement>,
    pub definitions: Vec<TopLevelDefinition>, // Contains blocks like types {}, services {}, etc.
    // Store the source file path if known
    pub source_path: Option<PathBuf>,
    // Store comments or other non-semantic elements if needed
    // pub comments: Vec<CommentSpan>,
} 