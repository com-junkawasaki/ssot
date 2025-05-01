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

import ssot_parser.ast.AstNode;
import ssot_parser.ast.nodes.*;
import ssot_parser.ast.type.*;

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
            @id(400)
            User { $description("End user"); }

            System { }
        }
        """;
        SsotRoot root = parseAndBuildAst(input);
        assertEquals(2, root.getActorDefinitions().size());

        ActorNode user = root.getActorDefinitions().stream()
                           .filter(a -> a instanceof ActorNode && ((ActorNode)a).getName().equals("User"))
                           .map(a -> (ActorNode)a).findFirst().orElse(null);
        assertNotNull(user);
        assertEquals("User", user.getName());
        assertEquals(Optional.of(400L), user.getId());
        assertEquals(1, user.getAnnotations().size());
        assertEquals("description", user.getAnnotations().get(0).getName());

        ActorNode system = root.getActorDefinitions().stream()
                             .filter(a -> a instanceof ActorNode && ((ActorNode)a).getName().equals("System"))
                             .map(a -> (ActorNode)a).findFirst().orElse(null);
        assertNotNull(system);
        assertEquals("System", system.getName());
        assertTrue(system.getId().isEmpty());
        assertTrue(system.getAnnotations().isEmpty());
    }
} 