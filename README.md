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
    *   **Rust Code:** State/event enums, machine struct with transition logic, callback traits (`lib.rs`).
    *   **Cap'n Proto Schemas:** `.capnp` definitions mirroring the FSM structure (`codegen_capnp.rs`).
    *   **TypeScript Types:** Interfaces and type aliases for states, events, and payloads (`codegen_ts.rs`).
    *   **XState Configuration:** TypeScript code compatible with XState v5 (`codegen_xstate.rs`).
    *   **SCXML Documents:** Standard XML representation for state machines (`codegen_scxml.rs`).
*   `crates/fsm-example`: Demonstrates usage with a sample `spec/my_fsm.ssot` and a `build.rs` script for invoking the generators.
*   `crates/ssot-linter`: (Placeholder) Intended for linting and validating `.ssot` files.

## Usage (Example Workflow)

1.  **Define FSM:** Create/edit a `.ssot` file (e.g., `crates/fsm-example/spec/my_fsm.ssot`).
2.  **Annotate:** Use annotations to specify output locations and add documentation:
    ```ssot
    @0xcafebabe12345678; // File ID (required, Cap'n Proto compatible)

    // Specify output directories (required at top-level or per machine)
    $rust_out("src/generated");
    $capnp_out("target/generated/capnp"); // Optional
    $ts_out("target/generated/ts");       // Optional
    $xstate_out("target/generated/xstate"); // Optional
    $scxml_out("target/generated/scxml");   // Optional

    $description("A simple light switch FSM.");
    stateMachine LightSwitch {
        $initial(Off);
        states {
            $description("The light is off.");
            Off @0;
            $description("The light is on.");
            On @1;
        }
        events {
            $description("Toggles the light state.");
            event Toggle @0 {}
            $description("Turns the light on with a specific brightness.");
            event TurnOn @1 {
                $description("Brightness level (0-255).");
                brightness @0 : UInt8;
            }
            $description("Turns the light off.");
            event TurnOff @2 {}
        }
        transitions {
            transition ToggleOffToOn from Off to On { on @0 Toggle; action @1 activate_light; };
            transition ToggleOnToOff from On to Off { on @0 Toggle; action @1 deactivate_light; };
            transition SpecificTurnOn from Off to On { on @1 TurnOn; action @1 activate_light_specific; };
            transition SpecificTurnOff from On to Off { on @2 TurnOff; action @1 deactivate_light_specific; };
        }
    }
    ```
3.  **Integrate with `build.rs`:** In your crate's `build.rs`, parse the `.ssot` file and call the generation functions from `fsm-codegen`. (See `crates/fsm-example/build.rs`).
4.  **Build:** Run `cargo build`. The `build.rs` script executes, generating files into the specified output directories before compiling your crate.
    *   The build script parses `.ssot`, calls `fsm_codegen::generate_*`, and writes outputs (e.g., `src/generated/light_switch.rs`, `target/generated/capnp/LightSwitch.capnp`).
    *   Your crate code (e.g., `fsm-example/src/lib.rs`) can then import the generated artifacts.

*(Refer to `fsm-example/build.rs` for a concrete implementation.)*

## Key DSL Annotations

*   **File ID:** `@0x...;`: **Required** top-level unique ID (Cap'n Proto compatible).
*   **Output Directories:**
    *   `$rust_out("path/to/dir")`: **Required** (top-level or per-machine). Generates `<MachineNameSnakeCase>.rs`.
    *   `$capnp_out("path/to/dir")`: Optional. Generates `<MachineName>.capnp`.
    *   `$ts_out("path/to/dir")`: Optional. Generates `<MachineName>.types.ts`.
    *   `$xstate_out("path/to/dir")`: Optional. Generates `<MachineName>.xstate.ts`.
    *   `$scxml_out("path/to/dir")`: Optional. Generates `<MachineName>.scxml`.
*   **Documentation:** `$description("...")`: Optional. Adds doc comments to generated Rust (`///`), Cap'n Proto (`#`), TypeScript (`/** ... */`), XState comments, SCXML `<datamodel>` comments.
*   **Initial State:** `$initial(StateName)`: **Required** on `stateMachine`. Specifies the entry state.
*   *(Others like `$version`, `$derive` might be parsed but aren't fully utilized yet.)*

## Current Status

*   ✅ DSL Parsing: States, events (with fields), transitions, basic annotations.
*   ✅ Rust Code Generation: Functional state/event enums, machine struct, basic transition logic, callback trait.
*   ✅ Cap'n Proto Schema Generation: Structures reflecting FSM states, events, and payloads.
*   ✅ TypeScript Type Generation: State unions, event discriminated unions, payload interfaces.
*   ✅ XState v5 Configuration Generation: TypeScript machine config with states, events, transitions, actions, guards.
*   ✅ SCXML Generation: Basic SCXML document structure with states and transitions.
*   ✅ Documentation Generation: From `$description` annotations for Rust, Cap'n Proto, TS, XState, SCXML.
*   ✅ Example `build.rs` Workflow: Demonstrates parsing and invoking all generators.
*   ✅ Entry/Exit Actions: Added syntax (`state S @N { entry: action1; exit: action2; }`) and Rust/XState/SCXML codegen support.
*   ✅ State Parsing Bug Fixed: The parser now correctly handles states with and without bodies, regardless of order.

## Known Issues

*   (No major known parsing or generation issues currently)

## Roadmap / Future Enhancements

The vision is to evolve `.ssot` into a comprehensive Single Source of Truth not just for FSM logic, but for related concerns across distributed systems. Key development areas include:

*   **Core DSL Enhancements:** Improving the expressiveness and robustness of the `.ssot` language itself.
    *   ✅ Add syntax for entry/exit actions on states.
    *   Enhance validation rules within the parser (e.g., duplicate name/ordinal checks, transition validity).
    *   Support for more complex annotation values or specific annotations (e.g., `$deprecated`).
    *   Support for hierarchical state machines in the DSL.
    *   Add syntax for defining distributed system components/services and their interactions (potentially referencing external `.ssot` or interface files).
    *   Support for multi-file/directory projects: Allow definitions to be split across multiple `.ssot` files, including discovery, integration, namespacing/imports, and conflict resolution.
    *   Support for directory-level configuration (e.g., via a `.ssotconfig` file).
*   **Expanding SSOT Scope (within DSL):** Defining more system aspects directly in `.ssot`.
    *   Integrate routing definition capabilities (e.g., mapping states/events to routes or defining navigation flows).
    *   Specify inter-service communication (e.g., RPC, messaging) linked to FSM events/actions, defaulting to Cap'n Proto.
    *   Define related concerns:
        *   UI/UX Component Mapping (Linking states/events to UI elements/actions).
        *   API Call / Event Integration (Defining external calls or internal events triggered by FSM).
        *   Authorization / Permissions (Specifying required roles/permissions for transitions/events).
        *   Test Scenario / BDD Definitions (Describing test cases/features based on FSM paths).
        *   Monitoring / Logging / Alerting Rules (Defining observability requirements per state/transition).
        *   Configuration Management / Feature Flag Integration (Defining or linking configuration values).
        *   Compliance / Audit Log Requirements (Specifying necessary audit trails).
        *   Formal Verification / Simulation Support (Adding annotations for model checking/simulation).
*   **Core Code Generation Enhancements:** Refining the existing generators.
    *   Generate placeholder or skeleton functions for defined `guard` and `action` attributes **in Rust**.
    *   Improve generation for hierarchical state machines (dependent on DSL enhancement).
    *   Generate Cap'n Proto interfaces (`.capnp`) for defined inter-service interactions.
    *   Generate Cap'n Proto client/server communication stubs (Rust, TypeScript, etc.).
*   **Expanded Code Generation Targets & Formats:** Supporting more output types.
    *   Explore generating visualization outputs (e.g., Mermaid syntax, Graphviz DOT) from the AST.
    *   Generate advanced documentation (state tables, sequence diagrams, etc.).
    *   Integrate generation capabilities based on various standard formats:
        *   **Requirements:** ReqIF (`.xml`), Markdown/Asciidoc (`.md`, `.adoc`)
        *   **Process/Architecture:** BPMN (`.bpmn`), ArchiMate (`.xml`)
        *   **System Modeling:** UML/SysML (XMI `.xmi`)
        *   **Data/Schema:** JSON Schema (`.json`), XSD (`.xsd`), Avro (`.avsc`), Protocol Buffers (`.proto`)
        *   **API/Interface:** OpenAPI (`.yaml`/`.json`), gRPC (`.proto`), GraphQL SDL (`.graphql`), AsyncAPI (`.yaml`/`.json`)
        *   **Database Schema:** Prisma Schema (`.prisma`), Drizzle ORM (`.ts`), Drizzle Kit Config (`drizzle.config.ts`), SQL (`.sql`), DBML (`.dbml`), Liquibase (`.xml`, `.yaml`, `.json`, `.sql`)
*   **Project-Specific Generation & Integration:** Tailoring output for specific frameworks and platforms.
    *   Generate Next.js routing configurations (e.g., `app/` directory structure, `route.ts` handlers based on FSM states/events).
    *   Generate database migration scripts (e.g., SQL `up`/`down`) based on diffs between `.ssot` file versions.
    *   Implement project-specific code generation orchestration (e.g., specifying `nextjs` target generates relevant Rust, TS, routing, migration scripts, etc.).
    *   Explore generating deployment/orchestration configuration hints (e.g., Docker Compose, Kubernetes manifests) based on the defined distributed system structure.
    *   Generate platform-specific configurations and deployment helpers (e.g., `fly.toml`, `vercel.json`, Supabase functions/config).
*   **Developer Experience & Tooling:** Improving usability and integration.
    *   Improve error messages and diagnostics from the parser and code generator.
    *   Enhance automated tests, particularly for `build.rs` logic (e.g., via integration tests) and edge cases in generation.
    *   Implement the `#[state_machine(...)]` procedural macro approach as an alternative integration method for Rust projects.
    *   Improve tooling support for managing multi-file/directory `.ssot` projects.
