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

import ssot_parser.SSoTLexer;
import ssot_parser.SSoTParser;
import ssot_parser.AstBuilderVisitor;
import ssot_parser.ast.AstNode;
import ssot_parser.ast.SsotRoot;
import ssot_parser.ast.nodes.MachineNode; // Correct import
import ssot_parser.ast.nodes.*;
import ssot_parser.ast.type.*;
import ssot_parser.ast.nodes.state.*;

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
            @id(1) $description("A simple machine")
            machine SimpleMachine {
                initial state Idle;
                state Idle {}
            }
        }
        """;
        SsotRoot root = parseAndBuildAst(input);
        assertEquals(1, root.getMachineDefinitions().size());

        // Use MachineNode
        MachineNode sm = (MachineNode) root.getMachineDefinitions().get(0); 
        assertEquals("SimpleMachine", sm.getMachineName());
        assertEquals(Optional.of(1L), sm.getId());
        assertEquals(1, sm.getAnnotations().size());
        assertTrue(sm.getAnnotations().containsKey("description"));
        assertEquals("A simple machine", sm.getAnnotations().get("description"));
        assertEquals("Idle", sm.getInitialStateName());
        assertEquals(1, sm.getStates().size());
        assertEquals("Idle", ((StateNode)sm.getStates().get(0)).getStateName()); 
    }

    @Test
    void testMachineWithTransition() throws Exception {
        String input = """
        machines {
            machine TrafficLight {
                initial state Red;
                state Red {
                    on TIMER transition Green;
                }
                state Green {
                    on TIMER transition Yellow;
                }
                state Yellow {
                    on TIMER transition Red;
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

        StateNode redState = sm.getStates().stream()
                             .filter(s -> s instanceof StateNode && ((StateNode)s).getStateName().equals("Red"))
                             .map(s -> (StateNode)s)
                             .findFirst().orElse(null);
        assertNotNull(redState);
        assertEquals(1, redState.getTransitions().size());
        TransitionNode redTransition = redState.getTransitions().get(0);
        assertEquals("TIMER", redTransition.getEvent());
        assertEquals("Green", redTransition.getTargetStateName());
        assertTrue(redTransition.getGuards().isEmpty());
        assertTrue(redTransition.getActions().isEmpty());
        assertTrue(redTransition.getAnnotations().isEmpty());

        StateNode greenState = sm.getStates().stream()
                               .filter(s -> s instanceof StateNode && ((StateNode)s).getStateName().equals("Green"))
                               .map(s -> (StateNode)s)
                               .findFirst().orElse(null);
        assertNotNull(greenState);
        assertEquals(1, greenState.getTransitions().size());
        assertEquals("TIMER", greenState.getTransitions().get(0).getEvent());
        assertEquals("Yellow", greenState.getTransitions().get(0).getTargetStateName());

        StateNode yellowState = sm.getStates().stream()
                                .filter(s -> s instanceof StateNode && ((StateNode)s).getStateName().equals("Yellow"))
                                .map(s -> (StateNode)s)
                                .findFirst().orElse(null);
        assertNotNull(yellowState);
        assertEquals(1, yellowState.getTransitions().size());
        assertEquals("TIMER", yellowState.getTransitions().get(0).getEvent());
        assertEquals("Red", yellowState.getTransitions().get(0).getTargetStateName());
    }

    // TODO: Update remaining tests (testHistoryStates, testStateWithInvoke, etc.) 
    //       to use SsotRoot, parseAndBuildAst, MachineNode, getStateName/getMachineName, getters, and necessary casts.
    //       Remove redundant casts based on mvn test warnings once compilation is fixed.
    // Example update for testHistoryStates:
    /*
    @Test
    void testHistoryStates() throws Exception {
        String input = """
        machines {
            MyMachine {
                states {
                    Parent {
                        history shallow;
                        states {
                            Child1 {}
                            Child2 {}
                        }
                        on GoToChild1 transition Child1;
                    }
                    Sibling {
                         history deep;
                         @id(123)
                         $description("Deep history state")
                         states {
                            GrandChild {
                                history;
                            }
                         }
                    }
                }
            }
        }
        """;
        SsotRoot root = parseAndBuildAst(input);
        assertEquals(1, root.getMachineDefinitions().size());
        MachineNode sm = (MachineNode) root.getMachineDefinitions().get(0); // Use MachineNode

        StateNode parentState = sm.getStates().stream()
                                  .filter(s -> ((StateNode)s).getStateName().equals("Parent"))
                                  .map(s -> (StateNode)s)
                                  .findFirst().orElse(null);
        assertNotNull(parentState);
        assertEquals(StateType.COMPOUND, parentState.getType());
        assertEquals("Child1", parentState.getInitialStateName());

        assertTrue(parentState.getHistory().isPresent());
        HistoryNode historyNode = parentState.getHistory().get();
        assertEquals("H", historyNode.getName()); // Assuming name is H? Check DSL
        assertFalse(historyNode.isDeep());

        // Need to check MachineNode for transitions or how they are accessed
        // TransitionNode toHistory = sm.getTransitions()... 
    }
    */

    // ... other tests to be refactored ...
} 