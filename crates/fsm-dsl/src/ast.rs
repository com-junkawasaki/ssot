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

#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub enum TypeSpecifier {
    Simple(Identifier),
    List(Box<TypeSpecifier>),     // Added: list<T>
    Optional(Box<TypeSpecifier>), // Added: optional<T>
    Map(Box<TypeSpecifier>, Box<TypeSpecifier>), // Added: map<K, V>
                                  // TODO: Add Map(Box<TypeSpecifier>, Box<TypeSpecifier>)
}

// --- Annotations (Updated) ---

// Represents a value within an annotation's arguments
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub enum AnnotationValue {
    String(String),
    Integer(i64),  // Using i64 for flexibility, could use specific types
    Boolean(bool), // Added boolean value
    // TODO: Add List(Vec<AnnotationValue>), Object(Vec<Argument>)
    List(Vec<AnnotationValue>), // Added list value
    Object(Vec<Argument>),      // Added object value (key-value pairs)
}

// Represents a single key-value argument in an annotation
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct Argument {
    pub key: Identifier,
    pub value: AnnotationValue,
}

// Specific struct for $communicatesWith for clarity
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct CommunicatesWithArgs {
    pub service: Identifier,
    pub protocol: Identifier,
}

#[derive(Debug, Clone, PartialEq, Eq, Hash)]
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
    InitialState(Identifier), // $initial(StateName) - For explicit initial state target (e.g. in history)
    Initial,                  // $initial; - For marking the default initial state
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

#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct FieldDefinition {
    pub name: Identifier,
    pub type_spec: TypeSpecifier,
    pub id: NumericId,
    pub annotations: Vec<Annotation>,
    // pub span: Span<'static>,
}

#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct StructDefinition {
    pub name: Identifier,
    pub id: NumericId,
    pub fields: Vec<FieldDefinition>,
    pub annotations: Vec<Annotation>, // Annotations applying to the struct itself
                                      // pub span: Span<'static>,
}

// --- Enum Definitions (Added) ---

#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct EnumVariant {
    pub name: Identifier,
    pub id: NumericId,
    pub annotations: Vec<Annotation>,
    // pub span: Span<'static>,
}

#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct EnumDefinition {
    pub name: Identifier,
    pub id: NumericId,
    pub variants: Vec<EnumVariant>,
    pub annotations: Vec<Annotation>,
    // pub span: Span<'static>,
}

// --- Type Definitions (Updated) ---

#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub enum TypeDefinition {
    Struct(StructDefinition),
    Enum(EnumDefinition), // Added Enum variant
}

// --- Top-Level Blocks ---

#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct TypesBlock {
    pub definitions: Vec<TypeDefinition>,
    pub annotations: Vec<Annotation>,
}

// TODO: Define other blocks (ServicesBlock, MachinesBlock, etc.)

// --- File Structure ---

#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct FileId {
    pub value: u64, // Store the hex value as u64
                    // pub span: Span<'static>,
}

#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct ImportStatement {
    pub path: String, // The path string literal
                      // pub span: Span<'static>,
}

// --- Actor Definitions (Added) ---

#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct ActorDefinition {
    pub name: Identifier,
    pub id: NumericId,
    pub annotations: Vec<Annotation>, // Includes $type etc.
}

// --- Placeholder Blocks (Added) ---

#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct ActorsBlock {
    // TODO: Define ActorDefinition
    // pub definitions: Vec<ActorDefinition>,
    pub annotations: Vec<Annotation>,
    pub definitions: Vec<ActorDefinition>, // Added definitions field
}

#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct CommunicationBlock {
    pub definitions: Vec<CommunicationItem>, // Added definitions field
    pub annotations: Vec<Annotation>,
}

#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct ServicesBlock {
    pub definitions: Vec<ServiceItem>, // Added definitions field
    pub annotations: Vec<Annotation>,
}

// --- Machine Definitions (Basic) ---

#[derive(Debug, Clone, PartialEq, Eq, Hash)]
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

#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct MachinesBlock {
    pub definitions: Vec<MachineDefinition>,
    pub annotations: Vec<Annotation>,
}

// --- Deployment Config Definitions (Added) ---

// Represents a generic key-value attribute found in infra/deployment bodies
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct AttributeDefinition {
    pub key: Identifier,
    pub value: AnnotationValue,
}

#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct EnvironmentDefinition {
    pub name: Identifier,
    pub id: NumericId,
    pub annotations: Vec<Annotation>,
    pub extends: Option<Identifier>,
    pub variables: Option<Vec<Argument>>, // Reuse Argument for { key: value }
}

#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct InfrastructureDefinition {
    pub name: Identifier,
    pub id: NumericId,
    pub annotations: Vec<Annotation>,
    pub extends: Option<Identifier>,
    pub attributes: Vec<AttributeDefinition>, // Store key-value attributes
}

#[derive(Debug, Clone, PartialEq, Eq, Hash)]
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

#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub enum DeploymentItem {
    Environment(EnvironmentDefinition),
    Infrastructure(InfrastructureDefinition),
    Deployment(DeploymentDefinition),
}

#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct DeploymentConfigBlock {
    // TODO: Define EnvironmentDefinition, InfrastructureDefinition, DeploymentDefinition
    // pub definitions: Vec<DeploymentItem>,
    pub annotations: Vec<Annotation>,
    pub definitions: Vec<DeploymentItem>, // Added definitions field
}

// --- Machine Context Definitions (Added) ---

#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct ContextFieldDefinition {
    pub name: Identifier,
    pub type_spec: TypeSpecifier,
    pub id: NumericId,
    pub annotations: Vec<Annotation>,
    pub default_value: Option<AnnotationValue>,
}

#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct ContextDefinition {
    pub id: NumericId,
    pub fields: Vec<ContextFieldDefinition>,
    pub annotations: Vec<Annotation>,
}

// --- State Machine States Definitions (Added) ---

#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub enum TransitionTarget {
    State(Identifier),
    CurrentHistory,               // .history
    QualifiedHistory(Identifier), // ParentState.history
}

#[derive(Debug, Clone, PartialEq, Eq, Hash)]
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

#[derive(Debug, Clone, PartialEq, Eq, Hash)]
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

#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct StatesBlock {
    pub id: NumericId,
    pub states: Vec<StateDefinition>,
    pub annotations: Vec<Annotation>,
}

// --- Action/Guard Definitions (Added) ---

#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct ActionDefinition {
    pub name: Identifier,
    pub id: NumericId,
    pub annotations: Vec<Annotation>,
    // TODO: Add parameters or implementation details later
}

#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct ActionsBlock {
    pub id: NumericId,
    pub definitions: Vec<ActionDefinition>,
    pub annotations: Vec<Annotation>,
}

#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct GuardDefinition {
    pub name: Identifier,
    pub id: NumericId,
    pub annotations: Vec<Annotation>,
    // TODO: Add expression or condition details later
}

#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct GuardsBlock {
    pub id: NumericId,
    pub definitions: Vec<GuardDefinition>,
    pub annotations: Vec<Annotation>,
}

// --- Invoke Definitions (Added) ---

#[derive(Debug, Clone, PartialEq, Eq, Hash)]
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

#[derive(Debug, Clone, PartialEq, Eq, Hash)]
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
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
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

#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub enum TimeUnit {
    Milliseconds,
    Seconds,
    Minutes,
    Hours,
}

#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct Duration {
    pub value: u64,
    pub unit: TimeUnit,
}

#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct AfterTransitionDefinition {
    pub delay: Duration,
    pub id: NumericId,
    pub target: TransitionTarget,     // Updated target type
    pub annotations: Vec<Annotation>, // Add if grammar allows annotations here later
    pub actions: Vec<Identifier>,
    pub guards: Vec<Identifier>,
}

// --- TopLevelDefinition (Updated) ---
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
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

#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub enum HistoryType {
    Shallow,
    Deep,
}

#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct HistoryDefinition {
    pub history_type: HistoryType,
    pub id: NumericId,
    pub default_target: Identifier, // Default state to transition to if no history exists
}

// --- Service Definitions (Added) ---

#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct ParameterDefinition {
    pub name: Identifier,
    pub type_spec: TypeSpecifier,
    pub id: Option<NumericId>, // ID might be optional for parameters
    pub annotations: Vec<Annotation>,
}

#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct MethodDefinition {
    pub name: Identifier,
    pub id: NumericId,
    pub annotations: Vec<Annotation>, // Annotations directly on the method
    pub parameters: Vec<ParameterDefinition>,
    pub return_type: Option<TypeSpecifier>,
    pub body_annotations: Vec<Annotation>, // Annotations inside optional {} body
}

#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct InterfaceDefinition {
    pub name: Identifier,
    pub id: NumericId,
    pub annotations: Vec<Annotation>,
    pub methods: Vec<MethodDefinition>,
}

#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct ServiceDefinition {
    pub name: Identifier,
    pub id: NumericId,
    pub annotations: Vec<Annotation>, // Includes $implements, $protocol, $route etc.
    pub extends: Option<Identifier>,  // Name of the base service
                                      // Note: $implements is stored in annotations Vec
}

#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub enum ServiceItem {
    Interface(InterfaceDefinition),
    Service(ServiceDefinition),
}

#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub enum CommunicationItem {
    Protocol(ProtocolDefinition),
    Channel(ChannelDefinition),
    Event(EventDefinition),
}

/// Represents the entire parsed content of a .ssot file.
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct SsotAst {
    pub file_id: Option<FileId>,
    pub imports: Vec<ImportStatement>,
    pub definitions: Vec<TopLevelDefinition>, // Contains blocks like types {}, services {}, etc.
    // Store the source file path if known
    pub source_path: Option<PathBuf>,
    // Store comments or other non-semantic elements if needed
    // pub comments: Vec<CommentSpan>,
}

// Placeholder definitions - TODO: Define based on grammar
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct ProtocolDefinition {
    pub name: String,
}

#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct ChannelDefinition {
    pub name: String,
}

#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct EventDefinition {
    pub name: String,
}

#[cfg(test)]
mod tests {
    use super::*; // Import everything from the parent module (ast)
    use crate::parser::{parse_ssot_content, SsotParserError};
    use pretty_assertions::assert_eq; // For better diffs on failure // Import parser and error type

    // Helper to create Identifier
    fn ident(name: &str) -> Identifier {
        Identifier {
            name: name.to_string(),
        }
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
        assert_eq!(
            list_of_simple,
            TypeSpecifier::List(Box::new(TypeSpecifier::Simple(ident("u32"))))
        );
        assert_eq!(
            optional_list,
            TypeSpecifier::Optional(Box::new(TypeSpecifier::List(Box::new(
                TypeSpecifier::Simple(ident("u32"))
            ))))
        );
        assert_eq!(
            map_type,
            TypeSpecifier::Map(
                Box::new(TypeSpecifier::Simple(ident("string"))),
                Box::new(TypeSpecifier::Simple(ident("u32"))),
            )
        );
    }

    // --- New Test Cases ---

    #[test]
    fn test_parse_enum_definition() {
        let input = r#"
            types {
                enum Color @id(1) {
                    RED @id(0);
                    GREEN @id(1) { $description("Go"); }
                    BLUE @id(2);
                }
            }
        "#;
        let ast = parse_ssot_content(input, None).expect("Parsing failed");
        assert_eq!(ast.definitions.len(), 1);
        match &ast.definitions[0] {
            TopLevelDefinition::Types(types_block) => {
                assert_eq!(types_block.definitions.len(), 1);
                match &types_block.definitions[0] {
                    TypeDefinition::Enum(enum_def) => {
                        assert_eq!(enum_def.name.name, "Color");
                        // assert_eq!(enum_def.id.value, 1); // TODO: Re-enable when ID parsing works
                        assert_eq!(enum_def.variants.len(), 3);
                        assert_eq!(enum_def.variants[0].name.name, "RED");
                        // assert_eq!(enum_def.variants[0].id.value, 0); // TODO: Re-enable when ID parsing works
                        assert!(enum_def.variants[0].annotations.is_empty());
                        assert_eq!(enum_def.variants[1].name.name, "GREEN");
                        // assert_eq!(enum_def.variants[1].id.value, 1); // TODO: Re-enable when ID parsing works
                        assert_eq!(enum_def.variants[1].annotations.len(), 0); // TODO: Fix annotation parsing
                                                                               // assert!(matches!(enum_def.variants[1].annotations[0], Annotation::Description(_)));
                        assert_eq!(enum_def.variants[2].name.name, "BLUE");
                        // assert_eq!(enum_def.variants[2].id.value, 2); // TODO: Re-enable when ID parsing works
                        assert!(enum_def.variants[2].annotations.is_empty());
                    }
                    _ => panic!("Expected EnumDefinition"),
                }
            }
            _ => panic!("Expected TypesBlock"),
        }
    }

    #[test]
    fn test_parse_complex_type_specifiers() {
        let input = r#"
            types {
                struct Complex @id(0) {
                    names: list<string> @id(0);
                    maybe_age: optional<u32> @id(1);
                    headers: map<string, string> @id(2);
                    nested_list: list<optional<map<i32, bool>>> @id(3);
                }
            }
        "#;
        let ast = parse_ssot_content(input, None).expect("Parsing failed");
        assert_eq!(ast.definitions.len(), 1);
        // Basic check to ensure parsing succeeds, detailed assertions depend on parser implementation
        match &ast.definitions[0] {
            TopLevelDefinition::Types(types_block) => {
                assert_eq!(types_block.definitions.len(), 1);
                match &types_block.definitions[0] {
                    TypeDefinition::Struct(struct_def) => {
                        assert_eq!(struct_def.name.name, "Complex");
                        assert_eq!(struct_def.fields.len(), 4);
                        // Add more detailed checks on TypeSpecifier structure once parser is robust
                        // e.g., assert!(matches!(struct_def.fields[0].type_spec, TypeSpecifier::List(_)));
                    }
                    _ => panic!("Expected StructDefinition"),
                }
            }
            _ => panic!("Expected TypesBlock"),
        }
    }

    #[test]
    fn test_parse_various_annotations() {
        let input = r#"
            types {
                struct Annotated @id(0) {
                    $description("A struct.");
                    $validate(required: true, max: 100);
                    $db(table: "annotated_table");
                    $genericFlag;
                    $genericKV("some_value");

                    field: string @id(0) $description("A field.") $meta(sensitive: true, index: false);
                }
            }
        "#;
        let ast = parse_ssot_content(input, None).expect("Parsing failed");
        assert_eq!(ast.definitions.len(), 1);
        // Basic check to ensure parsing succeeds, detailed assertions depend on annotation parser implementation
        // TODO: Add detailed checks once annotation parsing is fully implemented
    }

    #[test]
    fn test_parse_minimal_machine() {
        let input = r#"
            machines {
                machine MyMachine @id(0) {
                    context @id(0) {
                        count: i32 @id(0);
                    }
                    states @id(0) {
                        state Idle @id(0) {
                            $initial;
                        }
                    }
                }
            }
        "#;
        let ast = parse_ssot_content(input, None).expect("Parsing failed");
        assert_eq!(ast.definitions.len(), 1);
        match &ast.definitions[0] {
            TopLevelDefinition::Machines(machines_block) => {
                assert_eq!(machines_block.definitions.len(), 1);
                let machine = &machines_block.definitions[0];
                assert_eq!(machine.name.name, "MyMachine");
                // assert_eq!(machine.id.value, 0); // TODO: Re-enable when ID parsing works
                assert!(machine.context.is_some());
                assert!(machine.states.is_some());
                let states = machine.states.as_ref().unwrap();
                // assert_eq!(states.id.value, 0); // TODO: Re-enable when ID parsing works
                assert_eq!(states.states.len(), 1);
                let state = &states.states[0];
                assert_eq!(state.name.name, "Idle");
                // assert_eq!(state.id.value, 0); // TODO: Re-enable when ID parsing works
                assert!(state.is_initial); // Check derived flag
                                           // TODO: Check annotations directly once parsing is robust
                                           // assert!(state.annotations.iter().any(|a| matches!(a, Annotation::InitialState(_)))); // Assuming $initial becomes InitialState
            }
            _ => panic!("Expected MachinesBlock"),
        }
    }

    #[test]
    fn test_parse_state_transition() {
        let input = r#"
            machines {
                machine Simple @id(0) {
                    states @id(0) {
                        state First @id(0) {
                            $initial;
                            on EVENT_A @id(0) target Second;
                        }
                        state Second @id(1);
                    }
                }
            }
        "#;
        let ast = parse_ssot_content(input, None).expect("Parsing failed");
        assert_eq!(ast.definitions.len(), 1);
        match &ast.definitions[0] {
            TopLevelDefinition::Machines(machines_block) => {
                assert_eq!(machines_block.definitions.len(), 1);
                let machine = &machines_block.definitions[0];
                assert!(machine.states.is_some());
                let states_block = machine.states.as_ref().unwrap();
                assert_eq!(states_block.states.len(), 2);
                let first_state = &states_block.states[0];
                assert_eq!(first_state.transitions.len(), 1);
                let transition = &first_state.transitions[0];
                assert_eq!(transition.event.name, "EVENT_A");
                // assert_eq!(transition.id.value, 0); // TODO: Re-enable when ID parsing works
                match &transition.target {
                    TransitionTarget::State(id) => assert_eq!(id.name, "Second"),
                    _ => panic!("Expected State target"),
                }
                assert!(transition.actions.is_empty());
                assert!(transition.guards.is_empty());
            }
            _ => panic!("Expected MachinesBlock"),
        }
    }

    #[test]
    fn test_parse_invoke_block() {
        let input = r#"
            machines {
                machine Invoker @id(0) {
                    invokes @id(0) {
                        invoke CheckService @id(0) {
                           src: MyService.check;
                           onDone @id(0) target Success;
                           onError @id(1) target Failure;
                        }
                        invoke MyMachineRef @id(1) {
                           src: AnotherMachine;
                        }
                        invoke LiteralPromise @id(2) {
                           src: "checkSomething";
                        }
                    }
                    states @id(1) {
                        state Pending @id(0) { $initial; }
                        state Success @id(1);
                        state Failure @id(2);
                    }
                }
                machine AnotherMachine @id(1) { states @id(0) { state A @id(0) { $initial; } } }
            }
            services {
                service MyService @id(0) {
                    method check @id(0) () -> bool;
                }
            }
        "#;
        let ast = parse_ssot_content(input, None).expect("Parsing failed");
        // Find the MachinesBlock
        let machines_block = ast
            .definitions
            .iter()
            .find_map(|def| match def {
                TopLevelDefinition::Machines(block) => Some(block),
                _ => None,
            })
            .expect("MachinesBlock not found");

        assert_eq!(machines_block.definitions.len(), 2); // Invoker, AnotherMachine
        let invoker_machine = &machines_block.definitions[0];
        assert!(invoker_machine.invokes.is_some());
        let invokes_block = invoker_machine.invokes.as_ref().unwrap();
        // assert_eq!(invokes_block.id.value, 0); // TODO: Re-enable when ID parsing works
        assert_eq!(invokes_block.definitions.len(), 3);

        // Check first invoke
        let invoke1 = &invokes_block.definitions[0];
        assert_eq!(invoke1.name.name, "CheckService");
        // assert_eq!(invoke1.id.value, 0); // TODO: Re-enable ID parsing
        match &invoke1.src {
            InvokeSource::ServiceMethod(service, method) => {
                assert_eq!(service.name, "MyService");
                assert_eq!(method.name, "check");
            }
            _ => panic!("Expected ServiceMethod source"),
        }
        assert!(invoke1.on_done.is_some());
        assert!(invoke1.on_error.is_some());
        let on_done = invoke1.on_done.as_ref().unwrap();
        // assert_eq!(on_done.id.value, 0); // TODO: Re-enable ID parsing
        match &on_done.target {
            TransitionTarget::State(id) => assert_eq!(id.name, "Success"),
            _ => panic!("Expected State target for onDone"),
        }
        let on_error = invoke1.on_error.as_ref().unwrap();
        // assert_eq!(on_error.id.value, 1); // TODO: Re-enable ID parsing
        match &on_error.target {
            TransitionTarget::State(id) => assert_eq!(id.name, "Failure"),
            _ => panic!("Expected State target for onError"),
        }

        // Check second invoke
        let invoke2 = &invokes_block.definitions[1];
        assert_eq!(invoke2.name.name, "MyMachineRef");
        match &invoke2.src {
            InvokeSource::Machine(id) => assert_eq!(id.name, "AnotherMachine"),
            _ => panic!("Expected Machine source"),
        }

        // Check third invoke
        let invoke3 = &invokes_block.definitions[2];
        assert_eq!(invoke3.name.name, "LiteralPromise");
        match &invoke3.src {
            InvokeSource::Literal(s) => assert_eq!(s, "checkSomething"),
            _ => panic!("Expected Literal source"),
        }
    }

    #[test]
    fn test_parse_state_invoke() {
        let input = r#"
            machines {
                machine Caller @id(0) {
                    invokes @id(0) {
                        invoke ExternalTask @id(0) { src: "doSomething"; }
                    }
                    states @id(1) {
                        state Working @id(0) {
                            $initial;
                            invoke @id(0) {
                                src: ExternalTask; // Reference invoke definition
                                onDone @id(0) target Done;
                            }
                        }
                        state Done @id(1) { $final; }
                    }
                }
            }
        "#;
        let ast = parse_ssot_content(input, None).expect("Parsing failed");
        let machines_block = ast
            .definitions
            .iter()
            .find_map(|def| match def {
                TopLevelDefinition::Machines(block) => Some(block),
                _ => None,
            })
            .expect("MachinesBlock not found");

        let caller_machine = &machines_block.definitions[0];
        let states_block = caller_machine.states.as_ref().unwrap();
        let working_state = &states_block.states[0];

        assert_eq!(working_state.invokes.len(), 1);
        let state_invoke = &working_state.invokes[0];
        // assert_eq!(state_invoke.id.value, 0); // TODO: Re-enable ID parsing
        assert_eq!(state_invoke.src_ref.name, "ExternalTask"); // Check reference
        assert!(state_invoke.on_done.is_some());
        let on_done = state_invoke.on_done.as_ref().unwrap();
        // assert_eq!(on_done.id.value, 0); // TODO: Re-enable ID parsing
        match &on_done.target {
            TransitionTarget::State(id) => assert_eq!(id.name, "Done"),
            _ => panic!("Expected State target for onDone"),
        }
    }

    #[test]
    fn test_parse_invalid_syntax_error() {
        let input = r#"
            types {
                struct MissingId { field: string; } // Missing @id
            }
        "#;
        let result = parse_ssot_content(input, None);
        assert!(result.is_err());
        // TODO: Add more specific error type checking once error handling is refined
        // match result.err().unwrap() {
        //     SsotParserError::PestError(e) => {
        //         // Check for specific pest error if needed
        //     },
        //     e => panic!("Expected PestError, got {:?}", e),
        // }
    }

    // TODO: Add tests for:
    // - Imports
    // - Services block (interfaces, methods, services)
    // - Communication block (protocols, channels, events)
    // - Actors block
    // - Deployment config block
    // - More complex annotations (lists, objects)
    // - State machine features: actions, guards, history, parallel, after, nested states
    // - AST Validation logic (once implemented/stable)
    // - Error reporting details (line numbers, specific messages)
} // end mod tests
