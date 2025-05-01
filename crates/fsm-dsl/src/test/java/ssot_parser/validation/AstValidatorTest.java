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

    // TODO: Add tests for context, initial state markers, etc.
    // TODO: Add tests for nested states and history states

} 