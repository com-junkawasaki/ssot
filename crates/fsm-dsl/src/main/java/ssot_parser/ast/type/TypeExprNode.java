package ssot_parser.ast.type;

import ssot_parser.ast.AstNode;
import ssot_parser.ast.NodeVisitor;

/**
 * Base interface for all type expression nodes in the AST.
 * Represents types like primitives (string, u32), references (MyStruct),
 * optionals (optional<T>), lists (list<T>), and maps (map<K, V>).
 */
public interface TypeExprNode extends AstNode {
    // Common methods for type nodes can be defined here if needed.

    // Accept method for the visitor pattern (implementation required by concrete classes)
    @Override
    <T> T accept(NodeVisitor<T> visitor);
} 