package ssot_parser.ast; // Corrected package

import org.antlr.v4.runtime.tree.ParseTree;
import java.util.Map;
import java.util.Optional;

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

    // Method to get the map of $name annotations associated with this node
    Map<String, Object> getAnnotations();

    // Method to get the @id annotation if present
    Optional<Long> getId();

    // Add accept method for Visitor pattern
    <T> T accept(NodeVisitor<T> visitor);
} 