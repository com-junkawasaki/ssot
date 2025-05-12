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
import java.util.stream.Collectors; // Needed for helper method

// Keep base parser/visitor imports
import ssot_parser.SSoTLexer;
import ssot_parser.SSoTParser;
import ssot_parser.AstBuilderVisitor;

// General AST node imports
import ssot_parser.ast.AstNode;
import ssot_parser.ast.SsotRoot;
import ssot_parser.ast.nodes.*; // Keep wildcard for MachineNode, StateNode etc.

// Specific type imports
import ssot_parser.ast.type.StateType;      // For NORMAL, COMPOUND etc.

// Specific state-related imports
import ssot_parser.ast.nodes.HistoryStateNode;   // Specific node import (assuming in nodes package)
import ssot_parser.ast.nodes.state.HistoryStateType; // For SHALLOW, DEEP enum

/**
 * Tests for parsing state machine definitions and building the corresponding AST.
 */
// Renamed class
public class MachineAstTest { 

    // Helper method similar to other test files
    private SsotRoot parseAndBuildAst(String inputString) throws Exception {
        InputStream is = new ByteArrayInputStream(inputString.getBytes(StandardCharsets.UTF_8));
        CharStream input = CharStreams.fromStream(is);
        SSoTLexer lexer = new SSoTLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        SSoTParser parser = new SSoTParser(tokens);

        parser.removeErrorListeners(); // Add error handling like other tests
        parser.addErrorListener(new BaseErrorListener() {
            @Override
            public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol,
                                    int line, int charPositionInLine, String msg, RecognitionException e) {
                throw new RuntimeException("Syntax error at line " + line + ":" + charPositionInLine + " " + msg, e);
            }
        });

        ParseTree tree = parser.file(); // Assuming 'file' is the root rule like other tests
        AstBuilderVisitor visitor = new AstBuilderVisitor();
        Object result = visitor.visit(tree);
        assertTrue(result instanceof SsotRoot, "AST building should return an SsotRoot");
        return (SsotRoot) result;
    }

    @Test
    void testMinimalMachine() throws Exception {
        String input = """
        machines {
            machine SimpleMachine {
                states {
                    initial state Idle;
                    state Idle {};
                }
            }
        }
        """;
        SsotRoot root = parseAndBuildAst(input);
        assertEquals(1, root.getMachineDefinitions().size());

        // Use MachineNode
        MachineNode sm = (MachineNode) root.getMachineDefinitions().get(0); 
        assertEquals("SimpleMachine", sm.getMachineName());
        assertEquals("Idle", sm.getInitialStateName());
        assertEquals(1, sm.getStates().size());
        // Find the state node within the list of AstNode
        StateNode idleState = findStateByName(sm.getStates(), "Idle");
        assertNotNull(idleState, "Idle state not found");
        assertEquals("Idle", idleState.getStateName());
        // Check type - assuming a simple state defined like this is NORMAL
        assertEquals(StateType.NORMAL, idleState.getType());
    }

    @Test
    void testMachineWithTransition() throws Exception {
        String input = """
        machines {
            machine TrafficLight {
                states {
                    initial state Red;
                    state Red {
                        on TIMER target Green;
                    };
                    state Green {
                        on TIMER target Yellow;
                    };
                    state Yellow {
                        on TIMER target Red;
                    };
                }
            }
        }
        """;
        SsotRoot root = parseAndBuildAst(input);
        assertEquals(1, root.getMachineDefinitions().size());
        // Use MachineNode
        MachineNode sm = (MachineNode) root.getMachineDefinitions().get(0); 
        assertEquals("TrafficLight", sm.getMachineName());
        assertEquals(3, sm.getStates().size());

        StateNode redState = findStateByName(sm.getStates(), "Red");
        assertNotNull(redState);
        assertEquals(1, redState.getTransitions().size());
        TransitionNode redTransition = redState.getTransitions().get(0);
        assertEquals("TIMER", redTransition.getEvent());
        assertEquals("Green", redTransition.getTargetStateName());
        assertTrue(redTransition.getGuards().isEmpty());
        assertTrue(redTransition.getActions().isEmpty());
        assertTrue(redTransition.getAnnotations().isEmpty());

        StateNode greenState = findStateByName(sm.getStates(), "Green");
        assertNotNull(greenState);
        assertEquals(1, greenState.getTransitions().size());
        assertEquals("TIMER", greenState.getTransitions().get(0).getEvent());
        assertEquals("Yellow", greenState.getTransitions().get(0).getTargetStateName());

        StateNode yellowState = findStateByName(sm.getStates(), "Yellow");
        assertNotNull(yellowState);
        assertEquals(1, yellowState.getTransitions().size());
        assertEquals("TIMER", yellowState.getTransitions().get(0).getEvent());
        assertEquals("Red", yellowState.getTransitions().get(0).getTargetStateName());
    }

    @Test
    void testHistoryStates() throws Exception {
        String input = """
        machines {
            machine MyMachine { // Added 'machine' keyword
                states {
                    initial state Parent; // Added initial state for clarity/validity?
                    state Parent {
                        history shallow;
                        initial state Child1; // Compound states need an initial state
                        states {
                           state Child1 {}; // Added 'state' keyword and {}
                           state Child2 {}; // Added 'state' keyword and {}
                        }
                        on GoToChild1 target Child1;
                    }
                    state Sibling { // Added 'state' keyword
                         history deep;
                         @id(123)
                         $description("Deep history state")
                         // Deep history itself doesn't define nested states in this block,
                         // it applies to the nested states defined within its 'states' block.
                         initial state GrandChild; // Required as it's compound
                         states {
                             state GrandChild { // Added 'state' keyword
                                history; // Default history (shallow) - applies to GGrandChild
                                initial state GGrandChild; // Required as it's compound
                                states {
                                    state GGrandChild {}; // Added 'state' keyword
                                }
                             }
                         }
                    }
                }
            }
        }
        """;
        SsotRoot root = parseAndBuildAst(input);
        assertEquals(1, root.getMachineDefinitions().size());
        assertTrue(root.getMachineDefinitions().get(0) instanceof MachineNode);
        MachineNode sm = (MachineNode) root.getMachineDefinitions().get(0); // Use MachineNode
        assertEquals("MyMachine", sm.getMachineName());

        // Find Parent state (assuming direct children of machine)
        StateNode parentState = findStateByName(sm.getStates(), "Parent");
        assertNotNull(parentState, "Parent state not found");
        assertEquals(StateType.COMPOUND, parentState.getType());
        // Check Optional initial state name
        assertTrue(parentState.getInitialStateName().isPresent(), "Parent state should have initial state name");
        assertEquals("Child1", parentState.getInitialStateName().get());

        assertTrue(parentState.getHistory().isPresent(), "Parent state should have history");
        HistoryStateNode parentHistory = parentState.getHistory().get(); // Use getHistory() and correct type
        assertEquals(HistoryStateType.SHALLOW, parentHistory.getType(), "Parent history should be shallow"); // Check type enum
        // assertEquals(parentState, parentHistory.getParentState(), "History node parent mismatch"); // HistoryStateNode might not hold parent state ref

        // Verify nested states in Parent
        assertEquals(2, parentState.getNestedStates().size()); // Use getNestedStates()
        StateNode child1 = findStateByName(parentState.getNestedStates(), "Child1"); // Use getNestedStates()
        StateNode child2 = findStateByName(parentState.getNestedStates(), "Child2"); // Use getNestedStates()
        assertNotNull(child1);
        assertNotNull(child2);
        assertEquals(StateType.NORMAL, child1.getType()); // Use NORMAL for simple states
        assertEquals(StateType.NORMAL, child2.getType()); // Use NORMAL for simple states

        // Find Sibling state
        StateNode siblingState = findStateByName(sm.getStates(), "Sibling");
        assertNotNull(siblingState, "Sibling state not found");
        assertEquals(Optional.of(123L), siblingState.getId());
        assertTrue(siblingState.getAnnotations().containsKey("description"));
        assertEquals("Deep history state", siblingState.getAnnotations().get("description"));
        assertEquals(StateType.COMPOUND, siblingState.getType());
        assertTrue(siblingState.getInitialStateName().isPresent(), "Sibling state should have initial state name");
        assertEquals("GrandChild", siblingState.getInitialStateName().get());

        assertTrue(siblingState.getHistory().isPresent(), "Sibling state should have history");
        HistoryStateNode siblingHistory = siblingState.getHistory().get(); // Use getHistory() and correct type
        assertEquals(HistoryStateType.DEEP, siblingHistory.getType(), "Sibling history should be deep"); // Check type enum
        // assertEquals(siblingState, siblingHistory.getParentState()); // Check if HistoryStateNode has parent link

        // Verify nested states in Sibling
        assertEquals(1, siblingState.getNestedStates().size()); // Use getNestedStates()
        StateNode grandChildState = findStateByName(siblingState.getNestedStates(), "GrandChild"); // Use getNestedStates()
        assertNotNull(grandChildState, "GrandChild state not found");
        assertEquals(StateType.COMPOUND, grandChildState.getType());
        assertTrue(grandChildState.getInitialStateName().isPresent(), "GrandChild state should have initial state name");
        assertEquals("GGrandChild", grandChildState.getInitialStateName().get());

        assertTrue(grandChildState.getHistory().isPresent(), "GrandChild state should have history");
        HistoryStateNode grandChildHistory = grandChildState.getHistory().get(); // Use getHistory() and correct type
        assertEquals(HistoryStateType.SHALLOW, grandChildHistory.getType(), "GrandChild history should be default (shallow)"); // Check type enum
        // assertEquals(grandChildState, grandChildHistory.getParentState()); // Check if HistoryStateNode has parent link

        // Verify nested states in GrandChild
        assertEquals(1, grandChildState.getNestedStates().size()); // Use getNestedStates()
        StateNode gGrandChildState = findStateByName(grandChildState.getNestedStates(), "GGrandChild"); // Use getNestedStates()
        assertNotNull(gGrandChildState, "GGrandChild state not found");
        assertEquals(StateType.NORMAL, gGrandChildState.getType()); // Use NORMAL for simple states


        // Check transition in Parent state
        assertEquals(1, parentState.getTransitions().size()); // Use getTransitions()
        TransitionNode goToChild1 = parentState.getTransitions().get(0);
        assertEquals("GoToChild1", goToChild1.getEvent());
        assertEquals("Child1", goToChild1.getTargetStateName());
    }

    // Helper method to find a state by name in a list of AstNode
    private StateNode findStateByName(List<? extends AstNode> nodes, String name) { // Accept List<? extends AstNode>
        return nodes.stream()
                    .filter(s -> s instanceof StateNode && ((StateNode)s).getStateName().equals(name))
                    .map(s -> (StateNode)s) // Cast to StateNode after filtering
                    .findFirst().orElse(null);
    }

    // ... other tests to be refactored ...
} 