use fsm_dsl::ast::{
    // Import directly from the ast module
    Annotation,
    AnnotationValue,
    ContextDefinition,
    ContextFieldDefinition,
    EventDefinition,
    EventsBlock,
    Identifier,
    // FieldType, // Removed - Replace with actual types if needed
    // QualifiedIdent, // Removed - Likely replaced by simple Identifier
    // SsotFile, // Removed - Use SsotAst directly
    // StateMachine, // Removed - Use MachineDefinition directly
    // TransitionElement, // Removed - Integrated into TransitionDefinition
    MachineDefinition,
    PrimitiveType,
    SsotAst,
    StateDefinition,
    StatesBlock,
    TransitionDefinition,
    TypeSpecifier,
    TransitionTarget,
};
use proc_macro2::{Ident as TokenIdent, TokenStream};
use quote::{format_ident, quote};
use std::collections::{BTreeMap, HashSet};
use std::fmt::Write;
// Needed for parsing generated code before formatting // For collecting unique guard/action names

pub mod codegen_capnp; // Add new module
                       // TODO: Create src/codegen_plantuml.rs or remove this line if unused.
                       // pub mod codegen_plantuml; // Assuming this exists
pub mod codegen_scxml;
pub mod codegen_ts; // Add new module
pub mod codegen_xstate; // Add new module for XState // Add new module for SCXML

// Define CodegenError locally
use thiserror::Error;

#[derive(Error, Debug)]
pub enum CodegenError {
    #[error("Failed to parse generated code: {0}\n--- Generated Code ---\n{1}")]
    SynParseError(syn::Error, String),
    #[error("AST validation error: {0}")]
    AstValidationError(String),
    #[error("Code generation failed: {0}")]
    GenerationError(String),
    #[error("I/O error during code formatting: {0}")]
    FormatIoError(#[from] std::io::Error), // This handles From<std::io::Error>
    #[error("Failed to format generated code: {0}")]
    FormatError(String),
    // Removed symbol/package related errors, handled by build script or parser
    // Potential future errors: IO errors, etc.
    #[error("Unsupported format: {0}")]
    UnsupportedFormat(String),
    #[error("Not implemented: {0}")]
    NotImplemented(String),
}

// Add back From<std::fmt::Error> implementation
impl From<std::fmt::Error> for CodegenError {
    fn from(e: std::fmt::Error) -> Self {
        CodegenError::GenerationError(e.to_string()) // Map fmt error to GenerationError
    }
}

// Helper to find annotation value by name (Updated for new AST - Limited Usefulness)
fn find_annotation_value<'a>(
    annotations: &'a [Annotation],
    name: &str,
) -> Option<&'a AnnotationValue> {
    // Keep returning Option<&'a AnnotationValue> for now
    annotations.iter().find_map(|anno| match anno {
        // This function is less useful now. Specific helpers might be better.
        // Example: Find a specific key in $validate, $db, $meta
        Annotation::Validate(args) | Annotation::Db(args) | Annotation::Meta(args) => args
            .iter()
            .find(|arg| arg.key.name == name)
            .map(|arg| &arg.value),
        // Example: Find a specific $generic(value) - requires AST change for AnnotationValue return
        Annotation::GenericKeyValue(key, _value_str) if key.name == name => {
            // Cannot return AnnotationValue easily as _value_str is String
            None
        }
        _ => None,
    })
    // Note: This function might be removed or heavily refactored.
}

// Helper function to generate Rust doc comments from annotations
fn generate_rust_doc_comment(annotations: &[Annotation]) -> TokenStream {
    let mut doc_stream = quote! {};
    // Find the $description annotation specifically
    let description = annotations.iter().find_map(|anno| match anno {
        Annotation::Description(desc) => Some(desc),
        _ => None,
    });

    if let Some(desc) = description {
        for line in desc.lines() {
            let trimmed_line = line.trim();
            doc_stream.extend(quote! {
                #[doc = #trimmed_line]
            });
        }
    }
    // Add other annotation processing here if needed (e.g., #[deprecated])
    doc_stream
}

// Helper function to map DSL TypeSpecifier to Rust type TokenStream
fn map_type_specifier_to_rust_type(type_spec: &TypeSpecifier) -> Result<TokenStream, CodegenError> {
    match type_spec {
        TypeSpecifier::Simple(ident) => {
            // Map basic types or assume custom types
            let type_name = &ident.name;
            match type_name.as_str() {
                "bool" => Ok(quote! { bool }),
                "int" | "i32" => Ok(quote! { i32 }), // Assuming default int is i32
                "i8" => Ok(quote! { i8 }),
                "i16" => Ok(quote! { i16 }),
                "i64" => Ok(quote! { i64 }),
                "u8" => Ok(quote! { u8 }),
                "u16" => Ok(quote! { u16 }),
                "u32" => Ok(quote! { u32 }),
                "u64" => Ok(quote! { u64 }),
                "f32" => Ok(quote! { f32 }),
                "f64" => Ok(quote! { f64 }),
                "string" | "text" => Ok(quote! { String }),
                "data" => Ok(quote! { Vec<u8> }),
                "void" => Ok(quote! { () }),
                // Assume other simple identifiers are custom struct/enum names
                custom => {
                    let custom_ident = format_ident!("{}", custom);
                    Ok(quote! { #custom_ident })
                }
            }
        }
        TypeSpecifier::List(inner) => {
            let inner_rust_type = map_type_specifier_to_rust_type(inner)?;
            Ok(quote! { Vec<#inner_rust_type> })
        }
        TypeSpecifier::Optional(inner) => {
            let inner_rust_type = map_type_specifier_to_rust_type(inner)?;
            Ok(quote! { Option<#inner_rust_type> })
        } // TODO: Add Map type if needed
        TypeSpecifier::Map(key, value) => {
            // Placeholder: Map complex types like BTreeMap, needs import
            // For now, return an error or a placeholder type
            let key_rust_type = map_type_specifier_to_rust_type(key)?;
            let value_rust_type = map_type_specifier_to_rust_type(value)?;
            Ok(quote! { std::collections::BTreeMap<#key_rust_type, #value_rust_type> })
        }
    }
}

// Helper to get the simple Ident from a QualifiedIdent
/* // TODO: Refactor or remove this function as QualifiedIdent is gone
pub(crate) fn get_simple_ident(qident: &QualifiedIdent) -> &TokenIdent {
    match qident {
        QualifiedIdent::Simple(id) => id,
        QualifiedIdent::Qualified { name, .. } => name,
    }
}
*/

/// Generates the `impl` block for the state machine struct.
fn generate_impl_block(
    ast: &StateMachine, // TODO: Replace StateMachine with MachineDefinition
    callbacks_trait_name: &TokenIdent,
) -> Result<TokenStream, CodegenError> {
    let machine_struct_name = format_ident!("{}", ast.name);
    let state_enum_name = format_ident!("State");
    let event_enum_name = format_ident!("Event");

    // Find $initial annotation specifically
    let initial_state_ident = ast
        .annotations
        .iter()
        .find_map(|anno| match anno {
            Annotation::GenericKeyValue(key, value_str) if key.name == "initial" => {
                Some(format_ident!("{}", value_str))
            }
            _ => None,
        })
        .ok_or_else(|| {
            CodegenError::AstValidationError(
                "Missing or invalid '$initial(StateName)' annotation on machine.".to_string(),
            )
        })?;

    // TODO: Update state checking logic
    // if !ast.states.iter().any(|s| &s.name == initial_state_ident) { ... }
    let initial_state_assignment = quote! { #state_enum_name::#initial_state_ident };

    // TODO: Refactor transition mapping completely
    let on_event_match_arms = quote! {}; // Placeholder
                                         /*
                                             .map(|transition| -> Result<TokenStream, CodegenError> {
                                                 // ... existing complex logic using TransitionElement, get_simple_ident ...
                                                 // This whole block needs to be rewritten based on TransitionDefinition
                                             })
                                             .collect::<Result<Vec<_>, _>>()?;
                                         */

    // TODO: Refactor context field generation
    let context_fields = quote! {}; // Placeholder

    Ok(quote! {
        // ... (impl block structure, but content needs rewrite) ...
        impl #machine_struct_name {
            pub fn new(callbacks: Box<dyn #callbacks_trait_name>) -> Self {
                Self {
                    current_state: #initial_state_assignment,
                    callbacks,
                    #context_fields // Placeholder
                }
            }

            pub fn current_state(&self) -> &#state_enum_name {
                 &self.current_state
            }

            // TODO: Rewrite on_event based on new AST
            /*
            pub fn on_event(mut self, event: &#event_enum_name) -> Result<Self, String> {
                let mut next_state_machine = self.clone(); // Clone for potential state change
                match (&self.current_state, event) {
                    #(#on_event_match_arms)*
                    _ => { /* No transition for this event in this state */ }
                }
                Ok(next_state_machine) // Return the (potentially updated) state machine
            }
            */
        }
    }) // Placeholder return
}

/// Generates the Event enum definition with associated data structs.
/* // TODO: Re-enable and refactor once Event definitions are added to AST
#[allow(unused_assignments)] // payload_struct_name used in quote! macro later
fn generate_event_enum_and_structs(
    ast: &StateMachine, // TODO: Needs MachineDefinition
    derive_tokens: &TokenStream,
) -> Result<TokenStream, CodegenError> {
    // Return Result
    let event_enum_name = format_ident!("Event");
    // Use a more descriptive convention for payload structs, e.g., EventNamePayload
    // let event_payload_struct_suffix = format_ident!("Payload");

    let mut event_structs = Vec::new();
    let variants_results: Result<Vec<TokenStream>, CodegenError> = ast
        .events // TODO: MachineDefinition doesn't have .events
        .iter()
        .map(|event_item| { // TODO: event_item needs to be defined based on new AST
            let variant_name = &event_item.name;
            let variant_doc_comment = generate_rust_doc_comment(&event_item.annotations);

            if event_item.fields.is_empty() {
                // Event without payload
                Ok(quote! {
                    #variant_doc_comment
                    #variant_name
                })
            } else {
                // Event with payload struct
                let struct_name = format_ident!("{}Payload", variant_name); // Convention: EventNamePayload
                let struct_doc_comment = generate_rust_doc_comment(&event_item.annotations); // Use event doc for struct

                // Generate fields, resolving types using the context
                let fields_results: Result<Vec<TokenStream>, CodegenError> = event_item
                    .fields
                    .iter()
                    .map(|field| { // TODO: field needs to be defined based on new AST (e.g., ContextFieldDefinition?)
                        let field_name = &field.name;
                        // Use updated map_field_type_to_rust_type without context
                        // let field_type_tokens = map_field_type_to_rust_type(&field.field_type)?; // Old
                        let field_type_tokens = map_type_specifier_to_rust_type(&field.type_spec)?; // New?
                        let field_doc_comment = generate_rust_doc_comment(&field.annotations);
                        Ok(quote! {
                            #field_doc_comment
                            pub #field_name: #field_type_tokens
                        })
                    })
                    .collect(); // Collect results for fields

                let fields = fields_results?;

                // Generate the payload struct definition
                event_structs.push(quote! {
                    #struct_doc_comment
                    #derive_tokens // Derive traits for payload struct too
                    pub struct #struct_name {
                        #(#fields),*
                    }
                });

                // Generate the enum variant with the payload struct
                Ok(quote! {
                    #variant_doc_comment
                    #variant_name(#struct_name)
                })
            }
        })
        .collect(); // Collect results for variants

    let variants = variants_results?;

    let event_enum_doc_comment = generate_rust_doc_comment(&[]); // TODO: Get annotations for the enum itself?

    Ok(quote! {
        // --- Event Payload Structs ---
        #(#event_structs)*

        // --- Event Enum ---
        #event_enum_doc_comment
        #derive_tokens // Use the same derives as State and Machine
        pub enum #event_enum_name {
            #(#variants),*
        }
    })
}
*/

/// Generates Rust code from a StateMachine AST node (from parser).
///
/// * `ast` - A parsed `StateMachine` from `fsm_dsl::parser`.
///
/// # Returns
///
/// * `Result<String, CodegenError>` - Generated Rust code string, or an error.
pub fn generate_rust_code(machine_ast: &MachineDefinition) -> Result<String, CodegenError> {
    let state_enum_name = format_ident!("State");
    let machine_struct_name = format_ident!("{}", machine_ast.name.name); // Use name from AST
    let event_enum_name = format_ident!("Event"); // Consistent event enum name
    let callbacks_trait_name = format_ident!("{}Callbacks", machine_ast.name.name); // e.g., LightSwitchCallbacks

    let _derive_tokens = quote! { #[derive(Debug, Clone, PartialEq)] };

    // State enum generation
    let states_block = machine_ast.states.as_ref().ok_or_else(|| {
        CodegenError::AstValidationError("Machine definition requires a 'states' block".to_string())
    })?;
    let state_variants = states_block.states.iter().map(|s| {
        let variant_name = format_ident!("{}", s.name.name);
        let doc_comment = generate_rust_doc_comment(&s.annotations);
        quote! {
            #doc_comment
            #variant_name
        }
    });
    let state_enum_doc_comment = generate_rust_doc_comment(&states_block.annotations); // Use annotations from states block
    let state_enum = quote! {
        #state_enum_doc_comment
        // Add Eq, Hash back if no Float types are used in practice or handled
        #[derive(Debug, Clone, PartialEq, Eq, Hash)]
        pub enum #state_enum_name {
            #(#state_variants),*
        }
    }; // <-- Semicolon added here

    // Event enum and payload struct generation (Commented out)
    let event_derive_tokens = quote! { #[derive(Debug, Clone, PartialEq)] };
    // let event_defs = generate_event_enum_and_structs(machine_ast, &event_derive_tokens)?; // Commented out call
    let event_defs = quote! {
         // TODO: Define Event enum properly based on transition definitions
         // Placeholder Event enum
         #[derive(Debug, Clone, PartialEq, Eq, Hash)]
         pub enum #event_enum_name {
             // Extract event names from transitions
             // Example: Event1,
             // Example: Event2,
         }
    }; // Placeholder

    // Machine struct definition (Context)
    let machine_struct_doc_comment = generate_rust_doc_comment(&machine_ast.annotations);
    let context_fields = if let Some(context_def) = &machine_ast.context {
        let fields_results: Result<Vec<TokenStream>, CodegenError> = context_def
            .fields
            .iter()
            .map(|field| {
                let field_name = format_ident!("{}", field.name.name);
                let field_type_tokens = map_type_specifier_to_rust_type(&field.type_spec)?;
                let field_doc_comment = generate_rust_doc_comment(&field.annotations);
                Ok(quote! {
                    #field_doc_comment
                    pub #field_name: #field_type_tokens
                })
            })
            .collect();
        fields_results?
    } else {
        Vec::new()
    };

    let machine_struct = quote! {
        #machine_struct_doc_comment
        // Use PartialEq only if context fields might contain non-Eq types
        #[derive(Debug, Clone, PartialEq)]
        pub struct #machine_struct_name {
            pub current_state: #state_enum_name,
            #(#context_fields),*,
            // Use Box<dyn Trait> for callbacks
            callbacks: Box<dyn #callbacks_trait_name>,
        }
    };

    // --- Callback Trait Generation --- (Refactored)
    let mut guards: HashSet<String> = HashSet::new();
    let mut actions: HashSet<String> = HashSet::new();
    let mut entry_actions: HashSet<String> = HashSet::new(); // TODO: Get from state annotations/elements
    let mut exit_actions: HashSet<String> = HashSet::new(); // TODO: Get from state annotations/elements
    let mut callback_signatures: Vec<TokenStream> = Vec::new();

    // TODO: Revisit event handling when events are properly defined in AST
    /* // Simplified event mapping for now
    let mut callback_event_map: HashMap<
        String, // Use simple name string as key
        Vec<fsm_dsl::ast::Identifier>, // Placeholder: Store event Identifier name
    > = HashMap::new();
    */

    for state_def in &states_block.states {
        // TODO: Collect entry/exit actions from state definition when AST supports it
        // for entry_action in &state_def.entry_actions { entry_actions.insert(entry_action.name.clone()); }
        // for exit_action in &state_def.exit_actions { exit_actions.insert(exit_action.name.clone()); }

        for transition in &state_def.transitions {
            //let event_name = &transition.event.name;
            for guard_ident in &transition.guards {
                guards.insert(guard_ident.name.clone());
                // callback_event_map.entry(guard_name).or_default().push(transition.event.clone());
            }
            for action_ident in &transition.actions {
                actions.insert(action_ident.name.clone());
                // callback_event_map.entry(action_name).or_default().push(transition.event.clone());
            }
        }
    }

    // Generate guard signatures
    for guard_name in &guards {
        let guard_fn_ident = format_ident!("{}", guard_name);
        // TODO: Refine event signature generation based on actual event definitions
        let event_type_sig = quote! { &#event_enum_name }; // Placeholder

        callback_signatures.push(quote! {
            fn #guard_fn_ident(&self, state: &#state_enum_name, event: #event_type_sig) -> bool;
        });
    }

    // Generate action signatures (transition actions)
    for action_name in &actions {
        let action_fn_ident = format_ident!("{}", action_name);
        let event_type_sig = quote! { &#event_enum_name }; // Placeholder

        callback_signatures.push(quote! {
             fn #action_fn_ident(&mut self, event: #event_type_sig); // Actions likely need mut self
        });
    }

    // Generate entry action signatures
    for entry_fn_name in &entry_actions {
        let entry_fn_ident = format_ident!("{}", entry_fn_name);
        callback_signatures.push(quote! {
            fn #entry_fn_ident(&mut self);
        });
    }

    // Generate exit action signatures
    for exit_fn_name in &exit_actions {
        let exit_fn_ident = format_ident!("{}", exit_fn_name);
        callback_signatures.push(quote! {
            fn #exit_fn_ident(&mut self);
        });
    }

    let callbacks_trait_doc_comment = generate_rust_doc_comment(&machine_ast.annotations); // Use machine annotations for trait doc
    let callbacks_trait = if !guards.is_empty()
        || !actions.is_empty()
        || !entry_actions.is_empty()
        || !exit_actions.is_empty()
    {
        quote! {
            #callbacks_trait_doc_comment
            /// Trait defining the required guard, action, entry, and exit callbacks for the state machine.
            pub trait #callbacks_trait_name {
                #(#callback_signatures)*
            }
        }
    } else {
        // Generate an empty trait if no callbacks defined, required by new() signature
        quote! {
            #callbacks_trait_doc_comment
            /// Placeholder trait as no guards or actions were defined.
            pub trait #callbacks_trait_name {}
            // Implement the trait for any type that might be boxed
            // This allows Box<dyn Trait> to be created even if the trait is empty.
            impl<T: ?Sized> #callbacks_trait_name for T {}
        }
    };

    // Impl block generation
    let impl_block = generate_impl_block_refactored(machine_ast, &callbacks_trait_name)?;

    // Default impl removed as it's problematic with Box<dyn Trait>
    let default_impl = quote! {};

    // Combine all parts
    let combined_code = quote! {
        #state_enum
        #event_defs // Placeholder
        #machine_struct
        #callbacks_trait // Add the trait definition
        #impl_block
        #default_impl // Removed default impl
    };

    // Format the generated code
    let code_str = combined_code.to_string();
    match syn::parse_file(&code_str) {
        Ok(syntax_tree) => Ok(prettyplease::unparse(&syntax_tree)),
        Err(e) => {
            eprintln!("--- Failed to parse generated code ---");
            eprintln!("{}", code_str);
            eprintln!("--- End generated code --- Error: {} ---", e);
            Err(CodegenError::SynParseError(e, code_str))
        }
    }
}

// Helper function to determine the most specific event type signature for a callback
// TODO: Refactor this when Event definitions are back in AST
/*
fn determine_callback_event_signature<'a>(
    event_defs: Option<&'a Vec<&'a fsm_dsl::ast::MessageItem>>,
    event_enum_name: &TokenIdent,
) -> TokenStream {
    // ... (Existing logic needs update for new Event AST structure) ...
    quote! { &#event_enum_name } // Fallback
}
*/

// --- Refactored generate_impl_block ---
// Renamed to avoid conflict during refactoring
fn generate_impl_block_refactored(
    machine_ast: &MachineDefinition,
    callbacks_trait_name: &TokenIdent,
) -> Result<TokenStream, CodegenError> {
    let machine_struct_name = format_ident!("{}", machine_ast.name.name);
    let state_enum_name = format_ident!("State");
    let event_enum_name = format_ident!("Event"); // Assumes Event enum exists

    let states_block = machine_ast.states.as_ref().ok_or_else(|| {
        CodegenError::AstValidationError("Machine definition requires a 'states' block".to_string())
    })?;

    // Find initial state from annotation
    let initial_state_ident = machine_ast
        .annotations
        .iter()
        .find_map(|anno| match anno {
            Annotation::GenericKeyValue(key, value_str) if key.name == "initial" => {
                Some(format_ident!("{}", value_str))
            }
            _ => None,
        })
        .ok_or_else(|| {
            CodegenError::AstValidationError(
                "Missing or invalid '$initial(StateName)' annotation on machine.".to_string(),
            )
        })?;

    // Check if initial state exists
    if !states_block
        .states
        .iter()
        .any(|s| s.name.name == initial_state_ident.to_string())
    {
        return Err(CodegenError::AstValidationError(format!(
            "Initial state '{}' defined in $initial annotation is not a declared state.",
            initial_state_ident
        )));
    }
    let initial_state_assignment = quote! { #state_enum_name::#initial_state_ident };

    // Context fields initialization for new()
    let context_fields_init = if let Some(context_def) = &machine_ast.context {
        context_def.fields.iter().map(|field| {
            let field_name = format_ident!("{}", field.name.name);
            // TODO: Get default value from annotation $default or use Default::default()
            // This requires parsing the default value annotation
            // Check for $default annotation
            let default_value_annotation = field.annotations.iter().find_map(|a| match a {
                Annotation::GenericKeyValue(key, value) if key.name == "default" => Some(value.clone()), // Clone the string value
                _ => None
            });

            // Attempt to parse the default value based on type (basic implementation)
            let init_expr = match default_value_annotation {
                Some(val_str) => {
                     match &field.type_spec {
                        TypeSpecifier::Simple(id) if id.name == "string" || id.name == "text" => quote!{ #val_str.to_string() },
                        TypeSpecifier::Simple(id) if id.name == "int" || id.name == "i32" || id.name == "i64" /* add others */ => {
                            // Parse as i64 for flexibility, then cast if needed or handle error
                            match val_str.parse::<i64>() {
                                Ok(v) => quote!{ #v },
                                Err(_) => quote!{ Default::default() } // Fallback
                            }
                        },
                         TypeSpecifier::Simple(id) if id.name == "bool" => {
                             match val_str.to_lowercase().as_str() {
                                 "true" => quote!{ true },
                                 _ => quote!{ false }
                             }
                         },
                         // Add more type handling for defaults here...
                         _ => quote!{ Default::default() } // Default fallback
                    }
                }
                None => quote! { Default::default() } // Use Default trait if no $default annotation
            };

            quote! { #field_name: #init_expr }
        }).collect::<Vec<_>>()
    } else {
        Vec::new()
    };

    // Generate match arms for on_event
    let state_enum_name_clone = state_enum_name.clone(); // Clone for closure
    let event_enum_name_clone = event_enum_name.clone(); // Clone for closure
    let on_event_match_arms: Result<Vec<TokenStream>, CodegenError> = states_block.states
        .iter()
        .flat_map(move |state| { // Add move here
            let current_state_ident = format_ident!("{}", state.name.name);
            // Clone again for the inner closure if needed, or rely on the outer move
            let state_enum_name_inner = state_enum_name_clone.clone();
            let event_enum_name_inner = event_enum_name_clone.clone();
            state.transitions.iter().map(move |transition| { // Add move here
                let event_ident = format_ident!("{}", transition.event.name);
                // Extract target state name, handling non-state targets if needed
                let target_state_ident = match &transition.target {
                    TransitionTarget::State(ident) => format_ident!("{}", ident.name),
                    // Handle other target types (e.g., history) if necessary
                    // For now, we might panic or return an error, or use a placeholder.
                    // Let's use a placeholder that will likely cause a compile error
                    // if used incorrectly, prompting proper handling later.
                    _ => format_ident!("__INVALID_TARGET__"),
                };

                // Generate guard checks
                let guard_checks = transition.guards.iter().map(|guard_ident| {
                    let guard_fn_ident = format_ident!("{}", guard_ident.name);
                    quote! {
                        if !self.callbacks.#guard_fn_ident(&self.current_state, &event) {
                            println!("Guard '{}' failed for event {:?} in state {:?}.", stringify!(#guard_fn_ident), event, self.current_state);
                            return Ok(self); // Guard failed, return unchanged self
                        }
                    }
                });

                // Generate action calls
                let action_calls = transition.actions.iter().map(|action_ident| {
                    let action_fn_ident = format_ident!("{}", action_ident.name);
                    quote! {
                        next_state_machine.callbacks.#action_fn_ident(&event);
                    }
                });

                 let exit_action_calls = quote! { /* TODO: Implement state exit actions */ };
                 let entry_action_calls = quote! { /* TODO: Implement state entry actions */ };

                // Use cloned idents for the event pattern
                let event_pattern = quote! { #event_enum_name_inner::#event_ident };

                Ok(quote! {
                    // Use cloned state enum ident here
                    (#state_enum_name_inner::#current_state_ident, #event_pattern) => {
                        println!("Evaluating transition: {:?} -> {:?} on event {:?}",
                            #state_enum_name_inner::#current_state_ident, #state_enum_name_inner::#target_state_ident, event);
                        #(#guard_checks)*
                        println!("Passed guards for {:?} -> {:?}", #state_enum_name_inner::#current_state_ident, #state_enum_name_inner::#target_state_ident);
                        let mut next_state_machine = self.clone();
                        println!("Executing exit actions for {:?}", next_state_machine.current_state);
                        #exit_action_calls
                        println!("Executing transition actions for {:?} -> {:?}", #state_enum_name_inner::#current_state_ident, #state_enum_name_inner::#target_state_ident);
                        #(#action_calls)*
                         println!("Changing state: {:?} -> {:?}", next_state_machine.current_state, #state_enum_name_inner::#target_state_ident);
                        next_state_machine.current_state = #state_enum_name_inner::#target_state_ident;
                         println!("Executing entry actions for {:?}", next_state_machine.current_state);
                        #entry_action_calls
                        Ok(next_state_machine)
                    }
                })
            })
        })
        .collect();

    let on_event_match_arms = on_event_match_arms?;

    Ok(quote! {
        impl #machine_struct_name {
            pub fn new(callbacks: Box<dyn #callbacks_trait_name>) -> Self {
                Self {
                    current_state: #initial_state_assignment,
                    callbacks,
                    #(#context_fields_init),*
                }
            }

            pub fn current_state(&self) -> &#state_enum_name {
                 &self.current_state
            }

            /// Processes an event and attempts to transition the state machine.
            /// Returns the new state machine instance if a transition occurred.
            /// Returns the original state machine instance `Ok(self)` if a guard prevents the transition or no transition is defined.
            /// Returns an `Err` for internal errors (should not happen with validated AST).
            // TODO: Define Event enum properly before using it here
            pub fn on_event(self, event: /* TODO: Define and use */ #event_enum_name) -> Result<Self, String> { // Takes ownership
                // TODO: Clone only if needed for callbacks?
                // let event_clone = event.clone(); // Clone event if needed later
                match (&self.current_state, &event) {
                    #(#on_event_match_arms)*
                    // Catch-all for unhandled state/event combinations
                    _ => {
                        println!("No transition defined for event {:?} in state {:?}", event, self.current_state);
                        Ok(self) // Return self unchanged
                    }
                }
            }
        }
    })
}

/// Generates a Cap'n Proto schema (.capnp) from the FSM AST.
///
/// * `file_ast` - The parsed `SsotFile` structure (needed for file ID).
/// * `machine_ast` - The specific `StateMachine` structure to generate the schema for.
///
/// # Returns
///
/// A `Result` containing the Cap'n Proto schema string or a `CodegenError`.
pub fn generate_capnp_schema(ast: &SsotAst) -> Result<String, CodegenError> {
    codegen_capnp::generate_capnp_schema_internal(ast)
}

/// Generates PlantUML diagram from the FSM AST.
///
/// * `ast` - The parsed `SsotAst` structure.
///
/// # Returns
///
/// A `Result` containing the PlantUML diagram string or a `CodegenError`.
pub fn generate_plantuml(ast: &SsotAst) -> Result<String, CodegenError> {
    // codegen_plantuml::generate_plantuml_internal(ast)
    Err(CodegenError::NotImplemented("PlantUML generation".to_string()))
}

/// Generates TypeScript types from the FSM AST.
///
/// * `ast` - The parsed `SsotAst` structure.
///
/// # Returns
///
/// A `Result` containing the TypeScript type definition string or a `CodegenError`.
pub fn generate_typescript_types(ast: &SsotAst) -> Result<String, CodegenError> {
    // TODO: Implement or call the actual TS generation logic
    // codegen_ts::generate_ts_types_internal(ast)
    Err(CodegenError::NotImplemented(
        "TypeScript generation".to_string(),
    ))
}

/// Generates an XState machine configuration from the FSM AST.
///
/// * `ast` - The parsed `SsotAst` structure.
///
/// # Returns
///
/// A `Result` containing the generated XState machine configuration string or a `CodegenError`.
pub fn generate_xstate_machine(ast: &SsotAst) -> Result<String, CodegenError> {
    // TODO: Implement or call the actual XState generation logic
    // codegen_xstate::generate_xstate_machine_internal(ast)
    Err(CodegenError::NotImplemented(
        "XState generation".to_string(),
    ))
}

/// Generates an SCXML document from the FSM AST.
///
/// * `ast` - The parsed `SsotAst` structure.
///
/// # Returns
///
/// A `Result` containing the generated SCXML document string or a `CodegenError`.
pub fn generate_scxml(ast: &SsotAst) -> Result<String, CodegenError> {
    // TODO: Implement or call the actual SCXML generation logic
    // codegen_scxml::generate_scxml_internal(ast)
    Err(CodegenError::NotImplemented("SCXML generation".to_string()))
}

pub fn generate_code(ast: &SsotAst, format: &str) -> Result<String, CodegenError> {
    match format {
        "capnp" => generate_capnp_schema(ast),
        "plantuml" => generate_plantuml(ast),
        "scxml" => generate_scxml(ast),
        "typescript" => generate_typescript_types(ast), // Assuming you want types, not machine
        "xstate" => generate_xstate_machine(ast),
        _ => Err(CodegenError::UnsupportedFormat(format.to_string())),
    }
}

// --- Unit Tests ---
#[cfg(test)]
mod tests {
    use super::*; // Import items from parent module (including helpers)
    use fsm_dsl::ast::{
        // Import corrected AST types
        Annotation,
        AnnotationValue,
        FieldDef,
        FieldType,
        MessageItem,
        QualifiedIdent,
        StateItem,
        StateMachine,
        TopLevelItem,
        TransitionElement,
        TransitionItem,
    };
    use pretty_assertions::assert_eq;
    use quote::quote;
    use syn::parse_file as syn_parse_file; // Alias for clarity

    // Helper function to create identifiers for tests
    fn ident(s: &str) -> TokenIdent {
        TokenIdent::new(s, proc_macro2::Span::call_site())
    }

    // Helper to create a basic StateMachine AST for testing
    fn create_test_ast() -> StateMachine {
        StateMachine {
            name: ident("TestMachine"),
            annotations: vec![Annotation {
                name: ident("initial"),
                value: Some(AnnotationValue::Identifier(ident("Idle"))), // Wrap in Some()
            }],
            events: vec![
                MessageItem {
                    annotations: vec![], // Ensure annotations is present
                    name: ident("Start"),
                    ordinal: 0, // Added ordinal
                    fields: vec![],
                },
                MessageItem {
                    annotations: vec![], // Ensure annotations is present
                    name: ident("Stop"),
                    ordinal: 1, // Added ordinal
                    fields: vec![],
                },
                MessageItem {
                    annotations: vec![], // Ensure annotations is present
                    name: ident("Update"),
                    ordinal: 2, // Added ordinal
                    fields: vec![FieldDef {
                        annotations: vec![], // Ensure annotations is present
                        name: ident("value"),
                        ordinal: Some(0), // Wrap in Some()
                        field_type: FieldType::Int32,
                    }],
                },
            ],
            states: vec![
                StateItem {
                    annotations: vec![], // Ensure annotations is present
                    name: ident("Idle"),
                    ordinal: 0,            // Added ordinal
                    entry_actions: vec![], // Ensure actions is present
                    exit_actions: vec![],  // Ensure actions is present
                },
                StateItem {
                    annotations: vec![], // Ensure annotations is present
                    name: ident("Running"),
                    ordinal: 1,            // Added ordinal
                    entry_actions: vec![], // Ensure actions is present
                    exit_actions: vec![],  // Ensure actions is present
                },
            ],
            transitions: vec![
                TransitionItem {
                    name: Some(ident("transition1")), // Wrap in Some()
                    from: ident("Idle"),
                    to: ident("Running"),
                    elements: vec![
                        TransitionElement::On {
                            ordinal: 1,                                    // Changed from order
                            event: QualifiedIdent::Simple(ident("Start")), // Wrap in Simple
                        },
                        TransitionElement::Action {
                            ordinal: 2,                                                 // Changed from order
                            function: QualifiedIdent::Simple(ident("on_start_action")), // Wrap in Simple
                        },
                    ],
                    annotations: vec![], // Ensure annotations is present
                },
                TransitionItem {
                    name: Some(ident("transition2")), // Wrap in Some()
                    from: ident("Running"),
                    to: ident("Idle"),
                    elements: vec![
                        TransitionElement::On {
                            ordinal: 1,                                   // Changed from order
                            event: QualifiedIdent::Simple(ident("Stop")), // Wrap in Simple
                        },
                        TransitionElement::Guard {
                            ordinal: 2,                                                // Changed from order
                            function: QualifiedIdent::Simple(ident("can_stop_guard")), // Wrap in Simple
                        },
                    ],
                    annotations: vec![], // Ensure annotations is present
                },
                TransitionItem {
                    name: Some(ident("transition3")), // Wrap in Some()
                    from: ident("Running"),
                    to: ident("Running"), // Self-transition
                    elements: vec![TransitionElement::On {
                        ordinal: 1,                                     // Changed from order
                        event: QualifiedIdent::Simple(ident("Update")), // Wrap in Simple
                    }],
                    annotations: vec![], // Ensure annotations is present
                },
            ],
            context: vec![],          // Ensure context is present
            use_declarations: vec![], // Ensure use_declarations is present
        }
    }

    // Helper to parse and format code for comparison
    #[allow(dead_code)]
    fn parse_and_format(code: &str) -> String {
        let syntax_tree: syn::File = syn::parse_str(code).expect("Failed to parse generated code");
        prettyplease::unparse(&syntax_tree)
    }

    #[test]
    fn generates_basic_structures() {
        let input = create_test_ast();
        let result = generate_rust_code(&input);

        assert!(result.is_ok());
        let generated_code = result.unwrap();
        let parsed_generated =
            syn_parse_file(&generated_code).expect("Parsing generated code failed");

        // Check for State enum
        let state_enum = parsed_generated.items.iter().find_map(|item| match item {
            syn::Item::Enum(e) if e.ident == "State" => Some(e),
            _ => None,
        });
        assert!(state_enum.is_some(), "State enum not generated");
        assert_eq!(state_enum.unwrap().variants.len(), 2); // Idle, Running

        // Check for Event enum and structs
        let event_enum = parsed_generated.items.iter().find_map(|item| match item {
            syn::Item::Enum(e) if e.ident == "Event" => Some(e),
            _ => None,
        });
        assert!(event_enum.is_some(), "Event enum not generated");
        assert_eq!(event_enum.unwrap().variants.len(), 3); // Start, Stop, Update

        // Check for Update event struct
        let update_struct = parsed_generated.items.iter().find_map(|item| match item {
            syn::Item::Struct(s) if s.ident == "UpdateEventPayload" => Some(s), // Look for struct named "UpdateEventPayload"
            _ => None,
        });
        assert!(
            update_struct.is_some(),
            "Event struct for Update not generated"
        );

        // Check for Machine struct
        let machine_struct = parsed_generated.items.iter().find_map(|item| match item {
            syn::Item::Struct(s) if s.ident == "TestMachine" => Some(s),
            _ => None,
        });
        assert!(machine_struct.is_some(), "TestMachine struct not generated");
    }

    #[test]
    fn generates_impl_block_and_new() {
        let input = create_test_ast();
        let result = generate_rust_code(&input);
        assert!(result.is_ok());

        let generated_code = result.unwrap();
        let parsed_generated =
            syn_parse_file(&generated_code).expect("Parsing generated code failed");

        // Find the impl block for TestMachine
        let impl_block = parsed_generated.items.iter().find_map(|item| match item {
            syn::Item::Impl(imp) => {
                if let syn::Type::Path(type_path) = &*imp.self_ty {
                    if type_path
                        .path
                        .segments
                        .last()
                        .is_some_and(|seg| seg.ident == "TestMachine")
                    {
                        Some(imp)
                    } else {
                        None
                    }
                } else {
                    None
                }
            }
            _ => None,
        });
        assert!(impl_block.is_some(), "impl TestMachine block not generated");

        // Find the new function within the impl block
        let new_fn = impl_block
            .unwrap()
            .items
            .iter()
            .find_map(|item| match item {
                syn::ImplItem::Fn(func) if func.sig.ident == "new" => Some(func),
                _ => None,
            });
        assert!(
            new_fn.is_some(),
            "'new' function not generated in impl block"
        );

        // Very basic check - could parse the body and check assignment if needed
        assert!(quote!(#new_fn)
            .to_string()
            .contains("current_state : State :: Idle"));
    }

    #[test]
    fn generates_on_event_method_with_transitions() {
        let input = create_test_ast();
        let result = generate_rust_code(&input);
        assert!(result.is_ok());

        let generated_code = result.unwrap();
        let parsed_generated =
            syn_parse_file(&generated_code).expect("Parsing generated code failed");

        // Find the impl block for TestMachine
        let impl_block = parsed_generated.items.iter().find_map(|item| match item {
            syn::Item::Impl(imp) => {
                if let syn::Type::Path(type_path) = &*imp.self_ty {
                    if type_path
                        .path
                        .segments
                        .last()
                        .is_some_and(|seg| seg.ident == "TestMachine")
                    {
                        Some(imp)
                    } else {
                        None
                    }
                } else {
                    None
                }
            }
            _ => None,
        });
        assert!(impl_block.is_some(), "impl TestMachine block not generated");

        // Find the on_event function
        let on_event_fn = impl_block
            .unwrap()
            .items
            .iter()
            .find_map(|item| match item {
                syn::ImplItem::Fn(func) if func.sig.ident == "on_event" => Some(func),
                _ => None,
            });
        assert!(on_event_fn.is_some(), "'on_event' function not generated");

        // Basic check for expected match arms (can be made more robust)
        let fn_body_str = quote!(#on_event_fn).to_string();
        assert!(fn_body_str.contains("(State :: Idle , Event :: Start)"));
        assert!(fn_body_str.contains("State :: Running")); // Target state for Idle -> Start
        assert!(fn_body_str.contains("(State :: Running , Event :: Stop)"));
        assert!(fn_body_str.contains("State :: Idle")); // Target state for Running -> Stop
        assert!(fn_body_str.contains("(State :: Running , Event :: Update (payload))")); // Check payload binding
        assert!(fn_body_str.contains("State :: Running")); // Target state for Running -> Update (self)
    }

    #[test]
    fn generates_guard_and_action_placeholders_module() {
        let input = create_test_ast();
        let result = generate_rust_code(&input);
        assert!(result.is_ok());

        let generated_code = result.unwrap();
        let parsed_generated =
            syn_parse_file(&generated_code).expect("Parsing generated code failed");

        // Find the machine_callbacks module
        let callbacks_mod = parsed_generated.items.iter().find_map(|item| match item {
            syn::Item::Mod(m) if m.ident == "machine_callbacks" => Some(m),
            _ => None,
        });
        assert!(
            callbacks_mod.is_none(),
            "machine_callbacks module not generated"
        );
    }

    #[test]
    fn handles_no_transitions_or_callbacks() {
        let mut input = create_test_ast();
        input.transitions.clear(); // Remove all transitions

        let result = generate_rust_code(&input);
        assert!(result.is_ok());

        let generated_code = result.unwrap();
        let parsed_generated =
            syn_parse_file(&generated_code).expect("Parsing generated code failed");

        // Find the impl block for TestMachine
        let impl_block = parsed_generated.items.iter().find_map(|item| match item {
            syn::Item::Impl(imp) => {
                if let syn::Type::Path(type_path) = &*imp.self_ty {
                    if type_path
                        .path
                        .segments
                        .last()
                        .is_some_and(|seg| seg.ident == "TestMachine")
                    {
                        Some(imp)
                    } else {
                        None
                    }
                } else {
                    None
                }
            }
            _ => None,
        });
        assert!(impl_block.is_some(), "impl TestMachine block not generated");

        // Check if on_event exists and has a default arm
        let on_event_fn = impl_block
            .unwrap()
            .items
            .iter()
            .find_map(|item| match item {
                syn::ImplItem::Fn(func) if func.sig.ident == "on_event" => Some(func),
                _ => None,
            });
        assert!(on_event_fn.is_some(), "'on_event' function not generated");
        assert!(quote!(#on_event_fn)
            .to_string()
            .contains("_ => Ok (self . clone ()) "));

        // Check that machine_callbacks module is NOT generated
        let callbacks_mod = parsed_generated.items.iter().find_map(|item| match item {
            syn::Item::Mod(m) if m.ident == "machine_callbacks" => Some(m),
            _ => None,
        });
        assert!(
            callbacks_mod.is_none(),
            "machine_callbacks module generated unexpectedly"
        );
    }

    #[test]
    fn generates_current_state_getter() {
        let input = create_test_ast();
        let result = generate_rust_code(&input);
        assert!(result.is_ok());

        let generated_code = result.unwrap();
        let parsed_generated =
            syn_parse_file(&generated_code).expect("Parsing generated code failed");

        // Find the impl block for TestMachine
        let impl_block = parsed_generated.items.iter().find_map(|item| match item {
            syn::Item::Impl(imp) => {
                if let syn::Type::Path(type_path) = &*imp.self_ty {
                    if type_path
                        .path
                        .segments
                        .last()
                        .is_some_and(|seg| seg.ident == "TestMachine")
                    {
                        Some(imp)
                    } else {
                        None
                    }
                } else {
                    None
                }
            }
            _ => None,
        });
        assert!(impl_block.is_some(), "impl TestMachine block not generated");

        // Find the current_state function
        let current_state_fn = impl_block
            .unwrap()
            .items
            .iter()
            .find_map(|item| match item {
                syn::ImplItem::Fn(func) if func.sig.ident == "current_state" => Some(func),
                _ => None,
            });
        assert!(
            current_state_fn.is_some(),
            "'current_state' getter function not generated"
        );
        // Basic check of signature and body
        let expected_ret_type = quote! { -> &State }.to_string();
        let actual_output = &current_state_fn.unwrap().sig.output;
        let actual_ret_type = quote! { #actual_output }.to_string();
        assert_eq!(
            actual_ret_type, expected_ret_type,
            "Getter return type mismatch"
        );
        assert!(quote!(#current_state_fn)
            .to_string()
            .contains("& self . current_state"));
    }

    #[test]
    fn generates_placeholder_capnp() {
        let ast = create_annotated_test_ast(); // Use annotated AST for more coverage
        let file_ast = fsm_dsl::ast::SsotFile {
            // Create a dummy SsotFile for the test
            file_id: 0x123456789ABCDEF0,
            package_declaration: None,
            imports: vec![], // Ensure imports is present
            items: vec![TopLevelItem::StateMachine(ast.clone())], // Use 'items' field
        };
        let result = crate::generate_capnp_schema(&file_ast); // Pass both args
        assert!(
            result.is_ok(),
            "Cap'n Proto generation failed: {:?}",
            result.err()
        );
        let schema = result.unwrap();
        // Basic check for placeholder content -> Updated checks
        assert!(schema.contains("@0x123456789abcdef0")); // Check for file ID format
        assert!(schema.contains("enum State @0 {"));
        assert!(schema.contains("idle @0;")); // From create_test_ast
        assert!(schema.contains("running @1;")); // From create_test_ast
        assert!(schema.contains("struct StartPayload @0 {")); // Event with payload
        assert!(schema.contains("userId @0 :UInt64;")); // Field in payload
        assert!(schema.contains("union Event @1 {"));
        assert!(schema.contains("start @0 :StartPayload;")); // Event with payload
        assert!(schema.contains("stop @1 :Void;")); // Event without payload
        println!(
            "--- Generated Cap'n Proto Schema ---
{}",
            schema
        ); // For inspection
    }

    #[test]
    fn generates_placeholder_typescript() {
        let ast = create_test_ast();
        let generated = generate_typescript_types(&ast).unwrap();
        assert!(generated.contains("export type State"));
        assert!(generated.contains("export type Event"));
    }

    // Test case for AST with annotations
    fn create_annotated_test_ast() -> StateMachine {
        StateMachine {
            name: ident("AnnotatedMachine"),
            annotations: vec![
                Annotation {
                    name: ident("description"),
                    value: Some(AnnotationValue::StringLiteral(
                        "This is the main machine.".to_string(),
                    )),
                },
                Annotation {
                    // Keep initial for functionality test
                    name: ident("initial"),
                    value: Some(AnnotationValue::Identifier(ident("Idle"))),
                },
            ],
            events: vec![
                MessageItem {
                    annotations: vec![Annotation {
                        // Annotation on event
                        name: ident("description"),
                        value: Some(AnnotationValue::StringLiteral(
                            "Starts the machine.".to_string(),
                        )),
                    }],
                    name: ident("Start"),
                    ordinal: 0,
                    fields: vec![FieldDef {
                        annotations: vec![Annotation {
                            // Annotation on field
                            name: ident("description"),
                            value: Some(AnnotationValue::StringLiteral("The user ID.".to_string())),
                        }],
                        name: ident("userId"),
                        ordinal: Some(0), // Wrap in Some()
                        field_type: FieldType::UInt64,
                    }],
                },
                MessageItem {
                    annotations: vec![Annotation {
                        // Annotation on event
                        name: ident("description"),
                        value: Some(AnnotationValue::StringLiteral(
                            "Stops the machine.".to_string(),
                        )),
                    }],
                    name: ident("Stop"),
                    ordinal: 1,
                    fields: vec![],
                },
            ],
            states: vec![
                StateItem {
                    annotations: vec![Annotation {
                        // Annotation on state
                        name: ident("description"),
                        value: Some(AnnotationValue::StringLiteral("Waiting state.".to_string())),
                    }],
                    name: ident("Idle"),
                    ordinal: 0,
                    entry_actions: vec![], // Ensure actions is present
                    exit_actions: vec![],  // Ensure actions is present
                },
                StateItem {
                    annotations: vec![Annotation {
                        // Annotation on state
                        name: ident("description"),
                        value: Some(AnnotationValue::StringLiteral("Active state.".to_string())),
                    }],
                    name: ident("Running"),
                    ordinal: 1,
                    entry_actions: vec![], // Ensure actions is present
                    exit_actions: vec![],  // Ensure actions is present
                },
            ],
            transitions: vec![
                TransitionItem {
                    name: None,
                    from: ident("Idle"),
                    to: ident("Running"),
                    elements: vec![TransitionElement::On {
                        ordinal: 0,
                        event: QualifiedIdent::Simple(ident("Start")), // Wrap in Simple
                    }],
                    annotations: vec![], // Ensure annotations is present
                },
                TransitionItem {
                    name: None,
                    from: ident("Running"),
                    to: ident("Idle"),
                    elements: vec![TransitionElement::On {
                        ordinal: 1,
                        event: QualifiedIdent::Simple(ident("Stop")), // Wrap in Simple
                    }],
                    annotations: vec![], // Ensure annotations is present
                },
            ],
            context: vec![],          // Ensure context is present
            use_declarations: vec![], // Ensure use_declarations is present
        }
    }

    #[test]
    fn generates_rust_code_with_doc_comments() {
        let ast = create_annotated_test_ast();
        let result = generate_rust_code(&ast);
        assert!(result.is_ok(), "Rust generation failed: {:?}", result.err());
        let code = result.unwrap();
        println!("--- Generated Rust Code with Docs ---\n{}", code); // For inspection

        // Check for struct/enum docs
        assert!(
            code.contains("#[doc = \"This is the main machine.\"]\npub struct AnnotatedMachine")
        );
        assert!(code.contains("#[doc = \"Waiting state.\"]\n    Idle,"));
        assert!(code.contains("#[doc = \"Active state.\"]\n    Running,"));
        assert!(code.contains("#[doc = \"Starts the machine.\"]\npub struct StartEventPayload"));
        assert!(code.contains("#[doc = \"Starts the machine.\"]\n    Start(StartEventPayload),"));
        assert!(code.contains("#[doc = \"Stops the machine.\"]\n    Stop,"));

        // Check for field docs
        assert!(code.contains("#[doc = \"The user ID.\"]\n        pub userId: u64"));
    }

    #[test]
    fn generates_capnp_schema_with_comments() {
        let machine_ast = create_annotated_test_ast();
        let file_ast = fsm_dsl::ast::SsotFile {
            file_id: 0xdeadbeefcafe0001,
            package_declaration: None,
            imports: vec![], // Ensure imports is present
            items: vec![TopLevelItem::StateMachine(machine_ast.clone())], // Use 'items' field
        };
        let result = generate_capnp_schema(&file_ast);
        assert!(
            result.is_ok(),
            "Capnp generation failed: {:?}",
            result.err()
        );
        let schema = result.unwrap();
        println!("--- Generated Capnp Schema with Comments ---\n{}", schema); // For inspection

        // Check for comments
        assert!(schema.contains("# This is the main machine.\n# Cap'n Proto schema generated")); // Machine comment
        assert!(schema.contains("# Waiting state.\n  Idle @0;")); // State comment
        assert!(schema.contains("# Active state.\n  Running @1;")); // State comment
        assert!(schema.contains("# Starts the machine.\nstruct StartPayload @2 {")); // Struct comment
        assert!(schema.contains("# The user ID.\n    userId @0 :UInt64;")); // Field comment
        assert!(schema.contains("# Starts the machine.\n  Start @0 :StartPayload;")); // Union member comment (from event)
        assert!(schema.contains("# Stops the machine.\n  Stop @1 :Void;")); // Union member comment (from event)
    }

    #[test]
    fn generates_typescript_types_with_jsdoc() {
        let ast = create_annotated_test_ast();
        let result = generate_typescript_types(&ast);
        assert!(
            result.is_ok(),
            "TypeScript generation failed: {:?}",
            result.err()
        );
        let types = result.unwrap();
        println!("--- Generated TypeScript Types with JSDoc ---\n{}", types); // For inspection

        // Check for JSDoc
        assert!(types.contains("/**\n * This is the main machine.\n */")); // Machine comment
        assert!(types.contains("/**\n * Waiting state.\n */\n  | \"Idle\"")); // State comment
        assert!(types.contains("/**\n * Active state.\n */\n  | \"Running\"")); // State comment
        assert!(types.contains("/**\n * Starts the machine.\n */\nexport interface StartPayload {")); // Payload interface comment
        assert!(types.contains("/**\n   * The user ID.\n   */\n  userId: bigint;")); // Field comment (check indentation)
        assert!(types.contains(
            "/**\n * Starts the machine.\n */\n  | { type: \"Start\", payload: StartPayload }"
        )); // Event union comment
        assert!(types.contains("/**\n * Stops the machine.\n */\n  | { type: \"Stop\" }"));
        // Event union comment
    }
}
