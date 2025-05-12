package ssot_parser;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ParseTree;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List; // Needed for validation errors
// Remove unused List import if token printing is kept commented
// import java.util.List;
import ssot_parser.ast.AstNode;
import ssot_parser.ast.SsotRoot;

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

            // validation パッケージの AstValidator のみを使用
            ssot_parser.validation.AstValidator validator2 = new ssot_parser.validation.AstValidator(root);
            List<ssot_parser.validation.ValidationError> errors2 = validator2.validate(); // validate() を呼び出し、戻り値でエラーリストを取得
            System.out.println("Validation finished.");

            if (errors2.isEmpty()) {
                 System.out.println("\nAST validation successful!");
            } else {
                 System.err.println("\n--- AST Validation Failed ---");
                 for (ssot_parser.validation.ValidationError error : errors2) {
                     System.err.println("- " + error.getMessage());
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

        // validation パッケージの Validator の呼び出し箇所は上で統合したので削除
        // ssot_parser.validation.AstValidator validator2 = new ssot_parser.validation.AstValidator((ssot_parser.ast.SsotRoot) ast);
        // validator2.validate();
        // List<ssot_parser.validation.ValidationError> errors2 = validator2.getErrors();
        // if (!errors2.isEmpty()) { ... }
    }

    // underlineError method is not needed for this test
} 