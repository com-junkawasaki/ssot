package ssot_parser.ast.type;

/**
 * Represents the different types of states in a state machine.
 */
public enum StateType {
    /**
     * A simple state with no nested states.
     */
    ATOMIC,
    /**
     * A state that contains nested states (regions), one of which is active at a time.
     */
    COMPOUND,
    /**
     * A state that contains orthogonal regions, where each region has its own active state.
     * (Note: Current implementation might treat this similar to COMPOUND initially)
     */
    PARALLEL,
    /**
     * A terminal state for the state machine or a compound state.
     */
    FINAL,
    /**
     * Represents a reference to a state defined elsewhere (potentially used for history states later).
     * Might not be directly parsed but used internally.
     */
    // HISTORY // Consider adding if needed
} 