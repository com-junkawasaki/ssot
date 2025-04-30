package ssot_parser;

import ssot_parser.SSoTParser.*;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Visits the Parse Tree and builds an Abstract Syntax Tree (AST).
 */
public class AstBuilderVisitor extends SSoTBaseVisitor<AstNode> { // Return AstNode

    @Override
    public AstNode visitSsotFile(SsotFileContext ctx) {
        System.out.println("Building SsotRoot...");
        List<ImportNode> imports = new ArrayList<>();
        if (ctx.importStatement() != null) {
            for (ImportStatementContext importCtx : ctx.importStatement()) {
                imports.add((ImportNode) visit(importCtx));
            }
        }

        List<BlockNode> blocks = new ArrayList<>();
        // Assuming blocks are defined within a top-level structure or directly
        // This needs refinement based on the exact grammar structure for blocks
        if (ctx.typeDefBlock() != null) { // Example: only handling type blocks for now
             blocks.add((BlockNode) visit(ctx.typeDefBlock()));
        }
        // TODO: Add logic to visit other block types (service, machine, etc.)

        // TODO: Extract top-level file ID if defined in grammar
        Optional<String> fileId = Optional.empty();

        return new SsotRoot(fileId, imports, blocks);
    }

    @Override
    public AstNode visitImportStatement(ImportStatementContext ctx) {
        String path = stripQuotes(ctx.STRING_LITERAL().getText());
        System.out.println("  Building ImportNode: " + path);
        return new ImportNode(path);
    }

    @Override
    public AstNode visitTypeDefBlock(TypeDefBlockContext ctx) {
        System.out.println("Building TypeBlockNode...");
        List<TypeDefNode> typeDefs = new ArrayList<>();
        if (ctx.typeDefinition() != null) {
            for (TypeDefinitionContext typeDefCtx : ctx.typeDefinition()) {
                // Skip null results if a definition wasn't parsed correctly
                AstNode visitedNode = visit(typeDefCtx);
                if (visitedNode instanceof TypeDefNode) {
                    typeDefs.add((TypeDefNode) visitedNode);
                } else if (visitedNode != null) {
                    System.err.println("Warning: Visiting TypeDefinitionContext did not yield a TypeDefNode.");
                }
            }
        }
        return new TypeBlockNode(typeDefs);
    }

    @Override
    public AstNode visitTypeDefinition(TypeDefinitionContext ctx) {
        // Delegate to specific type definition visitors
        if (ctx.structDef() != null) {
            return visit(ctx.structDef());
        } else if (ctx.enumDef() != null) {
            return visit(ctx.enumDef());
        }
        // TODO: Add other type kinds if necessary
        System.err.println("Warning: Unhandled TypeDefinitionContext kind.");
        return null; // Or throw an error
    }

    @Override
    public AstNode visitStructDef(StructDefContext ctx) {
        String name = ctx.IDENTIFIER().getText();
        System.out.println("  Building StructDefNode: " + name);
        Optional<Long> id = extractId(ctx.annotation());

        List<FieldNode> fields = new ArrayList<>();
        // Assuming structBody rule contains structField*
        if (ctx.structBody() != null && ctx.structBody().structField() != null) {
            for (StructFieldContext fieldCtx : ctx.structBody().structField()) {
                AstNode visitedNode = visit(fieldCtx);
                if (visitedNode instanceof FieldNode) {
                     fields.add((FieldNode) visitedNode);
                } else if (visitedNode != null) {
                     System.err.println("Warning: Visiting StructFieldContext did not yield a FieldNode for struct '" + name + "'.");
                }
            }
        }

        return new StructDefNode(name, id, fields);
    }

    @Override
    public AstNode visitEnumDef(EnumDefContext ctx) {
        String name = ctx.IDENTIFIER().getText();
        System.out.println("  Building EnumDefNode: " + name);
        Optional<Long> id = extractId(ctx.annotation());

        List<EnumVariantNode> variants = new ArrayList<>();
        // Assuming enumBody rule contains enumVariant*
        if (ctx.enumBody() != null && ctx.enumBody().enumVariant() != null) {
            for (EnumVariantContext variantCtx : ctx.enumBody().enumVariant()) {
                 AstNode visitedNode = visit(variantCtx);
                 if (visitedNode instanceof EnumVariantNode) {
                    variants.add((EnumVariantNode) visitedNode);
                 } else if (visitedNode != null) {
                    System.err.println("Warning: Visiting EnumVariantContext did not yield an EnumVariantNode for enum '" + name + "'.");
                 }
            }
        }

        return new EnumDefNode(name, id, variants);
    }

    // --- Field and Variant Visitors (Assuming Grammar Rules) ---

    @Override
    public AstNode visitStructField(StructFieldContext ctx) {
        // Assuming StructFieldContext has IDENTIFIER, type, and optional annotation
        String fieldName = ctx.IDENTIFIER().getText();
        Optional<Long> fieldId = extractId(ctx.annotation());

        // Visit the type rule to get the type name
        String typeName = ""; // Default or error value
        if (ctx.type() != null) {
             AstNode typeNode = visit(ctx.type());
             if (typeNode instanceof TypeIdentifierNode) { // Assuming a simple wrapper for type string
                 typeName = ((TypeIdentifierNode) typeNode).getName();
             } else {
                  System.err.println("Warning: Could not determine type for field '" + fieldName + "'.");
             }
        }
        System.out.println("    Building FieldNode: " + fieldName + " : " + typeName + (fieldId.isPresent() ? " @id(" + fieldId.get() + ")" : ""));
        return new FieldNode(fieldName, typeName, fieldId);
    }

    @Override
    public AstNode visitEnumVariant(EnumVariantContext ctx) {
        // Assuming EnumVariantContext has IDENTIFIER and optional annotation
        String variantName = ctx.IDENTIFIER().getText();
        Optional<Long> variantId = extractId(ctx.annotation());
        System.out.println("    Building EnumVariantNode: " + variantName + (variantId.isPresent() ? " @id(" + variantId.get() + ")" : ""));
        return new EnumVariantNode(variantName, variantId);
    }

    // --- Type Visitor (Simplified) ---

    // Temporary record to wrap the type string from visitType
    private record TypeIdentifierNode(String name) implements AstNode {}

    @Override
    public AstNode visitType(TypeContext ctx) {
        // Highly simplified: assumes type is just a simple IDENTIFIER for now.
        // Grammar likely has more complex types (optional<T>, list<T>, qualified names etc.)
        if (ctx.IDENTIFIER() != null) {
            return new TypeIdentifierNode(ctx.IDENTIFIER().getText());
        }
        // TODO: Handle optional<T>, list<T>, qualified names, etc.
        System.err.println("Warning: Unhandled type structure in visitType.");
        return new TypeIdentifierNode("UNKNOWN_TYPE"); // Placeholder
    }

    // --- Helper Methods ---

    private Optional<Long> extractId(AnnotationContext annotationCtx) {
        if (annotationCtx != null && annotationCtx.idAnnotation() != null && annotationCtx.idAnnotation().integerLiteral() != null) {
            try {
                // Remove any potential suffixes like 'L' or 'l' if grammar allows them
                String literal = annotationCtx.idAnnotation().integerLiteral().getText().toUpperCase().replace("L", "");
                return Optional.of(Long.parseLong(literal));
            } catch (NumberFormatException e) {
                // Handle error: Invalid ID format
                System.err.println("Warning: Could not parse ID: " + annotationCtx.idAnnotation().integerLiteral().getText());
                return Optional.empty();
            }
        } else {
            return Optional.empty();
        }
    }

    private String stripQuotes(String text) {
        if (text != null && text.length() >= 2 && text.startsWith("\"") && text.endsWith("\"")) {
            return text.substring(1, text.length() - 1);
        }
        return text; // Return original if not quoted
    }

    // TODO: Add visit methods for other blocks, service definitions, machine definitions, etc.

} 