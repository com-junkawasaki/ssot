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
 * Tests for parsing type definitions (struct, enum) and building the corresponding AST.
 */
public class TypeAstTest {

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
    void testEnumDefinition() throws Exception {
        String input = """
        types {
            @id(300)
            enum Status {
                PENDING;
                @id(301) $description("Task succeeded")
                SUCCESS;
                FAILURE;
            }
        }
        """;
        SsotRoot root = parseAndBuildAst(input);
        assertEquals(1, root.getTypeDefinitions().size());
        assertTrue(root.getTypeDefinitions().get(0) instanceof EnumNode, "Definition should be EnumNode");

        EnumNode enumNode = (EnumNode) root.getTypeDefinitions().get(0);
        assertEquals("Status", enumNode.getName());
        assertEquals(Optional.of(300L), enumNode.getId());
        assertTrue(enumNode.getAnnotations().isEmpty()); // Annotations on enum itself
        assertEquals(3, enumNode.getVariants().size());

        EnumVariantNode pending = enumNode.getVariants().stream().filter(v -> v.getName().equals("PENDING")).findFirst().orElse(null);
        assertNotNull(pending);
        assertTrue(pending.getId().isEmpty());
        assertTrue(pending.getAnnotations().isEmpty());

        EnumVariantNode success = enumNode.getVariants().stream().filter(v -> v.getName().equals("SUCCESS")).findFirst().orElse(null);
        assertNotNull(success);
        assertEquals(Optional.of(301L), success.getId());
        assertEquals(1, success.getAnnotations().size());
        assertEquals("description", success.getAnnotations().get(0).getName());

        EnumVariantNode failure = enumNode.getVariants().stream().filter(v -> v.getName().equals("FAILURE")).findFirst().orElse(null);
        assertNotNull(failure);
        assertTrue(failure.getId().isEmpty());
        assertTrue(failure.getAnnotations().isEmpty());
    }

    // TODO: Add test for struct definition (already partially tested via fields/events?)
} 