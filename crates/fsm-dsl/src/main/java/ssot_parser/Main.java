package ssot_parser;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;
import java.io.FileInputStream;
import java.io.InputStream;

public class Main {
    public static void main(String[] args) throws Exception {
        // Always read from temp_types.ssot for this test
        String inputFile = "temp_types.ssot"; // Fixed input file

        // Check if temp_types.ssot exists (basic check)
        java.io.File tempFile = new java.io.File(inputFile);
        if (!tempFile.exists()) {
            System.err.println("Error: " + inputFile + " not found in the current directory.");
            System.err.println("Please create temp_types.ssot with the types { ... } block content.");
            System.exit(1);
        }


        InputStream is = new FileInputStream(inputFile);
        CharStream input = CharStreams.fromStream(is);
        SSoTLexer lexer = new SSoTLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        SSoTParser parser = new SSoTParser(tokens);

        // Add a simple error listener to report syntax errors
        parser.removeErrorListeners(); // Remove default ConsoleErrorListener
        parser.addErrorListener(new BaseErrorListener() {
            @Override
            public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e) {
                // Print more context for the error
                underlineError(recognizer, (Token)offendingSymbol, line, charPositionInLine);
                System.err.println("ERROR near line " + line + ":" + charPositionInLine + " - " + msg);
            }
        });

        try {
            System.out.println("Attempting to parse typesBlock from " + inputFile + "...");
            // Start parsing at the 'typesBlock' rule
            ParseTree tree = parser.typesBlock();
            System.out.println("Parsing successful for typesBlock!");
            // Optionally print the parse tree (LISP-style)
            System.out.println(tree.toStringTree(parser));
        } catch (Exception e) {
            System.err.println("Parsing failed: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    // Helper method to underline the error location (basic implementation)
    protected static void underlineError(Recognizer recognizer,
                                         Token offendingToken, int line,
                                         int charPositionInLine) {
        CommonTokenStream tokens =
            (CommonTokenStream)recognizer.getInputStream();
        String input = tokens.getTokenSource().getInputStream().toString();
        String[] lines = input.split("\\n");
        if (line > 0 && line <= lines.length) {
            String errorLine = lines[line - 1];
            System.err.println(errorLine);
            for (int i=0; i<charPositionInLine; i++) System.err.print(" ");
            int start = offendingToken.getStartIndex();
            int stop = offendingToken.getStopIndex();
            if ( start>=0 && stop>=0 ) {
                for (int i=start; i<=stop; i++) System.err.print("^");
            }
            System.err.println();
        }
    }
} 