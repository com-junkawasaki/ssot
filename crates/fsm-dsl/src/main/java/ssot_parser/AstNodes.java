package ssot_parser;

import java.util.List;
import java.util.Optional;

// --- Base Interfaces/Classes ---
interface AstNode {}

interface BlockNode extends AstNode {
    String getBlockType(); // e.g., "types", "services"
}

interface TypeDefNode extends AstNode {
    String getName();
    Optional<Long> getId();
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
    @Override public Optional<Long> getId() { return id; }
}

/** Represents an 'enum ... { ... }' definition */
record EnumDefNode(
    String name,
    Optional<Long> id,
    List<EnumVariantNode> variants // Assuming EnumVariantNode exists
) implements TypeDefNode {
    @Override public String getName() { return name; }
    @Override public Optional<Long> getId() { return id; }
}

// --- Supporting Nodes (Placeholders for now) ---

/** Represents a field within a struct */
record FieldNode(String name, String type, Optional<Long> id) implements AstNode {}

/** Represents a variant within an enum */
record EnumVariantNode(String name, Optional<Long> id) implements AstNode {}

// Add other node types as needed (ServiceBlockNode, MachineDefNode, etc.) 