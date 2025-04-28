#![allow(dead_code, unused_variables)] // Keep module level for now
use crate::ast::{self, SsotAst, TopLevelDefinition, Identifier, NumericId, TypeDefinition, MachineDefinition, StateDefinition, ActionDefinition, GuardDefinition, InvokeDefinition, TypeSpecifier, TransitionTarget, FieldDefinition, StructDefinition, EnumDefinition, StatesBlock, TransitionDefinition as AstTransitionDefinition, StateInvokeDefinition, InvokeSource, Annotation}; // Added Annotation import
use std::collections::{HashMap, HashSet}; // Added HashMap and HashSet
use strum_macros::Display;
use thiserror::Error;

// Represents the scope where a symbol is defined, using IDs for uniqueness
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub enum ScopeId {
    Global,         // ID 0 might represent the global scope
    Machine(u64),   // Machine ID
    State { machine_id: u64, state_id: u64 }, // Machine ID and State ID
    // Add other scopes as needed (e.g., Interface(u64), Actor(u64))
}

// Information stored for each symbol
#[derive(Debug, Clone)]
pub struct SymbolInfo {
    kind: SymbolKind,
    name: Identifier, // Keep the name for informational purposes
    id: NumericId,
    defined_in_scope: ScopeId, // Scope where this symbol was originally defined
    // TODO: Add Span/Location information later
}

#[derive(Error, Debug, Clone, PartialEq, Eq, Hash, Display)]
pub enum SymbolKind {
    Type,
    // TypeId, // Use Type with ID check
    // FieldId, // Field IDs are scoped within struct/enum
    // EnumVariantId, // Variant IDs are scoped within enum
    Service,
    Interface, // Can be used interchangeably with Service in some contexts
    // MethodId, // Scoped within interface/service
    // ParameterId, // Scoped within method
    Actor,
    Protocol,
    Channel,
    Event, // Maybe group under CommunicationItem?
    Machine,
    State, // State names are scoped within machine
    // StateId, // Use State with ID check
    // RegionId, // Scoped within state?
    Action, // Actions are scoped within machine
    // ActionId, // Use Action with ID check
    Guard, // Guards are scoped within machine
    // GuardId, // Use Guard with ID check
    Invoke, // Invokes are scoped within machine
    // InvokeId, // Use Invoke with ID check
    // ContextFieldId, // Scoped within context
    // TransitionId, // Scoped within state/machine? Needs clarification
    DeploymentTarget, // Generic for Env/Infra/Deploy
                       // DeploymentItemId, // Use DeploymentTarget with ID check
                       // ... other kinds as needed
}

#[derive(Error, Debug, Clone, PartialEq, Eq)]
pub enum ValidationError {
    #[error("Validation skipped: Not implemented yet for element.")]
    NotImplemented,
    #[error("Duplicate ID {id} for {kind} '{name}' in scope {scope:?}. First defined as '{existing_name}'.")]
    DuplicateIdInScope {
        scope: ScopeId,
        kind: SymbolKind,
        name: String, // Name of the symbol with the duplicate ID
        id: u64, // The duplicate ID value
        existing_name: String, // Name of the existing symbol with the same ID in this scope/kind
    },
    #[error("Duplicate Name '{name}' for {kind} in scope {scope:?}. Existing ID: {existing_id}, New ID: {new_id}")]
    DuplicateNameInScope {
        scope: ScopeId,
        kind: SymbolKind,
        name: String,
        existing_id: u64,
        new_id: u64,
    },
    #[error("Undefined reference to {kind} '{name}' when searching from scope {scope:?}.")]
    UndefinedReference {
        scope: ScopeId, // Scope where the reference occurs / search starts
        kind: SymbolKind, // Kind of symbol being referenced
        name: String, // Name of the referenced symbol
    },
    // TODO: Add more specific errors: InvalidType, MissingInitialState, etc.
}

#[derive(Debug, Default, Clone)]
pub struct SymbolTable {
    // ScopeId -> SymbolKind -> Name -> SymbolInfo (Primary lookup by name)
    symbols_by_name: HashMap<ScopeId, HashMap<SymbolKind, HashMap<Identifier, SymbolInfo>>>,
    // ScopeId -> SymbolKind -> ID -> SymbolInfo (Primary lookup by ID, also for duplicate ID check)
    symbols_by_id: HashMap<ScopeId, HashMap<SymbolKind, HashMap<u64, SymbolInfo>>>,
}

impl SymbolTable {
    pub fn new() -> Self {
        SymbolTable::default()
    }

    // Inserts a symbol, checking for name and ID duplicates within the specific scope.
    pub fn insert(
        &mut self,
        scope: ScopeId,
        kind: SymbolKind,
        name: Identifier,
        id: NumericId, // Keep ID mandatory for now, pass dummy for CommItems
    ) -> Result<(), ValidationError> {
        let scope_symbols_by_name = self
            .symbols_by_name
            .entry(scope.clone())
            .or_insert_with(HashMap::new);
        let kind_symbols_by_name = scope_symbols_by_name
            .entry(kind.clone())
            .or_insert_with(HashMap::new);

        let scope_symbols_by_id = self
            .symbols_by_id
            .entry(scope.clone())
            .or_insert_with(HashMap::new);
        let kind_symbols_by_id = scope_symbols_by_id
            .entry(kind.clone())
            .or_insert_with(HashMap::new);

        // DEBUG PRINT for ID check
        // println!(
        //     "[Insert ID Check] Scope: {:?}, Kind: {:?}, ID: {}, Name: {}, Map: {:?}",
        //     scope, kind, id.value, name.name,
        //     kind_symbols_by_id.keys() // Print existing IDs in this scope/kind
        // );

        // Check for ID duplicate within this specific scope and kind
        if let Some(existing_info) = kind_symbols_by_id.get(&id.value) {
            if existing_info.name == name {
                return Ok(()); // Allow idempotent inserts
            } else {
                return Err(ValidationError::DuplicateIdInScope {
                    scope,
                    kind,
                    name: name.name,
                    id: id.value,
                    existing_name: existing_info.name.name.clone(),
                });
            }
        }

        // DEBUG PRINT for Name check
        // println!(
        //     "[Insert Name Check] Scope: {:?}, Kind: {:?}, Name: {}, Map Keys: {:?}",
        //     scope, kind, name.name,
        //     kind_symbols_by_name.keys()
        // );

        // Check for Name duplicate within this specific scope and kind
        if let Some(existing_info) = kind_symbols_by_name.get(&name) {
            return Err(ValidationError::DuplicateNameInScope {
                scope,
                kind,
                name: name.name,
                existing_id: existing_info.id.value,
                new_id: id.value,
            });
        }

        // If no duplicates, insert into both maps
        let info = SymbolInfo {
            kind: kind.clone(),
            name: name.clone(),
            id: id.clone(),
            defined_in_scope: scope.clone(),
        };
        kind_symbols_by_name.insert(name, info.clone());
        kind_symbols_by_id.insert(id.value, info);

        Ok(())
    }

    // Looks up a symbol by NAME, searching scopes appropriately.
    pub fn lookup_by_name(
        &self,
        search_start_scope: &ScopeId,
        kind: &SymbolKind,
        name: &Identifier,
    ) -> Option<&SymbolInfo> {
        // Define the search order based on the starting scope
        let scopes_to_search = match search_start_scope {
            ScopeId::Global => vec![ScopeId::Global],
            ScopeId::Machine(machine_id) => vec![
                ScopeId::Machine(*machine_id),
                ScopeId::Global
            ],
            ScopeId::State { machine_id, state_id } => vec![
                // Search order: Current State -> Parent Machine -> Global
                ScopeId::State { machine_id: *machine_id, state_id: *state_id },
                ScopeId::Machine(*machine_id),
                ScopeId::Global,
            ],
            // Add search paths for other scope types if needed
        };

        for scope in scopes_to_search {
            if let Some(scope_symbols) = self.symbols_by_name.get(&scope) {
                if let Some(kind_symbols) = scope_symbols.get(kind) {
                    if let Some(info) = kind_symbols.get(name) {
                        return Some(info); // Found
                    }
                }
            }
        }

        None // Not found in any relevant scope
    }

    // Looks up a symbol by ID within a specific scope.
    // ID lookups usually don't need to traverse parent scopes unless specified.
    pub fn lookup_by_id(
        &self,
        scope: &ScopeId,
        kind: &SymbolKind,
        id: u64,
    ) -> Option<&SymbolInfo> {
         self.symbols_by_id
            .get(scope)
            .and_then(|scope_map| scope_map.get(kind))
            .and_then(|kind_map| kind_map.get(&id))
    }
}

#[derive(Debug)]
pub struct Validator<'a> {
    ast: &'a SsotAst,
    symbol_table: SymbolTable,
    errors: Vec<ValidationError>,
    // TODO: Add current_scope tracking during traversal
    // current_scope: ScopeId,
}

impl<'a> Validator<'a> {
    pub fn new(ast: &'a SsotAst) -> Self {
        Self {
            ast,
            symbol_table: SymbolTable::new(),
            errors: Vec::new(),
            // current_scope: ScopeId::Global, // Start in global
        }
    }

    // Entry point for validation
    pub fn validate(mut self) -> Vec<ValidationError> {
        // Phase 1: Populate Symbol Table (needs ScopeId)
        self.populate_symbols(ScopeId::Global); // Start population from Global

        // Phase 2: Resolve References (Recursive, tracks ScopeId)
        self.resolve_references(ScopeId::Global);

        self.errors
    }

    // Phase 1: Populate Symbol Table (Recursive, tracks ScopeId)
    fn populate_symbols(&mut self, current_scope: ScopeId) {
         // This needs to iterate through self.ast and call specific populators
         // passing the correct scope ID down.
        for definition in &self.ast.definitions {
             match definition {
                 TopLevelDefinition::Types(block) => self.populate_types_block(&ScopeId::Global, block),
                 TopLevelDefinition::Machines(block) => self.populate_machines_block(&ScopeId::Global, block),
                // TODO: Add other top-level block populators
                TopLevelDefinition::Services(block) => self.populate_services_block(&ScopeId::Global, block),
                TopLevelDefinition::Actors(block) => self.populate_actors_block(&ScopeId::Global, block),
                TopLevelDefinition::Communication(block) => self.populate_communication_block(&ScopeId::Global, block),
                TopLevelDefinition::DeploymentConfig(block) => self.populate_deployment_config_block(&ScopeId::Global, block),
                 // _ => {} // Remove this line as we handle all top levels now
             }
         }
         // Remove the placeholder NotImplemented error if all top levels are handled
         // self.errors.push(ValidationError::NotImplemented);
    }

    fn populate_types_block(&mut self, scope: &ScopeId, block: &'a ast::TypesBlock) {
        // Types are always global
        for type_def in &block.definitions {
             let (name, id, kind) = match type_def {
                 TypeDefinition::Struct(s) => {
                     // Populate fields within the struct's scope (using type ID? or just check locally?)
                     // For now, just register the struct type itself globally.
                     (&s.name, &s.id, SymbolKind::Type)
                 },
                 TypeDefinition::Enum(e) => {
                     // Populate variants within the enum's scope?
                     // For now, just register the enum type itself globally.
                     (&e.name, &e.id, SymbolKind::Type)
                 },
             };
             if let Err(e) = self.symbol_table.insert(ScopeId::Global, kind, name.clone(), id.clone()) {
                 self.errors.push(e);
             }
        }
    }

     fn populate_machines_block(&mut self, scope: &ScopeId, block: &'a ast::MachinesBlock) {
         // Machines are defined globally, but contain their own scope
         for machine_def in &block.definitions {
            // Register machine itself in Global scope
             if let Err(e) = self.symbol_table.insert(
                 ScopeId::Global,
                 SymbolKind::Machine,
                 machine_def.name.clone(),
                 machine_def.id.clone(),
             ) {
                 self.errors.push(e);
             }

             let machine_scope = ScopeId::Machine(machine_def.id.value);

             // Populate elements defined within the machine scope
             if let Some(actions) = &machine_def.actions {
                  self.populate_actions_block(&machine_scope, actions);
             }
             if let Some(guards) = &machine_def.guards {
                  self.populate_guards_block(&machine_scope, guards);
             }
             if let Some(invokes) = &machine_def.invokes {
                  self.populate_invokes_block(&machine_scope, invokes);
             }
             if let Some(states) = &machine_def.states {
                 // Pass the machine scope as the parent for top-level states
                 self.populate_states_block(&machine_scope, states);
             }
             // TODO: Populate context fields if they need symbol table entries
         }
     }

    // --- Populate helpers for machine elements (Actions, Guards, Invokes, States) ---
    // These seem okay as they receive the correct machine_scope
     fn populate_actions_block(&mut self, scope: &ScopeId, block: &'a ast::ActionsBlock) {
         for action_def in &block.definitions {
             if let Err(e) = self.symbol_table.insert(
                 scope.clone(), SymbolKind::Action, action_def.name.clone(), action_def.id.clone()
             ) {
                 self.errors.push(e);
             }
         }
     }
     fn populate_guards_block(&mut self, scope: &ScopeId, block: &'a ast::GuardsBlock) {
         for guard_def in &block.definitions {
             if let Err(e) = self.symbol_table.insert(
                 scope.clone(), SymbolKind::Guard, guard_def.name.clone(), guard_def.id.clone()
             ) {
                 self.errors.push(e);
             }
         }
     }
     fn populate_invokes_block(&mut self, scope: &ScopeId, block: &'a ast::InvokesBlock) {
         for invoke_def in &block.definitions {
             if let Err(e) = self.symbol_table.insert(
                 scope.clone(), SymbolKind::Invoke, invoke_def.name.clone(), invoke_def.id.clone()
             ) {
                 self.errors.push(e);
             }
         }
     }

     fn populate_states_block(&mut self, parent_scope: &ScopeId, block: &'a ast::StatesBlock) {
         for state_def in &block.states {
             // States are defined/registered within their parent scope (Machine or State)
             if let Err(e) = self.symbol_table.insert(
                 parent_scope.clone(), // Register in parent scope
                 SymbolKind::State,
                 state_def.name.clone(),
                 state_def.id.clone(),
             ) {
                 self.errors.push(e);
             }

             // Define the scope for potential nested states
             let current_state_scope = match parent_scope {
                 ScopeId::Machine(machine_id) => ScopeId::State { machine_id: *machine_id, state_id: state_def.id.value },
                 ScopeId::State { machine_id, .. } => ScopeId::State { machine_id: *machine_id, state_id: state_def.id.value },
                 ScopeId::Global => ScopeId::Global,
             };

             // Recursively populate nested regions using the state's own scope ID
             for region in &state_def.regions {
                 self.populate_states_block(&current_state_scope, region);
             }
         }
     }

    // --- Populate helpers for other top-level blocks (Services, Actors, etc.) ---
    fn populate_services_block(&mut self, scope: &ScopeId, block: &'a ast::ServicesBlock) {
        for item in &block.definitions {
            let (name, id, kind) = match item {
                ast::ServiceItem::Interface(i) => (&i.name, &i.id, SymbolKind::Interface),
                ast::ServiceItem::Service(s) => (&s.name, &s.id, SymbolKind::Service),
            };
             if let Err(e) = self.symbol_table.insert(ScopeId::Global, kind, name.clone(), id.clone()) {
                 self.errors.push(e);
             }
             // TODO: Populate methods within interface/service scope?
        }
    }

    fn populate_actors_block(&mut self, scope: &ScopeId, block: &'a ast::ActorsBlock) {
         for actor_def in &block.definitions {
             if let Err(e) = self.symbol_table.insert(
                 ScopeId::Global, SymbolKind::Actor, actor_def.name.clone(), actor_def.id.clone()
             ) {
                 self.errors.push(e);
             }
        }
    }

    fn populate_communication_block(&mut self, scope: &ScopeId, block: &'a ast::CommunicationBlock) {
         for item in &block.definitions {
             // Adapt to the actual structure from ast.rs (name: String, no id)
            let (name_str, kind) = match item {
                 // Correctly access the String name directly
                 ast::CommunicationItem::Protocol(p) => (p.name.clone(), SymbolKind::Protocol),
                 ast::CommunicationItem::Channel(c) => (c.name.clone(), SymbolKind::Channel),
                 ast::CommunicationItem::Event(ev) => (ev.name.clone(), SymbolKind::Event),
            };
             // Create Identifier from String, pass dummy ID (using a distinct value like MAX)
             let name = Identifier { name: name_str };
             // Use a specific value (e.g., u64::MAX or 0) consistently as the dummy ID.
             // Using MAX might be slightly safer to avoid clashes with real ID 0.
             let dummy_id = NumericId { value: u64::MAX };

             // Insert using Global scope and dummy ID
             if let Err(e) = self.symbol_table.insert(ScopeId::Global, kind, name, dummy_id) {
                 // Handle potential name duplication if dummy IDs are reused
                 match e {
                     ValidationError::DuplicateNameInScope { .. } => self.errors.push(e),
                     // Ignore DuplicateIdInScope errors potentially caused by the dummy ID
                     ValidationError::DuplicateIdInScope { .. } => { /* Ignored */ },
                     _ => self.errors.push(e),
                 }
             }
         }
     }

     fn populate_deployment_config_block(&mut self, scope: &ScopeId, block: &'a ast::DeploymentConfigBlock) {
         for item in &block.definitions {
            let (name, id, kind) = match item {
                 ast::DeploymentItem::Environment(env) => (&env.name, &env.id, SymbolKind::DeploymentTarget),
                 ast::DeploymentItem::Infrastructure(inf) => (&inf.name, &inf.id, SymbolKind::DeploymentTarget),
                 ast::DeploymentItem::Deployment(dep) => (&dep.name, &dep.id, SymbolKind::DeploymentTarget),
             };
             // Use DeploymentTarget kind for all, check uniqueness within Global scope for this kind
              if let Err(e) = self.symbol_table.insert(ScopeId::Global, kind, name.clone(), id.clone()) {
                 self.errors.push(e);
             }
         }
     }

    // Phase 2: Resolve References (Recursive, tracks ScopeId)
    fn resolve_references(&mut self, current_scope: ScopeId) {
        // Traverse the AST similar to populate_symbols, calling resolvers
        for definition in &self.ast.definitions {
            match definition {
                TopLevelDefinition::Types(block) => self.resolve_references_in_types_block(&current_scope, block),
                TopLevelDefinition::Machines(block) => self.resolve_references_in_machines_block(&current_scope, block),
                TopLevelDefinition::Services(block) => self.resolve_references_in_services_block(&current_scope, block),
                TopLevelDefinition::Communication(block) => self.resolve_references_in_communication_block(&current_scope, block),
                 // Actors and DeploymentConfig often don't have complex internal references to resolve in phase 2
                 // unless annotations are involved.
                 TopLevelDefinition::Actors(_) => { /* Skip for now */ },
                 TopLevelDefinition::DeploymentConfig(_) => { /* Skip for now */ },
            }
        }
        // Remove NotImplemented error if all relevant blocks are handled
        // self.errors.push(ValidationError::NotImplemented);
    }

    // --- Specific Reference Resolution Helpers (using ScopeId) ---

    fn resolve_references_in_types_block(&mut self, scope: &ScopeId, block: &'a ast::TypesBlock) {
        // Types are defined globally, so references within them are resolved starting from global
        let global_scope = ScopeId::Global;
        for type_def in &block.definitions {
            match type_def {
                TypeDefinition::Struct(s) => self.resolve_references_in_struct(&global_scope, s),
                TypeDefinition::Enum(e) => self.resolve_references_in_enum(&global_scope, e),
            }
        }
    }

    fn resolve_references_in_struct(&mut self, scope: &ScopeId, struct_def: &'a StructDefinition) {
        // Resolve field types
        for field in &struct_def.fields {
            self.resolve_type_specifier(scope, &field.type_spec);
        }
        // Resolve annotations like $implements
        for annotation in &struct_def.annotations {
             if let Annotation::Implements(interface_name) = annotation {
                 // Interfaces are global
                 self.resolve_reference(&ScopeId::Global, SymbolKind::Interface, interface_name);
             }
             // TODO: Resolve other annotation references if needed
         }
    }

    fn resolve_references_in_enum(&mut self, scope: &ScopeId, enum_def: &'a EnumDefinition) {
         // Resolve annotations like $implements
         for annotation in &enum_def.annotations {
             if let Annotation::Implements(interface_name) = annotation {
                 // Interfaces are global
                 self.resolve_reference(&ScopeId::Global, SymbolKind::Interface, interface_name);
             }
             // TODO: Resolve other annotation references if needed
         }
    }

    fn resolve_type_specifier(&mut self, scope: &ScopeId, type_spec: &'a TypeSpecifier) {
        match type_spec {
            TypeSpecifier::Simple(name) => {
                let primitives = HashSet::from(["string", "integer", "bool", "float", "number", "any", "void", "timestamp", "i32", "u64"]);
                if !primitives.contains(name.name.as_str()) {
                    // User-defined types are always global
                    self.resolve_reference(&ScopeId::Global, SymbolKind::Type, name);
                }
            }
            TypeSpecifier::List(inner) => self.resolve_type_specifier(scope, inner),
            TypeSpecifier::Optional(inner) => self.resolve_type_specifier(scope, inner),
            TypeSpecifier::Map(k, v) => {
                self.resolve_type_specifier(scope, k);
                self.resolve_type_specifier(scope, v);
            }
        }
    }

    fn resolve_references_in_machines_block(&mut self, scope: &ScopeId, block: &'a ast::MachinesBlock) {
        // Iterate through machines defined in this block
        for machine_def in &block.definitions {
            let machine_scope = ScopeId::Machine(machine_def.id.value);

            // Resolve context field types
            if let Some(context) = &machine_def.context {
                for field in &context.fields {
                    self.resolve_type_specifier(&machine_scope, &field.type_spec); // Resolve type starting from machine scope (though it will likely check global)
                }
            }

            // Resolve references within states, starting from the machine scope
            if let Some(states) = &machine_def.states {
                self.resolve_references_in_states_block(&machine_scope, states);
            }

            // Resolve references within invokes defined at machine level
             if let Some(invokes) = &machine_def.invokes {
                 self.resolve_references_in_invokes_block(&machine_scope, invokes);
             }
            // TODO: Resolve machine annotations
        }
    }

    // Updated state reference resolution using ScopeId
    fn resolve_references_in_states_block(&mut self, parent_scope: &ScopeId, block: &'a ast::StatesBlock) {
        for state_def in &block.states {
            // Define the scope for this state
            let current_state_scope = match parent_scope {
                ScopeId::Machine(machine_id) => ScopeId::State { machine_id: *machine_id, state_id: state_def.id.value },
                ScopeId::State { machine_id, .. } => ScopeId::State { machine_id: *machine_id, state_id: state_def.id.value },
                ScopeId::Global => parent_scope.clone(), // Should not happen
            };

            // Get the machine scope for resolving actions/guards/invokes
            let machine_scope = match parent_scope {
                 ScopeId::Machine(_) => parent_scope.clone(),
                 ScopeId::State { machine_id, .. } => ScopeId::Machine(*machine_id),
                 _ => parent_scope.clone(), // Fallback
            };

            // onEntry/onExit actions
            for action_ref in &state_def.on_entry {
                self.resolve_reference(&machine_scope, SymbolKind::Action, action_ref);
            }
            for action_ref in &state_def.on_exit {
                self.resolve_reference(&machine_scope, SymbolKind::Action, action_ref);
            }

            // Transitions
            for transition in &state_def.transitions {
                // Target state lookup starts from the parent scope (machine or parent state)
                self.resolve_transition_target(parent_scope, &machine_scope, &transition.target);
                // Actions/guards lookup happens in machine scope
                for action_ref in &transition.actions {
                     self.resolve_reference(&machine_scope, SymbolKind::Action, action_ref);
                }
                for guard_ref in &transition.guards {
                     self.resolve_reference(&machine_scope, SymbolKind::Guard, guard_ref);
                }
            }

            // After Transitions
            for transition in &state_def.after_transitions {
                 self.resolve_transition_target(parent_scope, &machine_scope, &transition.target);
                 for action_ref in &transition.actions {
                     self.resolve_reference(&machine_scope, SymbolKind::Action, action_ref);
                 }
                 for guard_ref in &transition.guards {
                     self.resolve_reference(&machine_scope, SymbolKind::Guard, guard_ref);
                 }
            }

            // State Invokes
            for invoke in &state_def.invokes {
                // Invoke definition lookup happens in machine scope
                self.resolve_reference(&machine_scope, SymbolKind::Invoke, &invoke.src_ref);
                // Resolve onDone/onError transitions
                if let Some(on_done) = &invoke.on_done {
                    // Target state lookup starts from parent scope, actions/guards from machine scope
                    self.resolve_invoke_transition_target(parent_scope, &machine_scope, on_done);
                }
                if let Some(on_error) = &invoke.on_error {
                     self.resolve_invoke_transition_target(parent_scope, &machine_scope, on_error);
                }
            }

            // History
             if let Some(history) = &state_def.history {
                 // Default target state lookup starts from parent scope
                 self.resolve_reference(parent_scope, SymbolKind::State, &history.default_target);
             }

            // Nested States (Regions)
            for region in &state_def.regions {
                // Recurse using the current state scope as the new parent
                self.resolve_references_in_states_block(&current_state_scope, region);
            }
        }
    }

    fn resolve_references_in_invokes_block(&mut self, machine_scope: &ScopeId, block: &'a ast::InvokesBlock) {
        for invoke_def in &block.definitions {
             match &invoke_def.src {
                 InvokeSource::ServiceMethod(service_name, method_name) => {
                     // Service is global
                     self.resolve_reference(&ScopeId::Global, SymbolKind::Service, service_name);
                     // TODO: Resolve method_name within the service scope
                 }
                 InvokeSource::Machine(machine_name) => {
                     // Machine is global
                     self.resolve_reference(&ScopeId::Global, SymbolKind::Machine, machine_name);
                 }
                 InvokeSource::Literal(_) => {}
             }

             // Resolve onDone/onError transitions (targets resolved starting from machine scope)
             if let Some(on_done) = &invoke_def.on_done {
                 self.resolve_invoke_transition_target(machine_scope, machine_scope, on_done);
             }
             if let Some(on_error) = &invoke_def.on_error {
                  self.resolve_invoke_transition_target(machine_scope, machine_scope, on_error);
             }
        }
    }

    // --- Resolve helpers for Services and Communication ---
    fn resolve_references_in_services_block(&mut self, scope: &ScopeId, block: &'a ast::ServicesBlock) {
         let global_scope = ScopeId::Global;
         for item in &block.definitions {
             match item {
                 ast::ServiceItem::Interface(i) => {
                     // Resolve method parameter types and return types
                     for method in &i.methods {
                         self.resolve_method_signature(&global_scope, method);
                     }
                 }
                 ast::ServiceItem::Service(s) => {
                     // Resolve $implements annotation
                     for annotation in &s.annotations {
                         if let Annotation::Implements(iface_name) = annotation {
                             self.resolve_reference(&global_scope, SymbolKind::Interface, iface_name);
                         }
                     }
                     // Resolve `extends` reference
                     if let Some(base_service_name) = &s.extends {
                         self.resolve_reference(&global_scope, SymbolKind::Service, base_service_name);
                     }
                 }
             }
         }
    }

    fn resolve_method_signature(&mut self, scope: &ScopeId, method: &'a ast::MethodDefinition) {
         for param in &method.parameters {
             self.resolve_type_specifier(scope, &param.type_spec);
         }
         if let Some(return_type) = &method.return_type {
             self.resolve_type_specifier(scope, return_type);
         }
         // TODO: Resolve annotations within method body?
    }

     fn resolve_references_in_communication_block(&mut self, scope: &ScopeId, block: &'a ast::CommunicationBlock) {
         let global_scope = ScopeId::Global;
         for item in &block.definitions {
             match item {
                 ast::CommunicationItem::Event(ev) => {
                     // Cannot resolve fields or annotations as they don't exist in current ast::EventDefinition
                     // If EventDefinition is expanded later in ast.rs to include fields/annotations,
                     // uncomment and adapt the following:
                     /*
                     // Resolve event field types
                     for field in &ev.fields { // Assumes ev has fields: Vec<FieldDefinition>
                          self.resolve_type_specifier(&global_scope, &field.type_spec);
                     }
                     // Resolve $channel annotation
                      for annotation in &ev.annotations { // Assumes ev has annotations: Vec<Annotation>
                         if let Annotation::Channel(channel_name) = annotation {
                             self.resolve_reference(&global_scope, SymbolKind::Channel, channel_name);
                         }
                     }
                     */
                 }
                 ast::CommunicationItem::Protocol(_) => { /* No internal refs */ }
                 ast::CommunicationItem::Channel(_) => { /* No internal refs */ }
             }
         }
     }

    // --- Core Reference Resolution Helpers (Using ScopeId) ---

    fn resolve_reference(&mut self, search_start_scope: &ScopeId, kind: SymbolKind, name: &'a Identifier) {
        // DEBUG PRINT for reference resolution
        // println!(
        //     "[Resolve Ref] Scope: {:?}, Kind: {:?}, Name: {}",
        //     search_start_scope, kind, name.name
        // );

        let lookup_result = self.symbol_table.lookup_by_name(search_start_scope, &kind, name);

        // DEBUG PRINT for lookup result
        // println!(
        //     "[Resolve Ref Result] Found: {}",
        //     lookup_result.is_some()
        // );

        if lookup_result.is_none() {
            self.errors.push(ValidationError::UndefinedReference {
                scope: search_start_scope.clone(),
                kind,
                name: name.name.clone(),
            });
        }
    }

    fn resolve_transition_target(
        &mut self,
        state_lookup_scope: &ScopeId, // Scope to start looking for state names
        machine_scope: &ScopeId,      // Machine scope for qualified history parent lookup
        target: &'a TransitionTarget
    ) {
        match target {
            TransitionTarget::State(name) => {
                // Lookup starts from state_lookup_scope (parent scope)
                self.resolve_reference(state_lookup_scope, SymbolKind::State, name);
            }
            TransitionTarget::CurrentHistory => { /* No name resolution */ }
            TransitionTarget::QualifiedHistory(parent_state_name) => {
                 // Parent state name lookup starts from machine scope
                 self.resolve_reference(machine_scope, SymbolKind::State, parent_state_name);
            }
        }
    }

     fn resolve_invoke_transition_target(
         &mut self,
         state_lookup_scope: &ScopeId, // Scope for resolving target state names
         machine_scope: &ScopeId,      // Scope for resolving actions/guards
         target: &'a ast::InvokeTransitionTarget
     ) {
         // Resolve target state (lookup starts from state_lookup_scope)
         self.resolve_transition_target(state_lookup_scope, machine_scope, &target.target);
         // Resolve actions/guards (lookup starts from machine_scope)
         for action_ref in &target.actions {
             self.resolve_reference(machine_scope, SymbolKind::Action, action_ref);
         }
         for guard_ref in &target.guards {
             self.resolve_reference(machine_scope, SymbolKind::Guard, guard_ref);
         }
     }

}

/// Main validation entry point
pub fn validate_ast(ast: &SsotAst) -> Result<(), Vec<ValidationError>> {
    let mut validator = Validator::new(ast);
    let errors = validator.validate(); // Consume the validator here

    if errors.is_empty() {
        Ok(())
    } else {
        // Filter out NotImplemented errors for now if we want to see only real errors
        let real_errors: Vec<_> = errors.into_iter().filter(|e| !matches!(e, ValidationError::NotImplemented)).collect();
         if real_errors.is_empty() {
            Ok(()) // No actual errors found yet
        } else {
            Err(real_errors)
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::ast::*;
    use crate::parser::parse_ssot_content; // Use the actual parser for realistic ASTs
    use std::path::PathBuf;

    // Helper to create Identifier
    fn ident(name: &str) -> Identifier {
        Identifier { name: name.to_string() }
    }

    // Helper to create NumericId
    fn num_id(value: u64) -> NumericId {
        NumericId { value }
    }

    #[test]
    fn test_symbol_table_insert_ok() {
        let mut table = SymbolTable::new();
        let scope = ScopeId::Global;
        let res = table.insert(scope, SymbolKind::Type, ident("MyType"), num_id(0));
        assert!(res.is_ok());
    }

    #[test]
    fn test_symbol_table_duplicate_id() {
        let mut table = SymbolTable::new();
        let scope = ScopeId::Global;
        table.insert(scope.clone(), SymbolKind::Type, ident("MyType1"), num_id(0)).unwrap();
        let res = table.insert(scope.clone(), SymbolKind::Type, ident("MyType2"), num_id(0));
        assert!(matches!(res, Err(ValidationError::DuplicateIdInScope { scope: ScopeId::Global, kind: SymbolKind::Type, id: 0, name: ref n, .. }) if n == "MyType2"));
    }

    #[test]
    fn test_symbol_table_duplicate_name() {
        let mut table = SymbolTable::new();
        let scope = ScopeId::Global;
        table.insert(scope.clone(), SymbolKind::Type, ident("MyType"), num_id(0)).unwrap();
        let res = table.insert(scope.clone(), SymbolKind::Type, ident("MyType"), num_id(1));
         assert!(matches!(res, Err(ValidationError::DuplicateNameInScope { scope: ScopeId::Global, kind: SymbolKind::Type, name: ref n, existing_id: 0, new_id: 1, .. }) if n == "MyType"));
    }

    #[test]
    fn test_symbol_table_insert_idempotent_error() {
        // Current implementation returns DuplicateId error even for idempotent inserts
        // UPDATED: Now expects Ok(()) for idempotent inserts
        let mut table = SymbolTable::new();
        let scope = ScopeId::Global;
        table.insert(scope.clone(), SymbolKind::Type, ident("MyType"), num_id(0)).unwrap();
        let res = table.insert(scope.clone(), SymbolKind::Type, ident("MyType"), num_id(0));
        // assert!(matches!(res, Err(ValidationError::DuplicateId { .. })));
        assert!(res.is_ok()); // Expect Ok(()) now
    }

    // --- Integration Tests using validate_ast ---

    fn run_validation(content: &str) -> Result<(), Vec<ValidationError>> {
        let ast = parse_ssot_content(content, Some(PathBuf::from("test.ssot")))
            .expect("Parsing failed in test setup");
        validate_ast(&ast)
    }

    #[test]
    fn test_validate_ok_simple() {
        let content = r#"
            types {
                struct Point @id(0) { x: integer @id(0); y: integer @id(1); }
            }
            machines {
                machine SimpleMachine @id(0) {
                    states @id(0) {
                        state Idle @id(0) { $initial; }
                    }
                }
            }
        "#;
        assert!(run_validation(content).is_ok());
    }

    #[test]
    fn test_validate_duplicate_type_id() {
        let content = r#"
            types {
                struct Point @id(0) { x: integer @id(0); }
                struct Vector @id(0) { x: integer @id(0); } // Duplicate ID 0
            }
        "#;
        let errors = run_validation(content).expect_err("Validation should fail");
        assert_eq!(errors.len(), 1);
        assert!(matches!(errors[0], ValidationError::DuplicateIdInScope { kind: SymbolKind::Type, id: 0, name: ref n, .. } if n == "Vector"));
    }

    #[test]
    fn test_validate_duplicate_machine_name() {
        let content = r#"
            machines {
                machine SimpleMachine @id(0) { states @id(0) { state A @id(0); } }
                machine SimpleMachine @id(1) { states @id(0) { state B @id(0); } } // Duplicate name
            }
        "#;
        let errors = run_validation(content).expect_err("Validation should fail");
        // assert_eq!(errors.len(), 1);
        // Duplicate name error should be reported once.
        assert_eq!(errors.len(), 1, "Expected 1 error, found: {:?}", errors);
         assert!(matches!(errors[0], ValidationError::DuplicateNameInScope { kind: SymbolKind::Machine, name: ref n, existing_id: 0, new_id: 1, .. } if n == "SimpleMachine"));
    }

    #[test]
    fn test_validate_duplicate_state_name_in_machine() {
        let content = r#"
            machines {
                machine SimpleMachine @id(0) {
                    states @id(0) {
                        state Idle @id(0);
                        state Idle @id(1); // Duplicate state name within machine
                    }
                }
            }
        "#;
         let errors = run_validation(content).expect_err("Validation should fail");
        assert_eq!(errors.len(), 1);
        assert!(matches!(errors[0], ValidationError::DuplicateNameInScope { kind: SymbolKind::State, name: ref n, existing_id: 0, new_id: 1, .. } if n == "Idle"));
    }

     #[test]
    fn test_validate_duplicate_action_id_in_machine() {
        let content = r#"
            machines {
                machine SimpleMachine @id(0) {
                    actions @id(0) {
                        action DoThingA @id(0);
                        action DoThingB @id(0); // Duplicate action ID
                    }
                    states @id(1) { state Idle @id(0); }
                }
            }
        "#;
         let errors = run_validation(content).expect_err("Validation should fail");
        assert_eq!(errors.len(), 1);
         assert!(matches!(errors[0], ValidationError::DuplicateIdInScope { kind: SymbolKind::Action, id: 0, name: ref n, .. } if n == "DoThingB"));
    }

    // --- Phase 2: Reference Resolution Tests ---

    #[test]
    fn test_validate_undefined_field_type() {
        let content = r#"
            types {
                struct Point @id(0) { x: UndefinedType @id(0); } // Undefined type reference
            }
        "#;
        let errors = run_validation(content).expect_err("Validation should fail");
        assert_eq!(errors.len(), 1);
         assert!(matches!(errors[0], ValidationError::UndefinedReference { kind: SymbolKind::Type, name: ref n, scope: ScopeId::Global, .. } if n == "UndefinedType"));
    }

     #[test]
    fn test_validate_undefined_transition_target() {
        let content = r#"
            machines {
                machine SimpleMachine @id(0) {
                    states @id(0) {
                        state Idle @id(0) {
                             on EVENT @id(0) target UndefinedState;
                        }
                        // UndefinedState is not defined
                    }
                }
            }
        "#;
        let errors = run_validation(content).expect_err("Validation should fail");
        assert_eq!(errors.len(), 1);
        assert!(matches!(errors[0], ValidationError::UndefinedReference { kind: SymbolKind::State, name: ref n, scope: ScopeId::Machine(0), .. } if n == "UndefinedState"));
    }

    #[test]
    fn test_validate_undefined_transition_action() {
        let content = r#"
            machines {
                machine SimpleMachine @id(0) {
                    actions @id(0) { action RealAction @id(0); }
                    states @id(1) {
                        state Idle @id(0) {
                             on EVENT @id(0) target Idle {
                                action UndefinedAction; // Undefined action reference
                             }
                        }
                    }
                }
            }
        "#;
        let errors = run_validation(content).expect_err("Validation should fail");
        assert_eq!(errors.len(), 1);
        assert!(matches!(errors[0], ValidationError::UndefinedReference { kind: SymbolKind::Action, name: ref n, scope: ScopeId::Machine(0), .. } if n == "UndefinedAction"));
    }

     #[test]
    fn test_validate_undefined_transition_guard() {
        let content = r#"
            machines {
                machine SimpleMachine @id(0) {
                    guards @id(0) { guard RealGuard @id(0); }
                    states @id(1) {
                        state Idle @id(0) {
                             on EVENT @id(0) target Idle {
                                guard UndefinedGuard; // Undefined guard reference
                             }
                        }
                    }
                }
            }
        "#;
        let errors = run_validation(content).expect_err("Validation should fail");
        assert_eq!(errors.len(), 1);
        assert!(matches!(errors[0], ValidationError::UndefinedReference { kind: SymbolKind::Guard, name: ref n, scope: ScopeId::Machine(0), .. } if n == "UndefinedGuard"));
    }

     #[test]
    fn test_validate_undefined_state_invoke_src() {
        let content = r#"
            machines {
                machine SimpleMachine @id(0) {
                     // Corrected invoke definition syntax: { src: ...; }
                     invokes @id(0) { invoke RealInvoke @id(0) { src: "some_literal"; } }
                     states @id(1) {
                         state Idle @id(0) {
                              // state invoke syntax seems okay, reference is the issue
                              invoke @id(0) {
                                  src: invokes.UndefinedInvoke; // Undefined invoke reference
                              }
                         }
                     }
                }
            }
        "#;
         let errors = run_validation(content).expect_err("Validation should fail");
         assert_eq!(errors.len(), 1);
         assert!(matches!(errors[0], ValidationError::UndefinedReference { kind: SymbolKind::Invoke, name: ref n, scope: ScopeId::Machine(0), .. } if n == "UndefinedInvoke"));
    }

}
