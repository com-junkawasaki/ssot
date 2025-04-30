package ssot_parser;

import java.util.Optional;

/** Interface for nodes that can have an @id annotation */
public interface NodeWithId extends AstNode { // Added public
    Optional<Long> getId();
} 