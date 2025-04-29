import { Point } from "web-tree-sitter";

/** Represents a range in the source code. */
export interface Span {
  start: Point;
  end: Point;
  /** Optional: Reference to the underlying Tree-sitter node */
  // node?: SyntaxNode; // Commented out due to import issues with npm pkg
}

/** Base interface for all AST nodes. */
export interface AstNode {
  kind: string; // Discriminated union type identifier
  span: Span;
  annotations?: AnnotationNode[]; // Most nodes can have annotations
}

// --- Basic Building Blocks ---

/** Represents an identifier (e.g., variable name, type name). */
export interface IdentifierNode extends AstNode {
  kind: "Identifier";
  name: string;
}

/** Represents a literal value. */
export type LiteralNode = StringLiteralNode | NumberLiteralNode | BooleanLiteralNode;

export interface StringLiteralNode extends AstNode {
  kind: "StringLiteral";
  value: string;
}

export interface NumberLiteralNode extends AstNode {
  kind: "NumberLiteral";
  value: number; // Representing integer and float for now
}

export interface BooleanLiteralNode extends AstNode {
  kind: "BooleanLiteral";
  value: boolean;
}

/** Represents an annotation (@id or $name). */
export type AnnotationNode = IdAnnotationNode | GenericAnnotationNode;

/** Represents the @id(value) annotation. */
export interface IdAnnotationNode extends AstNode {
  kind: "IdAnnotation";
  value: number; // ID value
}

/** Represents a generic $name(...) or $flag annotation. */
export interface GenericAnnotationNode extends AstNode {
  kind: "GenericAnnotation";
  name: IdentifierNode;
  // TODO: Define a more specific type for annotation parameters/values
  value?: AstNode; // Could be literal, identifier, list, object etc.
}

/** Represents an import statement. */
export interface ImportStatementNode extends AstNode {
  kind: "ImportStatement";
  path: StringLiteralNode;
}

/** Represents a type reference (primitive, list, optional, map, custom). */
export type TypeReferenceNode =
  | PrimitiveTypeNode
  | ListTypeNode
  | OptionalTypeNode
  | MapTypeNode
  | CustomTypeReferenceNode;

export interface PrimitiveTypeNode extends AstNode {
  kind: "PrimitiveType";
  typeName: "u8" | "u16" | "u32" | "u64" | "i8" | "i16" | "i32" | "i64" |
            "f32" | "f64" | "bool" | "string" | "timestamp";
}

export interface ListTypeNode extends AstNode {
  kind: "ListType";
  elementType: TypeReferenceNode;
}

export interface OptionalTypeNode extends AstNode {
  kind: "OptionalType";
  innerType: TypeReferenceNode;
}

export interface MapTypeNode extends AstNode {
  kind: "MapType";
  keyType: TypeReferenceNode; // Often restricted (e.g., string, int)
  valueType: TypeReferenceNode;
}

/** Represents a reference to a user-defined type (struct, enum, etc.). */
export interface CustomTypeReferenceNode extends AstNode {
  kind: "CustomTypeReference";
  name: IdentifierNode; // Name of the referenced type
  // TODO: Add namespace/module path if namespaces are implemented
}


// --- Top-Level Structure ---

/** Represents the root of the SSOT file AST. */
export interface SsotAst extends AstNode {
  kind: "SsotRoot";
  fileId?: IdAnnotationNode; // The top-level @0x...; annotation
  imports: ImportStatementNode[];
  blocks: TopLevelBlockNode[];
}

/** Represents one of the top-level blocks (types, services, etc.). */
export type TopLevelBlockNode =
  | TypesBlockNode
  | ServicesBlockNode
  | MachinesBlockNode
  | ActorsBlockNode
  | CommunicationBlockNode
  | DeploymentConfigBlockNode
  | DependenciesBlockNode;
  // Add other top-level blocks as needed

/** Base interface for top-level blocks. */
export interface BaseBlockNode extends AstNode {
  definitions: AstNode[]; // Placeholder for definitions within the block
}

// --- Specific Block & Definition Examples (Incomplete - To be expanded) ---

export interface TypesBlockNode extends BaseBlockNode {
  kind: "TypesBlock";
  definitions: TypeDefinitionNode[];
}

export type TypeDefinitionNode = StructDefinitionNode | EnumDefinitionNode;

export interface StructDefinitionNode extends AstNode {
  kind: "Struct";
  name: IdentifierNode;
  id: IdAnnotationNode;
  fields: FieldDefinitionNode[];
}

export interface FieldDefinitionNode extends AstNode {
  kind: "Field";
  name: IdentifierNode;
  id: IdAnnotationNode;
  type: TypeReferenceNode;
  // Annotations are part of AstNode
}

export interface EnumDefinitionNode extends AstNode {
  kind: "Enum";
  name: IdentifierNode;
  id: IdAnnotationNode;
  variants: EnumVariantNode[];
}

export interface EnumVariantNode extends AstNode {
  kind: "EnumVariant";
  name: IdentifierNode;
  id: IdAnnotationNode;
  // Annotations are part of AstNode
}

export interface ServicesBlockNode extends BaseBlockNode {
  kind: "ServicesBlock";
  definitions: ServiceRelatedDefinitionNode[];
}

export type ServiceRelatedDefinitionNode = ServiceDefinitionNode | InterfaceDefinitionNode;

export interface ServiceDefinitionNode extends AstNode {
  kind: "Service";
  name: IdentifierNode;
  id: IdAnnotationNode;
  extends?: CustomTypeReferenceNode;
  // Add implements, communicatesWith, etc.
}

export interface InterfaceDefinitionNode extends AstNode {
  kind: "Interface";
  name: IdentifierNode;
  id: IdAnnotationNode;
  methods: MethodDefinitionNode[];
}

export interface MethodDefinitionNode extends AstNode {
  kind: "Method";
  name: IdentifierNode;
  id: IdAnnotationNode;
  parameters: ParameterNode[];
  returnType?: TypeReferenceNode;
}

export interface ParameterNode extends AstNode {
    kind: "Parameter";
    name: IdentifierNode;
    type: TypeReferenceNode;
}


export interface MachinesBlockNode extends BaseBlockNode {
  kind: "MachinesBlock";
  definitions: MachineDefinitionNode[];
}

export interface MachineDefinitionNode extends AstNode {
  kind: "Machine";
  name: IdentifierNode;
  id: IdAnnotationNode;
  initialState: IdentifierNode; // Set via $initial annotation processing
  context?: ContextDefinitionNode;
  actions?: ActionsDefinitionNode;
  guards?: GuardsDefinitionNode;
  invokes?: InvokesDefinitionNode;
  states: StatesDefinitionNode;
}

// --- Placeholder Definitions (To be detailed) ---

export interface ContextDefinitionNode extends AstNode { kind: "Context"; /* ... fields ... */ }
export interface ActionsDefinitionNode extends AstNode { kind: "Actions"; /* ... actions ... */ }
export interface GuardsDefinitionNode extends AstNode { kind: "Guards"; /* ... guards ... */ }
export interface InvokesDefinitionNode extends AstNode { kind: "Invokes"; /* ... invokes ... */ }
export interface StatesDefinitionNode extends AstNode { kind: "States"; /* ... states ... */ }
export interface ActorsBlockNode extends BaseBlockNode { kind: "ActorsBlock"; /* ... actors ... */ }
export interface CommunicationBlockNode extends BaseBlockNode { kind: "CommunicationBlock"; /* ... protocols, channels, events ... */ }
export interface DeploymentConfigBlockNode extends BaseBlockNode { kind: "DeploymentConfigBlock"; /* ... environments, infrastructure, deployments ... */ }
export interface DependenciesBlockNode extends BaseBlockNode { kind: "DependenciesBlock"; /* ... rust, nodejs blocks ... */ }

// TODO: Define detailed interfaces for:
// - Machine components: State, Transition, Action, Guard, Invoke, History
// - Actor definition
// - Communication definitions: Protocol, Channel, Event
// - Deployment definitions: Environment, Infrastructure, Deployment
// - Dependency definitions: TargetBlock, DependencyEntry
// - Annotation values/parameters
