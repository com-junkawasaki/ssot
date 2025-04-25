use std::path::PathBuf;
use thiserror::Error;

/// Errors that can occur during code generation or symbol resolution within codegen.
#[derive(Debug, Error)]
pub enum CodegenError {
    #[error("Failed to parse generated code: {0}\n--- Generated Code ---\n{1}")]
    SynParseError(syn::Error, String),
    #[error("AST validation error: {0}")]
    AstValidationError(String),
    #[error("Code generation failed: {0}")]
    GenerationError(String),
    #[error("I/O error: {0}")] // Simplified I/O error
    IoError(#[from] std::io::Error),
    #[error("Formatting error: {0}")] // Simplified format error
    FormatError(String),

    // --- Resolution Errors (mirrored from BuildError) ---
    #[error("Duplicate package declaration '{package_name}'.")]
    DuplicatePackage { package_name: String },
    #[error("Package '{package_name}' imported but not found.")]
    PackageNotFound { package_name: String }, // Simplified: Path context might be lost here
    #[error("Duplicate definition for symbol '{symbol_name}'.")]
    DuplicateSymbol { symbol_name: String },
    #[error("Symbol '{symbol_name}' not found (context: {context_description}).")]
    SymbolNotFound {
        symbol_name: String,
        context_description: String,
    },
    #[error("Ambiguous symbol '{symbol_name}' (context: {context_description}).")]
    AmbiguousSymbol {
        symbol_name: String,
        context_description: String,
    },
    #[error("Circular dependency detected involving package '{package_name}'.")]
    CircularDependency { package_name: String },
    #[error("Internal resolution error: {0}")]
    InternalResolution(String),
}

// Implement From<std::fmt::Error> for easier error handling in formatting code
impl From<std::fmt::Error> for CodegenError {
    fn from(e: std::fmt::Error) -> Self {
        CodegenError::FormatError(e.to_string())
    }
} 