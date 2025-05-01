package ssot_parser;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Map;
import ssot_parser.ast.AstNode;

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
     * Validates the entire SSOT AST.
     * @param root The root node of the AST.
     * @return A list of validation errors found. Empty if validation passes.
     */
    public List<ValidationError> validate(SsotRoot root) {
        errors.clear();
        System.out.println("Starting AST validation...");

        if (root == null) {
            errors.add(new ValidationError("AST root node is null.", null));
            return errors;
        }

        // Validate different parts of the AST
        validateTypeDefinitions(root.getTypeDefinitions());

        // TODO: Add calls to validate other definition types (services, machines) using their getters
        // validateServiceDefinitions(root.getServiceDefinitions());
        // validateMachineDefinitions(root.getMachineDefinitions());

        System.out.println("AST validation finished.");
        return errors;
    }

    private void validateTypeDefinitions(List<AstNode> typeDefsPossiblyMixed) {
        System.out.println("Validating Type Definitions...");
        List<TypeDefNode> actualTypeDefs = new ArrayList<>();
        for (AstNode node : typeDefsPossiblyMixed) {
             if (node instanceof TypeDefNode) {
                 actualTypeDefs.add((TypeDefNode) node);
             } else if (node != null) {
                 // This might happen if AstBuilderVisitor puts non-TypeDefNodes in the list
                 errors.add(new ValidationError("Unexpected node type found in type definitions list: " + node.getClass().getSimpleName(), node));
             }
        }
        validateUniqueTypeNames(actualTypeDefs);
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

} 