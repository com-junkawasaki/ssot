import { parseSsotContent } from "./src/parser.js"; // Assuming parser entry point
import { validateAst } from "./src/validation.js"; // Assuming validator entry point
import { SsotAst } from "./src/ast.js"; // Assuming AST definition

async function main() {
  const ssotFilePath = Deno.args[0] ?? "example.ssot"; // Get path from args or use default

  if (!ssotFilePath) {
    console.error("Usage: deno run main.ts <path_to_ssot_file>");
    Deno.exit(1);
  }

  console.log(`Processing file: ${ssotFilePath}`);

  try {
    const ssotContent = await Deno.readTextFile(ssotFilePath);

    // 1. Parse the content using Tree-sitter and build the AST
    // TODO: Implement parseSsotContent
    // const ast: SsotAst = await parseSsotContent(ssotContent, ssotFilePath);
    console.log("Successfully parsed SSOT content! (Placeholder)");
    const ast: SsotAst = {} as SsotAst; // Placeholder AST

    // 2. Validate the AST
    // TODO: Implement validateAst
    // const validationErrors = await validateAst(ast);
    const validationErrors: any[] = []; // Placeholder errors

    if (validationErrors.length > 0) {
      console.error("--- Validation Failed ---");
      for (const error of validationErrors) {
        // TODO: Enhance error reporting with line/column numbers from AST spans
        console.error(`Validation Error: ${error.message}`); // Adjust based on actual error structure
        // if (error.span) {
        //    console.error(`  at Line: ${error.span.start.row + 1}, Column: ${error.span.start.column + 1}`);
        // }
      }
      console.error("-------------------------");
      return;
    }

    console.log("AST Validation Successful! (Placeholder)");

    // 3. Process the AST (e.g., code generation, analysis)
    console.log("Processing AST... (Placeholder)");
    // console.log("File ID:", ast.fileId);
    // console.log("Imports:", ast.imports);
    // ... etc ...

  } catch (error) {
    if (error instanceof Deno.errors.NotFound) {
      console.error(`Error: File not found at ${ssotFilePath}`);
    } else {
       console.error(`An error occurred processing ${ssotFilePath}:`, error);
    }
    Deno.exit(1);
  }
}

await main(); 