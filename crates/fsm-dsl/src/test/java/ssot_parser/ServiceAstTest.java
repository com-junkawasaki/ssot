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
        assertEquals(Optional.empty(), method.getReturnType());
    }

    @Test
    void testInterfaceWithDetails() throws Exception {
        String input = """
        services {
            @id(100)
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
        assertEquals(Optional.of(100L), iface.getId());
        assertEquals(2, iface.getMethods().size());

        MethodNode greetMethod = iface.getMethods().stream().filter(m -> m.getName().equals("greet")).findFirst().orElse(null);
        assertNotNull(greetMethod);
        assertEquals(Optional.of(101L), greetMethod.getId());
        assertEquals(1, greetMethod.getAnnotations().size());
        assertEquals("description", greetMethod.getAnnotations().get(0).getName());
        assertEquals(1, greetMethod.getParameters().size());
        assertEquals("name", greetMethod.getParameters().get(0).getName());
        assertEquals("string", greetMethod.getParameters().get(0).getType());
        assertEquals(Optional.of("string"), greetMethod.getReturnType());

        MethodNode logMethod = iface.getMethods().stream().filter(m -> m.getName().equals("log")).findFirst().orElse(null);
        assertNotNull(logMethod);
        assertEquals(1, logMethod.getParameters().size());
        assertEquals("message", logMethod.getParameters().get(0).getName());
        assertEquals("string", logMethod.getParameters().get(0).getType());
        assertEquals(Optional.empty(), logMethod.getReturnType());
    }

     @Test
     void testServiceImplementation() throws Exception {
         String input = """
         services {
             interface MyInterface { run(data: u32); }

             @id(200)
             service MyService implements MyInterface {
                 @id(201)
                 run(data: u32) { $complexity(5) }

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

         assertEquals(1, service.getImplementedInterfaces().size());
         assertEquals("MyInterface", service.getImplementedInterfaces().get(0));

         assertEquals(2, service.getMethods().size()); // run + internalHelper

         MethodNode runMethod = service.getMethods().stream().filter(m -> m.getName().equals("run")).findFirst().orElse(null);
         assertNotNull(runMethod);
         assertEquals(Optional.of(201L), runMethod.getId());
         assertEquals(1, runMethod.getAnnotations().size());
         assertEquals("complexity", runMethod.getAnnotations().get(0).getName());
         assertEquals(1, runMethod.getParameters().size());
         assertEquals(Optional.empty(), runMethod.getReturnType());

         MethodNode helperMethod = service.getMethods().stream().filter(m -> m.getName().equals("internalHelper")).findFirst().orElse(null);
         assertNotNull(helperMethod);
         assertTrue(helperMethod.getParameters().isEmpty());
     }

} 