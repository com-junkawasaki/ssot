package ssot_parser;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ParseTree;
import java.io.FileInputStream;
import java.io.InputStream;
// Remove unused List import if token printing is kept commented
// import java.util.List;

public class Main {
    public static void main(String[] args) throws Exception {
        // Use temp_comm.ssot for this lexer test
        String inputFile = "temp_comm.ssot";

        java.io.File targetFile = new java.io.File(inputFile);
        if (!targetFile.exists()) {
            System.err.println("Error: Input file '" + inputFile + "' not found.");
            System.exit(1);
        }

        InputStream is = new FileInputStream(inputFile);
        CharStream input = CharStreams.fromStream(is);
        SSoTLexer lexer = new SSoTLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        SSoTParser parser = new SSoTParser(tokens);

        System.out.println("Parsing input file...");
        ParseTree tree = parser.ssotFile();
        System.out.println("Parsing finished.");

        // Comment out the direct tree printing
        // System.out.println("\\n--- Parse Tree ---");
        // System.out.println(tree.toStringTree(parser));
        // System.out.println("--- End Parse Tree ---");

        // Create and use the visitor to build the AST
        System.out.println("\\n--- Building AST ---");
        AstBuilderVisitor visitor = new AstBuilderVisitor();
        AstNode astRootNode = visitor.visit(tree); // Visit and get the root AST node
        System.out.println("--- AST Building Finished ---");

        // Print the generated AST (using default record toString for now)
        if (astRootNode instanceof SsotRoot) {
            SsotRoot root = (SsotRoot) astRootNode;
            System.out.println("\\n--- Generated AST ---");
            System.out.println(root);
            System.out.println("--- End AST ---");
        } else {
             System.err.println("Error: AST root node is not of expected type SsotRoot.");
        }

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