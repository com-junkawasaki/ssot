/**
 * Represents a validation error.
 */
export class ValidationError extends Error {
  /**
   * @param {string} message The error message.
   * @param {import("./ast.js").Span} [span] Optional source code location of the error.
   */
  constructor(message, span) {
    super(message);
    this.name = "ValidationError";
    this.span = span; // Store span for better error reporting
  }
}

/**
 * Validates the given SSOT Abstract Syntax Tree (AST).
 *
 * @param {import("./ast.js").SsotAst} ast The AST to validate.
 * @returns {Promise<ValidationError[]>} A promise that resolves with an array of validation errors. Empty if valid.
 */
export async function validateAst(ast) {
  const errors = [];

  console.log("Running AST validation... (Placeholder)");

  // --- Example Validation Checks ---

  // 1. Check for duplicate top-level block types (e.g., multiple `types {}`)
  const blockKinds = new Set();
  for (const block of ast.blocks) {
    if (blockKinds.has(block.kind)) {
      errors.push(new ValidationError(`Duplicate top-level block found: ${block.kind}`, block.span));
    }
    blockKinds.add(block.kind);
  }

  // 2. Validate IDs: Check for uniqueness across the entire AST (if IDs are present)
  //    Requires traversing the AST to collect all @id annotations.
  //    (Implementation omitted for brevity in this basic structure)
  //    const allIds = collectAllIds(ast); // Hypothetical function
  //    // ... check for duplicates in allIds ...

  // 3. Validate Type References: Ensure all custom types used are defined.
  //    Requires collecting all defined types and then checking references.
  //    (Implementation omitted for brevity)

  // 4. Validate State Machine Structure (if MachinesBlock exists)
  //    - Check if initialState is defined.
  //    - Check if all states referenced in transitions are defined.
  //    - etc.
  //    (Implementation omitted for brevity)


  // Add more validation rules as needed based on the DSL's semantics.


  // --- End Example Checks ---

  // Simulate async operation if needed (e.g., fetching external schemas)
  // await new Promise(resolve => setTimeout(resolve, 10));

  return errors;
}

// Helper function example (to be implemented if needed)
// function collectAllIds(node) {
//   const ids = new Map(); // Map<number, AstNode> to store ID and its node/span
//   // ... recursive traversal logic ...
//   return ids;
// }
