package ssot_parser;

import org.antlr.v4.runtime.*;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;

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

        // Get all tokens from the lexer
        List<? extends Token> tokens = lexer.getAllTokens();

        // Print the tokens
        System.out.println("--- Tokens for " + inputFile + " ---");
        for (Token token : tokens) {
            // Get symbolic name from Vocabulary
            String symbolicName = SSoTParser.VOCABULARY.getSymbolicName(token.getType());
            System.out.println(
                "[@" + token.getTokenIndex() + "," +
                token.getStartIndex() + ":" + token.getStopIndex() + "='" +
                token.getText().replace("\n", "\\n") + // Escape newlines
                "'" + // Close the text part
                ",<" + (symbolicName != null ? symbolicName : String.valueOf(token.getType())) + ">," + // Use String.valueOf for type fallback
                token.getLine() + ":" + token.getCharPositionInLine() + "]"
            );
        }
         System.out.println("--- End Tokens ---");
    }

    // underlineError method is not needed for this test
} 