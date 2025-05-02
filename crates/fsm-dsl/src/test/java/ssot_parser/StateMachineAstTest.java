package ssot_parser;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ParseTree;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*; // Import static assertions

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import ssot_parser.ast.AstNode;
import ssot_parser.ast.nodes.*; // Import necessary AST node classes

/**
 * Tests for parsing state machine definitions and building the corresponding AST.
 */
public class StateMachineAstTest {

    // Helper method to parse input string and build AST
    private SsotRoot parseAndBuildAst(String inputString) throws Exception {
        InputStream is = new ByteArrayInputStream(inputString.getBytes(StandardCharsets.UTF_8));
        CharStream input = CharStreams.fromStream(is);
        SSoTLexer lexer = new SSoTLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        SSoTParser parser = new SSoTParser(tokens);

        // Basic error listener to throw exception on syntax error
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

    // --- Test Cases --- TODO: Add test cases below

    @Test
    void testMinimalMachine() throws Exception {
        String input = """
        machines {
            MyMachine {
                states {
                    StateA { }
                }
            }
        }
        """;
        SsotRoot root = parseAndBuildAst(input);

        assertEquals(1, root.getMachineDefinitions().size(), "Should have one machine definition");
        assertTrue(root.getMachineDefinitions().get(0) instanceof MachineNode, "Definition should be a MachineNode");

        MachineNode machine = (MachineNode) root.getMachineDefinitions().get(0);
        assertEquals("MyMachine", machine.getName(), "Machine name should be correct");
        assertEquals(1, machine.getStates().size(), "Machine should have one state");
        assertTrue(machine.getStates().get(0) instanceof StateNode, "State definition should be a StateNode");

        StateNode stateA = (StateNode) machine.getStates().get(0);
        assertEquals("StateA", stateA.getName(), "State name should be correct");
        assertTrue(stateA.getTransitions().isEmpty(), "State should have no transitions initially");
        assertTrue(stateA.getInvokes().isEmpty(), "State should have no invokes initially");
        assertTrue(stateA.getNestedStates().isEmpty(), "State should have no nested states initially");

        // Default initial state should be the first one if not specified
        assertEquals(Optional.of("StateA"), machine.getInitialState(), "Default initial state should be StateA");
    }

    @Test
    void testMachineWithTransition() throws Exception {
        String input = """
        machines {
            MyMachine {
                states {
                    StateA {
                        on GoToB target StateB;
                    }
                    StateB { }
                }
            }
        }
        """;
        SsotRoot root = parseAndBuildAst(input);
        assertEquals(1, root.getMachineDefinitions().size());
        MachineNode machine = (MachineNode) root.getMachineDefinitions().get(0);
        assertEquals(2, machine.getStates().size());

        StateNode stateA = machine.getStates().stream().filter(s -> s.getName().equals("StateA")).findFirst().orElse(null);
        StateNode stateB = machine.getStates().stream().filter(s -> s.getName().equals("StateB")).findFirst().orElse(null);
        assertNotNull(stateA, "StateA should exist");
        assertNotNull(stateB, "StateB should exist");

        assertEquals(1, stateA.getTransitions().size(), "StateA should have one transition");
        assertTrue(stateB.getTransitions().isEmpty(), "StateB should have no transitions");

        TransitionNode transition = stateA.getTransitions().get(0);
        assertEquals("GoToB", transition.getEvent(), "Transition event name should be correct");
        assertEquals("StateA", transition.getSourceState(), "Transition source should be correct");
        assertEquals("StateB", transition.getTargetState(), "Transition target should be correct");
        assertEquals(Optional.empty(), transition.getCondition(), "Transition should have no guard initially");
        assertEquals(Optional.empty(), transition.getAction(), "Transition should have no action initially");
        assertTrue(transition.getAnnotations().isEmpty(), "Transition should have no annotations initially");

        // Check MachineNode's transition list (collected from states)
        assertEquals(1, machine.getTransitions().size(), "MachineNode should contain the collected transition");
        assertSame(transition, machine.getTransitions().get(0), "MachineNode's transition should be the same object");

        assertEquals(Optional.of("StateA"), machine.getInitialState(), "Default initial state should still be StateA");
    }

    @Test
    void testHistoryStates() throws Exception {
        String input = """
        machines {
            MyMachine {
                states {
                    Parent {
                        history shallow; // Shallow history
                        states {
                            Child1 {}
                            Child2 {}
                        }
                        on GoToChild1 target Child1;
                    }
                    Sibling {
                         history deep; // Deep history
                         @id(123)
                         $description("Deep history state")
                         states {
                            GrandChild {
                                history; // Default shallow
                            }
                         }
                    }
                }
            }
        }
        """;
        SsotRoot root = parseAndBuildAst(input);
        assertEquals(1, root.getMachineDefinitions().size());
        MachineNode machine = (MachineNode) root.getMachineDefinitions().get(0);
        assertEquals(2, machine.getStates().size(), "Should have 2 top-level states");

        StateNode parent = machine.getStates().stream().filter(s -> s.getName().equals("Parent")).findFirst().orElse(null);
        StateNode sibling = machine.getStates().stream().filter(s -> s.getName().equals("Sibling")).findFirst().orElse(null);
        assertNotNull(parent);
        assertNotNull(sibling);

        // Test Parent state history
        assertTrue(parent.getHistory().isPresent(), "Parent state should have history");
        assertEquals(HistoryNode.HistoryType.SHALLOW, parent.getHistory().get().getHistoryType(), "Parent history should be shallow");
        assertTrue(parent.getHistory().get().getAnnotations().isEmpty(), "Parent history should have no annotations");
        assertEquals(2, parent.getNestedStates().size(), "Parent should have 2 nested states");

        // Test Sibling state history
        assertTrue(sibling.getHistory().isPresent(), "Sibling state should have history");
        assertEquals(HistoryNode.HistoryType.DEEP, sibling.getHistory().get().getHistoryType(), "Sibling history should be deep");
        assertEquals(Optional.of(123L), sibling.getHistory().get().getId(), "Sibling history should have @id");
        assertEquals(1, sibling.getHistory().get().getAnnotations().size(), "Sibling history should have one annotation ($description)");
        assertEquals("description", sibling.getHistory().get().getAnnotations().get(0).getName());

        // Test nested history in Sibling
        assertEquals(1, sibling.getNestedStates().size(), "Sibling should have 1 nested state");
        StateNode grandChild = sibling.getNestedStates().get(0);
        assertEquals("GrandChild", grandChild.getName());
        assertTrue(grandChild.getHistory().isPresent(), "GrandChild should have history");
        assertEquals(HistoryNode.HistoryType.SHALLOW, grandChild.getHistory().get().getHistoryType(), "GrandChild history should default to shallow");
    }

    @Test
    void testStateWithInvoke() throws Exception {
        String input = """
        machines {
            MyMachine {
                // Define invokes block if needed by grammar for resolving INVOKE_ID
                // invokes { MyService {} }
                states {
                    Idle {
                        on Start target Processing;
                    }
                    Processing {
                        @id(555)
                        invoke MyService { // Assuming MyService is resolvable
                           $description("Calling the service")
                           onDone Success;
                           onError Failure;
                        }
                    }
                    Success { }
                    Failure { }
                }
            }
        }
        """;
        SsotRoot root = parseAndBuildAst(input);
        assertEquals(1, root.getMachineDefinitions().size());
        MachineNode machine = (MachineNode) root.getMachineDefinitions().get(0);
        assertEquals(4, machine.getStates().size());

        StateNode processing = machine.getStates().stream().filter(s -> s.getName().equals("Processing")).findFirst().orElse(null);
        assertNotNull(processing);
        assertEquals(1, processing.getInvokes().size(), "Processing state should have one invoke");

        InvokeStateNode invoke = processing.getInvokes().get(0);
        assertEquals("MyService", invoke.getSrc(), "Invoke source should be MyService");
        assertEquals(Optional.of(555L), invoke.getId(), "Invoke should have @id"); // Check ID on invoke itself if grammar allows
        // Check annotations directly on invoke if grammar supports it
        assertEquals(1, invoke.getAnnotations().size());
        assertEquals("description", invoke.getAnnotations().get(0).getName());

        assertEquals(Optional.of("Success"), invoke.getOnDoneTarget(), "onDone target should be Success");
        assertEquals(Optional.of("Failure"), invoke.getOnErrorTarget(), "onError target should be Failure");
    }

    @Test
    void testInvokeSimple() throws Exception {
        String input = """
        machines {
            MyMachine {
                states {
                    Invoking {
                        invoke DataFetcher {
                            onDone { // Simple target
                                target Success;
                            }
                            onError { // Target with action and guard
                                target Failure;
                                guard CanRetry;
                                action LogError;
                            }
                        }
                    }
                    Success { }
                    Failure { }
                }
            }
        }
        """;
        SsotRoot root = parseAndBuildAst(input);
        assertEquals(1, root.getMachineDefinitions().size());
        MachineNode machine = (MachineNode) root.getMachineDefinitions().get(0);
        StateNode invoking = machine.getStates().stream().filter(s -> s.getName().equals("Invoking")).map(s -> (StateNode)s).findFirst().orElse(null);
        assertNotNull(invoking);
        assertEquals(1, invoking.getInvokes().size());

        InvokeStateNode invokeNode = invoking.getInvokes().get(0);
        assertEquals("DataFetcher", invokeNode.getSrc());
        assertTrue(invokeNode.getAnnotations().isEmpty());
        // assertEquals(Optional.empty(), invokeNode.getOnDoneTarget()); // Old assertion
        // assertEquals(Optional.empty(), invokeNode.getOnErrorTarget()); // Old assertion
        assertTrue(invokeNode.getOnDoneTransition().isEmpty(), "onDone should be empty");
        assertTrue(invokeNode.getOnErrorTransition().isEmpty(), "onError should be empty");
    }

    @Test
    void testInvokeWithOptions() throws Exception {
        String input = """
        machines {
            MyMachine {
                states {
                    Invoking {
                        invoke DataFetcher {
                            onDone { // Simple target
                                target Success;
                            }
                            onError { // Target with action and guard
                                target Failure;
                                guard CanRetry;
                                action LogError;
                            }
                        }
                    }
                    Success { }
                    Failure { }
                }
            }
        }
        """;
        SsotRoot root = parseAndBuildAst(input);
        assertEquals(1, root.getMachineDefinitions().size());
        MachineNode machine = (MachineNode) root.getMachineDefinitions().get(0);
        StateNode invoking = machine.getStates().stream().filter(s -> s.getName().equals("Invoking")).map(s -> (StateNode)s).findFirst().orElse(null);
        assertNotNull(invoking);
        assertEquals(1, invoking.getInvokes().size());

        InvokeStateNode invokeNode = invoking.getInvokes().get(0);
        assertEquals("DataFetcher", invokeNode.getSrc());

        // Check onDone
        assertTrue(invokeNode.getOnDoneTransition().isPresent(), "onDone should be present");
        InvokeStateNode.InvokeTransition onDone = invokeNode.getOnDoneTransition().get();
        assertEquals(Optional.of("Success"), onDone.target());
        assertTrue(onDone.action().isEmpty());
        assertTrue(onDone.guard().isEmpty());
        assertTrue(onDone.annotations().isEmpty());

        // Check onError
        assertTrue(invokeNode.getOnErrorTransition().isPresent(), "onError should be present");
        InvokeStateNode.InvokeTransition onError = invokeNode.getOnErrorTransition().get();
        assertEquals(Optional.of("Failure"), onError.target());
        assertEquals(Optional.of("LogError"), onError.action());
        assertEquals(Optional.of("CanRetry"), onError.guard());
        assertTrue(onError.annotations().isEmpty());
    }

    @Test
    void testNestedStatesParsing() throws Exception {
        String input = """
        machines {
            TrafficLight {
                 $initial("Red");
                 states {
                     Red { on Timer target Green; }
                     Green { on Timer target Yellow; }
                     Yellow { on Timer target Red; }
                     Off {
                         $initial("Solid"); // Initial state for nested
                         history shallow; // History for Off state
                         states {
                             Solid { on PowerOn target Red; }
                             Flashing { on PowerOn target Red; }
                         }
                         on PowerOff target Solid; // Transition within parent state
                     }
                 }
            }
        }
        """;
        SsotRoot root = parseAndBuildAst(input);
        assertEquals(1, root.getMachineDefinitions().size());
        MachineNode machine = (MachineNode) root.getMachineDefinitions().get(0);
        assertEquals("TrafficLight", machine.getName());
        assertEquals(Optional.of("Red"), machine.getInitialState());
        assertEquals(4, machine.getStates().size()); // Red, Green, Yellow, Off

        StateNode offState = machine.getStates().stream().filter(s -> s.getName().equals("Off")).map(s->(StateNode)s).findFirst().orElse(null);
        assertNotNull(offState);
        assertEquals(1, offState.getTransitions().size()); // PowerOff transition
        assertEquals("Solid", offState.getTransitions().get(0).getTargetState());
        assertTrue(offState.getHistory().isPresent());
        assertEquals(HistoryNode.HistoryType.SHALLOW, offState.getHistory().get().getHistoryType());

        assertEquals(2, offState.getNestedStates().size()); // Solid, Flashing
        StateNode solidState = offState.getNestedStates().stream().filter(s -> s.getName().equals("Solid")).map(s->(StateNode)s).findFirst().orElse(null);
        StateNode flashingState = offState.getNestedStates().stream().filter(s -> s.getName().equals("Flashing")).map(s->(StateNode)s).findFirst().orElse(null);
        assertNotNull(solidState);
        assertNotNull(flashingState);

        assertEquals(1, solidState.getTransitions().size());
        assertEquals("Red", solidState.getTransitions().get(0).getTargetState());
        assertEquals("PowerOn", solidState.getTransitions().get(0).getEvent());

        // Note: Nested states might have their own initial state marker within the grammar
        // The validator might need to check this. Here we just check parsing.
    }

    // TODO: Add tests for context, actions, guards, initial state markers, etc.
    // TODO: Add tests for nested states and history states
} 