# SSOT DSL Parser (Deno + Tree-sitter)

This project provides a parser and validator for the state machine description language (DSL) written in the `.ssot` (Single Source of Truth) file format. It uses [Tree-sitter](https://tree-sitter.github.io/tree-sitter/) for parsing and [Deno](https://deno.land/) (TypeScript) for implementation. The goal is to provide a foundation for model-driven development based on the `.ssot` specification.

## 目的 (Goals)

-   Define and manage state machines, types, services, etc., based on the `.ssot` format specification.
-   Provide a robust parser using Tree-sitter to generate a Concrete Syntax Tree (CST).
-   Generate a well-typed Abstract Syntax Tree (AST) in TypeScript from the CST.
-   Implement validation logic for the AST.
-   Support model-driven development practices by providing a reliable parser and validator for other tools (code generators, visualizers, etc.).

## アーキテクチャ (Architecture)

This project is being rewritten using the following technologies:

1.  **Tree-sitter:** Defines the `.ssot` grammar (`grammar.js`) and generates an efficient parser.
2.  **Deno/TypeScript:** Implements the logic to:
    -   Load and run the Tree-sitter parser.
    -   Traverse the generated CST.
    -   Build a TypeScript AST based on the CST and `src/ast.ts` definitions.
    -   Validate the generated AST (`src/validation.ts`).

## 今後のロードマップ (Roadmap)

The rewrite involves the following major steps:

1.  **Define Tree-sitter Grammar (High Priority):** Create `grammar.js` based on the existing `.ssot` syntax specification, resolving ambiguities and defining node types.
2.  **Generate Tree-sitter Parser (High Priority):** Use the Tree-sitter CLI to generate the parser (e.g., WASM binary).
3.  **Define TypeScript AST (High Priority):** Create `src/ast.ts` defining the structure of the Abstract Syntax Tree corresponding to the DSL elements.
4.  **Implement CST-to-AST Transformation (High Priority):** Write TypeScript code (`src/parser.ts`) to traverse the Tree-sitter CST and construct the TypeScript AST, including span information.
5.  **Implement Validation Logic (High Priority):** Re-implement validation rules (`src/validation.ts`) using the TypeScript AST and symbol table/scope management appropriate for TypeScript. Implement comprehensive checks (duplicate IDs/names, undefined references, type checking, structural consistency).
6.  **Implement Testing (High Priority):** Create comprehensive unit and integration tests using Deno's testing framework for the grammar, parser, AST construction, and validation logic.
7.  **Error Reporting Improvements (Medium Priority):** Ensure parser and validator errors provide clear messages and precise location information (file, line, column) using Tree-sitter's node positions.
8.  **(Optional) Code Generation (Low Priority):** Explore adding features to generate code (TypeScript, Mermaid, etc.) or other artifacts from the validated AST.

## 使用例 (Conceptual Usage Example - Deno/TypeScript)

```typescript
import { parseSsotContent } from "./src/parser.ts"; // Assuming parser entry point
import { validateAst } from "./src/validation.ts"; // Assuming validator entry point
import { SsotAst } from "./src/ast.ts"; // Assuming AST definition

async function main() {
  const ssotFilePath = "path/to/your/definition.ssot";
  const ssotContent = await Deno.readTextFile(ssotFilePath);

  try {
    // 1. Parse the content using Tree-sitter and build the AST
    const ast: SsotAst = await parseSsotContent(ssotContent, ssotFilePath);
    console.log("Successfully parsed SSOT content!");

    // 2. Validate the AST
    const validationErrors = await validateAst(ast);

    if (validationErrors.length > 0) {
      console.error("--- Validation Failed ---");
      for (const error of validationErrors) {
        // TODO: Enhance error reporting with line/column numbers from AST spans
        console.error(`Validation Error: ${error.message}`); // Adjust based on actual error structure
        if (error.span) {
           // Example: console.error(`  at Line: ${error.span.start.row + 1}, Column: ${error.span.start.column + 1}`);
        }
      }
      console.error("-------------------------");
      return;
    }

    console.log("AST Validation Successful!");

    // 3. Process the AST (e.g., code generation, analysis)
    // ... your logic here ...
    // console.log("File ID:", ast.fileId);
    // console.log("Imports:", ast.imports);
    // ... etc ...

  } catch (error) {
    console.error("An error occurred:", error);
  }
}

await main();
```

## 貢献 (Contributing)

Bug reports, feature requests, and pull requests are welcome. Please check the current roadmap and known issues before contributing.

# State Machine DSL Syntax (Inspired by Cap'n Proto)

This document defines the syntax for the Single Source of Truth (`.ssot`) files used to define state machines, services, interfaces, protocols, types, and their associated configurations for code generation (including visualizations, data schemas, API specifications, and database schemas with RLS). The syntax borrows concepts from Cap'n Proto's schema language for clarity and structure, using `@id` for stable numerical IDs and `$` for metadata and tool directives (annotations).

**This DSL recommends organizing definitions within logical blocks (e.g., `types {}`, `services {}`, `machines {}`). While top-level definitions might be supported for backward compatibility by some tools, using blocks is the standard and preferred approach for clarity and organization.**

## 1. Overall Structure Example (Using Blocks)

```ssot
# Unique ID for this definition file (Cap'n Proto compatible)
@0xabcdef1234567890; # Must be globally unique across all .ssot files in the project.

# --- Imports ---
import "/path/to/shared_types.ssot"; # Import definitions from other files.
import "/path/to/base_service.ssot";
import "/path/to/common_actions.ssot";

# --- Output Configuration (Top Level or Per-Block/Element) ---
# $rust_out("src/generated");         # Removed - Example target language output
$capnp_out("schema/capnp");       # Optional: Cap'n Proto schemas
$ts_out("schema/ts");             # Optional: TypeScript types
$mermaid_out("docs/diagrams");   # Optional: Mermaid diagrams
$dot_out("docs/graphs");          # Optional: Graphviz DOT graphs
$zod_out("schema/zod");          # Optional: Zod schemas (TypeScript)
$jsonschema_out("schema/json"); # Optional: JSON Schema files
$proto_out("schema/proto");       # Optional: Protocol Buffer definitions
$avro_out("schema/avro");         # Optional: Avro schemas
$openapi_out("schema/openapi");   # Optional: OpenAPI specifications
$grpc_out("schema/grpc");          # Optional: gRPC service definitions (might use $proto_out)
$graphql_out("schema/graphql");   # Optional: GraphQL schemas
$asyncapi_out("schema/asyncapi"); # Optional: AsyncAPI specifications
$sql_out("schema/sql");            # Optional: SQL DDL files
$prisma_out("schema/prisma");     # Optional: Prisma schema file content
$drizzle_out("schema/drizzle");   # Optional: Drizzle TS schema files
// ... rest of the DSL syntax description remains the same ...
// ... (Sections 2 through 5 are largely unchanged as they describe the DSL itself) ...

### 5.3. Advanced Generator Configuration & Customization
- **Goal:** Provide more granular control over generated code and artifacts beyond simple output paths.
- **Proposal:**
    - Introduce generator-specific configuration blocks or annotations.
      # - `$rust_out("...") config { derive_serde: true; use_chrono: true; serde_case: "camelCase"; }` # Example removed
      - `struct UserProfile { ... email: string { $ts(decorator: "@IsEmail()", typeOverride: "EmailString"); } ... }`
      - `$openapi_out("...") config { default_security_scheme: "jwtAuth"; info: { title: "...", version: ... }; }`
    - Allow specifying code snippets or templates to be injected at certain points.
      # - `action customLogic @id(...) { $rust(inline: "/* custom rust code here */"); }` # Example removed
      - `action customLogic @id(...) { $ts(inline: "// custom TypeScript code here"); }` # Example added
    - This allows tailoring output for specific frameworks, libraries, or project conventions.

// ... rest of the future extensions ...

### 5.6. Lifecycle Hooks for Tooling Integration
- **Goal:** Allow integration with external scripts or tools during the SSOT processing lifecycle.
- **Proposal:**
    - Introduce a `$hook` annotation attachable to the top-level or specific blocks/elements.
    # - `$hook(event: "pre_codegen", target: "rust", script: "./scripts/validate_rust_config.sh")` # Example removed
    - `$hook(event: "pre_codegen", target: "typescript", script: "./scripts/validate_ts_config.sh")` # Example added
    - `$hook(event: "post_analysis", command: "node ./scripts/generate_docs.js --input $CONTEXT_FILE")`
    - Possible events: `post_parse`, `pre_validate`, `post_validate`, `pre_codegen`, `post_codegen`.
    - Tooling would execute the specified script/command at the designated lifecycle stage, potentially passing context information.

These extensions aim to make the SSOT DSL an even more comprehensive and powerful tool for model-driven development, covering aspects from detailed logic and data to testing, security, and deployment integration. Integrating these would require careful consideration of syntax clarity and tooling complexity.
