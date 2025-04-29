package ssot_parser;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;
import java.io.FileInputStream;
import java.io.InputStream;

public class Main {
    public static void main(String[] args) throws Exception {
        String inputFile = null;
        if (args.length > 0) {
            inputFile = args[0];
        } else {
            System.err.println("Usage: java ssot_parser.Main <input_file.ssot>");
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
                System.err.println("line " + line + ":" + charPositionInLine + " " + msg);
            }
        });

        try {
            ParseTree tree = parser.file(); // Start parsing at the 'file' rule
            System.out.println("Parsing successful!");
            // Optionally print the parse tree (LISP-style)
            // System.out.println(tree.toStringTree(parser));
        } catch (Exception e) {
            System.err.println("Parsing failed: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
} 