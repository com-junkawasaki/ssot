package ssot_parser;

import java.util.Optional;
import ssot_parser.ast.AstNode;

/** Interface for nodes that can have an @id annotation */
public interface NodeWithId extends AstNode {
    Optional<Long> getId();
} 