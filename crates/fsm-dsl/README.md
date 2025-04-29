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

The project involves the following major steps:

1.  **Define Tree-sitter Grammar (`grammar.js`) (Mostly Done):**
    *   Core structure, types, machine elements, states, transitions, annotations, literals, dependencies defined. (Done)
    *   Grammar conflicts resolved. (Done)
    *   **TODO:** Refine action/guard/invoke bodies, service/communication/deployment details, complex expressions.
2.  **Generate Tree-sitter Parser (Done):** Parser (`src/parser.c`, `src/tree_sitter/parser.h`) generated successfully using Tree-sitter CLI.
3.  **Define TypeScript AST (`src/ast.js`) (High Priority - In Progress):** Create TypeScript interfaces/classes representing the structure of the Abstract Syntax Tree corresponding to the DSL elements defined in the grammar. Core structure defined, but detailed elements (machine internals, etc.) need completion.
4.  **Implement CST-to-AST Transformation (`src/parser.js`) (High Priority - In Progress):** Write TypeScript code to traverse the Tree-sitter CST and construct the TypeScript AST, including accurate source map (span) information. Basic node transformation implemented, but detailed DSL element transformation is pending.
5.  **Implement Validation Logic (`src/validation.js`) (High Priority - Not Started):** Implement validation rules using the TypeScript AST. Checks should include duplicate IDs/names, undefined references, type checking (where applicable), structural consistency based on the DSL rules. Placeholder exists.
6.  **Implement Testing (Medium Priority):**
    *   **Grammar Tests:** Use `tree-sitter test`. (Initial tests passing, needs more cases)
    *   **Parser/AST Tests:** Write Deno/Node tests to verify the CST-to-AST transformation. (Not Started)
    *   **Validation Tests:** Write Deno/Node tests to verify the validation logic. (Not Started)
7.  **Error Reporting Improvements (Medium Priority):** Ensure parser and validator errors provide clear messages and precise location information using AST span information. (Not Started)
8.  **Documentation (`DSL.md`) Update (Low Priority):** Ensure `DSL.md` fully reflects the latest `grammar.js` details (e.g., annotation placement on transitions, TODO items).
9.  **(Optional) Code Generation (Low Priority):** Explore adding features to generate code or other artifacts from the validated AST.

## 使用例 (Conceptual Usage Example - Deno/Node.js)

```typescript
// Assuming Node.js environment with ESM support
// If using Deno, adjust imports and file system access accordingly
import { initializeParser, parseSsotContent } from "./src/parser.js";
import { validateAst } from "./src/validation.js";
import { SsotAst } from "./src/ast.js"; // Assuming AST definition is exported correctly
import * as fs from 'node:fs/promises'; // Node.js file system

async function main() {
  // Example: Get path from command line arguments (Node.js style)
  const args = process.argv.slice(2);
  const ssotFilePath = args[0] ?? "example.ssot";

  if (!ssotFilePath) {
    console.error("Usage: node main.js <path_to_ssot_file>");
    process.exit(1);
  }

  console.log(`Processing file: ${ssotFilePath}`);

  try {
    // Ensure parser is initialized (crucial step)
    await initializeParser();

    const ssotContent = await fs.readFile(ssotFilePath, 'utf-8');

    // 1. Parse the content using Tree-sitter and build the AST
    const ast: SsotAst = parseSsotContent(ssotContent, ssotFilePath);
    console.log("Successfully parsed SSOT content!");

    // 2. Validate the AST
    const validationErrors = await validateAst(ast); // Note: validateAst is currently a placeholder

    if (validationErrors.length > 0) {
      console.error("--- Validation Failed ---");
      for (const error of validationErrors) {
        console.error(`Validation Error: ${error.message}`);
        if (error.span) {
           console.error(`  at Line: ${error.span.start.row + 1}, Column: ${error.span.start.column + 1}`);
        } else {
           // Handle errors without span if necessary
        }
      }
      console.error("-------------------------");
      return;
    }

    console.log("AST Validation Successful! (Currently Placeholder)");

    // 3. Process the AST (e.g., code generation, analysis)
    console.log("Processing AST...");
    console.log("File ID:", ast.fileId?.value); // Example access
    console.log("Imports:", ast.imports.map(imp => imp.path.value));
    console.log("Blocks:", ast.blocks.map(b => b.kind));
    // ... your logic here ...

  } catch (error) {
     if (error instanceof Error && error.code === 'ENOENT') { // Node.js file not found check
       console.error(`Error: File not found at ${ssotFilePath}`);
     } else {
       console.error(`An error occurred processing ${ssotFilePath}:`, error);
     }
     process.exit(1); // Ensure process exits on error
  }
}

await main();
```

## 貢献 (Contributing)

Bug reports, feature requests, and pull requests are welcome. Please check the current roadmap and known issues before contributing.

## State Machine DSL Syntax

For the detailed syntax specification of the `.ssot` language, please refer to the [DSL.md](./DSL.md) file.
