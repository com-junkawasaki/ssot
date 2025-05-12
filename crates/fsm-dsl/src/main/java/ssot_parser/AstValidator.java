package ssot_parser;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import ssot_parser.ast.AstNode;
import ssot_parser.ast.nodes.TypeDefNode;
import ssot_parser.ast.SsotRoot;

/**
 * Validates the constructed Abstract Syntax Tree (AST) for semantic errors,
 * inconsistencies, and adherence to DSL rules not caught by the parser.
 */
public class AstValidator {

    /** Represents a validation error */
    public static final class ValidationError {
        private final String message;
        private final AstNode node;

        public ValidationError(String message, AstNode node) {
            this.message = message;
            this.node = node;
        }

        public String message() {
            return message;
        }

        public AstNode node() {
            return node;
        }

        // Optional: Add equals, hashCode, toString if needed
    }

    private final List<ValidationError> errors = new ArrayList<>();

    /**
     * Validates the entire AST starting from the root node.
     * @param root The root node of the AST.
     */
    public void validate(SsotRoot root) {
        if (root == null) {
            errors.add(new ValidationError("Root node cannot be null.", null));
            return;
        }

        // Example validation: Check for duplicate type definitions
        Map<String, TypeDefNode> typeNames = new HashMap<>();
        for (TypeDefNode typeDef : root.getTypeDefinitions()) {
            if (typeNames.containsKey(typeDef.getName())) {
                errors.add(new ValidationError("Duplicate type definition: " + typeDef.getName(), typeDef));
            } else {
                typeNames.put(typeDef.getName(), typeDef);
            }
            // TODO: Add more specific validation for each type definition
        }

        // TODO: Add validation for other definitions (services, machines, etc.)

        // Example: Check for undefined type references within fields (if applicable)
        // This would require iterating through all structure fields, service parameters, etc.
    }

    private void validateTypeDefinitions(List<TypeDefNode> typeDefs) {
        System.out.println("Validating Type Definitions...");
        validateUniqueTypeNames(typeDefs);
        // TODO: Add validation for unique IDs within the type block
        // TODO: Validate fields and variants within each type definition
    }

    private void validateUniqueTypeNames(List<TypeDefNode> typeDefs) {
        Set<String> names = new HashSet<>();
        for (TypeDefNode typeDef : typeDefs) {
            String name = typeDef.getName();
            if (!names.add(name)) {
                // Found duplicate name
                errors.add(new ValidationError("Duplicate type definition name found in types block: '" + name + "'", null));
            }
        }
        System.out.println("  Checked for unique type names.");
    }

    // TODO: Add methods for other validation rules:
    // - validateUniqueIds(List<? extends AstNodeWithId> nodes)
    // - validateFieldTypes(List<FieldNode> fields, SymbolTable availableTypes)
    // - validateReferences(...) - Check if imported/referenced types/services exist

    /**
     * Returns the list of validation errors found.
     * @return A list of ValidationError objects.
     */
    public List<ValidationError> getErrors() {
        return errors;
    }
} 