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
        assertTrue(root.getTypeDefinitions().get(0) instanceof TypeDefNode);
        TypeDefNode typeDef = (TypeDefNode) root.getTypeDefinitions().get(0);
        assertEquals(TypeDefNode.TypeKind.ENUM, typeDef.getKind());

        assertEquals("Status", typeDef.getName());
        assertEquals(Optional.of(300L), typeDef.getId());
        assertTrue(typeDef.getAnnotations().isEmpty()); // Annotations on type itself
        assertEquals(3, typeDef.getVariants().size());

        EnumVariantNode pending = typeDef.getVariants().stream().filter(v -> v.getName().equals("PENDING")).findFirst().orElse(null);
        assertNotNull(pending);
        assertTrue(pending.getId().isEmpty());
        assertTrue(pending.getAnnotations().isEmpty());

        EnumVariantNode success = typeDef.getVariants().stream().filter(v -> v.getName().equals("SUCCESS")).findFirst().orElse(null);
        assertNotNull(success);
        assertEquals(Optional.of(301L), success.getId());
        assertEquals(1, success.getAnnotations().size());
        assertTrue(success.getAnnotations().containsKey("description"));
        assertEquals("Task succeeded", success.getAnnotations().get("description"));

        EnumVariantNode failure = typeDef.getVariants().stream().filter(v -> v.getName().equals("FAILURE")).findFirst().orElse(null);
        assertNotNull(failure);
        assertTrue(failure.getId().isEmpty());
        assertTrue(failure.getAnnotations().isEmpty());
    }

    @Test
    void testStructDefinition() throws Exception {
        String input = """
        types {
            @id(300)
            enum Status {
                PENDING;
                @id(301) $description("Task succeeded")
                SUCCESS;
                FAILURE;
            }

            @id(302)
            struct MyStruct {
                // Add some fields or events here
            }

            @id(303)
            struct User {
                // Add some fields or events here
            }
        }
        """;
        SsotRoot root = parseAndBuildAst(input);
        assertEquals(3, root.getTypeDefinitions().size());
        assertTrue(root.getTypeDefinitions().get(0) instanceof TypeDefNode);
        assertEquals(TypeDefNode.TypeKind.ENUM, ((TypeDefNode)root.getTypeDefinitions().get(0)).getKind());
        assertTrue(root.getTypeDefinitions().get(1) instanceof TypeDefNode);
        assertEquals(TypeDefNode.TypeKind.STRUCT, ((TypeDefNode)root.getTypeDefinitions().get(1)).getKind());
        assertTrue(root.getTypeDefinitions().get(2) instanceof TypeDefNode);
        assertEquals(TypeDefNode.TypeKind.STRUCT, ((TypeDefNode)root.getTypeDefinitions().get(2)).getKind());

        TypeDefNode enumDef = (TypeDefNode) root.getTypeDefinitions().get(0);
        assertEquals("Status", enumDef.getName());
        assertEquals(Optional.of(300L), enumDef.getId());
        assertTrue(enumDef.getAnnotations().isEmpty());
        assertEquals(3, enumDef.getVariants().size());

        EnumVariantNode pending = enumDef.getVariants().stream().filter(v -> v.getName().equals("PENDING")).findFirst().orElse(null);
        assertNotNull(pending);
        assertTrue(pending.getId().isEmpty());
        assertTrue(pending.getAnnotations().isEmpty());

        EnumVariantNode success = enumDef.getVariants().stream().filter(v -> v.getName().equals("SUCCESS")).findFirst().orElse(null);
        assertNotNull(success);
        assertEquals(Optional.of(301L), success.getId());
        assertEquals(1, success.getAnnotations().size());
        assertTrue(success.getAnnotations().containsKey("description"));
        assertEquals("Task succeeded", success.getAnnotations().get("description"));

        EnumVariantNode failure = enumDef.getVariants().stream().filter(v -> v.getName().equals("FAILURE")).findFirst().orElse(null);
        assertNotNull(failure);
        assertTrue(failure.getId().isEmpty());
        assertTrue(failure.getAnnotations().isEmpty());

        TypeDefNode structDef1 = (TypeDefNode) root.getTypeDefinitions().get(1);
        assertEquals("MyStruct", structDef1.getName());
        assertEquals(TypeDefNode.TypeKind.STRUCT, structDef1.getKind());
        assertTrue(structDef1.getFields().isEmpty());

        TypeDefNode structDef2 = (TypeDefNode) root.getTypeDefinitions().get(2);
        assertEquals("User", structDef2.getName());
        assertEquals(TypeDefNode.TypeKind.STRUCT, structDef2.getKind());
        assertTrue(structDef2.getFields().isEmpty());
    }
} 