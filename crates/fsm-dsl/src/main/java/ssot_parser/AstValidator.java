package ssot_parser;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Performs semantic validation on the generated AST.
 */
public class AstValidator {

    /** Represents a validation error */
    public record ValidationError(String message) {}

    private final List<ValidationError> errors = new ArrayList<>();

    /**
     * Validates the entire SSOT AST.
     * @param root The root node of the AST.
     * @return A list of validation errors found. Empty if validation passes.
     */
    public List<ValidationError> validate(SsotRoot root) {
        errors.clear();

        if (root == null) {
            errors.add(new ValidationError("AST root node is null."));
            return errors;
        }

        // Validate different parts of the AST
        validateBlocks(root.blocks());

        // Add more validation calls here (e.g., check imports, references, etc.)

        return errors;
    }

    private void validateBlocks(List<BlockNode> blocks) {
        for (BlockNode block : blocks) {
            if (block instanceof TypeBlockNode typeBlock) {
                validateTypeBlock(typeBlock);
            } else {
                // TODO: Add validation for other block types (Service, Machine, etc.)
                System.out.println("Skipping validation for block type: " + block.getBlockType());
            }
        }
        // TODO: Check for duplicate block kinds/names if necessary
    }

    private void validateTypeBlock(TypeBlockNode typeBlock) {
        System.out.println("Validating Type Block...");
        validateUniqueTypeNames(typeBlock.typeDefinitions());
        // TODO: Add validation for unique IDs within the type block
        // TODO: Validate fields and variants within each type definition
    }

    private void validateUniqueTypeNames(List<TypeDefNode> typeDefs) {
        Set<String> names = new HashSet<>();
        for (TypeDefNode typeDef : typeDefs) {
            String name = typeDef.getName();
            if (!names.add(name)) {
                // Found duplicate name
                errors.add(new ValidationError("Duplicate type definition name found in types block: '" + name + "'"));
            }
        }
        System.out.println("  Checked for unique type names.");
    }

    // TODO: Add methods for other validation rules:
    // - validateUniqueIds(List<? extends AstNodeWithId> nodes)
    // - validateFieldTypes(List<FieldNode> fields, SymbolTable availableTypes)
    // - validateReferences(...) - Check if imported/referenced types/services exist

} 