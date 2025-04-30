package ssot_parser;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ParseTree;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List; // Needed for validation errors
// Remove unused List import if token printing is kept commented
// import java.util.List;

public class Main {
    public static void main(String[] args) throws Exception {
        String inputFile = "temp_comm.ssot"; // Test communication definitions

        java.io.File targetFile = new java.io.File(inputFile);
        if (!targetFile.exists()) {
            System.err.println("Error: Input file '" + inputFile + "' not found.");
            System.exit(1);
        }

        System.out.println("Processing file: " + inputFile);
        InputStream is = new FileInputStream(inputFile);
        CharStream input = CharStreams.fromStream(is);
        SSoTLexer lexer = new SSoTLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        SSoTParser parser = new SSoTParser(tokens);

        // TODO: Add proper error listeners to lexer and parser
        // parser.removeErrorListeners();
        // parser.addErrorListener(new YourCustomErrorListener());

        System.out.println("\nParsing input file...");
        ParseTree tree = parser.file();
        System.out.println("Parsing finished.");

        // Check for syntax errors reported by ANTLR
        if (parser.getNumberOfSyntaxErrors() > 0) {
            System.err.println("\nSyntax errors detected. Halting before AST construction.");
            // Consider printing errors from a custom error listener here
            System.exit(1);
        }

        // Build the AST
        System.out.println("\nBuilding AST...");
        AstBuilderVisitor visitor = new AstBuilderVisitor();
        Object result = visitor.visit(tree);
        AstNode ast = null;
        if (result instanceof AstNode) {
            ast = (AstNode) result;
        } else {
            System.err.println("Error: AST building did not return an AstNode.");
            System.exit(1);
        }
        System.out.println("AST Building finished.");

        // Validate the AST
        if (ast instanceof SsotRoot) {
            SsotRoot root = (SsotRoot) ast;
            System.out.println("\nValidating AST...");
            AstValidator validator = new AstValidator();
            List<AstValidator.ValidationError> validationErrors = validator.validate(root);
            System.out.println("Validation finished.");

            if (validationErrors.isEmpty()) {
                System.out.println("\nAST validation successful!");
                // Print the generated AST (optional)
                // System.out.println("\n--- Generated AST ---");
                // System.out.println(root);
                // System.out.println("--- End AST ---");

                // Proceed to next steps (e.g., code generation)

            } else {
                System.err.println("\n--- AST Validation Failed ---");
                for (AstValidator.ValidationError error : validationErrors) {
                    System.err.println("- " + error.message());
                }
                System.err.println("---------------------------");
                System.exit(1); // Exit if validation fails
            }

        } else {
             System.err.println("Error: AST root node is not of expected type SsotRoot or is null.");
             System.exit(1);
        }

        System.out.println("\nProcessing completed successfully.");

        // --- Existing Token Printing Logic (Commented Out) ---
        /*
        lexer.reset();
        List<? extends Token> allTokens = lexer.getAllTokens();
        System.out.println("\\n--- Tokens for " + inputFile + " ---");
        for (Token token : allTokens) {
            String symbolicName = SSoTParser.VOCABULARY.getSymbolicName(token.getType());
            System.out.println(
                "[@" + token.getTokenIndex() + "," +
                token.getStartIndex() + ":" + token.getStopIndex() + "='" +
                token.getText().replace("\\n", "\\\\n") +
                "'" +
                ",<" + (symbolicName != null ? symbolicName : String.valueOf(token.getType())) + ">," +
                token.getLine() + ":" + token.getCharPositionInLine() + "]"
            );
        }
        System.out.println("--- End Tokens ---");
        */
    }

    // underlineError method is not needed for this test
} 