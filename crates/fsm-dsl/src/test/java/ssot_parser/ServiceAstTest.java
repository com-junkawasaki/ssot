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
            @id(100) interface Greeter {
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
        assertEquals(Optional.of(100L), iface.getId());
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
             interface MyInterface { run(data: u32); }

             @id(200)
             service MyService {
                 $implements(MyInterface);
                 @id(201)
                 run(data: u32) { $complexity(5) };

                 // Own method
                 internalHelper();
             }
         }
         """;
         SsotRoot root = parseAndBuildAst(input);
         assertEquals(2, root.getServiceDefinitions().size()); // Interface + Service

         ServiceNode service = root.getServiceDefinitions().stream()
                                .filter(s -> s instanceof ServiceNode)
                                .map(s -> (ServiceNode)s)
                                .findFirst().orElse(null);
         assertNotNull(service);
         assertEquals("MyService", service.getName());
         assertEquals(Optional.of(200L), service.getId());

         // Implementation check via annotation
         assertEquals(1, service.getAnnotations().size());
         assertTrue(service.getAnnotations().containsKey("implements"));
         assertEquals("MyInterface", service.getAnnotations().get("implements"));

         assertEquals(2, service.getMethods().size()); // run + internalHelper

         MethodNode runMethod = service.getMethods().stream().filter(m -> m.getName().equals("run")).findFirst().orElse(null);
         assertNotNull(runMethod);
         assertEquals(Optional.of(201L), runMethod.getId());
         assertEquals(1, runMethod.getAnnotations().size());
         assertTrue(runMethod.getAnnotations().containsKey("complexity"), "Should contain $complexity annotation");
         assertEquals(5L, runMethod.getAnnotations().get("complexity"));
         assertEquals(1, runMethod.getParameters().size());
         assertTrue(runMethod.getParameters().get(0).getType() instanceof PrimitiveTypeNode, "Run parameter type should be Primitive");
         assertEquals("u32", ((PrimitiveTypeNode)runMethod.getParameters().get(0).getType()).getTypeName());
         assertTrue(runMethod.getReturnType().isEmpty(), "Run return type should be empty");

         MethodNode helperMethod = service.getMethods().stream().filter(m -> m.getName().equals("internalHelper")).findFirst().orElse(null);
         assertNotNull(helperMethod);
         assertTrue(helperMethod.getParameters().isEmpty());
     }

    @Test
    void testComplexTypes() throws Exception {
        String input = """
        types {
            struct User {};
        }
        services {
            interface TypeTester {
                processOptional(data: optional<string>);
                processList(items: list<User>);
                processMap(lookup: map<string, list<optional<u64>>>);
            }
        }
        """;
        SsotRoot root = parseAndBuildAst(input);
        assertEquals(1, root.getServiceDefinitions().size()); // Only interface
        assertTrue(root.getServiceDefinitions().get(0) instanceof InterfaceNode);
        InterfaceNode iface = (InterfaceNode) root.getServiceDefinitions().get(0);
        assertEquals("TypeTester", iface.getName());
        assertEquals(3, iface.getMethods().size());

        // Test optional<string>
        MethodNode optMethod = iface.getMethods().stream().filter(m -> m.getName().equals("processOptional")).findFirst().orElse(null);
        assertNotNull(optMethod);
        assertEquals(1, optMethod.getParameters().size());
        ParameterNode optParam = optMethod.getParameters().get(0);
        assertTrue(optParam.getType() instanceof OptionalTypeNode, "Type should be Optional");
        OptionalTypeNode optType = (OptionalTypeNode) optParam.getType();
        assertTrue(optType.getInnerType() instanceof PrimitiveTypeNode, "Inner type should be Primitive");
        assertEquals("string", ((PrimitiveTypeNode)optType.getInnerType()).getTypeName());

        // Test list<User>
        MethodNode listMethod = iface.getMethods().stream().filter(m -> m.getName().equals("processList")).findFirst().orElse(null);
        assertNotNull(listMethod);
        assertEquals(1, listMethod.getParameters().size());
        ParameterNode listParam = listMethod.getParameters().get(0);
        assertTrue(listParam.getType() instanceof ListTypeNode, "Type should be List");
        ListTypeNode listType = (ListTypeNode) listParam.getType();
        assertTrue(listType.getElementType() instanceof ReferenceTypeNode, "Element type should be Reference");
        assertEquals("User", ((ReferenceTypeNode)listType.getElementType()).getReferencedTypeName());

        // Test map<string, list<optional<u64>>>
        MethodNode mapMethod = iface.getMethods().stream().filter(m -> m.getName().equals("processMap")).findFirst().orElse(null);
        assertNotNull(mapMethod);
        assertEquals(1, mapMethod.getParameters().size());
        ParameterNode mapParam = mapMethod.getParameters().get(0);
        assertTrue(mapParam.getType() instanceof MapTypeNode, "Type should be Map");
        MapTypeNode mapType = (MapTypeNode) mapParam.getType();

        assertTrue(mapType.getKeyType() instanceof PrimitiveTypeNode, "Map key type should be Primitive");
        assertEquals("string", ((PrimitiveTypeNode)mapType.getKeyType()).getTypeName());

        assertTrue(mapType.getValueType() instanceof ListTypeNode, "Map value type should be List");
        ListTypeNode mapValueListType = (ListTypeNode) mapType.getValueType();
        assertTrue(mapValueListType.getElementType() instanceof OptionalTypeNode, "Map value element type should be Optional");
        OptionalTypeNode mapValueOptionalType = (OptionalTypeNode) mapValueListType.getElementType();
        assertTrue(mapValueOptionalType.getInnerType() instanceof PrimitiveTypeNode, "Map value inner type should be Primitive");
        assertEquals("u64", ((PrimitiveTypeNode)mapValueOptionalType.getInnerType()).getTypeName());

    }

} 