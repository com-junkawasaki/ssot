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
import ssot_parser.ast.nodes.state.HistoryStateNode;

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
            OuterMachine {
                initial state OuterState1;
                states {
                    OuterState1 {
                        initial state InnerStateA;
                        on EventX target OuterState2;
                        states {
                            InnerStateA {
                                on InnerEvent1 target InnerStateB;
                            }
                            InnerStateB {
                                on InnerEvent2 target InnerStateA;
                            }
                        }
                    }
                    OuterState2 {}
                }
            }
        }
        """;
        RootNode root = parse(input);
        assertNotNull(root.getMachineNodes());
        assertEquals(1, root.getMachineNodes().size());
        StateMachineNode machine = root.getMachineNodes().get(0);

        assertEquals("OuterMachine", machine.getMachineName());
        assertEquals("OuterState1", machine.getInitialStateName());
        assertEquals(2, machine.getStates().size());

        StateNode outerS1 = (StateNode) machine.getStates().stream()
                                .filter(s -> ((StateNode)s).getStateName().equals("OuterState1"))
                                .findFirst().orElse(null);
        assertNotNull(outerS1);
        assertEquals(StateType.COMPOUND, outerS1.getType());
        assertEquals(Optional.of("InnerStateA"), outerS1.getInitialStateName());
        assertEquals(1, outerS1.getEventHandlers().size()); // EventX
        assertEquals(2, outerS1.getNestedStates().size()); // InnerStateA, InnerStateB

        StateNode innerSA = outerS1.getNestedStates().stream()
                                .filter(s -> s.getStateName().equals("InnerStateA"))
                                .findFirst().orElse(null);
        assertNotNull(innerSA);
        assertEquals(1, innerSA.getEventHandlers().size());
        assertEquals("InnerEvent1", innerSA.getEventHandlers().get(0).getEventName());
        assertEquals("InnerStateB", innerSA.getEventHandlers().get(0).getTargetStateName().get());


        StateNode outerS2 = (StateNode) machine.getStates().stream()
                                .filter(s -> ((StateNode)s).getStateName().equals("OuterState2"))
                                .findFirst().orElse(null);
        assertNotNull(outerS2);
        assertEquals(StateType.ATOMIC, outerS2.getType());
    }

    @Test
    void testStateWithEntryExitActions() throws Exception {
        String input = """
        machines {
            ActionMachine {
                actions {
                    entryAction1 {}
                    exitAction1 {}
                    anotherAction {}
                }
                states {
                    StateWithActions {
                        entry / entryAction1, anotherAction;
                        exit / exitAction1;
                        on EventGo target NextState;
                    }
                    NextState {}
                }
            }
        }
        """;
        RootNode root = parse(input);
        assertNotNull(root.getMachineNodes());
        assertEquals(1, root.getMachineNodes().size());
        StateMachineNode machine = root.getMachineNodes().get(0);
        assertNotNull(machine);

        StateNode stateWithActions = (StateNode) machine.getStates().stream()
            .filter(s -> ((StateNode)s).getStateName().equals("StateWithActions"))
            .findFirst().orElse(null);

        assertNotNull(stateWithActions);
        assertEquals(2, stateWithActions.getEntryActions().size());
        assertTrue(stateWithActions.getEntryActions().contains("entryAction1"));
        assertTrue(stateWithActions.getEntryActions().contains("anotherAction"));

        assertEquals(1, stateWithActions.getExitActions().size());
        assertTrue(stateWithActions.getExitActions().contains("exitAction1"));

        assertEquals(1, stateWithActions.getEventHandlers().size());
        assertEquals("EventGo", stateWithActions.getEventHandlers().get(0).getEventName());
    }

    @Test
    void testFinalState() throws Exception {
        String input = """
        machines {
            FinalStateTestMachine {
                initial state Active;
                states {
                    Active {
                        on FINISH target Done;
                    }
                    Done {
                        type final;
                    }
                }
            }
        }
        """;
        RootNode root = parse(input);
        assertNotNull(root.getMachineNodes());
        assertEquals(1, root.getMachineNodes().size());
        StateMachineNode machine = root.getMachineNodes().get(0);
        assertNotNull(machine);

        StateNode doneState = (StateNode) machine.getStates().stream()
            .filter(s -> ((StateNode)s).getStateName().equals("Done"))
            .findFirst().orElse(null);

        assertNotNull(doneState);
        assertEquals(StateType.FINAL, doneState.getType());
        assertTrue(doneState.getEntryActions().isEmpty());
        assertTrue(doneState.getExitActions().isEmpty());
        assertTrue(doneState.getEventHandlers().isEmpty());
        assertTrue(doneState.getNestedStates().isEmpty());
    }

    @Test
    void testConditionalTransitions() throws Exception {
        String input = """
        machines {
            ConditionalMachine {
                guards {
                    isReady { /* guard logic */ }
                    isValid { /* guard logic */ }
                }
                actions {
                    doSomething { /* action logic */ }
                    doElse { /* action logic */ }
                }
                states {
                    Checker {
                        if (isReady) target ReadyState do / doSomething;
                        else if (isValid) target ValidState;
                        else target ErrorState do / doElse;
                    }
                    ReadyState {}
                    ValidState {}
                    ErrorState {}
                }
            }
        }
        """;
        SsotRoot root = (SsotRoot) parse(input);
        assertNotNull(root.getMachineNodes());
        MachineNode machine = root.getMachineNodes().get(0);
        assertNotNull(machine);

        StateNode checkerState = (StateNode) machine.getStates().stream()
            .filter(s -> ((StateNode)s).getStateName().equals("Checker"))
            .findFirst().orElse(null);
        assertNotNull(checkerState);

        assertTrue(checkerState.getEventHandlers().isEmpty(), "Checker state should have no regular event handlers");
        assertEquals(3, checkerState.getIfTransitions().size(), "Checker state should have 3 conditional transitions");

        TransitionNode ifTran = checkerState.getIfTransitions().get(0);
        assertEquals(Optional.of("isReady"), ifTran.getCondition(), "Condition for first if should be 'isReady'");
        assertEquals("ReadyState", ifTran.getTargetStateName().orElse(null), "Target for first if should be 'ReadyState'");
        assertEquals(1, ifTran.getActions().size(), "Action list size for first if");
        assertEquals("doSomething", ifTran.getActions().get(0), "Action for first if should be 'doSomething'");

        TransitionNode elseIfTran = checkerState.getIfTransitions().get(1);
        assertEquals(Optional.of("isValid"), elseIfTran.getCondition(), "Condition for elseif should be 'isValid'");
        assertEquals("ValidState", elseIfTran.getTargetStateName().orElse(null), "Target for elseif should be 'ValidState'");
        assertTrue(elseIfTran.getActions().isEmpty(), "Elseif transition should have no actions");

        TransitionNode elseTran = checkerState.getIfTransitions().get(2);
        assertTrue(elseTran.getCondition().isEmpty(), "Condition for else should be empty (implicit true)");
        assertEquals("ErrorState", elseTran.getTargetStateName().orElse(null), "Target for else should be 'ErrorState'");
        assertEquals(1, elseTran.getActions().size(), "Action list size for else");
        assertEquals("doElse", elseTran.getActions().get(0), "Action for else should be 'doElse'");
    }

    @Test
    void testMultipleInvokesInState() throws Exception {
        String input = """
        machines {
            MultiInvokeSM {
                // Invokes must be defined in an 'invokes' block if they are to be referenced by name.
                // For this test, we assume 'ServiceA' and 'ServiceB' are valid invoke definition names.
                invokes {
                    ServiceA { src "some.service.A"; }
                    ServiceB { src "another.service.B"; }
                }
                states {
                    BossState {
                        invoke ServiceA {
                            id "invokeA_001";
                            onDone target NextFromA;
                        }
                        invoke ServiceB {
                            id "invokeB_002";
                            onDone target NextFromB;
                            onError target ErrorHandlerB;
                        }
                    }
                    NextFromA {}
                    NextFromB {}
                    ErrorHandlerB {}
                }
            }
        }
        """;
        SsotRoot root = (SsotRoot) parse(input);
        assertNotNull(root.getMachineNodes());
        MachineNode machine = root.getMachineNodes().get(0);
        assertNotNull(machine);

        StateNode bossState = (StateNode) machine.getStates().stream()
            .filter(s -> ((StateNode)s).getStateName().equals("BossState"))
            .findFirst().orElse(null);
        assertNotNull(bossState);

        assertEquals(2, bossState.getInvokeInvocations().size(), "BossState should have 2 invoke invocations");

        InvokeStateNode invokeA = bossState.getInvokeInvocations().get(0);
        assertEquals("ServiceA", invokeA.getInvokeDefinitionRef());
        assertEquals(Optional.of("invokeA_001"), invokeA.getStrId()); // Assuming getStrId for string id from @id("string")
        assertEquals(1, invokeA.getCompletionHandlers().size());
        assertEquals("onDone", invokeA.getCompletionHandlers().get(0).getTransitionMatcher());
        assertEquals("NextFromA", invokeA.getCompletionHandlers().get(0).getTransition().getTargetStateName().orElse(null));

        InvokeStateNode invokeB = bossState.getInvokeInvocations().get(1);
        assertEquals("ServiceB", invokeB.getInvokeDefinitionRef());
        assertEquals(Optional.of("invokeB_002"), invokeB.getStrId());
        assertEquals(2, invokeB.getCompletionHandlers().size());

        Optional<InvokeCompletionHandler> onDoneBHandler = invokeB.getCompletionHandlers().stream()
            .filter(h -> "onDone".equals(h.getTransitionMatcher()))
            .findFirst();
        assertTrue(onDoneBHandler.isPresent());
        assertEquals("NextFromB", onDoneBHandler.get().getTransition().getTargetStateName().orElse(null));

        Optional<InvokeCompletionHandler> onErrorBHandler = invokeB.getCompletionHandlers().stream()
            .filter(h -> "onError".equals(h.getTransitionMatcher()))
            .findFirst();
        assertTrue(onErrorBHandler.isPresent());
        assertEquals("ErrorHandlerB", onErrorBHandler.get().getTransition().getTargetStateName().orElse(null));
    }

    @Test
    void testParallelState() throws Exception {
        String input = """
        machines {
            ParallelSM {
                states {
                    P1 {
                        type parallel;
                        // Region 1
                        RegionA {
                            initial state R1S1;
                            states {
                                R1S1 { on E_R1 target R1S2; }
                                R1S2 { type final; }
                            }
                        }
                        // Region 2
                        RegionB {
                            initial state R2S1;
                            states {
                                R2S1 { on E_R2 target R2S2; }
                                R2S2 { type final; }
                            }
                        }
                        on GLOBAL_EV target P2; // Event for the parallel state itself
                    }
                    P2 {}
                }
            }
        }
        """;
        SsotRoot root = (SsotRoot) parse(input);
        assertNotNull(root.getMachineNodes());
        MachineNode machine = root.getMachineNodes().get(0);
        assertNotNull(machine);

        StateNode p1State = (StateNode) machine.getStates().stream()
            .filter(s -> ((StateNode)s).getStateName().equals("P1"))
            .findFirst().orElse(null);
        assertNotNull(p1State);

        assertEquals(StateType.PARALLEL, p1State.getType(), "P1 should be a parallel state");
        assertEquals(1, p1State.getEventHandlers().size(), "P1 should have one event handler for GLOBAL_EV");
        assertEquals("GLOBAL_EV", p1State.getEventHandlers().get(0).getEventName());
        assertEquals("P2", p1State.getEventHandlers().get(0).getTargetStateName().orElse(null));

        assertEquals(2, p1State.getNestedStates().size(), "P1 should have two nested states (regions)");

        StateNode regionA = p1State.getNestedStates().stream()
            .filter(s -> s.getStateName().equals("RegionA"))
            .findFirst().orElse(null);
        assertNotNull(regionA, "RegionA should exist");
        assertEquals(StateType.COMPOUND, regionA.getType(), "RegionA should be a compound state");
        assertEquals(Optional.of("R1S1"), regionA.getInitialStateName(), "RegionA initial state should be R1S1");
        assertEquals(2, regionA.getNestedStates().size(), "RegionA should have 2 states");

        StateNode regionB = p1State.getNestedStates().stream()
            .filter(s -> s.getStateName().equals("RegionB"))
            .findFirst().orElse(null);
        assertNotNull(regionB, "RegionB should exist");
        assertEquals(StateType.COMPOUND, regionB.getType(), "RegionB should be a compound state");
        assertEquals(Optional.of("R2S1"), regionB.getInitialStateName(), "RegionB initial state should be R2S1");
        assertEquals(2, regionB.getNestedStates().size(), "RegionB should have 2 states");
    }

    @Test
    void testStateWithAnnotations() throws Exception {
        String input = """
        machines {
            AnnotatedSM {
                states {
                    S1 {
                        @id(12345);
                        $name("My First State");
                        @customTag("important_state");
                        on EVENT target S2;
                    }
                    S2 {}
                }
            }
        }
        """;
        SsotRoot root = (SsotRoot) parse(input);
        assertNotNull(root.getMachineNodes());
        MachineNode machine = root.getMachineNodes().get(0);
        assertNotNull(machine);

        StateNode s1 = (StateNode) machine.getStates().stream()
            .filter(s -> ((StateNode)s).getStateName().equals("S1"))
            .findFirst().orElse(null);
        assertNotNull(s1);

        assertEquals(Optional.of(12345L), s1.getId(), "S1 should have numeric ID 12345");
        assertEquals("My First State", s1.getDisplayName(), "S1 display name should be 'My First State'");
        assertEquals(3, s1.getAnnotationNodes().size(), "S1 should have 3 annotation nodes");

        Optional<AnnotationNode> customTagAnnotation = s1.getAnnotationNodes().stream()
            .filter(a -> "@customTag".equals(a.getName()))
            .findFirst();
        assertTrue(customTagAnnotation.isPresent(), "S1 should have @customTag annotation");
        assertEquals(Optional.of("important_state"), customTagAnnotation.get().getValue(), "@customTag value should be 'important_state'");
    }

    @Test
    void testTransitionWithGuardAndAction() throws Exception {
        String input = """
        machines {
            TransitionDetailsSM {
                guards {
                    canProceed { /* guard logic */ }
                }
                actions {
                    logTransition { /* action logic */ }
                    anotherAction { /* action logic */ }
                }
                states {
                    A {
                        on EV1 [canProceed] target B do / logTransition, anotherAction;
                    }
                    B {}
                }
            }
        }
        """;
        SsotRoot root = (SsotRoot) parse(input);
        assertNotNull(root.getMachineNodes());
        MachineNode machine = root.getMachineNodes().get(0);
        assertNotNull(machine);

        StateNode stateA = (StateNode) machine.getStates().stream()
            .filter(s -> ((StateNode)s).getStateName().equals("A"))
            .findFirst().orElse(null);
        assertNotNull(stateA);
        assertEquals(1, stateA.getEventHandlers().size());

        TransitionNode transition = stateA.getEventHandlers().get(0);
        assertEquals("EV1", transition.getEventName());
        assertEquals(Optional.of("canProceed"), transition.getCondition(), "Transition guard should be 'canProceed'");
        assertEquals("B", transition.getTargetStateName().orElse(null));
        assertEquals(2, transition.getActions().size(), "Transition should have 2 actions");
        assertTrue(transition.getActions().contains("logTransition"));
        assertTrue(transition.getActions().contains("anotherAction"));
    }

    @Test
    void testMachineWithMixedStatesAndHistory() throws Exception {
        String input = """
        machines {
            MixedStatesSM {
                initial state NormalState;
                states {
                    NormalState {
                        on GOTO_HIST target TopHistory;
                    }
                    // Assuming HistoryStateNode can be a direct child in states block
                    history TopHistory {
                        $name("MainHistory");
                        @id(789);
                        default target NormalState;
                    } 
                    AnotherState {}
                }
            }
        }
        """;
        SsotRoot root = (SsotRoot) parse(input);
        assertNotNull(root.getMachineNodes());
        MachineNode machine = root.getMachineNodes().get(0);
        assertNotNull(machine);
        assertEquals("MixedStatesSM", machine.getMachineName());
        assertEquals(3, machine.getStates().size(), "Machine should have 3 top-level entries in states list (2 states, 1 history)");

        StateNode normalState = null;
        HistoryStateNode topHistory = null;
        StateNode anotherState = null;

        for (AstNode node : machine.getStates()) {
            if (node instanceof StateNode) {
                StateNode sn = (StateNode) node;
                if ("NormalState".equals(sn.getStateName())) normalState = sn;
                else if ("AnotherState".equals(sn.getStateName())) anotherState = sn;
            } else if (node instanceof HistoryStateNode) {
                HistoryStateNode hn = (HistoryStateNode) node;
                if ("TopHistory".equals(hn.getStateName())) topHistory = hn;
            }
        }

        assertNotNull(normalState, "NormalState should be found");
        assertNotNull(topHistory, "TopHistory should be found");
        assertNotNull(anotherState, "AnotherState should be found");

        assertEquals("NormalState", normalState.getStateName());
        assertEquals(1, normalState.getEventHandlers().size());

        assertEquals("TopHistory", topHistory.getStateName());
        assertEquals("MainHistory", topHistory.getDisplayName());
        assertEquals(Optional.of(789L), topHistory.getId());
        assertTrue(topHistory.getDefaultTransition().isPresent(), "TopHistory should have a default transition");
        assertEquals("NormalState", topHistory.getDefaultTransition().get().getTargetStateName().orElse(null) );
        assertFalse(topHistory.isDeep(), "TopHistory should be shallow by default");

        assertEquals("AnotherState", anotherState.getStateName());
    }

    @Test
    void testCompoundStateWithMultipleHistoryStates() throws Exception {
        String input = """
        machines {
            MultiHistorySM {
                states {
                    ParentState {
                        type compound;
                        initial state Child1;
                        history HShallow; // Shallow history
                        history* HDeep {
                           @id(1122);
                           $name("DeepMainHistory");
                           default target Child2; // Default transition for this history state
                        } 

                        states {
                            Child1 { on EV_C1_C2 target Child2; }
                            Child2 { on EV_C2_C1 target Child1; }
                        }
                        on EV_P_EXIT target AnotherTopState;
                    }
                    AnotherTopState {}
                }
            }
        }
        """;
        SsotRoot root = (SsotRoot) parse(input);
        assertNotNull(root.getMachineNodes());
        MachineNode machine = root.getMachineNodes().get(0);
        assertNotNull(machine);

        StateNode parentState = (StateNode) machine.getStates().stream()
            .filter(s -> ((StateNode)s).getStateName().equals("ParentState"))
            .findFirst().orElse(null);
        assertNotNull(parentState);

        assertEquals(StateType.COMPOUND, parentState.getType());
        assertEquals(2, parentState.getHistoryStates().size(), "ParentState should have 2 history states");

        HistoryStateNode hShallow = parentState.getHistoryStates().stream()
            .filter(h -> "HShallow".equals(h.getStateName()))
            .findFirst().orElse(null);
        assertNotNull(hShallow, "HShallow history state should exist");
        assertFalse(hShallow.isDeep(), "HShallow should be a shallow history");
        assertTrue(hShallow.getAnnotations().isEmpty(), "HShallow should have no annotations initially");
        assertTrue(hShallow.getDefaultTransition().isEmpty(), "HShallow should have no default transition initially");

        HistoryStateNode hDeep = parentState.getHistoryStates().stream()
            .filter(h -> "HDeep".equals(h.getStateName()))
            .findFirst().orElse(null);
        assertNotNull(hDeep, "HDeep history state should exist");
        assertTrue(hDeep.isDeep(), "HDeep should be a deep history");
        assertEquals(Optional.of(1122L), hDeep.getId());
        assertEquals("DeepMainHistory", hDeep.getDisplayName());
        assertTrue(hDeep.getDefaultTransition().isPresent(), "HDeep should have a default transition");
        assertEquals("Child2", hDeep.getDefaultTransition().get().getTargetStateName().orElse(null));

        assertEquals(2, parentState.getNestedStates().size(), "ParentState should have 2 child states");
        assertEquals(1, parentState.getEventHandlers().size(), "ParentState should have 1 event handler");
    }

    @Test
    void testComplexNestingWithDeepHistoryAndCrossLevelTransition() throws Exception {
        String input = """
        machines {
            ComplexSM {
                initial state SuperParent;
                states {
                    SuperParent {
                        type compound;
                        initial state Parent1;
                        history* HSuperDeep;

                        states {
                            Parent1 {
                                type compound;
                                initial state ChildA;
                                history* HParent1Deep;
                                states {
                                    ChildA { on EV_A_B target ChildB; }
                                    ChildB { on EV_B_A target ChildA; on EV_B_EXIT_ALL target ExitState; }
                                }
                                on EV_P1_P2 target Parent2;
                            }
                            Parent2 {
                                type compound;
                                initial state ChildC;
                                states {
                                    ChildC { on EV_C_D target ChildD; }
                                    ChildD { on EV_D_C target ChildC; }
                                }
                                on EV_P2_P1_HDEEP target Parent1.HParent1Deep; // Transition to nested history
                            }
                        }
                        on EV_SP_EXIT_ALL target ExitState;
                        on EV_SP_REENTER_DEEP target SuperParent.HSuperDeep; // Transition to own deep history
                    }
                    ExitState { type final; }
                }
            }
        }
        """;
        SsotRoot root = (SsotRoot) parse(input);
        assertNotNull(root.getMachineNodes());
        MachineNode machine = root.getMachineNodes().get(0);
        assertNotNull(machine, "Machine node should not be null");

        StateNode superParent = (StateNode) machine.getStates().stream()
            .filter(s -> ((StateNode)s).getStateName().equals("SuperParent"))
            .findFirst().orElse(null);
        assertNotNull(superParent, "SuperParent state should exist");
        assertEquals(StateType.COMPOUND, superParent.getType());
        assertEquals(Optional.of("Parent1"), superParent.getInitialStateName());
        assertEquals(1, superParent.getHistoryStates().size(), "SuperParent should have 1 history state (HSuperDeep)");
        assertTrue(superParent.getHistoryStates().get(0).isDeep());
        assertEquals(2, superParent.getNestedStates().size(), "SuperParent should have 2 nested states (Parent1, Parent2)");
        assertEquals(2, superParent.getEventHandlers().size(), "SuperParent should have 2 event handlers");

        StateNode parent1 = superParent.getNestedStates().stream()
            .filter(s -> s.getStateName().equals("Parent1"))
            .findFirst().orElse(null);
        assertNotNull(parent1, "Parent1 state should exist");
        assertEquals(StateType.COMPOUND, parent1.getType());
        assertEquals(Optional.of("ChildA"), parent1.getInitialStateName());
        assertEquals(1, parent1.getHistoryStates().size(), "Parent1 should have 1 history state (HParent1Deep)");
        assertTrue(parent1.getHistoryStates().get(0).isDeep());
        assertEquals(2, parent1.getNestedStates().size(), "Parent1 should have 2 nested states (ChildA, ChildB)");
        assertEquals(1, parent1.getEventHandlers().size(), "Parent1 should have 1 event handler (EV_P1_P2)");

        StateNode childB = parent1.getNestedStates().stream()
            .filter(s -> s.getStateName().equals("ChildB"))
            .findFirst().orElse(null);
        assertNotNull(childB, "ChildB state should exist");
        assertEquals(2, childB.getEventHandlers().size(), "ChildB should have 2 event handlers");
        Optional<TransitionNode> crossLevelTransition = childB.getEventHandlers().stream()
            .filter(t -> "EV_B_EXIT_ALL".equals(t.getEventName()) && "ExitState".equals(t.getTargetStateName().orElse(null)))
            .findFirst();
        assertTrue(crossLevelTransition.isPresent(), "ChildB should have a transition EV_B_EXIT_ALL to ExitState");

        StateNode parent2 = superParent.getNestedStates().stream()
            .filter(s -> s.getStateName().equals("Parent2"))
            .findFirst().orElse(null);
        assertNotNull(parent2, "Parent2 state should exist");
        assertEquals(1, parent2.getEventHandlers().size(), "Parent2 should have 1 event handler (EV_P2_P1_HDEEP)");
        TransitionNode transitionToNestedHistory = parent2.getEventHandlers().get(0);
        assertEquals("EV_P2_P1_HDEEP", transitionToNestedHistory.getEventName());
        assertEquals("Parent1.HParent1Deep", transitionToNestedHistory.getTargetStateName().orElse(null), "Transition should target Parent1.HParent1Deep");

        StateNode exitState = (StateNode) machine.getStates().stream()
            .filter(s -> ((StateNode)s).getStateName().equals("ExitState"))
            .findFirst().orElse(null);
        assertNotNull(exitState, "ExitState should exist");
        assertEquals(StateType.FINAL, exitState.getType());
    }

    // TODO: Add test cases for:
    // - More complex nested states with deep history and transitions crossing hierarchy levels. // Partially covered, can be expanded for more edge cases.
} 