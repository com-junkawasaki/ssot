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
3.  **Define TypeScript AST (`src/ast.ts`) (High Priority - Not Started):** Create TypeScript interfaces/classes representing the structure of the Abstract Syntax Tree corresponding to the DSL elements defined in the grammar. Placeholder exists.
4.  **Implement CST-to-AST Transformation (`src/parser.ts`) (High Priority - Not Started):** Write TypeScript code to traverse the Tree-sitter CST and construct the TypeScript AST, including accurate source map (span) information. Placeholder exists.
5.  **Implement Validation Logic (`src/validation.ts`) (High Priority - Not Started):** Implement validation rules using the TypeScript AST. Checks should include duplicate IDs/names, undefined references, type checking (where applicable), structural consistency based on the DSL rules. Placeholder exists.
6.  **Implement Testing (Medium Priority):**
    *   **Grammar Tests:** Use `tree-sitter test`. (Initial tests passing, needs more cases)
    *   **Parser/AST Tests:** Write Deno tests to verify the CST-to-AST transformation. (Not Started)
    *   **Validation Tests:** Write Deno tests to verify the validation logic. (Not Started)
7.  **Error Reporting Improvements (Medium Priority):** Ensure parser and validator errors provide clear messages and precise location information using AST span information.
8.  **Documentation (`DSL.md`) Update (Low Priority):** Ensure `DSL.md` fully reflects the latest `grammar.js` details (e.g., annotation placement on transitions, TODO items).
9.  **(Optional) Code Generation (Low Priority):** Explore adding features to generate code or other artifacts from the validated AST.

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

## State Machine DSL Syntax

For the detailed syntax specification of the `.ssot` language, please refer to the [DSL.md](./DSL.md) file.
