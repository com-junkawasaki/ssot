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
            @id(1) machine MyMachine {
                 actions { Worker(); }
                 states {
                    initial state StateA;
                    state StateA {
                        on Event1 transition StateB;
                        invoke Worker { onDone Success; onError Failure; };
                    };
                    state StateB { on Event2 transition StateA; };
                    state Success { type final; };
                    state Failure { type final; };
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
            @id(2) machine MyMachine {
                states {
                    state StateA {};
                    state StateB {};
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
            @id(3) machine MyMachine {
                states {
                    initial state StateA;
                    state StateA { on Event1 transition NonExistentState; };
                    state StateB {};
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
        assertTrue(error.getNode() instanceof StateNode && ((StateNode)error.getNode()).getStateName().equals("StateA"));
    }

     @Test
    void testInvalidInvokeTransitionTarget() throws Exception {
        String input = """
        machines {
            @id(4) machine MyMachine {
                 actions { Worker(); }
                 states {
                    initial state Processing;
                    state Processing {
                        invoke Worker { onDone NonExistentSuccess; onError AlsoNonExistent; };
                    };
                    state RealSuccess {};
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
            @id(5) machine MyMachine {
                 actions { Log(); DoSomething(); Cleanup(); }
                 guards { AlwaysTrue(); CheckSomething(); }
                 states {
                    initial state StateA;
                    state StateA {
                        onEntry action DoSomething();
                        on Event1 transition StateB guard CheckSomething();
                        onExit action Log();
                    }
                    state StateB {
                         invoke Log { onError transition StateA action Cleanup(); };
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
            @id(10) enum Color {
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
            @id(11) struct Point {
                x: i32;
                y: i32;
                x: string; // Duplicate name
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
             @id(12) interface MyIface {
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
             @id(13) interface BadInterface {
                 run(); run(); // Duplicate methods
             }
             @id(14) service MyService { $implements(BadInterface) }
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
             @id(6) service MyServiceImpl { $implements(UnknownInterface) }
         }
         """;
         SsotRoot root = parseAndBuildAst(input);
         AstValidator validator = new AstValidator(root);
         List<ValidationError> errors = validator.validate();
         assertEquals(1, errors.size());
         assertTrue(errors.get(0).getMessage().contains("Service 'MyService' implements undefined interface 'UnknownInterface'"));
         assertTrue(errors.get(0).getNode() instanceof ServiceNode);
    }

    @Test
    void testUndefinedTypeReference() throws Exception {
        String input = """
        types {
            @id(7) struct Box {
                content: UnknownType; // Undefined type
            }
            @id(8) enum Status {
                PENDING; SUCCESS; FAILURE;
            }
        }
        services {
            @id(9) interface Renderer {
                 renderShape(s: Shape); // Undefined Shape type
                 getStyles() -> map<string, Style>; // Undefined Style type
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
             @id(1) struct A { x: i32; }
         }
         services {
             @id(1) interface B { go(); }
         }
         machines {
             @id(2) machine C {
                 states { initial state S1; state S1 { @id(1) }; }
             }
             @id(2) machine D { states { initial state S2; state S2{}; }; }
         }
         """;
         SsotRoot root = parseAndBuildAst(input);
         AstValidator validator = new AstValidator(root);
         List<ValidationError> errors = validator.validate();

         assertEquals(3, errors.stream().filter(e -> e.getSeverity() == ValidationError.Severity.ERROR).count(),
                      "Should have 3 errors for duplicate IDs");

         // Check specific duplicate ID errors
         assertTrue(errors.stream().anyMatch(e -> e.getMessage().contains("Duplicate @id(100)") && e.getNode() instanceof EnumNode),
                    "Duplicate type ID error missing");
         assertTrue(errors.stream().anyMatch(e -> e.getMessage().contains("Duplicate @id(100)") && e.getNode() instanceof MethodNode),
                    "Duplicate machine inner ID (guard) error missing");
         assertTrue(errors.stream().anyMatch(e -> e.getMessage().contains("Duplicate @id(100)") && e.getNode() instanceof StateNode && ((StateNode)e.getNode()).getStateName().equals("S1")),
                    "Duplicate state ID error missing");

         // Optionally check the "first used near" part of the message if implemented fully
    }

    @Test
    void testUnreachableState() throws Exception {
        String input = """
        machines {
            @id(15) machine Unreachable {
                states {
                    initial state A;
                    state A { on Ev1 transition B; };
                    state B {};
                    state C {};
                }
            }
        }
        """;
        SsotRoot root = parseAndBuildAst(input);
        AstValidator validator = new AstValidator(root);
        List<ValidationError> errors = validator.validate();

        // Expect 1 warning for state UnreachableState
        assertEquals(1, errors.size(), "Should have 1 warning for unreachable state");
        assertEquals(1, errors.stream().filter(e -> e.getSeverity() == ValidationError.Severity.WARNING).count());
        ValidationError warning = errors.get(0);
        assertTrue(warning.getMessage().contains("State 'UnreachableState' is unreachable"), "Warning message mismatch: " + warning.getMessage());
        assertEquals(ValidationError.Severity.WARNING, warning.getSeverity());
        assertTrue(warning.getNode() instanceof StateNode && ((StateNode)warning.getNode()).getStateName().equals("UnreachableState"), "Warning should point to state UnreachableState");

    }

    @Test
    void testServiceImplementationChecks() throws Exception {
         String input = """
         types { \
             struct Request { data: string; } \
             struct Response { value: i32; } \
         }
         services {
             @id(20) interface Calculator {
                 add(a: i32, b: i32) -> i32;
                 process(req: Request) -> Response;
                 notify(msg: string); \
             }

             @id(21) service CalcImpl { $implements(Calculator) }

             @id(22) service WrongImpl { $implements(MissingInterface) }
         }
         """;
         SsotRoot root = parseAndBuildAst(input);
         AstValidator validator = new AstValidator(root);
         List<ValidationError> errors = validator.validate();

         // Expected errors: Missing read, Wrong create param, Wrong read return
         assertEquals(3, errors.size(), "Should have 3 errors");

         assertTrue(errors.stream().anyMatch(e -> e.getMessage().contains("missing implementation for method 'read'")
                                            && e.getNode() instanceof ServiceNode && ((ServiceNode)e.getNode()).getName().equals("MyServiceImpl_Missing")),
                    "Missing method error not found for MyServiceImpl_Missing");

         assertTrue(errors.stream().anyMatch(e -> e.getMessage().contains("Method signature mismatch for 'create'. Parameter types do not match")
                                            && e.getNode() instanceof MethodNode && ((MethodNode)e.getNode()).getName().equals("methodA")
                                            /* Check context points to MyServiceImpl_ParamType method */ ),
                    "Parameter type mismatch error not found for MyServiceImpl_ParamType.methodA");

         assertTrue(errors.stream().anyMatch(e -> e.getMessage().contains("Method signature mismatch for 'read'. Return type does not match")
                                            && e.getNode() instanceof MethodNode && ((MethodNode)e.getNode()).getName().equals("methodA")
                                            /* Check context points to MyServiceImpl_ReturnType method */ ),
                    "Return type mismatch error not found for MyServiceImpl_ReturnType.methodA");
    }

    @Test
    void testUndefinedInvokeSource() throws Exception {
         String input = """
         machines {
             @id(16) machine InvokeTest {
                  states {
                     initial state A;
                     state A { invoke UndefinedAction { onDone B; }; };
                     state B { type final; };
                  }
             }
         }
         """;
         SsotRoot root = parseAndBuildAst(input);
         AstValidator validator = new AstValidator(root);
         List<ValidationError> errors = validator.validate();

         // Should have 1 error for UndefinedThing, plus maybe 1 warning for default initial state
         assertEquals(1, errors.stream().filter(e -> e.getSeverity() == ValidationError.Severity.ERROR).count(), "Should have 1 ERROR");
         ValidationError error = errors.stream().filter(e -> e.getSeverity() == ValidationError.Severity.ERROR).findFirst().get();

         assertTrue(error.getMessage().contains("Invoke source 'UndefinedThing' does not resolve to a defined service or machine"), "Error message mismatch");
         assertTrue(error.getNode() instanceof InvokeStateNode, "Error should point to InvokeStateNode");
    }

    @Test
    void testValidNestedStates() throws Exception {
        String input = """
        machines {
            @id(17) machine Nested {
                states {
                    initial state Parent;
                    state Parent {
                        initial state ChildA;
                        state ChildA { on Ev transition ChildB; };
                        state ChildB {};
                    };
                }
            }
        }
        """;
        SsotRoot root = parseAndBuildAst(input);
        AstValidator validator = new AstValidator(root);
        List<ValidationError> errors = validator.validate();
        assertTrue(errors.isEmpty(), "Valid nested states should have no errors, but got: " + errors);
    }

    @Test
    void testInvalidNestedStateMissingInitial() throws Exception {
        String input = """
        machines {
            @id(18) machine Nested {
                states {
                    initial state Parent;
                    state Parent {
                        state ChildA {};
                    };
                }
            }
        }
        """;
        SsotRoot root = parseAndBuildAst(input);
        AstValidator validator = new AstValidator(root);
        List<ValidationError> errors = validator.validate();
        assertEquals(1, errors.stream().filter(e -> e.getSeverity() == ValidationError.Severity.ERROR).count(), "Should have 1 ERROR for missing initial state");
        ValidationError error = errors.stream().filter(e -> e.getSeverity() == ValidationError.Severity.ERROR).findFirst().get();
        assertTrue(error.getMessage().contains("Compound state 'Parent' must specify an initial state using $initial"), "Error message mismatch: " + error.getMessage());
        assertTrue(error.getNode() instanceof StateNode && ((StateNode)error.getNode()).getStateName().equals("Parent"), "Error should point to Parent state node");
    }

    @Test
    void testValidShallowHistory() throws Exception {
        String input = """
        machines {
            @id(19) machine HistoryTest {
                states {
                    initial state A;
                    state A { on Ev transition B; };
                    state B {
                        history shallow H;
                        initial state B1;
                        state B1 { on Ev transition B2; };
                        state B2 { on Ev transition A; };
                    };
                }
            }
        }
        """;
        SsotRoot root = parseAndBuildAst(input);
        AstValidator validator = new AstValidator(root);
        List<ValidationError> errors = validator.validate();
        assertTrue(errors.isEmpty(), "Valid shallow history should have no errors, but got: " + errors);
    }

    @Test
    void testValidDeepHistory() throws Exception {
        String input = """
        machines {
            @id(20) machine HistoryTest {
                states {
                    initial state A;
                    state A { on Ev transition B; };
                    state B {
                        history deep H;
                        initial state B1;
                        state B1 {
                            initial state B1a;
                            state B1a { on Ev transition B2; };
                        };
                        state B2 { on Ev transition A; };
                    };
                }
            }
        }
        """;
        SsotRoot root = parseAndBuildAst(input);
        AstValidator validator = new AstValidator(root);
        List<ValidationError> errors = validator.validate();
        assertTrue(errors.isEmpty(), "Valid deep history should have no errors, but got: " + errors);
    }

    @Test
    void testInvalidTransitionToMissingHistory() throws Exception {
        String input = """
        machines {
            @id(21) machine HistoryTest {
                states {
                    initial state A;
                    state A { on Ev transition B.H; };
                    state B {
                        initial state B1;
                        state B1 {};
                    };
                }
            }
        }
        """;
        SsotRoot root = parseAndBuildAst(input);
        AstValidator validator = new AstValidator(root);
        List<ValidationError> errors = validator.validate();

        assertEquals(2, errors.stream().filter(e -> e.getSeverity() == ValidationError.Severity.ERROR).count(), "Should have 2 ERRORs for invalid history transitions");

        assertTrue(errors.stream().anyMatch(e -> e.getMessage().contains("Transition target 'Parent.$H' refers to a shallow history state, but state 'Parent' does not define one")), "Shallow history error missing");
        assertTrue(errors.stream().anyMatch(e -> e.getMessage().contains("Transition target 'Parent.$H*' refers to a deep history state, but state 'Parent' does not define one")), "Deep history error missing");

        assertTrue(errors.stream().filter(e -> e.getSeverity() == ValidationError.Severity.ERROR).allMatch(e -> e.getNode() instanceof TransitionNode), "Errors should point to TransitionNode");
    }

    @Test
    void testDuplicateStateNameNested() throws Exception {
        String input = """
        machines {
            @id(22) machine NestedDup {
                states {
                    initial state A;
                    state A {
                        initial state B;
                        state B {};
                    };
                    state B {
                         initial state C;
                         state C{};
                    };
                    state A {
                       initial state D;
                       state D {};
                    };
                }
            }
        }
        """;
        SsotRoot root = parseAndBuildAst(input);
        AstValidator validator = new AstValidator(root);
        List<ValidationError> errors = validator.validate();

        assertEquals(2, errors.stream().filter(e -> e.getSeverity() == ValidationError.Severity.ERROR).count(), "Should have 2 ERRORs for duplicate state names");

        // Check duplicate within Parent
        assertTrue(errors.stream().anyMatch(e -> e.getMessage().contains("Duplicate state name 'Child' within parent state 'Parent'")
                                            && e.getNode() instanceof StateNode && ((StateNode)e.getNode()).getStateName().equals("Child")),
                    "Duplicate nested state name error missing");

        // Check duplicate at top level
        assertTrue(errors.stream().anyMatch(e -> e.getMessage().contains("Duplicate state name 'Parent' within parent machine 'MyMachine'")
                                            && e.getNode() instanceof StateNode && ((StateNode)e.getNode()).getStateName().equals("Parent")),
                    "Duplicate top-level state name error missing");
    }

    // TODO: Add tests for context, initial state markers, etc.
    // TODO: Add tests for nested states and history states

} 