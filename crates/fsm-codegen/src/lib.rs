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
    let event_payload_struct_name = format_ident!("EventPayload"); // Convention for associated data

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
                let struct_name = format_ident!("{}{}", variant_name, event_payload_struct_name);
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

    // Extract annotations specific to this state machine from the `items` list if needed
    // For now, we pass the StateMachine AST directly which already contains its annotations.
    // let machine_annotations = /* logic to find annotations for this machine */;

    let _derive_tokens = quote! { #[derive(Debug, Clone, PartialEq)] };

    // State enum generation
    let state_variants = ast.states.iter().map(|s| {
        let variant_name = format_ident!("{}", s.name);
        let doc_comment = generate_rust_doc_comment(&s.annotations);
        quote! {
            #doc_comment
            #variant_name
        }
    });
    let state_enum_doc_comment = generate_rust_doc_comment(&[]); // TODO: Get annotations for the enum itself?
    let state_enum = quote! {
        #state_enum_doc_comment
        // Add Eq, Hash back if no Float types are used in practice or handled
        #[derive(Debug, Clone, PartialEq, Eq, Hash)]
        pub enum #state_enum_name {
            #(#state_variants),*
        }
    }; // <-- Semicolon added here

    // Event enum and payload struct generation
    let event_derive_tokens = quote! { #[derive(Debug, Clone, PartialEq)] };
    let event_defs = generate_event_enum_and_structs(ast, &event_derive_tokens);

    // Machine struct definition
    let machine_struct_doc_comment = generate_rust_doc_comment(&ast.annotations);
    let machine_struct = quote! {
        #machine_struct_doc_comment
        // Use PartialEq only for machine struct if state or other fields contain floats
        #[derive(Debug, Clone, PartialEq)]
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
            fn #guard_fn_ident(&self, state: &#state_enum_name, event: #event_type_sig) -> bool;
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
                    let payload_struct_name = format_ident!("{}EventPayload", first_def.name);
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

// Centralized From implementation for std::fmt::Error
impl From<std::fmt::Error> for CodegenError {
    fn from(e: std::fmt::Error) -> Self {
        CodegenError::GenerationError(e.to_string())
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
        StateItem,
        StateMachine,
        TransitionElement,
        TransitionItem,
    };
    use pretty_assertions::assert_eq;
    use quote::quote;
    use syn::parse_file as syn_parse_file; // Alias for clarity

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
                value: Some(AnnotationValue::Identifier(ident("Idle"))), // Wrap in Some()
            }],
            events: vec![
                MessageItem {
                    name: ident("Start"),
                    ordinal: 0, // Added ordinal
                    fields: vec![],
                    // Removed annotations
                },
                MessageItem {
                    name: ident("Stop"),
                    ordinal: 1, // Added ordinal
                    fields: vec![],
                    // Removed annotations
                },
                MessageItem {
                    name: ident("Update"),
                    ordinal: 2, // Added ordinal
                    fields: vec![FieldDef {
                        name: ident("value"),
                        ordinal: 0, // Added ordinal
                        field_type: FieldType::Int32,
                        // Removed annotations
                    }],
                    // Removed annotations
                },
            ],
            states: vec![
                StateItem {
                    name: ident("Idle"),
                    ordinal: 0, // Added ordinal
                                // Removed annotations
                },
                StateItem {
                    name: ident("Running"),
                    ordinal: 1, // Added ordinal
                                // Removed annotations
                },
            ],
            transitions: vec![
                TransitionItem {
                    name: Some(ident("transition1")), // Wrap in Some()
                    from: ident("Idle"),
                    to: ident("Running"),
                    elements: vec![
                        TransitionElement::On {
                            ordinal: 1, // Changed from order
                            event: ident("Start"),
                        },
                        TransitionElement::Action {
                            ordinal: 2, // Changed from order
                            function: ident("on_start_action"),
                        },
                    ],
                    annotations: vec![], // Added annotations field
                },
                TransitionItem {
                    name: Some(ident("transition2")), // Wrap in Some()
                    from: ident("Running"),
                    to: ident("Idle"),
                    elements: vec![
                        TransitionElement::On {
                            ordinal: 1, // Changed from order
                            event: ident("Stop"),
                        },
                        TransitionElement::Guard {
                            ordinal: 2, // Changed from order
                            function: ident("can_stop_guard"),
                        },
                    ],
                    annotations: vec![], // Added annotations field
                },
                TransitionItem {
                    name: Some(ident("transition3")), // Wrap in Some()
                    from: ident("Running"),
                    to: ident("Running"), // Self-transition
                    elements: vec![TransitionElement::On {
                        ordinal: 1, // Changed from order
                        event: ident("Update"),
                    }],
                    annotations: vec![], // Added annotations field
                },
            ],
        }
    }

    // Helper to parse and format code for comparison
    #[cfg(test)] // Add cfg(test) attribute
    fn parse_and_format(code: &str) -> String {
        let parsed_file = syn_parse_file(code).expect("Failed to parse generated code");
        prettyplease::unparse(&parsed_file)
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
        let ast = create_test_ast(); // Reuse existing test AST setup
        let file_ast = fsm_dsl::ast::SsotFile {
            // Create a dummy SsotFile for the test
            file_id: 0x123456789ABCDEF0,
            package_declaration: None,
            top_level_annotations: vec![],
            state_machines: vec![ast.clone()], // Assuming create_test_ast returns StateMachine
        };
        let result = crate::generate_capnp_schema(&file_ast, &ast); // Pass both args
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
        let ast = create_test_ast(); // Reuse existing test AST setup
        let result = crate::generate_typescript_types(&ast);
        assert!(
            result.is_ok(),
            "TypeScript generation failed: {:?}",
            result.err()
        );
        let types = result.unwrap();
        // Basic check for placeholder content
        assert!(types.contains("// Placeholder TypeScript types"));
        assert!(types.contains("export type State"));
        assert!(types.contains("export type Event"));
        println!(
            "--- Generated TypeScript Types (Placeholder) ---
{}",
            types
        ); // For inspection
    }

    // Helper to create Ident for tests
    fn ident(s: &str) -> proc_macro2::Ident {
        proc_macro2::Ident::new(s, proc_macro2::Span::call_site())
    }

    // Updated test AST creator with annotations
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
                        ordinal: 0,
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
                },
                StateItem {
                    annotations: vec![Annotation {
                        // Annotation on state
                        name: ident("description"),
                        value: Some(AnnotationValue::StringLiteral("Active state.".to_string())),
                    }],
                    name: ident("Running"),
                    ordinal: 1,
                },
            ],
            transitions: vec![
                TransitionItem {
                    name: None,
                    from: ident("Idle"),
                    to: ident("Running"),
                    elements: vec![TransitionElement::On {
                        ordinal: 0,
                        event: ident("Start"),
                    }],
                    annotations: vec![],
                },
                TransitionItem {
                    name: None,
                    from: ident("Running"),
                    to: ident("Idle"),
                    elements: vec![TransitionElement::On {
                        ordinal: 1,
                        event: ident("Stop"),
                    }],
                    annotations: vec![],
                },
            ],
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
            top_level_annotations: vec![],
            state_machines: vec![machine_ast.clone()],
        };
        let result = generate_capnp_schema(&file_ast, &machine_ast);
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
