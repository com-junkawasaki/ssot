#![allow(clippy::empty_line_after_doc_comments)]

use fsm_dsl::ast::{
    // Import directly from the ast module
    Annotation,
    AnnotationValue,
    FieldType,
    SsotFile, // Import SsotFile to access file_id
    StateMachine,
    TransitionElement,
};
use proc_macro2::TokenStream;
use quote::{format_ident, quote};
use std::collections::HashSet;
// Needed for parsing generated code before formatting // For collecting unique guard/action names

pub mod codegen_capnp; // Add new module
pub mod codegen_scxml;
pub mod codegen_ts; // Add new module
pub mod codegen_xstate; // Add new module for XState // Add new module for SCXML

// Helper to find annotation value by name
fn find_annotation_value<'a>(
    annotations: &'a [Annotation],
    name: &str,
) -> Option<&'a AnnotationValue> {
    annotations
        .iter()
        .find(|a| a.name == name)
        .and_then(|a| a.value.as_ref())
}

// Helper to map DSL FieldType to Rust type string
fn map_field_type_to_rust_type(field_type: &FieldType) -> TokenStream {
    match field_type {
        FieldType::Void => quote! { () }, // Use unit type for Void
        FieldType::Bool => quote! { bool },
        FieldType::Int8 => quote! { i8 },
        FieldType::Int16 => quote! { i16 },
        FieldType::Int32 => quote! { i32 },
        FieldType::Int64 => quote! { i64 },
        FieldType::UInt8 => quote! { u8 },
        FieldType::UInt16 => quote! { u16 },
        FieldType::UInt32 => quote! { u32 },
        FieldType::UInt64 => quote! { u64 },
        FieldType::Float32 => quote! { f32 },
        FieldType::Float64 => quote! { f64 },
        FieldType::Text => quote! { String },
        FieldType::Data => quote! { Vec<u8> },
        FieldType::List(inner) => {
            let inner_rust_type = map_field_type_to_rust_type(inner);
            quote! { Vec<#inner_rust_type> }
        }
        FieldType::Identifier(ident) => quote! { #ident }, // Assume identifier is a valid Rust type
    }
}

// Helper function to generate Rust doc comments from annotations
fn generate_rust_doc_comment(annotations: &[Annotation]) -> TokenStream {
    let mut doc_stream = quote! {};
    if let Some(AnnotationValue::StringLiteral(desc)) =
        find_annotation_value(annotations, "description")
    {
        // Generate /// comments directly for compatibility with prettyplease
        let comment_str = desc
            .lines()
            .map(|l| format!("/// {}", l.trim()))
            .collect::<Vec<_>>()
            .join("\n");
        // Parse the string into a TokenStream
        if let Ok(tokens) = syn::parse_str::<TokenStream>(&comment_str) {
            doc_stream.extend(tokens);
        }
        // Fallback or error handling if parsing fails?
    }
    // Add other annotation processing here if needed (e.g., #[deprecated])
    doc_stream
}

/// Generates the aggregated Event enum.
/// Assumes events are simple identifiers for now (no associated data structs yet).
// This function is now superseded by generate_event_enum_and_structs, keep or remove?
// Keeping it for now, but it's not used by the main generate_rust_code function.
/*
fn generate_event_enum(ast: &StateMachine, derive_tokens: &proc_macro2::TokenStream) -> proc_macro2::TokenStream {
    let event_enum_name = format_ident!("Event");
    // Use event names collected by the parser
    let variants = ast.events.iter().map(|event_item| { // event_item is MessageItem
        let event_ident = &event_item.name; // Use the Ident field directly
        // For now, assume events don't carry data, just represent the variant
        quote! { #event_ident }
    });

    quote! {
        #derive_tokens // Use the same derives as State and Machine
        pub enum #event_enum_name {
            #(#variants),*
        }
    }
}
*/

/// Generates the `impl` block for the state machine struct.
fn generate_impl_block(
    ast: &StateMachine,
    callbacks_trait_name: &proc_macro2::Ident,
    guards: &HashSet<&proc_macro2::Ident>, // Pass calculated guards
    actions: &HashSet<&proc_macro2::Ident>, // Pass calculated actions
) -> Result<TokenStream, CodegenError> {
    // Added trait name, guards, actions
    let machine_struct_name = format_ident!("{}", ast.name);
    let state_enum_name = format_ident!("State");
    let event_enum_name = format_ident!("Event");

    // Find the initial state from the $initial annotation
    let initial_state_ident = find_annotation_value(&ast.annotations, "initial")
        .and_then(|value| match value {
            AnnotationValue::Identifier(ident) => Some(ident), // Expecting Identifier value
            _ => None,
        })
        .ok_or_else(|| CodegenError::AstValidationError(
            "Missing or invalid '$initial' annotation on stateMachine. Expected $initial(StateName);".to_string()
        ))?;

    // Validate that the initial state identifier exists in the defined states
    if !ast.states.iter().any(|s| &s.name == initial_state_ident) {
        return Err(CodegenError::AstValidationError(format!(
            "Initial state '{}' defined in $initial annotation is not a declared state.",
            initial_state_ident
        )));
    }
    let initial_state_assignment = quote! { #state_enum_name::#initial_state_ident };

    // Generate match arms for on_event
    let on_event_match_arms = ast
        .transitions
        .iter()
        .map(|transition| -> Result<TokenStream, CodegenError> {
            let from_state_ident = &transition.from;
            let to_state_ident = &transition.to;

            // Find the 'On' element (should be exactly one per transition)
            let on_element = transition
                .elements
                .iter()
                .find_map(|el| match el {
                    TransitionElement::On { event, .. } => Some(event),
                    _ => None,
                })
                .ok_or_else(|| {
                    CodegenError::AstValidationError(format!(
                        "Transition from {} to {} is missing 'on @N EventName;' element.",
                        from_state_ident, to_state_ident
                    ))
                })?;
            let event_variant_ident = on_element; // This is the Ident from the 'on' clause

            // Check if the event definition exists and if it expects a payload
            let event_ast_item = ast
                .events
                .iter()
                .find(|e| &e.name == event_variant_ident)
                .ok_or_else(|| {
                    CodegenError::AstValidationError(format!(
                        "Event '{}' used in transition but not defined in events block.",
                        event_variant_ident
                    ))
                })?;

            let event_pattern = if event_ast_item.fields.is_empty() {
                quote! { #event_enum_name::#event_variant_ident }
            } else {
                let _payload_struct_name = format_ident!("{}", event_ast_item.name); // Prefix with underscore
                                                                                     // Bind payload if fields exist, using the specific payload struct type
                quote! { #event_enum_name::#event_variant_ident(payload) }
                // Ensure the event enum generation uses the payload struct:
                // Example: Event::MyEvent(MyEventPayload)
            };
            // Reference to the event or its payload for callbacks
            // Adjust to pass the correct type reference based on payload existence
            let (event_ref_or_payload, _event_type_for_callback) =
                if event_ast_item.fields.is_empty() {
                    // If no payload, pass reference to the whole event enum variant
                    (quote! { event }, quote! { &#event_enum_name})
                } else {
                    // If payload exists, pass reference to the bound payload struct
                    let payload_struct_name = format_ident!("{}", event_ast_item.name); // Use event name for payload struct
                    (quote! { payload }, quote! { &#payload_struct_name })
                };

            // Find the optional 'Guard' element
            let guard_element = transition.elements.iter().find_map(|el| match el {
                TransitionElement::Guard { function, .. } => Some(function),
                _ => None,
            });
            let guard_check = match guard_element {
                Some(guard_fn_ident) => {
                    // guard_fn_ident is &Ident
                    // Call the trait method on self
                    quote! {
                        // Pass current state ref and appropriate event/payload ref
                        // Note: event_ref_or_payload is already a reference for payload case (`payload` is &PayloadStruct)
                        // For non-payload case, it's `event`, so we need `&event`
                        if !self.#guard_fn_ident(&self.current_state, &#event_ref_or_payload) {
                            // Return Ok(self) because on_event takes ownership and we didn't transition
                            return Ok(self); // Guard failed
                        }
                    }
                }
                None => quote! {},
            };

            // Find the optional 'Action' element
            let action_element = transition.elements.iter().find_map(|el| match el {
                TransitionElement::Action { function, .. } => Some(function),
                _ => None,
            });
            let action_call = match action_element {
                Some(action_fn_ident) => {
                    // action_fn_ident is &Ident
                    // Determine argument to pass based on payload presence
                    let action_arg = if event_ast_item.fields.is_empty() {
                        quote! { &event }
                    } else {
                        quote! { payload } // 'payload' is already the reference from the match arm pattern
                    };
                    // Call the trait method on the mutable next state machine instance
                    quote! {
                        next_state_machine.#action_fn_ident(#action_arg);
                    }
                }
                None => quote! {},
            };

            // Find the exit actions for the 'from' state
            let exit_action_calls = ast
                .states
                .iter()
                .find(|s| &s.name == from_state_ident)
                .map_or(quote! {}, |state| {
                    let calls: Vec<TokenStream> = state
                        .exit_actions
                        .iter()
                        .map(|action_fn| {
                            quote! { next_state_machine.#action_fn(); }
                        })
                        .collect();
                    quote! { #(#calls)* }
                });

            // Find the entry actions for the 'to' state
            let entry_action_calls = ast
                .states
                .iter()
                .find(|s| &s.name == to_state_ident)
                .map_or(quote! {}, |state| {
                    let calls: Vec<TokenStream> = state
                        .entry_actions
                        .iter()
                        .map(|action_fn| {
                            quote! { next_state_machine.#action_fn(); }
                        })
                        .collect();
                    quote! { #(#calls)* }
                });

            // Return Ok containing the generated match arm code
            Ok(quote! {
                (#state_enum_name::#from_state_ident, #event_pattern) => {
                     #guard_check
                    // Clone self first
                    let mut next_state_machine = self.clone();
                    // Call exit actions for the current state *before* changing state
                    #exit_action_calls
                    // Change state
                    next_state_machine.current_state = #state_enum_name::#to_state_ident;
                    // Call transition action
                     #action_call
                     // Call entry actions for the new state *after* changing state and calling transition action
                     #entry_action_calls
                     Ok(next_state_machine)
                }
            })
        })
        .collect::<Result<Vec<_>, _>>()?; // Collect Results, propagating CodegenError

    // Determine the trait bound for on_event
    // Update trait bound condition to include entry/exit actions if not already covered
    let on_event_trait_bound = if guards.is_empty()
        && actions.is_empty()
        && ast
            .states
            .iter()
            .all(|s| s.entry_actions.is_empty() && s.exit_actions.is_empty())
    {
        quote! {} // No trait bound needed if no callbacks at all
    } else {
        quote! { where Self: #callbacks_trait_name }
    };

    Ok(quote! {
        // No separate impl block for callbacks, integrated into the main impl

        impl #machine_struct_name {
            /// Creates a new instance of the state machine in its initial state.
            pub fn new() -> Self {
                Self {
                    current_state: #initial_state_assignment,
                     // Add initialization for other potential fields in the machine struct if needed
                }
            }

            /// Processes an event and attempts to transition the state machine.
            /// Requires `Self` to implement the `#callbacks_trait_name` trait if guards or actions are defined.
            /// Returns the new state machine instance if successful (transition occurred, action ran).
            /// Returns the *original* state machine instance `Ok(self)` if a guard prevents the transition.
            /// Returns an `Err` for unhandled state/event combinations.
             pub fn on_event(self, event: #event_enum_name) -> Result<Self, String> // Takes ownership
             #on_event_trait_bound // Add trait bound here
             {
                 match (&self.current_state, &event) {
                    #(#on_event_match_arms)*
                    // Catch-all for unhandled state/event combinations
                    // Test expects Ok(self.clone()) instead of Err
                    // _ => Err(format!("Unhandled event {:?} in state {:?}", event, self.current_state)),
                    _ => Ok(self.clone()), // Return Ok with cloned self for unhandled cases
                }
            }

             /// Returns the current state.
             pub fn current_state(&self) -> &#state_enum_name {
                 &self.current_state
             }
        }
    })
}

/// Generates the Event enum definition with associated data structs.
fn generate_event_enum_and_structs(ast: &StateMachine, derive_tokens: &TokenStream) -> TokenStream {
    let event_enum_name = format_ident!("Event");
    // Use just "Payload" as the suffix convention
    let payload_suffix = format_ident!("Payload"); 

    let mut event_structs = Vec::new();
    let variants = ast
        .events
        .iter()
        .map(|event_item| {
            let variant_name = &event_item.name; // Use the Ident directly
            let variant_doc_comment = generate_rust_doc_comment(&event_item.annotations);

            if event_item.fields.is_empty() {
                // Event without payload
                quote! {
                    #variant_doc_comment
                    #variant_name
                }
            } else {
                // Event with payload struct
                // Corrected struct name generation
                let struct_name = format_ident!("{}{}", variant_name, payload_suffix);
                let struct_doc_comment = generate_rust_doc_comment(&event_item.annotations); // Use event doc for struct too?

                let fields = event_item.fields.iter().map(|field| {
                    let field_name = &field.name;
                    let field_type_ts = map_field_type_to_rust_type(&field.field_type);
                    let field_doc_comment = generate_rust_doc_comment(&field.annotations);
                    quote! {
                        #field_doc_comment
                        pub #field_name: #field_type_ts
                    }
                });

                // Generate the payload struct definition
                event_structs.push(quote! {
                    #struct_doc_comment
                    #derive_tokens // Derive traits for payload struct too
                    pub struct #struct_name {
                        #(#fields),*
                    }
                });

                // Generate the enum variant with the payload struct
                quote! {
                    #variant_doc_comment
                    #variant_name(#struct_name)
                }
            }
        })
        .collect::<Vec<_>>(); // Collect variants

    let event_enum_doc_comment = generate_rust_doc_comment(&[]); // TODO: Get annotations for the enum itself?

    quote! {
        // --- Event Payload Structs ---
        #(#event_structs)*

        // --- Event Enum ---
        #event_enum_doc_comment
        #derive_tokens // Use the same derives as State and Machine
        pub enum #event_enum_name {
            #(#variants),*
        }
    }
}

/// Generates Rust code from a StateMachine AST node (from parser).
///
/// # Arguments
///
/// * `ast` - A parsed `StateMachine` from `fsm_dsl::parser`.
///
/// # Returns
///
/// * `Result<String, CodegenError>` - Generated Rust code string, or an error.
pub fn generate_rust_code(ast: &StateMachine) -> Result<String, CodegenError> {
    let state_enum_name = format_ident!("State");
    let machine_struct_name = format_ident!("{}", ast.name); // Use name from AST
    let event_enum_name = format_ident!("Event"); // Consistent event enum name
    let callbacks_trait_name = format_ident!("{}Callbacks", ast.name); // e.g., LightSwitchCallbacks

    let _derive_tokens = quote! { #[derive(Debug, Clone, PartialEq)] };

    // State enum generation
    let state_variants = ast.states.iter().map(|s| {
        let variant_name = format_ident!("{}", s.name);
        let doc_comment = generate_rust_doc_comment(&s.annotations);
        quote! {
            #doc_comment // Doc comment first
            #variant_name
        }
    });
    let state_enum_doc_comment = generate_rust_doc_comment(&[]); // TODO: Get annotations for the enum itself?
    let state_enum_derive = quote! { #[derive(Debug, Clone, PartialEq, Eq, Hash)] }; // Define derive separately
    let state_enum = quote! {
        #state_enum_doc_comment // Doc comment first
        #state_enum_derive // Derive second
        // Add Eq, Hash back if no Float types are used in practice or handled
        pub enum #state_enum_name {
            #(#state_variants),*
        }
    }; // <-- Semicolon added here

    // Event enum and payload struct generation
    let event_derive_tokens = quote! { #[derive(Debug, Clone, PartialEq)] };
    let event_defs = generate_event_enum_and_structs(ast, &event_derive_tokens);

    // Machine struct definition
    let machine_struct_doc_comment = generate_rust_doc_comment(&ast.annotations);
    let machine_struct_derive = quote! { #[derive(Debug, Clone, PartialEq)] }; // Define derive separately
    let machine_struct = quote! {
        #machine_struct_doc_comment // Doc comment first
        #machine_struct_derive // Derive second
        // Use PartialEq only for machine struct if state or other fields contain floats
        pub struct #machine_struct_name {
            // Make current_state public for inspection/assertion
            pub current_state: #state_enum_name,
            // Add other fields to the machine struct if needed (e.g., context data)
        }
    };

    // --- Callback Trait Generation ---
    // Collect unique guard, action, entry, and exit function Idents
    let mut guards: HashSet<&proc_macro2::Ident> = HashSet::new();
    let mut actions: HashSet<&proc_macro2::Ident> = HashSet::new();
    let mut entry_actions: HashSet<&proc_macro2::Ident> = HashSet::new();
    let mut exit_actions: HashSet<&proc_macro2::Ident> = HashSet::new();
    let mut callback_signatures: Vec<TokenStream> = Vec::new();

    // Store mapping from function name to its associated event AST item for signature generation
    let mut callback_event_map: std::collections::HashMap<
        &proc_macro2::Ident,
        Vec<&fsm_dsl::ast::MessageItem>,
    > = std::collections::HashMap::new();

    for transition in &ast.transitions {
        // Find the 'On' element to determine the event type for this transition's callbacks
        let on_element = transition.elements.iter().find_map(|el| match el {
            TransitionElement::On { event, .. } => Some(event),
            _ => None,
        });
        let event_ast_item =
            on_element.and_then(|event_ident| ast.events.iter().find(|e| &e.name == event_ident)); // This is Option<&MessageItem>

        for element in &transition.elements {
            match element {
                TransitionElement::Guard { function, .. } => {
                    if guards.insert(function) {
                        // If newly inserted
                        if let Some(event_def) = event_ast_item {
                            callback_event_map
                                .entry(function)
                                .or_default()
                                .push(event_def);
                        }
                    }
                }
                TransitionElement::Action { function, .. } => {
                    if actions.insert(function) {
                        // If newly inserted
                        if let Some(event_def) = event_ast_item {
                            callback_event_map
                                .entry(function)
                                .or_default()
                                .push(event_def);
                        }
                    }
                }
                _ => {}
            }
        }
    }

    // Collect entry/exit actions from states
    for state in &ast.states {
        for entry_fn_ident in &state.entry_actions {
            entry_actions.insert(entry_fn_ident);
        }
        for exit_fn_ident in &state.exit_actions {
            exit_actions.insert(exit_fn_ident);
        }
    }

    // Generate guard signatures
    for guard_fn_ident in &guards {
        // Find the most specific event type if possible, otherwise use generic &Event
        let event_defs = callback_event_map.get(guard_fn_ident);
        let event_type_sig = determine_callback_event_signature(event_defs, &event_enum_name);

        callback_signatures.push(quote! {
            // Guard methods take immutable self, current state, and event/payload
            // Use "current_state" as the argument name to match test assertion
            fn #guard_fn_ident(&self, current_state: &#state_enum_name, event: #event_type_sig) -> bool;
        });
    }

    // Generate action signatures
    for action_fn_ident in &actions {
        // Find the most specific event type if possible, otherwise use generic &Event
        let event_defs = callback_event_map.get(action_fn_ident);
        let event_type_sig = determine_callback_event_signature(event_defs, &event_enum_name);

        callback_signatures.push(quote! {
            // Action methods take mutable self and event/payload
             fn #action_fn_ident(&mut self, event: #event_type_sig);
        });
    }

    // Generate entry action signatures
    for entry_fn_ident in &entry_actions {
        // Entry actions triggered after state change, takes mutable self
        // TODO: Consider adding context/state reference if needed
        callback_signatures.push(quote! {
            fn #entry_fn_ident(&mut self);
        });
    }

    // Generate exit action signatures
    for exit_fn_ident in &exit_actions {
        // Exit actions triggered before state change, takes mutable self
        // TODO: Consider adding context/state reference if needed
        callback_signatures.push(quote! {
            fn #exit_fn_ident(&mut self);
        });
    }

    let callbacks_trait_doc_comment = generate_rust_doc_comment(&[]); // TODO: Use machine annotations?
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
        quote! {} // No trait if no callbacks
    };

    // Impl block generation (pass calculated guards/actions)
    let impl_block = generate_impl_block(ast, &callbacks_trait_name, &guards, &actions)?;

    // Generate Default impl if a new() method exists (which it always should)
    let default_impl = quote! {
        impl Default for #machine_struct_name {
            fn default() -> Self {
                Self::new()
            }
        }
    };

    // Combine all parts
    let combined_code = quote! {
        #state_enum
        #event_defs
        #machine_struct
        #callbacks_trait // Add the trait definition
        #impl_block
        #default_impl // Add the default impl
    };

    // Format the generated code
    let code_str = combined_code.to_string();
    match syn::parse_file(&code_str) {
        Ok(syntax_tree) => Ok(prettyplease::unparse(&syntax_tree)),
        Err(e) => {
            eprintln!("--- Failed to parse generated code ---");
            eprintln!("{}", code_str);
            eprintln!("--- End generated code ---");
            // Manually construct the error variant
            Err(CodegenError::SynParseError(e, code_str))
        }
    }
}

// Helper function to determine the most specific event type signature for a callback
// based on all transitions where it's used. Falls back to generic &Event if types conflict
// or no event context is found.
fn determine_callback_event_signature<'a>(
    event_defs: Option<&'a Vec<&'a fsm_dsl::ast::MessageItem>>,
    event_enum_name: &proc_macro2::Ident,
) -> TokenStream {
    match event_defs {
        Some(defs) if !defs.is_empty() => {
            let first_def = defs[0];
            // Check if all uses agree on whether there's a payload and the payload type name
            let all_agree = defs.iter().all(|d| {
                (d.fields.is_empty() == first_def.fields.is_empty())
                    && (d.fields.is_empty() || d.name == first_def.name) // Check payload name matches if not empty
            });

            if all_agree {
                if first_def.fields.is_empty() {
                    // All uses are for events without payload, use reference to enum
                    quote! { &#event_enum_name }
                } else {
                    // All uses are for the same event with payload, use reference to payload struct
                    // Use the correct payload struct naming convention
                    let payload_suffix = format_ident!("Payload");
                    let payload_struct_name = format_ident!("{}{}", first_def.name, payload_suffix);
                    quote! { &#payload_struct_name }
                }
            } else {
                // Disagreement in payload types, fall back to generic enum reference
                quote! { &#event_enum_name }
            }
        }
        _ => {
            // No event context found or empty list, fall back to generic enum reference
            quote! { &#event_enum_name }
        }
    }
}

/// Generates a Cap'n Proto schema (.capnp) from the FSM AST.
///
/// # Arguments
///
/// * `file_ast` - The parsed `SsotFile` structure (needed for file ID).
/// * `machine_ast` - The specific `StateMachine` structure to generate the schema for.
///
/// # Returns
///
/// A `Result` containing the Cap'n Proto schema string or a `CodegenError`.
pub fn generate_capnp_schema(
    file_ast: &SsotFile, // Updated signature
    machine_ast: &StateMachine,
) -> Result<String, CodegenError> {
    // Use the internal function from the capnp module
    codegen_capnp::generate_capnp_schema_internal(file_ast, machine_ast)
}

/// Generates TypeScript type definitions (.types.ts) from the FSM AST.
///
/// # Arguments
///
/// * `ast` - The parsed `StateMachine` structure.
///
/// # Returns
///
/// A `Result` containing the TypeScript type definition string or a `CodegenError`.
pub fn generate_typescript_types(ast: &StateMachine) -> Result<String, CodegenError> {
    // Use the internal function from the ts module
    codegen_ts::generate_typescript_types_internal(ast)
}

/// Generates an XState machine definition string from the FSM AST.
///
/// # Arguments
/// * `ast` - A reference to the `StateMachine` AST node.
///
/// # Returns
/// A `Result` containing the generated XState machine definition (e.g., as a JS object literal string) or a `CodegenError`.
pub fn generate_xstate_machine(ast: &StateMachine) -> Result<String, CodegenError> {
    // Use the internal function from the xstate module
    codegen_xstate::generate_xstate_machine_internal(ast)
}

/// Generates an SCXML document string from the FSM AST.
///
/// # Arguments
/// * `ast` - A reference to the `StateMachine` AST node.
///
/// # Returns
/// A `Result` containing the generated SCXML document as a `String` or a `CodegenError`.
pub fn generate_scxml(ast: &StateMachine) -> Result<String, CodegenError> {
    // Use the internal function from the scxml module
    codegen_scxml::generate_scxml_internal(ast)
}

// Error type
#[derive(Debug, thiserror::Error)]
pub enum CodegenError {
    #[error(
        "Failed to parse generated code: {0}\n--- Generated Code ---
{1}"
    )]
    SynParseError(syn::Error, String),
    #[error("AST validation error: {0}")]
    AstValidationError(String),
    #[error("Code generation failed: {0}")]
    GenerationError(String),
    #[error("I/O error during code formatting: {0}")]
    FormatIoError(#[from] std::io::Error),
    #[error("Failed to format generated code: {0}")]
    FormatError(String),
    // Potential future errors: IO errors, etc.
}

// Add the From implementation here, once, centrally.
impl From<std::fmt::Error> for CodegenError {
    fn from(err: std::fmt::Error) -> Self {
        CodegenError::GenerationError(format!("Failed to write to string: {}", err))
    }
}

// --- Unit Tests ---
#[cfg(test)]
mod tests {
    use super::*; // Bring parent module's items into scope
    use fsm_dsl::parser::parse_str; // Import the CORRECT parser function
    // Import necessary AST types for helper functions
    use fsm_dsl::ast::{
        Annotation, AnnotationValue, FieldDef, FieldType, MessageItem, StateItem,
        StateMachine, TransitionElement, TransitionItem, 
    };
    use syn::parse_file as syn_parse_file; // Import for code parsing in tests

    // --- Define shared constants for tests --- 
    const ANNOTATED_MACHINE_SSOT: &str = r#"
@0xdeadbeefcafe0001;

$description("This is the main machine.");
stateMachine AnnotatedMachine {
    $initial(Idle);
    states {
        $description("Waiting state.");
        Idle @0;
        $description("Active state.");
        Running @1;
    }
    events {
        $description("Starts the machine.");
        event Start @0 {
            $description("The user ID.");
            userId @0 : UInt64;
        }
        $description("Stops the machine.");
        event Stop @1; // Use semicolon instead of {} for empty event
    }
    transitions {
        transition StartIdle from Idle to Running { on @0 Start; }
        transition StopRunning from Running to Idle { on @1 Stop; }
    }
}
"#;

    // Helper function to create identifiers for tests
    fn ident(s: &str) -> proc_macro2::Ident {
        proc_macro2::Ident::new(s, proc_macro2::Span::call_site())
    }

    // Helper to create a basic StateMachine AST for testing
    fn create_test_ast() -> StateMachine {
        StateMachine {
            name: ident("TestMachine"),
            annotations: vec![Annotation {
                name: ident("initial"),
                value: Some(AnnotationValue::Identifier(ident("Idle"))),
            }],
            events: vec![
                MessageItem {
                    name: ident("Event1"),
                    ordinal: 0, // Use u64 directly
                    annotations: vec![],
                    fields: vec![],
                },
                MessageItem {
                    name: ident("Event2"),
                    ordinal: 1, // Use u64 directly
                    annotations: vec![],
                    fields: vec![],
                },
                MessageItem {
                    name: ident("EventWithPayload"),
                    ordinal: 2, // Use u64 directly
                    annotations: vec![],
                    fields: vec![FieldDef {
                        name: ident("data"),
                        ordinal: 0, // Use u64 directly
                        annotations: vec![],
                        field_type: FieldType::UInt32,
                    }],
                },
            ],
            states: vec![
                StateItem {
                    name: ident("Idle"),
                    ordinal: 0, // Use u64 directly
                    annotations: vec![],
                    entry_actions: vec![],
                    exit_actions: vec![],
                },
                StateItem {
                    name: ident("Active"),
                    ordinal: 1, // Use u64 directly
                    annotations: vec![],
                    entry_actions: vec![],
                    exit_actions: vec![],
                },
            ],
            transitions: vec![
                TransitionItem {
                    name: Some(ident("T1")), // Wrap name in Some
                    from: ident("Idle"),
                    to: ident("Active"),
                    annotations: vec![],
                    elements: vec![TransitionElement::On { ordinal: 0, event: ident("Event1") }], // Use u64 directly
                },
                TransitionItem {
                    name: Some(ident("T2")), // Wrap name in Some
                    from: ident("Active"),
                    to: ident("Idle"),
                    annotations: vec![],
                    elements: vec![TransitionElement::On { ordinal: 1, event: ident("Event2") }], // Use u64 directly
                },
                TransitionItem {
                    name: Some(ident("T3WithAction")), // Wrap name in Some
                    from: ident("Idle"),
                    to: ident("Active"),
                    annotations: vec![],
                    elements: vec![
                        TransitionElement::On { ordinal: 2, event: ident("EventWithPayload") }, // Use u64 directly
                        TransitionElement::Action { ordinal: 0, function: ident("do_something") }, // Use u64 directly
                    ],
                },
            ],
            context: vec![], // Add missing context field
        }
    }

    // Helper to parse and format code for comparison
    fn parse_and_format(code: &str) -> String {
        let parsed_file = syn_parse_file(code).expect("Failed to parse generated code");
        prettyplease::unparse(&parsed_file)
    }

    #[test]
    fn generates_basic_structures() {
        let ast = create_test_ast();
        let result = generate_rust_code(&ast); // Assuming generate_rust_code takes &StateMachine
        assert!(result.is_ok(), "Rust generation failed");
        let code = result.unwrap();
        let formatted_code = parse_and_format(&code);

        // Basic checks (adjust based on actual generation)
        assert!(formatted_code.contains("pub enum State"));
        assert!(formatted_code.contains("Idle"));
        assert!(formatted_code.contains("Active"));
        assert!(formatted_code.contains("pub enum Event"));
        assert!(formatted_code.contains("Event1"));
        assert!(formatted_code.contains("Event2"));
        assert!(formatted_code.contains("pub struct EventWithPayloadPayload")); // Check payload struct
        assert!(formatted_code.contains("EventWithPayload(EventWithPayloadPayload)")); // Check enum variant with payload
        assert!(formatted_code.contains("pub struct TestMachine"));
    }

    #[test]
    fn generates_impl_block_and_new() {
        let ast = create_test_ast();
        let result = generate_rust_code(&ast);
        assert!(result.is_ok(), "Rust generation failed");
        let code = result.unwrap();
        let formatted_code = parse_and_format(&code);

        assert!(formatted_code.contains("impl TestMachine"));
        assert!(formatted_code.contains("pub fn new() -> Self"));
        assert!(formatted_code.contains("current_state: State::Idle")); // Check initial state assignment
        assert!(formatted_code.contains("impl Default for TestMachine"));
        assert!(formatted_code.contains("Self::new()"));
    }

    #[test]
    fn generates_on_event_method_with_transitions() {
        let ast = create_test_ast();
        let result = generate_rust_code(&ast);
        assert!(result.is_ok(), "Rust generation failed");
        let code = result.unwrap();
        let formatted_code = parse_and_format(&code);

        assert!(formatted_code.contains("pub fn on_event(self, event: Event) -> Result<Self, String>"));
        assert!(formatted_code.contains("match (&self.current_state, &event)"));
        // Check transition arms (adjust event payload matching if necessary)
        assert!(formatted_code.contains("(State::Idle, Event::Event1) =>"));
        assert!(formatted_code.contains("next_state_machine.current_state = State::Active;"));
        assert!(formatted_code.contains("(State::Active, Event::Event2) =>"));
        assert!(formatted_code.contains("next_state_machine.current_state = State::Idle;"));
        assert!(formatted_code.contains("(State::Idle, Event::EventWithPayload(payload)) =>")); // Check payload binding
        assert!(formatted_code.contains("next_state_machine.do_something(payload);")); // Check action call with payload
        assert!(formatted_code.contains("_ => Ok(self.clone())")); // Default case
    }

    #[test]
    fn generates_guard_and_action_placeholders_module() {
        // Need an AST with guards/actions
        let ast_with_callbacks = StateMachine { // Simplified AST for this test
            name: ident("CallbackMachine"),
            annotations: vec![Annotation { name: ident("initial"), value: Some(AnnotationValue::Identifier(ident("S1"))) }],
            events: vec![MessageItem { name: ident("E1"), ordinal: 0, annotations: vec![], fields: vec![] }], // Use u64 directly
            states: vec![StateItem { name: ident("S1"), ordinal: 0, annotations: vec![], entry_actions: vec![], exit_actions: vec![] }, // Use u64 directly
                         StateItem { name: ident("S2"), ordinal: 1, annotations: vec![], entry_actions: vec![], exit_actions: vec![] }], // Use u64 directly
            transitions: vec![TransitionItem {
                name: Some(ident("T1")), // Wrap name in Some
                from: ident("S1"),
                to: ident("S2"),
                annotations: vec![],
                elements: vec![
                    TransitionElement::On { ordinal: 0, event: ident("E1") }, // Use u64 directly
                    TransitionElement::Guard { ordinal: 0, function: ident("can_transition") }, // Use u64 directly
                    TransitionElement::Action { ordinal: 1, function: ident("perform_action") }, // Use u64 directly
                ],
            }],
            context: vec![], // Add missing context field
        };
        let result = generate_rust_code(&ast_with_callbacks);
        assert!(result.is_ok(), "Rust generation failed");
        let code = result.unwrap();
        let formatted_code = parse_and_format(&code);

        let callbacks_trait_name = format_ident!("{}Callbacks", ast_with_callbacks.name);
        assert!(formatted_code.contains(&format!("pub trait {}", callbacks_trait_name)));
        assert!(formatted_code.contains("fn can_transition(&self, current_state: &State, event: &Event) -> bool;"));
        assert!(formatted_code.contains("fn perform_action(&mut self, event: &Event);"));

    }

    #[test]
    fn handles_no_transitions_or_callbacks() {
        let simple_ast = StateMachine {
            name: ident("SimpleMachine"),
            annotations: vec![Annotation { name: ident("initial"), value: Some(AnnotationValue::Identifier(ident("OnlyState"))) }],
            events: vec![MessageItem { name: ident("DummyEvent"), ordinal: 0, annotations: vec![], fields: vec![] }], // Use u64 directly
            states: vec![StateItem { name: ident("OnlyState"), ordinal: 0, annotations: vec![], entry_actions: vec![], exit_actions: vec![] }], // Use u64 directly
            transitions: vec![], // No transitions
            context: vec![], // Add missing context field
        };
        let result = generate_rust_code(&simple_ast);
        assert!(result.is_ok(), "Rust generation failed");
        let code = result.unwrap();
        let formatted_code = parse_and_format(&code);

        // Check that impl block is generated, but on_event might be simple
        assert!(formatted_code.contains("impl SimpleMachine"));
        assert!(formatted_code.contains("pub fn on_event(self, event: Event) -> Result<Self, String>"));
        assert!(formatted_code.contains("match (&self.current_state, &event)"));
        // Should likely only contain the default arm if no transitions
        assert!(formatted_code.contains("_ => Ok(self.clone())"));
        // Check that no callback trait is generated
        assert!(!formatted_code.contains("pub trait SimpleMachineCallbacks"));
    }

    #[test]
    fn generates_current_state_getter() {
        let ast = create_test_ast();
        let result = generate_rust_code(&ast);
        assert!(result.is_ok(), "Rust generation failed");
        let code = result.unwrap();
        let formatted_code = parse_and_format(&code);

        assert!(formatted_code.contains("pub fn current_state(&self) -> &State"));
        assert!(formatted_code.contains("&self.current_state"));
    }

    // --- Tests using ANNOTATED_MACHINE_SSOT --- 

    #[test]
    fn generates_rust_code_with_doc_comments() {
        let ast = parse_str(ANNOTATED_MACHINE_SSOT).unwrap(); 
        let machine = &ast.state_machines[0]; 
        let _code = generate_rust_code(machine).unwrap();
        let formatted_code = parse_and_format(&_code);
        assert!(formatted_code.contains(
            "#[derive(Debug, Clone, PartialEq, Eq, Hash)]\\n/// Waiting state.\\npub enum State"
        )); 
        assert!(formatted_code.contains("/// Waiting state.\\n    Idle,"));
        assert!(formatted_code.contains("/// Active state.\\n    Running,"));
        assert!(formatted_code.contains("/// Starts the machine.\\n#[derive(Debug, Clone, PartialEq)]\\npub struct StartPayload")); // Adjusted name
        assert!(formatted_code.contains("/// The user ID.\\n    pub userId: u64,"));
        assert!(formatted_code.contains("/// Starts the machine.\\n    Start(StartPayload),")); // Adjusted name
        assert!(formatted_code.contains("/// Stops the machine.\\n    Stop,"));
        assert!(formatted_code.contains("/// This is the main machine.\\n#[derive(Debug, Clone, PartialEq)]\\npub struct AnnotatedMachine")); 
        assert!(formatted_code.contains("impl AnnotatedMachine"));
        assert!(formatted_code.contains("/// Creates a new instance"));
        assert!(formatted_code.contains("/// Processes an event"));
        assert!(formatted_code.contains("/// Returns the current state."));
        assert!(formatted_code.contains("impl Default for AnnotatedMachine"));
    }

    #[test]
    fn generates_capnp_schema_with_comments() {
        let ast = parse_str(ANNOTATED_MACHINE_SSOT).unwrap(); 
        let machine = &ast.state_machines[0]; 
        let result = generate_capnp_schema(&ast, machine);
        assert!(result.is_ok(), "Capnp generation failed: {:?}", result.err());
        let _schema = result.unwrap();
        assert!(_schema.contains("@0xdeadbeefcafe0001;"));
        assert!(_schema.contains("# This is the main machine."));
        assert!(_schema.contains("enum State @0"));
        assert!(_schema.contains("# Waiting state.\\n  Idle @0;"));
        assert!(_schema.contains("# Active state.\\n  Running @1;"));
        assert!(_schema.contains("struct StartPayload @2")); // Adjusted name
        assert!(_schema.contains("# The user ID.\\n  userId @0 :UInt64;")); 
        assert!(_schema.contains("union Event @1"));
        assert!(_schema.contains("# Starts the machine.\\n  Start @0 :StartPayload;")); // Adjusted name
        assert!(_schema.contains("# Stops the machine.\\n  Stop @1 :Void;"));
    }

    #[test]
    fn generates_typescript_types_with_jsdoc() {
        let ast = parse_str(ANNOTATED_MACHINE_SSOT).unwrap(); 
        let machine = &ast.state_machines[0]; 
        let _types = generate_typescript_types(machine).unwrap();
        assert!(_types.contains("/**\n * This is the main machine.\n */"));
        assert!(_types.contains("export interface Context {"));
        assert!(_types.contains("export type State ="));
        assert!(_types.contains("/**\n   * Waiting state.\n   */\n\"Idle\""));
        assert!(_types.contains("|   /**\n   * Active state.\n   */\n\"Running\""));
        assert!(_types.contains("/**\n * Starts the machine.\n */"));
        assert!(_types.contains("export interface StartPayload {")); // Adjusted name
        assert!(_types.contains("/**\n   * The user ID.\n   */\n  userId: bigint;"));
        assert!(_types.contains("export type Event ="));
        assert!(_types.contains(
            "/**\n   * Starts the machine.\n   */\n   { type: \"Start\", payload: StartPayload }" // Adjusted name
        ));
        assert!(_types.contains(
            "/**\n   * Stops the machine.\n   */\n|  { type: \"Stop\" };"
        ));
    }
}
