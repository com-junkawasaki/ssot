package ssot_parser;

import org.antlr.v4.runtime.tree.ParseTree;

/**
 * Base interface for all nodes in the Abstract Syntax Tree (AST).
 * The AST represents the semantic structure of the parsed SSoT document,
 * abstracting away from the specific grammar rules.
 */
public interface AstNode {
    // Potential common methods for AST nodes:
    // - accept(AstVisitor visitor); // For visitor pattern
    // - getChildren(); // Get child nodes
    // - getLocation(); // Get source location (line, column) - requires storing from ParseTree
    // - toString(); // For debugging
} 