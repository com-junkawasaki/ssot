#![allow(dead_code, unused_variables)] // Allow unused for now
use crate::ast::SsotAst;
use strum_macros::Display;
use thiserror::Error;

#[derive(Error, Debug, Clone, PartialEq, Eq, Hash, Display)]
pub enum SymbolKind {
    Type,
    TypeId,
    FieldId,
    EnumVariantId,
    Service,
    Interface, // Can be used interchangeably with Service in some contexts
    MethodId,
    ParameterId,
    Actor,
    Protocol,
    Channel,
    Event, // Maybe group under CommunicationItem?
    Machine,
    State,
    StateId,  // Added for state IDs
    RegionId, // Added for region IDs
    Action,
    ActionId, // Added for action IDs
    Guard,
    GuardId, // Added for guard IDs
    Invoke,
    InvokeId,         // Added for invoke IDs
    ContextFieldId,   // Added for context field IDs
    TransitionId,     // Added for transition IDs
    DeploymentTarget, // Generic for Env/Infra/Deploy
    DeploymentItemId, // Added for Env/Infra/Deploy IDs
                      // ... other kinds as needed
}

#[derive(Error, Debug, Clone, PartialEq, Eq)]
pub enum ValidationError {
    #[error("Validation skipped: Not implemented")]
    NotImplemented,
    // Define other minimal error variants if needed for basic checks later
}

#[derive(Debug, Default)]
pub struct SymbolTable {
    // Minimal stub, no actual symbol tracking for now
    _priv: (),
}

impl SymbolTable {
    // Minimal stub methods
    pub fn new() -> Self {
        SymbolTable::default()
    }
}

#[derive(Debug)]
pub struct Validator<'a> {
    ast: &'a SsotAst,
    symbol_table: SymbolTable, // Keep the table structure
}

impl<'a> Validator<'a> {
    pub fn new(ast: &'a SsotAst) -> Self {
        Self {
            ast,
            symbol_table: SymbolTable::new(),
        }
    }

    pub fn validate(&self) -> Vec<ValidationError> {
        // Return empty Vec, effectively skipping validation
        Vec::new()
    }
}

/// Main validation entry point (stub)
pub fn validate_ast(ast: &SsotAst) -> Result<(), Vec<ValidationError>> {
    let validator = Validator::new(ast);
    let errors = validator.validate(); // This will be empty

    if errors.is_empty() {
        Ok(())
    } else {
        Err(errors)
    }
}

// No #[cfg(test)] mod tests { ... } for now
