package ssot_parser.ast.type;

/**
 * Represents the type of a state in a state machine (e.g., normal, initial, final, compound, parallel).
 */
public enum StateType {
    NORMAL,
    INITIAL,
    FINAL,
    COMPOUND,   // A state that has nested states
    PARALLEL    // A state that has parallel regions (not fully supported in grammar yet)
} 