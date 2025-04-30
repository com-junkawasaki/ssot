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
                typeDefs.add((TypeDefNode) visit(typeDefCtx));
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
        return null; // Or throw an error
    }


    @Override
    public AstNode visitStructDef(StructDefContext ctx) {
        String name = ctx.IDENTIFIER().getText();
        System.out.println("  Building StructDefNode: " + name);
        Optional<Long> id = extractId(ctx.annotation());

        // Placeholder for field visiting logic
        List<FieldNode> fields = Collections.emptyList();
        // TODO: Implement visiting struct fields (ctx.structBody() etc.)
        // fields = ctx.structField().stream().map(f -> (FieldNode) visit(f)).collect(Collectors.toList());

        return new StructDefNode(name, id, fields);
    }

    @Override
    public AstNode visitEnumDef(EnumDefContext ctx) {
        String name = ctx.IDENTIFIER().getText();
        System.out.println("  Building EnumDefNode: " + name);
        Optional<Long> id = extractId(ctx.annotation());

        // Placeholder for variant visiting logic
        List<EnumVariantNode> variants = Collections.emptyList();
        // TODO: Implement visiting enum variants (ctx.enumBody() etc.)
        // variants = ctx.enumVariant().stream().map(v -> (EnumVariantNode) visit(v)).collect(Collectors.toList());

        return new EnumDefNode(name, id, variants);
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

    // TODO: Add visit methods for struct fields, enum variants, other blocks, etc.

} 