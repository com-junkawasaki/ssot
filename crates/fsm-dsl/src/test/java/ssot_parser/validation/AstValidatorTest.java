package ssot_parser.validation;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ParseTree;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import ssot_parser.ast.SsotRoot;
import ssot_parser.ast.nodes.*;
import ssot_parser.AstBuilderVisitor;
import ssot_parser.SSoTLexer;
import ssot_parser.SSoTParser;


public class AstValidatorTest {

    // Helper to parse and build AST (copied)
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
    void testValidMachine() throws Exception {
        String input = """
        machines {
            MyMachine {
                 $initial("StateA"); // Explicit initial state
                 states {
                    StateA {
                        on Event1 target StateB;
                        invoke Worker { onDone Success; onError Failure; }
                    }
                    StateB { on Event2 target StateA; }
                    Success { }
                    Failure { }
                 }
            }
        }
        """;
        SsotRoot root = parseAndBuildAst(input);
        AstValidator validator = new AstValidator(root);
        List<ValidationError> errors = validator.validate();
        assertTrue(errors.isEmpty(), "Valid machine should have no errors, but got: " + errors);
    }

    @Test
    void testInvalidInitialState() throws Exception {
        String input = """
        machines {
            MyMachine {
                 $initial("NonExistentState"); // Invalid initial state
                 states {
                    StateA { }
                 }
            }
        }
        """;
        SsotRoot root = parseAndBuildAst(input);
        AstValidator validator = new AstValidator(root);
        List<ValidationError> errors = validator.validate();
        assertEquals(1, errors.size(), "Should have 1 error");
        assertTrue(errors.get(0).getMessage().contains("Initial state 'NonExistentState' is not defined"), "Error message mismatch");
        assertEquals(ValidationError.Severity.ERROR, errors.get(0).getSeverity());
    }

    @Test
    void testInvalidTransitionTarget() throws Exception {
        String input = """
        machines {
            MyMachine {
                 states {
                    StateA { on Event1 target NonExistentState; } // Invalid target
                    StateB { }
                 }
            }
        }
        """;
        SsotRoot root = parseAndBuildAst(input);
        AstValidator validator = new AstValidator(root);
        List<ValidationError> errors = validator.validate();
        // We might get a warning about default initial state + the target error
        assertEquals(1, errors.stream().filter(e -> e.getSeverity() == ValidationError.Severity.ERROR).count(), "Should have 1 ERROR");
        ValidationError error = errors.stream().filter(e -> e.getSeverity() == ValidationError.Severity.ERROR).findFirst().get();
        assertTrue(error.getMessage().contains("Transition target state 'NonExistentState' for event 'Event1' from state 'StateA' is not defined"), "Error message mismatch: " + error.getMessage());
        // Check if the error node is StateA
        assertTrue(error.getNode() instanceof StateNode && ((StateNode)error.getNode()).getName().equals("StateA"));
    }

     @Test
    void testInvalidInvokeTransitionTarget() throws Exception {
        String input = """
        machines {
            MyMachine {
                 states {
                    Processing {
                        invoke Worker { onDone NonExistentSuccess; onError AlsoNonExistent; }
                    }
                    RealSuccess {} // Only this state exists
                 }
            }
        }
        """;
        SsotRoot root = parseAndBuildAst(input);
        AstValidator validator = new AstValidator(root);
        List<ValidationError> errors = validator.validate();
        // Expect 2 errors (onDone, onError targets invalid) + 1 warning (default initial state)
        assertEquals(2, errors.stream().filter(e -> e.getSeverity() == ValidationError.Severity.ERROR).count(), "Should have 2 ERRORs");

        assertTrue(errors.stream().anyMatch(e -> e.getMessage().contains("Invoke onDone transition target state 'NonExistentSuccess' is not defined")), "onDone target error missing");
        assertTrue(errors.stream().anyMatch(e -> e.getMessage().contains("Invoke onError transition target state 'AlsoNonExistent' is not defined")), "onError target error missing");

        // Check if errors point to the InvokeStateNode
        assertTrue(errors.stream().filter(e -> e.getSeverity() == ValidationError.Severity.ERROR).allMatch(e -> e.getNode() instanceof InvokeStateNode));
    }

    @Test
    void testUndefinedActionGuard() throws Exception {
        String input = """
        machines {
            MyMachine {
                 actions { Log; } // Only Log is defined
                 guards { AlwaysTrue; } // Only AlwaysTrue is defined
                 states {
                    StateA {
                        on Enter action DoSomething; // Undefined action
                        on Event1 target StateB guard CheckSomething; // Undefined guard
                        on Exit action Log; // Defined action
                    }
                    StateB {
                         invoke Something { onError target StateA action Cleanup; } // Undefined action
                    }
                 }
            }
        }
        """;
        SsotRoot root = parseAndBuildAst(input);
        AstValidator validator = new AstValidator(root);
        List<ValidationError> errors = validator.validate();
        // Expected errors: Enter action, Event1 guard, invoke action
        assertEquals(3, errors.stream().filter(e -> e.getSeverity() == ValidationError.Severity.ERROR).count(), "Should have 3 ERRORs for undefined refs");

        assertTrue(errors.stream().anyMatch(e -> e.getMessage().contains("State onEntry action 'DoSomething' is not defined")), "onEntry action error missing");
        assertTrue(errors.stream().anyMatch(e -> e.getMessage().contains("Transition guard 'CheckSomething' is not defined")), "Transition guard error missing");
        assertTrue(errors.stream().anyMatch(e -> e.getMessage().contains("Invoke onError transition action 'Cleanup' is not defined")), "Invoke action error missing");

        // Check nodes associated with errors
        assertTrue(errors.stream().anyMatch(e -> e.getMessage().contains("DoSomething") && e.getNode() instanceof StateNode));
        assertTrue(errors.stream().anyMatch(e -> e.getMessage().contains("CheckSomething") && e.getNode() instanceof TransitionNode));
        assertTrue(errors.stream().anyMatch(e -> e.getMessage().contains("Cleanup") && e.getNode() instanceof InvokeStateNode));

    }

    @Test
    void testDuplicateEnumVariant() throws Exception {
        String input = """
        types {
            enum Color {
                RED;
                GREEN;
                RED; // Duplicate
            }
        }
        """;
        SsotRoot root = parseAndBuildAst(input);
        AstValidator validator = new AstValidator(root);
        List<ValidationError> errors = validator.validate();
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).getMessage().contains("Duplicate enum variant name 'RED'"));
        assertEquals(ValidationError.Severity.ERROR, errors.get(0).getSeverity());
        assertTrue(errors.get(0).getNode() instanceof EnumVariantNode, "Error should point to the duplicate EnumVariantNode");
    }

     @Test
    void testDuplicateStructField() throws Exception {
        String input = """
        types {
            struct Point {
                x: u32;
                y: u32;
                x: f64; // Duplicate
            }
        }
        """;
        SsotRoot root = parseAndBuildAst(input);
        AstValidator validator = new AstValidator(root);
        List<ValidationError> errors = validator.validate();
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).getMessage().contains("Duplicate struct field name 'x'"));
        assertEquals(ValidationError.Severity.ERROR, errors.get(0).getSeverity());
        assertTrue(errors.get(0).getNode() instanceof FieldNode, "Error should point to the duplicate FieldNode");
    }

    @Test
    void testDuplicateInterfaceMethod() throws Exception {
         String input = """
         services {
             interface MyIface {
                 doA();
                 doB();
                 doA(); // Duplicate
             }
         }
         """;
         SsotRoot root = parseAndBuildAst(input);
         AstValidator validator = new AstValidator(root);
         List<ValidationError> errors = validator.validate();
         assertEquals(1, errors.size());
         assertTrue(errors.get(0).getMessage().contains("Duplicate method name 'doA' in interface 'MyIface'"));
         assertTrue(errors.get(0).getNode() instanceof MethodNode);
    }

    @Test
    void testDuplicateServiceMethod() throws Exception {
         String input = """
         services {
             service MyService {
                 run();
                 stop();
                 run(); // Duplicate
             }
         }
         """;
         SsotRoot root = parseAndBuildAst(input);
         AstValidator validator = new AstValidator(root);
         List<ValidationError> errors = validator.validate();
         assertEquals(1, errors.size());
         assertTrue(errors.get(0).getMessage().contains("Duplicate method name 'run' in service 'MyService'"));
         assertTrue(errors.get(0).getNode() instanceof MethodNode);
    }

    @Test
    void testUndefinedImplementedInterface() throws Exception {
         String input = """
         services {
             service MyService implements NonExistentIface { // Undefined
                 run();
             }
             interface RealIface { run(); }
         }
         """;
         SsotRoot root = parseAndBuildAst(input);
         AstValidator validator = new AstValidator(root);
         List<ValidationError> errors = validator.validate();
         assertEquals(1, errors.size());
         assertTrue(errors.get(0).getMessage().contains("Service 'MyService' implements undefined interface 'NonExistentIface'"));
         assertTrue(errors.get(0).getNode() instanceof ServiceNode);
    }

    @Test
    void testUndefinedTypeReference() throws Exception {
        String input = """
        types {
            struct Point { x: u32; y: u32; }
        }
        services {
            interface Renderer {
                 renderPoint(p: Point); // Valid
                 renderShape(s: Shape); // Invalid - Shape not defined
                 getPoints() -> list<optional<Point>>; // Valid nested
                 getStyles() -> map<string, Style>; // Invalid - Style not defined
            }
        }
        """;
        SsotRoot root = parseAndBuildAst(input);
        AstValidator validator = new AstValidator(root);
        List<ValidationError> errors = validator.validate();
        // Expect 2 errors
        assertEquals(2, errors.size(), "Should have 2 errors for undefined types");

        assertTrue(errors.stream().anyMatch(e -> e.getMessage().contains("Referenced type 'Shape' is not defined")), "Shape error missing");
        assertTrue(errors.stream().anyMatch(e -> e.getMessage().contains("Referenced type 'Style' is not defined")), "Style error missing");

        // Check nodes associated with errors
        assertTrue(errors.stream().anyMatch(e -> e.getMessage().contains("Shape") && e.getNode() instanceof ParameterNode));
        assertTrue(errors.stream().anyMatch(e -> e.getMessage().contains("Style") && e.getNode() instanceof MethodNode)); // Error points to method for return type
    }

    @Test
    void testDuplicateIds() throws Exception {
         String input = """
         types {
             @id(1)
             struct A { @id(2) field1: u32; }
             @id(1) // Duplicate type ID
             enum B { @id(3) V1; }
         }
         machines {
            @id(10) // OK - different block
            MachineX {
                @id(11)
                actions { Action1; }
                @id(11) // Duplicate action ID within machine
                guards { Guard1; }
                states {
                    @id(12)
                    StateA {
                        @id(13)
                        on Event1 target StateB;
                    }
                    @id(12) // Duplicate state ID within machine
                    StateB { }
                }
            }
         }
         """;
         SsotRoot root = parseAndBuildAst(input);
         AstValidator validator = new AstValidator(root);
         List<ValidationError> errors = validator.validate();

         assertEquals(3, errors.stream().filter(e -> e.getSeverity() == ValidationError.Severity.ERROR).count(),
                      "Should have 3 errors for duplicate IDs");

         // Check specific duplicate ID errors
         assertTrue(errors.stream().anyMatch(e -> e.getMessage().contains("Duplicate @id(1)") && e.getNode() instanceof EnumNode),
                    "Duplicate type ID error missing");
         assertTrue(errors.stream().anyMatch(e -> e.getMessage().contains("Duplicate @id(11)") && e.getNode() instanceof GuardNode),
                    "Duplicate machine inner ID (guard) error missing");
         assertTrue(errors.stream().anyMatch(e -> e.getMessage().contains("Duplicate @id(12)") && e.getNode() instanceof StateNode && ((StateNode)e.getNode()).getName().equals("StateB")),
                    "Duplicate state ID error missing");

         // Optionally check the "first used near" part of the message if implemented fully
    }

    @Test
    void testUnreachableState() throws Exception {
        String input = """
        machines {
            MyMachine {
                 $initial("A");
                 states {
                    A { on Event1 target B; }
                    B { }
                    C { } // Unreachable
                    D { invoke X { onError C; } } // C becomes reachable via invoke
                    E { } // Still unreachable
                 }
            }
        }
        """;
        SsotRoot root = parseAndBuildAst(input);
        AstValidator validator = new AstValidator(root);
        List<ValidationError> errors = validator.validate();

        // Expect 1 warning for state E
        assertEquals(1, errors.size(), "Should have 1 warning for unreachable state");
        assertEquals(1, errors.stream().filter(e -> e.getSeverity() == ValidationError.Severity.WARNING).count());
        ValidationError warning = errors.get(0);
        assertTrue(warning.getMessage().contains("State 'E' is unreachable"), "Warning message mismatch: " + warning.getMessage());
        assertEquals(ValidationError.Severity.WARNING, warning.getSeverity());
        assertTrue(warning.getNode() instanceof StateNode && ((StateNode)warning.getNode()).getName().equals("E"), "Warning should point to state E");

    }

    @Test
    void testServiceImplementationChecks() throws Exception {
         String input = """
         types { struct Data {} }
         services {
             interface Crud {
                 create(d: Data);
                 read(id: u64) -> optional<Data>;
                 delete(id: u64) -> bool;
             }

             // Valid implementation
             service DataService implements Crud {
                 create(d: Data) { }
                 read(id: u64) -> optional<Data> { }
                 delete(id: u64) -> bool { }
             }

             // Missing method
             service PartialService implements Crud {
                 create(d: Data) { }
                 // Missing read
                 delete(id: u64) -> bool { }
             }

             // Wrong parameter type
             service WrongParamService implements Crud {
                 create(d: string); // Wrong type
                 read(id: u64) -> optional<Data>;
                 delete(id: u64) -> bool;
             }

             // Wrong return type
             service WrongReturnService implements Crud {
                 create(d: Data);
                 read(id: u64) -> Data; // Wrong return type (not optional)
                 delete(id: u64) -> bool;
             }
         }
         """;
         SsotRoot root = parseAndBuildAst(input);
         AstValidator validator = new AstValidator(root);
         List<ValidationError> errors = validator.validate();

         // Expected errors: Missing read, Wrong create param, Wrong read return
         assertEquals(3, errors.size(), "Should have 3 errors");

         assertTrue(errors.stream().anyMatch(e -> e.getMessage().contains("missing implementation for method 'read'")
                                            && e.getNode() instanceof ServiceNode && ((ServiceNode)e.getNode()).getName().equals("PartialService")),
                    "Missing method error not found for PartialService");

         assertTrue(errors.stream().anyMatch(e -> e.getMessage().contains("Method signature mismatch for 'create'. Parameter types do not match")
                                            && e.getNode() instanceof MethodNode && ((MethodNode)e.getNode()).getName().equals("create")
                                            /* Check context points to WrongParamService method */ ),
                    "Parameter type mismatch error not found for WrongParamService.create");

         assertTrue(errors.stream().anyMatch(e -> e.getMessage().contains("Method signature mismatch for 'read'. Return type does not match")
                                            && e.getNode() instanceof MethodNode && ((MethodNode)e.getNode()).getName().equals("read")
                                            /* Check context points to WrongReturnService method */ ),
                    "Return type mismatch error not found for WrongReturnService.read");
    }

    // TODO: Add tests for context, initial state markers, etc.
    // TODO: Add tests for nested states and history states

} 