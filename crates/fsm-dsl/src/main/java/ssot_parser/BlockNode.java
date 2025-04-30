package ssot_parser;

import java.util.List;
import java.util.Optional;

/** Represents a block containing types, states, transitions */
public class BlockNode implements NodeWithId {
    private final Optional<Long> id;
    public final List<TypeDefNode> typeDefs;
    public final List<StateNode> states;
    public final List<TransitionNode> transitions;

    public BlockNode(Optional<Long> id, List<TypeDefNode> typeDefs, List<StateNode> states, List<TransitionNode> transitions) {
        this.id = id;
        this.typeDefs = typeDefs;
        this.states = states;
        this.transitions = transitions;
    }

    @Override
    public Optional<Long> getId() {
        return id;
    }
} 