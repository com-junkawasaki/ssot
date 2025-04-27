use std::fmt;
use std::path::PathBuf; // Import fmt

// Re-export pest for convenience if needed later for spans, etc.
// extern crate pest;
// use pest::Span;

// --- Basic Building Blocks ---

#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct Identifier {
    pub name: String,
    // pub span: Span<'static>, // Consider adding spans later
}

// Implement Display for Identifier
impl fmt::Display for Identifier {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        write!(f, "{}", self.name)
    }
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
    List(Box<TypeSpecifier>),     // Added: list<T>
    Optional(Box<TypeSpecifier>), // Added: optional<T>
    Map(Box<TypeSpecifier>, Box<TypeSpecifier>), // Added: map<K, V>
                                  // TODO: Add Map(Box<TypeSpecifier>, Box<TypeSpecifier>)
}

// --- Annotations (Updated) ---

// Represents a value within an annotation's arguments
#[derive(Debug, Clone, PartialEq)]
pub enum AnnotationValue {
    String(String),
    Integer(i64),  // Using i64 for flexibility, could use specific types
    Boolean(bool), // Added boolean value
    // TODO: Add List(Vec<AnnotationValue>), Object(Vec<Argument>)
    List(Vec<AnnotationValue>), // Added list value
    Object(Vec<Argument>),      // Added object value (key-value pairs)
}

// Represents a single key-value argument in an annotation
#[derive(Debug, Clone, PartialEq)]
pub struct Argument {
    pub key: Identifier,
    pub value: AnnotationValue,
}

// Specific struct for $communicatesWith for clarity
#[derive(Debug, Clone, PartialEq)]
pub struct CommunicatesWithArgs {
    pub service: Identifier,
    pub protocol: Identifier,
}

#[derive(Debug, Clone, PartialEq)]
pub enum Annotation {
    // Documentation & Metadata
    Description(String),
    Meta(Vec<Argument>), // $meta(key: value, ...)

    // Validation & Data Mapping
    Validate(Vec<Argument>), // $validate(rule: value, ...)
    Db(Vec<Argument>),       // $db(table: "...", column: "...", ...)

    // Output / Code Generation Directives
    OutputDirective { directive: String, path: String }, // $rust_out("path"), $ts_out("path"), etc.

    // State Machine Structure
    InitialState(Identifier), // $initial(StateName)
    Final,                    // $final;
    Parallel,                 // $parallel;

    // Service & Communication
    Implements(Identifier),                 // $implements(InterfaceName)
    Protocol(Identifier),                   // $protocol(ProtocolName)
    CommunicatesWith(CommunicatesWithArgs), // $communicatesWith(Service using Protocol)
    Publishes(Identifier),                  // $publishes(ChannelName)
    Subscribes(Identifier),                 // $subscribes(ChannelName)
    Route(Vec<Argument>),                   // $route(method: "POST", path: "/...")

    // Event / Channel Association
    Channel(Identifier), // $channel(ChannelName)

    // Access Control
    AllowedActors(Vec<Identifier>), // $allowedActors([Actor1, Actor2])

    // Generic / Less Common Annotations (Consider removing if not needed)
    GenericFlag(Identifier),             // Example: $someFlag;
    GenericKeyValue(Identifier, String), // Example: $someKey("value")
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

// --- Actor Definitions (Added) ---

#[derive(Debug, Clone, PartialEq)]
pub struct ActorDefinition {
    pub name: Identifier,
    pub id: NumericId,
    pub annotations: Vec<Annotation>, // Includes $type etc.
}

// --- Placeholder Blocks (Added) ---

#[derive(Debug, Clone, PartialEq)]
pub struct ActorsBlock {
    // TODO: Define ActorDefinition
    // pub definitions: Vec<ActorDefinition>,
    pub annotations: Vec<Annotation>,
    pub definitions: Vec<ActorDefinition>, // Added definitions field
}

#[derive(Debug, Clone, PartialEq)]
pub struct CommunicationBlock {
    pub definitions: Vec<CommunicationItem>, // Added definitions field
    pub annotations: Vec<Annotation>,
}

#[derive(Debug, Clone, PartialEq)]
pub struct ServicesBlock {
    pub definitions: Vec<ServiceItem>, // Added definitions field
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

// --- Deployment Config Definitions (Added) ---

// Represents a generic key-value attribute found in infra/deployment bodies
#[derive(Debug, Clone, PartialEq)]
pub struct AttributeDefinition {
    pub key: Identifier,
    pub value: AnnotationValue,
}

#[derive(Debug, Clone, PartialEq)]
pub struct EnvironmentDefinition {
    pub name: Identifier,
    pub id: NumericId,
    pub annotations: Vec<Annotation>,
    pub extends: Option<Identifier>,
    pub variables: Option<Vec<Argument>>, // Reuse Argument for { key: value }
}

#[derive(Debug, Clone, PartialEq)]
pub struct InfrastructureDefinition {
    pub name: Identifier,
    pub id: NumericId,
    pub annotations: Vec<Annotation>,
    pub extends: Option<Identifier>,
    pub attributes: Vec<AttributeDefinition>, // Store key-value attributes
}

#[derive(Debug, Clone, PartialEq)]
pub struct DeploymentDefinition {
    pub name: Identifier,
    pub id: NumericId,
    pub annotations: Vec<Annotation>,
    pub target_environment: Option<Identifier>,
    pub target_infrastructure: Option<Vec<Argument>>, // Store as key-value pairs for now
    pub deployable: Option<Identifier>,
    pub config: Option<Vec<Argument>>, // Store as key-value pairs
    pub other_attributes: Vec<AttributeDefinition>, // For misc attributes like replicas, strategy
}

#[derive(Debug, Clone, PartialEq)]
pub enum DeploymentItem {
    Environment(EnvironmentDefinition),
    Infrastructure(InfrastructureDefinition),
    Deployment(DeploymentDefinition),
}

#[derive(Debug, Clone, PartialEq)]
pub struct DeploymentConfigBlock {
    // TODO: Define EnvironmentDefinition, InfrastructureDefinition, DeploymentDefinition
    // pub definitions: Vec<DeploymentItem>,
    pub annotations: Vec<Annotation>,
    pub definitions: Vec<DeploymentItem>, // Added definitions field
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
pub enum TransitionTarget {
    State(Identifier),
    CurrentHistory,               // .history
    QualifiedHistory(Identifier), // ParentState.history
}

#[derive(Debug, Clone, PartialEq)]
pub struct TransitionDefinition {
    pub event: Identifier,
    pub target: TransitionTarget, // Updated target type
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
    // pub history: Option<HistoryDefinition>,
    pub history: Option<HistoryDefinition>, // Added history state definition
    pub regions: Vec<StatesBlock>,          // Replaced nested_states for parallel regions
    // pub initial_state: Option<Identifier>, // Redundant if using $initial annotation
    // pub is_final: bool, // Redundant if using $final annotation
    // pub is_parallel: bool,
    // Helper flags derived from annotations for easier access
    pub is_initial: bool,
    pub is_final: bool,
    pub is_parallel: bool, // Added parallel flag
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
    Literal(String),                       // String literal for function/promise name
    Machine(Identifier),                   // MachineName
                                           // TODO: Potentially DatabaseOperation, etc.
}

// Placeholder for input mapping, onDone/onError transitions
// For now, just storing the target state identifier
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct InvokeTransitionTarget {
    pub target: TransitionTarget,     // Updated target type
    pub actions: Vec<Identifier>,     // Added actions for invoke transition
    pub guards: Vec<Identifier>,      // Added guards for invoke transition
    pub annotations: Vec<Annotation>, // Added annotations
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

#[derive(Debug, Clone, PartialEq, Eq, Hash)]
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
    pub target: TransitionTarget,     // Updated target type
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

// --- History State Definition (Added) ---

#[derive(Debug, Clone, PartialEq, Eq)]
pub enum HistoryType {
    Shallow,
    Deep,
}

#[derive(Debug, Clone, PartialEq)]
pub struct HistoryDefinition {
    pub history_type: HistoryType,
    pub id: NumericId,
    pub default_target: Identifier, // Default state to transition to if no history exists
}

// --- Service Definitions (Added) ---

#[derive(Debug, Clone, PartialEq)]
pub struct ParameterDefinition {
    pub name: Identifier,
    pub type_spec: TypeSpecifier,
    pub id: Option<NumericId>, // ID might be optional for parameters
    pub annotations: Vec<Annotation>,
}

#[derive(Debug, Clone, PartialEq)]
pub struct MethodDefinition {
    pub name: Identifier,
    pub id: NumericId,
    pub annotations: Vec<Annotation>, // Annotations directly on the method
    pub parameters: Vec<ParameterDefinition>,
    pub return_type: Option<TypeSpecifier>,
    pub body_annotations: Vec<Annotation>, // Annotations inside optional {} body
}

#[derive(Debug, Clone, PartialEq)]
pub struct InterfaceDefinition {
    pub name: Identifier,
    pub id: NumericId,
    pub annotations: Vec<Annotation>,
    pub methods: Vec<MethodDefinition>,
}

#[derive(Debug, Clone, PartialEq)]
pub struct ServiceDefinition {
    pub name: Identifier,
    pub id: NumericId,
    pub annotations: Vec<Annotation>, // Includes $implements, $protocol, $route etc.
    pub extends: Option<Identifier>,  // Name of the base service
                                      // Note: $implements is stored in annotations Vec
}

#[derive(Debug, Clone, PartialEq)]
pub enum ServiceItem {
    Interface(InterfaceDefinition),
    Service(ServiceDefinition),
}

#[derive(Debug, Clone, PartialEq)]
pub enum CommunicationItem {
    Protocol(ProtocolDefinition),
    Channel(ChannelDefinition),
    Event(EventDefinition),
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

#[cfg(test)]
mod tests {
    use super::*; // Import everything from the parent module (ast)
    use pretty_assertions::assert_eq; // For better diffs on failure

    // Helper to create Identifier
    fn ident(name: &str) -> Identifier {
        Identifier { name: name.to_string() }
    }

    // Helper to create NumericId
    fn num_id(value: u64) -> NumericId {
        NumericId { value }
    }

    #[test]
    fn test_identifier_display() {
        let id = ident("MyState");
        assert_eq!(format!("{}", id), "MyState");
    }

    #[test]
    fn test_struct_definition_equality() {
        let field1 = FieldDefinition {
            name: ident("fieldA"),
            type_spec: TypeSpecifier::Simple(ident("string")),
            id: num_id(0),
            annotations: vec![],
        };
        let struct1 = StructDefinition {
            name: ident("MyStruct"),
            id: num_id(1),
            fields: vec![field1.clone()],
            annotations: vec![],
        };
        let struct2 = StructDefinition {
            name: ident("MyStruct"),
            id: num_id(1),
            fields: vec![field1], // Use the same field def
            annotations: vec![],
        };
        assert_eq!(struct1, struct2);
    }

    #[test]
    fn test_annotation_creation_and_equality() {
        let arg1 = Argument {
            key: ident("key1"),
            value: AnnotationValue::String("value1".to_string()),
        };
        let arg2 = Argument {
            key: ident("key2"),
            value: AnnotationValue::Integer(123),
        };
        let anno1 = Annotation::Meta(vec![arg1.clone(), arg2.clone()]);
        let anno2 = Annotation::Meta(vec![arg1, arg2]);
        assert_eq!(anno1, anno2);
    }

    #[test]
    fn test_type_specifier_creation() {
        let simple = TypeSpecifier::Simple(ident("u32"));
        let list_of_simple = TypeSpecifier::List(Box::new(simple.clone()));
        let optional_list = TypeSpecifier::Optional(Box::new(list_of_simple.clone()));
        let map_type = TypeSpecifier::Map(
            Box::new(TypeSpecifier::Simple(ident("string"))),
            Box::new(simple.clone()),
        );

        assert_eq!(simple, TypeSpecifier::Simple(ident("u32")));
        assert_eq!(list_of_simple, TypeSpecifier::List(Box::new(TypeSpecifier::Simple(ident("u32")))));
        assert_eq!(optional_list, TypeSpecifier::Optional(Box::new(TypeSpecifier::List(Box::new(TypeSpecifier::Simple(ident("u32")))))));
        assert_eq!(map_type, TypeSpecifier::Map(
            Box::new(TypeSpecifier::Simple(ident("string"))),
            Box::new(TypeSpecifier::Simple(ident("u32"))),
        ));
    }
}
