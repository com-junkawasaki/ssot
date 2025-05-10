package ssot_parser.ast.values;

/**
 * Enum representing the different types a ValueNode can hold.
 */
public enum ValueNodeType {
    STRING,
    NUMBER, // General number, can be int or float
    INTEGER, // Specific integer type
    FLOAT,   // Specific float type
    BOOLEAN,
    NULL,
    ARRAY,
    OBJECT,
    REFERENCE // For identifiers that refer to other entities
} 