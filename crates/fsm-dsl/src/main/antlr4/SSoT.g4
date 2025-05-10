grammar SSoT;

@header {
package ssot_parser;
}

// ==========================================
//  Parser Rules
// ==========================================

// Entry point for the parser - Allow annotations at various top-level points
file: fileId? annotation* importStatement* annotation* definitionBlock* annotation* EOF;

fileId
    : AT HEX_ID SEMI
    ;

importStatement
    : IMPORT STRING SEMI
    ;

definitionBlock
    : typesBlock
    | servicesBlock
    | machinesBlock
    | actorsBlock
    | communicationBlock
    | deploymentConfigBlock
    | dependenciesBlock
    ;

// --- Annotations ---
annotationName
    : ID | CHANNEL | PROTOCOL | DESCRIPTION | VALIDATE | DB | META | ROUTE | PUBLISHES | INPUT | ONDONE | ONERROR | SRC | VERSION | TYPE | PARAMETERS | IMPLEMENTS | COMMUNICATESWITH | TAGS | OPERATIONID | COMPLEXITY | RESPONSIBLETEAM | FINAL | DEFAULT | TS_OUT | CAPNP_OUT | MERMAID_OUT | DOT_OUT | ZOD_OUT | JSONSCHEMA_OUT | PROTO_OUT | AVRO_OUT | OPENAPI_OUT | GRPC_OUT | GRAPHQL_OUT | ASYNCAPI_OUT | SQL_OUT | PRISMA_OUT | DRIZZLE_OUT // Added known keywords
    ;

annotation
    : AT ID LPAREN INT RPAREN // @id(integer) - No semicolon
    | DOLLAR annotationName LPAREN annotationValue? RPAREN // $name(...) - No semicolon
    | DOLLAR annotationName SEMI // $flag; - Has semicolon
    ;

// Annotation values: Assume a list of key:value pairs OR a single value inside parentheses
annotationValue
    : {_input.LA(2) == COLON}? attributePairList // Use _input.LA(2) for Java target
    | value                             // Otherwise, it's a single value
    ;

// Value definition: handles primitives, references, objects, and arrays recursively
value
    : primitiveValue
    | referenceValue  // ID or ID.ID...
    | objectValue     // { key: value, ... }
    | arrayValue      // [ value, ... ]
    ;

attributePairList
    : attributePair (COMMA attributePair)*
    ;

// Modify attributePair for debugging
attributePair
    : ID COLON (primitiveValue | referenceValue | objectValue | arrayValue) // Explicitly list value alternatives
    ;

primitiveValue
    : STRING | INT | FLOAT | BOOLEAN
    ;

referenceValue
    : ID (DOT ID)*
    ;

objectValue
    : LBRACE attributePairList? RBRACE // Object content is also key:value pairs
    ;

arrayValue
    : LBRACK valueList? RBRACK // Array content is a list of values
    ;

valueList
    : value (COMMA value)*
    ;


// --- Type Definitions Block ---
typesBlock
    : TYPES LBRACE annotation* typeDefinition* RBRACE
    ;

typeDefinition
    : structDefinition
    | enumDefinition
    ;

structDefinition
    : STRUCT ID annotation* LBRACE annotation* fieldDefinition* RBRACE
    ;

fieldDefinition // Semicolon is now required at the end
    : ID COLON typeExpr annotation* (LBRACE annotation* RBRACE)? SEMI
    ;

enumDefinition
    : ENUM ID annotation* LBRACE annotation* enumVariant* RBRACE
    ;

enumVariant // Semicolon is now required at the end
    : ID annotation* SEMI
    ;

// --- Type Expressions ---
typeExpr
    : primitiveTypeName // e.g., string, bool, u32
    | referenceValue    // Reference to struct/enum by ID.ID...
    | OPTIONAL LT typeExpr GT
    | LIST LT typeExpr GT
    | MAP LT typeExpr COMMA typeExpr GT
    ;

// Specific token types for primitive type names
primitiveTypeName
    : T_U8 | T_U16 | T_U32 | T_U64
    | T_I8 | T_I16 | T_I32 | T_I64
    | T_F32 | T_F64
    | T_BOOL
    | T_STRING
    | T_TIMESTAMP
    ;


// --- Actor Definitions Block ---
actorsBlock
    : ACTORS LBRACE annotation* actorDefinition* RBRACE
    ;

// Actor details primarily defined via annotations ($description, $type)
actorDefinition
    : ACTOR ID annotation* LBRACE annotation* RBRACE
    ;

// --- Communication Definitions Block ---
communicationBlock
    : COMMUNICATION LBRACE annotation* communicationDefinition* RBRACE
    ;

communicationDefinition
    : protocolDefinition
    | channelDefinition
    | eventDefinition
    ;

// Protocol details primarily via annotations
protocolDefinition
    : PROTOCOL ID annotation* LBRACE annotation* RBRACE
    ;

// Channel details primarily via annotations ($description, $parameters if needed)
channelDefinition
    : CHANNEL ID annotation* LBRACE annotation* channelBodyElement* RBRACE
    ;

// Allow parameters definition inside channel block if needed (alternative to annotation)
channelBodyElement
    : annotation
    | channelParameterDefinition
    ;

channelParameterDefinition
    : PARAMETERS LBRACE attributePairList? RBRACE // Example: parameters { userId: string }
    ;

// Event definition contains fields similar to structs
eventDefinition
    : EVENT ID annotation* LBRACE annotation* fieldDefinition* RBRACE
    ;


// --- Service Definitions Block ---
servicesBlock
    : SERVICES LBRACE annotation* serviceElement* RBRACE
    ;

serviceElement
    : interfaceDefinition
    | serviceDefinition
    ;

interfaceDefinition
    : INTERFACE ID annotation* LBRACE annotation* methodDefinition* RBRACE
    ;

// Method definition requires semicolon at the end
methodDefinition
    : ID annotation* LPAREN parameterList? RPAREN (ARROW typeExpr)? (LBRACE annotation* RBRACE)? SEMI
    ;

parameterList
    : parameter (COMMA parameter)*
    ;

parameter
    : ID COLON typeExpr
    ;

// Service details via annotations ($implements, $communicatesWith, $route, etc.)
serviceDefinition
    : SERVICE ID annotation* (EXTENDS referenceValue)? LBRACE annotation* RBRACE
    ;

// --- State Machine Definitions Block ---
machinesBlock
    : MACHINES LBRACE annotation* machineDefinition* RBRACE
    ;

machineDefinition
    : MACHINE ID annotation* LBRACE annotation* machineBodyElement* RBRACE
    ;

machineBodyElement
    : contextDefinition
    | actionsDefinition
    | guardsDefinition
    | invokesDefinition
    | statesDefinition
    | annotation
    ;

contextDefinition
    : CONTEXT annotation* LBRACE annotation* contextField* RBRACE
    ;

// Context field requires semicolon at the end
contextField
    : ID COLON typeExpr annotation* (LBRACE annotation* RBRACE)? SEMI
    ;

actionsDefinition
    : ACTIONS annotation* LBRACE annotation* actionDefinition* RBRACE
    ;

// Action definition requires semicolon at the end
actionDefinition
    : ID annotation* (LPAREN ID? (COMMA ID)? RPAREN)? (COLON typeExpr)? SEMI
    ;

guardsDefinition
    : GUARDS annotation* LBRACE annotation* guardDefinition* RBRACE
    ;

// Guard definition requires semicolon at the end
guardDefinition
    : ID annotation* (LPAREN ID? RPAREN)? ARROW T_BOOL SEMI
    ;

invokesDefinition
    : INVOKES annotation* LBRACE annotation* invokeDefinition* RBRACE
    ;

// Invoke definition details primarily via annotations
invokeDefinition
    : ID annotation* LBRACE annotation* invokeDefinitionBody? RBRACE // Added invokeDefinitionBody
    ;

// Added rule for content of invokeDefinition
invokeDefinitionBody
    : invokeAttribute (SEMI? invokeAttribute)* // Allow attributes separated by optional semicolon
    ;

// Added rule for individual attributes within invokeDefinition
invokeAttribute
    : invokeSrc
    | invokeInputMapping
    | invokeOutputMapping
    | invokeOnDone
    | invokeOnError
    | annotation // Allow annotations directly as attributes
    ;

invokeSrc : SRC COLON invokeSource ;
invokeInputMapping : INPUT COLON LBRACE keyValuePairList? RBRACE ;
invokeOutputMapping : OUTPUT COLON LBRACE keyValuePairList? RBRACE ; // Assuming similar structure for output
invokeOnDone : ONDONE COLON invokeCompletion ;
invokeOnError : ONERROR COLON invokeCompletion ;

invokeCompletion
    : annotation* (actionReferenceList | transitionSpec)
    ;

invokeSource
    : STRING // e.g. "serviceName.methodName" or "actorName"
    | expressionValue // For dynamic/reference based source
    ;

expressionValue // A placeholder for more complex expressions if needed later
    : value
    ;

keyValuePairList
    : keyValuePair (COMMA keyValuePair)*
    ;

keyValuePair
    : STRING COLON value // Key is always a string
    ;


statesDefinition
    : STATES annotation* LBRACE annotation* stateDefinitionOrHistoryState* RBRACE // Changed stateDefinition*
    ;

stateDefinitionOrHistoryState // New rule
    : stateDefinition
    | historyDefinition
    ;

stateDefinition
    : stateName=ID annotation* LBRACE annotation* stateBodyElement* RBRACE
    ;

stateBodyElement // This rule might need to be explicitly defined if not already, or alternatives added where it's used.
    : onEntryExit
    | invokeState
    | onTransition
    | afterTransition
    | ifTransitionStatement // Added new alternative
    | statesDefinition      // Nested states block
    | historyDefinition
    | annotation            // Annotations directly in the state body
    ;

// onEntry/onExit require semicolon
onEntryExit : (ON_ENTRY | ON_EXIT) actionReference SEMI;
actionReference : referenceValue; // e.g., actionName, actions.actionName

// Invocation within a state requires semicolon
invokeState : INVOKE ID annotation* LBRACE annotation* invokeStateBody? RBRACE SEMI; // invokeStateBody added for consistency

// Added rule for content of invokeState, if needed for more complex invoke bodies
invokeStateBody
    : invokeAttribute (SEMI? invokeAttribute)*
    ;

onTransition // Requires semicolon
    : ON event=ID annotation* transitionSpec SEMI
    ;

afterTransition // Requires semicolon
    : AFTER duration annotation* transitionSpec SEMI
    ;

// New rule for If-Transitions
ifTransitionStatement // Requires semicolon
    : IF condition=guardReference annotation* transitionSpec SEMI
    ;

// Transition requires target state, options must be in a block {}
transitionSpec
    : TRANSITION targetState (LBRACE transitionOptions RBRACE)? // No semicolon here, handled by caller
    ;

targetState
    : referenceValue // e.g., StateName, Parent.Child
    | DOT HISTORY    // .history
    ;

// Options within the transition block {}
transitionOptions
    : transitionOption (COMMA? transitionOption)* // Allow options separated by comma or just space/newline
    | annotation* // Allow only annotations inside the block as well
    ;

transitionOption
    : ACTION COLON actionReferenceList
    | GUARD COLON guardReferenceList
    | ALLOWED_ACTORS COLON actorReferenceList
    | annotation // Allow annotations mixed with options
    ;

// Allow single reference or list in brackets
actionReferenceList: LBRACK actionReference (COMMA actionReference)* RBRACK | actionReference;
guardReferenceList: LBRACK guardReference (COMMA guardReference)* RBRACK | guardReference;
actorReferenceList: LBRACK referenceValue (COMMA referenceValue)* RBRACK | referenceValue;

guardReference: referenceValue (LPAREN NOT RPAREN)?; // guardName or guardName(not)

duration
    : INT DURATION_UNIT
    ;

// History definition requires semicolon
historyDefinition
    : HISTORY historyType=(SHALLOW | DEEP)? annotation* (TARGET targetRef=ID)? transitionSpec? SEMI // Made target optional, added transitionSpec
    ;

// --- Deployment Configuration Block ---
deploymentConfigBlock
    : DEPLOYMENT_CONFIG LBRACE annotation* deploymentElement* RBRACE
    ;

deploymentElement
    : environmentDefinition
    | infrastructureDefinition
    | deploymentDefinition
    ;

// Environment attributes via annotations or variables block
environmentDefinition
    : ENVIRONMENT ID annotation* (EXTENDS referenceValue)? LBRACE annotation* variablesBlock? RBRACE
    ;

variablesBlock
    : VARIABLES LBRACE variableAssignment* RBRACE
    ;

// Variable assignment requires semicolon
variableAssignment
    : ID COLON value SEMI
    ;

// Infrastructure attributes via annotations or direct attribute assignments
infrastructureDefinition
    : INFRASTRUCTURE ID annotation* LBRACE (annotation | attributeAssignment)* RBRACE
    ;

// Deployment attributes via annotations or direct attribute assignments
deploymentDefinition
    : DEPLOYMENT ID annotation* LBRACE (annotation | attributeAssignment)* RBRACE
    ;

// Reusable attribute assignment rule requires semicolon
attributeAssignment
    : ID COLON value SEMI
    ;

// --- Dependencies Block (NEW) ---
dependenciesBlock
    : DEPENDENCIES LBRACE annotation* targetDependencyBlock* RBRACE
    ;

targetDependencyBlock
    : targetType STRING annotation* LBRACE annotation* dependencyEntry* RBRACE
    ;

targetType
    : RUST
    | NODEJS
    ;

// Dependency entry details via annotations or direct attribute assignments
dependencyEntry
    : ID annotation* LBRACE (annotation | dependencyAttribute)* RBRACE
    ;

// Dependency attributes: specific handling for features, rest use attributeAssignment
dependencyAttribute
    : (FEATURES COLON arrayValue) // features: ["feat1", "feat2"] - No semicolon needed?
    | attributeAssignment          // version: "...", dev: true, path: "...", etc. - Has semicolon via attributeAssignment
    | annotation                   // Annotations don't have semicolon here
    ;


// ==========================================
//  Lexer Rules
// ==========================================

// --- Keywords ---
IMPORT: 'import';
TYPES: 'types';
ACTORS: 'actors';
COMMUNICATION: 'communication';
SERVICES: 'services';
MACHINES: 'machines';
DEPLOYMENT_CONFIG: 'deployment_config';
DEPENDENCIES: 'dependencies';
STRUCT: 'struct';
ENUM: 'enum';
MACHINE: 'machine';
CONTEXT: 'context';
ACTIONS: 'actions';
GUARDS: 'guards';
INVOKES: 'invokes';
STATES: 'states';
ON: 'on';
AFTER: 'after';
TRANSITION: 'transition';
HISTORY: 'history';
SHALLOW: 'shallow';
DEEP: 'deep';
TARGET: 'target';
SERVICE: 'service';
INTERFACE: 'interface';
PROTOCOL: 'protocol';
ACTOR: 'actor';
CHANNEL: 'channel';
EVENT: 'event';
EXTENDS: 'extends';
ENVIRONMENT: 'environment';
INFRASTRUCTURE: 'infrastructure';
DEPLOYMENT: 'deployment';
RUST: 'rust';
NODEJS: 'nodejs';
VARIABLES: 'variables';
PARAMETERS: 'parameters'; // Added for channel parameters block
ACTION: 'action'; // Used in transition options
GUARD: 'guard'; // Used in transition options
ALLOWED_ACTORS: 'allowedActors'; // Used in transition options
NOT: 'not'; // Used in guard negation
INVOKE: 'invoke'; // Used within state
ON_ENTRY: 'onEntry';
ON_EXIT: 'onExit';
OUTPUT: 'output'; // Added for invokeOutputMapping
DESCRIPTION: 'description'; // Added for annotations
FEATURES: 'features'; // Used in Rust dependencies

// Primitive Types (used in parser rules via primitiveTypeName)
T_U8: 'u8'; T_U16: 'u16'; T_U32: 'u32'; T_U64: 'u64';
T_I8: 'i8'; T_I16: 'i16'; T_I32: 'i32'; T_I64: 'i64';
T_F32: 'f32'; T_F64: 'f64';
T_BOOL: 'bool';
T_STRING: 'string';
T_TIMESTAMP: 'timestamp';
OPTIONAL: 'optional';
LIST: 'list';
MAP: 'map';

// --- Identifiers ---
// Allow PascalCase, camelCase, snake_case, UPPER_SNAKE_CASE
ID: [a-zA-Z_] [a-zA-Z0-9_]* ;

// --- Literals ---
HEX_ID : '0x' [0-9a-fA-F]+ ;
INT    : [0-9]+ ;
FLOAT  : [0-9]+ '.' [0-9]+ ;
STRING : '"' ( '\\' . | ~["\\] )* '"' ; // Handles basic escapes
BOOLEAN: 'true' | 'false';
DURATION_UNIT: 'ms' | 's' | 'm' | 'h';

// --- Symbols ---
AT      : '@';
DOLLAR  : '$';
LPAREN  : '(';
RPAREN  : ')';
LBRACE  : '{';
RBRACE  : '}';
LBRACK  : '[';
RBRACK  : ']';
LT      : '<';
GT      : '>';
SEMI    : ';';
COLON   : ':';
COMMA   : ',';
DOT     : '.';
ARROW   : '->';

// --- Comments ---
COMMENT
    : ('#' | '//') ~[\r\n]* -> skip // Combine patterns and apply skip once
    ;
ML_COMMENT
    : '/*' .*? '*/' -> skip
    ;

// --- Whitespace ---
WS : [ \t\r\n]+ -> skip ; // Skip whitespace and newlines 