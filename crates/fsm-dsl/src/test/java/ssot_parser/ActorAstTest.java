package ssot_parser;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ParseTree;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import ssot_parser.AstBuilderVisitor;
import ssot_parser.ast.AstNode;
import ssot_parser.ast.nodes.*;
import ssot_parser.ast.type.*;
import ssot_parser.ast.SsotRoot;

/**
 * Tests for parsing actor definitions and building the corresponding AST.
 */
public class ActorAstTest {

    // Helper method (Copied from StateMachineAstTest)
    private SsotRoot parseAndBuildAst(String inputString) throws Exception {
        InputStream is = new ByteArrayInputStream(inputString.getBytes(StandardCharsets.UTF_8));
        CharStream input = CharStreams.fromStream(is);
        SSoTLexer lexer = new SSoTLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        SSoTParser parser = new SSoTParser(tokens);

        parser.removeErrorListeners();
        parser.addErrorListener(new BaseErrorListener() {
            @Override
            public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol,
                                    int line, int charPositionInLine, String msg, RecognitionException e) {
                throw new RuntimeException("Syntax error at line " + line + ":" + charPositionInLine + " " + msg, e);
            }
        });

        ParseTree tree = parser.file();
        AstBuilderVisitor visitor = new AstBuilderVisitor();
        Object result = visitor.visit(tree);
        assertTrue(result instanceof SsotRoot, "AST building should return an SsotRoot");
        return (SsotRoot) result;
    }

    @Test
    void testSimpleActor() throws Exception {
        String input = """
        actors {
            @id(400) actor User { $description("End user") }
            actor System { }
        }
        """;
        SsotRoot root = parseAndBuildAst(input);
        assertEquals(2, root.getActorDefinitions().size());

        assertTrue(root.getActorDefinitions().get(0) instanceof ActorNode);
        ActorNode user = (ActorNode) root.getActorDefinitions().get(0);
        assertEquals("User", user.getName());
        assertEquals(Optional.of(400L), user.getId());
        assertEquals(1, user.getAnnotations().size());
        assertTrue(user.getAnnotations().containsKey("description"));
        assertEquals("End user", user.getAnnotations().get("description"));

        assertTrue(root.getActorDefinitions().get(1) instanceof ActorNode);
        ActorNode system = (ActorNode) root.getActorDefinitions().get(1);
        assertEquals("System", system.getName());
        assertTrue(system.getId().isEmpty());
        assertTrue(system.getAnnotations().isEmpty());
    }
} 