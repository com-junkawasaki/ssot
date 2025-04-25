# FSM DSL and Code Generator (`.ssot`)

Define Finite State Machines (FSMs) declaratively using a dedicated Domain Specific Language (`.ssot`) and automatically generate type-safe **Rust code**, **Cap'n Proto schemas**, **TypeScript types**, and potentially more. This project uses `.ssot` files as the **Single Source of Truth** for your state machine logic, ensuring consistency across different parts of your system.

## Background & Goal

Defining complex stateful logic manually across different languages and platforms is error-prone and time-consuming. This project aims to solve this by providing:

*   **An Intuitive DSL (`.ssot`):** Clearly represent states, events, and transitions inspired by Cap'n Proto schema syntax.
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
*   **Documentation:** `$description("...")`: Optional. Adds doc comments to generated Rust (`///`), Cap'n Proto (`#`), and TypeScript (`/** ... */`). Applicable to `stateMachine`, `state`, `event`, `field`.
*   **Initial State:** `$initial(StateName)`: **Required** on `stateMachine`. Specifies the entry state.
*   *(Others like `$version`, `$derive` might be parsed but aren't fully utilized yet.)*

## Current Status

*   ✅ DSL Parsing: States, events (with fields), transitions, basic annotations.
*   ✅ Rust Code Generation: Functional state/event enums, machine struct, basic transition logic, callback trait.
*   ✅ Cap'n Proto Schema Generation: Structures reflecting FSM states, events, and payloads.
*   ✅ TypeScript Type Generation: State unions, event discriminated unions, payload interfaces.
*   ✅ Documentation Generation: From `$description` annotations for all targets.
*   ✅ Example `build.rs` Workflow: Demonstrates parsing and invoking all generators.

## Roadmap / Future Enhancements

The vision is to evolve `.ssot` into a comprehensive Single Source of Truth not just for FSM logic, but for related concerns across distributed systems. Key development areas include:

*   **DSL Improvements:** Enhance the `.ssot` language expressiveness, validation, and structure.
    *   Entry/exit actions, hierarchical states, advanced annotations (`$deprecated`).
    *   Improved validation rules (duplicate checks, transition logic).
    *   Support for multi-file/directory projects (imports, discovery, config).
    *   Explicit output file mapping and manifest generation.

*   **Expanded SSOT Scope:** Define more system aspects within the DSL.
    *   Distributed system components/services definition.
    *   Inter-service communication specification (RPC, messaging, default to Cap'n Proto).
    *   Integration with related concerns: Routing, UI/UX mapping, API calls, authorization, testing (BDD), observability (logging/monitoring), configuration/feature flags, compliance/auditing, formal verification/simulation.

*   **Code Generation Enhancements:** Refine existing generators and add new capabilities.
    *   Generate skeleton `guard`/`action` functions (Rust).
    *   Improved hierarchical state machine generation.
    *   Generate Cap'n Proto interfaces and client/server stubs.

*   **Expanded Target Formats & Integrations:** Support more output types and standard formats.
    *   Visualizations (Mermaid, Graphviz DOT), advanced documentation (state tables, sequence diagrams).
    *   Integration with standard formats: Requirements (ReqIF), Process/Architecture (BPMN, ArchiMate), System Modeling (UML/SysML), Data/Schema (JSON Schema, Protobuf, Avro, etc.), API/Interface (OpenAPI, gRPC, GraphQL, AsyncAPI), DB Schema (SQL, Prisma, Drizzle, Liquibase, etc.).

*   **Project-Specific Generation:** Tailor output for specific frameworks and platforms.
    *   Generate Next.js routing (`app/` router), database migrations (SQL diffs), platform configs (`fly.toml`, `vercel.json`), deployment hints (Docker, K8s).
    *   Implement target-based orchestration (e.g., `nextjs` target triggers Rust, TS, routing, DB migration generation).

*   **Developer Experience & Tooling:** Improve usability, diagnostics, and integration.
    *   Better error messages and diagnostics.
    *   Source map generation (linking generated code back to `.ssot`).
    *   Enhanced automated testing (especially for `build.rs` and generation edge cases).
    *   Alternative Rust integration via procedural macro (`#[state_machine(...)]`).
    *   Improved tooling for multi-file/directory `.ssot` projects.
