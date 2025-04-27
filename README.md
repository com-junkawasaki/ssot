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

The vision is to evolve `.ssot` into a comprehensive Single Source of Truth not just for FSM logic, but for related concerns across distributed systems. Key development areas include:

**Near-Term Focus (Core Functionality & Refinement):**

*   **DSL Improvements & Validation:**
    *   ✅ **Implemented:** Basic ID/Name validation, Initial/Parallel/History state structure checks.
    *   🔜 **Next:**
        *   **More Validation:** Unused definition checks (actions, guards), detailed transition target validation, type checking for actions/guards/invokes, refine annotation handling (required, value types).
        *   Implement entry/exit actions, hierarchical state refinements, advanced annotations (`$deprecated`).
        *   Support for multi-file/directory projects (imports, discovery, configuration).
        *   Explicit output file mapping and manifest generation.
*   **Code Generation Enhancements (Rust, Cap'n Proto, TypeScript):**
    *   Update generators to utilize fully validated AST/SymbolTable information.
    *   Generate skeleton `guard`/`action` functions (Rust).
    *   Improved hierarchical state machine generation.
    *   Generate Cap'n Proto interfaces and basic client/server stubs.
*   **Developer Experience & Tooling:**
    *   Better error messages and diagnostics (leveraging validation results).
    *   Source map generation (linking generated code back to `.ssot`).
    *   Enhanced automated testing (especially for `build.rs` and generation edge cases).
    *   Explore alternative Rust integration via procedural macro (`#[state_machine(...)]`).

**Mid-Term Goals (Expanding Scope & Integrations):**

*   **Expanded SSOT Scope (Initial Steps):**
    *   Define basic distributed system components/services.
    *   Specify inter-service communication patterns (initially focusing on Cap'n Proto RPC/messaging).
*   **Expanded Target Formats & Integrations (Core Formats):**
    *   Visualizations (Mermaid, Graphviz DOT).
    *   Integration with common schema formats: JSON Schema, Zod Schemas.
    *   Integration with common API formats: OpenAPI, AsyncAPI.
    *   Integration with common DB Schema definitions (SQL DDL, potentially Prisma/Drizzle).
*   **Project-Specific Generation (Examples):**
    *   Generate basic Next.js routing (`app/` router), database migrations (SQL diffs).

**Long-Term Vision (Comprehensive SSOT & Ecosystem):**

*   **Full SSOT Scope:**
    *   Comprehensive Infrastructure Configuration definition (IaC targets like Terraform, CDK).
    *   Detailed integration definitions: Routing, UI/UX mapping, API calls, authorization, testing (BDD), observability, configuration/feature flags, compliance/auditing, formal verification/simulation.
*   **Broader Integrations:**
    *   Support for more standard formats: Requirements (ReqIF), Process/Architecture (BPMN, ArchiMate), System Modeling (UML/SysML), other Data/API/DB formats.
    *   Deeper platform/framework integration (e.g., `fly.toml`, `vercel.json`, Docker, K8s manifest generation).
*   **Advanced Tooling:**
    *   Mature tooling for managing complex multi-file/directory `.ssot` projects.
