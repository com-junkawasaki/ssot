package ssot_parser.ast.nodes.state;

/**
 * Defines the type of history state (shallow or deep).
 */
public enum HistoryStateType {
    SHALLOW, // Remembers only the immediate substate
    DEEP     // Remembers the full state configuration down to atomic substates
} 