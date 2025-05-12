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
 * Tests for parsing service and interface definitions and building the corresponding AST.
 */
public class ServiceAstTest {

    // Helper method to parse input string and build AST (Copied from StateMachineAstTest)
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
    void testMinimalInterface() throws Exception {
        String input = """
        services {
            interface MyInterface {
                 doSomething();
            }
        }
        """;
        SsotRoot root = parseAndBuildAst(input);
        assertEquals(1, root.getServiceDefinitions().size());
        assertTrue(root.getServiceDefinitions().get(0) instanceof InterfaceNode);
        InterfaceNode iface = (InterfaceNode) root.getServiceDefinitions().get(0);

        assertEquals("MyInterface", iface.getName());
        assertEquals(1, iface.getMethods().size());
        MethodNode method = iface.getMethods().get(0);
        assertEquals("doSomething", method.getName());
        assertTrue(method.getParameters().isEmpty());
        assertTrue(method.getReturnType().isEmpty(), "Return type should be empty (void)");
    }

    @Test
    void testInterfaceWithDetails() throws Exception {
        String input = """
        services {
            interface Greeter {
                @id(101) $description("Says hello")
                greet(name: string) -> string;

                // Method with no return type
                log(message: string);
            }
        }
        """;
        SsotRoot root = parseAndBuildAst(input);
        assertEquals(1, root.getServiceDefinitions().size());
        InterfaceNode iface = (InterfaceNode) root.getServiceDefinitions().get(0);

        assertEquals("Greeter", iface.getName());
        assertEquals(2, iface.getMethods().size());

        MethodNode greetMethod = iface.getMethods().stream().filter(m -> m.getName().equals("greet")).findFirst().orElse(null);
        assertNotNull(greetMethod);
        assertEquals(Optional.of(101L), greetMethod.getId());
        assertEquals(1, greetMethod.getAnnotations().size());
        assertTrue(greetMethod.getAnnotations().containsKey("description"), "Should contain $description annotation");
        assertEquals("Says hello", greetMethod.getAnnotations().get("description"));
        assertEquals(1, greetMethod.getParameters().size());
        assertEquals("name", greetMethod.getParameters().get(0).getName());
        assertTrue(greetMethod.getParameters().get(0).getType() instanceof PrimitiveTypeNode, "Parameter type should be Primitive");
        assertEquals("string", ((PrimitiveTypeNode)greetMethod.getParameters().get(0).getType()).getTypeName());

        assertTrue(greetMethod.getReturnType().isPresent(), "Greet should have a return type");
        assertTrue(greetMethod.getReturnType().get() instanceof PrimitiveTypeNode, "Return type should be Primitive");
        assertEquals("string", ((PrimitiveTypeNode)greetMethod.getReturnType().get()).getTypeName());

        MethodNode logMethod = iface.getMethods().stream().filter(m -> m.getName().equals("log")).findFirst().orElse(null);
        assertNotNull(logMethod);
        assertEquals(1, logMethod.getParameters().size());
        assertEquals("message", logMethod.getParameters().get(0).getName());
        assertTrue(logMethod.getParameters().get(0).getType() instanceof PrimitiveTypeNode, "Log Parameter type should be Primitive");
        assertEquals("string", ((PrimitiveTypeNode)logMethod.getParameters().get(0).getType()).getTypeName());
        assertTrue(logMethod.getReturnType().isEmpty(), "Log return type should be empty");
    }

     @Test
     void testServiceImplementation() throws Exception {
         String input = """
         services {
             interface MyInterface { doSomething(); }
             service MyServiceImpl { $implements(MyInterface) }
         }
         """;
         SsotRoot root = parseAndBuildAst(input);
         assertEquals(2, root.getServiceDefinitions().size()); // Interface + Service

         ServiceNode service = root.getServiceDefinitions().stream()
                                .filter(s -> s instanceof ServiceNode)
                                .map(s -> (ServiceNode)s)
                                .findFirst().orElse(null);
         assertNotNull(service);
         assertEquals("MyServiceImpl", service.getName());

         // Implementation check via annotation
         assertEquals(1, service.getAnnotations().size());
         assertTrue(service.getAnnotations().containsKey("implements"));
         assertEquals("MyInterface", service.getAnnotations().get("implements"));

         assertEquals(1, service.getMethods().size()); // doSomething

         MethodNode method = service.getMethods().stream().filter(m -> m.getName().equals("doSomething")).findFirst().orElse(null);
         assertNotNull(method);
         assertTrue(method.getParameters().isEmpty());
     }

    @Test
    void testComplexTypes() throws Exception {
        String input = """
        types {
            struct Point { x: i32; y: i32; }
            enum Style { SOLID; DASHED; }
        }
        services {
            interface Renderer {
                renderPoint(p: Point);
                renderShape(s: Shape);
                getPoints() -> list<optional<Point>>;
                getStyles() -> map<string, Style>;
            }
        }
        """;
        SsotRoot root = parseAndBuildAst(input);
        assertEquals(1, root.getServiceDefinitions().size()); // Only interface
        assertTrue(root.getServiceDefinitions().get(0) instanceof InterfaceNode);
        InterfaceNode iface = (InterfaceNode) root.getServiceDefinitions().get(0);
        assertEquals("Renderer", iface.getName());
        assertEquals(4, iface.getMethods().size());

        // Test renderPoint(p: Point)
        MethodNode pointMethod = iface.getMethods().stream().filter(m -> m.getName().equals("renderPoint")).findFirst().orElse(null);
        assertNotNull(pointMethod);
        assertEquals(1, pointMethod.getParameters().size());
        ParameterNode pointParam = pointMethod.getParameters().get(0);
        assertTrue(pointParam.getType() instanceof ReferenceTypeNode, "Parameter type should be Reference");
        ReferenceTypeNode pointType = (ReferenceTypeNode) pointParam.getType();
        assertEquals("Point", pointType.getReferencedTypeName());

        // Test renderShape(s: Shape)
        MethodNode shapeMethod = iface.getMethods().stream().filter(m -> m.getName().equals("renderShape")).findFirst().orElse(null);
        assertNotNull(shapeMethod);
        assertEquals(1, shapeMethod.getParameters().size());
        ParameterNode shapeParam = shapeMethod.getParameters().get(0);
        assertTrue(shapeParam.getType() instanceof ReferenceTypeNode, "Parameter type should be Reference");
        ReferenceTypeNode shapeType = (ReferenceTypeNode) shapeParam.getType();
        assertEquals("Shape", shapeType.getReferencedTypeName());

        // Test getPoints() -> list<optional<Point>>
        MethodNode pointsMethod = iface.getMethods().stream().filter(m -> m.getName().equals("getPoints")).findFirst().orElse(null);
        assertNotNull(pointsMethod);
        assertEquals(0, pointsMethod.getParameters().size());
        assertTrue(pointsMethod.getReturnType().isPresent(), "Method should have a return type");
        assertTrue(pointsMethod.getReturnType().get() instanceof ListTypeNode, "Return type should be List");
        ListTypeNode pointsReturnType = (ListTypeNode) pointsMethod.getReturnType().get();
        assertTrue(pointsReturnType.getElementType() instanceof OptionalTypeNode, "List element type should be Optional");
        OptionalTypeNode pointsElementType = (OptionalTypeNode) pointsReturnType.getElementType();
        assertTrue(pointsElementType.getInnerType() instanceof ReferenceTypeNode, "Optional inner type should be Reference");
        assertEquals("Point", ((ReferenceTypeNode)pointsElementType.getInnerType()).getReferencedTypeName());

        // Test getStyles() -> map<string, Style>
        MethodNode stylesMethod = iface.getMethods().stream().filter(m -> m.getName().equals("getStyles")).findFirst().orElse(null);
        assertNotNull(stylesMethod);
        assertEquals(0, stylesMethod.getParameters().size());
        assertTrue(stylesMethod.getReturnType().isPresent(), "Method should have a return type");
        assertTrue(stylesMethod.getReturnType().get() instanceof MapTypeNode, "Return type should be Map");
        MapTypeNode stylesReturnType = (MapTypeNode) stylesMethod.getReturnType().get();
        assertTrue(stylesReturnType.getKeyType() instanceof PrimitiveTypeNode, "Map key type should be Primitive");
        assertEquals("string", ((PrimitiveTypeNode)stylesReturnType.getKeyType()).getTypeName());
        assertTrue(stylesReturnType.getValueType() instanceof ReferenceTypeNode, "Map value type should be Reference");
        ReferenceTypeNode stylesValueType = (ReferenceTypeNode) stylesReturnType.getValueType();
        assertEquals("Style", stylesValueType.getReferencedTypeName());
    }

} 