import Parser, { Point, SyntaxNode, Tree, Language } from 'web-tree-sitter'; // Use standard Node.js import
import * as path from 'node:path'; // Use node:path for paths
import * as fs from 'node:fs/promises'; // Use node:fs for file access
import { fileURLToPath } from 'node:url'; // Helper for __dirname in ESM context if needed

import {
  SsotAst,
  AstNode,
  Span,
  // Import necessary AST node types from ast.ts as needed
  IdentifierNode,
  StringLiteralNode,
  NumberLiteralNode,
  BooleanLiteralNode,
  IdAnnotationNode,
  GenericAnnotationNode,
  AnnotationNode, // Added AnnotationNode
  ImportStatementNode,
  PrimitiveTypeNode, // Added PrimitiveTypeNode
  CustomTypeReferenceNode, // Added CustomTypeReferenceNode
  TopLevelBlockNode, // Added TopLevelBlockNode
  // ... add other types used in the transformation
} from "./ast.ts";

// Helper to get __dirname in ESM modules (if needed, depends on how script is run)
// const __filename = fileURLToPath(import.meta.url);
// const __dirname = path.dirname(__filename);

// Helper function to create Span from SyntaxNode
function nodeToSpan(node: SyntaxNode): Span {
  return { start: node.startPosition, end: node.endPosition };
}

// Helper function to extract text from a node
function nodeToText(node: SyntaxNode, sourceCode: string): string {
    return sourceCode.substring(node.startIndex, node.endIndex);
}

// Main parser instance (initialize once)
let parser: Parser | null = null;
// Language doesn't need to be stored globally if set per parse
// let ssotLanguage: Language | null = null;

/**
 * Initializes the Tree-sitter parser with WASM and SSOT grammar.
 * Must be called before parsing.
 */
export async function initializeParser(): Promise<void> {
  if (parser) return; // Already initialized

  try {
    // Initialize the library. It will try to load tree-sitter.wasm automatically.
    // If this fails, we might need the locateFile option or copy the wasm file.
    await Parser.init();
    parser = new Parser();
    console.log("Tree-sitter WASM runtime initialized.");

    // Load the SSOT grammar WASM file.
    // Construct the path relative to the current file or use an absolute path.
    // __dirname might not work directly in all module contexts, adjust as needed.
    // For simplicity, let's assume it's relative to the project root for now.
    const grammarPath = path.resolve("src/tree_sitter/ssot.wasm");

    // Check if grammar file exists before attempting to load
    try {
        await fs.access(grammarPath);
    } catch (e) {
         console.error(`Grammar file not found at: ${grammarPath}`);
         throw new Error(`SSOT grammar file not found.`);
    }


    const ssotLanguageWasm = await fs.readFile(grammarPath);
    const ssotLanguage = await Parser.Language.load(ssotLanguageWasm);
    parser.setLanguage(ssotLanguage);
    console.log("Tree-sitter parser initialized with SSOT grammar.");

  } catch (error) {
    console.error("Error initializing Tree-sitter parser:", error);
    throw new Error("Failed to initialize Tree-sitter parser.");
  }
}

/**
 * Parses the SSOT content string and transforms the CST into an AST.
 * Ensure initializeParser() has been called successfully before using this.
 *
 * @param ssotContent The string content of the .ssot file.
 * @param filePath Optional file path for error reporting context.
 * @returns The root SsotAst node.
 * @throws Error if parser is not initialized or parsing fails.
 */
export function parseSsotContent(ssotContent: string, filePath?: string): SsotAst {
  if (!parser) {
    throw new Error("Parser not initialized. Call initializeParser() first.");
  }
  // Language should be set during initialization, re-setting might not be needed
  // parser.setLanguage(ssotLanguage);

  const tree: Tree = parser.parse(ssotContent);
  const rootNode: SyntaxNode = tree.rootNode;

  // Check for parsing errors
  if (rootNode.hasError()) {
      console.error(`Parsing errors found in ${filePath ?? 'input'}:`);
      // TODO: Implement more detailed error traversal
      throw new Error(`Syntax errors encountered during parsing.`);
  }

  // Start the transformation process from the root CST node
  const transformedRoot = transformNode(rootNode, ssotContent);
  if (!transformedRoot || transformedRoot.kind !== "SsotRoot") {
      throw new Error("Failed to transform the root node into SsotAst.");
  }
  // Type assertion is safe here due to the check above
  return transformedRoot as SsotAst;
}

// --- CST to AST Transformation Logic ---

/**
 * Generic function to transform a SyntaxNode into an AstNode.
 * This acts as a dispatcher based on the node type.
 *
 * IMPORTANT: This is a placeholder and needs to be implemented fully
 *            to handle all node types defined in grammar.js.
 */
function transformNode(node: SyntaxNode, sourceCode: string): AstNode | null {
  const span = nodeToSpan(node);

  // Dispatch based on the type name from the grammar (node.type)
  switch (node.type) {
    case "source_file":
      return transformSourceFile(node, sourceCode);

    case "file_id":
        const idValueNodeHex = node.childForFieldName("value");
        if (idValueNodeHex?.type === "hex_integer_literal") {
            return {
                kind: "IdAnnotation",
                value: parseInt(nodeToText(idValueNodeHex, sourceCode), 16),
                span: span,
            } satisfies IdAnnotationNode;
        }
        console.warn("Unexpected structure for file_id:", node.text);
        return null;

    case "import_statement":
      const pathNode = node.childForFieldName("path");
      const transformedPath = pathNode ? transformNode(pathNode, sourceCode) : null;
      if (transformedPath?.kind === "StringLiteral") {
        return {
            kind: "ImportStatement",
            path: transformedPath as StringLiteralNode,
            span: span,
        } satisfies ImportStatementNode;
      }
      console.warn("Unexpected structure for import_statement:", node.text);
      return null;

    case "identifier":
      return {
        kind: "Identifier",
        name: nodeToText(node, sourceCode),
        span: span,
      } satisfies IdentifierNode;

    case "string_literal":
        const stringValue = nodeToText(node, sourceCode).slice(1, -1);
        return {
            kind: "StringLiteral",
            value: stringValue,
            span: span,
        } satisfies StringLiteralNode;

    case "integer_literal":
    case "hex_integer_literal":
        const numText = nodeToText(node, sourceCode);
        const isHex = node.type === 'hex_integer_literal' || numText.toLowerCase().startsWith('0x');
        return {
            kind: "NumberLiteral",
            value: parseInt(numText, isHex ? 16 : 10),
            span: span,
        } satisfies NumberLiteralNode;

    case "float_literal":
        return {
            kind: "NumberLiteral",
            value: parseFloat(nodeToText(node, sourceCode)),
            span: span,
        } satisfies NumberLiteralNode;

    case "boolean_literal":
        return {
            kind: "BooleanLiteral",
            value: nodeToText(node, sourceCode) === "true",
            span: span,
        } satisfies BooleanLiteralNode;

    // --- Block Transformations (Placeholders) ---
    case "types_block":
    case "services_block":
    case "machines_block":
    case "actors_block":
    case "communication_block":
    case "deployment_config_block":
    case "dependencies_block":
      return transformTopLevelBlock(node, sourceCode);

    // --- Annotation Transformations ---
    case "id_annotation":
        const idIntValueNode = node.childForFieldName("value");
        if (idIntValueNode?.type === "integer_literal") {
             return {
                 kind: "IdAnnotation",
                 value: parseInt(nodeToText(idIntValueNode, sourceCode), 10),
                 span: span,
             } satisfies IdAnnotationNode;
        }
        console.warn("Unexpected structure for id_annotation:", node.text);
        return null;

    case "generic_annotation":
        const nameNode = node.childForFieldName("name");
        const valueNode = node.childForFieldName("value");
        const transformedName = nameNode ? transformNode(nameNode, sourceCode) : null;
        if (transformedName?.kind === "Identifier") {
            return {
                kind: "GenericAnnotation",
                name: transformedName as IdentifierNode,
                value: valueNode ? transformNode(valueNode, sourceCode) ?? undefined : undefined,
                span: span,
            } satisfies GenericAnnotationNode;
        }
        console.warn("Unexpected structure for generic_annotation:", node.text);
        return null;

    // --- Type Reference Transformations (Placeholders) ---
    case "primitive_type":
        const typeNamePrimitive = nodeToText(node, sourceCode);
        if (["u8", "u16", "u32", "u64", "i8", "i16", "i32", "i64", "f32", "f64", "bool", "string", "timestamp"].includes(typeNamePrimitive)) {
             return {
                 kind: "PrimitiveType",
                 typeName: typeNamePrimitive as PrimitiveTypeNode["typeName"],
                 span: span,
             } satisfies PrimitiveTypeNode;
        }
        console.warn(`Unknown primitive type: ${typeNamePrimitive}`);
        return null;

    case "list_type":
    case "optional_type":
    case "map_type":
    // case "custom_type": // Assuming identifier is used directly or wrapped
      return transformComplexTypeReference(node, sourceCode);

    // --- Definition Transformations (Placeholders) ---
    case "struct_definition":
    case "enum_definition":
    case "service_definition":
    case "interface_definition":
    case "machine_definition":
      // TODO: Add specific transformation functions for each definition type
      console.warn(`Transformation not yet implemented for node type: ${node.type}`);
      return null; // Placeholder

    // Add cases for all other node types from your grammar...
    // e.g., fields, variants, states, transitions, actions, guards, etc.

    case "comment": // Comments are often ignored in AST
      return null;

    default:
      // Handle unexpected or unhandled node types
      // Only log warnings for non-essential/structural nodes.
      // Throw errors for critical unhandled types if necessary.
      if (node.isNamed) { // Only warn for named nodes we haven't explicitly handled
          console.warn(`Unhandled named node type in transformation: ${node.type} ("${node.text}")`);
      }
      // For unnamed nodes or structural elements, we might traverse children
      // For simplicity now, we just return null for unhandled types.
      return null;
  }
}

/**
 * Transforms the root 'source_file' node.
 */
function transformSourceFile(node: SyntaxNode, sourceCode: string): SsotAst | null {
  if (node.type !== "source_file") return null;

  const fileIdNode = node.childForFieldName("file_id");
  const imports: ImportStatementNode[] = [];
  const blocks: TopLevelBlockNode[] = []; // Use specific type

  // Iterate over children to find imports and blocks
  node.namedChildren.forEach((child: SyntaxNode) => {
    // Skip file_id as it's handled separately
    if (child.id === fileIdNode?.id) return;

    const transformedChild = transformNode(child, sourceCode);
    if (transformedChild) {
      if (transformedChild.kind === "ImportStatement") {
        imports.push(transformedChild as ImportStatementNode);
      } else if (isTopLevelBlock(transformedChild)) {
         blocks.push(transformedChild);
      } else if (!(transformedChild.kind === "IdAnnotation" && child.type === 'file_id')) {
         // Only warn if it's not the already handled file_id annotation
         console.warn(`Unexpected top-level AST node kind: ${transformedChild.kind}`, transformedChild);
      }
    }
  });

  const fileIdAst = fileIdNode ? transformNode(fileIdNode, sourceCode) as IdAnnotationNode | null : null;

  // Transform top-level annotations as well
  const topLevelAnnotations = transformAnnotations(node, sourceCode)
                                .filter(a => a.kind === "GenericAnnotation") as GenericAnnotationNode[];

  return {
    kind: "SsotRoot",
    fileId: fileIdAst ?? undefined,
    imports: imports,
    blocks: blocks, // Now correctly typed
    span: nodeToSpan(node),
    annotations: topLevelAnnotations, // Add top-level annotations here
  } satisfies SsotAst;
}

/** Type guard to check if a node is a top-level block */
function isTopLevelBlock(node: AstNode): node is TopLevelBlockNode {
    // Add all valid top-level block kinds here
    return [
        "TypesBlock",
        "ServicesBlock",
        "MachinesBlock",
        "ActorsBlock",
        "CommunicationBlock",
        "DeploymentConfigBlock",
        "DependenciesBlock"
    ].includes(node.kind);
}


/**
 * Placeholder for transforming top-level blocks like 'types {}', 'services {}', etc.
 */
function transformTopLevelBlock(node: SyntaxNode, sourceCode: string): TopLevelBlockNode | null {
    const blockKindMapping: { [key: string]: TopLevelBlockNode['kind'] } = { // Use specific kinds
        "types_block": "TypesBlock",
        "services_block": "ServicesBlock",
        "machines_block": "MachinesBlock",
        "actors_block": "ActorsBlock",
        "communication_block": "CommunicationBlock",
        "deployment_config_block": "DeploymentConfigBlock",
        "dependencies_block": "DependenciesBlock",
    };
    const blockKind = blockKindMapping[node.type];
    if (!blockKind) { // Check if mapping exists
        console.error(`No AST mapping found for block type: ${node.type}`);
        return null;
    }

    const definitions: AstNode[] = [];
    const blockBody = node.childForFieldName("body"); // Assuming grammar uses 'body' for the {} content

    if (blockBody) {
        blockBody.namedChildren.forEach((child: SyntaxNode) => {
            if (child.type === "id_annotation" || child.type === "generic_annotation") return;

            const transformedDef = transformNode(child, sourceCode);
            if (transformedDef) {
                definitions.push(transformedDef);
            }
        });
    }

    // Cast to specific block type - requires more detailed implementation
    // to be truly type-safe (e.g., ensuring definitions match the block type)
    const blockNode = {
        kind: blockKind,
        definitions: definitions, // This needs validation/casting based on blockKind
        span: nodeToSpan(node),
        annotations: transformAnnotations(node, sourceCode),
    } as TopLevelBlockNode; // Use cast for now, but ideally return specific type

    // Validate the constructed node if possible, or refine return type
    if (!isTopLevelBlock(blockNode)) {
        console.error(`Constructed block node is not a valid TopLevelBlockNode: ${blockKind}`);
        return null;
    }

    return blockNode;
}

/**
 * Placeholder for transforming type references like 'string', 'list<u8>', 'optional<MyStruct>'.
 */
function transformComplexTypeReference(node: SyntaxNode, sourceCode: string): AstNode | null {
     // TODO: Implement
     console.warn(`Transformation not yet implemented for complex type reference: ${node.type}`);

     // Temporary fallback for identifiers used as custom types (if grammar allows)
     if (node.type === 'identifier') {
         const typeName = nodeToText(node, sourceCode);
         return {
             kind: "CustomTypeReference",
             name: { kind: "Identifier", name: typeName, span: nodeToSpan(node) } satisfies IdentifierNode,
             span: nodeToSpan(node),
         } satisfies CustomTypeReferenceNode;
     }

     return null;
}

/**
 * Extracts and transforms annotations attached to a node.
 * Assumes annotations are direct children or attached via a specific field.
 * Adjust based on how annotations are structured in your grammar.js.
 */
function transformAnnotations(node: SyntaxNode, sourceCode: string): AnnotationNode[] {
    const annotations: AnnotationNode[] = [];
    // Add type annotation to child parameter
    node.namedChildren.forEach((child: SyntaxNode) => {
        if (child.type === "id_annotation" || child.type === "generic_annotation") {
            const annotationAst = transformNode(child, sourceCode);
            // Check specific kinds before pushing
            if (annotationAst?.kind === "IdAnnotation" || annotationAst?.kind === "GenericAnnotation") {
                annotations.push(annotationAst);
            }
        }
    });
    return annotations;
}

// TODO: Implement transformation functions for all other definition types:
// - transformStructDefinition
// - transformEnumDefinition
// - transformFieldDefinition
// - transformEnumVariant
// - transformServiceDefinition
// - transformInterfaceDefinition
// - transformMethodDefinition
// - transformParameter
// - transformMachineDefinition
// - transformContextDefinition
// - transformActionsDefinition (and individual ActionDefinition)
// - transformGuardsDefinition (and individual GuardDefinition)
// - transformInvokesDefinition (and individual InvokeDefinition)
// - transformStatesDefinition (and individual StateDefinition, TransitionDefinition, etc.)
// - transformActorDefinition
// - transformProtocolDefinition
// - transformChannelDefinition
// - transformEventDefinition
// - transformEnvironmentDefinition
// - transformInfrastructureDefinition
// - transformDeploymentDefinition
// - transformDependenciesBlock (and Rust/NodeJS specific blocks/entries)
// - transformAnnotationValue (handling lists, objects within annotations)
