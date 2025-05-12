grammar SSoT;

// import CommonLexerRules; // Removed import, lexer rules defined below.

@header {
package ssot_parser;
}

// ==========================================
// Parser Rules
// ==========================================

// Entry point
file
    : definitionBlock* EOF
    ;

// Top-level organization: annotations can precede major blocks
definitionBlock
    : annotation* actorsBlock        # ActorsBlockDefinition
    | annotation* typesBlock         # TypesBlockDefinition
    | annotation* servicesBlock      # ServicesBlockDefinition
    | annotation* communicationBlock # CommunicationBlockDefinition
    | annotation* machinesBlock      # MachinesBlockDefinition
    ;

// --- Actors Block ---
actorsBlock
    : ACTORS LBRACE actorDefinition* RBRACE
    ;

actorDefinition // annotations for actor itself, and details inside braces
    : annotation* ACTOR ID LBRACE annotation* RBRACE
    ;

// --- Types Block ---
typesBlock
    : TYPES LBRACE typeDefinition* RBRACE
    ;

typeDefinition // annotations for struct/enum definitions are handled by definitionBlock
    : structDefinition
    | enumDefinition
    ;

structDefinition
    : STRUCT ID LBRACE structFieldDefinition* RBRACE // No annotations directly after { for the struct itself
    ;

structFieldDefinition
    : annotation* ID COLON typeReference (LBRACE annotation* RBRACE)? SEMI // Field annotations allowed
    ;

enumDefinition
    : ENUM ID LBRACE enumVariantDefinition* RBRACE // No annotations directly after { for the enum itself
    ;

enumVariantDefinition
    : annotation* ID SEMI // Variant annotations allowed
    ;

// --- Communication Block ---
communicationBlock
    : COMMUNICATION LBRACE communicationDefinition* RBRACE
    ;

communicationDefinition // annotations for protocol/channel/event are handled by definitionBlock
    : protocolDefinition
    | channelDefinition
    | eventDefinition
    ;

protocolDefinition // Inner annotations for protocol details (e.g., $version)
    : PROTOCOL ID LBRACE annotation* RBRACE
    ;

channelDefinition // Inner annotations for channel details (e.g., $protocol)
    : CHANNEL ID LBRACE annotation* RBRACE
    ;

eventDefinition // Inner annotations and field definitions
    : EVENT ID LBRACE annotation* eventFieldDefinition* RBRACE
    ;

eventFieldDefinition // Field annotations allowed
    : annotation* ID COLON typeReference SEMI
    ;

// --- Services Block ---
servicesBlock
    : SERVICES LBRACE serviceElement* RBRACE
    ;

serviceElement // annotations for interface/service are handled by definitionBlock
    : interfaceDefinition
    | serviceDefinition
    ;

interfaceDefinition // Inner annotations (e.g., for methods)
    : INTERFACE ID LBRACE annotation* methodDefinition* RBRACE
    ;

serviceDefinition // Inner annotations ($implements)
    : SERVICE ID LBRACE annotation* RBRACE
    ;

methodDefinition // Method annotations allowed
    : annotation* ID LPAREN paramList? RPAREN (ARROW typeReference)? SEMI
    ;

// --- Machines Block ---
machinesBlock
    : MACHINES LBRACE machineDefinition* RBRACE
    ;

machineDefinition // annotations for machine are handled by definitionBlock
    : MACHINE ID LBRACE machineBodyElement* RBRACE
    ;

machineBodyElement
    : statesDefinition
    | actionsDefinition
    | guardsDefinition
    // Invokes are typically part of state transitions or state bodies
    ;

actionsDefinition
    : ACTIONS LBRACE actionDefinition* RBRACE
    ;

actionDefinition // Actions are signatures; implementation is external
    : ID LPAREN paramList? RPAREN SEMI
    ;

guardsDefinition
    : GUARDS LBRACE guardDefinition* RBRACE
    ;

guardDefinition // Guards are function-like, returning boolean (implicitly or explicitly)
    : ID LPAREN paramList? RPAREN (COLON typeReference)? SEMI // typeReference should resolve to boolean
    ;

statesDefinition
    : STATES LBRACE initialStateDefinition? stateDefinitionOrHistoryState* RBRACE
    ;

initialStateDefinition
    : INITIAL STATE ID SEMI
    ;

stateDefinitionOrHistoryState
    : stateDefinition
    | historyStateDefinition
    ;

historyStateDefinition
    : HISTORY historyType=(SHALLOW | DEEP)? (TARGET targetState=ID)? SEMI
    ;

stateDefinition
    : stateType? STATE ID stateBody
    ;

stateType : PARALLEL | FINAL ;

stateBody // A state can be a block or just a semicolon (for placeholder states)
    : LBRACE annotation* stateBodyElement* RBRACE // No semicolon after the brace for block states
    | SEMI
    ;

stateBodyElement
    : entryExitAction
    | transitionDefinition
    | invokeDefinition
    | stateDefinitionOrHistoryState // For nested states
    | annotation // Allow annotations on specific elements within a state if needed
    ;

entryExitAction
    : (ON_ENTRY | ON_EXIT) actionReference SEMI
    ;

actionReference // Reference to an action defined in the actions block
    : ID
    ;

transitionDefinition
    : ON event=ID (LBRACK guard=ID RBRACK)? (SLASH action=ID)? transitionTarget SEMI
    ;

transitionTarget
    : TARGET state=ID // Target state for a transition
    ;

invokeDefinition // Invoking an action or service
    : INVOKE src=ID (LBRACE invokeCallback* RBRACE)? SEMI
    ;

invokeCallback
    : (ONDONE | ONERROR) transitionTarget SEMI // What happens on invoke completion/error
    ;

// =========================================
// Common Parser Rules
// =========================================

annotation
    : AT ID LPAREN INT RPAREN            # IdAnnotation
    | DOLLAR annotationName annotationValue # ValueAnnotation
    ;

annotationName : ID ;

annotationValue
    : LPAREN literal RPAREN
    | LPAREN ID RPAREN // Reference type for annotation value
    | LPAREN LBRACK (literal (COMMA literal)*)? RBRACK RPAREN // Array of literals
    ;

literal
    : STRING
    | INT
    | FLOAT
    | BOOLEAN
    | NULL
    ;

paramList // Parameter list for methods, actions, guards
    : parameter (COMMA parameter)*
    ;

parameter
    : ID COLON typeReference
    ;

typeReference // Defines various ways a type can be referenced
    : simpleType
    | listType
    | mapType
    | optionalType
    | ID // Reference to a custom type (struct or enum) defined in a types block
    ;

simpleType: PRIMITIVE_TYPE | TIMESTAMP_TYPE ;
listType: LIST LT typeReference GT ;
mapType: MAP LT typeReference COMMA typeReference GT ;
optionalType: OPTIONAL LT typeReference GT ;

// =========================================
// Lexer Rules
// =========================================

// Keywords
ACTORS : 'actors';
ACTOR : 'actor';
TYPES : 'types';
STRUCT : 'struct';
ENUM : 'enum';
COMMUNICATION : 'communication';
PROTOCOL : 'protocol';
CHANNEL : 'channel';
EVENT : 'event';
SERVICES : 'services';
INTERFACE : 'interface';
SERVICE : 'service';
MACHINES : 'machines';
MACHINE : 'machine';
INITIAL : 'initial';
STATE : 'state';
STATES : 'states';
HISTORY : 'history';
SHALLOW : 'shallow';
DEEP : 'deep';
PARALLEL: 'parallel';
FINAL: 'final';
TARGET : 'target';
ACTIONS : 'actions';
GUARDS : 'guards';
ON : 'on';
INVOKE : 'invoke';
ONDONE : 'onDone';
ONERROR : 'onError';
ON_ENTRY : 'onEntry';
ON_EXIT : 'onExit';
LIST: 'list';
MAP: 'map';
OPTIONAL: 'optional';

// Primitive Types
PRIMITIVE_TYPE: T_U8 | T_U16 | T_U32 | T_U64 | T_I8 | T_I16 | T_I32 | T_I64 | T_F32 | T_F64 | T_BOOL | T_STRING ;
fragment T_U8 : 'u8';
fragment T_U16 : 'u16';
fragment T_U32 : 'u32';
fragment T_U64 : 'u64';
fragment T_I8 : 'i8';
fragment T_I16 : 'i16';
fragment T_I32 : 'i32';
fragment T_I64 : 'i64';
fragment T_F32 : 'f32';
fragment T_F64 : 'f64';
fragment T_BOOL : 'bool';
fragment T_STRING : 'string';
TIMESTAMP_TYPE: 'timestamp';

// Symbols
LBRACE : '{';
RBRACE : '}';
LPAREN : '(';
RPAREN : ')';
LBRACK : '[';
RBRACK : ']';
SEMI : ';';
COMMA : ',';
COLON : ':';
ARROW : '->';
SLASH : '/';
AT : '@';
DOLLAR : '$';
LT: '<';
GT: '>';
DOT: '.';

// Literals
STRING : '"' ( ESC | ~["\\\r\n] )* '"' ;
fragment ESC : '\\' (["\\/bfnrt] | UNICODE) ;
fragment UNICODE : 'u' HEX HEX HEX HEX ;
fragment HEX : [0-9a-fA-F] ;

INT : '-'? [0-9]+ ;
FLOAT : '-'? [0-9]+ '.' [0-9]+ ( [eE] [+\-]? [0-9]+ )? ;
BOOLEAN: 'true' | 'false' ;
NULL: 'null';

// Identifiers
ID : [a-zA-Z_] [a-zA-Z0-9_]* ;

// Whitespace and Comments
WS : [ \t\r\n]+ -> skip ;
COMMENT : '//' .*? '\n' -> skip ; 