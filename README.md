# FSM DSL and Code Generator (`.ssot`)

Define Finite State Machines (FSMs) declaratively using a dedicated Domain Specific Language (`.ssot`) and automatically generate type-safe **Rust code**, **Cap'n Proto schemas**, **TypeScript types**, and potentially more. This project uses `.ssot` files as the **Single Source of Truth** for your state machine logic, ensuring consistency across different parts of your system.

## Background & Goal

Defining complex stateful logic manually across different languages and platforms is error-prone and time-consuming. This project aims to solve this by providing:

*   **An Intuitive DSL (`.ssot`):** Clearly represent states, events, and transitions inspired by Cap'n Proto schema syntax.
    *   **Core Principle:** The `.ssot` DSL grammar, defined in `crates/fsm-dsl/src/ssot.pest`, serves as the foundational definition. Unless explicitly requested for specific enhancements, **this grammar definition (`ssot.pest`) should be considered stable and remain unchanged** to ensure parser consistency.
*   **Multi-Target Code Generation:** Automatically generate boilerplate code and definitions for various targets (initially Rust, Cap'n Proto, TypeScript).
*   **Type Safety:** Ensure correctness through compile-time checks (Rust) or strong typing (TypeScript, Cap'n Proto).
*   **Centralized Logic:** Use the `.ssot` file as the definitive source, reducing redundancy and simplifying updates.

## Workspace Structure

This project is organized as a Cargo workspace:

*   `crates/fsm-dsl`: Defines the `.ssot` grammar (`ssot.pest`), Abstract Syntax Tree (`ast.rs`), and parser (`parser.rs`) using `pest`.
*   `crates/fsm-codegen`: Consumes the AST from `fsm-dsl` to generate:
    *   **Rust Code:** State/event enums, machine struct with transition logic, callback traits (`lib.rs`, `codegen_rust.rs`).
    *   **Cap'n Proto Schemas:** `.capnp` definitions mirroring the FSM structure (`codegen_capnp.rs`).
    *   **TypeScript Types:** Interfaces and type aliases for states, events, and payloads (`codegen_ts.rs`).
*   `crates/fsm-example`: Demonstrates usage with a sample `spec/my_fsm.ssot` and a `build.rs` script for invoking the generators.

## Usage (Example Workflow)

1.  **Define FSM:** Create/edit a `.ssot` file (e.g., `crates/fsm-example/spec/my_fsm.ssot`).
2.  **Annotate:** Use annotations to specify output locations and add documentation:
    ```fsm
    @0x...; // File ID (required, Cap'n Proto compatible)

    // Specify output directories (required at top-level or per machine)
    $rust_out("src/generated");
    $capnp_out("schema/capnp"); // Optional
    $ts_out("schema/ts");       // Optional

    $description("Description for the whole state machine.");
    stateMachine MyMachine {
        states {
            $description("Initial state.");
            Idle @0;
            $description("Active state.");
            Running @1;
        }
        events {
            $description("Event to start.");
            event Start @0 {
                $description("User ID.");
                userId @0 : UInt64;
            }
            $description("Event to stop.");
            event Stop @1;
        }
        // ... transitions ...
    }
    ```
3.  **Integrate with `build.rs`:** In your crate's `build.rs`, parse the `.ssot` file and call the generation functions from `fsm-codegen`. (See `crates/fsm-example/build.rs`).
4.  **Build:** Run `cargo build`. The `build.rs` script executes, generating files into the specified output directories before compiling your crate.
    *   The build script parses `.ssot`, calls `fsm_codegen::generate_*`, and writes outputs (e.g., `src/generated/my_machine.rs`, `schema/capnp/MyMachine.capnp`).
    *   Your crate code (e.g., `fsm-example/src/main.rs`) can then `include!` or import the generated artifacts.

*(Refer to `fsm-example/build.rs` for a concrete implementation.)*

## Key DSL Annotations

*   **File ID:** `@0x...;`: **Required** top-level unique ID (Cap'n Proto compatible).
*   **Output Directories:**
    *   `$rust_out("path/to/dir")`: **Required** (top-level or per-machine). Generates `<MachineNameSnakeCase>.rs`.
    *   `$capnp_out("path/to/dir")`: Optional. Generates `<MachineName>.capnp`.
    *   `$ts_out("path/to/dir")`: Optional. Generates `<MachineName>.types.ts`.
    *   **(Other data/API/DB schema outputs like `$zod_out`, `$openapi_out`, `$sql_out`...)**
    *   **`$terraform_out("path")`, `$cdk_out("path")`, etc.:** Optional. Generates Infrastructure as Code (IaC) configurations.
*   **Documentation:** `$description("...")`: Optional. Adds doc comments to generated Rust (`///`), Cap'n Proto (`#`), and TypeScript (`/** ... */`). Applicable to `stateMachine`, `state`, `event`, `field`.
*   **Initial State:** `$initial(StateName)`: **Required** on `stateMachine`. Specifies the entry state.
*   **(Others like `$version`, `$derive` might be parsed but aren't fully utilized yet.)*

## Current Status

*   ✅ DSL Parsing: States, events (with fields), transitions, annotations (including types, machines, parallel/history states).
*   ✅ Validation (Basic): 
    *   ID Uniqueness Check (Global).
    *   Name Resolution (Types, States, Actions, Guards, Invokes within scope).
    *   Structural Checks:
        *   Machine requires exactly one `$initial` state.
        *   `parallel` state requires nested `states` block (regions).
        *   `history` state requires exactly one eventless default transition.
*   🚧 Rust Code Generation: Functional state/event enums, machine struct, basic transition logic, callback trait.
*   🚧 Cap'n Proto Schema Generation: Structures reflecting FSM states, events, and payloads.
*   🚧 TypeScript Type Generation: State unions, event discriminated unions, payload interfaces.
*   ✅ Documentation Generation: From `$description` annotations for all targets.
*   ✅ Example `build.rs` Workflow: Demonstrates parsing and invoking generators.

## DSL Completion Status (as of [Current Date/Time])

Based on the current codebase structure and analysis:

**Core DSL Crate (`fsm-dsl`):**

*   **AST Definition (`ast.rs`):** Mature and comprehensive. Defines structures for types, services, actors, communication protocols, state machines (including context, states, transitions, actions, guards, invokes, parallel/history states), and deployment configurations.
*   **Parsing (`parser.rs`, `ssot.pest`):** A substantial parser exists using `pest`. It likely covers a large portion of the defined grammar, translating DSL text into the AST. Full coverage requires further verification against the `ast.rs` definitions.
*   **Validation (`validation.rs`):** Extensive validation logic is implemented, covering identifier uniqueness, name resolution, and structural rules for various DSL constructs (e.g., initial states, parallel states, history states).

**Supporting Crates:**

*   **Code Generation (`fsm-codegen`):** This crate exists, but its implementation status is unknown. Transforming the validated AST into executable code or configurations appears to be the primary remaining work area for the core DSL functionality.
*   **Example Usage (`fsm-example`):** Purpose and content need review. Likely intended to demonstrate DSL usage.
*   **Linter (`ssot-linter`):** Purpose and content need review. Likely intended for enforcing DSL style or rules outside core validation.

**Overall Assessment:**

The DSL definition, parsing, and validation components are well-developed. The main focus for completion seems to be implementing the code generation or runtime aspects to make the DSL definitions executable/usable. Further review of the `fsm-codegen` crate and test coverage is recommended for a more detailed picture.

## Roadmap / Future Enhancements

The vision is to evolve `.ssot` into a comprehensive Single Source of Truth. The immediate priority is to ensure the core tooling (validation, code generation) fully supports the currently defined DSL specification (see `crates/fsm-dsl/README.md`).

**Near-Term Focus (Implement Defined DSL & Core Tooling):**

*   **Validation Enhancements (High Priority):**
    *   Implement **full name resolution** across imports and scopes for all referenced types, services, actors, states, actions, guards, invokes, channels, etc.
    *   Implement comprehensive **type checking** for context variables, action/guard/method parameters & return types, invoke input/output mappings.
    *   Validate **all annotation types** (`$db`, `$validate`, `$route`, `$allowedActors`, etc.) for correct arguments and structure based on the DSL spec.
    *   Perform detailed **structural validation** for all DSL blocks (e.g., ensure `$implements` refers to a defined `interface`, `invoke` sources are valid, transition targets exist, deployment targets resolve).
    *   Add checks for **unused definitions** (actions, guards, invokes, etc.).
    *   Detect **circular dependencies** in imports and potentially service/machine extensions.
    *   Improve **error reporting** with precise location info (file, line, column) and clear messages for all validation errors.
*   **Code Generation Implementation (High Priority - `fsm-codegen`):**
    *   **Rust:** Generate code for *all* defined AST elements: actions, guards, invokes (with `onDone`/`onError`), context manipulation, parallel/history states, services, communication (basic stubs), deployment config (structs/enums). Generate skeleton functions/traits for user implementation.
    *   **Cap'n Proto:** Generate schemas reflecting *all* defined types, services (interfaces), communication patterns (events), and machine structures.
    *   **TypeScript:** Generate types/interfaces for *all* defined DSL elements for frontend/backend integration.
    *   Ensure generators utilize fully validated AST/SymbolTable information.
    *   Implement documentation generation (`$description`) for all targets consistently.
*   **Testing & DX:**
    *   Significantly increase **test coverage** for the parser, validator (covering all new checks), and *each* code generator (Rust, Capnp, TS) with complex DSL examples.
    *   Implement **source mapping** (optional) to link generated code back to `.ssot` files.

**Mid-Term Goals (Expand DSL & Integrations):**

*   **DSL Evolution (Based on `crates/fsm-dsl/README.md` Future Extensions):**
    *   Explore standardized error handling framework (`errors {}` block, `Result<>` types).
    *   Consider integrated test definitions (`tests {}` block, mocking).
    *   Refine generator configuration (`$rust_out(...) config {}`).
    *   Investigate explicit namespacing/modules.
    *   Explore enhanced security policy definitions (`security {}` block, `$auth`).
*   **Expanded Target Formats (Core):**
    *   Visualizations (Mermaid, Graphviz DOT).
    *   Schema Formats (JSON Schema, Zod).
    *   API Formats (OpenAPI, AsyncAPI).
    *   DB Schema Definitions (SQL DDL, potentially Prisma/Drizzle - leveraging `$db` annotations).
*   **Tooling:**
    *   Support for multi-file/directory projects (imports, discovery).
    *   Explicit output file mapping and manifest generation.
    *   Explore alternative Rust integration (e.g., procedural macro).

**Long-Term Vision (Comprehensive SSOT & Ecosystem):**

*   **Full SSOT Scope:** IaC (Terraform, CDK), UI/UX mapping, Testing (BDD), Observability, Compliance, Formal Verification.
*   **Broader Integrations:** ReqIF, BPMN, ArchiMate, UML/SysML, etc.
*   **Advanced Tooling:** Mature multi-file project management.
