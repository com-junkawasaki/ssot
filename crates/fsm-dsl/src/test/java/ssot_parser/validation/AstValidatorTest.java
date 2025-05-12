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
            machine MyMachine {
                 states {
                    initial state NonExistentState;
                    state StateA { }
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
            machine MyMachine {
                 states {
                    initial state StateA;
                    state StateA { on Event1 transition NonExistentState; }
                    state StateB { }
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
            machine MyMachine {
                 actions { Worker; }
                 states {
                    initial state Processing;
                    state Processing {
                        invoke Worker { onDone NonExistentSuccess; onError AlsoNonExistent; };
                    }
                    state RealSuccess {}
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
            machine MyMachine {
                 actions { Log; }
                 guards { AlwaysTrue; }
                 states {
                    initial state StateA;
                    state StateA {
                        onEntry action DoSomething;
                        on Event1 transition StateB guard CheckSomething;
                        onExit action Log;
                    }
                    state StateB {
                         invoke Log { onError transition StateA action Cleanup; };
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
             service MyServiceImpl { $implements(UnknownInterface); }
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
            struct Point {
                x: u32;
                y: NotAType;
            }
            enum Status {
                ACTIVE;
                INACTIVE(reason: MaybeType);
            }
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
             @id(100)
             struct TypeA {}
             @id(100)
             enum TypeB { V1; }
         }
         services {
             @id(200)
             interface IfaceA { m(); }
             @id(100)
             service ServiceA { $implements(IfaceA); }
         }
         machines {
             @id(300)
             machine MachineA {
                 actions { ActionA; }
                 states { initial state S1; state S1{}; }
             }
             @id(300)
             machine MachineB {
                 states { initial state S2; state S2{}; }
             }
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
            machine MyMachine {
                 states {
                    initial state StateA;
                    state StateA { on E1 transition StateB; }
                    state StateB { }
                    state UnreachableState { }
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
         types { struct InputData {}; struct OutputData {}; }
         services {
             interface MyService {
                 methodA(data: InputData) -> OutputData;
                 methodB(flag: bool);
             }

             service MyServiceImpl_Correct { $implements(MyService);
                  methodA(data: InputData) -> OutputData;
                  methodB(flag: bool);
             }

             service MyServiceImpl_Missing { $implements(MyService);
                  methodA(data: InputData) -> OutputData;
             }

             service MyServiceImpl_ParamType { $implements(MyService);
                  methodA(data: string) -> OutputData;
                  methodB(flag: bool);
             }

             service MyServiceImpl_ReturnType { $implements(MyService);
                  methodA(data: InputData) -> string;
                  methodB(flag: bool);
             }

              service MyServiceImpl_Extra { $implements(MyService);
                   methodA(data: InputData) -> OutputData;
                   methodB(flag: bool);
                   methodC();
              }
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
             machine MyMachine {
                  states {
                     initial state Idle;
                     state Idle {
                         invoke NonExistentService { onDone Success; };
                     }
                     state Success { type final; }
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
            MyMachine {
                 $initial("OuterA");
                 states {
                    OuterA {
                        $initial("InnerA1"); // Initial for nested
                        states {
                           InnerA1 { on Event1 target InnerA2; }
                           InnerA2 { }
                        }
                        on Event2 target OuterB;
                    }
                    OuterB { }
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
            MyMachine {
                 $initial("OuterA");
                 states {
                    OuterA { // Compound state missing $initial
                        states {
                           InnerA1 { on Event1 target InnerA2; }
                           InnerA2 { }
                        }
                        on Event2 target OuterB;
                    }
                    OuterB { }
                 }
            }
        }
        """;
        SsotRoot root = parseAndBuildAst(input);
        AstValidator validator = new AstValidator(root);
        List<ValidationError> errors = validator.validate();
        assertEquals(1, errors.stream().filter(e -> e.getSeverity() == ValidationError.Severity.ERROR).count(), "Should have 1 ERROR for missing initial state");
        ValidationError error = errors.stream().filter(e -> e.getSeverity() == ValidationError.Severity.ERROR).findFirst().get();
        assertTrue(error.getMessage().contains("Compound state 'OuterA' must specify an initial state using $initial"), "Error message mismatch: " + error.getMessage());
        assertTrue(error.getNode() instanceof StateNode && ((StateNode)error.getNode()).getStateName().equals("OuterA"), "Error should point to OuterA state node");
    }

    @Test
    void testValidShallowHistory() throws Exception {
        String input = """
        machines {
            MyMachine {
                 $initial("Group");
                 states {
                    Group {
                        $initial("A");
                        $H; // Shallow history marker
                        states {
                           A { on Ev1 target B; }
                           B { on Ev2 target C; }
                           C { }
                        }
                        on Interrupt target Interrupted;
                    }
                    Interrupted {
                        on Resume target Group.$H; // Transition to history
                    }
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
            MyMachine {
                 $initial("Group1");
                 states {
                    Group1 {
                        $initial("A");
                        $H*; // Deep history marker
                        states {
                           A { on Ev1 target Group2; }
                           Group2 {
                               $initial("B");
                               $H; // Shallow history within deep
                               states {
                                  B { on Ev2 target C; }
                                  C { }
                               }
                               on Back target A;
                           }
                        }
                        on Interrupt target Interrupted;
                    }
                    Interrupted {
                        on Resume target Group1.$H*; // Transition to deep history
                    }
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
            MyMachine {
                 $initial("Group");
                 states {
                    Group { // No history marker ($H or $H*)
                        $initial("A");
                        states {
                           A { on Ev1 target B; }
                           B { }
                        }
                        on Interrupt target Interrupted;
                    }
                    Interrupted {
                        on Resume target Group.$H; // Invalid: Group has no $H
                    }
                    InterruptedDeep {
                        on ResumeDeep target Group.$H*; // Invalid: Group has no $H*
                    }
                 }
            }
        }
        """;
        SsotRoot root = parseAndBuildAst(input);
        AstValidator validator = new AstValidator(root);
        List<ValidationError> errors = validator.validate();

        assertEquals(2, errors.stream().filter(e -> e.getSeverity() == ValidationError.Severity.ERROR).count(), "Should have 2 ERRORs for invalid history transitions");

        assertTrue(errors.stream().anyMatch(e -> e.getMessage().contains("Transition target 'Group.$H' refers to a shallow history state, but state 'Group' does not define one")), "Shallow history error missing");
        assertTrue(errors.stream().anyMatch(e -> e.getMessage().contains("Transition target 'Group.$H*' refers to a deep history state, but state 'Group' does not define one")), "Deep history error missing");

        assertTrue(errors.stream().filter(e -> e.getSeverity() == ValidationError.Severity.ERROR).allMatch(e -> e.getNode() instanceof TransitionNode), "Errors should point to TransitionNode");
    }

    @Test
    void testDuplicateStateNameNested() throws Exception {
        String input = """
        machines {
            MyMachine {
                 $initial("OuterA");
                 states {
                    OuterA {
                        $initial("InnerA");
                        states {
                           InnerA { }
                           InnerB { }
                           InnerA { } // Duplicate name within OuterA
                        }
                    }
                    OuterB {
                        $initial("InnerA"); // OK, different parent
                        states {
                            InnerA {}
                        }
                    }
                    OuterC { }
                    OuterA { } // Duplicate name at top level
                 }
            }
        }
        """;
        SsotRoot root = parseAndBuildAst(input);
        AstValidator validator = new AstValidator(root);
        List<ValidationError> errors = validator.validate();

        assertEquals(2, errors.stream().filter(e -> e.getSeverity() == ValidationError.Severity.ERROR).count(), "Should have 2 ERRORs for duplicate state names");

        // Check duplicate within OuterA
        assertTrue(errors.stream().anyMatch(e -> e.getMessage().contains("Duplicate state name 'InnerA' within parent state 'OuterA'")
                                            && e.getNode() instanceof StateNode && ((StateNode)e.getNode()).getStateName().equals("InnerA")), // Points to the second InnerA
                    "Duplicate nested state name error missing");

        // Check duplicate at top level
        assertTrue(errors.stream().anyMatch(e -> e.getMessage().contains("Duplicate state name 'OuterA' within parent machine 'MyMachine'")
                                            && e.getNode() instanceof StateNode && ((StateNode)e.getNode()).getStateName().equals("OuterA")), // Points to the second OuterA
                    "Duplicate top-level state name error missing");
    }

    // TODO: Add tests for context, initial state markers, etc.
    // TODO: Add tests for nested states and history states

} 