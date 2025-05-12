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
            struct Point {
                x: i32;
                @id(304) $meta("coordinate")
                y: i32;
            }

            struct User {
                userId: string;
                isActive: bool { $default(true) };
                profile: Point;
            }
        }
        """;
        SsotRoot root = parseAndBuildAst(input);
        assertEquals(2, root.getTypeDefinitions().size());

        // --- Validate Point Struct ---
        assertTrue(root.getTypeDefinitions().get(0) instanceof TypeDefNode);
        TypeDefNode pointDef = (TypeDefNode) root.getTypeDefinitions().get(0);
        assertEquals(TypeDefNode.TypeKind.STRUCT, pointDef.getKind());
        assertEquals("Point", pointDef.getName());
        assertTrue(pointDef.getAnnotations().isEmpty());
        assertEquals(2, pointDef.getFields().size());

        FieldNode xField = pointDef.getFields().get(0);
        assertEquals("x", xField.getName());
        assertTrue(xField.getType() instanceof PrimitiveTypeNode);
        assertEquals("i32", ((PrimitiveTypeNode)xField.getType()).getTypeName());
        assertTrue(xField.getId().isEmpty());
        assertTrue(xField.getAnnotations().isEmpty());

        FieldNode yField = pointDef.getFields().get(1);
        assertEquals("y", yField.getName());
        assertTrue(yField.getType() instanceof PrimitiveTypeNode);
        assertEquals("i32", ((PrimitiveTypeNode)yField.getType()).getTypeName());
        assertEquals(Optional.of(304L), yField.getId());
        assertEquals(1, yField.getAnnotations().size());
        assertTrue(yField.getAnnotations().containsKey("meta"));
        assertEquals("coordinate", yField.getAnnotations().get("meta"));

        // --- Validate User Struct ---
        assertTrue(root.getTypeDefinitions().get(1) instanceof TypeDefNode);
        TypeDefNode userDef = (TypeDefNode) root.getTypeDefinitions().get(1);
        assertEquals(TypeDefNode.TypeKind.STRUCT, userDef.getKind());
        assertEquals("User", userDef.getName());
        assertTrue(userDef.getAnnotations().isEmpty());

        assertEquals(3, userDef.getFields().size());

        FieldNode userIdField = userDef.getFields().get(0);
        assertEquals("userId", userIdField.getName());
        assertTrue(userIdField.getType() instanceof PrimitiveTypeNode);
        assertEquals("string", ((PrimitiveTypeNode)userIdField.getType()).getTypeName());
        assertTrue(userIdField.getAnnotations().isEmpty());

        FieldNode activeField = userDef.getFields().get(1);
        assertEquals("isActive", activeField.getName());
        assertTrue(activeField.getType() instanceof PrimitiveTypeNode);
        assertEquals("bool", ((PrimitiveTypeNode)activeField.getType()).getTypeName());
        assertEquals(1, activeField.getAnnotations().size());
        assertTrue(activeField.getAnnotations().containsKey("default"));
        assertEquals(true, activeField.getAnnotations().get("default"));

        FieldNode profileField = userDef.getFields().get(2);
        assertEquals("profile", profileField.getName());
        assertTrue(profileField.getType() instanceof ReferenceTypeNode);
        assertEquals("Point", ((ReferenceTypeNode)profileField.getType()).getReferencedTypeName());
        assertTrue(profileField.getAnnotations().isEmpty());
    }
} 