package ssot_parser;

import java.util.List;
import java.util.Optional;

// --- Base Interfaces/Classes ---
interface AstNode {}

/** Interface for nodes that can have an @id annotation */
interface NodeWithId extends AstNode {
    Optional<Long> getId();
}

interface BlockNode extends AstNode {
    String getBlockType(); // e.g., "types", "services"
}

interface TypeDefNode extends NodeWithId {
    String getName();
}

// --- Concrete AST Nodes ---

/** Represents the root of an SSOT file AST */
record SsotRoot(
    Optional<String> fileId, // Assuming a top-level file ID might exist
    List<ImportNode> imports,
    List<BlockNode> blocks
) implements AstNode {}

/** Represents an import statement */
record ImportNode(String path) implements AstNode {}

/** Represents a 'types { ... }' block */
record TypeBlockNode(List<TypeDefNode> typeDefinitions) implements BlockNode {
    @Override
    public String getBlockType() { return "types"; }
}

/** Represents a 'struct ... { ... }' definition */
record StructDefNode(
    String name,
    Optional<Long> id,
    List<FieldNode> fields // Assuming FieldNode exists
) implements TypeDefNode {
    @Override public String getName() { return name; }
}

/** Represents an 'enum ... { ... }' definition */
record EnumDefNode(
    String name,
    Optional<Long> id,
    List<EnumVariantNode> variants // Assuming EnumVariantNode exists
) implements TypeDefNode {
    @Override public String getName() { return name; }
}

// --- Supporting Nodes (Placeholders for now) ---

/** Represents a field within a struct */
record FieldNode(String name, String type, Optional<Long> id) implements NodeWithId {
}

/** Represents a variant within an enum */
record EnumVariantNode(String name, Optional<Long> id) implements NodeWithId {
}

// Add other node types as needed (ServiceBlockNode, MachineDefNode, etc.) 