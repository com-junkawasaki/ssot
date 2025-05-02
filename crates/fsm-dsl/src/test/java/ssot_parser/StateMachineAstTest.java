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
import ssot_parser.antlr4.SSOTLexer;
import ssot_parser.antlr4.SSOTParser;
import ssot_parser.ast.AstBuilderVisitor;
import ssot_parser.ast.nodes.RootNode;
import ssot_parser.ast.nodes.StateMachineNode;
import ssot_parser.ast.nodes.StateNode;
import ssot_parser.ast.nodes.TransitionNode;
import ssot_parser.ast.nodes.InvokeStateNode;
import ssot_parser.ast.nodes.HistoryNode;
import ssot_parser.ast.nodes.type.StateType;

/**
 * Tests for parsing state machine definitions and building the corresponding AST.
 */
public class StateMachineAstTest {

    // Helper method to parse input string and build AST
    private RootNode parse(String dslContent) {
        var charStream = CharStreams.fromString(dslContent);
        var lexer = new SSOTLexer(charStream);
        var tokenStream = new CommonTokenStream(lexer);
        var parser = new SSOTParser(tokenStream);
        var tree = parser.ssot(); // parse the content

        var visitor = new AstBuilderVisitor();
        return (RootNode) visitor.visit(tree);
    }

    // --- Test Cases --- TODO: Add test cases below

    @Test
    void testSimpleStateMachine() {
        String dsl = """
        statemachine MyMachine {
            initial state S1;
            state S1 {
                on E1 goto S2;
            }
            state S2 {
                on E2 goto S1;
            }
        }
        """;
        RootNode root = parse(dsl);
        assertNotNull(root);
        assertEquals(1, root.stateMachineNodes.size());

        StateMachineNode sm = root.stateMachineNodes.get(0);
        assertEquals("MyMachine", sm.stateMachineName);
        assertEquals("S1", sm.initialStateName);
        assertEquals(2, sm.states.size());

        StateNode s1 = sm.states.get(0);
        assertEquals("S1", s1.stateName);
        assertEquals(1, s1.transitions.size());
        assertEquals("E1", s1.transitions.get(0).event);
        assertEquals("S2", s1.transitions.get(0).targetStateName);

        StateNode s2 = sm.states.get(1);
        assertEquals("S2", s2.stateName);
        assertEquals(1, s2.transitions.size());
        assertEquals("E2", s2.transitions.get(0).event);
        assertEquals("S1", s2.transitions.get(0).targetStateName);
    }

    @Test
    void testInvokeState() {
         String dsl = """
        statemachine Invoker {
            initial state Idle;
            state Idle {
                invoke ChildMachine {
                    onDone goto Done;
                    onError goto Error;
                }
            }
            state Done {}
            state Error {}
        }
        """;
        RootNode root = parse(dsl);
        assertNotNull(root);
        StateMachineNode sm = root.stateMachineNodes.get(0);
        assertEquals("Invoker", sm.stateMachineName);
        assertEquals(3, sm.states.size()); // Idle, Done, Error

        StateNode idleState = sm.states.stream().filter(s -> "Idle".equals(s.stateName)).findFirst().orElse(null);
        assertNotNull(idleState);
        assertTrue(idleState.invoke.isPresent());

        InvokeStateNode invokeNode = idleState.invoke.get();
        assertEquals("ChildMachine", invokeNode.invokedStateMachineName);
        assertEquals(2, invokeNode.transitions.size());

        TransitionNode onDone = invokeNode.transitions.stream().filter(t -> "onDone".equals(t.event)).findFirst().orElse(null);
        assertNotNull(onDone);
        assertEquals("Done", onDone.targetStateName);

        TransitionNode onError = invokeNode.transitions.stream().filter(t -> "onError".equals(t.event)).findFirst().orElse(null);
        assertNotNull(onError);
        assertEquals("Error", onError.targetStateName);
    }

    @Test
    void testHistoryStateShallow() {
         String dsl = """
        statemachine HistoryTest {
            initial state Parent;
            state Parent {
                initial state Child1;
                history H;
                state Child1 {
                    on EV_CHILD_GOTO_2 goto Child2;
                }
                state Child2 {
                     on EV_CHILD_GOTO_1 goto Child1;
                }
                on EV_PARENT_LEAVE goto FinalState;
            }
            state FinalState {}
            on EV_PARENT_ENTER goto Parent.H; // Transition to shallow history
        }
        """;
        RootNode root = parse(dsl);
        assertNotNull(root);
        StateMachineNode sm = root.stateMachineNodes.get(0);
        assertEquals("HistoryTest", sm.stateMachineName);
        assertEquals(1, sm.transitions.size()); // Top level transition

        StateNode parentState = sm.states.stream().filter(s -> "Parent".equals(s.stateName)).findFirst().orElse(null);
        assertNotNull(parentState);
        assertEquals(StateType.COMPOUND, parentState.type);
        assertEquals("Child1", parentState.initialStateName);
        assertEquals(1, parentState.transitions.size()); // Parent's own transition
        assertEquals(2, parentState.states.size()); // Child1, Child2

        assertTrue(parentState.history.isPresent());
        HistoryNode historyNode = parentState.history.get();
        assertEquals("H", historyNode.historyStateName);
        assertFalse(historyNode.isDeep); // Shallow history

        // Check top-level transition targeting history
        TransitionNode toHistory = sm.transitions.get(0);
        assertEquals("EV_PARENT_ENTER", toHistory.event);
        assertEquals("Parent.H", toHistory.targetStateName); // Target includes history state name
    }

     @Test
    void testHistoryStateDeep() {
         String dsl = """
        statemachine DeepHistoryTest {
            initial state Parent;
            state Parent {
                initial state Child1;
                history* HDeep; // Deep history indicated by *
                state Child1 {
                    initial state GrandChild1;
                    state GrandChild1 {
                        on GOTO_GC2 goto GrandChild2;
                    }
                    state GrandChild2 {}
                    on GOTO_CHILD2 goto Child2;
                }
                state Child2 {}
                on PARENT_LEAVE goto FinalState;
            }
            state FinalState {}
            on PARENT_ENTER goto Parent.HDeep; // Transition to deep history
        }
        """;
        RootNode root = parse(dsl);
        assertNotNull(root);
        StateMachineNode sm = root.stateMachineNodes.get(0);

        StateNode parentState = sm.states.stream().filter(s -> "Parent".equals(s.stateName)).findFirst().orElse(null);
        assertNotNull(parentState);
        assertTrue(parentState.history.isPresent());
        HistoryNode historyNode = parentState.history.get();
        assertEquals("HDeep", historyNode.historyStateName);
        assertTrue(historyNode.isDeep); // Deep history

        // Check top-level transition targeting history
        TransitionNode toHistory = sm.transitions.get(0);
        assertEquals("PARENT_ENTER", toHistory.event);
        assertEquals("Parent.HDeep", toHistory.targetStateName);
    }

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
        RootNode root = parse(input);

        assertEquals(1, root.stateMachineNodes.size(), "Should have one state machine definition");
        assertTrue(root.stateMachineNodes.get(0) instanceof StateMachineNode, "Definition should be a StateMachineNode");

        StateMachineNode machine = (StateMachineNode) root.stateMachineNodes.get(0);
        assertEquals("MyMachine", machine.stateMachineName, "State machine name should be correct");
        assertEquals(1, machine.states.size(), "State machine should have one state");
        assertTrue(machine.states.get(0) instanceof StateNode, "State definition should be a StateNode");

        StateNode stateA = (StateNode) machine.states.get(0);
        assertEquals("StateA", stateA.stateName, "State name should be correct");
        assertTrue(stateA.transitions.isEmpty(), "State should have no transitions initially");
        assertTrue(stateA.invoke.isEmpty(), "State should have no invokes initially");
        assertTrue(stateA.nestedStates.isEmpty(), "State should have no nested states initially");

        // Default initial state should be the first one if not specified
        assertEquals(Optional.of("StateA"), machine.initialStateName, "Default initial state should be StateA");
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
        RootNode root = parse(input);
        assertEquals(1, root.stateMachineNodes.size());
        StateMachineNode machine = (StateMachineNode) root.stateMachineNodes.get(0);
        assertEquals(2, machine.states.size());

        StateNode stateA = machine.states.stream().filter(s -> s.stateName.equals("StateA")).findFirst().orElse(null);
        StateNode stateB = machine.states.stream().filter(s -> s.stateName.equals("StateB")).findFirst().orElse(null);
        assertNotNull(stateA, "StateA should exist");
        assertNotNull(stateB, "StateB should exist");

        assertEquals(1, stateA.transitions.size(), "StateA should have one transition");
        assertTrue(stateB.transitions.isEmpty(), "StateB should have no transitions");

        TransitionNode transition = stateA.transitions.get(0);
        assertEquals("GoToB", transition.event, "Transition event name should be correct");
        assertEquals("StateA", transition.sourceStateName, "Transition source should be correct");
        assertEquals("StateB", transition.targetStateName, "Transition target should be correct");
        assertEquals(Optional.empty(), transition.condition, "Transition should have no guard initially");
        assertEquals(Optional.empty(), transition.action, "Transition should have no action initially");
        assertTrue(transition.annotations.isEmpty(), "Transition should have no annotations initially");

        // Check StateMachineNode's transition list (collected from states)
        assertEquals(1, machine.transitions.size(), "StateMachineNode should contain the collected transition");
        assertSame(transition, machine.transitions.get(0), "StateMachineNode's transition should be the same object");

        assertEquals(Optional.of("StateA"), machine.initialStateName, "Default initial state should still be StateA");
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
        RootNode root = parse(input);
        assertEquals(1, root.stateMachineNodes.size());
        StateMachineNode machine = (StateMachineNode) root.stateMachineNodes.get(0);
        assertEquals(2, machine.states.size(), "Should have 2 top-level states");

        StateNode parent = machine.states.stream().filter(s -> s.stateName.equals("Parent")).findFirst().orElse(null);
        StateNode sibling = machine.states.stream().filter(s -> s.stateName.equals("Sibling")).findFirst().orElse(null);
        assertNotNull(parent);
        assertNotNull(sibling);

        // Test Parent state history
        assertTrue(parent.history.isPresent(), "Parent state should have history");
        assertEquals(HistoryNode.HistoryType.SHALLOW, parent.history.get().historyType, "Parent history should be shallow");
        assertTrue(parent.history.get().annotations.isEmpty(), "Parent history should have no annotations");
        assertEquals(2, parent.nestedStates.size(), "Parent should have 2 nested states");

        // Test Sibling state history
        assertTrue(sibling.history.isPresent(), "Sibling state should have history");
        assertEquals(HistoryNode.HistoryType.DEEP, sibling.history.get().historyType, "Sibling history should be deep");
        assertEquals(Optional.of(123L), sibling.history.get().id, "Sibling history should have @id");
        assertEquals(1, sibling.history.get().annotations.size(), "Sibling history should have one annotation ($description)");
        assertEquals("description", sibling.history.get().annotations.get(0).name);

        // Test nested history in Sibling
        assertEquals(1, sibling.nestedStates.size(), "Sibling should have 1 nested state");
        StateNode grandChild = sibling.nestedStates.get(0);
        assertEquals("GrandChild", grandChild.stateName);
        assertTrue(grandChild.history.isPresent(), "GrandChild should have history");
        assertEquals(HistoryNode.HistoryType.SHALLOW, grandChild.history.get().historyType, "GrandChild history should default to shallow");
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
        RootNode root = parse(input);
        assertEquals(1, root.stateMachineNodes.size());
        StateMachineNode machine = (StateMachineNode) root.stateMachineNodes.get(0);
        assertEquals(4, machine.states.size());

        StateNode processing = machine.states.stream().filter(s -> s.stateName.equals("Processing")).findFirst().orElse(null);
        assertNotNull(processing);
        assertEquals(1, processing.invoke.size(), "Processing state should have one invoke");

        InvokeStateNode invoke = processing.invoke.get(0);
        assertEquals("MyService", invoke.invokedStateMachineName, "Invoke source should be MyService");
        assertEquals(Optional.of(555L), invoke.id, "Invoke should have @id"); // Check ID on invoke itself if grammar allows
        // Check annotations directly on invoke if grammar supports it
        assertEquals(1, invoke.annotations.size());
        assertEquals("description", invoke.annotations.get(0).name);

        assertEquals(Optional.of("Success"), invoke.onDoneTarget, "onDone target should be Success");
        assertEquals(Optional.of("Failure"), invoke.onErrorTarget, "onError target should be Failure");
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
        RootNode root = parse(input);
        assertEquals(1, root.stateMachineNodes.size());
        StateMachineNode machine = (StateMachineNode) root.stateMachineNodes.get(0);
        StateNode invoking = machine.states.stream().filter(s -> s.stateName.equals("Invoking")).map(s -> (StateNode)s).findFirst().orElse(null);
        assertNotNull(invoking);
        assertEquals(1, invoking.invoke.size());

        InvokeStateNode invokeNode = invoking.invoke.get(0);
        assertEquals("DataFetcher", invokeNode.invokedStateMachineName);
        assertTrue(invokeNode.annotations.isEmpty());
        // assertEquals(Optional.empty(), invokeNode.getOnDoneTarget()); // Old assertion
        // assertEquals(Optional.empty(), invokeNode.getOnErrorTarget()); // Old assertion
        assertTrue(invokeNode.onDoneTransition.isEmpty(), "onDone should be empty");
        assertTrue(invokeNode.onErrorTransition.isEmpty(), "onError should be empty");
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
        RootNode root = parse(input);
        assertEquals(1, root.stateMachineNodes.size());
        StateMachineNode machine = (StateMachineNode) root.stateMachineNodes.get(0);
        StateNode invoking = machine.states.stream().filter(s -> s.stateName.equals("Invoking")).map(s -> (StateNode)s).findFirst().orElse(null);
        assertNotNull(invoking);
        assertEquals(1, invoking.invoke.size());

        InvokeStateNode invokeNode = invoking.invoke.get(0);
        assertEquals("DataFetcher", invokeNode.invokedStateMachineName);

        // Check onDone
        assertTrue(invokeNode.onDoneTransition.isPresent(), "onDone should be present");
        InvokeStateNode.InvokeTransition onDone = invokeNode.onDoneTransition.get();
        assertEquals(Optional.of("Success"), onDone.target(), "onDone target should be Success");
        assertTrue(onDone.action().isEmpty(), "onDone should have no action");
        assertTrue(onDone.guard().isEmpty(), "onDone should have no guard");
        assertTrue(onDone.annotations().isEmpty(), "onDone should have no annotations");

        // Check onError
        assertTrue(invokeNode.onErrorTransition.isPresent(), "onError should be present");
        InvokeStateNode.InvokeTransition onError = invokeNode.onErrorTransition.get();
        assertEquals(Optional.of("Failure"), onError.target(), "onError target should be Failure");
        assertEquals(Optional.of("LogError"), onError.action(), "onError should have action LogError");
        assertEquals(Optional.of("CanRetry"), onError.guard(), "onError should have guard CanRetry");
        assertTrue(onError.annotations().isEmpty(), "onError should have no annotations");
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
        RootNode root = parse(input);
        assertEquals(1, root.stateMachineNodes.size());
        StateMachineNode machine = (StateMachineNode) root.stateMachineNodes.get(0);
        assertEquals("TrafficLight", machine.stateMachineName);
        assertEquals(Optional.of("Red"), machine.initialStateName);
        assertEquals(4, machine.states.size()); // Red, Green, Yellow, Off

        StateNode offState = machine.states.stream().filter(s -> s.stateName.equals("Off")).map(s->(StateNode)s).findFirst().orElse(null);
        assertNotNull(offState);
        assertEquals(1, offState.transitions.size()); // PowerOff transition
        assertEquals("Solid", offState.transitions.get(0).targetStateName);
        assertTrue(offState.history.isPresent());
        assertEquals(HistoryNode.HistoryType.SHALLOW, offState.history.get().historyType);

        assertEquals(2, offState.nestedStates.size()); // Solid, Flashing
        StateNode solidState = offState.nestedStates.stream().filter(s -> s.stateName.equals("Solid")).map(s->(StateNode)s).findFirst().orElse(null);
        StateNode flashingState = offState.nestedStates.stream().filter(s -> s.stateName.equals("Flashing")).map(s->(StateNode)s).findFirst().orElse(null);
        assertNotNull(solidState);
        assertNotNull(flashingState);

        assertEquals(1, solidState.transitions.size());
        assertEquals("Red", solidState.transitions.get(0).targetStateName);
        assertEquals("PowerOn", solidState.transitions.get(0).event);

        // Note: Nested states might have their own initial state marker within the grammar
        // The validator might need to check this. Here we just check parsing.
    }

    // TODO: Add tests for context, actions, guards, initial state markers, etc.
    // TODO: Add tests for nested states and history states
} 